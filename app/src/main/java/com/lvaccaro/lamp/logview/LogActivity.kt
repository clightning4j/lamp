package com.lvaccaro.lamp.logview

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.rootDir
import com.lvaccaro.lamp.utils.UI

import kotlinx.android.synthetic.main.activity_log.*

class LogActivity : AppCompatActivity() {

    private lateinit var logViewModel: LogViewModel
    private lateinit var editText: EditText
    private lateinit var containerLog: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        editText = findViewById(R.id.edit_text_container_log)
        editText.movementMethod = ScrollingMovementMethod()
        editText.isVerticalScrollBarEnabled = true
        containerLog = findViewById(R.id.container_log)
        containerLog.fullScroll(View.FOCUS_DOWN)

        logViewModel = ViewModelProvider(this).get(LogViewModel::class.java)
        logViewModel.lastResult.observe(this, Observer<String> { lastResult ->
            run {
                editText.append(lastResult.trim())
            }
        })

        logViewModel.daemon.observe(this, Observer<String> { _ ->
            run {
                editText.text.clear()
                logViewModel.onStartToReadLogFile(rootDir())
            }
        })
        logViewModel.onStartToReadLogFile(rootDir())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_lightning -> {
                UI.toast(this, "C-lightning log are loading")
                logViewModel.setLogDaemon("lightningd")
                true
            }
            R.id.action_tor -> {
                UI.toast(this,"Tor log are loading")
                logViewModel.setLogDaemon("tor")
                true
            }
            R.id.action_share_log -> {
                rootDir()
                val intent = Intent(Intent.ACTION_SEND)
                logViewModel.onShareLogContent(this, intent, packageManager)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}