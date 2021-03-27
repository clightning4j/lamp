package com.lvaccaro.lamp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.adapters.HashMapAdapter
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.utils.UI
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.lang.Exception

class ChannelFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_channel, container, false)
        val data = arguments?.getString("channel") ?: ""
        val channel = JSONObject(data)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = HashMapAdapter(
                HashMapAdapter.from(channel)
            )
        }

        val closeButton = view.findViewById<Button>(R.id.close_button)
        closeButton.apply {
            isEnabled = channel.getString("state") == "CHANNELD_NORMAL"
            setOnClickListener { doAsync { close(channel.getString("channel_id")) } }
        }
        return view
    }

    private fun close(cid: String) {
        val context = activity!!
        try {
            LightningCli().exec(
                context,
                arrayOf("close", cid),
                true
            ).toJSONObject()
            context.runOnUiThread {
                Toast.makeText(context, "Channel closing...", Toast.LENGTH_LONG).show()
                dismiss()
            }
        } catch (e: Exception) {
            context.runOnUiThread {
                UI.textAlertDialog(context, "Error", e.localizedMessage)
            }
        }
    }
}
