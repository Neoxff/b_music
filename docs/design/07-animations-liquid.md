# 07 — Animations liquid léger (spec)

> **Agent Designer**, branche `feat/glass-etape-1` (à tracer). Spécification UX/UI **sans code** des animations liquid léger, très fluides, **UNIQUEMENT** via les tokens de `Motion.kt` (ressorts, durées, stagger). **Règle d'or** : on anime **opacité, translation, échelle et fondu croisé** ; on n'anime **JAMAIS** le rayon de flou image par image.

## 1. Déclencheur → animations

### 1.1 Mini-player → lecteur plein écran (surface étendue, fond pré-flouté en fondu, pochette partagée)

- **Déclencheur** : drag vertical sur le mini-player (BottomSheet `prog` via `sliding_music_panel_layout.xml`, `MiniPlayerFragment`) ou tap sur la pochette.
- **Éléments animés** : surface du mini-player (translation Y + hauteur), fond pré-flouté (fondu croisé `Crossfade`), pochette (élément partagé existant, échelle).
- **Propriétés** : translation Y0→-H, alpha 0→1 (fond), échelle 1→1,05 (pochette).
- **Durée** : drag direct (pos follows doigt, 60 Hz vsync, pas d'animation) ; au `settleState` (état expanded/collapsed), retour en ressort `Motion.SpringSoft` (`dampingRatio=0.86`, `stiffness=380`) avec `DurationBase 220 ms` (seuil 12 dp, seuil vélocité selon conventions — complet quand `|v| > 0,5 dp/frame` ou seuil dépassé).
  **Annulation** : relâche tôt (release < 12 dp) → retour en ressort 220 ms (annulation douce, pas d'à-coup : `Dettents` + **annulation** complet avant rebond, pas de corré large).
- **Critère** : le fond pré-flouté est une image **pré-cachée** (cache 64 px) — le fondu ne floute jamais les pixels en direct (voir §5).

### 1.2 Barre de navigation du bas (apparition/disparition au défilement, changement d'onglet)

- **Déclencheur** : scroll down (hide) / scroll up (show) ; tap sur autre onglet.
- **Éléments animés** : barre de navigation (`BottomSheet` BottomNavigation).
- **Propriétés** : translation Y + opacité 0 ↔ 1.
- **Durée/ressort** : `tweenStd` (`DurationBase=220`, `EmphasizedDecelerate` pour slide-in, `EmphasizedAccelerate` pour slide-out) OU `SpringSoft` si ressort souhaité pour déplacer la grille (`stiffness=380`, `dampingRatio=0.86`).
- **Indicateur d'onglet** : `animatedTabPosition` via `SpringSoft` (ou `TabRow` `indicator`), pas d'animation de rayon.
- **Annulation** : revers texturel en plein geste (translation rebroussée : reverse du tween, pas de saut).

### 1.3 Barre supérieure (transparente → surface teintée selon défilement)

- **Déclencheur** : scroll d'une liste (`LazyColumn`) ou échelle de contenu.
- **Éléments** : fond teinte (`surface` ou `scrim`).
- **Propriété** : **opacité de la teinte seulement**, pas de flou animé ; `Crossfade` alpha 0 ↔ cible.
- **Durée** : `tweenStd` (220 ms).
- **Annulation** : retour instantané sur scroll inverse (Crossfade réversible).

### 1.4 Feuille d'aperçu du téléchargeur

- **Déclencheur** : tap sur résultat de recherche (`SearchTab.kt:PreviewSheet`).
- **Éléments** : bottom sheet (`ModalBottomSheet`).
- **Propriétés** : translation Y0→0, échelle 1 → 1, opacité 0→100 % ; **scrim opaque simple** (60 % noir, pas de flou animé).
- **Durée/ressort** : ressorts `SpringSoft` ou `SpringBounce` (damping 0.5, stiffness 550) selon politesse désirée ; scrim `tweenStd`.
- **Annulation** : swipe down en plein geste annule la dynamique (annullable).

### 1.5 Changement de titre — palette dominants dynamique

- **Déclencheur** : nouveau `song.id` / palette (`Palette`).
- **Éléments** : fond pré-flouté (cache dédié `AudioCoverFetcher.kt:26`, réduction 64 px puis flou en mémoire).
- **Propriétés** : **fondu croisé uniquement** (`Crossfade`), `≈ 400 ms` (`DurationLong` 300 ms + `EmphasizedDecelerate`) — **démaquillage** conservateur.
- **Annulation** : transition interrompue par un nouveau titre suivant → **restart Crossfade à 0 ms** (nouveau fondu).

### 1.6 Pression sur un bouton — micro-échelle tactile

- **Déclencheur** : touch down/up Button.
- **Éléments** : bouton (`Scale 1,0 → 0,96 → 1,0`).
- **Propriétés** : échelle.
- **Durée/ressort** : `SpringSnappy` (damping 0.55, stiffness 700) ou `tweenStd`.
- **Haptique** : `VIEW_GONE` guide `forceFeedback` (vibration 10 ms) ou `ForceFeedbackConstants` — pas d'haptique supplémentaire structurée.
- **Annulation** : reversible thumb release, retour à 1,0 en ressort court.

### 1.7 Listes — échelonnement, aucun saut, en-têtes qui se teintent

- **Déclencheur** : `scroll` ou toggle onglet.
- **Éléments** : items de liste.
- **Propriétés** : opacité (`AnimatedVisibility`, fondu croisé), translation X (grille) ou Y (liste) **court** (32 dp).
- **Durée** : `tweenStd` ou `DurationLong (300 ms)` selon désirabilité ; **stagger `StaggerMs=24`** sur **8** premiers items.
- **En-tête catégorie** : teinte progressive (opacité) sur `scrollY` (Crossfade).
- **Annulation** : scroll continu (items non visibles → annulé).

### 1.8 Animations réduites

- **Déclencheur** : `ANIMATOR_DURATION_SCALE=0` (`rememberReducedMotion()`) → durées ≈ 0 (pas de ressort, pas besé). Surfaces **opaques** (même repli).
- **Propriétés** : opaque + opacity.
- **Critère** : aucun long tween si `rememberReducedMotion()` est vrai (`scaleDuration(ms, reduced)`).

## Tableau récapitulatif (à centraliser dans Motion.kt)

| Animation | Déclencheur | Propriété animée | Durée / ressort | Délai / stagger | Annulation/retour |
|---|---|---|---|---|---|
| **Mini→plein drag** | drag vertical mini-player | translation Y + alpha fond | suivi doigt, settle `SpringSoft` 0,86/380 (DurationBase 220 ms) | échelonnement n/a | release <12 dp → retour ressort 220 ms (annulation douce) |
| **Fond pré-flouté fondu** | nouveau titre / palette | Crossfade (opacité) | **400 ms** (`DurationLong` 300 + `EmphasizedDecelerate`) | 0 ms | nouveau titre → **restart 0 ms** |
| **Barre navigation show/hide** | scroll up / down | translation Y + alpha | `tweenStd` 220 ms ou `SpringSoft` | 0 ms | reverse du tween |
| **Indicateur onglet** | tap autre onglet | translation (position) | `animatedTabPosition` via `SpringSoft` | 0 ms | annulation swipe direct |
| **Barre supérieure teinte** | scroll liste | alpha teinte | `tweenStd` 220 ms | 0 ms | retour instantané |
| **Feuille aperçu download** | tap résultat | translation 0→H + alpha scrim | `SpringSoft` (ou `SpringBounce` 0.5/550) | 0 ms | swipe down annule |
| **Micro-échelle bouton** | touch down/up | échelle 1→0,96→1 | `SpringSnappy` 0.55/700 ou `tweenStd` | 0 ms | reversible thumb |
| **Listes échelonnées** | scroll / toggle onglet | alpha + translation (corte 32 dp) | `DurationLong` 300 ms | `StaggerMs=24` (≤8) | scroll annulé |
| **Animations réduites** | `ANIMATOR_DURATION_SCALE=0` | opaque seul | `scaleDuration(ms,true)=0` | 0 ms | pas de ressort |

**Tokens à centraliser** : **aucun nouveau token** nécessaire — `SpringSoft`/`SpringBounce`/`SpringSnappy`/`tweenStd`/`StaggerMs` de `Motion.kt` réutilisés ; *ajout justifié* if `Crossfade400` souhaité (≈400 ms palette) — **justification** : `DurationLong` 300 ms trop court pour un fondu de couleur (justification de divergence ici).
