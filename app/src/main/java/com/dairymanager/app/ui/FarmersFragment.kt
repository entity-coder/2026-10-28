package com.dairymanager.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dairymanager.app.R
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.Farmer
import com.dairymanager.app.databinding.FragmentFarmersBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Farmer master: search, add, edit, hide/show. Tapping a row opens the
 * farmer's detail screen with history and totals. Deleting is only offered
 * on the detail screen, where the guard (no milk records) can be checked.
 */
class FarmersFragment : Fragment() {

    private var _binding: FragmentFarmersBinding? = null
    private val binding get() = _binding!!
    // One query flow, switched with flatMapLatest: typing never stacks up
    // collectors, and the list always reflects the latest search text.
    private val queryFlow = MutableStateFlow("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFarmersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dao = DairyDatabase.get(requireContext()).dairyDao()
        val adapter = FarmerAdapter(
            onClick = { farmer ->
                startActivity(
                    Intent(requireContext(), FarmerDetailActivity::class.java)
                        .putExtra(FarmerDetailActivity.EXTRA_FARMER_ID, farmer.id)
                )
            },
            onEdit = { farmer -> showFarmerDialog(farmer) },
            onToggle = { farmer ->
                viewLifecycleOwner.lifecycleScope.launch {
                    dao.updateFarmer(farmer.copy(active = !farmer.active))
                }
            }
        )
        binding.farmersList.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                queryFlow.value = s.toString()
            }
        })

        binding.addFarmerButton.setOnClickListener { showFarmerDialog(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                queryFlow.flatMapLatest { query ->
                    dao.searchFarmers("%$query%")
                }.collect { farmers ->
                    adapter.submitList(farmers)
                    binding.farmersEmpty.visibility =
                        if (farmers.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showFarmerDialog(existing: Farmer?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_farmer, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.farmerNameInput)
        val villageInput = dialogView.findViewById<EditText>(R.id.farmerVillageInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.farmerPhoneInput)
        val bankInput = dialogView.findViewById<EditText>(R.id.farmerBankInput)
        val ifscInput = dialogView.findViewById<EditText>(R.id.farmerIfscInput)
        if (existing != null) {
            nameInput.setText(existing.name)
            villageInput.setText(existing.village)
            phoneInput.setText(existing.phone)
            bankInput.setText(existing.bankAccount)
            ifscInput.setText(existing.ifsc)
        }
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), R.string.farmer_name_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val farmer = Farmer(
                    id = existing?.id ?: 0,
                    name = name,
                    village = villageInput.text.toString().trim(),
                    phone = phoneInput.text.toString().trim(),
                    bankAccount = bankInput.text.toString().trim(),
                    ifsc = ifscInput.text.toString().trim(),
                    active = existing?.active ?: true,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    val dao = DairyDatabase.get(requireContext()).dairyDao()
                    if (existing == null) dao.insertFarmer(farmer) else dao.updateFarmer(farmer)
                    Toast.makeText(requireContext(), R.string.farmer_saved, Toast.LENGTH_SHORT).show()
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
