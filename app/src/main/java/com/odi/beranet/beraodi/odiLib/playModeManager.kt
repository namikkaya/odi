package com.odi.beranet.beraodi.odiLib

import android.text.SpannableString
import com.odi.beranet.beraodi.models.playlistReplik
import kotlin.collections.ArrayList

class playModeManager: odiInterface {
    interface playModeManagerListener {
        fun playModeManagerListener_playModeText(subtitle: SpannableString?){}
        fun playModeManagerListener_playModeTextComplete(){}
    }

    private val TAG:String = "playModeManager:"

    var listener:playModeManagerListener? = null
    var replikList:ArrayList<playlistReplik> = ArrayList()


    internal fun startProject() {
        println("$TAG start project replikList => $replikList")
        startDialog()
    }

    private fun startDialog() {
        println("$TAG replikList array: ${replikList}")
        if(replikList.size > 0) {
            replikList[0].item?.playSound()
        }
    }

    fun stopAnimation() {
        replikList[0].item?.stopSound()
    }

    fun allStopAnimation() {
        replikList[0].item?.stopSound()
    }

    /**
     * Bir sonraki repliğe geçer
     */
    internal fun nextReplik() {
        stopAnimation()
        startDialog()
    }
}