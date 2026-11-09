/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bmusic.app.ui.screen.other

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil3.request.Disposable
import com.bmusic.app.R
import com.bmusic.app.coil.songImage
import com.bmusic.app.core.model.player.ProgressState
import com.bmusic.app.core.model.theme.NowPlayingButtonStyle
import com.bmusic.app.databinding.FragmentMiniPlayerBinding
import com.bmusic.app.extensions.isTablet
import com.bmusic.app.extensions.launchAndRepeatWithViewLifecycle
import com.bmusic.app.extensions.media.displayArtistName
import com.bmusic.app.extensions.resources.*
import com.bmusic.app.ui.component.base.SkipButtonTouchHandler
import com.bmusic.app.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.bmusic.app.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.bmusic.app.ui.screen.player.PlayerViewModel
import com.bmusic.app.ui.theme.GlassPolicy
import com.bmusic.app.ui.theme.GlassTokens
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs

class MiniPlayerFragment : Fragment(R.layout.fragment_mini_player),
    View.OnClickListener, SkipButtonTouchHandler.Callback {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var primaryColorSpan: ForegroundColorSpan
    private lateinit var secondaryColorSpan: ForegroundColorSpan

    private val buttonStyle: NowPlayingButtonStyle
        get() = if (Preferences.adaptiveControls) {
            Preferences.nowPlayingScreen.buttonStyle
        } else {
            NowPlayingButtonStyle.Normal
        }

    private var disposable: Disposable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)
        applyMiniPlayerGlass(view)
        binding.progressBar.installWavyAnimatorCleanup()
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect { currentSong ->
                disposable = binding.image.songImage(currentSong)
                binding.songTitle.isSelected = true
                binding.songTitle.text = currentSong.title
                binding.songArtist.isSelected = true
                binding.songArtist.text = currentSong.displayArtistName()
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(
                playerViewModel.progressFlow,
                playerViewModel.durationFlow
            ) { progress, duration -> ProgressState(progress, duration) }
                .filter { progress -> progress.mayUpdateUI }
                .collectLatest {
                    binding.progressBar.max = it.total.toInt()
                    binding.progressBar.setProgressCompat(it.progress.toInt(), true)
                }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.isPlayingFlow.collect { isPlaying ->
                updatePlayPause(isPlaying, buttonStyle)
            }
        }
        primaryColorSpan = textColorPrimary().toForegroundColorSpan()
        secondaryColorSpan = textColorSecondary().toForegroundColorSpan()
        setupImageStyle()
        setUpButtons()
        setUpProgressStyle()
        view.setOnTouchListener { _, event ->
            Preferences.miniPlayerSwipeToSkip && flingPlayBackController.onTouchEvent(event)
        }
    }

    fun setupImageStyle() {
        val cornerRadius = Preferences.getNowPlayingImageCornerRadius(requireContext())
        binding.image.setCornerRadius((cornerRadius / 2).toFloat())
    }

    private fun setUpButtons() {
        setupButtonStyle()
        setupExtraControls()
        binding.actionNext.setOnTouchListener(SkipButtonTouchHandler(DIRECTION_NEXT, this))
        binding.actionPrevious.setOnTouchListener(SkipButtonTouchHandler(DIRECTION_PREVIOUS, this))
        binding.actionPlayPause.setOnClickListener(this)
    }

    fun setUpProgressStyle() {
        val isWavy = playerViewModel.isPlaying && Preferences.squigglySeekBar
        binding.progressBar.setAnimatedWave(isWavy)
        binding.progressBar.setWavy(isWavy)
    }

    fun setupButtonStyle() {
        val buttonStyle = this.buttonStyle
        binding.actionNext.setIconResource(buttonStyle.skipNext)
        binding.actionPrevious.setIconResource(buttonStyle.skipPrevious)
        updatePlayPause(playerViewModel.isPlaying, buttonStyle)
    }

    fun setupExtraControls() {
        if (resources.isTablet) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.isVisible = Preferences.extraControls
            binding.actionPrevious.isVisible = Preferences.extraControls
        }
    }

    override fun onSkipButtonHold(direction: Int) {
        when (direction) {
            DIRECTION_NEXT -> playerViewModel.seekForward()
            DIRECTION_PREVIOUS -> playerViewModel.seekBack()
        }
    }

    override fun onSkipButtonTap(direction: Int) {
        when (direction) {
            DIRECTION_NEXT -> playerViewModel.seekToNext()
            DIRECTION_PREVIOUS -> playerViewModel.seekToPrevious()
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.actionPlayPause -> playerViewModel.togglePlayPause()
        }
    }

    override fun onDestroyView() {
        disposable?.dispose()
        super.onDestroyView()
        _binding = null
    }

    private fun updatePlayPause(isPlaying: Boolean, buttonStyle: NowPlayingButtonStyle) {
        if (isPlaying) {
            binding.actionPlayPause.setIconResource(buttonStyle.pause)
        } else {
            binding.actionPlayPause.setIconResource(buttonStyle.play)
        }
        setUpProgressStyle()
    }

    /**
     * Surface (1) mini-player — Étape 3, View XML (sans réécrire la navigation).
     * Solution validée par le plan : pas de flou temps-réel en View (le flou
     * temps-réel vit uniquement dans GlassSurface Compose). On applique les
     * tokens GlassTokens exactement : nég fond = couleur `surface` du thème
     * avec opacité Minimal (0,78 sombre / 0,82 clair) si GlassPolicy active,
     * sinon `surface` opaque (Désactivé = rendu identique, mise en page
     * inchangée). Rayon 16 dp, contour 1 dp côté bas, 2 surfaces simultanées
     * max par construction. TalkBack inchangé (sémantique View inchangée).
     */
    private fun applyMiniPlayerGlass(view: View) {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val signals = GlassPolicy.Signals(
            mode = GlassPolicy.fromPreference(Preferences.blurEffects),
            apiLevel = Build.VERSION.SDK_INT,
            isLowRamDevice = (requireContext().getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false),
            isPowerSaveMode = (requireContext().getSystemService(PowerManager::class.java)?.isPowerSaveMode ?: false),
            reducedMotion = Settings.Global.getFloat(requireContext().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f,
        )
        val active = GlassPolicy.isActive(signals)
        val alpha = glassBackgroundAlpha(active = active, dark = dark)
        val density = resources.displayMetrics.density
        val base = surfaceColor()
        val argb = android.graphics.Color.argb(
            (alpha * 255).toInt(),
            android.graphics.Color.red(base),
            android.graphics.Color.green(base),
            android.graphics.Color.blue(base),
        )
        val edgeCompose = if (dark) GlassTokens.EdgeDark else GlassTokens.EdgeLight
        val edge = android.graphics.Color.argb(
            (edgeCompose.alpha * 255).toInt(),
            (edgeCompose.red * 255).toInt(),
            (edgeCompose.green * 255).toInt(),
            (edgeCompose.blue * 255).toInt(),
        )
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = GlassViewTokens.radiusDp(GlassTokens.Level.Minimal) * density
            setColor(argb)
            setStroke((1 * density).toInt(), edge)
            gradientType = GradientDrawable.RECTANGLE
        }
        view.background = drawable
    }

    private var flingPlayBackController = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (abs(velocityX) > abs(velocityY)) {
                    if (velocityX < 0) {
                        playerViewModel.seekToNext()
                        return true
                    } else if (velocityX > 0) {
                        playerViewModel.seekToPrevious()
                        return true
                    }
                }
                return false
            }
        })
}

/**
 * Étape 3, surface (1) : alpha du fond de surface View.
 * Active (AUTO nominal) = tokens Minimal (0,78 sombre / 0,82 clair) ;
 * sinon (opaque / Désactivé) = 1,0 → rendu v1.1.x, mise en page inchangée.
 */
internal fun glassBackgroundAlpha(active: Boolean, dark: Boolean): Float =
    if (active) GlassTokens.opacity(GlassTokens.Level.Minimal, dark) else 1f

/** Tokens View (miroir des tokens Compose, pour GradientDrawable). */
internal object GlassViewTokens {
    fun radiusDp(level: GlassTokens.Level): Float = when (level) {
        GlassTokens.Level.Minimal -> GlassTokens.RadiusMinimal.toFloat()
        GlassTokens.Level.Standard -> GlassTokens.RadiusStandard.toFloat()
        GlassTokens.Level.Prominent -> GlassTokens.RadiusProminent.toFloat()
    }
}
