# Sonore — Revue de sécurité (Security Manager)

Périmètre : lecteur local, mono-utilisateur, cible **Android via Tauri 2 mobile** (PM §1, D2). Comparaison des enveloppes :

| Enveloppe | Surface | Verdict |
|---|---|---|
| **Tauri 2 mobile** (retenu) | IPC Rust mince, pas de Node dans le webview, Rust = parse robuste côté natif | ✅ Retenue — mais jeune : risque R1 PM, porte Go/No-Go à J1 (D2) |
| Electron | Chromium+Node par défaut ; chaque mauvais flag (nodeIntegration/contextIsolation/sandbox) ouvre RCE depuis une page web chargée | ❌ Écartée (plus lourde, plus de flags à tenir, multi-contexte) |
| Web localhost | Serveur sur 127.0.0.1 ; DNS rebinding + requêtes cross-origin si CSRF/Host absents ; attaque possible *depuis n'importe quel site visité* par l'utilisateur | ❌ Écartée sauf variante « fichier unique `file://` » sans serveur |

## 1. Modèle de menaces (STRIDE allégé)

**Actifs** : collection musicale (fichiers + métadonnées personnelles : goûts, chemins = infos privées), index de bibliothèque (titres/dossiers — cartographie du disque), préférences, intégrité du code exécuté, réputation. **Aucune donnée secrète** (pas de comptes, pas de clés).

**Surfaces d'attaque** : fichiers taguées hostiles (ID3/Vorbis/Pochette/nom de fichier) · pseudo-schémas internes (chemins, `asset:` URLs Tauri) · IPC commandes Rust ↔ webview · WebView elle-même · stockage local (sandbox app) · dépendances npm/cargo.

| Menace (STRIDE) | Actif touché | Mesure clé | Sévérité si absente |
|---|---|---|---|
| Spoofing — page web tierce parle au serveur local | Wrapper localhost | 127.0.0.1 only + vérif Host/Origin (écarté : pas de serveur) | Élevé |
| Tampering — tag ID3/pochette malformée exécutée ou surchargée | Intégrité parsing | Parsing côté Rust (crate taggée fuzzée), limites de taille, jamais de `innerHTML` | Élevé |
| Repudiation — n/a (local, pas d'actions sensibles) | — | Logs locaux minimaux, sans PII | Faible |
| Information disclosure — fichier lu hors périmètre (path traversal, symlink) | Fichiers de l'utilisateur | Allowlist de directeurs choisis (dialog), canonicalization + rejet symlinks/`..`/chemins absolus | Élevé |
| Denial of Service — fichier piégé (bombe archives, tag 1 Go) | Disponibilité app | Limites (fichier ≤ 200 Mo, tag image ≤ 20 Mo, pochette décodée ≤ 4096×4096), timeout parse, skip+toast | Moyen |
| Elevation of privilege — webview → IPC → FS sans contrôle | Disque entier | IPC allowlist stricte (commandes nommées), aucun `read_file(path_arbitraire)` | Élevé |
| Supply chain — dépendance compromise | Tout | Lockfile committé, `npm audit`/`cargo audit` à chaque ajout, zéro dep inutile, licences vérifiées | Moyen |

## 2. Exigences de sécurité (critères d'acceptation PM, bloquantes)

- **SEC-01** L'app n'ouvre **aucun socket réseau sortant** (ni HTTP(S), ni WS, ni télémétrie). Vérification : build puis proxy — 0 requête vers hôtes externes. CSP `connect-src 'none'` côté webview (à affiner aux seuls schémas Tauri `ipc:`/`tauri:` selon version installée, jamais `http:`).
- **SEC-02** Accès fichiers **strictement** confinés aux dossiers explicitement choisis via sélecteur natif ; URI persistés ; aucune API « lister racine » exposée. Tout autre chemin (inclusion, `..`, symlink, `file://` absolu injecté) → rejet net.
- **SEC-03** Canonicalisation des chemins + vérification `starts_with(dossier_autorisé)` côté Rust **avant** toute lecture ; symlinks résolus puis re-contrôlés ; EACCES/parse error → erreur typée, jamais de crash ni de contenu partiel renvoyé.
- **SEC-04** Tout texte issu de tags/noms de fichiers est **donnée opaque** : échappé avant rendu (textNode, jamais `innerHTML`/`dangerouslySetInnerHTML`) ; aucune URL dérivée de tags (pochettes en `data:`/`asset:` Tauri uniquement, ou décodage Rust → buffer).
- **SEC-05** Pochette : taille fichier ≤ 20 Mo, décodage limité (≤ 4096×4096 px, mémoire cap), format limité à JPEG/PNG/WebP ; sinon placeholder généré (dégradé déterministe) — jamais d'erreur propagée.
- **SEC-06** Fichiers audio : parse en Rust (taglib-class) avec limites et timeout ; fichier corrompu/tronqué → skip + toast « Fichier ignoré » ; l'app reste fonctionnelle (fuzz cible sur parser tag dans la CI si temps).
- **SEC-07** IPC minimal : commandes nommées uniquement (`scan_folder`, `read_track_meta`, `cover_bytes`, `save_prefs`…) ; **jamais** de commande générique « read arbitrary path » ; chaque commande valide ses arguments (longueur, allowlist, type).
- **SEC-08** Webview : `withGlobalTauri` off, CSP stricte au build, aucune ressource distante (police locale/système — Inter embarqué ou repli système ; icônes SVG inline Lucide — déjà conforme).
- **SEC-09** Stockage local uniquement : préférences + index bibliothèque dans le sandbox app ; **aucune PII** (pas de chemins absolus dans les logs ; logs ≤ debug en dev, silencieux en release) ; purge possible (« Effacer mes données »).
- **SEC-10** Zéro télémétrie / analytics / crash reporting tiers ; aucun outil externe (fonts, CDN, icônes distantes). Mises à jour : v1 = réinstaller manuellement ; si auto-update ajouté plus tard → signature + vérification obligatoire, sinon n'existe pas.
- **SEC-11** Dépendances : lockfiles committés (`package-lock.json`, `Cargo.lock`), `npm audit --omit=dev` et `cargo audit` à chaque ajout/release → 0 vuln critique/haute ouverte ; licences permissives uniquement (MIT/Apache/BSD/OFL) — **aucun asset Spotify** (nom, logo, #1DB954, icônes).
- **SEC-12** Android : permission stockage limitée à `READ_MEDIA_AUDIO` (+ `READ_EXTERNAL_STORAGE` ≤ API 32 si requis) ; storage scopé/URI persistants ; pas de `MANAGE_EXTERNAL_STORAGE` ; export de l'app sans permissions réseau/overlay/contacts/localisation.
- **SEC-13** CSP de la maquette/design conservée à l'implémentation : pas de `unsafe-inline` pour les styles dynamiques de pochettes (data-URI OK via `img-src data:`).

## 3. Checklist pré-release (J3)

- [ ] SEC-01 : `mitmproxy` clean sur 10 min d'usage (scan/play/search/playlist) ; build release sans hôte externe
- [ ] SEC-02/03 : tests traversée (`../`, symlink vers `/etc/passwd`, chemin absolu injecté via IPC) → tous rejetés
- [ ] SEC-04 : piste piégée `<img src=x onerror=alert(1)>` en titre → affichée en texte, aucune exécution (test manuel + jsdom)
- [ ] SEC-05/06 : pochette 30 Mo et 8000×8000 px, fichier 1 Mo de tags → gérés (skip/toast, app stable)
- [ ] SEC-07 : revue IPC — chaque commande argument-validée ; aucun `open(path)` direct non contrôlé
- [ ] SEC-11 : `npm audit --omit=dev` + `cargo audit` → 0 critique/haute ; lockfiles committés ; licences listées (1 page)
- [ ] SEC-12 : `aapt dump permissions` sur l'APK → aucune permission réseau/overlay/contacts/localisation
- [ ] SEC-08 : `grep -ri "http://\|https://" dist/` → aucune ressource distante (sauf schémas Tauri attendus)
- [ ] SEC-10 : `grep -ri "sentry\|analytics\|google-analytics\|crashlytics"` → vide
- [ ] Marque : aucun asset/nom/logo Spotify ; accent ambre uniquement (`#E8A33D`) ; polices OFL ; icônes Lucide (ISC)
- [ ] Écran « À propos » : liste des licences OSS
- [ ] Purge complète des données app testée (désinstallation → aucun fichier résiduel hors sandbox)

## 4. Audit des dépendances (état au 2026-11-06 — aucun code)

**Le projet n'a pas encore de `package.json`/`Cargo.toml`.** Aucun audit technique réel n'est possible ; voici le plan d'audit du socle retenu + exigence bloquante à J1 :

| Socle / dépendance prévue | Risque | Action exigée |
|---|---|---|
| `@tauri-apps/*` (api, cli) version pinnée | Webview/IPC — cœur du wrapper | Pinné dans lockfile ; suivre advisories Tauri ; MAJ à J1/J2/J3 |
| `tauri-plugin-*` (fs, dialog) | Accès FS via plugin — périmètre SEC-02 | N'activer que fs+dialog ; revue capabilities par plugin ; jamais `shell`/`http` |
| React 18/19 + TypeScript | UI only | Lockfile ; `npm audit` à chaque bump |
| Vite | Build dev | `--omit=dev` en prod ; audit dev inclus au moins aux jalons |
| Parsing tags côté Rust (crate taglib-class) | **Zone hostile n°1** | Crate maintenue, fuzzée ; version pinnée ; `cargo audit` à chaque jalon |
| Aucune lib audio JS | — | `<audio>`/WebAudio natifs ; **zéro dépendance audio tierce** sans validation |
| Polices/icônes | Licence + offline | Inter (OFL, embarquée) ; Lucide (ISC, inline) ; pochettes locales (SEC-05) |
| **Si Electron retenu** | nodeIntegration mal réglé = RCE | Non applicable (écarté) ; si retour Electron : SEC-08 renforcée (sandbox:true, contextIsolation:true, preload allowlist) |
| **Si variante localhost web** | DNS rebinding, CSRF, Host spoofing | Non applicable (écartée) ; si retenue : bind 127.0.0.1 + vérif `Host`/`Origin` + token éphémère par session |

Procédure d'audit à exécuter dès J0/J1 : `npm ci && npm audit --omit=dev` (rapport brut conservé dans `security/audit-<date>.txt`), `cargo audit`, contrôle licences (`license-checker`), puis blocage si critique/haute ouverte → **release gelée** jusqu'à correction ou justification écrite dans ce registre.

**Ce qui est refusé, avec alternative sûre** :
Nom/logo Spotify ou #1DB954 → alternative : accent ambre `#E8A33D` + wordmark « Sonore » (licence propre).
Fetch de pochettes/paroles en ligne (PM « Should/Could ») → alternative : pochettes embarquées + `.lrc` sidecar locaux uniquement.
Raccourcis clavier (PM "Won't v1") → pas de conflit, cohérent avec device mobile.
Écran de comptes/favoris cloud (brief v2) → refusé durablement ; alternative : tout local, export/import `.m3u` en v2 si demandé.

**Verdict** : posture maîtrisée *sur papier* ; aucune vulnérabilité critique ouverte (aucun code). Release gelée tant que SEC-01…SEC-12 ne sont pas tous validés en checklist J3. Validation nécessaire au J1 pour la porte Go/No-Go Tauri 2 mobile.

**Limites** : audit des tags non exécuté (pas de build) ; sandbox Android réel non testé ; checklist J3 = la promesse, pas encore la preuve.
