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
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.Permission_Result
import com.odi.beranet.beraodi.odiLib.nativePage
import com.odi.beranet.beraodi.odiLib.singleton
import com.odi.beranet.beraodi.odiLib.vibratePhone
import com.onesignal.OneSignal
import java.io.*
import java.lang.IllegalStateException
import java.util.*

class previewVideo : baseActivity(), SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl {
    override fun isPlaying(): Boolean {
        return mp!!.isPlaying
    }

    override fun canSeekForward(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return true
    }

    override fun getDuration(): Int {
        return mp!!.duration
    }

    override fun pause() {

    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun seekTo(pos: Int) {

    }

    override fun getCurrentPosition(): Int {
        return mp!!.currentPosition
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {

    }

    override fun getAudioSessionId(): Int {
       return 0
    }

    override fun canPause(): Boolean {
        return !mp!!.isPlaying
    }


    private val TAG:String = "previewVideo:"

    private var processType: nativePage? = null


    private lateinit var videoView:VideoView

    private lateinit var mySurface:SurfaceView
    private var surfaceHolder:SurfaceHolder? = null

    private lateinit var againButton:ImageButton
    private lateinit var saveButton:ImageButton
    private lateinit var uploadButton:ImageButton
    private var myActionBar: ActionBar? = null

    private var vMyUri:Uri? = null

    private var mp:MediaPlayer? = null

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

    private var currentVideoPath:String? = null
    private var projectId:String? = null
    private var userId:String? = null

    private var videoUri:Uri? = null


    private var mediaPlayerLayout:RelativeLayout? = null
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
        if(bundle!=null)
        {
            projectId = bundle.getString("projectId")
            userId = bundle.getString("userId")
            //vMyUri = bundle.getUr("videoPath")
            val str = bundle.getString("videoPath")

            vMyUri = Uri.parse(str)

            println("$TAG video uri : $vMyUri")

            var file = File(vMyUri!!.path)
            if (file.exists()){
                println("$TAG video dosyası bulundu")
                println("$TAG viedo dosyası boyutu: ${file.length()}")
            }else {
                println("$TAG video dosyasında problem var")
            }

        }

        navigationBarConfiguration()
        uiconfig()
    }

    private fun getUriToFile(myUri:Uri):File? {
        val path = FilePath2.getPath(this,myUri)
        val myFile = File(path)
        return myFile
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) { }

    override fun surfaceDestroyed(holder: SurfaceHolder?) { }

    override fun surfaceCreated(holder: SurfaceHolder?) {

        try {
            mp!!.setDataSource(this,vMyUri)
            mp!!.prepare()
            mp!!.setDisplay(holder)
            mp?.setOnPreparedListener(this)
            mp?.setOnCompletionListener(this)

            mp!!.start()
        }catch (e:IOException) {
            println("$TAG ${e.toString()}")
        }



    }

    var mediaController:MediaController? = null

    private fun uiconfig() {
        mediaPlayerLayout = findViewById(R.id.mediaPlayerLayout)
        videoView = findViewById(R.id.myVideoView_previewVideo)

        mp = MediaPlayer()




        againButton = findViewById(R.id.againButton_previewVideo)
        saveButton = findViewById(R.id.saveButton_previewVideo)
        uploadButton = findViewById(R.id.uploadButton_previewVideo)
        mySurface = findViewById(R.id.mySurface)

        againButton.setOnClickListener(clickListener)
        saveButton.setOnClickListener(clickListener)
        uploadButton.setOnClickListener(clickListener)


        window.setFormat(PixelFormat.UNKNOWN)
        surfaceHolder = videoView.holder
        surfaceHolder!!.setFixedSize(800, 480)
        surfaceHolder!!.addCallback(this)


        mediaController = MediaController(this)
        mediaController?.setAnchorView(mySurface)

        videoView.setMediaController(mediaController)


    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.recordButton -> OnAgainButtonEvent()
            R.id.saveButton_previewVideo -> OnSaveButtonEvent()
            R.id.uploadButton_previewVideo -> OnUploadbuttonEvent()
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        try {
            mp!!.seekTo(0)
            mp!!.start()
        }catch (e:IllegalStateException) {
            println("$TAG ${e.toString()} hata: 112")
        }
    }

    var mHandler: Handler? = null

    override fun onPrepared(mp: MediaPlayer?) {
        //mp!!.start()

        mHandler?.post(Runnable {
            mediaController?.isEnabled = true
            mediaController?.show()
        })
    }

    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    private fun OnAgainButtonEvent() {

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
                println("$TAG Gallery: SAVE VIDEO video dosyası var bulundu")
            } else {
                println("$TAG Gallery: SAVE VIDEO video dosyası yok bulunamadı")
            }

        } catch (e: FileNotFoundException) {
            Log.e("VideoComp", e.message)
        } catch (e: java.lang.Exception) {
            Log.e("VideoComp", e.message)
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