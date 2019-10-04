package com.odi.beranet.beraodi.odiLib

import android.net.Uri
import com.odi.beranet.beraodi.models.async_upload_video_complete
import java.io.IOException

interface odiInterface {
    fun Interface_profilPhotoUploadStatus(status:HTTP_PROCESS_STATUS) {}

    /** Progress başlatılıyor*/
    fun onStarted() {}

    /** başarılı bitti*/
    fun onCompleted(){}

    /** Hata oluştu*/
    fun onError(errorMessage: String?) {}

    /** Yükleme için bir id yollanır. Yollanan id ye ait işonCameraActivity_playlistSoundCompletelem bittiğinde bu method tetiklenir.
     * asyncUploadFile ile videoUploadViewModel arasında iletişimi sağlar
     * */
    fun uploadVideoAsyncTaskComplete(resultData: async_upload_video_complete?) {}

    fun uploadErrorAsyncTask(e:async_upload_video_complete){}

    /**Yükleme sırasında % de bilgilerinin kullanıcıya gösterilmesi için gereklidir. */
    fun onUploadVideoStatus(_id:String?, _progress:Int?, _complete:Boolean?) { }

    fun onUploadBitmapStatus(_id:String?, _progress:Int?, _complete:Boolean?) { }

    fun onUploadExitPreloader() {}
    /**
     * compress edilen videonun durumunu bildirir.
     */
    fun onCompressVideoStatus(_id: String?, _progress: Int?, _complete: Boolean?){}

    fun onProgressCallBack(progress:Int?, complete:Boolean?) {}

    fun onCameraActivity_playlistSoundComplete(index:Int?, duration:Long?) {}

    fun OnPlaylistItemPlayerEnd(index: Int?, type:RECORD_TYPE){}


}