package com.dairymanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dairymanager.app.data.Format
import com.dairymanager.app.data.SheetRow
import com.dairymanager.app.databinding.ItemSheetRowBinding

/** One payment-sheet row, with a Pay button that records against the sheet. */
class SheetAdapter(
    private val onPay: (SheetRow) -> Unit
) : RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

    private var rows: List<SheetRow> = emptyList()

    fun submitList(list: List<SheetRow>) {
        rows = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemSheetRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSheetRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.binding.sheetFarmerText.text = row.farmerName
        holder.binding.sheetNumbersText.text =
            "${Format.litres(row.litres)} • Gross ${Format.money(row.gross)} • " +
                "Paid ${Format.money(row.paidInPeriod)} • Pending ${Format.money(row.pending)}"
        holder.binding.sheetPayButton.setOnClickListener { onPay(row) }
    }

    override fun getItemCount(): Int = rows.size
}
