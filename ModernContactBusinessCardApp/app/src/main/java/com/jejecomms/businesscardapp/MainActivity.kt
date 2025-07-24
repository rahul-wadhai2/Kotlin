package com.jejecomms.businesscardapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.jejecomms.businesscardapp.databinding.ActivityMainBinding

/**
 * MainActivity is the main entry point of the application.
 */
class MainActivity : AppCompatActivity() {

    /**
     * ViewBinding property for this activity.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * Navigation Controller instance for managing app navigation.
     */
    private lateinit var navController: NavController

    /**
     * Store ID instead of View.
     */
    private var previouslySelectedMenuItemViewId: Int? = null

    /**
     * Scale factor for selected items.
     */
    private val selectedItemScale = 1.2f

    /**
     * Default scale factor for BottomNavigationView items when they are not selected.
     * This value represents 100% of the item's original size.
     */
    private val defaultItemScale = 1.0f

    /**
     * Duration for the scale animation of BottomNavigationView items, in milliseconds.
     * This value determines how long the transition takes when an item
     * scales up (on selection) or scales down (on deselection).
     */
    private val animationDuration = 150L

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarActivityMain)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setBackgroundDrawable(ResourcesCompat
            .getDrawable(resources, R.drawable.gradient_primary, null))

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_contacts, R.id.navigation_cards, R.id.navigation_scan,
                R.id.navigation_profile
            )
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.navView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom) // Apply bottom inset as padding
            windowInsets
        }

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController) // This sets the primary listener

        //Listen for Destination Changes to Apply Animations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuView = navView.getChildAt(0) as? BottomNavigationMenuView ?: return@addOnDestinationChangedListener

            // Scale down previously selected item
            previouslySelectedMenuItemViewId?.let { prevId ->
                if (prevId != destination.id) { // Only if it's a different item
                    val previousItemView = menuView.findViewById<View>(prevId)
                    if (previousItemView is BottomNavigationItemView) {
                        scaleItemView(previousItemView, defaultItemScale, animationDuration)
                    }
                }
            }

            // Scale up newly selected item
            val currentItemView = menuView.findViewById<View>(destination.id)
            if (currentItemView is BottomNavigationItemView) {
                scaleItemView(currentItemView, selectedItemScale, animationDuration)
            }
            previouslySelectedMenuItemViewId = destination.id
        }

        // Initial scale for the start destination (important!)
        navView.post { // Ensure views are laid out
            val menuView = navView.getChildAt(0) as? BottomNavigationMenuView
            navController.currentDestination?.id?.let { currentDestId ->
                previouslySelectedMenuItemViewId = currentDestId // Set initial selection
                val initialItemView = menuView?.findViewById<View>(currentDestId)
                if (initialItemView is BottomNavigationItemView) {
                    scaleItemView(initialItemView, selectedItemScale, 0L) // 0 duration for initial set
                }
            }
        }

        // Handle reselection if you want a specific animation
        // when the currently selected item is tapped again.
        navView.setOnItemReselectedListener { item ->
            val menuView = navView.getChildAt(0) as? BottomNavigationMenuView ?: return@setOnItemReselectedListener
            val reselectedItemView = menuView.findViewById<View>(item.itemId)
            if (reselectedItemView is BottomNavigationItemView) {
                val pulseAnimatorX = ObjectAnimator.ofFloat(reselectedItemView, View.SCALE_X, selectedItemScale, 1.1f, selectedItemScale)
                val pulseAnimatorY = ObjectAnimator.ofFloat(reselectedItemView, View.SCALE_Y, selectedItemScale, 1.1f, selectedItemScale)
                pulseAnimatorX.duration = animationDuration
                pulseAnimatorY.duration = animationDuration
                val set = AnimatorSet()
                set.playTogether(pulseAnimatorX, pulseAnimatorY)
                set.start()
            }
        }
    }

    // Modified scaleItemView function
    private fun scaleItemView(@SuppressLint("RestrictedApi") itemView: BottomNavigationItemView, scale: Float, duration: Long) {
        val animators = mutableListOf<ObjectAnimator>()

        animators.add(ObjectAnimator.ofFloat(itemView, View.SCALE_X, scale).setDuration(duration))
        animators.add(ObjectAnimator.ofFloat(itemView, View.SCALE_Y, scale).setDuration(duration))

        if (animators.isNotEmpty()) {
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animators.toList())
            animatorSet.start()
        }
    }
}