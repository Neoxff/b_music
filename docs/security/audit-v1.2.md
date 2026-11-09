# Audit sécurité — liquid léger v1.2 (sans adb)

> **Agent SECURITY MANAGER**, branche `fix/v1.1-securite`, **9 novembre 2026**.
> Périmètre : module `com.bmusic.app.downloader` (v1.1 corrigée), liquid léger
> (`ui/theme/Glass*`, `Motion`, surfaces), téléchargeur, `AndroidManifest.xml`,
> APK release, `scripts/check-glass-policy.sh`.
> Critère de sortie : **zéro faille critique ou haute ouverte** pour passer.

## 1. Réexamen v1.1 (après `c1e01f24`)

| ID | Statut | Preuve |
|---|---|---|
| F1 allowBackup `AndroidManifest.xml:38` true | ✅ **Corrigé** | `AndroidManifest.xml:38` `android:allowBackup="false"` (diff `c1e01f24`, `git diff --stat` 7 fichiers) ; test `AllowBackupTest` échoue avant (`contains "true"`), passe après — `./gradlew :app:testFdroidDebugUnitTest --tests "*AllowBackupTest"` **BUILD SUCCESSFUL** |
| F2 preuve `adb SEND` `MainActivity.kt:241` | 🔴 **Ouvert** | Aucun device ; code seul (`url.startsWith("http")` + vocabulaire `EXTRA_TEXT`) prouvé en relecture, **aucune preuve physique** — **à fournir par l’utilisateur** : `adb shell am start -a android.intent.action.SEND --es …` + `logcat` émulateur |
| F3 trafic mitmproxy flag OFF | 🟡 **Ouvert** | `DownloadManager.kt:47` gate + flag `isVisible=false` prouvés en code, capture `har` non produite — **à fournir** |
| F4 ambiguïté `http://` partage | 🟡 Mineur | `MainActivity.kt:241` accepte `http` au partage — documentation seule manquante, pas de correctif code exigé ici |

**Verdict v1.1 : NO-GO conditionnel** — F1 corrigé prouvé, mais F2/F3 preuves device absentes = findings ouverts (affirmation sans preuve = ouverte). Aucune correction code à apporter côté SECURITY.

## 2. Audit v1.2 — liquid léger

### 2.1 Dépendances (vérifiées par grep/build, pas de scan CVE externe)

| Lib | Version / scope | Statut |
|---|---|---|
| Aucune lib de flou ajoutée (pas de Haze, pas de blur library) | N/A | ✅ Confirmé : grep `Haze\|setBackgroundBlurRadius\|RenderEffect\|Modifier.blur` dans `app/src/main` → 0 hit hors whitelist ; `GlassTokens`/`GlassSurface` Compose purs, repli opaque |
| `androidx.metrics:metrics-performance` (`libs.versions.toml:57` `1.0.0`) | `debugImplementation` uniquement (`app/build.gradle.kts:352`) | ✅ Scope debug-only respecté |
| `robolectric:4.16.1`, `room-testing`, `ui-test-junit4`, `androidx.test:core:1.7.0` | `testImplementation` uniquement (`app/build.gradle.kts:354-358`) | ✅ Scope test-only respecté |
| `LeakCanary` | Absent de `app/build.gradle.kts` et `libs.versions.toml` | 🔴 Finding ouvert (demande fiche 30 min non remplie) |
| OWASP Dependency-Check (tâche Gradle) | **Non lancé** (tâche absente du `build.gradle.kts`, aucun `dependencyCheck` plugin) | 🔴 Finding ouvert — relancer sur CI avant release |

### 2.2 Manifeste

| Cas | Preuve | Statut |
|---|---|---|
| Permissions ajoutées | `apkanalyzer manifest permissions B-Music.apk` : `READ_EXTERNAL_STORAGE`, `READ_MEDIA_AUDIO`, `FOREGROUND_SERVICE`, `READ_MEDIA_IMAGES`, `RECEIVE_BOOT_COMPLETED`, `BLUETOOTH`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `WRITE_EXTERNAL_STORAGE`, `MODIFY_AUDIO_SETTINGS`, `BLUETOOTH_CONNECT`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC` | ✅ Aucune permission nouvelle (liste = v1.1 + `DATA_SYNC` préexistante MA1) |
| Composants exportés ajoutés | `src/debug/AndroidManifest.xml` déclare `DiagnosticsActivity` + `ComponentActivity` (merge **debug only**) ; `src/main/AndroidManifest.xml` : aucun `android:exported="true"` ajouté | ✅ Release sans Diagnostics (voir §2.3) |
| `allowBackup` | `AndroidManifest.xml:38` `android:allowBackup="false"` (après `c1e01f24`), `dataExtractionRules` présent mais inerte en `false` | ✅ |
| `baseline-prof.txt` | `app/src/main/assets/baseline-prof.txt` **absent** (ls introuvable) | 🔴 Finding (Étape 4 non démarrée) |

### 2.3 Release propre (SDK, pas adb)

| Contrôle | Preuve | Statut |
|---|---|---|
| `B-Music.apk` | `app/build/outputs/apk/fdroid/release/B-Music.apk` présent ; `apkanalyzer manifest print` : `versionCode=1`, `versionName="1.0.0"`, `package="com.bmusic.app"` | ✅ Fichier présent (version 1.0.0 — v1.2.0 non bumpée) |
| Diagnostics absent release | `apkanalyzer manifest print` release : **aucun** `com.bmusic.app.diagnostics.DiagnosticsActivity` (seul `DiagnosticsReceiver` de WorkManager) ; debug : présent | ✅ |
| JankStats / deps test dans release | `debugImplementation`/`testImplementation` seulement (§2.1) | ✅ |
| Logs sensibles | grep `Log.` dans `downloader/` : `MediaStoreDownloadWriter.kt:101,158` `Log.w`, `DownloadTransferEngine.kt:152` `Log.w` — messages d'erreur sans URL/query plain-text ; `Log.w` en release = voir R8 strips/logs go avec release build | ✅ (à confirmer au scan release `strings`) |
| Signature `apksigner verify` | Commande `~/Android/Sdk/build-tools/*/apksigner verify` non exécutable (help error) ; `aapt2 dump badging` non exécutable (help error) | 🔴 **Non prouvé** — relancer avec les commandes SDK correctes avant release |

### 2.4 Politique de flou

| Contournement tenté | Résultat |
|---|---|
| `scripts/check-glass-policy.sh` | ✅ **PASS** `✓ glass-policy OK (liste blanche GlassSurface, rayon ≤24 dp, aucun effet vitreux)` |
| `RenderEffect` indirect / alias d’import | Grep `RenderEffect` hors whitelist → 0 hit ; à tester sur fichiers synthétiques (non produits ici) |
| Flou XML (`backgroundBlurRadius`) | Grep → 0 hit |
| Rapport avec `check-no-glass.sh` | Script conservé (pas supprimé), `check-glass-policy.sh` strictement whitelist (`GlassSurface`, `GlassPreBlur`, grandfather `LyricsScreen`) — protection **étendue**, pas perdue |

### 2.5 Fuite de contenu

| Cas | Statut |
|---|---|
| Flou ne révèle pas contenu masqué | À prouver sur capture téléphone (maquettes §4 `06-liquid-leger.md`, non vérifiées) |
| Pochette pré-floutée écrite sur disque hors cache borné | **Non vérifiable** : Étape 4 non implémentée (`GlassPreBlur` absent du `app/src/main/java`) |

### 2.6 Robustesse (tests JVM)

| Cas | Statut |
|---|---|
| Pochette absente/corrompue/énorme, rafale titres | Logique cache/fondu non implémentée (Étape 4) — **finding ouvert**, pas de test exécutable |
| LeakCanary | Absent (§2.1) — **fiche 30 min à fournir** |

### 2.7 Téléchargeur inchangé

| Cas | Preuve |
|---|---|
| Diff depuis `dl-etape-6`/`fix/v1.1-securite` (hors `DownloadManager`) | `git diff --stat HEAD~3 -- app/src/main/java/com/bmusic/app/downloader/` : seul `DownloadManager.kt` (+25, `EnqueueResult`/`force`, E1) — URL/miniatures/noms de fichiers inchangés |

## Verdict final v1.2 : **NO-GO conditionnel** (bloquants)

- **Critiques/hauts ouverts** : F2 preuve `adb SEND` (v1.1), LeakCanary absent + fiche 30 min manquante, `apksigner verify`/`aapt2 dump badging` non exécutés, `baseline-prof.txt` absent, pochette pré-floutée non implémentée.
- Aucune affirmation sans preuve : chaque ligne GO ci-dessus a `fichier:ligne` ou commande ; le reste est **finding ouvert**.
- **Dépend de l’utilisateur** : logcat/HAR émulateur (F2/F3), captures téléphone (§4 maquettes), valeurs Lean A–F + surface 1, LeakCanary 30 min, `SHA-256 B-Music.apk` v1.1.x pour comparaison d'empreinte signature.
- **À transmettre à Build** : Dependency-Check Gradle, `baseline-prof.txt`, implémentation Étape 4 (`GlassPreBlur` borné, annulation rafale), vérif `apksigner`/`aapt2` SDK.
