package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ShutdownNode: IEventHandler {

    companion object{
        val TAG = ShutdownNode::class.java.canonicalName
        val NOTIFICATION: String = "SHUTDOWN_NODE_NOTIFICATION"
        val PATTERN = "lightningd: JSON-RPC shutdown"
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contains(PATTERN)){
            Log.d(NewChannelPayment.TAG, "****** Action received ${NOTIFICATION} ******")
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "Shutdown")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

}
