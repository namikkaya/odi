package com.odi.beranet.beraodi.Activities

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.View
import android.view.WindowManager
import com.odi.beranet.beraodi.Activities.cameraActivityFragments.previewFragment
import com.odi.beranet.beraodi.R
import com.odi.beranet.beraodi.odiLib.Activity_Result
import com.odi.beranet.beraodi.odiLib.Permission_Result
import com.odi.beranet.beraodi.odiLib.cameraFragmentViewPager

class cameraActivity() : AppCompatActivity(), ViewPager.OnPageChangeListener, previewFragment.previewFragmentInterface {
    private val TAG:String = "cameraActivity: "
    private var currentPageNo:Int = 0
    private lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: cameraFragmentViewPager

    private var list:ArrayList<Fragment> = ArrayList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        val cameraFragment = previewFragment()
        list.add(cameraFragment)

        viewPager = findViewById(R.id.viewPager)

        pagerAdapter = cameraFragmentViewPager(supportFragmentManager, list)
        viewPager.adapter = pagerAdapter

        viewPager.setCurrentItem(0, true)
        viewPager.addOnPageChangeListener(this)

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

}
