package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NewChannelPayment: IEventHandler {

    companion object{
        val TAG = NewChannelPayment::class.java.canonicalName
        val NOTIFICATION: String = "NODE_NOTIFICATION_FUNDCHANNEL"
        val PATTERN_ONE = "Balance"
        val PATTERN_TWO = "->"
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contains(PATTERN_ONE) && information.contains(PATTERN_TWO)){
            Log.d(TAG, "****** Action received ${NOTIFICATION} ******")
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "Payment received") //FIXME(vincenzopalazzo): Fix the message
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

}