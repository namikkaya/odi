package com.odi.beranet.beraodi.Activities

import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.odi.beranet.beraodi.R

class galeryActivity : AppCompatActivity() {

    private var myBackButton:ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galery)

        uiConfig()
    }

    private fun uiConfig() {
        val bar = supportActionBar
        bar!!.setBackgroundDrawable(ColorDrawable(-0x8500))
        bar.hide()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setHomeButtonEnabled(true)

        myBackButton = findViewById(R.id.myBackButton)
        myBackButton?.setOnClickListener(clickListener)
    }


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // Button Click Listener

    val clickListener = View.OnClickListener { view ->
        when (view.getId()) {
            R.id.myBackButton -> onBackButtonEvent()
        }
    }

    private fun onBackButtonEvent() {
        finish()
    }

    //-------------------------------------------------------------------

}
