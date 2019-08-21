package com.odi.beranet.beraodi.Activities
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.TextView
import com.odi.beranet.beraodi.R


class warningActivity : baseActivity() {
    private val TAG:String = "warningActivity:"

    var titleText:TextView? = null
    var descriptionText:TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warning)


        titleText = findViewById(R.id.warningTitleText)
        descriptionText = findViewById(R.id.warningDescriptionText)

        val extras = intent.extras
        if (extras != null) {
            val myTitle = extras.getString("warningTitle")
            val myDescription = extras.getString("warningDescription")

            titleText.let {
                it!!.text = myTitle
            }
            descriptionText.let {
                it!!.text = myDescription
            }
        }

        uiActionBar()
    }

    private fun uiActionBar () {
        val bar = supportActionBar
        bar!!.setBackgroundDrawable(ColorDrawable(-0x8500))
        bar.hide()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setHomeButtonEnabled(true)
    }

    override fun internetConnectionStatus(status: Boolean) {

        if (status) {
            this.finish()
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }

}
