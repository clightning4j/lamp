package com.lvaccaro.lamp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_channels.*
import kotlinx.android.synthetic.main.list_channel.view.*
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChannelAdapter(val list: ArrayList<JSONObject>)
    : RecyclerView.Adapter<ChannelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ChannelViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val item: JSONObject = list[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = list.size

}

class ChannelViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_channel, parent, false)) {

    fun bind(channel: JSONObject) {
        val cid = channel.getString("channel_id")
        val msatoshi_to_us = channel.getInt("msatoshi_to_us")/1000
        val msatoshi_total = channel.getInt("msatoshi_total")/1000
        itemView.findViewById<TextView>(R.id.cid).text = "CID: ${cid.subSequence(0,8)}..."
        itemView.findViewById<TextView>(R.id.status).text = channel.getString("state")
        itemView.findViewById<TextView>(R.id.mysats).text = "My balance: ${msatoshi_to_us.toString()} sat"
        itemView.findViewById<TextView>(R.id.availablesats).text = "Available to receive: ${msatoshi_total.toString()} sat"
        itemView.findViewById<ProgressBar>(R.id.progressBar).apply {
            max = msatoshi_total
            progress = msatoshi_to_us
        }
    }
}

class ChannelsActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)
        setSupportActionBar(toolbar)

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ChannelAdapter(ArrayList<JSONObject>())

        doAsync { refresh() }
    }

    fun refresh() {
        try {
            val res = LightningCli().exec(
                this@ChannelsActivity,
                arrayOf("listpeers"),
                true
            ).toJSONObject()

            val channels = ArrayList<JSONObject>()
            val peers = res["peers"] as JSONArray
            for (i in 0 until peers.length()) {
                val peer = peers.get(i) as? JSONObject
                val peerChannels = peer?.get("channels") as JSONArray
                for (j in 0 until peerChannels.length()) {
                    val channel = peerChannels.get(j) as JSONObject
                    channel.put("peer_id", peer.getString("id"))
                    channels.add(channel)
                }
            }

            runOnUiThread {
                val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
                val adapter = recyclerView.adapter as ChannelAdapter
                adapter.list.clear()
                adapter.list.addAll(channels)
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this@ChannelsActivity,
                    "Channel funded",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
