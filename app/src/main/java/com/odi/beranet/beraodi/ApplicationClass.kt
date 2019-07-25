package com.odi.beranet.beraodi

import android.app.Application
import com.onesignal.OneSignal

class ApplicationClass: Application() {
    override fun onCreate() {
        super.onCreate()
        // onesignal notification init
        OneSignal.startInit(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()
    }
}