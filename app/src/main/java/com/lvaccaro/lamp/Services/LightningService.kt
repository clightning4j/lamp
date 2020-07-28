package com.lvaccaro.lamp.Services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import androidx.preference.PreferenceManager
import com.lvaccaro.lamp.MainActivity
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.rootDir
import com.lvaccaro.lamp.util.LogObserver
import java.io.File
import java.util.logging.Logger

class LightningService : IntentService("LightningService") {

    val log = Logger.getLogger(LightningService::class.java.name)
    var process: Process? = null
    var globber: Globber? = null
    val NOTIFICATION_ID = 573948
    val daemon = "lightningd"
    lateinit var logObserver: FileObserver

    override fun onHandleIntent(workIntent: Intent?) {
        val dataString = workIntent!!.dataString
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        log.info("start $daemon service")
        val lightningDir = File(rootDir(), ".lightning")
        val bitcoinDir = File(rootDir(), ".bitcoin")
        val binaryDir = rootDir()
        if (!lightningDir.exists()) {
            lightningDir.mkdir()
        }
        if (!bitcoinDir.exists()) {
            bitcoinDir.mkdir()
        }

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val network = sharedPref.getString("network", "testnet").toString()
        val logLevel = sharedPref.getString("log-level", "io").toString()
        val rpcuser = sharedPref.getString("bitcoin-rpcuser", "").toString()
        val rpcpassword = sharedPref.getString("bitcoin-rpcpassword", "").toString()
        val rpcconnect = sharedPref.getString("bitcoin-rpcconnect", "127.0.0.1").toString()
        val rpcport = sharedPref.getString("bitcoin-rpcport", "9753").toString()
        var proxy = sharedPref.getString("proxy", "").toString()
        var announceaddr = sharedPref.getString("announce-addr", "").toString()
        var bindaddr = sharedPref.getString("bind-addr", "").toString()
        var addr = sharedPref.getString("addr", "").toString()
        val alias = sharedPref.getString("alias", "").toString()

        var options = arrayListOf<String>(
            String.format("%s/%s", binaryDir.canonicalPath, daemon),
            String.format("--network=%s", network),
            String.format("--log-level=%s", logLevel),
            String.format("--lightning-dir=%s", lightningDir.path),
            String.format("--plugin-dir=%s", File(binaryDir.path , "plugins").path),
            // 10 days to catch a cheating attempt
            String.format("--watchtime-blocks=%s", 10 * 24 * 6))

        if (alias.isNotEmpty()) {
            options.add(String.format("--alias=%s", alias))
        }

        if (sharedPref.getBoolean("enabled-esplora", true)) {
            val fileCert = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!, "cacert.pem")
            // set esplora plugin
            val endpoint = sharedPref.getString("esplora-api-endpoint", null) ?: "https://blockstream.info"
            options.addAll(arrayListOf<String>(
                String.format("--disable-plugin=%s", "bcli"),
                String.format("--esplora-cainfo=%s", fileCert.absolutePath),
                String.format("--esplora-api-endpoint=$endpoint/%s",
                    if ("testnet".equals(network)) "testnet/api" else "api")))
        } else {
            options.addAll(arrayListOf<String>(
                // set bitcoind rpc config
                String.format("--disable-plugin=%s", "esplora"),
                String.format("--bitcoin-datadir=%s", bitcoinDir.path),
                String.format("--bitcoin-cli=%s/bitcoin-cli", binaryDir.canonicalPath),
                String.format("--bitcoin-rpcconnect=%s", rpcconnect),
                String.format("--bitcoin-rpcuser=%s", rpcuser),
                String.format("--bitcoin-rpcport=%s", rpcport),
                String.format("--bitcoin-rpcpassword=%s", rpcpassword)))
        }

        if (sharedPref.getBoolean("enabled-tor", true)) {
            // setup Tor
            proxy = "127.0.0.1:9050"
            bindaddr = "127.0.0.1:9735"
            val torHiddenServiceDir = File(rootDir(), ".torHiddenService/hostname")
            val address = torHiddenServiceDir.readLines().first()
            announceaddr = address
            //addr = "$address:127.0.0.1:9051"
            options.add("--always-use-proxy=true")
        }

        if (proxy.isNotEmpty()) {
            options.add(String.format("--proxy=%s", proxy))
        }
        if (announceaddr.isNotEmpty()) {
            options.add(String.format("--announce-addr=%s", announceaddr))
        }
        if (bindaddr.isNotEmpty()) {
            options.add(String.format("--bind-addr=%s", bindaddr))
        }
        if (addr.isNotEmpty()) {
            options.add(String.format("--addr=%s", addr))
        }

        val pb = ProcessBuilder(options)
        pb.directory(binaryDir)
        pb.redirectErrorStream(true)
        logObserver = LogObserver(rootDir().absolutePath,"$daemon.log")
        val logFile = File(rootDir(),"$daemon.log")
        if(logFile.exists()){
            logFile.delete()
        }
        logFile.createNewFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        }
        process = pb.start()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Redirection output to file
            globber = Globber(process!!.inputStream, logFile);
            globber?.start()
        }
        //return super.onStartCommand(intent, flags, startId)
        log.info("exit $daemon service")

        startForeground()
        logObserver.startWatching();
        return Service.START_STICKY
    }

    private fun startForeground() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val notification = Notification.Builder(this)
            .setContentTitle("${getString(R.string.app_name)} is running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelName = "channel_00"
            val channel = NotificationChannel(channelName, getString(R.string.app_name), importance)
            channel.enableLights(true)
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
            notification.setChannelId(channelName)
        }

        startForeground(NOTIFICATION_ID, notification.build())
    }

    private fun cancelNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        log.info("destroying core service")
        process?.destroy()
        globber?.interrupt()
        process = null
        globber = null
        logObserver.startWatching()
        cancelNotification()
    }
}