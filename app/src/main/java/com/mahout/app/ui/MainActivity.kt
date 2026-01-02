package com.mahout.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.mahout.app.R
import com.mahout.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Toolbar back button should behave like "navigate up"
        binding.topAppBar.setNavigationOnClickListener { onSupportNavigateUp() }

        // Standard bottom nav wiring (keeps back stack behavior consistent)
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        // =========================
        // Phase 2: Center Elephant raised action
        // =========================
        binding.cardElephantFab.setOnClickListener {
            // Use selectedItemId so NavigationUI performs the navigation the same way
            // as all other tabs (and keeps UI selection in sync).
            if (binding.bottomNav.selectedItemId != R.id.elephantFragment) {
                binding.bottomNav.selectedItemId = R.id.elephantFragment
            }
        }

        // Keep the center button feeling "alive" when you are on Elephant tab.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val onElephant = destination.id == R.id.elephantFragment
            animateElephantFabSelected(onElephant)
        }

        // Optional: if you want the dock to respect system navigation bar insets (gesture mode),
        // we can push the dock up slightly based on insets.
        // This prevents the dock from feeling cramped on some devices.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Keep your content safe; bottom dock already has marginBottom, so we just add extra if needed.
            binding.root.updatePadding(bottom = sysBars.bottom)
            insets
        }
    }

    /**
     * Small premium micro-interaction:
     * - Selected: slightly larger + slightly higher elevation (feels "active")
     * - Not selected: normal size
     *
     * Why animate instead of state-list resources?
     * - Fast to iterate
     * - No extra XML animator files needed
     */
    private fun animateElephantFabSelected(selected: Boolean) {
        val targetScale = if (selected) 1.06f else 1.0f

        binding.cardElephantFab.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(160L)
            .start()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
