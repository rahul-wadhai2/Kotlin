package com.jejecomms.businesscardapp.ui.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.jejecomms.businesscardapp.R
import com.jejecomms.businesscardapp.databinding.FragmentProfileBinding
import com.jejecomms.businesscardapp.ui.base.BaseFragment
import com.jejecomms.businesscardapp.utils.ClipboardUtils
import com.jejecomms.businesscardapp.utils.ToastUtils

/**
 * Profile Fragment.
 */
class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>(),
    View.OnClickListener {

    // Initial position and size of the avatar when expanded
    private var initialAvatarX: Float = 0f
    private var initialAvatarY: Float = 0f
    private var initialAvatarSize: Int = 0

    // Target position and size of the avatar when collapsed (on the left)
    private var collapsedAvatarX: Float = 0f
    private var collapsedAvatarY: Float = 0f
    private var collapsedAvatarSize: Int = 0

    // Use a nullable Int to indicate if it's been set
    private var statusBarHeight: Int? = null

    // Flag to ensure initial calculations run only once after all data is ready
    private var hasCalculatedInitialPositions = false

    override fun createViewModel(): ProfileViewModel? {
        return ViewModelProvider(this).get(ProfileViewModel::class.java)
    }

    override fun createViewBinding(
        layoutInflater: LayoutInflater?,
        container: ViewGroup?
    ): FragmentProfileBinding? {
        return layoutInflater?.let { FragmentProfileBinding.inflate(it, container, false) }
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
        collapsingToolbarParallaxEffect()
    }

    /**
     *  Collapsing toolbar with parallax effect.
     */
    fun collapsingToolbarParallaxEffect() {
        // Listen for window insets to get status bar height
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val newStatusBarHeight = systemBarsInsets.top
            if (statusBarHeight == null || statusBarHeight != newStatusBarHeight) {
                statusBarHeight = newStatusBarHeight
                if (!hasCalculatedInitialPositions) {
                    binding.appBarLayout.requestLayout()
                }
            }
            insets
        }

        // Post to ensure AppBarLayout itself has been laid out initially
        binding.appBarLayout.post {
            binding.collapsingToolbarLayout.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Ensure views have been measured and laid out AND status bar height is known AND not already calculated
                    if (binding.avatarView.width > 0 && binding.collapsingToolbarLayout.height > 0 &&
                        binding.toolbar.height > 0 && statusBarHeight != null && !hasCalculatedInitialPositions
                    ) {
                        // Remove the listener immediately after valid dimensions and status bar height are obtained
                        binding.collapsingToolbarLayout.viewTreeObserver.removeOnGlobalLayoutListener(
                            this
                        )
                        hasCalculatedInitialPositions = true // Set flag to prevent re-calculation

                        // Initial (expanded) state
                        initialAvatarSize = binding.avatarView.width
                        initialAvatarX =
                            (binding.collapsingToolbarLayout.width / 2f) - (initialAvatarSize / 2f)
                        // Initial Y: Positioned relative to the bottom of the CollapsingToolbarLayout
                        initialAvatarY =
                            (binding.collapsingToolbarLayout.height - initialAvatarSize -
                                    50.convertDpToPx())

                        // Collapsed state (target position in toolbar's content area)
                        collapsedAvatarSize = 40.convertDpToPx().toInt() // e.g., 40dp
                        collapsedAvatarX =
                            -40.convertDpToPx() // A small margin from the left edge of the toolbar

                        // Collapsed Y position: Calculate relative to the AppBarLayout's top
                        collapsedAvatarY =
                            (statusBarHeight!! + (binding.toolbar.height / 2f) - (collapsedAvatarSize / 2f)) + 215

                        // Set initial position immediately for the start state
                        binding.avatarView.x = initialAvatarX
                        binding.avatarView.y = initialAvatarY
                    }
                }
            })
        }

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->

            val currentStatusBarHeight = statusBarHeight ?: 0
            // Ensure initial calculations have been performed using a flag
            if (!hasCalculatedInitialPositions) {
                return@OnOffsetChangedListener
            }

            val range = appBar.totalScrollRange
            val percentage = Math.abs(verticalOffset).toFloat() / range

            // Interpolate X position using translationX
            val dx = collapsedAvatarX - initialAvatarX
            binding.avatarView.translationX = dx * percentage + 400

            // Interpolate Y position using translationY
            val targetYInCollapsingLayout = collapsedAvatarY - currentStatusBarHeight
            val dy = targetYInCollapsingLayout - initialAvatarY
            binding.avatarView.translationY = dy * percentage + 200

            // Interpolate Scale
            val scale = 1f + ((collapsedAvatarSize.toFloat() / initialAvatarSize
                .toFloat()) - 1f) * percentage
            binding.avatarView.scaleX = scale
            binding.avatarView.scaleY = scale

            // Ensure the AvatarView is visible
            binding.avatarView.visibility = View.VISIBLE
        })
    }

    /**
     * Convert dp to px.
     */
    @Px
    private fun Int.convertDpToPx(): Float {
        return this * this@ProfileFragment.resources.displayMetrics.density
    }

    /**
     *  OnClickListener for relevant views.
     */
    fun setOnClickListener() {
        binding.copyPhoneButton.setOnClickListener(this)
        binding.copyEmailButton.setOnClickListener(this)
        binding.fabEdit.setOnClickListener(this)
        binding.fabShare.setOnClickListener(this)
    }

    /**
     *  Enabled edit text.
     */
    fun enabledEditText() {
        binding.phoneText.isEnabled = true
        binding.emailText.isEnabled = true
        binding.companyText.isEnabled = true
        binding.notesText.isEnabled = true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.copy_phone_button -> {
                context?.let { ClipboardUtils
                    .copyTextToClipboard(it,binding.phoneText, binding.phoneText.text.toString()) }
            }

            R.id.copy_email_button -> {
                context?.let { ClipboardUtils
                    .copyTextToClipboard(it,binding.emailText, binding.emailText.text.toString()) }
            }

            R.id.fab_edit -> {
                enabledEditText()
                ToastUtils.showLongMessage(getString(R.string.you_can_now_make_changes_to_the_text)
                ,context)
            }

            R.id.fab_share -> {

            }
        }
    }
}


