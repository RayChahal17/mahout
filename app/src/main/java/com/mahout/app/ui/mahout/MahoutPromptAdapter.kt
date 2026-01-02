package com.mahout.app.ui.mahout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mahout.app.R

class MahoutPromptAdapter(
    private val onPromptClicked: (MahoutPromptUi) -> Unit
) : ListAdapter<MahoutPromptUi, MahoutPromptAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mahout_prompt, parent, false)
        return VH(v, onPromptClicked)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onPromptClicked: (MahoutPromptUi) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val card: MaterialCardView = itemView.findViewById(R.id.cardPrompt)
        private val tv: TextView = itemView.findViewById(R.id.tvPrompt)

        private var model: MahoutPromptUi? = null

        init {
            card.setOnClickListener {
                model?.let(onPromptClicked)
            }
        }

        fun bind(item: MahoutPromptUi) {
            model = item
            tv.text = item.text
        }
    }

    private object Diff : DiffUtil.ItemCallback<MahoutPromptUi>() {
        override fun areItemsTheSame(oldItem: MahoutPromptUi, newItem: MahoutPromptUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MahoutPromptUi, newItem: MahoutPromptUi): Boolean =
            oldItem == newItem
    }
}
