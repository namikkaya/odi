package com.odi.beranet.beraodi.odiLib

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File

class odiFileManager {

    private val TAG:String = "odiFileManager: "


    /***
     * deletePath:
     * fullPath: dosya yolu
     * completion: true / false - başarılı / başarısız
     */
    fun deletePath(fullPath:String , completion: (Boolean) -> Unit) {
        var myFile:File = File(fullPath)
        if (myFile.exists()) {
            myFile.delete()
            println(TAG + " deletePath: dosya silindi")
            completion(true)
        }else {
            completion(false)
            println(TAG + "path: $fullPath deletePath: dosya yok")
        }

    }

    internal fun renameFile(from: File, toPrefix: String, toSuffix: String): File {
        val directory = from.parentFile
        if (!directory.exists()) {
            if (directory.mkdir()) {
                println("$TAG dosya oluşturuldu:  " + directory.absolutePath)
            }
        }
        var newFile = File(directory, toPrefix + toSuffix)
        var i = 1
        while (newFile.exists() && i < Integer.MAX_VALUE) {
            newFile = File(directory, "$toPrefix($i)$toSuffix")
            i++
        }
        if (!from.renameTo(newFile)) {
            println("$TAG Dosya adı değiştirilemedi " + newFile.absolutePath)
            return from
        }
        println("$TAG Dosya adı başarılı bir şekilde değiştirildi " + newFile.absolutePath)
        return newFile
    }


    /***
     * deleteProviderFile provider olan dosyaları siler.
     */
    fun deleteProviderFile(context: Context, path:Uri, completion: (Boolean) -> Unit) {

        var cr:ContentResolver = context.contentResolver
        var projection: Array<String> = arrayOf(MediaStore.MediaColumns.DATA)
        var cur:Cursor = cr.query(path, projection, null, null, null)

        if (cur != null) {
            if (cur.moveToFirst()) {
                var filePath:String = cur.getString(0);
                var newFile:File = File(filePath)
                if (newFile.exists()) {
                    println(TAG + " dosya bulundu silindi")
                    context.contentResolver.delete(path, null, null)
                    completion(true)
                }else {
                    println(TAG + " dosya bulunamadı")
                    completion(false)
                }

            }else {
                println(TAG + " uri iyiydi kayıt ama kayıt bulunamadı")
                completion(false)
            }
            cur.close()
        }else{
            println(TAG + " içerik uri geçersiz veya başka hata var")
            completion(false)
        }


    }




}