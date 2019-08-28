package com.odi.beranet.beraodi.Activities.cameraActivityFragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.ImageReader
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
import android.util.*
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import com.facebook.drawee.drawable.RoundedBitmapDrawable
import com.odi.beranet.beraodi.Activities.cameraActivity

import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.Permission_Result
import com.odi.beranet.beraodi.odiLib.nativePage
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

private const val _this = "param1"

class previewFragment : Fragment() {
    private val TAG: String? = "previewFragment"
    private var listener: previewFragmentInterface? = null
    private lateinit var textureView: TextureView
    private lateinit var recordButton: ImageButton
    private lateinit var changeCameraButton: ImageButton

    private var isRecording = false
    private val MAX_PREVIEW_WIDTH:Int = 1280
    private val MAX_PREVIEW_HEIGT:Int = 720
    private lateinit var  captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    private var previewSize:Point? = null

    private lateinit var cameraDevice: CameraDevice

    private var cameraPositionHolder: Camera_Position = Camera_Position.FRONT
    enum class Camera_Position (val value:Int){
        FRONT(CameraCharacteristics.LENS_FACING_FRONT),
        BACK(CameraCharacteristics.LENS_FACING_BACK)
    }

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


    /*private val mediaRecorder by lazy {
        MediaRecorder()
    }*/
    private var mediaRecorder:MediaRecorder? = null

    private lateinit var currentVideoFilePath: String

    private fun previewSession() {
        setupMediaRecorder()
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
                        captureSession.setRepeatingRequest(captureRequestBuilder!!.build(), null, null)
                    }
                }
            }, backgroundHandler)
    }


    private fun recordSession() {
        isRecording = true
        mediaRecorder!!.start()
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
        currentVideoFilePath = videoFile.absolutePath
        return videoFile
    }

    private fun setupMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder = null
        }

        mediaRecorder = MediaRecorder()
        println("$TAG orientation: ${activity?.windowManager?.defaultDisplay?.rotation}")

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
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setAudioEncodingBitRate(22050)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioChannels(2)
            setOutputFile(createVideoFile())
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(1280,720)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            prepare()
        }
    }

    private fun stopMediaRecorder() {
        mediaRecorder?.apply {
            try {
                stop()
                reset()

                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioEncodingBitRate(22050)
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
                Log.e(TAG, e.toString())
            }
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
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    private fun connectCamera() {


        val deviceId = cameraId(cameraPositionHolder.value) // Front
        println("$TAG connectCamera: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        }catch (e:CameraAccessException) {
            Log.e(TAG, e.toString())
        }catch (e: InterruptedException) {
            Log.e(TAG, "open camera device interputed while error")
        }

    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.recordButton -> onRecordButtonEvent()
            R.id.changeCamera -> onChangeCamera()
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
        val previewRectF:RectF = RectF(0F,0F,previewSize!!.y.toFloat(),previewSize!!.x.toFloat()) // yan olduğu için x ve y yer değiştirir.

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
        println("$TAG onCreateView")
        textureView = view.findViewById(R.id.textureView)
        recordButton = view.findViewById(R.id.recordButton)
        changeCameraButton = view.findViewById(R.id.changeCamera)


        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)


        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels

        var params:FrameLayout.LayoutParams = textureView.layoutParams as FrameLayout.LayoutParams
        params.height = height
        params.width = width

        textureView.layoutParams = params

        orientationListenerConfig()
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recordButton.setOnClickListener(clickListener)
        changeCameraButton.setOnClickListener(clickListener)
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

    interface previewFragmentInterface {
        fun onFragmentInteraction(uri: Uri) {}
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
        println("$TAG openCamera run")
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
        println("$TAG onChangeCamera: click")
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
            transformImage(textureView.width, textureView.height)
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
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), Permission_Result.CAMERA_PERMISSION.value)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permission_Result.CAMERA_PERMISSION.value == requestCode) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // izinler verilmiş devam
                connectCamera()

            }else {
                val alert = AlertDialog.Builder(activity)

                // Başlık
                alert.setTitle("İzinler")
                alert.setMessage("Kamera ve Ses kayıt için izin vermezseniz odi için bu özelliği kullanamazsınız.")
                alert.setCancelable(false)

                alert.setPositiveButton("Anladım") { dialogInterface: DialogInterface, i: Int ->
                }
                alert.show()
            }

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val currentOrientation = resources.configuration.orientation

        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE){
           Log.v("TAG","Landscape !!!");
        }
        else {
           Log.v("TAG","Portrait !!!");
       }
    }

    private lateinit var mOrientationListener: OrientationEventListener

    private fun orientationListenerConfig() {
        mOrientationListener = object: OrientationEventListener(this@previewFragment.activity, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                println("$TAG onOrientationChanged: $orientation")
                if(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    //println("$TAG onOrientationChanged: Ekran kilidi")
                }else {
                    //println("$TAG onOrientationChanged: Ekran Açık")
                    var degrees = 0
                    when (orientation) {
                        Surface.ROTATION_0 -> degrees = 180
                        Surface.ROTATION_90 -> degrees = 0
                        Surface.ROTATION_180 -> degrees = 0
                        Surface.ROTATION_270 -> degrees = 180
                    }
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
        if (isRecording) {
            isRecording = false
            stopRecordSession()
            println("$TAG onRecordButtonEvent stopRecording")
        }else {
            isRecording = true
            startRecordSession()
            println("$TAG onRecordButtonEvent startRecording")
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int, cameraCharacteristics: CameraCharacteristics): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 90 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 0 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                // invalid display rotation
            }
        }
        return swappedDimensions
    }

}
