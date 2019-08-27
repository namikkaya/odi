package com.odi.beranet.beraodi.Activities.cameraActivityFragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.*
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
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

    private var isRecording = false
    private val MAX_PREVIEW_WIDTH:Int = 1280
    private val MAX_PREVIEW_HEIGT:Int = 720
    private lateinit var  captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    private lateinit var cameraDevice: CameraDevice

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG , "camera açıldı")
            if(camera != null) {
                cameraDevice = camera
                previewSession()

            }

        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "camera device disconnected")
            camera.close()
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

    private val mediaRecorder by lazy {
        MediaRecorder()
    }

    private lateinit var currentVideoFilePath: String

    private fun previewSession() {
        val surfaceTexture = textureView.surfaceTexture

        surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGT)
        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(Arrays.asList(surface),
            object: CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating capture session failed!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if (session != null) {
                        captureSession = session
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    }
                }
            }, backgroundHandler)
    }

    private fun recordSession() {

        setupMediaRecorder()


        val surfaceTexture = textureView.surfaceTexture

        surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGT)
        val textureSurface = Surface(surfaceTexture)
        val recordSurface = mediaRecorder.surface

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(textureSurface)
        captureRequestBuilder.addTarget(recordSurface)

        val surfaces = ArrayList<Surface>().apply {
            add(textureSurface)
            add(recordSurface)
        }

        cameraDevice.createCaptureSession(surfaces,
            object: CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating record session failed!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if (session != null) {
                        captureSession = session
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                        try{
                            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        }catch (e:CameraAccessException){
                            println("$TAG camera problem:  ${e.toString()}")
                        }

                        isRecording = true
                        mediaRecorder.start()

                    }
                }
            }, backgroundHandler)
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
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        val sensorOrientation = cameraCharacteristics(
            cameraId(CameraCharacteristics.LENS_FACING_FRONT),
            CameraCharacteristics.SENSOR_ORIENTATION
        )

        when(sensorOrientation) {
            SENSOR_DEFAULT_ORIENTATION_DEGRESS ->
                mediaRecorder.setOrientationHint(DEFAULT_ORIENTATION.get(rotation!!))
            SENSOR_INVERSE_ORIENTATION_DEGRESS ->
                mediaRecorder.setOrientationHint(INVERSE_ORIENTATION.get(rotation!!))
        }

        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(createVideoFile())
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(1920,1080)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            prepare()
        }
    }

    private fun stopMediaRecorder() {
        mediaRecorder.apply {
            try {
                stop()
                reset()
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
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_FRONT)
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            println("$TAG onCreate init")

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_preview, container, false)
        println("$TAG onCreateView")
        textureView = view.findViewById(R.id.textureView)
        recordButton = view.findViewById(R.id.recordButton)



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
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }

        private val INVERSE_ORIENTATION = SparseIntArray().apply {
            append(Surface.ROTATION_0, 270)
            append(Surface.ROTATION_90, 180)
            append(Surface.ROTATION_180, 90)
            append(Surface.ROTATION_270, 0)
        }
    }

    fun openCamera() {
        println("$TAG openCamera run")
        checkGalleryPermission()

    }

    private fun startRecordSession() {
        recordSession()
    }

    private fun stopRecordSession() {
        stopMediaRecorder()
        previewSession()

    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        println("$TAG onResume")
        if (textureView.isAvailable) {
            println("$TAG onResume textureView.isAvailable true openCamera")
            openCamera()
        } else {
            println("$TAG onResume textureView.isAvailable false textureview surface listener add")
            textureView.surfaceTextureListener = surfaceListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private val surfaceListener = object: TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height")
            println("$TAG textureSurface width: $width height: $height")
            openCamera()
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            println("$TAG textureSurface width: $width height: $height")

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
    }

    private fun checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
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

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                    println("$TAG onOrientationChanged: Ekran kilidi")
                }else {
                    println("$TAG onOrientationChanged: Ekran Açık")
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

}
