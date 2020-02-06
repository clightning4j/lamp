package com.lvaccaro.lamp

import android.Manifest
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
import android.view.View
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.wifi.WifiManager
import android.os.*
import android.text.InputType
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.WriterException
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.NetworkInterface
import java.util.*
import java.util.logging.Logger


class MainActivity : AppCompatActivity() {

    val WRITE_REQUEST_CODE = 101
    val log = Logger.getLogger(MainActivity::class.java.name)
    val TAG = "MainActivity"
    var downloadID = 0L
    val cli = LightningCli()
    lateinit var downloadmanager: DownloadManager
    lateinit var powerImageView: PowerImageView

    companion object {

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
            val RELEASE = "v0.18.1.2"
            return "https://github.com/lvaccaro/bitcoin_ndk/releases/download/${RELEASE}/${TAR_FILENAME}"
        }
    }

    fun dir(): File {
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isServiceRunning()) {
            setTheme(R.style.AppTheme)
        } else {
            setTheme(R.style.AppTheme_Night)
        }
        setContentView(R.layout.activity_main)

        powerImageView = findViewById<PowerImageView>(R.id.powerImageView)
        powerImageView.setOnClickListener { this.onPowerClick() }

        val addressTextView = findViewById<TextView>(R.id.textViewQr)
        addressTextView.setOnClickListener { copyToClipboard("address", addressTextView.text.toString()) }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AlertDialog.Builder(this).setTitle("Sorry")
                .setMessage("Android >= 10 API is not supported").show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isServiceRunning()) {
            doAsync { getInfo() }
        }
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
            R.id.action_scan -> {
                IntentIntegrator(this@MainActivity).initiateScan()
                true
            }
            R.id.action_invoice -> {
                showInvoiceBuilder()
                true
            }
            R.id.action_invoice -> {
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
        Toast.makeText(this, "****", Toast.LENGTH_SHORT).show()
        var result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                doAsync { scanned(result.contents) }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Scan failed", Snackbar.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if("com.lvaccaro.lamp.LightningService".equals(service.service.getClassName())) {
                return true
            }
        }
        return false
    }

    fun onPowerClick() {
        if (powerImageView.isAnimating()) {
            return
        }
        if (powerImageView.isOn()) {
            // turn off
            doAsync {
                stop()
                runOnUiThread { powerOff() }
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
            Snackbar.make(findViewById(android.R.id.content), "Package already downloaded. Uncompressing...", Snackbar.LENGTH_LONG).show()
            powerImageView.animating()
            doAsync {
                uncompress(tarFile, rootDir())
                runOnUiThread {
                    powerOff()
                }
            }
            return
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Downloading...", Snackbar.LENGTH_LONG)
                .show()
            powerImageView.animating()
            download()
        }
    }

    fun powerOff() {
        powerImageView.off()
        recreate()
    }

    fun powerOn() {
        powerImageView.on()
        recreate()
    }

    fun getInfo() {
        try {
            val res = LightningCli().exec(this@MainActivity, arrayOf("getinfo"), true).toJSONObject()
            val id = res["id"] as String
            val addresses = res["address"] as JSONArray
            if (addresses.length() == 0)
                throw Exception("no address found")
            var address = addresses[0] as JSONObject
            if (!address.has("address"))
                throw Exception("no address found")
            val txt = id + address.getString("address")
            val alias = res["alias"] as String
            runOnUiThread {
                powerImageView.on()
                findViewById<TextView>(R.id.textViewQr).apply {
                    text = txt
                    visibility = View.VISIBLE
                }
                title = alias
            }
        } catch (e: Exception) {
            log.info("---" + e.localizedMessage + "---")
            runOnUiThread {
                Snackbar.make(findViewById(android.R.id.content), e.localizedMessage, Snackbar.LENGTH_LONG)
                    .show()
                powerImageView.off()
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
                Snackbar.make(findViewById(android.R.id.content), "Download Completed. Uncompressing...", Snackbar.LENGTH_LONG).show()
            }
            val tarFile = File(dir(), tarFilename())
            doAsync {
                uncompress(tarFile, rootDir())
                runOnUiThread { powerOff() }
            }
        }
    }

    fun download() {
        // Download package
        val tarFile = File(dir(), tarFilename())
        val request = DownloadManager.Request(Uri.parse(url()))
        request.setTitle("lightning")
        request.setDescription(getString(R.string.id_downloading))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setDestinationUri(tarFile.toUri())
        downloadID = downloadmanager.enqueue(request)
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

    fun start() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val rpcuser = sharedPref.getString("bitcoin-rpcuser", "").toString()
        val rpcpassword = sharedPref.getString("bitcoin-rpcpassword", "").toString()
        if (rpcuser === "" || rpcpassword === "") {
            AlertDialog.Builder(this).setTitle("warning")
                .setMessage("Go to Settings to set lightningd options before start").show()
            return
        }

        val torEnabled = sharedPref.getBoolean("enabled-tor", true)
        powerImageView.animating()
        doAsync {
            if (torEnabled) {
                runOnUiThread { startTor() }
                Thread.sleep(5000)
            }
            runOnUiThread { startLightning() }
            Thread.sleep(2000)
            try {
                LightningCli().exec(this@MainActivity, arrayOf("getinfo"), true).toJSONObject()
                runOnUiThread { powerOn() }
            } catch (e: Exception) {
                log.info("---" + e.localizedMessage + "---")
                runOnUiThread {
                    Snackbar.make(findViewById(android.R.id.content), e.localizedMessage, Snackbar.LENGTH_LONG)
                        .show()
                    powerImageView.off()
                }
            }
        }
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
            //Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            log.info(e.localizedMessage)
            e.printStackTrace()
        }
        stopService(Intent(this, LightningService::class.java))
        stopService(Intent(this, TorService::class.java))
    }

    fun scanned(text: String) {
        try {
            val res = cli.exec(this@MainActivity, arrayOf("decodepay", text), true).toJSONObject()
            runOnUiThread { showDecodePay(res) }
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

    fun showDecodePay(decoded: JSONObject) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("decodepay")
            .setMessage(decoded.toString())
            .setCancelable(true)
            .setPositiveButton("pay") { dialog, which ->
                try {
                    cli.exec(this@MainActivity, arrayOf("pay", decoded["bolt11"] as String), true)
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
            .setPositiveButton("fund channel") { dialog, which -> showFundChannel(id) }
            .setNegativeButton("cancel") { dialog, which -> }
            .show()
    }

    fun showFundChannel(id: String) {
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.setLayoutParams(lp)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "satoshi"

        AlertDialog.Builder(this@MainActivity)
            .setTitle("fund a channel")
            .setMessage("with ${id}")
            .setView(input)
            .setPositiveButton("confirm") { dialog, which ->
                try {
                    cli.exec(
                        this@MainActivity,
                        arrayOf("fundchannel", id, input.text.toString()),
                        true
                    ).toJSONObject()
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Channel funded",
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

    fun showInvoiceBuilder() {
        val msatoshi = EditText(this)
        val label = EditText(this)
        val description = EditText(this)
        msatoshi.inputType = InputType.TYPE_CLASS_NUMBER
        msatoshi.hint = "msatoshi"
        label.hint = "label"
        description.hint = "description"

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        container.addView(msatoshi)
        container.addView(label)
        container.addView(description)

        AlertDialog.Builder(this@MainActivity)
            .setTitle("Build invoice")
            .setView(container)
            .setPositiveButton("confirm") { dialog, which ->
                try {
                    val res = cli.exec(
                        this@MainActivity, arrayOf(
                            "invoice",
                            msatoshi.text.toString(),
                            label.text.toString(),
                            description.text.toString()
                        ), true
                    ).toJSONObject()
                    val bolt11 = res["bolt11"].toString()
                    runOnUiThread { showInvoice(bolt11, label.text.toString()) }
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
            qr.layoutParams = LinearLayout.LayoutParams(300, 300)
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
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

    fun showInvoice(bolt11: String, label: String) {
        val editText = EditText(this)
        val qr = ImageView(this)

        editText.setText(bolt11)
        qr.setImageBitmap(getQrCode(bolt11))
        qr.layoutParams = LinearLayout.LayoutParams(300, 300)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        container.addView(editText)
        container.addView(qr)

        AlertDialog.Builder(this@MainActivity)
            .setTitle("Invoice")
            .setMessage("Label: ${label}")
            .setView(container)
            .setNeutralButton("clipboard") { dialog, which ->
                copyToClipboard("invoice", bolt11)
            }
            .setPositiveButton("wait") { dialog, which ->
                try {
                    cli.exec(this@MainActivity, arrayOf("waitinvoice", label), true).toJSONObject()
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
            .setCancelable(false)
            .show()
    }

    fun copyToClipboard(key: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(key, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show()
    }
}
