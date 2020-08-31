package com.lvaccaro.lamp.utils

import android.content.Context
import android.os.FileObserver
import android.util.Log
import com.lvaccaro.lamp.handlers.*
import java.io.File
import java.io.LineNumberReader

/**
 * This class is an implementation of FileObserver discussed inside the PRs XX
 * The ideas is from @lvaccaro
 *
 * Info about the clightning node
 *
 * ---- Node create a transaction (with fundchannel or when I receive the onchain tx)----
 * Pattern log: DEBUG wallet: Owning output 1 89846sat (SEGWIT) txid 33c1f5d2df4f425898dc6eb49dae51aaab1d430ee7c0da2cab18123d5c1192f0
 * ---- Node make a withdraw action
 * Patter log: DEBUG lightningd: sendrawtransaction
 *
 * --- Node adding block
 * Pattern log: DEBUG lightningd: Adding block
 *
 * --- Node sendrawtransaction
 * Pattern log: DEBUG plugin-esplora: sendrawtx exit 0
 *
 * ---- Shutdown node with command close----
 * Pattern log: UNUSUAL lightningd: JSON-RPC shutdown
 *
 * ------ Node receive transaction to blockchain ----
 * Pattern log: No debug log
 *
 * ---- Node receive a lightning payment (keysend, pay)
 * Pattern log:
 *
 * ---- Node startup
 * Pattern log: INFO lightningd: Server started with public key
 *
 * ---- Node crash
 * Pattern log: **BROKEN**
 *
 *
 * @author https://github.com/vincenzopalazzo
 */
class LogObserver(val context: Context, val path: String, val nameFile: String) : FileObserver(path) {

    init {
        initHandler()
    }

    companion object {
        val TAG = LogObserver::class.java.canonicalName
    }

    private lateinit var actionHandler: ArrayList<IEventHandler>
    private lateinit var logFile: File
    private var actualLine = 0
    private var lineNumberReader: LineNumberReader? = null


    private fun initHandler() {
        actionHandler = ArrayList<IEventHandler>()
        actionHandler.addAll(
            arrayOf(
                NewChannelPayment(), ShutdownNode(),
                NewBlockHandler(), StartNode()
            )
        )
    }

    override fun onEvent(event: Int, file: String?) {
        if(file == null) return
        if (file == nameFile) {
            when (event) {
                MODIFY -> readNewLines()
            }
        }
    }

    private fun readNewLines() {
        if(lineNumberReader == null)
            initFileLog()

        //FIXME(vicenzopalazzo): This is real util?
        if(lineNumberReader == null) return
        lineNumberReader?.lineNumber = actualLine
        var line: String? = lineNumberReader?.readLine()
        while (line != null){
            readLogLine(line)
            Log.d(TAG, line)
            line = lineNumberReader?.readLine()
        }
    }

    private fun readLogLine(line: String) {
        actionHandler.forEach { it -> it.doReceive(context, line) }
        actualLine++
    }

    private fun initFileLog() {
        logFile = File(path, nameFile)
        lineNumberReader = LineNumberReader(logFile.reader())
        //FIXME: level api that are enable this line about is Android nougat
        // for the actual version of lightning_ndk I don't need to insert the check of the version
        actualLine = lineNumberReader!!.lines().count().toInt()
    }
}