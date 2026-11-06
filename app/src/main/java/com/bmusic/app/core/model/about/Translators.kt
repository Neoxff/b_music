package com.bmusic.app.core.model.about

import android.content.Context
import android.util.Log
import com.bmusic.app.extensions.readStringFromAsset
import kotlinx.serialization.json.Json

fun loadTranslators(context: Context): Map<String, String> = try {
    Json.decodeFromString(context.readStringFromAsset("translators.json").orEmpty())
} catch (e: Exception) {
    Log.e("Translators", "Failed to parse translators JSON", e)
    emptyMap()
}
