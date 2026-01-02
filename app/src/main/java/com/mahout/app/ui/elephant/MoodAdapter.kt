package com.mahout.app.ui.elephant

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.R

class MoodAdapter(
    private val onMoodSelected: (MoodUi) -> Unit
) : ListAdapter<MoodUi, MoodAdapter.VH>(Diff) {

    private var selectedMoodId: String? = null

    /**
     * Updates ONLY the old + new selected rows (no full list refresh).
     * This removes a lot of "jank" when selection changes quickly.
     */
    fun setSelectedMoodId(id: String?) {
        val oldId = selectedMoodId
        if (oldId == id) return

        selectedMoodId = id

        val oldIndex = currentList.indexOfFirst { it.id == oldId }
        val newIndex = currentList.indexOfFirst { it.id == id }

        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood_icon, parent, false)
        return VH(v, onMoodSelected)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item, isSelected = (item.id == selectedMoodId))
    }

    class VH(
        itemView: View,
        private val onMoodSelected: (MoodUi) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.ivMoodIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvMoodName)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivMoodCheck)

        private var mood: MoodUi? = null

        init {
            itemView.setOnClickListener { mood?.let(onMoodSelected) }
        }

        fun bind(item: MoodUi, isSelected: Boolean) {
            mood = item

            ivIcon.setImageResource(item.iconRes)
            tvName.text = item.label
            ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            // "Not now" elephant icon premium sizing (your earlier request)
            if (item.id == MoodCatalog.SKIP_MOOD_ID) {
                ivIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                setIconSizeDp(88)
                ivIcon.alpha = 0.95f
            } else {
                ivIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                setIconSizeDp(72)
                ivIcon.alpha = 1.0f
            }
        }

        private fun setIconSizeDp(dp: Int) {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                itemView.resources.displayMetrics
            ).toInt()

            val lp = ivIcon.layoutParams
            if (lp.width != px || lp.height != px) {
                lp.width = px
                lp.height = px
                ivIcon.layoutParams = lp
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<MoodUi>() {
        override fun areItemsTheSame(oldItem: MoodUi, newItem: MoodUi): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MoodUi, newItem: MoodUi): Boolean = oldItem == newItem
    }
}
