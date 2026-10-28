package com.dairymanager.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dairymanager.app.R
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.Format
import com.dairymanager.app.data.Payment
import com.dairymanager.app.data.SheetRow
import com.dairymanager.app.data.buildSheet
import com.dairymanager.app.databinding.FragmentSheetsBinding
import kotlinx.coroutines.launch

/**
 * Payment sheets: pick a period, see every farmer's opening balance,
 * milk and gross for the period, payments attributed to it, and what is
 * still pending. Paying from a row records a period-attributed payment
 * capped at that row's pending, so a sheet can never be overpaid. The
 * sheet can be shared or printed as a PDF.
 */
class SheetsFragment : Fragment() {

    private var _binding: FragmentSheetsBinding? = null
    private val binding get() = _binding!!

    private var currentRows: List<SheetRow> = emptyList()
    private var currentStart = ""
    private var currentEnd = ""
    private var dairyName = "My Dairy"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSheetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.periodStartInput.setText(Format.firstDayOfMonth())
        binding.periodEndInput.setText(Format.today())

        val adapter = SheetAdapter { row -> showPaymentDialog(row) }
        binding.sheetList.adapter = adapter

        binding.loadSheetButton.setOnClickListener { loadSheet(adapter) }
        binding.shareSheetButton.setOnClickListener {
            if (currentRows.isEmpty()) {
                Toast.makeText(requireContext(), R.string.sheet_empty, Toast.LENGTH_SHORT).show()
            } else {
                SheetPdf.share(requireContext(), dairyName, currentStart, currentEnd, currentRows)
            }
        }
        binding.printSheetButton.setOnClickListener {
            if (currentRows.isEmpty()) {
                Toast.makeText(requireContext(), R.string.sheet_empty, Toast.LENGTH_SHORT).show()
            } else {
                SheetPdf.print(requireContext(), dairyName, currentStart, currentEnd, currentRows)
            }
        }
        loadSheet(adapter)
    }

    private fun loadSheet(adapter: SheetAdapter) {
        val start = binding.periodStartInput.text.toString().trim()
        val end = binding.periodEndInput.text.toString().trim()
        if (!Format.isValidDate(start) || !Format.isValidDate(end) || start > end) {
            Toast.makeText(requireContext(), R.string.sheet_invalid_period, Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = DairyDatabase.get(requireContext()).dairyDao()
            dairyName = dao.getSettingsOnce()?.dairyName ?: "My Dairy"
            val rows = buildSheet(
                farmers = dao.allFarmersOnce(),
                collections = dao.collectionsInRange(start, end),
                payments = dao.allPaymentsOnce(),
                periodStart = start,
                periodEnd = end
            )
            currentRows = rows
            currentStart = start
            currentEnd = end
            adapter.submitList(rows)
            binding.sheetEmpty.visibility =
                if (rows.isEmpty()) View.VISIBLE else View.GONE
            val totalLitres = rows.sumOf { it.litres }
            val totalGross = rows.sumOf { it.gross }
            val totalPending = rows.sumOf { it.pending }
            binding.sheetTotalsText.text =
                "Period: ${Format.displayDate(start)} – ${Format.displayDate(end)}
" +
                    "Total: ${Format.litres(totalLitres)} • Gross ${Format.money(totalGross)} • " +
                    "Pending ${Format.money(totalPending)}"
        }
    }

    private fun showPaymentDialog(row: SheetRow) {
        if (row.pending <= 0.0) {
            Toast.makeText(requireContext(), R.string.nothing_pending, Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.paymentAmountInput)
        val methodSpinner = dialogView.findViewById<Spinner>(R.id.paymentMethodSpinner)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.record_payment) + " — " + row.farmerName)
            .setView(dialogView)
            .setPositiveButton(R.string.record) { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    Toast.makeText(requireContext(), R.string.payment_positive, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (amount > row.pending) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.payment_exceeds_pending, Format.money(row.pending)),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }
                val method = methodSpinner.selectedItem?.toString() ?: "Cash"
                viewLifecycleOwner.lifecycleScope.launch {
                    val dao = DairyDatabase.get(requireContext()).dairyDao()
                    dao.insertPayment(
                        Payment(
                            farmerId = row.farmerId,
                            periodStart = currentStart,
                            periodEnd = currentEnd,
                            amount = Format.round2(amount),
                            method = method
                        )
                    )
                    Toast.makeText(requireContext(), R.string.payment_recorded, Toast.LENGTH_SHORT).show()
                    loadSheet(binding.sheetList.adapter as SheetAdapter)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
