# B Music v1.2 — Budgets de performance post-retrait-recherche

> Agent Lean B Music. Date : 14 novembre 2026.
> Portée : toute modification (Designer, Build, Lean) est refusée si elle dépasse un budget.
> Cible device de référence : milieu de gamme 2023 (6 Go RAM, écran 60 Hz), mesurée aussi sur 120 Hz.
> Base : budgets v1.0 (`docs/lean/03-budgets-performance.md`) révisés après retrait de la recherche YouTube.

---

## 0. Décision du Plan sur le moteur

La recherche YouTube (`search()` de `MediaExtractorEngine`, package `search/`, `QueryValidator`) est retirée. Le moteur ne conserve que `resolve(url)`. Conséquence attendue sur l'APK : suppression du code de recherche, des fixtures, des règles R8 spécifiques et des strings liées. La réduction est chiffrée par `apkanalyzer` après build release (voir §5).

---

## 1. Budget APK

| Indicateur | Budget | Méthode | Justification |
|---|---|---|---|
| APK arm64-v8a (release, R8 minifié) | **≤ 12 Mo** | `apkanalyzer apk size app/build/outputs/apk/release/app-release.apk` | Retrait de la recherche réduit le contenu ; cible plus ambitieuse que les 15 Mo v1.0 |
| APK universelle | **non produite** | build `fdroid` uniquement, splits ABI | Règle v1.0 maintenue |
| Taille des classes `search/` retirées | à chiffrer | `apkanalyzer` avant/après | preuve de réduction effective |

---

## 2. Budget images / frames

| Indicateur | Budget | Méthode |
|---|---|---|
| Frames perdues, défilement historique 1 000 lignes (miniatures locales) | **0 frame perdue** | `adb shell dumpsys gfxinfo <pkg> framestats` ; Macrobenchmark `FrameTimingMetric` |
| Janky frames (frame > 2× budget) | **< 1 %** | Macrobenchmark + `FrameTimingMetric` (50 runs) |
| Aucune frame > 16 ms (60 Hz) / > 8 ms (120 Hz) | transitions principales | `dumpsys gfxinfo framestats` + Profile GPU Rendering |
| Nouvelle animation | ≤ 300 ms, courbe standard Material | revue Lean + capture `gfxinfo` |

Interdit : blur, glassmorphism, ombres lourdes, dégradés criards, animations > 400 ms.
Autorisé : surfaces opaques, élévation tonale, springs Compose.

---

## 3. Budget mémoire

| Indicateur | Budget | Méthode |
|---|---|---|
| RSS lecture en cours, historique 1 000 lignes avec miniatures locales | **< 200 Mo** | Android Studio Profiler ; `adb shell dumpsys meminfo <pkg>` |
| Cache de miniatures (borné) | **≤ 16 Mo** | valeur en configuration, revue code |
| Cache Coil (si conservé pour pochettes) | `memoryCacheSize = 15 %` RAM, `diskCacheSize = 128 Mo` max | revue code `App.kt` |
| Fuite mémoire | 0 activité/service retenu après fermeture | LeakCanary (debug) ; `meminfo` avant/après 10 cycles |
| `android:largeHeap` | **interdit** | manifest en revue |

---

## 4. Budget démarrage

| Indicateur | Budget | Méthode |
|---|---|---|
| Cold start (flag OFF) | **< 600 ms** | Macrobenchmark `StartupTimingMetric.COLD`, 10+ runs, médiane |
| Cold start (flag ON) | **< 600 ms** | idem — le flag ne doit pas alourdir le démarrage |
| Warm start | **< 250 ms | idem (`WARM`) |

---

## 5. Budget réseau / énergie

| Indicateur | Budget | Méthode |
|---|---|---|
| Requêtes réseau à l'ouverture de l'onglet Historique | **0** | `adb shell dumpsys netstats` ; journal d'étapes |
| Requêtes réseau au démarrage (flag OFF) | **0** | `dumpsys netstats` |
| Consommation batterie, 30 min téléchargements | **≤ +5 %** vs baseline | `adb shell dumpsys batterystats` avant/après |
| Boucle d'animation arrière-plan | **interdite** | revue code + `InfiniteTransition` absente hors écran visible |

---

## 6. Budget délais fonctionnels

| Délai | Cible | Méthode |
|---|---|---|
| Lien collé → aperçu affiché (Wi-Fi) | **< 800 ms** | mesure manuelle chronométrée ×5, médiane ; journal d'étapes |
| Lien collé → aperçu (3 sites, ×5 chacun) | **< 800 ms** médiane par site | idem |
| Transition mini-player → plein écran | **< 300 ms** | mesure ×10, médiane |

---

## 7. Règle d'arrêt (opposable)

> Tout dépassement d'un seul budget est **refusé**, quelle que soit la qualité perçue de l'UI, tant que la régression n'est pas corrigée et re-mesurée.
> Dérogation : uniquement par décision écrite dans `docs/lean/` (impacts + justification + plan de retour), jamais en commentaire de PR.
> Preuve obligatoire : les budgets §1, §2, §3, §4 exigent des **mesures jointes** (rapport `apkanalyzer`, `gfxinfo`, `meminfo`, `batterystats`, Macrobenchmark), pas des affirmations.

---

## 8. Références

| Fichier | Rôle |
|---|---|
| `docs/lean/03-budgets-performance.md` | Budgets v1.0 (base de référence) |
| `docs/lean/02-audit-gaspillage.md` | Audit anti-gaspillage (levés de réduction APK) |
| `docs/lean/01-perimetre-mvp.md` | Périmètre MVP |
| `docs/design/11-onglets-lien-historique.md` | Spécifications Lien/Historique |
| `docs/design/12-revue-lien-historique.md` | Revue d'implémentation |
