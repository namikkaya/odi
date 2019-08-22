package com.odi.beranet.beraodi.odiLib

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import com.odi.beranet.beraodi.Activities.introFragments.introFragment


class introFragmentPager(fragmentManager:FragmentManager, val list:ArrayList<String>): FragmentPagerAdapter(fragmentManager) {

    private var mCurrentFragment: Fragment? = null

    override fun getItem(p0: Int): Fragment {
        return introFragment.newInstance(list[p0])
    }

    override fun getCount(): Int {
        return list.size
    }

    fun getCurrentFragment(): Fragment? {
        return mCurrentFragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        val getFragment = `object` as Fragment
        val fragmentManager = `object`.fragmentManager
        val trans = fragmentManager!!.beginTransaction()
        trans.remove(getFragment)
        trans.commit()
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        if (getCurrentFragment() !== `object`) {
            mCurrentFragment = `object` as Fragment
        }
        super.setPrimaryItem(container, position, `object`)
    }
}
