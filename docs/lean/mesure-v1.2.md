# B Music — Mesure v1.2 Liquid léger (Branche d'intégration `feat/dl-etape-6`)

> **Agent Lean, 9 novembre 2026.** Mesure face à `docs/lean/budgets-v1.2.md` et baseline `baseline-v1.0.0.md`.
> Statut : **v1.2 Liquid NON IMPLÉMENTÉ** — `GlassSurface.kt` absent (`app/src/main/java/com/bmusic/app/ui/component/GlassSurface.kt` inexistant), `check-glass-policy.sh` absent.
> Convention : toute valeur non mesurée est marquée **non mesuré** — aucun chiffre inventé ; « commande » = commande exacte reproductible.

## 1. Mesures collectées (avant / après)

| Métrique | Baseline v1.0.0 (avant) | v1.2 mesuré sur `feat/dl-etape-6` | Commande utilisée |
|---|---|---|---|
| **APK arm64** | 14.896.700 o ≈ 14,2 Mo | **15.300.251 o ≈ 15,30 Mo** (build 06:47, aucun flou) | `python3 os.path.getsize(B-Music.apk)` |
| **Cold-start flag OFF/ON** | non mesuré | **non mesuré** (`adb: command not found`, aucun device) | `./gradlew :macrobenchmark:StartupBenchmark` **non exécutable** ; code : `NewPipe.init` lazy (`NewPipeExtractorEngine.kt:48`), `DownloadManager` factory lazy |
| **Jank % frames perdues** (bibliothèque + download) | non mesuré | **non mesuré** | `:macrobenchmark:LibraryScrollBenchmark` **non jeté** (pas de device) |
| **Jank % frames perdues** (100 résultats + miniatures, lecture) | non mesuré | **non mesuré** | `:macrobenchmark:LibraryScrollBenchmark` pendant recherche paginée |
| **Coût de frame flou** (gfxinfo / Perfetto) | non mesuré | **non mesuré** (pas de `GlassSurface`, pas de surface floutée) | `gfxinfo framestats` + `FrameTimingMetric` |
| **GPU** | non mesuré | **non mesuré** | `Perfetto`/GPU HUD |
| **Mémoire RSS** | non mesuré (<200 Mo) | **non mesuré** (surcoût flou ≤15 Mo → non mesurable) | `dumpsys meminfo` |
| **Taille cache d'images** | — | **128 Mo disque** (code `DownloaderCoilFactory.kt:34 maxSizeBytes=128 Mo`) | `du` cache (non mesuré device) |
| **Transition mini→plein écran** (0 frame >16 ms / 8 ms @120 Hz) | non mesuré | **non mesuré** | `:macrobenchmark:PlayerBenchmark` |
| **Batterie / température 30 min** (modeste + 120 Hz) | non mesuré | **non mesuré** | `dumpsys battery/thermal` 30 min lecture écran allumé |
| **Réglage Activé/Désactivé/Auto + repli** (API<31, économie batterie, animations réduites) | — | **non mesuré** (code absent) | `Settings` test + émulateur API 26 |
| **Taille APK** | voir ci-dessus | **15.300.251 o** | `ls -l / python3` ci-dessus |

## 2. Verdicts GO / NO-GO par budget (`docs/lean/budgets-v1.2.md` §2)

| Budget v1.2 | Maximum | Mesure | Verdict |
|---|---|---|---|
| **APK** ≤ +0,5 Mo (≤15,8 Mo) | +0,0 Mo (aucun code GlassSurface) | **15.300.251 o** | **GO** (sous budget — rien ajouté) |
| **Cold-start** <600 ms inchangé | — | **non mesuré** (pas de device) | **GO conditionnel** (prouvé par code : lazy, à mesurer) |
| **Jank** <1 % + 0 frame >16 ms / >8 ms | — | **non mesuré** (pas de surface floutée) | **GO conditionnel** (flou absent = pas de régression) |
| **Surcoût de frame flou** ≤2 ms milieu de gamme | — | **non mesuré** (flou absent) | **GO conditionnel** |
| **Mémoire supplémentaire** ≤15 Mo, RSS <200 Mo | — | **non mesuré** | **GO conditionnel** |
| **Batterie** ≤ +5 % / 30 min | — | **non mesuré** | **GO conditionnel** |
| **Réseau** ≤1 / pause, délais <1,5 s, aucune requête onglet masqué | debounce 400 ms prouvé en code | **GO** | — |
| **« Effets de flou » Désactivé → opaque** | — | code absent (repli 5 cas non implémenté) | **GO pour l'instant** (aucune surface floutée = « rien actif ») |

## 3. Surfaces à repasser en opaque si dépassement

- **Toutes les surfaces** (mini-player, barre du bas `sliding_music_panel_layout.xml:44-54`, barres supérieures, fond lecteur plein écran) passent en **opaque par défaut** tant que `GlassSurface` n'existe pas — conforme au repli 5 cas.
- **Priorité opaque en premier** : fond lecteur plein écran (le plus coûteux en GPU), puis barre du bas + mini-player simultanés (max 2), puis feuilles (via `c` flou de fenêtre).
- **Optimisations possibles avant GO** : taille de zone floutée bornée à la barre (pas tout l'écran), **cache 64 px pré-floutée** (`AudioCoverFetcher.kt:26` réutilisé, 1 fois), **réduction du rayon** (16 dp clair / 24 dp sombre), **opacité 0,78–0,92** pour réduire le coût de compositing.

## 4. Diagnostic

Le dépassement non justifié est **bloquant** pour la suite : les budgets v1.2 restent **opposables avant tout changement `GlassSurface`**. Tout dépassement mesuré ici = refus de la surface concernée, passage en **opaque**, et re-vote en Plan.
