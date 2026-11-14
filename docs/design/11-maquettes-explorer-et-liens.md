# B Music — Maquettes des écrans « Explorer » et « Liens » (v1.2)

> Agent lean designer B Music. Date : 14 novembre 2026. **Phase design = lecture seule.**
> Sources : tokens réels (`ui/theme/Color.kt`, `Dimens.kt`, `Shape.kt`, `Type.kt`, `Motion.kt`) · composants documentés (`docs/design/01-design-system.md` §2→§6) · maquettes du dossier produit (`bmusic/docs/design/05-ecran-telecharger.md`) · spécification PM (`docs/pm/05-spec-explorer-liens.md` §7 et §8) · matrice d'états (`docs/pm/06-matrice-plateformes.md`).
> Renvois : `09-audit-et-direction-artistique-v1.2.md` · `10-navigation-et-icones.md` · `12-tokens-composants-et-motion.md` · `13-responsive-accessibilite-et-checklist.md`.

**Règles communes aux deux écrans**
- Une seule colonne, mesure de lecture plafonnée à **640 dp**, centrée au-delà.
- Marge d'écran **20 dp** (`Dimens.ScreenMargin`), grille **4 dp**.
- **Un seul accent visible par vue** (principe P2) : l'ambre est réservé à l'action principale de l'écran.
- Aucune transparence, aucun dégradé, aucune ombre (P1, P4) ; séparateurs **1 dp** `outline` (P5).
- Tout texte est un rôle typographique existant ; aucune taille hors échelle.
- Les états sont **mutuellement exclusifs** et jamais superposés à une liste obsolète (règle R8 du PM).

---

## 1. Écran Explorer (DESIGN-04)

### 1.1 Anatomie

```
┌───────────────────────────────────────────────┐  ← surface #131318 (sombre)
│  Explorer                        YouTube      │  ← headlineLarge 22/28 (onSurface)
│                                            │  ← bodySmall 13/18 (onSurfaceVariant)
│  ┌───────────────────────────────────────┐  │
│  │ ⌕  Rechercher sur YouTube          ×  │  │  ← champ 48 dp, rayon 8, surfaceContainer
│  └───────────────────────────────────────┘  │     loupe 20 dp · close 20 dp (si texte)
│                              12 résultats   │  ← bodySmall 13/18, onSurfaceVariant, à droite
├───────────────────────────────────────────────┤
│  ┌────────────┐  Titre de la vidéo sur deux  │  ← rangée 96 dp
│  │            │  lignes maximum                │    vignette 112×63 dp, rayon 8
│  │  vignette  │  Chaîne · 3:42                │    titre titleMedium 16/24 (2 lignes, ellipsis)
│  │  16:9      │                         ↗    │  ← meta bodySmall 13/18 onSurfaceVariant
│  └────────────┘                               │    ↗ ic_open_in_new 24 dp (48 dp cible)
├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤  ← filet 1 dp outline, retrait 12 dp
│  … (rangée suivante)                          │
│              Voir plus                         │  ← bouton texte, 48 dp de haut
└───────────────────────────────────────────────┘
```

### 1.2 Spécification par bloc

| Bloc | Géométrie | Couleurs | Typographie |
|---|---|---|---|
| Titre d'écran | hauteur 28 dp, retrait 20 dp | `onSurface` | `headlineLarge` 22/28 |
| Mention de source | 4 dp sous le titre | `onSurfaceVariant` | `bodySmall` 13/18 |
| Champ de recherche | hauteur **48 dp**, rayon **8 dp**, retrait 20 dp, fond `surfaceContainer` | loupe et effacement `onSurfaceVariant`, placeholder `onSurfaceVariant`, curseur `primary` | `bodyLarge` 16/24 |
| Compteur de résultats | 8 dp sous le champ, aligné à droite | `onSurfaceVariant` | `bodySmall` 13/18 |
| Rangée de résultat | hauteur **96 dp**, vignette **112×63 dp** (16:9, rayon 8), gouttière **16 dp**, texte sur 2 lignes (titre + méta) | texte `onSurface`, méta `onSurfaceVariant`, icône externe `onSurfaceVariant` | titre `titleMedium` 16/24 · méta `bodySmall` 13/18 |
| Séparateur | 1 dp `outline`, retrait **12 dp** (aligné sur le texte, pas sur la vignette) | `outline` | — |
| « Voir plus » | hauteur 48 dp, centré | `primary` | `titleMedium` 16/24 |
| Fin de liste | hauteur 48 dp, centré | `onSurfaceVariant` | `bodySmall` 13/18 |

**Pas de carte autour d'une rangée.** Un résultat est une **ligne**, pas une carte : c'est la densité attendue d'un navigateur de découverte, et cela évite l'effet « tuiles surchargées » reproché par DESIGN-04.

### 1.3 Les 7 états

| État | Rendu | Hauteur du bloc d'état |
|---|---|---|
| **Repos** (avant recherche) | Champ + ligne d'aide « Recherchez une vidéo YouTube, puis appuyez sur Entrée. » (`bodySmall`, `onSurfaceVariant`). La liste est vide, pas de grand vide blanc : l'aide occupe la bande sous le champ. | ≤ 48 dp |
| **Chargement** | **4 squelettes** de rangée : bloc 112×63 dp + 2 barres de texte (60 % puis 40 % de largeur), fond `surfaceContainer`, shimmer 1200 ms | — |
| **Résultats** | Rangées (voir §1.2) | — |
| **Vide** | Bloc centré dans la zone liste : icône 24 dp `onSurfaceVariant` + « Aucun résultat » `titleMedium` + « Essayez d'autres mots-clés. » `bodySmall` `onSurfaceVariant`. **Aucune action** (l'action naturelle est de reformuler, pas de réessayer). | ≤ 160 dp, centré |
| **Erreur** | Bloc centré : icône 24 dp `error` + message `titleMedium` (cause nommée) + `bodySmall` (précision) + bouton **primaire plein** « Réessayer » (40 dp, rayon 8) + bouton texte « Ouvrir dans YouTube ». | ≤ 220 dp, centré |
| **Pagination** | Pied de liste : « Voir plus » (bouton texte) ou « Vous êtes au bout des résultats ». En cas d'échec de pagination : message `bodySmall` `error` **sous** la liste, les résultats existants restant intacts. | 48 dp |
| **Vidéo non intégrable** | Pas d'état d'écran : la fiche de détail affiche une ligne d'information « Lecture dans YouTube » + le bouton d'ouverture externe. | — |

### 1.4 Fiche de détail d'un résultat (feuille basse)

```
┌───────────────────────────────────────────────┐  ← surface, rayon 20 dp en haut
│                 ▁▁▁▁                          │  ← poignée 32×4 dp, outline
│  ┌────────────────────┐                       │
│  │   vignette 160×90  │                       │  ← rayon 12 dp
│  └────────────────────┘                       │
│  Titre de la vidéo sur deux lignes maximum    │  ← headlineMedium 20/26
│  Nom de la chaîne · 3:42 · YouTube           │  ← bodySmall 13/18, onSurfaceVariant
│                                               │
│  ┌──────────────────────────┐   Copier le lien │  ← primaire plein 40 dp / rayon 8
│  │  Ouvrir dans YouTube     │                  │    + bouton texte
│  └──────────────────────────┘                  │
└───────────────────────────────────────────────┘
```

- Hauteur : contenu, **320 dp maximum**, aligné en bas ; au-delà de 320 dp de contenu, la feuille devient défilable.
- **Un seul accent** : le bouton primaire ; « Copier le lien » est un bouton texte `primary` — donc **deux éléments accentués dans la même vue**. Correction : « Copier le lien » est un bouton **secondaire** (`surfaceContainer`, texte `onSurface`) et seul « Ouvrir dans YouTube » porte l'accent (principe P2 appliqué).
- Aucun lecteur intégré : la feuille **ne contient pas** de zone de lecture. C'est volontaire (absence de WebView, décision D2) et c'est dit à l'utilisateur.

---

## 2. Écran Liens (DESIGN-05)

### 2.1 Anatomie

```
┌───────────────────────────────────────────────┐
│  Liens                                         │  ← headlineLarge 22/28
│  Collez un lien par ligne                      │  ← bodySmall 13/18, onSurfaceVariant
│  ┌───────────────────────────────────────┐    │
│  │ https://…                              │    │  ← champ multiline 96 dp (4 lignes)
│  │                                       │    │    rayon 8, surfaceContainer
│  └───────────────────────────────────────┘    │
│  ┌──────────────┐  ┌────────┐      Vider      │  ← Analyser (primaire) · Coller (secondaire)
│  │   Analyser   │  │ Coller │                 │    · Vider (texte, si champ non vide)
│  └──────────────┘  └────────┘                 │
│        2 liens ajoutés · 1 ligne ignorée       │  ← bodySmall, onSurfaceVariant
├───────────────────────────────────────────────┤
│  ┌────┐  Titre ou URL de la vidéo          ↗  🗑 │  ← rangée 88 dp
│  │ YT │  YouTube · aperçu disponible           │  ← marque 40×40 dp (surfaceContainerHigh)
│  └────┘                                       │
├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│  ┌────┐  Titre ou URL                      ↗  🗑 │
│  │ TT │  TikTok · non lisible dans B Music      │  ← état non pris en charge
│  └────┘                                       │
└───────────────────────────────────────────────┘
```

### 2.2 Spécification par bloc

| Bloc | Géométrie | Couleurs | Typographie |
|---|---|---|---|
| Zone de collage | hauteur **96 dp** (4 lignes), rayon **8 dp**, fond `surfaceContainer`, placeholder « https://… » | `onSurfaceVariant` | `bodyLarge` 16/24 |
| Bouton « Analyser » | primaire **plein**, hauteur 40 dp, rayon 8, padding 16×12 | fond `primary`, texte `onPrimary` | `labelLarge` 13/18 |
| Bouton « Coller » | secondaire tonal, hauteur 40 dp, rayon 8 | fond `surfaceContainer`, texte `onSurface` | `labelLarge` 13/18 |
| Bouton « Vider » | bouton texte, 48 dp de haut, visible seulement si le champ n'est pas vide | `onSurfaceVariant` | `labelLarge` 13/18 |
| Compteur | 8 dp sous les boutons | `onSurfaceVariant` | `bodySmall` 13/18 |
| Rangée de lien | hauteur **88 dp**, marque de plateforme **40×40 dp** (rayon 8, `surfaceContainerHigh`), gouttière 12 dp | marque : `onSurfaceVariant` · titre `onSurface` · état `onSurfaceVariant` | titre `titleMedium` 16/24 · état `bodySmall` 13/18 |
| Marque de plateforme | 40×40 dp : **lettre ou monogramme** (pas de logo de marque, pas de ressource distante) | fond `surfaceContainerHigh`, texte `onSurfaceVariant` | `labelMedium` 11/16 |
| Actions de fin de rangée | icône « ouvrir à l'extérieur » 24 dp + icône « supprimer » 24 dp, chacune 48 dp de cible | `onSurfaceVariant` | — |
| Séparateur | 1 dp `outline`, retrait 12 dp | `outline` | — |

### 2.3 Les états d'une entrée (la partie qui compte)

| État | Ligne | Traitement visuel |
|---|---|---|
| **Prévisualisable** | « YouTube · aperçu disponible » | État neutre ; l'icône d'ouverture externe est active |
| **Non pris en charge** | « TikTok · non lisible dans B Music » | État **neutre**, pas une erreur : fond `surfaceContainer`, icône `open_in_new` ; le libellé nomme la plateforme |
| **Plateforme non reconnue** | « Plateforme non reconnue » | Même traitement ; **aucune requête réseau** n'a été émise pour découvrir la plateforme |
| **Lien invalide** | « Lien non valide » | Seule ligne en `error` (#FFB4AB / #BA1A1A) + action « Corriger » qui replace le focus dans le champ |
| **Doublon** | « Déjà dans la liste » | État neutre + action « Ajouter quand même » |
| **Contenu restreint** | « Contenu privé ou indisponible » | État neutre ; l'ouverture externe reste disponible |
| **Aperçu en cours** | La rangée affiche un bloc de chargement 88 dp (2 barres) | Shimmer 1200 ms |

**Règle** : l'état est porté par **texte + icône**, jamais par la seule couleur. Le rouge est réservé à l'invalidation de saisie (seule situation où l'utilisateur a fait une erreur).

### 2.4 Aperçu d'un lien (feuille basse)

Même gabarit que la fiche d'Explorer, avec trois différences :
1. le titre est celui du contenu distant (ou l'URL si l'aperçu n'a pas abouti) ;
2. la ligne de méta porte « chaîne · durée » ;
3. l'action primaire est **« Ouvrir à l'extérieur »** (jamais « Lire », qui prometrait une lecture intégrée).

Si l'aperçu échoue, la feuille **ne s'ouvre pas** : l'erreur est affichée **en place**, dans la rangée (état « non pris en charge » ou « contenu restreint »). Une feuille d'aperçu vide serait exactement l'« écran vide » que le PM interdit.

### 2.5 États d'écran

| État | Rendu |
|---|---|
| **Repos / liste vide** | Bloc compact centré : icône 24 dp `onSurfaceVariant` + « Aucun lien ajouté » `titleMedium` + « Collez un lien pour voir sa plateforme et son aperçu. » `bodySmall` |
| **Links saisis** | Le compteur remplace le bloc vide ; la zone de collage reste visible en haut |
| **Traitement** | La ou les lignes concernées passent en squelette 88 dp ; le reste de la liste reste interactif |

---

## 3. Ce qui est explicitement exclu des maquettes

| Exclu | Raison |
|---|---|
| Lecteur intégré, WebView, zone de lecture | Aucun WebView dans le produit (D2) ; promesse impossible |
| Logos de plateformes (YouTube, TikTok…) | Ressources de marque tierces non distribuables ; on utilise un monogramme |
| Champ de filtres avancés | Le modèle ne connaît que `ALL` / `MUSIC` ; hors MVP (P1) |
| Historique de recherche visible par défaut | P1 ; existe déjà en interne (max 10) |
| Carte de résultat avec titre, badge, bouton et durée superposés | DESIGN-04 : pas de cartes surchargées |
| Grande zone vide sous le champ en état repos | DESIGN-04 : éviter les grandes zones vides |
| Spinner | `03-ecrans.md` : squelettes |

## 4. Traçabilité vers la spécification PM

| Élément de maquette | Règle PM | Critère PM |
|---|---|---|
| Champ unique, loupe, effacement | R2, R9 | EX-2, EX-3 |
| Ligne résultat : vignette + titre + chaîne + durée | R11 | EX-5 |
| Compteur de résultats | O3 | EX-5 |
| Squelettes | O3 | EX-4 |
| État vide | O3 | EX-6 |
| Bloc erreur + « Réessayer » + repli externe | R7, O3 | EX-7 |
| « Voir plus » + pied de liste | R14 | EX-8 |
| Fiche détail + ouverture externe + copie | R13 | EX-9, EX-10 |
| Zone de collage multiligne + « Analyser » / « Coller » / « Vider » | R15 | LI-2 |
| Badge plateforme + état textuel | R17, R20 | LI-3, LI-9 |
| Dédup + « Ajouter quand même » | R18 | LI-5 |
| Suppression + confirmation « Tout effacer » | — | LI-6 |
| Aperçu en feuille basse, sans lecteur | R19, D2 | LI-7 |
| État « non lisible dans B Music » + « Ouvrir à l'extérieur » | O6 | LI-8 |
