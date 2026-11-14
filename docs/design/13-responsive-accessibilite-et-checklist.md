# B Music — Responsive, accessibilité visuelle et revue avant merge (v1.2)

> Agent lean designer B Music. Date : 14 novembre 2026. **Phase design = lecture seule.**
> Sources : `Dimens.kt` (`ScreenMargin 20`, `TouchTarget 48`) · `Type.kt` · `Motion.kt` (`scaleDuration`) · `scripts/check-glass-policy.sh` · `docs/design/01-design-system.md` §2.5/§5/§6 · `bmusic/docs/design/08-ecarts-liquid.md` (constats repris) · mesures de contraste du script `contrast_check.py`.
> Renvois : `09-audit-et-direction-artistique-v1.2.md` · `10-navigation-et-icones.md` · `11-maquettes-explorer-et-liens.md` · `12-tokens-composants-et-motion.md`.

## 1. Responsive (DESIGN-08)

### 1.1 Points de rupture retenus

| Point | Largeur | Cas |
|---|---|---|
| **Compact** | 320 – 359 dp | Petit mobile, mode 2× , Android Go |
| **Standard** | 360 – 599 dp | Mobile courant |
| **Tablette** | 600 – 839 dp | Tablette portrait, fenêtre divisée |
| **Large** | ≥ 840 dp | Tablette paysage, fenêtre large |

### 1.2 Règles par point de rupture

| Élément | Compact (320-359) | Standard (360-599) | Tablette (600-839) | Large (≥840) |
|---|---|---|---|---|
| **Marge latérale** | 16 dp (marge 20 dp + inset système) | 20 dp | 20 dp | 20 dp |
| **Mesure de contenu** | pleine largeur | pleine largeur | **640 dp centrés** | **640 dp centrés** |
| **Champ de recherche / zone de collage** | pleine largeur utile | pleine largeur utile | 640 dp, aligné sur la liste | 640 dp, aligné sur la liste |
| **Liste** | 1 colonne | 1 colonne | 1 colonne (pas de 2 colonnes : la densité d'un navigateur de découverte tient en 1 colonne) | 1 colonne |
| **Feuille basse (fiche / aperçu)** | pleine largeur, rayon 20 dp au sommet | idem | **480 dp de large, centrée horizontalement** (une feuille pleine largeur de tablette est disproportionnée) | idem |
| **Barre de navigation** | **5 entrées maximum** : 3 catégories + Explorer + Liens. L'entrée conditionnelle « Télécharger » bascule dans le menu de la Bibliothèque (voir §1.4) | 6 entrées maximum | 6 entrées, items plus larges (largeur répartie), labels inchangés | idem |
| **Compactage** | 1 colonne | 1 colonne | 1 colonne | 1 colonne |

**Pourquoi une seule colonne partout** : DESIGN-06 interdit de créer un système plus complexe que nécessaire, et le benefit d'une grille à 2 colonnes sur ces écrans est faible (le contenu est une liste de métadonnées, pas une collection d'objets). La lecture reste fluide jusqu'à 640 dp.

### 1.3 Adaptation aux contraintes système

| Contrainte | Traitement |
|---|---|
| **Rotation** | Les deux écrans sont des listes : aucun état local fragile ; la requête, la position de liste et la page chargée sont conservées (rotation = recréation de l'activité). |
| **Mode paysage (hauteur < 480 dp)** | La feuille basse conserve sa hauteur maximale (320 dp) ; la liste reste défilante ; le champ reste visible en haut (pas de champ collé au clavier). |
| **Échelle de police 200 %** | Les hauteurs de rangée sont des **minimales** (`heightIn(min = 96 dp)`), jamais fixes : le texte peut occuper 3 lignes sans tronquer. Les titres de résultat sont limités à 2 lignes avec `ellipsis` ; l'information complète est dans la fiche. |
| **Insets (encoche, barre gestuelle, clavier)** | Marges système ajoutées à `ScreenMargin` ; le champ remonte au-dessus du clavier (etatime `imePadding`). |
| **Écran très étroit (320 dp)** | Les libellés de navigation les plus longs sont tronqués par le composant : mitigation par la règle de 5 entrées (§1.4). |
| **Lecture des durées** | Chiffres tabulaires (`tnum`) pour les durées, comme le design system l'exige — pas de jitter horizontal pendant la mise à jour. |

### 1.4 Cas particulier : le nombre d'entrées de la barre à 320 dp

À 320 dp, 6 entrées de barre ne laissent que **53 dp** par entrée : un libellé « Explorer » (11 sp, tracking +0,3) tient, « Télécharger » ne tient pas.

Règle retenue, sans toucher au composant Material :
- **≤ 359 dp** : la barre contient au maximum **5 entrées** — les 3 catégories visibles + **Explorer** + **Liens**. L'entrée conditionnelle « Télécharger » (option `Preferences.downloaderEnabled`) **reste accessible via l'écran Bibliothèque** (elle y est de toute façon un onglet de l'application). Ce comportement est **conditionné par la largeur**, pas par le nombre de catégories.
- **≥ 360 dp** : 6 entrées, comme le permet la limite Material.

Cette règle est à implémenter dans `updateTabs()` (le point où le menu est déjà reconstruit) : elle est donc **compatible avec le mécanisme runtime existant** et n'ajoute aucune dépendance.

## 2. Accessibilité visuelle (DESIGN-08)

### 2.1 Contraste

| Élément | Exigence | Mesure |
|---|---|---|
| Texte principal (`onSurface` sur `surface` / `surfaceContainer`) | ≥ 4,5:1 | 16,52 (sombre) · 17,17 (clair) ✅ |
| Texte secondaire (`onSurfaceVariant` sur `surface`) | ≥ 4,5:1 | 9,40 (sombre) · 8,78 (clair) ✅ |
| Badge de plateforme (`onSurfaceVariant` sur `surfaceContainerHigh`) | ≥ 4,5:1 | 8,00 (sombre) · 6,93 (clair) ✅ |
| Icône active (`primary` sur `surface`) | ≥ 3:1 | 6,95 (sombre) · 5,04 (clair) ✅ |
| Bouton primaire (`onPrimary` sur `primary`) | ≥ 4,5:1 | 6,78 (sombre) · 5,04 (clair) ✅ |
| Message d'erreur (`error` sur `surface`) | ≥ 4,5:1 | 10,91 (sombre) · 6,46 (clair) ✅ |

**26 paires calculées, 0 échec** (script `contrast_check.py`, WCAG 2.1).

### 2.2 Focus clavier et DPAD

| Élément | Focus visible | Ordre |
|---|---|---|
| Entrées de la barre | anneau 2 dp `primary` + 2 dp fond, offset 2 dp | de gauche à droite |
| Champ de recherche | idem + curseur `primary` | premier de l'écran |
| Rangée de résultat | anneau sur la rangée entière (pas sur la vignette seule) | ordre de la liste |
| Boutons de la fiche | anneau | après la liste |
| Feuille basse | focus capturé (dialogue), Échap ferme | — |

- **Entrée** clavier : déclenche la recherche (et seulement elle) ; **Échap** ferme la fiche ; **Tab** parcourt les actions de la rangée.
- La vignette est **décorative** : elle ne prend pas le focus, l'étiquette est portée par la ligne (« Titre — chaîne, durée »).

### 2.3 Lecture d'écran (TalkBack)

| Élément | Annonce attendue |
|---|---|
| Entrée de barre | « Explorer, onglet » / « Liens, onglet » (+ « sélectionné » quand actif) |
| Champ de recherche | « Rechercher sur YouTube, champ de texte » ; le message d'aide est annoncé au changement d'état |
| Rangée de résultat | « Titre de la vidéo — Nom de la chaîne, 3 minutes 42, bouton » |
| Ligne de lien | « Titre — YouTube, aperçu disponible, bouton » ; pour une plateforme non lisible : « — TikTok, non lisible dans B Music, bouton » |
| État d'écran | « Recherche en cours », « 12 résultats », « Aucun résultat », « Connexion impossible, Réessayer, bouton » |
| Feuille basse | rôle dialogue ; titre et actions annoncés à l'ouverture ; retour arrière ferme |

**Règle** : aucune information n'est portée par la couleur seule (badge + texte, état + icône + texte), conformément à `A11Y-1`.

### 2.4 Mouvement, thème, tactile

| Sujet | Règle |
|---|---|
| Mouvement réduit | `Motion.scaleDuration(ms, reduced)` → 0 ms ; le shimmer de squelette devient un bloc statique ; aucune translation |
| Thème | Les deux écrans existent en clair et sombre avec les mêmes rôles ; aucun fond codé en dur |
| Tactile | Toutes les cibles ≥ **48 dp** (`Dimens.TouchTarget`), y compris les icônes de 24 dp (`hitSlop` ou zone de 48 dp) |
| Vibration | Aucune vibration sur les deux écrans (le retour visuel suffit) |

## 3. Checklist de revue avant merge (DESIGN-09)

### 3.1 Principe de la revue

La revue **ne demande pas de refonte**. Elle compare l'implémentation aux principes de `09-…` et aux gabarits de `11-…`, et **liste des corrections locales, classées par priorité**. Toute correction proposée doit être applicable sans toucher aux composants hors périmètre.

### 3.2 Corrections P0 — bloquantes avant merge

| # | Constat à vérifier | Correction précise | Preuve attendue |
|---|---|---|---|
| P0-1 | **Flou / transparence accidentelle** dans les deux écrans | Retirer tout `RenderEffect`, `Modifier.blur`, `copy(alpha)` décoratif ; ne conserver que le scrim 60 % et le shimmer | `grep -rInE 'RenderEffect\|Modifier\.blur\|copy\(alpha' app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → vide ; `bash scripts/check-glass-policy.sh` → OK |
| P0-2 | **États manquants** (7 pour Explorer, 7 pour Liens) | Ajouter chaque état de `11-…` §1.3 et §2.5, y compris « erreur de pagination » et « plateforme non reconnue » | Test de capture par état, ou parcours manuel daté |
| P0-3 | **Chaînes en dur** | Basculer les 42 microcopies du PM en ressources ; plus aucun `Text("…")` littéral | `grep -R 'Text("' app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → vide ; lint `HardcodedText` |
| P0-4 | **Glyphes Unicode** | Remplacer `⌕`, `⛓`, `⌚`, `×` par des vecteurs tintables | `grep -R 'Text("⌕\|Text("⛓\|Text("⌚\|Text("×"' app/src/main/java` → vide dans les écrans repris |
| P0-5 | **Icône de chaîne illisible** | Ajuster la géométrie de `ic_link_open_24dp.xml` jusqu'à lisibilité à 16 dp (ouverture ≥ 100 unités sur le viewport 960) | Capture à 24 et 16 dp, thèmes clair et sombre, 3 fonds |
| P0-6 | **Deux accents dans la fiche** | « Copier le lien » devient un bouton **secondaire** ; seul « Ouvrir dans YouTube » porte l'accent | Capture de la fiche |
| P0-7 | **Cibles < 48 dp** | `IconButton` → zone 48 dp (`Dimens.TouchTarget` + `hitSlop`) | Audit des `IconButton` des deux écrans |
| P0-8 | **Focus invisible** | Anneau 2 dp `primary` + 2 dp fond sur tous les éléments interactifs | Parcours clavier + capture |
| P0-9 | **Animation infinie hors squelette** | Supprimer tout `rememberInfiniteTransition` hors squelette | `grep -R 'rememberInfiniteTransition' app/src/main/java/com/bmusic/app/ui/screen/{explore,links}` → 1 occurrence |
| P0-10 | **Barre au-delà de 6 entrées** (contrainte Material) | Appliquer l'arbitrage D8 ; le lot L6 ne peut pas ajouter 2 items à un menu de 6 | Test Robolectric : 5 catégories + téléchargement + réseau → la barre se construit sans exception |
| P0-11 | **Information portée par la seule couleur** | Ajouter texte/icône à chaque badge et chaque état | Revue de `11-…` §2.3 |
| P0-12 | **Message d'erreur non actionnable** | Chaque erreur porte une action (« Réessayer », « Ouvrir à l'extérieur », « Corriger ») | Table erreur → message → action revue |

### 3.3 Corrections P1 — avant la version publique

| # | Constat | Correction |
|---|---|---|
| P1-1 | Libellés de barre tronqués à 320 dp | Appliquer la règle des 5 entrées (§1.4) dans `updateTabs()` |
| P1-2 | Aucune baseline de capture | Générer les captures de référence (2 écrans × 2 thèmes × 3 largeurs) via les tests Robolectric de capture |
| P1-3 | `Motion.DurationCrossfade` absent | Ajouter la constante (400 ms) dans `Motion.kt` |
| P1-4 | Monogrammes de plateforme ambigus (Twitter/X, TikTok/…) | Préfixer d'une lettre distinctive (`X`, `TT`, `IG`, `FB`, `SC`) et **doubler d'un texte** dans la méta-ligne |
| P1-5 | Feuille pleine largeur en tablette | Contraindre à 480 dp centrés au-delà de 600 dp |
| P1-6 | Aucune preuve TalkBack sur ces écrans | Rédiger le script d'audit TalkBack et joindre les enregistrements |
| P1-7 | Toast utilisé pour une erreur | Réserver le toast aux confirmations ; les erreurs restent en place |

### 3.4 Corrections P2 — confort

| # | Constat | Correction |
|---|---|---|
| P2-1 | Ligne de lien sans indication de plateforme quand le monogramme est ambigu | Icône secondaire devant le texte d'état |
| P2-2 | « Voir plus » sans retour de position | Mémoriser la position et la restaurer après pagination |
| P2-3 | Absence de vignette pour les résultats sans miniature | Icône locale de substitution dédiée (pas de réseau) |
| P2-4 | Champ de collage sans compteur pendant la saisie | Afficher le compteur en direct (lignes valides / ignorées) |

### 3.5 Ce que la revue **ne** demande pas

- De refondre l'écran *Télécharger* au-delà du remplacement des glyphes et des chaînes en dur.
- De traiter la ligne « liquid léger » du lecteur et du mini-player (hors périmètre, déjà auditée par le designer précédent).
- De créer un composant générique de « carte de résultat » réutilisable ailleurs : le gabarit est propre à l'écran Explorer.
- De migrer le jeu d'icônes vers un style trait (D10 : on suit le format réel du dépôt).

## 4. Checklist de recette visuelle (à joindre au lot L6)

| # | Élément | Attendu |
|---|---|---|
| V-1 | Capture de la barre avec 3 catégories + Explorer + Liens + Télécharger | 6 entrées lisibles, état actif ambre sur l'entrée courante |
| V-2 | Capture de l'onglet Explorer : repos, chargement, résultats, vide, erreur | 5 états, aucun vide blanc, aucun chevauchement |
| V-3 | Capture de l'onglet Liens : vide, liste, ligne invalide, ligne non prise en charge | 4 états, badge + texte, aucune couleur seule |
| V-4 | Capture de la fiche (Explorer et Liens) | Un seul accent, pas de zone de lecture |
| V-5 | Captures clair et sombre × 320 / 411 / 768 dp | Même hiérarchie, aucune troncature de texte |
| V-6 | Capture de l'icône de chaîne à 24 et 16 dp | Deux maillons distincts, ouverture visible |
| V-7 | Séquence des 3 animations clés (résultats, ajout de lien, ouverture de fiche) | Durées conformes à `12-…` §3.2 ; version « mouvement réduit » à 0 ms |

## 5. Décisions ouvertes

| # | Décision | Défaut | Statut |
|---|---|---|---|
| **D8** | Structure de navigation (limite Material 6) | option A : 2 emplacements réservés, 3 catégories visibles | **à arbitrer (plan + coordinator) avant L6** |
| **D9** | Corpus `docs/design/tokens/*.kt` non importé par le produit | aligner le corpus sur le code | à trancher avant la fusion (L1) |
| **D10** | Format des icônes | suivre le format réel (rempli, 960×960) | **retenu** |
| **D6** | Cibles tactiles | **48 dp** (le code est conforme) | **retenu** — la documentation est à corriger |
| **D11** | Glyphes Unicode → vecteurs | oui, dans les écrans repris | **retenu** (avec L4) |
| **D12** | Règle des 5 entrées à ≤ 359 dp | recommandée (entrée « Télécharger » déplacée dans Bibliothèque) | à confirmer avec `build` (implémentation dans `updateTabs()`) |
