package com.odi.beranet.beraodi.Activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.widget.ProgressBar
import android.widget.TextView
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.singleton
import com.onesignal.OneSignal
import com.odi.beranet.beraodi.models.progressData
import com.odi.beranet.beraodi.odiLib.odiInterface


class preloaderActivity : baseActivity(), odiInterface {
    private val TAG:String = "preloaderActivity"

    private lateinit var progressBar: CircularProgressBar
    private lateinit var titleText:TextView
    private var myActionBar: ActionBar? = null

    private var listener: odiInterface? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preloader)

        progressBar = findViewById(R.id.kayaProgressBar)
        titleText = findViewById(R.id.progressBarTitle)

        navigationBarConfiguration()
        getMyExtras()
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

    public fun progressChangeData(_progress:Int? , _title:String?, _complete:Boolean?) {
        println("$TAG veri title: $_title - progress: $_progress - complete: $_complete")
        if (_complete != null) {
            if (_complete!!) {
                finish()
            }
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

    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    private fun getMyExtras() {
        val extras = intent.extras
        if (extras != null) {
            val myTitle = extras.getString("title")
            val progress = extras.getString("progress")

            titleText.let {
                it!!.text = myTitle
            }
        }
    }



}



