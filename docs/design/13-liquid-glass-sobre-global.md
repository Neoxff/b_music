# B Music — Liquid Glass Sobre, global : spécifications de design (v1.3)

> Agent DESIGNER — SPECIFICATION DE DESIGN. **Aucun code modifié**, aucune mesure claimée sur un appareil.
> Date : 16 novembre 2026. Branche de travail : `feat/glass-global`.
> Documents-liants : `13-liquid-glass-sobre.md` (périmètre **Téléchargeur**, v1.2 — reste la référence de ce périmètre),
> `11-onglets-lien-historique.md` (écrans Lien/Historique), `12-tokens-composants-et-motion.md` (tokens), `Motion.kt` (durées/springs).
> Contraintes : réutilisation exclusive de `GlassSurface`, `GlassPolicy`, `GlassTokens`, `Motion.kt`. **Aucun nouveau site de flou.**
> Contraintes de marque : **aucune icône, aucune police, aucun logo Apple**. Langage *inspiré* des interfaces récentes
> d'iPhone, jamais copié.

---

## 0. Ce que ce document change par rapport à `13-liquid-glass-sobre.md`

| | v1.2 (`13-liquid-glass-sobre.md`) | v1.3 (ce document) |
|---|---|---|
| Périmètre | 6 éléments, écrans Lien/Historique | **Toute l'application** (13 écrans + 4 types de surfaces) |
| Matière | flou 16 dp, opacité 0,86/0,90, contour 12 % | flou **16–24 dp selon la surface**, opacité **0,72–0,90** (plancher dur **0,70**), contour 8 %/12 % |
| Formes | rayon 16/20 dp | **capsules flottantes** (rayon = hauteur ÷ 2) + coins concentriques |
| Teinte | fond neutre | **teinte adaptative** (pochette / accent) |
| Reflet | « interdit » | **reflet spéculaire fin statique** sur le bord haut + liseré lumineux très léger en bas |
| Mouvement | fondus et ressorts | ressorts + **déploiement depuis la position d'origine** + compression au défilement |
| Repli | 5 conditions | 5 conditions + **couleur exacte par surface** (clair / sombre) |

**Aucun des deux documents n'est réécrit** : v1.2 reste valable pour les 6 éléments qu'il couvre, et ce document
**étend** son périmètre. En cas de contradiction sur un élément du Téléchargeur, **ce document (v1.3) prévaut**
car il est plus récent et plus contraignant ; `13-liquid-glass-sobre.md` sera alors amendé par le Build.

---

## 1. PRINCIPES

### 1.1 Hiérarchie (règle n° 1)

> **Le CONTENU reste net et opaque. Le verre est réservé aux couches de CONTRÔLE qui flottent au-dessus.**

| Couche | Traitement | Exemples |
|---|---|---|
| Contenu | **opaque**, jamais de flou | pochettes, titres, lignes de liste, paragraphes, avatars |
| Couche de contrôle flottante | **verre** | barre du bas, barre d'en-tête réduite, mini-lecteur, champ de saisie, feuille, dialogue, boutons ronds, poignée de curseur |
| Chrome système | hors périmètre | notification de lecture, sélecteur de fichiers, Android Sharesheet |

### 1.2 Formes

| Règle | Valeur |
|---|---|
| Coins concentriques | rayon intérieur = rayon extérieur − marge (marge 8 dp) |
| Capsules | rayon = hauteur ÷ 2 (ex. barre 64 dp → rayon 32 dp) — **forme dérivée**, pas un nouveau token |
| Barres flottantes | **détachées** des bords : marge 16 dp (jamais collées) |
| Feuilles / menus | coins **28 dp** (16 dp sur les angles inférieurs adjacents au bord) |
| Boutons ronds | diamètre 48 dp, rayon 24 dp, écart 8 dp |
| Poignée de curseur | diamètre 14 dp, rayon 7 dp |

### 1.3 Matière

| Paramètre | Valeur | Plancher / plafond |
|---|---|---|
| Flou (rayon) | **16 dp** (barres, champs, puces) · **20 dp** (barre d'onglets, mini-lecteur) · **24 dp** (feuilles, dialogues, lecteur) | ≤ 24 dp (plafond `GlassTokens`) |
| Opacité de surface | **0,78 → 0,90** selon niveau et thème (§1.5) | **jamais < 0,70** |
| Contour | 1 dp, blanc 8 % (sombre) / noir 8 % (clair) · **12 %** sur les surfaces marquées | 8 %–16 % |
| Reflet spéculaire | dégradé **statique** blanc 6 % → transparent sur les **40 % supérieurs** | statique, jamais animé |
| Liseré bas | blanc **4 %** (sombre) / noir 4 % (clair), 1 dp | très léger |
| Ombre | **une seule** ombre douce, élévation 2–4 dp, `0x1A000000` (sombre) / `0x14101828` (clair) | pas de double ombre |
| Saturation du fond | × **1,1 maximum** | jamais au-delà |

### 1.4 Teinte adaptative

- La surface prend **légèrement la teinte du fond** : couleur dominante de la pochette en cours (extrait une fois, mis en cache) ou couleur d'accent de l'app.
- **Sombre** : la teinte est **assombrie** (surface plus profonde) pour garder la profondeur du fond.
- **Clair** : la teinte est **éclaircie** (surface plus lumineuse).
- Plafond : la teinte ne peut pas s'éloigner de plus de **6 %** du gris de base de la surface.
- **Contraste AA obligatoire** sur le texte posé dessus (§6).

### 1.5 Échelle d'opacité (tokens étendus **dans la plage autorisée**)

| Niveau | Sombre | Clair | Usage |
|---|---|---|---|
| `Minimal` (léger) | **0,78** | **0,82** | barre du bas, champ de saisie, puce de filtre, barre d'en-tête réduite |
| `Standard` | **0,83** | **0,87** | barre d'onglets, mini-lecteur, feuille, en-tête, dialogue |
| `Prominent` (marqué) | **0,86** | **0,90** | pastille de sélection, boutons ronds du lecteur, poignée de curseur, barre d'onglets active |
| Intensité « Très discret » | niveau − 0,06 | niveau − 0,06 | plancher absolu : **0,72** (sombre) / **0,76** (clair) → **jamais < 0,70** |

> `GlassTokens.opacity()` today : `Minimal 0,78/0,82` ✅ inchangé, `Standard 0,83/0,87` ✅ inchangé,
> `Prominent 0,88/0,92` → **à recaler à 0,86/0,90** pour rester dans la bande globale ≤ 0,90 et pour que
> la pastille de sélection reste dans la fenêtre 0,72–0,86 demandée. C'est le **seul** ajustement de token demandé.

### 1.6 Mouvement

| Moment | Animation | Durée / courbe | Mouvement réduit |
|---|---|---|---|
| Apparition d'une surface verre | fondu + glissement 8 dp | 220 ms · `SpringSoft` (0,86/380) | fondu ≤ 150 ms |
| Pression | **échelle 0,97** | 120 ms · `SpringSnappy` (0,55/700) | échelle immédiate |
| Défilement : en-tête | se **réduit en capsule** (hauteur 64 → 56 dp, rayon 28 dp, titre en fondu) | suit le défilement, ressort 220 ms · `SpringSnappy` | réduction instantanée |
| Défilement : barre du bas | se **compacte** (marge 16 → 10 dp, hauteur 64 → 56 dp) | suit le défilement | compactage instantané |
| Pastille de sélection | glisse sous l'icône active | 220 ms · `SpringSnappy` (0,55/700) | glissement immédiat |
| Mini-lecteur → lecteur plein écran | **l'élément se déploie depuis sa position** (rect capsule → rect plein écran) | 300 ms · `SpringSnappy` | fondu simple 150 ms |
| Ligne → feuille d'actions | **la feuille grandit depuis le rectangle de la ligne** | 220 ms · `SpringSoft` | apparition sans glissement |
| Indicateur d'onglet | glisse en ressort | 220 ms · `SpringSnappy` | glissement immédiat |

**Interdits (absolus)** : animation du rayon de flou · animation de l'opacité de surface pendant un défilement ·
lueur / halo / réfraction animés · `rememberInfiniteTransition` sur une surface verre.

---

## 2. ÉCRAN PAR ÉCRAN

Légende : **V** = verre · **O** = opaque · **O** (lignes) = liste opaque · repli = surface opaque de même teinte/ contour/ mise en page.

| Écran | Élément | Verre/Opaque | Rayon | Opacité (sombre / clair) | Contour | Animation | Repli (sombre / clair) |
|---|---|---|---|---|---|---|---|
| **Global** | Barre du bas (capsule flottante) | V | 32 dp (capsule) | 0,78 / 0,82 | 1 dp 8 % | suit le défilement (compacte 64→56 dp) | `#1A1A21` / `#E4E4EA` |
| Global | Pastille de sélection (sous l'icône active) | V | 16 dp (pastille 40×40) | **0,86 / 0,90** | 1 dp 12 % | glisse en ressort 220 ms `SpringSnappy` | `primary` à 24 % / `primary` à 18 % |
| Global | Mini-lecteur (capsule flottante, 16 dp au-dessus de la barre) | V | 28 dp (capsule 56 dp) | 0,83 / 0,87 | 1 dp 8 % | se déploie depuis sa position (300 ms) | `#1A1A21` / `#E4E4EA` |
| Global | Boutons ronds du lecteur (play/next/précédent) | V | 24 dp (Ø 48 dp) | 0,86 / 0,90 | 1 dp 12 % | échelle 0,97 à la pression | `#2A2A34` / `#DBDCE2` |
| Global | Curseur de progression | O (rail) + V (poignée) | rail 2 dp / poignée 7 dp (Ø 14) | poignée 0,86 / 0,90 | poignée 1 dp 12 % | position en ressort 220 ms | rail `#3A3A46` / `#C9CAD2`, poignée `#5A5A68` / `#A8A9B2` |
| **Accueil** | Barre d'en-tête (réduite au défilement) | V (réduite) / O (étendue) | 28 dp (capsule 56 dp) | 0,83 / 0,87 | 1 dp 8 % | réduction 220 ms | `#131318` / `#FFFFFF` |
| Accueil | Carte «Reprendre» | O | 20 dp | — | 1 dp `outline` | — | inchangé |
| **Bibliothèque** | En-tête + onglets de filtre | V (barre d'onglets) | 20 dp | 0,83 / 0,87 | 1 dp 8 % | glissement de l'onglet 220 ms | `#1A1A21` / `#E4E4EA` |
| Bibliothèque | **Lignes de contenu** | **O** | — | — | séparateur 1 dp | apparition échelonnée 8 max | inchangé |
| Bibliothèque | Puces de filtre | V | 16 dp (hauteur 32 dp) | 0,78 / 0,82 | 1 dp 8 % | glissement de la pastille 220 ms | `#2A2A34` / `#DBDCE2` |
| **Albums / Artistes / Playlists** | Grilles et listes | **O** | — | — | — | — | inchangé |
| Albums / Artistes | Champ de recherche | V | 28 dp (pilule 56 dp) | 0,78 / 0,82 | 1 dp 8 % | — | `#1A1A21` / `#E4E4EA` |
| **Recherche locale** | Barre de recherche | V | 28 dp | 0,78 / 0,82 | 1 dp 8 % | — | `#1A1A21` / `#E4E4EA` |
| Recherche locale | Résultats | **O** | — | — | — | — | inchangé |
| **File d'attente** | Feuille (bottom sheet) | V | 28 dp (haut) | 0,83 / 0,87 | 1 dp 8 % | monte + scrim 60 %, 220 ms `SpringSoft` | `#1A1A21` / `#FFFFFF` |
| File d'attente | Lignes de la file | **O** | — | — | — | — | inchangé |
| **Lecteur plein écran** | Fond | pochette **pré-floutée** (64 px, cache) + teinte adaptative + voile 40 % | — | voile 40 % | — | fondu 400 ms au changement de titre | voile uni `#0B0B0E` / `#FFFFFF` |
| Lecteur plein écran | Pochette (96 dp) | O | 12 dp | — | — | — | inchangé |
| Lecteur plein écran | Commandes principales (play/next/précédent) | V | 24 dp (Ø 48 dp) | 0,86 / 0,90 | 1 dp 12 % | échelle 0,97 | `#2A2A34` / `#DBDCE2` |
| Lecteur plein écran | Curseur de progression | O (rail) + V (poignée) | 2 dp / 7 dp | poignée 0,86 / 0,90 | 1 dp 12 % | ressort 220 ms | rail `#3A3A46` / `#C9CAD2` |
| Lecteur plein écran | Feuille de file / de détails | V | 28 dp | 0,83 / 0,87 | 1 dp 8 % | grandit depuis sa position | `#1A1A21` / `#FFFFFF` |
| **Réglages** | Groupes | O (cartes arrondies **légèrement** translucides sur fond uni) | 20 dp | 0,90 / 0,92 sur fond uni, **sans flou** | 1 dp `outline` | — | `#131318` / `#FFFFFF` |
| Réglages | Champ / sélecteur | V | 16 dp | 0,78 / 0,82 | 1 dp 8 % | — | `#1A1A21` / `#E4E4EA` |
| **Télécharger** | Barre d'onglets Lien / Historique | V | 20 dp | 0,83 / 0,87 | 1 dp 8 % | indicateur 3 dp en ressort | `#1A1A21` / `#E4E4EA` |
| Télécharger > Lien | Zone de saisie centrée | V | 28 dp (champ) | 0,78 / 0,82 | 1 dp 8 % | remontée en ressort 220 ms | `#1A1A21` / `#E4E4EA` |
| Télécharger > Lien | Carte d'aperçu | V | 12 dp | 0,83 / 0,87 | 1 dp 8 % | apparition 220 ms | `#1A1A21` / `#E4E4EA` |
| Télécharger > Lien | Puces des sites pris en charge | V | 8 dp (24 dp de haut) | 0,78 / 0,82 | 1 dp 8 % | — | `#2A2A34` / `#DBDCE2` |
| Télécharger > Historique | **Lignes d'historique** | **O** | — | — | séparateur 1 dp | apparition échelonnée 8 max | inchangé |
| Télécharger > Historique | En-têtes de date collants | V | 16 dp | 0,78 / 0,82 | 1 dp 8 % | — | `#1A1A21` / `#E4E4EA` |
| Télécharger > Historique | Feuille d'actions d'une ligne | V | 28 dp | 0,83 / 0,87 | 1 dp 8 % | grandit depuis la ligne | `#1A1A21` / `#FFFFFF` |
| **Dialogues** | Surface | V (petite) | 28 dp | 0,86 / 0,90 | 1 dp 12 % | échelle 0,96 + fondu 150 ms | `#1A1A21` / `#FFFFFF` |
| Dialogues | Bouton principal | **O** (`primary` plein) | 8 dp | — | — | — | inchangé |
| **Menus contextuels** | Surface | V | 28 dp | 0,83 / 0,87 | 1 dp 8 % | fondu + glissement 8 dp, 220 ms | `#1A1A21` / `#FFFFFF` |
| **Notifications de lecture** | Notification média | **hors périmètre** | — | — | — | — | inchangé |

---

## 3. LISTES ET PERFORMANCE

| Règle | Valeur | Justification |
|---|---|---|
| Flou par ligne | **interdit** | un flou par ligne = coût GPU par frame |
| Surfaces verre simultanées | **≤ 3** visibles (barre du bas + mini-lecteur + **une** feuille ou barre du haut) | au-delà, les surfaces surnuméraires passent **opaques** (le même rendu, sans flou) |
| Fond flouté du lecteur | image **pré-floutée** (64 px) + **mise en cache**, jamais de flou temps réel sur le contenu défilant | un flou temps réel d'une grille en défilement = images lentes |
| Coût d'une surface verre | 1 `RenderEffect` par surface, **partagé** si deux surfaces se chevauchent | pas de empilement de flous |
| Cible | **images lentes < 1 %** pendant le défilement de la bibliothèque | valeur fournie par l'utilisateur via l'écran **Diagnostics** (build debug) |
| Budget mémoire | ≤ 3 surfaces verre × 1 texture de flou + 1 image pré-floutée (64 px ≈ 16 Ko en ARGB_8888) | — |

> **L'agent DESIGNER ne prétend rien avoir mesuré.** Les valeurs de performance proviennent de l'écran
> Diagnostics (build debug) et du journal d'étapes exporté par Partager, exécutés par l'utilisateur.

---

## 4. REPLIS OPAQUES

### 4.1 Conditions de repli (5)

| Condition | Déclencheur | Effet |
|---|---|---|
| API < 31 | `Build.VERSION.SDK_INT < 31` | surfaces opaques |
| Mémoire faible | `ActivityManager.isLowRamDevice()` | surfaces opaques |
| Économiseur de batterie | `PowerManager.isPowerSaveMode()` | surfaces opaques |
| « Effets de transparence » désactivé | préférence `blurEffects = off` | surfaces opaques |
| Animations réduites | `ANIMATOR_DURATION_SCALE == 0` | surfaces opaques **+** animations en fondu simple |

### 4.2 Couleurs exactes du repli (par surface)

Le repli conserve : **même teinte**, **même contour**, **même mise en page**, **mêmes rayons**.

| Surface | Sombre | Clair | Contour sombre / clair |
|---|---|---|---|
| Barre du bas (capsule) | `#1A1A21` | `#E4E4EA` | 1 dp `#3A3A46` / `#D0D0D8` |
| Pastille de sélection | `primary` à **24 %** | `primary` à **18 %** | 1 dp 12 % |
| Mini-lecteur | `#1A1A21` | `#E4E4EA` | 1 dp `#3A3A46` / `#D0D0D8` |
| Boutons ronds du lecteur | `#2A2A34` | `#DBDCE2` | 1 dp 12 % |
| Curseur (rail / poignée) | `#3A3A46` / `#5A5A68` | `#C9CAD2` / `#A8A9B2` | 1 dp 12 % (poignée) |
| Barre d'onglets | `#1A1A21` | `#E4E4EA` | 1 dp `#3A3A46` / `#D0D0D8` |
| En-tête (réduit) | `#131318` | `#FFFFFF` | 1 dp `#2A2A34` / `#E4E4EA` |
| Champ / puce | `#1A1A21` / `#2A2A34` | `#E4E4EA` / `#DBDCE2` | 1 dp `#3A3A46` / `#D0D0D8` |
| Feuille / menu | `#1A1A21` | `#FFFFFF` | 1 dp `#3A3A46` / `#D0D0D8` |
| Dialogue | `#1A1A21` | `#FFFFFF` | 1 dp 12 % |
| Carte d'aperçu | `#1A1A21` | `#E4E4EA` | 1 dp `#3A3A46` / `#D0D0D8` |
| Fond du lecteur | voile uni `#0B0B0E` à 92 % | voile uni `#FFFFFF` à 88 % | — |

### 4.3 Ce que le repli **ne change jamais**

La mise en page, les rayons, les tailles, les positions, les textes, les animations de structure
(la pastille glisse toujours, la capsule reste une capsule) — **seule la matière** change.

---

## 5. RÉGLAGE UTILATEUR

**Réglages → Apparence**

| Réglage | Valeurs | Défaut | Effet |
|---|---|---|---|
| **Effets de transparence** | `auto` · `on` · `off` | `auto` | `auto` = verre si les 4 conditions techniques le permettent (API ≥ 31, RAM ok, pas d'économiseur) ; `on` = forcé (mais le repli reste appliqué si API < 31) ; `off` = **toujours opaque** |
| **Intensité** | `sobre` · `very_discreet` | `sobre` | `very_discreet` = opacité − 0,06 sur chaque niveau (plancher 0,72) **+** reflet réduit à 4 % |
| Contraste renforcé | `auto` · `on` | `auto` | `on` = opacité + 0,04 sur chaque niveau (plafond 0,90) — utilisé quand le contraste AA n'est pas atteint |

**Aperçu en direct** : chaque changement de réglage s'applique **immédiatement** à l'écran courant
(sans redémarrage) et un encart de prévisualisation montre la surface sur la pochette en cours
(claire et foncée) — c'est le seul endroit où deux états du verre sont montrés côte à côte.

**Clés de préférences** : `blur_effects` (`auto|on|off`, déjà implémenté dans `Preferences.blurEffects`) ·
`blur_intensity` (`sobre|very_discreet`, **à créer**) · `blur_contrast` (`auto|on`, **à créer**).

---

## 6. LISIBILITÉ (contraste AA)

| Règle | Valeur |
|---|---|
| Texte sur verre | **≥ 4,5:1** (AA) sur le **pire fond** : pochette très claire **et** pochette très foncée |
| Grand texte (≥ 18,66 dp gras ou 24 dp) sur verre | ≥ 3:1 |
| Si insuffisant | **augmenter l'opacité** (jamais au-delà de 0,90) → **jamais** augmenter le flou |
| Plancher d'opacité | **0,70** (jamais en dessous) |
| Icônes de contrôle sur verre | ≥ 3:1 contre la surface **rendue** (opacité + fond) |
| Texte d'état / erreur | jamais posé sur verre : toujours sur fond opaque |
| Bordure de pastille / poignée | 1 dp 12 % : purement décorative, ne porte jamais l'information seule |

---

## 7. « RENDU IPHONE RÉCENT » — le verre doit se VOIR, tout en restant sobre

> Cible : des surfaces qui **flottent** au-dessus du contenu, **translucides**, **arrondies**, avec un
> **reflet fin** et de la profondeur. Language inspiré des interfaces récentes d'iPhone, **version sobre**,
> **sans copier** d'élément propriétaire Apple (aucune icône, police ou logo Apple).

### 7.1 Barre du bas — capsule flottante

| Attribut | Valeur | Clair | Sombre |
|---|---|---|---|
| Marge aux bords | **16 dp** (gauche, droite, bas) | idem | idem |
| Hauteur | **64 dp** (compacte : 56 dp au défilement) | idem | idem |
| Rayon | **complet** = 32 dp (capsule) | idem | idem |
| Fond | verre `Minimal` | opacité **0,82** | opacité **0,78** |
| Contour | 1 dp noir 8 % | `#14000000` | — |
| Contour sombre | 1 dp blanc 8 % | — | `#14FFFFFF` |
| Reflet haut | blanc 6 % → transparent, 40 % supérieurs, **statique** | idem | idem |
| Liseré bas | 1 dp, 4 % | noir 4 % | blanc 4 % |
| Ombre | unique, 4 dp, `0x14101828` | — | `0x1A000000` |
| **Pastille de sélection** | verre **plus lumineux**, 40×40 dp, rayon 16 dp, **sous l'icône active** | opacité **0,90** | opacité **0,86** |
| Mouvement de la pastille | glisse sous l'icône active | 220 ms · `SpringSnappy` (0,55/700) | idem |
| Repli opaque | capsule `#E4E4EA` + contour 1 dp `#D0D0D8` | — | capsule `#1A1A21` + contour 1 dp `#3A3A46` |

### 7.2 Mini-lecteur — capsule flottante au-dessus de la barre

| Attribut | Valeur | Clair | Sombre |
|---|---|---|---|
| Position | 16 dp au-dessus de la barre du bas, 16 dp de marge latérale | idem | idem |
| Hauteur / rayon | 56 dp / **28 dp** (capsule) | idem | idem |
| Fond | verre `Standard` | opacité 0,87 | opacité 0,83 |
| Contour / reflet / liseré | 1 dp 8 % · blanc 6 % haut · 4 % bas | idem | idem |
| Transition vers le lecteur | **se déploie depuis sa position** (rect capsule → rect plein écran), 300 ms · `SpringSnappy` | idem | idem |
| Repli opaque | `#E4E4EA` | — | `#1A1A21` |

### 7.3 En-têtes — réduction en capsule au défilement

| Attribut | Valeur |
|---|---|
| État étendu | 64 dp, fond **opaque** (le contenu doit rester net) |
| État réduit | 56 dp, **capsule** rayon 28 dp, verre `Standard` (0,87 / 0,83), marge 16 dp |
| Mouvement | hauteur + rayon + opacité suivent le défilement ; ressort 220 ms · `SpringSnappy` ; **jamais** d'animation du rayon de flou ni de l'opacité |
| Titre | passe de `titleLarge` (22/28) à `titleMedium` (16/24) en fondu 150 ms |
| Repli opaque | réduit `#FFFFFF` / `#131318` + contour 1 dp `#E4E4EA` / `#2A2A34` |

### 7.4 Boutons ronds de lecture / suivant / précédent

| Attribut | Valeur | Clair | Sombre |
|---|---|---|---|
| Diamètre / rayon | 48 dp / 24 dp | idem | idem |
| Fond | verre `Prominent` | opacité 0,90 | opacité 0,86 |
| Contour | 1 dp 12 % | `#1E000000` | `#1EFFFFFF` |
| Reflet | blanc 6 % sur 40 % supérieurs, statique | idem | idem |
| Icône | 24 dp, trait 1,75 dp, `onSurface` | idem | idem |
| Pression | échelle 0,97, 120 ms · `SpringSnappy` | idem | idem |
| Repli opaque | `#DBDCE2` / `#2A2A34` | — | — |

### 7.5 Curseur de progression — rail fin + poignée en verre

| Attribut | Valeur | Clair | Sombre |
|---|---|---|---|
| Rail | hauteur 2 dp, rayon 1 dp, **opaque** `#C9CAD2` / `#3A3A46` | idem | idem |
| Partie lue | `primary` | idem | idem |
| Poignée | Ø **14 dp**, rayon 7 dp, verre `Prominent` | opacité 0,90 | opacité 0,86 |
| Contour de la poignée | 1 dp 12 % | idem | idem |
| Mouvement | position en ressort 220 ms · `SpringSnappy` ; à la saisie, rayon porté à 18 dp (agrandissement de la cible) | idem | idem |
| Repli opaque | poignée `#A8A9B2` / `#5A5A68` | — | — |

### 7.6 Feuilles et menus — coins 28 dp

| Attribut | Valeur | Clair | Sombre |
|---|---|---|---|
| Rayon haut | **28 dp** | idem | idem |
| Rayon bas | 16 dp (adjacent au bord) | idem | idem |
| Fond | verre `Standard` | opacité 0,87 | opacité 0,83 |
| Flou | 24 dp | idem | idem |
| Contour / reflet / liseré | 1 dp 8 % · blanc 6 % haut · 4 % bas | idem | idem |
| Scrim | 40 % (60 % pour une feuille modale) | idem | idem |
| Ouverture | **grandit depuis le rectangle de la ligne** (ou depuis le bas pour une feuille modale), 220 ms · `SpringSoft` | idem | idem |
| Repli opaque | `#FFFFFF` / `#1A1A21` | — | — |

### 7.7 Lecteur plein écran — pochette floutée + teinte adaptative

| Attribut | Valeur | Clair | Sombre |
|---|---|---|---|
| Fond | pochette **pré-floutée** (64 px, cache) | idem | idem |
| Teinte adaptative | couleur dominante de la pochette, ≤ 6 % d'écart, appliquée sur le voile | pochette éclaircie | pochette assombrie |
| Voile | 40 % (`#0B0B0E` 40 % / `#FFFFFF` 40 %) pour garantir l'AA | idem | idem |
| Changement de titre | fondu 400 ms (image pré-floutée) | idem | idem |
| Pochette | 96 dp, rayon 12 dp, **opaque** | idem | idem |
| Commandes | boutons ronds verre `Prominent` (§7.4) | idem | idem |
| Curseur | rail + poignée verre (§7.5) | idem | idem |
| Repli opaque | voile uni `#FFFFFF` 88 % / `#0B0B0E` 92 %, **sans** pochette floutée | idem | idem |

### 7.8 Transitions « déploiement depuis la position »

| Transition | Principe | Durée / courbe | Mouvement réduit |
|---|---|---|---|
| Mini-lecteur → lecteur plein écran | le **rectangle** de la capsule se déploie vers le rectangle plein écran ; pochette, titre et commandes suivent le même rectangle | 300 ms · `SpringSnappy` (0,55/700) | fondu simple 150 ms |
| Ligne d'historique → feuille d'actions | la feuille **grandit depuis le rectangle de la ligne** (pas depuis le bas) | 220 ms · `SpringSoft` (0,86/380) | apparition sans glissement |
| Barre d'onglets → contenu | glissement court 24 dp + fondu 150 ms (contenu qui change) | 220 ms · `SpringSnappy` | fondu seul |
| Barre du bas → compacte | hauteur + marge animées au défilement, **jamais** l'opacité ni le flou | suit le défilement | compactage instantané |

---

## 8. LIVRABLES

| # | Livrable | Where |
|---|---|---|
| L-1 | Tokens étendus **dans la plage autorisée** : `Prominent` recalé 0,86 / 0,90 ; **nouveaux** : `blur_intensity`, `blur_contrast` (préférences) ; **aucun** nouveau site de flou | `ui/theme/GlassTokens.kt`, `util/Preferences.kt` |
| L-2 | Tableau des écrans (§2) | ce document |
| L-3 | Schémas ASCII clair / sombre / repli par écran | ce document §9 + annexes à venir |
| L-4 | Règle « capsule = rayon = hauteur ÷ 2 » (forme dérivée) + adaptation de `check-glass-policy.sh` (aujourd'hui : rayon max **24 dp** → les capsules 28/32 dp seraient rejetées) | `scripts/check-glass-policy.sh` |
| L-5 | `GlassSurface` doit poser un **flou d'arrière-plan réel** (aujourd'hui : `Color.Black.copy(alpha = 0.5f)`, à savoir une opacité plate, **pas** de flou) + reflet + liseré + ombre + teinte adaptative | `ui/theme/GlassSurface.kt` |
| L-6 | Écran « Journal » (déjà livré) — **hors périmètre verre** | — |

---

## 9. CAPTURES QUE BUILD DOIT PRODUIRE (Paparazzi/Roborazzi)

Format : `écran × (verre | repli) × (clair | sombre) × (pochette claire | pochette foncée) × (portrait | paysage) × (clavier fermé | ouvert)`.

| # | Écran / élément | Surface | Thème | Pochette | Orientation | Clavier |
|---|---|---|---|---|---|---|
| G-01 | Barre du bas, capsule + pastille | Verre | Sombre | Claire | Portrait | Fermé |
| G-02 | Barre du bas, capsule + pastille | Verre | Sombre | Foncée | Portrait | Fermé |
| G-03 | Barre du bas, capsule + pastille | Verre | Clair | Claire | Portrait | Fermé |
| G-04 | Barre du bas, capsule + pastille | Verre | Clair | Foncée | Portrait | Fermé |
| G-05 | Barre du bas **compactée** (défilement) | Verre | Sombre | Claire | Portrait | Fermé |
| G-06 | Barre du bas | **Repli opaque** | Sombre | Claire | Portrait | Fermé |
| G-07 | Barre du bas | **Repli opaque** | Clair | Claire | Portrait | Fermé |
| G-08 | Mini-lecteur (capsule) | Verre | Sombre | Claire | Portrait | Fermé |
| G-09 | Mini-lecteur | **Repli opaque** | Sombre | Claire | Portrait | Fermé |
| G-10 | En-tête réduit (capsule) | Verre | Sombre | Claire | Portrait | Fermé |
| G-11 | En-tête réduit | **Repli opaque** | Sombre | Claire | Portrait | Fermé |
| G-12 | Lecteur plein écran, fond pochette floutée | Verre | Sombre | Claire | Portrait | Fermé |
| G-13 | Lecteur plein écran, fond pochette floutée | Verre | Sombre | Foncée | Portrait | Fermé |
| G-14 | Lecteur plein écran | **Repli opaque** | Sombre | Claire | Portrait | Fermé |
| G-15 | Boutons ronds + curseur à poignée | Verre | Sombre | Claire | Portrait | Fermé |
| G-16 | Boutons ronds + curseur | **Repli opaque** | Clair | Claire | Portrait | Fermé |
| G-17 | Feuille d'actions (28 dp) | Verre | Sombre | — | Portrait | Fermé |
| G-18 | Feuille d'actions | **Repli opaque** | Sombre | — | Portrait | Fermé |
| G-19 | Menu contextuel (28 dp) | Verre | Clair | — | Portrait | Fermé |
| G-20 | Dialogue (petite surface verre + bouton plein) | Verre | Sombre | — | Portrait | Fermé |
| G-21 | Champ de saisie (pilule 56 dp) | Verre | Sombre | — | Portrait | Ouvert |
| G-22 | Champ de saisie | **Repli opaque** | Sombre | — | Portrait | Ouvert |
| G-23 | Puces de filtre | Verre | Clair | — | Portrait | Fermé |
| G-24 | Réglages, cartes translucides | Verre (léger, sans flou) | Sombre | — | Portrait | Fermé |
| G-25 | Barre d'onglets Lien / Historique | Verre | Sombre | Claire | Portrait | Fermé |
| G-26 | Barre d'onglets | **Repli opaque** | Sombre | Claire | Portrait | Fermé |
| G-27 | Zone de saisie centrée | Verre | Sombre | — | Portrait | Fermé |
| G-28 | Zone de saisie centrée | Verre | Sombre | — | **Paysage** | Fermé |
| G-29 | Zone de saisie centrée | Verre | Sombre | — | Portrait | **Ouvert** |
| G-30 | Zone de saisie centrée | **Repli opaque** | Sombre | — | Portrait | Ouvert |
| G-31 | Carte d'aperçu | Verre | Sombre | Claire | Portrait | Fermé |
| G-32 | Carte d'aperçu | **Repli opaque** | Sombre | Claire | Portrait | Fermé |
| G-33 | Feuille File d'attente | Verre | Sombre | — | Portrait | Fermé |
| G-34 | Historique (lignes **opaques**) + en-tête collant | Verre (en-tête seul) | Sombre | — | Portrait | Fermé |
| G-35 | Historique | **Repli opaque** | Sombre | — | Portrait | Fermé |
| G-36 | Transition mini-lecteur → plein écran (3 images) | Verre | Sombre | Claire | Portrait | Fermé |

**36 captures.** Règles de capture : fond = **pochette réelle** du dépôt (pas de dégradé de substitution) ·
verre et repli pour la même scène (comparaison directe) · **jamais** de capture en réseau.

---

## 10. ÉCARTS CONSTATÉS AVEC LE CODE ACTUEL (signalés, non corrigés)

| ID | Gravité | Écart | Fichier:ligne |
|---|---|---|---|
| V-01 | **Bloquant** | `GlassSurface` **ne fait pas de flou** : il pose `Color.Black.copy(alpha = 0.5f)` (opacité plate). Aucun flou d'arrière-plan, donc **aucun verre réel** | `ui/theme/GlassSurface.kt:56` |
| V-02 | **Bloquant** | Aucun **reflet**, **liseré**, **ombre** ni **teinte adaptative** dans `GlassSurface` | `ui/theme/GlassSurface.kt:50-60` |
| V-03 | Majeur | Opacité réelle = 0,5 plat, hors de la plage 0,72–0,90 | `ui/theme/GlassSurface.kt:56` |
| V-04 | Majeur | La barre du bas n'est pas une capsule flottante (c'est une `NavigationBarView` Material) | `ui/component/base/AbsSlidingMusicPanelActivity.kt:137` |
| V-05 | Majeur | Pas de pastille de sélection en verre qui glisse | idem |
| V-06 | Majeur | Mini-lecteur, boutons ronds du lecteur, curseur à poignée : **aucune** surface verre | — |
| V-07 | Majeur | Réglages : pas de carte translucide par groupe | `ui/screen/settings/` |
| V-08 | Mineur | `check-glass-policy.sh` interdit tout rayon > 24 dp → les capsules (28/32 dp) seraient rejetées | `scripts/check-glass-policy.sh` |
| V-09 | Mineur | Réglages « Intensité » et « Contraste renforcé » absents (`blur_intensity`, `blur_contrast`) | `util/Preferences.kt:440-450` |
| V-10 | Mineur | `GlassTokens.Prominent` = 0,88 / 0,92 → hors de la bande globale (≤ 0,90) | `ui/theme/GlassTokens.kt:20-21` |

---

## 11. CRITÈRES DE CONFORMITÉ (vérifiables)

| ID | Critère | Preuve attendue |
|---|---|---|
| GC-1 | Aucun nouveau site de flou hors `GlassSurface` (+ helper pochette) | `bash scripts/check-glass-policy.sh` → exit 0 |
| GC-2 | Aucune capsule au-delà du rayon de flou max (24 dp) → **forme dérivée** acceptée | script étendu (V-08) |
| GC-3 | Opacité de chaque surface dans 0,72–0,90, **jamais < 0,70** | revue `GlassTokens` |
| GC-4 | Reflet statique 6 % haut + liseré 4 % bas, non animés | revue `GlassSurface` |
| GC-5 | Ombre unique 2–4 dp | revue `GlassSurface` |
| GC-6 | ≤ 3 surfaces verre simultanées | revue par écran + captures |
| GC-7 | Repli opaque aux 5 conditions, avec les couleurs exactes du §4.2 | test JVM `GlassPolicy` |
| GC-8 | Contraste AA ≥ 4,5:1 sur pochette claire **et** foncée | mesure sur les captures G-01…G-36 |
| GC-9 | Aucune animation du rayon de flou ni de l'opacité au défilement | revue `Motion.kt` + captures |
| GC-10 | Réglage « Effets de transparence » (auto/on/off) + « Intensité » + aperçu en direct | revue `Preferences` + captures Réglages |
| GC-11 | **Aucune** icône, police ou logo Apple dans les ressources | `grep` sur `res/drawable` et `res/font` |
| GC-12 | Images lentes < 1 % au défilement de la bibliothèque | écran Diagnostics (build debug), valeurs fournies par l'utilisateur |

---

## 12. Conclusion

Le liquid glass de B Music est **sobre mais visible** : des surfaces de contrôle qui flottent
(capsule 16 dp / 64 dp, pastille plus lumineuse qui glisse, mini-lecteur, en-tête qui se réduit),
translucides dans une fenêtre **0,72–0,90** (plancher dur 0,70), avec un reflet spéculaire fin
et un liseré très léger — et un repli opaque de même teinte, même contour, même mise en page quand
la technique ou l'utilisateur le demande. Le contenu, lui, reste **net et opaque**.
Les 6 écarts bloquants relevés dans le code (§10) sont **signalés, non corrigés** : l'implémentation
revient à l'agent BUILD.

*Spécification produite par l'agent DESIGNER. **Aucun code modifié**, aucune mesure claimée sur un appareil.*
