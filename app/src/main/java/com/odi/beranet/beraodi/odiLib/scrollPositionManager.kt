package com.odi.beranet.beraodi.odiLib

import android.graphics.Rect
import android.widget.TextView
import com.odi.beranet.beraodi.models.karaokeModel

class scrollPositionManager {
    private val TAG:String = "scrollPositionManager:"

    var scrollPositionHolder:Int? = null

    fun positionManage(textView:TextView?, charIndex:Int?) {
        //println("$TAG positionManage: $charIndex")
        val lineArray = getTextLine(textView)
        //println("$TAG kareokeList count => ${lineArray.count()}")

        val scrollPosition = getScrollPosition(lineArray, charIndex!!)

        if (scrollPositionHolder != null) {
            if (scrollPosition != null) {
                if (scrollPositionHolder!! >= scrollPosition) {
                    return
                }
            }
        }
        if (scrollPositionHolder == null) {
            scrollPositionHolder = scrollPosition
            setScrollPosition(scrollPosition,textView)
        }else {
            if (scrollPosition != scrollPositionHolder) {
                scrollPositionHolder = scrollPosition
                setScrollPosition(scrollPosition,textView)
            }
        }
    }

    fun setScrollPosition(position:Int?, textView: TextView?) {
        println("$TAG scrollPosition $position")
        textView.let { it ->
            it!!.post( Runnable {
                it.scrollTo(0, position!!)
                //it.scrollBy(0,position!!)
            })
        }
    }

    fun getTextLine(textView:TextView?):ArrayList<karaokeModel> {
        var line:ArrayList<karaokeModel> = ArrayList()
        val myLayout = textView?.layout
        val text = textView?.text.toString()

        var start:Int = 0
        var end:Int = 0
        for(i in 0 until textView!!.lineCount) {
            end = myLayout!!.getLineEnd(i)

            var data = text.substring(start,end)

            var bound = Rect()
            myLayout.getLineBounds(i,bound)

            var myModel = karaokeModel(start, end, data, i, bound)
            line.add(myModel)

            start = end
        }

        return line
    }

    fun getScrollPosition(line:ArrayList<karaokeModel>, _charIndex: Int):Int? {
        if (line == null) {
            return 0
        }

        var charIndex = _charIndex + 2

        for (i in 0 until line.size) {
            if (charIndex >= line[i].startIndex!! && charIndex <= line[i].endIndex!!) {
                var lineTop = 0
                if (i >= 2) {
                    lineTop = line[i-1].bound!!.top
                }

                if (i < 2 && i >= 0) {
                    lineTop = line[0].bound!!.top
                }

                if (lineTop <= 0) {
                    lineTop = 0
                }else if (lineTop >= line[line.size-1].bound!!.top) {
                    lineTop = line[line.size - 1].bound!!.top
                }
                return lineTop
            }

        }
        return 0
    }

    fun stop() {
        scrollPositionHolder = null
    }

}