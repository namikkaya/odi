package com.odi.beranet.beraodi.Activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBar
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import com.odi.beranet.beraodi.MainActivity
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.SELECTED_CONTAINER
import com.odi.beranet.beraodi.odiLib.introFragmentPager

class introActivity : AppCompatActivity(), ViewPager.OnPageChangeListener {



    private var currentPageNo:Int = 0
    private lateinit var viewPager:ViewPager
    private lateinit var pagerAdapter:introFragmentPager

    private var list:ArrayList<String> = ArrayList<String>()

    var myActionBar: ActionBar? = null

    lateinit var one: FrameLayout
    lateinit var two: FrameLayout
    lateinit var three: FrameLayout

    lateinit var nextButton: Button
    lateinit var okeyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        navigationBarConfiguration()

        one = findViewById(R.id.slidePagerItemOne)
        two = findViewById(R.id.slidePagerItemTwo)
        three = findViewById(R.id.slidePagerItemThree)

        nextButton = findViewById(R.id.slideNextButton)
        okeyButton = findViewById(R.id.slideOkeyButton)

        nextButton.setOnClickListener(clickListener)
        okeyButton.setOnClickListener(clickListener)

        list.add("http://odi.odiapp.com.tr/bannerimg/1.png")
        list.add("http://odi.odiapp.com.tr/bannerimg/2.png")
        list.add("http://odi.odiapp.com.tr/bannerimg/3.png")

        viewPager = findViewById(R.id.viewPager)

        pagerAdapter = introFragmentPager(supportFragmentManager, list)
        viewPager.adapter = pagerAdapter

        viewPager.setCurrentItem(0, true)
        viewPager.addOnPageChangeListener(this)

    }
    // mainActivity için navigasyon barı
    private fun navigationBarConfiguration() {
        myActionBar = supportActionBar
        myActionBar?.let {
            myActionBar?.hide()
        }
    }

    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.slideNextButton -> nextPage()
            R.id.slideOkeyButton -> sendActivity()
        }
    }

    private fun sendActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }

    private fun nextPage() {
        viewPager.setCurrentItem(currentPageNo + 1, true)
    }

    private fun setSlideNo(i: Int) {
        when (i) {
            0 -> {
                println("sayfa 1")
                one.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round_selected)
                two.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round)
                three.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round)
            }
            1 -> {
                println("sayfa 2")
                one.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round)
                two.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round_selected)
                three.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round)
            }
            2 -> {
                println("sayfa 3")
                one.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round)
                two.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round)
                three.background = ContextCompat.getDrawable(this, R.drawable.slide_viewpager_round_selected)
                checkButtonControl()
            }
            else -> println("default")
        }
    }

    private fun checkButtonControl() {
        if (nextButton.visibility == View.VISIBLE && okeyButton.visibility == View.GONE) {
            nextButton.visibility = View.GONE
            okeyButton.visibility = View.VISIBLE
        }
    }

    override fun onPageSelected(p0: Int) {
        currentPageNo = p0
        setSlideNo(currentPageNo)
    }

    override fun onPageScrollStateChanged(p0: Int) {

    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {

    }

    override fun onBackPressed() {
        return
    }

}
