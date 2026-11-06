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

package com.bmusic.app.ui.screen.settings

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import coil3.SingletonImageLoader
import com.google.android.material.color.DynamicColors
import com.bmusic.app.BuildConfig
import com.bmusic.app.R
import com.bmusic.app.core.model.lyrics.LyricsViewSettings
import com.bmusic.app.data.local.room.InclExclDao
import com.bmusic.app.data.model.network.ScrobblingService
import com.bmusic.app.extensions.hasR
import com.bmusic.app.extensions.hasS
import com.bmusic.app.extensions.isTablet
import com.bmusic.app.extensions.materialSharedAxis
import com.bmusic.app.extensions.navigation.findActivityNavController
import com.bmusic.app.extensions.requestContext
import com.bmusic.app.extensions.showToast
import com.bmusic.app.extensions.utilities.dateStr
import com.bmusic.app.extensions.utilities.toEnum
import com.bmusic.app.ui.component.compose.languageEntries
import com.bmusic.app.ui.component.preferences.ProgressIndicatorPreference
import com.bmusic.app.ui.component.preferences.SwitchWithButtonPreference
import com.bmusic.app.ui.component.preferences.ThemePreference
import com.bmusic.app.ui.component.preferences.dialog.ActionOnCoverPreferenceDialog
import com.bmusic.app.ui.component.preferences.dialog.CategoriesPreferenceDialog
import com.bmusic.app.ui.component.preferences.dialog.ClearQueueActionPreferenceDialog
import com.bmusic.app.ui.component.preferences.dialog.ExtraInfoPreferenceDialog
import com.bmusic.app.ui.component.preferences.dialog.LanguageSelectionDialog
import com.bmusic.app.ui.component.preferences.dialog.NowPlayingScreenPreferenceDialog
import com.bmusic.app.ui.component.preferences.dialog.SingleSelectionDialog
import com.bmusic.app.ui.component.preferences.dialog.SongClickActionPreferenceDialog
import com.bmusic.app.ui.dialogs.library.BlacklistWhitelistDialog
import com.bmusic.app.ui.screen.backup.BackupActivity
import com.bmusic.app.ui.screen.library.LibraryViewModel
import com.bmusic.app.ui.screen.lyrics.LyricsViewModel
import com.bmusic.app.ui.screen.scrobbling.ScrobblingServiceLoginFragment
import com.bmusic.app.ui.screen.update.UpdateSearchResult
import com.bmusic.app.ui.screen.update.UpdateViewModel
import com.bmusic.app.util.ADD_EXTRA_CONTROLS
import com.bmusic.app.util.AUTO_LANGUAGE
import com.bmusic.app.util.BLACKLIST_ENABLED
import com.bmusic.app.util.BLACK_THEME
import com.bmusic.app.util.COVER_DOUBLE_TAP_ACTION
import com.bmusic.app.util.COVER_LEFT_DOUBLE_TAP_ACTION
import com.bmusic.app.util.COVER_LONG_PRESS_ACTION
import com.bmusic.app.util.COVER_RIGHT_DOUBLE_TAP_ACTION
import com.bmusic.app.util.COVER_SINGLE_TAP_ACTION
import com.bmusic.app.util.ENABLE_ROTATION_LOCK
import com.bmusic.app.util.GENERAL_THEME
import com.bmusic.app.util.IGNORE_MEDIA_STORE
import com.bmusic.app.util.LANGUAGE_NAME
import com.bmusic.app.util.LASTFM_LOGIN
import com.bmusic.app.util.LIBRARY_CATEGORIES
import com.bmusic.app.util.LISTENBRAINZ_LOGIN
import com.bmusic.app.util.MATERIAL_YOU
import com.bmusic.app.util.NOW_PLAYING_EXTRA_INFO
import com.bmusic.app.util.NOW_PLAYING_SCREEN
import com.bmusic.app.util.ON_CLEAR_QUEUE_ACTION
import com.bmusic.app.util.ON_SONG_CLICK_ACTION
import com.bmusic.app.util.PREFERRED_IMAGE_SIZE
import com.bmusic.app.util.Preferences
import com.bmusic.app.util.TRASH_MUSIC_FILES
import com.bmusic.app.util.USE_CUSTOM_FONT
import com.bmusic.app.util.USE_FOLDER_ART
import com.bmusic.app.util.WHITELIST_ENABLED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AppearancePreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_appearance)
    }
}

class NowPlayingPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_now_playing)
    }
}

class LyricsPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_lyrics)
    }
}

class PlaybackPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_playback)
    }
}

class LibraryPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_library)
    }
}

class NetworkPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_network)
    }
}

class AdvancedPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_advanced)
        findPreference<ListPreference>(LANGUAGE_NAME)?.let { preference ->
            preference.entries = requireContext().languageEntries()
                .map { it.title }
                .toTypedArray()
        }
    }
}

open class PreferenceScreenFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val updateViewModel: UpdateViewModel by activityViewModel()

    private val preferences: SharedPreferences by inject()

    private val importFontLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                lyricsViewModel.importCustomFont(requireContext(), uri)
                    .observe(viewLifecycleOwner) { success ->
                        if (success) {
                            showToast(R.string.font_imported_successfully)
                        } else {
                            showToast(R.string.could_not_import_font)
                        }
                    }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences.registerOnSharedPreferenceChangeListener(this)
        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            listView.updatePadding(bottom = it.getWithSpace())
        }
        setDivider(Color.TRANSPARENT.toDrawable())
        materialSharedAxis(view)
        preparePreferences()
    }

    fun preparePreferences() {
        findPreference<Preference>("about")?.summary =
            getString(R.string.about_summary, BuildConfig.VERSION_NAME)

        findPreference<ThemePreference>(GENERAL_THEME)?.apply {
            customCallback = object : ThemePreference.Callback {
                override fun onThemeSelected(themeName: String) {
                    Preferences.generalTheme = themeName
                    setDefaultNightMode(Preferences.getDayNightMode(themeName))
                    restartActivity()
                }
            }
        }

        findPreference<Preference>(BLACK_THEME)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val themeName = Preferences.getGeneralTheme((newValue as Boolean))
                setDefaultNightMode(Preferences.getDayNightMode(themeName))
                requireActivity().recreate()
                true
            }
        }

        findPreference<Preference>(MATERIAL_YOU)?.apply {
            isVisible = hasS()
            setOnPreferenceChangeListener { _, newValue ->
                val activity = requireActivity()
                if (newValue as Boolean) {
                    DynamicColors.applyToActivityIfAvailable(activity)
                }
                activity.recreate()
                true
            }
        }

        findPreference<Preference>(USE_CUSTOM_FONT)?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }

        findPreference<Preference>(ADD_EXTRA_CONTROLS)?.isVisible = !resources.isTablet

        findPreference<ListPreference>(LyricsViewSettings.Key.BACKGROUND_EFFECT)?.apply {
            if (!hasS()) {
                val indexOfBlur = entryValues.indexOf("blur")
                entries = entries.filterIndexed { index, _ -> index != indexOfBlur }
                    .toTypedArray()
                entryValues = entryValues.filterIndexed { index, _ -> index != indexOfBlur }
                    .toTypedArray()
            }
        }

        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isVisible = hasS()

        findPreference<Preference>(LyricsViewSettings.Key.SELECTED_CUSTOM_FONT)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            importFontLauncher.launch(
                arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf")
            )
            true
        }

        findPreference<Preference>("clear_lyrics")
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lyricsViewModel.deleteLyrics()
            showToast(R.string.lyrics_cleared)
            true
        }

        if (!hasR()) {
            findPreference<Preference>(TRASH_MUSIC_FILES)?.isVisible = false
        }

        findPreference<SwitchWithButtonPreference>(WHITELIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.WHITELIST)
                }
            })
        }

        findPreference<SwitchWithButtonPreference>(BLACKLIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.BLACKLIST)
                }
            })
        }

        findPreference<Preference>(IGNORE_MEDIA_STORE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(PREFERRED_IMAGE_SIZE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(USE_FOLDER_ART)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(LANGUAGE_NAME)?.setOnPreferenceChangeListener { _, newValue ->
            val languageTag = (newValue as? String)?.takeIf { it != AUTO_LANGUAGE }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            true
        }

        findPreference<Preference>("backup_and_restore")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), BackupActivity::class.java))
            true
        }

        findPreference<Preference>(ENABLE_ROTATION_LOCK)?.isVisible = !resources.isTablet

        val updateSearchPreference = findPreference<ProgressIndicatorPreference>("search_for_update")
        if (updateSearchPreference != null) {
            updateSearchPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                updateViewModel.searchForUpdate(true)
                true
            }

            updateViewModel.updateEventObservable.observe(viewLifecycleOwner) {
                val result = it.peekContent()
                when (result.state) {
                    UpdateSearchResult.State.Searching -> {
                        updateSearchPreference.showProgressIndicator()
                        updateSearchPreference.isEnabled = false
                        updateSearchPreference.summary = getString(R.string.checking_please_wait)
                    }

                    UpdateSearchResult.State.Completed,
                    UpdateSearchResult.State.Failed -> {
                        updateSearchState(updateSearchPreference, result.executedAtMillis)
                    }

                    else -> {
                        updateSearchState(updateSearchPreference, Preferences.lastUpdateSearch)
                    }
                }
            }
        }

        onUpdateNowPlayingScreen()
        onUpdateCoverActions()
        onUpdateLyricsPreferences()
        onUpdateQueuePreferences()
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment = if (preference.key == LANGUAGE_NAME) {
                LanguageSelectionDialog.newInstance(preference.key)
            } else SingleSelectionDialog.newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            val dialogFragment: DialogFragment? = when (preference.key) {
                LIBRARY_CATEGORIES -> CategoriesPreferenceDialog()
                NOW_PLAYING_SCREEN -> NowPlayingScreenPreferenceDialog()
                NOW_PLAYING_EXTRA_INFO -> ExtraInfoPreferenceDialog.nowPlaying()
                ON_SONG_CLICK_ACTION -> SongClickActionPreferenceDialog()
                ON_CLEAR_QUEUE_ACTION -> ClearQueueActionPreferenceDialog()
                COVER_DOUBLE_TAP_ACTION,
                COVER_SINGLE_TAP_ACTION,
                COVER_LONG_PRESS_ACTION,
                COVER_LEFT_DOUBLE_TAP_ACTION,
                COVER_RIGHT_DOUBLE_TAP_ACTION -> ActionOnCoverPreferenceDialog.newInstance(preference.key)
                LASTFM_LOGIN -> ScrobblingServiceLoginFragment.create(ScrobblingService.Lastfm)
                LISTENBRAINZ_LOGIN -> ScrobblingServiceLoginFragment.create(ScrobblingService.ListenBrainz)
                else -> null
            }
            if (dialogFragment != null) {
                dialogFragment.show(childFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val settingsScreen = preference.key.toEnum<SettingsScreen>()
        return when {
            settingsScreen != null -> {
                findNavController().navigate(settingsScreen.navAction)
                true
            }
            preference.key == "about" -> {
                findActivityNavController(R.id.fragment_container).navigate(R.id.nav_about)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            NOW_PLAYING_SCREEN -> onUpdateNowPlayingScreen()
            COVER_DOUBLE_TAP_ACTION,
            COVER_LEFT_DOUBLE_TAP_ACTION,
            COVER_RIGHT_DOUBLE_TAP_ACTION,
            COVER_LONG_PRESS_ACTION -> onUpdateCoverActions()
            ON_SONG_CLICK_ACTION,
            ON_CLEAR_QUEUE_ACTION -> onUpdateQueuePreferences()
            LyricsViewSettings.Key.BACKGROUND_EFFECT -> onUpdateLyricsPreferences()
        }
    }

    private fun onUpdateNowPlayingScreen() {
        findPreference<Preference>(NOW_PLAYING_SCREEN)?.summary =
            getString(Preferences.nowPlayingScreen.titleRes)
    }

    private fun onUpdateCoverActions() {
        findPreference<Preference>(COVER_SINGLE_TAP_ACTION)?.summary =
            getString(Preferences.coverSingleTapAction.titleRes)

        findPreference<Preference>(COVER_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LEFT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverLeftDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_RIGHT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverRightDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LONG_PRESS_ACTION)?.summary =
            getString(Preferences.coverLongPressAction.titleRes)
    }

    private fun onUpdateLyricsPreferences() {
        val hasBackgroundEffects =
            preferences.getString(LyricsViewSettings.Key.BACKGROUND_EFFECT, "none") != "none"
        findPreference<Preference>(LyricsViewSettings.Key.SHADOW_EFFECT)
            ?.isEnabled = hasBackgroundEffects
        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isEnabled = hasBackgroundEffects
    }

    private fun onUpdateQueuePreferences() {
        findPreference<Preference>(ON_SONG_CLICK_ACTION)
            ?.summary = getString(Preferences.songClickAction.titleRes)
        findPreference<Preference>(ON_CLEAR_QUEUE_ACTION)
            ?.summary = getString(Preferences.clearQueueAction.titleRes)
    }

    private fun showLibraryFolderSelector(type: Int) {
        BlacklistWhitelistDialog.newInstance(type).show(childFragmentManager, "LIBRARY_PATHS_PREFERENCE")
    }

    private fun clearImageLoaderCache() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val imageLoader = SingletonImageLoader.get(requireContext())
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            Log.e("Settings", "Failed to clear image loader cache", e)
        }
    }

    private fun updateSearchState(preference: ProgressIndicatorPreference?, lastUpdateSearch: Long) {
        requestContext {
            preference?.hideProgressIndicator()
            preference?.isEnabled = true
            preference?.summary = getString(R.string.last_update_search_x, it.dateStr(lastUpdateSearch))
        }
    }

    private fun restartActivity() {
        activity?.recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}