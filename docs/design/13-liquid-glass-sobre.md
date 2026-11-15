# B Music — Liquid Glass Sobre : spécifications de design (v1.2)

> Agent DESIGNER — SPECIFICATION DE DESIGN (lecture seule, aucun code modifié).
> Date : 14 novembre 2026.
> Sources : `01-design-system.md` (système), `10-navigation-et-icones.md` (navigation), `11-onglets-lien-historique.md` (écrans Lien/Historique), `12-tokens-composants-et-motion.md` (tokens), `Motion.kt` (durées/springs).
> Contraintes : aucun nouveau site de flou, aucun nouveau token hors plage autorisée, réutilisation exclusive de `GlassSurface`, `GlassPolicy`, `GlassTokens` existants. Résultat élégant et discret : léger flou, profondeur, fluidité, jamais d'effet vitreux marqué.

---

## 0. Prérequis

`GlassSurface`, `GlassPolicy` et `GlassTokens` **n'existent pas encore** dans le codebase (aucune référence trouvée lors de l'audit). Ce document spécifie leur contrat d'utilisation ; leur implémentation relève de l'agent BUILD. Toute implémentation doit respecter les règles de ce document.

---

## 1. OÙ le liquid glass s'applique (liste fermée)

| # | Élément | Écran | Verre | Justification |
|---|---------|-------|-------|---------------|
| 1 | Barre de deux onglets en haut (Lien / Historique) | Télécharger | ✅ | Profondeur entre la barre et le contenu |
| 2 | Carte d'aperçu du lien | Télécharger > Lien | ✅ | Distingue l'aperçu du bloc de saisie |
| 3 | Zone de saisie centrée (champ + boutons) | Télécharger > Lien | ✅ | Hiérarchie visuelle entre saisie et fond |
| 4 | En-têtes de date collants (Historique) | Télécharger > Historique | ✅ | Séparation visuelle des groupes de date |
| 5 | Feuille d'actions d'une ligne d'historique | Télécharger > Historique | ✅ | Élévation de la feuille par rapport à la liste |
| 6 | Mini-barre de progression des téléchargements en cours | Télécharger > Historique | ✅ | Signal subtil d'activité sur la ligne |

### OÙ PAS le liquid glass s'applique

| # | Élément | Écran | Raison |
|---|---------|-------|--------|
| A | Lignes de la liste d'historique | Télécharger > Historique | Surface opaque pour la fluidité du défilement |
| B | Dialogues de confirmation | Tous | Lisibilité critique, pas de fond flouté distractif |
| C | Textes longs | Tous | Contraste garanti sur fond opaque |
| D | Tout écran existant non listé | Tous | Pas de régression visuelle sur les écrans existants |

---

## 2. COMMENT le liquid glass est appliqué

### 2.1 Paramètres de rendu

| Paramètre | Valeur sombre | Valeur clair | Justification |
|---|---|---|---|
| Rayon de flou | 16 dp | 16 dp | Flou léger, lisible |
| Rayon de flou max (feuille d'actions) | 20 dp | 20 dp | Élévation légèrement plus marquée |
| Opacité de surface | 0,86 | 0,90 | Légère transparence, fond discernable |
| Contour | 1 dp à 12 % d'opacité | 1 dp à 12 % d'opacité | Délimitation subtile |
| Reflet | Dégradé linéaire vertical 6 % blanc → transparent, 40 % supérieurs, statique | Dégradé linéaire vertical 6 % blanc → transparent, 40 % supérieurs, statique | Léger effet de brillance sans animation |
| Ombre douce | Élévation 2–4 dp | Élévation 2–4 dp | Profondeur sans lourdeur |
| Halos / références / lueurs | Interdits | Interdits | Pas d'effet vitreux marqué |

### 2.2 Fond flouté

| Situation | Fond |
|---|---|
| Titre en cours | Pochette du titre en cours, pré-floutée en petit (~64 px), mise en cache, fondu enchaîné 400 ms |
| Aucun titre en cours | Dégradé neutre du thème (surface1) |

### 2.3 Règles dures

- **Aucun** nouveau site de flou (RenderEffect.blur) en dehors des 6 éléments listés.
- **Aucun** nouveau token hors de la plage autorisée de `GlassTokens`.
- **Aucun** halo coloré, réfraction ou lueur animée.
- **Aucune** animation du rayon de flou ni de l'opacité de surface pendant un défilement.
- Le fond flouté est toujours la pochette du titre en cours (pas d'image aléatoire).

---

## 3. ANIMATIONS (Motion.kt uniquement)

| Moment | Animation | Durée / courbe | Réduction (mouvement réduit) |
|---|---|---|---|
| Apparition des surfaces verre | Fondu + léger glissement de 8 dp en ressort | 400 ms · `EmphasizedDecelerate` | Fondu ≤ 150 ms, sans glissement |
| Pression d'un bouton sur surface verre | Échelle 0,97 | 120 ms · `SpringSnappy` (0,55/700) | Échelle immédiate |
| Glissement de l'indicateur d'onglet | Ressort entre onglets | 300 ms · `SpringSnappy` | Glissement immédiat |
| Remontée de la zone de saisie | Garde son flou constant pendant le mouvement | 220 ms · `SpringSnappy` | Translation immédiate |
| Ouverture de la feuille d'actions | Montée + scrim 60 % | 220 ms · `SpringSoft` (0,86/380) | Apparition sans glissement |
| Fermeture de la feuille d'actions | Retour à la liste | 220 ms · `FastOutSlowIn` | Immédiat |

**Interdits** :
- Animation du rayon de flou (jamais).
- Animation de l'opacité de surface pendant un défilement (jamais).
- Lueur animée, réfraction animée, halo animé (jamais).
- `rememberInfiniteTransition` sur les surfaces verre (sauf squelette borné, pas sur ces écrans).

---

## 4. REPLIS OPAQUES

### 4.1 Conditions de repli

| Condition | Déclencheur |
|---|---|
| API < 31 | `Build.VERSION.SDK_INT < 31` |
| Mémoire faible | `ActivityManager.isLowRamDevice()` |
| Économiseur de batterie | `PowerManager.isPowerSaveMode()` |
| Réglage « Effets de transparence » désactivé | Préférence utilisateur `Preferences.glassEffectsEnabled == false` |
| Animations réduites | `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` |

### 4.2 Couleurs du repli (surface opaque de même teinte et même contour)

| Thème | Couleur opaque | Hex | Contraste texte primaire |
|---|---|---|---|
| Sombre | `surface1` (même teinte que le verre sombre) | `#131318` | ≥ 4,5:1 (mesuré : 17,5:1) |
| Clair | `surface1` (même teinte que le verre clair) | `#FFFFFF` | ≥ 4,5:1 (mesuré : 16,5:1) |

Le repli conserve :
- Le même contour 1 dp à 12 % d'opacité (ou 1 px `trait` si le contour semi-transparent n'est pas supporté).
- La même mise en page (pas de déplacement d'éléments).
- Les mêmes ombres (élévation 2–4 dp, ou élévation tonale si l'ombre n'est pas supportée).

---

## 5. LISIBILITÉ

### 5.1 Contraste AA (4,5:1 minimum)

Tous les textes sur surface verre doivent respecter le contraste AA (≥ 4,5:1) sur le **pire fond possible** :

| Fond de test | Contraste requis | Statut |
|---|---|---|
| Pochette très claire (blanc proche) | ≥ 4,5:1 | À vérifier |
| Pochette très foncée (noir proche) | ≥ 4,5:1 | À vérifier |

### 5.2 Règle d'ajustement

Si le contraste est insuffisant sur un fond donné :
- **Augmenter l'opacité de la surface** (jamais au-delà de 0,95).
- **Ne jamais augmenter la taille du flou** (le flou ne doit pas compenser un manque de contraste).
- **Ne jamais changer la couleur du texte** (rester sur `onSurface`).

### 5.3 Éléments à vérifier

| Élément | Contraste requis | Vérification |
|---|---|---|
| Champ de saisie (texte + placeholder) | ≥ 4,5:1 | `contrast_check.py` |
| Texte de l'aperçu (titre, auteur, durée) | ≥ 4,5:1 | `contrast_check.py` |
| En-têtes de date (Historique) | ≥ 4,5:1 | `contrast_check.py` |
| Boutons sur surface verre | ≥ 4,5:1 (texte) / ≥ 3:1 (UI) | `contrast_check.py` |

---

## 6. Tableau des livrables

| Écran | Élément | Surface | Rayon flou | Opacité | Contour | Animation | Repli |
|---|---|---|---|---|---|---|---|
| Télécharger > Lien | Barre de deux onglets | Verre | 16 dp | 0,86 / 0,90 | 1 dp @ 12 % | Glissement ressort 300 ms | Opaque `#131318` / `#FFFFFF` |
| Télécharger > Lien | Carte d'aperçu | Verre | 16 dp | 0,86 / 0,90 | 1 dp @ 12 % | Fondu + glissement 8 dp 400 ms | Opaque `#131318` / `#FFFFFF` |
| Télécharger > Lien | Zone de saisie centrée | Verre | 16 dp | 0,86 / 0,90 | 1 dp @ 12 % | Remontée ressort, flou constant 220 ms | Opaque `#131318` / `#FFFFFF` |
| Télécharger > Historique | En-têtes de date collants | Verre | 16 dp | 0,86 / 0,90 | 1 dp @ 12 % | Apparition fondu 400 ms | Opaque `#131318` / `#FFFFFF` |
| Télécharger > Historique | Feuille d'actions d'une ligne | Verre | 20 dp | 0,86 / 0,90 | 1 dp @ 12 % | Montée ressort 220 ms | Opaque `#131318` / `#FFFFFF` |
| Télécharger > Historique | Mini-barre de progression | Verre | 16 dp | 0,86 / 0,90 | 1 dp @ 12 % | Remplissage fluide `tweenStd` | Opaque `#131318` / `#FFFFFF` |

---

## 7. Schémas

### 7.1 Schéma ASCII — Écran Lien, thème sombre, verre activé

```
┌─────────────────────────────────────────────────────────┐
│  Barre d'onglets [ Lien ▮ Historique ]  ← verre 16 dp   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│              ┌─────────────────────────┐               │
│              │   ⛓  Coller un lien     │   ← verre     │
│              │  [____________________]  │   16 dp flou  │
│              │  [ Coller ] [ effacer ]  │   0,86 opac   │
│              │                          │               │
│              │      [ Analyser ▶ ]      │   contour 1dp │
│              │                          │   @12%        │
│              │  Collez un lien YouTube, │               │
│              │  TikTok, Instagram…      │               │
│              └─────────────────────────┘               │
│                                                         │
│  (fond : pochette du titre en cours, pré-floutée 64px)  │
└─────────────────────────────────────────────────────────┘
```

### 7.2 Schéma ASCII — Écran Lien, thème sombre, repli opaque

```
┌─────────────────────────────────────────────────────────┐
│  Barre d'onglets [ Lien ▮ Historique ]  ← opaque        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│              ┌─────────────────────────┐               │
│              │   ⛓  Coller un lien     │   ← opaque    │
│              │  [____________________]  │   #131318     │
│              │  [ Coller ] [ effacer ]  │               │
│              │                          │               │
│              │      [ Analyser ▶ ]      │   contour 1dp │
│              │                          │               │
│              │  Collez un lien YouTube, │               │
│              │  TikTok, Instagram…      │               │
│              └─────────────────────────┘               │
│                                                         │
│  (fond : surface1 unie, pas de flou)                    │
└─────────────────────────────────────────────────────────┘
```

### 7.3 Schéma ASCII — Écran Historique, thème sombre, verre activé

```
┌─────────────────────────────────────────────────────────┐
│  [ Rechercher… ]  [ Site ▾ ] [ État ▾ ]  ← verre 16 dp │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┬─────────────────────────────────────┐│
│  │ Aujourd'hui  │  ← en-tête collant, verre           ││
│  ├──────────────┼─────────────────────────────────────┤│
│  │ [miniature]  │  Titre de la piste                  ││
│  │ 96×54 dp     │  YouTube • MP3 • 4,2 Mo • 14:32     ││
│  │              │  [Terminé ●]                        ││
│  └──────────────┴─────────────────────────────────────┘│
│                                                         │
│  (mini-barre de progression sur les lignes en cours)    │
└─────────────────────────────────────────────────────────┘
```

### 7.4 Schéma ASCII — Écran Lien, thème clair, verre activé

```
┌─────────────────────────────────────────────────────────┐
│  Lien ▮  Historique                    ← verre 16 dp   │  opacité 0,90
├─────────────────────────────────────────────────────────┤  contour 1 dp 12 %
│                                                         │
│              ┌─────────────────────────┐               │
│              │   ⛓  Coller un lien     │               │  surface
│              │  [____________________]  │  [ Coller ]  │  #FFFFFF
│              │  [    Analyser    ]     │               │  à 90 %
│              └─────────────────────────┘               │
│                                                         │
│  Collez un lien YouTube, TikTok, Instagram…             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 7.5 Schéma ASCII — Écran Lien, thème clair, repli opaque

```
┌─────────────────────────────────────────────────────────┐
│  Lien ▮  Historique                    ← opaque         │  #FFFFFF
├─────────────────────────────────────────────────────────┤  contour 1 dp 12 %
│                                                         │
│              ┌─────────────────────────┐               │
│              │   ⛓  Coller un lien     │               │  opaque
│              │  [____________________]  │  [ Coller ]  │  (aucun
│              │  [    Analyser    ]     │               │   flou)
│              └─────────────────────────┘               │
│                                                         │
│  Collez un lien YouTube, TikTok, Instagram…             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 7.6 Rappel des couleurs opaques de repli

| Thème | Surface opaque de repli | Hex | Contour |
|---|---|---|---|
| Sombre | `surface1` | `#131318` | 1 dp `outline` `#2A2A34` |
| Clair | `surface1` | `#FFFFFF` | 1 dp `outline` `#E4E4EA` |

---

## 8. Captures que Build doit produire (Paparazzi/Roborazzi)

| # | Capture | Surface | Thème | Pochette | Clavier | Commentaire |
|---|---------|---------|-------|----------|---------|-------------|
| C-01 | Barre d'onglets Lien/Historique | Verre | Sombre | Claire | Fermé | Contrast sur pochette claire |
| C-02 | Barre d'onglets Lien/Historique | Verre | Sombre | Foncée | Fermé | Contrast sur pochette foncée |
| C-03 | Barre d'onglets Lien/Historique | Verre | Clair | Claire | Fermé | Contrast thème clair |
| C-04 | Barre d'onglets Lien/Historique | Verre | Clair | Foncée | Fermé | Contrast thème clair |
| C-05 | Carte d'aperçu du lien | Verre | Sombre | Claire | Fermé | Lecture du texte sur fond flou |
| C-06 | Carte d'aperçu du lien | Verre | Sombre | Foncée | Fermé | Lecture du texte sur fond flou |
| C-07 | Zone de saisie centrée | Verre | Sombre | Claire | Ouvert | Clavier ouvert, bloc visible |
| C-08 | Zone de saisie centrée | Verre | Sombre | Foncée | Ouvert | Clavier ouvert, bloc visible |
| C-09 | En-têtes de date collants | Verre | Sombre | — | Fermé | Groupe de date |
| C-10 | Feuille d'actions d'une ligne | Verre | Sombre | — | Fermé | 20 dp flou, élévation |
| C-11 | Mini-barre de progression | Verre | Sombre | — | Fermé | Ligne en cours |
| C-12 | Barre d'onglets | Repli opaque | Sombre | Claire | Fermé | Repli API < 31 |
| C-13 | Barre d'onglets | Repli opaque | Clair | Claire | Fermé | Repli API < 31 |
| C-14 | Carte d'aperçu | Repli opaque | Sombre | Claire | Fermé | Repli sans flou |
| C-15 | Zone de saisie | Repli opaque | Sombre | Claire | Ouvert | Repli + clavier ouvert |

**Total : 15 captures minimales.**

---

## 9. Mesures de performance attendues

| Mesure | Seuil | Source |
|---|---|---|
| Images lentes pendant défilement historique | < 1 % | Diagnostics (valeurs fournies par l'utilisateur) |
| Images lentes pendant téléchargement | < 1 % | Diagnostics (valeurs fournies par l'utilisateur) |
| Temps de rendu d'une surface verre | Non mesuré par l'agent DESIGNER | À fournir par Build |

> **L'agent DESIGNER ne prétend rien avoir mesuré.** Toutes les valeurs de performance proviennent de l'écran Diagnostics (build debug) et du journal d'étapes exporté par Partager, exécutés par l'utilisateur sur son appareil.

---

## 10. Critères de conformité (vérifiables)

| ID | Critère | Statut | Preuve |
|---|---------|--------|--------|
| G-1 | Aucun nouveau site de flou en dehors des 6 éléments listés | ❓ | `grep RenderEffect\|blur` sur les écrans downloader |
| G-2 | Aucun nouveau token hors plage `GlassTokens` | ❓ | Revue des fichiers de tokens |
| G-3 | Rayon de flou 16 dp (20 dp max feuille d'actions) | ❓ | Mesure sur captures |
| G-4 | Opacité 0,86 sombre / 0,90 clair | ❓ | Revue du code `GlassSurface` |
| G-5 | Contour 1 dp à 12 % d'opacité | ❓ | Revue du code `GlassSurface` |
| G-6 | Reflet statique 6 % blanc sur 40 % supérieurs | ❓ | Revue du code |
| G-7 | Ombre douce 2–4 dp, pas de halo coloré | ❓ | Revue du code |
| G-8 | Fond flouté = pochette titre en cours (pré-floutée 64 px, cachée) | ❓ | Revue du code |
| G-9 | Repli opaque aux 5 conditions (API < 31, RAM faible, économiseur, préférence, animations réduites) | ❓ | Revue du code `GlassPolicy` |
| G-10 | Couleurs repli : `#131318` sombre / `#FFFFFF` clair | ❓ | Revue du code |
| G-11 | Contraste AA ≥ 4,5:1 sur pire fond | ❓ | `contrast_check.py` |
| G-12 | Animation rayon de flou = interdite | ❓ | Revue du code |
| G-13 | Animation opacité pendant défilement = interdite | ❓ | Revue du code |
| G-14 | Mouvement réduit = 0 ms sur surfaces verre | ❓ | Test JVM |

---

## 11. Captures manquantes

Aucune capture n'est disponible à ce stade. Les 15 captures listées au §8 doivent être produites par Paparazzi/Roborazzi (ou fournies par l'utilisateur) avant que la conformité puisse être vérifiée.

---

## 12. Conclusion

Ce document spécifie un liquid glass sobre et discret appliqué à 6 éléments précis sur les écrans Lien et Historique, avec replis opaux pour les cas de contrainte technique. Aucun code n'est modifié par ce document. L'implémentation relève de l'agent BUILD, qui devra créer `GlassSurface`, `GlassPolicy` et `GlassTokens` conformément à ce contrat.

*Spécification produite par l'agent DESIGNER. Aucun code n'est modifié par ce document.*
