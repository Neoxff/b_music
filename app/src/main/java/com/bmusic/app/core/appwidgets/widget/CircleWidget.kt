package com.bmusic.app.core.appwidgets.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.TextAlign
import com.bmusic.app.core.appwidgets.component.RoundWidget
import com.bmusic.app.core.appwidgets.component.TrackText
import com.bmusic.app.core.appwidgets.component.WavyProgress
import com.bmusic.app.core.appwidgets.component.WidgetShape
import com.bmusic.app.core.appwidgets.component.fillBackground
import com.bmusic.app.core.appwidgets.config.WidgetConfig
import com.bmusic.app.core.appwidgets.config.WidgetSetting
import com.bmusic.app.core.appwidgets.state.PlaybackState
import com.bmusic.app.core.appwidgets.WidgetColors
import com.bmusic.app.core.appwidgets.BMusicWidget
import com.bmusic.app.core.appwidgets.openApp

class CircleWidget : BMusicWidget() {

    override val settings = listOf(
        WidgetSetting.DynamicColors,
        WidgetSetting.ShowArtist,
        WidgetSetting.ShowProgress,
        WidgetSetting.ShowControls,
        WidgetSetting.SideButtons
    )

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        RoundWidget(
            playbackState = playbackState,
            colors = colors,
            showControls = config.showControls,
            satellite = config.leading,
            controlsAt = Alignment.TopEnd,
            playAt = Alignment.BottomStart,
            playShape = if (playbackState.isPlaying) WidgetShape.Scallop else WidgetShape.Button
        ) { diameter ->
            val large = diameter >= LARGE_FROM
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .fillBackground(colors.surface, WidgetShape.Stadium)
                    .openApp()
                    .padding(horizontal = TEXT_INSET),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackText(
                    playbackState = playbackState,
                    colors = colors,
                    titleSize = if (large) 16.sp else 13.sp,
                    artistSize = if (large) 13.sp else 11.sp,
                    align = TextAlign.Center,
                    showArtist = config.showArtist
                )
                if (config.showProgress) {
                    WavyProgress(
                        playbackState = playbackState,
                        colors = colors,
                        modifier = GlanceModifier.padding(top = 2.dp),
                        trackWidth = diameter - TEXT_INSET * 2
                    )
                }
            }
        }
    }

    private companion object {
        val TEXT_INSET = 14.dp
        val LARGE_FROM = 210.dp
    }
}
