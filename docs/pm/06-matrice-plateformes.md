# B Music — Matrice de compatibilité des plateformes (Explorer / Liens)

> Agent Product Manager B Music. Date : 13 novembre 2026.
> Objet : établir, plateforme par plateforme, ce que B Music sait faire d'un lien vidéo — sans jamais promettre une lecture intégrée qui n'existe pas.
> Renvois : `docs/pm/05-spec-explorer-liens.md` (règles R15→R21) · `docs/pm/07-criteres-acceptation-explorer-liens.md` · `docs/security/01-modele-menaces.md` · `docs/design/03-ecrans.md`.
> Sources de fait : registre de services de l'extracteur embarqué (`NewPipeExtractorEngine.kt` KDoc L30-34), validateur d'URL (`UrlValidator.kt`), garde anti-SSRF (`SsrfGuard.kt`), politique de miniatures (`ImageUrlPolicy.kt`).

## 0. Règle de statut (à lire avant la matrice)

Une plateforme n'est **jamais** marquée « prise en charge » sans **preuve d'un test fonctionnel exécuté sur la méthode officielle en vigueur**. Trois statuts seulement sont utilisés :

| Statut | Signification | Condition |
|---|---|---|
| **Résolu en interne** | B Music sait extraire les métadonnées de cette plateforme via le moteur embarqué. | Le registre du moteur la déclare (preuve : KDoc du moteur). **Ce n'est pas une intégration officielle** : c'est un mécanisme non officiel, dont l'écart est acté par la décision D1. |
| **Détecté seulement** | B Music reconnaît la plateforme, ne sait pas la résoudre, et propose l'ouverture externe. | La plateforme figure dans la table de détection ; aucune résolution n'est tentée. |
| **À vérifier** | Ni la détection ni la résolution ne sont établies. | Aucune source du dépôt ne permet de conclure. |

**Trois précisions qui valent pour toute la matrice**
1. **Aucune lecture vidéo intégrée n'existe dans B Music.** Le code ne contient aucun WebView (vérifié sur 8 refs). La colonne « lecture intégrée » vaut donc **« non » partout** — ce n'est pas un manque d'implémentation à venir dans cette version, c'est un choix (décision D2).
2. **L'intégration officielle n'est pas implémentée pour aucune plateforme.** Les mécanismes officiels listés plus bas sont des **candidats à vérifier**, pas des fonctionnalités : aucun test fonctionnel ne les a validés dans ce dépôt.
3. **La base de résolution n'est pas une liste blanche de plateformes** : `UrlValidator` valide la *syntaxe* de l'URL (schéma, hôte, port, fragment, userinfo, caractères de contrôle) et la *détection* de plateforme est portée par une table dédiée à créer (lot L5). La matrice ci-dessous décrit ce qui **sera** détecté, pas ce qui l'est aujourd'hui.

## 1. Matrice principale

| Plateforme | Liens reconnus | Statut | Résolution interne | Prévisualisation | Lecture intégrée | Ouverture externe | Limitation principale |
|---|---|---|---|---|---|---|---|
| **YouTube** | `youtube.com/watch?v=`, `youtu.be/…`, `youtube.com/shorts/…`, `m.youtube.com/…` | Résolu en interne (résolution + recherche de résultats) | Oui | Oui (titre, chaîne, durée, miniature) | **Non** | Oui | Moteur non officiel (D1) ; results page non intégrable ; contenu privé/âgé/région → état dédié |
| **TikTok** | `tiktok.com/@…/video/…`, `vm.tiktok.com/…` | Détecté seulement | Non | Non (miniature seule si la politique l'autorise) | **Non** | Oui | Pas de service enregistré dans le moteur ; aucun aperçu de titre |
| **Snapchat** | `snapchat.com/…` | Détecté seulement | Non | Non | **Non** | Oui | Aucun service enregistré ; contenu éphémère et souvent lien personnel |
| **Facebook** | `facebook.com/watch?v=…`, `fb.watch/…`, `facebook.com/…/videos/…` | Détecté seulement | Non | Non | **Non** | Oui | Aucun service enregistré ; visibilité dépend des paramètres de confidentialité du compte |
| **Instagram** | `instagram.com/reel/…`, `instagram.com/p/…`, `instagram.com/tv/…` | Détecté seulement | Non | Non | **Non** | Oui | Aucun service enregistré ; contenu privé fréquent ; publication désactivable par l'auteur |
| **X (Twitter)** | `x.com/…/status/…`, `twitter.com/…/status/…` | Détecté seulement | Non | Non | **Non** | Oui | Aucun service enregistré ; vidéo souvent réservée à un compte ; médias sensibles non accessibles |
| **SoundCloud** | `soundcloud.com/…` | Résolu en interne | Oui | Oui | **Non** | Oui | Minutes du compte non gérées par l'aperçu ; miniature `i1.scdn.co` à autoriser |
| **Bandcamp** | `*.bandcamp.com/track/…`, `*.bandcamp.com/album/…` | Résolu en interne | Oui | Oui | **Non** | Oui | Miniature `f4.bcbits.com` à autoriser ; contenu lié à une page de vente (bandcamp) |
| **Vimeo** | `vimeo.com/…` | Résolu en interne | Oui | Oui | **Non** | Oui | Vidéos privées / protégées → état « contenu restreint » |
| **PeerTube** | `*.tube/…`, `*.peertube.tv/…` | Résolu en interne | Oui | Oui | **Non** | Oui | Instances auto-hébergées : format d'URL variable à confirmer ; miniature variable |
| **mediaCCC** | `media.ccc.de/…` | Résolu en interne | Oui | Oui | **Non** | Oui | Contenu allemand ; droits de diffusion selon l'auteur |
| **Toute autre URL** | `http(s)://…` valide | À vérifier (détection « plateforme non reconnue ») | Non | Non | **Non** | Oui | Aucune donnée envoyée à un tiers ; l'URL est seulement validée localement |

> Colonnes « Format de liens reconnus », « Méthode officielle d'intégration à vérifier », « Authentification requise », « Type de contenu pris en charge », « Message utilisateur », « Tests nécessaires » : voir les fiches §2 (une fiche par plateforme).

## 2. Fiches par plateforme

### 2.1 YouTube — seul support de **recherche**

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://www.youtube.com/watch?v=ID` · `https://youtu.be/ID` · `https://www.youtube.com/shorts/ID` · `https://m.youtube.com/watch?v=ID` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : API YouTube Data v3 (`search.list` + `videos.list`, clé requise côté serveur) pour la recherche ; `oEmbed` public pour les métadonnées d'une URL ; lecteur `<iframe>` officiel `youtube.com/embed/ID` pour la lecture intégrée. Aucun de ces mécanismes n'est implémenté ni testé ici. |
| Authentification requise | Pour la méthode Data API : une clé (jamais embarquée — décision taken). Pour oEmbed : aucune. Pour l'iframe : aucune, mais conditions de lecture tierces. |
| Type de contenu pris en charge | Vidéo. L'audio seul n'est pas exposé par l'aperçu. |
| Prévisualisation possible | **Oui** : titre, chaîne, durée, miniature (`ytimg.com`, déjà autorisé par la politique d'images). |
| Lecture intégrée possible | **Non** — aucun WebView dans le code (D2). Repli : ouverture externe via `ACTION_VIEW`. |
| Limites | Moteur non officiel (risque ReCaptcha → état « service indisponible ») ; résultats de recherche limités à 20 par page ; requête de 2 à 200 caractères ; contenu privé, soumis à l'âge, supprimé ou géo-bloqué → états distincts ; une recherche peut ne jamais être possible en l'absence de réseau. |
| Message utilisateur | « YouTube est momentanément indisponible. Réessayez plus tard. » · « Contenu privé ou indisponible. » · « Ce contenu est réservé à certains comptes. » · « Contenu indisponible dans votre région. » |
| Tests nécessaires | (1) recherche d'un terme courant → 1 résultat avec miniature/titre/chaîne/durée ; (2) ouverture d'un résultat → application YouTube ou navigateur ; (3) vidéo privée → état « contenu privé » ; (4) vidéo supprimée → état « indisponible » ; (5) recherche en mode avion → état « connexion impossible » ; (6) `ACTION_SEND` d'une URL YouTube → champ Liens pré-rempli sans action automatique. |

### 2.2 TikTok

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://www.tiktok.com/@<user>/video/<id>` · `https://vm.tiktok.com/<code>` · `https://vt.tiktok.com/<code>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed public TikTok (`https://www.tiktok.com/oembed?url=…`) — à confirmer sur la documentation officielle, notamment son comportement sur les vidéos privées. |
| Authentification requise | oEmbed : aucune en théorie ; à confirmer. |
| Type de contenu pris en charge | Vidéo courte. |
| Prévisualisation possible | **Non** — aucun service enregistré dans le moteur embarqué. Une ouverture externe reste possible. |
| Lecture intégrée possible | **Non**. |
| Limites | Lien court `vm.tiktok.com` à développer avant toute détection fiable ; contenu soumis au pays de l'auteur ; vidéos supprimées ou privées ; underage → masqué. |
| Message utilisateur | « TikTok n'est pas lisible dans B Music. » + action « Ouvrir à l'extérieur » |
| Tests nécessaires | (1) URL `tiktok.com/@x/video/123` → badge « TikTok » + état non pris en charge ; (2) `vm.tiktok.com` → badge « TikTok » (développement du lien court) ; (3) ouverture externe → TikTok ou navigateur. |

### 2.3 Snapchat

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://www.snapchat.com/…` (Spotlight, profil public) |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed Snapchat — disponibilité et conditions à confirmer auprès de la source officielle. |
| Authentification requise | À vérifier ; les contenus Snap sont par ailleurs ephémères et liés à une session. |
| Type de contenu pris en charge | Vidéo courte éphémère. |
| Prévisualisation possible | **Non**. |
| Lecture intégrée possible | **Non**. |
| Limites | Contenu éphémère (lien qui expire) ; profil privé ; contenu supprimé après quelques heures. |
| Message utilisateur | « Snapchat n'est pas lisible dans B Music. » + action « Ouvrir à l'extérieur » |
| Tests nécessaires | (1) URL `snapchat.com/…` → badge + état non pris en charge ; (2) ouverture externe ; (3) lien expiré → pas de crash, message d'indisponibilité. |

### 2.4 Facebook

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://www.facebook.com/watch?v=…` · `https://fb.watch/…` · `https://www.facebook.com/<page>/videos/<id>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed Facebook (historiquement réservé à certaines applications) — à confirmer ; sinon API Graph avec jeton d'application, hors périmètre client. |
| Authentification requise | oEmbed : à vérifier (souvent aucun pour les posts publics, mais variable). |
| Type de contenu pris en charge | Vidéo. |
| Prévisualisation possible | **Non**. |
| Lecture intégrée possible | **Non**. |
| Limites | Visibilité dépendante des réglages de confidentialité de l'auteur et de l'utilisateur ; contenu réservé aux amis ; les liens `fb.watch` sont des redirections à développer. |
| Message utilisateur | « Facebook n'est pas lisible dans B Music. » + action « Ouvrir à l'extérieur » |
| Tests nécessaires | (1) URL `facebook.com/watch?v=…` → badge + état non pris en charge ; (2) URL `fb.watch/…` → badge « Facebook » (raccourci) ; (3) ouverture externe. |

### 2.5 Instagram

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://www.instagram.com/reel/<id>` · `https://www.instagram.com/p/<id>` · `https://www.instagram.com/tv/<id>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed Instagram (réservé aux comptes Professional/LinkedIn selon la documentation) ; API Graph Instagram pour les médias de comptes autorisés. |
| Authentification requise | oEmbed : conditions de compte à vérifier ; API Graph : jeton. |
| Type de contenu pris en charge | Reel, publication vidéo, IGTV. |
| Prévisualisation possible | **Non**. |
| Lecture intégrée possible | **Non**. |
| Limites | Publication désactivable par l'auteur ; contenu privé ; carrousels (le lien ne pointe qu'un média) ; compte professionnel requis pour l'oEmbed. |
| Message utilisateur | « Instagram n'est pas lisible dans B Music. » + action « Ouvrir à l'extérieur » |
| Tests nécessaires | (1) URL `instagram.com/reel/…` → badge + état non pris en charge ; (2) ouverture externe ; (3) lien de publication privée → message « contenu privé ou indisponible » côté ouverture externe. |

### 2.6 X (Twitter)

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://x.com/<user>/status/<id>` · `https://twitter.com/<user>/status/<id>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed X (publication de tweets, dont les médias) — conditions de compte et limites de taux à vérifier. |
| Authentification requise | oEmbed : à vérifier (historiquement lié à l'API payante). |
| Type de contenu pris en charge | Vidéo intégrée dans un post. |
| Prévisualisation possible | **Non**. |
| Lecture intégrée possible | **Non**. |
| Limites | Médias sensibles non accessibles ; compte requis pour une partie des contenus ; URL `x.com` et `twitter.com` à traiter comme équivalentes. |
| Message utilisateur | « X n'est pas lisible dans B Music. » + action « Ouvrir à l'extérieur » |
| Tests nécessaires | (1) URL `x.com/…/status/…` → badge « X » ; (2) URL `twitter.com/…/status/…` → badge « X » (équivalence) ; (3) ouverture externe. |

### 2.7 SoundCloud — résolu en interne

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://soundcloud.com/<user>/<track>` · `…/sets/<playlist>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed SoundCloud — à confirmer. |
| Authentification requise | Aucune pour l'aperçu ; compte requis pour certains contenus. |
| Type de contenu pris en charge | Piste audio. |
| Prévisualisation possible | **Oui** — après autorisation de l'hôte de miniature `i1.scdn.co` / `i.scdn.co` (politique d'images à étendre, lot L5). |
| Lecture intégrée possible | **Non**. |
| Limites | Contenus « privés » ou supprimés ; miniature non autorisée = icône locale de substitution (pas de recherche supplémentaire) ; durée parfois absente. |
| Message utilisateur | « Aperçu indisponible pour ce lien. » (miniature absente) · « Contenu privé ou indisponible. » |
| Tests nécessaires | (1) URL publique → badge + aperçu avec miniature ; (2) miniature non autorisée → substitution locale, pas d'erreur ; (3) contenu privé → état dédié. |

### 2.8 Bandcamp — résolu en interne

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://<artiste>.bandcamp.com/track/<nom>` · `…/album/<nom>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed Bandcamp — à confirmer. |
| Authentification requise | Aucune. |
| Type de contenu pris en charge | Piste audio (et=vidéo pour certains artistes). |
| Prévisualisation possible | **Oui** — après autorisation de `f4.bcbits.com` (lot L5). |
| Lecture intégrée possible | **Non**. |
| Limites | URL multi-sous-domaine ; contenu vendu/lié à un compte ; miniature absente sur certains albums. |
| Message utilisateur | « Aperçu indisponible pour ce lien. » |
| Tests nécessaires | (1) URL `*.bandcamp.com/track/…` → badge + aperçu ; (2) sous-domaine non standard → détection correcte ; (3) miniature non autorisée → substitution locale. |

### 2.9 Vimeo

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://vimeo.com/<id>` · `https://player.vimeo.com/video/<id>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : oEmbed Vimeo — à confirmer. |
| Authentification requise | Aucune pour les vidéos publiques. |
| Type de contenu | Vidéo. |
| Prévisualisation possible | **Oui** — `i.vimeocdn.com` est déjà autorisé par la politique d'images. |
| Lecture intégrée possible | **Non**. |
| Limites | Vidéos privées ou protégées par mot de passe → état « contenu restreint » ; vidéos supprimées. |
| Message utilisateur | « Contenu privé ou indisponible. » |
| Tests nécessaires | (1) URL publique → badge + aperçu + miniature ; (2) vidéo protégée → état « contenu restreint » ; (3) ouverture externe. |

### 2.10 PeerTube

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://<instance>/w/<uuid>` · `https://<instance>/videos/watch/<uuid>` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : API `/api/v1/videos/<uuid>` (publique sur la plupart des instances) — le format d'URL varie selon l'instance, à confirmer. |
| Authentification requise | Généralement aucune pour les vidéos publiques. |
| Type de contenu | Vidéo. |
| Prévisualisation possible | **Oui**, sous réserve que l'hôte de miniature de l'instance soit autorisé (instances arbitraires → décision de politique, lot L5). |
| Lecture intégrée possible | **Non**. |
| Limites | Instance auto-hébergée : format d'URL, miniature et disponibilité varient ; instance hors ligne. |
| Message utilisateur | « Aperçu indisponible pour ce lien. » |
| Tests nécessaires | (1) URL d'une instance connue → badge + aperçu ; (2) instance injoignable → état « connexion impossible » ; (3) miniature non autorisée → substitution locale. |

### 2.11 mediaCCC

| Champ | Contenu |
|---|---|
| Liens reconnus | `https://media.ccc.de/…` |
| Méthode officielle à vérifier | **Candidat (non vérifié)** : API/JSON publicly documented — à confirmer. |
| Authentification requise | Aucune. |
| Type de contenu | Vidéo (contenu de conférence, majoritairement allemand). |
| Prévisualisation possible | **Oui**, sous réserve d'autorisation de l'hôte de miniature. |
| Lecture intégrée possible | **Non**. |
| Limites | Droits de diffusion ; contenu allemand. |
| Message utilisateur | « Aperçu indisponible pour ce lien. » |
| Tests nécessaires | (1) URL `media.ccc.de/…` → badge + aperçu ; (2) miniature non autorisée → substitution locale. |

### 2.12 Plateforme non reconnue

| Champ | Contenu |
|---|---|
| Liens reconnus | Toute URL `http(s)` valide, quel que soit l'hôte. |
| Méthode officielle | Sans objet. |
| Authentification requise | Aucune. |
| Type de contenu | Inconnu. |
| Prévisualisation possible | **Non**. |
| Lecture intégrée possible | **Non**. |
| Limites | **Aucune requête n'est envoyée** pour une plateforme inconnue : seule la syntaxe est vérifiée localement. |
| Message utilisateur | « Plateforme non reconnue. » + action « Ouvrir à l'extérieur » |
| Tests nécessaires | (1) URL d'un site inconnu → badge « Plateforme non reconnue », aucune requête réseau ; (2) ouverture externe. |

## 3. Ce que la matrice ne dit pas

- Elle ne dit pas qu'une plateforme est « lisible dans B Music » : elle dit **ce que B Music sait en faire** (détecter, prévisualiser, ouvrir).
- Elle ne dit pas qu'un contenu sera disponible : disponibilité, âge, région, compte et confidentialité appartiennent à la plateforme, pas à B Music.
- Elle ne dit pas qu'une ouverture externe réussira : l'application cible peut être absente, le contenu supprimé, la région bloquée. B Music affiche alors un message d'indisponibilité et **ne masque jamais** l'échec.
- Elle ne dit pas qu'une **lecture intégrée** est possible : elle ne l'est nulle part en v1.2 (D2).

## 4. Ce qu'il reste à vérifier avant de figer la matrice

| # | À vérifier | Source attendue | Échéance |
|---|---|---|---|
| V1 | Liste exacte des hôtes et chemins acceptés par chaque plateforme (existence de `youtu.be`, `shorts`, `fb.watch`, `vm.tiktok.com`) | Documentation officielle de chaque plateforme | avant implémentation de `PlatformPolicy` |
| V2 | Existence et conditions d'usage des mécanismes oEmbed candidats | Documentation officielle de chaque plateforme | avant toute revendication d'intégration officielle |
| V3 | Plateformes réellement enregistrées dans le moteur embarqué (la liste du KDoc n'est pas une assertion d'exécution) | Test fonctionnel sur l'artefact | avant la recette |
| V4 | Jeu de tests de liens par plateforme, versionnés | `PlatformPolicy` + tests JVM | lot L5 |
| V5 | Hôtes de miniatures réellement servis par chaque plateforme | Observation réseau pendant la recette | lot L5 |
