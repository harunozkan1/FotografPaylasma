package com.example.fotografpaylasma.view

import android.content.Context
import android.util.AttributeSet
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

class zoomableImageView(context: Context, attrs: AttributeSet) : ImageView(context, attrs) {

    private var scaleFactor = 1f
    private val scaleGestureDetector: ScaleGestureDetector

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.1f, min(scaleFactor, 5.0f))
            scaleX = scaleFactor
            scaleY = scaleFactor
            return true
        }
    }
}