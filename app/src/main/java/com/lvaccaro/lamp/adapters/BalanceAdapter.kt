package com.lvaccaro.lamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lvaccaro.lamp.R

data class Balance (val title: String, val subtitle: String, val value: String)

typealias BalanceClickListener = (Int) -> Unit

class BalanceAdapter(
    val list: ArrayList<Balance>,
    private val onClickListener: BalanceClickListener?
)
    : RecyclerView.Adapter<BalanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return BalanceViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        val item: Balance = list[position]
        holder.bind(item.title, item.subtitle, item.value)
        holder.itemView.setOnClickListener { onClickListener?.invoke(position) }
    }

    override fun getItemCount(): Int = list.size

}

class BalanceViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_balance, parent, false)) {
    fun bind(title: String, subtitle: String, value: String) {
        itemView.findViewById<TextView>(R.id.title).text = title
        itemView.findViewById<TextView>(R.id.subtitle).text = subtitle
        itemView.findViewById<TextView>(R.id.value).text = value
    }
}