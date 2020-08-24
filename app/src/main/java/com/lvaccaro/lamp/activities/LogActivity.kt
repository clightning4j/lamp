package com.lvaccaro.lamp.activities

import android.content.Intent
import android.net.Uri
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
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.rootDir
import com.lvaccaro.lamp.utils.UI
import kotlinx.android.synthetic.main.activity_log.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.RandomAccessFile

class LogActivity : AppCompatActivity() {

    companion object {
        val TAG = LogActivity::class.java.canonicalName
    }

    private var daemon = "lightningd"
    private val maxBufferToLoad = 200
    private var sizeBuffer = 0

    //UI component
    private lateinit var editText: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        editText = findViewById(R.id.edit_text_container_log)
        editText.apply {
            movementMethod = ScrollingMovementMethod()
            isVerticalScrollBarEnabled = true
        }
        progressBar = findViewById(R.id.loading_status)
        progressBar.max = maxBufferToLoad
        readLog()
    }

    override fun onRestart() {
        super.onRestart()
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
            R.id.action_share_log -> {
                shareLogByIntent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareLogByIntent(){
        doAsync {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                val addresses = arrayOf("vincenzopalazzodev@gmail.com")
                putExtra(Intent.EXTRA_EMAIL, addresses)
                putExtra(Intent.EXTRA_SUBJECT, "Lamp Log")
                val logFile = File(rootDir(), "$daemon.log")
                if (!logFile.exists()) {
                    runOnUiThread{
                        UI.showMessageOnToast(applicationContext, "No log file found")
                    }
                    return@doAsync
                }
                val body = StringBuilder()
                body.append("PERSONAL TEXT").append("\n\n\n")
                body.append("------- LOG $daemon.log CONTENT ----------").append("\n")
                val lines = logFile.readLines()
                val totSize = lines.size
                var startLine = 0
                if(totSize > 450){
                    startLine = totSize - 200
                }
                for(at in startLine..totSize - 1){
                    val line = lines[at]
                    body.append(line).append("\n")
                }
                putExtra(Intent.EXTRA_TEXT, body.toString())
            }
            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, "Send email..."))
                return@doAsync
            }
            runOnUiThread{
                UI.showMessageOnToast(applicationContext, "Intent resolving error")
            }
        }
    }

    private fun readLog() {
        title = "Log $daemon"
        val logFile = File(rootDir(), "$daemon.log")
        if (!logFile.exists()) {
            UI.showMessageOnToast(this, "No log file found")
            return
        }
        editText.setText("")
        doAsync {
            runOnUiThread {
                Toast.makeText(this@LogActivity, "Loading", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.VISIBLE
            }
            val randomAccessFile = RandomAccessFile(logFile, "r")
            read(randomAccessFile, editText)
        }
    }

    private fun read(randomAccessFile: RandomAccessFile, et: EditText) {
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
