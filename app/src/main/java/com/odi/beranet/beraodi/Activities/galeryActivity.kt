package com.odi.beranet.beraodi.Activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.provider.MediaStore
import android.support.annotation.NonNull
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.github.chrisbanes.photoview.PhotoView
import com.odi.beranet.beraodi.MainActivity
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.Model_images
import com.odi.beranet.beraodi.odiLib.*
import com.onesignal.OneSignal
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Fotoğraf kolajı yaptırılan activity.
 */
class galeryActivity : baseActivity(), AdapterView.OnItemClickListener, odiInterface {

    var warningIntent:Intent? = null
    /**
     * override method baseActivity
     * internet bağlantısını kontrol eder ve durum değiştiiğinde tetiklenir.
     * */
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


    private var TAG: String = "galeryActivity: "

    // - object
    private var myBackButton: ImageButton? = null
    private var mySaveButton: Button? = null
    private var leftImage: PhotoView? = null
    private var rightTopImage: PhotoView? = null
    private var rightBottomImage: PhotoView? = null
    private var leftContainer: LinearLayout? = null
    private var rightTopContainer: LinearLayout? = null
    private var rightBottomContainer: LinearLayout? = null
    private lateinit var contentPreloader:RelativeLayout
    private var gridView: GridView? = null
    var uploadProfilePhotoMessageHandler: Handler? = null


    // - class
    private var gridViewAdapter: Adapter_PhotosFolder? = null



    // - values
    private var column_index_data: Int = 0
    private var selectedImage: SELECTED_CONTAINER? = null
    private var userId: String? = null
    private var al_images: ArrayList<Model_images> = ArrayList()
    var filepath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galery)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        userId = singleton.userId
        uiActionBar()
        checkGalleryPermission()
    }

    private fun uiActionBar () {
        val bar = supportActionBar
        bar!!.setBackgroundDrawable(ColorDrawable(-0x8500))
        bar.hide()
        bar.setDisplayHomeAsUpEnabled(true)
        bar.setHomeButtonEnabled(true)
    }

    private fun uiConfig() {

        contentPreloader = findViewById(R.id.contentPreloader_gallery)
        myBackButton = findViewById(R.id.myBackButton)
        myBackButton?.setOnClickListener(clickListener)

        mySaveButton = findViewById(R.id.mySaveButton)
        mySaveButton?.setOnClickListener(clickListener)

        // containers
        leftContainer = findViewById(R.id.leftContainer)
        rightTopContainer = findViewById(R.id.rightTopContainer)
        rightBottomContainer = findViewById(R.id.rightBottamContainer)

        leftContainer?.setOnClickListener(clickListener)
        rightTopContainer?.setOnClickListener(clickListener)
        rightBottomContainer?.setOnClickListener(clickListener)


        // images
        leftImage = findViewById(R.id.LeftImage)
        rightTopImage = findViewById(R.id.RightTopImage)
        rightBottomImage = findViewById(R.id.RightBottomImage)

        leftImage?.setOnClickListener(clickListener)
        rightTopImage?.setOnClickListener(clickListener)
        rightBottomImage?.setOnClickListener(clickListener)

        gridView = findViewById(R.id.gv_folder)
        gridView?.isVerticalScrollBarEnabled = false

        fn_imagespath()

        leftImage?.isDrawingCacheEnabled = true
        rightTopImage?.isDrawingCacheEnabled = true
        rightBottomImage?.isDrawingCacheEnabled = true
    }

    // onesignal configuration--
    private fun oneSignalConfiguration() {
        println("$TAG oneSignalConfiguration: start")
        OneSignal.idsAvailable { userId, registrationId ->
            if (registrationId != null) {
                singleton.onesignal_playerId = userId
                singleton.onesignal_registrationId = registrationId
                println("$TAG oneSignalConfiguration: Notification için id eklendi")
            }else {
                // timer kurulacak...
                println("$TAG oneSignalConfiguration: HATA player id alınamadı...")
            }
        }
    }

    fun fn_imagespath(): ArrayList<Model_images> {
        al_images.clear()

        var absolutePathOfImage: String? = null
        var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.Images.Media.DATA)

        val orderBy = MediaStore.Images.Media.DATE_TAKEN
        var cursor = applicationContext.contentResolver.query(uri, projection, null, null, orderBy + " DESC")

        column_index_data = cursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data)

            val al_path = ArrayList<String>()
            al_path.add(absolutePathOfImage)
            val obj_model = Model_images()
            obj_model.setAl_imagepath(al_path)

            al_images.add(obj_model)

        }

        gridViewAdapter = Adapter_PhotosFolder(applicationContext, al_images)
        gridView?.adapter = gridViewAdapter

        gridView?.onItemClickListener = this

        return al_images
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // Button Click Listener

    // gridView click listener
    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if (selectedImage == null) {
            Toast.makeText(applicationContext, "Lütfen Çerçeveyi Seçiniz !", Toast.LENGTH_LONG).show()
            return
        }

        val path = al_images[p2].getAl_imagepath()?.get(0)

        var ratio:Float? = null

        when (selectedImage) {
            SELECTED_CONTAINER.LEFT -> {
                leftContainer?.post(Runnable {
                    run {
                        val height = leftContainer?.height
                        val width = leftContainer?.width

                        if (height != null) {
                            ratio = height.toFloat() / width!!.toFloat()
                        }

                        val contentURI = Uri.fromFile(File(path))
                        val destinationFileName = "SAMPLE_CROPPED_IMAGE_NAME"+".jpg"
                        val cropper = UCrop.of(contentURI, Uri.fromFile(File(cacheDir, destinationFileName)))

                        if (ratio != null) {
                            cropper.withAspectRatio(1F, ratio!!)
                        }

                        cropper.start(this)
                    }
                })
            }
            SELECTED_CONTAINER.RIGHT_TOP -> {
                rightTopContainer?.post(Runnable {
                    run {
                        val height = rightTopContainer?.height
                        val width = rightTopContainer?.width

                        if (height != null) {
                            ratio = height.toFloat() / width!!.toFloat()
                        }


                        val contentURI = Uri.fromFile(File(path))
                        val destinationFileName = "SAMPLE_CROPPED_IMAGE_NAME"+".jpg"
                        val cropper = UCrop.of(contentURI, Uri.fromFile(File(cacheDir, destinationFileName)))
                        if (ratio != null) {
                            cropper.withAspectRatio(1F, ratio!!)
                        }
                        cropper.start(this)
                    }
                })

            }
            SELECTED_CONTAINER.RIGHT_BOTTOM -> {
                rightBottomContainer?.post(Runnable {
                    run {
                        val height = rightBottomContainer?.height
                        val width = rightBottomContainer?.width

                        if (height != null) {
                            ratio = height.toFloat() / width!!.toFloat()
                        }
                        val contentURI = Uri.fromFile(File(path))
                        val destinationFileName = "SAMPLE_CROPPED_IMAGE_NAME"+".jpg"
                        val cropper = UCrop.of(contentURI, Uri.fromFile(File(cacheDir, destinationFileName)))
                        if (ratio != null) {
                            cropper.withAspectRatio(1F, ratio!!)
                        }
                        cropper.start(this)
                    }
                })
            }
        }


    }


    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.myBackButton -> onBackButtonEvent()
            R.id.mySaveButton -> onSaveEventAction()
            R.id.leftContainer -> selectedContainer(SELECTED_CONTAINER.LEFT)
            R.id.rightTopContainer -> selectedContainer(SELECTED_CONTAINER.RIGHT_TOP)
            R.id.rightBottamContainer -> selectedContainer(SELECTED_CONTAINER.RIGHT_BOTTOM)
            R.id.LeftImage -> selectedContainer(SELECTED_CONTAINER.LEFT)
            R.id.RightTopImage -> selectedContainer(SELECTED_CONTAINER.RIGHT_TOP)
            R.id.RightBottomImage -> selectedContainer(SELECTED_CONTAINER.RIGHT_BOTTOM)
        }
    }


    private fun onBackButtonEvent() {
        finish()
    }

    private fun onSaveEventAction() {
        print("Portre onSaveEventAction")
        savePicture()
    }

    // contanier seçimi
    private fun selectedContainer(status: SELECTED_CONTAINER) {
        when (status) {
            SELECTED_CONTAINER.LEFT -> {
                leftContainer?.setBackgroundResource(R.drawable.gallery_image_border_selected)
                rightTopContainer?.setBackgroundResource(R.drawable.gallery_image_border_normal)
                rightBottomContainer?.setBackgroundResource(R.drawable.gallery_image_border_normal)
                selectedImage = SELECTED_CONTAINER.LEFT
            }
            SELECTED_CONTAINER.RIGHT_TOP -> {
                leftContainer?.setBackgroundResource(R.drawable.gallery_image_border_normal)
                rightTopContainer?.setBackgroundResource(R.drawable.gallery_image_border_selected)
                rightBottomContainer?.setBackgroundResource(R.drawable.gallery_image_border_normal)
                selectedImage = SELECTED_CONTAINER.RIGHT_TOP
            }
            else -> {
                leftContainer?.setBackgroundResource(R.drawable.gallery_image_border_normal)
                rightTopContainer?.setBackgroundResource(R.drawable.gallery_image_border_normal)
                rightBottomContainer?.setBackgroundResource(R.drawable.gallery_image_border_selected)
                selectedImage = SELECTED_CONTAINER.RIGHT_BOTTOM
            }
        }
    }

    //-------------------------------------------------------------------
    private fun getBitmapCopy(path: String, v: Size): Bitmap? {
        try {
            var b: Bitmap? = null
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true

            val matrix = Matrix()
            val exifReader = ExifInterface(path)
            val orientation = exifReader.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            var rotate = 0
            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
                // Do nothing. The original image is fine.
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotate = 90
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotate = 180
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotate = 270
            }
            matrix.postRotate(rotate.toFloat())
            try {
                b = loadBitmap(path, rotate, v.width, v.height)

            } catch (e: OutOfMemoryError) {
            }

            System.gc()
            return b
        } catch (e: Exception) {
            Log.e("my tag", e.message, e)
            return null
        }

    }


    private fun getBitmap(path: String, v: ImageView): Bitmap? {
        try {
            var b: Bitmap? = null
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true

            val matrix = Matrix()
            val exifReader = ExifInterface(path)
            val orientation = exifReader.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            var rotate = 0
            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
                // Do nothing. The original image is fine.
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotate = 90
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotate = 180
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotate = 270
            }
            matrix.postRotate(rotate.toFloat())
            try {
                b = loadBitmap(path, rotate, v.width, v.height)

            } catch (e: OutOfMemoryError) {
            }

            System.gc()
            return b
        } catch (e: Exception) {
            Log.e("my tag", e.message, e)
            return null
        }

    }

    fun loadBitmap(path: String, orientation: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            var sourceWidth: Int
            var sourceHeight: Int
            if (orientation == 90 || orientation == 270) {
                sourceWidth = options.outHeight
                sourceHeight = options.outWidth
            } else {
                sourceWidth = options.outWidth
                sourceHeight = options.outHeight
            }
            if (sourceWidth > targetWidth || sourceHeight > targetHeight) {
                val widthRatio = sourceWidth.toFloat() / targetWidth.toFloat()
                val heightRatio = sourceHeight.toFloat() / targetHeight.toFloat()
                val maxRatio = Math.max(widthRatio, heightRatio)
                options.inJustDecodeBounds = false
                options.inSampleSize = maxRatio.toInt()
                bitmap = BitmapFactory.decodeFile(path, options)
            } else {
                bitmap = BitmapFactory.decodeFile(path)
            }
            if (orientation > 0) {
                val matrix = Matrix()
                matrix.postRotate(orientation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            sourceWidth = bitmap!!.width
            sourceHeight = bitmap.height
            if (sourceWidth != targetWidth || sourceHeight != targetHeight) {
                val widthRatio = sourceWidth.toFloat() / targetWidth.toFloat()
                val heightRatio = sourceHeight.toFloat() / targetHeight.toFloat()
                val maxRatio = Math.max(widthRatio, heightRatio)
                sourceWidth = (sourceWidth.toFloat() / maxRatio).toInt()
                sourceHeight = (sourceHeight.toFloat() / maxRatio).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, sourceWidth, sourceHeight, true)
            }
        } catch (e: Exception) {
        }

        return bitmap
    }

    override fun onBackPressed() {
        return
    }


    private fun savePicture() {
        print("Portre savePicture")
        if (leftImage?.drawable == null || rightTopImage?.drawable == null || rightBottomImage?.drawable == null) {
            val builder = AlertDialog.Builder(this)

            builder.setMessage("Tüm çerçeveleri doldurmanız gerekiyor.").setTitle("Odi")
                .setCancelable(false)
                .setPositiveButton("Tamam", DialogInterface.OnClickListener { dialog, id -> return@OnClickListener })
            val alert = builder.create()
            alert.show()
            return

        }



        leftImage?.destroyDrawingCache()
        leftImage?.buildDrawingCache()
        val bitmap1 = Bitmap.createBitmap(leftImage?.drawingCache)


        rightTopImage?.destroyDrawingCache()
        rightTopImage?.buildDrawingCache()
        val bitmap2 = Bitmap.createBitmap(rightTopImage?.drawingCache)

        rightBottomImage?.destroyDrawingCache()
        rightBottomImage?.buildDrawingCache()
        val bitmap3 = Bitmap.createBitmap(rightBottomImage?.drawingCache)

        var _bitmapFinal = combineImageIntoOne(bitmap1, bitmap2, bitmap3)

        var bitmapFinal:Bitmap?

        if (_bitmapFinal.width.toDouble() > 1000 || _bitmapFinal.height > 1000) {
            if (_bitmapFinal.width >= _bitmapFinal.height) {
                val rate:Double = _bitmapFinal.width.toDouble() / _bitmapFinal.height.toDouble()
                val width = 1000.0
                val height:Double = width / rate

                val bitmapResizeFile = resizeBitmap(_bitmapFinal, width.toInt(), height.toInt())
                val outputStream = ByteArrayOutputStream()
                bitmapResizeFile.compress(Bitmap.CompressFormat.JPEG,100,outputStream)

                bitmapFinal = bitmapResizeFile

            }else {
                val rate:Double = _bitmapFinal.height.toDouble() / _bitmapFinal.width.toDouble()
                val height = 1000.0
                val width:Double = height / rate

                val bitmapResizeFile = resizeBitmap(_bitmapFinal, width.toInt(), height.toInt())
                val outputStream = ByteArrayOutputStream()
                bitmapResizeFile.compress(Bitmap.CompressFormat.JPEG,100,outputStream)

                bitmapFinal = bitmapResizeFile
            }


        }else {
            val bitmapResizeFile = resizeBitmap(_bitmapFinal, _bitmapFinal.width, _bitmapFinal.height)
            val outputStream = ByteArrayOutputStream()
            bitmapResizeFile.compress(Bitmap.CompressFormat.JPEG,80,outputStream)

            bitmapFinal = bitmapResizeFile
        }


        contentPreloader.visibility = View.VISIBLE

        val filename = "profil_$userId.jpg"
        println("$TAG filename : $filename")
        val sd = Environment.getExternalStorageDirectory()
        val dest = File(sd, filename)

        if (dest.exists()) {
            dest.delete()
        }
        try {
            val out = FileOutputStream(dest)
            bitmapFinal.setPixel(bitmapFinal.width - 1, bitmapFinal.height - 1, PixelFormat.RGBA_8888)
            bitmapFinal.compress(Bitmap.CompressFormat.JPEG, 80, out)

            out.flush()
            out.close()

            MediaStore.Images.Media.insertImage(contentResolver, dest.absolutePath, dest.name, dest.name)

            filepath = dest.path

            uploadProfilePhotoMessageHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(message: Message) {
                    println("$TAG  sendProfilePhoto message: $message " + message.obj)
                    val msg: String? = message.obj as? String
                    msg.let { value ->
                        if (value.equals("suc")) {
                            //contentPreloader.visibility = View.INVISIBLE
                            showAlert()
                        }else if (value.equals("start")) {
                            Toast.makeText(this@galeryActivity, "Kolaj fotoğrafı işleme alındı. Yükleniyor...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            Thread(Runnable {
                val multipart = MultipartUtility(singleton.FILE_UPLOAD_URL, "UTF-8")
                multipart.delegate = this
                multipart.addFormField("website", "www.asd.com.tr")
                multipart.addFormField("email", "abc@gmail.com")
                multipart.addFilePart("image", File(filepath))
                val responseserver = multipart.finish()
                println("$TAG multipart response: $responseserver ")
            }).start()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }

    }

    // Method to resize a bitmap programmatically
    private fun resizeBitmap(bitmap:Bitmap, width:Int, height:Int):Bitmap{
        return Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            true
        )
    }

    private fun combineImageIntoOne(bLeft: Bitmap, RightTop: Bitmap, RightBot: Bitmap): Bitmap {

        val temp = Bitmap.createBitmap(bLeft.width + 12 + RightTop.width, bLeft.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(temp)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bLeft, 0f, 0f, null)
        canvas.drawBitmap(RightTop, (bLeft.width + 12).toFloat(), 0f, null)
        canvas.drawBitmap(RightBot, (bLeft.width + 12).toFloat(), (RightTop.height + 12).toFloat(), null)

        return temp
    }

    override fun Interface_profilPhotoUploadStatus(status: HTTP_PROCESS_STATUS) {
        if (HTTP_PROCESS_STATUS.success == status) {
            workerThread("suc")
        } else if (HTTP_PROCESS_STATUS.start == status) {
            workerThread("start")
        }
    }

    fun workerThread(status: String) {
        var message: Message = uploadProfilePhotoMessageHandler!!.obtainMessage(1, status)
        message.sendToTarget()
    }

    private fun showAlert() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Portre Kolajınız Kaydedildi.").setTitle("Odi")
                .setCancelable(false)
                .setPositiveButton("Tamam") { dialog, id ->
                    val intent = Intent()
                    intent.putExtra("PLAY_SHOW", "done")
                    setResult(Activity.RESULT_FIRST_USER, intent)
                    finish()

                }
            val alert = builder.create()
            alert.show()

        } catch (ex: Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
            println("$TAG checkGalleryPermission: OKEY")
            uiConfig()
        } else {
            println("$TAG checkGalleryPermission: FAIL")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), Activity_Result.GALLERY.value)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Activity_Result.GALLERY.value == requestCode) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                uiConfig()
            }else {
                setResult(RESULT_OK)
                finish()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            handleCropResult(data!!)
        }
    }

    private fun handleCropResult(@NonNull result:Intent) {

        val resultUri: Uri? = UCrop.getOutput(result)

        if (resultUri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)

            when (selectedImage) {
                SELECTED_CONTAINER.LEFT -> {
                    leftImage!!.setImageBitmap(bitmap)
                }
                SELECTED_CONTAINER.RIGHT_TOP -> {
                    rightTopImage!!.setImageBitmap(bitmap)
                }
                SELECTED_CONTAINER.RIGHT_BOTTOM -> {
                    rightBottomImage!!.setImageBitmap(bitmap)
                }
            }

        } else {
            Toast.makeText(this, "Resim düzenlenirken bir hata oluştu. kod:11", Toast.LENGTH_SHORT).show()
        }
    }

}
