package com.mahout.app.ui.path

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemActionBinding
import com.mahout.app.databinding.ItemSectionHeaderBinding
import com.mahout.app.domain.path.model.Action

/**
 * Sectioned adapter for Path Actions screen.
 *
 * Rows:
 * - HeaderRow (spans full width in GridLayoutManager via SpanSizeLookup)
 * - ActionRow (grid card)
 * - MessageRow (spans full width)
 *
 * Day 12/13-safe:
 * - We do NOT compute session totals here.
 * - Fragment computes progress + button state and passes them in PathRow.ActionRow.
 */
class ActionListAdapter(
    private val onActionClick: (Action) -> Unit,
    private val onActionLongClick: (Action) -> Unit,
    private val onActionTimerClick: (Action) -> Unit,
    private val onToggleCardSize: () -> Unit
) : ListAdapter<PathRow, RecyclerView.ViewHolder>(Diff) {

    /**
     * Controls whether cards show full details (meta/progress)
     * or just title + Start/Stop.
     */
    enum class CardSize { COMPACT, EXPANDED }

    // Default = expanded, like your design (more info visible).
    private var cardSize: CardSize = CardSize.EXPANDED

    /**
     * ✅ Called by PathFragment via: onToggleCardSize = { adapter.toggleCardSize() }
     * This toggles the adapter's internal state and refreshes rows.
     */
    fun toggleCardSize() {
        cardSize = if (cardSize == CardSize.EXPANDED) CardSize.COMPACT else CardSize.EXPANDED
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PathRow.HeaderRow -> VIEW_HEADER
            is PathRow.ActionRow -> VIEW_ACTION
            is PathRow.MessageRow -> VIEW_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_HEADER -> {
                val binding = ItemSectionHeaderBinding.inflate(inflater, parent, false)
                HeaderVH(binding, onToggleCardSize)
            }

            VIEW_MESSAGE -> {
                // Reuse the same header layout for a message row.
                // We hide the toggle button.
                val binding = ItemSectionHeaderBinding.inflate(inflater, parent, false)
                MessageVH(binding)
            }

            else -> {
                val binding = ItemActionBinding.inflate(inflater, parent, false)
                ActionVH(
                    binding = binding,
                    onClick = onActionClick,
                    onLongClick = onActionLongClick,
                    onTimerClick = onActionTimerClick,
                    cardSizeProvider = { cardSize }
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is PathRow.HeaderRow -> (holder as HeaderVH).bind(row)
            is PathRow.ActionRow -> (holder as ActionVH).bind(row)
            is PathRow.MessageRow -> (holder as MessageVH).bind(row)
        }
    }

    private class HeaderVH(
        private val binding: ItemSectionHeaderBinding,
        private val onToggleCardSize: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: PathRow.HeaderRow) {
            binding.tvHeaderTitle.text = row.title

            // Only show the expand/compact icon where desired (usually "Today’s actions")
            binding.btnHeaderToggle.isVisible = row.showSizeToggle
            binding.btnHeaderToggle.setOnClickListener { onToggleCardSize() }
        }
    }

    private class MessageVH(
        private val binding: ItemSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: PathRow.MessageRow) {
            binding.tvHeaderTitle.text = row.message
            binding.btnHeaderToggle.isVisible = false
        }
    }

    private class ActionVH(
        private val binding: ItemActionBinding,
        private val onClick: (Action) -> Unit,
        private val onLongClick: (Action) -> Unit,
        private val onTimerClick: (Action) -> Unit,
        private val cardSizeProvider: () -> CardSize
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: PathRow.ActionRow) {
            val action = row.action

            // Basic content
            binding.tvActionTitle.text = action.title
            binding.tvActionMeta.text = row.metaText

            // Progress UI (values already computed in Fragment)
            binding.pbProgress.max = 100
            binding.pbProgress.progress = row.progressPercent
            binding.tvProgressLabel.text = row.progressLabel

            // Timer button UI (values already computed in Fragment)
            binding.btnTimer.text = row.timerButtonText
            binding.btnTimer.isEnabled = row.timerButtonEnabled

            // ✅ Density toggle: compact hides meta + progress.
            val compact = (cardSizeProvider() == CardSize.COMPACT)
            binding.tvActionMeta.isVisible = !compact
            binding.pbProgress.isVisible = !compact
            binding.tvProgressLabel.isVisible = !compact

            // Clicks
            binding.root.setOnClickListener { onClick(action) }
            binding.root.setOnLongClickListener {
                onLongClick(action)
                true
            }
            binding.btnTimer.setOnClickListener { onTimerClick(action) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PathRow>() {
        override fun areItemsTheSame(old: PathRow, new: PathRow): Boolean {
            return when {
                old is PathRow.HeaderRow && new is PathRow.HeaderRow -> old.title == new.title
                old is PathRow.MessageRow && new is PathRow.MessageRow -> old.message == new.message
                old is PathRow.ActionRow && new is PathRow.ActionRow -> old.action.id == new.action.id
                else -> false
            }
        }

        override fun areContentsTheSame(old: PathRow, new: PathRow): Boolean = old == new
    }

    private companion object {
        private const val VIEW_HEADER = 1
        private const val VIEW_ACTION = 2
        private const val VIEW_MESSAGE = 3
    }
}

/**
 * List rows rendered by ActionListAdapter.
 */
sealed class PathRow {
    data class HeaderRow(
        val title: String,
        val showSizeToggle: Boolean
    ) : PathRow()

    data class ActionRow(
        val action: Action,
        val metaText: String,
        val progressPercent: Int,
        val progressLabel: String,
        val timerButtonText: String,
        val timerButtonEnabled: Boolean
    ) : PathRow()

    data class MessageRow(
        val message: String
    ) : PathRow()
}
