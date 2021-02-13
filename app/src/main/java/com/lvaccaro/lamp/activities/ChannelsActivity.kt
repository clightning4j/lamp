package com.lvaccaro.lamp.activities

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lvaccaro.lamp.fragments.ChannelFragment
import com.lvaccaro.lamp.fragments.FundChannelFragment
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import kotlinx.android.synthetic.main.activity_channels.*
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import kotlin.collections.ArrayList

typealias ChannelClickListener = (JSONObject) -> Unit

class ChannelAdapter(val list: ArrayList<JSONObject>,
                     private val onClickListener: ChannelClickListener
) : RecyclerView.Adapter<ChannelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ChannelViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val item: JSONObject = list[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClickListener(item) }
    }

    override fun getItemCount(): Int = list.size

}

class ChannelViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.list_channel, parent, false)) {

    fun bind(channel: JSONObject) {
        val cid = channel.getString("channel_id")
        val msatoshiToUs = channel.getDouble("msatoshi_to_us") / 1000
        val msatoshiTotal = channel.getDouble("msatoshi_total") / 1000
        itemView.findViewById<TextView>(R.id.cid).text = "CID: ${cid.subSequence(0, 8)}..."
        itemView.findViewById<TextView>(R.id.status).text = channel.getString("state")
        itemView.findViewById<TextView>(R.id.mysats).text = "My balance: $msatoshiToUs sat"
        itemView.findViewById<TextView>(R.id.availablesats).text = "Available to receive: $msatoshiTotal sat"
        itemView.findViewById<ProgressBar>(R.id.progressBar).apply {
            max = msatoshiTotal.toInt()
            progress = msatoshiToUs.toInt()
        }
    }
}

class ChannelsActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ChannelAdapter(
                ArrayList(),
                this::showChannel
        )

        doAsync { refresh() }
    }

    private fun showChannel(channel: JSONObject) {
        val bundle = Bundle()
        bundle.putString("channel", channel.toString())
        val fragment = ChannelFragment()
        fragment.arguments = bundle
        fragment.show(supportFragmentManager, "ChannelFragment")
    }

    private fun refresh() {
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
                val total = channels.sumBy { it.getInt("msatoshi_to_us") / 1000 }
                findViewById<TextView>(R.id.total_text).text = "${total} sat in channels"
                val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
                val adapter = recyclerView.adapter as ChannelAdapter
                adapter.apply {
                    list.clear()
                    list.addAll(channels)
                    notifyDataSetChanged()
                }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_channels, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                val bottomSheetDialog =
                        FundChannelFragment()
                bottomSheetDialog.show(supportFragmentManager, "Fund channel")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
