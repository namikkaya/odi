package com.odi.beranet.beraodi.Activities

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.ActionBar
import android.support.v7.widget.DialogTitle
import android.view.View
import android.widget.*
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.odi.beranet.beraodi.MainActivityMVVM.videoUploadViewModel
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.*
import com.onesignal.OneSignal

class upload_from_gallery : baseActivity(), odiInterface {


    private val TAG:String = "upload_from_gallery"
    var videoUploadController: videoUploadViewModel? = null

    private var selectedUri: Uri? = null
    private var processType: nativePage? = null

    //-- object
    public var videoView:VideoView? = null
    private var mediaController: MediaController? = null
    private var progressBarContainer:RelativeLayout? = null
    private var myActionBar: ActionBar? = null
    private lateinit var cancelButton:Button
    private lateinit var sendButton:Button
    private lateinit var testImage:ImageView
    // -- values

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_from_gallery)


        //testImage = findViewById(R.id.testImage)

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
            // internet geldiğinde tekrar ettir.
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

        //videoUploadController?.getImageUrlWithAuthority("videoUpload", this,selectedUri!!, processType!!)
        preloader(null,null)
    }


    var preloaderIntent: Intent? = null
    private fun preloader(_progress: Int?, title:String?) {
        preloaderIntent = Intent(this, preloaderActivity::class.java)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        preloaderIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        //warningIntent?.putExtra("warningTitle", "Bağlantı Sorunu")
        //warningIntent?.putExtra("warningDescription", "İnternet bağlantınızda problem var. Lütfen bağlantınızı kontrol edip tekrar deneyin.")
        startActivity(preloaderIntent)

    }

    fun bro() {
        println("$TAG bro tetiklendi")
    }
    /**
     * Yükleme bilgisini döndüdür
     * */
    override fun onUploadVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadVideoStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.video,_progress,_complete)
    }

    override fun onUploadBitmapStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadBitmapStatus(_id, _progress, _complete)
        preloaderManage(UI_PRELOADER.bitmap,_progress,_complete)
    }

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
            println("$TAG BİTMAP YÜKLEME DEVAM EDİYOR $progress")

        }else {
            println("$TAG BİTMAP YÜKLEME TAMAMLANDI")
        }
    }

    private fun preloaderVideoCompress(progress: Int?, complete: Boolean?){
        if(!complete!!) {
            println("$TAG VİDEO COMPRESS EDİLİYOR $progress")
        }else {
            println("$TAG VİDEO COMPRESS BİTTİ")
        }
    }

    // video upload bittiğinde complete geldiğinde işlemin tamamı bitmiş olur
    private fun preloaderVideoUpload(progress: Int?, complete: Boolean?){
        if(!complete!!) {
            println("$TAG VİDEO YÜKLEME DEVAM EDİYOR $progress")

        }else {
            println("$TAG VİDEO YÜKLEME TAMAMLANDI")
            println("$TAG İşlem bitti yahoooo")
        }
    }

    private fun getContext() {

        val act = singleton.preloaderContext as preloaderActivity
        if (act != null) {
            act.progressChangeData(null,null,null)
        }
    }
}
