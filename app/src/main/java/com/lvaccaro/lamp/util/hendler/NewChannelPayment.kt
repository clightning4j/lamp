package com.lvaccaro.lamp.util.hendler

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NewChannelPayment(val actionName: String): IEventHandler{

    companion object{
        val TAG = NewChannelPayment::class.java.canonicalName
        val PATTERN_ONE = "DEBUG wallet: Owning output"
        val PATTERN_TWO = "sendrawtx exit 0"
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contains(PATTERN_ONE) || information.contains(PATTERN_TWO)){
            Log.e(TAG, "****** Action received ${actionName} ******")
            val intent = Intent()
            intent.action = actionName
            intent.putExtra("message", "Payment sent") //FIXME(vincenzopalazzo): Fix the message
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

}