# B Music — Budgets Lean v1.1 (téléchargeur)

> Agent Lean. Date : 8 novembre 2026. Source : `docs/lean/baseline-v1.0.0.md`.
> Statut : budgets **opposables** — toute étape qui dépasse un budget non justifié
> est **refusée** (règle d’arrêt, §3).

## 1. Valeurs de référence v1.0.0 et maxima v1.1

| Budget | Référence v1.0.0 (mesurée) | Maximum v1.1 accepté | Justification de l’écart |
|---|---|---|---|
| **APK arm64** (`B-Music.apk`, fdroid, R8) | **14 896 700 o ≈ 14,2 MiB** (mesuré, 8 nov. 2026) | **≤ 20 Mo** | NewPipeExtractor (Kotlin pur) : +2 à 5 Mo mesurés en build. Au-delà de 20 Mo, l’étape est refusée ou l’extracteur doit être remplacé par une alternative plus légère. |
| **Cold-start** | **non mesuré** (aucun device) — cible v1.0.0 < 600 ms | **< 600 ms, inchangé** | Aucune raison de régresser : le moteur d’extraction et WorkManager sont initialisés **paresseusement** (à l’ouverture de l’écran Télécharger, jamais dans `App.onCreate()` ni `MainActivity.onCreate()`). Tout dépassement = régression pure, refusée. |
| **RSS en lecture** | **non mesuré** — cible v1.0.0 < 200 Mo | **< 200 Mo pendant téléchargement** | Le download s’exécute sur `Dispatchers.IO`, 1 simultané par défaut, buffers par chunks (pas de chargement en mémoire du fichier entier). Le pic mémoire du download + lecture simultanée doit rester sous 200 Mo. |
| **Jank (défilement + download)** | **non mesuré** — cible v1.0.0 < 1 % | **< 1 % pendant download + défilement** | Même seuil qu’en v1.0.0 : le download ne doit jamais dégrader le scroll bibliothèque. Échec = baisse de la priorité/concurrence (1 simultané, Wi-Fi seulement) ou refus de l’étape. |
| **Transition mini-player → plein écran** | **non mesuré** — cible ≤ 16 ms | **0 frame > 16 ms** (8 ms @120 Hz) | Zone la plus sensible (spring `Motion.SpringSoft` 0,86/380 sur BottomSheet). Le download ne touche ni au `PlaybackService` ni à la BottomSheet : toute frame perdue pendant cette transition est une régression bloquante. |
| **Temps de scan 5 000 titres** | **non mesuré** — cible < 5 s | **< 5 s, inchangé** | Les fichiers téléchargés sont insérés une seule fois (`IS_PENDING=0` à la fin) ; aucun rescan complet ne doit être déclenché par le module. |

Notes : « non mesuré » signifie honnêtement *pas encore mesuré faute de device* —
ces valeurs doivent être mesurées sur device avant l’étape 5 (UI) et serviront
de référence absolue ; les maxima v1.1 ci-dessus restent opposables même sans
baseline mesurée.

## 2. Commandes de mesure exactes (reproductibles par n’importe quel agent)

Pré-requis commun : device ou émulateur branché, `adb` dans `~/Android/Sdk/platform-tools/`.

| Budget | Commande exacte |
|---|---|
| APK arm64 | `ls -l bmusic/app/build/outputs/apk/fdroid/release/B-Music.apk` (≤ 20 000 000 o) + `~/Android/Sdk/build-tools/36.0.0/aapt dump badging <apk> \| grep -E "package:\|application-label:"` |
| Cold-start / warm-start | `./gradlew :macrobenchmark:connectedFdroidBenchmark --tests "com.bmusic.app.macrobenchmark.StartupBenchmark"` (médiane 5 itérations, `StartupTimingMetric`) |
| Jank défilement + download | `./gradlew :macrobenchmark:connectedFdroidBenchmark --tests "com.bmusic.app.macrobenchmark.LibraryScrollBenchmark"` **pendant** un download actif ; compléter par `~/Android/Sdk/platform-tools/adb shell dumpsys gfxinfo com.bmusic.app framestats` |
| Transition mini→plein écran | `./gradlew :macrobenchmark:connectedFdroidBenchmark --tests "com.bmusic.app.macrobenchmark.PlayerBenchmark"` — vérifier 0 frame > 16 ms (8 ms @120 Hz) |
| RSS pendant download | Android Studio Profiler (Memory) ou `adb shell dumpsys meminfo com.bmusic.app` pendant lecture + download |
| Scan 5 000 titres | Lancer `MainActivity.scanAllPaths()` sur un jeu de test de 5 000 fichiers et chronométrer jusqu’au toast `scan_finished` |

## 3. Règle d’arrêt

**Une étape qui dépasse un budget non justifié est refusée.** Concrètement :
1. Le dépassement est mesuré avec la commande du §2 (pas d’estimation).
2. L’agent de l’étape propose une justification écrite (cause, alternative rejetée).
3. Si la justification est acceptée par Lean, le budget est révisé ici **avant**
   de continuer ; sinon, l’étape est corrigée ou annulée (retour arrière du plan).

## 4. Checklist de revue Lean (10 points, après chaque étape)

- [ ] **1. Mesures faites** : chaque budget concerné a été mesuré avec la commande du §2, valeurs consignées (pas d’affirmation sans preuve).
- [ ] **2. APK** : `B-Music.apk` arm64 ≤ 20 Mo ; si dépassement, justification Lean avant merge.
- [ ] **3. Cold-start** : < 600 ms, inchangé ; aucune init lourde ajoutée à `App.onCreate()` / `MainActivity.onCreate()`.
- [ ] **4. Jank** : < 1 % pendant download + défilement ; 0 frame > 16 ms sur mini→plein écran.
- [ ] **5. Mémoire** : RSS < 200 Mo en lecture + download ; pas de chargement de fichier entier en mémoire.
- [ ] **6. Scan** : 5 000 titres < 5 s ; aucun rescan complet déclenché par le module.
- [ ] **7. Flag** : avec `DOWNLOADER_ENABLED=false`, rien d’actif (pas de service, worker, onglet, intent) — vérifié par revue de code + test manuel.
- [ ] **8. Zones intouchées** : `playback/`, `ui/theme/`, `MediaStoreObserver`, entités Room et migrations 1→8 inchangés ou modifications strictement justifiées.
- [ ] **9. Design** : `scripts/check-no-glass.sh` vert ; animations uniquement via `Motion` ; captures avant/après si UI modifiée.
- [ ] **10. Retour arrière** : le plan de retour de l’étape a été relu et est applicable (commandes + résultat attendu documentés).
