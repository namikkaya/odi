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
import com.odi.beranet.beraodi.odiLib.nativePage
import com.odi.beranet.beraodi.odiLib.odiInterface
import com.odi.beranet.beraodi.odiLib.singleton
import com.onesignal.OneSignal

class upload_from_gallery : baseActivity(), odiInterface {

    enum class UI_preloaderBar{
        endless,
        progress
    }

    private val TAG:String = "upload_from_gallery"
    var videoUploadController: videoUploadViewModel? = null

    private var selectedUri: Uri? = null
    private var processType: nativePage? = null

    //-- object
    private var videoView:VideoView? = null
    private var mediaController: MediaController? = null
    private var progressBarContainer:RelativeLayout? = null
    private var myActionBar: ActionBar? = null
    private lateinit var cancelButton:Button
    private lateinit var sendButton:Button
    private lateinit var progressBarTitle: TextView
    private lateinit var  kayaProgressBar: CircularProgressBar
    private lateinit var endlessProgress: ProgressBar
    // -- values

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_from_gallery)
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
        endlessProgress = findViewById(R.id.compressImage)

        kayaProgressBar = findViewById(R.id.kayaProgressBar)

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
        videoUploadController?.getImageUrlWithAuthority(this,selectedUri!!,processType!!)
    }

    override fun onUploadVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?) {
        super.onUploadVideoStatus(_id, _progress, _complete)
        if(!_complete!!) {
            println("$TAG YÜKLEME DEVAM EDİYOR $_progress")
        }else {
            println("$TAG YÜKLEME TAMAMLANDI")

        }
    }

    private fun preloaderStatus(status:UI_preloaderBar) {
        if (status == UI_preloaderBar.endless) {

        }
    }

}
