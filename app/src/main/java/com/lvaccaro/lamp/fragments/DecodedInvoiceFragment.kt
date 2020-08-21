package com.lvaccaro.lamp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.adapters.HashMapAdapter
import org.jetbrains.anko.doAsync
import java.lang.Exception

class DecodedInvoiceFragment : BottomSheetDialogFragment() {

    private val cli = LightningCli()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_decoded_invoice, container, false)
        val bolt11 =  arguments?.getString("bolt11") ?: ""

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val payButton = view.findViewById<Button>(R.id.pay_button)
        payButton.setOnClickListener { doAsync { pay(bolt11) } }

        val context = activity!!
        doAsync {
            try {
                val res = cli.exec(context, arrayOf("decodepay", bolt11), true)
                    .toJSONObject()
                context.runOnUiThread {
                    recyclerView.adapter =
                        HashMapAdapter(
                            HashMapAdapter.from(res)
                        )
                }
            } catch (e: Exception) {
                context.runOnUiThread {
                    AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage(e.localizedMessage)
                        .show()
                }
            }
        }
        return view
    }

    private fun pay(bolt11: String) {
        // Pay invoice
        val context = activity!!
        try {
            cli.exec(context, arrayOf("pay", bolt11), true)
                .toJSONObject()
            context.runOnUiThread {
                Toast.makeText(context, "Invoice paid", Toast.LENGTH_LONG).show()
                dismiss()
            }
        } catch (e: Exception) {
            context.runOnUiThread {
                AlertDialog.Builder(context)
                    .setTitle("Error")
                    .setMessage(e.localizedMessage)
                    .show()
            }
        }
    }
}