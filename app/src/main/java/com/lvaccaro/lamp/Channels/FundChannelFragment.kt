package com.lvaccaro.lamp.Channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import org.jetbrains.anko.doAsync
import java.lang.Exception

class FundChannelFragment: BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fundchannel, container, false)
        val uri = arguments?.getString("uri")
        view.findViewById<TextInputEditText>(R.id.node_text).setText(uri ?: "")
        view.findViewById<Button>(R.id.button).setOnClickListener {
            doAsync {
                fund(
                    view.findViewById<TextInputEditText>(R.id.node_text).text.toString(),
                    if (view.findViewById<SwitchMaterial>(R.id.fundmax_switch).isChecked) "all" else view.findViewById<TextInputEditText>(
                        R.id.satoshiText
                    ).text.toString(),
                    "normal",
                    if (view.findViewById<SwitchMaterial>(R.id.private_switch).isChecked) "false" else "true"
                )
            }
        }
        return view
    }

    fun fund(id: String, amount: String, feerate: String, announce: String) {
        try {
            LightningCli().exec(
                context!!,
                arrayOf("fundchannel", id, amount, feerate, announce),
                true
            ).toJSONObject()
            activity?.runOnUiThread {
                Toast.makeText(
                    context,
                    "Channel funded",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            activity?.runOnUiThread {
                showWarning(e.localizedMessage)
            }
        }
    }

    fun showWarning(message: String) {
        AlertDialog.Builder(context!!)
            .setTitle(R.string.id_warning)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, which -> }
            .show()
    }
}