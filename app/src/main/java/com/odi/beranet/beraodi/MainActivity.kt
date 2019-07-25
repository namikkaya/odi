package com.odi.beranet.beraodi

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.view.View
import android.webkit.*
import com.ahmadnemati.clickablewebview.ClickableWebView
import com.ahmadnemati.clickablewebview.listener.OnWebViewClicked
import com.odi.beranet.beraodi.Activities.galeryActivity
import com.odi.beranet.beraodi.MainActivityMVVM.photoViewModel
import com.onesignal.OneSignal
import com.onesignal.SyncService
import android.widget.Toast
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.*
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.ImageView
import com.odi.beranet.beraodi.odiLib.*
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), OnWebViewClicked, odiInterface {


    val TAG:String = "-MainActivity: "

    companion object {
        private val IMAGE_DIRECTORY = "/odi"
    }


    // -- object
    var webView:ClickableWebView? = null
    var myActionBar:ActionBar? = null
    var imageView: ImageView? = null

    // -- class
    var finder:textFinder? = null
    var photoController:photoViewModel? = null
    var myFileManager: odiFileManager? = null

    // -- Handler
    var uploadProfilePhotoMessageHandler:Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        configuration()
    }

    private fun configuration() {
        managersConfiguration()
        navigationBarConfiguration()
        webViewConfiguration()
        oneSignalConfiguration()
    }

    private fun managersConfiguration() {
        finder = textFinder()
        photoController = photoViewModel(this)
        myFileManager = odiFileManager()
    }

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
            webView?.setOnWebViewClickListener(this)

            webView?.settings?.javaScriptEnabled = true
            webView?.settings?.allowFileAccess = true
            webView?.settings?.domStorageEnabled = true



            webView?.webViewClient = object : WebViewClient(){

            }



            webView?.webChromeClient = object : WebChromeClient() {


                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage.let {
                        val message: String? = consoleMessage?.message()
                        if (message != null) {
                            finder?.inspector(message, {
                                nativePageDecider(it)
                            })
                        }

                    }

                    return true //super.onConsoleMessage(consoleMessage)

                }
            }
        }

    }

    private fun webViewOnLoad(userId:String?) {
        userId?.let {
            //System.out.println(TAG + " webViewOnLoad: http://odi.odiapp.com.tr")
            webView?.loadUrl("http://odi.odiapp.com.tr")
        }

    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // webView Click Listener
    override fun onClick(url: String?) {
        System.out.println(TAG + " onClick: url: $url")
    }
    //-------------------------------------------------------------------


    // onesignal configuration--
    private fun oneSignalConfiguration() {
        OneSignal.idsAvailable { userId, registrationId ->
            if (registrationId != null) {
                System.out.println(TAG + " onesignal userId: $userId")
                singleton.onesignal_playerId = userId
                singleton.onesignal_registrationId = registrationId
                webViewOnLoad(userId)
            }else {
                // timer kurulacak...
                System.out.println(TAG + " oneSignalConfiguration: HATA player id alınamadı...")
                webViewOnLoad(null)
            }
        }
    }

    // HAREKET
    fun nativePageDecider(page:nativePage){
        when(page){

            nativePage.getPhotoAlbum -> {
                println(TAG + "fotoğraf albümü aç")

                if (photoController != null) {
                    photoController?.showPictureDialog()
                }

            }

            nativePage.photoCollage -> {
                println(TAG + "fotoğraf kolajı aç")
            }

            nativePage.videoPlayer -> {
                println(TAG + "videoPlayer aç")
            }

        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        println("$TAG resultCode: $resultCode")

        // PHOTO GALLERY
        if (requestCode == Activity_Result.GALLERY.value && resultCode == Activity.RESULT_OK) { // galeri
            println(TAG + "onActivityResult galeri dönüşü")
            if (data != null)
            {

                val contentURI = data!!.data
                try
                {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                    val path = saveImage(bitmap)


                    var myFile = File(path)
                    sendProfilePhoto(myFile)

                    Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
                    imageView!!.setImageBitmap(bitmap)

                }
                catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
                }

            }

        }
        // CAMERA
        else if (requestCode == Activity_Result.CAMERA.value && resultCode == Activity.RESULT_OK) { // camera


            val myBitmap = grabImage(photoController?.picUri!!) // image view basar
            imageView?.setImageBitmap(myBitmap)

            sendProfilePhoto(photoController!!.imageFile!!)

            //cropIntent()



            //performCrop(uri)

        }
        // CROP
        else if (requestCode == Activity_Result.CROP.value && resultCode == Activity.RESULT_OK) {
            println(TAG + " cropUri: " + cropUri)

            /*
            if (data != null) {

                if (data.extras != null) {
                    println(TAG + " image ataması yapılması gerekiyor...")
                    var bundle:Bundle = data.extras
                    var bitmap:Bitmap = bundle.getParcelable("data")
                    imageView?.setImageBitmap(bitmap)
                }

            }
            */


        }
    }

    var cropUri:Uri? = null
    private fun performCrop(picUri:Uri) {
        println(TAG + " performCrop: $picUri")
        try {
            val cropIntent = Intent("com.android.camera.action.CROP")
            cropIntent.setDataAndType(picUri, "image/*")
            cropIntent.putExtra("crop", "true")
            cropIntent.putExtra("aspectX", 2)
            cropIntent.putExtra("aspectY", 2)
            cropIntent.putExtra("outputX", 256)
            cropIntent.putExtra("outputY", 256)
            cropIntent.putExtra("return-data", true)

            //ile = new File(Environment.getExternalStorageDirectory(), "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

            val imageFilePath = Environment.getExternalStorageDirectory().absolutePath + "/temp_crop2.jpg"
            val myFile = File(imageFilePath)
            cropUri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", myFile)

            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri)
            cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())

            startActivityForResult(cropIntent, Activity_Result.CROP.value)
        } catch (anfe: ActivityNotFoundException) {
            println(TAG + " performCrop Error: " + anfe.message)
            val toast = Toast.makeText(this, "Bu cihaz fotoğraf editlemeyi desteklemiyor.", Toast.LENGTH_LONG)
            toast.show()
        }
    }


    private fun cropIntent() {
        var photoPickerIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        photoPickerIntent.setType("image/*")
        photoPickerIntent.putExtra("crop", "true")

        photoPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri)
        photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
        startActivityForResult(photoPickerIntent, Activity_Result.CROP.value)
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

    private fun convertBitmap(imgUri:Uri) {
        var options:BitmapFactory.Options = BitmapFactory.Options();
        options.inSampleSize = 4

        var bitmap:Bitmap = BitmapFactory.decodeFile(imgUri.path, options)
        imageView?.setImageBitmap(bitmap)
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
            println(TAG + " image gallery Saving")
            val f = File(wallpaperDirectory, ((Calendar.getInstance()
                .getTimeInMillis()).toString() + ".jpg"))
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
                println("$TAG  sendProfilePhoto message: $message " + message.obj)
                val msg: String? = message.obj as? String
                msg.let { value ->
                    if (value.equals("suc")) {
                        showUploadProfilePhotoAlert()
                    }else if (value.equals("start")) {
                        Toast.makeText(this@MainActivity, "Profil fotoğrafı işleme alındı. Yükleniyor...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

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



}
