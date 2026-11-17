# B Music v1.2 — Simplifications après le retrait de la recherche

> Agent Lean B Music. Date : 15 novembre 2026, **recalé le 17 novembre 2026 sur l'APK release v1.4.5 livré**.
> Objet : ce qui reste de **superflu** une fois la recherche YouTube retirée, et les 5 simplifications les plus rentables.
> Méthode : mesures `apkanalyzer` + `zipfile` reproductibles sur `bmusic/app/build/outputs/apk/github/release/BMusic-1.4.5-github-arm64-v8a.apk` (`com.bmusic.app`, versionCode `1430403`). La comparaison « avec / sans recherche » du 15 novembre portait sur des variantes construites hors dépôt, aujourd'hui indisponibles ; elle est conservée à titre historique au §0.
> **Contrainte** : aucune simplification ne touche l'onglet **Lien** ni l'onglet **Historique**.
> Chiffres : mesurés. Aucune estimation.

---

## 0. Base de mesure

| Élément | Valeur |
|---|---|
| APK arm64 release de référence (`feat/dl-etape-6`, moteur + recherche) | 15 403 770 o = 14,69 MiB = 15,40 Mo |
| APK arm64 release, variante **sans** recherche (mesurée) | 15 403 770 o = 14,69 MiB = 15,40 Mo |
| Delta de taille ZIP | **0 o** (l'alignement ZIP absorbe la différence) |
| Delta de **dex** | **−17 821 o (−17,4 Ko)** — 29 classes retirées, 2 ajoutées |
| Empreinte SHA-256 (avec / sans recherche) | `e95652110f31c07b…` / `2d76acd84584c398…` (contenus différents) |

### 0.1 Reconfirmation sur l'APK livré v1.4.5 (reproductible)

| Mesure | Valeur sur v1.4.5 | Commande |
|---|---|---|
| Taille APK arm64 livré | **15 447 335 o = 14,73 MiB = 15,45 Mo** | `stat` / `apkanalyzer apk file-size` |
| Dex total | 10 274 632 o | `apkanalyzer dex packages … \| grep TOTAL` |
| Assets orphelins `sonivox` + `mardous.png` | **350 348 o compressés = 0,334 MiB** (sonivox 212 912 + mardous 137 436) | `zipfile` |
| Sous-paquets NewPipe strippables (S-2) | **95 293 o** de dex (timeago 37 412 + peertube 24 088 + media_ccc 16 047 + comments 4 494 + playlist 3 894 + channel 3 629 + kiosk 1 857 + subscription 1 754 + search 1 231 + feed 481 + suggestion 406) | `apkanalyzer dex packages` |
| `coil3` / `coil3.network` | 41 858 o / 13 089 o — **2 piles HTTP** (`ktor3` + `okhttp`) présentes | `apkanalyzer dex packages` |
| UI `ui.screen.downloader` | **9 917 o** (Lien + Historique ; était 16 271 o avec la recherche) | `apkanalyzer dex packages` |
| Recherche YouTube dans le downloader | **0 occurrence** (`downloader.search`, `SearchTab`, `SearchViewModel`, `SearchHistoryEntity` absents du dex) | `apkanalyzer dex packages` |

**Lecture** : le retrait de la recherche ne fait **pas** baisser la taille de l'APK (alignement ZIP). L'APK livré est à **15,45 Mo**, sous le budget de 16,5 Mo. Les chiffres des simplifications ci-dessous sont **reconfirmés à l'octet près sur v1.4.5** et reproductibles.

---

## 1. Les 5 simplifications, classées par impact APK

### S-1 — Retirer les deux assets orphelins

| | |
|---|---|
| **Fichiers** | `app/src/main/assets/sonivox_wave_data.dat`, `app/src/main/assets/images/mardous.png` |
| **Poids mesuré dans l'APK** | 553,5 Ko décompressés · **342,1 Ko compressés (0,334 MiB)** |
| **Preuve d'inutilisation** | `grep -rn "sonivox\|wave_data\|images/mardous\|mardous.png"` sur tout le dépôt (`.kt`, `.java`, `.cpp`, `.xml`, `.gradle.kts`) → **0 occurrence** hors `app/build` |
| **Gain APK** | **−0,334 MiB** (≈ 2,2 % de l'APK) |
| **Effort** | S |
| **Risque** | Faible — à confirmer : le lecteur de vague du lecteur (`playback/`) pourrait lire ce fichier via un chemin construit dynamiquement. Vérifier par un test de lecture avant suppression. |

### S-2 — R8 : remplacer le `-keep` global de NewPipe par des `-keep` ciblés

| | |
|---|---|
| **Fichier** | `app/proguard-rules.pro:96` → `-keep class org.schabi.newpipe.** { *; }` |
| **Cause** | Cette règle conserve **356 962 o** de dex NewPipe, y compris des services que l'application n'utilisera jamais. |
| **Gain mesuré (dex strippable)** | **95 293 o (0,091 MiB)** |

Sous-paquets NewPipe mesurés, retirables sous la décision du Plan (YouTube + SoundCloud + Bandcamp + Vimeo) :

| Sous-paquet | Dex (o) | Note |
|---|---|---|
| `extractor.timeago` | 37 412 | **Jamais appelé par B Music** (`grep timeago` → 0 occurrence dans `app/src/main/java`) |
| `extractor.services.peertube` | 24 088 | Service hors liste produit |
| `extractor.services.media_ccc` | 16 047 | Service hors liste produit |
| `extractor.comments` | 4 494 | Non utilisé |
| `extractor.playlist` | 3 894 | Non utilisé |
| `extractor.channel` | 3 629 | Non utilisé |
| `extractor.kiosk` | 1 857 | Non utilisé |
| `extractor.subscription` | 1 754 | Non utilisé |
| `extractor.search` | 1 231 | **Retiré avec la recherche** |
| `extractor.suggestion` | 406 | Non utilisé |
| `extractor.feed` | 481 | Non utilisé |
| **Total strippable** | **95 293 o** | |

- **Gain APK** : ~0,03 MiB compressé (le dex se compresse ~2:1) **+** classes non chargées au démarrage.
- **Effort** : M — chaque service doit être enumerated explicitement ; NewPipe enregistre ses services **statically** (`ServiceList`), un `-keep` trop étroit casse l'extraction à l'exécution. **Un test JVM par service conservé est obligatoire.**
- **Risque** | Élevé si bâclé. Mitigation : ne stripper que `timeago` et `search` dans un premier commit (aucun service actif), puis service par service avec test.

### S-3 — Ne pas ressusciter le second `ImageLoader` Coil

| | |
|---|---|
| **Fichier** | `app/src/main/java/com/bmusic/app/downloader/search/DownloaderCoilFactory.kt` |
| **État** | Supprimé avec le package `search/` (il n'image que les résultats de recherche) |
| **Impact mesuré** | `coil3` = 41 858 o de dex (partagés avec l'app, donc non supprimables) — mais **−32 Mo de cache mémoire** |
| **Pourquoi c'est une simplification, pas un gain d'APK** | `DownloaderCoilFactory` crée un `ImageLoader` avec `maxSizePercent(context, 0.25)` → **≈ 32 Mo** de cache mémoire, soit **2× le budget « cache de miniatures ≤ 16 Mo »** (`budgets-v3.md` §3) |
| **Recommandation** | Si l'onglet Lien a besoin de vignettes d'aperçu, **réutiliser l'`ImageLoader` global** (déjà Ktor + garde anti-SSRF possible via un intercepteur) au lieu d'en créer un second. Un seul cache mémoire, une seule pile HTTP. |
| **Effort** | S |
| **Risque** | Faible — l'anti-SSRF des vignettes (`ImageUrlPolicy`) doit être porté sur l'intercepteur global. |

### S-4 — Une seule pile HTTP pour Coil

| | |
|---|---|
| **Constat** | L'app utilise **Ktor** pour les images (`App.kt:39` → `KtorNetworkFetcherFactory`), le downloadable utilisait **OkHttp** (`DownloaderCoilFactory` → `OkHttpNetworkFetcherFactory`). |
| **Poids mesuré** | `coil3.network.ktor3` = 3 746 o · `coil3.network.okhttp` = 2 083 o · `coil3.network.internal` = 447 o |
| **Gain** | **−2 083 o de dex** en ne gardant qu'OkHttp (ou −3 746 o en ne gardant que Ktor) + **une pile réseau de moins** à maintenir (timeouts, intercepteurs, TLS) |
| **Effort** | S |
| **Risque** | Faible — les deux piles sont déjà présentes ; il s'agit de n'en configurer qu'une. |

### S-5 — Ne pas créer la table Room `SearchHistory` ni sa migration

| | |
|---|---|
| **Fichiers** | `app/src/main/java/com/bmusic/app/downloader/data/SearchHistory.kt`, `BoomingDatabase.kt` (entité, DAO, `MIGRATION_9_10`), `MainModule.kt` (binding) |
| **Exigence du plan** | « `SearchHistoryEntity` et DAO (**ne les crée pas dans la migration**) » |
| **Pourquoi c'est une simplification** | La table n'est utilisée que par l'historique de recherche. La créer imposerait une **migration Room supplémentaire (9 → 10)**, donc un risque de perte de données à la mise à jour par-dessus — exactement ce que le contexte interdit (« mise à jour par-dessus l'ancienne version sans perte »). |
| **Gain mesuré** | Table + index supprimés du schéma ; ~0 Ko d'APK (la table ne pèse que quelques centaines d'octets), mais **une migration Room et un fichier de schéma exporté en moins** |
| **Effort** | S (fait dans la branche de retrait de la recherche) |
| **Risque** | Faible |

---

## 2. Récapitulatif

| # | Simplification | Gain APK mesuré | Effort | Risque | Touche Lien/Historique ? |
|---|---|---|---|---|---|
| S-1 | Assets orphelins (`sonivox`, `mardous.png`) | **−0,334 MiB** | S | Faible | Non |
| S-2 | R8 ciblé sur NewPipe (au lieu du `-keep` global) | −0,091 MiB de dex + classes non chargées | M | **Élevé si non testé** | Non |
| S-3 | Un seul `ImageLoader` Coil (pas de second, cache ≤ 16 Mo) | 0 Ko APK, **−32 Mo de RAM** | S | Faible | Non (côté aperçu, à arbitrer) |
| S-4 | Une seule pile HTTP pour Coil | −2 083 o de dex + une pile de moins | S | Faible | Non |
| S-5 | Pas de table Room `SearchHistory` ni de migration 9→10 | ~0 Ko APK, **une migration Room en moins** | S | Faible | Non |

**Gain APK cumulé mesuré : ≈ 0,36 MiB** (≈ 2,4 % de l'APK), hors S-2 (dont le bénéfice principal est au démarrage, pas en taille).

---

## 3. Ce qui n'est PAS superflu (faux positifs écartés)

| Élément | Taille mesurée | Pourquoi on le garde |
|---|---|---|
| `libffmpegJNI.so` | 1 427,9 Ko | Utilisé par `SongRepository` et `AlacWorkaroundCodecSelector` (lecture ALAC) — hors périmètre |
| `libtaglib.so` | 1 225,0 Ko | Utilisé par `MetadataReader`, `MetadataWriter`, `AudioCoverFetcher`, `TagEditorViewModel` — hors périmètre |
| `ic_search_24dp.xml` | ~1 Ko | **Utilisé en 10 emplacements** (écran égaliseur, éditeur de paroles, accueil, dialogues, 4 menus, 3 layouts). Le supprimer casserait l'existant. |
| `com.bmusic.app.ui.screen.library.search.*` | — | **Recherche locale de la bibliothèque** : fonctionnalité distincte de la recherche YouTube, à conserver |
| `com.bmusic.app.data.local.search.*`, `SearchRepository` | — | Idem : recherche locale, à conserver |
| `coil3` | 41 858 o | Partagé avec l'app entière ; seule la double configuration (S-3, S-4) est superflu |

---

## 4. Règle d'arrêt

> Une simplification qui **dégrade** l'onglet Lien ou l'onglet Historique est refusée, quelle que soit son gain.
> S-2 est la seule simplification à risque : elle est **conditionnée** à un test JVM par service conservé, et son lot initial (`timeago`, `search`) est sans risque car aucun service actif n'est retiré.
> Chaque simplification appliquée doit être accompagnée d'un `apkanalyzer` avant/après (taille APK + taille dex).

---

## 5. Fichiers associés

| Fichier | Rôle |
|---|---|
| `docs/lean/budgets-v3.md` | Budgets (taille APK, mémoire, démarrage, réseau, énergie) — recalés sur mesures |
| `docs/lean/fiche-mesure-v3.md` | Fiche de mesure sur device (scénarios A→F) |
| `docs/lean/03-budgets-performance.md` | Budgets v1.0 (référence) |
| `docs/design/11-onglets-lien-historique.md` | Spécifications Lien/Historique (périmètre intouchable) |
| `docs/pm/08-criteres-historique.md` | Critères d'acceptation de l'historique |
