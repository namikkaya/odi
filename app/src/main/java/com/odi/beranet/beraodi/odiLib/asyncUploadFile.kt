package com.odi.beranet.beraodi.odiLib

import android.os.AsyncTask
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.io.CopyStreamAdapter
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import org.apache.commons.net.io.CopyStreamListener as CopyStreamListener

class asyncUploadFile: AsyncTask<File, Int, String?>() {

    var streamListener:CopyStreamAdapter? = null

    override fun doInBackground(vararg params: File?): String? {
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

    internal var holderBytes = 0

    private fun uploadFile(file: File?) {
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
                println("async: transfer : $totalBytesTransferred")


                val myPercent = (totalBytesTransferred * 100 / (file!!.length()).toInt())

                //String percentString = Integer.toString(myPercent);
                println("Yükleme:holder: $holderBytes percent: $myPercent")

                if (holderBytes <= myPercent) {
                    println("async: Video yükleniyor... $holderBytes")
                    holderBytes = myPercent.toInt()
                } else {
                    println("async: Video yükleniyor... Yükleme sonuçlandı")

                    val url = "http://odi.odiapp.com.tr/?yeni_islem=tanitim&id=161&uzanti=mp4"

                    request(url)

                    removeCopyStreamListener(streamListener)
                }


            }

        }

        ftpClient.copyStreamListener = streamListener

        ftpClient.storeFile(file!!.getName().replace(file!!.getName(), "tanitim_161.mp4"), buffIn)

        buffIn.close()
        ftpClient.logout()
        ftpClient.disconnect()

    }


    @Throws(IOException::class)
    fun request(uri: String): String {
        val ab = StringBuilder()
        val url = URL(uri)
        val conn = url.openConnection() as HttpURLConnection
        try {

            while (true){

                val inStr = BufferedInputStream(conn.inputStream)
                val bf = BufferedReader(InputStreamReader(inStr))

                val inputLine: String? = bf.readLine()
                if (inputLine!! != "")
                    break
                ab.append(inputLine)
                println("async: request complete ")
            }

        } finally {
            conn.disconnect()
        }


        return ab.toString()
    }

}