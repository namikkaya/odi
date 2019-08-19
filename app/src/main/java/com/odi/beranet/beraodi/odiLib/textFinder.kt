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
                    completion(nativePage.getPhotoAlbum,null,null)
                    println( "odi parameter 1")
                    return
                }else if (second == "updateprofil.png") { // kolaj
                    println("odi parameter 2")

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
                /*
                val secondArray = strArray[1].split("?")
                val second = secondArray[0]
                if (second == "prf.png"){
                    completion(nativePage.getPhotoAlbum,null,null) // profil photo
                }else {
                    completion(nativePage.photoCollage,null,null) // foto collage
                }

                val userIDArray = secondArray[1].split("=")
                val myUserID = userIDArray[1]
                singleton.userId = myUserID
                println("$TAG myUserID: $myUserID add singleton")
                return*/

            }
        }


        println("odi parameter 6")

        println("Showreel: string veri: $str")
        val uploadString = str.split("=")
        // showreel veya tanitim ise UPLOAD
        if (uploadString[0] == "ek") {
            val ds = uploadString[1].split("_")
            println("Showreel: odi parameter ek: " + ds[1])
            singleton.userId = ds[1]
            if (ds[0] == "showreel") { // showreel upload
                completion(nativePage.uploadShowReel, str, ds[1])
            }else{ // tanitim upload
                completion(nativePage.uploadTanitim, str, ds[1])
            }

        }

    }



}