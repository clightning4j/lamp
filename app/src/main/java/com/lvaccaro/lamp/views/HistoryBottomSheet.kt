package com.lvaccaro.lamp.views

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.fragments.RecyclerViewFragment
import com.lvaccaro.lamp.toJSONObject
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ListAdapter(
    val list: JSONArray,
    private val onItemClick: ((JSONObject) -> Unit)?
) :
    RecyclerView.Adapter<ItemViewHolder>() {

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
        bind(msatoshi, createdAt, "Payment ID $id", true)
    }

    fun bind(msatoshi: Int, paidAt: Int, label: String, incoming: Boolean) {
        val date = Date(paidAt * 1000L)
        mAmountView?.text = String.format("%s %dmsat", if (incoming) "+" else "-", msatoshi)
        mDateView?.text = SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(date)
        mLabelView?.text = label
    }
}

class HistoryBottomSheet(val context: Context, val view: View?) :
    BottomSheetBehavior.BottomSheetCallback() {

    private val bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private val recyclerView: RecyclerView

    init {
        bottomSheetBehavior = BottomSheetBehavior.from<LinearLayout>(view as LinearLayout)
        bottomSheetBehavior.addBottomSheetCallback(this)
        recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        val imageView = view?.findViewById<ImageView>(R.id.arrow_image)
        when (newState) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                imageView?.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_up))
            }
            BottomSheetBehavior.STATE_HIDDEN -> {}
            BottomSheetBehavior.STATE_EXPANDED -> {
                imageView?.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_down))
                doAsync { reload() }
            }
            BottomSheetBehavior.STATE_DRAGGING -> {}
            BottomSheetBehavior.STATE_SETTLING -> { }
        }
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
    }

    private fun slideUpDown() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun reload() {
        val list = getData()
        context.runOnUiThread {
            recyclerView.adapter = ListAdapter(list) {
                val bundle = Bundle()
                bundle.putString("title", it.getString("type"))
                bundle.putString("data", it.toString())
                val fragment = RecyclerViewFragment()
                fragment.arguments = bundle
                fragment.show((context as AppCompatActivity).supportFragmentManager, "RecyclerViewFragment")
            }
            view?.apply {
                findViewById<TextView>(R.id.loading_text)?.visibility = View.GONE
                findViewById<TextView>(R.id.no_transactions_text)?.visibility = if (list.length() == 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun getData(): JSONArray {
        val listinvoices = LightningCli()
            .exec(context, arrayOf("listinvoices"), true).toJSONObject()
        val listsendpays = LightningCli()
            .exec(context, arrayOf("listsendpays"), true).toJSONObject()

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
