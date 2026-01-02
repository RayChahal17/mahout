package com.mahout.app.ui.aim

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemGoalBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoalListAdapter(
    private val onClick: (GoalRowUiModel) -> Unit,
    private val onLongClick: ((GoalRowUiModel) -> Unit)? = null
) : ListAdapter<GoalRowUiModel, GoalListAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGoalBinding.inflate(
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
        private val binding: ItemGoalBinding,
        private val onClick: (GoalRowUiModel) -> Unit,
        private val onLongClick: ((GoalRowUiModel) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GoalRowUiModel) {
            binding.tvGoalTitle.text = item.title

            binding.tvGoalWhy.text = item.why.orEmpty()
            binding.tvGoalWhy.isVisible = !item.why.isNullOrBlank()

            binding.tvGoalLinkedSummary.text = item.linkedSummary
            binding.tvGoalLinkedSummary.isVisible = item.linkedCount > 0

            val raw = item.horizon.name.lowercase().replace('_', ' ')
            binding.chipGoalHorizon.text =
                raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            // Target date chip
            val targetDate = item.targetDate
            binding.chipGoalTarget.isVisible = targetDate != null
            if (targetDate != null) {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                binding.chipGoalTarget.text = "Due ${targetDate.format(formatter)}"
            }

            // Weekly stats: "This week 1h 20m · Checks 2"
            val hasStats = item.weeklyTimeMillis > 0 || item.weeklySessionCount > 0
            binding.tvGoalWeeklyStats.isVisible = hasStats
            if (hasStats) {
                val timeText = formatDuration(item.weeklyTimeMillis)
                binding.tvGoalWeeklyStats.text = "This week $timeText · Checks ${item.weeklySessionCount}"
            }

            // Active days: "Active 4/7 days · Last Today"
            val hasActiveDays = item.activeDays > 0 || item.lastTouchedDate != null
            binding.tvGoalActiveDays.isVisible = hasActiveDays
            if (hasActiveDays) {
                val activeText = "Active ${item.activeDays}/7 days"
                val lastText = item.lastTouchedDate?.let { formatLastTouched(it) } ?: ""
                binding.tvGoalActiveDays.text = if (lastText.isNotEmpty()) "$activeText · Last $lastText" else activeText
            }

            // Progress bar for weekly consistency
            binding.pbGoalProgress.isVisible = hasStats
            binding.pbGoalProgress.progress = item.progressPercent
            binding.tvGoalProgressLabel.isVisible = hasStats
            if (hasStats) {
                binding.tvGoalProgressLabel.text = "Weekly Consistency ${item.progressPercent}%"
            }

            // Status badge (always show "In progress" for active goals)
            binding.chipGoalStatus.isVisible = hasStats
            binding.chipGoalStatus.text = "In progress"

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick?.invoke(item)
                onLongClick != null
            }
        }

        private fun formatDuration(millis: Long): String {
            val totalMin = (millis / 60_000L).coerceAtLeast(0)
            val h = totalMin / 60
            val m = totalMin % 60
            return when {
                h <= 0 -> "${m}m"
                m == 0L -> "${h}h"
                else -> "${h}h ${m}m"
            }
        }

        private fun formatLastTouched(date: LocalDate): String {
            val today = LocalDate.now()
            return when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                date.isAfter(today.minusDays(7)) -> {
                    val formatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                    date.format(formatter)
                }
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    date.format(formatter)
                }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GoalRowUiModel>() {
        override fun areItemsTheSame(oldItem: GoalRowUiModel, newItem: GoalRowUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GoalRowUiModel, newItem: GoalRowUiModel): Boolean =
            oldItem == newItem
    }
}
