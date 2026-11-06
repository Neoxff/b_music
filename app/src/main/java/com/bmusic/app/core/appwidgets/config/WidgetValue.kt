package com.bmusic.app.core.appwidgets.config

import androidx.annotation.StringRes
import com.bmusic.app.core.appwidgets.WidgetData

interface WidgetValue {

    val name: String

    @get:StringRes
    val label: Int

    val requires: WidgetData?
        get() = null
}
