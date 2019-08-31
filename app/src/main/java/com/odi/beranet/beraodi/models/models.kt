package com.odi.beranet.beraodi.models

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import com.odi.beranet.beraodi.odiLib.*
import java.io.File
import java.io.IOException
import android.media.AudioManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler


class Model_images {
    private var al_imagepath:ArrayList<String>? = null

    fun getAl_imagepath(): ArrayList<String>? {
        return al_imagepath
    }

    fun setAl_imagepath(al_imagepath:ArrayList<String>?) {
        this.al_imagepath = al_imagepath
    }
}

data class async_upload_video (val _id:String?,
                               val _uploadFile: File?,
                               val _listener: odiInterface,
                               val type:nativePage,
                               val userId:String,
                               val uploadFileType:UPLOAD_FILE_TYPE) {}

/**
 * _uploadStatus = true ise yükleme tamamlandı, false ise devam ediyor
 * _uploadProgress =  _uploadStatus true ise yükleme durumunu haber verir.
 * */
data class async_upload_video_complete(val _id:String?,
                                       val _userId:String?,
                                       val requestPath:String?,
                                       val _uploadStatus:Boolean?,
                                       val _uploadProgress:Int?,
                                       val _uploadFileType:UPLOAD_FILE_TYPE?) {}

/**
 * @param complete : eğer true giderse finish olacak
 */
data class progressData(val progress:Int?,
                        val title:String?,
                        val complete: Boolean?) {}

/**
 * JSON da ATTR kısmı
 * @param index => bilinmiyor
 * @param text => altyazı
 * @param duration => zaman
 * @param soundFile => dosya yolu
 * @param type => bilinmiyor.
 * @param context => context
 */
data class playlistItemDataModel(val index:Int?,
                                 val text:String?,
                                 val duration:Int?,
                                 val soundFile:String?,
                                 val type:String?,
                                 val context: Activity,
                                 val listener:odiInterface?) {

    private var mediaPlayer:MediaPlayer? = null
    internal var handler = Handler()

    /**
     * ses dosyasını çalar
     */
    public fun playSound() {
        if (mediaPlayer != null || soundFile != "") {
            mediaPlayer?.start()
        }
    }

    init {
        println("playlistItemDataModel: init")

        if (soundFile != null) {
            mediaPlayer = MediaPlayer()

            if (Build.VERSION.SDK_INT >= 21) {
                mediaPlayer?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            } else {
                mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }

            try {
                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(context, Uri.parse(soundFile))
                mediaPlayer?.prepare()
                mediaPlayer?.prepareAsync()

                mediaPlayer?.setOnPreparedListener {
                    handler.post(Runnable {
                        // çalmaya hazır olduğunu beyan eder.
                        listener?.onCameraActivity_playlistSoundComplete(index)
                    })
                }

            }catch (e:IllegalArgumentException) {
                e.printStackTrace()
            } catch (e:IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}


/**
 *@param type => monolog/dialog/playmode
 * @param dataList =>  gelen dialogların bütünü bir listede
 */
data class playlistDataModel(val type:RECORD_TYPE?, val dataList:ArrayList<playlistItemDataModel>?) {}

data class playlistReplik(val text:String, val duration: Int?, val type: String)

data class karaokeModel(val startIndex:Int?, val endIndex:Int?, val subtitle:String?, val lineNo:Int?, val bound:Rect?) {}
