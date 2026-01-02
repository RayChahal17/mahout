package com.mahout.app.ui.aim

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemGoalLinkedActionBinding

class LinkedActionsAdapter(
    private val onUnlinkClick: (actionId: String, title: String) -> Unit
) : ListAdapter<LinkedActionUi, LinkedActionsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGoalLinkedActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onUnlinkClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemGoalLinkedActionBinding,
        private val onUnlinkClick: (String, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LinkedActionUi) {
            binding.tvActionTitle.text = item.title
            binding.tvActionWeek.text = item.weekText
            binding.btnUnlink.setOnClickListener { onUnlinkClick(item.actionId, item.title) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LinkedActionUi>() {
        override fun areItemsTheSame(oldItem: LinkedActionUi, newItem: LinkedActionUi): Boolean =
            oldItem.actionId == newItem.actionId

        override fun areContentsTheSame(oldItem: LinkedActionUi, newItem: LinkedActionUi): Boolean =
            oldItem == newItem
    }
}
