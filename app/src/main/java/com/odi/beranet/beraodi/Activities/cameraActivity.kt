package com.odi.beranet.beraodi.Activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.support.v4.app.Fragment
import android.support.v4.text.HtmlCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.odi.beranet.beraodi.Activities.cameraActivityFragments.previewFragment
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.models.playlistDataModel
import com.odi.beranet.beraodi.models.playlistItemDataModel
import com.odi.beranet.beraodi.models.videoData
import com.odi.beranet.beraodi.odiLib.*
import com.odi.beranet.beraodi.odiLib.dataBaseLibrary.videoGalleryManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.XML
import java.io.UnsupportedEncodingException
import java.lang.IllegalStateException
import java.nio.charset.Charset


class cameraActivity() : baseActivity(),
    ViewPager.OnPageChangeListener,
    previewFragment.previewFragmentInterface,
    odiInterface{

    override fun internetConnectionStatus(status: Boolean) {

    }

    private val TAG:String = "cameraActivity: "
    private var currentPageNo:Int = 0
    private lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: cameraFragmentViewPager
    private lateinit var orientationInfo: RelativeLayout
    private lateinit var contentPreloader: RelativeLayout

    private var list:ArrayList<Fragment> = ArrayList<Fragment>()

    var projectId:String? = null
    var userId:String? = null
    var processType:nativePage? = null
    var bottomBarHeight:Int? = null

    var myCameraFragment:previewFragment? = null

    override fun onPreviewFragment_infoNullEx() {
        super.onPreviewFragment_infoNullEx()
        if (myCameraFragment != null) {
            myCameraFragment!!.userId = this.userId
            myCameraFragment!!.projectId = this.projectId
            myCameraFragment!!.processType = this.processType
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bottomBarHeight = this!!.getBottomHeight()


        val bundle=intent.extras
        if(bundle!=null)
        {
            projectId = bundle.getString("projectId")
            userId = bundle.getString("userId")
            processType = bundle.getSerializable("type") as nativePage?
            println("$TAG projectID: $projectId - userId: $userId")
            singleton.userId = userId
            println("$TAG processType: $processType")
        }

        contentPreloader = findViewById(R.id.contentPreloader)
        orientationInfo = findViewById(R.id.orientationInfo)

        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        window.decorView.apply {
            systemUiVisibility = /*View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or*/
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }


        val cameraFragment = previewFragment.newInstance()
        myCameraFragment = cameraFragment
        cameraFragment.userId = this.userId
        cameraFragment.projectId = this.projectId
        cameraFragment.processType = this.processType

        singleton.cameraUserID = this.userId
        singleton.cameraProjectID = this.projectId
        singleton.cameraProcessType = this.processType

        if (bottomBarHeight != null){
            cameraFragment.bottomBarHeight = bottomBarHeight
        }

        list.add(cameraFragment)

        viewPager = findViewById(R.id.viewPager)

        pagerAdapter = cameraFragmentViewPager(supportFragmentManager, list)
        viewPager.adapter = pagerAdapter

        viewPager.setCurrentItem(0, true)
        viewPager.addOnPageChangeListener(this)

        getProjectData()

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.apply {
            systemUiVisibility = /*View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or*/
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }
    }

    private fun nextPage() {
        viewPager.setCurrentItem(currentPageNo + 1, true)
    }

    override fun onPageScrollStateChanged(p0: Int) { }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) { }

    override fun onPageSelected(p0: Int) {
        currentPageNo = p0
        setSlideNo(currentPageNo)
    }

    private fun setSlideNo(i: Int) {

    }

    override fun onResume() {
        super.onResume()
        onCheckFreeSpace()

        videoGalleryManager.clearExpired(applicationContext)  { status:Boolean ->
            if (status) {
                println("$TAG silinecek videolar silindi")
            }else {
                println("$TAG silinecek video yok")
            }
        }
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

    override fun onPreviewFragment_orientationInfo(status: Boolean) {
        super.onPreviewFragment_orientationInfo(status)
        if (status) {
            orientationInfo.visibility = View.VISIBLE
        }else {
            orientationInfo.visibility = View.INVISIBLE
        }
    }

    override fun onPreviewFragment_closeEvent() {
        super.onPreviewFragment_closeEvent()
        finish()
    }

    override fun onPreviewFragment_GalleryActivityStart() {
        super.onPreviewFragment_GalleryActivityStart()
        goToPreviewVideo(null,true)
    }

    override fun onBackPressed() {
        return
    }

    fun fixEncodingUnicode(response: String): String {
        var str = ""
        try {
            val charset: Charset = Charsets.UTF_8
            str = String(response.toByteArray(charset("ISO-8859-1")), charset)
        } catch (e: UnsupportedEncodingException) {

            e.printStackTrace()
        }

        return HtmlCompat.fromHtml(str, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    var dataTypeHolder:RECORD_TYPE? = null
    fun getProjectData() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)

        var url:String = ""
        if (processType == nativePage.cameraOdile) {
            url = "http://odi.odiapp.com.tr/core/odi.php?id=$userId"
        }else {
            url = "http://odi.odiapp.com.tr/core/odi.php?id=$projectId"
        }

        println("$TAG jsonData: strResp url: $url ->url ")

        val stringReq = StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->

                var strResp = response.toString()
                println("$TAG jsonData: strResp str: $strResp")

                var data:JSONObject? = null
                try {
                    data = XML.toJSONObject(strResp)
                }catch (e: JSONException) {
                    Log.d(TAG, e.toString() + " --> jsonError")
                    e.printStackTrace()
                }

                try {
                    val in_data = data?.getJSONObject("PROJE")
                    println("$TAG jsonData: strRespAll : $in_data")
                    val in_projectType: Int? = in_data?.getInt("TIP") // 1

                    when (in_projectType) {
                        1 -> dataTypeHolder = RECORD_TYPE.MONOLOG
                        2 -> dataTypeHolder = RECORD_TYPE.DIALOG
                        3 -> dataTypeHolder = RECORD_TYPE.PLAYMODE
                    }

                    if (dataTypeHolder != null) {
                        if (dataTypeHolder == RECORD_TYPE.MONOLOG) {
                            val in_Attr: JSONObject? = in_data?.getJSONObject("ATTR") //ATTR
                            //println("$TAG jsonData: strResp : $in_Attr")
                            jsonParser(dataTypeHolder, in_Attr, null)
                        }
                        if (dataTypeHolder == RECORD_TYPE.DIALOG) {
                            val intAttrArray:JSONArray? = in_data?.getJSONArray("ATTR")
                            //println("$TAG jsonData: strResp dialog : $intAttrArray")
                            jsonParser(dataTypeHolder, null, intAttrArray)
                        }
                        if(dataTypeHolder == RECORD_TYPE.PLAYMODE) {
                            val in_Attr: JSONObject? = in_data?.getJSONObject("ATTR") //ATTR
                            jsonParser(dataTypeHolder, in_Attr, null)
                        }
                    }
                }catch (e:Exception) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.contentProblemTitle)
                    builder.setMessage(R.string.contentProblemDesc)

                    builder.setPositiveButton(R.string.Okey) { dialog, which ->
                        this.finish()
                    }
                    builder.show()
                }

            }, Response.ErrorListener {
                println("$TAG errorVolley error Listener")
            }
        )
        queue.add(stringReq)
    }

    val playlistArray = ArrayList<playlistItemDataModel>()

    fun jsonParser(type:RECORD_TYPE?, attr:JSONObject?, attr2:JSONArray?){
        if (type != null) {
            when(type!!) {
                RECORD_TYPE.PLAYMODE -> {
                    if (attr != null) {
                        contentPreloader.visibility = View.VISIBLE
                        soundFileCountHolder = 0

                        val in_index:Int? = attr?.getInt("index")
                        val in_text:String? = fixEncodingUnicode(attr?.getString("text"))
                        val in_soundFilePath:String? = attr?.getString("soundfile")
                        val in_duration:String? = attr?.getString("duration")
                        val in_type:String? = attr?.getString("type")

                        println("$TAG jsonparser playModeManager: $in_index - $in_text - $in_soundFilePath")

                        var _playlistItemDataModel = playlistItemDataModel(in_index,
                            in_text,
                            null,
                            in_soundFilePath,
                            in_type,
                            this,
                            this,
                            RECORD_TYPE.PLAYMODE)

                        playlistArray.add(_playlistItemDataModel)
                        /*var playlistDataModel = playlistDataModel(RECORD_TYPE.PLAYMODE, playlistArray)
                        var myFragment = pagerAdapter.getCurrentFragment() as previewFragment
                        myFragment.getData(playlistDataModel)*/

                    }
                }
                RECORD_TYPE.DIALOG -> {
                    if (attr2 != null) {

                        contentPreloader.visibility = View.VISIBLE

                        for (i in 0 until attr2.length()) {
                            //println("$TAG jsonData: strResp item : ${attr2[i]} i: $i ")
                            val itemData = attr2[i] as JSONObject
                            //println("$TAG jsonData: final : ${itemData.getString("text")} i: $i ")
                            val in_index:Int? = itemData?.getInt("index")
                            val in_text:String? = fixEncodingUnicode(itemData?.getString("text"))
                            val in_soundFilePath:String? = itemData?.getString("soundfile")
                            var in_duration_:String? = itemData?.getString("duration")

                            var duration:Long? = null
                            if(in_duration_ == "") {
                                duration = null
                                soundFileCountHolder++
                            }else {
                                duration = in_duration_?.toLong()
                            }

                            val in_type:String? = itemData?.getString("type")

                            println("$TAG jsonData: sound: soundfile:$in_soundFilePath - type: $in_type duration: $duration")


                            val _playlistItemDataModel = playlistItemDataModel(in_index,
                                in_text,
                                duration,
                                in_soundFilePath,
                                in_type,
                                this,
                                this,
                                RECORD_TYPE.DIALOG)

                            playlistArray.add(_playlistItemDataModel)

                        }
                    }

                }
                RECORD_TYPE.MONOLOG -> {
                    if (attr != null) {

                        /*Yükleme işi yok. Monolog kendi tanıtımı preloader çıkmasada olur.*/
                        val in_index:Int? = attr?.getInt("index")
                        val in_text:String? = fixEncodingUnicode(attr?.getString("text"))
                        val in_soundFilePath:String? = attr?.getString("soundfile")
                        val in_duration:Int? = attr?.getInt("duration")
                        val in_type:String? = attr?.getString("type")

                        var _playlistItemDataModel = playlistItemDataModel(in_index,
                            in_text,
                            in_duration!!.toLong(),
                            in_soundFilePath,
                            in_type,
                            this,
                            this,
                            RECORD_TYPE.MONOLOG)

                        val playlistArray = ArrayList<playlistItemDataModel>()
                        playlistArray.add(_playlistItemDataModel)

                        var playlistDataModel = playlistDataModel(RECORD_TYPE.MONOLOG, playlistArray)
                        var myFragment = pagerAdapter.getCurrentFragment() as previewFragment
                        myFragment.getData(playlistDataModel)
                    }
                }
            }
        }
    }

    var soundFileCountHolder:Int = 0
    var soundCounter:Int = 0
    override fun onCameraActivity_playlistSoundComplete(index: Int?, duration: Long?) {
        super.onCameraActivity_playlistSoundComplete(index,duration)
        println("$TAG onCameraActivity_playlistSoundComplete: $index nolu indexli item yüklendi")
        soundCounter ++
        if (soundFileCountHolder <= soundCounter) {
            println("$TAG onCameraActivity_playlistSoundComplete SUCCESS")

            when(dataTypeHolder) {
                RECORD_TYPE.DIALOG -> {
                    contentPreloader.visibility = View.INVISIBLE
                    var playlistDataModel = playlistDataModel(RECORD_TYPE.DIALOG, playlistArray)
                    var myFragment = pagerAdapter.getCurrentFragment() as previewFragment
                    myFragment.getData(playlistDataModel)
                    println("Tektik: playMode dialog get data çalıştı")
                }
                RECORD_TYPE.PLAYMODE -> {
                    println("Tektik: playMode playModeget data çalıştı")
                    contentPreloader.visibility = View.INVISIBLE
                    var playlistDataModel = playlistDataModel(RECORD_TYPE.PLAYMODE, playlistArray)
                    var myFragment = pagerAdapter.getCurrentFragment() as previewFragment
                    myFragment.getData(playlistDataModel)
                }
            }
        }
    }

    override fun onPreviewFragment_Record_Success(path: Uri?) {
        super.onPreviewFragment_Record_Success(path)
        goToPreviewVideo(path)
    }

    override fun OnPlaylistItemPlayerEnd(index: Int?, type: RECORD_TYPE) {
        super.OnPlaylistItemPlayerEnd(index, type)
        if (type == RECORD_TYPE.PLAYMODE) {
            println("$TAG OnPlaylistItemPlayerEnd")
            var myFragment = pagerAdapter.getCurrentFragment() as previewFragment
            myFragment.onRecordStopTrigger()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity_Result.PREVIEW_VIDEO_RESULT.value) {

            println("$TAG dbTakip: onActivityResult çalıştı data: $data = ${data?.getStringExtra("STATUS")} resultcode: $requestCode")


            if (data?.getStringExtra("STATUS") == "OKEY" || singleton.cameraResult == 1) {
                singleton.cameraResult = 0
                intent.putExtra("STATUS", "OKEY")
                setResult(RESULT_OK, intent)
                finish()
                println("$TAG dbTakip: geri dönüş okey")
            }else if (data?.getStringExtra("STATUS") == "RESET" || singleton.cameraResult == 2) {
                singleton.cameraResult = 0
                var intent = intent
                this.finish()
                startActivity(intent)
                overridePendingTransition(0,0)
                println("$TAG takip: geri dönüş reset")
            }

        }

        if (requestCode == Activity_Result.PREVIEW_VIDEO_RESULT.value && resultCode == Activity.RESULT_OK) {

        }
    }


    private fun goToPreviewVideo(videoPath:Uri?, onStartVideoGalleryStatus:Boolean = false) {
        val intent = Intent(this@cameraActivity, previewVideo::class.java)

        var myUri:String? = null
        videoPath?.let{ it->
            myUri = it.toString()
        }

        if (onStartVideoGalleryStatus) {
            singleton.previewVideoStatus = VIDEO_PREVIEW_STATUS.SAVED
            singleton.originalVideoPath = null
        }else {
            singleton.previewVideoStatus = VIDEO_PREVIEW_STATUS.RECORDING
            singleton.originalVideoPath = myUri
        }

        val sendVideoData = videoData(videoPath)
        intent.putExtra("videoPath", myUri)
        intent.putExtra("userId", userId)
        intent.putExtra("projectId", projectId)
        intent.putExtra("type", processType)
        startActivityForResult(intent, Activity_Result.PREVIEW_VIDEO_RESULT.value)

    }

    private fun getBottomHeight (): Int? {
        val resources = applicationContext!!.resources
        val resourcesId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourcesId > 0) {
            var returnData:Int? = null
            try {
                returnData = resources.getDimensionPixelSize(resourcesId)
            }catch (e: IllegalStateException){
                Log.e("$TAG" , "${e.toString()} -> hata: 512")
            }

            return returnData
        }
        return 0
    }

}
