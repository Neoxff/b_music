package com.bmusic.app.test

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.bmusic.app.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Application de test Robolectric.
 *
 * Pourquoi une Application dédiée plutôt que [com.bmusic.app.App] ?
 *
 * 1. `App` est une classe Kotlin **finale** : elle ne peut pas être étendue.
 * 2. `App.attachBaseContext()` appelle `startKoin { … }`. Robolectric
 *    réinstancie l'Application pour chaque méthode de test : le second
 *    `startKoin` lève `KoinApplicationAlreadyStartedException` (Koin est un
 *    singleton global JVM).
 *
 * `GlobalContext.getOrNull()` permet de démarrer Koin **une seule fois** et de
 * le réutiliser ensuite : le conteneur DI est alors partagé par tous les tests
 * de la JVM, comme en production.
 *
 * Aucun `try/catch` : `stopKoin()` est sûr même quand Koin n'est pas démarré
 * (le bytecode teste `null` avant `close()`).
 */
class TestApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@TestApplication)
                modules(appModules)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // `App.onCreate()` appelle `PreferenceManager.setDefaultValues(...)` pour
        // les écrans de réglages. On ne le fait pas ici : l'inflation des XML
        // de préférences échoue sous Robolectric (`Resources$NotFoundException`)
        // et n'est pas nécessaire au test de lancement — `Preferences` lit
        // chaque valeur avec son défaut en ligne.
        //
        // Seule la lecture du mode nuit est reproduite : elle conditionne le
        // thème de l'activité (variante claire / sombre).
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
    }
}
