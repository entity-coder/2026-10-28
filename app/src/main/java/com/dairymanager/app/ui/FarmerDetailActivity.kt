package com.dairymanager.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dairymanager.app.R
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.Format
import com.dairymanager.app.databinding.ActivityFarmerDetailBinding
import kotlinx.coroutines.launch

/**
 * One farmer: contact/bank details, lifetime totals, and milk history.
 * Deleting is refused when milk records exist — hiding is the safe action
 * then, because payments reference the farmer's history. Hiding and
 * deleting both return the user to the previous screen.
 */
class FarmerDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FARMER_ID = "farmer_id"
    }

    private lateinit var binding: ActivityFarmerDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val farmerId = intent.getLongExtra(EXTRA_FARMER_ID, -1L)
        if (farmerId == -1L) {
            finish()
            return
        }
        val dao = DairyDatabase.get(this).dairyDao()

        lifecycleScope.launch {
            val farmer = dao.farmerById(farmerId)
            if (farmer == null) {
                finish()
                return@launch
            }
            binding.farmerDetailName.text = farmer.name
            val meta = listOf(
                farmer.village,
                farmer.phone,
                if (farmer.bankAccount.isNotBlank()) "A/C ${farmer.bankAccount}" else "",
                farmer.ifsc
            ).filter { it.isNotBlank() }.joinToString(" • ")
            binding.farmerDetailMeta.text = meta
            binding.hideShowButton.text =
                if (farmer.active) getString(R.string.hide) else getString(R.string.show)
            binding.hideShowButton.setOnClickListener {
                lifecycleScope.launch {
                    dao.updateFarmer(farmer.copy(active = !farmer.active))
                    finish()
                }
            }
            binding.deleteFarmerButton.setOnClickListener {
                lifecycleScope.launch {
                    val litres = dao.totalLitresForFarmer(farmerId)
                    if (litres > 0.0) {
                        Toast.makeText(
                            this@FarmerDetailActivity,
                            R.string.cannot_delete_farmer,
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    AlertDialog.Builder(this@FarmerDetailActivity)
                        .setMessage(R.string.delete_farmer_message)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lifecycleScope.launch {
                                dao.deleteFarmer(farmerId)
                                Toast.makeText(
                                    this@FarmerDetailActivity,
                                    R.string.farmer_deleted,
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
            val litres = dao.totalLitresForFarmer(farmerId)
            val gross = dao.totalAmountForFarmer(farmerId)
            val paid = dao.totalPaymentsForFarmer(farmerId)
            binding.farmerDetailTotals.text =
                "${getString(R.string.total_litres)}: ${Format.litres(litres)}
" +
                    "${getString(R.string.total_amount)}: ${Format.money(gross)}
" +
                    "${getString(R.string.total_paid)}: ${Format.money(paid)}
" +
                    "${getString(R.string.total_pending)}: ${Format.money(gross - paid)}"
        }

        val adapter = CollectionAdapter(mapOf(farmerId to ""), null)
        binding.historyList.adapter = adapter
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.collectionsForFarmer(farmerId).collect { entries ->
                    // Names are blank here on purpose: the screen already
                    // names the farmer, so rows show date/shift/rate instead.
                    adapter.submitList(entries)
                    binding.historyEmpty.visibility =
                        if (entries.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
