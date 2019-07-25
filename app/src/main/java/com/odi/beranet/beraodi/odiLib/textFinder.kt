package com.odi.beranet.beraodi.odiLib

class textFinder {
    val TAG:String = "textFinder: "
    public fun inspector(str:String, completion: (nativePage) -> Unit) {
        println(TAG + "path0 = $str")

        val strArray = str.split("/")
        val first = strArray[0]

        if (first.equals("design")) {

            val secondArray = strArray[1].split("?")
            val second = secondArray[0]

            if (second.equals("prf.png")){
                completion(nativePage.getPhotoAlbum) // profil photo
            }else {
                completion(nativePage.photoCollage) // foto collage
            }

            val userIDArray = secondArray[1].split("=")
            val myUserID = userIDArray[1]
            singleton.userId = myUserID
            println("$TAG myUserID: $myUserID add singleton")

        }else{
            completion(nativePage.videoPlayer)
        }
    }
}