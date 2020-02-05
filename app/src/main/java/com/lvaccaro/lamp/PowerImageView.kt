package com.lvaccaro.lamp

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.widget.ImageView

class PowerImageView: ImageView {

    private var light = false

    constructor(context: Context?): super(context)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

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