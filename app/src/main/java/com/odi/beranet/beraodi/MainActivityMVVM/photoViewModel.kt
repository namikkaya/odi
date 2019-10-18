package com.odi.beranet.beraodi.MainActivityMVVM

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.odi.beranet.beraodi.MainActivity
import com.odi.beranet.beraodi.odiLib.Activity_Result
import com.odi.beranet.beraodi.odiLib.Permission_Result
import java.io.File


class photoViewModel (val _this:MainActivity) {

    val TAG:String = "photoViewModel:";
    var picUri:Uri? = null

    fun showPictureDialog() {
        CHECKGALLERYPERMISSION()
    }

    private fun choosePhotoFromGallery(){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //galleryIntent.putExtra("crop", "true")
        galleryIntent.type = "image/*"
        /*galleryIntent.putExtra("crop", "true")
        galleryIntent.putExtra("outputX", 200)
        galleryIntent.putExtra("outputY", 200)
        galleryIntent.putExtra("aspectX", 1)
        galleryIntent.putExtra("aspectY", 1)
        galleryIntent.putExtra("scale", true)*/
        galleryIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())

        _this.startActivityForResult(galleryIntent, Activity_Result.GALLERY.value)
    }


    var imageFile:File? = null

    private fun takePhotoFromCamera() {
       try {
           val imageFilePath = Environment.getExternalStorageDirectory().absolutePath + "/picture.jpg"
           imageFile = File(imageFilePath)
           picUri = FileProvider.getUriForFile(_this.applicationContext, _this.applicationContext.packageName + ".provider", imageFile!!) // convert path to Uri*/
       }catch (e : Exception){
           println(TAG + "Camera hata: " + e.message)
           Toast.makeText(_this, e.message, Toast.LENGTH_SHORT).show()
       }
        var takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri)
        takePictureIntent.putExtra("return-data", true)
        _this.startActivityForResult(takePictureIntent, Permission_Result.CAMERA_CAPTURE.value)
    }

    private fun CHECKGALLERYPERMISSION() {
        if (ContextCompat.checkSelfPermission(_this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(_this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(_this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            permissionOkey()
        } else {
            ActivityCompat.requestPermissions(_this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), Permission_Result.GALLERY.value)
        }
    }

    private fun permissionOkey() {
        val pictureDialog = AlertDialog.Builder(_this)
        pictureDialog.setTitle("Profil Resmi")
        val pictureDialogItems = arrayOf("Galeri", "Kamera", "İptal")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

}