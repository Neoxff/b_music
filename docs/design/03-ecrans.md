# B Music — Écrans (v1.0)

> Agent Designer B Music. Date : 6 novembre 2026.
> Référence : `01-design-system.md` (couleurs, typo, composants) et `02-animations.md` (renvois `§ anim`).
> Tous les écrans existent en **sombre (défaut)** et **clair** ; mêmes dp, mêmes états.
> Unités : dp. Marges d'écran 20 dp. Grille 4 dp. Cible tactile ≥ 44 dp.
> Notation : `H` hauteur, `M` marge, `R` rayon, `A` accent de marque (ou dynamique, écrans lecteur).

---

## 1. Accueil / Bibliothèque

**Hiérarchie** : titre de section (Display 32/40) → onglets → grille d'albums 2 colonnes → mini-player collé bas.

```
┌──────────────────────────────────────┐
│  B Music                    [●]      │  ← Micro 11/16, icône app discrète, 20 dp du haut
│  Bibliothèque                        │  ← Display 32/40 SC700 · M 20
│  ────────────────────────────────    │  ← trait 1 px (option)
│  Titres  Albums  Artistes  Playlists │  ← onglets, Corps Medium 16 · indicateur 2 px accent, 28 dp H
│                                      │
│  ┌─────────┐  ┌─────────┐            │  ← grille 2 col., GAP 12 dp, M 20
│  │ POCHETTE│  │ POCHETTE│            │  ← pochette carrée, R 12 dp
│  │ 12 R    │  │ 12 R    │            │
│  │ Titre   │  │ Titre   │            │  ← Corps Medium 16 (2 lignes max)
│  │ Artiste │  │ Artiste │            │  ← Légende 13/18 onBgSecondary
│  └─────────┘  └─────────┘            │
│  …                                   │
├──────────────────────────────────────┤  ← séparateur 1 px trait
│ ▣ Mini-player                        │  ← 64 dp, pochette 48 dp R 12
└──────────────────────────────────────┘
```

- **Onglets** : 4, répartis sur la largeur, hauteur 44 dp, indicateur souligné 2 dp `accent` sous l'onglet actif, `onBg` actif / `onBgMuted` inactif. Tap = changement avec navigation § anim 6.
- **Grille** : cartes sans fond (pochette seule + texte) ; `surface2` en survol/pression (P : `surface3`) ; skeleton 6 tuiles au chargement ; état vide : illustration tonale + « Aucun titre trouvé » + bouton « Choisir un dossier ».
- **Défilement** : `LazyVerticalGrid`, en-tête onglets collant (`stickyHeader`), pas de clignotement ; pochettes chargées via Coil (mémoires bornées, cf. budgets).
- **Scroll-to-top** : tap sur l'onglet actif remonte en `spring` discret.
- **États** : chargement (skeleton ×6), vide (illustration + CTA), erreur permissions (bouton « Autoriser l'accès audio », message actionnable), erreur lecture (toast discret, jamais modal).
- **Tablette/paysage** : grille 3–4 colonnes (largeur ≥ 720 dp), marge 32 dp, en-tête + grid side by side (master-detail 380 dp / reste pochettes).
- **Animations** : § anim 5 (stagger ligne), § anim 6 (onglets), § anim 1 (mini → plein).

## 2. Lecteur plein écran

**Hiérarchie** : grande respiration haut → pochette (héros) → métadonnées → progression → contrôles → rangée secondaire.

```
┌──────────────────────────────────────┐
│                 ↓ (chevron 40 dp)    │  ← ferme, M haut 16 dp
│                                      │  ← respiration 24 dp
│        ┌──────────────────┐          │  ← pochette ≈ 80 % largeur (écran − 40 dp M)
│        │                  │          │  ← carrée, R 28 dp, héros visuel
│        │                  │          │
│        └──────────────────┘          │  ← marge basse 20 dp
│                                      │
│  Titre de la piste                   │  ← Titre 22/28 SC600, M 20, max 2 lignes
│  Artiste — Album                     │  ← Légende 13/18, **accent dynamique** (ou marque)
│                                      │
│  ━━━━━━━●━━━━━━━━━━━━━               │  ← progression 4 dp (8 au toucher), A accent
│  1:23                    -2:47       │  ← Micro 11/16 tnum, onBgMuted
│  ◯  ◀◀   ▶ / ❚❚   ▶▶   ⋮             │  ← contrôles : préc/suiv 40 dp, lecture **cercle 72 dp**
│                                      │
│  shuffle   repeat   favori   file    │  ← rangée secondaire 44 dp, M 20
└──────────────────────────────────────┘
```

- **Pochette** : largeur = min(écran − 40, 480 dp), R 28, centrée ; halo **opaque** (couleur dominante −8 % tonalité, jamais blur) derrière, 24 dp de padding autour.
- **Titre** : Titre 22/28 `onBg` ; **artiste en accent** (dynamique ou marque) Légende 13/18 — seul usage de l'accent en texte, limité à cette ligne.
- **Contrôles** : préc/suiv `skip_previous/next` 32 dp, cercle 44 dp touchable ; lecture **cercle plein 72 dp** bouton lecture (§ anim 2), icône 36 dp `onAccent`.
- **Rangée secondaire** : shuffle / répéter (off–all–one) / favori / file d'attente — icônes 24 dp, 44 dp touchables, actif = accent.
- **Grande respiration** : 32–48 dp entre blocs ; le bas suit l'inset système + safe area.
- **Défilement** : aucun (contenu tient en hauteur ; en fenêtre courte, pochette réduit à 62 %, contrôles conservés).
- **États** : sans pochette (placeholder `surface2` + icône `album` 64 dp `trait`), erreur lecture (icône + « Impossible de lire ce fichier » + suivant), pause (icône pause, égaliseur figé § anim 9).
- **Back** : geste/bouton → retour mini-player avec élément partagé inverse (`§ anim 1`), pas de fermeture de la lecture.
- **Tablette/paysage** : deux colonnes — pochette + contrôles à gauche (520 dp), file/métadonnées à droite ; ou pochette 55 % + panneau « Suivant dans la file ».
- **Animations** : § anim 1 (transition), § anim 2 (play/pause), § anim 3 (changement piste), § anim 4 (progression), § anim 7 (haptique), § anim 8 (favori), § anim 9 (égaliseur).

## 3. File d'attente

**Hiérarchie** : titre « File d'attente » + total → liste réordonnable → footer actions.

```
┌──────────────────────────────────────┐
│  File d'attente          [Effacer]   │  ← Titre 22/28, action Micro accent
│  24 titres · 1 h 32 min              │  ← Légende onBgSecondary
│  ────────────────────────────────    │
│  ≡  Pochette  Titre          3:12    │  ← ligne 64 dp : drag handle 24 dp, pochette 48 R 12
│  ≡  Pochette  Titre          4:05    │
│  …                                   │
├──────────────────────────────────────┤
│  [+ Ajouter des titres]              │  ← bouton secondaire pleine largeur (40 dp)
└──────────────────────────────────────┘
```

- **Réordonnancement** : `drag_handle` à gauche, drag-and-drop Compose (`reorderable`, déjà retenu dans l'audit — un seul mécanisme) ; ligne en cours de drag : `surface3` + élévation tonale (surface3), autres lignes se décalent (`animateItemPlacement`, ressort, 240 ms).
- **En cours de lecture** : 1ʳᵉ ligne marquée « En lecture » (Micro accent + point 6 dp), pas déplacable sans confirmation.
- **Clear** : confirmation sheet (« Vider la file ? La lecture s'arrêtera. ») — destructif rouge accessible.
- **États** : file vide (illustration + « Votre file est vide » + bouton « Lire la bibliothèque »), erreur, chargement métadonnées (skeleton lignes).
- **Défilement** : `LazyColumn`, en-tête sticky ; swipe-horizontal sur ligne = retrait (avec undo 5 s inline, § PM) ; drag = réordonner.
- **Tablette** : panneau latéral droit (400 dp) depuis le lecteur au lieu d'écran dédié.
- **Animations** : § anim 5 (entrée), réordonnancement (placement spring), § anim 7 (haptique au drop).

## 4. Détail album / artiste / playlist

**Hiérarchie** : en-tête (grande pochette + métadonnées) → actions (Lecture / Aléatoire) → liste de titres → (artiste : sections Albums / Titres).

```
┌──────────────────────────────────────┐
│  ← retour                            │  ← icône 24 dp, 44 dp touchable
│        ┌────────────┐                │  ← pochette 160 dp, R 12, centrée ou M 20
│        │  ALBUM     │                │
│        └────────────┘                │
│  Album — Artiste                     │  ← Titre 22/28
│  2023 · 12 titres · FLAC            │  ← Légende onBgSecondary
│  ┌───────────┐ ┌───────────┐        │  ← [▶ Lecture]  cercle-ish rect R 8, accent
│  │ ▶ Lecture │ │ ⇄ Aléa    │        │  ← [Aléatoire] surface2
│  └───────────┘ └───────────┘        │
│  ────────────────────────────────    │
│  1   Titre de la piste     3:45   ⋮  │  ← lignes 56 dp, M 20
│  2   …                                │
└──────────────────────────────────────┘
```

- **En-tête artiste** : nom Display 32 + « N albums · M titres » ; sections « Albums » (grille 2 col) puis « Titres » (liste, top 10 + « Tout voir »).
- **En-tête playlist** : nom, description (1 ligne, ellipsis), nombre de titres + durée ; bouton « Modifier » (renommer/supprimer) en Micro accent, visible en mode édition uniquement.
- **Boutons** : « Lecture » = bouton principal (accent plein, 44 dp, R 8 — cercle réservé au transport), « Aléatoire » = bouton secondaire surface2.
- **Liste** : numérotée (micro onBgMuted), pochette optionnelle 40 dp (artiste), `more_vert` 44 dp touchable par ligne.
- **État vide** (playlist sans titres) : illustration + « Playlist vide » + « Ajouter depuis la bibliothèque ».
- **Erreurs** : fichier manquant → ligne grisée + icône `folder_off`, skip auto à la lecture.
- **Tablette/paysage** : master-detail — en-tête à gauche (420 dp), liste à droite.
- **Animations** : § anim 3 (tap piste → lecture), § anim 5 (liste), § anim 6 (retour), § anim 7.

## 5. Recherche

**Hiérarchie** : champ (focus immédiat) → résultats groupés (Titres / Albums / Artistes) → état vide soigné.

```
┌──────────────────────────────────────┐
│  ‹ [ 🔍 Rechercher…               ✕ ]│  ← champ 48 dp, surface2, R 8, M 20 ; focus auto
│  Titres                              │  ← en-tête section Micro onBgMuted
│  Pochette  Titre                     │  ← lignes 56 dp
│  Albums                              │
│  ┌────┐ Album — Artiste              │  ← tuiles horizontales 72 dp pochette R 8
│  Artistes                            │
│  Artiste (12 albums)                 │
│                                      │
│  Aucun résultat pour « xyzzy »       │  ← état vide : illustration + suggestion
│  Essayez un titre, un artiste…       │
└──────────────────────────────────────┘
```

- **Champ** : autofocus, clavier `TextField` Compose, recherche **locale** (Room/FTS), debounce 120 ms, résultats < 150 ms sur 5 000 titres (critère PM), accentuation des termes en gras (`search` surlignage `accent` 20 % opaque — pas d'alpha décorative, couleur tonale dédiée `surface3`).
- **Groupes** : en-têtes Micro MAJUSCULES tracking +0,5, sticky ; maximum 6 titres + « Voir tout dans Titres ».
- **États** : vide initial (illustration + « Cherchez dans votre bibliothèque »), aucun résultat (suggestion : vérifier l'orthographe / réindexer), chargement (skeleton ×4), erreur DB (message + « Réindexer la bibliothèque »).
- **Défilement** : sticky headers, close clavier au scroll ; historique local (20 derniers) affiché au focus vide (stockage local uniquement).
- **Tablette** : champ + résultats côte à côte (master-detail 420 dp).
- **Animations** : § anim 5 (résultats), § anim 6 (clavier show/hide 220 ms).

## 6. Paramètres

**Hiérarchie** : 3 sections simples — Apparence · Lecture · À propos.

```
┌──────────────────────────────────────┐
│  Réglages                            │  ← Titre 22/28
│  ────────────────────────────────    │
│  Apparence                           │  ← En-tête section Micro onBgMuted
│  Thème               [ Sombre ▾ ]    │  ← ligne 52 dp : label + valeur + chevron
│  Accent dynamique    [ (•) ]         │  ← switch : extraction pochette
│  Lecture                             │
│  Fondu enchaîné      [ 0,0 s — 12 s] │  ← slider step 0/2/6/12 s
│  Saut de silence     [   ]           │  ← switch
│  À propos                            │
│  Version             1.0.0 (100)     │  ← non cliquable
│  Licences open source        [ › ]   │  ← écran About (aboutlibraries)
│  À propos de B Music          [ › ]  │  ← copyright, GPL-3.0, RetroMusic/Booming
└──────────────────────────────────────┘
```

- **Thème** : Sombre (défaut) / Clair / Système — sheet de choix, preview instantanée.
- **Accent dynamique** : switch on/off ; si on, la prochaine pochette applique l'accent aux éléments autorisés (§ 01 §3) ; si off, retour `#E8862E`.
- **Fondu enchaîné** : crossfade entre pistes (0–12 s) ; **saut de silence** : skip < 1,5 s de silence tête de piste (option, désactivé par défaut).
- **À propos** : licences (aboutlibraries — obligation GPL-3.0), version + build, copyright « © RetroMusic / Booming Music / B Music », lien dépôt (offline : affiché, non cliquable sans réseau… v1 : bouton « Copier l'URL »).
- **Pas** de télémétrie, pas de compte, pas de don — écran identique en clair.
- **États** : choix thème appliqué immédiatement (persistance DataStore), aucun état vide.
- **Tablette** : deux colonnes (nav gauche / contenu droite) au-delà de 840 dp.
- **Animations** : § anim 6 (navigation), switch (thumb slide spring 0.7/400, 200 ms).

## 7. Mini-player

**Hiérarchie** : pochette mini → titre → contrôles (play/pause) → (suivant).

```
┌──────────────────────────────────────┐  ← séparateur 1 px trait au-dessus
│ ▣  Titre de la piste        ▶/❚❚  ⏭ │  ← 64 dp, surface1 opaque
│    Artiste                           │  ← pochette 48 dp R 12, M 8/20
└──────────────────────────────────────┘
```

- **Dimensions** : hauteur **64 dp** + 1 px séparateur ; pochette **48 dp** R 12 ; titre Corps Medium 1 ligne ; artiste Micro/Légende onBgMuted ; boutons 40 dp touchables (play/pause principal, suivant secondaire) ; tap corps → plein écran ; swipe vertical ↑ → plein écran ; swipe horizontal → piste (avec § anim 1 partagée).
- **Progression fine** : ligne de 2 dp `accent` en surbrillance basse du mini-player (subtile, opaque, pas de hauteur).
- **Masqué** quand onboarding/permission refusée → CTA « Autoriser l'accès audio » à la place (états Accueil).
- **Accessibilité** : `semantics { contentDescription = "Lecture : titre, artiste — double-tap pour ouvrir le lecteur" }`.
- **Animations** : § anim 1 (mini→plein), § anim 2 (play/pause), § anim 4 (progression fine).

## 8. Notification média et écran de verrouillage

**Style système** (MediaStyle) — B Music n'habille pas la notification, elle fournit : MediaSession active, `MediaDescription` complète (titre, artiste, album, durée, pochette).

```
┌──────────────────────────────────────┐
│ [pochette 48] Titre                 │  ← petit format (une ligne) ou grand (MediaStyle)
│               Artiste — Album        │
│  ⏮        ▶/❚❚        ⏭              │  ← 3 actions : précédent · lecture/pause · suivant
└──────────────────────────────────────┘
Écran verrouillé : identique + pochette centrée 96 dp (petit format réduit), actions 3, tap → déverrouillage app.
```

- **Actions** : Précédent · Lecture/Pause · Suivant (`PlaybackStateCompat`, `MediaButtonReceiver`) ; pochette : `METADATA_KEY_ALBUM_ART` (bitmap 512², cache borné — budget mémoire).
- **Grand format** (déplié) : 3 actions larges + temps écoulé/durée Micro.
- **Verrouillage** : `VISIBILITY_PUBLIC`, pochette visible, pas de métadonnées sensibles au-delà du titre (pas de chemin de fichier).
- **Couleurs** : thème du système (l'OS contrôle le rendu) ; B Music ne force pas de couleur ; pochette + accent dynamique ignorés ici (leçon : laisser l'OS).
- **États** : pause (actions reflètent l'état, icône pause), fin de file (notification retirée, service arrêté), erreur (notification d'erreur courte 3 s, ou silencieuse + logs).
- **Animations** : système (l'OS gère) ; aucun traitement custom.
- **Exigence** : fonctionnement 100 % offline — aucune pochette réseau, pochette embarquée/cache uniquement.

---

## Adaptations tablette / paysage (résumé)

| Écran | < 600 dp | 600–839 dp | ≥ 840 dp |
|---|---|---|---|
| Accueil | grille 2 col | grille 3 col | master-detail (nav rail + grille 4 col) |
| Lecteur | vertical | vertical, pochette 62 % | deux colonnes (pochette + métadonnées) |
| File | plein écran | plein écran | panneau latéral du lecteur |
| Détail | vertical | vertical | en-tête gauche / liste droite |
| Recherche | plein écran | plein écran | master-detail 420 dp |
| Réglages | liste simple | liste simple | deux colonnes |
| Mini-player | 64 dp bas | 64 dp bas | 72 dp, master-detail possible |

Navigation : barre basse < 600 dp ; **navigation rail** (80 dp, icônes 24 dp) ≥ 840 dp.
Paysage : hauteur réduite → pochette lecteur 62 %, respiration 16 dp, contrôles conservés ; aucun scroll sur lecteur (tout tient).

## Règles transverses

- Chaque écran a **une** idée forte (§ 01 §2) — ne jamais empiler deux CTA accent.
- États **vide/chargement/erreur** obligatoires pour tout écran de données (§ ci-dessus).
- FR par défaut ; toutes les chaînes en `res/values[-fr]/strings.xml`.
- Contraste AA minimum partout ; focus visible (DPAD/clavier) ; talkback labels.
- Aucun écran n'introduit de composant hors `01 §6` sans mise à jour du design system.
