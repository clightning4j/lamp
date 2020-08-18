package com.lvaccaro.lamp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ListAdapter(val list: JSONArray)
    : RecyclerView.Adapter<ItemViewHolder>() {

    var onItemClick: ((JSONObject) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ItemViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val json = list.getJSONObject(position)
        val type = json["type"] as String
        if (type == "payment")
            holder.payment(json)
        else
            holder.invoice(json)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(json)
        }
    }

    override fun getItemCount(): Int = list.length()

}
class ItemViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_tx, parent, false)) {
    private var mDateView: TextView? = null
    private var mAmountView: TextView? = null
    private var mLabelView: TextView? = null

    init {
        mDateView = itemView.findViewById(R.id.date)
        mAmountView = itemView.findViewById(R.id.amount)
        mLabelView = itemView.findViewById(R.id.label)
    }

    fun invoice(invoice: JSONObject) {
        val paidAt = invoice["paid_at"] as Int
        val msatoshi = invoice["msatoshi"] as Int
        val label = invoice["label"] as String
        bind(msatoshi, paidAt, label, true)
    }

    fun payment(payment: JSONObject) {
        val createdAt = payment["created_at"] as Int
        val msatoshi = payment["msatoshi"] as Int
        val id = payment["id"] as Int
        bind(msatoshi, createdAt, "Payment ID ${id.toString()}", true)
    }

    fun bind(msatoshi: Int, paidAt: Int, label: String, incoming: Boolean) {
        val date = Date(paidAt * 1000L)
        mAmountView?.text = String.format("%s %dmsat", if (incoming) "+" else "-", msatoshi)
        mDateView?.text = SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(date)
        mLabelView?.text = label
    }
}

class HistoryFragment: BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        doAsync {
            val list = getData()
            activity?.runOnUiThread {
                val adapter = ListAdapter(list)
                recyclerView.adapter = adapter
                adapter.onItemClick = { json ->
                    val bundle = Bundle()
                    bundle.putString("title", json.getString("type"))
                    bundle.putString("data", json.toString())
                    val fragment = RecyclerViewFragment()
                    fragment.arguments = bundle
                    fragment.show(activity!!.supportFragmentManager, "RecyclerViewFragment")
                }
            }
        }
        return view
    }

    fun getData(): JSONArray {
        val listinvoices = LightningCli().exec(context!!, arrayOf("listinvoices"), true).toJSONObject()
        val listsendpays = LightningCli().exec(context!!, arrayOf("listsendpays"), true).toJSONObject()

        val invoices = listinvoices["invoices"] as JSONArray
        val payments = listsendpays["payments"] as JSONArray

        val output = JSONArray()
        for (i in 0 until invoices.length()) {
            val invoice = invoices.getJSONObject(i)
            if (invoice.getString("status") == "paid")
                output.put(invoice.put("type", "invoice"))
        }
        for (i in 0 until payments.length()) {
            val payment = payments.getJSONObject(i)
            if (payment.getString("status") == "complete")
                output.put(payment.put("type", "payment"))
        }
        return output
    }
}