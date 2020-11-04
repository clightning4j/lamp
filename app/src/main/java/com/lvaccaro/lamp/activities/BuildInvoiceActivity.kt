package com.lvaccaro.lamp.activities

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.UI
import kotlinx.android.synthetic.main.activity_build_invoice.*
import kotlinx.android.synthetic.main.list_tx.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class BuildInvoiceActivity : AppCompatActivity() {

    private val cli = LightningCli()
    private var decoded: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_build_invoice)
        balanceText.requestFocus()

        floatingActionButton.setOnClickListener {
            val amount = balanceText.text.toString()
            var sat = if (amount.isEmpty()) "any" else (amount.toDouble() * 1000).toLong().toString()
            doAsync { invoice(sat, labelText.text.toString(), descriptionText.text.toString()) }
        }
    }

    fun invoice(amount: String, label: String, description: String) {
        try {
            val res = cli.exec(this,
                arrayOf(
                    "invoice",
                    amount,
                    label,
                    description
                ), true
            ).toJSONObject()
            runOnUiThread { showInvoice(res["bolt11"] as String) }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun showInvoice(bolt11: String) {
        balanceText.isEnabled = false
        labelText.isEnabled = false
        descriptionText.isEnabled = false
        floatingActionButton.visibility = View.GONE
        labelText.visibility = View.GONE
        copyShareLayout.visibility = View.VISIBLE

        copyButton.setOnClickListener { UI.copyToClipboard(this, "bolt11", bolt11) }
        shareButton.setOnClickListener { UI.share(this, "bolt11", bolt11) }

        // hide keyboard
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(contentView?.windowToken, 0)

        // show bolt11
        val qr = UI.getQrCode(bolt11)
        qrImage.setImageBitmap(qr)

        // get expired time
        doAsync { decodeInvoice(bolt11) }
    }

    private fun decodeInvoice(bolt11: String) {
        val res = cli.exec(this, arrayOf("decodepay", bolt11), true)
            .toJSONObject()
        decoded = res
        val created_at = res["created_at"] as Int
        val expiry = res["expiry"] as Int
        val date = Date(created_at * 1000L + expiry)
        runOnUiThread {
            expiredTitle.visibility = View.VISIBLE
            expiredText.visibility = View.VISIBLE
            expiredText.text = SimpleDateFormat("HH:mm:ss, dd MMM yyyy").format(date)
        }
    }
}
