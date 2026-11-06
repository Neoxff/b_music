# B Music v1.0 — budgets de performance (mesurables, opposables)

> Agent Lean B Music. Date : 6 novembre 2026.
> Portée : toute modification (Designer, Build, Lean) est refusée si elle dépasse un budget.
> Cible device de référence : milieu de gamme 2023 (6 Go RAM, écran 60 Hz), mesurée aussi sur 120 Hz.

## 1. Budget images / frames

| Indicateur | Budget | Méthode |
|---|---|---|
| Frames perdues, défilement bibliothèque (5 000 titres, liste + pochettes) | **0 frame perdue** | `adb shell dumpsys gfxinfo <pkg> framestats` ; Macrobenchmark `FrameTimingMetric` |
| « Janky frames » (frame > 2× budget) | **< 1 %** des frames | Macrobenchmark + `FrameTimingMetric` (50 runs) |
| Aucune frame > 16 ms (60 Hz) / > 8 ms (120 Hz) | transitions principales : Now Playing ↔ liste, ouverture/fermeture lecteur, seek, mini-player | `dumpsys gfxinfo framestats` + Profile GPU Rendering |
| Nouvelle animation | doit être ≤ 300 ms, courbe standard Material (`EmphasizedDecelerate` / springs Compose), 60/120 fps tenus | revue Lean + capture `gfxinfo` |

Interdit : blur, glassmorphism, ombres lourdes, dégradés criards, animations > 400 ms.
Autorisé : surfaces opaques, élévation tonale, springs Compose (`animate*AsState`, `AnimatedVisibility`).

## 2. Budget mémoire

| Indicateur | Budget | Méthode |
|---|---|---|
| RSS lecture en cours, bibliothèque 5 000 titres | **< 200 Mo** | Android Studio Profiler (Memory) ; `adb shell dumpsys meminfo <pkg>` |
| Pochettes en cache (Coil) | **borné** : `memoryCacheSize = 15 %` RAM disponible, `diskCacheSize = 128 Mo` max — pas de cache non borné | revue code + config Coil dans `App.kt` |
| Fuite mémoire (Now Playing / service) | 0 activité/service retenu après fermeture | LeakCanary (build debug uniquement) ; `meminfo` avant/après 10 cycles lecture |
| `android:largeHeap` | **interdit** (retiré, cf. audit #1) | manifest en revue |

## 3. Budget batterie / énergie

| Règle | Exigence |
|---|---|
| Aucune boucle d'animation active écran éteint / app en arrière-plan | animations Compose pilotées par le cycle de vie ; `AndroidView` pausé ; aucune `InfiniteTransition` persistante hors écran visible |
| Service média propre | `MediaSessionService` + `mediaPlayback` FGS uniquement pendant la lecture ; arrêt complet (`stopSelf`) à la fin de la file ou `pause` > 10 min ? → **non** : FGS maintenu tant que la session est active, arrêté à l'arrêt définitif ; notification retirée sinon |
| Aucun réveil inutile | pas d'`AlarmManager` pour la lecture ; pas de polling réseau (v1 offline) ; wake lock uniquement pendant la lecture audio |
| Consommation mesurée | `adb shell dumpsys batterystats` avant/après 1 h de lecture : pas de wakelock partiel inexpliqué |

## 4. Budget démarrage

| Indicateur | Budget | Méthode |
|---|---|---|
| Froid (cold start, après `am force-stop`) | **< 600 ms** jusqu'à premier écran interactif | Macrobenchmark `StartupBench` (`StartupTimingMetric.COLD`), 10+ runs, médiane |
| Chaud (warm) | **< 250 ms** | idem (`WARM`) |
| Mesures obligatoires avant merge | tableau avant/après dans la PR | template PR (§6) |

## 5. Comment mesurer (outillage)

1. **Macrobenchmark** (`:benchmark` module) : `StartupBench`, `LibraryScrollBench`, `NowPlayingTransitionBench` → `StartupTimingMetric`, `FrameTimingMetric`.
2. **Baseline Profiles** : générés par les tests Macrobenchmark (startup + hot paths) ; inclusion dans le build release ; gains attendus −20 à −30 % sur cold start.
3. **Android Studio Profiler** : CPU (jank), Memory (fuites/RSS), Energy.
4. **`adb shell dumpsys gfxinfo <pkg> reset` puis interaction puis `dumpsys gfxinfo <pkg> framestats`** : %janky, p90/p99 frame times.
5. **`adb shell dumpsys meminfo`**, **`dumpsys batterystats`**, **`am start -W`** (mesure rapide, non contractuelle).

## 6. Règle « stop » (opposable)

> Toute PR qui dépasse **un seul** budget (APK ≥ 15 Mo arm64-v8a, démarrage ≥ 600 ms froid, RSS ≥ 200 Mo, janky > 1 %, 0 frame > 16/8 ms, frame perdue en bibliothèque, boucle d'animation en arrière-plan) est **refusée**, quelle que soit la qualité perçue de l'UI, tant que la régression n'est pas corrigée et re-mesurée.
> Dérogation : uniquement par décision écrite dans `docs/lean/` (impacts + justification + plan de retour), jamais en commentaire de PR.

## 7. Checklist de revue Build (12 points — à valider AVANT chaque livraison)

1. APK `B-Music.apk` (arm64-v8a) **< 15 Mo** ; universal **non produit** en release.
2. Cold start mesuré **< 600 ms**, warm **< 250 ms** (tableau Macrobenchmark joint à la PR).
3. `dumpsys gfxinfo` : **< 1 % janky**, aucune frame > 16 ms (ou 8 ms @120 Hz) sur les 3 parcours critiques.
4. Défilement bibliothèque 5 000 titres : **0 frame perdue** (mesure jointe).
5. RSS lecture **< 200 Mo** ; caches Coil bornés (valeurs en config) ; pas de `largeHeap`.
6. **Lint** `UnusedResources` propre ; `dependencyAnalysis` sans dépendance à zéro référence ajoutée.
7. **Aucune permission réseau** (`INTERNET`/`ACCESS_NETWORK_STATE`) ni `largeHeap` / `requestLegacyExternalStorage` dans le manifest v1.
8. **Zéro tracking / zéro SDK propriétaire** ; `dependenciesInfo` off ; pas de nouveau dépôt Maven hors google()/mavenCentral()/jitpack (jitpack seulement pour libs déjà auditées).
9. **FR + EN** externalisés (`values/`, `values-fr/`, `values-en/`) ; aucune chaîne en dur ; FR par défaut.
10. **Interdits design respectés** : aucun blur/glassmorphism/dégradé criard/ombre lourde ; surfaces opaques uniquement.
11. **60/120 fps** tenus sur device 60 Hz **et** 120 Hz ; Baseline Profile régénéré si le graphe de démarrage change.
12. **Licences** : écran About + `LICENSE.txt` à jour (GPL-3.0, RetroMusicPlayer, BoomingMusic) ; `B-Music.apk` nommé exactement ; versionCode cohérent.

Échec d'un seul point = livraison refusée. Les points 2, 3, 4 exigent des **mesures jointes**, pas des affirmations.
