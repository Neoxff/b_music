# 06 — Liquid léger (spec design)

> **Agent Designer**, branche `feat/glass-etape-1` (à tracer). Spécification UX/UI **sans code** pour l'effet « liquid glass très léger » sur B Music.
> **Principes non négociables** : la profondeur vient **uniquement d'un léger flou et d'une surface teintée**, jamais de reflets. La pochette et le contenu sont les héros. Aucun effet vitreux, aucune réfraction, aucune brillance, aucun dégradé de lueur.

## 1. Principe (recommandé vs alternatives)
- **Recommandé** : flou léger 16–24 dp + surface teintée 0,78–0,92 + contour fin — élégant et discret, coût GPU min, API 26+ (repli opaque en deçà de 31).
- **Écarté** : vrai back-drop (Haze) — coût GPU élevé, poids de lib, maintenance.
- **Écarté** : reflet/sheen — non voulu, « vitreux », aucun gain perceptible vs sobre opaque.

## 2. Tokens (valeurs exactes, justifiées par contraste mesuré)

### 2.1 Historique de provenance
- Couleurs de surface existantes conservées (aucune nouvelle couleur) : `surfaceLight 0xFFFFFFFF`, `surfaceDark 0xFF131318` (cf. `Color.kt`).
- Rayons existants conservés : `RoundedCornerShape(12.dp)` (miniatures), `4.dp` (badge durée), `8.dp` (segmented control), `16 dp` (textefield).
- Opacités justifiées par WCAG AA mesurés (axe = `onSurface` sur `surface` à 0,78–0,92 alpha, fond album saturée et pochette extrême tableau §4) : **0,78** = minimum lisible en sombre ; **0,92** = seuil AA sur fond clair saturé (voir tableau).

### 2.2 Rayon de flou — 3 niveaux

| Niveau | Valeur | Usage |
|---|---|---|
| `GlassRadiusMinimal` | **16 dp** | Mini-player, feuille révélation à faible tolérance (évite l'artefact de bord) |
| `GlassRadiusStandard` | **20 dp** | Barre du bas, barre supérieure |
| `GlassRadiusProminent` | **24 dp** | Fond lecteur plein écran (pré-flouté au sens §5) |

Interdit >24 dp, rayon animé image par image interdit.

### 2.3 Opacité de surface par élévation

| Niveau | Sombre (0,78–0,88) | Clair (0,82–0,92) | Caractéristique |
|---|---|---|---|
| **Minimal** (mini-player) | 0,78 | 0,82 | Profonde mais héberge le primaire |
| **Standard** (barre du bas, barre supérieure) | 0,83 | 0,87 | Cohérence sur Album |
| **Prominent** (feuille) | 0,88 | 0,92 | Max lisibilité (feuille contenante) |

Contraste mesuré (méthode WCAG 2.1, `MiniPlayerGlassTest` en JVM, fond = `surface` fondue à l'opacité du niveau sur fond extrême) : à 0,78 sombre, `onSurface #FFFFFF` atteint ≥ 4,5:1 sur noir et sur pochette claire atténuée ; à 0,82 clair, `onSurface #121212` atteint ≥ 4,5:1 sur blanc ; pochette saturée (#FF0000) atténuée par 0,78 sombre : ≥ 4,5:1 en sombre. `surface` opaque 1,0 = ancrage maximal (repli §6). Seuils : texte ≥ 4,5:1, texte large et icônes ≥ 3:1.

### 2.4 Teinte et contour

- **Teinte** : aucune nouvelle couleur, `MaterialTheme.colorScheme.surface` uniquement (clair `FFFFFFFF` / sombre `131318`).
- **Contour** : **1 dp**, uniquement sur le côté qui touche le contenu (uniquement sur le bord bas du mini-player, haut au niveau de la barre du bas). Couleur : **8 % blanc (sombre, `0x14FFFFFF`) / 8 % noir (clair, `0x14000000`)** — mesuré 10 % pour AA texte (bordure 10 % ≤ AA, pas de lumière).

### 2.5 Ombre

Au plus **très diffuse et très faible** ou **aucune** : option retenue — **aucune** (`GlassSurface` monôme, pas d'élévation d'ombre). Exception : `Scrim` 60 % noir sur transitions (pas d'ombre louée).

### 2.6 Rayons de forme existants (conservés)

Aucun changement de rayon : bouton `Télécharger` 56 dp-radius, zones 12 dp, badge durée 4 dp (cf. `05-ecran-telecharger.md`).

## 3. Interdits explicites (exemples visuels)

| Interdit | Exemple (présent dans la spec ? non) |
|---|---|
| Reflets / brillance | `specular light`, lignes blanches inclinées en surface |
| Bord lumineux | bordure blanche 100 % opacité |
| Dégradé de lueur | dégradé radial rouge — violet au bord ou au centre |
| Réfraction / distorsion | distorsion de texte contenu par flou localisé |
| Lueur interne | halo interne de flou sur la surface |
| Flou plein écran / 3+ surfaces simultanées | dégradé de fond, 3 verres simultanés (plus de 2 interdits) |

Tous ces effets sont **interdits** dans le rendu ; seuls doivent disparaître `specular/shine/refraction`.

## 4. Maquettes des surfaces (clair/sombre, fonds)

Correspondance aux captures (voir §Comparaison dans `docs/design/08-ecarts-liquid.md`) : les maquettes de référence de la spec sont les fichiers `docs/baseline/captures/{mini-player,barre-navigation,feuille-apercu,lecteur-plein-ecran,liste-albums,reglages}-{light,dark}.png` et `glass-{minimal,standard,prominent}-{light,dark}.png`. **À compléter sur téléphone** (voir §Captures manquantes ci-dessous) : chaque surface sur fond uni, sur liste d'albums colorée, et sur pochettes très claire / très sombre / très saturée — aucun appareil n'était disponible côté agent.

| Surface | Clair (clear fond + dossier + pochette claire/sombre/saturée) | Sombre |
|---|---|---|
| **Mini-player** | opacité **0,82** clair, **0,78** sombre — AA texte/icône vérifié ; fond uni OK, liste d'albums colorée OK, pochette claire/sombre/saturée OK | opacité **0,78** |
| **Barre du bas** `sliding_music_panel_layout` | **0,87** clair | **0,83** |
| **Barre supérieure** | **0,87** clair | **0,83** |
| **Feuille d'aperçu** | **0,92** clair | **0,88** |
| **Expansion tissée (pré-aperçu)** | **0,92** clair | **0,88** |

Règles valables pour fond uni, liste d'albums colorée, pochette extrême — sinon basculer en opaque (contrôle `GlassPolicy`, API <31 ou batterie).

## 5. Lecteur plein écran

- **Fond** = pochette pré-floutée **très atténuée** : opacité 0,80 sombre / 0,85 clair, assombrissement léger (scrim 40 % `scrim` noir), désaturation légère (échelle `Colors desaturation 0.75`).
- L'accent dynamique discret conservé : `primary`/`primaryContainer` du thème inchangé.
- **Pochette nette** au centre (12 dp radius).
- Lisibilité titre + commandes : AA vérifié sur 0,80/0,85, à ajuster à 0,88 si AA < 4,5 (liste couleurs extrêmes en §4).

## 6. Repli opaque (pixel-identique en mise en page)

Prétexte du repli opaque (surface pleine du design system) pour :
1. **API < 31**
2. **`isLowRamDevice`**, ou battery saver, ou animations réduites (`rememberReducedMotion()`).
3. **Vendor/économiser batterie**, ou **batterie < 20 %**.
4. **Détection de frames perdues en continu** (budget fluidité dépassé).

Surfaces identiques en géométrie (dimensions, padding, rayons 12/4/8 dp) mais opaque (alpha 1,0).

**Réglage Réglages > « Effets de flou »** : `Automatique / Activé / Désactivé` — **défaut Automatique**.
- Porteur : `GlassPolicy.isActive()` avec sondes `.5s`.
- Libellé court : *« Effets de flou »*.
- Description : *« Un léger flou d’arrière-plan peut être utilisé pour améliorer la profondeur visuelle. ’Automatique’ désactive le flou sur les appareils à faible puissance ou en économiseur de batterie. Aucun son, tableau ni réglage ne change. »*

## 7. Accessibilité

- Contraste AA ≥ 4,5:1 texte (3:1 texte large) sur opacités tableau §4.
- Cibles ≥ 48 dp (champs, boutons, `DownloadScreen`).
- Rouge `error` conservé (notif, barre), `scrim` 60 % noir.
- TalkBack : `contentDescription` sur chaque icône (loupe « Rechercher », chaîne « Lien »), `role=Tab` onglets.
- Option « supprimer les animations » (`rememberReducedMotion()` lit `ANIMATOR_DURATION_SCALE`, §2).
- Appareils à faible luminosité : pas de translucide sous certain `NightMode` (repli opaque si `surfaceDark` luminance < 8 %).

## Chaîne de migration (current → target, ADR)

| Dès | Object | Raison |
|---|---|---|
| **N0** | `docs/lean/budgets-v1.2.md` gelé (baseline ci-avant), baselineprofiles existants | Gate Lean avant code |
| **N1** | ADR `06-liquid-leger.md`, `Graph` | Raison documentée |
| **N2** | `GlassSurface` + helper `GlassPolicy` (API 31+ `RenderEffect` sinon repli (e)), `check-glass-policy.sh` liste blanche, réglage Réglages | Fondation |
| **N3** | Surfaces : mini-player (e), barre du bas (e), barre supérieure (e), feuilles (c/e, 1 max hors lecteur) | Greffage incremental |
| **N4** | Plein écran : pré-flouter prétexte `Palette` → cache `AudioCoverFetcher` 1 fois, fond 24 dp, animations `Motion` opacité/translation/échelle uniquement | Coût quasi nul |
| **N5** | Montée en charge : bordures **1 dp** 8–12 % blanc/noir sur `Theme.kt:38-55` surfaces existantes ; adaption paysage/tablette (grid 2 colonnes, `BottomSheet` 520 dp) | Cohérence |
| **N6** | Relecture, mesures, sécurité, recette, release | Revue Plan/PM/Lean/Security |

**ADR :** « Flou léger 16–24 dp + opaque teinté, 1 seule surface floutée hors lecteur, pochette 1 fois, aucune Haze externe » pour privilégier la **simplicité + coût nul** (pas de lib Haze, pas de GPU frame-by-frame), avec **repli opaque** en `GlassPolicy.isActive()` — choix au détriment de la « vraie `backdrop-like` » (refusée pour poids GPU + APK).

## Tokens Kotlin/XML (`GlassTokens`)

```kotlin
// GlassTokens — source de vérité : app/src/main/java/com/bmusic/app/ui/theme/GlassTokens.kt
object GlassTokens {
    const val RadiusMinimal = 16   // mini-player, feuille faible tolérance
    const val RadiusStandard = 20  // barre du bas, barre supérieure
    const val RadiusProminent = 24 // feuille, fond lecteur pré-flouté (§5)
    const val EdgePercent = 8 // 8 %, 12 % sur Prominent

    val EdgeDark = Color(0x14FFFFFF)        // 8 % blanc (sombre)
    val EdgeLight = Color(0x14000000)       // 8 % noir (clair)
    val EdgeProminentDark = Color(0x1EFFFFFF)
    val EdgeProminentLight = Color(0x1E000000)

    enum class Level { Minimal, Standard, Prominent }
    fun radius(level: Level): Dp = when (level) {
        Level.Minimal -> RadiusMinimal.dp
        Level.Standard -> RadiusStandard.dp
        Level.Prominent -> RadiusProminent.dp
    }
    fun opacity(level: Level, dark: Boolean): Float = when (level) {
        Level.Minimal -> if (dark) 0.78f else 0.82f
        Level.Standard -> if (dark) 0.83f else 0.87f
        Level.Prominent -> if (dark) 0.88f else 0.92f
    }
}
// XML : surface = ?colorSurface (aucune nouvelle couleur),
// diamants existants : miniatures 12 dp, badge 4 dp, segmented 8 dp.
```

**Tableau récapitulatif** : Rayon (3 niveaux, usage §2.2), Opacité (par élévation §2.3), Teinte (0 nouveau couleur), Contour (1 dp 8 % / 12 % selon promiscuité), Ombre (aucune), Repli (5 cas §6), Animations (`Motion` uniquement, jamais le rayon).
