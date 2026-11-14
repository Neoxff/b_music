# B Music — Tokens, composants et mouvement pour « Explorer » et « Liens » (v1.2)

> Agent lean designer B Music. Date : 14 novembre 2026. **Phase design = lecture seule.**
> Sources : tokens réels (`bmusic/app/src/main/java/com/bmusic/app/ui/theme/{Color,Dimens,Shape,Type,Motion}.kt`) · composants documentés (`docs/design/01-design-system.md` §2→§6) · `bmusic/scripts/check-glass-policy.sh` · tokens CSS du prototype (`app/www/tokens.css`).
> Renvois : `09-audit-et-direction-artistique-v1.2.md` (principes P1→P6) · `10-navigation-et-icones.md` · `11-maquettes-explorer-et-liens.md` · `13-responsive-accessibilite-et-checklist.md`.

## 0. Principe de fabrication

**Aucun token, aucune valeur, aucun composant nouveau** n'est introduit sans justification. Ce document **réutilise** le système existant et n'en ajoute que trois choses, chacune vérifiable :

| Ajout | Valeur | Justification |
|---|---|---|
| `Motion.DurationCrossfade = 400` | 400 ms | Écart déjà signalé par le designer précédent (`bmusic/docs/design/08-ecarts-liquid.md` §2) : la spec animations le demande, `Motion.kt` ne l'a pas. Une seule constante, aucun impact. |
| `ic_link_open_24dp.xml` | vecteur | Icône de chaîne à deux maillons dont un ouvert (exigée par le dossier) ; absente du jeu (203 vecteurs, aucun glyphe de chaîne — `ic_phonelink_ring` est un téléphone). |
| `ic_content_copy_24dp.xml` | vecteur | Action « Copier le lien » ; aucun vecteur `copy` dans le dépôt. |

Tout le reste existe déjà : `Dimens`, `Shape`, `Type`, `Motion`, la palette M3 B Music, et le composant « champ de recherche » décrit au §6 de `01-design-system.md`.

---

## 1. Jetons mobilisés (aucune valeur arbitraire)

### 1.1 Couleurs

| Rôle | Token | Sombre | Clair | Emploi |
|---|---|---|---|---|
| Fond | `background` | `#0B0B0E` | `#FAFAFB` | Écran |
| Surface | `surface` | `#131318` | `#FFFFFF` | Liste, feuille basse |
| Surface élément | `surfaceContainer` | `#1A1A21` | `#E9E9EE` | Champ, ligne pressée, squelette |
| Surface badge | `surfaceContainerHigh` | `#22222B` | `#E4E4EA` | Marque de plateforme, badge d'état |
| Texte | `onSurface` | `#F2F2F0` | `#1B1B1F` | Titres |
| Texte secondaire | `onSurfaceVariant` | `#B8B8C0` | `#4A4A52` | Chaîne, durée, états |
| Séparateur | `outline` | `#2A2A34` | `#E4E4EA` | Filet 1 dp |
| Accent | `primary` | `#E8862E` | `#A85B0B` | Action principale unique, état actif |
| Texte sur accent | `onPrimary` | `#241203` | `#FFFFFF` | Label du bouton primaire |
| Erreur | `error` | `#FFB4AB` | `#BA1A1A` | Message d'erreur, ligne invalide |

Contrastes : **26 paires calculées, 0 échec** (texte ≥ 4,5:1 ; UI ≥ 3:1) — voir `09-audit-…` §3.2 et le script `contrast_check.py`.

**Règles**
- Aucune couleur hors de cette liste. Pas de gris « bespoke », pas de couleur de plateforme.
- Aucune transparence décorative (P1). Exceptions fonctionnelles admises par le design system : scrim de feuille modale à 60 % et shimmer de squelette sur trait opaque.
- Un seul accent visible par vue (P2).

### 1.2 Typographie

| Rôle | Style | Taille / interlignage | Emploi dans les deux écrans |
|---|---|---|---|
| Titre d'écran | `headlineLarge` | 22 / 28 | « Explorer », « Liens » |
| Titre de feuille | `headlineMedium` | 20 / 26 | Titre du contenu dans la fiche |
| Titre de ligne | `titleMedium` | 16 / 24 | Titre de résultat, titre de lien |
| Corps | `bodyLarge` | 16 / 24 | Saisie |
| Légende | `bodySmall` | 13 / 18 | Chaîne, durée, états, compteurs |
| Micro | `labelMedium` | 11 / 16 (+0,3) | Labels de navigation, badges, monogrammes |

### 1.3 Mesures, formes, espacements

| Jeton | Valeur | Emploi |
|---|---|---|
| `Dimens.ScreenMargin` | 20 dp | Marge latérale |
| `Dimens.TouchTarget` | **48 dp** | Toute cible interactive (l'implémentation est déjà conforme) |
| `Dimens.Space4/8/12/16/20/24` | 4 → 24 dp | Rythme vertical interne |
| `Shape.Small` | 8 dp | Champ de recherche, champ de collage, bouton, marque de plateforme |
| `Shape.Cover` | 12 dp | Vignette de résultat, vignette de la fiche |
| `Shape.Panel` | 16 dp | (non employé sur ces écrans) |
| `Shape.Hero` | 20 dp | Feuille basse (sommet) |
| Grille | 4 dp | Tout espacement multiple de 4 |
| Séparateur | 1 dp | `outline` |

> Écart assumé avec le prototype web : celui-ci utilise un champ de recherche en pilule (`--radius-pill`) ; le produit impose **8 dp** pour les champs (`01-design-system.md` §4). On suit le produit, pas le prototype.

### 1.4 Mouvement

| Jeton | Valeur | Emploi |
|---|---|---|
| `Motion.DurationShort` | 120 ms | Retour visuel immédiat (pression, apparition d'icône) |
| `Motion.DurationBase` | 220 ms | Apparition d'un élément, ouverture de feuille |
| `Motion.DurationLong` | 300 ms | Changement d'onglet, transition de contenu |
| `Motion.DurationXLong` | 420 ms | Transition de contenu la plus lourde (jamais sur ces écrans) |
| `Motion.DurationSkeleton` | 1200 ms | Shimmer du squelette |
| `Motion.DurationCrossfade` | **400 ms (à créer)** | Fondu entre états d'une même zone (liste → détail) |
| `Motion.StaggerMs` / `MaxStaggerItems` | 24 ms / 8 | Entrée en cascade des résultats (8 premiers éléments seulement) |
| `Motion.SpringSoft` | 0,86 / 380 | Sortie de feuille basse |
| `Motion.SpringSnappy` | 0,55 / 700 | Confirmation d'ajout d'un lien |
| `Motion.EmphasizedDecelerate` | (0,05 ; 0,7 ; 0,1 ; 1,0) | Entrées (résultats, lien ajouté) |
| `Motion.FastOutSlowIn` | (0,4 ; 0 ; 0,2 ; 1,0) | Changement de contenu dans la même zone |
| `Motion.scaleDuration(ms, reduced)` | 0 si mouvement réduit | Application systématique |

---

## 2. Composants (DESIGN-06)

Chaque composant existe ou est une **variante d'un composant existant**. États : **N** normal · **P** pressé · **D** désactivé · **F** focus · + états spécifiques.

### 2.1 Champ de recherche (composant existant, documenté §6)

| État | Spécification |
|---|---|
| N | hauteur 48 dp, rayon 8 dp, fond `surfaceContainer`, loupe 20 dp `onSurfaceVariant` à gauche (gouttière 12 dp), placeholder `onSurfaceVariant`, texte `onSurface` |
| P | aucun changement de fond (le champ n'est pas une liste) |
| D | fond `surfaceContainer`, texte et icône `outline`, non focusable |
| F | anneau 2 dp `primary` + 2 dp fond, offset 2 dp ; curseur `primary` |
| Texte présent | icône « close » 20 dp à droite (48 dp de cible), qui vide le champ |
| Erreur | **pas d'erreur dans le champ** : le message d'aide est **sous** le champ, en `error` (c'est le PM qui l'exige, `R9`) |

### 2.2 Champ de collage multi-lignes

Variante du champ ci-dessus : hauteur **96 dp** (4 lignes), alignement du texte en haut, padding 16 dp horizontal / 12 dp vertical, rayon 8 dp, fond `surfaceContainer`. Mêmes états. Un bouton « Vider » (texte) apparaît sous le champ quand il n'est pas vide.

### 2.3 Bouton primaire (plein)

| État | Spécification |
|---|---|
| N | fond `primary`, texte `onPrimary`, hauteur 40 dp, rayon 8 dp, padding 16×12, `labelLarge` 13/18 |
| P | même fond, échelle 0,98 |
| D | fond `surfaceContainer`, texte `onSurfaceVariant`, non cliquable |
| F | anneau 2 dp `primary` + 2 dp fond |
| Loading | le libellé est remplacé par un bloc texte de 60 % de largeur, **jamais** un spinner |

### 2.4 Bouton secondaire (tonal)

N : fond `surfaceContainer`, texte `onSurface`. P : fond `surfaceContainerHigh`. D : texte `outline`. F : anneau.

### 2.5 Bouton texte

N : fond transparent, texte `primary`. P : fond `surfaceContainer`. F : anneau. D : `outline`. Hauteur 48 dp (cible).

### 2.6 Bouton icône

N : transparent, icône 24 dp `onSurfaceVariant` (dans les écrans : `onSurface` pour les actions de ligne). P : cercle `surfaceContainerHigh`. D : `outline`. F : anneau. **Cible 48 dp** (`Dimens.TouchTarget`), `hitSlop` si la zone visuelle est plus petite.

### 2.7 Ligne de résultat (nouveau composant, gabarit de `Ligne de titre` §6)

Hauteur 96 dp · vignette 112×63 dp (rayon 8, `Cover`) · gouttière 16 dp · colonne titre (`titleMedium`, 2 lignes, `ellipsis`) + méta (`bodySmall`) · action « ouvrir à l'extérieur » 24 dp / 48 dp de cible. Séparateur 1 dp `outline` avec retrait 12 dp (aligné sur le texte). La ligne entière est cliquable ; l'action externe a sa propre cible.

### 2.8 Ligne de lien (nouveau composant)

Hauteur 88 dp · marque de plateforme 40×40 dp (`surfaceContainerHigh`, rayon 8, monogramme `labelMedium`) · gouttière 12 dp · colonne titre + état · 1 à 2 actions en fin de ligne. États.textuels par état (voir `11-…` §2.3).

### 2.9 Badge / monogramme de plateforme

Conteneur `surfaceContainerHigh`, rayon 8 dp, hauteur 24 dp, padding 8 dp horizontal, texte `labelMedium` `onSurfaceVariant`. **Toujours accompagné d'un texte** (l'état) — jamais une pastille de couleur seule.

### 2.10 Bloc d'état (vide / erreur)

Bloc vertical centré : icône 24 dp (`onSurfaceVariant` ou `error`), titre `titleMedium`, précision `bodySmall`, action(s). Conteneur : aucun fond (transparent) — l'écran reste sur `surface`. Hauteur ≤ 220 dp.

### 2.11 Squelette

Bloc `surfaceContainer` (pas de dégradé) + shimmer 1200 ms sur l'opacité du bloc (exception fonctionnelle admise : « skeleton shimmer »). Deux variantes : **rangée** (112×63 + 2 barres 60 % / 40 %) et **bloc d'état** (2 barres centrées 40 %).

### 2.12 Feuille basse (aperçu / fiche)

`surface`, rayon 20 dp au sommet, poignée 32×4 dp `outline`, scrim 60 % (exception admise), items de 56 dp, séparateur 1 dp. Fermeture : geste vers le bas ou tap sur le scrim. Hauteur : contenu, 320 dp maximum, défilable au-delà.

### 2.13 Toast / message éphémère

Barre `inverseSurface` / `inverseOnSurface` (tokens M3 déjà présents), rayon 8 dp, hauteur 40 dp, 4 s, sans icône. **Réservé** aux confirmations non critiques (« Lien ajouté », « Lien copié »). Les erreurs ne passent **jamais** par un toast : elles restent en place (règle R7).

### 2.14 Menu d'options

Feuille `surface`, rayon 20 dp, items 56 dp (icône 24 dp + `bodyLarge` + action finale optionnelle), séparateur 1 dp entre groupes, état pressé `surfaceContainer`. L'action destructive est `error`.

### 2.15 Séparateur

1 dp `outline`, retrait horizontal 12 dp, opacité **1,0** (jamais de `copy(alpha)` décoratif).

---

## 3. Langage de mouvement (DESIGN-07)

### 3.1 Principes

1. **Une animation = une information.** Si l'animation n'explique ni un changement d'état, ni une relation, ni une continuité, elle est supprimée.
2. **Durées courtes et cohérentes** : 120 / 220 / 300 ms, taken from `Motion`. Jamais au-delà de 420 ms sur ces écrans.
3. **Un seul groupe animé à la fois.** Les résultats qui apparaissent ne sont pas animés ligne par ligne au-delà de 8 éléments (`MaxStaggerItems`).
4. **Jamais d'animation infinie** hors squelette (qui est borné et s'arrête à l'état vide/erreur).
5. **Aucune animation ne bloque l'interaction** : les entrées sont cliquables dès le premier frame ; leTouchable ne dépend d'aucune animation.
6. **Mouvement réduit = 0 ms**, pas « plus lent » : `Motion.scaleDuration(ms, reduced)` → 0, les squelettes deviennent des blocs statiques, les ressorts deviennent des fondus de 0 ms.

### 3.2 Table des animations

| Moment | Animation | Durée / courbe | Réduction |
|---|---|---|---|
| **Changement d'onglet** (Explorer ⇄ Liens) | Fondus croisé du contenu + glissement horizontal 24 dp | 300 ms · `EmphasizedDecelerate` | fondu ≤ 150 ms, sans glissement |
| **Apparition des résultats** | Entrée par blocs : alpha 0→1 + translationY 16→0, décalage 24 ms, **8 premiers éléments seulement** | 300 ms · `EmphasizedDecelerate` + `StaggerMs` | apparition immédiate |
| **Chargement → résultats** | Le squelette se fond vers les lignes, position stable | `DurationCrossfade` 400 ms · `FastOutSlowIn` | remplacement instantané |
| **Pagination** | Les nouveaux résultats entrent comme le premier groupe (24 ms de décalage) | 300 ms | apparition immédiate |
| **Ajout d'un lien** | La rangée entre par translationY 8→0 + alpha, la zone de collage remonte de 4 dp | 220 ms · `SpringSnappy` (0,55/700) | apparition immédiate |
| **Retrait d'un lien** | La rangée s'effondre (hauteur → 0) puis disparaît, la liste se recompacte | 220 ms · `FastOutSlowIn` | disparition immédiate |
| **Changement d'état d'une ligne** (ex. invalide → valide) | Le texte d'état change en fondu, **la ligne ne se déplace pas** | 120 ms | immédiat |
| **Ouverture de la fiche / de l'aperçu** | Feuille qui monte + scrim à 60 % | 220 ms · `SpringSoft` (0,86/380) | apparition sans glissement |
| **Fermeture de la fiche** | Retour à la liste, **la position de la liste est conservée** | 220 ms · `FastOutSlowIn` | immédiat |
| **Ouverture externe** | **Aucune animation** : le système prend le relais (`ACTION_VIEW`) | — | — |
| **État vide / erreur** | Apparition du bloc d'état, fondu simple | 220 ms · `FastOutSlowIn` | immédiat |
| **Squelette** | Shimmer en boucle sur bloc opaque | 1200 ms | **shimmer désactivé**, bloc statique |

### 3.3 Ce qui n'est pas animé

- Le compteur de résultats (texte mis à jour sans transition).
- Le badge de plateforme (apparaît avec sa ligne, pas seul).
- Le bouton « Voir plus » (l'action est immédiate ; c'est la liste qui grandit).
- Les états d'erreur (le message **remplace** le contenu : pas d'animation croisée qui laisserait coexister deux messages).
- La barre de navigation (elle ne bouge pas au changement d'onglet ; seul le contenu glisse).

---

## 4. Contrôles de conformité (vérifiables)

| # | Contrôle | Commande / test |
|---|---|---|
| C-1 | Aucun flou, aucun `copy(alpha)` décoratif dans les fichiers des deux écrans | `grep -rInE 'RenderEffect\|Modifier\.blur\|copy\(alpha' app/src/main/java/com/bmusic/app/ui/screen/explore app/src/main/java/com/bmusic/app/ui/screen/links` → **vide** |
| C-2 | La politique verre de la CI passe | `bash scripts/check-glass-policy.sh` → `✓ glass-policy OK` |
| C-3 | Aucun accent double dans une même vue | Revue des 2 écrans + test de capture par écran et par thème |
| C-4 | Aucun texte hors échelle typographique | `grep -R "fontSize = " app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → aucune occurrence (on utilise `MaterialTheme.typography`) |
| C-5 | Aucune chaîne en dur | `grep -R "Text(\"" app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → **vide** (les 42 microcopies du PM sont en ressources) |
| C-6 | Aucune animation infinie hors squelette | `grep -R "rememberInfiniteTransition" app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → **une seule occurrence**, dans le squelette |
| C-7 | Durées issues des tokens | `grep -R "tween(\|spring(" app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → uniquement via `Motion.*` |
| C-8 | Mouvement réduit respecté | Test JVM : `Motion.scaleDuration(300, reduced = true)` → 0, sur chaque animation |
| C-9 | Cibles ≥ 48 dp | `Dimens.TouchTarget` + audit des `IconButton` des deux écrans |
