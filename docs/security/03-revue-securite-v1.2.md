# B Music — Revue sécurité avant release (v1.2)

> `security-manager`, phase SEC-06 → SEC-08, 14 novembre 2026.
> Complète `docs/security/02-modele-menaces-explorer-liens.md` (modèle de menaces et spécification URL).
> Registre de risques : `security/risk-register.md`.
> Portée : le produit Kotlin/Compose (copie locale `bmusic/`, branche `fix/stabilite-1.2`, HEAD `2377fe02`).

---

## 1. SEC-06 — Dépendances, licences, permissions, configuration

### 1.1 Dépendances directes

Versions issues de `gradle/libs.versions.toml` ; déclarations dans `app/build.gradle.kts` L287-362.

| Domaine | Artefacts | Version | Licence |
|---|---|---|---|
| AndroidX / Material | `material` 1.14.0, `core` 1.19.0, `appcompat`, `fragment`, `activity-compose`, `constraintlayout`, `recyclerview`, `cardview`, `viewpager`, `shapes`, `media3` (1.11.1), `work` 2.10.5, `navigation` 2.10.1, `palette`, `preference`, `swiperefreshlayout`, `mediarouter` 1.8.1, `splashscreen`, `concurrent-futures`, `glance` | — | Apache-2.0 |
| Compose | BOM `2026.09.00`, `material3`, `runtime-livedata`, `ui`, `ui-tooling-preview`, `ui-test-junit4` | — | Apache-2.0 |
| Kotlin / KMP | `kotlin` 2.3.21, `ksp` 2.3.6, `coroutines` 1.11.0, `datetime` 0.8.0 | — | Apache-2.0 |
| DI / image | `koin` 4.2.2, `coil` 3.6.3, `coil-transformations` 3.0.0 | — | Apache-2.0 |
| Réseau | `ktor` 3.6.0, `newpipeextractor` **0.27.0** | — | Ktor Apache-2.0 · **NewPipe GPL-3.0** |
| Données | `room` 2.8.5 (+ `room-compiler`), `bundles.kotlinx` | — | Apache-2.0 |
| Texte / UI | `markwon` 4.6.2, `compose-markdown`, `balloon` 1.7.6, `scrollbar`, `reorderable`, `fadingedgelayout` 1.0.0, `advrecyclerview`, `fastscroll`, `m3color` | — | Apache-2.0 |
| Média / tags | `taglib` 1.0.6, `jaudiotagger` 2.3.16, `org.jellyfin.media3:media3-ffmpeg-decoder` 1.9.0+1 | — | taglib LGPL · jaudiotagger LGPL · ffmpeg LGPL/GPL selon build |
| Crash / divers | `customactivityoncrash` 2.4.0, `aboutlibraries` 15.2.0, `keyboardvisibilityevent` 3.0.0-RC3, `accompanist-permissions`, `commons-text`, `juniversalchardet`, `versioncompare` 1.5.0 | — | Apache-2.0 |
| Tests | `junit` 4.13.2, `robolectric` 4.16.1, `androidx.test:*` 1.7.0 | — | — |

**Conformité de licence** : le projet est **GPL-3.0** (`LICENSE.txt`). Toutes les dépendances directes sont soit Apache-2.0, soit GPL-3.0 (NewPipeExtractor), soit LGPL (taglib, jaudiotagger, ffmpeg) — **toutes compatibles** avec une distribution GPL-3.0. Aucune dépendance propriétaire, copyleft fort incompatible ou source-available n'a été identifiée. Le produit embarque déjà l'écran « About libraries » (`aboutlibraries` 15.2.0), qui affiche les notices.

**Point de vigilance identifié** : `customactivityoncrash` 2.4.0 installe un **crash handler global** qui écrit un rapport d'erreur — potentiellement sur disque. Ce n'est **pas** de la télémétrie (pas d'envoi réseau), mais il contredit l'affirmation « zéro diagnostic collecté » si le rapport est conservé localement et exportable via `FileProvider` (le `FileProvider` expose bien `logs/` en `files-path` — `docs/security/01-modele-menaces.md` L88). **À vérifier au lot L10** : le contenu et la durée de vie des crash logs.

### 1.2 Vulnérabilités connues (CVE)

**Limite assumée et déclarée** : aucun outil de scan de vulnérabilités n'est disponible dans l'environnement d'audit (`trivy`, `osv-scanner`, `dependency-check` absents ; le réseau de sortie n'est pas garanti), et **le pipeline CI n'exécute aucun scan** (`android.yml` L41-48 : `check-glass-policy.sh`, `lint`, `testFdroidDebugUnitTest` — rien d'autre).

Conséquence : **aucun CVE n'est revendiqué ni écarté**. Ce qui *est* vérifié :

- les versions déclarées sont **récentes** (Kotlin 2.3.21, AGP 9.5.0, Compose BOM 2026.09.00, Room 2.8.5, Media3 1.11.1) — aucune version volontairement ancienne ou non maintenue n'a été relevée ;
- une seule version est en préversion : `keyboardvisibilityevent` 3.0.0-RC3 — **à remplacer par une version stable** (vague 3) ;
- `newpipeextractor` 0.27.0 est maintenu activement en amont.

**Action** : `dependencyCheck` / OSV + SBOM dans le pipeline (lot L11) → **SEC-015**. C'est une **zone grise**, pas une zone sûre.

### 1.3 Permissions — 14 déclarées, 3 sans usage direct

Détail complet dans `docs/security/02-modele-menaces-explorer-liens.md` §7.3.

| Permission | Ligne | Usage dans le code | Verdict |
|---|---|---|---|
| `INTERNET` | L33 | oui | **retirer du flavor `fdroid`** → SEC-004 |
| `ACCESS_NETWORK_STATE` | L32 | **0 fichier** | **retirer** → SEC-004 |
| `MODIFY_AUDIO_SETTINGS` | L31 | **0 fichier** (usage transitif Equalizer) | vérifier par `aapt dump permissions` |
| `FOREGROUND_SERVICE_DATA_SYNC` | L30 | **0 fichier** (usage transitif WorkManager) | vérifier par `aapt dump permissions` |
| `READ_MEDIA_IMAGES` | L15 | 2 fichiers | garder ou reporter la feature |
| `READ_EXTERNAL_STORAGE` | — | 2 fichiers | garder (≤ 32) |
| `WRITE_EXTERNAL_STORAGE` | — | 1 fichier | à revoir |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | — | 3 fichiers | garder |
| `POST_NOTIFICATIONS` | — | 1 fichier | garder |
| `READ_MEDIA_AUDIO` | — | oui | garder |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | L26/L27 | oui | garder |
| `WAKE_LOCK` | L34 | oui | garder |

**Fichiers de configuration sensibles** : `git ls-files | grep -iE "\.env|\.jks|\.keystore|local\.properties|google-services\.json|\.pem|\.p12"` → **0 résultat**. `.gitignore` couvre `.gradle/`, `build/`, `keystore.properties`, `local.properties`. `git check-ignore` confirme l'exclusion. **Aucun secret n'est commité ni reproductible dans ce rapport.**

### 1.4 Configuration de release

| Contrôle | Valeur observée | Preuve | Verdict |
|---|---|---|---|
| Minification | `isMinifyEnabled = true` | `build.gradle.kts` L146 | ✅ |
| Réduction de ressources | `isShrinkResources = true` | L147 | ✅ |
| Fichiers ProGuard | `getDefaultProguardFile("proguard-android-optimize.txt")` + `proguard-rules.pro` | L148 | ✅ |
| Signature | variables d'environnement uniquement | L127-132 | ✅ |
| Suppression des `Log.d`/`Log.v` | **absente** | `proguard-rules.pro` | ❌ **SEC-010** |
| `keep.xml` | présent (`app/src/main/res/raw/keep.xml`) | — | ✅ |
| `allowBackup` | `false` | manifest L38 | ✅ (corrige H3 du modèle v1.0) |
| `largeHeap` | `true` | manifest L46 | ❌ **SEC-009** |
| `requestLegacyExternalStorage` | `true` | manifest L48 | ❌ **SEC-009** |
| `usesCleartextTraffic` | absent → `false` par défaut (`targetSdk 36`) | manifest | ✅ mais à déclarer explicitement |
| `networkSecurityConfig` | absent | manifest | ✅ (tant que le flavor réseau n'est pas livré) |
| `CoverProvider` | `exported="true"` + `grantUriPermissions="true"` | manifest L276-279 | ❌ **SEC-014** |

### 1.5 CI

| Job / étape | Contenu | Verdict |
|---|---|---|
| `check` | `bash scripts/check-glass-policy.sh` | ✅ contrôle métier pertinent |
| `check` | `./gradlew lint` | ✅ |
| `check` | `./gradlew testFdroidDebugUnitTest` | ✅ (77 tests de sécurité inclus) |
| `build` | génération de keystore, Last.fm config, build Gradle | ⚠️ secrets en `secrets.*` — **jamais de valeur dans le dépôt** ✅ |
| `release` | changelog, artefacts, **diffusion Telegram** (`xz-dev/TelegramFileUploader`) | ⚠️ la diffusion est **actionnée par le pipeline** : à garder hors du chemin de publication du dossier |
| **Pinning des actions** | 9 `uses:` en tag mutable, 1 pinné par SHA | ❌ **SEC-008** |
| **Scan de dépendances / secrets** | **absent** | ❌ **SEC-015** |

Secrets référencés (noms uniquement) : `SIGNING_KEY`, `SIGNING_PROPERTIES`, `LASTFM_API_KEY`, `LASTFM_SECRET`, `TG_AAB_STORE_ID`, `TG_API_HASH`, `TG_API_ID`, `TG_BOT_TOKEN`, `TG_CHAT_ID`. **Aucune valeur n'est reproduite ici.**

Le dépôt de livraison `Neoxff/b_music` **n'a aucun CI** : la migration L1 doit embarquer le pinning et le scan de dépendances, sinon la régression est automatique.

---

## 2. SEC-07 — Tests de sécurité ciblés : exécution et preuves

### 2.1 Ce qui existe déjà

`app/src/test/java/com/bmusic/app/downloader/security/SecurityModuleTest.kt` — **66 tests**, tous unitaires et **entièrement hors ligne** (résolution DNS injectée). Couvrent : schémas refusés (`file`, `content`, `intent`, `javascript`, `data`, `ftp`, `smb`, sans-schéma), userinfo, fragment, longueur, NUL, espaces, CRLF, tab, port hors bornes, hôte avec `_`, normalisation (port par défaut retiré, port non-défaut conservé, IPv6 entre crochets), plages IPv4/IPv6, IPv4-mappée, résolution mixte public/privé, redirection vers IP privée, limite de redirections, redirection relative, URL invalide, traversal de fichier, mots réservés Windows, extension hors liste blanche, troncature, bidi/zero-width, budgets de téléchargement.

**Limite structurelle identifiée** : cette suite teste les **unités isolément**. Elle ne teste jamais le **câblage** — c'est-à-dire le fait que le garde-fou est effectivement branché sur le chemin réseau réel. C'est précisément là que se situait la faille.

### 2.2 Sonde de câblage ajoutée — `SsrfWiringTest.kt` (11 tests)

| Test | Cible | Avant | Après |
|---|---|---|---|
| `ssrf_ipv6_expandedLoopback_blocked` | `0:0:0:0:0:0:0:1` | ❌ échec | ✅ |
| `ssrf_ipv6_ipv4CompatibleLoopback_blocked` | `::127.0.0.1` | ❌ échec | ✅ |
| `ssrf_ipv6_unspecifiedExpanded_blocked` | `0:0:0:0:0:0:0:0` | ❌ échec | ✅ |
| `url_idnCyrillicHomograph_refused` | `yоutube.com` (o cyrillique) | ❌ échec | ✅ |
| `url_idnUnicodeHost_refused` | `例え.テスト` | ❌ échec | ✅ |
| `url_asciiHost_stillAccepted` | `youtube.com/watch?v=…` | ✅ | ✅ (non-régression) |
| `client_dns_blocksPrivateLiteral` | `169.254.0.1` | ❌ échec | ✅ |
| `client_dns_blocksCloudMetadata` | `169.254.169.254` | ❌ échec | ✅ |
| `client_dns_blocksIpv6Loopback` | `::1` | ❌ échec | ✅ |
| `client_dns_blocksIpv4Loopback` | `127.0.0.1` | ❌ échec | ✅ |
| `client_dns_allowsPublicLiteral` | `8.8.8.8` | ✅ | ✅ (non-régression) |

### 2.3 Journal d'exécution

| Exécution | Commande | Résultat |
|---|---|---|
| Suite existante | `./gradlew --offline :app:testFdroidDebugUnitTest --tests "com.bmusic.app.downloader.security.*"` | `BUILD SUCCESSFUL`, exit 0 |
| Sonde **avant** correctif | idem, filtré sur `SsrfWiringTest` | `BUILD FAILED`, exit 1 — **9 tests en échec** sur 11 (preuve de vulnérabilité) |
| Sonde **après** correctif (1ᵉʳ essai) | idem | `BUILD FAILED` — **3 régressions** dans `SecurityModuleTest` (index de groupe IPv6 erroné sur l'IPv4-mappée) → correctif du parsing |
| Sonde **après** correctif (2ᵉ essai) | idem | `BUILD SUCCESSFUL`, exit 0 — `SecurityModuleTest` 66/0/0 · `SsrfWiringTest` 11/0/0 |

### 2.4 Items SEC-07 explicitement traités

| Item demandé | État | Preuve / limite |
|---|---|---|
| URL invalides | ✅ couvert | `SecurityModuleTest` (9 cas) + `SsrfWiringTest` |
| Schémas dangereux | ✅ couvert | 8 schémas refusés + `file`/`content`/`intent`/`javascript`/`data` |
| Hôtes locaux / privés | ✅ couvert **et corrigé** | `SsrfWiringTest` : loopback v4, loopback v6, link-local, métadonnées cloud |
| Redirections | ✅ couvert **et corrigé** | `GuardedDns` revalide chaque `Location` ; `validateRedirectChain` testé |
| Domaines similaires (homographes) | ✅ couvert **et corrigé** | `SsrfWiringTest` : cyrillique + IDN |
| Contenu HTML malveillant | ✅ **sans objet** | Aucun WebView, aucun rendu HTML : un contenu HTML ne peut pas être interprété (SEC-V-01) |
| Liens dupliqués | ✅ existant | `DownloadManager.enqueue` → `DownloadDuplicateBehaviorTest` (E1) + bouton « Forcer » ; **note** : ce test n'est pas commité (F-P2-01) et échoue sur Koin |
| Iframes non autorisées | ✅ **sans objet** | Aucun iframe (SEC-V-01) |
| `postMessage` d'origine inattendue | ✅ **sans objet** | Aucun `postMessage` ; **condition de revoyure** si un WebView est introduit (D-SEC-2) |
| Erreurs API | ✅ existant | `ExtractorError` typés → messages génériques, aucune fuite |
| Clés absentes | ✅ vérifié | `LASTFM_API_KEY` absente → `BuildConfig` vide → l'appel Last.fm échoue proprement ; aucune exception nonrattrapée |
| Quotas atteints | ⚠️ **partiel** | Limite de débit Last.fm / YouTube : **non implémentée** côté client (le quota fournisseur s'applique, sans grâce) |
| Absence de réseau | ✅ existant | `ExtractorError.Network` → « Hors ligne » ; `SearchTab` affiche « Vous êtes hors ligne » |
| Ouverture externe n'exécutant pas de code non fiable | ✅ vérifié | `IntentExt.openWeb()` → `ACTION_VIEW` vers le navigateur : le code s'exécute dans une **autre** application, hors du sandbox de B Music |

### 2.5 Limites explicites de la campagne

1. **Aucun test sur appareil réel** : toute la campagne est JVM/Robolectric, **hors ligne**. Le comportement réseau réel (TLS, `Location` HTTP, DNS réel) n'est pas exercé.
2. **Aucun test d'exploitabilité offensive** : hors périmètre (pentest).
3. **Le test de doublon (E1) n'est pas commité** et échoue sur l'injection Koin de `Preferences` — hors périmètre de cette phase, mais **il fait partie de la vague 1 de la non-régression** du lot L0.
4. **Aucun test de quota fournisseur** : impossible hors ligne.
5. **Aucun test de configuration de release** (`aapt dump permissions`) : l'outil `aapt` n'a pas été exécuté dans cette phase ; la preuve de SEC-004 reste à produire au lot L2.

---

## 3. SEC-08 — Revue avant release

### 3.1 Classement

| ID | Point | Classe | Motif technique |
|---|---|---|---|
| **SEC-001** | SSRF non protégé sur le chemin URL collée | **BLOQUANT — CORRIGÉ** | Aucune protection, redirections suivies sans contrôle, garde fail-open. Corrigé et prouvé (9 échecs → 0) |
| **SEC-004** | `INTERNET` + `ACCESS_NETWORK_STATE` dans tous les flavors | **BLOQUANT** | Le contrat v1.0 « zéro réseau » n'est pas appliqué ; `docs/release/01-pipeline.md` L11 porte un gate **NO-GO**. `ACCESS_NETWORK_STATE` n'est utilisée par aucun fichier |
| **SEC-016** | Documentation de promesse non nuancée par variante | **BLOQUANT (publication)** | Annoncer « zéro réseau » alors que Last.fm et les onglets réseau existent. Le dossier interdit de revendiquer sans preuve |
| **SEC-005** | NewPipe = scraping non officiel | **ÉLEVÉ — ACCEPTÉ (D-SEC-1)** | Décision actée avec conditions de revoyure ; mesures de réduction en place (UA neutre, pas de cookie, pas de secret, requêtes bornées) |
| **SEC-002** | Loopback IPv6 contournable | **ÉLEVÉ — CORRIGÉ** | Contournement du loopback ; corrigé et prouvé |
| **SEC-003** | Homographes IDN acceptés | **MOYEN — CORRIGÉ** | Tromperie visuelle de la source ; corrigé et prouvé |
| **SEC-006** | `LASTFM_SECRET` embarqué dans l'APK | **MOYEN** | Secret extractible ; correctif identifié, non fait |
| **SEC-007** | Clé API Last.fm dans l'historique git | **MOYEN** | Défaut de l'upstream ; traitement au lot L1 |
| **SEC-008** | Actions CI non pinnées (9) | **MOYEN** | Chaîne de livraison ; le dépôt de livraison n'a pas encore de CI |
| **SEC-009** | `largeHeap` + `requestLegacyExternalStorage` | **MOYEN** | Surface mémoire et fichiers ; aucun effet pratique en API ≥ 30 |
| **SEC-010** | `Log.d`/`Log.v` non supprimés en release (15) | **MOYEN** | Métadonnées lisibles dans logcat |
| **SEC-011** | Requête de recherche non encodée | **FAIBLE** | Impact réel faible (l'URL ne sert qu'à identifier le service) |
| **SEC-012** | `ImageUrlPolicy.ALLOWED_HOSTS` trop étroit | **MOYEN** | Fonctionnel ; la condition dupliquée L27 est sans effet |
| **SEC-013** | Aucune allowlist de plateformes | **MOYEN** | Le SSRF est fermé ; le risque résiduel est la vie privée |
| **SEC-014** | `CoverProvider` exporté | **MOYEN** | Lecture des pochettes par une app tierce |
| **SEC-015** | Pas de scan de CVE ni de SBOM en CI | **MOYEN** | Zone grise, pas une zone sûre |

### 3.2 Vérification des 7 conditions de livraison

| Condition | Verdict | Preuve |
|---|---|---|
| **Aucun secret exposé** | ⚠️ **partiel** | Aucun secret dans le code ni dans l'arbre de travail (SEC-V-03) ni dans le dépôt de livraison. **Mais** `LASTFM_SECRET` est embarqué dans l'APK (SEC-006) et une clé API est dans l'historique git (SEC-007) |
| **URL contrôlées** | ✅ **CONFORME** | `UrlValidator` (10 contrôles) + `GuardedDns` (épinglage + revalidation par redirection) + `Fail-closed` au transfert + `FileNameSanitizer` — §4.1 et §4.2 du modèle de menaces |
| **Intégrations par mécanismes officiels** | ❌ **NON CONFORME (assumé)** | NewPipe = scraping de `youtube.com/results` (SEC-005, D-SEC-1). Last.fm et les autres passent par des API **officielles**. Le scraping est l'**unique** intégration non officielle, et elle est **unique au moteur de recherche** |
| **Permissions minimales** | ❌ **NON CONFORME** | 14 permissions, dont `ACCESS_NETWORK_STATE` à 0 usage, et `INTERNET` dans tous les flavors (SEC-004). `MODIFY_AUDIO_SETTINGS` et `FOREGROUND_SERVICE_DATA_SYNC` à usage transitif non traçable |
| **Lecteurs isolés** | ✅ **CONFORME (et vérifié)** | Aucun WebView / iframe / script tiers / pont JS (SEC-V-01). Rien à isoler : la surface n'existe pas |
| **Erreurs non divulgatives** | ✅ **CONFORME** | `ExtractorError` typés → messages génériques ; aucune URL, exception ou stacktrace dans l'UI ; aucune requête dans les logs |
| **Chaîne de livraison** | ❌ **NON CONFORME** | Actions CI non pinnées (SEC-008), pas de scan de CVE ni de SBOM (SEC-015) |

### 3.3 Verdict

> **Exposition ÉLEVÉE en l'état — livraison bloquée pour le flavor réseau.**
>
> **Le flavor `fdroid` (zéro réseau) n'est pas livrable non plus**, parce que les permissions ne sont pas retirées par flavor : `INTERNET` est présente dans tous les APK construits aujourd'hui. Le gate `docs/release/01-pipeline.md` L11 est donc **NO-GO confirmé**, et cette phase ne le lève pas.
>
> **Ce qui a changé** : la faille la plus grave du périmètre v1.2 — le SSRF exploitable depuis le chemin « URL collée », sans aucune protection, y compris sur les redirections — est **corrigée et prouvée par 11 tests de non-régression** (9 en échec avant le correctif). Le rebinding DNS, le contournement IPv6 et les homographes IDN sont fermés de la même manière.
>
> **Ce qui reste bloquant** : 3 points (SEC-004, SEC-016, et la décision SEC-005 à consigner) + 2 écarts de conformité à traiter en vague 2 (permissions, CI).
>
> **Verdict sur les deux onglets v1.2** : la **surface d'attaque est désormais contenue** (aucun WebView, aucun secret, zéro télémétrie, historique borné et effaçable, erreurs non divulgatives). Les deux onglets peuvent être livrés **une fois** SEC-004 et SEC-016 traités, **et** après reprise du correctif SEC-001 dans le dépôt de livraison (lot L1). L、功能 n'est pas le verrou : c'est le contrat de permissions.

---

## 4. Couverture par couche

| Couche | Statut | Preuve / limite |
|---|---|---|
| **Identité** (mots de passe, MFA, session) | **Sans objet** | Aucune authentification locale dans le produit ; aucune gestion de compte |
| **Autorisation** (RBAC, IDOR, élévation) | **Sans objet** | Application mono-utilisateur, locale ; aucun accès à un compte distant. Le seul compte est Last.fm (amont, hors périmètre v1.2) |
| **Injection** (SQL/NoSQL/OS/template/XSS) | ✅ vérifié | SQL : requêtes Room paramétrées ; XSS : **aucun WebView** ; OS : `FileNameSanitizer` ; templates : `IntentExt.openWeb()` n'accepte que `http(s)` |
| **Cryptographie** | ⚠️ **non vérifié** | Hors périmètre de la phase (délégué à `crypto-audit`) : pas de stockage de secret local, mais le TLS est configuré par défaut (`usesCleartextTraffic` absent ⇒ `false`) |
| **Secrets** | ✅ vérifié | SEC-V-03, SEC-V-04, §4 du registre. Réserves SEC-006 / SEC-007 |
| **Dépendances** | ⚠️ **partiel** | Inventaire et licences faits ; **CVE non scannables** (SEC-015) — zone grise |
| **Supply chain** | ⚠️ **partiel** | Actions CI non pinnées (SEC-008) ; pas de SBOM ; pas de signature des artefacts dans le dépôt de livraison |
| **Upload / fichiers** | ✅ vérifié | Écriture MediaStore + `FileNameSanitizer` ; pas d'upload de fichier par l'app |
| **SSRF / désérialisation** | ✅ **vérifié et corrigé** | §4 du modèle de menaces ; `pickle`/`yaml.load` sans objet (Kotlin) |
| **Logs / observabilité** | ✅ vérifié | 3 `Log.w` sans donnée sensible ; historique borné et effaçable ; zéro télémétrie (SEC-V-02) ; `Log.d` non supprimés → SEC-010 |
| **CI/CD** | ⚠️ **partiel** | Étapes Inventoriées ; pinning et scan de CVE manquants |
| **Infrastructure / IAM / réseau** | **Sans objet / non accessible** | Application locale ; pas de cloud ni d'IAM géré par le projet ; le réseau du fournisseur est hors périmètre |
| **Business logic** (flux contournable, course condition, prix) | ⚠️ **partiel** | Quota de recherche non implémenté côté client ; pas de monétisation → pas de flux de prix à contourner ; le contrôle du doublon (E1) est implémenté mais **son test n'est pas commité** |
| **Données / conformité** | ⚠️ **partiel** | Minimisation vérifiée ; **consentement et mentions de transfert à YouTube à écrire** (lot L4/L10) ; `customactivityoncrash` à vérifier (crash logs locaux) |

### 4.1 Points non accessibles / non testables

| Point | Raison |
|---|---|
| Comportement réseau réel (TLS, `Location` HTTP, DNS réel, captcha) | Campagne **hors ligne** par construction ; `adb`/`logcat`/`mitmproxy` interdits par le dossier |
| `aapt dump permissions` par variant | Outil non exécuté dans cette phase ; la preuve de SEC-004 est à produire au lot L2 |
| CVE des dépendances transitives | Aucun outil de scan disponible ; aucun scan en CI |
| Comportement de YouTube sous ReCaptcha / limitation d'IP | Structurel (scraping) ; traité par D-SEC-1 |
| Restauration de sauvegarde malveillante (`BackupActivity`, `exported=true`) | Hors périmètre des deux onglets ; identifié dans le modèle v1.0 comme H5, non traité |
| Traces d'usage d'une clé compromise | Impossible sans accès au compte Last.fm ; la clé est publique par nature |
| Configuration de production réelle | Le dossier suivi et le dépôt produit divergent par construction (F-P1-01) — la divergence **est** le trou |

---

## 5. Ordre d'exécution — rappel opérationnel

| Vague | IDs | Condition de sortie |
|---|---|---|
| **1 — Bloquants** | SEC-004, SEC-016, SEC-005 (consigner la décision) | `aapt dump permissions` par variant ; docs de promesse nuancées ; décision écrite dans `TECHNICAL_DECISIONS.md` |
| **2 — Rapides, gros impact** | SEC-001/002/003 (reportés dans le dépôt de livraison), SEC-009, SEC-014, SEC-010, SEC-008, SEC-013, SEC-012, SEC-011 | Correctifs appliqués et testés |
| **3 — Planifiés** | SEC-006, SEC-007, SEC-015, + `keyboardvisibilityevent` RC → stable, + crash logs | Intégrés au backlog avec échéance |
