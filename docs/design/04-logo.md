# B Music — Logo (guide d'usage, v1.0)

> Agent Designer B Music. Date : 6 novembre 2026.
> Fichiers : `assets/brand/` (SVG source + res/ Android). Piste retenue : **C — « égaliseur »**, voir §2.

## 1. Concept retenu

**Piste C — « B formé de barres verticales d'égaliseur de hauteurs différentes »** : 4 barres à caps arrondis (stadium), hauteurs différentes (24 / 44 / 28 / 34 dp), espacement régulier, grille 64.

Pourquoi :

- **Lisible à 24 px et à 512 px** : formes pleines, aucun trou, aucun détail fin, aucun dégradé — vérifié à 48 / 72 / 192 px (rendu `pistes/check-row.png`, aucune ambiguïté, silhouette stable).
- **Sens immédiat** : un égaliseur est le signe universel de l'audio — pas besoin de lire « B Music » pour comprendre « musique ».
- **Monochrome trivial** : 4 rects — fonctionne en glyphe de thème Android 13+, en gravure, en empreinte, en embossage.
- **Mémorable** : silhouette asymétrique (barre centrale haute), distincte de Spotify (cercle+ondes), Apple (pomme), SoundCloud (bonhomme/barres symétriques).
- **Cohérence** : prolonge l'égaliseur animé de l'écran lecteur (§ animations #9) — le logo est « un égaliseur arrêté ».

## 2. Les trois pistes — évaluation honnête

| Piste | Description | Verdict |
|---|---|---|
| A — « B » à deux demi-cercles (ondes/haut-parleurs) | fût + 2 arcs de cercle arrondis | **Rejetée** : les arcs détachés du fût lisent comme des parenthèses/croissants « radio », pas comme un B ; silhouette fragile à 24 px (ouvertures < 2 px) |
| B — « B » à contre-forme bouton lecture (triangle) | forme pleine avec trou triangulaire | **Rejetée** : triangle noyé dans la masse à 24 px ; monochrome mauvais (le trou devient zone de fond) ; silhouette organique peu géométrique |
| **C — égaliseur 4 barres** | **retenue** | voir §1 |

Aperçus rendus : `assets/brand/pistes/piste-a.svg`, `piste-b.svg`, `piste-c.svg` (+ `compare-192.png`).

## 3. Construction (grille)

- Grille **64 × 64**, marge optique 7, barres 10 dp, espacement 4 dp, caps `rx = 5` (demi-largeur).
- Hauteurs : barre 1 = 20 (y 22–42), barre 2 = **44** (y 10–54), barre 3 = 28 (y 24–52), barre 4 = 34 (y 15–49).
- Adaptive icon : matrice 108 dp, zone de sécurité **66 dp** (barres confinées à x∈[23, 85), y∈[32, 76), centrées sur 54) ; fond `#0B0B0E` unie.
- Wordmark : symbole + « B Music » en **Inter SemiBold 600**, tracking −0,5, base alignée sur l'axe optique des barres.

## 4. Couleurs exactes

| Usage | Hex | Contraste mesuré |
|---|---|---|
| Symbole — fond sombre | `#E8862E` | sur `#0B0B0E` : **7,4:1** (AA) |
| Symbole — fond clair | `#A85B0B` | sur `#FAFAFB` : **4,8:1** (AA) |
| Symbole mono sombre | `#FFFFFF` | — |
| Symbole mono clair | `#1B1B1F` | — |
| Fond icône | `#0B0B0E` | — |
| Texte « B Music » sur sombre | `#F2F2F0` | 17,5:1 |
| Texte « B Music » sur clair | `#1B1B1F` | 16,5:1 |

Règle : **jamais** d'autre teinte ; jamais le symbole en `#E8A33D` (ambre saturé de la maquette — remplacé) ; jamais de dégradé sur les barres ; aucune variante « illustrée ».

## 5. Zone d'espace libre & tailles minimales

- **Clearspace** = hauteur d'une barre (10 dp) sur les 4 côtés — aucun texte/élément n'entre dans cette zone.
- **Taille minimale** : 24 dp (symbole seul) ; wordmark : 120 dp de large. En dessous : symbole seul.
- Tailles de test validées : 48 / 72 / 192 px (icônes), 512 px (Play Store), 1024×500 (bannière).

## 6. Interdits

- ❌ Pas de rotation, inclinaison, perspective, ombre, reflet, glow, blur, contour.
- ❌ Pas de recoloration hors §4 (pas de version arc-en-ciel, pas de version « accent dynamique »).
- ❌ Pas de recomposition (ne pas espacer/resserrer les barres ; ne pas ajouter de 5ᵉ barre).
- ❌ Pas de fond dégradé ni imagerie derrière le logo ; fond uni `#0B0B0E` (sombre) ou `#FAFAFB`/blanc (clair).
- ❌ Pas de « B Music » en capitales espacées ; la casse exacte est `B Music`.
- ❌ Le symbole ne porte jamais de texte à l'intérieur.

## 7. Inventaire des fichiers

| Fichier | Usage |
|---|---|
| `logo-bmusic.svg` | symbole — fond sombre (#E8862E) |
| `logo-bmusic-light.svg` | symbole — fond clair (#A85B0B) |
| `logo-bmusic-mono.svg` / `-mono-black.svg` | empreintes (blanc / noir) |
| `logo-bmusic-wordmark.svg` / `-wordmark-light.svg` / `-wordmark-mono.svg` / `-wordmark-mono-black.svg` | symbole + nom, 4 contextes |
| `res/drawable/ic_launcher_foreground.xml` | foreground adaptive (108 dp) |
| `res/drawable/ic_launcher_background.xml` | fond adaptive (#0B0B0E) |
| `res/drawable/ic_launcher_monochrome.xml` | icône thématique Android 13+ |
| `res/mipmap-anydpi-v26/ic_launcher.xml` / `ic_launcher_round.xml` | adaptive icon (réfère foreground/background/monochrome) |
| `res/mipmap-{mdpi48,hdpi72,xhdpi96,xxhdpi144,xxxhdpi192}/ic_launcher.png` + `ic_launcher_round.png` | icônes classiques secours (rond/carré) |
| `ic_launcher-playstore.png` | 512×512 Play Store |
| `feature-1024x500.svg` / `.png` | bannière Play Store (fond uni, logo, nom uniquement) |
| `res/drawable/ic_splash_logo.xml` + `ic_splash_animated.xml` | splash Android 12+ (API 31) |
| `res/animator/splash_scale_in.xml` + `splash_alpha_in.xml` | échelle 0,9→1 + fondu, 350 ms |
| `res/values-v31/themes.xml` | config Splash API (`windowSplashScreenBackground #0B0B0E`, durée 400 ms) |
| `icon-store.svg` / `icon-store-round.svg` | sources stores (re-rendu PNGs) |

## 8. Intégration Android (fork BoomingMusic)

1. Copier `res/drawable/*`, `res/animator/*`, `res/mipmap-*`, `res/mipmap-anydpi-v26/*` dans `app/src/main/res/`.
2. `AndroidManifest.xml` : `android:label="@string/app_name"` avec `app_name="B Music"` (values `values/` + `values-fr/`) — nom affiché **exactement** « B Music ».
3. `windowSplashScreenAnimatedIcon` = `@drawable/ic_splash_animated` ; appliquer `Theme.BMusic.Splash` au démarrage puis remplacer par le thème final.
4. `mipmap-anydpi-v26` gère adaptive + monochrome ; les `mipmap-*` PNG restent les secours < API 26.
5. Vérifier sur device : icône lanceur (rond + squircle), icône thématique 13+, splash ≤ 400 ms, pas de flash blanc (fond `#0B0B0E` avant chargement).
