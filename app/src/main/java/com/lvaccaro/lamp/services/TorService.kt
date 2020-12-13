package com.lvaccaro.lamp.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lvaccaro.lamp.MainActivity
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.rootDir
import java.io.File
import java.lang.reflect.Field
import java.util.logging.Logger


class TorService : IntentService("TorService") {

    val log = Logger.getLogger(TorService::class.java.name)
    companion object{
        val NOTIFICATION_ID = 432432
    }
    val daemon = "tor"
    var process: Process? = null
    var globber: Globber? = null

    override fun onHandleIntent(workIntent: Intent?) {
        val dataString = workIntent!!.dataString
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        log.info("start $daemon service")
        val torDir = File(rootDir(), ".tor")
        val torHiddenServiceDir = File(rootDir(), ".torHiddenService")
        val  binaryDir = rootDir()
        if (!torDir.exists()) {
            torDir.mkdir()
        }
        if (!torHiddenServiceDir.exists()) {
            torHiddenServiceDir.mkdir()
        }

        val options = arrayListOf<String>(
            String.format("%s/%s", binaryDir.canonicalPath, daemon),
            "SafeSocks", "1",
            "SocksPort", "9050",
            "NoExec", "1",
            "ControlPort", "9051",
            "DataDirectory", torDir.path,
            "HiddenServiceDir", torHiddenServiceDir.path,
            "HiddenServicePort", String.format("%d %s:%d", 9735, "127.0.0.1", 9735)
        )

        val pb = ProcessBuilder(options)
        pb.directory(binaryDir)
        pb.redirectErrorStream(true)
        val logFile = File(rootDir(), "$daemon.log")
        logFile.delete()
        logFile.createNewFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        }
        process = pb.start()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Redirection output to file
            globber = Globber(
                process!!.inputStream,
                logFile
            );
            globber?.start()
        }

        //return super.onStartCommand(intent, flags, startId)
        log.info("exit $daemon service")

        startForeground()
        return Service.START_STICKY
    }

    fun startForeground() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this,
            NOTIFICATION_ID, intent, PendingIntent.FLAG_ONE_SHOT)

        val notification = Notification.Builder(this)
            .setContentTitle("$daemon is running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_tor)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelName = "channel_01"
            val channel = NotificationChannel(channelName, "$daemon", importance)
            channel.enableLights(true)
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
            notification.setChannelId(channelName)
        }

        startForeground(NOTIFICATION_ID, notification.build())
    }

    private fun cancelNotification() {
        Log.d(this::class.java.canonicalName, "******** Cancel notification called ********")
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        log.info("destroying $daemon service")
        if (process != null) {
            android.os.Process.sendSignal(getPid(process!!), 15)
        }
        process?.destroy()
        globber?.interrupt()
        process = null
        globber = null
        cancelNotification()
    }

    fun getPid(p: Process): Int {
        var pid = -1
        try {
            val f: Field = p::class.java.getDeclaredField("pid")
            f.setAccessible(true)
            pid = f.getInt(p)
            f.setAccessible(false)
        } catch (e: Throwable) {
        }
        return pid
    }
}
