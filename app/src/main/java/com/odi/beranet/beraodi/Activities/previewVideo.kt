package com.odi.beranet.beraodi.Activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.*
import android.widget.*
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.correctionData
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.models.toolTipModel
import com.odi.beranet.beraodi.odiLib.*
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.animationManager
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.videoGalleryManager
import com.onesignal.OneSignal
import com.squareup.picasso.Picasso
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
                //deleteVideoFile() // yükleme bittiğinde video silinmesi kapatıldığından dolayı commentli
            }
            println("$TAG dbTakip: onActivityResult ")
            Toast.makeText(applicationContext, "Video başarılı bir şekilde yüklendi.", Toast.LENGTH_LONG).show()
            intent.putExtra("STATUS", "OKEY")
            setResult(RESULT_OK, intent)

            singleton.cameraResult = 1

            finish()
        }
        if (requestCode == Activity_Result.VIDEO_GALLERY_PAGE.value) {
            if (resultCode == RESULT_OK) {
                data?.let { it->
                    if (it.getSerializableExtra("selectedData") != null) {
                        val myModel = it.getSerializableExtra("selectedData") as dataBaseItemModel
                        println("$TAG video path: ${myModel.videoPath}")

                        singleton.uriPath = myModel.videoPath
                        singleton.thumbPath = myModel.thumb

                        singleton.cameraResult = 2
                    }
                }

            }
            if(resultCode == RESULT_CANCELED) {
                if (singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.SAVED || singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.RECORDING) {
                    finish()
                }
            }
        }
    }

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
        var resultStatus:Boolean = false
        if (mp != null) {
            resultStatus = mp!!.isPlaying
        }
        return resultStatus
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        var resultDuration:Int = 0
        if (mp != null) {
            resultDuration = mp!!.duration
        }
        return resultDuration
    }

    override fun pause() {
        try {
            if (mp != null) {
                mp!!.pause()
            }
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
        var resultPosition:Int = 0
        if (mp != null) {
            resultPosition = mp!!.currentPosition
        }
        return resultPosition
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        try {
            if (mp != null) {
                mp!!.start()
            }
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
    private lateinit var projegaleri:ImageView
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
    var bottomBarHeight:Int? = null

    private var tooltipManager:tooltipAnimationManager? = null

    var preloaderIntent: Intent? = null
    private val VIDEO_DIRECTORY = "videosOfOdi"
    private val VIDEO_DIRECTORY_2 = "videoOfOdiRecord"

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bottomBarHeight = this!!.getBottomHeight()

        window.decorView.apply {
            systemUiVisibility = /*View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or*/
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }


        navigationBarConfiguration()
        uiconfig()

        val bundle=intent.extras
        if(bundle!=null) {
            processType = bundle.getSerializable("type") as nativePage
            projectId = bundle.getString("projectId")
            userId = bundle.getString("userId")

            println("$TAG projectId: $projectId")

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.apply {
            systemUiVisibility = /*View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or*/
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }
    }


    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mediaPlayerConfig(holder)
        println("$TAG surfaceChanged")
    }

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
        }catch (e:IOException) {
            println("$TAG ${e.toString()} -- hata32")
        }catch (e:IllegalStateException) {
            println("$TAG ${e.toString()} -- hata33")
        }
    }

    private fun uiconfig() {
        mediaPlayerLayout = findViewById(R.id.mediaPlayerLayout)
        videoView = findViewById(R.id.myVideoView_previewVideo)

        projegaleri = findViewById(R.id.projegaleri)

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

    }

    private fun getGalleryData() {
        val correction = getProjectAndUserData()
        videoGalleryManager.getProjectVideos(applicationContext, correction.projectId!!) { status, items:ArrayList<dataBaseItemModel>? ->
            items?.let { itv ->
                itv.reverse()
                if (itv.size > 0) {
                    galleryButton.visibility = View.VISIBLE
                    if (singleton.thumbPath == null) {
                        for (i in 0 until itv.size) {
                            //println("$TAG saveDataBase: video: ${itv[i].videoPath} thumb: ${itv[i].thumb}")
                            if (i == 0) {
                                var file = File(itv[i].thumb)
                                Picasso.get().load(file).into(galleryButton)
                            }
                        }
                    }else {
                        var file = File(singleton.thumbPath)
                        Picasso.get().load(file).into(galleryButton)
                    }


                }else {
                    galleryButton.visibility = View.INVISIBLE
                }
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
        if (mp != null) {
            try {
               mp?.pause()
            }catch (e:IllegalStateException){
                println("$TAG hata: ${e.toString()}")
            }

        }
    }

    private fun toolTipCheckStart() {
        if (processType == nativePage.cameraOdile) {
            if (Prefs.sharedData!!.getFirstLookPreviewTooltip() == null || Prefs.sharedData!!.getFirstLookPreviewTooltip() == false) {

                projegaleri.visibility = View.VISIBLE

                val timer = object: CountDownTimer(2000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {

                    }

                    override fun onFinish() {
                        val item2 = toolTipModel(projegaleri,"Kaydettiğin videolar proje galerisinde saklanır.", Gravity.START)
                        val item1 = toolTipModel(saveButton,"Çektiğin videoları kaydedebilirsin.", Gravity.END)
                        //val item3 = toolTipModel(uploadButton,"Seçili videoyu odiye yollar.", Gravity.START)

                        val toolTipArray:ArrayList<toolTipModel> = ArrayList<toolTipModel>()
                        //toolTipArray.add(item2)
                        toolTipArray.add(item1)
                        toolTipArray.add(item2)

                        tooltipManager = tooltipAnimationManager(this@previewVideo,toolTipArray)
                        tooltipManager!!.startTooltip(projegaleri)
                        Prefs.sharedData!!.setFirstLookPreviewTooltip(true)
                    }
                }
                timer.start()
            }
        }
    }

    var firstStart:Boolean = false
    override fun onResume() {
        super.onResume()

        toolTipCheckStart()

        if (singleton.gotoCamera) {
            singleton.gotoCamera = false
            finish()
            return
        }

        getGalleryData()
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
                if (mediaController != null) {
                    mediaController?.isEnabled = true
                    mediaController?.show(0)
                }

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

        singleton.uriPath = null
        singleton.thumbPath = null

        if (!videoSaveStatus && singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.RECORDING) {
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
            intent.putExtra("STATUS", "RESET")
            setResult(RESULT_OK, intent)
            finish()
        }

        alert.setNegativeButton(R.string.exitSaveButtonNo){ dialog, which ->
            vibratePhone()
        }

        alert.show()
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


        println("$TAG uploadID: ${projectId}")

        val data = getProjectAndUserData()

        println("$TAG uploadID: userID: ${data.userId} - projetId: ${data.projectId} ")

        // bu idler geldiğinde showreel projesi olarak kaydet...
        if (data.projectId == "87" ||
            data.projectId == "663" ||
            data.projectId == "661" ||
            data.projectId == "664" ||
            data.projectId == "665" ||
            data.projectId == "666" ||
            data.projectId == "667") {

            processType = nativePage.cameraShowReel

            println("$TAG uploadID: processId: ${processType}")
        }

        println("$TAG uploadID: projectID: p:${data.projectId} - u:${data.userId} - pt: ${processType}")
        videoUploadController?.let {
            //it.uploadStart(projectId!!, userId!!, this, vMyUri!!, processType!!, myFile)
            it.uploadStart(data.projectId!!, data.userId!!, this, vMyUri!!, processType!!, myFile)
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

    // video telefon galerisine kaydeder...
    private fun saveVideoGallery(filePath: File?) {

        saveButton.isEnabled = false

        val values = ContentValues(3)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

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
        if (vMyUri == null) {
            return
        }
        var myFile = File(vMyUri!!.path)
        if (myFile.exists()) {
            myFile.delete()
            println("$TAG dbTakip recording dosya siliniyor...")
        }
    }

    // test et user idyi

    private var animManager:animationManager? = null

    private fun saveDataBase() {
        val correction = getProjectAndUserData()
        val bitmap = createVideoThumb(vMyUri!!.path)
        var randomName:String = getRandomString(16)
        val imageFile = createImage(randomName, bitmap)

        thumbImage.setImageBitmap(bitmap)

        animManager = animationManager(galleryButton,thumbImage)
        animManager?.startAnimation()

        // klasör oluşturur
        var f1 = File(Environment.getExternalStorageDirectory().absolutePath, VIDEO_DIRECTORY_2)
        if (!f1.exists()) {
            f1.mkdirs()
        }

        println("$TAG saveDataBase dbTakip orjinal: ${vMyUri!!.path} --")

        var videoPath = vMyUri!!.path
        if (processType == nativePage.cameraShowReel || processType == nativePage.cameraIdentification) {
            val newName = randomName + ".mp4"
            val myFileName = File(f1,newName)

            println("$TAG saveDataBase dbTakip yeni: ${myFileName.path} -- ")

            var newFile = renameFile(vMyUri!!.path , myFileName.path)
            videoPath = newFile!!.path
        }

        var checkFile = File(vMyUri!!.path)
        if (checkFile.exists()) {
            println("$TAG saveDataBase dbTakip: eski dosya kontrolü: VAR")
        }else{
            println("$TAG saveDataBase dbTakip: eski dosya kontrolü: YOK")
        }

        val item = dataBaseItemModel(null, videoPath, correction.projectId, imageFile!!.path)
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

        getGalleryData()
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

        if (VIDEO_PREVIEW_STATUS.RECORDING != singleton.previewVideoStatus || videoSaveStatus) {
            vibratePhone()
            val correction = getProjectAndUserData()

            videoGalleryManager.getProjectVideos(applicationContext, correction.projectId!!) { status, data:ArrayList<dataBaseItemModel>? ->
                videoGalleryIntent = Intent(this, videoGalleryActivity::class.java)
                videoGalleryIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                videoGalleryIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                videoGalleryIntent?.putExtra("galleryData", data)
                videoGalleryIntent?.putExtra("projectId", correction.projectId!!)

                if (vMyUri != null) {
                    singleton.removeVideoPath = vMyUri!!.path
                }else {
                    singleton.removeVideoPath = null
                }

                startActivityForResult(videoGalleryIntent, Activity_Result.VIDEO_GALLERY_PAGE.value)
            }
        }else if (!videoSaveStatus){
            val alert = AlertDialog.Builder(this)

            alert.setTitle(R.string.exitSaveVideoTitle)
            alert.setMessage(R.string.galleriGidis)

            alert.setCancelable(false)

            alert.setPositiveButton(R.string.exitSaveButtonNext){ dialog, which ->
                vibratePhone()
                deleteVideoFile()
                val correction = getProjectAndUserData()

                videoGalleryManager.getProjectVideos(applicationContext, correction.projectId!!) { status, data:ArrayList<dataBaseItemModel>? ->
                    videoGalleryIntent = Intent(this, videoGalleryActivity::class.java)
                    videoGalleryIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    videoGalleryIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    videoGalleryIntent?.putExtra("galleryData", data)
                    videoGalleryIntent?.putExtra("projectId", correction.projectId!!)

                    if (vMyUri != null) {
                        singleton.removeVideoPath = vMyUri!!.path
                    }else {
                        singleton.removeVideoPath = null
                    }

                    startActivityForResult(videoGalleryIntent, Activity_Result.VIDEO_GALLERY_PAGE.value)
                }
            }

            alert.setNegativeButton(R.string.exitSaveButtonNo){ dialog, which ->
                vibratePhone()
            }

            alert.show()
        }
    }

    private fun getBottomHeight (): Int? {
        val resources = applicationContext!!.resources
        val resourcesId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourcesId > 0) {
            var returnData:Int? = null
            try {
                returnData = resources.getDimensionPixelSize(resourcesId)
            }catch (e: IllegalStateException){
                Log.e("$TAG" , "${e.toString()} -> hata: 513")
            }

            return returnData
        }
        return 0
    }


}

