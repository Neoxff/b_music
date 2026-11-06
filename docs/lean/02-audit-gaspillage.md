# B Music v1.0 — audit anti-gaspillage

> Agent Lean B Music. Date : 6 novembre 2026.
> Base auditée : **mardous/BoomingMusic v1.4.2** (clone shallow `/tmp/opencodev2/booming`),
> comparée à l'état actuel du dépôt produit (`app/` Capacitor, maquette « Sonore »).
> Règle : **aucune modification de code** ; actions destinées à l'agent Build.
>
> Honnêteté de mesure : « mesuré » = valeur observée (fichiers, API GitHub, `du`).
> « estimé » = projection chiffrée à valider par Build. Le lint Android **n'a pas pu aller au bout**
> (téléchargement Gradle 9.6 interrompu, pas d'appareil branché) : les comptages « zéro référence »
> sont faits par analyse statique (`grep`), pas par Lint `UnusedResources`. Build doit rejouer
> `./gradlew :app:lintFdroidRelease` et joindre le rapport HTML.

## 1. Mesures avant

### 1.1 APK (mesuré, API GitHub releases v1.4.2)

| Artefact fdroid | Taille (octets) | Taille |
|---|---|---|
| universal | 22 968 019 | **21,9 Mo — hors budget** |
| arm64-v8a | 15 205 167 | 14,5 Mo — **à la limite** |
| armeabi-v7a | 14 659 661 | 14,0 Mo |
| x86 | 15 161 685 | 14,5 Mo |
| x86_64 | 15 299 614 | 14,6 Mo |

- Cible v1.0 : **< 15 Mo (arm64-v8a)**, universal supprimé du release (ABI splits seules).
- État actuel du dépôt (maquette Capacitor, mesuré ici) : `app-release.apk` **970 663 o (0,93 Mo)**, `app-debug.apk` 4 128 734 o (3,9 Mo). Petite taille, mais **ce n'est pas un lecteur** : mesure non comparable au produit v1.0.
- Constat : BoomingMusic est **déjà** en R8 + `isShrinkResources = true` + ABI splits + `dependenciesInfo` désactivé. Le levier n'est donc pas la config, c'est le **contenu** (réseau, widgets, locales, polices).

### 1.2 Ressources (mesuré, `du` / comptages)

| Poste | Mesuré | Note |
|---|---|---|
| `res/` total | **6,2 Mo** | 468 fichiers |
| `values*/strings.xml` (~46 locales) | **2,9 Mo** | base `values/` = 148 Ko / 998 chaînes ; fr = 88 Ko |
| `font/` GoogleSansFlex ×7 graisses | **900 Ko** | variable font unique ×7 fichiers |
| `drawable*` (y c. xxxhdpi) | **~1,3 Mo** | 580 Ko d'images png/webp/jpg au total |
| Mipmaps launcher | ~80 Ko | OK |
| Layouts XML (dont legacy View) | **106 fichiers** | coexiste avec Compose → dette, pas du gaspillage de poids |
| Code Kotlin | **482 fichiers** | dont écrans legacy (Fragments/View) + Compose |

### 1.3 Dépendances redondantes / à zéro référence (analyse statique)

| Dépendance (catalogue) | Références dans le code | Sortie prévue |
|---|---|---|
| `androidx.swiperefreshlayout:swiperefreshlayout` | **0** | retirer |
| `com.github.bosphere.android-fadingedgelayout:fadingedgelayout` | **0** | retirer |
| `com.github.albfernandez:juniversalchardet` | **0** (encodage détecté ailleurs) | retirer |
| `com.github.Commit451.coil-transformations:transformations` | **0** (`transformations` n'apparaît pas) | retirer |
| `lottie` | **absent** — une seule approche d'animation (Compose `compose.animation`, 20 fichiers) | ✅ rien à faire |
| `markwon` (1 fichier, `ViewExt.kt`) **et** `compose-markdown` (1 fichier) | **doublon** Markdown View + Compose | garder **un seul** (compose-markdown), retirer markwon (~700 Ko estimés) |
| `mediarouter` (3 fichiers) | Chromecast | retirer (hors périmètre v1) |
| `glance` (22 fichiers) | Widgets Glance | reporter/retirer du build v1 (~1,5 Mo estimés avec préview) |
| `ktor-client-*` + okhttp (21 fichiers) | Réseau (paroles en ligne, artistes, update) | retirer pour v1 (~1,5–2 Mo estimés) ; `coil-network-ktor3` → coil mémoire/disque local uniquement |
| `taglib` (4) + `jaudiotagger` (4) | Éditeur de tags (reporté) | retirer du build v1, branche `tags/` conservée (~2 Mo estimés avec JNI) |
| `versioncompare` (1) | Mise à jour intégrée (supprimée) | retirer |
| `reorderable` (1) + `advrecyclerview` (2) | **deux** mécanismes de réordonnancement (Compose vs legacy) | garder `reorderable` (Compose) |
| `fastscroll` (8) + `LazyColumnScrollbar` (13) | **deux** scrollbars | garder un seul mécanisme |
| `keyboardvisibilityevent` (1, View) | clavier legacy | retirer si la recherche est Compose |
| `aboutlibraries` (2) | Attributions licences | **garder** (obligation GPL-3.0) |

### 1.4 Manifest (mesuré)

À **retirer** pour v1 (outil identique liste déjà dans `PERMISSIONS.md`, donc maintenue) :

- `INTERNET`, `ACCESS_NETWORK_STATE` → **aucun besoin offline** (artwork local via composants existants + pochette embarquée).
- `MODIFY_AUDIO_SETTINGS` → non requis pour média 3 (l'app ne règle pas le volume système) ; à valider par Build, sinon garder.
- `android:requestLegacyExternalStorage="true"` → ignoré ≥ targetSdk 30, retirer.
- `android:largeHeap="true"` → **contredit le budget mémoire (< 200 Mo)** et masque les fuites ; retirer.
- Garder : `READ_MEDIA_AUDIO` (33+), `READ_EXTERNAL_STORAGE` (≤ 32), `READ_MEDIA_IMAGES` (couvertures `cover.jpg`), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE(_MEDIA_PLAYBACK)`, `WAKE_LOCK`, `BLUETOOTH(_CONNECT)` (boutons média), `WRITE_EXTERNAL_STORAGE` ≤ 29 (export).
- Widgets (5 receivers) + Android Auto (meta-data) + `WidgetSettingsActivity` : **retirer du manifest v1** (fonctionnalités reportées) — supprime aussi les `xml/widget_*_info`.

### 1.5 Temps de démarrage à froid

- **Non mesurable ici** (pas de device/émulateur ; baseline AndroidX ReBaseline absente). Cible : **< 600 ms** froid, **< 250 ms** chaud (cf. `03-budgets-performance.md`).
- Avant (ordre de grandeur, à valider) : un fork de cette taille avec splash Compose + Koin en eager + largeHeap démarre typiquement entre **700 et 1100 ms** sur milieu de gamme ; le travail de Baseline Profile d'AndroidX déjà présent (`baselineProfiles` vu côté Capacitor, à recréer côté fork) est le levier n°1.
- Procédure fournie au §4.

## 2. Actions proposées (ordre de priorité)

| # | Action (agent Build) | Gain estimé | Risque |
|---|---|---|---|
| 1 | Flavor `fdroid` seul ; supprimer permissions `INTERNET`/`ACCESS_NETWORK_STATE`, `largeHeap`, `requestLegacyExternalStorage` | **0 Mo APK**, ~50–150 ms au cold start, mémoire plus saine | faible |
| 2 | Locales : garder `values` (EN défaut), `values-fr`, `values-en` ; supprimer ~43 locales | **~2,3 Mo res** (~1,5–2 Mo APK) | faible (FR/EN seulement, README déjà bilingue) |
| 3 | Retirer réseau : ktor + okhttp + coil-network ; coil → cache local borné ; retirer lyrics en ligne, update intégré (`versioncompare`) | **~1,5–2 Mo APK** + moins de surfaces | moyen (garder les chemins de code désactivables par bool `network_features_enabled_by_default=false`) |
| 4 | Retirer widgets Glance + Android Auto + tag editor + Chromecast (code mort en v1) | **~3–4 Mo APK** | moyen (suppression mécanique ciblée, tests de build) |
| 5 | Retirer deps zéro-référence + un seul Markdown (compose-markdown) + une seule scrollbar/réordonnancement Compose | **~1–1,5 Mo APK** | faible |
| 6 | Polices : 7 fichiers GoogleSansFlex → garder Regular/Medium/Bold (ou subset variable) | **~400–500 Ko** | faible |
| 7 | Lint `UnusedResources` + `dependencyAnalysis` (`./gradlew :app:lintFdroidRelease`, et `buildHealth`) ; brancher `checkDependencies` si adopté | révèle les reliquats (layouts legacy, drawables orphelins) | faible |
| 8 | Per-ABI uniquement ; ne pas diffuser l'universal ; nommé `B-Music.apk` (arm64-v8a) | universal **21,9 → ~13 Mo** arm64 | faible |
| 9 | Baseline Profile (Macrobenchmark `StartupBench`) + profil de démarrage ; mesurer froid/chaud sur device | cold start **−150 à −300 ms** | faible |
| 10 | `largeHeap` retiré + caches bornés (cf. `03-budgets-performance.md`) | jank/GC réduits | faible |

**Gain total estimé : ~6–9 Mo APK (arm64-v8a ≈ 14,5 → ~6–9 Mo marge ; cible < 15 Mo largement tenue) et ~200–400 ms au démarrage à froid.**

## 3. Repo produit (hors APK, mesuré)

| Élément | Taille mesurée | Décision |
|---|---|---|
| `mobile/node_modules` (Expo) | **360 Mo** | hors périmètre — sortie du dépôt produit (prototype abandonné) |
| `tauri/node_modules` | **21 Mo** | idem |
| `app/android` (build Capacitor) | **52 Mo** | garder seulement si la maquette Capacitor est archivée comme référence design ; sinon retirer |
| `docs/lean/` | — | garder |
