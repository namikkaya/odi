package com.odi.beranet.beraodi.MainActivityMVVM

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.arthenica.mobileffmpeg.*
import com.odi.beranet.beraodi.Activities.FilePath
import com.odi.beranet.beraodi.Activities.FolderManager
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.async_upload_video
import com.odi.beranet.beraodi.models.async_upload_video_complete
import com.odi.beranet.beraodi.odiLib.*
import com.vincent.videocompressor.VideoCompress
import kotlinx.android.synthetic.main.activity_preview_video.*
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


class videoUploadViewModel (val _this: AppCompatActivity, val listener:odiInterface?): odiInterface {
    private val TAG:String = "videoUploadViewModel"
    //var listener:odiInterface? = null

    var uploadId:String? = null
    var uploadVideoMessageHandler: Handler? = null

    internal var outputDir3 = Environment.getExternalStorageDirectory().toString() + File.separator + System.currentTimeMillis()
    var videoUri:Uri? = null
    var pageType:nativePage? = null
    var context:Context? = null
    var myVideoFile:File? = null

    var uploadClass:asyncUploadFile? = null


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
    fun getImageUrlWithAuthority(_uploadId:String, context: Context, uri: Uri, type:nativePage, videoFile:File?):String? {
        this.uploadId = _uploadId
        this.videoUri = uri
        this.pageType = type
        this.context = context
        this.myVideoFile = videoFile

        val thumb = getThumbnail(uri)


        val rate:Double = thumb.width.toDouble() / thumb.height.toDouble()
        val width:Double = 320.0
        val height = width / rate
        val resizeBitmapFile = resizeBitmap(thumb,width.toInt(),height.toInt())
        val out = ByteArrayOutputStream()
        resizeBitmapFile.compress(Bitmap.CompressFormat.JPEG, 70, out)



        val thumbFile = bitmapToFile(resizeBitmapFile)

        uploadFile(thumbFile, pageType!!, UPLOAD_FILE_TYPE.bitmap)

        return null
    }

    // Method to resize a bitmap programmatically
    private fun resizeBitmap(bitmap:Bitmap, width:Int, height:Int):Bitmap{
        return Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            true
        )
    }

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

                    if (value!!._id == "error") {
                        listener?.onUploadExitPreloader()

                        val builder = AlertDialog.Builder(_this)
                        builder.setTitle(R.string.UploadError)
                        builder.setMessage(R.string.UploadErrorDesc)

                        builder.setPositiveButton(R.string.Okey) { dialog, which ->
                            _this.finish()
                        }

                        builder.show()

                        uploadClass.let { value ->
                            value!!.cancel(true)
                        }

                        return
                    }


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

                                println("$TAG uploadFile: trigger videoUploadStart ->>")
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
            println("$TAG type: $type - userId: ${singleton.userId!!} - uploadType!!: ${uploadType}")
            val myModel = async_upload_video(this.uploadId, file, this, type, singleton.userId!!, uploadType!!)

            uploadClass = asyncUploadFile().execute(myModel) as asyncUploadFile?
        }else {
            println("$TAG singleton userId nil devam edemez.")
        }

    }


    // işlem bittiğinde en son bu dosya silinecek
    /**
     * samsung hata uri dosyası boş geliyor...
     * yeniden test edilmesi gerekiyor...
     */

    var sendFileStatus:Boolean = false
    var videoCheckCount:Int = 0
    var sendStart:Boolean = false
    fun videoUploadStart(context: Context, uri:Uri, type:nativePage) {

        //var myVideoFile = File(Environment.getExternalStorageDirectory().absolutePath+File.separator+"sil_ffmpegfile_odi_22.mp4")
        var myVideoFile_string = FolderManager.tempFolder()!!.absolutePath + File.separator + getRandomString(8) + "_odi.mp4"//"odi_tempVideo_tikitiki.mp4"

        var myVideoFile = File(myVideoFile_string)

        val myFile = getUriToFile(uri)
        Thread(Runnable {

            val rc = FFmpeg.getLastReturnCode()
            if (rc == FFmpeg.RETURN_CODE_SUCCESS) {
                println("$TAG : bubo4 işlem takibi")
            } else if (rc == FFmpeg.RETURN_CODE_CANCEL) {
                println("$TAG bubo4: Kullanıcı işlemi durdurdu.")
            } else {
                println("$TAG bubo4: komut başarısız oldu")
            }

            var duration:Long = 0L
            val info = FFmpeg.getMediaInformation(myFile!!.absolutePath)
            duration = info!!.duration


            Config.enableLogCallback(object : LogCallback {
                override fun apply(message: LogMessage?) {
                    println("$TAG hata mesage1 : ${message!!.text}")
                    println("$TAG hata mesage2 : ${message!!.level.value}")
                    if (message!!.level == Level.AV_LOG_QUIET) {
                        println("$TAG AV_LOG_QUIET: --")
                        videoCheckCount += 1
                        if (!sendFileStatus && sendStart && videoCheckCount == 4) {
                            println("$TAG bubo4 : video göndiriliyor -8")
                            sendFileStatus = true
                            sendStart = false
                            videoCheckCount = 0
                            listener?.onCompressVideoStatus(uploadId,null,true)
                            uploadFile(myVideoFile,type,UPLOAD_FILE_TYPE.video)
                        }
                    }else if (message!!.level == Level.AV_LOG_ERROR) {
                        println("$TAG AV_LOG_ERROR: --")
                    }
                }


            })

            Config.enableStatisticsCallback(object: StatisticsCallback {
                override fun apply(statistics: Statistics?) {
                    statistics.let {
                        val checekTime:Int = statistics!!.time
                        val calc = (100*checekTime)/ (duration.toInt())
                        sendStart = true
                        println("$TAG bubo4 frame: ${statistics!!.videoFrameNumber} : time: ${statistics.time} totalduration: ${duration} yuzde: $calc")
                        listener?.onCompressVideoStatus(uploadId,calc,false)

                        if (checekTime >= duration.toInt()) {
                            if (!sendFileStatus) {
                                println("$TAG bubo4 video bitti gönderiliyor normal")
                                sendFileStatus = true
                                sendStart = false
                                videoCheckCount = 0
                                listener?.onCompressVideoStatus(uploadId,null,true)
                                uploadFile(myVideoFile,type,UPLOAD_FILE_TYPE.video)
                            }
                        }
                    }
                }

            } )

            FFmpeg.execute("-i " + myFile!!.absolutePath + " -vcodec libx264 -acodec aac -vf scale=960:540 -b:v 1.5M " + myVideoFile.absolutePath)

        }).start()



        /*
        val newListener = object: VideoCompress.CompressListener {
            override fun onStart() {

            }

            override fun onSuccess() {
                //videoProcess(context, uri, type)

                val extStore = Environment.getExternalStorageDirectory().absolutePath

                val file = File ("$outputDir3.mp4")
                val filePath = file.absolutePath
                val fileUri = Uri.parse(extStore+file.toString())

                if (file.exists()) {
                    println("$TAG path: dosya var dosya boyu: ${file.length()}")
                }else {
                    println("$TAG path: dosya yok")
                }

                val getPath = file.path
                println("$TAG path outputPath orj: $uri")
                println("$TAG path outputPath dir3: $outputDir3")
                println("$TAG path outputPath filepath: $filePath")
                println("$TAG path outputPath file uri: $fileUri")
                println("$TAG path outputPath file getPath: $getPath")
                val path = Uri.parse(getPath)

                fileDeletedEnd_holder = file

                listener?.onCompressVideoStatus(uploadId,null,true)

                //videoProcess(context, path, type)

                Thread {
                    uploadFile(file,type,UPLOAD_FILE_TYPE.video)
                    //getUriToFile(uri)?.let { uploadFile(it,type,UPLOAD_FILE_TYPE.video) } // samsung da çalıştı
                }.run()
            }

            override fun onFail() {
                println("$TAG onFail---")
            }

            override fun onProgress(percent: Float) {
                listener?.onCompressVideoStatus(uploadId,percent.toInt(),false)
            }

        }*/


        /*
        uri.let {
            val myFile = getUriToFile(uri!!)
            myFile.let {
                if (myFile!!.exists()) {
                    println("$TAG fileSize: video dosyası bulundu ")

                    val file_size = (myFile.length() / (1024 * 1024)).toString().toInt()
                    if (file_size < 30) { // video 30MB dan küçük ise

                        println("$TAG video 30MB dan küçük direk yükleme yapılacak")

                        // --
                        val extStore = Environment.getExternalStorageDirectory().absolutePath
                        val filePath = myFile.absolutePath
                        val fileUri = Uri.parse(extStore+myFile.toString())

                        if (myFile.exists()) {
                            println("$TAG path: dosya var dosya boyu: ${myFile.length()}")
                        }else {
                            println("$TAG path: dosya yok")
                        }

                        val getPath = myFile.path
                        println("$TAG path outputPath orj: $uri")
                        println("$TAG path outputPath dir3: $outputDir3")
                        println("$TAG path outputPath filepath: $filePath")
                        println("$TAG path outputPath file uri: $fileUri")
                        println("$TAG path outputPath file getPath: $getPath")
                        val path = Uri.parse(getPath)

                        fileDeletedEnd_holder = myFile

                        listener?.onCompressVideoStatus(uploadId,null,true)

                        //videoProcess(context, path, type)

                        Thread {
                            uploadFile(myFile,type,UPLOAD_FILE_TYPE.video)
                            //getUriToFile(uri)?.let { uploadFile(it,type,UPLOAD_FILE_TYPE.video) } // samsung da çalıştı
                        }.run()

                        //
                    }else { // 30 mb büyük ise
                        println("$TAG video 30MB dan büyük compress yapılacak")
                        VideoCompress.compressVideoMedium(getRealPathFromURI(_this.applicationContext,uri), "$outputDir3.mp4", newListener)
                    }
                }else {
                    println("$TAG fileSize: video dosyası bulunamadı")
                }
            }
        }*/


        //VideoCompress.compressVideoLow(getRealPathFromURI(_this.applicationContext,uri), "$outputDir3.mp4", newListener)




        //String filePath = PathUtil.getPath(context,uri);
        //var filePath = PathUtil.getPath(_this.applicationContext, uri)

        //var myUri:Uri = Uri.fromFile(getUriToFile(uri))

        //var myfile = getUriToFile(uri)
        //var newUri = FilePath.getPath(_this,uri)
        //VideoCompress.compressVideoLow(getRealPathFromURI(_this,uri), "$outputDir3.mp4", newListener)

    }

    fun getRandomString(length: Int) : String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun getUriToFile(myUri:Uri):File? {
        val path = FilePath.getPath(_this,myUri)
        val myFile = File(path)
        return myFile
    }

    fun getRealPathFromURI(context: Context, contentUri: Uri): String {
        var cursor: Cursor? = null
        var column_index:Int? = null
        try {
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            column_index = cursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            cursor.moveToFirst()
            return cursor!!.getString(column_index!!)
        } finally {
            cursor?.close()
        }
    }

    private fun bitmapToFile(bitmap:Bitmap): File {
        val wrapper = ContextWrapper(_this.applicationContext)

        var file = wrapper.getDir("Images",Context.MODE_PRIVATE)
        file = File(file,"mdroid.jpg")

        try{
            val stream:OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
            e.printStackTrace()
        }

        val returningFile:File = File(file.absolutePath)
        return returningFile
    }

    internal fun check_writeRead_permission(completion: (Boolean?) -> Unit) {
        if (ContextCompat.checkSelfPermission(_this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(_this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(_this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED  ) {
            completion(true)
        } else {
            completion(false)
            ActivityCompat.requestPermissions(_this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                Permission_Result.UPLOAD_VIDEO_GALLERY.value)
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

                /*
                if (fileDeletedEnd_holder != null) {
                    if (fileDeletedEnd_holder!!.exists()) {
                        if (fileDeletedEnd_holder!!.delete()) {
                            println("$TAG delete file: dosya başarı ile silindi")
                        }
                    }
                }
                */

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

        if (cursor == null) {
            val bitmap = ThumbnailUtils.createVideoThumbnail(uri.path, MediaStore.Images.Thumbnails.MINI_KIND)
            //_this.testImage.setImageBitmap(bitmap)
            return bitmap
        }
        cursor.moveToFirst()

        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
        val picturePath = cursor.getString(columnIndex)
        cursor.close()

        return ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MINI_KIND)
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

}

