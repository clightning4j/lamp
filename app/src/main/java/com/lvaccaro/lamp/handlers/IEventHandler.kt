package com.lvaccaro.lamp.handlers

import android.content.Context

interface IEventHandler {

    fun doReceive(context: Context, information: String)
}