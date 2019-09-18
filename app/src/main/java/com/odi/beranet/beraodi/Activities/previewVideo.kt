package com.odi.beranet.beraodi.Activities

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.ActionBar
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import com.odi.beranet.beraodi.MainActivityMVVM.videoUploadViewModel
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.*
import com.onesignal.OneSignal
import java.io.*
import java.lang.IllegalStateException
import java.util.*

class previewVideo : baseActivity(),
    SurfaceHolder.Callback,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener,
    MediaController.MediaPlayerControl,
    odiInterface{

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <preloader --- >

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity_Result.PRELOADER_FINISH.value && resultCode == RESULT_OK) {
            intent.putExtra("STATUS", "OKEY")
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    var preloaderIntent: Intent? = null

    private fun preloader() {
        preloaderIntent = Intent(this, preloaderActivity::class.java)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        //startActivity(preloaderIntent)
        startActivityForResult(preloaderIntent, Activity_Result.PRELOADER_FINISH.value)
    }

    override fun onUploadBitmapStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadBitmapStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.bitmap,_progress,_complete)
    }

    override fun onCompressVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onCompressVideoStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.compress,_progress,_complete)
    }

    override fun onUploadVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadVideoStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.video,_progress,_complete)

    }

    override fun onUploadExitPreloader() {
        super.onUploadExitPreloader()
        getPreloaderContext().let {
            getPreloaderContext()?.exitPreloader()
        }
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

    //-------------------------------------------------------------------
    override fun isPlaying(): Boolean {
        return mp!!.isPlaying
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        return mp!!.duration
    }

    override fun pause() {
        try {
            mp!!.pause()
        }catch (e:IllegalStateException){
            println("$TAG ${e.toString()} -> kod: 126")
        }
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun seekTo(pos: Int) {
        mp!!.seekTo(pos)
    }

    override fun getCurrentPosition(): Int {
        return mp!!.currentPosition
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        try {
            mp!!.start()
        }catch (e:IllegalStateException){
            println("$TAG hata: ${e.toString()} -> kod 125")
        }
    }

    override fun getAudioSessionId(): Int {
       return 0
    }

    override fun canPause(): Boolean {
        return true
    }

    private val TAG:String = "previewVideo:"

    private lateinit var videoView:VideoView

    private lateinit var mySurface:SurfaceView
    private var surfaceHolder:SurfaceHolder? = null
    private var mediaController:MediaController? = null
    private var mediaPlayerLayout:RelativeLayout? = null

    private lateinit var againButton:ImageButton
    private lateinit var saveButton:ImageButton
    private lateinit var uploadButton:ImageButton
    lateinit var testImage:ImageView
    private var myActionBar: ActionBar? = null

    private var projectId:String? = null
    private var userId:String? = null
    private var vMyUri:Uri? = null
    private var processType: nativePage? = null

    var mHandler: Handler? = null

    private var mp:MediaPlayer? = null
    var warningIntent: Intent? = null
    var videoUploadController: cameraUploadViewModel? = null


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

    override fun onBackPressed() {
        mp.let { _value ->
            _value!!.pause()
            _value!!.stop()
            _value!!.release()

        }
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_video)

        val bundle=intent.extras
        if(bundle!=null) {
            projectId = bundle.getString("projectId")
            userId = bundle.getString("userId")
            processType = bundle.getSerializable("type") as nativePage
            val str = bundle.getString("videoPath")
            vMyUri = Uri.parse(str)
        }

        videoUploadController = cameraUploadViewModel(this,this)

        navigationBarConfiguration()
        uiconfig()
    }


    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) { }

    override fun surfaceDestroyed(holder: SurfaceHolder?) { }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mediaPlayerConfig(holder)
    }

    private fun mediaPlayerConfig(holder:SurfaceHolder?) {
        try {
            mp!!.setDataSource(this,vMyUri)
            mp!!.prepare()
            mp!!.setDisplay(holder)

            surfaceHolder = holder

            mp?.setOnPreparedListener(this)
            mp?.setOnCompletionListener(this)

            //mp!!.start()
        }catch (e:IOException) {
            println("$TAG ${e.toString()}")
        }
    }

    private fun uiconfig() {
        mediaPlayerLayout = findViewById(R.id.mediaPlayerLayout)
        videoView = findViewById(R.id.myVideoView_previewVideo)
        testImage = findViewById(R.id.testImage)

        againButton = findViewById(R.id.againButton_previewVideo)
        saveButton = findViewById(R.id.saveButton_previewVideo)
        uploadButton = findViewById(R.id.uploadButton_previewVideo)
        mySurface = findViewById(R.id.mySurface)


        againButton.setOnClickListener(clickListener)
        saveButton.setOnClickListener(clickListener)
        uploadButton.setOnClickListener(clickListener)
        videoView.setOnClickListener(clickListener)

        addMediaController()
    }

    private fun addMediaController() {
        mp = MediaPlayer()

        window.setFormat(PixelFormat.UNKNOWN)
        surfaceHolder = videoView.holder
        surfaceHolder!!.setFixedSize(800, 480)
        surfaceHolder!!.addCallback(this)

        videoView.requestFocus()

    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.recordButton -> OnAgainButtonEvent()
            R.id.saveButton_previewVideo -> OnSaveButtonEvent()
            R.id.uploadButton_previewVideo -> OnUploadbuttonEvent()
            R.id.myVideoView_previewVideo -> OnMediaControllerStatusEvent()
        }
    }

    private fun OnMediaControllerStatusEvent () {
        if (mediaController != null) {
            if (mediaController!!.isShowing) {
                mediaController!!.hide()
            }else {
                mediaController!!.show(0)
            }
        }

    }

    override fun onPause() {
        super.onPause()
        println("$TAG onPause")
        if (mp != null) {
            try {
               mp?.pause()

            }catch (e:IllegalStateException){
                println("$TAG hata: ${e.toString()}")
            }

        }
    }

    var firstStart:Boolean = false
    override fun onResume() {
        super.onResume()
        if (mp != null) {
            try {
                if (firstStart) {
                    finish();
                    startActivity(intent)
                }
                firstStart = true
            }catch (e:IllegalStateException){
                println("$TAG ${e.toString()}")
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        try {
            //mp!!.seekTo(0)
            //mp!!.start()
        }catch (e:IllegalStateException) {
            println("$TAG ${e.toString()} hata: 112")
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mediaController = MediaController(this)
        mediaController?.setMediaPlayer(this)
        mediaController?.setAnchorView(mediaPlayerLayout)


        try {
            videoView.setMediaController(mediaController)
            mp!!.start()
        }catch (e:IllegalStateException) {
            println("$TAG hata: ${e.toString()} -- kod 124")
        }

        mHandler = Handler()
        mHandler?.post(Runnable {
            kotlin.run {
                mediaController?.isEnabled = true
                mediaController?.show(0)
            }
        })
    }

    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    private fun OnAgainButtonEvent() {
        vibratePhone()
        finish()
    }

    private fun OnSaveButtonEvent() {
        vibratePhone()

        //saveVideoGallery()
        check_writeRead_permission { status ->
            if (status == true) {
                var myFile = File(vMyUri!!.path)
                saveVideoGallery(myFile)
            }else {
                Toast.makeText(applicationContext, "Okuma ve Yazma izinleriniz eksik.", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun OnUploadbuttonEvent() {
        vibratePhone()

        preloader()

        println("$TAG onuploadbuttonEvent : click")
        var myFile = File(vMyUri!!.path)
        // gönderim işlemi başlatıldı.
        videoUploadController?.let {
            it.uploadStart(projectId!!,userId!!,this,vMyUri!!,processType!!,myFile)
        }
    }

    companion object {
        private val ALLOWED_CHARACTERS_RANDOM = "0123456789qwertyuiopasdfghjklzxcvbnm"
    }

    private fun getRandomString(sizeOfRandomString: Int): String {
        val random = Random()
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString)
            sb.append(ALLOWED_CHARACTERS_RANDOM[random.nextInt(ALLOWED_CHARACTERS_RANDOM.length)])
        return sb.toString()
    }

    private val VIDEO_DIRECTORY = "/odiVideo"

    private fun saveVideoGallery(filePath: File?) {

        saveButton.isEnabled = false

        var randomName:String = getRandomString(8)
        val values = ContentValues(3)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        //values.put(MediaStore.Video.Media.TITLE, "odi_$randomName")
        //values.put(MediaStore.Video.Media.DATA, filePath?.absolutePath)

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

            var buffer = ByteArray(4096)
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
                println("$TAG Gallery: SAVE VIDEO video dosyası var bulundu")
            } else {
                println("$TAG Gallery: SAVE VIDEO video dosyası yok bulunamadı")
            }

            Toast.makeText(this, "Video başarılı bir şekilde kaydedildi.", Toast.LENGTH_LONG).show()

        } catch (e: FileNotFoundException) {
            Log.e("VideoComp", e.message)
            Toast.makeText(this, "Video bir hatadan dolayı kaydedilemedi. kod:2", Toast.LENGTH_LONG).show()
            saveButton.isEnabled = true
        } catch (e: java.lang.Exception) {
            Log.e("VideoComp", e.message)
            Toast.makeText(this, "Video bir hatadan dolayı kaydedilemedi. kod:3", Toast.LENGTH_LONG).show()
            saveButton.isEnabled = true
        }
    }

    internal fun check_writeRead_permission(completion: (Boolean?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            completion(true)
        } else {
            completion(false)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), Permission_Result.UPLOAD_VIDEO_GALLERY.value)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permission_Result.UPLOAD_VIDEO_GALLERY.value == requestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                var myFile = File(vMyUri!!.path)
                saveVideoGallery(myFile)
            }else {
                setResult(RESULT_OK)
                println("$TAG izin verilmedi okuma yazma")
                // alert buraya yazılacak
                Toast.makeText(applicationContext, "Okuma ve Yazma izinleriniz eksik.", Toast.LENGTH_LONG).show()
            }

        }
    }

}

object FilePath2 {

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