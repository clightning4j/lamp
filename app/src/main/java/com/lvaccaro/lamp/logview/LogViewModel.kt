package com.lvaccaro.lamp.logview

import android.content.Intent
import android.content.pm.PackageManager
import android.os.FileObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.lvaccaro.lamp.utils.UI
import kotlinx.coroutines.*
import java.io.File
import java.io.LineNumberReader

/**
 * @author https://github.com/vincenzopalazzo
 */
class LogViewModel : ViewModel() {

    internal var daemon = MutableLiveData<String>("lightningd")
    private lateinit var logObserver: FileLogObserver
    private lateinit var logReader: LineNumberReader
    private lateinit var rootPath: File
    internal var lastResult = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        if (this::logObserver.isInitialized)
            logObserver.stopWatching()
        viewModelScope.cancel()
    }

    internal fun onStartToReadLogFile(path: File) {
        this.rootPath = path
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path, "${daemon.value}.log")
            if (!file.exists()) {
                viewModelScope.launch(Dispatchers.Main){
                    lastResult.value = "Log file not found"
                }
                return@launch
            }
            logReader = LineNumberReader(file.reader())

            logObserver = FileLogObserver(path, "${daemon.value}.log", this@LogViewModel)
            logObserver.startWatching()
            readLog()
        }
    }

    private fun readLog() {
        while(readByStep());
    }

    private fun readByStep(): Boolean {
        val line: String = logReader.readLine() ?: return false
        viewModelScope.launch{
            // with log level to IO, esplora generate a lot of log like hex string
            //This don't have send for the user, and also we need to resolve this
            if (line.length > 700) return@launch
            lastResult.value = line.plus("\n")
        }
        return true
    }

    fun onShareLogContent( context: AppCompatActivity, shareIntent: Intent, packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            shareIntent.apply {
                type = "text/plain"
                val body = StringBuilder()
                body.append("------- LOG ${daemon.value}.log CONTENT ----------").append("\n")
                val file = File(rootPath, "${daemon.value}.log")
                val localReader = LineNumberReader(file.reader())
                val lines = localReader.readLines()
                val sizeNow = lines.size
                val difference = 0
                if (sizeNow > 450) sizeNow - 200
                for (at in difference until sizeNow) {
                    val line = lines[at]
                    body.append(line).append("\n")
                }
                putExtra(Intent.EXTRA_TEXT, body.toString())
            }
            if (shareIntent.resolveActivity(packageManager) != null) {
                context.startActivity(Intent.createChooser(shareIntent, null))
                return@launch
            }
            viewModelScope.launch {
                UI.toast(context, "Intent resolving error")
            }
        }
    }

    fun onStartObserverLogFile(){
        viewModelScope.launch(Dispatchers.IO) {
            readLog()
        }
    }

    //Getter and Setter methods
    fun setLogDaemon(nameDaemon: String) {
        this.daemon.value = nameDaemon
        this.onStartObserverLogFile()
    }

    private class FileLogObserver(root: File, val nameFile: String, val viewModel: LogViewModel) :
        FileObserver(root.absolutePath) {
        override fun onEvent(event: Int, path: String?) {
            if (path == null) return
            if (path == nameFile) {
                when (event) {
                    MODIFY -> seeChange()
                }
            }
        }

        private fun seeChange() {
            viewModel.onStartObserverLogFile()
        }
    }
}