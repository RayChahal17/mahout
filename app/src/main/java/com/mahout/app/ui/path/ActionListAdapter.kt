package com.mahout.app.ui.path

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemActionBinding
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus

/**
 * Grid card adapter for "Today's actions".
 *
 * Timer behavior (V1 safe):
 * - If timer is running/paused for a DIFFERENT action, disable Start on other cards.
 * - If timer is for THIS action, show Stop (we map to service ACTION_STOP).
 *
 * NOTE:
 * This file assumes your `Action` model uses:
 * - id, title, cadence, targetValue
 */
class ActionListAdapter(
    private val onClick: (Action) -> Unit,
    private val onLongClick: (Action) -> Unit,
    private val onTimerClick: (Action) -> Unit
) : ListAdapter<Action, ActionListAdapter.VH>(Diff) {

    private var timerState: TimerState? = null

    fun updateTimerState(state: TimerState) {
        timerState = state
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onClick, onLongClick, onTimerClick) { timerState }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemActionBinding,
        private val onClick: (Action) -> Unit,
        private val onLongClick: (Action) -> Unit,
        private val onTimerClick: (Action) -> Unit,
        private val timerStateProvider: () -> TimerState?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Action) {
            binding.tvActionTitle.text = item.title

            // Simple meta labels (we'll refine later when timeline/sessions exist)
            val cadenceLabel = when (item.cadence) {
                ActionCadence.DAILY -> "Repeats daily"
                ActionCadence.WEEKLY -> "Repeats weekly"
                ActionCadence.ONE_TIME -> "One-time"
            }

            val targetLabel = item.targetValue?.let { minutes ->
                "Target ${minutes}m"
            }

            binding.tvActionMeta.text =
                if (targetLabel == null) cadenceLabel else "$cadenceLabel • $targetLabel"

            // Progress is placeholder for now (we'll compute using sessions later).
            binding.pbProgress.progress = 0
            binding.tvProgressLabel.text = "0m / ${item.targetValue ?: 0}m"

            // Timer button state
            val state = timerStateProvider()
            val isThisAction = state?.actionId == item.id

            when (state?.status ?: TimerStatus.STOPPED) {
                TimerStatus.STOPPED -> {
                    binding.btnTimer.text = "Start"
                    binding.btnTimer.isEnabled = true
                }

                TimerStatus.RUNNING,
                TimerStatus.PAUSED -> {
                    if (isThisAction) {
                        binding.btnTimer.text = "Stop"
                        binding.btnTimer.isEnabled = true
                    } else {
                        binding.btnTimer.text = "Start"
                        binding.btnTimer.isEnabled = false
                    }
                }
            }

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
            binding.btnTimer.setOnClickListener { onTimerClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Action>() {
        override fun areItemsTheSame(old: Action, new: Action): Boolean = old.id == new.id
        override fun areContentsTheSame(old: Action, new: Action): Boolean = old == new
    }
}
