# Recette Étape 4 — Intégration bibliothèque (téléchargeur v1.1)

> **Agent PM, branche `feat/dl-etape-4`, 9 novembre 2026.** Recette exécutable, preuves jointes quand mesurables, écarts signalés sans correction.
> Critères au format **Given / When / Then**.
> Périmètre : filtre `Téléchargements` (`Music/B Music`, `SpecialRepository.downloadsFolder()`), flag `Preferences.downloaderEnabled`, `DownloadDao`, `MediaStoreObserver`.

## C1 — Apparition < 2 s
> **Given** un téléchargement terminé (`DownloadWorker` → `MediaStoreDownloadWriter` avec `IS_PENDING=1` pendant l'écriture, `IS_PENDING=0` à la vérification)
> **When** l'utilisateur ouvre la bibliothèque
> **Then** le titre apparaît dans `Téléchargements` en **< 2 s** avec titre, artiste et pochette

**Preuve code :** `MediaStoreObserver:16,38` — `REFRESH_DELAY = 500` ms (suivi + `postDelayed 500` ms), donc apparition analysée à **500 ms < 2 s**. Le filtrage `SpecialRepository:178-183` utilise `song.data` (`/Music/B Music/xxx.m4a`) avec tri `FolderSongs`, pochette couverte par incrémentation `Audio.Media` existante.
**Procédure d'exécution :** télécharger un titre de test → chronométrer `adb logcat | grep MediaStoreObserver` + `adb shell dumpsys media.player` jusqu'à visible dans `Folders/Téléchargements` — **mesure non réalisée ici (pas de device)**, rapport d'analyse code 500 ms < 2 s.
**Verdict partiel :** ✅ logique (mécanisme), ⚠️ **mesure device + pochette non jointes**.

## C2 — Scan 5 000 inchangé
> **Given** 5 000 titres existants
> **When** l'utilisateur lance un scan (`scanAllPaths()`)
> **Then** la durée est identique à la baseline v1.0.0 **± 10 %**

**Preuve code :** `SpecialRepository.musicFolders()` et `downloadsFolder()` réutilisent **le même scan** (`songRepository.songs()`), aucune lecture supplémentaire — scan inchangé par construction.
**Procédure d'exécution :** `chrono <scan>` avant/après sur jeu de 5 000 fichiers (`MainActivity.scanAllPaths()` + toast `scan_finished`), `cmd|tail|awk` — **mesure non réalisée ici (pas de device/jeu 5 000 ici)**.
**Verdict partiel :** ✅ logique (même scan), ⚠️ **chiffres non joints**.

## C3 — Flag off : invisible
> **Given** le flag `DOWNLOADER_ENABLED = false`
> **When** l'utilisateur ouvre l'application
> **Then** **aucune trace** de `Téléchargements` ni de l'onglet Télécharger n'est visible

**Preuve code :** `SpecialRepository:179` — `if (!Preferences.downloaderEnabled) return null`, défaut **false** dans `Preferences:630`. `DownloadManager` abandonne aussi l'`enqueue` si flag off.
**Verdict :** ✅ **prouvé par code** (filtre effacé + aucun onglet/entrée telemétré à l'étape 4).

## C4 — Suppression : cohérence
> **Given** un fichier supprimé du dossier `Music/B Music`
> **Then** il disparaît de la bibliothèque **et** l'historique de téléchargement reste cohérent (pas de ligne fantôme)

**Preuve code :** suppression bibliothèque via `MediaStoreObserver` (déjà existant) ; historique via `DownloadDao` avec `deleteById` (`DownloadDao:57`) et `cleanupOrphans` au démarrage du worker (`MediaStoreDownloadWriter:110`) — cohérence logique, **pas de nettoyage au démarrage de l'app** (prévu).
**Procédure d'exécution :** supprimer physiquement `Music/B Music/xxx.mp3` → ouvrir Bibliothèque (`adb logcat`), vérifier disparition liste + aller en historique (étape 5) — **non réalisé ici (pas d'UI exposée à l'étape 4)**.
**Verdict partiel :** ✅ mécanisme, ⚠️ **preuve d'exécution absente**.

## C5 — Déjà téléchargé : avertissement
> **Given** un titre déjà téléchargé (même `platformId + format`)
> **Then** l'utilisateur est **averti avant** un nouveau téléchargement

**Preuve code :** `DownloadManager:53-56` — `dao.getByDedupHash(dedup)?.let { existing -> return@withContext existing }` : **réutilisation silencieuse, aucun avertissement UI/dialog** dans le code à l'étape 4. `DownloadDao:57` & `dedupHash` prouvent le mécanisme mais pas l'avertissement demandé.
**Écart :** 🔴 **à corriger dans l'étape UI (5/5-bis)** — retourner `existing` ne prévient pas l'utilisateur ; un état/exception explicite + dialog est attendu (à concevoir en UI).

## Preuves et mesures jointes

| Item | Mesure/commande | Résultat |
|---|---|---|
| Apparition code | `MediaStoreObserver:38 REFRESH_DELAY=500` | 500 ms < 2 s (analyse) |
| Scan | `musicFolders`/`downloadsFolder` même `songs()` | inchangé par construction |
| Flag | `Preferences:630 downloader_enabled` défaut false + `SpecialRepository:179` | invisible si off ✅ |
| Check glass | `./scripts/check-no-glass.sh` | `GLASS:0` |
| Cold-start / mémoire | `:macrobenchmark:StartupBenchmark` / Profiler | **non mesurés** (pas de device) |
| APK arm64 | `ls -l .../B-Music.apk` | 15,3 Mo ≤ 20 Mo ✅ |

## Feuillete de signalement des écarts
| # | Gravité | Critère | Écart | Correction demandée (agent concerné) |
|---|---|---|---|---|
| E1 | 🟠 bloquant | C5 | `dedup` silencieux, pas de toast/dialog d'avertissement | `DownloadManager.enqueue` doit retourner `Existing` exploitable + dialog UI (BUILD UI) |
| E2 | 🟡 majeur | C1/C2/C4 | Preuves device manquantes (mesure <2 s, scan 5 000 ±10 %, suppress cohérence live) | Fournir `logcat`/mesure + capture `Téléchargements` avant clôture recette (BUILD) |
| E3 | 🟢 info | C1 | Pochette non vérifiée dans le filtre | Capture pochette sur titre téléchargé (BUILD) |
| E4 | 🟢 info | C3 | Non-régression 2 h / macrobenchmark / notif Android 14/15 | Relancer `macrobenchmark` planifiés à l'étape 7 (BUILD) |

*Non-régression (doc. Lean budgets v1.1) : 2 h sans coupure, macrobenchmark scroll pendant téléchargement (jank <1 %, 0 frame perdue), contrôle notif 14/15 — hors périmètre de la recette fonctionnelle ci-dessus.*
