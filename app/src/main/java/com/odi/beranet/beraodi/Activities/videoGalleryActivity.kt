package com.odi.beranet.beraodi.Activities

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.GridView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.videoGalleryManager
import com.odi.beranet.beraodi.odiLib.singleton
import com.odi.beranet.beraodi.odiLib.vibratePhone
import com.odi.beranet.beraodi.odiLib.videoGalleryLibrary.videoGalleryGridViewAdapter
import kotlin.collections.ArrayList

class videoGalleryActivity : AppCompatActivity(),AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private val TAG:String = "videoGalleryActivity:"

    // OBJECT
    private lateinit var galleryGridView: GridView
    private lateinit var videoGalleryCloseButton:Button
    private var myActionBar: ActionBar? = null

    // DATA
    var videoGalleryData:ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()
    var projectId:String? = null

    // CLASS
    private lateinit var galleryAdapter: videoGalleryGridViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)
        videoGalleryCloseButton = findViewById(R.id.videoGalleryCloseButton)
        videoGalleryCloseButton.setOnClickListener(clickListener)


        navigationBarConfiguration()

        val extras = intent.extras
        if (extras != null) {
            if (extras.getSerializable("galleryData") != null) {
                videoGalleryData = extras.getSerializable("galleryData") as ArrayList<dataBaseItemModel>
                videoGalleryData.reverse()
                gridConfiguration()
            }
            if (extras.getString("projectId") != null) {
                projectId = extras.getString("projectId")
            }
        }
    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.videoGalleryCloseButton -> onCloseButtonEvent()
        }
    }

    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    private fun gridConfiguration() {
        galleryGridView = findViewById(R.id.videoGalleryGridView)
        galleryAdapter = videoGalleryGridViewAdapter(this,videoGalleryData)
        galleryGridView.adapter = galleryAdapter
        galleryAdapter.notifyDataSetChanged()
        galleryGridView.onItemClickListener = this
        galleryGridView.onItemLongClickListener = this
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        vibratePhone()
        singleton.previewVideoStatus = null
        println("$TAG onItemClick +++")
        println("$TAG onItemClick: id: ${videoGalleryData[position].id}")
        intent.putExtra("selectedData", videoGalleryData[position])
        intent.putExtra("closePage", false)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        println("$TAG onItemLongClick")
        vibratePhone()
        deleteAlertView(videoGalleryData[position])

        return true
    }

    private fun deleteAlertView(deleteItem:dataBaseItemModel) {
        vibratePhone()
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.videoRemoveTitle)
        alert.setMessage(R.string.videoRemoveDesc)

        alert.setCancelable(false)

        alert.setPositiveButton(R.string.videoRemoveYes){ dialog, which ->
            vibratePhone()
            deleteItem(deleteItem)
        }

        alert.setNegativeButton(R.string.videoRemoveNo){ dialog, which ->
            vibratePhone()
        }

        alert.show()
    }

    var exitDeleteVideoStatus:Boolean = false
    fun deleteItem(item:dataBaseItemModel?) {
        if (item != null) {
            // kontrol eğer izlenen video siliniyorsa geri dönüş cameraya yapılır kontrolü
            if (singleton.removeVideoPath != null) {
                println("$TAG deleteTakip: sing: ${singleton.removeVideoPath} == ${item.videoPath}")
                if (item.videoPath == singleton.removeVideoPath) {
                    singleton.removeVideoPath = null
                    exitDeleteVideoStatus = true

                }
            }
            videoGalleryManager.deleteVideoItem(applicationContext,item) { status ->
                if (status) {
                    println("$TAG yep: deleteItem gelAllResetData çağırılacak")
                    getAllResetData()
                }
            }
        }
    }

    fun getAllResetData() {
        if (projectId != null) {
            videoGalleryManager.getProjectVideos(applicationContext,projectId!!){ status:Boolean, data: ArrayList<dataBaseItemModel>?->
                if (data != null) {

                    if (data.size <= 0 || exitDeleteVideoStatus) {
                        onCloseButtonEvent()
                        println("$TAG deleteTakip: getAllResetData if ${exitDeleteVideoStatus}")
                        singleton.gotoCamera = true
                        return@getProjectVideos
                    }
                    videoGalleryData = data
                    videoGalleryData.reverse()

                    galleryAdapter = videoGalleryGridViewAdapter(this,videoGalleryData)
                    galleryGridView.adapter = galleryAdapter
                    galleryAdapter.notifyDataSetChanged()

                    galleryGridView.invalidateViews()
                    galleryGridView.adapter = galleryAdapter
                    println("$TAG yep: getAllResetData işlemler tamam")
                }
            }
        }

    }


    override fun onBackPressed() {
        //super.onBackPressed()
        onCloseButtonEvent()
    }

    private fun onCloseButtonEvent () {
        vibratePhone()
        intent.putExtra("closePage", true)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }
}
