package com.example.githubrepository.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import com.example.githubrepository.R
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    /**
     * `alpha` controls the fade-in effect of the splash icon, starting fully transparent.
     */
    val alpha = remember { Animatable(0f) }

    /**
     * `scale` controls the zoom-in effect, starting at 50% of its original size.
     */
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(key1 = true) {

        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }

        delay(2000)

        onTimeout()
    }

    SplashContent(alpha.value, scale.value)
}

@Composable
fun SplashContent(alpha: Float, scale: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24292E)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "GitHub Logo",
            tint = Color.White,
            modifier = Modifier
                .size(120.dp)
                .alpha(alpha)
                .scale(scale)
        )
    }
}