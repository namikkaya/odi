package com.odi.beranet.beraodi.odiLib.videoGalleryLibrary;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class dayGridLayout extends LinearLayout {
    public dayGridLayout(Context context) {
        super(context);
    }

    public dayGridLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public dayGridLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /*
    public dayGridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    */

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(heightMeasureSpec < widthMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        } else if(widthMeasureSpec < heightMeasureSpec) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
