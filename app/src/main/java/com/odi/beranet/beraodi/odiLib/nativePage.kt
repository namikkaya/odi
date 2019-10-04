package com.odi.beranet.beraodi.odiLib

import java.io.Serializable

enum class nativePage:Serializable {
    videoPlayer,
    photoCollage,
    getPhotoAlbum,
    uploadShowReel,
    uploadTanitim,
    cameraShowReel,
    cameraOdile,
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
    PICK_VIDEO_FOR_UPLOAD_SHOWREEL(9), // Video upload showreal
    PRELOADER_FINISH(10),
    UPLOAD_VIDEO_PAGE_RESULT(11),
    CAMERA_SHOW_REEL_RESULT(12),
    CAMERA_TANITIM_RESULT(13),
    CAMERA_ODILE_RESULT(14),
    PREVIEW_VIDEO_RESULT(15),
    VIDEO_GALLERY_PAGE(16)
}

enum class Permission_Result(val value:Int) {
    GALLERY(2),
    CAMERA_CAPTURE(1),
    UPLOAD_VIDEO_GALLERY(3),
    CAMERA_PERMISSION(4)

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

enum class RECORD_TYPE {
    MONOLOG,
    DIALOG,
    PLAYMODE
}


/***
 * Yükleme yapılan durumun image mı yoksa video mu olduğu
 */
enum class UPLOAD_FILE_TYPE {
    bitmap,
    video
}

/***
 * Yükleme sayfasında karşıya yapılan yüklemenin preloaderının durumlarını haber verir
 */
enum class UI_PRELOADER {
    bitmap,
    video,
    compress,
    complete
}


/**
 * video izleme sayfasına gönderirken hangi durumdan gelindiğini haber verir
 */
enum class VIDEO_PREVIEW_STATUS {
    SAVED,
    RECORDING
}
