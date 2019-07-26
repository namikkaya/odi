package com.odi.beranet.beraodi.odiLib

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
    }
}