package com.odi.beranet.beraodi.MainActivityMVVM

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.odi.beranet.beraodi.MainActivity
import com.odi.beranet.beraodi.odiLib.Permission_Result
import com.odi.beranet.beraodi.odiLib.nativePage
import java.io.*
import java.lang.Exception

class videoUploadViewModel (val _this: MainActivity) {
    private val TAG:String = "videoUploadViewModel"

    internal var outputDir2 =
        Environment.getExternalStorageDirectory().toString() + File.separator + "Odi_output" + File.separator + System.currentTimeMillis()


    fun getImageUrlWithAuthority(context: Context, uri: Uri):String? {
        var inputStream:InputStream? = null

        if (uri.authority != null) {
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                saveToVideo(inputStream, outputDir2)
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

    private fun saveToVideo(_inPath: InputStream, outputPath:String) {
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
}