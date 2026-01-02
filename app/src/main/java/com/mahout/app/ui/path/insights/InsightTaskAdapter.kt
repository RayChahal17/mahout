package com.mahout.app.ui.path.insights

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemInsightTaskBinding
import com.mahout.app.ui.path.timeline.ActionColors
import java.util.concurrent.TimeUnit

class InsightTaskAdapter : ListAdapter<TaskUiModel, InsightTaskAdapter.VH>(Diff) {

    class VH(
        private val binding: ItemInsightTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TaskUiModel, maxMillis: Long) {
            binding.tvTaskTitle.text = item.title
            binding.tvTaskDuration.text = formatDuration(item.totalMillis)

            // Set color indicator as hollow sphere based on action ID
            val color = ActionColors.forActionId(item.actionId)
            val gd = (binding.viewColorIndicator.background as? GradientDrawable)
                ?.mutate() as? GradientDrawable
            gd?.setColor(Color.TRANSPARENT)
            gd?.setStroke(dp(binding.viewColorIndicator, 2f), color)

            // Calculate progress percentage (0-100) - all bars same grey color
            val progress = if (maxMillis > 0) {
                ((item.totalMillis * 100) / maxMillis).toInt().coerceIn(0, 100)
            } else {
                0
            }
            binding.progressBar.progress = progress
            // All progress bars use the same grey color (not individual action colors)
            binding.progressBar.progressTintList = ColorStateList.valueOf(0xFFD0D0D0.toInt())
        }

        private fun dp(view: View, v: Float): Int {
            val d = view.resources.displayMetrics.density
            return (v * d).toInt().coerceAtLeast(1)
        }

        private fun formatDuration(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "0m"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemInsightTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val maxMillis = currentList.maxOfOrNull { it.totalMillis } ?: 1L
        holder.bind(item, maxMillis)
    }

    private object Diff : DiffUtil.ItemCallback<TaskUiModel>() {
        override fun areItemsTheSame(old: TaskUiModel, new: TaskUiModel): Boolean {
            return old.actionId == new.actionId
        }

        override fun areContentsTheSame(old: TaskUiModel, new: TaskUiModel): Boolean {
            return old == new
        }
    }
}

