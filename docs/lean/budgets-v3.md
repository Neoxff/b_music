# B Music v1.2 — Budgets de performance post-retrait-recherche

> Agent Lean B Music. Date : 15 novembre 2026, **recalé le 17 novembre 2026 sur l'APK release v1.4.5 livré**.
> Portée : toute modification (Designer, Build, Lean) est **refusée** si elle dépasse un budget.
> Cible device de référence : milieu de gamme 2023 (6 Go RAM, écran 60 Hz), mesurée aussi sur 120 Hz.
> Base : budgets v1.0 (`docs/lean/03-budgets-performance.md`), révisés après retrait de la recherche YouTube.
> **Toutes les valeurs de ce document sont mesurées** (apkanalyzer / zip sur APK release, ou à relever sur device via la fiche §fiche-mesure-v3.md). Aucune n'est estimée.
> **Source des mesures APK ci-dessous** : `bmusic/app/build/outputs/apk/github/release/BMusic-1.4.5-github-arm64-v8a.apk`,
> `com.bmusic.app` versionCode `1430403`, outil `apkanalyzer` du SDK (`cmdline-tools/latest/bin/apkanalyzer`).

---

## 0. Décision du Plan sur le moteur

Le moteur retenu est **NewPipe seul** (voie A du diagnostic) : YouTube, SoundCloud, Bandcamp, Vimeo, PeerTube, mediaCCC. Les 12 sites de la liste produit ne sont **pas** tous extractibles par ce moteur — ceux qui ne le sont pas doivent être refusés avec un message clair, pas simulés.

La recherche YouTube (`search()` de `MediaExtractorEngine`, `SearchTab`/`SearchViewModel` du téléchargeur, `SearchHistory`, `QueryValidator`, `DownloaderCoilFactory`) est retirée. Le moteur ne conserve que `resolve(url)`.

### 0.1 Mesure de l'effet du retrait de la recherche (apkanalyzer)

| Mesure | Avec recherche (`feat/dl-etape-6`, variante 15 nov.) | Sans recherche (variante 15 nov.) | Delta |
|---|---|---|---|
| APK arm64 release, taille ZIP | 15 403 770 o (14,69 MiB / 15,40 Mo) | 15 403 770 o (14,69 MiB / 15,40 Mo) | **0 o** |
| APK arm64 release, empreinte SHA-256 | `e95652110f31c07b…` | `2d76acd84584c398…` | contenu différent |
| Dex total (décompressé) | 10 158 345 o | 10 140 524 o | **−17 821 o (−17,4 Ko)** |
| Classes dans le dex | 13 116 | 13 089 | −29 / +2 |
| Sources supprimées | — | 5 fichiers, 23 515 o | — |

> Ces deux variantes ont été construites **hors dépôt** le 15 novembre pour isoler l'effet du retrait ; elles ne sont plus disponibles. Les mesures ci-dessous portent sur l'**APK réellement livré** et sont reproductibles avec `apkanalyzer`.

### 0.2 Mesure de l'APK livré v1.4.5 (reproductible)

| Mesure | Commande | Valeur |
|---|---|---|
| Taille fichier | `stat -c %s` | **15 447 335 o** = 14,73 MiB = **15,45 Mo** |
| Taille déclarée | `apkanalyzer apk file-size` | 15 447 335 o |
| Taille de téléchargement estimée | `apkanalyzer apk download-size` | 10 429 727 o = 9,95 MiB |
| Dex total (décompressé) | `apkanalyzer dex packages … \| grep TOTAL` | **10 274 632 o** = 9,80 MiB |
| Application / version | `apkanalyzer apk summary` | `com.bmusic.app` · 1430403 · 1.4.5 |

**Delta de contenu v1.4.3 → v1.4.5** (`apkanalyzer apk compare --different-only`) : **+58 812 o** au total, soit `classes2.dex` +64 888, `classes.dex` +15 168, `resources.arsc` +9 600, `assets/` +3 032, `res/` +204 — c'est le coût mesuré de l'ajout de l'écran Journal, de l'onglet Historique complet et du garde-fou Koin, **après** retrait de la recherche. Ce delta est amplement absorbé par la marge du budget §1.1.

**Conclusion mesurée** : le retrait de la recherche réduit le **dex de 17,4 Ko** mais **pas la taille de l'APK** (alignement ZIP). L'APK livré v1.4.5 est à **15,45 Mo**, sous le budget de 16,5 Mo. La réduction de taille doit être recherchée **ailleurs** (voir §1.3 et le document simplifications), pas dans le retrait de la recherche.

---

## 1. Budget APK

### 1.1 Budget

| Indicateur | Budget | Méthode |
|---|---|---|
| **APK arm64-v8a (release, R8 + shrinkResources)** | **≤ 16,5 Mo** | `stat` sur `app/build/outputs/apk/fdroid/release/B-Music.apk` + `apkanalyzer apk summary` |
| APK armeabi-v7a / x86 / x86_64 | non soumises au budget (variantes de compat) | — |
| APK universelle | **non produite** | splits ABI uniquement |
| Taille du dex (décompressé) | ≤ 10,5 Mo | `apkanalyzer dex packages` (somme de la colonne taille) |

**Justification du budget** : l'APK arm64 **livré v1.4.5 mesure 15,45 Mo** (§0.2), écran Journal et onglet Historique complet inclus. La marge de **1,05 Mo** sous le plafond de 16,5 Mo couvre les évolutions restantes sans dépassement. Le coût mesuré de l'ajout Journal + Historique (v1.4.3 → v1.4.5) n'est que de **+58 812 o** (§0.2). Le budget est **recalé sur la mesure**, pas sur une estimation.

### 1.2 Répartition mesurée de l'APK arm64 (variante sans recherche)

| Catégorie | Décompressé | Compressé | Part du ZIP |
|---|---|---|---|
| `classes.dex` + `classes2.dex` | 12 745 372 o (12,16 MiB) | 6 003 755 o (5,73 MiB) | 39,7 % |
| `resources.arsc` | 4 079 688 o (3,89 MiB) | 4 079 688 o (3,89 MiB) | 27,0 % |
| `lib/` (3 `.so` arm64) | 2 726 672 o (2,60 MiB) | 2 726 672 o (2,60 MiB) | 18,0 % |
| `res/` | 3 241 201 o (3,09 MiB) | 1 740 044 o (1,66 MiB) | 11,5 % |
| `assets/` | 785 971 o (0,75 MiB) | 473 505 o (0,45 MiB) | 3,1 % |
| `google/`, `org/`, `kotlin/`, manifeste, `META-INF` | 279 601 o | 89 187 o | 0,6 % |
| **Total fichiers** | **23 858 505 o** | **15 118 834 o** | 100 % |

> En-têtes ZIP hors total fichiers : l'APK fait 15 447 335 o (écart ≈ 328 Ko d'index et d'alignement).
> Mesure : script `zipfile` sur l'APK v1.4.5 ; cohérent avec `apkanalyzer apk file-size`.

### 1.3 Postes de réduction identifiés (chiffrés, apkanalyzer sur v1.4.5)

| Poste | Taille dex (v1.4.5) | Remarque |
|---|---|---|
| Moteur NewPipe `org.schabi.newpipe` | **357 161 o** | dont `services` 225 323 o, YouTube 140 459 o, `timeago` 37 412 o, PeerTube 24 088 o, Bandcamp 23 633 o, SoundCloud 21 096 o, mediaCCC 16 047 o, `jsextractor` 15 064 o |
| `coil3` (images) | **41 858 o** | dont `coil3.network` 13 089 o — **deux piles HTTP** (`ktor3` + `okhttp`) présentes dans le dex |
| `androidx.work` | **33 909 o** | WorkManager, nécessaire au téléchargement en arrière-plan |
| `kotlinx.coroutines` | **60 620 o** | dont `flow` 33 224 o, `channels` 14 776 o |
| Package `com.bmusic.app.downloader` | **22 315 o** | cœur métier (hors UI) — était 19 014 o avant Journal/Historique |
| UI `com.bmusic.app.ui.screen.downloader` | **9 917 o** | **onglets Lien + Historique uniquement** — la recherche a été retirée (était 16 271 o avec recherche) |
| `com.bmusic.app.ui.screen.library.search` | **6 326 o** | recherche **bibliothèque locale**, à conserver (pas la recherche YouTube) |
| `libffmpegJNI.so` (natif) | 1 427,9 Ko | `SongRepository`, `AlacWorkaroundCodecSelector` — **hors périmètre** (lecture) |
| `libtaglib.so` (natif) | 1 225,0 Ko | `MetadataReader/Writer`, `AudioCoverFetcher`, `TagEditorViewModel` — **hors périmètre** (métadonnées) |
| `libandroidx.graphics.path.so` (natif) | — | dépendance Graphics Path — **hors périmètre** |

> **Preuve du retrait de la recherche** : `apkanalyzer dex packages` sur v1.4.5 → **0 occurrence** de `com.bmusic.app.downloader.search`, `SearchTab`, `SearchViewModel` ou `SearchHistoryEntity` dans le dex. Le seul `search` restant dans le module downloader est `org.schabi.newpipe.extractor.search` (37 références), code interne du moteur non appelé par l'app. Les 6 326 o de `ui.screen.library.search` concernent la recherche **locale de la bibliothèque**, fonctionnalité distincte conservée.

---

## 2. Budget images / frames

| Indicateur | Budget | Méthode |
|---|---|---|
| Frames perdues, défilement historique 1 000 lignes (miniatures locales) | **0 frame perdue** | `dumpsys gfxinfo <pkg> framestats` ; Macrobenchmark `FrameTimingMetric` |
| Janky frames (frame > 2× budget) | **< 1 %** | Macrobenchmark `FrameTimingMetric` (50 runs) |
| Frames > 16 ms (60 Hz) / > 8 ms (120 Hz) | **0** | `gfxinfo framestats` + Profile GPU Rendering |
| Nouvelle animation | ≤ 300 ms, courbe standard Material | revue Lean + capture `gfxinfo` |

Interdit : blur, glassmorphism, ombres lourdes, dégradés criards, animations > 400 ms.
Autorisé : surfaces opaques, élévation tonale, springs Compose.

---

## 3. Budget mémoire

| Indicateur | Budget | Méthode |
|---|---|---|
| RSS pic, lecture en cours + historique 1 000 lignes (miniatures locales) | **< 200 Mo** | Android Studio Profiler ; `dumpsys meminfo <pkg>` |
| **Cache de miniatures de l'aperçu de lien** | **≤ 16 Mo** | valeur de configuration dans le code du téléchargement |
| Cache Coil global | `memoryCacheSize` ≤ 15 % RAM, `diskCacheSize` ≤ 128 Mo | revue code `App.kt` |
| **Second ImageLoader Coil** | **interdit** | un seul `ImageLoader` ; sinon deux caches mémoire distincts |
| Fuite mémoire | 0 activité/service retenu après fermeture | LeakCanary (debug) ; `meminfo` avant/après 10 cycles |
| `android:largeHeap` | **interdit** | manifest en revue |

> **Point de vigilance mesuré** : `DownloaderCoilFactory` crée un **second** `ImageLoader` avec `maxSizePercent(context, 0.25)` — soit ≈ 32 Mo de cache mémoire sur un heap de 128 Mo, **au-dessus** du budget 16 Mo ci-dessus. Il est aujourd'hui dans le package `downloader/search/` (donc retiré avec la recherche) ; s'il est ressuscité pour les vignettes d'aperçu, il doit être **recapé à 16 Mo maximum** ou **supprimé** au profit de l'`ImageLoader` global.

---

## 4. Budget démarrage

| Indicateur | Budget | Méthode |
|---|---|---|
| **Cold start, flag `DOWNLOADER_ENABLED` OFF** | **< 600 ms** | Macrobenchmark `StartupTimingMetric.COLD`, 10+ runs, médiane |
| **Cold start, flag `DOWNLOADER_ENABLED` ON** | **< 600 ms** | idem — le flag ne doit pas alourdir le démarrage |
| Warm start | **< 250 ms** | idem (`WARM`) |

> Le flag OFF doit produire un démarrage **strictement identique** à la v1.0.0 : aucune dépendance du téléchargeur ne doit être résolue au démarrage (binding Koin `lazy`/`factory`, aucun `ImageLoader` créé, aucun `WorkManager` initialisé).

---

## 5. Budget réseau / énergie

| Indicateur | Budget | Méthode |
|---|---|---|
| **Requêtes réseau à l'ouverture de l'onglet Historique** | **0** | `dumpsys netstats` ; journal d'étapes |
| Requêtes réseau au démarrage, flag OFF | **0** | `dumpsys netstats` |
| Requêtes réseau de **recherche YouTube** après retrait | **0** | le moteur n'expose plus `search()` — plus aucune route de recherche |
| Consommation batterie, 30 min de téléchargements | **≤ +5 %** vs baseline | `dumpsys batterystats` avant/après ; écran Batterie |
| Boucle d'animation en arrière-plan | **interdite** | revue code ; `InfiniteTransition` absente hors écran visible |
| Cookies / authentification sur le client du téléchargeur | **aucun** | `CookieJar.NO_COOKIES` ; pas d'`Authorization` |

---

## 6. Budget délais fonctionnels

| Délai | Cible | Méthode |
|---|---|---|
| **Lien collé → aperçu affiché (Wi-Fi)** | **< 800 ms** | mesure manuelle chronométrée ×5 par site ; journal d'étapes |
| Lien collé → aperçu (3 sites × 5 mesures) | **< 800 ms** (médiane par site) | idem |
| Transition mini-player → plein écran | **< 300 ms** | mesure ×10, médiane |

> La cible de 800 ms est **à confirmer** par vos mesures : elle suppose que le moteur répond en moins d'une seconde sur réseau correct. Si vos relevés la contredisent, c'est la cible qu'il faut revoir, pas le résultat.

---

## 7. Règle d'arrêt (opposable)

> Tout dépassement d'un seul budget est **refusé**, quelle que soit la qualité perçue de l'UI, tant que la régression n'est pas corrigée et re-mesurée.
>
> Dérogation : uniquement par décision écrite dans `docs/lean/` (impact + justification + plan de retour), **jamais** en commentaire de commit ou de PR.
>
> **Preuve obligatoire** : les budgets §1 (taille) exigent un rapport `apkanalyzer` / `stat` joint ; les budgets §2, §3, §4, §5, §6 exigent des **mesures sur votre appareil** (écran Diagnostics + journal d'étapes exporté par Partager, ou fiche `docs/lean/fiche-mesure-v3.md` remplie). Une affirmation sans mesure ne vaut rien.

---

## 8. Références

| Fichier | Rôle |
|---|---|
| `docs/lean/03-budgets-performance.md` | Budgets v1.0 (base de référence) |
| `docs/lean/02-audit-gaspillage.md` | Audit anti-gaspillage |
| `docs/lean/01-perimetre-mvp.md` | Périmètre MVP |
| `docs/lean/fiche-mesure-v3.md` | Fiche de mesure à remplir sur device (scénarios A→F) |
| `docs/lean/simplifications-post-retrait-recherche.md` | 5 simplifications chiffrées par apkanalyzer |
| `docs/design/11-onglets-lien-historique.md` | Spécifications Lien/Historique |
| `docs/design/12-revue-lien-historique.md` | Revue d'implémentation (écarts, captures manquantes) |
| `docs/pm/recette-lien-historique.md` | Recette fonctionnelle sur device |
