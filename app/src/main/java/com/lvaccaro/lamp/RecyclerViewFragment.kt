package com.lvaccaro.lamp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lvaccaro.lamp.util.UI
import org.json.JSONObject

class RecyclerViewFragment : BottomSheetDialogFragment() {

    class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(android.R.layout.two_line_list_item, parent, false)) {

        fun bind(key: String, value: String) {
            itemView.findViewById<TextView>(android.R.id.text1).text = key
            itemView.findViewById<TextView>(android.R.id.text2).text = value
        }
    }

    class HashMapAdapter(val map: LinkedHashMap<String, String>) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater, parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val key = map.keys.toList()[position]
            val value = map.values.toList()[position]
            holder.bind(key, value)
            holder.itemView.setOnClickListener { UI.copyToClipboard(holder.itemView.context, key, value) }
        }

        override fun getItemCount(): Int = map.count()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val title =  arguments?.getString("title")
        val data =  arguments?.getString("data")
        val map = getLinkedHashMap(JSONObject(data))
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = HashMapAdapter(map)
        view.findViewById<TextView>(R.id.title).text = title

        return view
    }

    fun getLinkedHashMap(json: JSONObject): LinkedHashMap<String, String> {
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