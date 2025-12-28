package com.mahout.app.ui.path

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.mahout.app.databinding.ItemActionBinding
import com.mahout.app.databinding.ItemSectionHeaderBinding
import com.mahout.app.domain.path.model.Action
import com.mahout.app.ui.path.timeline.ActionColors

class ActionListAdapter(
    private val onActionClick: (Action) -> Unit,
    private val onActionLongClick: (Action) -> Unit,
    private val onActionTimerClick: (Action) -> Unit,
    private val onToggleCardSize: () -> Unit
) : ListAdapter<PathRow, RecyclerView.ViewHolder>(Diff) {

    enum class CardSize { COMPACT, EXPANDED }
    private var cardSize: CardSize = CardSize.EXPANDED

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

        private fun dp(v: Float): Int {
            val d = binding.root.resources.displayMetrics.density
            return (v * d).toInt()
        }

        fun bind(row: PathRow.ActionRow) {
            val action = row.action

            // ✅ One source of truth (same color everywhere)
            val accent = ActionColors.forActionId(action.id)

            val card = (binding.root as? MaterialCardView)

            val surface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)
            val onSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
            val onSurfaceVar = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurfaceVariant)
            val surfaceVar = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant)

            // --- Premium card treatment ---
            // subtle background tint (keeps it premium, not neon)
            val tintedBg = ColorUtils.blendARGB(surfaceVar, accent, 0.06f)
            card?.setCardBackgroundColor(tintedBg)

            // thin accent stroke instead of thick neon border
            card?.strokeWidth = dp(1.4f)
            card?.strokeColor = ColorUtils.setAlphaComponent(accent, 0xD0)
            card?.cardElevation = dp(2f).toFloat()

            // --- Text ---
            binding.tvActionTitle.text = action.title
            // keep readable: blend accent into onSurface (not pure neon)
            binding.tvActionTitle.setTextColor(ColorUtils.blendARGB(onSurface, accent, 0.70f))

            binding.tvActionMeta.text = row.metaText
            binding.tvActionMeta.setTextColor(onSurfaceVar)

            // --- Progress ---
            binding.pbProgress.max = 100
            binding.pbProgress.progress = row.progressPercent

            binding.tvProgressLabel.text = row.progressLabel
            binding.tvProgressLabel.setTextColor(ColorUtils.setAlphaComponent(accent, 0xB8))

            binding.pbProgress.progressTintList = ColorStateList.valueOf(accent)
            binding.pbProgress.progressBackgroundTintList = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(onSurfaceVar, 0x20)
            )

            // --- Timer button ---
            binding.btnTimer.text = row.timerButtonText
            binding.btnTimer.isEnabled = row.timerButtonEnabled

            val disabledText = ColorUtils.setAlphaComponent(onSurfaceVar, 0xAA)
            val enabledText = ColorUtils.blendARGB(onSurface, accent, 0.75f)

            binding.btnTimer.setTextColor(if (row.timerButtonEnabled) enabledText else disabledText)

            // outlined look: accent stroke + subtle tint fill
            binding.btnTimer.strokeWidth = dp(1.2f)
            binding.btnTimer.strokeColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 0xCC))
            binding.btnTimer.backgroundTintList = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(accent, if (row.timerButtonEnabled) 0x18 else 0x10)
            )

            // --- Compact / expanded ---
            val compact = (cardSizeProvider() == CardSize.COMPACT)
            binding.tvActionMeta.isVisible = !compact
            binding.pbProgress.isVisible = !compact
            binding.tvProgressLabel.isVisible = !compact

            // --- Clicks ---
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
        val timerButtonEnabled: Boolean,
        val isOverTarget: Boolean
    ) : PathRow()

    data class MessageRow(
        val message: String
    ) : PathRow()
}
