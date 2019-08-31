package com.odi.beranet.beraodi.Activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.text.HtmlCompat
import android.support.v4.view.ViewPager
import android.text.Html
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
import com.odi.beranet.beraodi.odiLib.RECORD_TYPE
import com.odi.beranet.beraodi.odiLib.cameraFragmentViewPager
import com.odi.beranet.beraodi.odiLib.odiInterface
import org.json.JSONException
import org.json.JSONObject
import org.json.XML
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset


class cameraActivity() : AppCompatActivity(),
    ViewPager.OnPageChangeListener,
    previewFragment.previewFragmentInterface,
    odiInterface{
    
    private val TAG:String = "cameraActivity: "
    private var currentPageNo:Int = 0
    private lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: cameraFragmentViewPager
    private lateinit var orientationInfo: RelativeLayout

    private var list:ArrayList<Fragment> = ArrayList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        orientationInfo = findViewById(R.id.orientationInfo)

        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        window.decorView.apply {
            systemUiVisibility = /*View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or*/
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        val cameraFragment = previewFragment.newInstance()
        list.add(cameraFragment)

        viewPager = findViewById(R.id.viewPager)

        pagerAdapter = cameraFragmentViewPager(supportFragmentManager, list)
        viewPager.adapter = pagerAdapter

        viewPager.setCurrentItem(0, true)
        viewPager.addOnPageChangeListener(this)

        getProjectData()

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
        val url: String = "http://odi.odiapp.com.tr/core/odi.php?id=15"

        // Request a string response from the provided URL.
        val stringReq = StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->

                var strResp = response.toString()
                /*var strResp = String(response.deco,"UTF-8")
                try {
                    strResp = fixEncodingUnicode(response)
                } catch (e: UnsupportedEncodingException) {

                    e.printStackTrace()
                }*/


                //String utf8String = new String(response.data, "UTF-8");

                println("$TAG jsonData: strResp str: $strResp")

                var data:JSONObject? = null
                try {
                    data = XML.toJSONObject(strResp)

                }catch (e: JSONException) {
                    Log.d(TAG, e.toString() + " --> jsonError")
                    e.printStackTrace()
                }

                val in_data = data?.getJSONObject("PROJE")
                println("$TAG jsonData: strRespAll : $in_data")
                val in_projectType: Int? = in_data?.getInt("TIP") // 1

                val in_Attr: JSONObject? = in_data?.getJSONObject("ATTR") //ATTR
                println("$TAG jsonData: strResp : $in_Attr")

                when (in_projectType) {
                    1 -> dataTypeHolder = RECORD_TYPE.MONOLOG
                    2 -> dataTypeHolder = RECORD_TYPE.DIALOG
                    3 -> dataTypeHolder = RECORD_TYPE.PLAYMODE
                }

                jsonParser(dataTypeHolder, in_Attr)

            }, Response.ErrorListener {
                println("$TAG errorVolley error Listener")
            }
        )
        queue.add(stringReq)
    }


    fun jsonParser(type:RECORD_TYPE?, attr:JSONObject?){
        if (type != null && attr != null) {
            when(type!!) {
                RECORD_TYPE.PLAYMODE -> {

                }
                RECORD_TYPE.DIALOG -> {

                }
                RECORD_TYPE.MONOLOG -> {

                    /*Yükleme işi yok. Monolog kendi tanıtımı preloader çıkmasada olur.*/
                    val in_index:Int? = attr?.getInt("index")
                    val in_text:String? = fixEncodingUnicode(attr?.getString("text"))
                    val in_soundFilePath:String? = attr?.getString("soundfile")
                    val in_duration:Int? = attr?.getInt("duration")
                    val in_type:String? = attr?.getString("type")

                    var _playlistItemDataModel = playlistItemDataModel(in_index,
                        in_text,
                        in_duration,
                        in_soundFilePath,
                        in_type,
                        this,
                        this)

                    val playlistArray = ArrayList<playlistItemDataModel>()
                    playlistArray.add(_playlistItemDataModel)

                    var playlistDataModel = playlistDataModel(RECORD_TYPE.MONOLOG, playlistArray)
                    var myFragment = pagerAdapter.getCurrentFragment() as previewFragment
                    myFragment.getData(playlistDataModel)
                }
            }
        }
    }

    override fun onCameraActivity_playlistSoundComplete(index: Int?) {
        super.onCameraActivity_playlistSoundComplete(index)
        println("$TAG onCameraActivity_playlistSoundComplete: $index nolu indexli item yüklendi")
    }


}
