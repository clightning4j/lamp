package com.lvaccaro.lamp

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
import android.text.InputType
import android.text.format.Formatter
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

    val WRITE_REQUEST_CODE = 101
    val log = Logger.getLogger(MainActivity::class.java.name)
    val TAG = "MainActivity"
    var downloadID = 0L
    val cli = LightningCli()
    lateinit var downloadmanager: DownloadManager

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
            val PACKAGE = "lightning"
            return "${ARCH}-${PACKAGE}.tar.xz"
        }

        fun url(): String {
            val TAR_FILENAME = tarFilename()
            val RELEASE = "release_0.2"
            return "https://github.com/lvaccaro/clightning_ndk/releases/download/${RELEASE}/${TAR_FILENAME}"
        }
    }

    fun dir(): File {
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        doAsync {
            Thread.sleep(2000)
            getInfo(true)
        }
    }

    fun getInfo(start: Boolean = false) {
        // if lightning is up and running update link
        try {
            val res = LightningCli().exec(this, arrayOf("getinfo"), true).toJSONObject()
            val id = res["id"].toString()
            val address = getWifiIPAddress() ?: getMobileIPAddress() ?: ""
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val announceaddr = sharedPref.getString("announce-addr", "").toString()

            val text = "${id}@" + if (!announceaddr.isEmpty()) announceaddr else "${address}"
            runOnUiThread(Runnable {
                findViewById<TextView>(R.id.textViewQr).text = text
                findViewById<ImageView>(R.id.qrcodeImageView).setImageBitmap(getQrCode(text))
                findViewById<ImageView>(R.id.qrcodeImageView).setOnClickListener {
                    copyToClipboard("peernode", text)
                }
                findViewById<TextView>(R.id.textViewQr).visibility = View.VISIBLE
                findViewById<ImageView>(R.id.qrcodeImageView).visibility = View.VISIBLE
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                findViewById<Button>(R.id.start).isEnabled = false
                findViewById<Button>(R.id.stop).isEnabled = true


                listOf(recyclerView.adapter)
            })
        } catch (e: Exception) {
            // if lightning is down
            log.info("---" + e.localizedMessage + "---")
            if (start) {
                // trying to start
                runOnUiThread { onStart(null) }
                Thread.sleep(3000)
                getInfo(false)
                return
            }
            runOnUiThread(Runnable {
                findViewById<TextView>(R.id.textViewQr).visibility = View.GONE
                findViewById<ImageView>(R.id.qrcodeImageView).visibility = View.GONE
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                findViewById<Button>(R.id.start).isEnabled = true
                findViewById<Button>(R.id.stop).isEnabled = false
            })
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

    private val onDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID != id)
                return

            runOnUiThread {
                findViewById<Button>(R.id.download).isEnabled = false
                Toast.makeText(
                    this@MainActivity,
                    "Download Completed. Uncompressing...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val tarFile = File(dir(), tarFilename())
            doAsync { uncompress(tarFile, rootDir()) }
        }
    }

    fun onDownload(view: View?) {
        val tarFile = File(dir(), tarFilename())
        if (tarFile.exists()) {
            // Uncompress package
            Toast.makeText(this, "Package already downloaded. Uncompressing...", Toast.LENGTH_LONG)
                .show()
            doAsync { uncompress(tarFile, rootDir()) }
            return
        }

        // Download package
        val request = DownloadManager.Request(Uri.parse(url()))
        request.setTitle("lightning")
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
        runOnUiThread(Runnable { reload() })
    }

    fun onStart(view: View?) {

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val rpcuser = sharedPref.getString("bitcoin-rpcuser", "").toString()
        val rpcpassword = sharedPref.getString("bitcoin-rpcpassword", "").toString()
        if (rpcuser === "" || rpcpassword === "") {
            AlertDialog.Builder(this).setTitle("warning")
                .setMessage("Go to Settings to set lightningd options before start").show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, LightningService::class.java))
        } else {
            startService(Intent(this, LightningService::class.java))
        }
    }

    fun onStop(view: View?) {
        log.info("---onStop---")
        try {
            val res = LightningCli().exec(this, arrayOf("stop"))
            log.info("---" + res.toString() + "---")
            reload()
        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            log.info(e.localizedMessage)
            e.printStackTrace()
        }
        stopService(Intent(this, LightningService::class.java))
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
        var result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                doAsync { scanned(result.contents) }
            } else {
                Toast.makeText(this@MainActivity, "Scan failed", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
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

    fun getWifiIPAddress(): String? {
        val wifiMgr = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
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
