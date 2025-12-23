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

            val cadenceLabel = when (item.cadence) {
                ActionCadence.DAILY -> "Daily"
                ActionCadence.WEEKLY -> "Weekly"
                ActionCadence.ONE_TIME -> "One-time"
            }

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

            val state = timerStateProvider()
            val isThisAction = state?.actionId == item.id

            when (state?.status ?: TimerStatus.STOPPED) {
                TimerStatus.STOPPED -> {
                    binding.btnTimer.text = "Start"
                    binding.btnTimer.isEnabled = true
                }

                TimerStatus.RUNNING -> {
                    if (isThisAction) {
                        binding.btnTimer.text = "Pause"
                        binding.btnTimer.isEnabled = true
                    } else {
                        binding.btnTimer.text = "Start"
                        binding.btnTimer.isEnabled = false
                    }
                }

                TimerStatus.PAUSED -> {
                    if (isThisAction) {
                        binding.btnTimer.text = "Resume"
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
