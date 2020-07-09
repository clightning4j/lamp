package com.lvaccaro.lamp

import android.os.AsyncTask
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_log.*
import java.lang.Exception

class ConsoleActivity : AppCompatActivity() {

    lateinit var text: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val editText = findViewById<EditText>(R.id.editText)
        text = findViewById<EditText>(R.id.text)

        findViewById<ImageButton>(R.id.send).setOnClickListener {
            val text = editText.text.toString()
            if (text != "") {
                if(text.equals("clean", true)){
                    //Command to clean console
                    this.text.setText("")
                    this.editText.setText("")
                }else{
                    CommandTask().execute(text)
                }
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
                return LightningCli().exec(this@ConsoleActivity, args, true).toText()
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
