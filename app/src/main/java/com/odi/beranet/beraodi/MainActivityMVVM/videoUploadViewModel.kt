package com.odi.beranet.beraodi.MainActivityMVVM

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.WebViewClient
import com.odi.beranet.beraodi.models.async_upload_video
import com.odi.beranet.beraodi.models.async_upload_video_complete
import com.odi.beranet.beraodi.odiLib.*
import com.vincent.videocompressor.VideoCompress
import kotlinx.android.synthetic.main.activity_upload_from_gallery.*
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


class videoUploadViewModel (val _this: AppCompatActivity, val listener:odiInterface?): odiInterface {
    private val TAG:String = "videoUploadViewModel"
    //var listener:odiInterface? = null

    var uploadId:String? = null
    var uploadVideoMessageHandler: Handler? = null

    internal var outputDir =
        Environment.getExternalStorageDirectory().toString() + File.separator + "Odi" + File.separator + System.currentTimeMillis()
    internal var outputDir2 =
        Environment.getExternalStorageDirectory().toString() + File.separator + "Odi_output" + File.separator + System.currentTimeMillis()
    internal var outputDir3 =
        Environment.getExternalStorageDirectory().toString() + File.separator + "Odi_output_test" + File.separator + System.currentTimeMillis()
    var videoUri:Uri? = null
    var pageType:nativePage? = null
    var context:Context? = null

    override fun uploadVideoAsyncTaskComplete(resultData: async_upload_video_complete?) {
        super.uploadVideoAsyncTaskComplete(resultData)

        var message:Message = uploadVideoMessageHandler!!.obtainMessage(1,resultData)
        message.sendToTarget()
    }

    private fun uploadFile(file:File, type:nativePage, uploadType:UPLOAD_FILE_TYPE) {

        uploadVideoMessageHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                val msg: async_upload_video_complete? = message.obj as? async_upload_video_complete
                msg.let { value ->

                    if (!value!!._uploadStatus!!) { // false ise yükleme yapılmaya devam ediliyor
                        val progressValue:Int = value!!._uploadProgress!!
                        val tipi = value._uploadFileType
                        if (value._uploadFileType!! == UPLOAD_FILE_TYPE.bitmap) {
                            listener?.onUploadBitmapStatus(uploadId, progressValue,false)
                        }else {
                            listener?.onUploadVideoStatus(uploadId,progressValue,false)
                        }

                    }else {
                        Thread(Runnable { // yükleme işlemi bitti request atılacak
                            println("$TAG uploadVideoAsyncTaskComplete:  başarılı bir şekilde yüklendi")
                            if (value._uploadFileType == UPLOAD_FILE_TYPE.video) {
                                request(value!!.requestPath!!)
                            }else {
                                // image yüklendi video işlemi için çağrı
                                listener?.onUploadBitmapStatus(uploadId,null,true)
                                // video yükleme işlemi başlatılıyor
                                /*videoProcessthis@videoUploadViewModel.context!!,
                                    this@videoUploadViewModel.videoUri!!,
                                    this@videoUploadViewModel.pageType!!*/
                                videoUploadStart(this@videoUploadViewModel.context!!,
                                this@videoUploadViewModel.videoUri!!,
                                this@videoUploadViewModel.pageType!!)
                            }
                        }).start()
                    }

                }
            }
        }





        if (singleton.userId != null) {
            println("TİP: $uploadType")
            val myModel = async_upload_video("denemeID", file, this, type, singleton.userId!!, uploadType!!)
            var upload = asyncUploadFile().execute(myModel)
        }

    }

    /**
     * @param _uploadId : işlem id si
     * @param context: activity
     * @param uri: video uri
     * @param type: nativePage
     * 1. işlem createBitmap
     * 2. işlem upload bitmap
     * 3. işlem videoCompress
     * 4. işlem upload video
     * */
    fun getImageUrlWithAuthority(_uploadId:String, context: Context, uri: Uri, type:nativePage):String? {
        this.uploadId = _uploadId
        this.videoUri = uri
        this.pageType = type
        this.context = context

        val thumb = getThumbnail(uri)
        _this.testImage.setImageBitmap(thumb)

        val thumbFile = bitmapToFile(thumb)

        uploadFile(thumbFile, pageType!!, UPLOAD_FILE_TYPE.bitmap)

        /*
        webView?.webViewClient = object : WebViewClient(){

        }*/


        return null
    }

    fun videoUploadStart(context: Context, uri:Uri, type:nativePage) {
        val newListener = object: VideoCompress.CompressListener {
            override fun onStart() {
                println("Dinleyici : onStart")
            }

            override fun onSuccess() {
                println("Dinleyici : onSuccess")
                videoProcess(context, uri, type)
            }

            override fun onFail() {
                println("Dinleyici : onFail")
            }

            override fun onProgress(percent: Float) {
                println("Dinleyici : onProgress: $percent")
            }

        }


        VideoCompress.compressVideoLow(getRealPathFromURI(_this.applicationContext,uri),outputDir3, newListener)
    }


    fun getRealPathFromURI(context: Context, contentUri: Uri): String {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } finally {
            cursor?.close()
        }
    }

    private fun bitmapToFile(bitmap:Bitmap): File {
        val wrapper = ContextWrapper(_this.applicationContext)

        var file = wrapper.getDir("Images",Context.MODE_PRIVATE)
        file = File(file,"mdroid.jpg")

        try{
            // Compress the bitmap and save in jpg format
            val stream:OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
            e.printStackTrace()
        }

        val returningFile:File = File(file.absolutePath)
        // Return the saved bitmap uri
        return returningFile
    }


    // video işlemi yapılıyor
    private fun videoProcess(context: Context, uri:Uri, type:nativePage) {


        var inputStream:InputStream? = null

        if (uri.authority != null) {
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                saveToVideo(inputStream, outputDir3, type) // outputDir2 çalışan versiyon
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
    }

    private fun saveToVideo(_inPath: InputStream, outputPath:String, type:nativePage) {
        var inputStream: InputStream? = _inPath
        var output: OutputStream?  = null

        try {
            val dir = File(outputPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            output = FileOutputStream("$outputPath.mp4")

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
                uploadFile(newFile,type,UPLOAD_FILE_TYPE.video)
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
                //listener?.onUploadVideoStatus(uploadId, null, true)
                listener?.onUploadVideoStatus(uploadId,null,true)
            }

        } finally {
            conn.disconnect()
        }


        return ab.toString()
    }


    private fun getThumbnail(uri:Uri):Bitmap{
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = _this.applicationContext.contentResolver.query(uri, filePathColumn, null, null, null)
        cursor.moveToFirst()

        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
        val picturePath = cursor.getString(columnIndex)
        cursor.close()

        val bitmap = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MINI_KIND)
        return bitmap
    }

    private fun bitmapToFile(bitmap: Bitmap, name: String):File?{
        val filesDir = _this.applicationContext.getFilesDir()
        val imageFile = File(filesDir, "$name.jpg")

        val os: OutputStream
        return try {
            os = FileOutputStream(imageFile)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()

            imageFile
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Error writing bitmap", e)
            null
        }


    }

}