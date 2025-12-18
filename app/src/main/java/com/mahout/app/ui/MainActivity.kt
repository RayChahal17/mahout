package com.mahout.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.mahout.app.R
import com.mahout.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity is the single Activity host for Mahout (single-activity app).
 *
 * It owns the "chrome" (UI that stays constant):
 * - Toolbar
 * - Bottom navigation
 *
 * And it hosts the NavHostFragment which swaps screens (Fragments).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // ViewBinding instance for activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // Navigation controller drives fragment navigation
    private lateinit var navController: NavController

    // Top-level destinations: these should NOT show an Up arrow
    private val topLevelDestinations = setOf(
        R.id.aimFragment,
        R.id.pathFragment,
        R.id.elephantFragment,
        R.id.northStarFragment,
        R.id.mahoutFragment
    )

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout with ViewBinding (safer than findViewById)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use our MaterialToolbar as the Activity's ActionBar
        setSupportActionBar(binding.topAppBar)

        // Grab NavController from NavHostFragment in activity_main.xml
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configure top-level destinations so up button works correctly
        appBarConfiguration = AppBarConfiguration(topLevelDestinations)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Wire bottom navigation to Navigation Component
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        // Toolbar menu (Settings)
        binding.topAppBar.inflateMenu(R.menu.menu_top_app_bar)
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    if (navController.currentDestination?.id != R.id.settingsFragment) {
                        navController.navigate(R.id.settingsFragment)
                    }
                    true
                }
                else -> false
            }
        }

        // Hide bottom nav on non-tab destinations (Settings, future screens)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = topLevelDestinations.contains(destination.id)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Makes toolbar Up button respect the navigation back stack
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
