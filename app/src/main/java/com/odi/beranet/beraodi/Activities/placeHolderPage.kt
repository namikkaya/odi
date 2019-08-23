package com.odi.beranet.beraodi.Activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.odi.beranet.beraodi.MainActivity
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.Prefs
import com.odi.beranet.beraodi.odiLib.sharedObject

class placeHolderPage : AppCompatActivity() {
    private val TAG:String = "placeHolderPage:"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_holder_page)
        supportActionBar!!.hide()

        Handler().postDelayed(
            {
                this.onFakeCreate()
            },
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        )
    }


    private fun onFakeCreate() {
        setTheme(R.style.AppTheme)

        Handler().postDelayed({
            if (Prefs.sharedData!!.getFirstLook() != null) {

                if (Prefs.sharedData!!.getFirstLook()!!) {
                    startActivity(Intent(this@placeHolderPage, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    this@placeHolderPage.finish()
                }else {
                    Prefs.sharedData!!.setFirstLook(true)
                    val intent = Intent(this@placeHolderPage, introActivity::class.java)
                    startActivity(intent)
                    this@placeHolderPage.finish()
                }
            } else {
                Prefs.sharedData!!.setFirstLook(true)
                val intent = Intent(this@placeHolderPage, introActivity::class.java)
                startActivity(intent)
                this@placeHolderPage.finish()
            }
        }, 1000)
    }

}
