package com.lvaccaro.lamp

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

import kotlinx.android.synthetic.main.activity_log.*
import org.jetbrains.anko.doAsync
import java.io.BufferedReader
import java.io.File
import java.util.*

class LogActivity : AppCompatActivity() {

    var daemon = "lightningd"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        readLog()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_lightning -> {
                daemon = "lightningd"
                readLog()
                true
            }
            R.id.action_tor -> {
                daemon = "tor"
                readLog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun readLog() {
        val logFile = File(rootDir(),"$daemon.log")
        if (!logFile.exists()) {
            Toast.makeText(this, "No log file found", Toast.LENGTH_LONG).show()
            return
        }
        val et = findViewById<EditText>(R.id.editText)
        et.movementMethod = ScrollingMovementMethod()
        et.isVerticalScrollBarEnabled = true
        et.setText("")
        val logReader = logFile.bufferedReader()
        doAsync {
            val text = read100(logReader)
            runOnUiThread { et.append(text) }
        }
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
