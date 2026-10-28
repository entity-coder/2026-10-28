package com.dairymanager.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dairymanager.app.R
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.Format
import com.dairymanager.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The counter view: milk in today (split by shift), how many farmers
 * delivered, what is still owed overall, and milk this month. Nothing here
 * edits data — it only shows the state of the dairy at a glance.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dao = DairyDatabase.get(requireContext()).dairyDao()
        val today = Format.today()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        dao.litresForDate(today),
                        dao.litresForDateShift(today, "Morning"),
                        dao.litresForDateShift(today, "Evening"),
                        dao.farmersForDate(today),
                        dao.litresForMonth(Format.monthPrefix() + "%"),
                        dao.totalCollectionAmount(),
                        dao.totalPayments()
                    ) { todayLitres, morning, evening, farmersToday, monthLitres, total, paid ->
                        DashboardState(
                            todayLitres = todayLitres,
                            morningLitres = morning,
                            eveningLitres = evening,
                            farmersToday = farmersToday,
                            monthLitres = monthLitres,
                            pending = total - paid
                        )
                    }.collect { state ->
                        binding.todayLitresValue.text = Format.litres(state.todayLitres)
                        binding.morningLitresValue.text =
                            getString(R.string.morning_litres) + ": " + Format.litres(state.morningLitres)
                        binding.eveningLitresValue.text =
                            getString(R.string.evening_litres) + ": " + Format.litres(state.eveningLitres)
                        binding.farmersTodayValue.text = state.farmersToday.toString()
                        binding.pendingPaymentsValue.text = Format.money(state.pending)
                        binding.monthLitresValue.text = Format.litres(state.monthLitres)
                    }
                }
            }
        }
    }

    private data class DashboardState(
        val todayLitres: Double,
        val morningLitres: Double,
        val eveningLitres: Double,
        val farmersToday: Int,
        val monthLitres: Double,
        val pending: Double
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
