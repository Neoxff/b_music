package com.bmusic.app.core.appwidgets.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import com.bmusic.app.core.appwidgets.component.RoundWidget
import com.bmusic.app.core.appwidgets.component.WidgetArtwork
import com.bmusic.app.core.appwidgets.component.WidgetShape
import com.bmusic.app.core.appwidgets.config.WidgetConfig
import com.bmusic.app.core.appwidgets.config.WidgetSetting
import com.bmusic.app.core.appwidgets.state.PlaybackState
import com.bmusic.app.core.appwidgets.WidgetColors
import com.bmusic.app.core.appwidgets.BMusicWidget
import com.bmusic.app.core.appwidgets.openApp

class CookieWidget : BMusicWidget() {

    override val settings = listOf(
        WidgetSetting.DynamicColors,
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
            controlsAt = Alignment.TopStart,
            playAt = Alignment.BottomEnd,
            playShape = WidgetShape.Button
        ) {
            WidgetArtwork(
                songId = playbackState.songId,
                colors = colors,
                modifier = GlanceModifier.fillMaxSize().openApp(),
                shape = WidgetShape.Scallop
            )
        }
    }
}
