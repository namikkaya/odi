package com.odi.beranet.beraodi.odiLib

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import java.io.File

fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}

fun Fragment.vibratePhone() {
    val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(80)
    }
}

fun AppCompatActivity.vibratePhone() {
    val vibrator = this?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(80)
    }
}