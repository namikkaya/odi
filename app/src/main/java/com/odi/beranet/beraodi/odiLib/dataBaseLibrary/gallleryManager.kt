package com.odi.beranet.beraodi.odiLib.dataBaseLibrary

import android.content.Context
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.models.dataBaseProjectModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class gallleryManager (val context: Context){
    private val timeFormat:String = "yyyy-MM-dd HH:mm:ss"

    // ***********
    // DATE --
    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }

    fun String.toDate(format: String): Date { // format "dd-MM-yyyy" -- date
        val date = SimpleDateFormat(format).parse(this)
        println(date.time)
        return date
    }

    // DATE
    // ------------
    private val TAG:String = "galleryManager:"

    private var myDbManager:dbManager
    private var flManager: FLManager

    init {
        flManager = FLManager(context)
        myDbManager = dbManager(context)
    }

    /**
     * Kullanım süresi dolmuş videoları siler
     */
    fun clearExpired(callback:(Boolean) -> Unit) {
        val allProjectData = myDbManager.allProject
        var status:Boolean = false
        for (i in 0 until allProjectData.size) {
            val itemHard = allProjectData[i] as HashMap<String, String>

            val item = dataBaseProjectModel(itemHard.get(dbManager.ID),
                itemHard.get(dbManager.PROJECTID),
                itemHard.get(dbManager.STATUS),
                itemHard.get(dbManager.CREATEDATE))


            val currentDate: Date = getCurrentDateTime()
            val projectDate: Date = item.createDate!!.toDate(timeFormat)

            val finalDate = currentDate.time - projectDate.time
            val seconds = finalDate / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val daysRemaining = hours / 24

            println("$TAG clearExpired: seconds: ${seconds} minutes: ${minutes} hours: ${hours} days: ${daysRemaining}")

            if (daysRemaining >= 5) {
                deleteProject(item)

                status = true
            }
        }

        callback(status)
    }


    /**
     * @param projectId : projectID si
     * @param callback : ArrayList<dataBaseItemModel>?
     */
    fun getProjectVideos(projectId:String, callback: (Boolean, ArrayList<dataBaseItemModel>?) -> Unit) {
        var callbackData: ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()
        myDbManager.getProjectVideos(projectId) { data ->
            for (i in 0 until data.size) {
                var myVideoFile = File(data[i].videoPath)
                var myThumbFile = File(data[i].thumb)
                if (this.flManager.checkFile(myVideoFile)) {
                    if (this.flManager.checkFile(myVideoFile)) {
                        callbackData.add(data[i])
                    }else{
                        this.videoDelete(data[i])
                    }
                }else {
                    this.videoDelete(data[i])
                }
            }


            callback(true,callbackData)
        }
    }

    /**
     * video item yazar sadece --
     * @param item => yazılacak model
     * @param callback => verinin durumu
     */
    fun insertVideoItem(item:dataBaseItemModel, callback:(Boolean) -> Unit) {

        val date = getCurrentDateTime()
        val dateInString = date.toString(timeFormat)

        myDbManager.insertItem(item) { status ->
            if (status) {
                val insertProjectData = dataBaseProjectModel(null, item.projectId, "true", dateInString)
                insertProject(insertProjectData) { status ->
                    if (status) {
                        callback(true)
                    }else {
                        callback(false)
                    }
                }
            }
        }

    }

    /**
     * video dosyası bulunur. id den ilk başta ismi bulunması (path i) gerekiyor...
     * @param id => video id si
     */
    fun videoDelete(item: dataBaseItemModel?) {
        item?.let {
            myDbManager.deleteItem(it.id)

            // video dosyasının silinmesi
            flManager.pathToFile(it.videoPath) { status, value ->
                if (status) {
                    flManager.deleteFile(value!!)
                    println("$TAG video DELETE FİLE")
                }
            }

            flManager.pathToFile(it.thumb) { status, value ->
                if (status) {
                    flManager.deleteFile(value!!)
                    println("$TAG bitmap DELETE FİLE")
                }
            }

            // item silindikten sonra eğer kendi gibi aynı projeye dahil item yoksa proje silinir...
            myDbManager.getAllItem (){ data: ArrayList<HashMap<String, String>> ->
                var removeLineCheck = true

                for (i in 0 until data.size) {
                    val checkData :String = data.get(i).get(dbManager.PROJECTID)!!
                    if (checkData == it.projectId){
                        removeLineCheck = false
                    }
                }

                // bu line silinmesi gerekiyor...
                if (removeLineCheck) {
                    val allProjectData = myDbManager.allProject
                    for (s in 0 until allProjectData.size) {
                        if (allProjectData[s].get(dbManager.PROJECTID) == it.projectId) {
                            val id:String = allProjectData[s].get(dbManager.ID) as String
                            val projectId:String = allProjectData[s].get(dbManager.PROJECTID) as String
                            val projectStatus:String = allProjectData[s].get(dbManager.STATUS) as String
                            val projectCreateDate:String = allProjectData[s].get(dbManager.CREATEDATE) as String
                            val checkData = dataBaseProjectModel(id,projectId,projectStatus, projectCreateDate)
                            deleteProject(checkData)
                        }
                    }
                }
            }
        }
    }

    /**
     * Projenin tabloya eklenmesi için daha önceden eklenmemiş olaması gerekiyor.
     * Proje database e daha önce eklenmiş ise callback false,
     * Proje database e yeni eklenmiş ise callback true döner
     * @param ->  dataBaseProjectModel?
     */
    fun insertProject(item:dataBaseProjectModel?, callback: (Boolean) -> Unit) {
        item?.let {
            myDbManager.insertProject(it) { status:Boolean ->
                if (status) {
                    println("$TAG proje eklendi")
                    callback(true)
                }else {
                    println("$TAG proje zaten vardı")
                    callback(false)
                }
            }
        }
    }

    /**
     * Proje id si gönderilir. Projeyi tablodan kaldırırken, projeye bağlı tabloda ki
     * satırları ve video dosyalarınıda kaldırır.
     * @param item -> silinecek proje dataBaseProjectModel
     */
    fun deleteProject(item: dataBaseProjectModel?) {
        item?.let {
            // proje kaldırılıyor
            myDbManager.deleteProject(item.id) { status->
                if (status) {
                    // proje kaldırıldıktan sonra projeye ait olan video item da kaldırılıyor.
                    myDbManager.getAllItem { data ->
                        for (i in 0 until data.size) {
                            val videoId:String = data.get(i).get(dbManager.ID) as String
                            val projectId:String = data.get(i).get(dbManager.PROJECTID) as String
                            val thumb:String = data.get(i).get(dbManager.THUMB) as String
                            val videoPath:String = data.get(i).get(dbManager.PATH) as String
                            val videoItem = dataBaseItemModel(videoId,videoPath,projectId,thumb)

                            if (projectId == item.projectId) {
                                videoDelete(videoItem)
                                // dosyanın silinmesi
                                flManager.pathToFile(videoItem.videoPath) { status, value ->
                                    if (status) {
                                        flManager.deleteFile(value!!)
                                        // dosya silindi...
                                    }else {
                                        // dosya yok
                                    }
                                }
                                flManager.pathToFile(videoItem.videoPath) { status, value ->
                                    if (status) {
                                        flManager.deleteFile(value!!)
                                        // dosya silindi...
                                    }else {
                                        // dosya yok
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Bütün videoları getirir
     */
    fun getAllVideos(callback: (Boolean, ArrayList<dataBaseItemModel>?) -> Unit) {
        myDbManager.getAllItem() { data: ArrayList<HashMap<String, String>>? ->
            println("$TAG getAllVideos: ${data!!.size}")
            data?.let {
                val myList: ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()

                for (i in 0 until data.size) {
                    val id:String = data.get(i).get(dbManager.ID) as String
                    val path:String = data.get(i).get(dbManager.PATH) as String
                    val projectId:String = data.get(i).get(dbManager.PROJECTID) as String
                    val thumb:String = data.get(i).get(dbManager.THUMB) as String
                    val model = dataBaseItemModel(id,path,projectId,thumb)
                    myList.add(model)
                }

                callback(true,myList)
                return@getAllItem
            }

            callback(false,null)
        }

    }

    /**
     * Bütün projeleri getirir.
     */
    fun getAllProject(callback: (Boolean, ArrayList<dataBaseProjectModel>?) -> Unit) {
        val data: ArrayList<HashMap<String, String>> = myDbManager.allProject
        val myList: ArrayList<dataBaseProjectModel> = ArrayList<dataBaseProjectModel>()
        for (i in 0 until data.size) {
            val id:String = data.get(i).get(dbManager.ID) as String
            val projectId:String = data.get(i).get(dbManager.PROJECTID) as String
            val status:String = data.get(i).get(dbManager.STATUS) as String
            val createDate:String = data.get(i).get(dbManager.CREATEDATE) as String
            val data:dataBaseProjectModel = dataBaseProjectModel(id,projectId,status,createDate)
            myList.add(data)
        }
        callback(true,myList)
    }

}