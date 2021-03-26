package com.lvaccaro.lamp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.LampKeys
import com.lvaccaro.lamp.utils.UI
import org.jetbrains.anko.doAsync
import java.lang.Exception

class WithdrawFragment : BottomSheetDialogFragment() {

    val cli = LightningCli()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_withdraw, container, false)
        val address = arguments?.getString(LampKeys.ADDRESS_KEY) ?: ""
        val satoshi = arguments?.getString(LampKeys.AMOUNT_KEY) ?: ""
        view.findViewById<TextView>(R.id.addressText).text = address
        view.findViewById<TextView>(R.id.satoshiText).text = satoshi
        view.findViewById<TextView>(R.id.satoshiText).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                view.findViewById<SwitchMaterial>(R.id.switch_send_all).isChecked = s?.length == 0
            }
        })
        view.findViewById<SwitchMaterial>(R.id.switch_send_all).isChecked = satoshi.trim().isEmpty()
        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            var satoshi = view.findViewById<TextView>(R.id.satoshiText).text.toString()
            val address = view.findViewById<TextView>(R.id.addressText).text.toString()

            val sendAll = view.findViewById<SwitchMaterial>(R.id.switch_send_all).isChecked
            if (sendAll) satoshi = "all"

            doAsync { withdraw(address, satoshi) }
        }
        return view
    }

    fun withdraw(address: String, satoshi: String) {
        try {
            val res = cli.exec(
                context!!,
                arrayOf(
                    "withdraw",
                    address,
                    satoshi
                ),
                true
            ).toJSONObject()
            val txid = res["txid"].toString()
            activity?.runOnUiThread {
                AlertDialog.Builder(context!!)
                    .setTitle("Transaction Sent")
                    .setMessage("Tx ID: $txid")
                    .setPositiveButton("clipboard") { dialog, which ->
                        UI.copyToClipboard(context!!, "address", address)
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
}
