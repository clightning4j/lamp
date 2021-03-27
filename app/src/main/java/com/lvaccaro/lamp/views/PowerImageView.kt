package com.lvaccaro.lamp.views

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.lvaccaro.lamp.R

class PowerImageView : AppCompatImageView {

    private var light = false

    constructor(context: Context?): super(context!!)
    constructor(context: Context?, attrs: AttributeSet?): super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)

    fun on() {
        light = true
        isClickable = true
        setBackgroundResource(R.drawable.ic_lamp_on)
    }

    fun off() {
        light = false
        isClickable = true
        setBackgroundResource(R.drawable.ic_lamp_off)
    }

    fun animating() {
        isClickable = false
        setBackgroundResource(R.drawable.ic_lamp)
        val animation: AnimationDrawable = background as AnimationDrawable
        animation.start()
    }

    fun isAnimating(): Boolean {
        return !isClickable
    }

    fun isOn(): Boolean {
        return light
    }
}
