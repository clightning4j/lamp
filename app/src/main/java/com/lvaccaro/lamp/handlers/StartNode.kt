package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * @author https://github.com/vincenzopalazzo
 */
class StartNode: IEventHandler{

    companion object{
        val TAG = StartNode::class.java.canonicalName
        const val NOTIFICATION: String = "NODE_NOTIFICATION_STARTUP"
        const val PATTERN = "lightningd: Server started with public key"
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contains(PATTERN)){
            Log.d(TAG, "****** Action received ${NOTIFICATION} ******")
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "Lightning startup")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

}