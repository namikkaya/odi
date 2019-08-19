package com.odi.beranet.beraodi

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.ActionBar
import android.webkit.*
import com.ahmadnemati.clickablewebview.ClickableWebView
import com.ahmadnemati.clickablewebview.listener.OnWebViewClicked
import com.odi.beranet.beraodi.MainActivityMVVM.photoViewModel
import com.onesignal.OneSignal
import android.widget.Toast
import android.media.MediaScannerConnection
import android.os.*
import android.provider.MediaStore
import android.support.annotation.NonNull
import com.odi.beranet.beraodi.Activities.baseActivity
import com.odi.beranet.beraodi.Activities.galeryActivity
import com.odi.beranet.beraodi.Activities.warningActivity
import com.odi.beranet.beraodi.odiLib.*
import com.yalantis.ucrop.UCrop
import java.io.*
import java.lang.Exception
import java.util.*
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.odi.beranet.beraodi.Activities.upload_from_gallery
import com.odi.beranet.beraodi.MainActivityMVVM.videoUploadViewModel


class MainActivity : baseActivity(), OnWebViewClicked, odiInterface {


    private val TAG:String = "-MainActivity: "


    companion object {
        private const val IMAGE_DIRECTORY = "/odi"
    }


    // -- object
    var webView:ClickableWebView? = null
    var myActionBar:ActionBar? = null
    //var imageView: ImageView? = null

    // -- class
    var finder:textFinder? = null
    var photoController:photoViewModel? = null
    var videoUploadController:videoUploadViewModel? = null
    var myFileManager: odiFileManager? = null


    // -- Handler
    var uploadProfilePhotoMessageHandler:Handler? = null

    // bu değişkene yapılacak işlem tutulur
    var processType:nativePage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configuration()
    }

    var warningIntent:Intent? = null
    override fun internetConnectionStatus(status: Boolean) {
        warningIntent = Intent(this, warningActivity::class.java)
        warningIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        warningIntent?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        warningIntent?.putExtra("warningTitle", "Bağlantı Sorunu")
        warningIntent?.putExtra("warningDescription", "İnternet bağlantınızda problem var. Lütfen bağlantınızı kontrol edip tekrar deneyin.")
        if (!status) {
            startActivity(warningIntent)
        }else {
            // internet geldiğinde tekrar ettir.
            oneSignalConfiguration()
        }
    }


    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }


    private fun configuration() {
        managersConfiguration()
        navigationBarConfiguration()
        webViewConfiguration()
        oneSignalConfiguration()
        // internetConnectionStatus --> config devamı

        //asyncUploadFile().execute("naber")
        val myUserId = singleton.userId
        println("userId: $myUserId ")
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // MANAGERS
    private fun managersConfiguration() {
        finder = textFinder()
        photoController = photoViewModel(this)
        videoUploadController = videoUploadViewModel(this,this)
        myFileManager = odiFileManager()
    }

    // mainActivity için navigasyon barı
    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    // ui desing (web sayfası yükleniyor)
    private fun webViewConfiguration() {
        webView = findViewById(R.id.webView)

        webView?.let {

            println("$TAG webview tanımlı")
            webView?.setOnWebViewClickListener(this)

            webView?.settings?.javaScriptEnabled = true
            webView?.settings?.allowFileAccess = true
            webView?.settings?.domStorageEnabled = true

            webView?.webViewClient = object : WebViewClient(){

            }

            // her click ten sonra finder a sorulur nereye gidileceği finder dan gelen dönüşe göre karar verilir.
            webView?.webChromeClient = object : WebChromeClient() {

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage.let {
                        val message: String? = consoleMessage?.message()
                        if (message != null) {
                            finder?.inspector(message) { nativePage, sendId, buttonId ->
                                println("odi parameters: sendId: $sendId buttonId $buttonId")
                                nativePageDecider(nativePage,sendId,buttonId)
                            }
                        }
                    }
                    return true
                }
            }
        }

    }


    // onesignal configuration--
    private fun oneSignalConfiguration() {
        println("$TAG oneSignalConfiguration: start")
        OneSignal.idsAvailable { userId, registrationId ->
            if (registrationId != null) {
                System.out.println("$TAG onesignal userId: $userId")
                webViewOnLoad(userId)
                singleton.onesignal_playerId = userId
                singleton.onesignal_registrationId = registrationId
                println("$TAG oneSignalConfiguration: Notification için id eklendi")
            }else {
                // timer kurulacak...
                println("$TAG oneSignalConfiguration: HATA player id alınamadı...")
                webViewOnLoad(null)
            }
        }
    }
    //-------------------------------------------------------------------
    //-------------------------------------------------------------------

    // HAREKET
    fun nativePageDecider(page:nativePage, sendId:String?, buttonId:String?){
        when(page){

            nativePage.getPhotoAlbum -> {
                println(TAG + "fotoğraf albümü aç")
                if (photoController != null) {
                    photoController?.showPictureDialog()
                }
            }

            nativePage.photoCollage -> {
                val intent = Intent(this, galeryActivity::class.java)
                startActivityForResult(intent, Activity_Result.PHOTO_COLLAGE.value)
            }

            nativePage.videoPlayer -> {
                println(TAG + "videoPlayer aç")
            }

            nativePage.uploadTanitim -> {
                println(TAG + "upload Tanitim")
            }

            nativePage.uploadShowReel -> { // SHOWREEL
                // izin kontrollleri
                videoUploadController?.check_writeRead_permission { status->
                    if (status == true) {
                        processType = nativePage.uploadShowReel
                        openSelectVideo()
                    }
                }

            }

        }

    }

    fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {

            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else
            return null
    }

    private fun webViewOnLoad(userId:String?) {
        println("$TAG $userId sayfanın yüklenmesi gerekiyor")
        if (userId != null) {
            webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=$userId")
            webView?.reload()
            println("$TAG webViewOnLoad : $userId kullanıcının id si server a gönderildi")
        }else {
            if (singleton.onesignal_playerId != null) {
                webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=" + singleton.onesignal_playerId)
                webView?.reload()
            }else {
                webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=")
                webView?.reload()
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // webView Click Listener
    override fun onClick(url: String?) {
        System.out.println("$TAG onClick: url: $url")
    }
    //-------------------------------------------------------------------


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permission_Result.UPLOAD_VIDEO_GALLERY.value == requestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                openSelectVideo()
            }else {
                setResult(RESULT_OK)
                println("$TAG izin verilmedi okuma yazma")
                // alert buraya yazılacak
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // PHOTO GALLERY
        if (requestCode == Activity_Result.GALLERY.value && resultCode == Activity.RESULT_OK) {
            println(TAG + "onActivityResult galeri dönüşü")
            if (data != null) {
                val contentURI = data!!.data
                val destinationFileName = "SAMPLE_CROPPED_IMAGE_NAME"+".jpg"

                val cropper = UCrop.of(contentURI, Uri.fromFile(File(cacheDir, destinationFileName)))
                cropper.withAspectRatio(1F, 1F)
                cropper.start(this)
            }
        }
        // CAMERA
        else if (requestCode == Activity_Result.CAMERA.value && resultCode == Activity.RESULT_OK) {
            val destinationFileName = "SAMPLE_CROPPED_IMAGE_NAME"+".jpg"
            val cropper = UCrop.of(photoController!!.picUri!!, Uri.fromFile(File(cacheDir, destinationFileName)))
            cropper.withAspectRatio(1F, 1F)
            cropper.start(this)
        }
        // CROP
        else if (requestCode == UCrop.REQUEST_CROP) {
            handleCropResult(data!!)
        }


        if (Activity_Result.PHOTO_COLLAGE.value == requestCode) {
            println("$TAG resultGalleryActivity: RELOADED")
            webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=" + singleton.onesignal_playerId)
            webView?.reload()
        }

        if (resultCode == Activity.RESULT_OK &&
            requestCode == Activity_Result.PICK_VIDEO_FOR_UPLOAD_SHOWREEL.value) {

            val selectedImageUri:Uri = data!!.data

            val filemanagerstring = selectedImageUri.path


            val selectedImagePath = getPath(selectedImageUri)
            if (selectedImagePath != null) {
                println("$TAG video path: $selectedImagePath") // play etmek için

            }

            /*
            //getImageUrlWithAuthority(this,selectedImageUri)
            if (videoUploadController != null) {
                videoUploadController?.getImageUrlWithAuthority(this, selectedImageUri, processType!!)
            }
            */

            val intent = Intent(this, upload_from_gallery::class.java)
            intent.putExtra("selectedPath", selectedImageUri.toString())
            intent.putExtra("processType", processType)
            startActivityForResult(intent, Activity_Result.PICK_VIDEO_FOR_UPLOAD_SHOWREEL.value)

        }

        if (resultCode == Activity.RESULT_OK) {
            webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=" + singleton.onesignal_playerId)
            webView?.reload()
        }
    }

    // ucrop event dönüşü
    private fun handleCropResult(@NonNull result:Intent) {
        val resultUri: Uri? = UCrop.getOutput(result)
        if (resultUri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
            val path = saveImage(bitmap)
            var myFile = File(path)
            sendProfilePhoto(myFile)

        } else {
            Toast.makeText(this, "Resim düzenlenirken bir hata oluştu.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getImageUri(context: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    /**
     * uri resmi bitmap olarak kulanır.
     * */
    private fun grabImage(myUri:Uri):Bitmap? {
        println(TAG + " grabImage: " + myUri)
        this.contentResolver.notifyChange(myUri,null)
        val cr:ContentResolver = this.contentResolver

        var bitmap:Bitmap? = null

        try {
            bitmap = MediaStore.Images.Media.getBitmap(cr, myUri)
        }catch (e:Exception) {
            Toast.makeText(this, "Resim alınamadı", Toast.LENGTH_SHORT).show()
        }

        return bitmap
    }

    // save library image
    fun saveImage(myBitmap: Bitmap):String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val wallpaperDirectory = File((Environment.getExternalStorageDirectory()).toString() + IMAGE_DIRECTORY)
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }

        try
        {
            println("$TAG image gallery Saving")
            val f = File(wallpaperDirectory, ((Calendar.getInstance()
                .timeInMillis).toString() + ".jpg"))
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(this, arrayOf(f.getPath()), arrayOf("image/jpeg"), null)
            fo.close()

            println(TAG + " image gallery saved")

            return f.getAbsolutePath()
        }
        catch (e1: IOException) {
            e1.printStackTrace()
        }

        return ""
    }

    private fun sendProfilePhoto(oldFile:File) {
        val userUserId:String = singleton.userId!!
        val file = myFileManager?.renameFile(oldFile, "profilImage_$userUserId", ".jpg")

        Thread(Runnable {
            val multipart = MultipartUtility(singleton.FILE_UPLOAD_URL, "UTF-8")
            multipart.delegate = this
            multipart.addFilePart("image", file)
            multipart.finish()
        }).start()

        uploadProfilePhotoMessageHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                val msg: String? = message.obj as? String
                msg.let { value ->
                    if (value.equals("suc")) { // başarılı
                        showUploadProfilePhotoAlert()
                    }else if (value.equals("start")) { // işleme başlandı
                        Toast.makeText(this@MainActivity, "Profil fotoğrafı işleme alındı. Yükleniyor...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // Delegate --- upload
    /**
     * MultiPartUtility den tetiklenir.
     */
    override fun Interface_profilPhotoUploadStatus(status: HTTP_PROCESS_STATUS) {
        if (HTTP_PROCESS_STATUS.success == status) {
            workerThread("suc")
        }else if (HTTP_PROCESS_STATUS.start == status) {
            workerThread("start")
        }
    }

    //-------------------------------------------------------------------

    // Interface_profilPhotoUploadStatus => trigger
    fun workerThread(status: String) {
        var message:Message = uploadProfilePhotoMessageHandler!!.obtainMessage(1,status)
        message.sendToTarget()
    }


    private fun showUploadProfilePhotoAlert() {
        try {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setMessage("Profil Resminiz Kaydedildi.").setTitle("Odi")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    val intent = Intent()
                    intent.putExtra("PLAY_SHOW", "done")
                    setResult(Activity.RESULT_FIRST_USER, intent)
                    webView?.reload()
                }
            val alert = builder.create()
            alert.show()
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_LONG).show()
            return
        }
    }


    private fun openSelectVideo() {
        val videoIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        videoIntent.type = "video/*"
        startActivityForResult(Intent.createChooser(videoIntent, "Video Seç"), Activity_Result.PICK_VIDEO_FOR_UPLOAD_SHOWREEL.value)

    }







}
