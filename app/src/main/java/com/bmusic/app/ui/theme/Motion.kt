package com.bmusic.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/**
 * B Music — motion tokens (docs/design/02-animations.md). Single source of truth for all
 * durations/springs/curves used by the app. No magic numbers in UI code.
 *
 * Accessibility: gate every animation behind [rememberReducedMotion] — when the system animator
 * scale is 0, [Motion.of(...)] collapses durations to 0 and springs to snap.
 */
object Motion {
    val DurationShort = 120
    val DurationBase = 220
    val DurationLong = 300
    val DurationXLong = 420
    val DurationSkeleton = 1200
    val SpringSoft = spring<Float>(dampingRatio = 0.86f, stiffness = 380f)
    val SpringBounce = spring<Float>(dampingRatio = 0.5f, stiffness = 550f)
    val SpringSnappy = spring<Float>(dampingRatio = 0.55f, stiffness = 700f)
    const val StaggerMs = 24
    const val MaxStaggerItems = 8
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val FastOutSlowIn = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    fun scaleDuration(ms: Int, reduced: Boolean): Int = if (reduced) 0 else ms
    fun tweenStd(ms: Int) = tween<Int>(durationMillis = ms, easing = EmphasizedDecelerate)
    fun springX(damping: Float, stiffness: Float) = spring<Float>(damping, Spring.Medium, stiffness)
}

/** Système : "Supprimer les animations" (ANIMATOR_DURATION_SCALE == 0). */
@Composable
@ReadOnlyComposable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f
    }
}
