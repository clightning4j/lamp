package com.lvaccaro.lamp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lvaccaro.lamp.activities.InvoiceActivity
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.UI
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

class PeerInfoFragment: BottomSheetDialogFragment() {

    var address = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_peer_info, container, false)
        view.findViewById<Button>(R.id.copyButton).setOnClickListener {
            UI.copyToClipboard(context!!, "peer_node", address)
        }
        view.findViewById<Button>(R.id.shareButton).setOnClickListener {
            UI.share(context!!, "Peer node", address)
        }
        doAsync { getInfo(view) }
        return view
    }


    fun getInfo(view: View) {
        try {
            val res =
                LightningCli().exec(context!!, arrayOf("getinfo"), true).toJSONObject()
            val id = res["id"] as String
            val addresses = res["address"] as JSONArray
            // the node has an address? if not hide the UI node info
            address = id
            if (addresses.length() != 0) {
                val addressText = addresses[0] as JSONObject
                address = id + "@" + addressText.getString("address")
            }

            activity?.runOnUiThread {
                view.findViewById<TextView>(R.id.peerText).text = address
            }

            val qrcode = UI.getQrCode(address)
            activity?.runOnUiThread {
                view.findViewById<ImageView>(R.id.peerImage).setImageBitmap(qrcode)
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