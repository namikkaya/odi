package com.odi.beranet.beraodi.odiLib

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.support.customtabs.CustomTabsClient.getPackageName
import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import com.odi.beranet.beraodi.R

class countDownManager(val layout:RelativeLayout, val context:Activity) {
    interface countDownManagerListener {
        fun onCountDownManagerListener_progress(count:String) {}
        fun onCountDownManagerListener_complete() {}
    }


    private val TAG:String = "countDownManager"
    var listener:countDownManagerListener? = null

    private var countDownTextView: TextView

    var countDownStrings: Array<String> = arrayOf("3","2", "1", "")
    private var counter:Int = 0

    init {
        countDownTextView = layout.findViewById(R.id.countDownText)
        tickPrepare()
        finalPrepare()
    }

    fun startCountDown() {
        countDownTextView.text = "3"
        val timer = object: CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (counter >= countDownStrings.size) {
                    return
                }

                if (counter <= 2) {
                    tickPlay()
                }else if (counter == 3) {
                    finalPlay()
                }
                countDownTextView.text = countDownStrings[counter]
                stepTimer(countDownStrings[counter])
                counter++
            }

            override fun onFinish() {
                println("$TAG onFinish: $counter")
                counter = 0
                completeTimer()
            }
        }
        timer.start()
    }


    fun completeTimer() {
        listener?.onCountDownManagerListener_complete()
    }

    fun stepTimer(count:String) {
        listener?.onCountDownManagerListener_progress(count)
    }

    var tickMediaPlayer:MediaPlayer? = null
    var finalMediaPlayer:MediaPlayer? = null

    fun tickPlay() {
        tickMediaPlayer!!.start()
    }

    fun tickPrepare() {
        var mediaPath = context.resources.getIdentifier("dit","raw",context.packageName )
        tickMediaPlayer = MediaPlayer.create(context,mediaPath)
        try {
            tickMediaPlayer?.prepare()
        }catch (e:Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun finalPlay() {
        finalMediaPlayer?.start()
    }

    fun finalPrepare() {
        var mediaPath = context.resources.getIdentifier("finaldit","raw",context.packageName )
        finalMediaPlayer = MediaPlayer.create(context,mediaPath)
        try {

            finalMediaPlayer!!.prepare()
        }catch (e:Exception) {
            Log.e(TAG, e.toString())
        }
    }



}