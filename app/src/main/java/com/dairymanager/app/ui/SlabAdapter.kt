package com.dairymanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dairymanager.app.data.Format
import com.dairymanager.app.data.RateSlab
import com.dairymanager.app.databinding.ItemSlabBinding

/** One rate-chart row with a delete button. */
class SlabAdapter(
    private val onDelete: (RateSlab) -> Unit
) : RecyclerView.Adapter<SlabAdapter.ViewHolder>() {

    private var slabs: List<RateSlab> = emptyList()

    fun submitList(list: List<RateSlab>) {
        slabs = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemSlabBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSlabBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slab = slabs[position]
        holder.binding.slabLineText.text =
            "Fat ${slab.minFat}–${slab.maxFat} • SNF ${slab.minSnf}–${slab.maxSnf} → " +
                "${Format.money(slab.ratePerLitre)}/L"
        holder.binding.slabDeleteButton.setOnClickListener { onDelete(slab) }
    }

    override fun getItemCount(): Int = slabs.size
}
