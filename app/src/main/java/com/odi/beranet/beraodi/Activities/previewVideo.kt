package com.odi.beranet.beraodi.Activities

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ThumbnailUtils
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
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import com.odi.beranet.beraodi.MainActivityMVVM.videoUploadViewModel
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.correctionData
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.odiLib.*
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.animationManager
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.videoGalleryManager
import com.onesignal.OneSignal
import java.io.*
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList

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

            if (!videoSaveStatus) {
                deleteVideoFile()
            }

            Toast.makeText(applicationContext, "Video başarılı bir şekilde yüklendi.", Toast.LENGTH_LONG).show()
            intent.putExtra("STATUS", "OKEY")
            setResult(RESULT_OK, intent)
            finish()
        }
        if (requestCode == Activity_Result.VIDEO_GALLERY_PAGE.value) {
            if (resultCode == RESULT_OK) {
                data?.let { it->
                    if (it.getSerializableExtra("selectedData") != null) {
                        val myModel = it.getSerializableExtra("selectedData") as dataBaseItemModel
                        println("$TAG video path: ${myModel.videoPath}")

                        singleton.uriPath = myModel.videoPath

                        /*var myFile = File(myModel.videoPath)
                        vMyUri = Uri.fromFile(myFile)
                        addMediaController()*/
                    }
                }

            }
            if(resultCode == RESULT_CANCELED) {
                if (singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.SAVED) {
                    finish()
                }
            }
        }
    }

    var preloaderIntent: Intent? = null

    private fun preloader() {
        preloaderIntent = Intent(this, preloaderActivity::class.java)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
    private lateinit var thumbImage:ImageView
    private lateinit var galleryButton:RoundRectCornerImageView
    //lateinit var testImage:ImageView
    private var myActionBar: ActionBar? = null

    private var projectId:String? = null
    private var userId:String? = null
    private var vMyUri:Uri? = null
    private var processType: nativePage? = null
    private var videoSaveStatus:Boolean = false

    var mHandler: Handler? = null

    private var mp:MediaPlayer? = null
    var warningIntent: Intent? = null
    var videoUploadController: cameraUploadViewModel? = null

    public var errorVideoProblem:Boolean = false


    var videoGalleryIntent: Intent? = null

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
        if (mp != null) {
            mp!!.pause()
            mp!!.stop()
            mp!!.release()
        }
        super.onBackPressed()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_video)

        println("$TAG resetTakip onCreate")

        navigationBarConfiguration()
        uiconfig()

        val bundle=intent.extras
        if(bundle!=null) {
            processType = bundle.getSerializable("type") as nativePage
            projectId = bundle.getString("projectId")
            userId = bundle.getString("userId")

            if (singleton.uriPath != null) {
                var myFile = File(singleton.uriPath)
                vMyUri = Uri.fromFile(myFile)
                addMediaController()
                println("$TAG sing: true")
            }else {
                if(bundle.getString("videoPath") != null) {
                    val str = bundle.getString("videoPath")
                    vMyUri = Uri.parse(str)
                    addMediaController()
                    println("$TAG sing: false")
                }
            }




        }

        println("$TAG previewVideoStatus = ${singleton.previewVideoStatus}")
        if(singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.SAVED ||
            singleton.previewVideoStatus == null) {
            saveButton.isEnabled = false
            saveButton.visibility = View.INVISIBLE
        }


        videoUploadController = cameraUploadViewModel(this,this)



    }


    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mediaPlayerConfig(holder)
        println("$TAG surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) { }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mediaPlayerConfig(holder)
        println("$TAG surfaceCreated")
    }

    private fun mediaPlayerConfig(holder:SurfaceHolder?) {
        println("$TAG mediaPlayerConfig init ${vMyUri?.path}")

        try {
            mp!!.setDataSource(this,vMyUri)
            mp!!.prepare()
            mp!!.setDisplay(holder)


            surfaceHolder = holder

            mp?.setOnPreparedListener(this)
            mp?.setOnCompletionListener(this)

            //mp!!.start()
        }catch (e:IOException) {
            println("$TAG ${e.toString()} -- hata32")
        }catch (e:IllegalStateException) {
            println("$TAG ${e.toString()} -- hata33")
        }
    }

    private fun uiconfig() {
        mediaPlayerLayout = findViewById(R.id.mediaPlayerLayout)
        videoView = findViewById(R.id.myVideoView_previewVideo)
        //testImage = findViewById(R.id.testImage)

        againButton = findViewById(R.id.againButton_previewVideo)
        saveButton = findViewById(R.id.saveButton_previewVideo)
        uploadButton = findViewById(R.id.uploadButton_previewVideo)
        mySurface = findViewById(R.id.mySurface)
        thumbImage = findViewById(R.id.thumbImage)
        galleryButton = findViewById(R.id.galleryButton)


        againButton.setOnClickListener(clickListener)
        saveButton.setOnClickListener(clickListener)
        uploadButton.setOnClickListener(clickListener)
        videoView.setOnClickListener(clickListener)
        galleryButton.setOnClickListener(clickListener)

        videoGalleryManager.getProjectVideos(applicationContext,projectId!!){ status: Boolean, data: ArrayList<dataBaseItemModel>? ->
            data.let {

            }

        }


    }

    private fun addMediaController() {
        if (mp != null) {
            mp!!.pause()
            mp!!.stop()
            mp!!.release()
            mp = null
        }

        if (surfaceHolder != null) {
            surfaceHolder!!.removeCallback(this)
            surfaceHolder = null
        }

        mp = MediaPlayer()

        window.setFormat(PixelFormat.UNKNOWN)
        surfaceHolder = videoView.holder
        surfaceHolder!!.setFixedSize(800, 480)
        surfaceHolder!!.addCallback(this)

        videoView.requestFocus()
        println("$TAG addMediaController init")

    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.againButton_previewVideo -> OnAgainButtonEvent()
            R.id.saveButton_previewVideo -> OnSaveButtonEvent()
            R.id.uploadButton_previewVideo -> OnUploadbuttonEvent()
            R.id.myVideoView_previewVideo -> OnMediaControllerStatusEvent()
            R.id.galleryButton -> OnOpenVideoGalleryActivityEvent()
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
        println("$TAG resetTakip onResume")
        if (mp != null) {
            try {

                if (errorVideoProblem) {
                    errorVideoProblem = false
                    Toast.makeText(getApplicationContext(), "Video yüklenemedi. Şuan bir sorun var. Lütfen videonuzu kaydedip odi ekibiyle iletişime geçin.", Toast.LENGTH_LONG).show();
                }

            }catch (e:IllegalStateException){
                println("$TAG ${e.toString()}")
            }
        }

        if (singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.SAVED){
            OnOpenVideoGalleryActivityEvent()
        }

        if (firstStart) {
            finish()
            startActivity(intent)
            overridePendingTransition(0,0)
        }
        firstStart = true
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
        mediaController?.setPadding(0,0,0,0)

        val viewGroupLevel1 = mediaController?.getChildAt(0)
        viewGroupLevel1?.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.blackTransparent))

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
        println("$TAG onAgainButtonEvent")

        singleton.uriPath = null

        if (!videoSaveStatus) {
            onAlert_againAlert()
        }else {
            intent.putExtra("STATUS", "RESET")
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun onAlert_againAlert() {
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.exitSaveVideoTitle)
        alert.setMessage(R.string.exitSaveVideoDesc)

        alert.setCancelable(false)

        alert.setPositiveButton(R.string.exitSaveButtonYes){ dialog, which ->
            vibratePhone()
            deleteVideoFile()
            //finish()

            intent.putExtra("STATUS", "RESET")
            setResult(RESULT_OK, intent)
            finish()
        }

        alert.setNegativeButton(R.string.exitSaveButtonNo){ dialog, which ->
            vibratePhone()
        }

        alert.show()

        println("$TAG onAlertdialog open")
    }

    private fun OnSaveButtonEvent() {
        vibratePhone()

        saveButton.isEnabled = false
        saveButton.alpha = 0.5f

        mediaController?.let { it ->
            if (it.isShowing) {
                it.hide()
            }
        }

        check_writeRead_permission { status ->
            if (status == true) {
                var myFile = File(vMyUri!!.path)
                //saveVideoGallery(myFile)
                saveDataBase()
            }else {
                Toast.makeText(applicationContext, "Okuma ve Yazma izinleriniz eksik.", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun OnUploadbuttonEvent() {
        vibratePhone()

        preloader()
        mp?.let {
            it.pause()
        }

        var myFile = File(vMyUri!!.path)

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




    private val VIDEO_DIRECTORY = "videosOfOdi"

    // video telefon galerisine kaydeder...
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

            // klasör oluşturur
            var f1 = File(Environment.getExternalStorageDirectory().absolutePath, VIDEO_DIRECTORY)
            if (!f1.exists()) {
                f1.mkdirs()
            }


            var wallpaperDirectory = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + VIDEO_DIRECTORY)

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
                //saveVideoGallery(myFile)
                saveDataBase()
                saveButton.isEnabled = false
                saveButton.alpha = 0.5f
            }else {
                saveButton.isEnabled = true
                saveButton.alpha = 1f
                setResult(RESULT_OK)
                println("$TAG izin verilmedi okuma yazma")
                Toast.makeText(applicationContext, "Okuma ve Yazma izinleriniz eksik.", Toast.LENGTH_LONG).show()
            }

        }
    }

    //** delete
    fun deleteVideoFile() {
        /*if (vMyUri == null) {
            return
        }

        var myFile = File(vMyUri!!.path)
        if (myFile.exists()) {
            myFile.delete()
        }*/
    }

    // test et user idyi

    private var animManager:animationManager? = null

    private fun saveDataBase() {
        val correction = getProjectAndUserData()

        val bitmap = createVideoThumb(vMyUri!!.path)
        var randomName:String = getRandomString(8)
        val imageFile = createImage(randomName, bitmap)

        thumbImage.setImageBitmap(bitmap)

        animManager = animationManager(galleryButton,thumbImage)
        animManager?.startAnimation()

        val item = dataBaseItemModel(null,vMyUri!!.path, correction.projectId, imageFile!!.path)
        println("$TAG saveDataBase projectId: ${correction.projectId}")
        println("$TAG saveDataBase userId: ${correction.userId}")
        println("$TAG saveDataBase imagePath: ${imageFile!!.path}")
        println("$TAG saveDataBase video path: ${vMyUri!!.path} --")
        videoGalleryManager.insertVideoItem(applicationContext,item){ status ->
            if(status) {
                println("$TAG saveDataBase: video yazıldı. yeni proje dosyası açıldı")
            }else {
                println("$TAG saveDataBase: video var olan proje ye yazıldı.")
            }
            videoSaveStatus = true
        }
    }

    private fun createVideoThumb(videoPath:String):Bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND)

    private fun createImage(name:String, bitmap:Bitmap):File? {
        val file_path = Environment.getExternalStorageDirectory().absolutePath + "/odiThumb"
        val file = File(file_path)
        if (!file.exists()) {
            file.mkdirs()
        }
        var myFile = File(file,name+".jpg")
        try {
            var fOut = FileOutputStream(myFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut)
            fOut.flush()
            fOut.close()
        }catch (e: Exception) {
            e.printStackTrace()
        }


        return myFile
    }

    private fun getProjectAndUserData() : correctionData {
        var myUserId:String = ""
        var myProjectId:String = ""
        if (processType == nativePage.cameraOdile) {
            myUserId = projectId!!
            myProjectId = userId!!
        }else {
            myUserId = userId!!
            myProjectId = projectId!!
        }
        return correctionData(myUserId, myProjectId)
    }

    private fun OnOpenVideoGalleryActivityEvent() {

        vibratePhone()
        val correction = getProjectAndUserData()

        videoGalleryManager.getProjectVideos(applicationContext, correction.projectId!!) { status, data:ArrayList<dataBaseItemModel>? ->
            videoGalleryIntent = Intent(this, videoGalleryActivity::class.java)
            videoGalleryIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            videoGalleryIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            videoGalleryIntent?.putExtra("galleryData", data)

            startActivityForResult(videoGalleryIntent, Activity_Result.VIDEO_GALLERY_PAGE.value)
        }
    }


}

