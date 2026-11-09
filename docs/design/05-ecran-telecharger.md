# 05 — Écran Télécharger (design spec)

> **Agent Designer**, branche `feat/dl-etape-5bis` (à tracer). Spécification UX/UI **sans code**, alignée sur le design system **existant** : `BMusicTheme` (`Color`, `Type`, `Shape`), `Motion.kt` (tokens ci-dessous). Aucun flou, aucun verre, aucune transparence décorative — `scripts/check-no-glass.sh` doit rester **VERT**.

## 0. Tokens appliqués (ne pas les redéfinir)

| Usage | Token | Valeur |
|---|---|---|
| Tween standard | `Motion.tweenStd` | `tween<Int>(DurationBase, EmphasizedDecelerate)` |
| Ressort doux | `Motion.SpringSoft` | `spring<Float>(dampingRatio=0.86f, stiffness=380f)` |
| Échelonement résultats | `Motion.StaggerMs` | `24` (≤ 8 visible), `delay = index * StaggerMs` |
| Réduction animations | `rememberReducedMotion()` | `Boolean` — si `true`, pas de glissement ni fondu |

Couleurs : uniquement `ColorScheme` du Compose Material 3 (clair/sombre). Aucune nouvelle couleur/échelle — tout vient de `BMusicTheme`. Focus visible : bordure 2 dp `primary` + 4 dp d'extérieur (pas d'ombre décorative).

## 1. Structure d'écran — UNE entrée Télécharger

**Barre de navigation du bas** : même jeu d'icônes (trait **1,75 dp**), **UNE entrée "Télécharger"** (`Favorites`-like, icône flèche vers le bas) **visible seulement si `Preferences.downloaderEnabled`** (flag OFF = invisible, tout autre comportement identique à v1.0.0).

**En-tête de l'écran** : `TopAppBar` (opaque, surface 2) + **deux onglets** (`TabRow`+`HorizontalPager`, hauteur des onglets **56 dp**, `contentDescription`) :

| Onglet | Icône + libellé | Défaut |
|---|---|---|
| **1 — Recherche** | `ic_search` (loupe) + "Recherche" | ✅ actif par défaut (flag ON) |
| **2 — Lien** | `ic_link` (chaîne) + "Lien" | inactif |

**Indicateur d'onglet actif** : trait **3 dp** en couleur d'accent (`primary`), **glisse** (`animatedTabPosition` + `SpringSoft`) d'un onglet à l'autre ; contenu qui change **glisser horizontal court + fondu** (durée et ressort pris dans `Motion.kt` : transition via `Crossfade`+`AnimatedContent` avec `tweenStd` si `!rememberReducedMotion`, glissement du `HorizontalPager` sur swipe latéral). Le balayage latéral change d'onglet.

### Шезы верхняя часть экрана (ASCII)

```
┌───────────────────────────────┐
│ ▾ Télécharger          [↓·3] │ ← TopAppBar opaque
│ ⌕ Recherche     ⛓ Lien        │ ← TabRow 56dp, indicateur 3dp primary
│ ──────────  (actif bordure)   │
├───────────────────────────────┤
│  Recherche (ou Lien) section │
│ ... (suivant l'onglet) ...     │
└───────────────────────────────┘
      ⬆ barre de navigation du bas (inchangée)
```

**Message de premier usage** (placard opaque, sous les onglets, une phrase sobre) : *« Téléchargez uniquement les contenus que vous avez le droit de télécharger ; B Music ne contourne aucune protection. »*

## 2. Onglet RECHERCHE (icône loupe)

### 2.1 Barre de recherche
- `SearchBar` **opaque**, `surface=2`, `radius=28 dp`, **hauteur 56 dp** (cibles ≥ 48 dp).
- Contenu surligné : `TrailingIconButton("×" contentDescription="Effacer")` ; **"Annuler"** (textButton) quand le clavier est ouvert (`IME` visible → barre du clavier adaptée).
- `KeyboardOptions.imeAction = Search` : touche **"Rechercher"** du clavier lance la recherche immédiatement.
- La liste reste utilisable avec clavier ouvert ; **fermeture du clavier au défilement** de la liste (`nestedScroll`).

### 2.2 Avant la saisie
- **Historique** (5–10 requêtes) : lignes simples (`ic_history` 20 dp, **horloge**), `Clock`-badge, *toucher = relancer*, **croix** (`ic_close`) à droite pour supprimer une ligne, **"Tout effacer"** (textButton, 48 dp).
- **État vide** sobre : illustration ternaire sobre + texte *"Recherchez une vidéo YouTube ou un son à télécharger"*.

### 2.3 Résultats — liste verticale (`LazyColumn`)

Chaque ligne = `SearchResultRow` :

```
[██/mini]  Title (2 lignes)                     [↓]
[112dp]    Auteur (chaîne)       Durée [03:24]  [v] bouton "Télécharger"
[à 16:9]   Vues (si ‒ disponibles)
```

- Miniature **16:9, 112 dp** de large, **rayon 12 dp**, `ContentScale.Crop`.
- **Durée** : badge **opaque** sur la miniature (bas-droit, `surface=3`, 4 dp padding).
- **Vues** si disponibles (petit texte ternaire).
- **Bouton Télécharger** (`ic_download` 20 dp) à droite, cible **48 dp**.
- **Hauteur de ligne constante** (112 dp de miniature + métas) pour défilement fluide (`LazyColumn` `key = SearchResultItem.id`).

### 2.4 Aperçu au toucher d'une ligne

Feuille d'aperçu **opaque** (`ModalBottomSheet`, élévation tonale, pas de flou) : grande miniature (16:9 100 %), titre (2 lignes), chaîne, durée, segmented control **Audio / Vidéo**, sélecteur de qualité (`FormatSelector` opaque, dropdown menu), **bouton principal "Télécharger"** (`FilledTonalButton` 56 dp, pleine largeur).

### 2.5 États complets (tous requis)

| État | Description |
|---|---|
| **Vide** | Voir §2.2 — aucune historique |
| **Saisie en cours** | Curseur visible, liste cachée, "Rechercher" sur clavier |
| **Chargement** | **Squelettes** : 6 lignes (`SearchResultRow` skeleton, opacité `colorScheme.surface` 70 %, pas de spinner agressif) |
| **Résultats** | 5–20 lignes + sentinel pour pagination |
| **Chargement page suivante** | Petite ligne de chargement en bas (ligne skeleton 48 dp, "Chargement de la suite" en `labelMedium`) |
| **Fin des résultats** | Info "Vous êtes au bout des résultats" + bouton "Recommencer" (48 dp) |
| **Aucun résultat** | `"Aucun résultat pour « … »"` + conseil *"Essayez d'autres mots-clés"* |
| **Erreur réseau** | Erreur avec bouton **"Réessayer"** (`TextButton`) |
| **Hors ligne** | `"Vous êtes hors ligne"` + pas de requête en cours |
| **Source indisponible/moteur à mettre à jour** | `"Source temporairement indisponible, moteur à mettre à jour"` |
| **Requête trop courte** | `"2 caractères minimum"` (sous le champ, `supportingText`) |

### 2.6 Clavier

Touche "Rechercher" de l'IME lance la recherche ; liste utilisable avec clavier ouvert ; fermeture du clavier au défilement de la liste.

### 2.7 ASCII — onglet Recherche (états)

```
┌─────────────────────────────────┐
│ [🔍 Demander une recherche...] ×│ 56dp, radius 28, surface2
├─────────────────────────────────┤
│ ⌚ query récente        [×]      │ ← historique (toucher=relancer)
│ ⌚ autre query          [×]      │
│        Tout effacer             │
├─────────────────────────────────┤
│ [██] Title...   🎵/▶ Dur: 03:24  [↓]
│ [██] Title...   Chaîne…         [↓]  ← 5-20 lignes, 112dp mini
│ [██ Squelette - chargement]     │
│  Chargement de la suite…        │ ← page suivante
│  Aucun résultat pour «  … »     │ ← vide
│  Erreur réseau [Réessayer]      │ ← erreur
└─────────────────────────────────┘
```

## 3. Onglet LIEN (icône chaîne)

### 3.1 Champ de lien
- Titre court **"Coller un lien"** (`titleMedium`).
- `DownloadTextField` (opaque, hauteur 56 dp) avec **bouton "Coller"** (lit le presse-papiers **sur appui** uniquement, jamais en arrière-plan) et **effacement** (`ic_close`).
- Message sous le champ listant les sites pris en charge : *"SoundCloud, Bandcamp, Vimeo, YouTube… "*.

### 3.2 Aperçu (`MediaPreviewCard`)

Après validation d'une URL acceptable (`UrlValidator` http/https uniquement), retourne une carte d'aperçu :

```
┌──────────────────────────┐
│ [Pochette 48dp] Titre 12dp│  ← MediaPreviewCard
│                 Auteur    │  ← rayon 12 dp
│                 Durée     │
│ [Audio▢|🎥Vidéo▢]       │  ← segmented control
│ [Qualité: Haute▼]        │  ← FormatSelector opaque (ExposedDropdownMenu)
│     [ Télécharger ]       │  ← bouton principal 56 dp
└──────────────────────────┘
```

- Segmented control **Audio / Vidéo** (`SegmentedButton` opaque 40 dp).
- Sélecteur de qualité via **menu déroulant OPAQUE** (`ExposedDropdownMenu` opaque, 56 dp minimum).
- **Bouton Télécharger** principal (56 dp hauteur, accent color).

### 3.3 États

| État | Description |
|---|---|
| **Vide** | Titre + champ + sites supportés |
| **Extraction en cours** | Squelette sur carte aperçu (pas de spinner) |
| **Prêt** | Carte aperçu + segmented + sélecteur + bouton Télécharger |
| **Lien invalide** | `"Lien invalide"` (rouge ternaire `error`) |
| **Source non prise en charge** | `"Source non prise en charge (Instagram/TikTok/X partiels)"` |
| **Contenu protégé ou privé** | `"Contenu protégé ou privé, non téléchargeable"` |
| **Hors ligne** | `"Vous êtes hors ligne"` |
| **Erreur moteur** | `"Erreur moteur, moteur à mettre à jour"` |
| **Doublon détecté** | `"Déjà téléchargé — Télécharger ne re-téléchargera pas automatiquement"` + bouton "Forcer" optionnel |

### 3.4 ASCII — état prêt / erreurs

```
Prêt:
[🎵|🎥]  [Qualité ▼ Opaque]  [Télécharger]

Lien invalide:
Lien invalide

Contenu protégé:
Contenu protégé ou privé, non téléchargeable

Doublon:
Déjà téléchargé — déjà dans la bibliothèque [Re-télécharger]
```

## 4. Liste des téléchargements (commune)

Accès depuis le **bouton flèche vers le bas avec compteur** (`badge count`) en haut de l'écran (à droite du TopAppBar, **48 dp**, icône `ic_download`). La liste s'ouvre en `ModalBottomSheet` (même pattern que aperçu, opaque).

Chaque ligne = `ProgressRow` :

```
┌─────────────────────────────────────────┐
│ [48dp pochette] Titre          42% [⏸] [×]│
│                 Auteur                  │
│                [████████─     ]         │ ← barre linéaire primary
└─────────────────────────────────────────┘
```

- Pochette 48 dp, titre 1 ligne, auteur ternaire.
- Barre de progression **linéaire sobre** (`LinearProgressIndicator`, primary, hauteur 4 dp).
- Pourcentage lisible par TalkBack (ex. `⏸ Pausé, 42 %`).
- Boutons pause/reprise/annuler **48 dp** minimum.

**États** :

| État | Description |
|---|---|
| **En attente** | `PendingQueue` (file FIFO, 1 simultané par défaut) |
| **En cours** | Progress barre animée `LinearProgressIndicator` 0–100 % |
| **En pause** | `⏸` avec bouton `▶ Reprendre` |
| **Terminé** | Action **"Lire"** (`PlayArrow`) → passe par `PlaybackService` ( inchangé) |
| **Erreur** | `"Échec"` + bouton **"Réessayer"** (backoff ‖ travailleur `DownloadWorker`) |

## 5. Animations

- UNIQUEMENT via les tokens de `Motion.kt` : `tweenStd`, `SpringSoft` (0.86/380), `StaggerMs` (24, max 8 visible), `rememberReducedMotion`.
- Glissement de l'indicateur d'onglet : transition directe (`animatedTabPosition` + `SpringSoft`) ; pas de nouvelle icône.
- Changement de contenu entre onglets : **glisser horizontal court + fondu** (`AnimatedContent` + `tweenStd` si `!rememberReducedMotion`).
- Balayage latéral : glisser entre `Recherche`/`Lien` (`HorizontalPager`).
- **Apparition échelonnée des résultats** : chaque ligne `SearchResultRow` délais `index * StaggerMs` (max 8 index visibles), fondu léger (opacité) si disponible, sinon pas d'animation.
- **Ouverture de la feuille d'aperçu** : `ModalBottomSheet` avec `SpringSoft` (pas de nouvel ressort).
- **Progression fluide** : `LinearProgressIndicator` `LinearSizeIndicator` 0–100 (pas d'animation `Spring` supplémentaire).
- **Transition depuis le partage Android** : `Intent.ACTION_SEND` ouvre directement l'écran avec onglet pré-rempli (URL → **Lien**, texte libre → **Recherche**), fade `tweenStd`, **jamais d'action automatique**.
- **Aucun nouveau ressort** — si un param n'est pas couvert par `SpringSoft`/`tweenStd`, on réutilise `SpringSoft` et on ne rajoute rien à la table de `Motion.kt`.

## 6. Accessibilité

- Contraste **AA** vérifié sur tous les textes (`colorScheme` garantit ≥ 4.5:1).
- Cibles tactiles **≥ 48 dp** sur chaque icône (loupe, chaîne, ×, ↓, pause, réessayer, etc.).
- `contentDescription` sur chaque icône (loupe → `"Rechercher"`, chaîne → `"Onglet Lien"`, etc.).
- Ordre de focus vertical saut (1. Barre recherche → 2. Liste → 3. Tabs → 4. Boutons d'action).
- Annonce de la progression du téléchargement (`stateDescription`), page de résumé exhaustive par ligne (`Semantics{ stateDescription = "42 %, en cours" }`).
- État des onglets lu par **TalkBack** (`TabRow` avec `Role.Tab`, `selected`).
- Option **"supprimer les animations"** (`rememberReducedMotion` suit le réglage système, + désactive transitions) : `Motion.kt` `rememberReducedMotion` lit `Animator.AnimatorDurationScale` (register) ; lisible par TalkBack et respects `prefersReducedMotion`.

## 7. Adaptations

| Contexte | Adaptation |
|---|---|
| **Thème sombre** | `BMusicTheme(dark = true)` ; bouton Toulbar opaque — pas de nouveau token |
| **Thème clair** | Idem, contraste AA vérifié |
| **Paysage** | Tabs `TabRow` 1 rangée, listed related en 2 colonnes (`LazyVerticalGrid` 2-colonnes pour `SearchResultRow` si `width > 600dp`), aperçu `BottomSheet` largeur 520 dp centrée |
| **Tablette** | Grid 2–3 colonnes pour résultats, aperçu `ModalBottomSheet` largeur 560 dp ; pas de nouveau token layout (`WindowSizeClass` adapté) |

## 8. Liste des nouveaux composants

| Composant | Rôle | États (normal / pressé / désactivé / focus) |
|---|---|---|
| `SearchBar` | Barre de recherche opaque 56 dp, 28 dp radius | `normal` (opaque surface2), `pressé` (bordure primary 1 dp), `désactivé` (opacité 0.38), `focus` (bordure 2 dp primary + outline 4 dp) — **focus visible** |
| `SearchResultRow` | Ligne de résultat 112 dp mini, titre 1–2 lignes, chaîne, durée badge, bouton Télécharger 48 dp | `normal` (clair), `pressé` (fond `primaryContainer` 8 % teinte opaque), `désactivé` (opacité 0.38), `focus` (bordure 2 dp primary) |
| `MediaPreviewCard` | Carte d'aperçu (pochette, titre, auteur, durée) | `normal` (surface1), `pressé` (surface2), `désactivé` (opacité 0.38), `focus` (primary 2 dp) |
| `DownloadTextField` | Champ opaque (surface2, radius 16 dp, hauteur 56 dp) avec boutons "Coller"/"×" | `normal` (bordure 1 dp outline), `pressé` (opacité 0.8), `désactivé` (0.38), `focus` (bordure primary 2 dp) |
| `FormatSelector` | Sélecteur de qualité opaque (`ExposedDropdownMenu` 56 dp) | `normal` (surface2), `pressé` (surface3), `désactivé` (0.38), `focus` (primary 2 dp) |
| `ProgressRow` | Ligne de téléchargement (pochette 48 dp, barre linéaire primary, % tableau TalkBack, pause/reprise/annuler 48 dp) | `normal`, `pressé` (surface2, actions opaques), `désactivé` (squelette pour attente), `focus` (bordure primary) |
| `TabsHeader` | `TabRow` avec 2 onglets (loupe/Recherche, chaîne/Lien), indicateur 3 dp primary, `contentDescription`, label 56 dp | `normal`, `recherche actif` (indicateur currelié), `lien actif`, `désactivé` (onglet désactivé 0.38), `focus` (TabRow Selected à focus visible via bordure) |

Aucun flou, aucun verre, aucune transparence décorative — tout est opaque via `surface1/2/3`.
