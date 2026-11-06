package com.bmusic.app.core.appwidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.bmusic.app.core.appwidgets.config.WidgetConfigStore
import com.bmusic.app.core.appwidgets.widget.CardWidget
import com.bmusic.app.core.appwidgets.widget.CircleWidget
import com.bmusic.app.core.appwidgets.widget.CookieWidget
import com.bmusic.app.core.appwidgets.widget.GridWidget
import com.bmusic.app.core.appwidgets.widget.LibraryWidget

/** Refresh ony any change so Widgets dont go stale */
abstract class BMusicWidgetReceiver : GlanceAppWidgetReceiver() {

    final override val glanceAppWidget get() = widgetFor(this)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // The data itself is fetched inside the Glance session
        WidgetUpdater.notifyWidgetsChanged(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetConfigStore.clear(context, appWidgetIds)
    }
}

class CardWidgetReceiver : BMusicWidgetReceiver()

class CircleWidgetReceiver : BMusicWidgetReceiver()

class CookieWidgetReceiver : BMusicWidgetReceiver()

class GridWidgetReceiver : BMusicWidgetReceiver()

class LibraryWidgetReceiver : BMusicWidgetReceiver()

internal val widgetsByReceiver: Map<String, BMusicWidget> = mapOf(
    CardWidgetReceiver::class.java.name to CardWidget(),
    CircleWidgetReceiver::class.java.name to CircleWidget(),
    CookieWidgetReceiver::class.java.name to CookieWidget(),
    GridWidgetReceiver::class.java.name to GridWidget(),
    LibraryWidgetReceiver::class.java.name to LibraryWidget()
)

internal fun widgetFor(receiver: BMusicWidgetReceiver): GlanceAppWidget =
    widgetsByReceiver.getValue(receiver.javaClass.name)
