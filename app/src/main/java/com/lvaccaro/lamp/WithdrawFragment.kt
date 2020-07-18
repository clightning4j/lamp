package com.lvaccaro.lamp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.doAsync
import java.lang.Exception

class WithdrawFragment: BottomSheetDialogFragment() {

    val cli = LightningCli()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_withdraw, container, false)
        val address = arguments?.getString("address")
        view.findViewById<TextView>(R.id.addressText).text = address ?: ""
        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val satoshi = view.findViewById<TextView>(R.id.satoshiText).text.toString()
            val address = view.findViewById<TextView>(R.id.addressText).text.toString()
            doAsync { withdraw(address, satoshi) }
        }

        return view
    }

    fun withdraw(address: String, satoshi: String) {
        try {
            val res = cli.exec(
                context!!, arrayOf(
                    "withdraw",
                    address,
                    satoshi
                ), true
            ).toJSONObject()
            val txid = res["txid"].toString()
            activity?.runOnUiThread {
                AlertDialog.Builder(context!!)
                    .setTitle("Transaction Sent")
                    .setMessage("Tx ID: ${txid}")
                    .setPositiveButton("clipboard") { dialog, which ->
                        copyToClipboard("address", address)
                    }.setNegativeButton("continue") { dialog, which -> }
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            activity?.runOnUiThread {
                Toast.makeText(
                    activity,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun copyToClipboard(key: String, text: String) {
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(key, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context!!, "Copied to clipboard", Toast.LENGTH_LONG).show()
    }
}