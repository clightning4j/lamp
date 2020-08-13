package com.lvaccaro.lamp.view.pay

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.util.Validator
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.contentView
import org.json.JSONObject
import java.lang.Exception

class PayViewActivity : AppCompatActivity() {

    companion object {
        private val TAG = PayViewActivity::class.java.canonicalName
    }

    private val cli = LightningCli()

    private lateinit var editText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_view)
        editText = findViewById(R.id.content_text)
        this.findViewById<MaterialButton>(R.id.pay_button).setOnClickListener {
            val text = editText.text.toString()
            try {
                if (text.isNotEmpty() && Validator.isBolt11(text)) {
                    cli.exec(this, arrayOf("pay", text)) // 0.9.0 Have some problem with the pay command
                    showSnackBarMessage("Payed invoice")
                }
            } catch (ex: Exception) {
                val errorMessage = JSONObject(ex.localizedMessage)
                showSnackBarMessage(errorMessage["message"].toString())
            }
        }

        this.findViewById<MaterialButton>(R.id.copy_button).setOnClickListener {
            val text = copyFromClipboard()
            if(text.isNotEmpty() && Validator.isBolt11(text)){
                val decodePay = cli.exec(this, arrayOf("decodepay", text), true).toJSONObject()
                findViewById<TextView>(R.id.amount_edit_text).setText(decodePay["msatoshi"].toString() ?: "0")
            }else if(!Validator.isBolt11(text)){
                showSnackBarMessage("Text not valid")
            }
        }
    }

    private fun copyFromClipboard(): String {
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            Log.d(
                TAG,
                "Clipboard content: " + clipboard.primaryClip.getItemAt(0).text
            )
            editText.setText(clipboard.primaryClip.getItemAt(0).text)
            return editText.text.toString()
        }
        return ""
    }

    private fun showSnackBarMessage(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(contentView?.rootView!!, message, duration).show()
    }
}