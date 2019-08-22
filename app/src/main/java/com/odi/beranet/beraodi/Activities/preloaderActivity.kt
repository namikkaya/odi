package com.odi.beranet.beraodi.Activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.WindowManager
import android.widget.TextView
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.singleton
import com.onesignal.OneSignal
import com.odi.beranet.beraodi.models.progressData
import com.odi.beranet.beraodi.odiLib.odiInterface


class preloaderActivity : baseActivity() {
    private val TAG:String = "preloaderActivity"

    private lateinit var progressBar: CircularProgressBar
    private lateinit var titleText:TextView
    private var myActionBar: ActionBar? = null
    val animationDuration = 500
    var titleHolder:String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preloader)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        singleton.preloaderContext = this

        progressBar = findViewById(R.id.CircularProgressBarOBJ)
        titleText = findViewById(R.id.progressBarTitle)

        navigationBarConfiguration()
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

    public fun progressChangeData(_progress:Int?, _title:String?, _complete:Boolean?) {
        println("$TAG veri title: $_title - progress: $_progress - complete: $_complete")
        if (_complete != null) {
            if (_complete!!) {
                intent.putExtra("STATUS", "OKEY")
                setResult(RESULT_OK, intent)
                finish()
                return
            }
        }

        if (_progress != null) {
            runOnUiThread {
                progressBar.setProgressWithAnimation(_progress?.toFloat(), animationDuration.toLong())
            }

        }
        if (_title != null) {
            titleText.text = _title!!
            if (titleHolder != _title) {

                runOnUiThread {
                    progressBar.setProgressWithAnimation(0f, 0)
                }
            }
            titleHolder = _title
        }

    }

    private fun oneSignalConfiguration() {
        OneSignal.idsAvailable { userId, registrationId ->
            if (registrationId != null) {
                singleton.onesignal_playerId = userId
                singleton.onesignal_registrationId = registrationId
            }
        }
    }

    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    override fun onBackPressed() {
        return
    }

}



