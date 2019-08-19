package com.odi.beranet.beraodi.MainActivityMVVM

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.odi.beranet.beraodi.Activities.upload_from_gallery
import com.odi.beranet.beraodi.MainActivity
import com.odi.beranet.beraodi.models.async_upload_video
import com.odi.beranet.beraodi.models.async_upload_video_complete
import com.odi.beranet.beraodi.odiLib.*
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class videoUploadViewModel (val _this: AppCompatActivity, val listener:odiInterface?): odiInterface {
    private val TAG:String = "videoUploadViewModel"
    //var listener:odiInterface? = null

    var uploadVideoMessageHandler: Handler? = null
    var processType:nativePage? = null

    internal var outputDir2 =
        Environment.getExternalStorageDirectory().toString() + File.separator + "Odi_output" + File.separator + System.currentTimeMillis()

    override fun uploadVideoAsyncTaskComplete(resultData: async_upload_video_complete?) {
        super.uploadVideoAsyncTaskComplete(resultData)

        var message:Message = uploadVideoMessageHandler!!.obtainMessage(1,resultData)
        message.sendToTarget()
    }

    private fun uploadFile(file:File, type:nativePage) {

        uploadVideoMessageHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                val msg: async_upload_video_complete? = message.obj as? async_upload_video_complete
                msg.let { value ->
                    if (!value!!._uploadStatus!!) {
                        val progressValue:Int = value!!._uploadProgress!!
                        println("$TAG => progress: $progressValue")
                        listener?.onUploadVideoStatus("test", progressValue, false)
                    }else {
                        Thread(Runnable {
                            println("$TAG uploadVideoAsyncTaskComplete:  başarılı bir şekilde yüklendi")
                            request(value!!.requestPath!!)
                        }).start()
                    }

                }
            }
        }

        if (singleton.userId != null) {
            val myModel = async_upload_video("denemeID", file, this, type, singleton.userId!!)
            var upload = asyncUploadFile().execute(myModel)
        }

    }

    /**
     *
     * */
    fun getImageUrlWithAuthority(context: Context, uri: Uri, type:nativePage):String? {
        var inputStream:InputStream? = null

        if (uri.authority != null) {
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                saveToVideo(inputStream, outputDir2, type)
            }catch (e:FileNotFoundException) {
                e.printStackTrace()
            }finally {
                try {
                    if (inputStream != null) {
                        inputStream?.close()
                    }
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }

        }else {
            println("$TAG uri null değerinde")
        }
        return null
    }

    private fun saveToVideo(_inPath: InputStream, outputPath:String, type:nativePage) {
        var inputStream: InputStream? = _inPath
        var output: OutputStream?  = null

        try {
            println("$TAG hata burada....1")
            val dir = File(outputPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            println("$TAG hata burada....2")

            output = FileOutputStream("$outputPath.mp4")
            println("$TAG hata burada....3")

            var buffer = ByteArray(1024)
            if (inputStream != null) {

                while (true){
                    val read:Int? = inputStream?.read(buffer!!)
                    if (read!! <= 0)
                        break

                    output.write(buffer, 0, read)
                }
            }

            inputStream?.close()
            inputStream = null

            output.flush()
            output.close()
            output = null

            val newFile = File("$outputPath.mp4")

            if (newFile.exists()) {
                println("$TAG video dosyası var bulundu")
                uploadFile(newFile,type)
            }else {
                println("$TAG video dosyası yok bulunamadı")
            }

        }catch (e: FileNotFoundException) {
            Log.e("VideoComp", e.message)
        }
        catch (e: Exception) {
            Log.e("VideoComp", e.message)
        }
    }

    internal fun check_writeRead_permission(completion: (Boolean?) -> Unit) {
        if (ContextCompat.checkSelfPermission(_this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(_this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            completion(true)
        } else {
            completion(false)
            ActivityCompat.requestPermissions(_this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), Permission_Result.UPLOAD_VIDEO_GALLERY.value)
        }
    }

    @Throws(IOException::class)
    fun request(uri: String): String {
        val ab = StringBuilder()
        val url = URL(uri)
        val conn = url.openConnection() as HttpURLConnection
        try {

            while (true){

                val inStr = BufferedInputStream(conn.inputStream)
                val bf = BufferedReader(InputStreamReader(inStr))

                val inputLine: String? = bf.readLine()
                if (inputLine!! != "")
                    break
                ab.append(inputLine)
                println("$TAG async: request complete ")
                listener?.onUploadVideoStatus("test", null, true)
            }

        } finally {
            conn.disconnect()
        }


        return ab.toString()
    }





}