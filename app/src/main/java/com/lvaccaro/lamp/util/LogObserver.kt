package com.lvaccaro.lamp.util

import android.content.Context
import android.os.Build
import android.os.FileObserver
import android.util.Log
import com.lvaccaro.lamp.util.hendler.IEventHandler
import com.lvaccaro.lamp.util.hendler.NewChannelPayment
import com.lvaccaro.lamp.util.hendler.ShutdownNode
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
 * Pattern log: 2020-08-03T15:38:38.812Z UNUSUAL lightningd: JSON-RPC shutdown
 *
 * ------ Node receive transaction to blockchain ----
 * Pattern log: No debug log
 *
 * ---- Node receive a lightning payment (keysend, pay)
 * Pattern log:
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


    fun initHandler() {
        actionHandler = ArrayList<IEventHandler>()
        actionHandler.add(NewChannelPayment(LampKeys.NODE_NOTIFICATION_FUNDCHANNEL))
        actionHandler.add(ShutdownNode(LampKeys.NODE_NOTIFICATION_SHUTDOWN))
    }

    override fun onEvent(event: Int, file: String?) {
        if(file == null) return
        if (file?.equals(nameFile)) {
            when (event) {
                FileObserver.MODIFY -> readNewLines()
            }
        }
    }

    private fun readNewLines() {
        if(lineNumberReader == null)
            initFileLog()

        //FIXME(vicenzopalazzo): This is real util?
        if(lineNumberReader == null) return
        Log.d(TAG, "***** Actual line: ${actualLine}")
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