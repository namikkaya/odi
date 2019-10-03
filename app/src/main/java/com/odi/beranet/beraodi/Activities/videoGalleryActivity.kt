package com.odi.beranet.beraodi.Activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.odiLib.videoGalleryLibrary.videoGalleryGridViewAdapter

class videoGalleryActivity : AppCompatActivity(),AdapterView.OnItemClickListener {

    private val TAG:String = "videoGalleryActivity:"

    // OBJECT
    private lateinit var galleryGridView: GridView
    private var myActionBar: ActionBar? = null

    // DATA
    var videoGalleryData:ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()

    // CLASS
    private lateinit var galleryAdapter: videoGalleryGridViewAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)

        navigationBarConfiguration()

        val extras = intent.extras
        if (extras != null) {
            if (extras.getSerializable("galleryData") != null) {
                videoGalleryData = extras.getSerializable("galleryData") as ArrayList<dataBaseItemModel>
                println("$TAG videoGallery data: ${videoGalleryData}")
                gridConfiguration()
            }
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
        println("$TAG onItemClick +++")
        println("$TAG onItemClick: id: ${videoGalleryData[position].id}")
        intent.putExtra("selectedData", videoGalleryData[position])
        setResult(RESULT_OK, intent)
        finish()
    }
}
