package com.odi.beranet.beraodi.odiLib

interface odiInterface {
    fun Interface_profilPhotoUploadStatus(status:HTTP_PROCESS_STATUS) {}

    //!! -> uploadManager
    /** Progress başlatılıyor*/
    fun onStarted() {}

    /** başarılı bitti*/
    fun onCompleted(){}

    /** Hata oluştu*/
    fun onError(errorMessage: String?) {}

}