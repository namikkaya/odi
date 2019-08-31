package com.odi.beranet.beraodi.odiLib

import android.graphics.Color
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.odi.beranet.beraodi.models.playlistReplik
import java.util.*
import kotlin.collections.ArrayList

class monologManager {
    interface monologManagerListener {
        fun monologManagerListener_monologText(subtitle:SpannableString?){}
        fun monologManagerListener_monologTextComplete(){}
    }

    private val TAG:String = "monologManager:"

    var listener:monologManagerListener? = null
    var replikList:ArrayList<playlistReplik> = ArrayList()
    private var replikCounter:Int = 0

    var timer: Timer? = null
    var timerHandler: Handler? = null
    var timerTask: TimerTask? = null
    var characterCounter:Int = 0


    internal fun startDialog() {
        if(replikCounter >= replikList.size) {
            listener?.monologManagerListener_monologTextComplete()
            replikCounter = 0
            return
        }
        subtitle(replikList[replikCounter].duration, replikList[replikCounter].text)
        replikCounter++
    }

    private fun subtitle(duration: Int?, subtitle:String?) {
        if (duration != null && subtitle != null) {
            val textLength:Int = subtitle.length
            println("$TAG character duration1: $duration length: $textLength")
            var characterDuration:Double = 1000 * ( duration.toDouble() / textLength.toDouble() ) // bir karakter için harcanacak zaman
            println("$TAG character duration2: $characterDuration long ${characterDuration.toLong()}")


            startAnimation(subtitle, characterDuration.toLong())
        }
    }


    /**
     * @param => altyazı textinin tamamını
     * @param => Her bir karakterin kaç saniyede boyanacağı
     */
    private fun startAnimation(subtitle: String, animationDuration:Long) {
        stopAnimation()

        // first start call
        listener?.monologManagerListener_monologText(subtitlePainter(subtitle,0, characterCounter, Color.parseColor("#FF8400")))
        characterCounter++

        timerHandler = Handler()
        timerTask = object: TimerTask() {
            override fun run() {
                timerHandler!!.post(object: Runnable {
                    override fun run() {
                        println("$TAG timer: TEST ET")
                        listener?.monologManagerListener_monologText(subtitlePainter(subtitle,0, characterCounter, Color.parseColor("#FF8400")))
                        characterCounter++
                        if (characterCounter >= subtitle.length+1) {
                            stopAnimation()
                            startDialog()
                        }
                    }
                })
            }
        }

        timer = Timer()
        timer?.schedule(timerTask,0,animationDuration)

    }

    fun stopAnimation() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
        if (timerHandler != null) {
            timerHandler = null
        }
        if (timerTask != null) {
            timerTask = null
        }
    }

    private fun subtitlePainter(subtitle: String, startIndex:Int, endIndex:Int, color:Int): SpannableString? {
        val spannable = SpannableString(subtitle)
        spannable.setSpan(ForegroundColorSpan(color), startIndex, endIndex, 0)
        return spannable
    }

    /**
     * Bir sonraki repliğe geçer
     */
    internal fun nextReplik() {
        replikCounter++
        stopAnimation()
        startDialog()
    }



}