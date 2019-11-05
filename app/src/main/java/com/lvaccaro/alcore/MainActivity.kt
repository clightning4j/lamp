package com.lvaccaro.alcore

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.util.Log
import java.io.*
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import android.view.View
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.*
import android.text.format.Formatter
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.zxing.WriterException
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.lang.Exception
import java.net.NetworkInterface
import java.util.*
import java.util.logging.Logger


class MainActivity : AppCompatActivity() {

    companion object {
        var ARCH = "aarch64-linux-android"
        var PACKAGE = "lightning"
        var RELEASE = "release_0.2"
        var TAR_FILENAME = "${ARCH}-${PACKAGE}.tar.xz"
        var URL = "https://github.com/lvaccaro/clightning_ndk/releases/download/${RELEASE}/${TAR_FILENAME}"
        val WRITE_REQUEST_CODE = 101
    }

    val log = Logger.getLogger(MainActivity::class.java.name)
    val TAG = "MainActivity"
    var downloadID = 0L
    lateinit var downloadmanager: DownloadManager
    fun dir(): File { return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!! }
    fun rootDir(): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return noBackupFilesDir
        }
        return filesDir
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        downloadmanager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        registerReceiver(onDownloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_REQUEST_CODE
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    fun reload() {
        // Hide download button if package exist and is uncompressed
        val isLightningReady = File(rootDir(), "lightning-cli").exists()
        findViewById<Button>(R.id.start).visibility =
            if (isLightningReady) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.stop).visibility =
            if (isLightningReady) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.download).visibility =
            if (isLightningReady) View.GONE else View.VISIBLE

        if (!isLightningReady)
            return

        doAsync {
            Thread.sleep(3000)
            getInfo()
        }
    }

    fun getInfo() {
        // if lightning is up and running update link
        try {
            val res = LightningCli().exec(this, arrayOf("getinfo"), true)
            val json = LightningCli().toJSONObject(res)
            val id = json["id"].toString()
            val address = getWifiIPAddress() ?: getMobileIPAddress() ?: ""
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val port = sharedPref.getString("port", "9735").toString()
            val text = "${id}@${address}:${port}"
            runOnUiThread {
                findViewById<TextView>(R.id.textViewQr).text = text
                findViewById<ImageView>(R.id.qrcodeImageView).setImageBitmap(getQrCode(text))
                findViewById<ImageView>(R.id.qrcodeImageView).setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("peernode", text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show()
                }
                findViewById<TextView>(R.id.textViewQr).visibility = View.VISIBLE
                findViewById<ImageView>(R.id.qrcodeImageView).visibility = View.VISIBLE
                findViewById<Button>(R.id.start).isEnabled = false
                findViewById<Button>(R.id.stop).isEnabled = true
            }
        }catch (e: Exception) {
            log.info("---" + e.localizedMessage + "---")
            runOnUiThread {
                findViewById<TextView>(R.id.textViewQr).visibility = View.GONE
                findViewById<ImageView>(R.id.qrcodeImageView).visibility = View.GONE
                findViewById<Button>(R.id.start).isEnabled = true
                findViewById<Button>(R.id.stop).isEnabled = false
            }
        }
    }

    fun getQrCode(text: String): Bitmap {
        val SCALE = 4
        try {
            val matrix = Encoder.encode(text, ErrorCorrectionLevel.M).getMatrix()
            val height = matrix.getHeight() * SCALE
            val width = matrix.getWidth() * SCALE
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width)
                for (y in 0 until height) {
                    val point = matrix.get(x / SCALE, y / SCALE).toInt()
                    bitmap.setPixel(x, y, if (point == 0x01) Color.BLACK else 0)
                }
            return bitmap
        } catch (e: WriterException) {
            throw RuntimeException(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }
        }
    }

    private val onDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID != id)
                return

            runOnUiThread {
                findViewById<Button>(R.id.download).isEnabled = false
                Toast.makeText(this@MainActivity, "Download Completed. Uncompressing...", Toast.LENGTH_SHORT).show()
            }
            val tarFile = File(dir(), TAR_FILENAME)
            doAsync { uncompress(tarFile, rootDir()) }
        }
    }

    fun onDownload(view: View?) {
        val tarFile = File(dir(), TAR_FILENAME)
        if (tarFile.exists()) {
            // Uncompress package
            Toast.makeText(this, "Package already downloaded. Uncompressing...", Toast.LENGTH_LONG).show()
            doAsync { uncompress(tarFile, rootDir()) }
            return
        }

        // Download package
        val request = DownloadManager.Request(Uri.parse(URL))
        request.setTitle(PACKAGE)
        request.setDescription(getString(R.string.id_downloading))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setDestinationUri(tarFile.toUri())
        downloadID = downloadmanager.enqueue(request)

        findViewById<Button>(R.id.download).isEnabled = false
        Toast.makeText(this, "Start downloading", Toast.LENGTH_SHORT).show()
    }

    fun uncompress(inputFile: File, outputDir: File) {
        if (!outputDir.exists()) {
            outputDir.mkdir()
        }
        val pluginsDir = File(outputDir, "plugins")
        if (!pluginsDir.exists()) {
            pluginsDir.mkdir()
        }
        val input = TarArchiveInputStream(
            BufferedInputStream(
                XZCompressorInputStream(
                    BufferedInputStream(FileInputStream(inputFile))
                )
            )
        )
        var counter = 0
        var entry = input.nextEntry
        while (entry != null) {

            val name = entry.name
            Log.d(TAG, "Extracting $name")
            val f = File(outputDir, name)

            var out = FileOutputStream(f)
            try {
                IOUtils.copy(input, out)
            } finally {
                IOUtils.closeQuietly(out)
            }

            val mode = (entry as TarArchiveEntry).mode
            //noinspection ResultOfMethodCallIgnored
            f.setExecutable(true, mode and 1 == 0)
            entry = input.nextEntry
            counter++
        }
        IOUtils.closeQuietly(input)
        inputFile.delete()
        doAsync { reload() }
    }

    fun onStart(view: View?) {

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val rpcuser = sharedPref.getString("bitcoin-rpcuser", "").toString()
        val rpcpassword = sharedPref.getString("bitcoin-rpcpassword", "").toString()
        if (rpcuser === "" || rpcpassword === "") {
            AlertDialog.Builder(this).setTitle("warning").setMessage("Go to Settings to set lightningd options before start").show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, LightningService::class.java))
        } else {
            startService(Intent(this, LightningService::class.java))
        }

        doAsync {
            Thread.sleep(3000)
            getInfo()
        }
    }

    fun onStop(view: View?) {
        log.info("---onStop---")
        try {
            val res = LightningCli().exec(this, arrayOf("stop"))
            log.info("---" + res.toString() + "---")
            reload()
        }catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            log.info(e.localizedMessage)
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_log -> {
                startActivity(Intent(this, LogActivity::class.java))
                true
            }
            R.id.action_console -> {
                startActivity(Intent(this, ConsoleActivity::class.java))
                true
            }
            R.id.action_scan -> {
                IntentIntegrator(this@MainActivity).initiateScan()
                true
            }
            R.id.action_paste -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                val item = clip?.getItemAt(0)
                val text = item?.text.toString()
                doAsync { scanned(text) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if(result != null){
            if(result.contents != null) {
                Toast.makeText(this@MainActivity, result.contents, Toast.LENGTH_LONG).show()
                doAsync { scanned(result.contents) }
            } else {
                Toast.makeText(this@MainActivity, "Scan failed", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun scanned(text: String) {
        val cli = LightningCli()
        try {
            val res = cli.exec(this@MainActivity, arrayOf("decodepay", text), true)
            val json = cli.toJSONObject(res)
            runOnUiThread(Runnable {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("decodepay")
                    .setMessage(json.toString())
                    .setCancelable(true)
                    .setPositiveButton("pay") { dialog, which -> pay(text) }
                    .setNegativeButton("cancel") { dialog, which -> }
                    .show()
            })
        } catch (e: Exception) {
            try {
                val res = cli.exec(this@MainActivity, arrayOf("connect", text), true)
                val json = cli.toJSONObject(res)
                runOnUiThread(Runnable {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("connect")
                        .setMessage(json.toString())
                        .show()
                })
            } catch (e: Exception) {
                runOnUiThread(Runnable { Toast.makeText(this@MainActivity, "Operation failed", Toast.LENGTH_LONG).show() })
            }
        }
    }

    fun pay(bolt11: String) {
        try {
            val cli = LightningCli()
            val res = cli.exec(this@MainActivity, arrayOf("pay", bolt11), true)
            val json = cli.toJSONObject(res)
            runOnUiThread(Runnable {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("connect")
                    .setMessage(json.toString())
                    .show()
            })
        } catch (e: Exception) {
            runOnUiThread(Runnable { Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_LONG).show() })
        }
    }

    fun getWifiIPAddress(): String? {
        val wifiMgr= getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.getConnectionInfo()
        val ip = wifiInfo.getIpAddress()
        return Formatter.formatIpAddress(ip)
    }

    fun getMobileIPAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (intf in interfaces) {
                val addrs = Collections.list(intf.getInetAddresses());
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress()) {
                        return  addr.getHostAddress()
                    }
                }
            }
        } catch (ex: Exception) { }
        return null
    }
}
