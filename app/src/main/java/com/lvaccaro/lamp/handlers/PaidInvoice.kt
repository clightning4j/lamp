package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lvaccaro.lamp.utils.UI
import org.jetbrains.anko.runOnUiThread

class PaidInvoice : IEventHandler {

    companion object {
        val TAG = NewChannelPayment::class.java.canonicalName
        val NOTIFICATION: String = "NODE_NOTIFICATION_PAID_INVOICE"
        val PATTERN = "lightningd: Resolved invoice"
    }

    override fun doReceive(context: Context, information: String) {
        if (information.contains(PATTERN)) {
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "Paid invoice")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            context.runOnUiThread {
                UI.notification(context, "Paid invoice", "")
                UI.toast(context, "Paid invoice")
            }
        }
    }
}
