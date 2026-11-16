# Release v1.4.4 — correctif du crash « l'onglet Téléchargement crache »

> Règle **sans adb** : aucune commande `adb`, aucun appareil branché.
> Preuves : compilation locale, tests JVM/Robolectric, audit des bindings Koin, outils SDK.

## Le crash

**Symptôme** : tap sur l'onglet « Télécharger » (icône de téléchargement) → l'application plante.
**Cause racine** : deux types que Koin doit résoudre **n'étaient liés par aucun module** :

| Type manquant | Demandé par | Erreur |
|---|---|---|
| `DownloadDao` | `DownloadManager` (DownloaderModule.kt:21) | `NoDefinitionFoundException` |
| `Preferences` | `DownloadViewModel` (`prefs = get()`) | `NoDefinitionFoundException` |

`DownloadViewModel` est instancié par `DownloadFragment` (`by viewModel()`) : le premier accès à
l'onglet déclenche la résolution du graphe → `NoBeanDefFoundException` → crash.

**Correctif** : `MainModule.kt`
- `roomModule` : `factory { get<BMusicDatabase>().downloadDao() }`
- `mainModule` : `single { Preferences }`

## Garde-fous ajoutés (le crash ne peut plus revenir silencieusement)

| Garde-fou | Nature | Preuve |
|---|---|---|
| `scripts/check-koin-bindings.py` | Audit **statique** des 512 fichiers : tout type résolu par Koin (`get<T>()`, `by inject()`, **paramètres de constructeur**) doit avoir un binding | exit 0 sur l'état actuel ; **exit 1** si le binding `Preferences` est retiré (validé) |
| `KoinBindingsTest` | Résolution **runtime** des 14 types injectés + du graphe downloader | 3 tests, 0 échec |
| `KoinBindingsScriptTest` | Le script doit passer sur le dépôt **et** échouer sur un type non lié | 2 tests, 0 échec |

Résultat de l'audit : **50 types résolus par Koin, 79 bindings, 0 type non résolu.**

## Artefacts

Flavor `github`, build type `release` (R8 + shrink), signature **v2**.

| Artefact | Taille | SHA-256 |
|---|---|---|
| **`BMusic-1.4.4-github-arm64-v8a.apk`** (téléphone Android arm64) | 15 388 523 o (14,68 Mo) | `bb3f4a3838aa2a3989521feb28f65e120a81c23f66f4214a728051287be8c1fe` |
| `BMusic-1.4.4-github-armeabi-v7a.apk` | 14 843 017 o (14,16 Mo) | `c995199970c17a1a18a0897669e2d102c4ded7369ca6d485ce08ff7cce4d298e` |
| `BMusic-1.4.4-github-x86_64.apk` | 15 482 970 o (14,77 Mo) | `a92abeac2c6fcf1a3d9c25090a2efca15435d9c5c5bf42294208ff2deaced53a` |
| `BMusic-1.4.4-github-x86.apk` | 15 345 041 o (14,63 Mo) | `764f2343c764309a4fce0a09053240d2e101b2b8ccb26ee7189e3228e503dc60` |
| `BMusic-1.4.4-github-universal.apk` | 23 151 375 o (22,08 Mo) | `259680ed609066de8de5a5f7b8d7c19c8288ee6478327edec621b7ee82cc90e6` |

## Contrôles SDK

| Contrôle | Valeur | Attendu |
|---|---|---|
| `aapt2 dump badging` — package / version | `com.bmusic.app` / `1.4.4` / `1430402` | ✅ (> `1430301` de v1.4.3) |
| `aapt2 dump badging` — label / targetSdk / minSdk | `B Music` / `36` / `26` | ✅ |
| `apksigner verify` | v2 = **true**, v1/v3/v4 = false | ✅ |
| `apkanalyzer manifest print` — `com.bmusic.app.ui.debug` | absent | ✅ |
| `apkanalyzer manifest print` — `TestComposeActivity` | absent | ✅ |
| `launchable-activity` | `com.bmusic.app.MainActivity` | ✅ |
| Permissions | + `INTERNET` (téléchargeur), `FOREGROUND_SERVICE_DATA_SYNC` ; aucune localisation / caméra / SMS / contacts | ✅ |

## Tests

`./gradlew :app:testFdroidDebugUnitTest` → **BUILD SUCCESSFUL** — **121 tests, 0 échec, 0 erreur, 0 ignoré**

| Suite | Tests |
|---|---|
| SecurityModuleTest | 66 |
| GlassPolicyTest | 10 |
| DownloadManagerTest | 7 |
| **DownloadScreenTest** (onglet Lien par défaut, zone centrée) | 5 |
| MainActivityLaunchTest (Default/Landscape/Night/Tablet) | 16 |
| **KoinBindingsTest** (nouveau) | 3 |
| **KoinBindingsScriptTest** (nouveau) | 2 |
| GlassPolicyScriptTest / QueueStateHolderTest / MediaIDsTest / NewPipeExtractorEngineTest | 12 |

## Reste à vérifier sur l'appareil (aucune mesure device claimée ici)

| Action | Valeur attendue |
|---|---|
| Lancer l'app | aucun crash au démarrage |
| Tap sur l'onglet **Télécharger** | **l'écran s'ouvre** (onglet Lien actif, zone de saisie centrée) — c'est le correctif validé ici |
| Coller un lien → « Analyser » | aperçu, ou message « Source non prise en charge » explicite |
| Lecture écran verrouillé / mode avion | la lecture continue |
| Play Protect | « app non publiée » → *Installer quand même* |

## Résidu connu

L'APK est signé avec la **clé debug** locale (SHA-256 `eacbe8c8cd0a3b6a0ef4a3772bcc06c244c22c420814f73654fb129b7752bf22`) :
aucun `keystore.properties` ni variable d'environnement n'est disponible sur cette machine. Si une
version de l'application est déjà installée avec une **autre** clé, l'installation échouera
(« App not installed ») : désinstaller l'ancienne version, ou fournir le keystore de release
(`keystore.properties` ou `KEYSTORE_FILE` / `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_PASSWORD`).
