package com.odi.beranet.beraodi.odiLib

import android.graphics.Color
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.odi.beranet.beraodi.models.playlistDataModel
import com.odi.beranet.beraodi.models.playlistItemDataModel
import com.odi.beranet.beraodi.models.playlistReplik
import java.util.*
import kotlin.collections.ArrayList

class odiMediaManager (val playListData:playlistDataModel?):
    monologManager.monologManagerListener,
    dialogManager.dialogManagerListener,
    playModeManager.playModeManagerListener
{

    private val TAG:String = "odiMediaManager"

    interface odiMediaManagerListener {
        fun odiMediaManagerListener_monologText(subtitle:SpannableString?){}
        fun odiMediaManagerListener_monologTextComplete(){}

        fun odiMediaManagerListener_dialogText(subtitle:SpannableString?){}
        fun odiMediaManagerListener_dialogTextComplete(){}

        fun odiMediaManagerListener_nextButtonVisible(status:Boolean?){}
    }

    private var myMonologManager:monologManager? = null
    private var myDialogManager:dialogManager? = null
    private var myPlayModeManager:playModeManager? = null
    private var replikList:ArrayList<playlistReplik> = ArrayList()
    var listener:odiMediaManagerListener? = null
    private var recordTypeHolder:RECORD_TYPE? = null

    fun prepare() {
        if (playListData?.type != null) {
            when(playListData?.type!!) {
                RECORD_TYPE.PLAYMODE -> {
                    recordTypeHolder = RECORD_TYPE.PLAYMODE
                    playModeConfiguration(playListData.dataList)
                    println("$TAG prepare: ")
                }
                RECORD_TYPE.DIALOG -> {
                    // burayı hazırlayacağız...
                    recordTypeHolder = RECORD_TYPE.DIALOG
                    dialogConfiguration(playListData.dataList)
                }
                RECORD_TYPE.MONOLOG -> {
                    recordTypeHolder = RECORD_TYPE.MONOLOG
                    monologConfiguration(playListData.dataList)
                }
            }
        }
    }

    /**
     * ALTYAZIYI BAŞLATIR.
     */
    fun startDialog() {
        when(recordTypeHolder!!) {
            RECORD_TYPE.PLAYMODE -> {
                myPlayModeManager!!.startProject()
            }
            RECORD_TYPE.DIALOG -> {
                myDialogManager!!.startProject()
            }
            RECORD_TYPE.MONOLOG -> {
                myMonologManager!!.startProject()
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <MonologDelegate>

    private fun monologConfiguration(myData: ArrayList<playlistItemDataModel>?) {
        myMonologManager = monologManager()
        myMonologManager?.listener = this

        if (myData != null) {
            for (item: playlistItemDataModel in myData!!) {
                val replikData = playlistReplik(item.text!!, item.duration!!, "", item)
                replikList.add(replikData)
            }
            myMonologManager?.replikList = replikList
        }
    }

    override fun monologManagerListener_monologText(subtitle: SpannableString?) {
        super.monologManagerListener_monologText(subtitle)
        listener?.odiMediaManagerListener_monologText(subtitle)
    }

    override fun monologManagerListener_monologTextComplete() {
        super.monologManagerListener_monologTextComplete()
        listener?.odiMediaManagerListener_monologTextComplete()
    }
    //-------------------------------------------------------------------


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <dialogDelegate>

    private fun dialogConfiguration(myData: ArrayList<playlistItemDataModel>?) {
        myDialogManager = dialogManager()
        myDialogManager?.listener = this


        if (myData != null) {
            for (item: playlistItemDataModel in myData!!) {
                println("$TAG dialogConfiguration: text:${item.text} - duration: ${item.duration} - type: ${item.type} ")
                val replikData = playlistReplik(item.text, item.duration, item.type, item)
                replikList.add(replikData)
            }
            myDialogManager?.replikList = replikList
        }
    }

    override fun dialogManagerListener_dialogText(subtitle: SpannableString?) {
        super.dialogManagerListener_dialogText(subtitle)
        listener?.odiMediaManagerListener_dialogText(subtitle)
    }

    override fun dialogManagerListener_dialogTextComplete() {
        super.dialogManagerListener_dialogTextComplete()
        listener?.odiMediaManagerListener_dialogTextComplete()
    }

    override fun dialogManagerListener_dialogNextButtonVisible(status: Boolean?) {
        super.dialogManagerListener_dialogNextButtonVisible(status)
        listener?.odiMediaManagerListener_nextButtonVisible(status)
    }

    //-------------------------------------------------------------------

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <PlayModeDelegate>

    private fun playModeConfiguration(myData: ArrayList<playlistItemDataModel>?) {
        if (myPlayModeManager != null) {
            return
        }
        println("$TAG playModeConfiguration playModeManager ++")
        myPlayModeManager = playModeManager()
        myPlayModeManager?.listener = this

        if (myData != null) {
            for (item: playlistItemDataModel in myData!!) {
                val replikData = playlistReplik(item.text, item.duration, item.type, item)
                replikList.add(replikData)
                println("$TAG in playModeManager: ${replikList}")
            }
            myPlayModeManager?.replikList = replikList
        }
    }


    override fun playModeManagerListener_playModeText(subtitle: SpannableString?) {
        super.playModeManagerListener_playModeText(subtitle)
    }

    override fun playModeManagerListener_playModeTextComplete() {
        super.playModeManagerListener_playModeTextComplete()
    }

    //-------------------------------------------------------------------

    /**
     * Bir sonraki repliğe geçer
     */
    fun nextReplik() {
        when(playListData?.type!!) {
            RECORD_TYPE.MONOLOG -> {
                if (myMonologManager != null) {
                    myMonologManager?.nextReplik()
                }
            }
            RECORD_TYPE.DIALOG -> {
                if (myDialogManager != null) {
                    myDialogManager?.nextReplik()
                }
            }
            RECORD_TYPE.PLAYMODE -> {
                if (myPlayModeManager != null) {
                    myPlayModeManager?.nextReplik()
                }
            }
        }
    }

    fun stopAnimation () {
        when(playListData?.type!!) {
            RECORD_TYPE.MONOLOG -> {
                if (myMonologManager != null) {
                    myMonologManager?.stopAnimation()
                }
            }
            RECORD_TYPE.DIALOG -> {
                if (myDialogManager != null) {
                    myDialogManager?.stopAllAnimation()
                }
            }

            RECORD_TYPE.PLAYMODE -> {
                if (myPlayModeManager != null) {
                    myPlayModeManager?.allStopAnimation()
                }
            }
        }
    }

    fun onNext() {
        when(playListData?.type!!) {

            RECORD_TYPE.DIALOG -> {
                if (myDialogManager != null) {
                    myDialogManager?.nextReplik()
                }
            }

        }
    }



}