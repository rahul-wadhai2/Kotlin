package com.jejecomms.businesscardapp.ui.splashscreen

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.jejecomms.businesscardapp.MainActivity
import com.jejecomms.businesscardapp.R
import com.jejecomms.businesscardapp.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transitionToMainScreen()
    }

    /**
     * Transition to the main screen after the animation
     */
    fun transitionToMainScreen() {
        autoPlaySingleBounce(binding.businessCardIcon)
        // Animate the app name
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 2000
        fadeIn.fillAfter = true

        val typeface: Typeface? = ResourcesCompat.getFont(this, R.font.cursive)
        binding.appName.typeface = typeface

        binding.appName.startAnimation(fadeIn)

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)

                // Use ActivityOptions for starting the activity
                val options = ActivityOptions.makeCustomAnimation(
                    this@SplashActivity,
                    android.R.anim.fade_in, // Enter animation for MainActivity
                    android.R.anim.fade_out  // Exit animation for SplashActivity
                )
                startActivity(intent, options.toBundle())
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    /**
     * Applies a vertical bounce animation automatically to the given view.
     * This is typically used for elements like a business card icon.
     *
     * @param view The View to animate.
     */
    fun autoPlaySingleBounce(view: View) {
        val originalY = 0f // Assuming it starts at its layout position
        val bounceHeight = -60f

        // 1. Jump Up
        val jumpUp = ObjectAnimator.ofFloat(view, "translationY", originalY, bounceHeight).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        // 2. Fall Down with Bounce
        val fallDown = ObjectAnimator.ofFloat(view, "translationY", bounceHeight, originalY).apply {
            duration = 800
            interpolator = BounceInterpolator()
        }

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(jumpUp, fallDown)
        animatorSet.start()
    }
}
