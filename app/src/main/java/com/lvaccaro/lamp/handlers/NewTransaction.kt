package com.lvaccaro.lamp.handlers

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lvaccaro.lamp.utils.UI
import org.jetbrains.anko.runOnUiThread

class NewTransaction: IEventHandler {

    companion object{
        val TAG = NewChannelPayment::class.java.canonicalName
        val NOTIFICATION: String = "NODE_NOTIFICATION_NEW_TRANSACTION"
        val PATTERN = "wallet: Owning output"
    }

    override fun doReceive(context: Context, information: String) {
        if(information.contains(PATTERN)){
            val regex = "wallet: Owning output \\d [\\w|[^:]]+".toRegex()
            val amount = regex.find(information)?.value?.split(" ")?.get(4)
            val intent = Intent()
            intent.action = NOTIFICATION
            intent.putExtra("message", "new transaction")
            intent.putExtra("amount", amount)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            context.runOnUiThread {
                UI.notification(context, "New onchain transaction", "${amount}")
                UI.toast(context, "New onchain transaction ${amount}")
            }
        }
    }
}