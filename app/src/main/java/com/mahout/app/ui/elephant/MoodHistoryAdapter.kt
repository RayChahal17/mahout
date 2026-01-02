package com.mahout.app.ui.elephant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.R

/**
 * Adapter that renders a small “Recent” mood list inside the Elephant overlay.
 *
 * This is intentionally simple and stable:
 * - Headers for grouping (Today / Yesterday / date)
 * - Entries with icon + label + time + optional note
 * - Delete button on each entry (confirmation happens in the Fragment)
 */
class MoodHistoryAdapter(
    private val onDeleteClick: (moodLogId: String) -> Unit
) : ListAdapter<MoodHistoryRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MoodHistoryRow.Header -> VIEW_TYPE_HEADER
        is MoodHistoryRow.Entry -> VIEW_TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val v = inflater.inflate(R.layout.item_mood_history_header, parent, false)
                HeaderVH(v)
            }
            else -> {
                val v = inflater.inflate(R.layout.item_mood_history_entry, parent, false)
                EntryVH(v, onDeleteClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MoodHistoryRow.Header -> (holder as HeaderVH).bind(item)
            is MoodHistoryRow.Entry -> (holder as EntryVH).bind(item)
        }
    }

    private class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv = itemView.findViewById<TextView>(R.id.tvMoodHistoryHeader)
        fun bind(item: MoodHistoryRow.Header) {
            tv.text = item.title
        }
    }

    private class EntryVH(
        itemView: View,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon = itemView.findViewById<ImageView>(R.id.ivMoodHistoryIcon)
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvMoodHistoryTitle)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvMoodHistoryTime)
        private val tvNote = itemView.findViewById<TextView>(R.id.tvMoodHistoryNote)
        private val btnDelete = itemView.findViewById<ImageButton>(R.id.btnMoodHistoryDelete)

        private var currentId: String? = null

        init {
            btnDelete.setOnClickListener {
                val id = currentId ?: return@setOnClickListener
                onDeleteClick(id)
            }
        }

        fun bind(item: MoodHistoryRow.Entry) {
            currentId = item.id
            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.moodLabel
            tvTime.text = item.timeLabel

            val note = item.note?.trim().orEmpty()
            if (note.isEmpty()) {
                tvNote.visibility = View.GONE
                tvNote.text = ""
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = note
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<MoodHistoryRow>() {
        override fun areItemsTheSame(oldItem: MoodHistoryRow, newItem: MoodHistoryRow): Boolean {
            return when {
                oldItem is MoodHistoryRow.Header && newItem is MoodHistoryRow.Header ->
                    oldItem.title == newItem.title
                oldItem is MoodHistoryRow.Entry && newItem is MoodHistoryRow.Entry ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: MoodHistoryRow, newItem: MoodHistoryRow): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_ENTRY = 2
    }
}
