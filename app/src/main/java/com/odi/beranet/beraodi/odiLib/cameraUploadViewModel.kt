package com.odi.beranet.beraodi.odiLib

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.odi.beranet.beraodi.Activities.previewVideo
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.async_upload_video
import com.odi.beranet.beraodi.models.async_upload_video_complete
import com.vincent.videocompressor.VideoCompress
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

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

        println("$TAG orgVideoName: $orgVideoName")


        if (type == nativePage.cameraOdile) {
            var name = videoFile!!.name!!.replace(".mp4", "_out.jpg")
            val thumb = getThumbnail(uri)
            val thumbFile = bitmapToFile(thumb, name)

            val outputpath = outputDir4 + orgVideoName?.replace(".mp4", "_out.mp4")
            outputVideoName = outputpath

            uploadFile(thumbFile!!, type!!, UPLOAD_FILE_TYPE.bitmap)
        }

        if (type == nativePage.cameraShowReel) {
            val thumb = getThumbnail(uri)
            val thumbFile = bitmapToFile(thumb, orgVideoName!!)

            val outputpath = outputDir4 + orgVideoName
            outputVideoName = outputpath

            uploadFile(thumbFile!!, type!!, UPLOAD_FILE_TYPE.bitmap)
        }

        if (type == nativePage.cameraIdentification) {
            val thumb = getThumbnail(uri)
            val thumbFile = bitmapToFile(thumb, orgVideoName!!)

            val outputpath = outputDir4 + orgVideoName
            outputVideoName = outputpath

            uploadFile(thumbFile!!, type!!, UPLOAD_FILE_TYPE.bitmap)
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

                        // yükleme hatası oluştu alert açtır.

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
                            listener?.onUploadBitmapStatus(projectId, progressValue,false)
                        }else {
                            listener?.onUploadVideoStatus(projectId,progressValue,false)
                        }
                    }else {
                        Thread(Runnable { // yükleme işlemi bitti request atılacak
                            println("$TAG uploadVideoAsyncTaskComplete:  başarılı bir şekilde yüklendi")
                            if (value._uploadFileType == UPLOAD_FILE_TYPE.video) {

                                request(value!!.requestPath!!)
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


        val myModel = async_upload_video(projectId, file, this, type, userId!!, uploadType!!)
        uploadClass = asyncUploadFile().execute(myModel) as asyncUploadFile?

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
                if (inputLine == null) {
                    break
                }
                if (inputLine!! != "") {
                    break
                }
                ab.append(inputLine)
                println("$TAG async: request complete ")
                //listener?.onUploadVideoStatus(uploadId, null, true)

                if (fileDeletedEnd_holder != null) {
                    if (fileDeletedEnd_holder!!.exists()) {
                        if (fileDeletedEnd_holder!!.delete()) {
                            println("$TAG delete file: dosya başarı ile silindi")
                        }
                    }
                }

                listener?.onUploadVideoStatus(projectId,null,true)
            }

        } finally {
            conn.disconnect()
        }


        return ab.toString()
    }


    fun videoUploadStart(context: Context, uri:Uri, type:nativePage) {

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
        VideoCompress.compressVideoMedium(uri.path, outputVideoName, newListener)

    }


    private fun getThumbnail(uri:Uri): Bitmap {
        val bitmap = ThumbnailUtils.createVideoThumbnail(uri.path, MediaStore.Images.Thumbnails.MINI_KIND)
        val ism = _this as? previewVideo
        //_this.testImage.setImageBitmap(bitmap)
        ism?.let {
            it.testImage.setImageBitmap(bitmap)
        }
        return bitmap
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

}