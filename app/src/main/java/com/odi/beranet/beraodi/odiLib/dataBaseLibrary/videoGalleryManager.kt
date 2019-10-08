package com.odi.beranet.beraodi.odiLib.dataBaseLibrary

import android.content.Context
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.models.dataBaseProjectModel
import java.util.ArrayList

class videoGalleryManager () {
    companion object {
        private val TAG: String = "videoGalleryManager Singleton:"

        /**
         * Zamanı geçmiş video ve projeleri kaldırır.
         * @param applicationContext => applicationContext
         * @param callback => true / false
         */
        fun clearExpired(applicationContext: Context, callback: (Boolean) -> Unit) {
            var myManager: gallleryManager? = gallleryManager(applicationContext)
            myManager?.clearExpired { status: Boolean ->
                if (status) {
                    println("Bazı proje ve projeye bağlı videolar kaldırıldı")

                } else {
                    println("Kaldırılacak video yok")
                }
                callback(status)
                myManager = null
            }
        }

        fun deleteVideoItem(applicationContext: Context, itemModel: dataBaseItemModel, callback: (Boolean) -> Unit) {
            var myManager: gallleryManager? = gallleryManager(applicationContext)
            myManager?.videoDelete(itemModel)
            callback(true)
        }


        /**
         * Video ekler ve eğer bu video olmayan bir projeye ait ise buna ek olarak proje de oluşturur.
         * @param applicationContext
         * @param item : dataBaseItemModel
         * @param callback : true / false
         */
        fun insertVideoItem(applicationContext: Context, item: dataBaseItemModel, callback: (Boolean) -> Unit) {
            var myManager: gallleryManager? = gallleryManager(applicationContext)
            myManager?.insertVideoItem(item) { status ->
                if (status) {
                    println("$TAG video eklendi. Yeni bir proje oluşturuldu.")
                } else {
                    println("$TAG video eklendi. Var olan projeye ekledi.")
                }
                callback(status)
                myManager = null
            }
        }

        /**
         * projeye ait videoları getirir
         * @param applicationContext
         * @param id : projectID
         * @param callback : (true /false, ArrayList<dataBaseItemModel>?)
         */
        fun getProjectVideos(
            applicationContext: Context,
            id: String,
            callback: (Boolean, ArrayList<dataBaseItemModel>?) -> Unit
        ) {
            var myManager: gallleryManager? = gallleryManager(applicationContext)
            var videoGalleryData: ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()
            myManager?.getProjectVideos(id) { status, data ->
                if (status) {
                    data?.let {
                        println("$TAG selected => ProjectVideos video sayısı: ${data.size}")
                        for (i in 0 until data.size) {
                            //println("$TAG video id: ${item.id} - projectId: ${item.projectId} - path: ${item.videoPath}")
                            videoGalleryData.add(data[i])
                        }
                    }
                    callback(true, videoGalleryData)
                } else {
                    callback(false, videoGalleryData)
                }

            }
        }

        /**
         * bütün projeleri çeker
         * @param applicationContext:
         * @param callback: (true/false, ArrayList<dataBaseProjectModel>?)
         */
        fun getAllProject(applicationContext: Context, callback: (Boolean, ArrayList<dataBaseProjectModel>?) -> Unit) {
            var myManager: gallleryManager? = gallleryManager(applicationContext)
            myManager?.getAllProject { status, data ->
                if (status) {
                    data?.let {
                        callback(true, data)
                    }
                } else {
                    callback(false, data)
                }

            }
        }

        /**
         * Bütün videoları çeker
         * @param applicationContext:
         * @param callback: (true/false, ArrayList<dataBaseItemModel>?)
         */
        fun getAllVideos(applicationContext: Context, callback: (Boolean, ArrayList<dataBaseItemModel>?) -> Unit) {
            var myManager: gallleryManager? = gallleryManager(applicationContext)
            myManager?.getAllVideos { status: Boolean, data: ArrayList<dataBaseItemModel>? ->
                if (status) {
                    data?.let {
                        callback(true, data)
                    }
                } else {
                    callback(false, data)
                }

            }
        }

    }

}