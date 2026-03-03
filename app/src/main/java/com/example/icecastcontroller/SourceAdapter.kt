package com.example.icecastcontroller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.icecastcontroller.databinding.ItemSourceBinding

class SourceAdapter(
    private val onSelect: (StreamSource) -> Unit,
    private val onDelete: (StreamSource) -> Unit
) : ListAdapter<StreamSource, SourceAdapter.ViewHolder>(DIFF) {

    private var selectedId: String? = null

    fun setSelected(id: String?) {
        val old = selectedId
        selectedId = id
        // Refresh affected items
        currentList.forEachIndexed { index, source ->
            if (source.id == old || source.id == id) notifyItemChanged(index)
        }
    }

    inner class ViewHolder(private val binding: ItemSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: StreamSource) {
            binding.tvSourceName.text = source.name
            binding.tvSourceUrl.text = source.url
            binding.tvMimeType.text = source.mimeType
            binding.root.isSelected = source.id == selectedId
            binding.root.setOnClickListener { onSelect(source) }
            binding.btnDelete.setOnClickListener { onDelete(source) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StreamSource>() {
            override fun areItemsTheSame(a: StreamSource, b: StreamSource) = a.id == b.id
            override fun areContentsTheSame(a: StreamSource, b: StreamSource) = a == b
        }
    }
}
