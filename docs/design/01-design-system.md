# B Music — Design System (v1.0)

> Agent Designer B Music. Date : 6 novembre 2026.
> Direction : **sobre, premium, « à couper le souffle » par le calme.** Pas de liquid glass, pas de blur, pas de verre, pas de dégradés. La pochette d'album est le héros visuel ; tout le reste se tait.
> Cible : Android, Compose + Material 3. Thème **sombre par défaut**, clair optionnel.
> Fichiers de tokens associés : `docs/design/tokens/{Color.kt, Type.kt, Shape.kt, Dimens.kt}` (copies prêtes pour le fork BoomingMusic), maquette `design/tokens.css` (historique, accent ambre remplacé).

---

## 1. Principes

1. **Calme** — jamais plus d'un élément fort par écran. Le bruit est un bug.
2. **Contraste net** — texte primaire ≥ 4,5:1 partout, secondaire ≥ 7:1 en grand, légendes ≥ 4,5:1 (AA).
3. **Grande respiration** — marges 20 dp, sections espacées de 32/48 dp ; la hauteur est gratuite, le contenu ne l'est pas.
4. **Une seule idée forte par écran** — Accueil = la grille d'albums ; Lecteur = la pochette ; File = la file ; Recherche = le champ.
5. **La pochette est le héros** — elle seule porte de la couleur vibrante ; l'UI reste en gris neutres + **un** accent.
6. **Élévation tonale** — les surfaces montent en clarté, jamais en transparence ni en ombre. 4 niveaux : fond → 1 → 2 → 3.
7. **Aucun effet décoratif gratuit** — pas de blur, de glass, de reflets, de dégradés, d'ombres portées. Un séparateur 1 px ou un changement de ton suffisent.
8. **Mouvement signifiant** — voir `02-animations.md` ; tout mouvement a un sens physique.

---

## 2. Couleur

### 2.1 Thème sombre (défaut) — valeurs exactes

| Rôle | Hex | Usage | Contraste mesuré |
|---|---|---|---|
| `bg` | `#0B0B0E` | fond application | — |
| `surface1` | `#131318` | barre de nav, mini-player, sheets | +10 % tonal vs fond |
| `surface2` | `#1A1A21` | cartes, lignes survolées, tracks | +21 % |
| `surface3` | `#22222B` | sélection, appui (pressed) | +34 % |
| `trait` | `#2A2A34` | séparateurs **1 px** | — |
| `onBg` | `#F2F2F0` | texte primaire | **17,5:1** sur `bg` |
| `onBgSecondary` | `#B8B8C0` | texte secondaire | **9,9:1** |
| `onBgMuted` | `#8E8E98` | légendes (jamais du corps de texte interactif) | **5,6:1** |
| `onBgDisabled` | `#5A5A64` | désactivé | non-interactif |

### 2.2 Thème clair — valeurs exactes

| Rôle | Hex | Contraste mesuré |
|---|---|---|
| `bg` | `#FAFAFB` | — |
| `surface1` | `#FFFFFF` | — |
| `surface2` | `#F2F2F5` | — |
| `trait` | `#E4E4EA` | — |
| `onBg` | `#1B1B1F` | **16,5:1** |
| `onBgSecondary` | `#4A4A52` | **8,4:1** |
| `onBgMuted` | `#6E6E78` | **4,6:1** |

Élévation tonale (pas de `elevation` Compose avec ombre) : `bg → surface1 → surface2 → surface3` ; si un séparateur est utile, **1 px de `trait`** uniquement.

### 2.3 Accent de marque — « B Amber » (proposition retenue)

> **Un seul accent de marque**, décliné par thème (impossible de satisfaire AA 4,5:1 avec un seul hex sur `#0B0B0E` et `#FAFAFB` : les fenêtres de luminance ne se recouvrent pas — calcul vérifié). Même teinte, valeur ajustée par thème.

| Thème | Hex | Sur le fond | AA |
|---|---|---|---|
| Sombre (défaut) | **`#E8862E`** | `#0B0B0E` : **7,38:1** | ✅ AA normal |
| Clair | **`#A85B0B`** | `#FAFAFB` : **4,83:1** | ✅ AA normal |
| Texte sur accent sombre | `#241203` | `#E8862E` : **6,78:1** | ✅ |
| Texte sur accent clair | `#FFFFFF` | `#A85B0B` : **5,04:1** | ✅ |

Variantes pressed : `#C96A1D` (sombre), `#83460A` (clair). Aucune variante « hover » criarde ; pressed = plus foncé de ~18 %.

**Pourquoi ambre (et pas vert Spotify `#1DB954`, ni rouge Apple `#FA243C`, ni orange vif `#FF5A1F`)** : l'ambre est la couleur native du **filament et du VU-mètre** — chaud, analogue, musical ; il ne ressemble à aucun concurrent (Spotify vert, Apple rouge, YouTube rouge, SoundCloud orange) ; il tient AA sur fond noir **et** sur fond blanc avec des variantes ajustées ; il s'accorde avec des pochettes de toutes couleurs sans jamais « baver » dessus (utilisé avec parcimonie).

**Interdits d'usage de l'accent** : jamais en fond de carte, jamais pour un texte de corps, jamais sur la navigation, jamais plus d'un élément d'accent par zone visuelle.

### 2.4 Couleur dynamique (depuis la pochette — Palette API)

Accent **secondaire** extrait de la pochette, appliqué **exclusivement** à :

1. le bouton **lecture/pause** (cercle plein) ;
2. la **barre de progression** (piste + remplissage) ;
3. un **halo très discret** derrière la pochette (surface unie assombrie/éclaircie 8 %, **opaque** — jamais de blur).

Règles :

- **Teinte verrouillée** : l'extracteur prend la couleur dominante *« vibrant »* puis la **dé-sature à 45 %** et la **contraste à ≥ 4,5:1** contre le fond (sinon : fallback accent de marque `#E8862E`, jamais une couleur illisible).
- `onDynamic` calculé par luminance relative : sombre si luminance de l'accent > 0,35, sinon blanc.
- **Jamais** pour du texte, la nav, les chips, les listes. Jamais deux dynamiques simultanément (l'accent de marque ne cohabite pas avec le dynamique sur le même écran : le dynamique **remplace** l'accent sur les 3 éléments autorisés).
- Extraction via `androidx.palette:palette-ktx` (déjà présent, confirmé dans l'audit `02-audit-gaspillage.md` §1.3/1.4 — 23 refs) ; exécutée hors main thread ; résultat mis en cache par `albumId` (mémoire bornée).
- Per-pochette, jamais global fichier ; reset au changement d'album avec transition fondu 300 ms (§ animations).

### 2.5 Transparence et élévation — règles dures

- **Aucune** `Color.copy(alpha = …)` décorative. Exceptions fonctionnelles : scrim modal `bg @ 60 %` (exigence de contraste, pas décorative) ; skeleton shimmer (opacity animée sur trait opaque) ; état pressed des listes (`surface3` opaque, pas alpha).
- **Élévation = changement de ton** uniquement : `1 dp` = surface1, `3 dp` = surface2, `6 dp` = surface3 ; `trait` 1 px optionnel pour la délimitation.

---

## 3. Typographie

**Famille unique : Inter** (SIL Open Font License 1.1, libre, embarquable, variable). Poids : **400 / 500 / 600 seulement** (pas de 700, pas d'italique en v1).

| Rôle | Taille / Interlignage | Poids | Usage |
|---|---|---|---|
| Display | **32 / 40** | 600 | titre de section Accueil (« Bibliothèque »), nom d'écran vide |
| Titre | **22 / 28** | 600 | titre de piste (Lecteur), en-tête d'écran détail |
| Corps | **16 / 24** | 400 / 500 (medium pour lignes interactives) | texte courant, métadonnées longues |
| Légende | **13 / 18** | 500 | métadonnées (artiste — album), compteurs |
| Micro | **11 / 16** | 500, tracking +0,3 | labels de nav, tags, badges (ex. « FLAC ») |

- Toute occurrence de texte respecte l'interlignage ci-dessus ; pas de `fontSize` hors échelle.
- Chiffres en `tnum` pour les durées/positions (progression, minutage) — pas de jitter horizontal.
- Texte en français par défaut ; aucune chaîne en dur (cf. règles produit FR/EN).

---

## 4. Grille, espacement, formes

- **Grille de base 4 dp** : 4, 8, 12, 16, 20, **24**, 32, **48** dp.
- **Marge d'écran : 20 dp** (contenu) ; écran Lecteur : pochette centrée, mêmes marges.
- **Rayons** : **8** (inputs, chips, boutons secondaires), **12** (pochettes, cartes), **20** (sheets / feuilles d'options), **28** (lecteur plein écran), cercle plein pour les boutons principaux.
- Pochettes : **12 dp** systématiquement (mini 48 dp, grille, détail 96/160 dp).
- Boutons principaux (lecture/pause, Lecture) : **cercle plein**, jamais d'arrondi rectangulaire.
- Cible tactile ≥ **44 × 44 dp**, même pour les icônes Micro (hitSlop).

---

## 5. Icônes — jeu unique (trait 1,75 dp, coins arrondis, 24×24 viewport)

Règles : un seul auteur (jeu Material Symbols **Rounded** ou traits maison), trait constant **1,75 dp**, terminaisons arrondies, pas de remplissage mixte sauf `favorite` (plein/vide). 24 icônes nécessaires pour v1 :

`play` (plein) · `pause` (plein) · `skip_previous` · `skip_next` · `shuffle` · `repeat` · `repeat_one` · `favorite` (vide) · `favorite_filled` · `queue_music` (file) · `search` · `close` · `arrow_back` · `more_vert` (options) · `home` (bibliothèque) · `album` · `artist` (personne) · `playlist_play` · `check` · `add` · `remove` · `drag_handle` (réordonner) · `settings` · `info` · `open_in_new` (licences) · `volume_up` · `bedtime` (sommeil, si retenu) · `equalizer` (niveaux) · `folder` · `folder_off` · `delete` · `rename` · `download_done` (déjà téléchargé, offline) —
 liste complète v1 = **32 max** (au-delà = dette). Toute icône manquante est ajoutée au jeu, jamais importée d'un autre.

- Taille d'icône standard : **24 dp** ; nav bar 24 dp ; dans bouton 40/44 dp ; mini-player 24 dp.
- Couleur : `onBg` (défaut), `onBgMuted` (inactif), accent/dynamique (actif, ex. shuffle/répéter), `onAccent` (sur accent).
- `favorite_filled` en accent de marque uniquement (pas dynamique — éviter la concurrence).

---

## 6. Composants (états normal / pressé / désactivé / focus)

Notation : `N` = normal, `P` = pressé, `D` = désactivé, `F` = focus clavier/DPAD (anneau `2 dp accent + 2 dp fond`, offset 2 dp).

| Composant | Spécification |
|---|---|
| **Bouton lecture** (cercle 56/72 dp) | N : cercle plein `accent` (ou dynamique), icône `onAccent` 32 dp. P : `accentPressed`, échelle 0,94 (§ animations). D : `surface3`, icône `onBgDisabled`. F : anneau focus. Cercle **toujours** plein, jamais de dégradé. |
| **Bouton secondaire** (rect 8 dp, hauteur 40 dp, padding 16×12) | N : `surface2`, texte `onBg` (Corps Medium). P : `surface3`. D : `surface2` + texte `onBgDisabled`, non cliquable. F : anneau. Bordure 1 px `trait` optionnelle en clair. |
| **Bouton icône** (44 dp) | N : transparent, icône `onBg`. P : `surface3` cercle 50 %. D : icône `onBgDisabled`. F : anneau. |
| **Barre de progression** | Piste 4 dp `surface3`, remplissage `accent`/dynamique ; au toucher : piste **8 dp**, curseur `onAccent` 14 dp (visible 120 ms) ; chapitrage à chaque frame ; position via `MediaController` + callback (pas de polling 1 s). |
| **Slider volume** | Piste 4 dp, pouce 14 dp `accent`, ticks discrets ; désactivé si audio focus perdu → `onBgMuted`, non interactif. |
| **Ligne de titre** (liste) | 56 dp hauteur : pochette 48 dp (12 dp rayon) + Titre Corps Medium (1 ligne, ellipsis) + Légende artiste/album sur 1 ligne ; trailing icône `more_vert` 44 dp. N : transparent ; P : `surface2` (ligne entière cliquable) ; D : non applicable ; F : anneau, sélection avec `surface3`. |
| **Carte d'album** | Pochette carrée 12 dp + Titre Corps Medium (2 lignes max) + Légende artiste ; grille 2 colonnes, largeur = (écran − 40 − 12) / 2 ; P : échelle 0,98 + `surface3` ; focus anneau ; skeleton shimmer au chargement. |
| **Mini-player** | Hauteur **64 dp** opaque `surface1`, séparateur **1 px** `trait` au-dessus ; pochette **48 dp** 12 dp rayon ; titre Corps Medium 1 ligne + artiste Micro ; play/pause 40 dp + suivant 40 dp ; tap (hors boutons) → plein écran ; swipe-up même effet ; swipe horizontal → suivant/précédent (désactivé si file vide). |
| **Puce de filtre** | `surface2`, rayon 8 dp, hauteur 32 dp, texte Micro/Légende ; sélectionnée : `accent` de marque + `onAccent` texte (ou `surface3` + `onBg` si accent déjà pris par le CTA de l'écran — règle « un seul accent visible ») ; P : `surface3` ; D : `surface2` + `onBgDisabled`. |
| **Barre de navigation** | 4 onglets : Accueil · Recherche · File · Réglages ; 64 dp, `surface1`, séparateur 1 px au-dessus ; icône 24 + Micro label ; actif : icône+label `accent` (de marque), inactif `onBgMuted` ; badge sur File (nombre) Micro sur `accent`. |
| **Champ de recherche** | Hauteur 48 dp, rayon 8 dp, `surface2`, icône `search` 20 dp `onBgMuted` à gauche, clear `close` à droite si texte ; placeholder `onBgMuted` ; focus : anneau + curseur accent ; erreur : aucune (entrée libre) ; état « aucun résultat » → écran vide dédié (§ écrans). |
| **Feuille d'options** | Sheet `surface1`, rayon 20 dp en haut, poignée 32×4 dp `trait`, liste d'items 56 dp (icône + Corps + trailing optionnel), destructif en `#FF6B4A`-like rouge accessible (`#FF7A5C` sombre / `#C2410C` clair, ≥ 4,5:1), séparateur 1 px entre groupes ; P : item `surface2` ; focus anneau ; fermeture : swipe-down ou tap scrim (60 %). |

**Tous les composants** : `Modifier.semantics` (rôles, labels FR/EN), `rememberInfiniteTransition` banni en hors-écran (batterie), aucun `clickable` sans `indication` dessiné (RippleCompose `RippleTheme` opaque ou scale 0,98).

---

## 7. Mapping tokens ↔ code

| Doc | Code | Statut |
|---|---|---|
| §2 couleurs | `tokens/Color.kt` (`BMusicDarkColors`/`BMusicLightColors`, `DynamicAccent`) | prêt |
| §3 typo | `tokens/Type.kt` (`BMusicType`, `Inter`) | prêt (ajouter `res/font/inter_*.ttf` côté fork) |
| §4/§5 rayons/dims | `tokens/Shape.kt` (`BMusicShapes`), `tokens/Dimens.kt` (`BMusicDimens`) | prêt |
| États | `BMusicColors` exposés via `LocalBMusicColors` (CompositionLocal) | spécifié, impl fork |

**Écart signalé** : `design/tokens.css` (maquette, accent `#E8A33D` saturé, thème `#121212`) **est remplacé** par le présent système — la maquette reste référence de mise en page uniquement, pas de couleur.
