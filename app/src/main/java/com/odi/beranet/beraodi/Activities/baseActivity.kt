package com.odi.beranet.beraodi.Activities

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import ua.eshcherbinock.reachability.Reachability
import ua.eshcherbinock.reachability.ReachabilityStateObserver


abstract class baseActivity: AppCompatActivity(), ReachabilityStateObserver {


    private val baseTAG:String = "baseActivity:"
    var mReachability: Reachability? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseConfig()
    }

    fun baseConfig(){
        println("$baseTAG baseConfig init")
        reachabilityConfig()
    }

    fun reachabilityConfig() {
        mReachability = Reachability(this)
        mReachability?.addListener(this)
    }

    fun startListener() {
        mReachability?.let { myReachability ->
            myReachability.startNotifier()
        }
    }

    fun stopListener() {
        mReachability?.let { myReachability ->
            myReachability.stopNotifier()
        }
    }


    fun infoDialog(_title:String = "", _desc:String = "", _okeyButton:String = "Tamam") {
        val alert = AlertDialog.Builder(this)
        alert.setTitle(_title)
        alert.setMessage(_desc)
        alert.setCancelable(true)
        alert.setPositiveButton(_okeyButton) { _, _ ->

        }

        val builder = alert.create()
        builder.show()
    }


    override fun onReachabilityStateChange(newState: Reachability.State) {
        println("$baseTAG $newState")
        when (newState) {

            Reachability.State.REACHABLE -> {
                internetConnectionStatus(true)
            }

            Reachability.State.NOT_REACHABLE -> {
                internetConnectionStatus(false)
            }

        }
    }

    /** internet status true / false*/
    protected abstract fun internetConnectionStatus(status:Boolean)


    override fun onDestroy() {
        super.onDestroy()

        if (mReachability != null) {
            mReachability?.removeListener(this)
            mReachability = null
        }

    }

    override fun onResume() {
        super.onResume()
        println("$baseTAG onResume")
        startListener()
    }

    override fun onPause() {
        super.onPause()
        println("$baseTAG onPause")
        stopListener()
    }

}