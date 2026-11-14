# B Music — Modèle de menaces des onglets Explorer et Liens (v1.2)

> `security-manager`, phase SEC-01 → SEC-05, 14 novembre 2026.
> Périmètre : le produit Kotlin/Compose (copie locale `bmusic/`, branche `fix/stabilite-1.2`, HEAD `2377fe02`) et **les deux onglets v1.2** « Explorer » et « Liens ».
> Complète `docs/security/01-modele-menaces.md` (modèle v1.0 / flavor `fdroid`) : ce document couvre **la surface réseau et les entrées non fiables** que les deux onglets ouvrent.
> Le registre de risques associé : `security/risk-register.md`.

---

## 1. Cartographie de la surface exposée

### 1.1 Entrées non fiables

| # | Entrée | Origine | Point d'entrée dans le code | Contrôle appliqué |
|---|---|---|---|---|
| E1 | **URL collée** (champ « Liens ») | Utilisateur / autre app via `ACTION_SEND` | `DownloadScreen.kt` L147-221 (`LienTab`) → `DownloadViewModel.resolveLink()` L72-108 | `UrlValidator.validate` (syntaxe) + `SsrfGuard` (**corrigé** SEC-001) |
| E2 | **Requête de recherche** (champ « Explorer ») | Utilisateur | `SearchTab.kt` → `SearchViewModel.search()` L116/L139 | `QueryValidator` (2-200 car., pas de contrôle/NUL) |
| E3 | **Texte partagé** (`ACTION_SEND`) | Application tierce | `MainActivity.kt` L264-286 | Pré-remplissage seul, **aucune action automatique** |
| E4 | **Métadonnées distantes** (titre, auteur, pochette, URL de flux) | Fournisseur (YouTube, SoundCloud…) | `NewPipeExtractorEngine.resolve` L112-118 | `ImageUrlPolicy` (allowlist + HTTPS + 2 MiB) + `FileNameSanitizer` |
| E5 | **En-têtes de réponse HTTP** (`Location`, `Content-Disposition`, `Content-Length`) | Serveur distant | `DownloadTransferEngine.download` L66-94 | `UrlValidator` sur l'URL finale + `GuardedDns` (SEC-001) |
| E6 | **Nom de fichier fourni par le réseau** | Serveur distant | `FileNameSanitizer.sanitize` | Liste blanche d'extensions, anti-traversal, anti-bidi |
| E7 | **Intents entrants** (media search, PICK) | Applications tierces | `MainActivity.kt` L223-300 | `SearchQueryProvider.handleSearchIntent` |
| E8 | **Contenu d'une sauvegarde importée** (`BackupActivity`, `exported=true`) | Fichier choisi par l'utilisateur | `BackupActivity.kt` L140 (`ACTION_VIEW`) | **Zone grise** — audit hors périmètre de la phase v1.2 (cf. §6) |

### 1.2 Sorties

| Sortie | Destination | Contrôle |
|---|---|---|
| Requête de recherche | YouTube (`youtube.com/results`) | `QueryValidator` ; aucun identifiant de compte transmis |
| URL pasted | Fournisseur de la plateforme | `UrlValidator` + `GuardedDns` |
| Pochettes | Mémoire / cache Coil | `ImageUrlPolicy.ALLOWED_HOSTS` + garde Coil + `GuardedDns` + 2 MiB |
| Métadonnées affichées | UI | Rendu Compose, **pas d'interprétation HTML** |
| Logs | logcat | 3 `Log.w` dans le module downloader, aucun contenu de requête (SEC-V-06) |

### 1.3 Actifs

| # | Actif | Sensibilité | Où il vit |
|---|---|---|---|
| A1 | **Bibliothèque locale** (≈ 5 000 titres) | Haute | MediaStore / Scoped Storage |
| A2 | **Métadonnées, pochettes, playlists, favoris** | Haute | Room `music_database.db` + SharedPreferences |
| A3 | **Clé de signature release** | Critique | hors dépôt (variables d'environnement) — SEC-V-08 |
| A4 | **Intégrité de l'APK** | Haute | signature v2/v3 + SHA-256 (pipeline, lot L11) |
| A5 | **Vie privée** (goûts, requêtes, historique) | Haute | sur appareil ; historique borné à 10, effaçable |
| A6 | **Code source GPL-3.0** | Moyenne | fork publié, licence OSS only |
| A7 | **Réseau local de l'utilisateur** (box, NAS, partage Wi-Fi) | Haute | **nouveau** : les onglets ouvrent un accès réseau sortant |
| A8 | **Identité applicative** Last.fm / YouTube | Moyenne | `BuildConfig` — **le secret Last.fm y est embarqué** (SEC-006) |

### 1.4 Acteurs

| Acteur | Capacité | Intention |
|---|---|---|
| **Utilisateur légitime** | saisit des URL et des requêtes | utiliser la fonctionnalité |
| **Application tierce** (via `ACTION_SEND`) | fournit une chaîne arbitraire | injecter une URL piégée |
| **Serveur distant / contenu compromis** | contrôle les métadonnées et les redirections | servir une URL de redirection interne, un nom de fichier hostile |
| **Attaquant réseau passif** (Wi-Fi hostile) | observe le trafic clair | intercepter les requêtes / requêtes de recherche |
| **Attaquant avec accès au dépôt** | lit l'historique git | récupérer une clé embarquée (SEC-007) |
| **Attaquant供应链** (CI) | contrôle une action GitHub non pinnée | exécuter du code dans la chaîne de livraison (SEC-008) |

---

## 2. Frontières de confiance

```
┌─ B1 ──────────────────────────────────────────────────────────────┐
│  UI (Compose)  ──E1/E2──▶  ViewModel  ──E4──▶  état exposé      │
└───────────────────────────────┬───────────────────────────────────┘
                                │ B2 : URL non fiable
┌─ B3 ───────────────────────────▼───────────────────────────────────┐
│  Moteur d'extraction (NewPipe)  ──▶  client HTTP dédié           │
│  DownloaderHttpClient (+ GuardedDns)                             │
└───────────────────────────────┬───────────────────────────────────┘
                                │ B4 : réponses non fiables
┌─ B5 ───────────────────────────▼───────────────────────────────────┐
│  Internet (YouTube, SoundCloud, Last.fm)                         │
└───────────────────────────────────────────────────────────────────┘

  B6 : stockage local (Room / SharedPreferences) — hors réseau
  B7 : autre application (ACTION_VIEW sortant / ACTION_SEND entrant)
```

| Frontière | Ce qui la traverse | Invariant attendu |
|---|---|---|
| **B1 → B2** | URL brute | Schéma `http(s)`, longueur ≤ 2048, hôte ASCII valide, **destination réseau autorisée** |
| **B2 → B3** | URL résolue | Aucune IP privée / loopback / link-local / métadonnées cloud, à **chaque** redirection |
| **B3 → B4** | Métadonnées, URLs de flux, en-têtes | Filigranage : extension en liste blanche, nom de fichier sans chemin |
| **B4 → B5** | Requête de recherche | Pas d'identifiant de compte, pas de secret, borne 2-200 caractères |
| **B5 → B6** | Historique de recherche | ≤ 10 entrées, dédoublonné, effaçable par l'utilisateur, jamais envoyé à un tiers |
| **B6 → B7** | URL d'ouverture externe | Schéma `http(s)` uniquement — pas de `intent://`, `file://`, `javascript:` |

---

## 3. Matrice STRIDE par fonctionnalité

Légende : **I**mpact × **P**robabilité → `M`oyenne · `É`levée · `F`aible. Le renvoi `SEC-xxx` désigne la ligne du registre.

### F1 — Recherche YouTube (onglet « Explorer »)

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| Requête malveillante injectée dans l'URL construite | Tampering | A8 | M×F | **partiel** | `QueryValidator` borne et nettoie ; mais l'URL est interpolée **sans encodage** → **SEC-011** (lot L3) |
| CAPTCHA / blocage d'IP par YouTube | DoS | — | M×É | **non couvert** | NewPipe n'est pas un mécanisme officiel → **SEC-005** (risque assumé D-SEC-1) |
| Fuite des termes de recherche vers un tiers | Information disclosure | A5 | É×É | **par conception** | Inhérent à la recherche YouTube ; le **consentement et la mention** doivent figurer dans l'UI (lot L4/L10) |
| Interception des requêtes sur Wi-Fi hostile | Information disclosure | A5 | É×M | **non couvert** | HTTPS pour les métadonnées ; la **page de résultats** est le seul trafic non-garanti |

### F2 — Lien collé → résolution de métadonnées (onglet « Liens »)

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| **SSRF vers le réseau local / métadonnées cloud** | Elevation of privilege | **A7** | É×É | **corrigé** | `GuardedDns` (DNS épinglé + revalidation à chaque redirection) + garde fail-closed → **SEC-001**, **SEC-002**. *Avant correctif : aucune protection, redirections suivies sans contrôle* |
| Contournement du loopback par IPv6 non canonique | Elevation of privilege | A7 | M×M | **corrigé** | Parsing canonique 8 groupes → **SEC-002** |
| Hôte Unicode (homographe `yоutube.com`) | Spoofing | A8 | M×M | **corrigé** | ASCII strict sur les labels d'hôte → **SEC-003** |
| Fetch vers un hôte public arbitraire | Information disclosure | A5 | M×M | **partiel** | Aucune allowlist de plateformes → **SEC-013** (lot L5). Le SSRF est fermé ; le risque résiduel est la *vie privée*, pas le réseau interne |
| Fuite de l'URL collée vers la plateforme | Information disclosure | A5 | É×É | **par conception** | L'URL est transmise à la plateforme pour résoudre les métadonnées ; l'utilisateur doit le savoir |

### F3 — Téléchargement du flux

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| Flux pointant vers une IP privée | Elevation of privilege | A7 | É×M | **corrigé** | Garde fail-closed (`DownloadTransferEngine` L53-57) — *avant : exception avalée, le contrôle ne servait à rien* → **SEC-001** |
| Redirection vers une destination interne | Elevation of privilege | A7 | É×M | **corrigé** | `GuardedDns` revalidé à chaque saut ; contrôle de l'URL finale conservé |
| Débordement disque / zip-bomb | DoS | A1 | M×M | **couvert** | `DownloadLimits` : 200 Mo audio, arrêt si le flux dépasse l'annoncé |
| Nom de fichier hostile (traversal, extension exécutable) | Tampering | A1 | É×M | **couvert** | `FileNameSanitizer` : anti-traversal, liste blanche d'extensions, anti-bidi, troncature 100 |
| Exécution du média téléchargé | Elevation of privilege | A1 | F×É | **couvert** | Écriture MediaStore, pas d'exécution ; permissions non demandées |

### F4 — Pochettes distantes

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| SSRF via l'URL de pochette | Elevation of privilege | A7 | É×M | **couvert** | `ImageUrlPolicy` (allowlist stricte) + `CoilGuardInterceptor` + `GuardedDns` |
| Image immense (mémoire) | DoS | — | M×M | **couvert** | Plafond 2 MiB, 4 requêtes simultanées, cache mémoire 25 % |
| Hôte non listé → pochette non affichée | — | — | É×M | **fonctionnel** | **SEC-012** (lot L5) |

### F5 — Historique de recherche local

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| Conservation illimitée | Information disclosure | A5 | M×M | **couvert** | `LIMIT 10` + `trimToMax10()` + `clearAll()` + `deleteById` → SEC-V-05 |
| Synchronisation cloud involontaire | Information disclosure | A2, A5 | M×M | **couvert** | `allowBackup="false"` → SEC-V-07 |
| Fuite par logcat | Information disclosure | A5 | M×F | **couvert** | Requêtes jamais journalisées en clair → SEC-V-06 |

### F6 — Ouverture externe (`ACTION_VIEW`)

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| Ouverture d'un schéma dangereux (`intent://`, `file://`) | Elevation of privilege | A1 | M×F | **couvert** | `UrlValidator` refuse tout schéma non `http(s)` avant ouverture |
| Application tierce malveillante接收 l'URL | Spoofing | A5 | F×M | **résiduel assumé** | L'ouverture sort du contrôle de l'app ; l'utilisateur choisit la cible. Accepté (comportement standard Android) |
| **Intégration de contenu dans l'app** | Elevation of privilege | A1 | É×M | **absent par conception** | **Aucun WebView** → SEC-V-01, D-SEC-2. C'est le point de contrôle le plus important de ce dossier |

### F7 — Partage entrant (`ACTION_SEND`)

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| URL hostile pré-remplie | Tampering | A7 | M×M | **couvert** | Pré-remplissage seul, **aucune action automatique** ; l'utilisateur doit cliquer « Aperçu » → SEC-V-10 |
| Déclenchement de téléchargement sans consentement | Tampering | A1 | M×M | **couvert** | Aucune action automatique à l'identique |

### F8 — Intégration Last.fm (scrobble)

| Menace | STRIDE | Actif | I×P | État | Mesure / risque |
|---|---|---|---|---|---|
| Secret partagé extractible de l'APK | Spoofing | A8 | M×M | **ouvert** | **SEC-006** — le secret est dans `BuildConfig` pour `github`/`playstore` |
| Clé d'API dans l'historique git | Information disclosure | A8 | F×M | **ouvert** | **SEC-007** |
| Envoi d'historique d'écoute à Last.fm | Information disclosure | A5 | É×É | **documenté, non implémenté dans le périmètre v1.2** | Le scrobble existe dans le produit amont ; hors périmètre des deux onglets, mais la politique de confidentialité doit le dire (SEC-016) |

---

## 4. SEC-02 — Spécification du traitement des URL collées

### 4.1 Exigences et état réel

| # | Exigence | Implémentation | État |
|---|---|---|---|
| 1 | **Schéma HTTPS par défaut** | `UrlValidator` n'accepte que `http` et `https` ; `http` est **accepté** (pas imposé en HTTPS) | ⚠️ à trancher : le PM ne tranche pas ; l'impact est un downgrade vers une origine non chiffrée |
| 2 | **Parsing robuste** | Parsing manuel (pas `java.net.URL`), Authority / userinfo / port / IPv6 brackets gérés | ✅ |
| 3 | **Taille maximale** | `MAX_LENGTH = 2048`, refus `TOO_LONG` **avant** toute normalisation | ✅ |
| 4 | **Rejet des URL malformées** | 9 motifs de refus : `BAD_SCHEME`, `BAD_SYNTAX`, `HAS_USER_INFO`, `HAS_FRAGMENT`, `HAS_CONTROL_CHARS`, `MISSING_HOST`, `BAD_HOST`, `BAD_PORT`, `BLANK` | ✅ |
| 5 | **Domaines et sous-domaines validés** | Labels ≤ 63, ≤ 253 au total, pas de `_`, pas de tiret en bord ; **ASCII strict** depuis SEC-003 | ✅ |
| 6 | **Ports inhabituels** | Port ∈ 1..65535 ; le port par défaut est retiré ; pas de liste blanche de ports | ⚠️ le port est libre (impact faible : l'app n'expose pas de service) |
| 7 | **Normalisation sans altérer le lien** | Minuscules sur schéma et hôte seulement ; **chemin et requête conservés à l'octet** ; le port par défaut est retiré | ✅ |
| 8 | **Prévention des injections** | Refus CRLF, NUL, espaces internes, userinfo (`user:pass@`), fragment | ✅ |
| 9 | **Contrôle des redirections** | `followRedirects(true)` **avec** `GuardedDns` → chaque `Location` repasse par le résolveur guardé | ✅ *(corrigé)* |
| 10 | **Messages d'erreur sûrs** | `ExtractorError` typés → messages génériques (« Lien invalide », « Source non prise en charge », « Hors ligne ») ; **aucune URL ni stacktrace** dans l'UI | ✅ |

### 4.2 SSRF — état après correctif

| Plage | Traitement | Preuve |
|---|---|---|
| Loopback IPv4 `127/8` | bloqué | `isBlockedIpv4` |
| Loopback IPv6 `::1`, `0:0:0:0:0:0:0:1`, `0::1` | bloqué | `parseIpv6` canonique → SEC-002 |
| IPv4-compatible `::127.0.0.1` | bloqué | `isBlockedIpv6` → SEC-002 |
| Privées `10/8`, `172.16/12`, `192.168/16` | bloqué | `isBlockedIpv4` |
| Link-local `169.254/16` (dont `169.254.169.254`) | bloqué | `isBlockedIpv4` |
| CGNAT `100.64/10` | bloqué | `isBlockedIpv4` |
| ULA `fc00::/7`, link-local IPv6 `fe80::/10` | bloqué | `isBlockedIpv6` |
| Multicast `ff00::/8`, site-local `fec0::/10` | bloqué | `isBlockedIpv6` |
| IPv4-mappée `::ffff:a.b.c.d` | dédoublée puis testée en IPv4 | `isBlockedIpv6` |
| `0.0.0.0/8` | bloqué | `isBlockedIpv4` |
| Syntaxe d'adresse invalide | **fail-closed** (bloqué) | `isBlockedIpv4` / `parseIpv6` |
| **Rebinding DNS** | **fermé** : `GuardedDns` renvoie les IP validées et OkHttp s'y connecte ; chaque redirection revalide | `GuardedDns.kt` |

### 4.3 Règle d'or appliquée

> **Aucun fetch serveur arbitraire vers une URL utilisateur sans protection.**

Le contrôle est appliqué **au point de connexion** (`GuardedDns`), pas seulement sur la chaîne : c'est la seule façon de couvrir les redirections et le rebinding, qu'aucune validation de chaîne ne peut atteindre.

---

## 5. SEC-03 — Lecteurs intégrés, iframes et scripts tiers

### 5.1 Résultat de l'audit

| Élément | Présent ? | Preuve |
|---|---|---|
| `WebView` / `WebViewClient` | **Non** | `grep -rn "WebView\|setWebViewClient"` → 0 occurrence fonctionnelle |
| `loadUrl` / `loadData` / `loadDataWithBaseURL` | **Non** | 0 occurrence (les 3 hits sont des variables `downloadUrl`) |
| `addJavascriptInterface` / `@JavascriptInterface` | **Non** | 0 occurrence |
| `evaluateJavascript` | **Non** | 0 occurrence |
| `setJavaScriptEnabled(true)` | **Non** | 0 occurrence |
| `<iframe>` / script tiers | **Non** | le produit est une app native Kotlin, sans couche web |
| Sandboxed iframe / `sandbox=` | Non applicable | — |
| `postMessage` | **Non** | 0 occurrence |

### 5.2 Conséquences

1. **Aucun pont natif à révoquer** : il n'existe pas.
2. **Aucune origine à lister** : la politique d'origines autorisées est vide — ce qui est le meilleur état possible.
3. **Aucune exception fournisseur à justifier** : aucun fournisseur n'exige de lecteur intégré.
4. **L'ouverture externe n'exécute rien dans le contexte de l'application** : `IntentExt.openWeb()` (`IntentExt.kt` L35-36) émet un `ACTION_VIEW` vers le navigateur par défaut ; l'URL y est traitée par une application **distincte**, hors du sandbox de B Music.

### 5.3 Verrou à maintenir

Le lot L4 (extraction de `LienTab` / `SearchTab` en composables publics) **ne doit pas introduire de WebView**. Si un jour un aperçu intégré est proposé, il exige : origines autorisées listées, JS désactivé par défaut, aucun `addJavascriptInterface`, vérification d'**origine et de source** sur tout `postMessage`, politique de navigation restrictive, revue sécurité dédiée. C'est la condition de revoyure de **D-SEC-2**.

---

## 6. SEC-04 — Recherche YouTube, clés API et backend

### 6.1 Clés et secrets

| Question | Réponse | Preuve |
|---|---|---|
| Une clé YouTube / Data API v3 est-elle embarquée ? | **Non** | `grep -inE 'AIza\|api_key\|apiKey'` sur l'arbre de travail → 0 hit |
| Une clé Last.fm est-elle dans le code ? | **Non** | `build.gradle.kts` L236-248 : `local.properties` puis `System.getenv("LASTFM_API_KEY")` → `BuildConfig` |
| Un secret Last.fm est-il embarqué ? | **Oui** | `LASTFM_SECRET` → `BuildConfig` pour `github`/`playstore` → **SEC-006** |
| Y a-t-il des secrets dans l'historique git ? | **Oui** | clé API Last.fm dans `e038993f` (2025-04-16) et `6a7ddd3b` (2026-03-21) → **SEC-007** ; absente du dépôt de livraison |
| Le keystore de signature est-il dans le dépôt ? | **Non** | variables d'environnement ; `.gitignore` L22 |

### 6.2 Le projet a-t-il déjà un backend ?

**Non.** Le dépôt ne contient aucune fonction serveur, aucun `serverless/`, aucune configuration de déploiement. Le produit est une application Android **sans service tiers propre** : Last.fm et YouTube sont appelés **directement depuis le client**, via `KtorClient` (Last.fm) et `DownloaderHttpClient` (téléchargeur).

Conséquence directe : **il n'existe pas de solution existante et sécurisée** vers laquelle cannibaliser la clé. Le choix fait est donc : *pas de clé privée du tout* côté recherche YouTube (NewPipe n'en exige pas) — c'est la bonne réponse.

### 6.3 Proxy minimal — spécification (uniquement si une clé privée devient nécessaire)

Ce n'est **pas** aujourd'hui le cas. La spécification est donnée pour le cas où YouTube Data API v3, oEmbed officiel ou Last.fm_SESSION_KEY_secreshould exigerait un secret :

| Contrôle | Exigence | Justification |
|---|---|---|
| **Surface** | Un seul endpoint `GET /search?q=…&platform=…`, aucun endpoint générique de proxy | Un proxy générique devient un relais ouvert |
| **Validation des paramètres** | `q` : 2-200 caractères, caractères de contrôle refusés ; `platform` : liste blanche ; `page` : entier borné | Empêche l'usage comme proxy vers des cibles arbitraires |
| **Limitation de débit** | Par IP **et** par clé de client ; fenêtre glissante ; retour `429` | Anti-abus et respect des quotas fournisseur |
| **Cache** | TTL court (5-15 min), clé = hash de (plateforme, requête, page), taille bornée | Réduit la charge et le quota ; une requête ne part pas à chaque frappe |
| **Journalisation** | Journal **des codes de retour et des métadonnées de cache**, **jamais** des termes de recherche ni des clés ; purge à 7 jours | Minimisation |
| **Protection contre l'abus** | Quota global quotidien, blocage des URLs en sortie, pas de redirecteur, pas d'IP interne | Le proxy ne doit pas devenir un SSRF |
| **La clé** | Uniquement dans le secret manager du fournisseur d'hébergement ; jamais dans le dépôt, jamais dans l'APK | Règle du dossier : « aucune clé dans le client » |
| **Rotation** | Procédure trimestrielle documentée : rotation chez le fournisseur → mise à jour du secret manager → redéploiement → vérification par requête témoin | Évite qu'une clé compromise vive des mois |
| **Variables d'environnement** | `YOUTUBE_API_KEY`, `LASTFM_API_KEY`, `LASTFM_SECRET` (côté serveur uniquement), `PROXY_RATE_LIMIT_PER_MIN`, `PROXY_QUOTA_DAILY` | Noms documentés, valeurs hors dépôt |

**Décision** : le proxy n'est **pas** créé. La justification est inscrite en D-SEC-1 (§5 du registre).

### 6.4 Variables d'environnement et rotation (état réel)

| Variable | Où | Contient | Rotation |
|---|---|---|---|
| `LASTFM_API_KEY` | `local.properties` (hors dépôt) ou env | clé API publique Last.fm | selon la politique Last.fm |
| `LASTFM_SECRET` | env | **secret partagé** | **à faire lors du correctif SEC-006** |
| `KEYSTORE_FILE`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD` | env (CI) | clé de signature release | selon la politique de signature |
| `TG_*` (5 variables) | secrets GitHub | diffusion Telegram | — |

---

## 7. SEC-05 — Données personnelles et journaux

### 7.1 Ce qui est réellement collecté

| Donnée | Destination | Rétention | Base légale / justification |
|---|---|---|---|
| **Termes de recherche** | YouTube (via NewPipe) | côté YouTube : sa politique ; côté B Music : **aucune** | Fonctionnalité ; l'utilisateur voit la requête partir vers YouTube |
| **Historique de recherche** | Base Room locale `SearchHistory` | **≤ 10 entrées**, dédoublonnées, effaçables (`clearAll()`) | Minimisation ; effaçable par l'utilisateur |
| **URL collée** | plateforme cible (pour résoudre les métadonnées) | côté plateforme : sa politique ; **jamais stockée dans un journal** | Fonctionnalité |
| **URL source du téléchargement** | `DownloadEntity.sourceUrl` (base locale) | persistée avec l'entrée de téléchargement | Traçabilité de la file |
| **Métadonnées de la bibliothèque** | **aucune sortie réseau** | local | — |
| **Identifiant de compte plateforme** | **aucun collecté** | — | Ni token, ni cookie, ni header d'authentification sur le client downloader (`CookieJar.NO_COOKIES`) |
| **Télémétrie / analytics / crash-reporting** | **aucune** | — | Vérifié : 0 dépendance et 0 appel (SEC-V-02) |
| **Diagnostics** | logcat | tant que le tampon système conserve | 3 `Log.w` sans donnée sensible ; `Log.d`/`Log.v` non supprimés en release → **SEC-010** |

### 7.2 Minimisation — contrôles en place

| Contrôle | Preuve |
|---|---|
| Aucun cookie, aucun authentification sur le client downloader | `DownloaderHttpClient` : `CookieJar.NO_COOKIES` ; `DownloadLimits.USE_COOKIES = false` |
| Requête bornée et nettoyée | `QueryValidator` : 2-200 caractères, refus NUL et caractères de contrôle |
| Historique borné et effaçable | `SearchHistory.kt` : `LIMIT 10`, `trimToMax10()`, `clearAll()`, `deleteById()` |
| URL et requêtes absentes des journaux | `grep -rn "Log\." app/src/main/java/com/bmusic/app/downloader/` → 3 `Log.w`, aucun contenu de requête |
| Aucune sauvegarde cloud | `allowBackup="false"` (L38) |
| Aucun identifiant de compte plateforme | `DownloaderHttpClient` ne porte ni token ni cookie ; le scrobble Last.fm est une fonctionnalité amont hors périmètre v1.2 |

### 7.3 Permissions et messages utilisateur

| Permission | Déclarée | Utilisée par le code | Verdict |
|---|---|---|---|
| `INTERNET` | L33 | oui (téléchargeur, Last.fm) | **à retirer du flavor `fdroid`** → SEC-004 |
| `ACCESS_NETWORK_STATE` | L32 | **0 fichier** | **à retirer** → SEC-004 |
| `READ_MEDIA_AUDIO` | — | oui | garder |
| `READ_MEDIA_IMAGES` | L15 | 2 fichiers (pochettes externes) | garder — ou retirer si la feature est reportée |
| `READ_EXTERNAL_STORAGE` | — | 2 fichiers | garder (≤ 32) |
| `WRITE_EXTERNAL_STORAGE` | — | 1 fichier (éditeur de tags) | à revoir (M1 du modèle v1.0) |
| `BLUETOOTH`, `BLUETOOTH_CONNECT` | — | 3 fichiers | garder |
| `POST_NOTIFICATIONS` | — | 1 fichier | garder |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | — | oui | garder |
| `FOREGROUND_SERVICE_DATA_SYNC` | L30 | **0 fichier** (usage transitif) | à vérifier par `aapt dump permissions` |
| `MODIFY_AUDIO_SETTINGS` | L31 | **0 fichier** (usage transitif, Equalizer) | à vérifier par `aapt dump permissions` |
| `WAKE_LOCK` | L34 | oui | garder |

**Messages à l'utilisateur** : tous les messages d'erreur du chemin « Liens » sont **génériques** et ne divulguent ni URL, ni exception, ni stacktrace (`DownloadViewModel.resolveLink` L92-106). Les messages du moteur sont eux aussi génériques (« source temporarily unavailable, the engine may need an update »).

---

## 8. Points non vérifiés (zones grises)

| Point | Pourquoi | Qui peut le traiter |
|---|---|---|
| **E8 — Sauvegarde importée** (`BackupActivity`, `exported=true`, filtre `ACTION_VIEW` sur `content/*`) | Hors périmètre des deux onglets ; le modèle v1.0 l'avait identifié comme **H5** (validation inbound) sans suite | audit sécurité du module backup |
| **`CoverProvider` accessible par une app tierce** | Le composant Coil est `exported="true"` ; l'impact réel dépend des chemins exposés → **SEC-014** | build |
| **Comportement de YouTube sous ReCaptcha** | Non testable hors ligne ni sans compte ; l'issue est **structurelle** (scraping) → D-SEC-1 | lot L9 |
| **CVE des dépendances transitives** | Aucun outil de scan disponible dans l'environnement ; le pipeline n'en contient aucun → **SEC-015** | devops (lot L11) |
| **Traces d'usage d'une clé compromise** | La clé Last.fm de l'historique est publique par nature ; pas d'anomalie constatée, mais pas d'audit fournisseur possible | — |
