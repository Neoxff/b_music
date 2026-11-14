# B Music — Navigation et icônes « Explorer » / « Liens » (v1.2)

> Agent lean designer B Music. Date : 14 novembre 2026. **Phase design = lecture seule.**
> Sources : `sliding_music_panel_layout.xml` L44-45 · `MainActivity.kt` L136-209 · `AbsSlidingMusicPanelActivity.kt` L465-483 · `CategoryInfo.kt` L35-51 · `menu_bottom_navigation.xml` · `material-1.14.0.aar` (bytecode) · `app/src/main/res/drawable/` (203 vecteurs).
> Renvois : `09-audit-et-direction-artistique-v1.2.md` (audit, principes P1→P6) · `11-maquettes-explorer-et-liens.md` · `12-tokens-composants-et-motion.md` · `13-responsive-accessibilite-et-checklist.md`.

## 1. Le point dur : la barre basse est plafonnée à 6 entrées

### 1.1 Fait vérifié

| Élément | Constat | Preuve |
|---|---|---|
| Type du conteneur | `com.google.android.material.bottomnavigation.BottomNavigationView` | `app/src/main/res/layout/sliding_music_panel_layout.xml` L44-45 |
| Limite Material | `MAX_ITEM_COUNT = 6` | `javap -constants` sur `material-1.14.0.aar` → `private static final int MAX_ITEM_COUNT = 6` |
| Point de contrôle | L'ajout d'un 7ᵉ item lève une exception : `NavigationBarMenu.addInternal()` compare `menu.size() + 1` à `maxItemCount` et construit le message « Maximum number of items supported by … » | bytecode `com/google/android/material/navigation/NavigationBarMenu` |
| Contenu réel du menu | Le menu est **reconstruit à l'exécution** : `menu.clear()` puis ré-ajout des catégories de bibliothèque, puis de `nav_download` | `AbsSlidingMusicPanelActivity.kt` L465-483 |
| Plafond des catégories | `MAX_VISIBLE_CATEGORIES = 5` | `CategoryInfo.kt` L51 · `Preferences.kt` L136 |

### 1.2 Conséquence arithmétique

| Situation | Entrées dans la barre | Verdict |
|---|---|---|
| Aujourd'hui, 5 catégories visibles + téléchargement activé | 5 + 1 = **6** | **Exactement à la limite** — aucun jour de réserve. |
| Demain, + Explorer + Liens | 6 + 2 = **8** | **Dépasse la limite → `IllegalArgumentException` au montage de la barre.** |

Autrement dit : **ajouter les deux onglets ne peut pas se faire en ajoutant deux items au menu existant.** C'est une contrainte de plateforme, pas une préférence d'ergonomie, et elle doit être tranchée avant le lot L6 (qui enregistre les onglets).

## 2. Ce qui ne change pas

- Les onglets existants (catégories de bibliothèque, `nav_download`) **ne sont ni supprimés, ni déplacés, ni renommés**.
- La barre reste ancrée en bas, sur `surface`, avec un filet 1 dp au-dessus, icône 24 dp + label Micro 11/16.
- La reconstruction runtime du menu est conservée telle quelle (c'est le mécanisme qui a produit puis corrigé le NPE `7d78664f`) : toute solution retenue doit passer par `updateTabs()`, pas par un menu statique.
- L'onglet actif reste marqué par l'accent (icône + label en `primary`), l'inactif par `onSurfaceVariant`.

## 3. Trois structures conformes, avec trade-offs (arbitrage D8)

| | **A — Deux emplacements réservés** (recommandé) | B — Entrée unique « Découvrir » | C — Barre personnalisée épinglée |
|---|---|---|---|
| Principe | La barre accueille **Explorer** et **Liens** ; le nombre de catégories **visibles simultanément** passe de 5 à **3** (`MAX_VISIBLE_CATEGORIES`). | Une seule entrée « Découvrir » ouvre une feuille à deux choix (Explorer / Liens). Les 5 catégories restent visibles. | Une barre maison : 2 entrées de découverte **épinglées** + catégories de bibliothèque dans une zone **défilable horizontalement**. |
| Entrées max | 3 catégories + 2 découverte + 1 téléchargement = **6** ✅ | 5 catégories + 1 découverte + 1 téléchargement = **7** ❌ → il faudrait réduire à 4 catégories pour tenir | Illimité (hors Material) |
| Respect de la demande « deux espaces clairement accessibles » | **Oui**, deux onglets distincts | Partiellement : un onglet, deux écrans (2 taps) | Oui, mais structure non-Material |
| Impact produit | 2 catégories de bibliothèque.sortent de la barre (elles restent accessibles : le sélecteur de catégories existe déjà dans les préférences, et l'écran Bibliothèque doit exposer la liste complète) | Aucun | Remplacement d'un composant Material utilisé partout ; risque sur la visibilité conditionnelle, le « reselect → haut de page » et la reconstruction runtime |
| Coût | Faible : 1 constante + 2 items + 1 grid de catégories dans la bibliothèque | Moyen : 1 item + 1 feuille + 1 flux de navigation | Élevé : composant custom, tests de parité, accessibilité |
| Risque principal | **Régression de découvrabilité** des catégories bibliothèque | L'utilisateur doit aller dans une feuille pour Explorer | Régression de navigation sur tout le produit |

### 3.1 Recommandation

**Option A**, pour trois raisons :
1. elle **seule** respecte littéralement la demande (« deux espaces clairement accessibles depuis la navigation principale ») tout en tenant dans la limite Material ;
2. elle conserve le composant Material (donc le comportement, les tests et l'accessibilité existants) ;
3. la perte de découvrabilité des catégories est **compensable à coût quasi nul** : le produit possède déjà un sélecteur de catégories visibles (`CategoryInfoAdapter`, `Preferences.libraryCategories`) et l'écran Bibliothèque peut exposer la liste complète en une grille — un pattern déjà présent dans le produit (grille d'albums).

**Option B** est le repli si le coordinator refuse de réduire les catégories visibles : elle dégrade la promesse d'accès direct mais ne touche à rien d'existant.
**Option C** est écartée pour cette livraison : le rapport coût/risque est défavorable, et le rapport de non-régression de la navigation n'existe pas (E9 : pas de baseline).

### 3.2 Ordre des entrées (proposition)

`[Catégorie 1] [Catégorie 2] [Catégorie 3] │ [Explorer] [Liens] [Télécharger]`

- Les **positions existantes ne bougent pas** (pas de rupture du souvenir moteur) : les catégories restent à gauche.
- Les deux entrées de découverte sont **adossées à droite**, ce qui les rend固体ment identifiables comme un groupe sans introduire de séparateur (un `BottomNavigationView` ne gère pas de séparateur entre items).
- `nav_download` reste conditionnel (option `Preferences.downloaderEnabled`) et vient en dernier.
- Au-delà de 5 entrées, le libellé Micro des entrées de découverte reste lisible à 320 dp (voir `13-…` §3).

> Note d'implémentation : la barre ne permettant pas de séparateur visuel, la **distinction du groupe** repose sur l'icône (loupe vs chaîne vs icônes de bibliothèque), sur le libellé et sur le **scrim/filet de la barre**, pas sur un filet interne. C'est un choix assumé.

## 4. Icônes (DESIGN-03)

### 4.1 Explorer — loupe

| Attribut | Valeur | Source |
|---|---|---|
| Symbole | **Loupe** (cercle + manche) | Exigence du dossier |
| Fichier | **`ic_search_24dp.xml` — existe déjà, à réutiliser tel quel** | `app/src/main/res/drawable/ic_search_24dp.xml` (loupe Material Symbols) |
| Format | Vecteur rempli, viewport `960×960`, `24 dp`, `tint="?attr/colorControlNormal"` | Format réel du dépôt (E2) |
| Taille dans la barre | 24 dp | `01-design-system.md` §5 |
| États | actif : `primary` (#E8862E sombre / #A85B0B clair) ; inactif : `onSurfaceVariant` ; pressé : `primary` + `surfaceContainerHigh` ; désactivé : `outline` ; focus : anneau 2 dp `primary` + 2 dp fond, offset 2 dp | `01-design-system.md` §6 |
| Cible tactile | 48 dp (`Dimens.TouchTarget`) | `Dimens.kt` L8 |
| Label | FR « Explorer » · EN « Explore » · Micro 11/16, tracking +0,3 | `Type.kt` `labelMedium` |
| Accessibilité | `contentDescription` = libellé de l'onglet ; rôle « onglet » ; l'état actif est annoncé par le libellé, pas seulement par la couleur | `A11Y-3` |

**Risque de confusion, traité** : la loupe est déjà l'icône de la **recherche locale** dans la bibliothèque. Trois mesures le neutralisent :
1. le libellé de l'onglet est **« Explorer »**, jamais « Recherche » ;
2. le champ de l'écran porte la mention « sur YouTube » (microcopie `explore_search_hint` du PM) ;
3. l'icône **dans la barre** est la loupe, mais l'icône **dans le champ** est celle du composant « champ de recherche » documenté (20 dp) — deux usages distincts du même symbole, comme dans le reste du produit.

### 4.2 Liens — chaîne à deux maillons, l'un ouvert

| Attribut | Valeur |
|---|---|
| Symbole | **Deux anneaux** (maillons), dont **le second est ouvert** : la coupure, en haut à droite, doit rester lisible |
| Fichier | **À créer** : `ic_link_open_24dp.xml` |
| Anti-pattern | `ic_phonelink_ring_24dp.xml` **ne doit pas être réutilisé** : malgré son nom, c'est le glyphe Material « téléphone + anneau », pas une chaîne (lecture du `pathData` vérifiée) |
| Format | Vecteur **rempli** (style du dépôt), viewport `960×960`, `24 dp`, `tint="?attr/colorControlNormal"` — cohérent avec `ic_search_24dp.xml` |
| Géométrie | Deux rectangles arrondis (r≈ 110 unités,angle 45°) décalés de ~260 unités sur l'axe horizontal. L'anneau de droite est **interrompu** sur ~100 unités en haut à droite ; l'anneau de gauche est **séparé** de l'anneau de droite par un jeu visible d'au moins 40 unités, pour que les deux maillons se lisent à 24 dp |
| Contrainte de lisibilité | L'ouverture doit rester perceptible à **24 dp** (≥ 2,5 dp à l'échelle de l'icône) et à **16 dp** (≥ 1,6 dp) : en dessous, l'icône est jugée illisible et le libellé prend le relais |
| Contrôle de rendu | Vérification obligatoire : rendu à 24 dp et 16 dp, sur `surface` (#131318), `surfaceContainerHigh` (#22222B) et `primary` (#E8862E), thèmes clair et sombre. **Critère** : les deux maillons sont distinguables et l'ouverture est visible sans zoom |
| États | identiques à la loupe (actif `primary`, inactif `onSurfaceVariant`, pressé, désactivé, focus) |
| Cible tactile | 48 dp |
| Label | FR « Liens » · EN « Links » · Micro 11/16 |
| Accessibilité | `contentDescription` = « Liens » ; l'état « non pris en charge » d'une entrée est porté par **texte + icône**, jamais par la couleur seule |

### 4.3 Jeu d'icônes complémentaires des deux écrans

Toutes existent déjà dans le dépôt, aucune à créer :

| Usage | Vecteur existant |
|---|---|
| Loupe du champ | `ic_search_24dp.xml` (20 dp, `onSurfaceVariant`) |
| Effacer le champ | `ic_close_24dp.xml` (20 dp) |
| Effacer la zone de collage | `ic_clear_all_24dp.xml` |
| Coller | `ic_content_paste_24dp.xml` |
| Ouvrir à l'extérieur | `ic_open_in_new_24dp.xml` |
| Supprimer un lien | `ic_delete_24dp.xml` |
| Partager | `ic_share_24dp.xml` |
| Copier le lien | *aucun vecteur `copy` identifié* → **à créer** (`ic_content_copy_24dp.xml`), même format que les autres |

> Les glyphes Unicode `⌕`, `⛓`, `⌚`, `×` utilisés aujourd'hui dans l'écran *Télécharger* sont **remplacés** par ces vecteurs dans les écrans repris (constat E3, décision D11).

## 5. Ce que la navigation ne doit pas faire

| Interdit | Raison |
|---|---|
| Ajouter une 7ᵉ entrée à la barre existante | `IllegalArgumentException` Material (plafond 6, vérifié) |
| Remplacer la reconstruction runtime par un menu statique | Le menu dynamique est le mécanisme corrigé par `7d78664f` ; un menu statique reintroduirait le NPE |
| Supprimer, renommer ou déplacer un onglet existant | Contrainte DESIGN-03 + PM (« ne pas modifier arbitrairement les autres onglets ») |
| Ajouter une transparence, un dégradé ou une ombre à la barre | Principes P1/P4 |
| Animer la barre à chaque changement d'onglet au-delà de 300 ms | `Motion.DurationLong = 300` ; au-delà, l'interface « traîne » |
| Utiliser une icône filled pour l'état actif et une outline pour l'inactif | Le dépôt n'a qu'un style (rempli) ; le contraste d'état se joue sur la **couleur**, pas sur le style |

## 6. Critères de conformité (vérifiables)

| # | Critère | Vérification |
|---|---|---|
| N-1 | La barre ne lève jamais d'exception quel que soit le nombre de catégories visibles | Test Robolectric : `Preferences.libraryCategories` avec 5 catégories visibles + téléchargement activé + réseau activé → la barre se construit sans exception |
| N-2 | Les deux entrées existent, sont atteignables au clavier et annoncées | Test de sémantique : `contentDescription` FR/EN + rôle d'onglet |
| N-3 | La loupe est réutilisée, pas redessinée | `ic_search_24dp.xml` inchangé (diff du lot L6) |
| N-4 | L'icône de chaîne est lisible à 24 et 16 dp dans les deux thèmes | Rendu de contrôle (voir §4.2) + capture jointe au lot L6 |
| N-5 | L'état actif est porté par la couleur **et** par le libellé | Contraste `primary`/`surface` ≥ 3:1 (calculé : 6,95 sombre · 5,04 clair) + libellé Micro |
| N-6 | Les cibles font 48 dp | `Dimens.TouchTarget` + mesure sur la maquette |

## 7. Décision à escalader

**D8 — Structure de navigation.** Recommandation : **option A** (deux emplacements réservés, catégories visibles plafonnées à 3, ordre inchangé). Décideurs : `plan` + coordinator. Échéance : **avant le lot L6**, sans quoi l'implémentation des onglets bute sur une exception Material.
