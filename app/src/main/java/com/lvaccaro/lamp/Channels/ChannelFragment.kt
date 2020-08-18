package com.lvaccaro.lamp.Channels

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
import com.lvaccaro.lamp.util.HashMapAdapter
import com.lvaccaro.lamp.util.UI
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.lang.Exception

class ChannelFragment : BottomSheetDialogFragment() {

    val cli = LightningCli()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_channel, container, false)
        val data =  arguments?.getString("channel") ?: ""
        val channel = JSONObject(data)
        val cid = channel.getString("channel_id")

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = HashMapAdapter(HashMapAdapter.from(channel))

        val closeButton = view.findViewById<Button>(R.id.close_button)
        closeButton.isEnabled = channel.getString("state") == "CHANNELD_NORMAL"
        closeButton.setOnClickListener { doAsync { close(cid) } }
        return view
    }

    fun close(cid: String) {
        val context = activity!!
        try {
            val res = LightningCli().exec(
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
                AlertDialog.Builder(context)
                    .setTitle("Error")
                    .setMessage(e.localizedMessage)
                    .show()
            }
        }
    }
}