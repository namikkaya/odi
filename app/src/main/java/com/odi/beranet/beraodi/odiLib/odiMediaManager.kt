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
    dialogManager.dialogManagerListener {

    private val TAG:String = "odiMediaManager"

    interface odiMediaManagerListener {
        fun odiMediaManagerListener_monologText(subtitle:SpannableString?){}
        fun odiMediaManagerListener_monologTextComplete(){}

        fun odiMediaManagerListener_dialogText(subtitle:SpannableString?){}
        fun odiMediaManagerListener_dialogTextComplete(){}


    }

    private var myMonologManager:monologManager? = null
    private var myDialogManager:dialogManager? = null
    private var replikList:ArrayList<playlistReplik> = ArrayList()
    var listener:odiMediaManagerListener? = null
    private var recordTypeHolder:RECORD_TYPE? = null

    fun prepare() {
        if (playListData?.type != null) {
            when(playListData?.type!!) {
                RECORD_TYPE.PLAYMODE -> {
                    recordTypeHolder = RECORD_TYPE.PLAYMODE

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

    //-------------------------------------------------------------------

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <PlayModeDelegate>



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
        }
    }

    fun stopAnimation () {
        when(playListData?.type!!) {
            RECORD_TYPE.MONOLOG -> {
                if (myMonologManager != null) {
                    myMonologManager?.stopAnimation()
                }
            }
        }
    }





}