package com.odi.beranet.beraodi.Activities

import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.OrientationEventListener
import com.odi.beranet.beraodi.Activities.cameraActivityFragments.previewFragment
import com.odi.beranet.beraodi.R

class orientationActivity : AppCompatActivity() {
    private val TAG:String = "orientationActivity: "
    enum class Orientation_Status {
        LAND_SCAPE,
        OTHER
    }
    private var mOrientationStatus: previewFragment.Orientation_Status? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orientation)
        orientationListenerConfig()
        uiActionBar()
    }

    private fun uiActionBar () {
        val bar = supportActionBar
        bar!!.setBackgroundDrawable(ColorDrawable(-0x8500))
        bar.hide()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setHomeButtonEnabled(true)
    }

    private lateinit var mOrientationListener: OrientationEventListener

    private fun orientationListenerConfig() {
        mOrientationListener = object: OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation in 235..305) {
                    rotationStatus(previewFragment.Orientation_Status.LAND_SCAPE)
                }else {
                    rotationStatus(previewFragment.Orientation_Status.OTHER)
                }
            }
        }

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        } else {
            mOrientationListener.disable()
        }

    }

    private fun rotationStatus(status: previewFragment.Orientation_Status) {
        if (mOrientationStatus != null) {
            if (status != mOrientationStatus) {
                mOrientationStatus = status
                activityOrientationDesing(mOrientationStatus)
            }
        }else {
            mOrientationStatus = status
            activityOrientationDesing(mOrientationStatus)
        }

    }

    private fun activityOrientationDesing(desingStatus: previewFragment.Orientation_Status?) {
        when(desingStatus) {
            previewFragment.Orientation_Status.LAND_SCAPE -> {
                println("$TAG rotation: EKRAN AÇIK")
                finish()
            }
            previewFragment.Orientation_Status.OTHER -> {
                println("$TAG rotation: EKRAN KAPALI")
            }
        }
    }
}
