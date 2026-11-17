# B Music — Liquid glass sobre : widgets

> Agent DESIGNER — 17 novembre 2026. **Spécification, aucune modification de code.**
>
> Périmètre : widgets d'écran d'accueil (Glance) — lecteur compact 4×1, lecteur moyen 4×2, grand 4×4, widget de téléchargement (si flag actif). Clair / sombre, couleurs dynamiques Android 12+ quand disponibles.
>
> **Contrainte matérielle vérifiée (inventaire PLAN)** : les widgets sont en **Glance** (`GlanceAppWidget` / `GlanceAppWidgetReceiver`), rendus via `RemoteViews`. **Le flou du fond d'écran est impossible** : `RenderEffect` n'est pas transmissible à travers `RemoteViews`. Le « verre » des widgets est donc **simulé** : fond translucide teinté + texture de pochette pré-floutée + reflet statique. Aucune animation de widget n'est possible (`updatePeriodMillis=0`, mises à jour par `WidgetUpdater`) — voir §3.
>
> Cohérence : cette spec étend `13-liquid-glass-sobre-global.md` au cas particulier des widgets. Elle en reprend la règle d'or — **le verre ne sert que les couches de contrôle, le contenu reste lisible** — avec des plages d'opacité adaptées à un fond d'écran arbitraire (plus basses que pour une surface d'app, car le lanceur montre n'importe quelle image).

---

## 0. Principe directeur

Sur un écran d'accueil, le fond change à chaque utilisateur, chaque instant, chaque application. Un widget trop opaque devient une tuile grise ; trop transparent, il devient illisible. La cible est un **verre créditable mais discret** : on voit que la surface flotte, on devine la couleur du fond, mais le texte reste ferme.

Hiérarchie en trois couches (de l'arrière vers l'avant) :
1. **Fond teinté translucide** — la « matière » du verre.
2. **Texture de pochette pré-floutée**, très faible, masquée par un dégradé sous le texte.
3. **Contenu net et opaque** : titre, artiste, pochette, commandes.

Aucune icône, police ni logo Apple. Aucun nouveau site de flou : la pochette est **pré-floutée une fois** et servie en URI par `CoverProvider` (déjà le cas dans le code, `WidgetArtwork.kt:30-42`).

---

## 1. MATIÈRE

### 1.1 Valeurs

| Propriété | Sombre | Clair | Règle |
|---|---|---|---|
| Opacité du fond | **0,60 – 0,80** | **0,65 – 0,85** | jamais sous **0,55** (lisibilité sur n'importe quel fond d'écran) |
| Texture pochette pré-floutée | 35 – 50 % d'opacité | 35 – 50 % | flou **~96 px** appliqué à l'avance, jamais à l'exécution |
| Dégradé de masque | noir 0,25 → 0,45 sous le texte | blanc 0,35 → 0,55 sous le texte | renforcé si le contraste AA n'est pas atteint |
| Contour | 1 dp, blanc **12 – 16 %** | 1 dp, noir **12 – 16 %** | idem `GlassTokens.EdgeProminent` (0x1EFFFFFF / 0x1E000000) |
| Reflet spéculaire (bord haut) | dégradé blanc **8 – 10 %**, 1 dp de haut, statique | idem | jamais animé |
| Liseré lumineux (bord bas) | blanc 4 – 6 %, 1 dp | blanc 6 – 8 %, 1 dp | très léger |
| Ombre | **aucune ajoutée** | **aucune ajoutée** | gérée par le lanceur (`android:system_app_widget_elevation`) |

### 1.2 Réalisation Glance (réaliste)

- Le fond teinté est un **drawable de forme** (`widget_shape_card.xml`, `widget_shape_stadium.xml`, etc.) dont la couleur est un `ColorProvider` avec alpha — exactement le mécanisme déjà en place (`WidgetColors.kt:18-27`, `WidgetComponents.kt:46`).
- Le reflet et le liseré sont des **shape drawables à dégradé** empilés dans le fond (une seule `Image` Glance par couche), pas des filtres.
- La texture pochette est une **seconde `Image`** dont la source est une URI `CoverProvider` pré-floutée ; elle est clipée par la forme du widget et recouverte par le dégradé de masque.
- **Aucun `RenderEffect`, aucun `Blur`, aucun flou à la compilation ni à l'affichage.**

### 1.3 Comparaison d'approches (décision documentée)

| Approche | Avantage | Limite | Verdict |
|---|---|---|---|
| **A. Fond teinté alpha + drawables de reflet** (retenue) | Compatible `RemoteViews`, coût nul, contraste maîtrisable, cohérent avec `GlassTokens` | Verre « simulé », pas de vraie réfraction | ✅ **Retenue** |
| B. Vrai flou du fond d'écran | Rendu « iOS-like » fidèle | **Impossible** via `RemoteViews` ; nécessiterait un retour à une vue complète + capture, interdit par l'architecture Glance | ❌ Écarté (impossible technique) |
| C. Migration vers une vue Compose classuelle avec `RenderEffect` | Flou réel | Casserait le rendu widget (RemoteViews obligatoire), réintroduirait 5 `AppWidgetProvider`, régression `playback/` | ❌ Écarté (régression garantie) |

---

## 2. FORMES

| Élément | Règle |
|---|---|
| Coins du widget | `android:system_app_widget_background_radius` (valeur du lanceur). **Repli 28 dp** si la ressource système est absente. |
| Pochette | rayon **concentrique** = rayon extérieur du widget − marge intérieure (ex. 28 − 12 = 16 dp). |
| Boutons de contrôle | cercles **44 dp**, zone tactile effective **≥ 48 dp** (padding transparent inclus dans le `RemoteViews`). |
| Lecture / pause | **52 dp**, opacité **+0,05** par rapport aux autres boutons, contour blanc 16 % au lieu de 12 %. |
| Progression | rail fin **3 dp**, poignée non requise sur widget (pas de interaction de scrubbing fiable via RemoteViews). |
| Épaisseur de contour | 1 dp uniforme, identique sur les 4 bords. |

> Les 4 formes existantes (`Button`, `Card`, `Scallop`, `Stadium` — `WidgetComponents.kt:39-46`) sont conservées telles quelles ; seule leur couleur(alpha) change selon le tableau §1.1.

---

## 3. CONTENU ET ÉTATS

### 3.1 Contenu par taille

| Taille | Titre | Artiste | Pochette | Progression | Commandes |
|---|---|---|---|---|---|
| **4×1** (compact) | 1 ligne, 14 sp | secondaire 11 sp, 1 ligne | 40 dp | non | lecture/pause 52 dp + suivant |
| **4×2** (moyen) | 1 ligne, 16 sp | secondaire 12 sp | 64 dp | oui (3 dp) | précédent / lecture-pause / suivant (44 / 52 / 44 dp) |
| **4×4** (grand) | **2 lignes**, 16 sp | secondaire 12 sp | pleine largeur ou grille | oui (3 dp) | commandes + file d'attente / titres récents si le widget les affiche |
| **Téléchargement** (flag actif) | 1 ligne « Téléchargement » | site + état | miniature site générique | oui (3 dp) |ouvrir l'app ; aucun contrôle de lecture |

### 3.2 États

| État | Rendu | Texte |
|---|---|---|
| Rien en lecture | fond uni teinté (pas de texture pochette), opacité **+0,05** pour compenser l'absence de masque | « Appuie pour lancer ta musique » (`app_widget_nothing_playing` / `app_widget_tap_to_play` existants) |
| Chargement | fond teinté + placeholder de pochette (note musicale teintée), pas de texte additionnel | titre = « … » |
| Erreur / moteur indisponible | fond teinté, contour passage à **blanc 16 %**, icône d'attention discrète | « Lecture indisponible » — jamais de code d'erreur technique dans le widget |
| Lecture normale | texture pochette + masque + contenu | titre réel |

### 3.3 Transitions — réalisme

**Les widgets ne supportent pas les animations.** Le rendu est un snapshot RemoteViews : toute « transition » est un **changement de drawable entre deux snapshots**, sans interpolation. Conséquences :
- pas de fondu, pas de ressort, pas de déplacement progressif ;
- le changement d'état est **instantané** au prochain `WidgetUpdater.push/refresh` ;
- pour éviter l'effet « flash », on conserve la dernière image connue (déjà le cas : `WidgetUpdater.kt:96-99` garde la timeline précédente tant que la durée n'est pas connue) ;
- aucune animation du rayon de flou — inexistant sur widget.

---

## 4. LISIBILITÉ

- Contraste **AA (≥ 4,5:1)** mesuré sur deux fonds d'écran extrêmes : **très clair** (blanc pur) et **très foncé** (noir pur), plus un fond **coloré saturé**.
- Texte posé sur le verre : titre et artiste. Si le ratio AA n'est pas atteint, **augmenter l'opacité du fond** (jusqu'à 0,85 clair / 0,80 sombre) ou **renforcer le dégradé de masque** sous le texte — **jamais** l'inverse (baisser l'opacité pour « faire plus verre »).
- Le bouton lecture/pause, plus lumineux, doit garder un contraste ≥ 3:1 avec son icône.
- Le liseré et le reflet ne doivent jamais être perçus comme du texte : rester sous 10 % d'opacité.

---

## 5. REPLIS OPAQUES

Déclencheurs : lanceur sans transparence, API < 31, économiseur de batterie, pochette absente ou trop lourde, contraste renforcé, économie de données.

| Surface | Repli clair (couleur exacte) | Repli sombre (couleur exacte) |
|---|---|---|
| Fond widget | `#F5F5F7` (gris très clair) | `#1C1C1E` (gris très foncé) |
| Contour | `#1F000000` (12 %) | `#1FFFFFFF` (12 %) |
| Reflet / liseré | supprimés | supprimés |
| Texture pochette | supprimée | supprimée |
| Bouton contrôle | `#E5E5EA` | `#2C2C2E` |
| Bouton lecture/pause | `#FF2D55`-accent du thème (accent existant `BMusicTheme`) | même accent, +10 % luminosité |
| Texte titre | `#1C1C1E` | `#FFFFFF` |
| Texte artiste | `#6E6E73` | `#AEAEB2` |

Même rayon, même mise en page, mêmes tailles : **seule la matière change**. Le repli est déterminé une fois par `WidgetColors` (déjà `dayNightColorProvider`, `WidgetColors.kt:68-72`) ; aucune détection à chaque frame.

---

## 6. LIVRABLES

### 6.1 Tableau widget × taille × élément

| Widget | Taille | Élément | Matière | Opacité (sombre / clair) | Rayon | Repli |
|---|---|---|---|---|---|---|
| Lecteur | 4×1 | fond | teinté + reflet | 0,60–0,80 / 0,65–0,85 | système (28 dp) | opaque clair/sombre |
| Lecteur | 4×1 | pochette 40 dp | pré-floutée 96 px | 35–50 % | 16 dp concentrique | placeholder |
| Lecteur | 4×1 | lecture/pause 52 dp | verre +0,05 | 0,80 / 0,85 | cercle | accent opaque |
| Lecteur | 4×2 | fond | teinté + texture + masque | 0,65–0,80 / 0,70–0,85 | système (28 dp) | opaque |
| Lecteur | 4×2 | progression 3 dp | rail translucide | 0,50 / 0,55 | — | opaque |
| Lecteur | 4×4 | fond | teinté + texture large | 0,60–0,78 / 0,65–0,83 | système (28 dp) | opaque |
| Lecteur | 4×4 | titres/file | texte net sur masque renforcé | masque +0,10 | — | texte opaque |
| Téléchargement | 4×2 | fond | teinté, pas de texture | 0,70–0,80 / 0,75–0,85 | système (28 dp) | opaque |
| Téléchargement | 4×2 | miniature site | générique monochrome | 100 % | concentrique | identique |

### 6.2 Schémas ASCII

**4×1 — clair, fond d'écran foncé**
```
┌──────────────────────────────┐  ← reflet blanc 8-10 % (haut)
│ ▢  Titre · artiste      ▶    │  ← fond 0,70 / contour noir 12 %
└──────────────────────────────┘  ← liseré blanc 4-6 % (bas)
```

**4×2 — sombre, fond d'écran clair**
```
┌──────────────────────────────┐  ← reflet blanc 8-10 %
│╭────╮ Titre (1 ligne)        │
││cover│ artiste               │  ← texture 35-50 % + masque noir 0,25→0,45
│╰────╯ ▬▬▬▬▬▬▬▬ 3 dp          │
│   ⏮    ▶(52)    ⏭            │  ← boutons cercles 44/52/44 dp
└──────────────────────────────┘  ← liseré blanc 4-6 %
```

**4×4 — clair, fond d'écran coloré**
```
┌──────────────────────────────┐
│╭────────────────────────────╮│  ← pochette pré-floutée, masque renforcé
││ Titre sur 2 lignes         ││
││ artiste                    ││
││ ▬▬▬▬▬▬▬ 3 dp              ││
││ ⏮   ▶   ⏭   ♥   ⇄         ││
││ ── File d'attente ──       ││  ← titres récents, texte net
│╰────────────────────────────╯│
└──────────────────────────────┘
```

**Repli opaque (clair / sombre)** : mêmes schémas, reflet et liseré supprimés, fond `#F5F5F7` / `#1C1C1E`, contour conservé.

### 6.3 Captures que Build doit produire

| ID | Capture | Outil |
|---|---|---|
| W-01…W-03 | 4×1 clair sur fond clair / foncé / coloré | Roborazzi (snapshot Glance) |
| W-04…W-06 | 4×2 sombre sur fond clair / foncé / coloré | Roborazzi |
| W-07…W-09 | 4×4 clair sur fond clair / foncé / coloré | Roborazzi |
| W-10…W-12 | Téléchargement (flag on) sombre sur 3 fonds | Roborazzi |
| W-13…W-18 | Repli opaque : 3 tailles × clair/sombre | Roborazzi |
| W-19…W-21 | États : rien en lecture / chargement / erreur (4×2) | Roborazzi |
| W-22…W-24 | Couleurs dynamiques actives vs désactivées (4×2, clair/sombre) | Roborazzi |

> Si Roborazzi ne peut pas rendre un `RemoteViews` (limite connue), Build produit une **capture de mise en page de la vue** (XML `widget_*_preview.xml`) annotée des mêmes valeurs d'opacité, et le signale dans la fiche de recette. **Aucune mesure prétendue sur un appareil.**

### 6.4 Aucun élément propriétaire

Aucune icône, police ni logo Apple. Les formes (Card, Button, Scallop, Stadium) et les icônes (note musicale, lecture, précédent, suivant) sont les éléments existants de B Music. Le mot « verre » décrit une matière, pas une copie d'un élément propriétaire.

---

## 7. Ce que je signale (sans modifier le code)

| # | Écart / dépendance | Gravité | Agent |
|---|---|---|---|
| 1 | `GlassTokens` plafonne à 0,92 (clair) et 0,88 (sombre) — au-dessus de la plage widget 0,85/0,80. Il faut des **tokens dédiés widgets**, pas réutiliser ceux de l'app. | Moyen | BUILD |
| 2 | Le rayon système (`system_app_widget_background_radius`) n'est pas garanti sur tous les lanceurs — le repli 28 dp doit être câblé explicitement dans les drawables. | Moyen | BUILD |
| 3 | La pochette pré-floutée à 96 px n'existe pas encore : `CoverProvider` sert du 512 px et `WidgetDataSource` du 256 px. Ajouter une variante 96 px **sans toucher à `playback/`**. | Moyen | BUILD |
| 4 | Le widget de téléchargement dépend du flag `Preferences.DOWNLOADER_ENABLED` : hors flag, aucun widget ne doit apparaître (cohérent avec la règle « flag off = aucun onglet »). | Haut | BUILD |
| 5 | Aucune animation n'est possible : ne pas attendre de transition « déployante » comme sur l'app (cf. `13-liquid-glass-sobre-global.md` §7) — limitation RemoteViews. | Info | PM (recette) |

**Fin du document. Aucun code modifié.**
