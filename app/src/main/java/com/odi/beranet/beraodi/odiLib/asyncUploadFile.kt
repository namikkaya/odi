package com.odi.beranet.beraodi.odiLib

import android.os.AsyncTask
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.BufferedInputStream

class asyncUploadFile: AsyncTask<String, Int, String?>() {
    override fun doInBackground(vararg params: String?): String? {
        println("async: doInBackground " + params[0])
        uploadFile(params[0])
        return ""
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
    }

    override fun onCancelled(result: String?) {
        super.onCancelled(result)
        println("async: onCancelled")
    }

    private fun uploadFile(s: String?) {
        val ftpClient = FTPClient()
        ftpClient.connect("ftp.odiapp.com.tr", 21)
        val checkConnection = ftpClient.login("odiFtp@odiapp.com.tr", "Root123*")
        ftpClient.changeWorkingDirectory("/img/")
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

        var buffIn: BufferedInputStream? = null
        var buffIn1: BufferedInputStream? = null

        print("async: uploadFile: $s")



    }

}