# B Music — Budgets Lean v1.2 (Liquid léger)

> **Agent Lean, 9 novembre 2026.** Source : `docs/lean/baseline-v1.0.0.md` (v1.0.0) + mesures v1.1 obtenues dans la session (APK 15,30 Mo le 9/11).
> Statut : budgets **opposables** avant tout changement `GlassSurface`. Toute étape qui dépasse un budget non justifié est **refusée** (voir §4).
> Convention : toute valeur non mesurée est marquée **non mesuré** — aucun chiffre inventé ; « commande » = commande exacte reproductible.

## 1. Baseline (avant, commande de mesure)

| Métrique | Valeur avant v1.1.x | Commande de mesure |
|---|---|---|
| **APK arm64** | **14.896.700 o ≈ 14,2 Mo** (v1.0.0), **15.300.251 o ≈ 15,30 Mo** (v1.1, 9/11) | `ls -l bmusic/app/build/outputs/apk/fdroid/release/B-Music.apk` (≤20 000 000 o) |
| **Cold-start** | **non mesuré** (aucun device `adb`) — cible <600 ms | `./gradlew :macrobenchmark:connectedFdroidBenchmark --tests "com.bmusic.app.macrobenchmark.StartupBenchmark"` (médiane 5 itérations, `StartupTimingMetric`) |
| **Jank % et frames perdues** (bibliothèque) | **non mesuré** — cible <1 % | `:macrobenchmark:LibraryScrollBenchmark` + `adb shell dumpsys gfxinfo com.bmusic.app framestats` |
| **Jank % et frames perdues** (liste résultats téléchargeur) | **non mesuré** — cible <1 %, 0 frame perdue | `:macrobenchmark:LibraryScrollBenchmark` **pendant recherche paginée (100 résultats + miniatures)** |
| **Transition mini→plein écran** (durée des frames) | **non mesuré** — cible ≤16 ms (60 Hz), ≤8 ms (120 Hz) | `:macrobenchmark:PlayerBenchmark` |
| **Mémoire RSS** | **non mesuré** — cible <200 Mo | `dumpsys meminfo com.bmusic.app` / Android Studio Profiler (Memory) |
| **Température / consommation** 30 min écran allumé | **non mesuré** — cible batterie ≤ baseline +5 % | `adb shell dumpsys battery` + `thermal` , lecture 30 min màn hình allumé |
| Geräte (au moins 3) | **non mesuré** : modeste (API 31+, ~4 Go), milieu de gamme, 120 Hz — procédure en macrobenchmark | Traduction : exécuter chaque commande sur 3 appareils référents avant étape 1 |

## 2. Budgets maximaux après flou

| Budget | Maximum v1.2 accepté | Justification |
|---|---|---|
| **APK** | **≤ +0,5 Mo** vs 15,30 Mo (≤15,8 Mo) — ou justifié si bibliothèque de flou | Flou sans dépendance lourde (aucune lib Haze). Au-delà : refus ou demande de justification. |
| **Cold-start** | **inchangé <600 ms** | `GlassSurface` paresseux (initialisé à l'ouverture de la surface) ; aucune init dans `App.onCreate()`/`MainActivity.onCreate()`. |
| **Jank** | **<1 %** défilement + download + flou, **0 frame >16 ms (60 Hz) / >8 ms (120 Hz)** transitions principales | Même seuil que v1.0.0/v1.1 ; le flou ne doit jamais dégrader le scroll bibliothèque ni la transition mini→plein écran (`Motion.SpringSoft` 0.86/380). |
| **Surcoût de frame du flou** | **≤ 2 ms** sur milieu de gamme | Mesuré par `FrameTimingMetric` sur la surface floutée vs opaque. Au-delà : refus. |
| **Mémoire supplémentaire** | **≤ 15 Mo** (cache flou + surfaces supplémentaires) | `MemoryCache` Coil borné déjà 32 Mo ; ajout pouches floutées comptées incluses. |
| **Batterie 30 min lecture** | **≤ +5 %** vs baseline v1.1 | Mesuré `dumpsys battery` sur 30 min de lecture écran allumé (même brightness). |

## 3. Règle de désactivation automatique (mode Automatique)

Le mode **Automatique** (défaut) bascule vers **opaque** dès qu'un critère se déclenche :

- **API < 31** (pas de `RenderEffect` natif) → opaque.
- **`ActivityManager.isLowRamDevice`** (`≤ 2 Go RAM`) → opaque.
- **Économiseur de batterie activé** (`PowerManager.isPowerSaveMode`) → opaque.
- **Animations réduites** (`rememberReducedMotion()` / `ANIMATOR_DURATION_SCALE == 0`) → opaque.
- **Détection de images perdues en continu** : si >2 frames consécutives >16 ms (ou >8 ms @120 Hz) pendant le défilement d'une surface floutée, ou RSS >170 Mo pendant 3 secondes → opaque **immediately** (jusqu'à la prochaine session).

Implémentation attendue : singleton `GlassPolicy.isActive()` avec sondes périodiques (500 ms) ; aucun polling autre que `Choreographer` callbacks.

## 4. Plafonds d'usage

| Plafond | Valeur | Justification |
|---|---|---|
| **Surfaces floutées simultanées** | **≤ 2** (barre du bas + mini-player + top bar — hors lecteur) ; **plein écran : 1 fond pré-flouté + 1 surface max** | Évite la charge GPU cumulée ; l'aperçu flouté est décoratif, pas fonctionnel. |
| **Rayon maximal** | **24 dp** (cible 16–24 dp) | Au-delà, effet « vitreux/rembourré » + coût GPU ↑ (flou animé interdit). |
| **Rayon animé** | **INTERDIT** — jamais de `BlurRadius` animé image par image | Le shader ne doit être recalculé que sur changement de surface, pas par frame. |
| **Seuil batterie** | `isLowRamDevice` ou batterie < 20 % → opaque | Évite surchauffe. |

## 5. Règle d'arrêt

**Une étape qui dépasse un budget non justifié est refusée.** Concrètement :
1. Le dépassement est mesuré avec la commande du §1 (pas d'estimation).
2. L'agent de l'étape propose une justification écrite (cause, alternative rejetée).
3. Si la justification est acceptée par Lean, le budget est révisé ici **avant** de continuer ; sinon l'étape est corrigée ou annulée (retour arrière du plan).

## 6. Checklist de revue Lean (10 points, après chaque étape)

- [ ] **1. Mesures faites** : chaque budget concerné a été mesuré avec la commande du §1, valeurs consignées (pas d'affirmation sans preuve).
- [ ] **2. APK** : arm64 ≤15,8 Mo (+0,5 Mo) ou justification Lean avant merge ; R8 actif.
- [ ] **3. Cold-start** : <600 ms inchangé ; aucune init lourde ajoutée (`GlassSurface` paresseux).
- [ ] **4. Jank GPU** : <1 % pendant défilement + flou, 0 frame >16 ms (60 Hz) / >8 ms (120 Hz), surcoût flou ≤2 ms.
- [ ] **5. Mémoire** : RSS <200 Mo, surcoût flou ≤15 Mo, cache images borné.
- [ ] **6. Batterie** : surcoût ≤+5 % sur 30 min lecture écran allumé ; `isLowRamDevice`/batterie <20 % → opaque.
- [ ] **7. Flag** : avec « Effets de flou » Désactivé, rien d'actif (pas de flou, pas de couche supplémentaire) — vérifié par revue de code + test manuel.
- [ ] **8. Zones intouchées** : `playback/`, téléchargeur, Room/migrations intacts ou justifiés ; `check-glass-policy.sh` vert (liste blanche).
- [ ] **9. Design** : rayon ≤24 dp, opacité sombre/clair conformes §2, animations = opacité/translation/échelle via `Motion.kt` uniquement (jamais le rayon), captures sombre/clair si UI modifiée.
- [ ] **10. Retour arrière** : param « Effets de flou » `Désactivé` + `git revert` du seul commit `GlassSurface` ; surfaces ~0 pixel diff à l'état opaque.
