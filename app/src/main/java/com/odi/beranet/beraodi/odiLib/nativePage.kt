package com.odi.beranet.beraodi.odiLib

enum class nativePage {
    videoPlayer,
    photoCollage,
    getPhotoAlbum,
    uploadShowReel,
    uploadTanitim,
    cameraShowReel,
    uploadIdentification, // tanitim kartı yükleme
    cameraIdentification // tanitim kartı çekme
}

enum class Activity_Result (val value:Int) {
    CAMERA(1), //photo camera
    GALLERY(2), // photo gallery
    CROP(3), // crop
    PHOTO_COLLAGE(4),
    PICK_PHOTO_FOR_AVATAR_LEFT(5), // gallery sayfası kolaj fotoları
    PICK_PHOTO_FOR_AVATAR_RIGT_TOP(6), // gallery sayfası kolaj fotoları
    PICK_PHOTO_FOR_AVATAR_RIGT_BOT(7), // gallery sayfası kolaj fotoları
    PICK_VIDEO_FOR_UPLOAD_TANITIM(8), // video upload tanitim
    PICK_VIDEO_FOR_UPLOAD_SHOWREEL(9) // Video upload showreal
}

enum class Permission_Result(val value:Int) {
    GALLERY(2),
    CAMERA_CAPTURE(1),
    UPLOAD_VIDEO_GALLERY(3)

}

enum class HTTP_PROCESS_STATUS {
    start,
    fail,
    success
}

enum class SELECTED_CONTAINER {
    LEFT,
    RIGHT_TOP,
    RIGHT_BOTTOM
}
