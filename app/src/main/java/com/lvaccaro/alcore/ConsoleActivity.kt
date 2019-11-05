package com.lvaccaro.alcore

import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.method.KeyListener
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_log.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.math.log

class ConsoleActivity : AppCompatActivity() {

    lateinit var text: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val editText = findViewById<EditText>(R.id.editText)
        text = findViewById<EditText>(R.id.text)
        editText.hint = "help"

        findViewById<ImageButton>(R.id.send).setOnClickListener {
            val text = editText.text.toString()
            if (text != "") {
                CommandTask().execute(text)
            }
        }
    }

    inner class CommandTask : AsyncTask<String, Int, String>() {

        lateinit var params: String
        override fun onPreExecute() {
            super.onPreExecute()
            editText.setText("")
        }

        override fun doInBackground(vararg params: String): String {
            this.params = params[0]
            val args = params[0].split(" ").toTypedArray()
            try {
                return LightningCli().exec(this@ConsoleActivity, args, false).toString()
            }catch (e: Exception) {
                e.printStackTrace()
                return e.localizedMessage ?: "Error, params: ${args}"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            text.append("$ lightning-cli $params\n")
            text.append(result ?: "")
            text.append("\n")
        }
    }
}
