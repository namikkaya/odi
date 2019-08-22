package com.odi.beranet.beraodi.odiLib

class textFinder {
    val TAG:String = "textFinder: "
    public fun inspector(str:String, completion: (nativePage,String?,String?) -> Unit) {
        println(TAG + "odi parameter path: $str")

        val strArray = str.split("/")
        val first = strArray[0]

        if (strArray != null) {
            if (first == "design") {
                val secondArray = strArray[1].split("?")
                val second = secondArray[0]
                if (second == "prf.png") {
                    val userInfo = secondArray[1].split("=")
                    singleton.userId = userInfo[1]
                    val userID = singleton.userId
                    println("$TAG userID = $userID")
                    completion(nativePage.getPhotoAlbum,null,null)
                    return
                }else if (second == "updateprofil.png") { // kolaj
                    val userInfo = secondArray[1].split("=")
                    singleton.userId = userInfo[1]
                    val userID = singleton.userId
                    println("$TAG userID = $userID")
                    completion(nativePage.photoCollage,null,null)
                    return
                }else if (second == "odile.png") { // odileme video çekme kamera açılacak
                    val checkTanitimORShowReel = secondArray[1].split("-")

                    if (checkTanitimORShowReel[2] == "87") { // showReel
                        println(TAG + "odi parameter : showreel")
                    }else if (checkTanitimORShowReel[2] == "15") { // tanitim
                        println(TAG + "odi parameter : tanitim")
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



}