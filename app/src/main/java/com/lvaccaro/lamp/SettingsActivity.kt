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
                    val log = File(activity?.rootDir(), "log")
                    log.delete()
                    Toast.makeText(context, "Erased log: ${log.path}", Toast.LENGTH_LONG).show()
                    true
                }
                "cleardata" -> {
                    val datadir = File(activity?.rootDir(), ".lightning")
                    datadir.delete()
                    Toast.makeText(context, "Erased datadir: ${datadir.path}", Toast.LENGTH_LONG).show()
                    true
                }
                "clearbinary" -> {
                    File(activity?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!, MainActivity.TAR_FILENAME).delete()
                    File(activity?.rootDir(), "plugins").delete()
                    File(activity?.rootDir(), "lightningd").delete()
                    File(activity?.rootDir(), "lightning-cli").delete()
                    File(activity?.rootDir(), "bitcoin-cli").delete()
                    val dir = File(activity?.rootDir(), "")
                    Toast.makeText(context, "Erased binary in: ${dir.path}", Toast.LENGTH_LONG).show()
                    true
                } else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
        }
    }
}