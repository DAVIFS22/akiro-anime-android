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
        val view = LayoutInflater.from(ctx)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title ?: item.name ?: "Stream"
        holder.subtitle.text = buildString {
            item.quality?.takeIf { it.isNotBlank() }?.let { append(it) }
            item.size?.let {
                if (isNotEmpty()) append(" • ")
                append(formatBytes(it))
            }
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.2f MB".format(bytes / 1_048_576.0)
        bytes >= 1024L -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
        val subtitle: TextView = view.findViewById(android.R.id.text2)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StreamDto>() {
            override fun areItemsTheSame(oldItem: StreamDto, newItem: StreamDto) =
                (oldItem.url != null && oldItem.url == newItem.url) ||
                (oldItem.magnet != null && oldItem.magnet == newItem.magnet)

            override fun areContentsTheSame(oldItem: StreamDto, newItem: StreamDto) =
                oldItem == newItem
        }
    }
}
