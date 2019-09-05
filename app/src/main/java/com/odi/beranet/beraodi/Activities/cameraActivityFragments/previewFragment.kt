package com.odi.beranet.beraodi.Activities.cameraActivityFragments

import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.*
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.CameraViewModel
import com.odi.beranet.beraodi.models.playlistDataModel
import com.odi.beranet.beraodi.odiLib.*
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val _this = "param1"

class previewFragment : Fragment(), odiMediaManager.odiMediaManagerListener, countDownManager.countDownManagerListener {

    interface previewFragmentInterface {
        /**
         * @param status => true ise uyarıyı aç false ise uyarıyı kapa
         */
        fun onPreviewFragment_orientationInfo(status: Boolean) {}

        /**
         * Camera close event
         */
        fun onPreviewFragment_closeEvent() {}

        /**
         * Record success
         */
        fun onPreviewFragment_Record_Success(path:Uri?) {}
    }

    private var myMediaManager:odiMediaManager? = null

    // desing
    private var uiDesingHolder = UIDESIGN.NORMAL
    enum class UIDESIGN {
        NORMAL,
        LOCK,
        RECORDING,
        ENDING,
        COUNTDOWN
    }

    private var myScrollPositionManager: scrollPositionManager? = null

    private var textContainerVisible:Boolean = false
    private val TAG: String? = "previewFragment"
    private var listener: previewFragmentInterface? = null
    private lateinit var textureView: TextureView
    private lateinit var recordButton: ImageButton
    private lateinit var changeCameraButton: ImageButton
    private lateinit var cameraCloseButton: ImageButton
    private lateinit var textControlButton: ImageButton
    private lateinit var nextButton:ImageButton
    private lateinit var textContainer: RelativeLayout
    private lateinit var subtitleText: TextView
    private lateinit var countDownObject: RelativeLayout
    private lateinit var volumeButton: ImageButton
    private var timerManager:countDownManager? = null
    private var isRecording:Boolean = false
    private val MAX_PREVIEW_WIDTH:Int = 1280
    private val MAX_PREVIEW_HEIGT:Int = 720
    private lateinit var  captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    var recordTypeHolder:RECORD_TYPE? = null // dialog / monolog /play mode hangisinde kayıt yapılıyorsa

    private var previewSize:Point? = null

    private lateinit var cameraDevice: CameraDevice

    private lateinit var mOrientationListener: OrientationEventListener

    private var cameraPositionHolder: Camera_Position = Camera_Position.FRONT
    enum class Camera_Position (val value:Int){
        FRONT(CameraCharacteristics.LENS_FACING_FRONT),
        BACK(CameraCharacteristics.LENS_FACING_BACK)
    }



    /**
     * ekran duruşunu simgeler
     */
    enum class Orientation_Status {
        LAND_SCAPE,
        OTHER
    }

    private var mOrientationStatus:Orientation_Status? = null

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG , "managerProblem ")
            if(camera != null) {
                cameraDevice = camera

                previewSession()

            }

        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "camera device disconnected")
            camera.close()
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "camera device error")
            this@previewFragment.activity!!.finish()
        }
    }

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var mediaRecorder:MediaRecorder? = null

    private var currentVideoFilePath: String = ""

    private fun previewSession() {
        setupMediaRecorder()

        try {
            val surfaceTexture = textureView.surfaceTexture

            surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGT)
            val surface = Surface(surfaceTexture)

            val recordSurface = mediaRecorder!!.surface

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD) // TEMPLATE_PREVIEW
            captureRequestBuilder.addTarget(surface)
            captureRequestBuilder.addTarget(recordSurface)
            val surfaces = ArrayList<Surface>().apply {
                add(surface)
                add(recordSurface)
            }

            cameraDevice.createCaptureSession(surfaces,
                object: CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "creating capture session failed!")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        if (session != null) {
                            captureSession = session

                            captureRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            try{
                                captureSession.setRepeatingRequest(captureRequestBuilder!!.build(), null, null)
                            }catch (e:IllegalStateException){
                                Log.e(TAG, e.toString()+ " - previewSession captureSession")
                            }
                        }
                    }
                }, backgroundHandler)
        }catch (e:IllegalStateException){
            Log.e(TAG, e.toString() + " - previewSession 2")
        }catch (e:NullPointerException){
            Log.e(TAG, e.toString() + " - previewSession 3")
        }

    }

    private fun recordSession() {
        isRecording = true
        mediaRecorder!!.start()
        uiCameraDesing(UIDESIGN.RECORDING)
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized) {
            captureSession.close()
        }

        if (this::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2 Kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()

        try {
            backgroundThread.join()
        }catch (e: InterruptedException){
            Log.e(TAG, e.toString())
        }

    }

    private fun createVideoFileName():String {
        val timestamp = SimpleDateFormat("yyMMdd_HHmmss").format(Date())
        return "VIDEO_${timestamp}.mp4"
    }

    private fun createVideoFile():File {

        val videoFile = File(context?.filesDir, createVideoFileName())
        if (currentVideoFilePath == "") {
            currentVideoFilePath = videoFile.absolutePath
        }

        return videoFile
    }

    private fun getDirFile() {
        val letDirectory = File(context?.filesDir, "")
        //println("allfiles: ${letDirectory.mkdirs()}")
        val files = letDirectory.listFiles()
        //println("allfiles item files: ${files}")
        for (i in 0 until files.size){
            if (files[i].name.endsWith(".mp4")){
                files[i].delete()
            }
            //println("allfiles item: ${files[i].absolutePath}")
        }
    }

    private fun setupMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaRecorder = null
            }catch (e:IllegalStateException) {
                Log.e(TAG, e.toString() + " setupMediaRecorder")
            }

        }

        mediaRecorder = MediaRecorder()

        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        val sensorOrientation = cameraCharacteristics(
            cameraId(cameraPositionHolder.value), //LENS_FACING_FRONT
            CameraCharacteristics.SENSOR_ORIENTATION
        )

        when(sensorOrientation) {
            SENSOR_DEFAULT_ORIENTATION_DEGRESS -> {
                if (Camera_Position.BACK == cameraPositionHolder) {
                    mediaRecorder!!.setOrientationHint(INVERSE_ORIENTATION.get(rotation!!))
                }else {
                    mediaRecorder!!.setOrientationHint(DEFAULT_ORIENTATION.get(rotation!!))
                }
            }

            SENSOR_INVERSE_ORIENTATION_DEGRESS -> {
                mediaRecorder!!.setOrientationHint(INVERSE_ORIENTATION.get(rotation!!))
            }
        }

        mediaRecorder?.apply {

            try {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioChannels(2)
                setOutputFile(createVideoFile())
                setVideoEncodingBitRate(10000000)
                setVideoFrameRate(30)
                setVideoSize(1280,720)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                prepare()

            }catch (e:IllegalStateException){
                Log.e(TAG, e.toString() + " mediaRecorder 1")
            }catch (e:java.lang.RuntimeException) {
                Log.e(TAG, e.toString() + " mediaRecorder 2")
            }

        }

        /// orientation setup
        orientationListenerConfig()
    }

    private fun stopMediaRecorder() {
        if (mediaRecorder != null) {
            // test için kapatıldı
            /*try{
                mediaRecorder?.apply {
                    try {
                        stop()
                        reset()

                        setVideoSource(MediaRecorder.VideoSource.SURFACE)
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setAudioEncodingBitRate(44100)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioChannels(2)
                        setOutputFile(createVideoFile())
                        setVideoEncodingBitRate(10000000)
                        setVideoFrameRate(30)
                        setVideoSize(1280,720)
                        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)


                        prepare()
                    }catch (e:IllegalStateException){
                        Log.e(TAG, e.toString() + " stopMediaRecorder")
                    }
                }
            }catch(stopException:RuntimeException){
                Log.e(TAG, stopException.toString())
            }*/
        }
    }

    private fun <T> cameraCharacteristics(cameraId:String, key:CameraCharacteristics.Key<T>) :T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when(key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            CameraCharacteristics.SENSOR_ORIENTATION -> characteristics.get(key)
            else-> throw IllegalArgumentException("key not recognized")
        }
    }

    private fun cameraId(lens:Int):String {
        var deviceId = listOf<String>()

        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter { lens == cameraCharacteristics(it,CameraCharacteristics.LENS_FACING) }
        }catch (e:CameraAccessException){
            Log.e(TAG, e.toString() + " cameraId")
        }
        return deviceId[0]
    }

    private fun connectCamera() {

        uiCameraDesing(UIDESIGN.NORMAL)
        transformImage(textureView.width, textureView.height)

        val deviceId = cameraId(cameraPositionHolder.value) // Front
        println("$TAG connectCamera: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        }catch (e:CameraAccessException) {
            Log.e(TAG, e.toString() + " - connectCamera")
        }catch (e: InterruptedException) {
            Log.e(TAG, e.toString() + " - connectCamera2")
        }

    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.recordButton -> onRecordButtonEvent()
            R.id.changeCamera -> onChangeCamera()
            R.id.cameraCloseButton -> onCloseCameraButtonEvent()
            R.id.textControlButton -> onTextControlButtonEvent()
            R.id.nextStepButton -> onNextButtonEvent()
            R.id.volumeButton -> onVolumeButtonEvent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            println("$TAG onCreate init")

        }
    }

    private fun transformImage(width:Int, height:Int) {

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)

        var _width = displayMetrics.widthPixels
        var _height = displayMetrics.heightPixels

        previewSize = Point(_width,_height)
        println("$TAG orientation: previewSize: width: ${previewSize!!.x} height: ${previewSize!!.y}")

        if (previewSize == null || textureView == null) {
            println("$TAG orientation: transformImage return ")
            return
        }
        val matrix = Matrix()
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        val textureRectF = RectF(0F,0F,width.toFloat(),height.toFloat())

        var previewRectF:RectF = RectF(0F,0F,previewSize!!.y.toFloat(),previewSize!!.x.toFloat()) // yan olduğu için x ve y yer değiştirir.

        val centerX = textureRectF.centerX()
        val centerY = textureRectF.centerY()
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            println("$TAG orientation: in if ")
            previewRectF.offset(centerX-previewRectF.centerX(), centerY - previewRectF.centerY())
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL)
            val scale = Math.max(width.toFloat() / previewSize!!.x.toFloat(), height.toFloat() / previewSize!!.y.toFloat() )
            matrix.postScale(scale,scale, centerX,centerY)
            matrix.postRotate(90F*(rotation-2),centerX,centerY)
        }

        textureView.setTransform(matrix)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_preview, container, false)
        textureView = view.findViewById(R.id.textureView)
        recordButton = view.findViewById(R.id.recordButton)
        changeCameraButton = view.findViewById(R.id.changeCamera)
        cameraCloseButton = view.findViewById(R.id.cameraCloseButton)
        textControlButton = view.findViewById(R.id.textControlButton)
        textContainer = view.findViewById(R.id.textViewContainer)
        subtitleText = view.findViewById(R.id.subtitleTextView)
        countDownObject = view.findViewById(R.id.countDown)
        nextButton = view.findViewById(R.id.nextStepButton)
        volumeButton = view.findViewById(R.id.volumeButton)
        subtitleText.movementMethod = ScrollingMovementMethod()

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)

        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels

        var params:FrameLayout.LayoutParams = textureView.layoutParams as FrameLayout.LayoutParams
        params.height = height
        params.width = width

        textureView.layoutParams = params

        uiCameraDesing(UIDESIGN.LOCK)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        timerManager = countDownManager(countDownObject, this.activity!!)
        timerManager?.listener = this

        myScrollPositionManager = scrollPositionManager()

        recordButton.setOnClickListener(clickListener)
        changeCameraButton.setOnClickListener(clickListener)
        cameraCloseButton.setOnClickListener(clickListener)
        textControlButton.setOnClickListener(clickListener)
        nextButton.setOnClickListener(clickListener)
        volumeButton.setOnClickListener(clickListener)

        // video unutma
        getDirFile()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is previewFragmentInterface) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        @JvmStatic
        fun newInstance() =
            previewFragment().apply {
                arguments = Bundle().apply {

                }
            }
        private val SENSOR_DEFAULT_ORIENTATION_DEGRESS = 90
        private val SENSOR_INVERSE_ORIENTATION_DEGRESS = 270
        private val DEFAULT_ORIENTATION = SparseIntArray().apply {
            append(Surface.ROTATION_0, 270)
            append(Surface.ROTATION_90, 180)
            append(Surface.ROTATION_180, 90)
            append(Surface.ROTATION_270, 0)
        }

        private val INVERSE_ORIENTATION = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }
    }

    fun openCamera() {

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels
        previewSize = Point(width,height)

        checkCameraPermission()
    }

    private fun startRecordSession() {
        recordSession()
    }

    private fun stopRecordSession() {
        stopMediaRecorder()
        previewSession()
        uiCameraDesing(UIDESIGN.NORMAL)
        // thumbnail
        //createRoundThumb() // bitmap olarak dönecek
    }

    private fun createVideoThumb() = ThumbnailUtils.createVideoThumbnail(currentVideoFilePath, MediaStore.Video.Thumbnails.MICRO_KIND)
    private fun createRoundThumb(): android.support.v4.graphics.drawable.RoundedBitmapDrawable {
        val drawable = RoundedBitmapDrawableFactory.create(resources, createVideoThumb())
        drawable.isCircular = true
        return drawable
    }

    private fun onChangeCamera() {
        vibratePhone()
        if (cameraPositionHolder == Camera_Position.FRONT) {
            cameraPositionHolder = Camera_Position.BACK
            removeCamera()
            setupCamera()
        }else {
            cameraPositionHolder = Camera_Position.FRONT
            removeCamera()
            setupCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        setupCamera()
    }

    override fun onPause() {
        removeCamera()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeCamera()
        if (myMediaManager != null) {
            myMediaManager?.stopAnimation()
            myMediaManager = null
        }

    }

    private fun removeCamera() {
        stopRecordSession()
        stopBackgroundThread()
        closeCamera()
        mOrientationListener.disable()
    }

    private fun setupCamera() {
        startBackgroundThread()
        if (textureView.isAvailable) {
            println("$TAG onResume textureView.isAvailable true openCamera")
            openCamera()
        } else {
            println("$TAG onResume textureView.isAvailable false textureview surface listener add")
            textureView.surfaceTextureListener = surfaceListener
        }
    }

    private val surfaceListener = object: TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height")
            println("$TAG textureSurface width: $width height: $height")
            transformImage(width, height)
            openCamera()
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            println("$TAG textureSurface width: $width height: $height")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity!!, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            println("$TAG checkGalleryPermission: OKEY")
            connectCamera()
        } else {
            println("$TAG checkGalleryPermission: FAIL")
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                Permission_Result.CAMERA_PERMISSION.value)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permission_Result.CAMERA_PERMISSION.value == requestCode) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // izinler verilmiş devam
                connectCamera()
            }else {
                val alert = AlertDialog.Builder(activity)
                alert.setTitle("İzinler")
                alert.setMessage("Kamera ve Ses kayıt için izin vermezseniz odi için bu özelliği kullanamazsınız.")
                alert.setCancelable(false)

                alert.setPositiveButton("Anladım") { dialogInterface: DialogInterface, i: Int ->
                }
                alert.show()
            }
        }
    }

    private fun orientationListenerConfig() {
        mOrientationListener = object: OrientationEventListener(this@previewFragment.activity, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation in 235..305) { // arasında
                    rotationStatus(Orientation_Status.LAND_SCAPE)
                }else {
                    rotationStatus(Orientation_Status.OTHER)
                }
            }
        }

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        } else {
            mOrientationListener.disable()
        }

    }

    private fun onRecordButtonEvent() {
        vibratePhone()
        if (isRecording) {
            isRecording = false
            stopRecordSession()

            if (myMediaManager != null) {
                myMediaManager!!.stopAnimation()
            }
            Log.d(TAG, "stopRecording")
            // işlem buraya
            val myUri = Uri.fromFile(File(currentVideoFilePath))
            listener?.onPreviewFragment_Record_Success(myUri)

        }else {
           startCountDown()
        }
    }

    private fun startRecording() {
        isRecording = true
        startRecordSession()

        // alt yazı başlatılıyor...
        if (myMediaManager != null) {
            myMediaManager?.startDialog()
        }
    }

    private fun onCloseCameraButtonEvent() {
        listener?.onPreviewFragment_closeEvent()
    }

    private fun onTextControlButtonEvent() {
        vibratePhone()
        if (textContainer.visibility == View.VISIBLE) {
            textControlButton.setImageResource(R.drawable.textoff)
            textContainerVisible = false
            textContainer.visibility = View.INVISIBLE
        }else {
            textControlButton.setImageResource(R.drawable.texton)
            textContainerVisible = true
            textContainer.visibility = View.VISIBLE
        }
    }

    var volumeStatus:Boolean = true

    private fun onVolumeButtonEvent() {
        vibratePhone()
        if (volumeStatus) {
            volumeButton.setImageResource(R.drawable.ses_kapali)
            volumeStatus = false
            if (myMediaManager != null) {
                myMediaManager?.onSetVolume(false)
            }
        }else {
            volumeButton.setImageResource(R.drawable.ses_acik)
            volumeStatus = true
            if (myMediaManager != null) {
                myMediaManager?.onSetVolume(true)
            }
        }
    }

    private fun rotationStatus(status:Orientation_Status) {
        if (mOrientationStatus != null) {
            if (status != mOrientationStatus) {
                // yeni bir hareket
                mOrientationStatus = status
                activityOrientationDesing(mOrientationStatus)
            }
        }else {
            // yeni bir hareket
            mOrientationStatus = status
            activityOrientationDesing(mOrientationStatus)
        }

    }

    private fun activityOrientationDesing(desingStatus: Orientation_Status?) {
        if (isRecording || uiDesingHolder == UIDESIGN.COUNTDOWN || uiDesingHolder == UIDESIGN.LOCK) {
            return
        }
        when(desingStatus) {
            Orientation_Status.LAND_SCAPE -> {
                println("$TAG rotation: EKRAN AÇIK")
                listener?.onPreviewFragment_orientationInfo(false)
            }
            Orientation_Status.OTHER -> {
                println("$TAG rotation: EKRAN KAPALI")
                listener?.onPreviewFragment_orientationInfo(true)
            }
        }
    }

    private fun buttonEnabled(status:Boolean) {
        recordButton.isEnabled = status
        changeCameraButton.isEnabled = status
        if (status) {
            recordButton.alpha = 1F
            changeCameraButton.alpha = 1F
        }else {
            recordButton.alpha = 0.5F
            changeCameraButton.alpha = 0.5F
        }
    }

    private fun uiCameraDesing(status:UIDESIGN) {
        when(status){
            UIDESIGN.NORMAL -> {
                uiDesingHolder = UIDESIGN.NORMAL
                buttonEnabled(true)
                recordButton.setImageResource(R.drawable.rec)
                textContainer.visibility = View.INVISIBLE
                nextButton.visibility = View.INVISIBLE
                cameraCloseButton.visibility = View.VISIBLE
                changeCameraButton.visibility = View.VISIBLE
                volumeButton.visibility = View.VISIBLE
            }
            UIDESIGN.RECORDING -> {
                uiDesingHolder = UIDESIGN.RECORDING
                buttonEnabled(true)
                recordButton.setImageResource(R.drawable.stop)
                textContainer.visibility = View.VISIBLE
                changeCameraButton.visibility = View.INVISIBLE
                volumeButton.visibility = View.INVISIBLE
            }
            UIDESIGN.ENDING -> {
                uiDesingHolder = UIDESIGN.ENDING
                textContainer.visibility = View.INVISIBLE
                nextButton.visibility = View.INVISIBLE
                if (myMediaManager != null) {
                    myMediaManager!!.stopAnimation()
                }
            }
            UIDESIGN.LOCK -> {
                uiDesingHolder = UIDESIGN.LOCK
                buttonEnabled(false)
            }
            UIDESIGN.COUNTDOWN -> {
                uiDesingHolder = UIDESIGN.COUNTDOWN
                buttonEnabled(false)
                cameraCloseButton.visibility = View.INVISIBLE
                changeCameraButton.visibility = View.INVISIBLE
                volumeButton.visibility = View.INVISIBLE
            }
        }
    }

    // datalar indiriliyor ...
    fun getData(dataList:playlistDataModel){
        println("$TAG getData: ${dataList.type}")
        recordTypeHolder = dataList.type
        myMediaManager = odiMediaManager(dataList)
        myMediaManager!!.listener = this
        myMediaManager!!.prepare()
    }

    // -- Monolog başladı

    override fun odiMediaManagerListener_monologText(subtitle: SpannableString?, charIndex: Int?) {
        super.odiMediaManagerListener_monologText(subtitle, charIndex)
        subtitleText.setText(subtitle,TextView.BufferType.SPANNABLE)

        if (myScrollPositionManager != null) {
            myScrollPositionManager?.positionManage(subtitleText,charIndex)
        }
    }

    override fun odiMediaManagerListener_monologTextComplete() {
        super.odiMediaManagerListener_monologTextComplete()
        println("$TAG ÇEKİM BİTTİ MONOLOG")
        isRecording = false
        stopRecordSession()
        uiCameraDesing(UIDESIGN.ENDING)
        val myUri = Uri.fromFile(File(currentVideoFilePath))
        listener?.onPreviewFragment_Record_Success(myUri)
    }

    // -- Monolog bitti

    // -- Dialog başladı
    override fun odiMediaManagerListener_dialogText(subtitle: SpannableString?, charIndex: Int?) {
        super.odiMediaManagerListener_dialogText(subtitle, charIndex)

        subtitleText.post(Runnable {
            subtitleText.setText(subtitle,TextView.BufferType.SPANNABLE)
        })
        if (myScrollPositionManager != null) {
            myScrollPositionManager?.positionManage(subtitleText,charIndex)
        }
    }

    override fun odiMediaManagerListener_dialogTextComplete() {
        super.odiMediaManagerListener_dialogTextComplete()
        println("$TAG ÇEKİM BİTTİ DİALOG")
        isRecording = false
        stopRecordSession()
        uiCameraDesing(UIDESIGN.ENDING)
        val myUri = Uri.fromFile(File(currentVideoFilePath))
        listener?.onPreviewFragment_Record_Success(myUri)
    }

    override fun odiMediaManagerListener_nextButtonVisible(status: Boolean?) {
        super.odiMediaManagerListener_nextButtonVisible(status)
        if (status != null) {
            if (status) {
                nextButton.visibility = View.VISIBLE
            }else {
                nextButton.visibility = View.INVISIBLE
            }
        }
    }

    override fun odiMediaManagerListener_clearText() {
        super.odiMediaManagerListener_clearText()
        subtitleText.post(Runnable {
            println("$TAG characterCounter odiMediaManagerListener_clearText")
            val spannable = SpannableString("")
            subtitleText.setText(spannable,TextView.BufferType.SPANNABLE)
            subtitleText.post {
                val scrollAmount = subtitleText.layout.getLineTop(subtitleText.lineCount) - subtitleText.height
                subtitleText.scrollTo(0, scrollAmount)
            }
            if (myScrollPositionManager != null) {
                myScrollPositionManager?.stop()
            }
        })
    }

    // -- Dialog bitti

    private fun startCountDown() {
        countDownObject.visibility = View.VISIBLE
        uiCameraDesing(UIDESIGN.COUNTDOWN)
        timerManager?.startCountDown()
    }

    // Gerisayım -- Countdown delegate
    override fun onCountDownManagerListener_progress(count: String) {
        super.onCountDownManagerListener_progress(count)
    }

    override fun onCountDownManagerListener_complete() {
        super.onCountDownManagerListener_complete()
        countDownObject.visibility = View.INVISIBLE
        println("$TAG onCountDownManagerListenerComplete: ")
        startRecording()
        uiCameraDesing(UIDESIGN.RECORDING)
    }

    public fun onRecordStopTrigger() {
        isRecording = false
        stopRecordSession()
    }

    private fun onNextButtonEvent() {
        vibratePhone()
        println("$TAG onNextButtonEvent")
        myMediaManager?.onNext()
    }



}
