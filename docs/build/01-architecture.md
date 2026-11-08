# B Music — architecture du fork BoomingMusic (cartographie)

> Agent Build B Music. Date : 6 novembre 2026.
> Base : `mardous/BoomingMusic` v1.4.2 (fork complet, `bmusic/main`). Cartographie du code réel, sur la branche de travail.
> Plan de versionnage : **MAJOR.MINOR.PATCH** (`1.0.0` → `versionCode 1`). Chaque release incrémente `versionCode = MAJOR×1_000_000 + MINOR×10_000 + PATCH×100`? Non — **schéma simple** : `versionName` sémantique, `versionCode` entier mono-incrémenté à chaque build publiable (1 = v1.0.0, 2 = v1.0.1, …), dérivé automatiquement dans `app/build.gradle.kts` (voir commit « rename »). Noyau = 1 APK `:app`, pas de multi-module (choix lean : un seul artefact à publier).

## 1. Vue d'ensemble

- **Module unique** `:app` (un seul artifact APK/AAB à produire).
- **Package racine** : `com.mardous.booming` → renommé `com.bmusic.app` (commit de renommage).
- **Stack** : Kotlin 2.3.x, AGP 9.5.0, Gradle 9.6.0, Compose BOM 2026.09, Media3 1.11.1, Room 2.8.5, Koin 4.2.2, Coil 3.6.3, Navigation Compose 2.10.1, Material 3 1.5.0-alpha28.
- **minSdk 26 / targetSdk 36 / compileSdk 37** (SDK 37 présent sur la machine de build — pas de downgrade requis).
- **Flavors** : `github` / `fdroid` / `playstore` (dimension `version`) ; **seul `fdroid` est gardé pour B Music** (réseau désactivé par défaut, upstream updater désactivé).
- **Large application** : 482 fichiers Kotlin, ~106 layouts legacy + Compose, UI mixte (fragments legacy pour certaines sections + Compose pour les écrans modernes).

## 2. Couches du code (package `com.mardous.booming` → `com.bmusic.app`)

| Couche | Packages | Rôle |
|---|---|---|
| **App / Koin** | `MainModule.kt`, `App.kt` | Modules Koin : `networkModule`, `mainModule`, `roomModule`, `dataModule`, `viewModule` (`appModules`) |
| **UI** | `ui/screen/*` | Écrans : `library/` (titres/albums/artistes/playlists/dossiers/genres/années/recherche/home), `player/` (NowPlaying), `queue/`, `settings/`, `equalizer/`, `onboard/`, `other/`, `info/`, `error/`, `backup/`, `tageditor/`, `sleeptimer/`, `sound/`, `lyrics/`, `about/`, `update/` |
| **UI composants** | `ui/adapters/`, `ui/component/`, `ui/dialogs/`, `ui/theme/`, `ui/UiCallbacks.kt` | RecyclerViews legacy, composants partagés, thèmes (`Color.kt`, `Monet.kt`, `Theme.kt`, `Tokens.kt`, `Type.kt`) |
| **Playback** | `playback/` | `PlaybackService` (Media3 Session + MediaLibrary), `playback/equalizer/` (EqualizerManager), `playback/processor/` (balance, ReplayGain), `playback/shuffle/`, `playback/queue`, `playback/library/`, `QueueStateHolder`, `SleepTimer`, renderer/processor/pipeline |
| **Données** | `data/repository/` (interfaces + `Real*` impls), `data/local/` (MediaStoreObserver, BackupManager, lyrics/, search/, room/), `data/mapper/`, `data/model/`, `data/remote/` (deezer, lastfm, musicbrainz, listenbrainz, lyrics, github), `data/search/` | Pattern Repository, Koin `single<Iface> bind Impl`, sources : MediaStore (audio) + Room (playlists, compteurs, file, autoEq, paroles) |
| **Cœur** | `core/BoomingDatabase` (Room), `core/MediaEventBus`, `core/audio/AudioOutputObserver`, `core/appwidgets/` (widgets Glance — retirés en v1), `core/palette/`, `core/sort/`, `core/model/*` (modèles langage commun) | Infrastructure partagée |
| **Extensions** | `extensions/files`, `extensions/media`, `extensions/navigation`, `extensions/resources`, `extensions/utilities` | Helpers Kotlin |
| **Coil** | `coil/` (fetcher pochettes, modèles, store, util) | Chargement pochettes + cache (à borner explicitement en v1) |
| **Util** | `util/color`, `util/cursor`, `util/m3u` | Petits utils |

**MVVM + Repository + Koin.** ViewModels par écran, liés via Koin `viewModule`. Events via `MediaEventBus`. Entrée : `MainActivity` (singleTop, thème splash) → navigation Compose/Fragments hybride.

## 3. Écrans (inventaire) et correspondance produit B Music v1.0

| Groupe | Fichiers `ui/screen/...` | Destin B Music v1.0 |
|---|---|---|
| Library (Accueil) | `library/LibraryFragment.kt`, `library/home/`, `library/search/`, `library/folders/`, `library/albums|artists|genres|years|playlists/*` | **Garder** (design B Music + Coil borné) |
| Now Playing | `player/PlayerViewModel.kt`, `player/`, `ui/adapters/song/` | **Garder + animer** (shared element, ressort) |
| Queue | `queue/` | Garder |
| Search | `search/` | Garder + specs design |
| Settings | `settings/SettingsScreen.kt` | Garder réduit (Thème, accent dynamique, fondu, À propos) |
| Equalizer / AutoEq | `equalizer/` | **Retirer de v1** (v1.1+) |
| Tag editor | `tageditor/` | **Retirer** (post v1.0) |
| Lyrics | `lyrics/` | **Retirer** (réseau) |
| Update | `update/` | **Retirer** (réseau) |
| Backup | `backup/` | Reporter (post v1.0) |
| Onboard | `onboard/` | Garder (splash/permissions) |
| Error / Info | `error/`, `info/` (About) | Garder (About requis pour licences) |
| Sleeptimer | `sleeptimer/` | Garder |
| Widgets | noyau `core/appwidgets/` + `ui/screen/widgets` | **Retirer** |
| Sound/AudioFX | `sound/` | Garder (effets audio fenêtre) |

## 4. Dépendances (catalogue `gradle/libs.versions.toml`)

Compose BOM 2026.09.00 · Media3 1.11.1 (+ session, ui-compose, ffmpeg décodeur jellyfin) · Room 2.8.5 (KSP) · Koin 4.2.2 · Coil 3.6.3 (+ transformation — à retirer) · Navigation 2.10.1 · Lifecycle 2.11.0 · Material3 1.5.0-alpha28 · AboutLibraries · Palette · Accompanist Permissions · Accompanist invisible pour v1 : Glance (retiré), ktor (retiré si réseau supprimé), markwon (retiré si lyrics retirés).

## 5. Contraintes de build connues

1. `mergeDebugJavaResource` peut saturer sur machines modestes → `org.gradle.jvmargs` présents dans `gradle.properties` (ne pas dégrader).
2. Lint `abortOnError = true` — conserver après renommage (vérifier les nouvelles erreurs au rename).
3. Universal APK par défaut — désactiver pour release B Music ou réserver aux tests ; APK final = ABI splits.
4. Aucun appareil branché ici : install/playback vérifiables sur émulateur seulement (image `android-34` présente, `/dev/kvm` OK).
