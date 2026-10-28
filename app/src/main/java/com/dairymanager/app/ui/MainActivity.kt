package com.dairymanager.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dairymanager.app.R
import com.dairymanager.app.data.DairyDatabase
import com.dairymanager.app.data.Settings
import com.dairymanager.app.data.defaultSlabs
import com.dairymanager.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Single-activity shell: four tabs, switched with fragment transactions.
 * A dairy phone is used one-handed at the collection counter, so bottom
 * navigation — not a drawer — is the right call.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // First launch: seed the settings row and the starter rate chart.
        // Seeding here (not in a Room callback) keeps it visible and testable.
        lifecycleScope.launch {
            val dao = DairyDatabase.get(this@MainActivity).dairyDao()
            if (dao.getSettingsOnce() == null) {
                dao.insertSettings(Settings(dairyName = "My Dairy", defaultRate = 30.0))
                defaultSlabs().forEach { dao.insertSlab(it) }
            }
        }

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(DashboardFragment())
                R.id.nav_farmers -> showFragment(FarmersFragment())
                R.id.nav_collect -> showFragment(CollectFragment())
                R.id.nav_sheets -> showFragment(SheetsFragment())
                else -> false
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}
