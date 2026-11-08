# B Music — Modèle de menaces (Security Manager)

> Agent Security Manager B Music. Date : 6 novembre 2026.
> Périmètre : fork `bmusic/` (BoomingMusic, GPL-3.0 modifié), flavor `fdroid` (réseau désactivé par défaut), cible v1.0.
> Ce document est la source de vérité pour les risques ; **chaque ligne reçoit un correctif priorisé** (transmis à l'agent Build).

## 1. Actifs à protéger

| # | Actif | Sensibilité | Comment il vit |
|---|---|---|---|
| A1 | **Bibliothèque locale de l'utilisateur** (fichiers audio, 5 000 titres estimés) | Haute (données privées) | MediaStore/Scoped Storage local ; aucun envoi réseau |
| A2 | **Métadonnées** (titres, pochettes, compteurs de lecture, playlists, favoris, file) | Haute | Room `music_database.db` + SharedPreferences (préférences, playlists) |
| A3 | **Clé de signature release** (keystore B Music) | **Critique** | Fichier `~/.keystore/bmusic-release.jks` (jamais commité) — voir `03-signature.md` |
| A4 | **Intégrité de l'APK/AAB** (téléchargement, Distribution) | Haute | Signature v2/v3 + SHA-256 publiés |
| A5 | **Vie privée** (usage écoute, goûts musicaux) | Haute | Sur appareil uniquement ; aucune donnée sortante |
| A6 | **Code source GPL-3.0** | Moyenne | Fork publié ; aucune dépendance propriétaire tolérée |

## 2. Menaces — méthode STRIDE

### Méthode
Chaque menace est classée selon les 6 catégories STRIDE, rattachée à un actif, avec **probabilité × impact** (Élevée/Moyenne/Faible), et un correctif priorisé pour Build.

### Matrice STRIDE (v1.0, flavor fdroid)

| # | Menace | STRIDE | Actifs | P × I | Détail / preuve | Correctif | Priorité |
|---|---|---|---|---|---|---|---|
| T1 | **APK modifié / repackagé** (fuite du canal de distri, resignature par un tiers) | Spoofing | A4 | F × É | Signature release RSA 4096 + empreinte SHA-256 publiée (`03-signature.md`) atténue le risque de substitution | Build : **signer release, jamais commiter le keystore, publier les empreintes** | **Critique** |
| T2 | **Composants `exported=true` abusés** (lecteur audio contenu injecté, backup restauré) | Elevation of privilege / Tampering | A1, A2 | M × É | Voir §3 : `AudioPreviewActivity`, `BackupActivity`, `CoverProvider`, `WidgetSettingsActivity`, `PlaybackService`, `MediaButtonReceiver` | Build : réviser chaque composant exporté (voir §3) ; **composants sans intent-filter → `exported=false`** explicite | **Haute** |
| T2 | Composants exportés abusés : pièces manquantes | Elevation of privilege / Tampering | A2 | M × É | `SongTagEditorActivity` etc. ont `exported` implicite → statuer explicite | Build : ajouter `android:exported="false"` sur tous les composants internes | **Haute** |
| T3 | **Intents malveillants** (ACTION_VIEW audio hostile, PICK OPENABLE agressif, MEDIA_PLAY_FROM_SEARCH) | Tampering / Information disclosure | A1 | M × M | `MainActivity` avec `vnd.android.cursor.dir/*`, `OPEN_DOCUMENT_TREE` ; `AudioPreviewActivity` avec URI content/file+browsable sans vérification de l'extension réelle | Build : **validation systématique des données entrantes** (mime + schéma URI), refus propre et journal, pas de `finish()` masquants | **Haute** |
| T4 | **Fichiers audio piégés** (parsers tag/editor/libav native) | Denial of service / Elevation of privilege | A1, A2 | M × É | TagLib (bibliothèque native fuzzeable) appelé par l'éditeur; ffmpeg decoder `jellyfin:ffmpeg` chaîné ; Jugend citations ci-dessus (§5) | Build : **catch toutes les exceptions native → toast, skip** ; validation taille < 20 Mo ; fichier non fiable = skip gracieux (< 1 s) | **Haute** |
| T5 | **Fuite de données** (envoi réseau déguisé, analytics, backup cloud) | Information disclosure | A2, A5 | F × É | `INTERNET` + `ACCESS_NETWORK_STATE` déclarées (manifest) alors que **v1 est 100 % offline en fdroid** | Build : **retirer `INTERNET` + `ACCESS_NETWORK_STATE` en fdroid** (manifest merger override par flavor) | **Critique** |
| T6 | **Dépendances compromises** (CWE vertigo) | Elevation of privilege | — | M × É | Voir `02-audit.md` (inventaire + CVE à traiter) | Build : `dependencyCheck` en CI, lockfile stabilisé, licence OSS only | **Haute** |
| T7 | **Logs sensibles** (chemins fichiers, métadonnées audio, erreurs parsers) | Information disclosure | A1, A2 | M × M | `Log.d` présents (ex. `MetadataWriter`, tag editor) — débugging repose sur logcat lisible par d'autres apps sur rooted | Build : **R8 supprime les `Log.d`/`Log.v`** en release (`-assumenosideeffects`), log via `Log` wrapper no-op en release | **Haute** |
| T8 | **Sauvegarde automatique et Cloud** (backup cloud Google Drive exploité sur mobile volé) | Information disclosure | A2 | M × M | `allowBackup=true` + `dataExtractionRules/backup_descriptor.xml` : inclus `music_database.db` (playcounts) ; listes exclues OK | Build : `allowBackup=true` + `dataExtractionRules` durcis (inclure uniquement la DB via des règles explicites ; **exclure prefs privées et pochettes**), `android:backupRules` par défaut `true`→ vérifier `backup_descriptor` couvre tout ; flag OK par défaut sur appareil perdu | **Haute** |
| T9 | **Intégrité des métadonnées** (pollution tag, fermeture de session altérée par deeplinking) | Repudiation / Elevation of privilege | A2 | F × M | Isolated `intent-filter` OPENABLE PICK bénoints ; média whitelist via received intent validation à faire (§3) | Build : `FLAG_SECURE` pour les écrans avec métadonnées privées ? non — discrétion standard ; plus validation deeplink/URI DATA (T3) | **Moyenne** |
| T10 | **Délivrance d'app via APK/AAB non vérifié** | Information disclosure (supply supply chain) | A4 | F × É | Publication release sans fingerprint connu → remplacement de l'empreinte cert | Build : republier le fingerprint dans `03-signature.md` + checksums + CI Builds depuis sources | **Critique (process)** |

## 3. Revue du AndroidManifest.xml

### 3.1 Permissions — justification une par une

| Permission | Version | Justification | Verdict |
|---|---|---|---|
| `BLUETOOTH` (≤ 30) | — | Lecture Bluetooth auto / casque | **Garder** (léger, legacy pour ≤ 30) |
| `BLUETOOTH_CONNECT` (31+, neverForLocation) | 31+ | Sortie audio BT / boutons média | **Garder** |
| `POST_NOTIFICATIONS` (33+) | 33+ | Notification média | **Garder** |
| `READ_MEDIA_IMAGES` (33+) | 33+ | **Cover art externes** (`cover.jpg`/`albumart.jpg`) — évitable : les pochettes embarquées sont lisibles via `READ_MEDIA_AUDIO` (MetaRetriever) | **À TRANCHER** : si la feature cover externe est reportée sonra v1.0, **retirer aussi cette permission** (sinon garder avec licence justifiée) |
| `READ_MEDIA_AUDIO` (33+) | 33+ | Accès bibliothèque audio Scoped Storage | **Garder** |
| `READ_EXTERNAL_STORAGE` (≤ 32) | ≤ 32 | Pont historique pré-33 | **Garder** avec justification version |
| `WRITE_EXTERNAL_STORAGE` (≤ 29) | ≤ 29 | Editeur de tags + backup export legacy | **À TRANCHER** : si éditeur/export retiré de v1.0 → **retirer** la permission (fichier écrit via SAF à la place) |
| `FOREGROUND_SERVICE` | — | Transport playback (min tout) | **Garder** |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (34+) | 34+ | TypeFGS `mediaPlayback` (Google Play requirement) | **Garder** |
| `MODIFY_AUDIO_SETTINGS` | — | Focus audio & gestion AudioManager (voluमें) — utilisé par Equalizer/AutoEq | **À RETIRER si EQ désactivé** (EQ activé seulement en github flavor) → suppression conditionnelle (exploiter le manifest overlayfdroid `tools:remove`) |
| `ACCESS_NETWORK_STATE` | — | État réseau pour le côté réseau (ktor/actifs) | **RETIRER en fdroid** (v1.0 100 % offline) — voir T5 |
| `INTERNET` | — | Appels Last.fm, paroles en ligne, cover externe via network, MAJ intégrées (updater) | **RETIRER en fdroid** (idem T5) |
| `WAKE_LOCK` | — | Keep-alive pendant la lecture | **Garder** |

**Note** : `largeHeap=true` est à supprimer (consommation mémoire, anxiété ANR) — probable avec audit Lean < 200 Mo (déjà noté).

### 3.2 Composants exportés

| Composant | exported | Verdict |
|---|---|---|
| `MainActivity` | true (launcher/media/search/PICK) | OK (intents utilisateur; validation inbound à durcir — T3) |
| `AudioPreviewActivity` | true VIEW audio/* + browsable content/file | **Risqué** : activité de prévisualisation de fichiers externes ; valider URI + type réel ; pas de notification |
| `BackupActivity` | true VIEW fichiers content/* (octet-stream/zip, motifs .bmgbak) | **Risqué** : entrée de fichier est un vecteur haut risque → valider l'extension, ne jamais écraser les données existantes sans confirmation (pattern zip-slip) |
| `SongTagEditorActivity` / `AlbumTagEditorActivity` / `ArtistTagEditorActivity` | non défini (false par défaut pré-31, mais minSdk 26 implicite true ??? pre-Sdk 31 components without intent-filter content defaults false ✓) | **Action** : `android:exported="false"` explicite (recommandé) |
| `OnboardActivity`, `ErrorActivity` | absent (0 filtre) | OK — incohérent mais sans intent-filter → false. Ajouter explicite pour clarté |
| `PlaybackService` (MediaSession/MediaLibrary/Browse) | true | OK (standard Media3) |
| `MediaButtonReceiver` | true MEDIA_BUTTON | OK (standard) |
| `WidgetSettingsActivity` | true APPWIDGET_CONFIGURE ✓ | OK (standard) |
| WidgetReceivers | false | OK |
| `FileProvider` | false | OK — bases = files-path/ephemeral dirs uniquement ✓ |
| `CoverProvider` (Coil) | **true**, grantUriPermissions=true | **RISQUE MOYEN** : n'importe quelle app peut lire des covers via `content://${applicationId}.cover/...` — restreindre ou libellé signature permission |
| `AppLocalesMetadataHolderService` | false/disabled ✓ | OK |

### 3.3 Divers manifest

| Attribut | État actuel | Critique |
|---|---|---|
| `allowBackup` | `true` avec `backup_descriptor` **inclusif** (`music_database.db`) ; `sharedpref` custom_artist_images/widget_config **exclus** | **À DURCIR** : préférence sans backup ou whitelist plus stricte ; `dataExtractionRules.xml` couvre `@xml/backup_descriptor.xml` avec `allowBackup=true` — max risk si mobile volé → les playcounts leak |
| `dataExtractionRules` / backup_rules | `backup_descriptor.xml` présent, ref à `@xml/backup_descriptor` | Vérifier `data_extraction_rules.xml` + `backup_rules.xml` (backup-device-to-device, cloud) — à harmoniser sur les mêmes exclusions (ne pas inclure DB entière en cloud) |
| `largeHeap="true"` | PRÉSENT (couplé à Memory budget audit) | **Retirer** |
| `requestLegacyExternalStorage="true"` | PRÉSENT | Retirer (targetSdk ≥ 30 ignore; compat myth only) |
| `usesCleartextTraffic` | ABSENT → default `false` (targetSdk ≥ 28) ✓ | OK (à déclarer explicitement pour audit — recommandé) |
| `networkSecurityConfig` | ABSENT — OK car pas de réseau v1.0 en fdroid ; une fois `INTERNET` retiré, aucun traçage HTTPEx nécessaire |
| `FileProvider` grandeur | paths: files-path `logs/` , external-cache `exports/`+`backups/` | Le partage crash logs / exports EQ est local scoped ; ne pas élargir. `factory grantUriPermissions` →  permission tutoriaux OK |
| WebView | Recherche `grep -ri 'WebView\|loadUrl' app/src/main` : **aucun WebView** dans le code Kotlin (AboutLibraries uniquement via url intents externes — n'ouvre pas de WebView embarqué) ✓ | OK |

## 4. Correctifs priorisés pour Build (résumé)

**Critique**
- C1 Retirer `INTERNET` / `ACCESS_NETWORK_STATE` en `fdroid` (manifest merger override) — T5
- C2 Keystore/orts de signature (voir `03-signature.md`) — T1/T10

**Haute**
- H1 Restreindre `CoverProvider` (`exported=false` ou signature permission) — T2
- H2 Expliciter `exported=false` sur tag editors + Onboard/Error — T2
- H3 Durcir `allowBackup` + backup_rules (`false` ou whitelist stricte) — T8
- H4 Supprimer `largeHeap`/`requestLegacyExternalStorage` — budget + hygiène
- H5 Validation inbound URI/contenu (`AudioPreviewActivity`, `BackupActivity`, `MainActivity` intents OPENABLE/PICK) — T3
- H6 TagLib/generator d'événements audio: catch squelettes d'exception > toast+skip — T4

**Moyenne**
- M1 Supprimer `WRITE_EXTERNAL_STORAGE` (≤ 29) + `READ_MEDIA_IMAGES` (33+) si éditeur de tags/covers externes reportés
- M2 `usesCleartextTraffic="false"` explicite
- M3 `FileProvider`: restreindre les roots à des dossiers précis (retirer `logs` export file-path si toujours nécessaire)
- M4 Permissions formulaires Google Play retravaillées avec le Play Console

**Basse**
- B1 `android:enableOnBackInvokedCallback` documenté
- B2 Disclaimer opt-in HTTP pour Lyrics si réseau activé (github flavor seulement)

*Les preuves et gravité détaillées sont dans `02-audit.md` ; les correctifs Build sont validés dans `04-validation-finale.md` (GO/NO-GO).*
