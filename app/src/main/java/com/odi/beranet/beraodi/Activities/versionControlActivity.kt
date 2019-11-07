package com.odi.beranet.beraodi.Activities

import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.odi.beranet.beraodi.R

class versionControlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version_control)
        uiActionBar()

    }

    private fun uiActionBar () {
        val bar = supportActionBar
        bar!!.setBackgroundDrawable(ColorDrawable(-0x8500))
        bar.hide()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setHomeButtonEnabled(true)
    }
}
