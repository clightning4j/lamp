package com.lvaccaro.lamp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.lvaccaro.lamp.Channels.FundChannelFragment
import com.lvaccaro.lamp.Services.CLightningException
import com.lvaccaro.lamp.util.LampKeys
import com.lvaccaro.lamp.util.Validator
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
                resultCommand = runCommandCLightning(LampKeys.DECODEPAY_COMMAND, arrayOf(text))
                runOnUiThread { showDecodePay(text, resultCommand.toString()) }
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
                showToastMessage(
                    message,
                    Toast.LENGTH_LONG
                )
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
            showToastMessage(answer[LampKeys.MESSAGE_JSON_KEY].toString(), Toast.LENGTH_LONG)
            throw CLightningException(ex.cause)
        }
    }

    private fun showDecodePay(bolt11: String, decoded: String) {
        AlertDialog.Builder(this)
            .setTitle("decodepay")
            .setMessage(decoded.toString())
            .setCancelable(true)
            .setPositiveButton("pay") { _, _ ->
                // Pay invoice
                try {
                    cli.exec(this, arrayOf("pay", bolt11), true)
                        .toJSONObject()
                    runOnUiThread {
                        showToastMessage("Invoice paid", Toast.LENGTH_LONG)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Error")
                            .setMessage(e.localizedMessage)
                            .show()
                    }
                }
            }
            .setNegativeButton("cancel") { _, _ -> }
            .show()
    }

    private fun showConnect(id: String) {
        AlertDialog.Builder(this)
            .setTitle("connect")
            .setMessage(id)
            .setPositiveButton("fund channel") { _, _ ->
                // Open fund channel fragment
                val bottomSheetDialog = FundChannelFragment()
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
            showToastMessage(networkCheck, Toast.LENGTH_LONG)
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

    protected fun showToastMessage(message: String, duration: Int) {
        if(message.isEmpty()) return
        Toast.makeText(
            this, message,
            duration
        ).show()
    }

    protected fun showSnackBar(message: String, duration: Int){
        Snackbar.make(findViewById(android.R.id.content), message, duration).show()
    }
}