package com.lvaccaro.lamp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.adapters.HashMapAdapter
import org.json.JSONObject

class RecyclerViewFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val title = arguments?.getString("title")
        val data = arguments?.getString("data")
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = HashMapAdapter(
                HashMapAdapter.from(JSONObject(data))
            )
        }
        view.findViewById<TextView>(R.id.title).text = title
        return view
    }
}
