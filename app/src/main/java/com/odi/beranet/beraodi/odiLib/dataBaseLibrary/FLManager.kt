package com.odi.beranet.beraodi.odiLib.dataBaseLibrary

import android.content.Context
import java.io.File

class FLManager (val context: Context){

    init {

    }

    /**
     * path yolunu file olarak dönüştürür.
     * @param path -> dosya yolu
     * @param callback -> callback true/false ,File/null
     */
    fun pathToFile(path:String?,callback:(Boolean, File?) -> Unit) {
        val file = File(path)
        if (file.exists()) {
            callback(true,file)
        }else {
            callback(false,null)
        }
    }

    /**
     * Dosyanın olup olmadığı kontrolunu yapar
     * @param file => dosya
     */
    fun checkFile(file: File?): Boolean {
        file?.let {
            return file.exists()
        }
        return false
    }

    /**
     * gelen dosyayı direk siler
     * @param file -> dosya
     */
    fun deleteFile(file: File?) {
        file?.let {
            if (checkFile(file)) {
                file.delete()
            }
        }
    }

}