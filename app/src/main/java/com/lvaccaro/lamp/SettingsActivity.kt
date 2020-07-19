package com.lvaccaro.lamp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.apache.commons.compress.utils.IOUtils
import org.jetbrains.anko.doAsync
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    class SettingsFragment : PreferenceFragmentCompat() {

        val REQUEST_CREATE_FILE = 999
        val REQUEST_OPEN_FILE = 998

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            return when (preference?.key) {
                "clearlogs" -> {
                    var resultOperation = File(activity?.rootDir(), "lightningd.log").delete() &&
                            File(activity?.rootDir(), "tor.log").delete()

                    if(resultOperation){
                        showToast("Erased logs", Toast.LENGTH_LONG)
                    }else{
                        showToast("Error during Erasing logs", Toast.LENGTH_LONG)
                    }
                    return resultOperation
                }
                "cleardata" -> {
                    var resultOperation = File(activity?.rootDir(), ".lightning").deleteRecursively() &&
                            File(activity?.rootDir(), ".bitcoin").deleteRecursively() &&
                            File(activity?.rootDir(), ".tor").deleteRecursively() &&
                            File(activity?.rootDir(), ".torHiddenService").deleteRecursively()

                    if (resultOperation) {
                        showToast("Erased datadir", Toast.LENGTH_LONG)
                    } else {
                        showToast("Error during erasing datadir", Toast.LENGTH_LONG)
                    }
                    return resultOperation
                }
                "clearbinary" -> {
                    val dir = File(activity?.rootDir(), "")
                    val downloadDir =
                        activity?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    File(downloadDir, MainActivity.tarFilename()).delete()
                    File(downloadDir, "cacert.pem").delete()
                    val resultOperation = File(dir, "lightningd").delete() &&
                    File(dir, "lightning-cli").delete() &&
                    File(dir, "lightning_channeld").delete() &&
                    File(dir, "lightning_closingd").delete() &&
                    File(dir, "lightning_connectd").delete() &&
                    File(dir, "lightning_onchaind").delete() &&
                    File(dir, "lightning_openingd").delete() &&
                    File(dir, "lightning_gossipd").delete() &&
                    File(dir, "lightning_hsmd").delete() &&
                    File(dir, "bitcoin-cli").delete() &&
                    File(dir, "bitcoind").delete() &&
                    File(dir, "plugins").deleteRecursively() &&
                    File(dir, "tor").delete()

                    if(resultOperation){
                        showToast(
                            "Erased binary in: ${dir.path} and downloaded files",
                            Toast.LENGTH_LONG
                        )
                    }else{
                        showToast(
                            "Error during erasing binary in: ${dir.path} and downloaded files",
                            Toast.LENGTH_LONG
                        )
                    }

                    return resultOperation
                }
                "exportdata" -> {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                        putExtra(Intent.EXTRA_TITLE, "clightning_data.zip")
                    }
                    startActivityForResult(intent, REQUEST_CREATE_FILE)
                    true
                }
                "importdata" -> {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "application/zip"
                    startActivityForResult(intent, REQUEST_OPEN_FILE)
                    true
                }
                "enabled-tor" -> {
                    showToast("The change require the node restart", Toast.LENGTH_LONG)
                    true
                }
                "enabled-esplora" ->{
                    showToast("The change require the node restart", Toast.LENGTH_LONG)
                    true
                }
                else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
        }

        fun showToast(message: String, duration: Int) {
            Toast.makeText(context, message, duration).show()
        }

        fun import(selected: Uri) {
            val outputDir = File(activity?.rootDir(), ".lightning")
            outputDir.deleteRecursively()
            outputDir.mkdir()
            val stream = activity!!.contentResolver.openInputStream(selected)
            val input = ZipInputStream(BufferedInputStream(stream))
            var entry = input.nextEntry
            while (entry != null) {
                val currFile = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    currFile.mkdirs();
                } else {
                    val parent: File = currFile.parentFile

                    if (!parent.exists()) {
                        parent.mkdirs()
                    }

                    val out: OutputStream
                    try {
                        out = FileOutputStream(currFile)
                        IOUtils.copy(input, out)
                        IOUtils.closeQuietly(out)
                    } catch (e: IOException) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context!!, "Error while copying '" + currFile.absolutePath
                                        + "': " + e.message + "'", Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    }
                }

                entry = input.nextEntry
            }
            IOUtils.closeQuietly(input)
            stream?.close()
            activity?.runOnUiThread {
                Toast.makeText(
                    activity!!, "Import success", Toast.LENGTH_LONG
                ).show()
            }
        }

        fun export(selected: Uri) {
            val stream = activity!!.contentResolver.openOutputStream(selected, "w")
            val inputFolder = File(activity?.rootDir(), ".lightning")
            ZipOutputStream(BufferedOutputStream(stream)).use {
                it.use {
                    compress(it, inputFolder, "")
                    it.close()
                }
            }
            stream?.close()

            activity?.apply {
                runOnUiThread {
                    try {
                        contentResolver.openInputStream(selected)
                        Toast.makeText(this, "Export success", Toast.LENGTH_LONG).show()
                    } catch (e: java.io.FileNotFoundException) {
                        Toast.makeText(this, "Export failure", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        fun compress(zos: ZipOutputStream, inputFolder: File, basePath: String) {
            for (file in inputFolder.listFiles()) {
                if (file.isDirectory) {
                    val entry = ZipEntry(basePath + File.separator + file.name + File.separator)
                    entry.size = file.length()
                    zos.putNextEntry(entry)
                    compress(zos, file, file.name)
                } else {
                    val origin = BufferedInputStream(FileInputStream(file))
                    origin.use {
                        val entryName = basePath + File.separator + file.name
                        zos.putNextEntry(ZipEntry(entryName))
                        origin.copyTo(zos, 2048)
                    }
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CREATE_FILE && resultCode == RESULT_OK) {
                data?.data?.let {
                    doAsync { export(it) }
                }
            } else if (requestCode == REQUEST_OPEN_FILE && resultCode == RESULT_OK) {
                data?.data?.let {
                    doAsync { import(it) }
                }
            }
        }
    }
}
