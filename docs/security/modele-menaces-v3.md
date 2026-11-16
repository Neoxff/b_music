# Modèle de menaces v3 — B Music (module « Télécharger »)

> Audit du 16/11/2026, branche `feat/dl-lien-diagnostic`.
> Règle **sans adb** : toute preuve est un test JVM/Robolectric, un script, ou un
> outil du SDK (`aapt2`, `apksigner`, `apkanalyzer`). **Aucune mesure n'est
> claimée sur un appareil.**

---

## 1. Surface réseau : ce qui a été retiré

| Surface retirée | Effet | Preuve |
|---|---|---|
| **Recherche YouTube** (requêtes réseau + listes de miniatures distantes) | Supprimée : plus aucune requête de recherche, plus aucune liste de miniatures distantes | `MediaExtractorEngine` n'expose plus que `resolve(url)` ; `SearchTab.kt`/`SearchViewModel.kt` (downloader) supprimés ; `scripts/check-koin-bindings.py` exit 0 ; APK release : 0 occurrence de `robolectric`/`junit`/`org.junit` |

| Surface **résiduelle** | Origine | Statut |
|---|---|---|
| Moteur d'extraction (1 moteur : NewPipeExtractor 0.27.0) | `resolve(url)` + GET sur la page du site | contrôlé |
| Page de lien | GET de la page du site (via `NewPipeDownloader`) | contrôlé |
| Miniature d'aperçu | GET de l'image de couverture, par Coil | contrôlé par `ImageUrlPolicy` |
| Flux audio/vidéo | GET du flux, après validation de l'URL | contrôlé |
| Intent de partage (entrant) | `ACTION_SEND` → pré-remplissage du champ Lien | contrôlé |
| Presse-papiers (sortant) | bouton « Coller » → lecture explicite | contrôlé |
| Historique local | Room, **aucune** requête à l'affichage | contrôlé |
| Journal d'étapes | fichier privé + mémoire | contrôlé |

**Total : 2 surfaces réseau sortantes** (résolution d'extraction, téléchargement du flux)
+ 1 GET de miniature. Aucune recherche, aucune liste distante.

---

## 2. STRIDE par composant

| Composant | Menace (STRIDE) | Scénario | Mesure en place | Preuve | Risque résiduel |
|---|---|---|---|---|---|
| **Entrée : champ / presse-papiers / partage** | *Tampering*, *Spoofing* | URL injectée (`javascript:`, `file://`, `intent://`), identifiants dans l'URL, CRLF/NUL, URL > 2 048 car., redirection vers `127.0.0.1` | `UrlValidator` (schéma http/https seuls, 2 048 car., pas d'userinfo, pas de contrôles, pas d'espace) **appliqué à l'entrée** (ViewModel) **et** au moteur (défense en profondeur) ; fragment **retiré**, paramètres de suivi **nettoyés** | `UrlValidatorAcceptanceTest` (65 URL légitimes acceptées), `UrlValidatorRejectionTest` (45 attaques de syntaxe + 20 attaques IP/DNS + 15 domaines hors registre), `EntryPointsSecurityTest` (5 entrées forgées) | **Lien collé = HTML/JS non exécuté** (jamais interprété) ; un lien vers un site connu mais non extractible est refusé par le registre |
| **Client HTTP dédié** | *Information disclosure*, *Elevation of privilege* (SSRF) | Redirection vers une IP privée ; rebinding DNS | Redirections **pilotées par l'app** (`followRedirects(false)`), **chaque saut re-validé** (`RedirectGuard` + `SsrfGuard`, 5 sauts max) ; **DNS épinglé** (`GuardedDns` : adresses validées, conservées, jamais re-résolues) ; aucun cookie ; UA neutre fixe ; timeouts 10/30/30/120 s | `SsrfRedirectTest` (9 tests), `UrlValidatorRejectionTest` (20 IP dont `::ffff:` et formes obfusquées) | Une plateforme qui **refuse** l'UA neutre peut demander un CAPTCHA → message honnête « moteur à mettre à jour » |
| **Résolution DNS** | *Denial of service*, *Information disclosure* | Résolution sur le thread principal ; DNS piégé | Résolution **uniquement** dans `Dispatchers.IO` (moteur et worker) ; jamais sur le thread UI | `DownloadTransferEngine`/`NewPipeExtractorEngine` : `withContext(Dispatchers.IO)` | Une résolution lente bloque le thread IO du worker (pas l'UI) |
| **Registre des sites** | *Spoofing* (hameçonnage) | `youtube.com.evil.com`, domaine inconnu | Le moteur n'est appelé que pour un domaine **du registre** ; correspondance par **suffixe** (`host == domaine` ou `host.endsWith("." + domaine)`) → pas d'homographe par suffixe | `SiteRegistryTest` (13 sites), `UrlValidatorRejectionTest` (« le registre ne reconnaît pas un domaine piege ») | Un **homographe Unicode** (`уoutube.com` en cyrillique) serait accepté par le validateur mais **refusé** par le registre (domaine ASCII absent) |
| **Moteur NewPipeExtractor 0.27.0** | *Elevation of privilege*, *Information disclosure* | Moteur qui enverrait une requête vers un service tiers ; binaire natif non vérifié | Version **épinglée** (0.27.0), licence **GPL-3.0-or-later** déclarée dans `NOTICE.md` + `LICENSE.txt` ; dépendance unique `nanojson` ; **0 classe de télémétrie/analytics** dans le jar ; **aucun cookie** (`CookieJar.NO_COOKIES`) ; aucun compte, aucun en-tête d'authentification ; pas de binaire natif (Java pur) | `javap` sur `ServiceList.class` (4 services : youtube, soundcloud, bandcamp, peertube) ; `grep` du jar : 0 classe telemetry ; `apksigner` : 1 seul signer | Le moteur **suit la logique du site** : une MAJ de plateforme peut faire échouer l'extraction → message typé, jamais un crash. **CVE : audit hors-ligne impossible** (voir §5) |
| **Écriture fichier (MediaStore)** | *Tampering*, *Elevation of privilege* | Nom de fichier piloté par le réseau ; traversée de chemin ; fichier partiel exposé | `FileNameSanitizer` (traversée, caractères de contrôle, Unicode invisible/bidi, noms réservés Windows, extension **liste blanche**, troncature 100 car.) ; `RELATIVE_PATH` = `Music/B Music` ou `Movies/B Music` ; `IS_PENDING=1` pendant l'écriture, `IS_PENDING=0` **seulement** après vérification de taille | `SecurityModuleTest` (66 tests, dont FileNameSanitizer) ; `MediaStoreDownloadWriter` : `IS_PENDING` 1→0 contrôlé par taille | Un `.part` orphelin peut subsister si le processus est tué → **nettoyé au démarrage du worker** (`cleanupOrphanParts`) |
| **Limite de taille** | *Denial of service* (épuisement disque) | Flux infini ou plus gros que l'annoncé | `DownloadLimits` : arrêt si le flux dépasse la taille annoncée (`IOException("limit: …")`) → échec typé | `DownloadTransferEngine.kt:117` | Pas de plafond global en octets : le contrôle est « taille annoncée » → à compléter si un site annonce faux |
| **Historique local (Room)** | *Information disclosure* | Liens et titres **en clair** dans la base ; fuite par sauvegarde | `DownloadEntity` : `sourceUrl` **normalisée** (fragment retiré, tracking nettoyé) ; historique effaçable (par ligne et en bloc) ; **aucune requête réseau** à l'affichage ; miniatures dans le cache borné de l'app | `EntryPointsSecurityTest` (« l URL stockee dans l historique est normalisee ») ; `backup_descriptor.xml` / `data_extraction_rules.xml` incluent `music_database.db` | **RISQUE ASSUMÉ (voir §3)** : la base est incluse dans la sauvegarde cloud |
| **Journal d'étapes** | *Information disclosure* | Lien complet ou donnée personnelle dans le journal | Le lien est **réduit au domaine + hash SHA-256 tronqué** ; `DownloadError.redact()` réduit toute URL à son domaine, **y compris dans la pile de crash** ; rapport partagé **uniquement par action explicite** (« Partager ») | `DownloadTraceTest` (7), `CrashReporterTest` (5) | Le rapport contient les **messages techniques** des bibliothèques (noms de classes, tailles) : pas de donnée personnelle, mais une information sur l'implémentation |
| **Intent de partage (entrant)** | *Tampering*, *Elevation of privilege* | `EXTRA_TEXT` forgé, démarrage automatique d'un téléchargement | Le partage ne fait que **pré-remplir** le champ (aucune résolution, aucun téléchargement) ; la validation a lieu à l'analyse ; flag off → intent **ignoré** | `EntryPointsSecurityTest` (« un partage ne declenche ni resolution ni telechargement », 5 entrées forgées) | L'onglet Lien s'ouvre sur un partage (navigation) — comportement voulu, pas de téléchargement |
| **Feature flag** | *Denial of service*, *Elevation of privilege* | Activité réseau malgré le flag désactivé | Flag off → **aucun** appel moteur, **aucune** entité mise en file, **aucun** worker ; le worker **contrôle le flag à chaque exécution** (défense en profondeur : un téléchargement en file est annulé proprement si le flag est coupé entre-temps) | `EntryPointsSecurityTest` (2 tests flag off) | Le flag est à `true` par défaut depuis la v1.4.3 (décision produit) : l'onglet est visible |
| **Worker / service au premier plan** | *Elevation of privilege*, *Denial of service* | Service FGS déclaré sans type → crash Android 14+ ; démarrage FGS en arrière-plan | `android:foregroundServiceType="dataSync"` déclaré pour le service WorkManager ; `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` ; `POST_NOTIFICATIONS` **demandée à l'exécution** (le téléchargement continue si refusée) | `ManifestForegroundServiceTest` (rouge→vert) ; APK release : `foregroundServiceType=0x00000001` | Android 15 : quota FGS `dataSync` (6 h/jour) → le worker long est interrompu |
| **Notifications** | *Information disclosure* | Notifications révélant une donnée personnelle | Le contenu de la notification de téléchargement est le **titre** du média (déjà connu de l'utilisateur) ; `POST_NOTIFICATIONS` est facultatif | `PermissionsExt.kt:40` | Le titre apparaît dans le volet de notifications si l'utilisateur l'accorde |

---

## 3. Décisions assumées (avec qui, quand, condition de revoyure)

### D-1 — L'historique du téléchargeur part dans la sauvegarde cloud *(risque résiduel accepté)*

- **Constat** : `backup_descriptor.xml` et `xml-v31/data_extraction_rules.xml` incluent
  `domain="database" path="music_database.db"` → `DownloadEntity` (`sourceUrl`, `title`,
  `author`, `platformId`, `streamUrl`) **part dans la sauvegarde** de l'utilisateur.
- **Risque** : *Information disclosure* — l'historique de navigation (sites consultés) est
  une donnée personnelle ; elle est copiée dans le compte de sauvegarde de l'utilisateur.
- **Décision** : **accepté pour la v1.4.x**, reporté au-delà.
- **Motif** : (1) l'application est **personnelle et jamais publiée** ; la sauvegarde est le
  **compte de l'utilisateur lui-même** ; (2) l'exclusion exige de **scinder la base**
  (base `downloads.db` dédiée + migration Room), donc un risque de régression supérieur
  au bénéfice à ce stade ; (3) mesure compensatoire **déjà appliquée** : l'URL stockée est
  **normalisée** → l'historique ne contient ni identifiant de campagne (`utm_*`, `fbclid`,
  `igshid`…) ni fragment de partage.
- **Qui décide** : l'utilisateur (arbitrage SECURITY MANAGER proposé, date 16/11/2026).
- **Condition de revoyure** : dès que l'application est partagée ou publiée, **ou** si la
  sauvegarde cloud est activée sur un appareil partagé.

### D-2 — Second moteur d'extraction : **non accepté**

Aucun second moteur n'est intégré : les sites TikTok, Instagram, Facebook, X, Snapchat,
Vimeo, Dailymotion, Reddit et Twitch sont en `NON_PRIS_EN_CHARGE` et l'app affiche un
message **honnête** (« Ce site n'est pas encore pris en charge ») au lieu d'une erreur
générique. Conséquence assumée : ces sites ne sont pas extractibles tant que la décision
n'est pas prise.

### D-3 — Signature de release : clé debug *(F-003, obstacle externe)*

Aucun `keystore.properties` ni variable d'environnement n'est disponible : l'APK est signé
avec la clé debug locale. L'installation par-dessus une version signée avec une autre clé
échoue (« App not installed »). À traiter par l'utilisateur (keystore de release).

---

## 4. Mesures appliquées dans cet audit (avec la preuve rouge → vert)

| Mesure | Fichier:ligne | Preuve |
|---|---|---|
| Fragment **retiré** (et non refusé) + paramètres de suivi **nettoyés** | `security/UrlValidator.kt:65`, `:99-130` | `UrlValidatorAcceptanceTest` : **rouge** (2 URL légitimes refusées, tracking conservés) → **vert** (65 acceptées) |
| Redirections **re-validées par saut** (max 5) | `manager/DownloaderHttpClient.kt:57-95`, `security/RedirectGuard.kt` | `SsrfRedirectTest` (9 tests) |
| DNS **épinglé** (anti-rebinding) | `manager/DownloaderHttpClient.kt:97-117` | `GuardedDns` : adresses validées puis conservées |
| Validation à **l'entrée** (champ / presse-papiers / partage / historique) | `ui/screen/downloader/DownloadViewModel.kt:96-118`, `extractor/NewPipeExtractorEngine.kt:65-75` | `EntryPointsSecurityTest` (5 entrées forgées → 0 appel moteur) |
| **Flag off** : aucun appel, et le worker annule proprement | `worker/DownloadWorker.kt:72-83` | `EntryPointsSecurityTest` (2 tests) |
| Aucun `catch` vide | `transfer/DownloadTransferEngine.kt:53-58`, `storage/MediaStoreDownloadWriter.kt:103-107,127-131`, `worker/DownloadWorker.kt:146-152` | `grep "catch (_:"` → plus aucun `catch (_:)` muet dans le module |

---

## 5. Limites explicites de cet audit

| Limite | Conséquence | Couverture de remplacement |
|---|---|---|
| **OWASP Dependency-Check non disponible hors-ligne** (rien dans le cache Gradle, pas de base NVD locale) | Pas de scan CVE automatisé | Audit manuel équivalent : **versions épinglées** dans `gradle/libs.versions.toml` (moteur 0.27.0, OkHttp 5.5.0, Ktor 3.6.0, Koin 4.2.2, Room 2.8.5, Media3 1.11.1, Coil 3.6.3, WorkManager 2.10.5, nanojson transitif unique) ; licence et absence de télémétrie vérifiées sur le jar ; **à exécuter avec réseau** avant toute diffusion publique |
| Pas de test sur appareil (règle sans adb) | Comportement réseau réel non observé | Le journal d'étapes exporté (Réglages → Journal → Partager) est le substitut : il donne l'étape exacte, le code d'erreur et la durée |
| `mockwebserver` absent du cache | Pas de test de redirection bout en bout (HTTP réel) | `SsrfRedirectTest` valide la **logique** de redirection hors-ligne (résolveur injecté) ; le câblage OkHttp est relu et testé par le compileur |
| Pas de base NVD locale | Les CVE **non intersécables** ne sont pas énumérables ici | Les versions sont épinglées et datées ; à rejouer avec réseau |

---

## 6. Ce qu'il reste à vérifier sur l'appareil (fiche)

| Action | Valeur attendue |
|---|---|
| Lancer l'app, onglet Lien, coller un lien | Un message par site (extrait / « non pris en charge » / erreur typée), jamais un crash |
| Réglages → Journal → Partager | Un rapport texte par tentative, **sans lien complet** |
| Couper la notifications (Android 13+) puis télécharger | Le téléchargement **continue**, sans notification |
| Télécharger un audio court public en release | Le fichier apparaît dans `Music/B Music`, absent de `Movies/B Music`, puis dans l'historique |
| Partager un lien vers l'app | L'onglet Lien s'ouvre **pré-rempli**, **aucun** téléchargement lancé |

---

## 7. Verdict

**GO AVEC RÉSERVES** — les réserves portent sur l'environnement, pas sur le code :

1. **F-003** — signature debug (obstacle externe, décision utilisateur requise) ;
2. **D-1** — historique inclus dans la sauvegarde cloud (risque résiduel accepté, reporté) ;
3. **CVE** — scan OWASP Dependency-Check à exécuter **avec réseau** avant toute diffusion ;
4. **Second moteur** — non accepté (9 sites honnêtement refusés) ;
5. Aucune mesure device claimée : les contrôles ci-dessus sont des preuves **hors-ligne**.
