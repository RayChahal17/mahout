package com.mahout.app.ui.path

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mahout.app.databinding.ItemActionBinding
import com.mahout.app.domain.path.model.Action

class ActionListAdapter : ListAdapter<Action, ActionListAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Action) {
            binding.tvActionTitle.text = item.title
            binding.tvActionMeta.text = item.cadence.toString()
        }
    }

    private object Diff : DiffUtil.ItemCallback<Action>() {
        override fun areItemsTheSame(old: Action, new: Action): Boolean = old.id == new.id
        override fun areContentsTheSame(old: Action, new: Action): Boolean = old == new
    }
}
