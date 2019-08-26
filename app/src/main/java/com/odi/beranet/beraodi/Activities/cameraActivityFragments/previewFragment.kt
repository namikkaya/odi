package com.odi.beranet.beraodi.Activities.cameraActivityFragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.*
import com.odi.beranet.beraodi.Activities.cameraActivity

import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.Permission_Result
import com.odi.beranet.beraodi.odiLib.nativePage
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Semaphore

private const val _this = "param1"

class previewFragment : Fragment(), TextureView.SurfaceTextureListener {

    private val TAG: String? = "previewFragment"
    private var listener: previewFragmentInterface? = null
    private lateinit var textureView: TextureView

    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun <T> cameraCharacteristics(cameraId:String, key:CameraCharacteristics.Key<T>) :T {

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when(key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
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
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_BACK)
        println("$TAG connectCamera: $deviceId")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_preview, container, false)
        println("$TAG onCreateView")
        textureView = view.findViewById(R.id.textureView)
        checkGalleryPermission()
        return view
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
    }


    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        println("$TAG onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        println("$TAG onSurfaceTextureUpdated")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        println("$TAG onSurfaceTextureDestroyed")
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        println("$TAG onSurfaceTextureAvailable width: $width -- height: $height")
        openCamera()
    }

    fun openCamera() {
        println("$TAG openCamera run")
        connectCamera()
    }

    override fun onResume() {
        super.onResume()
        if (textureView.isAvailable) {
            println("$TAG onResume openCamera")
            openCamera()
        } else {
            println("$TAG onResume listener")
            textureView.surfaceTextureListener = this
        }
    }

    override fun onPause() {
        super.onPause()
    }


    private fun checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
            println("$TAG checkGalleryPermission: OKEY")
            openCamera()
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
                openCamera()

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


}
