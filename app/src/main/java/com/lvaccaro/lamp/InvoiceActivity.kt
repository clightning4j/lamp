package com.lvaccaro.lamp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlinx.android.synthetic.main.activity_invoice.*
import org.jetbrains.anko.doAsync
import java.text.SimpleDateFormat
import java.util.*


class InvoiceActivity : AppCompatActivity() {

    private val cli = LightningCli()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        val bolt11 = intent.getStringExtra("bolt11")
        val label = intent.getStringExtra("label")

        bolt11Text.text = bolt11
        bolt11Text.setOnClickListener { copyToClipboard("bolt11", bolt11) }

        // get qrcode
        doAsync {
            val bitmap = getQrCode(bolt11)
            runOnUiThread {
                qrcodeImage.setImageBitmap(bitmap)
            }
        }

        // get expired time
        doAsync { decodeInvoice(bolt11) }

        // wait invoice
        doAsync { waitInvoice(label) }
    }


    private fun decodeInvoice(bolt11: String) {
        val res = cli.exec(this, arrayOf("decodepay", bolt11), true)
            .toJSONObject()
        val amountMsat = res["amount_msat"] as String
        val created_at = res["created_at"] as Int
        val expiry = res["expiry"] as Int
        val date = Date(created_at * 1000L + expiry)
        runOnUiThread {
            expiredText.text = SimpleDateFormat("HH:mm:ss, dd MMM yyyy").format(date)
            title = amountMsat
        }
    }

    private fun waitInvoice(label: String) {
        try {
        cli.exec(this, arrayOf("waitinvoice", label), true).toJSONObject()
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Invoice paid",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
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


    private fun copyToClipboard(key: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(key, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show()
    }

    private fun getQrCode(text: String): Bitmap {
        val SCALE = 16
        try {
            val matrix = Encoder.encode(text, ErrorCorrectionLevel.M).getMatrix()
            val height = matrix.getHeight() * SCALE
            val width = matrix.getWidth() * SCALE
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width)
                for (y in 0 until height) {
                    val point = matrix.get(x / SCALE, y / SCALE).toInt()
                    bitmap.setPixel(x, y, if (point == 0x01) Color.BLACK else 0)
                }
            return bitmap
        } catch (e: WriterException) {
            throw RuntimeException(e)
        }
    }
}
