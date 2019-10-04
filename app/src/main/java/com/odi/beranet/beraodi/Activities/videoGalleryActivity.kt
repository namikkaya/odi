package com.odi.beranet.beraodi.Activities

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.GridView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.odiLib.VIDEO_PREVIEW_STATUS
import com.odi.beranet.beraodi.odiLib.singleton
import com.odi.beranet.beraodi.odiLib.videoGalleryLibrary.videoGalleryGridViewAdapter
import java.util.*
import kotlin.collections.ArrayList

class videoGalleryActivity : AppCompatActivity(),AdapterView.OnItemClickListener {

    private val TAG:String = "videoGalleryActivity:"

    // OBJECT
    private lateinit var galleryGridView: GridView
    private lateinit var videoGalleryCloseButton:Button
    private var myActionBar: ActionBar? = null

    // DATA
    var videoGalleryData:ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()

    // CLASS
    private lateinit var galleryAdapter: videoGalleryGridViewAdapter

    private var cameraBack:Boolean = false

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
                println("$TAG videoGallery data: ${videoGalleryData}")
                gridConfiguration()
            }
        }

        if (singleton.previewVideoStatus == VIDEO_PREVIEW_STATUS.SAVED) {
            //singleton.onStartOpenVideoGalleryStatus = false
            cameraBack = true
            println("$TAG camera sayfasına dönüş yapılması gerekiyor...")
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
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        singleton.previewVideoStatus = null

        println("$TAG onItemClick +++")
        println("$TAG onItemClick: id: ${videoGalleryData[position].id}")
        intent.putExtra("selectedData", videoGalleryData[position])
        intent.putExtra("closePage", false)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        //super.onBackPressed()

    }

    private fun onCloseButtonEvent () {
        intent.putExtra("closePage", true)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }
}
