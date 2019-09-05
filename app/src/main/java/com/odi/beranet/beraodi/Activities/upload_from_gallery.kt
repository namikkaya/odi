package com.odi.beranet.beraodi.Activities

import android.content.*
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DialogTitle
import android.view.View
import android.widget.*
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.odi.beranet.beraodi.MainActivityMVVM.videoUploadViewModel
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.*
import com.onesignal.OneSignal
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.database.Cursor
import android.os.*
import android.os.Environment.getExternalStorageDirectory
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import java.io.*
import java.util.*
import java.nio.file.Files.exists
import com.facebook.common.file.FileUtils.mkdirs
import android.os.Environment.getExternalStorageDirectory
import android.content.ContentValues
import android.content.Intent








class upload_from_gallery : baseActivity(), odiInterface {

    private val TAG:String = "upload_from_gallery"
    var videoUploadController: videoUploadViewModel? = null

    private var selectedUri: Uri? = null
    private var processType: nativePage? = null

    //-- object
    var videoView:VideoView? = null
    private var mediaController: MediaController? = null
    private var progressBarContainer:RelativeLayout? = null
    private var myActionBar: ActionBar? = null
    private lateinit var cancelButton:Button
    private lateinit var sendButton:Button

    // -- values

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.odi.beranet.beraodi.R.layout.activity_upload_from_gallery)

        navigationBarConfiguration()
        onGetIntentData()
        onGalleryConfiguration()
        onUIDesignConfiguration()

    }

    var warningIntent: Intent? = null
    /**
     * internet durum kontrolü
     * */
    override fun internetConnectionStatus(status: Boolean) {
        warningIntent = Intent(this, warningActivity::class.java)
        warningIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        warningIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        warningIntent?.putExtra("warningTitle", "Bağlantı Sorunu")
        warningIntent?.putExtra("warningDescription", "İnternet bağlantınızda problem var. Lütfen bağlantınızı kontrol edip tekrar deneyin.")
        if (!status) {
            startActivity(warningIntent)
        }else {
            oneSignalConfiguration()
        }
    }

    private fun oneSignalConfiguration() {
        println("$TAG oneSignalConfiguration: start")
        OneSignal.idsAvailable { userId, registrationId ->
            if (registrationId != null) {
                singleton.onesignal_playerId = userId
                singleton.onesignal_registrationId = registrationId
            }else {

            }
        }
    }

    private fun onGalleryConfiguration() {
        videoUploadController = videoUploadViewModel(this,this)
    }

    private fun onGetIntentData() {
        val intent = intent
        val uriString = intent.getStringExtra("selectedPath") as String
        selectedUri = Uri.parse(uriString)
        processType = intent.getSerializableExtra("processType") as nativePage
    }

    private fun onUIDesignConfiguration() {
        videoView = findViewById(R.id.myVideoView)
        progressBarContainer = findViewById(R.id.progressBarContainer)
        cancelButton = findViewById(R.id.uploadCancelButton) as Button
        sendButton = findViewById(R.id.uploadSendButton)

        cancelButton.setOnClickListener(clickListener)
        sendButton.setOnClickListener(clickListener)
        progressBarContainer?.post(Runnable { kotlin.run {
            progressBarContainer?.visibility = View.INVISIBLE
        } })

        videoPlayerConfig()
    }

    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    private fun videoPlayerConfig() {
        mediaController = MediaController(this)
        mediaController?.setAnchorView(videoView)

        videoView?.setMediaController(mediaController)
        videoView?.setVideoURI(selectedUri)
        videoView?.requestFocus()
        videoView?.start()

        selectedUri.let {
            val myFile = getUriToFile(selectedUri!!)
            myFile.let {
                if (myFile!!.exists()) {
                    println("$TAG fileSize: video dosyası bulundu")


                    val file_size = (myFile.length() / (1024 * 1024)).toString().toInt()
                    if (file_size > 50) {
                        sendButton.isClickable = false
                        sendButton.alpha = 0.5F
                        this@upload_from_gallery.infoDialog("Video Boyutu","Video boyutu 50 MB sınırını aşıyor. Lütfen videonuzu tekrar gözden geçirip deneyin.")
                    }
                }else {
                    println("$TAG fileSize: video dosyası bulunamadı")
                }
            }
        }
    }

    private fun getUriToFile(myUri:Uri):File? {
        val path = FilePath.getPath(this,myUri)
        val myFile = File(path)
        return myFile
    }


    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.uploadCancelButton -> onCancelButtonEvent()
            R.id.uploadSendButton -> onSendButtonEvent()
        }
    }

    private fun onCancelButtonEvent() {
        videoView!!.stopPlayback()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        return
    }


    private fun onSendButtonEvent() {
        videoView?.pause()
        preloader()
        videoUploadController?.getImageUrlWithAuthority("videoUpload", this,selectedUri!!, processType!!)

        val myFile = getUriToFile(selectedUri!!)
        //saveVideo(myFile)
        saveVideoGallery(myFile)
    }


    var preloaderIntent: Intent? = null

    private fun preloader() {
        preloaderIntent = Intent(this, preloaderActivity::class.java)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        //startActivity(preloaderIntent)
        startActivityForResult(preloaderIntent, Activity_Result.PRELOADER_FINISH.value)
    }

    /**
     * Video Yükleme bilgisini gelir
     * */
    override fun onUploadVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadVideoStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.video,_progress,_complete)
    }

    /**
     * Bitmap Yükleme bilgisini gelir
     * */
    override fun onUploadBitmapStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadBitmapStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.bitmap,_progress,_complete)
    }

    /**
     * video compress bilgisi gelir
     * */
    override fun onCompressVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onCompressVideoStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.compress,_progress,_complete)
    }

    private fun preloaderManage(status:UI_PRELOADER, progress:Int?, complete: Boolean?) {
        when(status) {
            UI_PRELOADER.video -> preloaderVideoUpload(progress, complete)
            UI_PRELOADER.bitmap -> preloaderBitmapUpload(progress,complete)
            UI_PRELOADER.compress -> preloaderVideoCompress(progress,complete)
            else -> println("Problem oldu...")
        }
    }


    private fun preloaderBitmapUpload(progress:Int?, complete:Boolean?) {
        if(!complete!!) {
            getPreloaderContext().let {
                getPreloaderContext()?.progressChangeData(progress, "Resim Yükleniyor", false)
            }
        }else {
            getPreloaderContext().let {
                getPreloaderContext()?.progressChangeData(progress, "Resim Yüklendi", false)
            }
        }
    }

    private fun preloaderVideoCompress(progress: Int?, complete: Boolean?){
        if(!complete!!) {
            getPreloaderContext().let {
                getPreloaderContext()?.progressChangeData(progress, "Video Sıkıştırılıyor", false)
            }
        }else {
            getPreloaderContext().let {
                getPreloaderContext()?.progressChangeData(progress, "Video Hazır", false)
            }
        }
    }

    // video upload bittiğinde complete geldiğinde işlemin tamamı bitmiş olur
    private fun preloaderVideoUpload(progress: Int?, complete: Boolean?){
        if(!complete!!) {
            getPreloaderContext().let {
                getPreloaderContext()?.progressChangeData(progress, "Video Yükleniyor", false)
            }

        }else {
            println("$TAG VİDEO YÜKLEME TAMAMLANDI")
            getPreloaderContext().let {
                getPreloaderContext()?.progressChangeData(progress, "Video Hazır", true)
            }

        }
    }

    private fun getPreloaderContext():preloaderActivity? {

        if (singleton.preloaderContext as? preloaderActivity != null) {
            val act = singleton.preloaderContext as? preloaderActivity
            return act
        }
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity_Result.PRELOADER_FINISH.value && resultCode == RESULT_OK) {
            intent.putExtra("STATUS", "OKEY")
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    companion object {
        private val ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm"
    }

    private fun getRandomString(sizeOfRandomString: Int): String {
        val random = Random()
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        return sb.toString()
    }

    private val VIDEO_DIRECTORY = "/odiVideo"


    private fun saveVideoGallery(filePath: File?) {

        var randomName:String = getRandomString(8)
        val values = ContentValues(3)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        //values.put(MediaStore.Video.Media.TITLE, "odi_$randomName");
        //values.put(MediaStore.Video.Media.DATA, filePath?.getAbsolutePath());
        //values.put(MediaStore.Video.Media.DATA, f.getAbsolutePath());

        // Add a new record (identified by uri) without the video, but with the values just set.
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        try {
            val currentFile = filePath
            var wallpaperDirectory = File(Environment.getExternalStorageDirectory().absolutePath + VIDEO_DIRECTORY)
            var newfile = File(wallpaperDirectory, Calendar.getInstance().timeInMillis.toString() + ".mp4")


            var inputStream: InputStream = FileInputStream(currentFile)
            var output: OutputStream? = contentResolver.openOutputStream(uri)


            val dir = newfile
            if (!dir.exists()) {
                dir.mkdirs()
            }

            var buffer = ByteArray(1024)
            if (inputStream != null) {

                while (true) {
                    val read: Int? = inputStream?.read(buffer!!)
                    if (read!! <= 0)
                        break

                    output?.write(buffer, 0, read)
                }
            }

            inputStream?.close()


            output?.flush()
            output?.close()
            output = null

            //val newFile = File("$outputPath.mp4")
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))

            if (newfile.exists()) {
                println("Gallery: SAVE VIDEO video dosyası var bulundu")
            } else {
                println("Gallery: SAVE VIDEO video dosyası yok bulunamadı")
            }

        } catch (e: FileNotFoundException) {
            Log.e("VideoComp", e.message)
        } catch (e: java.lang.Exception) {
            Log.e("VideoComp", e.message)
        }
    }

    private fun saveVideo(filePath: File?) {
        try {
            val currentFile = filePath
            var wallpaperDirectory = File(Environment.getExternalStorageDirectory().absolutePath + VIDEO_DIRECTORY)
            var newfile = File(wallpaperDirectory, Calendar.getInstance().timeInMillis.toString() + ".mp4")


            var inputStream: InputStream = FileInputStream(currentFile)
            var output: OutputStream? = null


            val dir = newfile
            if (!dir.exists()) {
                dir.mkdirs()
            }

            output = FileOutputStream(newfile)

            var buffer = ByteArray(1024)
            if (inputStream != null) {

                while (true) {
                    val read: Int? = inputStream?.read(buffer!!)
                    if (read!! <= 0)
                        break

                    output?.write(buffer, 0, read)
                }
            }

            inputStream?.close()


            output?.flush()
            output?.close()
            output = null

            //val newFile = File("$outputPath.mp4")

            if (newfile.exists()) {
                println("Gallery: SAVE VIDEO video dosyası var bulundu")
            } else {
                println("Gallery: SAVE VIDEO video dosyası yok bulunamadı")
            }

        } catch (e: FileNotFoundException) {
            Log.e("VideoComp", e.message)
        } catch (e: java.lang.Exception) {
            Log.e("VideoComp", e.message)
        }
    }

    fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {

            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else
            return null
    }

}


object FilePath {

    /**
     * Method for return file path of Gallery image/ Document / Video / Audio
     *
     * @param context
     * @param uri
     * @return path of the selected image file from gallery
     */
    fun getPath(context: Context, uri: Uri): String? {

        // check here to KITKAT or new version
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    //return ( Environment.getExternalStorageDirectory()+"/"+split[1] )
                    val enPath = Environment.getExternalStorageDirectory()
                    val sp = split[1]
                    return "$enPath/$sp"
                }
            } else if (isDownloadsDocument(uri)) {

                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(
                    context, contentUri, selection,
                    selectionArgs
                )
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)

        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     * The context.
     * @param uri
     * The Uri to query.
     * @param selection
     * (Optional) Filter used in the query.
     * @param selectionArgs
     * (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(
        context: Context, uri: Uri?,
        selection: String?, selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.getContentResolver().query(
                uri, projection,
                selection, selectionArgs, null
            )
            if (cursor != null && cursor!!.moveToFirst()) {
                val index = cursor!!.getColumnIndexOrThrow(column)
                return cursor!!.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor!!.close()
        }
        return null
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri
            .authority
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri
            .authority
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri
            .authority
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri
            .authority
    }



}
