# B Music — Registre de risques

> **Source de vérité unique** du dossier sécurité. Toute ligne porte un identifiant stable : un risque corrigé garde son ID.
> Mis à jour par le `security-manager` le **14 novembre 2026** (phase SEC-01 → SEC-08).
> Périmètre : produit Kotlin/Compose (copie locale `bmusic/`, branche `fix/stabilite-1.2`, HEAD `2377fe02`) + dépôt de livraison `Neoxff/b_music`.

---

## 0. Conventions

| Colonne | Valeurs autorisées |
|---|---|
| **Statut** | `ouvert` · `en cours` · `corrigé` · `accepté` · `transféré` |
| **Impact / Prob.** | `Faible` · `Moyenne` · `Élevée` · `Critique` |
| **Effort** | `S` (≤ 1 j) · `M` (2-5 j) · `L` (≥ 5 j) |
| **Sévérité** | `Critique` · `Élevée` · `Moyenne` · `Faible` · `Info` |

**Règle de sévérité** : `Sévérité = f(Impact, Probabilité)`. L'effort de correction **n'entre pas** dans la sévérité : il détermine l'**ordre d'exécution** (§3).
**Règle de déduplication** : un même défaut (CWE + surface) trouvé par plusieurs agents reste **une** ligne. Les identifiants des phases antérieures sont conservés dans la colonne « Origine ».

**Actifs** : `A1` bibliothèque locale · `A2` métadonnées et pochettes · `A3` clé de signature release · `A4` intégrité de l'APK · `A5` vie privée / habitudes d'écoute · `A6` code source GPL-3.0 · `A7` réseau local de l'utilisateur (partage Wi-Fi, box, NAS) · `A8` identité applicativeLast.fm / YouTube.

---

## 1. Registre — risques de sécurité

| ID | Finding | CWE | Surface | Actif | Impact | Prob. | Effort | Sévérité | Statut | Propriétaire | Décision |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **SEC-001** | **SSRF non protégé sur le chemin « URL collée → fetch ».** Le client HTTP du module downloader suivait les redirections (`followRedirects(true)`) **sans aucun contrôle de destination** ; le garde SSRF du transfert était *fail-open* (`catch (_: Exception) {}`) ; le moteur d'extraction ne validait que la syntaxe avant de laisser NewPipe télécharger l'URL. | CWE-918 | `DownloaderHttpClient.kt`, `NewPipeExtractorEngine.resolve`, `DownloadTransferEngine.download` | A1, A7 | Sonde du réseau local depuis l'appareil de l'utilisateur ; lecture de métadonnées cloud (`169.254.169.254`) | Élevée | S | **Critique** | `corrigé` | security-manager | Corrigé le 2026-11-14 : `GuardedDns` branché sur les 2 clients, garde fail-closed, garde typé au moteur. Preuve : 11 tests `SsrfWiringTest` (9 en échec avant, 0 après) |
| **SEC-002** | **Contournement du loopback IPv6 par forme non canonique.** `SsrfGuard.isBlockedAddress` comparait des chaînes : `0:0:0:0:0:0:0:1` (forme renvoyée par `Inet6Address.getHostAddress()`) et `::127.0.0.1` (IPv4-compatible) passaient le contrôle. | CWE-918 | `SsrfGuard.kt` `isBlockedAddress` | A7 | Accès au loopback de l'appareil | Moyenne | S | **Élevée** | `corrigé` | security-manager | Corrigé le 2026-11-14 : parsing canonique en 8 groupes + IPv4-mappée / IPv4-compatible explicites |
| **SEC-003** | **Hôtes Unicode acceptés par le validateur d'URL** (`isLetterOrDigit()` est Unicode-aware) : `yоutube.com` (o cyrillique) passe la validation et s'affiche comme `youtube.com`. | CWE-1007 | `UrlValidator.kt` `isValidHostSyntax` | A8 | Tromperie visuelle de l'utilisateur sur la source du lien | Moyenne | S | Moyenne | `corrigé` | security-manager | Corrigé le 2026-11-14 : ASCII strict sur les labels d'hôte et sur les littéraux IPv6 |
| **SEC-004** | **`INTERNET` + `ACCESS_NETWORK_STATE` déclarées dans `app/src/main/AndroidManifest.xml`**, donc dans *tous* les flavors : aucun `app/src/fdroid/AndroidManifest.xml` n'existe. La variante « zéro réseau » du contrat v1.0 n'est pas techniquement appliquée. `ACCESS_NETWORK_STATE` n'est par ailleurs utilisée par **aucun** fichier du code (0 occurrence). | CWE-250 | `app/src/main/AndroidManifest.xml` L32-33 | A5 | Sortie réseau possible dans la variante censée être hors-ligne | Élevée | S | **Élevée** | `ouvert` | build (lot L2) | **Aucun contournement.** Bloquant livraison tant que non fait (idem F-P1-03) |
| **SEC-005** | **NewPipe = scraping non officiel de `youtube.com/results`**, contraire à la règle du dossier « mécanismes officiellement autorisés ». Pas de clé API (donc pas de secret client), mais scraping d'un endpoint non documenté : fragilité (captcha, blocage IP), dépendance à une page non contractuelle, risque juridique. | CWE-441 | `feat/dl-etape-2` `NewPipeExtractorEngine.search` L140 | A8, A6 | Rupture de service, action en justice, réputation | Élevée | M | **Élevée** | `accepté` | coordinator | **Accepté explicitement** le 2026-11-14 sous conditions (voir §5, D-SEC-1). Condition de revoyure : blocage/captcha de YouTube ou décision de(last).fm→ YouTube → ré-evaluate l'API Data v3 |
| **SEC-006** | **Le secret partagé Last.fm (`LASTFM_SECRET`) est injecté dans `BuildConfig`** pour les flavors `github` et `playstore` → présent dans l'APK, extractible par quiconque télécharge l'application. Seule la clé API publique devrait y figurer. | CWE-798 | `app/build.gradle.kts` L241-248 | A8 | Forge de session key Last.fm, abuse de quota sous l'identité de l'app | Moyenne | S | Moyenne | `ouvert` | build | **Aucun contournement.** Corriger au lot L5/L10 : le secret ne doit pas être embarqué (authentification par jeton de session porteur côté serveur, ou Last.fm sans secret) |
| **SEC-007** | **Clé API Last.fm présente dans l'historique git** de l'upstream BoomingMusic (commits `e038993f` « first commit » 2025-04-16 et `6a7ddd3b` 2026-03-21). Retirée de l'arbre de travail, **présente dans l'historique**. Absente du dépôt de livraison `Neoxff/b_music`. | CWE-798 | historique `bmusic/` | A8 | Identité Last.fm de l'upstream compromise (défaut de la clé publique Last.fm) | Faible | M | Moyenne | `ouvert` | security-manager | À traiter à la migration L1 (réécriture d'historique ou non-migration de l'historique amont). **Ne pas reproduire la valeur dans ce registre** |
| **SEC-008** | **Actions GitHub CI non pinnées par SHA de commit** : 9 occurrences en tag mutable (`actions/checkout@v6`, `actions/setup-java@v5`, `actions/upload-artifact@v7`, `actions/download-artifact@v8`, `actions/github-script@v9`, `android-actions/setup-android@v4`, `softprops/action-gh-release@v3`). Une seule action tierce est pinnée (`xz-dev/TelegramFileUploader@f597b98…`). | CWE-829 | `.github/workflows/android.yml`, `release.yml` | A4 | Exécution de code arbitraire dans la chaîne de livraison (tag mutable = référence non immuable) | Moyenne | S | Moyenne | `ouvert` | devops | Recommandé avant le lot L11. Le repo de livraison n'a **aucun** CI aujourd'hui : la migration doit embarquer le pinning |
| **SEC-009** | **`largeHeap="true"` et `requestLegacyExternalStorage="true"`** présents dans le manifest. Le second est sans effet à `targetSdk 36` ; le premier augmente la surface mémoire (téléphone volé/occupé). | CWE-400 / CWE-922 | `app/src/main/AndroidManifest.xml` L46, L48 | A1 | Épuisement mémoire ; accès fichiers hors périmètre sur API ≤ 29 | Moyenne | S | Moyenne | `ouvert` | build | **Aucun contournement.** Retirer (correctif H4 du modèle v1.0, jamais exécuté) |
| **SEC-010** | **`Log.d` / `Log.v` ne sont pas supprimés en build release** : 15 occurrences dans le produit, et `proguard-rules.pro` ne contient pas de règle `-assumenosideeffects`. Les logs restent lisibles par les autres applications sur appareil rooté. | CWE-532 | produit entier ; `app/proguard-rules.pro` | A1, A2 | Chemins de fichiers, métadonnées audio, requêtes dans logcat | Moyenne | S | Moyenne | `ouvert` | build | Corriger au lot L11 : `-assumenosideeffects class android.util.Log { public static int d(...); ... }`. Le module downloader n'expose que 3 `Log.w` sans donnée sensible → impact localisé |
| **SEC-011** | **Requête de recherche interpolée sans encodage** dans `"https://www.youtube.com/results?search_query=$clean"` (branche `feat/dl-etape-2`). Le nettoyage retire les caractères de contrôle mais laisse `&`, `#`, `?`. | CWE-88 | `feat/dl-etape-2` `NewPipeExtractorEngine.search` | A8 | Pollution de paramètres dans l'URL construite | Faible | S | Faible | `ouvert` | build | Exigence au lot L3 : encoder la requête (`URLEncoder`) **et** passer par `UrlValidator.validate`. Impact réel faible : l'URL ne sert qu'à identifier le service, la requête réelle transite par `fromQuery(clean)` |
| **SEC-012** | **`ImageUrlPolicy.ALLOWED_HOSTS` trop étroit** (7 hôtes YouTube/Vimeo) : les pochettes SoundCloud / Bandcamp / PeerTube sont bloquées par `CoilGuardInterceptor`. La ligne 27 contient en outre une condition dupliquée (`h.endsWith(".$a") \|\| h.endsWith(".$a")`). | CWE-184 | `ImageUrlPolicy.kt` L12-28 | — | Pochettes non affichées sur 3 plateformes supportées | Élevée | S | Moyenne | `ouvert` | build | Étendre au lot L5 (idem F-P1-05). La condition dupliquée est sans effet (le cas exact est traité en L25) |
| **SEC-013** | **Aucune allowlist de plateformes pour les liens collés** : `UrlValidator` ne valide que la syntaxe ; `NewPipe.getServiceByUrl` accepte n'importe quel hôte public. Une URL collée vers un site inconnu déclenche un fetch réseau (borné désormais par le garde SSRF, mais pas par une politique de plateformes). | CWE-20 | `NewPipeExtractorEngine.resolve` ; `UrlValidator.kt` | A8 | Fetch vers un hôte arbitraire ; message d'erreur non conforme au PM | Moyenne | M | Moyenne | `ouvert` | build | Table `PlatformPolicy` au lot L5 (idem F-P1-06). **Le garde SSRF (SEC-001) est en place : le risque résiduel est l'atteinte à la vie privée, pas le SSRF** |
| **SEC-014** | **`CoverProvider` (Coil) est `exported="true"` avec `grantUriPermissions="true"`** : toute application tierce peut lire les pochettes via `content://<applicationId>.cover/…`. | CWE-926 | `app/src/main/AndroidManifest.xml` L276-279 | A2 | Lecture des pochettes de la bibliothèque par une app tierce | Moyenne | S | Moyenne | `ouvert` | build | Restreindre (correctif H1 du modèle v1.0, jamais exécuté). `allowBackup` est déjà `false` → correctif H3 **fermé** |
| **SEC-015** | **Aucun scan de dépendances / CVE en CI** : le workflow n'exécute que `check-glass-policy.sh`, `lint` et `testFdroidDebugUnitTest`. Pas de `dependencyCheck`, pas de SBOM. | CWE-1104 | `.github/workflows/android.yml` | A4 | Vulnérabilité transitives non détectée avant livraison | Moyenne | M | Moyenne | `ouvert` | devops | À ajouter au lot L11 (pipeline Gradle + SHA-256 + SBOM) |
| **SEC-016** | **La documentation de promesse n'est pas nuancée par variante** : « aucune autorisation réseau n'est utilisée » (`docs/pm/04-acceptation-lancement.md` L137-138), « DoD #3 zéro requête sortante » (`docs/lean/01-perimetre-mvp.md` L165-166) — alors que les flavors `github`/`playstore` contactent Last.fm et que les onglets réseau sont prévus. | CWE-451 | `docs/pm/`, `docs/lean/`, fiche store | A5 | Information inexacte à l'utilisateur et à l_authority de magasin | Élevée | S | Moyenne | `ouvert` | pm | Nuancer au lot L10 (idem F-P2-02). **Conditionne la publication** : le dossier interdit d'annoncer « zéro réseau » sans preuve |

### 1 bis. Points de contrôle vérifiés **sans anomalie** (positifs)

Ces lignes ne sont pas des risques : ce sont des contrôles **vérifiés conformes**. Elles sont conservées pour empêcher un réexamen inutile et pour tracer la couverture.

| ID | Point de contrôle | Preuve | Statut |
|---|---|---|---|
| SEC-V-01 | **Aucun WebView, iframe, script tiers ni pont JavaScript** dans le produit Kotlin | `grep -rn "WebView\|loadUrl\|loadData\|addJavascriptInterface\|evaluateJavascript\|setJavaScriptEnabled\|@JavascriptInterface\|shouldOverrideUrlLoading"` sur `app/src/main/java` → 0 occurrence fonctionnelle (les 3 hits sont des noms de variables `downloadUrl`) | `corrigé` / sans objet — **à préserver au lot L4** (D2 : l'aperçu Liens reste métadonnées + ouverture externe) |
| SEC-V-02 | **Zéro télémétrie, zéro analytics, zero crash-reporting** | `grep -rniE "firebase\|crashlytics\|sentry\|bugsnag\|analytics\|telemetry\|mixpanel\|amplitude\|appcenter"` → 0 correspondance réelle (les hits sont `AMPLITUDE_DP` d'un widget et `isSettingsEntry` du backup) | conforme |
| SEC-V-03 | **Aucun secret dans le code ni dans l'arbre de travail** | `grep -rniE "AIza…\|api_key\s*=\s*\"\|client_secret\|bearer \|password\s*=\s*\"\|token\s*=\s*\""` → 0 hit ; la clé Last.fm provient de `local.properties` / variable d'environnement `LASTFM_API_KEY` injectée en `BuildConfig` | conforme (hors SEC-006 : le *secret* reste embarqué) |
| SEC-V-04 | **Fichiers sensibles non suivis par git** | `git ls-files \| grep -iE "\.env\|\.jks\|\.keystore\|local\.properties\|google-services\.json\|\.pem\|\.p12"` → 0 résultat ; `.gitignore` L18-22, L78 ignorent `.gradle/`, `build/`, `keystore.properties`, `local.properties` | conforme |
| SEC-V-05 | **Historique de recherche borné et effaçable** | `SearchHistory.kt` : `LIMIT 10`, index unique sur `normalized_query`, `trimToMax10()`, `clearAll()` appelé depuis `SearchViewModel.kt` L167, `deleteById` L160 | conforme |
| SEC-V-06 | **Requêtes de recherche jamais journalisées en clair** | `QueryValidator.kt` KDoc L6 ; 3 `Log.w` dans tout le module downloader, aucun ne contient d'URL ni de requête | conforme |
| SEC-V-07 | **`allowBackup="false"`** | `app/src/main/AndroidManifest.xml` L38 → correctif H3 du modèle v1.0 **fermé** | conforme |
| SEC-V-08 | **Signing release par variables d'environnement, jamais par fichier commité** | `app/build.gradle.kts` L127-132 : `KEYSTORE_FILE`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD` via `System.getenv` | conforme |
| SEC-V-09 | **Minification + réduction de ressources actives en release** | `app/build.gradle.kts` L146-148 : `isMinifyEnabled = true`, `isShrinkResources = true`, `proguardFiles(…)` | conforme (cf. SEC-010 pour les logs) |
| SEC-V-10 | **Le partage `ACTION_SEND` ne déclenche aucune action automatique** | `MainActivity.kt` L264-286 : le texte est seulement pré-remplissé ; l'URL n'est exécutée que sur action explicite de l'utilisateur | conforme |

---

## 2. Risques non-sécurité (rappel des phases antérieures)

Ces risques ne relèvent pas du `security-manager` mais conditionnent la livraison ; ils sont listés ici pour que le registre reste **unique**.

| ID | Finding | Phase d'origine | Sévérité | Statut | Propriétaire |
|---|---|---|---|---|---|
| F-P1-01 | Le dépôt `Neoxff/b_music` ne contient pas le produit (145 fichiers, 0 Kotlin) | PLAN | Critique | `residual` | build (L1) |
| F-P1-02 | L'override `search` YouTube n'existe que sur `feat/dl-etape-2` → l'onglet Explorer échoue sur HEAD | PLAN | Élevée | `residual` | build (L3) |
| F-P1-08 | Un onglet top-level exige 5 fichiers (menu reconstruit à l'exécution) | PLAN | Élevée | `residual` | build (L6) |
| F-P1-09 / F-P1-10 | `nav_download` absent de la barre visible ; icônes manquantes | PLAN | Faible | `residual` | build (L6) |
| F-P1-11 / F-P1-12 | Prototype Capacitor hors périmètre | PLAN | Moyenne | `out_of_scope` | — |
| F-P2-01 | 4 fichiers non commités | PLAN | Faible | `residual` | build (L0) |
| F-PM-01 / F-PM-02 | `A11Y-4` contredit le code ; cibles tactiles 48 dp vs doc 44 dp | PM | Moyenne / Faible | `residual` | pm / design |
| F-PM-03 | Liens courts et équivalences de domaines non traités | PM | Moyenne | `residual` | build (L5) |
| F-PM-05 | Recette PM-08 inexécutable (0 scénario exécuté) | PM | Moyenne | `residual` | qa (post-L6) |
| F-DS-01 | `BottomNavigationView` plafonné à 6 entrées ; le menu atteint déjà 6 → +2 onglets = exception | DESIGN | **Critique** | `residual` | plan + coordinator (D8) |
| F-DS-02 | Glyphes Unicode comme icônes + chaînes en dur dans les écrans Téléchargeur | DESIGN | Élevée | `residual` | build (L4) |
| F-DS-03 / F-DS-04 / F-DS-05 | Corpus de tokens et d'icônes désynchronisé du code | DESIGN | Moyenne / Faible | `residual` | design |
| F-DS-07 | Aucune baseline de capture → non-régression visuelle non prouvable | DESIGN | Moyenne | `residual` | build (L6) |

---

## 3. Ordre d'exécution (3 vagues)

### Vague 1 — Bloquants la mise en production

| ID | Action | Pourquoi c'est bloquant |
|---|---|---|
| **SEC-004** | Créer `app/src/fdroid/AndroidManifest.xml` retirant `INTERNET` et `ACCESS_NETWORK_STATE` (`tools:node="remove"`), et `app/src/<flavor-réseau>/AndroidManifest.xml` les portant. Preuve attendue : `aapt dump permissions` par variant. | Le contrat v1.0 « zéro réseau » n'est pas appliqué. `ACCESS_NETWORK_STATE` n'est utilisée par aucun fichier → suppression sans impact. `docs/release/01-pipeline.md` L11 porte un gate **NO-GO** |
| **SEC-016** | Nuancer les docs de promesse par variante avant publication | Le dossier interdit d'annoncer « zéro réseau » sans preuve |
| **SEC-005** | Acter la décision (déjà acceptée, §5) et la consigner dans `TECHNICAL_DECISIONS.md` avant le lot L3 | Condition posée par la phase PLAN : aucun port du `search` YouTube sans décision écrite |

### Vague 2 — Vite corrects, gros impact

| ID | Action | Effort |
|---|---|---|
| **SEC-001 / SEC-002 / SEC-003** | ✅ **Déjà corrigés** le 2026-11-14 — reste à reporter dans le dépôt de livraison (lot L1) | S (fait) |
| **SEC-009** | Retirer `largeHeap` et `requestLegacyExternalStorage` | S |
| **SEC-014** | Restreindre `CoverProvider` (`exported="false"` ou permission de signature) | S |
| **SEC-010** | Ajouter `-assumenosideeffects` pour `Log.d`/`Log.v` | S |
| **SEC-008** | Pinner les 9 actions CI par SHA | S |
| **SEC-013 / SEC-012** | Table `PlatformPolicy` + extension de `ImageUrlPolicy.ALLOWED_HOSTS` (lot L5) | M |
| **SEC-011** | Encoder la requête de recherche + `UrlValidator.validate` (lot L3) | S |

### Vague 3 — Planifiés (backlog)

| ID | Action | Échéance proposée |
|---|---|---|
| **SEC-006** | Sortir le secret partagé Last.fm de l'APK (jeton porteur, ou Last.fm sans secret) | Lot L10 (avant publication du flavor `playstore`) |
| **SEC-007** | Nettoyer l'historique git de la clé Last.fm au moment de la migration L1 | Lot L1 |
| **SEC-015** | `dependencyCheck` / SBOM en CI | Lot L11 |

---

## 4. Secrets — état

| Secret | Où | Statut | Action |
|---|---|---|---|
| Clé API Last.fm | `local.properties` / env `LASTFM_API_KEY` → `BuildConfig` | **Absent** de l'arbre de travail et du dépôt de livraison | Aucune rotation requise. **Présent dans l'historique** de `bmusic/` (SEC-007) |
| Secret partagé Last.fm | env `LASTFM_SECRET` → `BuildConfig` (flavors `github`, `playstore`) | **Embarqué dans l'APK** | Correctif SEC-006 : ne plus l'embarquer ; rotation du secret Last.fm à faire chez Last.fm |
| Clé de signature release | env `KEYSTORE_FILE` / `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_PASSWORD` | **Jamais commité** (`.gitignore` L22 `keystore.properties`) | Aucune rotation requise |
| Secrets CI | `SIGNING_KEY`, `SIGNING_PROPERTIES`, `LASTFM_API_KEY`, `LASTFM_SECRET`, `TG_*` | Définis côté GitHub, **jamais dans le dépôt** | Aucune valeur reproduite ici |
| Clé YouTube / Data API v3 | — | **Aucune** (`grep -inE 'AIza\|api_key\|apiKey'` → 0 hit sur l'arbre de travail) | Aucune rotation requise |

**Aucune valeur de secret n'est reproduite dans ce registre**, conformément à la règle du dossier.

---

## 5. Risques acceptés (décisions datées)

### D-SEC-1 — NewPipe conservé comme moteur de recherche (SEC-005)

| Champ | Valeur |
|---|---|
| **ID** | SEC-005 (origine F-P1-13) |
| **Justification** | (1) NewPipeExtractor est la bibliothèque de référence du domaine, **GPL-3.0** comme le projet : aucune licence propriétaire introduite. (2) L'alternative « YouTube Data API v3 » exige une **clé API dans le client** — un secret extractible de n'importe quel APK, ce qui viole la règle « aucune clé dans le client » et crée un risque de quota et d'usurpation d'identité. (3) Aucun backend n'existe dans le dossier ; en créer un serait du greenfield hors périmètre. (4) Le contrôle SSRF (SEC-001) borne désormais ce que l'app peut atteindre. |
| **Décideur** | coordinator (décision initiale) **actée par le security-manager** le 2026-11-14 |
| **Date** | 2026-11-14 |
| **Condition de revoyure** | YouTube bloque ou soumet le scraper (captcha, 429, changement de page) **ou** l'un des événements suivants survient : mise en avant d'une API officielle stable (Data API v3 avec backend, oEmbed officiel) ; décision de ne plus distribuer sur le Play Store ; revue sécurité du lot L9. |
| **Mesures de réduction** | User-Agent neutre et fixe (`B Music/1.1 (Android)`), aucun cookie (`CookieJar.NO_COOKIES`), aucun header d'authentification, requêtes bornées par `QueryValidator` (2-200 caractères), historique local borné à 10 et effaçable, aucun secret, aucun compte plateforme collecté. |

### D-SEC-2 — Pas de WebView dans l'aperçu Liens (SEC-V-01)

| Champ | Valeur |
|---|---|
| **Justification** | L'aperçu d'un lien rendu dans un WebView embarqué ouvrirait une surface d'exécution de JS arbitraire, un pont natif, et des scripts tiers. L'alternative retenue — **métadonnées extraites côté client + ouverture externe via `ACTION_VIEW`** — supprime la surface. |
| **Décideur** | coordinator (D2) **confirmé** par le security-manager le 2026-11-14 |
| **Date** | 2026-11-14 |
| **Condition de revoyure** | Toute proposition d'aperçu intégré dans l'application. Si elle est retenue, elle exige : origines autorisées listées, `setJavaScriptEnabled(false)` par défaut, aucun `addJavascriptInterface`, vérification d'origine **et** de source sur tout `postMessage`, politique de navigation restrictive, et revue sécurité dédiée. |

---

## 6. Risques transférés

| Risque | Transféré à | Garantie contractuelle | Statut |
|---|---|---|---|
| Fragilité du scraping YouTube (SEC-005) | Fournisseur (YouTube) | **Aucune** — pas d'API publique sous contrat | Transféré **sans garantie** → à traiter comme un risque assumé (D-SEC-1), pas comme un risque couvert |
| Disponibilité / exactitude des métadonnées des plateformes | Fournisseurs (SoundCloud, Bandcamp, Vimeo, PeerTube) | Aucune | Accepté (mécanismes non officiels, aucun contrat) |
| Défaillance du service d'icônes / polices distantes | Fournisseurs tiers | Aucune | Faible — non applicable aux écrans v1.2 (aucune ressource distante) |

---

## 7. Registre de décision — écarts assumés

| # | Écart par rapport à la bonne pratique | Pourquoi il est assumé | Qui | Date | Revoyure |
|---|---|---|---|---|---|
| 1 | Moteur de recherche non officiel (scraping) au lieu d'une API | §5 D-SEC-1 | coordinator / security-manager | 2026-11-14 | Blocage YouTube ou apparition d'une API officielle |
| 2 | `LASTFM_SECRET` embarqué dans l'APK | Le secret est propre à Last.fm ; le retirer casse la session scrobble. **Correctif identifié** (SEC-006), pas un acceptation | build | — | Lot L10 |
| 3 | `ACCESS_NETWORK_STATE` conservée alors qu'aucun code ne l'utilise | Elle est requise par les bibliothèques réseau (Ktor/OkHttp) pour le flavor réseau ; le retrait par flavor est le bon levier, pas le retrait global | build | 2026-11-14 | Lot L2 |
| 4 | `WRITE_EXTERNAL_STORAGE` (≤ 29) conservée | Utilisée par 1 fichier (éditeur de tags / export) ; `minSdk 26` | build | 2026-11-14 | Report de l'éditeur de tags hors v1.2 |
| 5 | `MODIFY_AUDIO_SETTINGS` et `FOREGROUND_SERVICE_DATA_SYNC` sans usage direct dans le code | Déclarées pour des fonctionnalités tierces (Equalizer/AutoEq, téléchargement en arrière-plan) ; l'usage transitif par bibliothèque n'est pas traçable par grep | build | 2026-11-14 | Lot L2 (vérification `aapt dump permissions` par variant) |
| 6 | `READ_MEDIA_IMAGES` conservée | Pochettes externes (`cover.jpg` / `albumart.jpg`) — 2 fichiers l'utilisent | build | 2026-11-14 | Report de la feature hors v1.2 |
| 7 | Actions CI non pinnées | Le dépôt de livraison n'a pas encore de CI ; le pinning sera appliqué au moment de la migration (L1) et du pipeline (L11) | devops | 2026-11-14 | Lot L11 |
