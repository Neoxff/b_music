# B Music — Spécifications design : onglets « Lien » et « Historique » (v1.2)

> Agent DESIGNER — SPECIFICATION DE DESIGN (lecture seule, aucun code modifié).
> Date : 14 novembre 2026.
> Sources : `01-design-system.md` (système), `10-navigation-et-icones.md` (navigation), `12-tokens-composants-et-motion.md` (tokens), `Motion.kt` (durées/springs).
> Contraintes : pas de Liquid Glass, pas de blur, pas de verre, pas de dégradé, pas d'ombre portée. Élévation = changement de ton uniquement. Un seul accent visible par vue. Animation uniquement via `Motion.kt`. Jamais d'animation du rayon de flou.

---

## 0. Principe de fabrication

**Aucun token, aucune valeur, aucun composant nouveau** n'est introduit sans justification. Ce document **réutilise** le système existant et n'ajoute que :
- `ic_link_24dp.xml` (deux anneaux de chaîne entrelacés) — icône de l'onglet Lien
- `ic_history_24dp.xml` (horloge) — icône de l'onglet Historique

Toute la mise en page, les couleurs, la typographie, les formes et les mouvements sont issus des tokens déjà documentés.

---

## 1. Structure de navigation

### 1.1 Entrée « Télécharger » dans la barre du bas

| Attribut | Valeur | Justification |
|---|---|---|
| Localisation | Barre basse Material (`BottomNavigationView`), en dernière position conditionnelle | Déjà documenté (`10-navigation-et-icones.md` §3.2) |
| Condition d'apparition | `Preferences.DOWNLOADER_ENABLED == true` | Feature flag, flag off = comportement strictement identique à v1.0.0 |
| Icône | `ic_download_24dp` (existe déjà dans le dépôt) | Reutilisation du jeu existant |
| Label | FR « Télécharger » · EN « Download » · Micro 11/16 | Règle FR/EN |
| Cible tactile | 48 dp | `Dimens.TouchTarget` |
| États | N : `onSurfaceVariant` · P : `surfaceContainerHigh` + `primary` · D : `outline` · F : anneau 2 dp `primary` | `01-design-system.md` §6 |

> **Règle d'or** : flag OFF = aucune entrée, aucun onglet, aucun intent, aucune requête. Menu identique à la v1.0.0.

### 1.2 Barre de deux onglets en haut de l'écran « Télécharger »

| Attribut | Valeur |
|---|---|
| Localisation | En haut de l'écran, sous la barre de navigation si présente, sinon en haut de l'écran |
| Hauteur | 56 dp (cohérence avec les autres barres de l'app) |
| Fond | `surface1` (`#131318` sombre / `#FFFFFF` clair) |
| Séparateur inférieur | 1 dp `outline` (`#2A2A34` sombre / `#E4E4EA` clair) |
| Onglets | **Lien** (par défaut, à gauche) · **Historique** (à droite) |
| Icônes | `ic_link_24dp` (deux anneaux) · `ic_history_24dp` (horloge) |
| Labels | FR « Lien » / « Historique » · EN « Link » / « History » · Micro 11/16 |
| État actif | Icône + label `primary` (`#E8862E` sombre / `#A85B0B` clair) |
| État inactif | Icône + label `onSurfaceVariant` (`#B8B8C0` sombre / `#4A4A52` clair) |
| État pressé (onglet) | L'onglet pressé garde sa couleur d'état (active ou inactive) ; fond `surfaceContainerLow` (`#16161C` sombre / `#F5F5F7` clair), coins 8 dp, hauteur 40 dp — **pas** de changement de couleur d'icône |
| Indicateur de sélection | Barre 3 dp de hauteur, couleur `primary`, glisse en ressort (`SpringSnappy`) entre les onglets |
| Durée de glissement | 300 ms · `EmphasizedDecelerate` |
| Changement de contenu | Glissement court (24 dp horizontal) + fondu (`FastOutSlowIn`, 300 ms) |
| Balayage latéral | Autorisé sur la zone de contenu pour changer d'onglet |

### 1.3 Justification de l'ordre et de l'onglet par défaut

**Lien en premier (par défaut, à gauche)** est retenu pour trois raisons :

1. **Action principale** : l'écran « Télécharger » a pour but premier l'ajout d'un nouveau lien. L'utilisateur ouvre cet écran pour coller un lien, pas pour consulter l'historique (action secondaire, moins fréquente).
2. **Mémoire moteur** : l'action la plus répétée (coller → analyser → télécharger) doit être accessible en un seul tap sans changement d'onglet.
3. **Convention produit** : dans le reste de l'application, l'action de création/saisie vient toujours en premier (ex. champ de recherche en premier écran).

**Alternative rejetée** : Historique à gauche, Lien à droite. Cela placerait l'action secondaire en position primaire, ce qui augmente le nombre de taps pour l'action principale et contrevient au principe « une seule idée forte par écran » (ici : ajouter un lien).

---

## 2. Ontologie des icônes (trait 1,75 dp, 24×24 viewport)

| Nom | Symbole | Fichier | États | Taille de contrôle |
|---|---|---|---|---|
| `ic_link` | Deux anneaux de chaîne entrelacés, le second ouvert en haut à droite (coupure ≥ 2,5 dp à 24 dp) | `ic_link_24dp.xml` (à créer) | N : `onSurfaceVariant` · P : `surfaceContainerHigh` + `primary` · D : `outline` · F : anneau | 24, 32, 48 dp |
| `ic_history` | Horloge (cercle + aiguilles) | `ic_history_24dp.xml` (à créer) | Idem | 24, 32, 48 dp |
| `ic_download` | Flèche vers le bas dans un cercle | existant | Idem | 24 dp |
| `ic_paste` | Presse-papiers | `ic_content_paste_24dp.xml` (existant) | Idem | 24 dp |
| `ic_delete` | Poubelle | `ic_delete_24dp.xml` (existant) | Idem | 24 dp |
| `ic_copy_link` | Deux rectangles chevauchés | `ic_content_copy_24dp.xml` (à créer) | Idem | 24 dp |
| `ic_error` | Cercle avec point d'exclamation | existant | `error` | 24 dp |
| `ic_site_generic` | Monogramme générique (pas de logo de marque) | à créer, vecteur monochrome | `surfaceContainerHigh` | 40×40 dp (marque de plateforme) |

**Règle** : aucune icône ne reprend un logo de marque (YouTube, TikTok, etc.). Les icônes de plateforme sont des monogrammes génériques dans un conteneur `surfaceContainerHigh` (rayon 8 dp).

---

## 3. Onglet LIEN — spécification détaillée

### 3.1 État vide (initial)

Le bloc unique est centré horizontalement et verticalement dans l'espace disponible, avec les contraintes suivantes :

| Attribut | Valeur |
|---|---|
| Conteneur | Aucun fond (transparent) — l'écran reste sur `surface` |
| Largeur maximale | 560 dp |
| Marges latérales | 20 dp (toujours respectées, même sur très grand écran) |
| Centrage | Horizontal et vertical |
| Hauteur du bloc | ≤ 220 dp |
| Icône | `ic_link`, 48 dp, `onSurfaceVariant` |
| Titre | « Coller un lien » · `titleMedium` (16/24) · `onSurface` |
| Champ de saisie | Hauteur 56 dp, rayon 28 dp (forme « pilule »), fond `surfaceContainer` (`#1A1A21` sombre) |
| Contenu du champ | Icône `search` 20 dp à gauche (gouttière 12 dp) · texte `bodyLarge` (16/24) · bouton « Coller » à droite (texte `primary`, cible 48 dp) · bouton « effacer » (icône `close` 20 dp) si texte présent |
| Bouton principal | « Analyser », fond `primary`, texte `onPrimary`, hauteur 40 dp, rayon 8 dp, padding 16×12, cible 48 dp minimum |
| Phrase courte | Sous le champ, `bodySmall` (13/18) · `onSurfaceVariant` |
| Rangée de puces | Sous la phrase, puces des sites pris en charge (10 max visibles, défilable horizontalement au-delà) |
| Puce de site | Conteneur `surfaceContainerHigh`, rayon 8 dp, hauteur 24 dp, padding 8 dp horizontal, monogramme `labelMedium` `onSurfaceVariant` + point de statut (pris en charge : `primary` / partiel : `onSurfaceVariant`) |

**Avec clavier ouvert** : le bloc reste visible grâce aux insets (`imePadding`). Il monte juste assez pour ne pas être recouvert par le clavier (pas de recouvrement). La hauteur du clavier est soustraite de l'espace de centrage.

### 3.2 États de saisie et validation

| État | Visuel | Action |
|---|---|---|
| Vide | Bloc vide comme ci-dessus | Aucune |
| Texte saisi | Champ rempli, bouton « Analyser » activé | Taper « Analyser » lance la validation |
| Validation en cours | Bouton « Analyser » passe en état Loading (bloc texte 60% de largeur, pas de spinner) | Aucune interaction sur le bouton |
| Lien invalide | Message sous le champ en `error` (`#FFB4AB` sombre / `#BA1A1A` clair) + bouton « Réessayer » | L'utilisateur corrige le lien |
| Site non pris en charge | Message « Site non pris en charge » + bouton « Voir les sites » | L'utilisateur consulte la liste |
| Connexion requise | Message « Connexion requise » + bouton « Réessayer » | L'utilisateur réessaie |
| Contenu protégé / privé | Message « Contenu protégé » (aucune action) | Aucune |
| Moteur à mettre à jour | Message « Moteur à mettre à jour » + bouton « Ouvrir le journal » | L'utilisateur ouvre le journal |
| Hors ligne | Message « Hors ligne » + bouton « Réessayer » | L'utilisateur réessaie |

### 3.3 Après analyse réussie

| Élément | Spécification |
|---|---|
| Remontée du bloc | Le bloc de saisie remonte en ressort (`SpringSnappy`, 0,55/700, 220 ms) vers le haut de l'écran |
| Carte d'aperçu | Apparaît dessous, fond `surface`, rayon 12 dp, séparateur 1 dp `outline` |
| Pochette | 96×96 dp (ou 160 dp sur tablette), rayon 12 dp, `Cover` |
| Titre | `titleMedium` (16/24), 2 lignes max, `ellipsis` |
| Auteur | `bodySmall` (13/18), `onSurfaceVariant` |
| Durée | `bodySmall`, `tnum` |
| Badge du site | Marque de plateforme 40×40 dp, rayon 8 dp, `surfaceContainerHigh` |
| Choix format | Audio / Vidéo (puces de filtre, `surface2`, rayon 8 dp, hauteur 32 dp) |
| Qualité | Liste déroulante ou puces (selon disponibilité moteur) |
| Bouton « Télécharger » | Fond `primary`, texte `onPrimary`, hauteur 40 dp, rayon 8 dp |

### 3.4 États multiples

| État | Spécification |
|---|---|
| Plusieurs liens détectés | Liste à cases à cocher (une par lien détecté dans le texte collé, 10 max) + bouton « Tout télécharger » en bas |
| Doublon détecté | Message « Lien déjà dans l'historique » + action « Voir dans l'historique » |

---

## 4. Onglet HISTORIQUE — spécification détaillée

### 4.1 En-tête

| Élément | Spécification |
|---|---|
| Champ de filtre | Hauteur 56 dp, rayon 28 dp, fond `surfaceContainer`, icône `search` 20 dp `onSurfaceVariant` à gauche, texte `bodyLarge` |
| Puces de filtre | Sous le champ : puces « Site » et « État » (forme de puce de filtre §2.19 de `12-tokens-…`) |
| Bouton « Tout effacer l'historique » | Dans le menu d'options (3 points), texte `error` |

### 4.2 Liste groupée par date

| Attribut | Valeur |
|---|---|
| Groupes | « Aujourd'hui », « Hier », « Cette semaine », « Plus ancien » |
| En-têtes de groupe | Collants (sticky), texte « Légende » `labelMedium` (11/16) + `onSurfaceVariant`, fond `surface1` |
| Séparateur de groupe | 1 dp `outline` avec retrait 12 dp |
| Hauteur de ligne | Constante, 88 dp |
| Apparition | Échelonnée (stagger 24 ms, max 8 éléments) via `Motion.SpringSoft` |

### 4.3 Ligne d'historique

| Élément | Spécification |
|---|---|
| Miniature locale | 96×54 dp (16:9), rayon 12 dp, `Cover`, fond `surfaceContainer` si absente |
| Titre | `titleMedium` (16/24), 2 lignes max, `ellipsis`, `onSurface` |
| Métadonnées | Site + format + taille sur 1 ligne, `bodySmall` (13/18), `onSurfaceVariant` |
| Date | `bodySmall`, `tnum`, alignée à droite |
| État | Badge texte + point de couleur (voir tableau ci-dessous) |
| Actions | Menu opaque (appui long ou icône `more_vert`) : Lire, Retélécharger, Copier le lien, Partager, Supprimer de l'historique, Supprimer le fichier (confirmation) |

### 4.4 États de ligne

| État | Visuel | Action disponible |
|---|---|---|
| Terminé | Texte « Terminé », point `primary` | Lire, Retélécharger, Copier, Partager, Supprimer |
| En cours | Barre de progression sobre (4 dp `surface3`, remplissage `primary`), texte « En cours… » | Pause, Annulation |
| En pause | Texte « En pause », point `onSurfaceVariant` | Reprise, Annulation |
| Erreur | Texte « Erreur », point `error` | Retélécharger, Voir le journal |
| Fichier supprimé | Texte « Fichier supprimé », point `onSurfaceVariant` | Retélécharger |

### 4.5 Balayage latéral (suppression)

| Élément | Spécification |
|---|---|
| Geste | Balayage vers la gauche sur une ligne |
| Feedback | La ligne se décolore, fond passe en `error` léger |
| Barre de confirmation | Apparaît à droite, texte « Annuler » en `primary`, durée 4 s |
| Annulation | La ligne revient à son état initial (fondé 220 ms) |
| Confirmation | La ligne disparaît (hauteur → 0, 220 ms), la liste se recompacte |

### 4.6 États d'écran

| État | Spécification |
|---|---|
| Vide | Illustration sobre (icône `ic_history` 48 dp `onSurfaceVariant`), titre « Aucun téléchargement pour l'instant » `titleMedium`, bouton « Coller un lien » `primary` qui bascule vers l'onglet Lien |
| Chargement | Squelette (bloc `surfaceContainer`, shimmer 1200 ms sur opacité, exception fonctionnelle admise) |
| Erreur | Bloc d'état centré : icône `ic_error` 24 dp `error`, titre, message, bouton « Réessayer » |
| Très longue liste | Défilement normal, LazyColumn avec clés stables, hauteur de ligne constante |

---

## 5. Accessibilité et lisibilité

| Attribut | Valeur |
|---|---|
| Contraste texte primaire | ≥ 4,5:1 sur `surface` (mesuré : 17,5:1 sombre, 16,5:1 clair) |
| Contraste texte secondaire | ≥ 4,5:1 (mesuré : 9,9:1 sombre, 8,4:1 clair) |
| Cible tactile | ≥ 44 × 44 dp (toutes les cibles), 48 dp pour les boutons principaux |
| Focus clavier/DPAD | Anneau 2 dp `primary` + 2 dp fond, offset 2 dp |
| TalkBack | Annonce de l'état et de la progression pour chaque ligne d'historique ; annonce du statut de chargement dans l'onglet Lien |
|contentDescription | Toutes les icônes ont un `contentDescription` FR/EN |
| Mouvement réduit | `Motion.scaleDuration(ms, reduced)` → 0, squelettes statiques, fondus ≤ 150 ms |

---

## 6. Clair / sombre

Tous les écrans existent en deux thèmes. Les valeurs exactes sont dans `12-tokens-composants-et-motion.md` §1.1. Aucun aspect vitreux (pas de `copy(alpha)` décoratif, pas de `RenderEffect.blur`).

---

## 7. Paysage et tablette

| Attribut | Valeur |
|---|---|
| Centrage | Même centrage horizontal et vertical du bloc de saisie |
| Largeur maximale | 560 dp (plaflonné, même sur grand écran) |
| Paysage | Même mise en page, le bloc reste centré, la largeur est plafonnée à 560 dp |
| Tablette | Même centrage, largeur plafonnée à 560 dp, marges 20 dp respectées |
| Historique sur tablette | Même liste groupée, même hauteur de ligne, même centrage de la miniature |

---

## 8. Animations (tokens `Motion.kt`)

| Moment | Animation | Durée / courbe | Réduction |
|---|---|---|---|
| Changement d'onglet (Lien ⇄ Historique) | Fondus croisé + glissement horizontal 24 dp | 300 ms · `EmphasizedDecelerate` | fondu ≤ 150 ms, sans glissement |
| Remontée de la zone de saisie | TranslationY + ressort | 220 ms · `SpringSnappy` (0,55/700) | traduction immédiate |
| Apparition de la carte d'aperçu | Alpha 0→1 + translationY 16→0 | 300 ms · `EmphasizedDecelerate` | apparition immédiate |
| Apparition échelonnée des lignes | Alpha + translationY, décalage 24 ms, 8 max | 300 ms + `StaggerMs` | apparition immédiate |
| Barre de progression | Remplissage fluide | `tweenStd` (220 ms) | mise à jour instantanée |
| Suppression par balayage | Hauteur → 0 puis disparition | 220 ms · `FastOutSlowIn` | disparition immédiate |
| État vide / erreur | Apparition du bloc d'état, fondu simple | 220 ms · `FastOutSlowIn` | immédiat |
| Squelette | Shimmer en boucle sur bloc opaque | 1200 ms | **shimmer désactivé**, bloc statique |

**Jamais** d'animation du rayon de flou. **Jamais** d'animation infinie hors squelette.

---

## 9. Textes FR/EN (strings.xml)

| Clé | FR | EN |
|---|---|---|
| `downloader_tab_link` | Lien | Link |
| `downloader_tab_history` | Historique | History |
| `downloader_link_hint` | Coller un lien | Paste a link |
| `downloader_button_paste` | Coller | Paste |
| `downloader_button_clear` | Effacer | Clear |
| `downloader_button_analyze` | Analyser | Analyze |
| `downloader_button_download` | Télécharger | Download |
| `downloader_error_invalid` | Lien invalide | Invalid link |
| `downloader_error_unsupported` | Site non pris en charge | Site not supported |
| `downloader_error_private` | Contenu protégé | Protected content |
| `downloader_error_login` | Connexion requise | Sign-in required |
| `downloader_error_offline` | Hors ligne | Offline |
| `downloader_error_engine` | Moteur à mettre à jour | Engine needs update |
| `downloader_error_duplicate` | Lien déjà dans l'historique | Link already in history |
| `downloader_retry` | Réessayer | Retry |
| `downloader_view_sites` | Voir les sites | View sites |
| `downloader_open_journal` | Ouvrir le journal | Open journal |
| `downloader_history_empty` | Aucun téléchargement pour l'instant | No downloads yet |
| `downloader_history_paste_link` | Coller un lien | Paste a link |
| `downloader_history_clear_all` | Tout effacer l'historique | Clear all history |
| `downloader_history_delete` | Supprimer de l'historique | Remove from history |
| `downloader_history_delete_file` | Supprimer le fichier | Delete file |
| `downloader_history_cancel` | Annuler | Cancel |
| `downloader_status_done` | Terminé | Done |
| `downloader_status_in_progress` | En cours… | In progress… |
| `downloader_status_paused` | En pause | Paused |
| `downloader_status_error` | Erreur | Error |
| `downloader_status_file_deleted` | Fichier supprimé | File deleted |
| `downloader_first_use` | Ne télécharge que ce que tu as le droit de télécharger. B Music ne contourne aucune protection. | Only download what you have the right to download. B Music does not circumvent any protection. |
| `a11y_tab_link` | Onglet Lien | Link tab |
| `a11y_tab_history` | Onglet Historique | History tab |
| `a11y_download_state` | État de téléchargement : {état} | Download state: {state} |
| `a11y_progress` | Progression : {pourcentage} % | Progress: {percentage} % |

---

## 10. États interactifs (normal / pressé / désactivé / focus)

Ces états s'appliquent à **tous** les éléments interactifs des deux onglets. Ils sont issus de `01-design-system.md` §6 ; aucune couleur ni durée nouvelle n'est introduite.

### 10.1 Onglets (Lien / Historique)

| État | Fond | Icône + label | Élévation |
|---|---|---|---|
| **Normal** | transparent | actif : `primary` · inactif : `onSurfaceVariant` | 0 dp |
| **Pressé** | `surfaceContainerLow` (`#16161C` sombre / `#F5F5F7` clair), coins 8 dp | **inchangé** (la couleur d'état est conservée) | 0 dp |
| **Désactivé** | transparent | `outline` (`#6E6E78` sombre / `#C4C4CC` clair), opacité 38 % | 0 dp |
| **Focus** | transparent | inchangé | anneau 2 dp `primary`, offset 2 dp |

### 10.2 Bouton principal « Analyser » / « Télécharger »

| État | Fond | Texte | Élévation |
|---|---|---|---|
| **Normal** | `primary` (`#E8862E` sombre / `#A85B0B` clair) | `onPrimary` | 0 dp |
| **Pressé** | `primary` à 88 % d'opacité | `onPrimary` | 0 dp |
| **Désactivé** | `surfaceContainerHigh` | `onSurfaceVariant` à 38 % | 0 dp |
| **Focus** | inchangé | inchangé | anneau 2 dp `primary`, offset 2 dp |

### 10.3 Champ de saisie

| État | Contour | Fond | Texte |
|---|---|---|---|
| **Normal** | 1 dp `outline` (`#2A2A34` sombre / `#E4E4EA` clair) | `surfaceContainer` (`#1A1A21` sombre) | `onSurface` |
| **Pressé** | 1 dp `outline` | inchangé | inchangé |
| **Désactivé** | 1 dp `outline` à 38 % | `surfaceContainer` à 38 % | `onSurfaceVariant` à 38 % |
| **Focus** | 2 dp `primary` | inchangé | inchangé |
| **Erreur** (lien invalide) | 1 dp `error` (`#FFB4AB` sombre / `#BA1A1A` clair) | inchangé | inchangé |

### 10.4 Boutons secondaires (« Coller », « effacer », puces, actions de ligne)

| État | Fond | Contenu |
|---|---|---|
| **Normal** | transparent | `primary` (texte) ou `onSurfaceVariant` (icône) |
| **Pressé** | `surfaceContainerHigh`, coins 8 dp | inchangé |
| **Désactivé** | transparent | 38 % d'opacité |
| **Focus** | transparent | anneau 2 dp `primary`, offset 2 dp |

### 10.5 Ligne d'historique

| État | Fond |
|---|---|
| **Normal** | `surface` (opaque — jamais de verre, cf. `13-liquid-glass-sobre.md` §1) |
| **Pressé** | `surfaceContainerLow` |
| **Désactivé** | `surface` à 38 % |
| **Focus** | `surface` + anneau 2 dp `primary`, offset 2 dp |
| **Balayée** (suppression) | `error` à 12 % |

---

## 11. Schémas ASCII

### 11.1 Onglet LIEN — portrait, thème sombre, clavier fermé (état vide)

```
┌──────────────────────────────────────────────┐  360 dp
│  Lien ▮  Historique                          │  barre d'onglets 56 dp
├──────────────────────────────────────────────┤  séparateur 1 dp outline
│                                              │
│                                              │
│                 ⛓  (48 dp)                  │
│              Coller un lien                  │  titleMedium 16/24
│                                              │
│      ┌────────────────────────────────┐      │
│      │ 🔍  lien…            [Coller] │      │  champ 56 dp · rayon 28 dp
│      └────────────────────────────────┘      │  marge 20 dp
│                                              │
│           [    Analyser    ]                 │  primary · hauteur 40 dp
│                                              │
│    Collez un lien YouTube, TikTok,           │  bodySmall 13/18
│    Instagram…                                 │
│                                              │
│   (YT) (IG) (TT) (FB) (X)  →                │  puces 24 dp
│                                              │
└──────────────────────────────────────────────┘  bloc centré H+V
                     largeur max 560 dp
```

### 11.2 Onglet LIEN — portrait, thème clair, clavier fermé (état vide)

Identique à §11.1, avec : fond `#FFFFFF`, champ `#F5F5F7`, contour `#E4E4EA`, texte `#1A1A1C`, accent `#A85B0B`.

### 11.3 Onglet LIEN — portrait, clavier ouvert

```
┌──────────────────────────────────────────────┐
│  Lien ▮  Historique                          │
├──────────────────────────────────────────────┤
│                                              │
│                 ⛓  (48 dp)                  │  ← le bloc est remonté
│              Coller un lien                  │    juste assez pour
│                                              │    ne pas être recouvert
│      ┌────────────────────────────────┐      │
│      │ 🔍  lien…            [Coller] │      │
│      └────────────────────────────────┘      │
│           [    Analyser    ]                 │
│                                              │
└──────────────────────────────────────────────┘
╔══════════════════════════════════════════════╗
║  AZERTY                                        ║  clavier (imePadding
╚══════════════════════════════════════════════╝   appliqué)
```

### 11.4 Onglet LIEN — après analyse (remontée en ressort + carte d'aperçu)

```
┌──────────────────────────────────────────────┐
│  Lien ▮  Historique                          │
├──────────────────────────────────────────────┤
│  ┌────────────────────────────────┐          │
│  │ 🔍  https://youtu.be/… [Coller]│          │  bloc remonté
│  └────────────────────────────────┘          │  en haut
│  ┌────────────────────────────────────────┐  │
│  │ ┌──────┐  Titre de la vidéo            │  │
│  │ │      │  Auteur · 3:42 · (YT)        │  │  carte d'aperçu
│  │ │ poch │  [Audio] [Vidéo]  [720p ▾]   │  │
│  │ └──────┘  [    Télécharger    ]        │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
   pochette 96 dp (rayon 12 dp)
```

### 11.5 Onglet LIEN — paysage et tablette (même centrage, largeur plafonnée)

```
Paysage (800×360 dp)              Tablette (1280×800 dp)
┌────────────────────────────────┐  ┌──────────────────────────────────────────┐
│ Lien ▮ Historique               │  │ Lien ▮ Historique                       │
├────────────────────────────────┤  ├──────────────────────────────────────────┤
│                                │  │                                          │
│      ┌──────────────────┐      │  │          ┌──────────────────┐            │
│      │ ⛓ Coller un lien │      │  │          │ ⛓ Coller un lien │            │
│      │ [    Analyser   ]│      │  │          │ [    Analyser   ]│            │
│      └──────────────────┘      │  │          └──────────────────┘            │
│           560 dp max            │  │                 560 dp max              │
└────────────────────────────────┘  └──────────────────────────────────────────┘
   bloc centré H+V                    bloc centré H+V
```

### 11.6 Onglet HISTORIQUE — portrait, thème sombre

```
┌──────────────────────────────────────────────┐
│  Lien   Historique ▮                         │
├──────────────────────────────────────────────┤
│  ┌────────────────────────────────┐          │  champ de filtre 56 dp
│  │ 🔍  Filtrer…                   │          │  rayon 28 dp
│  └────────────────────────────────┘          │
│   (YT) (IG) (TT)   [Tous ▾] [Tous ▾]         │  puces de filtre
├──────────────────────────────────────────────┤
│  AUJOURD'HUI                    (collant)    │  Légende 11/16
│  ┌────┐  Titre de la vidéo                   │
│  │min │  YouTube · MP3 · 4,2 Mo · 14:32      │  miniature 96×54 dp
│  └────┘  ● Terminé                           │
│  ┌────┐  Autre titre                         │
│  │min │  TikTok · MP4 · 12,1 Mo · 14:30      │
│  └────┘  ▓▓▓▓▓▓░░░░ 62 %  En cours…        │  barre 4 dp
│  ┌────┐  Troisième titre                     │
│  │min │  Vimeo · MP4 · 8,0 Mo · hier        │
│  └────┘  ● Fichier supprimé                 │
├──────────────────────────────────────────────┤
│  ⬇ Téléchargements                            │  barre basse
└──────────────────────────────────────────────┘
```

### 11.7 Onglet HISTORIQUE — état vide

```
┌──────────────────────────────────────────────┐
│  Lien   Historique ▮                         │
├──────────────────────────────────────────────┤
│                                              │
│                 🕐  (48 dp)                  │
│          Aucun téléchargement                │
│             pour l'instant                    │  titleMedium
│                                              │
│           [    Coller un lien   ]            │  primary
│              (bascule vers Lien)             │
│                                              │
└──────────────────────────────────────────────┘
```

### 11.8 Barre de confirmation après balayage

```
┌──────────────────────────────────────────────┐
│  ⬇ Téléchargements                            │
├──────────────────────────────────────────────┤
│  ┌────┐  Titre de la vidéo                   │
│  │min │  YouTube · MP3 · 4,2 Mo · 14:32      │
│  └────┘  ● Terminé                           │
├──────────────────────────────────────────────┤
│  Entrée supprimée       [   Annuler   ]      │  barre 48 dp
└──────────────────────────────────────────────┘
```

---

## 12. Critères de conformité (vérifiables)

| # | Critère | Vérification |
|---|---|---|
| D-1 | Aucun `copy(alpha)` décoratif, aucun `RenderEffect.blur` sur les écrans Lien/Historique | `grep -rInE 'RenderEffect\\|Modifier\\.blur\\|copy\\(alpha' app/src/main/java/com/bmusic/app/ui/screen/downloader` → vide |
| D-2 | Politique verre de la CI passe | `bash scripts/check-glass-policy.sh` → ✓ |
| D-3 | Aucun accent double dans une même vue | Revue des écrans + captures par thème |
| D-4 | Aucun `fontSize` en dur | Utilise `MaterialTheme.typography` |
| D-5 | Aucune chaîne en dur | Toutes les microcopies en `strings.xml` |
| D-6 | Cibles ≥ 48 dp | `Dimens.TouchTarget` + audit des `IconButton` |
| D-7 | Mouvement réduit = 0 ms | Test JVM sur chaque animation |
| D-8 | Bloc de saisie centré (horizontal + vertical) | Mesure au dp près sur captures |
| D-9 | Contraste AA respecté | Script `contrast_check.py` sur tous les états |

---

## 13. Compléments v1.3 (issus de la revue d'implémentation)

> Ces sections complètent le document **sans le réécrire** : elles couvrent ce que la revue
> (`12-revue-lien-historique.md`, v1.3) a relevé comme manquant ou non spécifié.

### 13.1 Icônes à créer (4 vecteurs manquants)

Viewport **24×24 dp**, trait **1,75 dp**, extrémités et jointures **arrondies**, aucun logo de marque.

| Icône | Fichier cible | Géométrie | Couleurs (N / P / D / F) | Tailles à vérifier |
|---|---|---|---|---|
| `ic_resume` | `ic_resume_24dp.xml` | triangle plein, base 8 dp, hauteur 9 dp, pointe à droite, centré (12,12) | N `onSurface` · P `primary` · D `outline` · F `primary` | 24 / 32 / 48 dp |
| `ic_copy_link` | `ic_copy_link_24dp.xml` | deux rectangles 10×12 dp décalés de 2 dp (droite en bas, gauche en haut), trait 1,75 dp | idem | 24 dp |
| `ic_paste` | `ic_paste_24dp.xml` | rectangle 14×16 dp + bandeau supérieur 8×3 dp | idem | 24 dp |
| `ic_site_generic` | `ic_site_generic_24dp.xml` | cercle Ø 20 dp **vide** (monogramme injecté par texte, pas un tracé) | `surfaceContainerHigh` + `onSurfaceVariant` | 40 dp (badge) |

**Protocole de vérification** : rendu à 24, 32 et 48 dp · thèmes clair **et** sombre · sur fond `surface` et
`surfaceContainer` · trait effectif ≥ 1,75 dp · coupure de `ic_link` ≥ 2,5 dp à 24 dp · **aucune** marque,
**aucun** glyphe de marque (le badge est un monogramme de lettre).

### 13.2 Puces des sites pris en charge et badge de détection en direct

| Élément | Spécification |
|---|---|
| Source | `SiteRegistry` (13 sites : YouTube, SoundCloud, Bandcamp, PeerTube **pris en charge** ; Vimeo, Dailymotion, TikTok, Instagram, Facebook, X, Snapchat, Reddit, Twitch **non pris en charge**) |
| Puce | hauteur 24 dp, rayon 8 dp, padding 8 dp, monogramme `labelMedium` + **point de statut** Ø 6 dp |
| Point « pris en charge » | `primary` · « partiel » `onSurfaceVariant` · « non pris en charge » `outline` |
| Ordre | sites pris en charge d'abord (ordre alphabétique), puis les autres (grisés, `onSurfaceVariant` 60 %) |
| Défilement | horizontal ; 4 puces visibles en portrait, 6 en paysage / tablette ; 13 au total |
| **Badge en direct** | à droite du champ (16 dp de marge) : nom du site + point de statut, apparaît **à partir de 8 caractères** saisis (évite le clignotement) ; disparaît si le domaine est inconnu |

### 13.3 Textes FR/EN complémentaires (clés à ajouter à `strings.xml`)

| Clé | FR | EN |
|---|---|---|
| `downloader_first_use` | Ne télécharge que ce que tu as le droit de télécharger. B Music ne contourne aucune protection. | Only download what you are allowed to download. B Music does not bypass any protection. |
| `downloader_error_protected` | Ce contenu est protégé et ne peut pas être téléchargé | This content is protected and cannot be downloaded |
| `downloader_error_storage` | Impossible d'écrire le fichier sur l'appareil | Could not write the file to the device |
| `downloader_error_notifications` | Notifications bloquées : le téléchargement continue, sans suivi | Notifications blocked: the download continues, without tracking |
| `downloader_error_generic` | Impossible de lire ce lien | Could not read this link |
| `downloader_action_notifications` | Activer les notifications | Enable notifications |
| `downloader_sites_supported` | Pris en charge | Supported |
| `downloader_sites_partial` | Partiel | Partial |
| `downloader_sites_unsupported` | Non pris en charge | Not supported |
| `downloader_multi_title` | Liens détectés | Links found |
| `downloader_multi_select_all` | Tout télécharger | Download all |
| `downloader_history_group_today` | Aujourd'hui | Today |
| `downloader_history_group_yesterday` | Hier | Yesterday |
| `downloader_history_group_week` | Cette semaine | This week |
| `downloader_history_group_older` | Plus ancien | Older |
| `downloader_history_play` | Lire | Play |
| `downloader_history_redownload` | Retélécharger | Download again |
| `downloader_history_copy_link` | Copier le lien | Copy link |
| `downloader_history_share` | Partager | Share |
| `downloader_history_file_missing` | Fichier supprimé | File deleted |
| `downloader_a11y_tab` | Onglet %1$s | %1$s tab |
| `downloader_a11y_clear` | Effacer le lien | Clear the link |
| `downloader_a11y_sites` | Sites pris en charge | Supported sites |

> Le message `downloader_first_use` s'affiche **une seule fois**, la première ouverture de l'onglet Lien
> (drapeau `downloader_first_use_shown` dans les préférences), en un bandeau de 2 lignes au-dessus du bloc.

### 13.4 Correspondance code typé → texte → action (11 codes)

| Code | Texte affiché (FR) | Action |
|---|---|---|
| `LIEN_INVALIDE` | Ce n'est pas un lien valide | **Réessayer** |
| `SITE_NON_PRIS_EN_CHARGE` | Ce site n'est pas encore pris en charge | **Voir les sites pris en charge** |
| `CONTENU_PRIVE` | Ce contenu est privé, supprimé ou inaccessible | **Réessayer** |
| `CONNEXION_REQUISE` | Ce contenu demande une connexion, qui n'est pas supportée | aucune |
| `CONTENU_PROTEGE` | Ce contenu est protégé et ne peut pas être téléchargé | aucune |
| `MOTEUR_A_METTRE_A_JOUR` | Ce site a changé : le moteur doit être mis à jour | **Voir les sites pris en charge** |
| `HORS_LIGNE` | Aucun réseau disponible | **Réessayer** |
| `ECHEC_EXTRACTION` | Impossible de lire ce lien | **Ouvrir le journal** |
| `PERMISSION_NOTIFICATIONS` | Notifications bloquées : le téléchargement continue, sans suivi | **Activer les notifications** |
| `STOCKAGE_INDISPONIBLE` | Impossible d'écrire le fichier sur l'appareil | **Ouvrir le journal** |
| `TELECHARGEMENT_ECHEC` | Le téléchargement a échoué | **Ouvrir le journal** |

Règles : une seule action par erreur · l'action est **un bouton** sous le message (jamais une icône seule) ·
le message est posé sur **fond opaque**, jamais sur le verre de la zone de saisie.

### 13.5 État « plusieurs liens » (texte collé)

| Élément | Spécification |
|---|---|
| Détection | 10 liens maximum, extraits du texte collé ; **aucun lien n'est exécuté** à la détection |
| Champ | reste **vide** ; la liste apparaît sous le bloc |
| Liste | une case à cocher par lien (case 28 dp, cible 48 dp) + domaine + extrait de 48 caractères |
| Doublon | case cochée **désactivée**, badge « Déjà en cours », action « Voir dans l'historique » |
| Pied | bouton « Tout télécharger » (`primary`), 40 dp de haut, rayon 8 dp |
| Annulation | « Effacer » efface la liste **et** le champ |

### 13.6 Historique — regroupement par date et actions

| Élément | Spécification |
|---|---|
| Groupes | **Aujourd'hui** (0–24 h) · **Hier** (24–48 h) · **Cette semaine** (2–7 j) · **Plus ancien** (> 7 j) |
| En-tête de groupe | collant, verre `Minimal` (0,78 / 0,82), 32 dp de haut, `labelMedium` |
| Ligne | hauteur **constante 72 dp**, miniature 48 dp (cache borné 20 Mo, éviction LRU), titre 1 ligne `ellipsis`, site · format · taille · état |
| Clé de composition | `key = "dl-<id>"` (**jamais** l'index) — évite la réutilisation de composition au défilement |
| 6 actions | **Lire** (passe le titre au lecteur existant, **sans modifier `playback/`**) · **Retélécharger** · **Copier le lien** · **Supprimer de l'historique** · **Supprimer le fichier** (confirmation) · **Partager** |
| Fichier disparu | état « Fichier supprimé » + action « Retélécharger » ; vérification **paresseuse à l'affichage**, jamais au démarrage |
| Téléchargements en cours | en **tête** de liste, avec progression, Pause, Reprendre, Annuler |

### 13.7 Animations complémentaires (tokens `Motion.kt`)

| Moment | Animation | Durée / courbe | Mouvement réduit |
|---|---|---|---|
| Apparition de la carte d'aperçu | glissement 8 dp + fondu | 220 ms · `SpringSoft` (0,86/380) | fondu 150 ms |
| Badge / pastille de site | fondu + échelle 0,96 → 1 | 150 ms · `FastOutSlowIn` | fondu seul |
| Apparition échelonnée des lignes | **24 ms d'écart, 8 lignes max** | 220 ms · `SpringSoft` par ligne | fondu simultané 150 ms |
| Suppression par balayage | fond suit le doigt (1:1), action révélée | 220 ms · `SpringSoft` | apparition sans glissement |
| Progression | barre mise à jour **par pas de 120 ms** (jamais d'animation continue du texte) | 120 ms · `FastOutSlowIn` | pas d'animation |
| Indicateur d'onglet | glisse en ressort | 220 ms · `SpringSnappy` (0,55/700) | glissement immédiat |
| Remontée de la zone de saisie | ressort, −120 dp | 220 ms · `SpringSnappy` | translation immédiate |

**Interdits (absolus)** : animation du rayon de flou · animation de l'opacité de surface pendant un défilement ·
`rememberInfiniteTransition` sur une surface verre.

---

## 14. Fichiers associés

| Fichier | Rôle |
|---|---|
| `docs/design/01-design-system.md` | Système de design (couleurs, typo, formes, composants) |
| `docs/design/10-navigation-et-icones.md` | Navigation et icônes (barre basse, plafond 6) |
| `docs/design/12-tokens-composants-et-motion.md` | Tokens et mouvement (durées, springs, composants) |
| `bmusic/app/src/main/java/com/bmusic/app/ui/theme/Motion.kt` | Tokens de mouvement implémentés |
| `docs/design/12-revue-lien-historique.md` | Revue d'implémentation (écarts, captures manquantes) |
| `docs/design/13-liquid-glass-sobre-global.md` | Verre global v1.3 (toute l'app) + « Rendu iPhone récent » |

---

*Spécification produite par l'agent DESIGNER. Aucun code n'est modifié par ce document. L'implémentation doit suivre ces spécifications sans les dénaturer.*
