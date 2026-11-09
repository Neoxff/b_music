# Recette B Music v1.1 — Téléchargeur complet

> **Agent PM, branche d'intégration `feat/dl-etape-6`, 9 novembre 2026.**
> Recette **entière** exécutée par inspection de code + mesures disponibles, sans modification de code.
> `check-no-glass` 0, APK 15,30 Mo ≤20 Mo (Lean v1.1). **Critère de sortie : verdict GO/NO-GO** ci-dessous.
> Appareils : analyse code + rétake (aucun capture/mesure live sans device — `adb devices` vide confirmé le 8-9/11).

## A — Régression v1.0 (S1–S12) flag OFF puis ON — comparés aux captures `docs/baseline/captures/`
| Scénario | Attendu | Preuve code | Verdict |
|---|---|---|---|
| S1–S12 navigation, playback, library, queue, settings, scan | Identique à v1.0.0 ; flag OFF invisible ; flag ON seul ajout `nav_download` | `MainActivity.kt:161` `isVisible = Preferences.downloaderEnabled` ; `graph_main.xml` `nav_download` unique ; `SpecialRepository.kt:179` filtre `null` si off ; audits `playback/`, `ui/theme/`, `Motion`, Room 1→8 intacts | ✅ code, ⚠️ captures non produites (device absent → comparer sans pixel preuve) |
| Captures avant/après | `docs/baseline/captures/*.png` (14 fichiers) | `docs/baseline/captures/README.md` : **non générées 8/11 (aucun device)** ; diff `git diff --stat` sur existants = 0 ligne hors downloader/menu | ⚠️ **manquant** (F1) |

## B — Onglet RECHERCHE
| # | Critère | Preuve | Verdict |
|---|---|---|---|
| B1 | Requête → résultats <1,5 s, miniatures/titres/chaînes/durées | `SearchViewModel` latency mesurée en code <400 ms debounce + `items[0]` (pas de device pour timing réel) ; `SearchTab.kt` vignette 112 dp 16:9 rayon 12 dp, titre 2 lignes, chaîne, badge durée | ✅ code, ⚠️ timing non chronométré |
| B2 | Défilement infini 5+ pages sans saccade/doublon | `SearchViewModel` pagination seuil 5 éléments, `distinctBy { it.id }`, `LazyColumn` clés stables | ✅ code |
| B3 | Toucher → aperçu → Audio/qualité → télécharger → jouable | `SearchTab.kt:PreviewSheet` `resolve(url)` + bouton Télécharger → `DownloadWorker.enqueue` | ✅ code, ⚠️ parcours live non rejoué ici |
| B4 | Requête 1 char / vide / très longue / spéciaux | `QueryValidator` 2–200, contrôles/nuls refusés ; `SearchViewModel.UiState.TooShort/NoResult` | ✅ code |
| B5 | Aucun résultat, avion, instable | `UiState.NoResult/Offline/SourceGone/Error` + bouton Réessayer (`SearchTab` skeleton 6 lignes) | ✅ code |
| B6 | Historique : ajout, relance, suppression, tout effacer | `SearchHistoryDao` max 10 (`SearchHistory.kt:24 DELETE LIMIT 10`), UI ⌚ + croix, tout effacer | ✅ code |
| B7 | Recherche pendant 2 h lecture — coupure audio/animations | `SearchViewModel.setVisible(false)` annule jobs, 1 download simultané `Dispatchers.IO`, pas de partage `PlaybackService` | ✅ code, ⚠️ 2 h non rejouées |

## C — Onglet LIEN
| # | Critère | Preuve | Verdict |
|---|---|---|---|
| C1 | Coller valide (SC/Bandcamp/Vimeo/YouTube) → aperçu → télécharger → lire | `DownloadScreen.LienTab` → `engine.resolve` → `MediaPreviewCard` → `enqueue` | ✅ code |
| C2 | Invalide / privé / non pris en charge (IG/TikTok/X) | `UrlValidator` + `ExtractorError.UrlInvalid/Unsupported/ProtectedContent` messages FR | ✅ code |
| C3 | Coller/effacer, doublon averti | `Clipboard.getText()` sur appui (jamais arrière-plan) ; `DownloadManager:53` retourne `existing` — ⚠️ **silencieux sans toast** (E1 recette étape 4) | ⚠️ **Bug E1** (majeur) |

## D — Transverse
| # | Critère | Preuve | Verdict |
|---|---|---|---|
| D1 | Partage URL→Lien, texte→Recherche, jamais d'action auto | `MainActivity.handlePlaybackIntent()` `ACTION_SEND` — URL `http` → `download_prefill_url`, texte → `download_prefill_query` (sans `searchNow`) | ✅ code |
| D2 | Wi-Fi seulement → mobile → attente/reprise | `DownloadWorker` `Constraints.NetworkType.UNMETERED`, reprise `Range` + `.part`; **pas de Wi-Fi seulement activé ici ?** (paramètre `wifiOnly=false` par défaut dans `enqueue`) | ⚠️ **partiel** (D2 non branché en bouton — E2) |
| D3 | App tuée → aucun partiel, reprise | `IS_PENDING=1→0` (`MediaStoreDownloadWriter`), cleanup orphelins au démarrage worker | ✅ code |
| D4 | Vidéo → Movies/B Music, audio seul | `MediaStoreDownloadWriter.kt` `RELATIVE_PATH Movies/B Music` (vidéo) ; lecture audio-only v1.1 assume `PlaybackService` inchangé — ⚠️ non prouvé vidéo sur device | ✅ code, ⚠️ à confirmer |
| D5 | Changement d'onglet appui/balayage, rotation, clair/sombre, TalkBack, animations réductibles | `TabRow`+`HorizontalPager`, `rememberReducedMotion()` présent, `contentDescription` onglets, `isVisible=downloaderEnabled` gate ; paysage/tablette : **aucun adaptation** (1 colonne) | ⚠️ paysage/tablette non porté (E3) |
| D6 | MAJ par-dessus v1.0.0 (bibliothèque, playlists, file) + flag défaut | `BMusicDatabase.VERSION 9→10`, `MIGRATION_8_9/9_10` additive (`BoomingDatabase.kt`), `Preferences.DOWNLOADER_ENABLED=false` par défaut | ✅ code, ⚠️ test MAJ non rejoué |

## E — Preuves
| Item | Preuve | Mesure |
|---|---|---|
| `check-no-glass` | `scripts/check-no-glass.sh` | **0** |
| APK arm64 | `B-Music.apk` | **15,30 Mo ≤20 Mo** ✅ |
| Unit tests | `:app:testFdroidDebugUnitTest` (downloader) | **vert** (88 `SecurityModuleTest`) |
| Macrobenchmark/scroll 100 résultats + RSS | Non lancée (pas de device) | **manquant** |
| Captures/vidéos | `docs/baseline/captures/` | **non générées** (README) |
| Crash-free | Non mesurable localement | — |

## F — Verdict final : **NO-GO conditionnel**
**2 bloqueurs + 3 moyens** (zéro critique de sécurité ouverte en code, mais 2 écarts produit bloquants) :

| ID | Gravité | Reproduction (appareil) | Recommandation |
|---|---|---|---|
| **E1** | 🟠 bloquant | C3/D3 : coller un lien déjà téléchargé → aucune avertissement, réutilisation silencieuse (`DownloadManager.kt:53`) — tous appareils | Ajouter toast/dialogue doublon + Forcer (BUILD, étape 5-bis) |
| **E2** | 🟡 moyen | D2 : Wi-Fi seulement non branché en UI (param `wifiOnly=false` fixe) | Brancher l'option au corps d'URL/champ téléchargeur (PLAN/BUILD) |
| **E3** | 🟡 moyen | D5 : paysage/tablette (grid 2 colonnes) non porté (spec §7) | Adaptation responsive (DESIGN/BUILD) |
| **F1** | 🔴 fix secondaire | Captures baseline `docs/baseline/captures/*.png` manquantes | Produire avant merge (BUILD/PM) |
| **F2** | 🔴 fix secondaire | Macrobenchmarks (jank/RSS/cold-start) non lancés | Rejouer `StartupBenchmark`/`LibraryScrollBenchmark` (BUILD/LEAN) |

*Non bloquant code : aucune faille critique/haute de sécurité ouverte à l'audit `docs/security/audit-v1.1.md` (verdict sécurité NO-GO conditionnel sur 2 findings documentaires F1/F2 — déférents `SecurityManager` : `allowBackup` + preuve `adb SEND` à relancer en parallèle, pas de correction ici).*
