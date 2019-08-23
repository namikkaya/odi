package com.odi.beranet.beraodi

import android.app.Application
import android.content.Context
import com.onesignal.OneSignal

class ApplicationClass: Application() {
    override fun onCreate() {
        super.onCreate()
        // onesignal notification init

        OneSignal.startInit(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()

        val context: Context = ApplicationClass.applicationContext()
    }

    init {
        instance = this
    }

    companion object {
        private var instance: ApplicationClass? = null
        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

}