package com.odi.beranet.beraodi.odiLib

import com.crashlytics.android.Crashlytics

class textFinder {
    val TAG:String = "textFinder: "
    public fun inspector(str:String, completion: (nativePage,String?,String?) -> Unit) {
        println(TAG + "clickParameter: $str")

        val strArray = str.split("/")
        val first = strArray[0]

        if (strArray != null) {
            if (first == "design") {
                val secondArray = strArray[1].split("?")
                val second = secondArray[0]
                if (second == "prf.png") {
                    val userInfo = secondArray[1].split("=")
                    singleton.userId = userInfo[1]
                    Crashlytics.setUserIdentifier(singleton.userId)

                    val userID = singleton.userId
                    println("$TAG userID = $userID")
                    completion(nativePage.getPhotoAlbum,null,null)
                    return
                }else if (second == "updateprofil.png") { // kolaj
                    val userInfo = secondArray[1].split("=")
                    singleton.userId = userInfo[1]
                    Crashlytics.setUserIdentifier(singleton.userId)

                    val userID = singleton.userId
                    println("$TAG userID = $userID")
                    completion(nativePage.photoCollage,null,null)
                    return
                }else if (second == "odile.png") { // odileme video çekme kamera açılacak

                    val checkTanitimORShowReel = secondArray[1].split("-")
                    println("$TAG checkTanitimORShowReel $checkTanitimORShowReel")
                    if (checkTanitimORShowReel[2] == "87") { // showReel
                        val id = checkTanitimORShowReel[1]
                        singleton.userId = id
                        Crashlytics.setUserIdentifier(singleton.userId)
                        completion(nativePage.cameraShowReel, checkTanitimORShowReel[2], id) // çekim yapılan 87 / userid
                    }else if (checkTanitimORShowReel[2] == "15") { // tanitim
                        val id = checkTanitimORShowReel[1]
                        singleton.userId = id
                        Crashlytics.setUserIdentifier(singleton.userId)
                        completion(nativePage.cameraIdentification, checkTanitimORShowReel[2], id) // çekim yapılan 87 / userid
                    }
                    println("odi parameter 3")
                    return
                }

            }
        }

        val uploadString = str.split("=")
        if (uploadString[0] == "ek") {
            val ds = uploadString[1].split("_")
            singleton.userId = ds[1]
            Crashlytics.setUserIdentifier(singleton.userId)
            if (ds[0] == "showreel") { // showreel upload
                completion(nativePage.uploadShowReel, str, ds[1])
            }else{ // tanitim upload
                completion(nativePage.uploadTanitim, str, ds[1])
            }
        }

        val videoPlayer = str.split("=")
        if (videoPlayer[0] == "videoPlayer") {
            println("$TAG video Player OKEY")
            completion(nativePage.videoPlayer, videoPlayer[1],null)
        }
    }


    public fun inspectorOnClick(str:String, completion: (nativePage,String?,String?) -> Unit) {
        println(TAG + "clickParameter: $str")

        val strArray = str.split("/")
        if (strArray[4] != null) {
            val checkArray = strArray[4].split("-")
            if (checkArray[0] != null) {
                if ("odile.png?" == checkArray[0]) {
                    val userId:String = checkArray[1]
                    val projectId:String = checkArray[2]
                    singleton.userId = userId
                    Crashlytics.setUserIdentifier(singleton.userId)
                    completion(nativePage.cameraOdile,userId,projectId)
                }
            }
        }

        println("$TAG inspectorOnClick => $strArray")


    }

}