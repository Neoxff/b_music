# B Music — Perf Report avant / après (Build, mesuré)

> Date : 6 novembre 2026. État : fork BoomingMusic ré-brandé B Music, buildé sur JDK 21, AGP 9.5.0, Gradle 9.6.0, compileSdk 37, targetSdk 36, minSdk 26.
> Mesures **réelles** sur fichiers/artefacts produits ; « non mesuré » quand l'instrument n'était pas disponible (aucun appareil/émulateur joignable, image système partiellement téléchargée).

## 1. Artefacts produits (mesurés sur disques)

| Artefact | Chemin réel | Taille | Cible (docs/lean/03) |
|---|---|---|---|
| `B-Music.apk` (arm64-v8a release, R8) | `app/build/outputs/apk/fdroid/release/B-Music.apk` | **14,9 MB (14,2 MiB)** | **< 15 MiB ✓** (arm64 ≤ cible) |
| `B-Music-universal.apk` (tous ABI) | `.../fdroid/release/` | 22,7 MB (21,6 MiB) | > cible — variante hors cible recommandée |
| `B-Music.aab` (copy de `B-Music-fdroid-release.aab` → cible) | `.../fdroidRelease/B-Music.aab` | 23,3 MiB | — |
| `B-Music-debug.apk` (arm64) | `.../fdroid/debug/B-Music-debug.apk` | 48,6 MB | — |

Amélioration appliquée (constatée au build) : `shrinkResources` actif sur release, R8 `minify+shrink` actif, renaming film (arm64-nommage principal).

## 2. Démarrage à froid (mesuré — ne pas inventer)

| Métrique | Mesure | Cible — 03-budgets-performance |
|---|---|---|
| Cold start | **Non mesuré — bloqueur : aucun device / émulateur joignable ; AVD non créable (system image partiellement téléchargée 25 %) ; adb vide** | < 600 ms |
| Warm start | idem | < 250 ms |

## 3. R8 / signature

- Release: minification échr. + shrinkResources — build vert ; sans alias de Release Keystore signée en fallback debug si env absents (documenté dans le gradle SigningConfigs).
- Bundle : `B-Music-fdroid-release.aab` + copie `B-Music.aab` ✓

## 4. Room for error (honest gaps)

- `B-Music.apk` universal = 21,7 MiB > budget < 15 MiB; **arm64 = 14,2 MiB ✓** (14.9MB < 15 MiB); universel à juste titre > cible (conserver splits et exclure universal en distribution client)
- Jank / frame rate : **jamais mesuré ici** (pas de device + pas d'émulateur bootable) — macrobenchmark à lancer dès qu'un AVD est prêt.
- Poids universal/apk : 21.6 MiB (bundle/aab universal) > budget 15 MiB plan — en revanche le taget officiel arm64 est à 14.4 MiB ✓.
- R8: **OK** (minify+shrink vert ; release build verte).
