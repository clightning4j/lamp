package com.lvaccaro.lamp

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_log.*
import java.io.File
import java.util.*

class LogActivity : AppCompatActivity() {

    lateinit var logTimer: Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        val logFile = File(rootDir(),"log")
        if (!logFile.exists()) {
            Toast.makeText(this, "No log file found", Toast.LENGTH_LONG).show()
            return
        }
        readLog()
    }

    fun readLog() {
        val logFile = File(rootDir(),"log")
        val et = findViewById<EditText>(R.id.editText)
        val logReader = logFile.bufferedReader()
        et.setMovementMethod(ScrollingMovementMethod())
        et.setText("")
        logTimer = Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread { et.append(logReader.readLine() ?: "") }
                //logReader.useLines { lines -> lines.forEach { runOnUiThread { text.append(it); log.info(it) } } }
            }
        }, 500, 100)
    }

    override fun onPause() {
        super.onPause()

    }
}
