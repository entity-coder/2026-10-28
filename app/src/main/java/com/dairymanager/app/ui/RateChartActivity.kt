package com.dairymanager.app.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dairymanager.app.R
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.RateSlab
import com.dairymanager.app.data.Settings
import com.dairymanager.app.databinding.ActivityRateChartBinding
import kotlinx.coroutines.launch

/**
 * Rate chart + settings: edit the dairy name and default rate, add or
 * delete slabs. Slab edits never rewrite history — collections store the
 * rate they were saved with — so changing the chart only affects future
 * collections, which is exactly what a dairy wants at a rate revision.
 */
class RateChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRateChartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRateChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dao = DairyDatabase.get(this).dairyDao()

        val adapter = SlabAdapter { slab ->
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_slab)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch { dao.deleteSlab(slab.id) }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.slabList.adapter = adapter

        lifecycleScope.launch {
            val settings = dao.getSettingsOnce()
            if (settings != null) {
                binding.dairyNameInput.setText(settings.dairyName)
                binding.defaultRateInput.setText(settings.defaultRate.toString())
            }
        }

        binding.saveSettingsButton.setOnClickListener {
            val name = binding.dairyNameInput.text.toString().trim()
            val rate = binding.defaultRateInput.text.toString().toDoubleOrNull()
            if (name.isBlank() || rate == null || rate <= 0.0) {
                Toast.makeText(this, R.string.settings_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val existing = dao.getSettingsOnce()
                if (existing == null) dao.insertSettings(Settings(dairyName = name, defaultRate = rate))
                else dao.updateSettings(existing.copy(dairyName = name, defaultRate = rate))
                Toast.makeText(this@RateChartActivity, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }
        }

        binding.addSlabButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_slab, null)
            val minFat = dialogView.findViewById<EditText>(R.id.slabMinFatInput)
            val maxFat = dialogView.findViewById<EditText>(R.id.slabMaxFatInput)
            val minSnf = dialogView.findViewById<EditText>(R.id.slabMinSnfInput)
            val maxSnf = dialogView.findViewById<EditText>(R.id.slabMaxSnfInput)
            val rate = dialogView.findViewById<EditText>(R.id.slabRateInput)
            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save) { _, _ ->
                    val loFat = minFat.text.toString().toDoubleOrNull()
                    val hiFat = maxFat.text.toString().toDoubleOrNull()
                    val loSnf = minSnf.text.toString().toDoubleOrNull()
                    val hiSnf = maxSnf.text.toString().toDoubleOrNull()
                    val rateValue = rate.text.toString().toDoubleOrNull()
                    if (loFat == null || hiFat == null || loSnf == null || hiSnf == null ||
                        rateValue == null || loFat >= hiFat || loSnf >= hiSnf || rateValue <= 0.0
                    ) {
                        Toast.makeText(this, R.string.slab_invalid, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    lifecycleScope.launch {
                        dao.insertSlab(
                            RateSlab(
                                minFat = loFat, maxFat = hiFat,
                                minSnf = loSnf, maxSnf = hiSnf,
                                ratePerLitre = rateValue
                            )
                        )
                        Toast.makeText(this@RateChartActivity, R.string.slab_saved, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.allSlabs().collect { slabs ->
                    adapter.submitList(slabs)
                    binding.slabEmpty.visibility =
                        if (slabs.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
