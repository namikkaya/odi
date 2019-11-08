package com.odi.beranet.beraodi.Activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.SELECTED_CONTAINER
import org.w3c.dom.Text

class versionControlActivity : AppCompatActivity() {
    lateinit var googlePlayButton: ImageButton
    lateinit var versionDecs:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version_control)
        uiActionBar()



        versionDecs = findViewById(R.id.versionDecs)
        googlePlayButton = findViewById(R.id.googlePlayButton)

        if (intent.getStringExtra("desc") != null){
            val desc = intent.getStringExtra("desc")
            versionDecs.text = desc
        }


        googlePlayButton.setOnClickListener(clickListener)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }

    private fun uiActionBar () {
        val bar = supportActionBar
        bar!!.setBackgroundDrawable(ColorDrawable(-0x8500))
        bar.hide()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setHomeButtonEnabled(true)
    }

    private fun gotoStore() {
        val appPackageName = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)))
        }catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
        }
    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.googlePlayButton -> gotoStore()
        }
    }
}
