# 08 — Comparaison implémentation ↔ 06/07 (signatures « sans code »)

> Agent DESIGNER, signalement seul (aucun code modifié). Sources : `docs/design/06-liquid-leger.md`,
> `docs/design/07-animations-liquid.md`, captures générées par les tests de captures
> (`docs/baseline/captures/*.png`, clair/sombre) et lecture d'images. **Aucune capture
> téléphone fournie dans la conversation ni retrouvée dans le workspace** au 2026-11-09.

## 1. Ce que montrent les captures lues

- `glass-standard-light.png` / `glass-standard-dark.png` : boîte arrondie `GlassSurface` + texte « contenu » + `Button` « Bouton » sur fond plein. Pas de flou d'arrière-plan visible, pas de contenu réel derrière la surface.
- `mini-player-light.png` : mise en page de maquette (titre, pastille, bouton) sur fond blanc — pas un mini-player réel, pas de pochette, pas d'arrière-plan liste.
- Les 12 images `glass-*-{light,dark}.png` et les 12 `*-{light,dark}.png` de `docs/baseline/captures/` sont des gabarits Compose `Box`/`Text`, pas des surfaces réelles sur fonds variés.

## 2. Tableau des écarts

| § | Spec (06/07) | Implémentation | Écart (mesuré / constaté) |
|---|---|---|---|
| Rayon de flou | 16 / 20 / 24 dp, flou temps-réel dans `GlassSurface` | `GlassSurface.kt` : aucun `RenderEffect`/`blur` — fond `Color.Black` alpha 0,5 si actif, 0 sinon ; mini-player Via `GradientDrawable` teinté seulement | **ÉCART majeur : effet « flou » = 0 dp effectif.** Les tokens `GlassTokens.radius()` sont appliqués à la **forme**, pas à un flou. Aucune distorsion, aucun halo (bien), mais la profondeur promises par la spec n'est pas rendue. |
| Opacité | Minimal 0,78/0,82 ; Standard 0,83/0,87 ; Prominent 0,88/0,92, teinte `surface` | Surface Compose : alpha 0,5 noir (spec ≈ +0,28/+0,42) ; View mini-player : `glassBackgroundAlpha` 0,78/0,82 **conforme** sur fond `surface` | **Compose : NON CONFORME (0,5 vs 0,78–0,92, teinte noire vs surface). View : CONFORME.** |
| Contour | 1 dp, 8 % (12 % Prominent), uniquement côté contenu | `GlassSurface` : `border(1.dp, edge, shape)` sur les 4 côtés ; mini-player View : `setStroke(1dp)` idem | **ÉCART mineur : contour sur 4 côtés, pas uniquement le côté contenu.** |
| Ombre | Aucune | Aucune (pas d'`elevation`) | **CONFORME.** |
| Effet vitreux | Reflets, brillance, bord lumineux, dégradé, réfraction, lueur interne **interdits** | Aucun terme détecté hors whitelist (`scripts/check-glass-policy.sh` PASS) ; captures lues : aucun reflet/brillance/dégradé visible | **CONFORME (absence vitreux vérifiée sur captures lues).** |
| Contraste AA | ≥ 4,5:1 texte, ≥ 3:1 icônes/grand texte, sur fonds extrêmes | `MiniPlayerGlassTest` (JVM, WCAG) : ≥ 4,5:1 pour Minimal 0,78/0,82 sur noir/blanc et 0,78 sur pochette saturée | **Conforme en calcul ; NON VÉRIFIÉ sur photo réelle** (captures gabarits, pas de fond/pochette/réel). |
| Lecteur plein écran | Fond = pochette 64 px pré-floutée, assombrie 0,80/0,85, désaturée, accent discret | Non implémenté (Étape 4 non démarrée) ; `lecteur-plein-ecran-*.png` = gabarit | **NON IMPLÉMENTÉ — capture conforme impossible.** |
| Repli opaque | Géométrie identique, `surface` alpha 1,0 | Procédé : `GlassPolicy` → alpha 1,0 (View) / fond 0 + bordure (Compose, fond 0) ; captures lentes uniquement en mode par défaut | **Partiellement conforme ; captures repli opaque manque**. |
| Maquettes §4 (fonds) | Chaque surface sur fond uni, liste d'albums colorée, pochettes claire/sombre/saturée | Captures = fond uni `surface`/`#121212` uniquement | **NON COUVERT : 3 fonds manquants par surface.** |
| Durées/ressorts (07) | `SpringSoft` 0,86/380, `SpringBounce` 0,5/550, `SpringSnappy` 0,55/700, `DurationBase` 220, `Long` 300, `Crossfade` ≈ 400 | `Motion.kt` : conforme, sauf §7 (ligne suivante) | **CONFORME sauf Crossfade.** |
| Token fondu (07 §1.5) | FenêtreCrossfade ≈ 400 ms, token justifié à centraliser | Absent : `DurationCrossfade`/`tweenCrossfade` non présents dans `Motion.kt` | **ÉCART : ajouter `val DurationCrossfade = 400` (justifié dans 07).** |
| Animations réduites | Durées ≈ 0, opaque, via `scaleDuration(...,true)=0` | `Motion.scaleDuration` conforme ; `GlassPolicy` force opaque si `reducedMotion` | **CONFORME en tokens ; non observable sur capture statique.** |
| TalkBack / sémantique | Inchangé sur surfaces concernées | Mini-player : sémantique View inchangée ; aucune capture/esquisse TalkBack fournie | **NON VÉRIFIABLE en capture statique — à prouver par tests de sémantique.** |
| Écrans non concernés | Inchangés vs `docs/baseline/captures/` v1.0.0 | `docs/baseline/captures/README.md` : **baselines v1.0.0 « non générées »** (pas de device le 2026-11-08) | **IMPOSSIBLE À VÉRIFIER : aucune référence v1.0.0 réelle.** |

## 3. Captures manquantes (précises)

**Tests de captures (Robolectric, sans appareil) — à générer :**
1. Chaque surface (mini-player, barre du bas, barre supérieure, feuille) × {clair, sombre} × {fond uni, liste d'albums colorée, pochette très claire, pochette très sombre, pochette très saturée} — soit **4 × 2 × 5 = 40 PNG** (actuel : 8 gabarits fond uni).
2. Tailles : 320 px (`w320dp`), tablette 768 px (`w768dp`), bureau 1440 px — actuel : un seul `qualifiers = "w400dp-h800dp"`.
3. Lecteur : pochettes très claire / sombre / saturée × clair/sombre, avec fond pré-flouté activé/désactivé.
4. Repli opaque explicite (réglage `Désactivé`) pour `GlassSurface` et mini-player, clair/sombre.
5. Animations : impossible en PNG — fournir WebM/GIF ou séquence d'images pour les 8 animations 07 (mini→plein, barre bas, barre haut, feuille, changement titre, pression, listes).

**Téléphone (à fournir par l'utilisateur) :**
6. Captures réelles des 4 surfaces × clair/sombre, sur fond uni, liste d'albums et pochette extrême.
7. Captures du lecteur plein écran (titre/commandes lisibles) + comparatives en `Effets de flou` Activé/Désactivé.
8. Captures de TalkBack (ordre de focus) ou-export des sémantiques des 4 surfaces.
9. Deux captures comparatives 60 Hz vs 120 Hz, et mesures Diagnostics (voir `docs/lean/fiche-mesure-v1.1.md`).

*Sans ces captures, les écarts §§ maquettes, repli, TalkBack, animations et écrans non concernés restent **non vérifiés** — seuls les écarts calculables (tokens, contraste JVM, politique CI) sont tranchés ci-dessus.*
