package com.lvaccaro.lamp.util.hendler

import android.content.Context

interface IEventHandler {

    fun doReceive(context: Context, information: String)
}