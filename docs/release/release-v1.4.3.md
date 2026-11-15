# Release v1.4.3 — onglet « Lien » par défaut + correctif de lancement

> Règle **sans adb** appliquée : aucune commande `adb`, aucun appareil branché.
> Toutes les preuves ci-dessous proviennent de la **compilation locale**, des **tests JVM/Robolectric**,
> des **scripts de politique** et des **outils SDK** (`apksigner`, `aapt2`, `apkanalyzer`).

## Artefacts

Flavor `github`, build type `release` (R8 + shrink des ressources), signature v2.

| Artefact | Taille | SHA-256 |
|---|---|---|
| `BMusic-1.4.3-github-arm64-v8a.apk` **(téléphone Android arm64)** | 15 388 523 o (14,68 MiB) | `3a2694f84ed51a304fc054aae4fd7fe6a17a38c7e4f4ed0d5a77a861c0d7a95b` |
| `BMusic-1.4.3-github-armeabi-v7a.apk` | 14 843 017 o (14,16 MiB) | `91501293e4db1403f3e6aeb7c849000f18f6aace1501a6c142f52f7649e56afe` |
| `BMusic-1.4.3-github-x86_64.apk` | 15 482 970 o (14,77 MiB) | `5bce2159382480da533c964d1a766419f53f0403c919794f6509471b3800aea3` |
| `BMusic-1.4.3-github-x86.apk` | 15 345 041 o (14,63 MiB) | `490ac756c0ff04917edb6e67d5a3da421600157575bedca51d7f8ac3a75af584` |
| `BMusic-1.4.3-github-universal.apk` | 23 151 375 o (22,08 MiB) | `6ed82e4744f8751b108d179af9e8e4277f9fb7420e00220bde8a828e9204af95` |

Budget LEAN : APK ≤ 16,5 Mo (`docs/lean/budgets-v3.md`) → **14,68 MiB respecté** pour l'arm64.
L'universal (22,08 MiB) dépasse le budget : il est fourni par confort, l'arm64 est l'artefact de référence.

## Identité de l'application (contrôles SDK)

| Contrôle | Valeur observée | Attendu |
|---|---|---|
| `aapt2 dump badging` — package | `com.bmusic.app` | ✅ |
| `aapt2 dump badging` — label | `B Music` | ✅ |
| `aapt2 dump badging` — versionName | `1.4.3` | ✅ |
| `aapt2 dump badging` — versionCode | `1430301` | ✅ > `1420300` (v1.4.2) |
| `aapt2 dump badging` — targetSdk / minSdk | `36` / `26` | ✅ |
| `aapt2 dump badging` — ABI | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` | ✅ |
| `apksigner verify --verbose` | `Verifies`, v2 = **true**, v1/v3/v4 = false | ✅ (v2 suffit pour minSdk 26) |
| `apkanalyzer manifest print` — `com.bmusic.app.ui.debug` | **absent** | ✅ (Diagnostics hors release) |
| `apkanalyzer manifest print` — `TestComposeActivity` | **absent** | ✅ (variante debug exclue du release) |
| `launchable-activity` | `com.bmusic.app.MainActivity` | ✅ |

### Signature — **point de vigilance**

```
V2 Signer: certificate DN: C=US, O=Android, CN=Android Debug
V2 Signer: certificate SHA-256: eacbe8c8cd0a3b6a0ef4a3772bcc06c244c22c420814f73654fb129b7752bf22
```

Aucun `keystore.properties` ni variable `KEYSTORE_FILE`/`KEY_ALIAS`/`KEY_PASSWORD`/`STORE_PASSWORD`
→ `app/build.gradle.kts` bascule sur la **clé de debug locale**.

Conséquence : l'installation **par-dessus** une version installée avec une *autre* clé
(APK publié, ou APK construit sur une autre machine) échoue avec « *App not installed* ».
Il faut alors désinstaller l'ancienne app (perte des données de l'app) **ou** fournir le keystore de release.

## Permissions déclarées

```
ACCESS_NETWORK_STATE · BLUETOOTH_CONNECT · BLUETOOTH (maxSdk 30) · FOREGROUND_SERVICE ·
FOREGROUND_SERVICE_DATA_SYNC · FOREGROUND_SERVICE_MEDIA_PLAYBACK · INTERNET ·
MODIFY_AUDIO_SETTINGS · POST_NOTIFICATIONS · READ_EXTERNAL_STORAGE (maxSdk 32) ·
READ_MEDIA_AUDIO · READ_MEDIA_IMAGES · RECEIVE_BOOT_COMPLETED · WAKE_LOCK ·
WRITE_EXTERNAL_STORAGE (maxSdk 29) · com.bmusic.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

- `INTERNET` est requis par le **téléchargeur** (onglet Lien) : l'onglet n'est actif que si
  `Preferences.downloaderEnabled` est vrai — désormais **vrai par défaut** (voir « Décisions »).
- `WRITE_EXTERNAL_STORAGE` est plafonné à API 29 ; au-delà, l'écriture passe par MediaStore.
- `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` est générée par AndroidX, non demandée à la main.
- Aucune permission de localisation, de caméra, de contacts, de SMS, d'installations.

## Tests (aucune commande adb)

`./gradlew :app:testFdroidDebugUnitTest` → **BUILD SUCCESSFUL**

| Suite | Tests | Échecs | Erreurs |
|---|---|---|---|
| SecurityModuleTest | 66 | 0 | 0 |
| GlassPolicyTest | 10 | 0 | 0 |
| GlassPolicyScriptTest | 4 | 0 | 0 |
| DownloadManagerTest | 7 | 0 | 0 |
| MainActivityLaunchTest (Default/Landscape/Night/Tablet) | 16 | 0 | 0 |
| **DownloadScreenTest (nouveau)** | **5** | 0 | 0 |
| QueueStateHolderTest + MediaIDsTest + NewPipeExtractorEngineTest | 8 | 0 | 0 |
| **Total** | **116** | **0** | **0** |

`DownloadScreenTest` (Robolectric + Compose) prouve :

1. `DownloadViewModel.tab` vaut **0 (Lien)** au démarrage, et `prefill(url, toLink=true)` y reste ;
2. le contenu affiché est celui de l'onglet **Lien** (« Coller un lien »), celui d'**Historique** n'est pas composé ;
3. la zone de saisie est **centrée horizontalement** — centre X du titre = centre X de l'écran (tolérance 2 dp) ;
4. la zone de saisie est **centrée verticalement** — centre du bloc = centre de la zone de contenu (tolérance 16 dp).

## Scripts de politique (verdicts sur la source)

| Script | Verdict |
|---|---|
| `scripts/check-glass-policy.sh` | `✓ glass-policy OK` (liste blanche, rayon ≤ 24 dp, aucun effet vitreux) — exit 0 |
| `scripts/check-no-glass.sh` | `✓ aucun blur/glass (RenderEffect/BlurMaskFilter/haze) dans app/src` — exit 0 |

## Décisions de version et d'exposition

- **Version** : `1.0.0`/code `1` → **`1.4.3`/code `1430301`**
  (schéma `1*MAJOR + 4*MINOR + 3*PATCH + 0*VARIANT + 301`, MAJOR=1e6, MINOR=1e5, PATCH=1e4).
  Motif : le `versionCode` 1 était **inférieur** à celui de v1.4.2 (1420300) → Android refusait
  l'installation (« package downgraded »). Le nom suit le tag précédent (1.4.x).
- **Feature flag `downloaderEnabled`** : défaut passé de `false` à **`true`**.
  Motif : l'onglet « Lien » doit être **visible** (livrable « Lien en premier / par défaut »).
  Reverti en une ligne : `Preferences.kt` → `getBoolean(DOWNLOADER_ENABLED, false)`.

## Contenu de la release

**Correctif de lancement (bloquant)**

- `MainActivity.kt:165` : `findItem(R.id.nav_download)?.isVisible = …` (le `?.` seul masquait le crash
  en laissant l'onglet invisible) **et** `updateTabs()` ré-ajoute `nav_download` quand le flag est actif.
- Cause : `updateTabs()` fait `menu.clear()` puis `nav_download` est absent → `MenuItem.setVisible`
  sur `null` → `NullPointerException` **inconditionnel** au lancement.
- Preuve rouge→vert : **16 tests rouges** (message exact `Cannot invoke "android.view.MenuItem.setVisible(boolean)"
  because the return value of "android.view.Menu.findItem(int)" is null`) puis **97 tests verts** avec le correctif.

**Écran « Télécharger »**

- Onglet **Lien** en **premier** et **par défaut**, onglet **Historique** en second (barre en verre, indicateur 3 dp glissant en ressort via `Motion.kt`).
- Zone de saisie **centrée** (H et V), largeur plafonnée à **560 dp**, marges 20 dp, champ 56 dp / rayon 28 dp, remontée en ressort à l'aperçu.
- Verre posé **uniquement** sur : barre d'onglets, zone de saisie, carte d'aperçu, mini-barre de progression.
- Icône `ic_link_24dp` créée (2 anneaux entrelacés, trait 1,75 dp).
- **Onglet Recherche retiré** : `SearchTab.kt` + `SearchViewModel.kt` supprimés (recherche du téléchargeur
  sans implémentation) et binding Koin retiré. La **recherche locale** (bibliothèque) est intacte.

## Ce qui n'est **pas** prouvé (à faire sur l'appareil, sans adb)

| Point | État | Ce qu'il faut |
|---|---|---|
| Lecture en arrière-plan écran verrouillé | non testé | ouvrir un morceau, verrouiller l'écran, écouter |
| Commandes de l'écran de verrouillage | non testé | pause/reprise depuis le verrouillage |
| Mode avion | non testé | lecture hors ligne |
| Sélection de fichiers (sélecteur) | non testé | toucher « Coller » puis choisir un fichier |
| Extraction d'un lien réel (YouTube/TikTok/Instagram) | non testé | coller un lien, appuyer sur « Analyser » |
| Journal d'échec (`DownloadTrace`) | **absent du code** | pas de diagnostic d'échec : finding F-004 ouvert |
| Jank / PSS (budgets LEAN A–F) | non mesuré | export de l'écran **Diagnostics** (debug) |
| Erreurs Play Protect | attendu | « apps non publiées » → *Installer quand même* |
