package com.mahout.app.ui.path

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemActionBinding
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence

/**
 * Day 12:
 * - Tap row => edit
 * - Long press => archive confirm
 *
 * NOTE:
 * We keep the binding IDs stable (tvActionTitle, tvActionMeta) so layouts don’t break.
 */
class ActionListAdapter(
    private val onClick: (Action) -> Unit,
    private val onLongClick: (Action) -> Unit
) : ListAdapter<Action, ActionListAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemActionBinding,
        private val onClick: (Action) -> Unit,
        private val onLongClick: (Action) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Action) {
            binding.tvActionTitle.text = item.title

            // "Cadence" must be exhaustive for enums (fixes your ONE_TIME compile error).
            val cadenceLabel = when (item.cadence) {
                ActionCadence.DAILY -> "Daily"
                ActionCadence.WEEKLY -> "Weekly"
                ActionCadence.ONE_TIME -> "One-time"
            }

            // Target is optional. We format minutes into h/m for readability.
            val targetLabel = item.targetValue?.let { minutes ->
                if (minutes >= 60) {
                    val h = minutes / 60
                    val m = minutes % 60
                    if (m == 0) "${h}h" else "${h}h ${m}m"
                } else {
                    "${minutes}m"
                }
            }

            binding.tvActionMeta.text =
                if (targetLabel == null) cadenceLabel else "$cadenceLabel • $targetLabel target"

            // Interactions
            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Action>() {
        override fun areItemsTheSame(old: Action, new: Action): Boolean = old.id == new.id
        override fun areContentsTheSame(old: Action, new: Action): Boolean = old == new
    }
}
