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
import android.widget.TextView
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.dataBaseItemModel
import com.odi.beranet.beraodi.models.dataBaseProjectModel
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.videoGalleryManager
import com.odi.beranet.beraodi.odiLib.singleton
import com.odi.beranet.beraodi.odiLib.vibratePhone
import com.odi.beranet.beraodi.odiLib.videoGalleryLibrary.videoGalleryGridViewAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class videoGalleryActivity : AppCompatActivity(),AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private val TAG:String = "videoGalleryActivity:"

    // OBJECT
    private lateinit var galleryGridView: GridView
    private lateinit var videoGalleryCloseButton:Button
    private var myActionBar: ActionBar? = null
    private lateinit var daysText:TextView

    // DATA
    var videoGalleryData:ArrayList<dataBaseItemModel> = ArrayList<dataBaseItemModel>()
    var projectId:String? = null

    // CLASS
    private lateinit var galleryAdapter: videoGalleryGridViewAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)

        videoGalleryCloseButton = findViewById(R.id.videoGalleryCloseButton)
        daysText = findViewById(R.id.daysText)
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

        videoGalleryManager.getAllProject(applicationContext){ status:Boolean, data: ArrayList<dataBaseProjectModel>?->
            if (status) {
                for (i in 0 until data!!.size) {
                    if (projectId == data[i].projectId) {
                        println("$TAG project expired time: ${data[i].createDate}")

                        val currentDate: Date = getCurrentDateTime()
                        val projectDate: Date = data[i].createDate!!.toDate(timeFormat)

                        var projectTimeAdd = toCalendar(projectDate)
                        projectTimeAdd.add(Calendar.DATE, 5)

                        println("$TAG project expired ekli zaman: ${projectTimeAdd.time}")

                        val finalDate = projectTimeAdd.time.time - currentDate.time
                        val seconds = finalDate / 1000
                        val minutes = seconds / 60
                        val hours = minutes / 60
                        val daysRemaining = hours / 24
                        println("$TAG expired geriye kalan zaman:  ${daysRemaining} gün - $hours saat - $minutes dakika  sonra bu videolar silinecek ")

                        var myDays:String? = ""
                        if (daysRemaining < 1) {
                            daysText.text = "Bugun içinde bu videolar silinecek."
                        }else{
                            myDays = daysRemaining.toString()
                            daysText.text = "$myDays gün sonra bu videolar silinecek."
                        }

                    }
                }
            }
        }

    }

    fun toCalendar(date:Date):Calendar {
        val cal = Calendar.getInstance()
        cal.time = date
        return cal
    }


    private val timeFormat:String = "yyyy-MM-dd HH:mm:ss"

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }

    fun String.toDate(format: String): Date { // format "dd-MM-yyyy" -- date
        val date = SimpleDateFormat(format).parse(this)
        println(date.time)
        return date
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
