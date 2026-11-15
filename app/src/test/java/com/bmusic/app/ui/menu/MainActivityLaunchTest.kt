package com.bmusic.app.ui.menu

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.bmusic.app.MainActivity
import com.bmusic.app.R
import com.bmusic.app.util.Preferences
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Test de régression du **NPE au lancement** de `MainActivity`.
 *
 * Cause historique : `AbsSlidingMusicPanelActivity.updateTabs()` fait
 * `navigationView.menu.clear()` puis ne ré-ajoute que les catégories de
 * bibliothèque visibles. `nav_download` n'était donc **plus dans le menu**,
 * et `MainActivity.setupNavigationController()` faisait
 * `navigationView.menu.findItem(R.id.nav_download).isVisible = …` **sans appel
 * sûr** → `findItem()` renvoyait `null` → `NullPointerException` dès
 * `onCreate`, quel que soit l'état du flag.
 *
 * Ce test vérifie les deux faces du correctif :
 * 1. l'activité se lance **sans exception** (avant : NPE) ;
 * 2. l'entrée « Télécharger » est **présente et visible** quand le flag est
 *    actif, et **absente** quand il est inactif (avant : absente dans les
 *    deux cas → l'onglet était inaccessible).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = com.bmusic.app.test.TestApplication::class)
abstract class MainActivityLaunchTest {

    private fun launch(downloaderEnabled: Boolean): MainActivity {
        // Positionné avant la création de l'activité : `updateTabs()` et
        // `setupNavigationController()` lisent le flag pendant `onCreate`.
        Preferences.downloaderEnabled = downloaderEnabled

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        // Sans permission de stockage, `AbsSlidingMusicPanelActivity.onCreate`
        // redirige vers `OnboardActivity` et `finish()` : le chemin testé
        // (reconstruction du menu) ne serait pas emprunté.
        shadowOf(app).grantPermissions(
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        val intent = Intent(app, MainActivity::class.java)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        // `MainActivity` déclare le thème splash (`Theme.BMusic.SplashScreen`),
        // dont `windowSplashScreenAnimatedIcon` pointe sur un
        // `animated-vector` que Robolectric ne sait pas résoudre : l'échec
        // surviendrait dans `AppCompatDelegateImpl.attachToWindow`, avant
        // même `onCreate`, et masquerait le NPE que ce test doit détecter.
        // On utilise donc le thème applicatif — aucun changement de production.
        controller.get().setTheme(R.style.Theme_BMusic_FollowSystem)
        return controller.create().get()
    }

    @Test
    fun `lancement sans exception avec le flag Telechargeur desactive`() {
        val activity = launch(downloaderEnabled = false)
        assertNotNull("MainActivity doit être instanciée", activity)
    }

    @Test
    fun `lancement sans exception avec le flag Telechargeur active`() {
        val activity = launch(downloaderEnabled = true)
        assertNotNull("MainActivity doit être instanciée", activity)
    }

    @Test
    fun `flag actif - l entree Telecharger est presente et visible`() {
        val activity = launch(downloaderEnabled = true)
        val item = activity.navigationView.menu.findItem(R.id.nav_download)
        assertNotNull("l'entrée « Télécharger » doit exister dans le menu reconstruit", item)
        assertTrue("l'entrée « Télécharger » doit être visible", item!!.isVisible)
    }

    @Test
    fun `flag inactif - aucune entree Telecharger dans le menu`() {
        val activity = launch(downloaderEnabled = false)
        assertNull(
            "l'entrée « Télécharger » doit être absente quand le flag est inactif",
            activity.navigationView.menu.findItem(R.id.nav_download),
        )
    }
}

/** Variante portrait (téléphone). */
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class MainActivityDefaultLaunchTest : MainActivityLaunchTest()

/** Variante paysage. */
@Config(sdk = [34], qualifiers = "w800dp-h400dp-land")
class MainActivityLandscapeLaunchTest : MainActivityLaunchTest()

/** Variante tablette. */
@Config(sdk = [34], qualifiers = "sw600dp-w800dp-h1280dp")
class MainActivityTabletLaunchTest : MainActivityLaunchTest()

/** Variante thème sombre. */
@Config(sdk = [34], qualifiers = "w400dp-h800dp-night")
class MainActivityNightLaunchTest : MainActivityLaunchTest()
