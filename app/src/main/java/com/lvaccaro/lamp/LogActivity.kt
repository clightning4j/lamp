package com.lvaccaro.lamp

import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_log.*
import java.io.File
import java.io.LineNumberReader
import java.util.stream.Collectors

class LogActivity : AppCompatActivity() {

    companion object {
        val TAG = LogActivity::class.java.canonicalName
    }

    private var daemon = "lightningd"

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

    fun readLog() {
        title = "Log $daemon"

        val logFile = File(rootDir(), "$daemon.log")
        Log.d(TAG, "File log: ${logFile.absolutePath}")
        Log.d(TAG, "File dim: ${logFile.length()} Mb")
        Log.d(TAG, "------------------------------------------")
        Log.d(TAG, logFile.readText())
        if (!logFile.exists()) {
            Toast.makeText(this, "No log file found", Toast.LENGTH_LONG).show()
            return
        }
        val et = findViewById<EditText>(R.id.editText)
        et.movementMethod = ScrollingMovementMethod()
        et.isVerticalScrollBarEnabled = true
        et.setText("Waiting log")

        val loadLogTask = LoadLogTask(this, et)
        loadLogTask.execute(logFile)
    }

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_LONG){
        Toast.makeText(this, message, duration).show()
    }

    private class LoadLogTask(val activity: LogActivity, val editText: EditText): AsyncTask<File, String, String>() {

        override fun doInBackground(vararg params: File?): String? {
            var text: String
            var logReader = LineNumberReader(params[0]?.reader())
            var lines: List<String> = logReader.lines().collect(Collectors.toList())
            text = readBuffer(lines)
            return text
        }

        override fun onPreExecute() {
            super.onPreExecute()
            activity.showToastMessage("Loading log")
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            editText.setText(result)
            activity.showToastMessage("Log ready")
        }

        fun readBuffer(lines: List<String>): String {
            val sb = StringBuilder()
            val linesIt = lines.iterator()
            while (linesIt.hasNext()) {
                sb.append(linesIt.next())
            }
            return sb.toString()
        }
    }
}
