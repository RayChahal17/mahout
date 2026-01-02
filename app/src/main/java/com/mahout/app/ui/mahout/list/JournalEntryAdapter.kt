package com.mahout.app.ui.mahout.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemJournalEntryBinding

class JournalEntryAdapter(
    private val onClick: (JournalListViewModel.RowUi) -> Unit
) : ListAdapter<JournalListViewModel.RowUi, JournalEntryAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemJournalEntryBinding,
        private val onClick: (JournalListViewModel.RowUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var model: JournalListViewModel.RowUi? = null

        init {
            binding.root.setOnClickListener {
                model?.let(onClick)
            }
        }

        fun bind(item: JournalListViewModel.RowUi) {
            model = item
            binding.tvType.text = item.typeLabel
            binding.tvDate.text = item.dateLabel
            binding.tvPreview.text = item.preview
        }
    }

    private object Diff : DiffUtil.ItemCallback<JournalListViewModel.RowUi>() {
        override fun areItemsTheSame(oldItem: JournalListViewModel.RowUi, newItem: JournalListViewModel.RowUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: JournalListViewModel.RowUi, newItem: JournalListViewModel.RowUi): Boolean =
            oldItem == newItem
    }
}
