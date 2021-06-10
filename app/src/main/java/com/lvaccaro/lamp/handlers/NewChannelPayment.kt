package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lvaccaro.lamp.utils.UI
import org.jetbrains.anko.runOnUiThread

class NewChannelPayment : IEventHandler {

    companion object {
        val TAG = NewChannelPayment::class.java.canonicalName
        val NOTIFICATION: String = "NODE_NOTIFICATION_FUNDCHANNEL"
        val PATTERN = "peer_out WIRE_FUNDING_LOCKED"
    }

    override fun doReceive(context: Context, information: String) {
        if (information.contains(PATTERN)) {
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "New channel founded")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            context.runOnUiThread {
                UI.notification(context, "New channel founded", "")
            }
        }
    }
}
