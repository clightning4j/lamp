package com.lvaccaro.lamp.util.hendler

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ShutdownNode(val actionName: String): IEventHandler{

    companion object{
        val TAG = ShutdownNode.javaClass.canonicalName
        val PATTERN = "UNUSUAL lightningd: JSON-RPC shutdown"
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contentEquals(PATTERN)){
            Log.e(NewChannelPayment.TAG, "****** Action received ${actionName} ******")
            val intent = Intent()
            intent.action = actionName
            intent.putExtra("message", "Shutdown")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

}