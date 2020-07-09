package com.lvaccaro.lamp

import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
                    var resultOperation = File(activity?.rootDir(), ".lightning").delete() &&
                            File(activity?.rootDir(), ".bitcoin").delete() &&
                            File(activity?.rootDir(), ".tor").delete() &&
                            File(activity?.rootDir(), ".torHiddenService").delete()

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
                    var resultOperation = File(downloadDir, MainActivity.tarFilename()).delete() &&
                    File(downloadDir, "cacert.pem").delete() &&
                    File(dir, "plugins").delete() &&
                    File(dir, "lightningd").delete() &&
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
                    File(dir, "plugins").delete() &&
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
                    doAsync { exportData() }
                    true
                }
                "importdata" -> {
                    showImportDialog()
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

        fun showImportDialog() {
            val input = EditText(activity)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.setLayoutParams(lp)
            input.hint = "zip filepath"

            AlertDialog.Builder(context!!)
                .setTitle("Import file")
                .setMessage("Importing will delete the current lightning wallet")
                .setView(input)
                .setPositiveButton("import") { dialog, which ->
                    doAsync {
                        val dataFolder = File(activity?.rootDir(), ".lightning")
                        dataFolder.deleteRecursively()
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context!!, "Copying the content of " + input.text
                                        + " into " + dataFolder, Toast.LENGTH_LONG
                            ).show()
                        }
                        uncompress(File(input.text.toString()), dataFolder)
                        activity?.runOnUiThread {
                            Toast.makeText(context!!, "Importing successful ", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
                .setNegativeButton("cancel") { dialog, which -> }
                .show()
        }


        fun uncompress(inputFile: File, outputDir: File) {
            if (!outputDir.exists()) {
                outputDir.mkdir()
            }
            val input = ZipInputStream(BufferedInputStream(FileInputStream(inputFile)))
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
                    }
                }

                entry = input.nextEntry
            }
            IOUtils.closeQuietly(input)
        }

        fun exportData() {
            val inputFolder = File(activity?.rootDir(), ".lightning")
            val outputFolder = context!!.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
            if (!outputFolder.exists())
                outputFolder.mkdir()
            val zipFile = File(outputFolder, "export.zip")
            zipFile.delete()
            zipFile.createNewFile()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use {
                it.use {
                    compress(it, inputFolder, "")
                    it.close()
                }
            }

            activity?.let {
                it.runOnUiThread {
                    if (zipFile.exists()) {
                        Toast.makeText(it, "Export in ${zipFile}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(it, "Export error", Toast.LENGTH_LONG).show()
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
    }
}