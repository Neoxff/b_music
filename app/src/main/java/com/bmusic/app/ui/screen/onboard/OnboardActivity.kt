/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.bmusic.app.ui.screen.onboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.preference.PreferenceManager
import com.bmusic.app.App
import com.bmusic.app.MainActivity
import com.bmusic.app.extensions.EXTRA_IS_PERMISSION_REQUEST
import com.bmusic.app.extensions.hasS
import com.bmusic.app.extensions.observeKeyAsState
import com.bmusic.app.ui.component.base.AbsBaseActivity
import com.bmusic.app.ui.theme.BMusicTheme
import com.bmusic.app.util.GENERAL_THEME
import com.bmusic.app.util.GeneralTheme
import com.bmusic.app.util.MATERIAL_YOU
import com.bmusic.app.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class OnboardActivity : AbsBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isPermissionRequest = intent.getBooleanExtra(EXTRA_IS_PERMISSION_REQUEST, false)

        setContent {
            val prefs = remember { PreferenceManager.getDefaultSharedPreferences(this) }

            val generalTheme by prefs.observeKeyAsState(GENERAL_THEME, GeneralTheme.AUTO)
            val materialYou by prefs.observeKeyAsState(MATERIAL_YOU, hasS())

            BMusicTheme(
                darkTheme = when (generalTheme) {
                    GeneralTheme.LIGHT -> false
                    GeneralTheme.DARK -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = materialYou
            ) {
                OnboardScreen(
                    onFinish = {
                        Preferences.onboardShown = true
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    },
                    onBackToExit = {
                        finishAffinity()
                    },
                    // A restored backup replaces the preference file on disk, so the stale
                    // in-memory values must never be flushed over it.
                    onRestartRequired = { App.restart(this) },
                    availableSteps = if (isPermissionRequest) {
                        listOf(OnboardStep.PERMISSIONS)
                    } else {
                        OnboardStep.entries
                    }
                )
            }
        }
    }
}
