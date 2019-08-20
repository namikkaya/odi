package com.odi.beranet.beraodi.odiLib

import com.odi.beranet.beraodi.models.async_upload_video_complete

interface odiInterface {
    fun Interface_profilPhotoUploadStatus(status:HTTP_PROCESS_STATUS) {}

    /** Progress başlatılıyor*/
    fun onStarted() {}

    /** başarılı bitti*/
    fun onCompleted(){}

    /** Hata oluştu*/
    fun onError(errorMessage: String?) {}

    /** Yükleme için bir id yollanır. Yollanan id ye ait işlem bittiğinde bu method tetiklenir.
     * asyncUploadFile ile videoUploadViewModel arasında iletişimi sağlar
     * */
    fun uploadVideoAsyncTaskComplete(resultData: async_upload_video_complete?) {}

    /**Yükleme sırasında % de bilgilerinin kullanıcıya gösterilmesi için gereklidir. */
    fun onUploadVideoStatus(_id:String?, _progress:Int?, _complete:Boolean?) { }

    fun onUploadBitmapStatus(_id:String?, _progress:Int?, _complete:Boolean?) { }

    /*
    /**Yükleme bilgilerini içerir.
     * Dinleyici: upload_from_gallery
     * Tetikleyici: videoUploadViewModel
     *
     * @param id : Yükleme ayrıştırmak için id
     * @param uploadType : Hangi işlemin yapıldığı
     * @param progress : Yapılan işlemin durumunu haber verir
     * @param complete : İşlemlerin bittiğini haber verir
     *
     * */
    fun onUploadFileStatusListener(id: String?, uploadType:UPLOADPROCESS, progress:Int?, complete:Boolean?) {}*/
}