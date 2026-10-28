package com.dairymanager.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dairymanager.app.R
import com.dairymanager.app.data.CollectionEntry
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.Format
import com.dairymanager.app.data.RateSlab
import com.dairymanager.app.data.Settings
import com.dairymanager.app.data.lookupRate
import com.dairymanager.app.databinding.FragmentCollectBinding
import kotlinx.coroutines.launch

/**
 * The counter screen: pick a farmer, enter litres/fat/SNF, see the rate
 * live, save. The rate preview is computed from the same lookup the save
 * uses, so what the user sees is exactly what gets stored. One entry per
 * farmer per date per shift — a second save for the same shift is refused,
 * because a farmer delivers once per shift and a duplicate is almost
 * always a double-tap.
 */
class CollectFragment : Fragment() {

    private var _binding: FragmentCollectBinding? = null
    private val binding get() = _binding!!

    private var slabs: List<RateSlab> = emptyList()
    private var settings: Settings? = null
    private var farmerIdsByLabel: Map<String, Long> = emptyMap()
    private var farmerNames: Map<Long, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dao = DairyDatabase.get(requireContext()).dairyDao()
        val today = Format.today()
        binding.collectDateInput.setText(today)

        // One adapter for the whole visit: names refresh in place, so a
        // farmer added elsewhere still shows up on today's rows.
        val todayAdapter = CollectionAdapter(farmerNames) { entry ->
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.delete_collection)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        dao.deleteCollection(entry.id)
                        Toast.makeText(requireContext(), R.string.collection_deleted, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.todayList.adapter = todayAdapter

        val previewWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updatePreview()
        }
        binding.fatInput.addTextChangedListener(previewWatcher)
        binding.snfInput.addTextChangedListener(previewWatcher)

        binding.rateChartButton.setOnClickListener {
            startActivity(Intent(requireContext(), RateChartActivity::class.java))
        }

        binding.saveCollectionButton.setOnClickListener {
            val label = binding.farmerPicker.text.toString()
            val farmerId = farmerIdsByLabel[label]
            val date = binding.collectDateInput.text.toString().trim()
            val shift = binding.shiftSpinner.selectedItem?.toString() ?: "Morning"
            val litres = binding.litresInput.text.toString().toDoubleOrNull()
            val fat = binding.fatInput.text.toString().toDoubleOrNull()
            val snf = binding.snfInput.text.toString().toDoubleOrNull()
            if (farmerId == null || !Format.isValidDate(date) ||
                litres == null || litres <= 0.0 ||
                fat == null || fat < 0.0 || fat > 15.0 ||
                snf == null || snf < 0.0 || snf > 20.0
            ) {
                Toast.makeText(requireContext(), R.string.collection_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val existing = dao.existingEntry(farmerId, date, shift)
                if (existing != null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.duplicate_shift, shift, Format.displayDate(date)),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                val result = lookupRate(slabs, fat, snf, settings?.defaultRate ?: 0.0)
                val amount = Format.round2(litres * result.rate)
                dao.insertCollection(
                    CollectionEntry(
                        farmerId = farmerId,
                        date = date,
                        shift = shift,
                        litres = litres,
                        fat = fat,
                        snf = snf,
                        ratePerLitre = result.rate,
                        amount = amount
                    )
                )
                binding.litresInput.text.clear()
                binding.fatInput.text.clear()
                binding.snfInput.text.clear()
                Toast.makeText(requireContext(), R.string.collection_saved, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    dao.allFarmers().collect { farmers ->
                        val active = farmers.filter { it.active }
                        farmerIdsByLabel = active.associate { farmer ->
                            val label = if (farmer.village.isBlank()) farmer.name
                            else "${farmer.name} (${farmer.village})"
                            label to farmer.id
                        }
                        farmerNames = farmers.associate { it.id to it.name }
                        todayAdapter.updateNames(farmerNames)
                        binding.farmerPicker.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                farmerIdsByLabel.keys.toList()
                            )
                        )
                    }
                }
                launch {
                    dao.collectionsForDate(today).collect { entries ->
                        todayAdapter.submitList(entries)
                        binding.todayEmpty.visibility =
                            if (entries.isEmpty()) View.VISIBLE else View.GONE
                        val total = entries.sumOf { it.litres }
                        binding.todaySummaryText.text =
                            "Total: ${Format.litres(total)} • ${entries.size} entries"
                    }
                }
            }
        }
        // Rate chart + settings are read once per visit; edits made in the
        // RateChartActivity are picked up when the user returns here.
        viewLifecycleOwner.lifecycleScope.launch {
            slabs = dao.allSlabsOnce()
            settings = dao.getSettingsOnce()
            updatePreview()
        }
    }

    private fun updatePreview() {
        val fat = binding.fatInput.text.toString().toDoubleOrNull()
        val snf = binding.snfInput.text.toString().toDoubleOrNull()
        val defaultRate = settings?.defaultRate ?: 0.0
        if (fat == null || snf == null) {
            binding.ratePreviewText.text = getString(R.string.rate_preview_default)
            return
        }
        val result = lookupRate(slabs, fat, snf, defaultRate)
        binding.ratePreviewText.text =
            if (result.fromSlab) "Rate: ${Format.money(result.rate)}/L (from slab)"
            else "Rate: ${Format.money(result.rate)}/L (default rate — no slab matched)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
