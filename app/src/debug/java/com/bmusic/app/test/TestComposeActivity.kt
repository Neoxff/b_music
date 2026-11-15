package com.bmusic.app.test

import androidx.activity.ComponentActivity

/**
 * Activité **de test uniquement** (variante debug).
 *
 * `MainActivity` ne peut pas servir d'hôte aux tests Compose : `AbsSlidingMusicPanelActivity.onCreate`
 * appelle `finish()` quand les permissions Android manquent (cas Robolectric), ce qui détruit
 * l'activité et fait échouer `createAndroidComposeRule`.
 *
 * Cette activité n'héberge que le contenu Compose testé, sans démarrer l'application.
 * Elle n'existe que dans la variante debug : le build release n'en contient aucune trace.
 */
class TestComposeActivity : ComponentActivity()
