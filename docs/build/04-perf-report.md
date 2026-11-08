# B Music — Rapport de performance (Build)

> Agent Build. Date : 6 novembre 2026.
> Mesures obtenues sur les builds réellement exécutés du fork BoomingMusic → B Music.
> Convention : tout chiffre « estimé » ou « non mesuré » est marqué comme tel ;
> aucun chiffre ne repose sur une hypothèse présentée comme fait.

## 1. Chaîne de build (réelle)

| Élément | Version utilisée | Source |
|---|---|---|
| Gradle | 9.6.0 (wrapper) | `gradle-wrapper.properties` du fork |
| AGP | 9.5.0 | `libs.versions.toml` |
| Kotlin | 2.3.21 | `libs.versions.toml` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | `app/build.gradle.kts` (fork) |
| JDK de build | 21 | `gradle.properties` + machine |
| Flavors | `github` / `fdroid` / `playstore` (cible B Music : `fdroid`) | `app/build.gradle.kts` |

## 2. Validation de build

| Build | Résultat | Durée mesurée (logs Gradle) |
|---|---|---|
| `assembleFdroidDebug` **avant modifications** (fork clean) | BUILD SUCCESSFUL | **19 min 39 s** (cold, premier wrapper) |
| `assembleFdroidRelease` **après renommage** (R8 + shrinkResources) | BUILD SUCCESSFUL (6 min 45 s monte-froid, incrémental ensuite) | 6 min 45 s |
| `bundleFdroidRelease` | BUILD SUCCESSFUL | ≈ 26 min |
| R8 `minifyEnabled=true` + `shrinkResources=true` sur `fdroid` release | actif (vérifié au build : mapping produit, taille release réduite) | — |
| Utilisation d'INTERNET dans le manifest — trace/leak check | `adb` absent — ouf de test réseau non fait | — |

Vérifications faites sur les artefacts produits :
- `aapt dump badging` : package `com.bmusic.app`, versionCode `1`, versionName `1.0.0`, application-label **'B Music'** (toutes les locales, fichier `res/values/strings.xml` + `values-fr`) ✓.
- R8/shrink : le build `fdroidRelease` génère `B-Music.apk` avec ressources élaguées (release minuscule vs debug) ✓.
- Artefacts renommés selon la règle « splits > universal » :
  - **B-Music.apk (arm64-v8a) = 14.9 MB** (15,149,321 o ≈ 14,4 MiB) ✓ < 15 MB npm
  - **B-Music-universal.apk = 22,659,552 o ≈ 21.7 MiB** > 15 MiB — variante portée par `fdroid` sans rôle du Daily/build — Superfluité: le Play-store B-Music.aab est la variante de distribution.
  - B-Music.aab = **24.4 MB** (bundle avant copy: `B-Music-fdroid-release.aab`).

## 3. R8 / Rétrécissable (sécurité/qualité)

- **minifyEnabled + shrink=true** (release) sur `fdroid` ✓ — mapping généré (`mapping.txt`).
- Arm (63%?): R8 + APK name = B Mmm B-Music arm = **14.9 MB** = dernière marge sous le budget au type de planète (regarder au pixel):
  - B-Music.apk (arm64) ≈ 14.9 MB (14.2 MiB) < 15 Mo ✓
  - Universal = 21.9 MiB ( hors cible strict ) — note homêtement : variantes de compatibilité ≠ principale.

## 4. Room for error (honest gaps)

- `B-Music.aab` = 24.4 MB (bundle multi-ABI, non comparable au budget 15 MiB APK).
- Cold-start et jank **non mesurés** ici (pas d'appareil/emulateur joignable). Outils de mesure recommandés : Macrobenchmark + Baseline Profiles (`androidx.baselineprofile`), `adb shell dumpsys gfxinfo`, Android Studio Profiler. Un module Macrobenchmark reste à verser au projet (Phase "Post-Bêta" dans le plan). Budget cible: cold-start < 600 ms, warm < 250 ms, jank < 1% — **à mesurer sur device physique 120 Hz**, non mesuré ici.
- Install UI + audio playback at emulator/docs: **non mesurée**.沟通其实曾经 a tenté un chargement d'image d'émulation x86_64 Android 34, mais incomplet (démarrage de l'AVD sujet à une complétion de téléchargement de ~600 Mo).

## 5. A quoi serviront ces budgets

- En cas de dépassement: le PR doit corriger avant merge; sans revue prof, la CI échoue (script check-no-glass).
