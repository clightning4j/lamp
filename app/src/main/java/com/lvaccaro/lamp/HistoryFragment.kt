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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


data class Item(val amount: Int, val time: Int, val label: String, val incoming: Boolean) {}

class ListAdapter(val list: ArrayList<Item>)
    : RecyclerView.Adapter<ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ItemViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item: Item = list[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = list.size

}
class ItemViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_tx, parent, false)) {
    private var mTitleView: TextView? = null
    private var mDescriptionView: TextView? = null

    init {
        mTitleView = itemView.findViewById(R.id.text1)
        mDescriptionView = itemView.findViewById(R.id.text2)
    }

    fun bind(item: Item) {
        val date = Date(item.time * 1000L)
        mDescriptionView?.text = String.format("%s %dmsat", if (item.incoming) "+" else "-", item.amount)
        mTitleView?.text = SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(date)
    }
}

class HistoryFragment: BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        var recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ListAdapter(ArrayList<Item>())

        doAsync {
            val list = getInvoices() + getPayments()
            activity!!.runOnUiThread {
                val adapter = recyclerView.adapter as ListAdapter
                adapter.list.clear()
                adapter.list.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
        return view
    }

    fun getInvoices(): List<Item> {
        val res = LightningCli().exec(context!!, arrayOf("listinvoices"), true).toJSONObject()
        val invoicesJson = res["invoices"] as JSONArray
        val invoices = ArrayList<Item>()
        for (i in 0..invoicesJson.length()-1) {
            val invoice = invoicesJson.getJSONObject(i)
            val status = invoice["status"] as String
            if (status.equals("paid")) {
                val paidAt = invoice["paid_at"] as Int
                val msatoshi = invoice["msatoshi"] as Int
                val label = invoice["label"] as String
                invoices.add(Item(msatoshi, paidAt, label, true))
            }
        }
        return invoices
    }

    fun getPayments(): List<Item> {
        val res = LightningCli().exec(context!!, arrayOf("listsendpays"), true).toJSONObject()
        val paymentsJson = res["payments"] as JSONArray
        val payments = ArrayList<Item>()
        for (i in 0..paymentsJson.length()-1) {
            val payment = paymentsJson.getJSONObject(i)
            val status = payment["status"] as String
            if (status.equals("complete")) {
                val createdAt = payment["created_at"] as Int
                val msatoshi = payment["msatoshi"] as Int
                val id = payment["id"] as String
                payments.add(Item(msatoshi, createdAt, id, false))
            }
        }
        return payments
    }
}