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
import com.odi.beranet.beraodi.odiLib.*
import com.yalantis.ucrop.UCrop
import java.io.*
import java.lang.Exception
import java.util.*
import android.content.Intent
import android.content.pm.PackageManager
import com.odi.beranet.beraodi.MainActivityMVVM.videoUploadViewModel
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import com.odi.beranet.beraodi.Activities.*
import android.webkit.WebViewClient as WebViewClient1


class MainActivity : baseActivity(), OnWebViewClicked, odiInterface {



    private val TAG: String? = MainActivity::class.qualifiedName


    companion object {
        private const val IMAGE_DIRECTORY = "/odi"
    }

    // selected

    // -- object
    var webView:ClickableWebView? = null
    var myActionBar:ActionBar? = null
    //var imageView: ImageView? = null
    lateinit var myAnimationLayout:RelativeLayout

    // -- class
    var finder:textFinder? = null
    var photoController:photoViewModel? = null
    var videoUploadController:videoUploadViewModel? = null
    var myFileManager: odiFileManager? = null


    // -- Handler
    var uploadProfilePhotoMessageHandler:Handler? = null

    // bu değişkene yapılacak işlem tutulur tanitim veya showreel
    var processType:nativePage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        println("Takip mainActivity onCreate")
        configuration()
        onCheckFreeSpace()

    }

    private fun onCheckFreeSpace() {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable:Long = stat.blockSizeLong*stat.blockCountLong
        val mbAvaible = bytesAvailable / (1024*1024)
        println("$TAG boş alan: $mbAvaible")
        if (mbAvaible <= 1000) {
            infoDialog("Yetersiz Disk Alanı", "Cihazınızın hafızası dolmak üzere. Yetersiz hafıza uygulamanın çalışmasını engelleyebilir ve çökmelere sebep olabilir. Lütfen cihazınızda bulunan gereksiz görsel ve dosyaları silerek yer açın. Eğer sorun devam ederse cihazınızı kapatıp açtıktan sonra tekrar deneyin.")
        }
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
            println("$TAG oneSignalConfiguration internet")
            //oneSignalConfiguration()
        }
    }


    override fun onResume() {
        super.onResume()
        println("Takip mainactivity onResume")
    }

    override fun onPause() {
        super.onPause()
        println("Takip mainactivity onPause")

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

    var animationStatus:Boolean = false
    var animationFirstStart:Boolean = true

    // ui desing (web sayfası yükleniyor)
    private fun webViewConfiguration() {
        webView = findViewById(R.id.webView)
        myAnimationLayout = findViewById(R.id.myAnimationLayout)

        webView?.let {

            println("$TAG webview tanımlı")
            webView?.setOnWebViewClickListener(this)

            webView?.settings?.javaScriptEnabled = true
            webView?.settings?.allowFileAccess = true
            webView?.settings?.domStorageEnabled = true

            webView?.webViewClient = object : WebViewClient1(){
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    println("$TAG animation: onPageFinished")
                    if (animationFirstStart) {
                        animationFirstStart = false
                        if (animationStatus) {
                            animationStatus = false
                            println("$TAG animation: start")
                            val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.animation_load_page)
                            anim.setAnimationListener(object : Animation.AnimationListener {
                                override fun onAnimationStart(animation: Animation) {

                                }

                                override fun onAnimationEnd(animation: Animation) {
                                    myAnimationLayout.setVisibility(View.GONE)
                                }

                                override fun onAnimationRepeat(animation: Animation) {

                                }
                            })

                            myAnimationLayout.startAnimation(anim)
                        }
                    }


                }

            }

            // her click ten sonra finder a sorulur nereye gidileceği finder dan gelen dönüşe göre karar verilir.
            webView?.webChromeClient = object : WebChromeClient() {

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage.let {
                        val message: String? = consoleMessage?.message()
                        println("$TAG clickParameter: ** $message")
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
                animationStatus = true
                webViewOnLoad(userId)
                singleton.onesignal_playerId = userId
                singleton.onesignal_registrationId = registrationId
                println("$TAG oneSignalConfiguration: Notification için id eklendi")
            }else {
                // timer kurulacak...
                println("$TAG oneSignalConfiguration: HATA player id alınamadı...")
                animationStatus = true
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
                val PathUri = Uri.parse(sendId)
                val intent = Intent(Intent.ACTION_VIEW, PathUri)
                intent.setDataAndType(PathUri, "video/*")
                startActivity(intent)
            }

            nativePage.uploadTanitim -> {
                println(TAG + "upload Tanitim")
                videoUploadController?.check_writeRead_permission { status->
                    if (status == true) {
                        processType = nativePage.uploadTanitim
                        openSelectVideo()
                    }
                }
            }

            nativePage.uploadShowReel -> { // SHOWREEL upload için
                // izin kontrollleri
                videoUploadController?.check_writeRead_permission { status->
                    if (status == true) {
                        processType = nativePage.uploadShowReel
                        openSelectVideo()
                    }
                }

            }

            nativePage.cameraShowReel -> {
                println("$TAG cameraStatus: showReel")
                val intent = Intent(this, cameraActivity::class.java)
                intent.putExtra("userId", buttonId)
                intent.putExtra("projectId", sendId)
                intent.putExtra("type", nativePage.cameraShowReel)
                startActivityForResult(intent, Activity_Result.CAMERA_SHOW_REEL_RESULT.value)
            }

            nativePage.cameraIdentification -> {
                println("$TAG cameraStatus: Tanitim")
                val intent = Intent(this, cameraActivity::class.java)
                intent.putExtra("userId", buttonId)
                intent.putExtra("projectId", sendId)
                intent.putExtra("type", nativePage.cameraIdentification)
                startActivityForResult(intent, Activity_Result.CAMERA_TANITIM_RESULT.value)
            }

            nativePage.cameraOdile -> {
                println("$TAG cameraStatus: Camera Status")
                val intent = Intent(this, cameraActivity::class.java)
                intent.putExtra("userId", sendId)
                intent.putExtra("projectId", buttonId)
                intent.putExtra("type", nativePage.cameraOdile)
                startActivityForResult(intent, Activity_Result.CAMERA_ODILE_RESULT.value)
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
            println("Takip: webViewOnload reload1")
        }else {
            if (singleton.onesignal_playerId != null) {
                webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=" + singleton.onesignal_playerId)
                webView?.reload()
                println("Takip: webViewOnload reload2")
            }else {
                webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=")
                webView?.reload()
                println("Takip: webViewOnload reload3")
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // webView Click Listener
    override fun onClick(url: String?) {
        if (finder != null && url != null) {
            finder?.inspectorOnClick(url!!) { nativePage, sendId, buttonId ->
                println("$TAG inspectorOnClick: np: $nativePage")
                println("$TAG inspectorOnClick: userId: $sendId")
                println("$TAG inspectorOnClick: projectId: $buttonId")
                nativePageDecider(nativePage,sendId,buttonId)
            }
        }
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
        println("takibim: data $data")
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
            // huvai telefonda crop özelliğinden dolayı image alınamadı
            handleCropResult(data!!)

        }

        if (Activity_Result.PHOTO_COLLAGE.value == requestCode) {
            webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=" + singleton.onesignal_playerId)
            webView?.reload()
            println("Takip: PHOTO_COLLAGE reload4")
        }

        if (resultCode == Activity.RESULT_OK &&
            requestCode == Activity_Result.PICK_VIDEO_FOR_UPLOAD_SHOWREEL.value) {

            val selectedImageUri:Uri = data!!.data

            val intent = Intent(this, upload_from_gallery::class.java)
            intent.putExtra("selectedPath", selectedImageUri.toString())
            println("yükleme: mainActivity $processType")
            intent.putExtra("processType", processType)
            startActivityForResult(intent, Activity_Result.UPLOAD_VIDEO_PAGE_RESULT.value)
        }

        if(requestCode == Activity_Result.UPLOAD_VIDEO_PAGE_RESULT.value && resultCode == Activity.RESULT_OK){
            if (data?.extras != null) {
                val status = data?.extras.getString("STATUS")
                if (status == "OKEY") {
                    Toast.makeText(applicationContext, "İşlem Başarılı.", Toast.LENGTH_SHORT).show()
                    webView?.loadUrl("http://odi.odiapp.com.tr/?kulID=" + singleton.onesignal_playerId)
                    webView?.reload()
                    println("Takip: UPLOAD_VIDEO_PAGE_RESULT reload5")
                }
            }
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
            Toast.makeText(this, "Resim düzenlenirken bir hata oluştu. kod:12", Toast.LENGTH_SHORT).show()
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
                    println("Takip: showUploadProfilePhotoAlert reload6")
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

    override fun onBackPressed() {
        return
    }




}
