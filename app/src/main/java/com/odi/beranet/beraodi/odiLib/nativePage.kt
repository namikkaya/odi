package com.odi.beranet.beraodi.odiLib

enum class nativePage {
    videoPlayer,
    photoCollage,
    getPhotoAlbum,
    uploadShowReel,
    cameraShowReel,
    uploadIdentification, // tanitim kartı yükleme
    cameraIdentification // tanitim kartı çekme
}

enum class Activity_Result (val value:Int) {
    CAMERA(1),
    GALLERY(2),
    CROP(3)
}

enum class Permission_Result(val value:Int) {
    GALLERY(2),
    CAMERA_CAPTURE(1)
}

enum class HTTP_PROCESS_STATUS {
    start,
    fail,
    success
}