package com.lvaccaro.lamp.util.hendler

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NewChannelPayment(val actionName: String): IEventHandler{

    companion object{
        val TAG = NewChannelPayment::class.java.canonicalName
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contains("DEBUG wallet: Owning output")){
            Log.e(TAG, "****** Action received ${actionName} ******")
            val intent = Intent()
            intent.action = actionName
            intent.putExtra("message", "Payment sent")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

}