package com.lvaccaro.lamp.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar

object UI {
    fun copyToClipboard(context: Context, key: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(key, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_LONG).show()
    }

    companion object {

        fun copyToClipboard(context: Context, key: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText(key, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_LONG).show()
        }

    fun textAlertDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .show()
    }

        fun showMessageOnToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT){
            Toast.makeText(context, message, duration).show()
        }

        fun textAlertDialog(context: Context, title: String, message: String) {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .show()
        }
    }

    fun showMessageOnSnackBar(context: AppCompatActivity,  message: String,
                              duration: Int = Snackbar.LENGTH_LONG) {
        if (message.isEmpty()) return
        Snackbar.make(context.findViewById(android.R.id.content), message, duration).show()
    }

    fun showMessageOnToast(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
        if (message.isEmpty()) return
        Toast.makeText(context, message, duration).show()
    }

}
