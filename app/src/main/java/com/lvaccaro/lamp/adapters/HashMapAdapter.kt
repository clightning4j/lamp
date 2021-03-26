package com.lvaccaro.lamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lvaccaro.lamp.utils.UI
import org.json.JSONObject

class HashMapAdapter(val map: LinkedHashMap<String, String>) :
    RecyclerView.Adapter<HashMapAdapter.ViewHolder>() {

    class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(android.R.layout.two_line_list_item, parent, false)) {

        fun bind(key: String, value: String) {
            itemView.findViewById<TextView>(android.R.id.text1).text = key
            itemView.findViewById<TextView>(android.R.id.text2).text = value
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            inflater,
            parent
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = map.keys.toList()[position]
        val value = map.values.toList()[position]
        holder.bind(key, value)
        holder.itemView.setOnClickListener {
            UI.copyToClipboard(
                holder.itemView.context,
                key,
                value
            )
        }
    }

    override fun getItemCount(): Int = map.count()

    companion object {
        fun from(json: JSONObject): LinkedHashMap<String, String> {
            val temp = json.keys()
            val hashMap = LinkedHashMap<String, String>()
            while (temp.hasNext()) {
                val key = temp.next()
                val value = json[key].toString()
                hashMap.put(key, value)
            }
            return hashMap
        }
    }
}
