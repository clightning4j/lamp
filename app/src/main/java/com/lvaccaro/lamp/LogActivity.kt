package com.lvaccaro.lamp

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_log.*
import org.jetbrains.anko.doAsync
import java.io.BufferedReader
import java.io.File
import java.util.*

class LogActivity : AppCompatActivity() {

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
        et.movementMethod = ScrollingMovementMethod()
        et.isVerticalScrollBarEnabled = true
        et.setText("")
        val logReader = logFile.bufferedReader()
        doAsync { read(logReader, et) }
    }

    fun read(logReader: BufferedReader, et: EditText) {
        val text = read100(logReader)
        runOnUiThread { et.append(text) }
        Thread.sleep(1000)
        read(logReader, et)
    }

    fun read100(logReader: BufferedReader): String {
        var sb = StringBuilder()
        var line = logReader.readLine()
        var counter = 0
        while (line != null && counter++ < 100) {
            line = logReader.readLine()
            sb.append(line)
        }
        return sb.toString()
    }
}
