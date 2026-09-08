package com.akiro.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.akiro.addon.StreamDto

class StreamsAdapter(
    private val ctx: Context,
    private val onClick: (StreamDto) -> Unit
) : ListAdapter<StreamDto, StreamsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title ?: item.name ?: "Stream"
        val subtitle = StringBuilder()
        if (!item.quality.isNullOrEmpty()) subtitle.append(item.quality)
        if (item.size != null) {
            if (subtitle.isNotEmpty()) subtitle.append(" • ")
            subtitle.append("${item.size} bytes")
        }
        holder.subtitle.text = subtitle.toString()
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(android.R.id.text1)
        val subtitle: TextView = v.findViewById(android.R.id.text2)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StreamDto>() {
            override fun areItemsTheSame(oldItem: StreamDto, newItem: StreamDto): Boolean {
                return (oldItem.url == newItem.url) || (oldItem.magnet == newItem.magnet)
            }

            override fun areContentsTheSame(oldItem: StreamDto, newItem: StreamDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}
