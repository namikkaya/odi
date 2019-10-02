package com.odi.beranet.beraodi.odiLib.dataBaseLibrary

import android.graphics.Point
import android.view.View
import android.view.animation.*

class animationManager (val targetButton: View, val targetImageView: View) {

    fun startAnimation() {
        var targetPoint: Point = animationGetCoordinate(targetButton)
        firstAnimation(targetImageView, targetPoint, targetButton)
    }

    private fun firstAnimation(obj: View, point: Point, targetView: View) {

        obj.translationZ = 999F
        obj.bringToFront()

        val scaleAnimation = ScaleAnimation(1f, 1.4f, 1f, 1.4f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 150

        val alpha = AlphaAnimation(1F,0.7F)
        alpha.duration = 150

        val animSet = AnimationSet(true)
        animSet.fillAfter = true
        animSet.isFillEnabled = true

        animSet.addAnimation(scaleAnimation)
        animSet.addAnimation(alpha)

        obj.startAnimation(animSet)

        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                secondAnimation(obj, point.x.toFloat(), point.y.toFloat(), targetView)
            }

            override fun onAnimationStart(animation: Animation?) {

            }

        })

    }

    private fun secondAnimation(obj: View, toX:Float, toY:Float, targetView: View) {

        val slideUp = TranslateAnimation(obj.x, toX, obj.y, toY)
        slideUp.duration = 300

        val scaleAnimation = ScaleAnimation(1.4f, 0.1f, 1.4f, 0.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 300

        val alpha = AlphaAnimation(0.7F,0F)
        alpha.duration = 300

        val animSet = AnimationSet(true)
        animSet.fillAfter = true
        animSet.addAnimation(scaleAnimation)
        animSet.addAnimation(slideUp)
        animSet.addAnimation(alpha)
        obj.startAnimation(animSet)
        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                obj.visibility = View.GONE
                buttonAnimation(targetView)
            }

            override fun onAnimationStart(animation: Animation?) {

            }

        })

    }

    private fun buttonAnimation(obj: View) {
        val scaleAnimation = ScaleAnimation(1f, 1.4f, 1f, 1.4f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 300

        val animSet = AnimationSet(true)
        animSet.fillAfter = true
        animSet.addAnimation(scaleAnimation)
        obj.startAnimation(animSet)
        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                buttonAnimation2(obj)
            }

            override fun onAnimationStart(animation: Animation?) {

            }

        })
    }

    private fun buttonAnimation2(obj: View) {
        val scaleAnimation = ScaleAnimation(1.4f, 1f, 1.4f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 300

        val animSet = AnimationSet(true)
        animSet.fillAfter = true
        animSet.addAnimation(scaleAnimation)
        obj.startAnimation(animSet)
        animSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationStart(animation: Animation?) {

            }

        })
    }


    private fun animationGetCoordinate(view: View): Point {
        val buttonCoordinate = getCenterPointOfView(view)
        return buttonCoordinate
    }

    private fun getCenterPointOfView(view: View): Point {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val x = location[0] + view.width / 2
        val y = location[1] + view.height / 2
        return Point(x, y)
    }
}