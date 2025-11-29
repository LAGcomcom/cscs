package com.feiyang.smssync

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.*

class SmsAdapter(private var items: List<Sms>) :
    RecyclerView.Adapter<SmsAdapter.VH>() {

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            setPadding(24, 24, 24, 24)
            textSize = 14f
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sms = items[position]
        holder.tv.text = buildString {
            append("[${DateFormat.getDateTimeInstance().format(sms.date)}]\n")
            append("${sms.address}:\n")
            append(sms.body)
        }
    }

    override fun getItemCount() = items.size

    fun submit(list: List<Sms>) {
        items = list
        notifyDataSetChanged()
    }
}
