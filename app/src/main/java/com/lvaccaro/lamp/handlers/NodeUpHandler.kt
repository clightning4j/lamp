package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * @author https://github.com/vincenzopalazzo
 */
class NodeUpHandler : IEventHandler {

    companion object {
        val TAG = NodeUpHandler::class.java.canonicalName
        val NOTIFICATION: String = "NODE_UP_NOTIFICATION"
        val PATTERN_ONE = "lightningd: Server started with public key"
    }

    override fun doReceive(context: Context, information: String) {
        if (information.contains(PATTERN_ONE)) {
            Log.d(TAG, "****** Action received $NOTIFICATION ******")
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "Node running")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}
