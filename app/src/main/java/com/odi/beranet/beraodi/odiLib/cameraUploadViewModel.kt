package com.odi.beranet.beraodi.odiLib

import android.content.Context
import android.content.ContextWrapper
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import com.odi.beranet.beraodi.Activities.previewVideo
import com.odi.beranet.beraodi.models.async_upload_video
import com.odi.beranet.beraodi.models.async_upload_video_complete
import com.vincent.videocompressor.VideoCompress
import org.jetbrains.anko.doAsync
import java.io.*
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.runBlocking


class cameraUploadViewModel(val _this: AppCompatActivity, val listener:odiInterface?): odiInterface  {
    private val TAG:String = "cameraUploadViewModel"

    //internal var outputDir3 = Environment.getExternalStorageDirectory().toString() + File.separator + System.currentTimeMillis()

    internal var outputDir4 = Environment.getExternalStorageDirectory().toString() + File.separator

    var userId:String? = null
    var projectId:String? = null
    var type:nativePage? = null
    var videoUri:Uri? = null
    var context: Context? = null
    var videoFile: File? = null

    var orgVideoName:String? = null

    var uploadClass:asyncUploadFile? = null

    var uploadVideoMessageHandler: Handler? = null

    var fileDeletedEnd_holder:File? = null

    var outputVideoName:String? = null

    private val VIDEO_DIRECTORY_2 = "videoOfOdiRecord"

    /***
     * @param uploadId => projectId
     * @param userId => user id
     * @param context => context
     * @param uri => dosya uri si
     * @param type => odile, tanitim, showreel
     * @param videoFile => video dosyası
     */
    fun uploadStart(uploadId:String,
                    userId:String,
                    context: Context,
                    uri: Uri,
                    type:nativePage,
                    videoFile: File?) {

        this.projectId = uploadId
        this.userId = userId
        this.context = context
        this.videoUri = uri
        this.type = type
        this.videoFile = videoFile

        orgVideoName = videoFile?.name


        if (type == nativePage.cameraOdile) {
            //var name = videoFile!!.name!!.replace(".mp4", "_out.jpg")

            var name = videoFile!!.name!!.replace(".mp4", ".jpg")
            val thumb = getThumbnail(uri)
            val thumbFile = bitmapToFile(thumb, name)

            println("$TAG thumbnail path image: $name")

            val outputpath = outputDir4 + orgVideoName?.replace(".mp4", "_out.mp4")
            outputVideoName = outputpath

            println("$TAG thumbnail path video: $outputpath")

            uploadFile(thumbFile!!, type!!, UPLOAD_FILE_TYPE.bitmap)
        }

        if (type == nativePage.cameraShowReel) {
            val thumb = getThumbnail(uri)
            val thumbFile = bitmapToFile(thumb, orgVideoName!!)

            val outputpath = outputDir4 + createVideoFileName(type)

            println("$TAG dbTakip: outputpath: $outputpath - dir: ${outputpath} - orgVideoName: $orgVideoName")

            outputVideoName = outputpath

            uploadFile(thumbFile!!, type!!, UPLOAD_FILE_TYPE.bitmap)
        }

        if (type == nativePage.cameraIdentification) {
            val thumb = getThumbnail(uri)
            val thumbFile = bitmapToFile(thumb, orgVideoName!!)

            val outputpath = outputDir4 + createVideoFileName(type)


            println("$TAG dbTakip: outputpath: $outputpath - dir: ${outputpath} - orgVideoName: $orgVideoName")

            outputVideoName = outputpath

            uploadFile(thumbFile!!, type!!, UPLOAD_FILE_TYPE.bitmap)
        }
    }

    private fun createVideoFileName(processType:nativePage):String {

        if (processType == nativePage.cameraIdentification) { // tanitim
            return "tanitim_$userId.mp4"
        }else if (processType == nativePage.cameraShowReel) {
            return "showreel_$userId.mp4"
        }else {
            val timestamp = SimpleDateFormat("yyMMdd_HHmmss").format(Date())
            return "${this.userId}_${this.projectId}_VID_$timestamp.mp4"
        }
    }


    private fun renameFile(oldName:String, newName:String):File? {
        val dir = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + VIDEO_DIRECTORY_2)
        if (dir.exists()) {
            var from = File(oldName)
            var to = File(newName)
            if (from.exists()) {
                from.renameTo(to)
                println("$TAG dbTakip rename file from: ${from.path} -- to: ${to.path}")
                return to
            }else {
                return null
            }
        }else {
            return null
        }
    }

    override fun uploadVideoAsyncTaskComplete(resultData: async_upload_video_complete?) {
        super.uploadVideoAsyncTaskComplete(resultData)

        var message: Message = uploadVideoMessageHandler!!.obtainMessage(1,resultData)
        message.sendToTarget()
    }

    private fun uploadFile(file:File, type:nativePage, uploadType:UPLOAD_FILE_TYPE) {
        println("$TAG uploadFile çalıştı")
        uploadVideoMessageHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                val msg: async_upload_video_complete? = message.obj as? async_upload_video_complete
                msg.let { value ->

                    if (value!!._id == "error") {
                        listener?.onUploadExitPreloader()
                        Thread(Runnable { // yükleme işlemi bitti request atılacak
                            val myThis = _this as? previewVideo
                            if (myThis != null) {
                                myThis.errorVideoProblem = true
                            }
                        }).start()

                        uploadClass.let { value ->
                            value!!.cancel(true)
                        }
                        return
                    }


                    if (!value!!._uploadStatus!!) { // false ise yükleme yapılmaya devam ediliyor
                        val progressValue:Int = value!!._uploadProgress!!
                        val tipi = value._uploadFileType
                        if (value._uploadFileType!! == UPLOAD_FILE_TYPE.bitmap) {
                            listener?.onUploadBitmapStatus(projectId, progressValue,false)
                        }else {
                            listener?.onUploadVideoStatus(projectId,progressValue,false)
                        }
                    }else {
                        Thread(Runnable { // yükleme işlemi bitti request atılacak
                            println("$TAG uploadVideoAsyncTaskComplete:  başarılı bir şekilde yüklendi")
                            if (value._uploadFileType == UPLOAD_FILE_TYPE.video) {

                                request(value!!.requestPath!!)

                                //sendGet(value!!.requestPath!!)

                                val future = doAsync {
                                    // do your background thread task
                                    //result = someTask
                                    /*
                                    uiThread {
                                        // use result here if you want to update ui
                                        updateUI(result)
                                    }*/
                                }


                                // bitti
                                println("$TAG request atılmadı. işlem sona erdirildi. ${value!!.requestPath}")


                            }else {
                                // image yüklendi video işlemi için çağrı
                                listener?.onUploadBitmapStatus(projectId,null,true)
                                // video yükleme işlemi başlatılıyor

                                println("$TAG uploadFile: trigger videoUploadStart ->>")

                                // image upload ettikten sonra buraya geçer burayı açmayı unutma

                                videoUploadStart(this@cameraUploadViewModel.context!!,
                                    this@cameraUploadViewModel.videoUri!!,
                                    this@cameraUploadViewModel.type!!)
                            }
                        }).start()
                    }

                }
            }
        }

        println("$TAG userId: $userId - $projectId")
        val myModel = async_upload_video(projectId, file, this, type, userId!!, uploadType!!)
        uploadClass = asyncUploadFile().execute(myModel) as asyncUploadFile?

    }

    /*fun sendGet(url:String) {
        val url = URL(url)

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET

            println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

            inputStream.bufferedReader().use {
                it.lines().forEach { line ->
                    println("$TAG line: $line")
                }
            }
            println("$TAG request atılmış olması gerekiyor-----")
        }

        listener?.onUploadVideoStatus(projectId,null,true)
    }*/

    @Throws(IOException::class)
    fun request(uri: String): String {
        println("$TAG request atılacak")
        val ab = StringBuilder()
        val url = URL(uri)
        val conn = url.openConnection() as HttpURLConnection
        try {

            while (true){

                val inStr = BufferedInputStream(conn.inputStream)
                val bf = BufferedReader(InputStreamReader(inStr))

                val inputLine: String? = bf.readLine()
                if (inputLine == null) {
                    break
                }
                if (inputLine!! != "") {
                    break
                }
                ab.append(inputLine)

                // note: yeni 19 12 2019
                //listener?.onUploadVideoStatus(projectId,null,true)

            }
            println("$TAG request atılmış olması gerekiyor-----")
            listener?.onUploadVideoStatus(projectId,null,true)

        } finally {
            conn.disconnect()
        }


        return ab.toString()
    }


    fun videoUploadStart(context: Context, uri:Uri, type:nativePage) {

        /*
        val newListener = object: VideoCompress.CompressListener {
            override fun onStart() {

            }

            override fun onSuccess() {
                //videoProcess(context, uri, type)

                val extStore = Environment.getExternalStorageDirectory().absolutePath



                val file = File (outputVideoName)
                val filePath = file.absolutePath
                val fileUri = Uri.parse(extStore+file.toString())

                if (file.exists()) {
                    println("$TAG path: dosya var dosya boyu: ${file.length()}")
                }else {
                    println("$TAG path: dosya yok")
                }

                val getPath = file.path
                println("$TAG path outputPath orj: $uri")
                println("$TAG path outputPath dir3: $outputVideoName")
                println("$TAG path outputPath filepath: $filePath")
                println("$TAG path outputPath file uri: $fileUri")
                println("$TAG path outputPath file getPath: $getPath")
                val path = Uri.parse(getPath)

                fileDeletedEnd_holder = file

                listener?.onCompressVideoStatus(projectId,null,true)

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
                listener?.onCompressVideoStatus(projectId,percent.toInt(),false)
            }

        }

        VideoCompress.compressVideoMedium(uri.path, outputVideoName, newListener)*/
        // note: yeni 19 12 2019 yukarıda ki kompresor kaldırıldı.

        val lFile = File(uri.path)
        //val newFile = renameFileName(lFile, outputVideoName!!)
        Thread {
            if (lFile != null) {
                uploadFile(lFile,type,UPLOAD_FILE_TYPE.video)
            }
        }.run()
        /*
        runBlocking {
            val newFile = renameFileName(lFile, outputVideoName!!)
            Thread {
                if (newFile != null) {
                    uploadFile(newFile,type,UPLOAD_FILE_TYPE.video)
                }
            }.run()
        }*/

    }

    fun renameFileName(file:File, newName:String):File? {
        var from = file
        var to = File(newName)
        if (from.exists()) {
            from.renameTo(to)
            println("$TAG dbTakip rename file from: ${from.path} -- to: ${to.path}")
            return from
        }else {
            return null
        }
    }


    private fun getThumbnail(uri:Uri): Bitmap {
        println("bitmapTakip: uri: "+ uri)
        var bitmap = ThumbnailUtils.createVideoThumbnail(uri.path, MediaStore.Images.Thumbnails.MINI_KIND)
        if (bitmap == null) {
            var file:File = File(uri.path)
            sleep(100)
            bitmap = ThumbnailUtils.createVideoThumbnail(uri.path, MediaStore.Images.Thumbnails.MINI_KIND)
            sleep(100)
        }

        val rate:Double = bitmap.width.toDouble() / bitmap.height.toDouble()
        val width:Double = 320.0
        val height = width / rate
        val resizeBitmapFile = resizeBitmap(bitmap,width.toInt(),height.toInt())
        val out = ByteArrayOutputStream()
        resizeBitmapFile.compress(Bitmap.CompressFormat.JPEG, 70, out)

        return ByteArrayToBitmap(out.toByteArray())
    }

    fun ByteArrayToBitmap(byteArray: ByteArray): Bitmap {
        val arrayInputStream = ByteArrayInputStream(byteArray)
        return BitmapFactory.decodeStream(arrayInputStream)
    }

    private fun bitmapToFile(bitmap:Bitmap, name:String): File {
        val wrapper = ContextWrapper(_this.applicationContext)

        var file = wrapper.getDir("Images",Context.MODE_PRIVATE)
        file = File(file,name)

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

    // Method to resize a bitmap programmatically
    private fun resizeBitmap(bitmap:Bitmap, width:Int, height:Int):Bitmap{
        return Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            true
        )
    }


}