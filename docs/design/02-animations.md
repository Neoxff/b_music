# B Music — Animations (v1.0)

> Agent Designer B Music. Date : 6 novembre 2026.
> Principes : **ressorts (physique), jamais de décor gratuit, continuité spatiale obligatoire.**
> Toute animation a : déclencheur, éléments, propriété, durée, courbe/ressort, délai, annulation/retournement.
> Règle absolue : si l'animation n'aide pas la compréhension spatiale ou la confirmation d'action → **elle n'existe pas**.

---

## 1. Mini-player → Lecteur plein écran (transition d'élément partagé)

- **Déclencheur** : tap mini-player / swipe-up (geste vertical, suivi du doigt).
- **Éléments animés** : pochette (shared element), titre, artiste, barre de progression + contrôles (fade-slide), fond.
- **Propriétés** :
  - Pochette : `translationX/Y`, `scale`, `cornerRadius` 12 → 28 dp.
  - Titre/artiste : `translationY` (depuis bas du mini-player vers le slot du lecteur), `alpha`.
  - Fond : couleur de surface `surface2`/`surface1` → `bg` (interpolation Color, pas d'alpha sur scrim).
  - Contrôles + progression : `alpha` + `translationY` 16 dp → 0.
- **Durée / ressort** :
  - Ressort : `dampingRatio = 0.86`, `stiffness = 380` (Compose `spring()`), **plafond 420 ms** ; geste suivi : `spring(dampingRatio = 1f, stiffness = 500f)` pendant le drag ; à la vitesse de relâchement > 1 800 dp/s → terminer vers écran plein, sinon retour selon vélocité (seuil 30 % ou vélocité).
  - Le geste **suit le doigt** 1:1 (`Modifier.pointerInput` + `draggable`), pas de délai ; la transition ne démarre qu'au relâchement si geste annulé.
- **Délai** : 0.
- **Annulation/retournement** : geste vers le bas sur le lecteur → retour inverse symétrique (même ressort) ; interruption possible à tout moment ; si la lecture change en plein vol, la pochette se rabat sur la nouvelle (fade 120 ms, pas de rupture).
- Compose : `SharedTransitionLayout` + `Modifier.sharedElement(...)` + `retainBoundsAfter` ; fallback (Reduced Motion) : fondu croisé simple 150 ms.

## 2. Lecture / pause

- **Déclencheur** : tap bouton lecture/pause (mini-player 40 dp, plein écran 72 dp) ou raccourci média (clavier/écouteurs).
- **Éléments** : icône centrale (play ↔ pause), cercle.
- **Propriétés** : morphing path (play triangle → deux barres) + `scale` cercle.
- **Durée** : **220 ms**, courbe `EmphasizedDecelerate` (Material) pour le morph ; rebond échelle **1.0 → 0.94 → 1.0** via ressort `dampingRatio = 0.55, stiffness = 700` (≈ 300 ms de ressort, overlap avec morph).
- **Délai** : 0. **Annulation** : re-tap = morph inverse immédiat ; le ressort se re-vise depuis la valeur courante.

## 3. Changement de titre (piste suivante/précédente)

- **Déclencheur** : fin de piste, next/prev, sélection dans la file.
- **Éléments** : pochette (card), titre, artiste.
- **Propriétés** : pochette `translationX` (±28 dp, direction = sens de navigation) + `alpha` fondu croisé ; titre/artiste `alpha` + `translationY` 8 dp.
- **Durée** : pochette **280 ms** `EmphasizedDecelerate` ; titre/artiste **220 ms**, **décalés de 40 ms** après la pochette.
- **Délai** : 0 / 40 ms. **Annulation** : interruption → l'animation courante se termine en 80 ms vers l'état cible du nouveau titre (pas de retour).

## 4. Barre de progression

- **Déclencheur** : lecture en cours (tick média), toucher/drag sur piste.
- **Éléments** : piste (hauteur), remplissage (largeur), curseur (alpha + scale), tooltip temps (optionnel).
- **Propriétés** : hauteur piste **4 → 8 dp** (toucher), curseur `alpha` 0 → 1 et `scale` 0.6 → 1, remplissage `width` interpolée chaque frame.
- **Durée** : apparition curseur **120 ms** `FastOutSlowIn` ; passage hauteur 4→8 **120 ms** ; mise à jour continue : interpolation frame-to-frame (pas de steps d'1 s) ; la piste suit `positionMs` avec lissage `animateFloatAsState(target, tween(90))` pour masquer la latence Media3.
- **Délai** : 0. **Annulation** : doigt levé → retour hauteur 8→4 en 160 ms ; drag en cours bloque la mise à jour auto.

## 5. Listes (bibliothèque, recherche, file)

- **Déclencheur** : affichage initial de la liste / changement de filtre.
- **Éléments** : lignes visibles (max **8**), en-têtes de section.
- **Propriétés** : `alpha` 0 → 1, `translationY` 16 → 0.
- **Durée** : entrant **240 ms** `EmphasizedDecelerate` ; **stagger 24 ms / élément**, plafond 8 éléments (les suivants apparaissent d'un bloc).
- **En-têtes collants** : sticky header Compose `stickyHeader`/pinnable ; pas de clignotement : hauteur constante, pas d'animation à chaque bind ; changement de section : crossfade 150 ms sur le label seul.
- **Délai** : 0 + stagger. **Annulation** : si l'utilisateur fling avant la fin, les items restants sautent sans animation (ils sont déjà au-delà).

## 6. Navigation entre onglets (Accueil · Recherche · File · Réglages)

- **Déclencheur** : tap icône nav / swipe horizontal autorisé sur la zone de contenu.
- **Éléments** : contenu d'écran.
- **Propriétés** : `translationX` (±24 dp, sens du déplacement) + `alpha`.
- **Durée** : **300 ms**, courbe **Emphasized standard** (Material `emphasizedDecelerate`/`emphasizedAccelerate` selon sens) — pas de spring ici (navigation doit être prévisible).
- **Délai** : 0. **Annulation** : swipe contraire en cours → l'onglet suit le doigt (1:1), décision au relâchement.

## 7. Retour tactile (haptique)

- **Déclencheur** : lecture/pause, favori, ajout/retrait file, ajout playlist.
- **Implémentation** : `HapticFeedbackType.LongPress` allégé (Compose) / `performHapticFeedback(HapticFeedbackConstants.CONFIRM)` côté service ; **jamais** sur seek (trop fréquent), jamais en mode silencieux système.
- **Durée** : impulsion système (~10 ms). **Annulation** : n/a.

## 8. Favori (cœur)

- **Déclencheur** : tap favori (ligne, lecteur, en-tête).
- **Éléments** : icône cœur.
- **Propriétés** : `scale` 1.0 → 1.35 → 1.0, `alpha` du remplissage 0 → 1 (clip vertical du fill).
- **Durée** : **350 ms**, ressort `dampingRatio = 0.5, stiffness = 550` ; micro-échelle du cercle bouton 1.0 → 1.12 → 1.0 (même ressort).
- **Délai** : 0. **Annulation** : tap rapide double (on/off) → le ressort repart de la position courante, pas de rupture visuelle.

## 9. Égaliseur / visualiseur léger (écran lecteur, titre en cours)

- **Déclencheur** : lecture active au premier plan ; **coupé** en pause ou en arrière-plan ou batterie économie.
- **Éléments** : 3–4 barres (4 dp larges, coin 2 dp) à droite du titre / sous la pochette.
- **Propriétés** : `scaleY` (ancrage bas) — **pas de hauteur animée** (recomposition), valeurs d'un `FloatArray` lissé `N = 3` bins RMS via `Player.Listener` (analyser max 10 Hz, interpolé côté UI).
- **Durée** : frame-by-frame, **60 fps** (Choreographer) ; lissage 150 ms entre valeurs ; amplitude < 45 % de la zone.
- **CPU** : désactivé si `LifecycleEventEffect(ON_PAUSE/ON_STOP)` ou `isPowerSaveMode` ou reduced motion.
- **Annulation** : retour pause → barres descendent vers 0 en 200 ms puis 0 frames CPU.

## 10. États vides et chargement

- **Squelettes** : blocs `surface2`/`surface3` (jamais alpha sur blanc/one), hauteur/largeur du layout final ; shimmer = `translationX` d'un reflet **opaque** (pas de gradient brillant), 1 200 ms, boucle ; rayons identiques au composant cible.
- **États vides** : illustration géométrique tonale (traits `trait`), titre Micro/Légende + action primaire (ex. « Ajouter un dossier ») ; aucun spinner plein écran.
- **Chargement liste** : skeleton ×6 lignes max ; jamais de « chargement… » texte seul ; erreur : icône + message + bouton « Réessayer » (accent), pas de toast rouge seul.

---

## 11. Tableau récapitulatif (constantes à centraliser — `BMusicMotion.kt`)

| # | Animation | Durée | Courbe / Ressort (damping, stiffness) | Délai |
|---|---|---|---|---|
| 1 | Mini → plein écran ( pochette ) | ≤ 420 ms | spring (0.86, 380) ; drag: spring(1.0, 500) | 0 |
| 1b | Mini → plein (retour bg/contrôles) | 320 ms | EmphasizedDecelerate | 0 |
| 2 | Play ↔ pause morph | 220 ms | EmphasizedDecelerate (+ rebond spring 0.55/700) | 0 |
| 3 | Changement de piste — pochette | 280 ms | EmphasizedDecelerate | 0 |
| 3b | Changement de piste — titre/artiste | 220 ms | EmphasizedDecelerate | **40 ms** |
| 4 | Progression — hauteur piste | 120 ms | FastOutSlowIn | 0 |
| 4b | Progression — curseur | 120 ms | FastOutSlowIn | 0 |
| 4c | Progression — lissage tick | 90 ms | linear (animateFloatAsState) | 0 |
| 5 | Liste — entrée | 240 ms | EmphasizedDecelerate, stagger **24 ms/élément, max 8** | 0+stagger |
| 6 | Onglets — navigation | 300 ms | Emphasized standard | 0 |
| 7 | Haptique lecture/favori/file | ~10 ms | système | 0 |
| 8 | Favori — ressort | 350 ms | spring (0.5, 550) | 0 |
| 9 | Égaliseur — lissage bins | 150 ms | lerp linéaire | frame |
| 10 | Skeleton shimmer | 1 200 ms boucle | linear | 0 |

**Constantes** : `DurationShort=120, DurationBase=220, DurationLong=300, DurationXLong=420; SpringSoft=(0.86,380), SpringBounce=(0.5,550), SpringSnappy=(0.55,700); Stagger=24ms; MaxStaggerItems=8.`

## 12. Accessibilité — « Supprimer les animations »

Si `Settings.Global.ANIMATOR_DURATION_SCALE == 0` ou `prefers-reduced-motion` :

- Toutes les durées → **0–1 ms** ; transitions remplacées par **fondu simple ≤ 150 ms** ;
- ressorts remplacés par `tween(0)` ;
- staggers supprimés (apparition immédiate) ;
- égaliseur désactivé ;
- aucune information ne dépend du mouvement (l'état courant est toujours textuel/iconique).
- Compose : `LocalInspectionMode`/`platformAnimationScale` via `AnimatedContent` conditionnel ou `BMusicMotion.reduced`.

## 13. Ce qui est interdit

- `InfiniteTransition` hors CTA chargé ; parallax ; blur animé ; rotation gratuite ; bounce excessif (damping < 0.4) ; durées > 500 ms hormis shimmer ; animations de layout non nécessaires (préférer `animate*AsState` bornés).
