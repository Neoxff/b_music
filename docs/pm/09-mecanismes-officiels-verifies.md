# B Music — Mécanismes officiels vérifiés (v1.2)

> Vérification documentaire du 14 novembre 2026, sources officielles consultées en direct.
> Vérifie les candidats listés en `docs/pm/06-matrice-plateformes.md` §4 (V1→V5) et alimente les arbitrages **ADR-002** et **ADR-003** de `docs/architecture/TECHNICAL_DECISIONS.md`.
> Règle appliquée : **aucune intégration n'est annoncée sans vérification**. Ce qui n'est pas confirmé est marqué `[À VÉRIFIER]`.

---

## 1. Verdict par plateforme

| Plateforme | Lecture intégrée | Métadonnées (titre / miniature / auteur / durée) | Verdict |
|---|---|---|---|
| **YouTube** | `youtube.com/embed/ID` — **sans clé**, mais Referer obligatoire, WebView requis | **oEmbed sans clé** → titre, auteur, miniature ; **durée absente**. `videos.list` → clé requise | `EMBED_SANS_CLE` + `METADATA_SANS_CLE` (sans durée) |
| **TikTok** | `tiktok.com/player/v1/{id}` — sans clé, `postMessage` documenté | oEmbed sans clé mais **vide** (ni titre, ni miniature, ni durée) ; Display API = **backend** | `EMBED_SANS_CLE` ; métadonnées ⇒ `METADATA_AVEC_CLE_SERVEUR` |
| **Facebook** | `fb-video` + SDK `connect.facebook.net`, ou `video/embed?video_id=` — sans clé, orienté DOM | oEmbed **sans clé depuis le 15/06/2026** (1 000/h) mais **sans titre, ni miniature, ni auteur** | `EMBED_SANS_CLE` ; métadonnées ⇒ `METADATA_AVEC_CLE_SERVEUR` |
| **Instagram** | **Aucun player natif officiel** : `embed.js` scanne le DOM d'une page web | oEmbed sans clé (1 000/h) mais **sans titre, ni miniature, ni auteur** | `NON_INTEGRABLE` (nativement) ; métadonnées ⇒ `METADATA_SANS_CLE` (très pauvre) |
| **X** | `widgets.js` vivant (93 Ko) mais produit « X for Websites » | oEmbed **sans clé, non rate-limité** → auteur + texte ; pas de miniature, pas de vidéo, pas de durée | oEmbed `METADATA_SANS_CLE` ; player ⇒ `NON_INTEGRABLE` (non documenté) |
| **Snapchat** | **Aucun player intégrable** : `embed.js` web uniquement | **Aucun endpoint de métadonnées** ; pas d'oEmbed (404 vérifié) | `NON_INTEGRABLE` |

## 2. Les trois faits qui décident

### 2.1 Recherche YouTube : **aucune voie officielle sans clé serveur**

| Voie | État | Source |
|---|---|---|
| Data API v3 `search.list` | **exige une clé API** — `403 PERMISSION_DENIED` vérifié en direct | [authentication](https://developers.google.com/youtube/v3/guides/authentication) · [search.list](https://developers.google.com/youtube/v3/docs/search/list) |
| IFrame Player API `listType=search` | **déprécié et non supporté depuis le 15/11/2020** | [iframe_api_reference](https://developers.google.com/youtube/iframe_api_reference) |
| YouTube Music API | **aucune API publique de recherche documentée** | `[NON VÉRIFIÉ]` |
| `youtubei` / Innertube | **non documenté** ⇒ hors périmètre (ce serait du scraping) | — |
| oEmbed | keyless mais **résout une URL connue** — ne cherche rien | — |

**Coût de la voie Data API v3** (pour la décision) : `search.list` = 1 unité par appel avec un **bucket dédié plafonné à 100 appels/jour** (reset minuit PT) ; `videos.list` = 1 unité dans le budget partagé de 10 000/jour. Chaque page supplémentaire = 1 unité de `search.list`.
⇒ **~100 recherches par jour pour TOUTE l'application**, quel que soit le nombre d'utilisateurs. Source : [quota calculator](https://developers.google.com/youtube/v3/determine-quota-cost) (maj. 2026-11-12).

**Échappatoire officielle à backend** : Google documente la restriction d'une clé API par type d'application « Android apps » (package name + empreinte SHA-1) — [cloud.google.com/docs/authentication/api-keys](https://cloud.google.com/docs/authentication/api-keys). Une clé ainsi restreinte ne peut être utilisée que par l'APK, et n'est donc **pas exploitable par un tiers**. C'est le mécanisme **officiellement supporté** pour appeler la Data API depuis une app Android sans serveur. Mais la clé reste **embarquée dans le client** (contrairement à la règle du dossier), et le **plafond de 100 recherches/jour est inchangé**.

### 2.2 Les oEmbed keyless ne donnent plus de métadonnées exploitables (hors YouTube)

Meta a supprimé `author_name`, `author_url`, `thumbnail_url`, `thumbnail_width`, `thumbnail_height` des réponses oEmbed (dépréciés puis retirés le **2025-11-03**) : [blog officiel](https://developers.facebook.com/blog/post/2026/06/15/tokenless-access-to-meta-oembed-apis/) · [oembed-post](https://developers.facebook.com/docs/graph-api/reference/oembed-post/). TikTok n'a jamais retourné titre, miniature ni durée dans son oEmbed. X ne retourne que l'auteur et le texte.

**Conséquence directe sur BUILD-11** : « afficher le titre, l'auteur et la miniature **seulement si les métadonnées sont obtenues légalement et de manière fiable** » (BUILD-11). En métadonnées officielles keyless, **seul YouTube est exploitable** (titre, auteur, miniature — sans durée). C'est exactement ce que le `ProviderRegistry` implémenté encode : `previewAvailable = true` pour YouTube, SoundCloud, Bandcamp, Vimeo, PeerTube (moteur d'extraction), `false` pour TikTok, Facebook, Instagram, X, Snapchat.

**Contrainte Meta explicite** : sur Instagram, « using metadata … for any purpose other than providing a front-end view … is strictly prohibited » — les métadonnées oEmbed ne peuvent pas être extraites ni persistées à des fins d'analyse. Source : [instagram-platform/oembed](https://developers.facebook.com/docs/instagram-platform/oembed/) (maj. 2026-05-15).

### 2.3 Les lecteurs intégrés exigent tous un WebView, et 2 plateformes sur 6 n'en ont pas

- **YouTube** : `youtube.com/embed/VIDEO_ID` sans clé, mais le **Referer HTTP est vide par défaut dans un WebView** et YouTube l'impose — [Required Minimum Functionality](https://developers.google.com/youtube/terms/required-minimum-functionality) ; auto-play encadré (un seul player par écran, viewport ≥ 200×200), pas d'overlay devant le player.
- **TikTok** : `player/v1/{id}` sans clé ; enveloppe `postMessage` `EmbeddedPlayerMessage { 'x-tiktok-player', value, type }` ; **aucune contrainte d'`origin` documentée** `[À VÉRIFIER]`. L'oEmbed TikTok est déprécié : « Embed Videos will be deprecated soon… use Embed Player » ([embed-player](https://developers.tiktok.com/doc/embed-player/), maj. 2026-10-28).
- **Facebook** : `fb-video` + `connect.facebook.net` SDK, orienté DOM (WebView uniquement) ; `postMessage` non documenté `[À VÉRIFIER]`.
- **Instagram** : `embed.js` scanne le DOM, **Shadow DOM non supporté** → aucune intégration native.
- **X** : `widgets.js` est un produit « X for Websites » ; usage dans un WebView d'app native non documenté `[À VÉRIFIER]` ; API X v2 (médias) = Bearer token **pay-per-use** ⇒ backend.
- **Snapchat** : `embed.js` web uniquement ; seule la forme `lens/{id}/embed` est exemplifiée ; `spotlight/{id}/embed` répond 200 mais n'est **pas documentée** `[À VÉRIFIER]`.

## 3. Ce que cela change pour le dossier

| Décision | Effet de la vérification |
|---|---|
| **ADR-002** (moteur de recherche) | Le « mécanisme officiel » **exige soit une clé, soit un backend**, et le plafond de **100 recherches/jour** rend la voie Data API v3 **non exploitable en production** quel que soit le nombre d'utilisateurs. La troisième voie — clé restreinte à l'APK Android (officielle, sans serveur) — supprime le backend mais **viole la règle « aucune clé dans le client »** et ne lève pas le plafond. ⇒ Le choix se resserre entre NewPipe (D-SEC-1) et un backend proxy. |
| **ADR-003** (lecteur intégré) | Un WebView n'apporterait la lecture que pour **YouTube et TikTok** ; Facebook et X exigent du DOM/JS tiers, Instagram et Snapchat n'ont **rien** de natif. Le coût (WebView + JS + origines tierces + revue sécurité dédiée) ne couvre que 2 plateformes sur 6. ⇒ Renforce la recommandation « métadonnées + ouverture externe ». |
| **BUILD-11** (carte de prévisualisation) | Le « titre / auteur / miniature » **officiel et keyless** n'existe que pour YouTube. Pour les autres, l'app affiche la **plateforme + l'URL + l'état**, sans inventer de métadonnées (BUILD-11 : « ne simule jamais de métadonnées manquantes »). C'est le comportement déjà implémenté. |
| **BUILD-07** (registre) | `embedOrigins` reste **vide** : aucune origine d'iframe ne peut être déclarée tant qu'ADR-003 n'est pas rendu, et le registre ne doit pas annoncer ce qui n'est pas vérifié. |

## 4. Points non confirmés

1. YouTube — limite de débit de l'oEmbed non documentée `[À VÉRIFIER]`.
2. YouTube — enveloppe JSON exacte de l'IFrame Player API `postMessage` `[À VÉRIFIER]`.
3. YouTube — géo-blocage / restriction d'âge dans `search.list` `[À VÉRIFIER]`.
4. TikTok — contrainte d'`origin` sur le `postMessage` du Embed Player `[À VÉRIFIER]`.
5. TikTok — support des **Reels** (le player déclare « video, image ») `[À VÉRIFIER]`.
6. Facebook — `postMessage` du player vidéo `[À VÉRIFIER]`.
7. Instagram — `postMessage` de `embed.js` `[À VÉRIFIER]`.
8. X — usage de `widgets.js` dans un WebView d'app native ; maintenance du widget vidéo `[À VÉRIFIER]`.
9. Snapchat — forme exacte de l'URL d'embed Spotlight `[À VÉRIFIER]`.
10. Snapchat — quotas d'embed et périmètre exact des contenus privés `[À VÉRIFIER]`.
