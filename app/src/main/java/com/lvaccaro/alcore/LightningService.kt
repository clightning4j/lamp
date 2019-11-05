package com.lvaccaro.alcore

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File
import java.util.logging.Logger

class LightningService : IntentService("LightningService") {

    val log = Logger.getLogger(LightningService::class.java.name)
    var process: Process? = null

    override fun onHandleIntent(workIntent: Intent?) {
        val dataString = workIntent!!.dataString
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        log.info("start lightningd service")
        val lightningDir = File(rootDir(), ".lightning")
        val bitcoinDir = File(rootDir(), ".bitcoin")
        val binaryDir = rootDir()
        if (!lightningDir.exists()) {
            lightningDir.mkdir()
        }
        if (!bitcoinDir.exists()) {
            bitcoinDir.mkdir()
        }

        val daemon = "lightningd"
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val network = sharedPref.getString("network", "testnet").toString()
        val logLevel = sharedPref.getString("log-level", "io").toString()
        val rpcuser = sharedPref.getString("bitcoin-rpcuser", "").toString()
        val rpcpassword = sharedPref.getString("bitcoin-rpcpassword", "").toString()
        val rpcconnect = sharedPref.getString("bitcoin-rpcconnect", "127.0.0.1").toString()
        val rpcport = sharedPref.getString("bitcoin-rpcport", "9753").toString()
        val proxy = sharedPref.getString("proxy", null)
        val announceaddr = sharedPref.getString("announce-addr", null)

        val options = arrayListOf<String>( String.format("%s/%s", binaryDir.canonicalPath, daemon),
            String.format("--network=%s", network),
            String.format("--log-level=%s", logLevel),
            String.format("--lightning-dir=%s", lightningDir.path),
            String.format("--bitcoin-cli=%s/bitcoin-cli", binaryDir.canonicalPath),
            String.format("--bitcoin-datadir=%s", bitcoinDir.path),
            String.format("--plugin-dir=%s", File(binaryDir.path , "plugins").path),
            String.format("--bitcoin-rpcconnect=%s", rpcconnect),
            String.format("--bitcoin-rpcuser=%s", rpcuser),
            String.format("--bitcoin-rpcport=%s", rpcport),
            String.format("--bitcoin-rpcpassword=%s", rpcpassword))

        if (proxy != null) {
            options.add(String.format("--proxy=%s", proxy))
        }
        if (announceaddr != null) {
            options.add(String.format("--announce-addr=%s", announceaddr))
        }

        val pb = ProcessBuilder(options)
        pb.directory(binaryDir)
        pb.redirectErrorStream(true)
        val logFile = File(rootDir(),"log")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        }
        process = pb.start()
        //return super.onStartCommand(intent, flags, startId)
        log.info("exit lightningd service")

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log.info("destroying core service")
        process?.destroy()
        process = null
    }

    fun rootDir(): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return noBackupFilesDir
        }
        return filesDir
    }
}