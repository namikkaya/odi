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

    private var streamListener:CopyStreamAdapter? = null
    private var listener:odiInterface? = null
    private var uploadID:String? = null
    private var userID:String? = null
    private var type:nativePage? = null
    internal var holderBytes = 0
    private var returningRequestPath:String? = null

    private var resultDataModel:async_upload_video_complete? = null
    private var progressDataModel:async_upload_video_complete? = null

    private fun setDelegate(listener: odiInterface?) {
        this.listener = listener
    }

    override fun doInBackground(vararg params: async_upload_video?): String? { // vararg params: File?
        println("async: doInBackground " + params[0])

        params[0]?._listener?.let { setDelegate(it) }
        uploadID = params[0]?._id
        userID = params[0]?.userId
        type = params[0]?.type

        var fileStringName:String = ""
        if (type == nativePage.uploadShowReel) {
            returningRequestPath = "http://odi.odiapp.com.tr/?yeni_islem=showreel&id="+userID+"&uzanti=mp4"
            fileStringName = "showreel_" + userID + ".mp4"
        }else {

        }
        resultDataModel = async_upload_video_complete(uploadID,userID,returningRequestPath,true, null)

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
        var buffIn1: BufferedInputStream? = null

        buffIn = BufferedInputStream(FileInputStream(file), 8192)

        print("async: uploadFile: $file")

        streamListener = object : CopyStreamAdapter() {

            override fun bytesTransferred(totalBytesTransferred: Long, bytesTransferred: Int, streamSize: Long) {
                super.bytesTransferred(totalBytesTransferred, bytesTransferred, streamSize)
                //println("async: transfer : $totalBytesTransferred")


                val myPercent = (totalBytesTransferred * 100 / (file!!.length()).toInt())

                //String percentString = Integer.toString(myPercent);
                //println("Yükleme:holder: $holderBytes percent: $myPercent")

                if (holderBytes <= myPercent) {
                    //println("async: Video yükleniyor... $holderBytes")
                    holderBytes = myPercent.toInt()
                    progressDataModel = async_upload_video_complete(null,null,null,false, holderBytes)
                    listener?.uploadVideoAsyncTaskComplete(progressDataModel)
                } else {
                    println("async: Video yükleniyor... Yükleme sonuçlandı")
                    removeCopyStreamListener(streamListener)
                }


            }

        }

        ftpClient.copyStreamListener = streamListener

        ftpClient.storeFile(file!!.getName().replace(file!!.getName(), fileName!!), buffIn)

        buffIn.close()
        ftpClient.logout()
        ftpClient.disconnect()

    }

}