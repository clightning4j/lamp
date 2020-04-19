package com.lvaccaro.lamp

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.jetbrains.anko.doAsync
import java.io.*
import java.util.zip.ZipEntry
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
                    File(activity?.rootDir(), "lightningd.log").delete()
                    File(activity?.rootDir(), "tor.log").delete()
                    Toast.makeText(context, "Erased logs", Toast.LENGTH_LONG).show()
                    true
                }
                "cleardata" -> {
                    File(activity?.rootDir(), ".lightning").delete()
                    File(activity?.rootDir(), ".bitcoin").delete()
                    File(activity?.rootDir(), ".tor").delete()
                    File(activity?.rootDir(), ".torHiddenService").delete()
                    Toast.makeText(context, "Erased datadir", Toast.LENGTH_LONG).show()
                    true
                }
                "clearbinary" -> {
                    val dir = File(activity?.rootDir(), "")
                    val downloadDir =
                        activity?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    File(downloadDir, MainActivity.tarFilename()).delete()
                    File(downloadDir, "cacert.pem").delete()
                    File(dir, "plugins").delete()
                    File(dir, "lightningd").delete()
                    File(dir, "lightning-cli").delete()
                    File(dir, "lightning_channeld").delete()
                    File(dir, "lightning_closingd").delete()
                    File(dir, "lightning_connectd").delete()
                    File(dir, "lightning_onchaind").delete()
                    File(dir, "lightning_openingd").delete()
                    File(dir, "lightning_gossipd").delete()
                    File(dir, "lightning_hsmd").delete()
                    File(dir, "bitcoin-cli").delete()
                    File(dir, "bitcoind").delete()
                    File(dir, "plugins").delete()
                    File(dir, "tor").delete()
                    Toast.makeText(
                        context,
                        "Erased binary in: ${dir.path} and downloaded files",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }
                "exportdata" -> {
                    doAsync { exportData() }
                    true
                }
                "importdata" -> {
                    true
                } else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
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