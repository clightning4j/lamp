package com.lvaccaro.alcore

import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.io.File

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actiivty_settings)
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
                    File(activity?.rootDir(), "log").delete()
                    true
                }
                "cleardata" -> {
                    File(activity?.rootDir(), ".lightning").delete()
                    true
                }
                "clearbinary" -> {
                    File(activity?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!, MainActivity.TAR_FILENAME).delete()
                    File(activity?.rootDir(), "plugins").delete()
                    File(activity?.rootDir(), "lightningd").delete()
                    File(activity?.rootDir(), "lightning-cli").delete()
                    File(activity?.rootDir(), "bitcoin-cli").delete()
                    true
                } else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
        }
    }
}