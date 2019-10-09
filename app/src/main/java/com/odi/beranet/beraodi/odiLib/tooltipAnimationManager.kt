package com.odi.beranet.beraodi.odiLib

import android.content.Context
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import com.odi.beranet.beraodi.models.toolTipModel
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip

class tooltipAnimationManager (val context: Context, val data:ArrayList<toolTipModel>?) {

    private val TAG:String = "tooltipAnimationManager: "
    var currentToolTip:Int = 0


    fun startTooltip(finalView:View? = null){
        println("$TAG startTooltip + ${finalView}")

        showTooptip(currentToolTip,finalView)
    }

    private fun showTooptip(count:Int, finalView:View? = null) {

        val simpleTooltip = SimpleTooltip.Builder(context)
            .anchorView(data!![count].view)
            .text(data!![count].title)
            .gravity(data!![count].gravity!!)
            .animated(true)
            .dismissOnOutsideTouch(false)
            .dismissOnInsideTouch(false)
            .transparentOverlay(false)
            .build()

        simpleTooltip!!.show()

        val timer = object: CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                currentToolTip += 1
                simpleTooltip!!.dismiss()
                if (currentToolTip >= data!!.size) {
                    // animasyon biter
                    println("$TAG startTooltip animasyon bitti")
                    if (finalView != null) {
                        println("$TAG startTooltip visible ")
                        finalView.visibility = View.INVISIBLE
                    }
                    return
                }else {
                    startTooltip(finalView)
                }
            }
        }
        timer.start()
    }

}