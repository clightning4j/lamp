package com.lvaccaro.lamp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

class UI {

    companion object {

        fun copyToClipboard(context: Context, key: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText(key, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_LONG).show()
        }

        fun snackBar(
            context: AppCompatActivity,
            message: String,
            duration: Int = Snackbar.LENGTH_LONG
        ) {
            Snackbar.make(context.findViewById(android.R.id.content), message, duration).show()
        }

        fun showMessageOnToast(
            context: Context,
            message: String,
            duration: Int = Toast.LENGTH_SHORT
        ) {
            Toast.makeText(context, message, duration).show()
        }

        fun textAlertDialog(context: Context, title: String, message: String) {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .show()
        }

        fun share(context: Context, title: String, text: String) {
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, text)
            sendIntent.type = "text/plain"
            val shareIntent = Intent.createChooser(sendIntent, title)
            context.startActivity(shareIntent)
        }

        fun getQrCode(text: String): Bitmap {
            val SCALE = 16
            try {
                val matrix = Encoder.encode(text, ErrorCorrectionLevel.M).matrix
                val height = matrix.height * SCALE
                val width = matrix.width * SCALE
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

        fun notification(context: Context, title: String, body: String) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "basic_notification"
            val channelName = "basic_channel_notification"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val notificationChannel = NotificationChannel(
                    channelId,
                    channelName,
                    importance
                )
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.YELLOW
                notificationChannel.enableVibration(true)
                notificationChannel.vibrationPattern =
                    longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                manager.createNotificationChannel(notificationChannel)
            }
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(com.lvaccaro.lamp.R.drawable.ic_notification)
                .setAutoCancel(true)
                .setChannelId(channelId)
                .build()
            manager.notify(101, notification)
        }
    }
}
