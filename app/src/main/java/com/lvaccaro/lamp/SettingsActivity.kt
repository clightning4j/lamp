package com.lvaccaro.lamp

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.io.File

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
                    val downloadDir = activity?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
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
                    Toast.makeText(context, "Erased binary in: ${dir.path} and downloaded files", Toast.LENGTH_LONG).show()
                    true
                } else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
        }
    }
}