package com.lvaccaro.lamp.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lvaccaro.lamp.fragments.FundChannelFragment
import com.lvaccaro.lamp.fragments.DecodedInvoiceFragment
import com.lvaccaro.lamp.fragments.WithdrawFragment
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.services.CLightningException
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.LampKeys
import com.lvaccaro.lamp.utils.UI
import com.lvaccaro.lamp.utils.Validator
import org.json.JSONObject
import java.lang.Exception

open class UriResultActivity() : AppCompatActivity() {

    val cli = LightningCli()
    val TAG = "UriResultActivity"

    fun parse(text: String) {
        // Check is if a Bitcoin payment
        val isBitcoinAddress = Validator.isBitcoinAddress(text)
        val isBitcoinURI = Validator.isBitcoinURL(text)
        val isBoltPayment = Validator.isBolt11(text)
        val isURINodeConnect = Validator.isLightningNodURI(text)
        var resultCommand: JSONObject? = null
        try {
            if (isBitcoinAddress) {
                val parm = HashMap<String, String>()
                Log.d(TAG, "*** Bitcoin address")
                parm.put(LampKeys.ADDRESS_KEY, text)
                runOnUiThread {
                    showWithdraw(parm)
                }
            } else if (isBitcoinURI) {
                Log.d(TAG, "*** Bitcoin URI")
                val result = Validator.doParseBitcoinURL(text)
                runOnUiThread { showWithdraw(result) }
            } else if (isBoltPayment) {
                Log.d(TAG, "*** Bolt payment")
                val bolt11 = Validator.getBolt11(text)
                runOnUiThread { showDecodePay(bolt11) }
            } else if (isURINodeConnect) {
                Log.d(TAG, "*** Node URI connect ${text}")
                resultCommand = runCommandCLightning(LampKeys.CONNECT_COMMAND, arrayOf(text))
                runOnUiThread { showConnect(resultCommand!!["id"].toString()) }
            } else {
                resultCommand = JSONObject()
                resultCommand.put("message", "No action found")
            }
        } catch (ex: CLightningException) {
            //FIXME: This have sense?
            Log.e(TAG, ex.localizedMessage)
            resultCommand = JSONObject(ex.localizedMessage)
            ex.printStackTrace()
        } finally {
            if(resultCommand == null){
                return
            }
            var message = ""
            if(resultCommand.has(LampKeys.MESSAGE_JSON_KEY)){
                message = resultCommand.get(LampKeys.MESSAGE_JSON_KEY).toString()
            }else if(resultCommand.has("id")){
                message = "Connected to node"
            }
            runOnUiThread {
                UI.toast(applicationContext, message, Toast.LENGTH_LONG)
            }
        }
    }

    fun runCommandCLightning(command: String, parameter: Array<String>): JSONObject {
        try {
            val payload = ArrayList<String>()
            payload.add(command)
            payload.addAll(parameter)
            payload.forEach { Log.d(TAG, "***** ${it}") }
            val rpcResult =
                cli.exec(this, payload.toTypedArray()).toJSONObject()
            Log.d(TAG, rpcResult.toString())
            return rpcResult
        } catch (ex: Exception) {
            //FIXME: This have sense?
            val answer = JSONObject(ex.localizedMessage)
            runOnUiThread {
                UI.toast(applicationContext, answer[LampKeys.MESSAGE_JSON_KEY].toString(), Toast.LENGTH_LONG)
            }
            throw CLightningException(ex.cause)
        }
    }

    private fun showDecodePay(bolt11: String) {
        val bundle = Bundle()
        bundle.putString("bolt11", bolt11)
        val fragment = DecodedInvoiceFragment()
        fragment.arguments = bundle
        fragment.show(supportFragmentManager, "DecodedInvoiceFragment")
    }

    private fun showConnect(id: String) {
        AlertDialog.Builder(this)
            .setTitle("connect")
            .setMessage(id)
            .setPositiveButton("fund channel") { _, _ ->
                // Open fund channel fragment
                val bottomSheetDialog =
                    FundChannelFragment()
                val args = Bundle()
                args.putString("uri", id)
                bottomSheetDialog.arguments = args
                bottomSheetDialog.show(supportFragmentManager, "Fund channel")
            }
            .setNegativeButton("cancel") { _, _ -> }
            .show()
    }

    private fun showWithdraw(param: HashMap<String, String>?) {
        val bottomSheetDialog = WithdrawFragment()
        val bundle = Bundle()
        val address = param?.get(LampKeys.ADDRESS_KEY) ?: ""
        val networkCheck = Validator.isCorrectNetwork(cli, this.applicationContext, address)
        if(networkCheck != null){
            runOnUiThread {
                UI.toast(applicationContext, networkCheck, Toast.LENGTH_LONG)
            }
            return
        }
        var amount = ""
        if(param!!.contains(LampKeys.AMOUNT_KEY)){
            //FIXME(vincenzopalazzo): create a converted class to set the set the correct ammounet.
            //For instance, Validator.toMilliSatoshi()
            amount = (param!![LampKeys.AMOUNT_KEY]!!.toDouble() * 100000000).toLong().toString()
        }
        bundle.putString(LampKeys.ADDRESS_KEY, address)
        bundle.putString(LampKeys.AMOUNT_KEY, amount)
        bottomSheetDialog.arguments = bundle
        bottomSheetDialog.show(supportFragmentManager, "WithdrawFragment")
    }

}
