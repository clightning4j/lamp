package com.lvaccaro.lamp.util

import android.os.Build
import android.os.FileObserver
import android.util.Log
import com.lvaccaro.lamp.util.hendler.IEventHandler
import java.io.File
import java.io.LineNumberReader

/**
 * This class is an implementation of FileObserver discussed inside the PRs XX
 * The ideas is ideated from @lvaccaro
 *
 * Info about the clightning node
 *
 * ---- Node create a transaction (with keysend, pay)----
 * Pattern log: 2020-07-28T09:26:28.411Z DEBUG wallet: Owning output 1 89846sat (SEGWIT) txid 33c1f5d2df4f425898dc6eb49dae51aaab1d430ee7c0da2cab18123d5c1192f0
 *
 * ------ Node receive transaction to blockchain ----
 * Pattern log: No debug log
 *
 * ---- Node receive a lightning payment (keysend, pay)
 * Pattern log:
 *
 * @author https://github.com/vincenzopalazzo
 */
class LogObserver(var path: String, var nameFile: String) : FileObserver(path) {

    companion object {
        val TAG = LogObserver::class.java.canonicalName
    }

    private lateinit var actionHandler: Map<String, IEventHandler>
    private lateinit var logFile: File
    private var actualLine = 0
    private var lineNumberReader: LineNumberReader? = null

    fun initHandler() {
        //TODO init after
    }

    override fun onEvent(event: Int, file: String?) {
        if(file == null) return
        if (file?.equals(nameFile)) {
            Log.d(TAG, "********* Event inside log ${file} ******")
            when (event) {
                FileObserver.MODIFY -> readNewLines()
            }
        }
    }

    private fun readNewLines() {
        if(lineNumberReader == null)
            initFileLog()

        if(lineNumberReader == null) return
        Log.d(TAG, "***** FILE modified *******")
        Log.d(TAG, "***** Actual line: ${actualLine}")
        lineNumberReader?.lineNumber = actualLine
        var line: String? = lineNumberReader?.readLine()
        while (line != null){
            readLogLine(line)
            Log.e(TAG, line)
            line = lineNumberReader?.readLine()
        }
        Log.d(TAG, "****** New number of line ${actualLine} ********")
    }

    private fun readLogLine(line: String) {
        Log.e(TAG, "******** ACTUAL LINE ${line} ********")
        actualLine++
    }

    private fun initFileLog() {
        logFile = File(path, nameFile)
        lineNumberReader = LineNumberReader(logFile.reader())
        //FIXME: level api that are enable this line about is Android nougat
        // for the actual version of lightning_ndk I don't need to insert the check of the version
        actualLine = lineNumberReader!!.lines().count().toInt()
        Log.d(TAG, "The log file contains ${actualLine} lines")
    }
}