package com.lvaccaro.lamp.activities

import android.os.AsyncTask
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toText

import java.lang.Exception

class ConsoleActivity : AppCompatActivity() {

    private lateinit var editTextResult: EditText
    private lateinit var editTextCmd: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editTextCmd = findViewById(R.id.edit_text_console_message)
        editTextResult = findViewById(R.id.edit_text_result_command)

        findViewById<ImageButton>(R.id.send).setOnClickListener {
            val textContent = editTextCmd.text.toString()
            if (textContent != "") {
                if(textContent.equals("clean", true)){
                    //Command to clean console
                    editTextResult.setText("")
                    editTextCmd.setText("")
                }else{
                    CommandTask().execute(textContent)
                }
            }
        }
    }

    inner class CommandTask : AsyncTask<String, Int, String>() {

        lateinit var params: String
        override fun onPreExecute() {
            super.onPreExecute()
            editTextCmd.setText("")
        }

        override fun doInBackground(vararg params: String): String {
            this.params = params[0]
            val args = params[0].split(" ").toTypedArray()
            try {
                return LightningCli()
                    .exec(this@ConsoleActivity, args, true).toText()
            }catch (e: Exception) {
                e.printStackTrace()
                return e.localizedMessage ?: "Error, params: ${args}"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            editTextResult.append("$ lightning-cli $params\n")
            editTextResult.append(result ?: "")
            editTextResult.append("\n")
        }
    }
}
