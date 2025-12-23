package com.mahout.app.ui.aim

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemGoalBinding

/**
 * RecyclerView adapter for Goals.
 *
 * Uses ListAdapter + DiffUtil:
 * - Efficient updates (only changed rows rebind)
 * - Great for Flow-powered lists (Room emits often)
 */
class GoalListAdapter(
    private val onClick: (GoalRowUiModel) -> Unit
) : ListAdapter<GoalRowUiModel, GoalListAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGoalBinding.inflate(
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
        private val binding: ItemGoalBinding,
        private val onClick: (GoalRowUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GoalRowUiModel) {
            binding.tvGoalTitle.text = item.title

            binding.tvGoalWhy.text = item.why.orEmpty()
            binding.tvGoalWhy.isEnabled = !item.why.isNullOrBlank()

            // Horizon chip text (simple mapping; can be improved later)
            binding.chipGoalHorizon.text = item.horizon.name.lowercase().replace('_', ' ')

            // Target chip visibility handled by Fragment (formats date)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GoalRowUiModel>() {
        override fun areItemsTheSame(oldItem: GoalRowUiModel, newItem: GoalRowUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GoalRowUiModel, newItem: GoalRowUiModel): Boolean =
            oldItem == newItem
    }
}
