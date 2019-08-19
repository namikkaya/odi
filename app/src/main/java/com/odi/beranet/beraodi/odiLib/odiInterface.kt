package com.odi.beranet.beraodi.odiLib

import com.odi.beranet.beraodi.models.async_upload_video_complete

interface odiInterface {
    fun Interface_profilPhotoUploadStatus(status:HTTP_PROCESS_STATUS) {}

    //!! -> uploadManager
    /** Progress başlatılıyor*/
    fun onStarted() {}

    /** başarılı bitti*/
    fun onCompleted(){}

    /** Hata oluştu*/
    fun onError(errorMessage: String?) {}

    /** Yükleme için bir id yollanır. Yollanan id ye ait işlem bittiğinde bu method tetiklenir. */
    fun uploadVideoAsyncTaskComplete(resultData: async_upload_video_complete?) {}


    fun onUploadVideoStatus(_id:String?, _progress:Int?, _complete:Boolean?){}
}