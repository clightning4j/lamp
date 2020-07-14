package com.lvaccaro.lamp

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
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
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.lvaccaro.lamp.Channels.ChannelsActivity
import com.lvaccaro.lamp.Channels.FundChannelFragment
import com.lvaccaro.lamp.Services.LightningService
import com.lvaccaro.lamp.Services.TorService
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {

    val REQUEST_SCAN = 102
    val REQUEST_FUNDCHANNEL = 103
    val WRITE_REQUEST_CODE = 101
    val log = Logger.getLogger(MainActivity::class.java.name)
    val TAG = "MainActivity"
    var downloadID = 0L
    var downloadCertID = 0L
    val cli = LightningCli()
    lateinit var downloadmanager: DownloadManager
    lateinit var powerImageView: PowerImageView
    var timer: Timer? = null

    companion object {
        val RELEASE = "release_clightning_0.8.2"

        fun arch(): String {
            var abi: String?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                abi = Build.SUPPORTED_ABIS[0]
            } else {
                abi = Build.CPU_ABI
            }
            when (abi) {
                "armeabi-v7a" -> return "arm-linux-androideabi"
                "arm64-v8a" -> return "aarch64-linux-android"
                "x86" -> return "i686-linux-android"
                "x86_64" -> return "x86_64-linux-android"
            }
            throw Error("No arch found")
        }

        fun tarFilename(): String {
            val ARCH = arch()
            val PACKAGE = "bitcoin"
            return "${ARCH}_${PACKAGE}.tar.xz"
        }

        fun url(): String {
            val TAR_FILENAME = tarFilename()
            return "https://github.com/lvaccaro/lightning_ndk/releases/download/${RELEASE}/${TAR_FILENAME}"
        }
    }

    fun dir(): File {
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        powerImageView = findViewById<PowerImageView>(R.id.powerImageView)
        powerImageView.setOnClickListener { this.onPowerClick() }
        val arrowImageView = findViewById<ImageView>(R.id.arrowImageView)
        arrowImageView.setOnClickListener { this.onHistoryClick() }

        val addressTextView = findViewById<TextView>(R.id.textViewQr)
        addressTextView.setOnClickListener {
            copyToClipboard(
                "address",
                addressTextView.text.toString()
            )
        }

        val floatingActionButton = findViewById<FloatingActionButton>(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_SCAN)
        }

        downloadmanager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        registerReceiver(onDownloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    WRITE_REQUEST_CODE
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!File(rootDir(), "lightning-cli").exists()) {
            findViewById<TextView>(R.id.statusText).text =
                "Rub the lamp to download ${RELEASE} binaries."
            return
        }
        if (!isLightningRunning()) {
            findViewById<TextView>(R.id.statusText).text =
                "Offline. Rub the lamp to start."
            return
        }
        doAsync { getInfo() }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivityForResult(Intent(this, SettingsActivity::class.java), 100)
                true
            }
            R.id.action_log -> {
                startActivityForResult(Intent(this, LogActivity::class.java), 100)
                true
            }
            R.id.action_console -> {
                startActivityForResult(Intent(this, ConsoleActivity::class.java), 100)
                true
            }
            R.id.action_invoice -> {
                val bottomSheetDialog = InvoiceBuildFragment()
                bottomSheetDialog.show(getSupportFragmentManager(), "Custom Bottom Sheet")
                true
            }
            R.id.action_channels -> {
                startActivityForResult(Intent(this, ChannelsActivity::class.java), 100)
                true
            }
            R.id.action_withdraw -> {
                val bottomSheetDialog = WithdrawFragment()
                bottomSheetDialog.show(supportFragmentManager, "WithdrawFragment")
                true
            }
            R.id.action_new_address -> {
                doAsync { generateNewAddress() }
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
        if (requestCode == REQUEST_SCAN && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("text")
            if (result != null) {
                doAsync { scanned(result) }
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Scan failed",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun isServiceRunning(name: String): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (name.equals(service.service.getClassName())) {
                return true
            }
        }
        return false
    }

    fun isLightningRunning(): Boolean {
        return isServiceRunning(LightningService::class.java.canonicalName)
    }

    fun isTorRunning(): Boolean {
        return isServiceRunning(TorService::class.java.canonicalName)
    }

    fun onHistoryClick() {
        HistoryFragment().show(getSupportFragmentManager(), "History dialog")
    }

    fun onPowerClick() {
        if (powerImageView.isAnimating()) {
            return
        }
        if (powerImageView.isOn()) {
            // turn off
            doAsync {
                stop()
            }
            return
        }

        val isLightningReady = File(rootDir(), "lightning-cli").exists()
        if (isLightningReady) {
            // turn on
            start()
            return
        }
        val tarFile = File(dir(), tarFilename())
        if (tarFile.exists()) {
            // Uncompress package
            findViewById<TextView>(R.id.statusText).text =
                "Package already downloaded. Uncompressing..."
            powerImageView.animating()
            doAsync {
                uncompress(tarFile, rootDir())
                runOnUiThread {
                    powerOff()
                }
            }
        } else {
            findViewById<TextView>(R.id.statusText).text =
                "Downloading..."
            powerImageView.animating()
            download()
        }
    }

    fun powerOff() {
        powerImageView.off()
        timer?.cancel()
        findViewById<TextView>(R.id.statusText).text = "Offline. Rub the lamp to turn on."
        findViewById<ImageView>(R.id.qrcodeImageView).visibility = View.GONE
    }

    fun powerOn() {
        powerImageView.on()
        findViewById<TextView>(R.id.statusText).text = ""
    }

    fun getInfo() {
        try {
            val resChainInfo =
                LightningCli().exec(this@MainActivity, arrayOf("getchaininfo"), true).toJSONObject()
            val blockcount = resChainInfo["blockcount"] as Int
            val res =
                LightningCli().exec(this@MainActivity, arrayOf("getinfo"), true).toJSONObject()
            val id = res["id"] as String
            val addresses = res["address"] as JSONArray
            if (addresses.length() == 0)
                throw Exception("no address found")
            var address = addresses[0] as JSONObject
            if (!address.has("address"))
                throw Exception("no address found")
            val txt = id + "@" + address.getString("address")
            val alias = res["alias"] as String
            val blockheight = res["blockheight"] as Int

            // instantiate timer to monitor block syncing progress
            timer?.cancel()
            if (blockcount > blockheight) {
                timer = Timer()
                timer!!.schedule(5 * 1000) {
                    doAsync { getInfo() }
                }
            }

            runOnUiThread {
                title = alias
                powerImageView.on()
                findViewById<ImageView>(R.id.arrowImageView).visibility = View.VISIBLE
                findViewById<FloatingActionButton>(R.id.floating_action_button).show()
                findViewById<TextView>(R.id.textViewQr).apply {
                    text = txt
                    visibility = View.VISIBLE
                }
                val delta = blockcount - blockheight
                findViewById<TextView>(R.id.statusText).text = if (delta > 0) "Syncing blocks -${delta}" else "Online"
            }
            val qrcode = getQrCode(txt)
            runOnUiThread {
                findViewById<ImageView>(R.id.qrcodeImageView).apply {
                    visibility = View.VISIBLE
                    setImageBitmap(qrcode)
                }
            }
        } catch (e: Exception) {
            log.info("---" + e.localizedMessage + "---")
            runOnUiThread {
                stopLightningService()
                stopTorService()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    e.localizedMessage,
                    Snackbar.LENGTH_LONG
                ).show()
                powerOff()
            }
        }
    }

    fun getQrCode(text: String): Bitmap {
        val SCALE = 16
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

    private val onDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID != id)
                return

            runOnUiThread {
                findViewById<TextView>(R.id.statusText).text =
                    "Download Completed. Uncompressing..."
            }
            val tarFile = File(dir(), tarFilename())
            doAsync {
                uncompress(tarFile, rootDir())
                tarFile.delete();
                runOnUiThread { powerOff() }
            }
        }
    }

    fun download() {
        // Download bitcoin_ndk package
        val tarFile = File(dir(), tarFilename())
        val request = DownloadManager.Request(Uri.parse(url()))
        request.setTitle("lightning")
        request.setDescription(getString(R.string.id_downloading))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setDestinationUri(tarFile.toUri())
        downloadID = downloadmanager.enqueue(request)

        // Download CA certificates
        val cacert = "https://curl.haxx.se/ca/cacert.pem"
        val fileCert = File(dir(), "cacert.pem")
        val requestCert = DownloadManager.Request(Uri.parse(cacert))
        requestCert.setTitle("CA certificates")
        requestCert.setDescription(getString(R.string.id_downloading))
        requestCert.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        requestCert.setDestinationUri(fileCert.toUri())
        downloadCertID = downloadmanager.enqueue(requestCert)
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
    }

    fun waitTorBootstrap(): Boolean {
        val logFile = File(rootDir(), "tor.log")
        for (i in 0..10) {
            try {
                if (logFile.readText().contains("100%"))
                    return true
            } catch (err: Exception) {
            }
            Thread.sleep(2000)
        }
        return false;
    }

    fun waitLightningBootstrap(): Boolean {
        val logFile = File(rootDir(), "lightningd.log")
        for (i in 0..10) {
            try {
                if (logFile.readText().contains("lightningd: Server started with public key"))
                    return true
            } catch (err: Exception) {
                Log.d(TAG, err.localizedMessage)
            }
            Thread.sleep(2000)
        }
        return false;
    }

    fun start() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val rpcuser = sharedPref.getString("bitcoin-rpcuser", "").toString()
        val rpcpassword = sharedPref.getString("bitcoin-rpcpassword", "").toString()
        val esplora = sharedPref.getBoolean("enabled-esplora", true)
        if (!esplora && (rpcuser === "" || rpcpassword === "")) {
            AlertDialog.Builder(this)
                .setTitle("warning")
                .setMessage("Go to Settings to set lightningd options before start")
                .setNegativeButton("cancel") { dialog, which -> }
                .setPositiveButton("settings") { dialog, which ->
                    startActivityForResult(Intent(this, SettingsActivity::class.java), 100)
                }
                .show()
            return
        }

        val torEnabled = sharedPref.getBoolean("enabled-tor", true)

        // clear log files
        val torLogFile = File(rootDir(), "tor.log")
        val lightningLogFile = File(rootDir(), "lightningd.log")
        torLogFile.delete()
        lightningLogFile.delete()

        powerImageView.animating()
        doAsync {
            if (torEnabled) {
                // start service on main thread
                runOnUiThread {
                    findViewById<TextView>(R.id.statusText).text =
                        "Starting tor..."
                    startTor()
                }
                // wait tor to be bootstrapped
                if (!waitTorBootstrap()) {
                    runOnUiThread {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Tor start failed",
                            Snackbar.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "******** Tor run failed ********")
                        stopTorService()
                        powerOff()
                    }
                    return@doAsync
                }
            }
            // start service on main thread
            runOnUiThread {
                findViewById<TextView>(R.id.statusText).text =
                    "Starting lightning..."
                startLightning() }
            // wait lightning to be bootstrapped
            if (!waitLightningBootstrap()) {
                runOnUiThread {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Lightning start failed",
                        Snackbar.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "******** Lightning run failed ********")
                    stopLightningService()
                    powerOff()
                }
                return@doAsync
            }
            try {
                getInfo()
                runOnUiThread { powerOn() }
            } catch (e: Exception) {
                stop()
                log.info("---" + e.localizedMessage + "---")
                runOnUiThread {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        e.localizedMessage,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun stopTorService() {
        stopService(Intent(this, TorService::class.java))
    }

    private fun stopLightningService(){
        stopService(Intent(this, LightningService::class.java))
    }

    fun startTor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, TorService::class.java))
        } else {
            startService(Intent(this, TorService::class.java))
        }
    }

    fun startLightning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, LightningService::class.java))
        } else {
            startService(Intent(this, LightningService::class.java))
        }
    }

    fun stop() {
        log.info("---onStop---")
        try {
            val res = LightningCli().exec(this, arrayOf("stop"))
            log.info("---" + res.toString() + "---")
        } catch (e: Exception) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Error: ${e.localizedMessage}",
                Snackbar.LENGTH_LONG)
                .setBackgroundTint(Color.RED)
                .show()
            log.info(e.localizedMessage)
            e.printStackTrace()
        }
        runOnUiThread {
            stopLightningService()
            stopTorService()
            powerOff()
        }
    }

    fun scanned(text: String) {
        try {
            val res = cli.exec(this@MainActivity, arrayOf("decodepay", text), true).toText()
            runOnUiThread { showDecodePay(text, res) }
        } catch (e: Exception) {
            try {
                val res = cli.exec(this@MainActivity, arrayOf("connect", text), true).toJSONObject()
                runOnUiThread { showConnected(res["id"] as String) }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        e.localizedMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun showDecodePay(bolt11: String, decoded: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("decodepay")
            .setMessage(decoded.toString())
            .setCancelable(true)
            .setPositiveButton("pay") { dialog, which ->
                try {
                    cli.exec(this@MainActivity, arrayOf("pay", bolt11), true)
                        .toJSONObject()
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Invoice paid",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("cancel") { dialog, which -> }
            .show()
    }

    fun showConnected(id: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("connect")
            .setMessage(id)
            .setPositiveButton("fund channel") { dialog, which ->
                val bottomSheetDialog =
                    FundChannelFragment()
                val args = Bundle()
                args.putString("uri", id)
                bottomSheetDialog.arguments = args
                bottomSheetDialog.show(supportFragmentManager, "Fund channel")
            }
            .setNegativeButton("cancel") { dialog, which -> }
            .show()
    }

    fun generateNewAddress() {
        val res = cli.exec(
            this@MainActivity,
            arrayOf("newaddr"),
            true
        ).toJSONObject()
        runOnUiThread {
            val textView = TextView(this)
            val qr = ImageView(this)
            val address = res["address"].toString()
            textView.setText(address)
            qr.setImageBitmap(getQrCode(address))
            val layoutParams = LinearLayout.LayoutParams(300, 300)
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
            qr.layoutParams = layoutParams
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            container.setPadding(16, 16, 16, 16)
            container.addView(textView)
            container.addView(qr)
            AlertDialog.Builder(this@MainActivity)
                .setTitle("New address")
                .setView(container)
                .setPositiveButton("clipboard") { dialog, which ->
                    copyToClipboard("address", address)
                }.setNegativeButton("cancel") { dialog, which -> }
                .setCancelable(false)
                .show()
        }
    }

    fun copyToClipboard(key: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(key, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show()
    }
}
