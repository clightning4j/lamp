package com.lvaccaro.lamp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.doAsync
import java.lang.Exception

class InvoiceBuildFragment: BottomSheetDialogFragment() {

    private val cli = LightningCli()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_invoice, container, false)
        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val msatoshi = view.findViewById<TextView>(R.id.msatoshiText).text.toString()
            val label = view.findViewById<TextView>(R.id.labelText).text.toString()
            val description = view.findViewById<TextView>(R.id.descriptionText).text.toString()
            doAsync { invoice(msatoshi, label, description) }
        }

        return view
    }

    fun invoice(msatoshi: String, label: String, description: String) {
        try {
            val res = cli.exec(
                context!!, arrayOf(
                    "invoice",
                    msatoshi,
                    label,
                    description
                ), true
            ).toJSONObject()
            activity?.runOnUiThread {
                val intent = Intent(activity, InvoiceActivity::class.java)
                intent.putExtra("bolt11", res["bolt11"] as String)
                intent.putExtra("label", label)
                startActivity(intent)
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