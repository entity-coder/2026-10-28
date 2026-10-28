package com.dairymanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dairymanager.app.data.CollectionEntry
import com.dairymanager.app.data.Format
import com.dairymanager.app.databinding.ItemCollectionBinding

/**
 * One collection row. The farmer-name map is supplied by the caller, so
 * this adapter never queries the database itself; [updateNames] lets the
 * Collect screen refresh names without rebuilding the adapter.
 */
class CollectionAdapter(
    farmerNames: Map<Long, String>,
    private val onDelete: ((CollectionEntry) -> Unit)?
) : RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    private var entries: List<CollectionEntry> = emptyList()
    private var names: Map<Long, String> = farmerNames

    fun submitList(list: List<CollectionEntry>) {
        entries = list
        notifyDataSetChanged()
    }

    fun updateNames(newNames: Map<Long, String>) {
        names = newNames
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemCollectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val farmer = names[entry.farmerId] ?: "Farmer ${entry.farmerId}"
        // Blank names (the detail screen already names the farmer) are
        // dropped, so the line never starts with a stray separator.
        holder.binding.collectionLineText.text =
            listOf(farmer, entry.shift, Format.litres(entry.litres))
                .filter { it.isNotBlank() }.joinToString(" • ")
        holder.binding.collectionDetailText.text =
            "Fat ${entry.fat} • SNF ${entry.snf} • ${Format.money(entry.ratePerLitre)}/L • " +
                Format.money(entry.amount) + " • " + Format.displayDate(entry.date)
        val deleteButton = holder.binding.collectionDeleteButton
        if (onDelete == null) {
            deleteButton.visibility = ViewGroup.GONE
        } else {
            deleteButton.visibility = ViewGroup.VISIBLE
            deleteButton.setOnClickListener { onDelete.invoke(entry) }
        }
    }

    override fun getItemCount(): Int = entries.size
}
