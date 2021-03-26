package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class BrokenStatus : IEventHandler {

    companion object {
        val TAG = BrokenStatus::class.java.canonicalName
        const val NOTIFICATION: String = "NODE_NOTIFICATION_BROKEN"
        const val PATTERN = "**BROKEN**"
    }

    override fun doReceive(context: Context, information: String) {
        if (information.contains(PATTERN) && !information.contains("gossip_store_compact")) {
            Log.d(TAG, "****** Action received $NOTIFICATION ******")
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "Node crash, check logs")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}
