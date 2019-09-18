package com.odi.beranet.beraodi.odiLib

import android.os.AsyncTask
import com.odi.beranet.beraodi.models.async_upload_video
import com.odi.beranet.beraodi.models.async_upload_video_complete
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.io.CopyStreamAdapter
import java.io.*
import org.apache.commons.net.io.CopyStreamListener as CopyStreamListener


class asyncUploadFile: AsyncTask<async_upload_video, Int, String?>() {
    private var myTAG:String = "async"
    private var streamListener:CopyStreamAdapter? = null
    private var listener:odiInterface? = null
    private var uploadID:String? = null
    private var userID:String? = null
    private var type:nativePage? = null
    private var uploadFileType:UPLOAD_FILE_TYPE? = null
    internal var holderBytes = 0
    private var returningRequestPath:String? = null

    private var resultDataModel:async_upload_video_complete? = null
    private var progressDataModel:async_upload_video_complete? = null

    private var errorDataModel:async_upload_video_complete? = null

    private fun setDelegate(listener: odiInterface?) {
        this.listener = listener
    }

    override fun doInBackground(vararg params: async_upload_video?): String? {
        params[0]?._listener?.let { setDelegate(it) }

        uploadID = params[0]?._id
        userID = params[0]?.userId
        type = params[0]?.type
        uploadFileType = params[0]?.uploadFileType

        println("yükleme: ${params[0]?._uploadFile?.name}")

        var fileStringName:String = ""
        if (type == nativePage.uploadShowReel) {
            if (uploadFileType == UPLOAD_FILE_TYPE.video) { // hazır video showReel için
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=showreel&id=$userID&uzanti=mp4"
                fileStringName = "showreel_$userID.mp4"
            }else if (uploadFileType == UPLOAD_FILE_TYPE.bitmap) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=showreel&id=$userID&uzanti=mp4"
                fileStringName = "showreel_$userID.jpg"
            }
        }else if (type == nativePage.uploadTanitim){ // hazır video upload tanitim için
            println("Tip ne: $type")
            if (uploadFileType == UPLOAD_FILE_TYPE.video) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=tanitim&id=$userID&uzanti=mp4"
                fileStringName = "tanitim_$userID.mp4"
            }else if (uploadFileType == UPLOAD_FILE_TYPE.bitmap) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=tanitim&id=$userID&uzanti=mp4"
                fileStringName = "tanitim_$userID.jpg"
            }
        }else if (type == nativePage.cameraShowReel) { // cameradan alınmış showreel
            if (uploadFileType == UPLOAD_FILE_TYPE.video) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=showreel&id=$userID&uzanti=mp4"
                fileStringName = "showreel_$userID.mp4"
            }else if (uploadFileType == UPLOAD_FILE_TYPE.bitmap) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=showreel&id=$userID&uzanti=mp4"
                fileStringName = "showreel_$userID.jpg"
            }
        }else if (type == nativePage.cameraIdentification) { // cameradan alınmış tanitim
            if (uploadFileType == UPLOAD_FILE_TYPE.video) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=tanitim&id=$userID&uzanti=mp4"
                fileStringName = "tanitim_$userID.mp4"
            }else if (uploadFileType == UPLOAD_FILE_TYPE.bitmap) {
                returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=tanitim&id=$userID&uzanti=mp4"
                fileStringName = "tanitim_$userID.jpg"
            }
        }else if (type == nativePage.cameraOdile) { // camera odile

            val requestPath = "http://odi.odiapp.com.tr/upld.php?fileName=${params[0]?._uploadFile?.name}&uzanti=mp4"

            if (uploadFileType == UPLOAD_FILE_TYPE.video) {
                returningRequestPath = requestPath
                fileStringName = params[0]?._uploadFile?.name!!

            }else if (uploadFileType == UPLOAD_FILE_TYPE.bitmap) {
                // resim dosyasının ismini video mp4 uzantısı kesilerek ekleniyor


                var jpgName = params[0]?._uploadFile?.name!!.replace(".mp4", ".jpg")
                println("asyncN:  jpgName: $jpgName")
                fileStringName = jpgName

            }
        }

        resultDataModel = async_upload_video_complete(uploadID,userID,returningRequestPath,true, null, uploadFileType)
        uploadFile(params[0]?._uploadFile, fileStringName)
        return "upload"
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        println("async: onProgressUpdate")
    }

    override fun onPreExecute() {
        super.onPreExecute()
        println("async: onPreExecute")
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        println("async: onPostExecute")
        listener?.uploadVideoAsyncTaskComplete(resultDataModel)
    }

    override fun onCancelled(result: String?) {
        super.onCancelled(result)
        println("async: onCancelled")
    }

    private fun uploadFile(file: File?, fileName:String) {
        val ftpClient = FTPClient()
        ftpClient.connect("ftp.odiapp.com.tr", 21)
        val checkConnection = ftpClient.login("odiFtp@odiapp.com.tr", "Root123*")
        ftpClient.changeWorkingDirectory("/img/")
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

        var buffIn: BufferedInputStream? = null

        buffIn = BufferedInputStream(FileInputStream(file), 8192)


        streamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(totalBytesTransferred: Long, bytesTransferred: Int, streamSize: Long) {
                super.bytesTransferred(totalBytesTransferred, bytesTransferred, streamSize)
                val myPercent = (totalBytesTransferred * 100 / (file!!.length()).toInt())

                if (holderBytes <= myPercent) {
                    holderBytes = myPercent.toInt()
                    progressDataModel = async_upload_video_complete(null,null,null,false, holderBytes, uploadFileType)
                    listener?.uploadVideoAsyncTaskComplete(progressDataModel)
                } else {
                    println("async: Video yükleniyor... Yükleme sonuçlandı")
                    removeCopyStreamListener(streamListener)
                }
            }
        }

        ftpClient.copyStreamListener = streamListener

        try {
            ftpClient.storeFile(file!!.name.replace(file!!.name, fileName!!), buffIn)
        }catch (e:IOException) {
            println("$myTAG ${e.toString()}")
            errorDataModel = async_upload_video_complete("error",userID,null,false,null,uploadFileType)
            listener?.uploadVideoAsyncTaskComplete(errorDataModel)
        }

        buffIn.close()
        ftpClient.logout()
        ftpClient.disconnect()
    }

}