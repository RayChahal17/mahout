package com.mahout.app.ui.aim

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemGoalReceiptBinding

class GoalReceiptAdapter : ListAdapter<GoalReceiptUi, GoalReceiptAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGoalReceiptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemGoalReceiptBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GoalReceiptUi) {
//            binding.tvReceiptTitle.text = item.title
            binding.tvReceiptMeta.text = item.meta
        }
    }

    private object Diff : DiffUtil.ItemCallback<GoalReceiptUi>() {
        override fun areItemsTheSame(oldItem: GoalReceiptUi, newItem: GoalReceiptUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GoalReceiptUi, newItem: GoalReceiptUi): Boolean =
            oldItem == newItem
    }
}
