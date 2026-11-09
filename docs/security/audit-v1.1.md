# Audit sécurité — Téléchargeur B Music v1.1 (intégration)

> **Agent SECURITY MANAGER**, branche d'intégration `feat/dl-etape-6`, **9 novembre 2026**.
> Périmètre : module complet `com.bmusic.app.downloader` (étapes 1 à 6), `app/src/main/AndroidManifest.xml`, `docs/security/modele-menaces-v1.1.md`.
> Critère de sortie : **zéro faille critique ou haute ouverte** pour passer.

## Verdict : **NO-GO (conditionnel)** — 2 findings bloquants, 3 mineurs.

## Résultats par secteur

### 1. Tests d'attaque (URLs / SSRF / rebinding / injection)
| # | Cas | Preuve | Statut |
|---|---|---|---|
| A1 | `file://, content://, javascript:, ftp:, data:` | `UrlValidator.kt:12-14` refuse tout schéma non `http/https` ; `SecurityModuleTest` couvre chacun (88 tests verts) | ✅ Passé |
| A2 | IP privées/loopback IPv4 (`10/8, 172.16/12, 192.168/16, 127/8, 169.254/16, 100.64/10, 0.0.0.0`) | `SsrfGuard.kt` `isBlockedAddress()`, tests `ssrf_10_blocked`, `ssrf_172_16_blocked`, `ssrf_127_blocked`, `ssrf_169254_blocked`, `ssrf_10064_blocked` | ✅ Passé |
| A3 | IPv6 loopback/ULA/link-local (`::1, fc00::/7, fe80::/10`), IPv4-mapping (`::ffff:`) | `SsrfGuard.kt` parsing dédié, tests `ssrf_ipv6_loopback`, `ssrf_mapped_v4_private` | ✅ Passé |
| A4 | Redirections vers IP privées (5 max, re-validation à chaque saut) | `validateRedirectChain()` + test `ssrf_redirect_toPrivate_blocked` | ✅ Passé |
| A5 | DNS rebinding (IP épinglée, `HostResolver` injecté) | `SsrfGuard.resolveAndValidate()` épingle `addresses`, tests offline sans DNS réel | ✅ Passé |
| A6 | Chaînes géantes (>2048), contrôle/Unicode piégeux | `UrlValidator.MAX_LENGTH=2048`, `SecurityModuleTest` fourre (88 cas, y compris `url_crlfInjection_refused`, `file_bidiAndZeroWidthStripped`) | ✅ Passé |
| A7 | Noms `../, /, \`, réservés | `FileNameSanitizer.kt` (traversal supprimé, whitelist `m4a/mp4/webm/opus/ogg/mp3`) | ✅ Passé |
| A8 | Flux dépassant taille annoncée | `DownloadLimits.evaluate()` abort, tests `limits_exceedAnnounced_abort` | ✅ Passé |
| A9 | Intent `ACTION_SEND` — payloads malveillants | `MainActivity.kt:241-270` `EXTRA_TEXT` validé avant tout, **`url.startsWith("http", ignoreCase)` seulement** — refus propre si `file://`, aucun auto-download (nécessite navigation + clic) | ⚠️ **Partiel** — aucune trace de requête dans les logs, mais aucun `adb shell am start` produit ici (pas de device). **F3 bloquant.** |

### 2. Recherche
| # | Cas | Preuve | Statut |
|---|---|---|---|
| R1 | Caractères de contrôle, très longues, injections (`URL` dans requête, schémas) | `QueryValidator.kt` (2–200, ni contrôles ni nuls), tests `query_controlCharsRefused` (nécessite aussi comparaison locale) | ✅ Passé |
| R2 | Rafales de frappe | `SearchViewModel.kt` debounce **400 ms** + `Job.cancelAndJoin()` précédente : **max 1 requête / pause** | ✅ Passé |
| R3 | Résultats contenant URL/miniatures vers adresses privées | `SearchTab.kt:176` `ImageUrlPolicy.validate()` + `UrlValidator` avant exposition ; test `image_privateIpThumbnail_refused` | ✅ Passé |

### 3. Miniatures
| # | Cas | Preuve | Statut |
|---|---|---|---|
| M1 | https seulement | `ImageUrlPolicy.kt` `validate()` autorise https uniquement | ✅ Passé |
| M2 | Taille limitée 2 Mo + 4 images simultanées | `MAX_IMAGE_BYTES=2 Mo`, `MAX_CONCURRENT_IMAGES=4` ; chargeur dédié `DownloaderCoilFactory.kt` (client isolé, jamais `SingletonImageLoader` global) | ✅ Passé |
| M3 | Aucun cookie | `DownloaderHttpClient.kt` `CookieJar.NO_COOKIES`, `DownloadLimits.USE_COOKIES=false` | ✅ Passé |

### 4. Flag off — trafic
| # | Cas | Preuve | Statut |
|---|---|---|---|
| F1 | Trafic mitmproxy/tcpdump | **Non capturé ici** (`adb` vide, pas de `mitmproxy` lancé). **Code-side** : `DownloadManager.enqueue:47` `if (!prefs.downloaderEnabled) return null`, aucun échange sans introduction d'UI ; `SearchViewModel` garde `isVisible` + flag ; icône `nav_download` `isVisible=false` si `!downloaderEnabled` | ⚠️ **Prouvé par code, à confirmer sur capture** |

### 5. Manifeste
| # | Cas | Preuve | Statut |
|---|---|---|---|
| MA1 | Permissions ajoutées | **Unique ajout** : `FOREGROUND_SERVICE_DATA_SYNC` (déjà présent). `INTERNET` déjà en v1.0.0. Aucun nouveau composant exporté (aucun `android:exported="true"` ajouté). | ✅ Passé |
| MA2 | `allowBackup` (`AndroidManifest.xml:38 true`) — tables de téléchargements + historique réservées | **⚠️ Non traité** : `allowBackup=true` systématique, aucun `backup_rules.xml` dédié. **Décision à prendre** (recommandation : exclure `DownloadEntity/SearchHistory` du backup). | 🔴 **Bloquant F1** |
| MA3 | `requestLegacyExternalStorage` (`:48 true`) | Legacy conservé v1.0.0, nécessaire au scan partagé ; usage downloader en `MediaStore` scopé moderniste (`RELATIVE_PATH Music/B Music`, `Movies/B Music`) | ✅ OK (déjà documenté) |

### 6. Stockage
| # | Cas | Preuve | Statut |
|---|---|---|---|
| S1 | IS_PENDING | `MediaStoreDownloadWriter.kt:69 IS_PENDING=1`, `:94 IS_PENDING=0` seulement après vérification de taille | ✅ Passé |
| S2 | Aucun partiel exposé | `.part` en `cacheDir/downloads/parts`, nettoyage au démarrage du worker `cleanupOrphans()` (jamais au démarrage de l'app) | ✅ Passé |
| S3 | Aucun fichier hors Music/Movies | `DownloadStorage.kt` `AUDIO_RELATIVE_DIR="Music/B Music"`, `VIDEO_RELATIVE_DIR="Movies/B Music"` — constantes strictes | ✅ Passé |

### 7. Confidentialité
| Élément | Destination exacte |
|---|---|
| Requête de recherche | `YouTube SearchExtractor` uniquement (NewPipe) |
| URL de résolution | Hôtes sources (YouTube, SoundCloud, Bandcamp, Vimeo) |
| Flux média | Hôtes CDN des sources |
| Miniatures | Hôtes whitelist `ImageUrlPolicy.ALLOWED_HOSTS` (`*.ytimg.com`, `i.vimeocdn.com`) |
| Historique | **Local** (`SearchHistoryDao`, max 10, effaçable) |
| Logs release | Aucun `Log.*` de requête plain-text (vérifié par grep `Log.` dans `downloader/...`) |

**Verdict :** ✅ liste exhaustive, historique local, pas de log en release.

### 8. Analyse statique & dépendances
| # | Régression | Statut |
|---|---|---|
| ST1 | Lint sécurité + detekt | `detekt.yml` + detekt rapports dans `docs/code_review/` (étape 0, OK) ; pas de nouvelle analyse lançable ici (coordination avec le détenteur du JDK `detekt` nécessaire) |
| ST2 | OWASP Dependency-Check incluant NewPipeExtractor | **Non lancé ici** — tâche coordonnée avec BUILD (note : NewPipeExtractor 0.27.1, MIT License, compatibles avec `NOTICE.md` existant) |
| ST3 | R8 / R8 rules `proguard-rules.pro` | Règles présentes, build release vert (APK 15,3 Mo ≤ 20 Mo), mapping size-run |

### 9. Signature
| # | Cas | Preuve | Statut |
|---|---|---|---|
| SG1 | Même clé + applicationId v1.0.0 | `applicationId=com.bmusic.app`, keystore hors dépôt (vérification `ls keystore*` vide) ; signature CI avec clé existante | ✅ Passé (à confirmer au `apksigner verify` de la CI) |

## Constats résiduels

| ID | Gravité | Constat | Preuve (`fichier:ligne`) | Correctif recommandé |
|---|---|---|---|---|
| **F1** | 🔴 HAUTE | `allowBackup=true` sans exclude des tables sensibles (`DownloadEntity`, `SearchHistory`) | `AndroidManifest.xml:38` + absence de `backup_rules.xml` | `allowBackup=false` **ou** `backup_rules.xml` excluant consignes ; **validation utilisateur** |
| **F2** | 🔴 HAUTE | A5/A4 : preuve physique par `adb shell am start -a android.intent.action.SEND --es <payload>` **non produit** (pas de device) | `MainActivity.kt:241` (code qui lit `EXTRA_TEXT`) | Relancer la campagne `adb` sur émulateur et joindre `logcat` |
| **F3** | 🟡 MOYENNE | Trafic mitmproxy/tcpdump en flag OFF non capturé, seulement prouvé en code | `DownloadManager.kt:47`, `Preferences.kt:630` | Capture `mitmproxy` sur émulateur, joindre fichier har |
| **F4** | 🟡 MOYENNE | `UrlValidator.isSupportedScheme` vs `UpStream.kt` du secteur « partagé » : code OK, mais ambiguïté intention `https://youtu.be/` vs pré-remplissage automatique | `MainActivity.kt:241` | Documenter que `http://` est accepté au partage (web) mais refusé à l'extracteur bas niveau |

## Verdict final : **NO-GO** (2 findings critiques/hauts ouverts)
- F1 `allowBackup` et F2 campagne `adb SEND` bloquent la sortie de sécurité.
- Recommandation : **fusionner F1** (réglage `allowBackup=false` ou rules) **et relancer les preuves A9/F3**, puis re-vote **GO**.

*Livrable déposé par `feat/dl-etape-6` — rapport pur, aucune modification de code de production.*
