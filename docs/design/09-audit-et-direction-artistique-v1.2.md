# B Music — Audit visuel de l'existant et direction artistique v1.2

> Agent lean designer B Music. Date : 14 novembre 2026. **Phase design = lecture seule, aucun code ni ressource modifié.**
> Sources : implémentation Compose réelle (`bmusic/app/src/main/java/com/bmusic/app/ui/theme/`, `.../ui/screen/downloader/`, `app/src/main/res/drawable/`) · corpus design suivi (`docs/design/01-design-system.md`, `02-animations.md`, `03-ecrans.md`, `04-logo.md`) · corpus design produit (`bmusic/docs/design/05-ecran-telecharger.md`, `06-liquid-leger.md`, `07-animations-liquid.md`, `08-ecarts-liquid.md`) · `bmusic/scripts/check-glass-policy.sh` · `assets/brand/`.
> Renvois : `docs/pm/05-spec-explorer-liens.md` (spécification PM) · `docs/pm/06-matrice-plateformes.md` (états à représenter) · `docs/pm/07-criteres-acceptation-explorer-liens.md` (critères) · `docs/design/10-navigation-et-icones.md` · `11-maquettes-explorer-et-liens.md` · `12-tokens-composants-et-motion.md` · `13-responsive-accessibilite-et-checklist.md`.
> **Numérotation** : `05`→`08` sont réservés par le corpus produit (écran Télécharger, liquid-léger, animations-liquid, écarts-liquid) ; cette livraison démarre à `09`.

## 0. Méthode et règle de vérité

Chaque constat porte une étiquette : **[Vérifié]** (observé dans un fichier ou par commande), **[Déduit]** (raisonné à partir d'un fait), **[À vérifier]**, **[Bloqué]**.

Commandes de référence exécutées pour cette livraison :

| Objet | Commande | Résultat observé |
|---|---|---|
| Tokens couleur | `grep -nE "val \|const" bmusic/app/src/main/java/com/bmusic/app/ui/theme/Color.kt` | 78 lignes de couleurs Material 3 + surcouche B Music (sombre `#0B0B0E`→`#2A2A34`, clair `#FAFAFB`→`#E4E4EA`, accent `#E8862E` / `#A85B0B`) |
| Tokens de mesure | `…/theme/Dimens.kt` | `TouchTarget = 48.dp`, `ScreenMargin = 20.dp`, `Space4…Space48`, `MiniPlayerHeight = 64.dp`, `ArtworkMini = 48.dp` |
| Tokens de forme | `…/theme/Shape.kt` | `Small 8`, `Cover 12`, `Panel 16`, `Hero 20`, `HeroLarge 28`, `Circle` |
| Typographie | `…/theme/Type.kt` | Inter 400/500/600 ; 13 rôles de 11/16 à 32/40 |
| Mouvement | `…/theme/Motion.kt` | `DurationShort 120`, `Base 220`, `Long 300`, `XLong 420`, `Skeleton 1200`, `StaggerMs 24`, `MaxStaggerItems 8`, 3 ressorts, 3 courbes, `scaleDuration(reduced)=0` |
| Jeu d'icônes | `ls bmusic/app/src/main/res/drawable/ \| wc -l` | **203** vecteurs `ic_*_24dp.xml`, style Material Symbols **rempli**, viewport `960×960`, `tint="?attr/colorControlNormal"` ; **aucune dépendance `material-icons`**, aucun usage de `Icons.` |
| Icônes utiles | `ls …/drawable/ \| grep -E "search\|link\|open\|share"` | `ic_search_24dp.xml` (loupe, existe) · `ic_open_in_new_24dp.xml`, `ic_share_24dp.xml`, `ic_content_paste_24dp.xml`, `ic_clear_all_24dp.xml`, `ic_delete_24dp.xml`, `ic_close_24dp.xml` · `ic_phonelink_ring_24dp.xml` (**ce n'est pas une chaîne** : c'est le glyphe Material « téléphone + anneau ») |
| Limite Material | `javap -constants … BottomNavigationView` sur `material-1.14.0.aar` | `MAX_ITEM_COUNT = 6` ; message d'erreur « Maximum number of items supported by … » dans `NavigationBarMenu` |
| Politique verre | `bmusic/scripts/check-glass-policy.sh` | liste blanche `GlassSurface\|GlassPreBlur\|LyricsScreen\|PlayerColorScheme\|AnimatedBackground`, plafond de flou **24 dp**, motifs interdits `specular\|sheen\|shine\|glow\|refraction\|glare` |
| Contraste | `python3 contrast_check.py` (26 paires, WCAG 2.1) | **26 paires testées, 0 échec** (texte ≥ 4,5:1 ; UI ≥ 3:1) |

## 1. Audit de l'existant (DESIGN-01)

### 1.1 Forces à préserver

| # | Force | Preuve | Conséquence design |
|---|---|---|---|
| F1 | **Un accent, pas deux.** La marque tient dans un seul ambre (`primaryDark #E8862E` / `primaryLight #A85B0B`), et le design system l'exclut explicitement de toute autre surface. | `Color.kt` L41/L5 · `01-design-system.md` §2.3 | Le nouvel accent est **déjà là** : rien à créer, rien à re-teinter. |
| F2 | **Élévation par le ton, jamais par l'ombre.** Sept surfaces graduated existent, aucune ombre portée. | `Color.kt` `surfaceContainerLowest…Highest` · `01-design-system.md` §2.5 | Les deux nouveaux écrans n'ont besoin d'aucune ombre, d'aucun dégradé. |
| F3 | **Une échelle typographique complète et sans trou.** | `Type.kt` 13 rôles | Titres, lignes, métadonnées et badges ont une place assignée, sans valeur arbitraire. |
| F4 | **Un langage de mouvement déjà tokenisé et déjà « reduced-motion ».** | `Motion.kt` (`scaleDuration(ms, reduced)`) | Les animations des deux écrans s'écrivent avec les tokens existants, sans nouvelle constante. |
| F5 | **Des cibles tactiles déjà à 48 dp** — le code est plus exigeant que le corpus documentaire. | `Dimens.kt` `TouchTarget = 48.dp` (le doc §4 dit 44 dp) | L'arbitrage D6 du PM se tranche en faveur de **48 dp** : on aligne la documentation, pas le code. |
| F6 | **Une politique « zéro verre » outillée et exécutée en CI.** | `scripts/check-glass-policy.sh` + job `check` de `android.yml` L42 | Les deux écrans n'ont aucun flou à justifier : la contrainte est vérifiable automatiquement. |
| F7 | **Un jeu d'icônes cohérent et tintable** (Material Symbols rempli, une seule grille). | 203 vecteurs, `tint="?attr/colorControlNormal"` | La loupe d'Explorer **existe déjà** ; il n manque qu'une icône de chaîne. |
| F8 | **Une identité de marque déjà produite** (logo, monochrome, icône adaptative, feature graphic). | `assets/brand/` (8 fichiers) | Rien à redessiner. |

### 1.2 Incohérences, frictions et défauts constatés

| # | Constat | Gravité | Preuve | Traitement |
|---|---|---|---|---|
| **E1** | **Deux systèmes de tokens coexistent.** Le corpus `docs/design/tokens/*.kt` (package `com.bmusic.ui.theme`) documente un accent `#E8A33D`, un fond `#121212`, des cibles 44 dp — **valeurs qui ne sont pas celles du produit** (`#E8862E`, `#0B0B0E`, 48 dp) — et aucun fichier du produit n'importe ce package. | Haute | `grep -rln "com.bmusic.ui.theme" bmusic/app/src/main/java` → **aucun résultat** | Le corpus doit être aligné sur le code (le code fait foi). Décision D9. |
| **E2** | **Le corpus décrit un jeu d'icônes que le code n'utilise pas** : trait 1,75 dp / viewport 24×24 (§5) contre des vecteurs Material Symbols **remplis** en viewport 960×96… (`ic_search_24dp.xml`). | Moyenne | `01-design-system.md` §5 vs `ic_search_24dp.xml` | Les deux nouvelles icônes (loupe réutilisée, chaîne à créer) suivent le **format réel** : rempli, 960×960, tint `colorControlNormal`. Décision D10. |
| **E3** | **Des glyphes Unicode servent d'icônes** dans l'écran Télécharger : `Text("⌕")` (loupe), `Text("⛓")` (chaîne), `Text("⌚")` (historique), `Text("×")` (fermeture). | Haute | `SearchTab.kt` L74, L77, L93, L97 · `DownloadScreen.kt` L86/L93 | Ces glyphes dépendent de la police du système : non homogènes, non tintables, non accessibles. **À remplacer par des vecteurs** dans les écrans touchés. |
| **E4** | **Chaînes en dur** dans l'écran Télécharger : « Rechercher », « Minimum 2 caractères », « Vous êtes hors ligne », « Source temporairement indisponible, moteur à mettre à jour », « Réessayer »… | Haute | `SearchTab.kt` L73, L85, L140, L144 | Contredit `A11Y-4` et le lint `HardcodedText`. Les 42 microcopies du PM doivent devenir des ressources. |
| **E5** | **Un spinner pour la pagination**, là où le design system impose des squelettes sobres. | Moyenne | `SearchTab.kt` L121 `CircularProgressIndicator` | Le langage skeleton existe déjà (`Motion.DurationSkeleton = 1200`). |
| **E6** | **La barre basse Material est plafonnée à 6 entrées**, et le menu est reconstruit à l'exécution : 5 catégories (`MAX_VISIBLE_CATEGORIES = 5`) + `nav_download` = **6, exactement la limite**. Ajouter Explorer + Liens porte le total à **8 → exception Material à l'exécution**. | **Bloquante** | `javap -constants` sur `material-1.14.0` → `MAX_ITEM_COUNT = 6` · `CategoryInfo.kt` L51 · `AbsSlidingMusicPanelActivity.kt` L465-483 | Arbitrage D8 (voir `10-navigation-et-icones.md`) : c'est une **contrainte de plateforme**, pas une préférence. |
| **E7** | **Le « verre » n'est pas un verre.** `GlassSurface.kt` n'implémente aucun `RenderEffect`/`blur` ; le fond est un noir à 50 % d'opacité, la forme est arrondie mais le flou est nul. L'opacité réalisée (0,5) est loin de la spécifiée (0,78-0,92). | Haute | `bmusic/docs/design/08-ecarts-liquid.md` §2 (constat du designer précédent, repris tel quel) | Hors périmètre des deux nouveaux écrans, mais **décisif pour eux** : la ligne « liquid léger » n'est pas un modèle à reproduire, c'est une ligne à ne pas étendre. |
| **E8** | **Pas de token de fondu enchaîné** alors que la spec animations le demande (≈400 ms). | Basse | `08-ecarts-liquid.md` §2 (« ÉCART : ajouter `DurationCrossfade = 400` ») | À créer dans `Motion.kt` au passage (une seule constante). |
| **E9** | **Aucune baseline de capture v1.0.0** : pas de référence de non-régression visuelle possible. | Moyenne | `08-ecarts-liquid.md` §2 et §3 | Un test de capture Robolectric par écran et par thème devient le garde-fou de la non-régression (voir `13-…`). |
| **E10** | **Le corpus documente une navigation à 4 onglets** (Accueil · Recherche · File · Réglages) que le produit n'a pas : la barre réelle est reconstruite dynamiquement à partir des catégories de bibliothèque. | Moyenne | `01-design-system.md` §6 (« Barre de navigation ») vs `AbsSlidingMusicPanelActivity.kt` L465-483 | Le corpus doit décrire la barre réelle avant d'y ajouter deux entrées. |
| **E11** | **Le « dépoli » n'est pas un dépoli** : `GlassTokens.radius()` sert de rayon de forme, pas de rayon de flou. | Basse | `GlassTokens.kt` L43 `fun blurRadius(level) = radius(level)` | Ne pas s'en servir comme référence de profondeur pour les nouveaux écrans. |

### 1.3 Hiérarchie, lisibilité, densité, navigation, responsive, accessibilité

| Domaine | Constat | Verdict |
|---|---|---|
| **Hiérarchie** | Le lecteur et la bibliothèque sont structurés (titre 22/28, corps 16/24, légende 13/18). L'écran *Recherche* du Téléchargeur, lui, **entretient le même niveau de lecture pour le titre, l'historique, les résultats et les erreurs** : rien ne guide l'œil. | **À corriger** — les deux nouveaux écransReceived une hiérarchie explicite (champ → résultats → erreurs). |
| **Lisibilité** | 13/18 pour les métadonnées et 11/16 pour les badges : conformes AA, mais le badge plateforme à 11 sp sur fond `surfaceContainerHigh` doit rester au-dessus de 4,5:1 (calculé : 8,00:1 en sombre, 6,93:1 en clair — conforme). | **Conforme** |
| **Densité** | Les rangées de résultats actuelles sont en `padding(4.dp)` sans séparateur et sans métadonnées structurées ; à 20 sp de marge, la zone utile est sous-exploitée. | **À corriger** — gabarit de rangée défini (cf. `11-maquettes-…`). |
| **Navigation** | Voir E6 : contrainte Material à 6 entrées. | **Bloqué** — arbitrage D8. |
| **Responsive** | Le produit n'a que des valeurs `sw600dp`/`sw720dp` dans `values/` ; aucune maquette de tablette pour les écrans de listes. Le prototype web, lui, a un `--content-max` inutilisé. | **À définir** (cf. `13-…`). |
| **Accessibilité** | `Dimens.TouchTarget = 48.dp` (bon) ; `check-glass-policy.sh` interdit le verre (bon) ; TalkBack n'est vérifié par aucun test sur ces écrans. | **Partiellement conforme** — à prouver par tests de sémantique. |

## 2. Direction artistique v1.2 (DESIGN-02)

### 2.1 Intention en une phrase

> **« Mat & Signal »** — des surfaces **mates et opaques**, une **hiérarchie** portée par la typographie et l'espace, un **unique accent ambre** réservé à l'action principale du moment, et **aucune transparence décorative** : l'information se lit, elle ne scintille pas.

### 2.2 Six principes opposables

| # | Principe | Règle de conformité (vérifiable) |
|---|---|---|
| **P1** | **Mat** : aucun fond translucide, aucun flou, aucun dégradé, aucun reflet. | `grep -rInE 'RenderEffect\|Modifier\.blur\|copy\(alpha' app/src/main/java --include='*.kt'` sur les fichiers des deux écrans → **aucune occurrence** (hors scrim modal 60 % et shimmer de skeleton, exceptions fonctionnelles déjà admises par `01-design-system.md` §2.5). |
| **P2** | **Un seul accent visible par vue.** | L'ambre `primary` n'apparaît que sur l'action principale courante de l'écran (en général une seule fois). Pas d'accent sur les badges de plateforme, pas d'accent sur la vignette, pas d'accent sur deux éléments concurrents. |
| **P3** | **Hiérarchie par la typo et l'espace, pas par la couleur.** | Une seule taille `display`/`headline` par écran ; les écarts de niveau sont de 4 dp minimum (grille 4 dp) ; aucune couleur n'est utilisée pour créer un niveau hiérarchique. |
| **P4** | **Surface = ton.** | L'élévation est un passage `surface → surfaceContainer → surfaceContainerHigh`. Aucune ombre portée, aucun `shadow()`. |
| **P5** | **Séparation = 1 dp.** | Filet `outline` (`#2A2A34` sombre / `#E4E4EA` clair), jamais blanc, jamais dégradé. |
| **P6** | **Le mouvement explique.** | Une animation = une information nouvelle ou un changement d'état. Zéro animation décorative, zéro animation infinie hors squelette. Détail : `12-tokens-composants-et-motion.md`. |

### 2.3 Identité conservée (rien de nouveau à créer)

- **Police** : Inter 400/500/600, échelle existante (`displayLarge 32/40` → `labelSmall 11/16`).
- **Couleur** : les 26 paires testées sont **déjà conformes** (0 échec). L'ambre de marque reste le seul accent.
- **Formes** : 8 dp (champs, puces) · 12 dp (vignettes) · 16 dp (panneaux) · 20 dp (feuilles) · cercle (boutons principaux).
- **Grille** : 4 dp ; marge d'écran 20 dp ; cibles 48 dp.
- **Marque** : logo, monochrome, icône adaptative — non touchés.
- **Icônes** : format réel du dépôt (Material Symbols rempli, viewport 960×960, 24 dp, tint `colorControlNormal`).

### 2.4 Ce que la direction refuse — et la règle qui l'interdit

| Refus | Règle d'appui |
|---|---|
| Cartes translucides, surfaces givrées, reflets, brillance, lueur | Dossier (interdiction absolue) · `01-design-system.md` §2.5 · `check-glass-policy.sh` (motifs `specular\|sheen\|shine\|glow` interdits). |
| Flou décoratif, y compris « léger » | `DS-5` (P0) · `check-glass-policy.sh` (flou hors liste blanche = échec CI). |
| Cartes surchargées pour un simple résultat | `SE-2` · `03-ecrans.md` : lignes et grilles sobres. |
| Spinners | `03-ecrans.md` : squelettes sobres. |
| Deux accents dans la même vue | `01-design-system.md` §6 (« règle “un seul accent visible” »). |
| Texte en dur | `A11Y-4` + lint `HardcodedText`. |
| Icônes Unicode | E3 ci-dessus : non tintables, non homogènes, non accessibles. |

### 2.5 Position explicite sur la ligne « liquid léger » existante

Le produit embarque une direction « liquid léger » (`docs/design/06-liquid-leger.md`, `GlassPolicy`, `GlassTokens`, `GlassSurface`) dont le précédent designer a lui-même mesuré l'écart (E7 : flou effectif nul, opacité 0,5 au lieu de 0,78-0,92).

**Les deux nouveaux écrans n'utilisent pas cette ligne.** Motifs :
1. Le dossier l'interdit (interdiction absolue) et `DS-5` la rend non conforme.
2. Ces deux écrans sont des **écrans de travail denses** (champs, listes de 20 lignes, états d'erreur) : la transparence y dégraderait la lecture des métadonnées en 13 sp et la distinction « résultat / erreur ».
3. La politique glass existante **n'est ni touchée ni remise en cause** : elle reste applicable au lecteur et au mini-player, hors périmètre de cette livraison.

Conséquence pratique : les deux écrans n'héritent **d'aucun** `GlassSurface`, **d'aucune** opacité, **d'aucun** rayon de flou. Ils n'utilisent que les surfaces opaques `surface`, `surfaceContainer`, `surfaceContainerHigh`.

## 3. Jeton de couleur des deux écrans + contrastes calculés

### 3.1 Rôles retenus (aucun.hex inventé)

| Rôle d'interface | Token produit | Sombre | Clair | Usage dans Explorer / Liens |
|---|---|---|---|---|
| Fond d'écran | `background` | `#0B0B0E` | `#FAFAFB` | Fond de l'écran |
| Fond de contenu | `surface` | `#131318` | `#FFFFFF` | Zone de liste, champ |
| Fond d'élément | `surfaceContainer` | `#1A1A21` | `#E9E9EE` | Champ de saisie, ligne alternée, squelette |
| Fond de badge | `surfaceContainerHigh` | `#22222B` | `#E4E4EA` | Badge de plateforme, pastille d'état |
| Texte principal | `onSurface` | `#F2F2F0` | `#1B1B1F` | Titres de résultat, titres de section |
| Texte secondaire | `onSurfaceVariant` | `#B8B8C0` | `#4A4A52` | Chaîne, durée, états |
| Séparateur | `outline` | `#2A2A34` | `#E4E4EA` | Filet 1 dp |
| Accent | `primary` | `#E8862E` | `#A85B0B` | Action principale unique, état actif de la barre |
| Texte sur accent | `onPrimary` | `#241203` | `#FFFFFF` | Libellé du bouton principal |
| Erreur | `error` | `#FFB4AB` | `#BA1A1A` | Message d'erreur (texte) |
| Alerte | `errorContainer` / `onErrorContainer` | `#93000A` / `#FFDAD6` | `#FFDAD6` / `#93000A` | État « non pris en charge » (pastille) |

### 3.2 Contrastes calculés (WCAG 2.1, script `contrast_check.py`)

**26 paires testées, 0 échec.** Les valeurs clés :

| Thème | Paire | Ratio | Seuil | Verdict |
|---|---|---|---|---|
| Sombre | `onSurface` / `surface` | **16,52** | 4,5 | ✅ |
| Sombre | `onSurfaceVariant` / `surfaceContainerHigh` (badge) | **8,00** | 4,5 | ✅ |
| Sombre | `primary` / `surface` (icône active) | **6,95** | 3,0 | ✅ |
| Sombre | `onPrimary` / `primary` (bouton) | **6,78** | 4,5 | ✅ |
| Sombre | `error` / `surface` | **10,91** | 4,5 | ✅ |
| Sombre | `onTertiary` / `tertiary` (pastille neutre) | **5,66** | 4,5 | ✅ |
| Clair | `onSurface` / `surface` | **17,17** | 4,5 | ✅ |
| Clair | `onSurfaceVariant` / `surfaceContainerHigh` (badge) | **6,93** | 4,5 | ✅ |
| Clair | `primary` / `surface` (icône active) | **5,04** | 3,0 | ✅ |
| Clair | `onPrimary` / `primary` | **5,04** | 4,5 | ✅ |
| Clair | `error` / `surface` | **6,46** | 4,5 | ✅ |

**Règle appliquée** : une information n'est jamais portée par la couleur seule ; le badge de plateforme porte un **texte** en plus de sa couleur, l'état porte une **icône + un texte**.

## 4. Ce que cette direction ne change pas

Lecture locale, bibliothèque, file, playlists, favoris, notification, réglages, thème, accent dynamique, écran *Télécharger* (hors remplacement des glyphes Unicode et des chaînes en dur, cf. E3/E4), politique glass existante, logo et marque.

## 5. Décisions ouvertes issues de l'audit

| # | Décision | Options | Défaut retenu | Qui décide | Échéance |
|---|---|---|---|---|---|
| **D8** | Structure de navigation (limite Material à 6 entrées) | (a) barre perso épinglée + bibliothèque défilable ; (b) 2 emplacements réservés + catégories réduites ; (c) barre actuelle inchangée + entrées dans la barre supérieure |voir `10-navigation-et-icones.md` §3 | `plan` + `build` + coordinator | **avant le lot L6** |
| **D9** | Corpus `docs/design/tokens/*.kt` obsolète (non importé par le produit) | (a) aligner le corpus sur le code ; (b) supprimer le corpus et pointer vers `app/.../ui/theme/` | (a) | designer + `plan` | avant fusion (L1) |
| **D10** | Format des icônes (corpus « trait 1,75 dp » vs implémentation « rempli 960×960 ») | (a) suivre le format réel ; (b) migrer tout le jeu | (a) pour les 2 nouvelles icônes | designer | avant implémentation |
| **D6** | Cibles tactiles | — | **48 dp** : le code est déjà conforme, c'est la documentation (`01-design-system.md` §4 et `Dimens.kt` du corpus) qui doit être corrigée | designer | immédiat |
| **D11** | Remplacement des glyphes Unicode (`⌕`, `⛓`, `⌚`, `×`) par des vecteurs | (a) oui, dans les écrans touchés ; (b) plus tard | (a) — l'onglet Explore du Téléchargeur est de toute façon extrait (lot L4) | designer + `build` | avec L4 |
