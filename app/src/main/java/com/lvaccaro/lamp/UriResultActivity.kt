package com.lvaccaro.lamp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lvaccaro.lamp.Channels.FundChannelFragment
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
        lateinit var resultCommand: String
        if (isBitcoinAddress) {
            Log.d(TAG, "*** bitcoin address")
            resultCommand = runCommandCLightning(LampKeys.WITHDRAW_COMMAND, text)
            runOnUiThread { showWithdraw(resultCommand) }
        } else if (isBitcoinURI) {
            Log.d(TAG, "*** Bitcoin URI")
            val result = Validator.doParseBitcoinURL(text)
            resultCommand = runCommandCLightning(
                LampKeys.WITHDRAW_COMMAND,
                result[LampKeys.ADDRESS_KEY].toString(),
                result[LampKeys.AMOUNT_KEY].toString()
            )
            //TODO create the method to see this element
        } else if (isBoltPayment) {
            Log.d(TAG, "*** Bolt payment")
            resultCommand = runCommandCLightning(LampKeys.DECODEPAY_COMMAND, text)
            runOnUiThread { showDecodePay(text, resultCommand) }
        } else if (isURINodeConnect) {
            Log.d(TAG, "*** Node URI connect")
            resultCommand = runCommandCLightning(LampKeys.CONNECT_COMMAND, text)
            runOnUiThread { showConnect(resultCommand) }
        } else {
            resultCommand = "No action found"
        }

        runOnUiThread {
            showToastMessage(
                resultCommand,
                Toast.LENGTH_LONG
            )
        }

/*
        try {
            val res = cli.exec(this, arrayOf("decodepay", text), true).toText()
            runOnUiThread { showDecodePay(text, res) }
            return
        } catch (e: Exception) {
            Log.d(TAG, "decodepay: ${e.localizedMessage}")
        }

        try {
            val res = cli.exec(this, arrayOf("connect", text), true).toJSONObject()
            runOnUiThread { showConnect(res["id"] as String) }
            return
        } catch (e: Exception) {
            Log.d(TAG, "connect: ${e.localizedMessage}")
        }

        //TODO(vincenzopalazzo) this function not work with the URI like the string below
        //bitcoin:tb1qzy5mqyqpl6p67x8psu2phxwp0e7lz79w47e5ad
        try {
            // pre-parsing text to avoid multi strings text
            val address = text.split(" ").first()
            cli.exec(this, arrayOf("withdraw", address), true).toJSONObject()
            return
        } catch (e: Exception) {
            val res = JSONObject(e.localizedMessage)
            val message = res["message"] as String
            // withdraw fails due by missing satoshi field
            if (message == "missing required parameter: satoshi") {
                runOnUiThread { showWithdraw(text) }
                return
            }
            Log.d(TAG, "withdraw: ${e.localizedMessage}")
        }*/
    }

    fun runCommandCLightning(command: String, vararg parameter: String): String {
        try {
            val rpcResult =
                cli.exec(this, arrayOf(command, parameter.toString()), true).toJSONObject()
            return rpcResult.toString()
        } catch (ex: Exception) {
            val answer = JSONObject(ex.localizedMessage)
            showToastMessage(answer["massage"].toString(), Toast.LENGTH_LONG)
            return answer["message"].toString()
        }
    }

    fun showToastMessage(message: String, duration: Int) {
        Toast.makeText(
            this, message,
            duration
        ).show()
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

    private fun showWithdraw(address: String?) {
        val bottomSheetDialog = WithdrawFragment()
        val bundle = Bundle()
        bundle.putString("address", address ?: "")
        bottomSheetDialog.arguments = bundle
        bottomSheetDialog.show(supportFragmentManager, "WithdrawFragment")
    }
}