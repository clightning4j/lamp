package com.lvaccaro.lamp.activities

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.adapters.Balance
import com.lvaccaro.lamp.adapters.BalanceAdapter
import com.lvaccaro.lamp.adapters.BalanceClickListener
import com.lvaccaro.lamp.adapters.HashMapAdapter
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.UI
import com.lvaccaro.lamp.utils.Validator
import kotlinx.android.synthetic.main.activity_send.*
import org.jetbrains.anko.doAsync
import org.json.JSONObject


class SendActivity : AppCompatActivity(), BalanceClickListener {

    private val cli = LightningCli()
    private val REQUEST_SCAN = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            itemDecoration.setDrawable(getDrawable(R.drawable.divider)!!)
            addItemDecoration(itemDecoration)
        }

        val bolt11 = intent.getStringExtra("bolt11")
        if (bolt11 != null && bolt11.isNotEmpty() && Validator.isBolt11(bolt11)) {
            doAsync { decode(bolt11) }
            return
        }

        recyclerView.adapter = BalanceAdapter(
            arrayListOf(
                Balance("Scan QR", "", ""),
                Balance("Paste from clipboard", "", ""),
                Balance("Manual typing", "", "")
            ), this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SCAN && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("text")
            if (text == null || text.isEmpty() || !Validator.isBolt11(text))
                UI.snackBar(this, "No valid content found")
            else
                doAsync { decode(text) }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun decode(bolt11: String) {
        try {
            val res = cli.exec(this, arrayOf("decodepay", bolt11), true)
                .toJSONObject()
            runOnUiThread { showInvoice(bolt11, res) }
        } catch (e: Exception) {
            runOnUiThread { UI.textAlertDialog(this, "Error", e.localizedMessage) }
        }
    }

    private fun route(peer: String, msatoshi: String) {
        try {
            val res = cli.exec(this, arrayOf("getroute", peer, msatoshi, "10"), true)
                .toJSONObject()
            runOnUiThread { UI.textAlertDialog(this, "Error", res.toString()) }
        } catch (ex: Exception) {
            val errorMessage = JSONObject(ex.localizedMessage)
            runOnUiThread { UI.textAlertDialog(this, "Error", errorMessage["message"].toString()) }
        }
    }

    private fun pay(bolt11: String, msatoshi: String?) {
        try {
            val command = arrayListOf("pay", bolt11)
            if (msatoshi != null)
                command.add(msatoshi)
            var params: Array<String> = command.toArray(arrayOf<String>());
            cli.exec(this@SendActivity, params, true)
            runOnUiThread {
                UI.snackBar(this@SendActivity, "Payed invoice")
                finish()
            }
        } catch (ex: Exception) {
            val errorMessage = JSONObject(ex.localizedMessage)
            runOnUiThread { UI.textAlertDialog(this, "Error", errorMessage["message"].toString()) }
        }
    }

    private fun showInvoice(bolt11: String, invoice: JSONObject) {
        if (invoice.has("msatoshi")) {
            val sat = invoice["msatoshi"].toString().toDouble() / 1000
            balanceText.setText(sat.toInt().toString())
        }
        balanceText.isEnabled = !invoice.has("msatoshi")
        routeButton.isClickable = true
        payButton.isClickable = true
        payButton.setOnClickListener {
            if (balanceText.text.isEmpty()) return@setOnClickListener
            var msatoshi: String? = null
            if (!invoice.has("msatoshi"))
                msatoshi = (balanceText.text.toString().toDouble() * 1000).toInt().toString()
            doAsync { pay(bolt11, msatoshi) }
        }
        routeButton.setOnClickListener {
            if (balanceText.text.isEmpty()) return@setOnClickListener
            val msatoshi = balanceText.text.toString().toDouble() * 1000
            doAsync { route(invoice["payee"].toString(), msatoshi.toInt().toString()) }
        }
        recyclerView.adapter = HashMapAdapter(HashMapAdapter.from(invoice))
    }

    override fun invoke(p1: Int) {
        if (p1 == 0)
            startActivityForResult(Intent(this, ScanActivity::class.java), REQUEST_SCAN)
        else if (p1 == 1) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) {
                UI.snackBar(this, "No content found")
                return
            }
            val text = clipboard.primaryClip.getItemAt(0).text.toString()
            if (text == null || text.isEmpty() || !Validator.isBolt11(text))
                UI.snackBar(this, "No valid content found")
            else
                doAsync { decode(text) }
        } else {
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
                .setTitle("Send to").setMessage("Lightning invoice")
            val input = EditText(this)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.layoutParams = lp
            alertDialog.setView(input)
            alertDialog.setPositiveButton( android.R.string.ok ) { dialog, which ->
                val text = input.text.toString()
                if (text.isEmpty() || !Validator.isBolt11(text))
                    UI.snackBar(this, "No valid content found")
                else
                    doAsync { decode(text) }
            }
            alertDialog.setNegativeButton( android.R.string.cancel ) { dialog, which -> dialog.cancel() }
            alertDialog.show()
        }
    }
}