package com.dairymanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dairymanager.app.R
import com.dairymanager.app.data.Farmer
import com.dairymanager.app.databinding.ItemFarmerBinding

/** Simple adapter: small farmer lists do not need DiffUtil machinery. */
class FarmerAdapter(
    private val onClick: (Farmer) -> Unit,
    private val onEdit: (Farmer) -> Unit,
    private val onToggle: (Farmer) -> Unit
) : RecyclerView.Adapter<FarmerAdapter.ViewHolder>() {

    private var farmers: List<Farmer> = emptyList()

    fun submitList(list: List<Farmer>) {
        farmers = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemFarmerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFarmerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val farmer = farmers[position]
        val context = holder.itemView.context
        holder.binding.farmerNameText.text = farmer.name
        val meta = listOf(farmer.village, farmer.phone)
            .filter { it.isNotBlank() }.joinToString(" • ")
        holder.binding.farmerMetaText.text = meta
        holder.binding.farmerStatusText.text =
            if (farmer.active) context.getString(R.string.active)
            else context.getString(R.string.hidden)
        holder.binding.farmerToggleButton.text =
            if (farmer.active) context.getString(R.string.hide)
            else context.getString(R.string.show)
        holder.itemView.setOnClickListener { onClick(farmer) }
        holder.binding.farmerEditButton.setOnClickListener { onEdit(farmer) }
        holder.binding.farmerToggleButton.setOnClickListener { onToggle(farmer) }
    }

    override fun getItemCount(): Int = farmers.size
}
