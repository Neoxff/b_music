# B Music — Progression de téléchargement (Lien + Historique)

> Agent DESIGNER — 17 novembre 2026. **Spécification, aucun code modifié.**
> Design system existant : `BMusicTheme`, `GlassTokens`, `Motion.kt`. Verre léger uniquement via `GlassSurface`. Animations uniquement via `Motion.kt`, jamais d'animation du rayon de flou.
> Périmètre : carte de téléchargement en cours (onglets Lien et Historique), contrôles, animation de remplissage, notification, accessibilité, icônes à fournir.

---

## 0. Ancrage sur le design system réel

| Token existant | Valeur | Usage dans cette spec |
|---|---|---|
| `Motion.DurationBase` | 220 ms | interpolation du remplissage entre deux mises à jour |
| `Motion.DurationLong` | 300 ms | borne haute de l'interpolation (§3) |
| `Motion.DurationShort` | 120 ms | transition du bouton Pause ⇄ Reprendre |
| `Motion.EmphasizedDecelerate` | `CubicBezier(0.05, 0.7, 0.1, 1)` | easing du remplissage |
| `Motion.scaleDuration(ms, reduced)` | 0 si réduit | remplissage sans interpolation en animations réduites |
| `Motion.SpringSoft` | `damping 0,86 / stiffness 380` | ressort de la coche « terminé » |
| `GlassTokens.RadiusMinimal/Standard/Prominent` | 16 / 20 / 24 dp | rayon des cartes et de la feuille de confirmation |
| `GlassTokens.Opacity*` | 0,78–0,92 | opacité des `GlassSurface` (barre, boutons) |
| `GlassTokens.Edge*` | 8 % (12 % Prominent) | contour des surfaces verre |

Couleur d'accent : `BMusicTheme` (inchangé). Les teintes « avertissement » et « erreur » ci-dessous sont **dérivées du thème**, pas de nouvelles couleurs arbitraires.

---

## 1. CARTE DE TÉLÉCHARGEMENT EN COURS

### 1.1 Structure (Lien et Historique — même composant)

| Élément | Taille / valeur | Détail |
|---|---|---|
| Carte | largeur max **560 dp**, marges **20 dp**, rayon **20 dp** (`RadiusStandard`) | `GlassSurface` (surface de contrôle) ; dans l'Historique, la ligne reste opaque (§2 du doc 13) |
| Miniature | **96 × 54 dp** (16:9), rayon **12 dp** | pochette locale déjà mise en cache ; absence → fond teinté + note musicale |
| Titre | **1–2 lignes**, **16 sp**, poids medium, `maxLines = 2`, ellipsis | texte net opaque |
| Site | **12 sp**, `onSurfaceVariant`, puce générique monochrome + point de statut | jamais de logo de marque |
| Barre de progression | **hauteur 8 dp**, piste arrondie (rayon **4 dp**), remplissage en accent avec **léger dégradé** (accent → accent +8 % luminosité), extrémité **arrondie** | avance en continu, voir §3 |
| Pourcentage | **24 sp**, **chiffres tabulaires** (`fontFeatureSettings = "tnum"`), aligné à droite, largeur fixe **64 dp** | évite tout déplacement du texte pendant le remplissage |
| Ligne secondaire | **12 sp**, `onSurfaceVariant`, une seule ligne | vitesse · taille · temps restant, séparés par `·` |

### 1.2 Ligne secondaire — format

- Vitesse : `2,4 Mo/s` (une décimale, unité locale FR/EN).
- Taille : `30,2 / 48,0 Mo` (une décimale).
- Temps restant : `8 s` (arrondi ; `—` si inconnu).
- Forme complète : `2,4 Mo/s · 30,2 / 48,0 Mo · 8 s`.

### 1.3 États de la carte — tableau état × élément

| État | Barre | Pourcentage | Ligne secondaire | Icônes |
|---|---|---|---|---|
| **En attente** | indéterminée sobre : segment accent 25 % qui glisse sur piste neutre (2 s, `@android:interpolator` linéaire) | masqué (`—`) | « En attente » | Annuler |
| **En cours** | remplissage accent + dégradé, avance continue | `63 %` | vitesse · taille · reste | Pause, Annuler |
| **En pause** | barre **figée**, teinte **gris-accent** (accent désaturé 40 %), pas de dégradé | `63 %` (figé) | « En pause » | Reprendre, Annuler |
| **Erreur** | barre **rouge atténée** (erreur du thème à 70 % d'opacité), figée au point d'échec | `63 %` | message court + action « Réessayer » | Réessayer, Annuler |
| **Terminé** | barre **pleine**, transition vers accent puis **coche** au centre (ressort `SpringSoft`) | masqué ; coche à la place | « Terminé · 48,0 Mo » | aucune (actions de l'historique) |

### 1.4 Schémas ASCII par état

**En cours — clair**
```
╭──────────────────────────────────────╮  ← GlassSurface, rayon 20
│ ╭──────╮  Titre de la vidéo     63 %  │  ← 96×54 / 16 sp / 24 sp tnum
│ │cover │  youtube · Public            │
│ ╰──────╯  ████████████░░░░░░  8 dp   │  ← accent + dégradé, extrémité ronde
│           2,4 Mo/s · 30,2/48,0 Mo · 8 s│
│                        ( ‖ ) ( ✕ )    │  ← boutons 40 dp / verre
╰──────────────────────────────────────╯
```

**En pause — sombre**
```
╭──────────────────────────────────────╮
│ ╭──────╮  Titre                63 %  │
│ │cover │  site                        │
│ ╰──────╯  ▓▓▓▓▓▓▓▓▓░░░░░░░  figée   │  ← gris-accent, pas de dégradé
│           En pause                    │
│                        ( ▶ ) ( ✕ )    │  ← icône Reprendre
╰──────────────────────────────────────╯
```

**Erreur**
```
│ ╭──────╮  Titre                63 %  │
│ │cover │  site                        │
│ ╰──────╯  ▒▒▒▒▒▒▒▒░░░░░░░  rouge    │  ← erreur 70 %, figée
│           Réseau indisponible  ( ↻ )  │
```

**Terminé**
```
│ ╭──────╮  Titre                  ✓    │  ← coche, ressort SpringSoft
│ │cover │  site                        │
│ ╰──────╯  ████████████████  pleine   │
│           Terminé · 48,0 Mo           │
```

---

## 2. CONTRÔLES — ICÔNES SEULEMENT

### 2.1 Rangée de boutons

| Propriété | Valeur |
|---|---|
| Bouton | cercle **40 dp**, **zone tactile 48 dp** (padding transparent 4 dp inclus) |
| Surface | verre léger via `GlassSurface` (`GlassTokens.Minimal`), opacité 0,78 sombre / 0,82 clair |
| Contour | 1 dp, 8 % (`GlassTokens.EdgeDark/EdgeLight`) |
| Espacement entre boutons | **8 dp** |
| Retour haptique | léger (`HapticFeedbackType.TextHandleMove` ou équivalent) à l'appui |
| Libellé visible | **aucun** sur les boutons |

### 2.2 Boutons par état

| État | Bouton 1 | Bouton 2 |
|---|---|---|
| En attente | — | `ic_close` (Annuler, teinte avertissement discrète) |
| En cours | `ic_pause` → `ic_resume` (change selon l'état, transition 120 ms `Motion.DurationShort`) | `ic_close` |
| En pause | `ic_resume` | `ic_close` |
| Erreur | `ic_retry` | `ic_close` |
| Terminé | — | — |

`Annuler` teinte d'**avertissement discrète** : couleur erreur du thème à 60 % d'opacité sur le fond du bouton, icône en `onSurface` — jamais un rouge pur criard.

### 2.3 Confirmation d'annulation

- Appui sur `Annuler` → **petite feuille** (bottom sheet), rayon **24 dp** (`RadiusProminent`), `GlassSurface` Standard.
- Ici, **le texte est permis** : titre « Annuler le téléchargement ? », corps « Le fichier partiel sera supprimé. », actions « Annuler le téléchargement » (destructive) et « Garder ».
- Aucune animation du rayon de flou ; entrée/sortie par `Motion.EmphasizedDecelerate`.

### 2.4 Accessibilité — contentDescription (FR / EN)

| Icône | FR | EN |
|---|---|---|
| `ic_pause` | « Mettre en pause » | "Pause download" |
| `ic_resume` | « Reprendre » | "Resume download" |
| `ic_close` | « Annuler le téléchargement » | "Cancel download" |
| `ic_retry` | « Réessayer » | "Retry" |
| `ic_check` | « Terminé » | "Completed" |

---

## 3. ANIMATIONS

| Élément | Animation | Token |
|---|---|---|
| Remplissage entre deux mises à jour | interpolation **200–300 ms** : `animateFloatAsState(target, animationSpec = tween(Motion.DurationBase…DurationLong, EmphasizedDecelerate))` | `Motion.DurationBase` 220 / `DurationLong` 300 |
| Bouton Pause ⇄ Reprendre | fondu + échelle 0,97 sur **120 ms** | `Motion.DurationShort` |
| Coche « terminé » | échelle 0,8→1 avec ressort | `Motion.SpringSoft` |
| Barre indéterminée | translation linéaire 2 s, répétée | boucle, sans ressort |
| **Animations réduites** | remplissage **sans interpolation** (`Motion.scaleDuration(ms, reduced = true)` → 0), boutons sans échelle, coche sans ressort | `Motion.scaleDuration` |

**Interdit** : animation du rayon de flou, animation de l'opacité du verre, ressort sur la barre de progression (elle doit rester lisible, pas rebondir).

---

## 4. NOTIFICATION

| Attribut | Valeur |
|---|---|
| Type | progression **native déterminée** (`setProgress(max, progress, indeterminate = false)`) |
| Titre | titre du morceau (1 ligne) |
| Texte | `63 % · 2,4 Mo/s` |
| Petite icône | icône de téléchargement existante du projet |
| Actions | **en icônes seulement** quand le système le permet (`setIcon` sur `Notification.Action`) : Pause/Reprendre, Annuler |
| Limite système à signaler | Android impose souvent un **libellé court d'un mot** pour les actions de notification (« Pause », « Reprendre », « Annuler ») même avec icône ; certaines ROM ignorent l'icône seule. Ce comportement est **imposé par le système**, non corrigeable côté app — à documenter dans la fiche de recette. |
| Contraste | texte >= 4,5:1 sur le fond de notification ; actions icônes >= 3:1 |

---

## 5. LISIBILITÉ, RESPONSIVE, TALKBACK

### 5.1 Couleurs clair / sombre et contraste AA

| Élément | Clair | Sombre | Contraste cible |
|---|---|---|---|
| Piste | accent à 12 % sur surface | accent à 16 % | >= 3:1 (élément d'interface) |
| Remplissage | accent du thème | accent du thème | >= 3:1 sur piste |
| Pourcentage | `onSurface` | `onSurface` | >= 4,5:1 |
| Ligne secondaire | `onSurfaceVariant` | `onSurfaceVariant` | >= 4,5:1 |
| Erreur | erreur du thème à 70 % | erreur du thème à 75 % | >= 4,5:1 pour le texte associé |
| Avertissement (Annuler) | erreur 60 % | erreur 65 % | >= 3:1 (icône) |

Si le contraste AA n'est pas atteint : **augmenter l'opacité / assombrir le texte**, jamais baisser l'opacité du verre sous 0,70 (règle doc 13).

### 5.2 Paysage et tablette

| Contexte | Règle |
|---|---|
| Paysage (hauteur < 480 dp) | carte compacte : miniature **64×36 dp**, titre 1 ligne, barre 6 dp, pourcentage 20 sp, boutons 40 dp ; ligne secondaire sur une ligne |
| Tablette (>= 600 dp) | largeur max **560 dp** conservée, centrée ; miniature **112×63 dp**, titre 2 lignes ; pas d'étirement pleine largeur |
| Multi-fenêtre | mêmes rayons et marges ; la carte se réduit sans coupure du pourcentage (largeur fixe 64 dp conservée) |

### 5.3 TalkBack

- Annonce du **pourcentage toutes les 10 %** uniquement (0, 10, 20 … 100), pas à chaque mise à jour de byte.
- Format annoncé : « Téléchargement, 63 pour cent, 2,4 mégaoctets par seconde, 8 secondes restantes ».
- Changements d'état annoncés immédiatement : « En pause », « Erreur — réseau indisponible », « Terminé ».
- Boutons : `contentDescription` §2.4, `importantForAccessibility = yes`.
- Barre elle-même : `progressBar` avec `accessible = false` (l'annonce est portée par le texte, pas par la barre, pour éviter la double lecture).

---

## 6. ICÔNES À FOURNIR (Build / designer vecteur)

Vecteur **24 dp**, trait **1,75 dp**, `viewport 24×24`, vérifiées à **24 / 32 / 48 dp** en clair et sombre. Aucune icône Apple.

| Icône | État d'existence | Action attendue |
|---|---|---|
| `ic_pause` | ✅ déjà présent (`ic_pause_24dp.xml`, `ic_pause_m3_24dp.xml`) | vérifier trait 1,75 dp |
| `ic_resume` | ❌ **absent** (only `ic_play_24dp.xml` existe) | créer `ic_resume_24dp.xml` (triangle dans cercle ou simple triangle, cohérent avec `ic_play_m3`) |
| `ic_close` | ✅ déjà présent (`ic_close_24dp.xml`) | vérifier trait 1,75 dp |
| `ic_retry` | ❌ **absent** | créer `ic_retry_24dp.xml` (flèche circulaire) |
| `ic_check` | ✅ déjà présent (`ic_check_24dp.xml`) | vérifier trait 1,75 dp |

Build doit produire les captures de vérification 24/32/48 dp en clair et sombre pour les 5 icônes, et signaler toute incohérence de trait.

---

## 7. CE QUE JE SIGNALE (sans modifier le code)

| # | Point | Gravité | Agent |
|---|---|---|---|
| 1 | La mini-barre actuelle (`DownloadScreen.kt:438`, `LinearProgressIndicator`) n'a ni hauteur 8 dp, ni dégradé, ni pourcentage tabulaire, ni ligne vitesse/taille/reste — écart complet avec cette spec. | Haut | BUILD |
| 2 | `ic_resume` et `ic_retry` absents du drawable : à fournir avant implémentation des états pause/erreur. | Haut | BUILD |
| 3 | La notification doit utiliser `setProgress(..., indeterminate = false)` et des actions à icônes ; le libellé d'un mot imposé par Android est une limite système à documenter, pas un bug. | Moyen | PM (recette) |
| 4 | TalkBack : ne pas laisser la barre annoncer chaque byte ; porter l'annonce sur le texte (10 %). | Moyen | BUILD |
| 5 | Aucune animation du rayon de flou, conformément au design system — la carte reste une `GlassSurface` statique. | Info | BUILD |

**Fin du document. Aucun code modifié. Aucune mesure claimée sur un appareil.**
