package com.odi.beranet.beraodi.models

import com.odi.beranet.beraodi.odiLib.asyncUploadFile
import com.odi.beranet.beraodi.odiLib.nativePage
import com.odi.beranet.beraodi.odiLib.odiInterface
import java.io.File

class Model_images {
    private var al_imagepath:ArrayList<String>? = null

    fun getAl_imagepath(): ArrayList<String>? {
        return al_imagepath
    }

    fun setAl_imagepath(al_imagepath:ArrayList<String>?) {
        this.al_imagepath = al_imagepath
    }
}

data class async_upload_video (val _id:String?, val _uploadFile: File?, val _listener: odiInterface, val type:nativePage, val userId:String) {
}

/**
 * _uploadStatus = true ise yükleme tamamlandı, false ise devam ediyor
 * _uploadProgress =  _uploadStatus true ise yükleme durumunu haber verir.
 * */
data class async_upload_video_complete(val _id:String?, val _userId:String?, val requestPath:String?, val _uploadStatus:Boolean?, val _uploadProgress:Int?) {}