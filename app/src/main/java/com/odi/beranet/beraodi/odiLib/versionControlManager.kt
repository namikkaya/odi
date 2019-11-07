package com.odi.beranet.beraodi.odiLib

import android.os.AsyncTask
import com.odi.beranet.beraodi.models.async_versionControlModel
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class versionControlManager:AsyncTask<async_versionControlModel?, Int, HashMap<String, String>>() {


    private var listener:odiInterface? = null
    var empDataHashMap = HashMap<String, String>()

    private fun setDelegate(listener: odiInterface?) {
        this.listener = listener
    }

    override fun doInBackground(vararg params: async_versionControlModel?): HashMap<String, String> {
        params[0]?._listener?.let { setDelegate(it) }
        val thread = object : Thread() {
            override fun run() {
                try {
                    // Comment-out this line of code
                    val builderFactory = DocumentBuilderFactory.newInstance()
                    val docBuilder = builderFactory.newDocumentBuilder()
                    val doc = docBuilder.parse(InputSource(params[0]?._url?.openStream()))
                    doc.normalize()
                    // reading player tag


                    val nodeList:NodeList = doc.getElementsByTagName("androidApp")
                    //val str:String = nodeList.item(0).normalize().toString()
                    var sub = nodeList.item(0).childNodes.length
                    for (i in 0 until sub) {
                        if (nodeList.item(0).childNodes.item(i).nodeType == Node.ELEMENT_NODE) {
                            val versionControl = nodeList.item(0).childNodes.item(i).childNodes
                            for (s in 0 until versionControl.length) {
                                val myItem = nodeList.item(0).childNodes.item(i).childNodes.item(s)
                                if (myItem.nodeType == Node.ELEMENT_NODE) {
                                    val names = myItem.nodeName
                                    val values = myItem.firstChild.nodeValue
                                    //println("evrim: ic:${names + " - " + values} --")
                                    empDataHashMap[names] = values
                                }
                            }

                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ParserConfigurationException) {
                    e.printStackTrace()
                } catch (e: SAXException) {
                    e.printStackTrace()
                }
            }
        }

        thread.start()

        return  empDataHashMap
    }

    override fun onPostExecute(result: HashMap<String, String>?) {
        super.onPostExecute(result)
        println("evrim : -- onPostExecute")
        listener?.versionControlManagerDelegate(empDataHashMap)
    }


}