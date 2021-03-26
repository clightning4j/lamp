package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NewBlockHandler : IEventHandler {

    companion object {
        val TAG = NewChannelPayment::class.java.canonicalName
        val NOTIFICATION: String = "NODE_NOTIFICATION_NEW_BLOCK"
        val PATTERN = "lightningd: Adding block"
    }

    override fun doReceive(context: Context, information: String) {
        if (information.contains(PATTERN)) {
            Log.d(TAG, "****** Action received $NOTIFICATION ******")
            val regex = "Adding block [\\w|[^:]]+".toRegex()
            val height = regex.find(information)?.value?.split(" ")?.get(2)
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "new block")
            intent.putExtra("height", height?.toInt() ?: 0)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}
