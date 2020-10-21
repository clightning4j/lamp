package com.lvaccaro.lamp.activities

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.UI
import com.lvaccaro.lamp.utils.Validator
import org.jetbrains.anko.doAsync
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
            doAsync {
                var displayMessage = ""
                try{
                    val text = editText.text.toString()
                    if (text.isNotEmpty() && Validator.isBolt11(text)) {
                        cli.exec(applicationContext, arrayOf("pay", text)) // 0.9.0 Have some problem with the pay command
                        displayMessage = "Payed invoice"
                    }
                } catch (ex: Exception) {
                    val errorMessage = JSONObject(ex.localizedMessage)
                    displayMessage = errorMessage["message"].toString()
                }finally {
                    if(displayMessage.isNotEmpty()){
                        runOnUiThread{
                            UI.snackbar(this@PayViewActivity, displayMessage)
                        }
                    }

                }
            }
        }

        this.findViewById<MaterialButton>(R.id.copy_button).setOnClickListener {
            val text = copyFromClipboard()
            if (text.isNotEmpty() && Validator.isBolt11(text)) {
                doAsync {
                    val decodePay = cli.exec(this@PayViewActivity, arrayOf("decodepay", text), true).toJSONObject()
                    runOnUiThread {
                        findViewById<TextView>(R.id.amount_edit_text).text =
                            decodePay["msatoshi"].toString() ?: "0"
                    }
                }
            } else if (!Validator.isBolt11(text)) {
                UI.snackbar(this, "Text not valid")
            }
        }
    }

    private fun copyFromClipboard(): String {
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            Log.d(TAG,"Clipboard content: " + clipboard.primaryClip.getItemAt(0).text
            )
            editText.setText(clipboard.primaryClip.getItemAt(0).text)
            return editText.text.toString()
        }
        return ""
    }
}