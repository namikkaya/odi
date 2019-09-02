package com.odi.beranet.beraodi.odiLib

import android.app.Activity
import android.os.CountDownTimer
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
    }

    fun startCountDown() {
        countDownTextView.text = "3"
        val timer = object: CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (counter >= countDownStrings.size) {
                    return
                }
                println("$TAG ontick: $counter")
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


}