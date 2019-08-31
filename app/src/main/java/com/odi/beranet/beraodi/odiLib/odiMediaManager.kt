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

class odiMediaManager (val playListData:playlistDataModel?): monologManager.monologManagerListener {
    private val TAG:String = "odiMediaManager"


    interface odiMediaManagerListener {
        fun odiMediaManagerListener_monologText(subtitle:SpannableString?){}
        fun odiMediaManagerListener_monologTextComplete(){}
    }

    private var myMonologManager:monologManager? = null
    private var replikList:ArrayList<playlistReplik> = ArrayList()
    var listener:odiMediaManagerListener? = null
    private var recordTypeHolder:RECORD_TYPE? = null

    fun start() {


        if (playListData?.type != null) {
            when(playListData?.type!!) {
                RECORD_TYPE.PLAYMODE -> {

                }
                RECORD_TYPE.DIALOG -> {

                }
                RECORD_TYPE.MONOLOG -> {
                    recordTypeHolder = RECORD_TYPE.MONOLOG
                    monologConfiguration(playListData.dataList)
                }
            }
        }
    }

    private fun monologConfiguration(myData: ArrayList<playlistItemDataModel>?) {
        myMonologManager = monologManager()
        myMonologManager?.listener = this

        if (myData != null) {
            for (item: playlistItemDataModel in myData!!) {
                val replikData = playlistReplik(item.text!!, item.duration!!, "")
                replikList.add(replikData)

            }

            myMonologManager?.replikList = replikList
            myMonologManager?.startDialog()
        }
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // <MonologDelegate>
    override fun monologManagerListener_monologText(subtitle: SpannableString?) {
        super.monologManagerListener_monologText(subtitle)
        listener?.odiMediaManagerListener_monologText(subtitle)
    }

    override fun monologManagerListener_monologTextComplete() {
        super.monologManagerListener_monologTextComplete()
        listener?.odiMediaManagerListener_monologTextComplete()
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