package com.odi.beranet.beraodi.odiLib

import android.content.Context
import android.support.v4.app.ActivityCompat

class singleton {
    companion object {
        val FILE_UPLOAD_URL = "http://odi.odiapp.com.tr/profilupload.php"

        /**
         * Kullanıcının notification id si
         */
        var onesignal_playerId:String? = null
        var onesignal_registrationId:String? = null
        /**
         * kullanıcının odi id si
         */
        var userId:String? = null

        var preloaderContext:Any? = null

        //var onStartOpenVideoGalleryStatus:Boolean = false

        var previewVideoStatus:VIDEO_PREVIEW_STATUS? = null
        var originalVideoPath:String? = null

        var uriPath:String? = null
    }
}