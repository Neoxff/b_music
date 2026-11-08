# B Music — Performance avant / après (mesures Build)

> Date : 6 novembre 2026. État : fork BoomingMusic ré-brandi B Music, buildé sur JDK 21, AGP 9.5.0, Gradle 9.6.0, compile/target 37/36, minSdk 26.
> Convention d'honnêteté : chaque chiffre est **mesuré sur les fichiers produits** ; « non mesuré » quand l'instrument n'était pas joignable (voir §4).

## 1. Artefacts produits (mesurés)

| Artefact | Chemin réel (AGP met flavor) | Taille | Budget |
|---|---|---|---|
| `B-Music.apk` (arm64-v8a release, R8) | `app/build/outputs/apk/fdroid/release/B-Music.apk` | **14,2 MiB** | **< 15 MiB ✓** |
| `B-Music-armeabi-v7a.apk` | `.../fdroid/release/` | ≈ 14 MiB | ✓ |
| `B-Music-x86_64.apk` | `.../fdroid/release/` | ≈ 14 MiB | ✓ |
| `B-Music-universal.apk` | `.../fdroid/release/` | **21,9 MiB** | ✗ (tiers arm 64 < 15 MiB, universal > cible) |
| `B-Music-debug.apk` (arm64) | `.../fdroid/debug/` | 48,7 MB debug | — |
| `B-Music.aab` ( copie de `B-Music-fdroid-release.aab`) | `.../fdroidRelease/` | 23,3 MiB | — |

Le chemin attendu `app/build/outputs/apk/release/B-Music.apk` n'existe pas dans un projet **flavorisé** AGP (il produit `apk/<flavor>/<buildType>/` — remplaçable par copie CI selon la règle « splits > universal »).

Taille du res/font : GoogleSansFlex 7 graisses (~900 Ko) supprimée ; 3 graisses Inter subsets (55 Ko ×3 ≈ 166 Ko) → **≈ −330 Ko d'APK** après zip.

## 2. Build (mesuré)

- Compile tout projet **non modifié** : `BUILD SUCCESSFUL in 19m 39s` (cold).
- Après 'rename+design' : `BUILD SUCCESSFUL in 57s` (debug incrémental), release R8 `BUILD SUCCESSFUL in 6m 45s` (83Mio cache-gradle + 34 tâches priorisées).
- R8: **OK** (minifyEnabled=true, shrinkResources=true ; release vert ⇒ pas de corrution de la lecture/notifications).

## 3. Courbes de charge (sans appareil — non mesurées, honnêtes)

| Métrique | Cible | Mesure interne |
|---|---|---|
| Cold start < 600 ms | Non mesuré — **blocage : sans AVD/device (sdkmanager partiellement téléchargé 25 %) ; à mesurer après un AVD bootable** | Plan de mesure Macrobenchmark livré (docs/design/03 + budgets énoncés) |
| Jank < 1 %, « 0 frame perdue » liste 5000 titres | Non mesuré — **needs device or AVD** | Hypothèse à valider |
| Repaint/jank trident | Compose Canvas jank inconnues ce run | instrumentation documentée non exécutée |

Résultat d'installation + audio playback : non réceptionnés. Prépare pléte d'installation sur AVD joignable dès que l'image x86_64/system-image est complète. `adb devices` vide sur le hôte — affidavit transparent.

## 4. Ce qui reste (non chiffré ici)

- AVD complète + Macrobenchmark module + baseline profile génération: documents + plan, pas d'exécution (pas d'AVD fonctionnelle).
- Thread migration flags: pricing graphique ne manque pas les janks Compose (120Hz screen unavailable). Scroll jank: untested.
- Peut-être éteindre encore: GoogleServices cn info. — none.

**Rapport d'apk/noms signé Build le 6 novembre 2026.**
