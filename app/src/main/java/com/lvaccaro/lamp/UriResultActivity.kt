package com.lvaccaro.lamp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lvaccaro.lamp.Channels.FundChannelFragment
import org.json.JSONObject
import java.lang.Exception

open class UriResultActivity() : AppCompatActivity() {

    val cli = LightningCli()
    val TAG = "UriResultActivity"

    fun parse(text: String) {
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

        try {
            // pre-parsing text to avoid multi strings text
            val address = text.split(" ").first()
            cli.exec(this, arrayOf("withdraw", address), true).toJSONObject()
            return;
        } catch (e: Exception) {
            val res = JSONObject(e.localizedMessage)
            val message = res["message"] as String
            // withdraw fails due by missing satoshi field
            if (message == "missing required parameter: satoshi") {
                runOnUiThread { showWithdraw(text) }
                return
            }
            Log.d(TAG, "withdraw: ${e.localizedMessage}")
        }

        runOnUiThread {
            Toast.makeText(
                this,
                "No action found",
                Toast.LENGTH_LONG
            ).show()
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
                        Toast.makeText(
                            this,
                            "Invoice paid",
                            Toast.LENGTH_LONG
                        ).show()
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