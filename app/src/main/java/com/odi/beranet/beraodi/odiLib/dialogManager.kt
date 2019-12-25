package com.odi.beranet.beraodi.odiLib

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.odi.beranet.beraodi.models.playlistItemDataModel
import com.odi.beranet.beraodi.models.playlistReplik
import java.util.*
import kotlin.collections.ArrayList

class dialogManager {
    interface dialogManagerListener {
        fun dialogManagerListener_dialogText(subtitle: SpannableString?, charIndex:Int?){}
        fun dialogManagerListener_dialogTextComplete(){}
        fun dialogManagerListener_dialogNextButtonVisible(status:Boolean?){}
        fun dialogManagerListener_dialogText_clearText(){}
    }

    private val TAG:String = "dialogManager:"

    var listener: dialogManagerListener? = null
    var replikList:ArrayList<playlistReplik> = ArrayList()
    private var replikCounter:Int = 0

    var timer: Timer? = null
    var timerHandler: Handler? = null
    var timerTask: TimerTask? = null
    var characterCounter:Int = 0

    var holder:playlistItemDataModel? = null

    internal fun startProject() {
        replikCounter = 0
        characterCounter = 0
        startDialog()
    }

    private fun startDialog() {
        println("$TAG startDialog: replik sayısı: ${replikList.size} replikCounter: $replikCounter")
        stopAnimation()

        if(replikCounter >= replikList.size) {
            listener?.dialogManagerListener_dialogTextComplete()
            replikCounter = 0
            characterCounter = 0
            return
        }

        subtitle(replikList[replikCounter].duration,
            replikList[replikCounter].text,
            replikList[replikCounter].type,
            replikList[replikCounter])
    }

    private fun subtitle(duration: Long?,
                         subtitle:String?,
                         type:String?,
                         replikItem:playlistReplik?) {
        if (duration != null && subtitle != null) {
            val textLength:Int = subtitle.length
            println("$TAG character duration1: $duration length: $textLength")
            var characterDuration:Double = ( duration.toDouble() / textLength.toDouble() ) // bir karakter için harcanacak zaman
            println("$TAG character duration2: $characterDuration long ${characterDuration.toLong()}")

            if (type == "0") { // dış ses
                if (volumeStatus!!) {
                    replikItem?.item?.mediaPlayerSetVolume(1f)
                }else {
                    replikItem?.item?.mediaPlayerSetVolume(0f)
                }
                replikItem?.item?.playSound()
                holder = replikItem?.item
                startAnimation(subtitle, characterDuration.toLong(), Color.parseColor("#0083B2"))
                listener?.dialogManagerListener_dialogNextButtonVisible(false)
            }else { // ben
                startAnimation(subtitle, characterDuration.toLong(), Color.parseColor("#FF8400"))
                listener?.dialogManagerListener_dialogNextButtonVisible(true)
            }

        }
    }

    /**
     * @param => altyazı textinin tamamını
     * @param => Her bir karakterin kaç saniyede boyanacağı
     */
    private fun startAnimation(subtitle: String, animationDuration:Long, color:Int?) {
        stopAnimation()

        // first start call
        /*listener?.dialogManagerListener_dialogText(subtitlePainter(subtitle,0, characterCounter, color!!), characterCounter)
        characterCounter++*/


        timerHandler = Handler()
        timerTask = object: TimerTask() {
            override fun run() {
                timerHandler!!.post(object: Runnable {
                    override fun run() {

                        if (characterCounter >= subtitle.length+1) {

                            println("$TAG characterCounter: text sıfırla")
                            characterCounter = 0
                            replikCounter++
                            stopAnimation()
                            listener?.dialogManagerListener_dialogText_clearText()
                            startDialog()
                        }else {
                            listener?.dialogManagerListener_dialogText(subtitlePainter(subtitle,0, characterCounter, color!!),characterCounter)
                        }
                        characterCounter++
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

    fun stopAllAnimation() {
        for (item in replikList) {
            item.item?.stopSound()
        }
        stopAnimation()
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
        characterCounter = 0
        if (holder != null) {
            holder?.stopSound()
            holder = null
        }
        stopAnimation()
        startDialog()
    }

    private var volumeStatus:Boolean? = true
    internal fun onSetVolume (status:Boolean?) {
        volumeStatus = status
    }
}