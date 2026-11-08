# B Music — Baseline de non-régression v1.0.0 (étape 0)

> Agent Build, branche `feat/dl-etape-0`. Date : 8 novembre 2026.
> Objectif : figer les valeurs de référence avant l’ajout du téléchargeur (v1.1).
> Convention : toute valeur non mesurée est marquée **non mesuré** — aucun chiffre inventé.

## 1. Chaîne de build (mesurée)

| Élément | Version | Source |
|---|---|---|
| Gradle | 9.6.0 (wrapper) | `gradle-wrapper.properties` |
| AGP | 9.5.0 | `libs.versions.toml` |
| Kotlin | 2.3.21 | `libs.versions.toml` |
| Media3 | 1.11.1 | `libs.versions.toml` |
| Room | 2.8.5 | `libs.versions.toml` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | `app/build.gradle.kts` |
| JDK de build | 21.0.13 (Arch Linux) | `./gradlew --version` |
| Flavor de référence | `fdroid` | `app/build.gradle.kts` |

`./gradlew :app:assembleFdroidRelease` → **BUILD SUCCESSFUL** (vérifié le 8 nov. 2026).

## 2. Taille APK (mesurée)

| Artefact | Taille | Budget Lean | Verdict |
|---|---|---|---|
| `B-Music.apk` (arm64-v8a, fdroid release, R8) | **14 896 700 o ≈ 14,2 MiB** | < 15 Mo | ✅ vert |
| `B-Music-universal.apk` | ~22,7 Mo | — (non distribué) | info |
| `B-Music.aab` | 24 400 163 o | — (bundle multi-ABI) | info |

Vérifié par `aapt dump badging` : `com.bmusic.app`, versionCode `1`, versionName `1.0.0`, label « B Music », minSdk 26, targetSdk 36.

Pour la v1.1 (téléchargeur), le budget révisé proposé est **≤ 20 Mo arm64** (extracteur), à valider par Lean.

## 3. Métriques runtime (non mesurées — aucun device/émulateur disponible)

`adb devices` : **aucun appareil**. Aucune valeur ci-dessous n’a été mesurée ; les cibles sont celles de `docs/lean/03-budgets-performance.md`.

| Métrique | Baseline v1.0.0 | Cible Lean | Commande de mesure |
|---|---|---|---|
| Cold-start | **non mesuré** | < 600 ms | `:macrobenchmark:StartupBenchmark.measureColdStartup` |
| Warm-start | **non mesuré** | < 250 ms | `:macrobenchmark:StartupBenchmark.measureWarmStartup` |
| Jank défilement 5 000 titres | **non mesuré** | < 1 %, 0 frame perdue | `:macrobenchmark:LibraryScrollBenchmark` |
| Transition mini→plein écran | **non mesuré** | ≤ 16 ms (8 ms @120 Hz) | `:macrobenchmark:PlayerBenchmark` |
| Mémoire RSS en lecture | **non mesuré** | < 200 Mo | Android Studio Profiler, lecture 30 min |
| Temps de scan 5 000 titres | **non mesuré** | < 5 s | `MainActivity.scanAllPaths()` + chronomètre |

Commande unique (device requis) :

```bash
./gradlew :macrobenchmark:connectedFdroidBenchmark
```

## 4. Tests créés à l’étape 0

| Test | Fichier | Exécution |
|---|---|---|
| File d’attente (`QueueStateHolder`) | `app/src/test/.../QueueStateHolderTest.kt` | `./gradlew :app:testFdroidDebugUnitTest` |
| Résolution MediaItem ↔ Song (`MediaIDs`) | `app/src/test/.../MediaIDsTest.kt` | idem |
| Migration Room v8 (données intactes) | `app/src/androidTest/.../BMusicDatabaseMigrationTest.kt` | `./gradlew :app:connectedFdroidDebugAndroidTest` (device requis) |
| Macrobenchmark (startup/scroll/player) | `macrobenchmark/` | `./gradlew :macrobenchmark:connectedFdroidBenchmark` (device requis) |

Aucun code de production n’a été modifié à l’étape 0 (ajouts : tests, module `macrobenchmark`, dépendances de test, `include(":macrobenchmark")` dans `settings.gradle.kts`).

## 5. Captures de référence

Procédure et liste des fichiers attendus : voir `docs/baseline/captures/README.md`.
**Statut : captures non générées** (aucun device/émulateur disponible) — à produire sur device avant l’étape 5 (UI downloader).
