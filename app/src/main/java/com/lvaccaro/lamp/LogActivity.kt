package com.lvaccaro.lamp

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.android.synthetic.main.list_channel.*
import kotlinx.android.synthetic.main.list_channel.view.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.RandomAccessFile
import java.lang.StringBuilder

class LogActivity : AppCompatActivity() {

    companion object {
        val TAG = LogActivity::class.java.canonicalName
    }

    private var daemon = "lightningd"
    private val maxBufferToLoad = 200
    private var sizeBuffer = 0

    //UI component
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressBar = findViewById(R.id.loading_status)
        progressBar.max = maxBufferToLoad
    }

    override fun onResume() {
        super.onResume()
        readLog()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)
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

    private fun readLog() {
        title = "Log $daemon"
        val logFile = File(rootDir(), "$daemon.log")
        if (!logFile.exists()) {
            Toast.makeText(this, "No log file found", Toast.LENGTH_LONG).show()
            return
        }
        val et = findViewById<EditText>(R.id.editText)
        et.movementMethod = ScrollingMovementMethod()
        et.isVerticalScrollBarEnabled = true
        et.setText("")
        doAsync {
            runOnUiThread {
                Toast.makeText(this@LogActivity, "Loading", Toast.LENGTH_SHORT).show()
            }
            val randomAccessFile = RandomAccessFile(logFile, "r")
            read(randomAccessFile, et)
        }
    }

    private fun read(randomAccessFile: RandomAccessFile, et: EditText) {
        progressBar.visibility = View.VISIBLE
        Log.d(TAG, "Start to read the file with RandomAccessFile")
        //Set the position at the end of the file
        val fileSize = randomAccessFile.length() - 1
        randomAccessFile.seek(fileSize)
        //The maximum dimension of this object is one line
        val lineBuilder = StringBuilder()
        //This contains the each line of the logger, the line of the logger are fixed
        //to the propriety *maxBufferToLoad*
        val logBuilder = StringBuilder()
        for (pointer in fileSize downTo 1) {
            randomAccessFile.seek(pointer)
            val character = randomAccessFile.read().toChar()
            lineBuilder.append(character)
            if (character.equals('\n', false)) {
                sizeBuffer++
                logBuilder.append(lineBuilder.reverse().toString())
                lineBuilder.clear()
                runOnUiThread {
                    this.progressBar.progress = sizeBuffer
                }
                if (sizeBuffer == maxBufferToLoad) break
            }
        }
        Log.d(TAG, "Print lines to EditText")
        val lines = logBuilder.toString().split("\n").reversed()
        runOnUiThread {
            lines.forEach {
                if (it.trim().isNotEmpty() && it.length < 400)
                    et.append(it.plus("\n"))
            }
            progressBar.visibility = View.GONE
        }
    }
}
