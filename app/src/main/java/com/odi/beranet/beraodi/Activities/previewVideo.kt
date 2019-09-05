package com.odi.beranet.beraodi.Activities

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.View
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.VideoView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.nativePage
import com.odi.beranet.beraodi.odiLib.singleton
import com.onesignal.OneSignal
import java.io.File

class previewVideo : baseActivity() {
    private val TAG:String = "previewVideo:"

    private var processType: nativePage? = null

    private lateinit var videoView:VideoView
    private lateinit var againButton:ImageButton
    private lateinit var saveButton:ImageButton
    private lateinit var uploadButton:ImageButton
    private var myActionBar: ActionBar? = null

    private var mediaController: MediaController? = null

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_video)

        val bundle=intent.extras
        if(bundle!=null)
        {
            projectId = bundle.getString("projectId")
            userId = bundle.getString("userId")
            currentVideoPath = bundle.getString("videoPath")

            videoUri = Uri.parse(currentVideoPath)//Uri.fromFile(File(currentVideoPath))

            if (File(currentVideoPath).exists()) {
                println("$TAG bundle dosya var ")
            }else {
                println("$TAG bundle dosya yok ")
            }
        }

        navigationBarConfiguration()
        uiconfig()
        videoPlayerConfig()
    }


    private fun uiconfig() {
        videoView = findViewById(R.id.myVideoView_previewVideo)
        againButton = findViewById(R.id.againButton_previewVideo)
        saveButton = findViewById(R.id.saveButton_previewVideo)
        uploadButton = findViewById(R.id.uploadButton_previewVideo)

        againButton.setOnClickListener(clickListener)
        saveButton.setOnClickListener(clickListener)
        uploadButton.setOnClickListener(clickListener)
    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.recordButton -> OnAgainButtonEvent()
            R.id.saveButton_previewVideo -> OnSaveButtonEvent()
            R.id.uploadButton_previewVideo -> OnUploadbuttonEvent()
        }
    }

    private fun videoPlayerConfig() {
        mediaController = MediaController(this)
        mediaController?.setAnchorView(videoView)

        videoView?.setMediaController(mediaController)
        videoView?.setVideoURI(videoUri)
        videoView?.requestFocus()
        videoView?.start()

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

    }

    private fun OnUploadbuttonEvent() {

    }

}
