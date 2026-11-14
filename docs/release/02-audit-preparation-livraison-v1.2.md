# B Music — Audit de préparation à la livraison (v1.2)

> Agent release, phase REL-01 → REL-04. 14 novembre 2026.
> Périmètre : le produit Kotlin/Compose (copie locale `bmusic/`, branche `fix/stabilite-1.2`, HEAD `c488d525`) et le dépôt de livraison `Neoxff/b_music`.
> Chaîne cible : `bmusic/docs/release/01-pipeline.md` (v1.0.0, 8 novembre 2026). Prérequis v1.2 : `bmusic/docs/release/prerequis-v1.2.md`.
> **Aucun build signé, aucun tag, aucun push, aucune publication n'a été effectué.**

---

## 1. REL-01 — Ce qui est réellement en place

### 1.1 Plateforme cible (uniquement ce qui est documenté)

| Élément | Constat | Preuve |
|---|---|---|
| Plateforme | **Android** (Kotlin/Compose), `minSdk 26` / `targetSdk 36` | `aapt2 dump badging` sur l'APK produit |
| Formats produits | **APK** (`B-Music.apk`) et **AAB** (`B-Music.aab`) | `app/build/outputs/apk/fdroid/release/`, `app/build/outputs/bundle/fdroidRelease/` |
| Canaux | GitHub Release + F-Droid + Google Play (flavor `playstore`) | `01-pipeline.md` §5 · `public.properties` (`playstore.DOWNLOAD_URL`) · `metadata/` (métadonnées F-Droid) |
| iOS / web / desktop | **Aucune trace** dans le dépôt | aucune configuration, aucun workflow, aucun artefact |

### 1.2 Chaîne de livraison : cible vs réel

| Élément | Cible (`01-pipeline.md`) | Réel | État |
|---|---|---|---|
| Workflows PR | `lintFdroidRelease`, `testFdroidDebugUnitTest`, `assembleFdroidDebug`, `dependencyCheckAnalyze`, `scripts/check-no-glass.sh`, budget APK < 15 Mo, artefact 7 j | `bmusic/.github/workflows/android.yml` : `check-glass-policy.sh`, `lint`, `testFdroidDebugUnitTest`, `build`, upload artefact | ❌ non conforme (§3.3 le dit) |
| Workflows tag | gate Lean/Security/PM, `clean assembleFdroidRelease bundleFdroidRelease`, `apksigner verify`, `aapt dump badging`, `SHA256SUMS.txt`, `gh release create` | `bmusic/.github/workflows/release.yml` : artefacts `BMusic-*.apk`, **diffusion Telegram**, secrets Last.fm | ❌ non conforme |
| Dépôt de livraison | doit porter le produit + les workflows | `Neoxff/b_music` : **0 workflow**, produit **absent** (145 fichiers, 0 Kotlin) | ❌ **F-P1-01** |
| `CHANGELOG.md` (Keep a Changelog) à la racine | §2 | **absent** de `bmusic/` | ❌ manquant |
| `gitleaks` avant push | §4, obligatoire | ni installé, ni dans la CI | ❌ manquant |
| `dependencyCheck` / SBOM | §3.1 étape 5 | absent de la CI | ❌ SEC-015 |
| `release/v1.2.0/` (apk + SHA256SUMS + mapping) | checklist §7 | absent | ❌ manquant |
| `gh` CLI | checklist §7 (`gh auth status`) | non installé | ❌ manquant |
| Scripts | `check-glass-policy.sh` **et** `check-no-glass.sh` | les 2 existent ; la CI n'exécute que le 1er | ⚠️ le pipeline cite `check-no-glass.sh`, la CI utilise `check-glass-policy.sh` — **deux contrôles différents, un seul est branché** |

### 1.3 Reproductibilité

| Épinglage | Documenté | Constaté | État |
|---|---|---|---|
| Gradle | 9.6.0 | `gradle-wrapper.properties` → `gradle-9.6.0-bin.zip` | ✅ |
| AGP | 9.5.0 | `libs.versions.toml` `agp = "9.5.0"` | ✅ |
| Kotlin | 2.3.21 | `libs.versions.toml` `kotlin = "2.3.21"` | ✅ |
| JDK | 21 | local : `openjdk 21.0.13.0.3` | ✅ |
| `gradle.properties` | commité, sans secret | `configureondemand=true`, `configuration-cache=true`, `caching=true`, `parallel=true` | ✅ (mais voir §2.2) |
| `local.properties` | jamais commité | ignoré (`.gitignore:78`), ne contient que `sdk.dir` | ✅ |

### 1.4 Scripts et métadonnées de distribution

- `bmusic/scripts/check-glass-policy.sh` — contrôle zéro-verre (blanc-liste `GlassSurface`, rayon ≤ 24 dp, motifs `specular|sheen|shine|glow|refraction|glare` interdits). **Exécuté : ✓ OK.**
- `bmusic/scripts/check-no-glass.sh` — second contrôle, **non branché en CI**.
- `bmusic/metadata/` — métadonnées **F-Droid** : `short_description.txt` et `full_description.txt` par locale (24 locales), `changelogs/<versionCode>.txt` (`1030300.txt`, `1040300.txt`, `1100300.txt`). Le nommage confirme la règle de `versionCode` de `01-pipeline.md` §2 (`1_000_000·major + 100_000·minor + 10_000·patch + commits`).
- `bmusic/PERMISSIONS.md` — documentation des permissions, à recaler après SEC-004.
- `bmusic/public.properties` — **suivi par git**, URLs publiques uniquement (`DOWNLOAD_URL`, `DONATION_LINK`, `LYRICALLY_API_URL`) + surcharges `fdroid.` / `playstore.`. Aucun secret. ✅
- `bmusic/mobile/` — **prototype Capacitor encore présent** (2ᵉ application, `package.json` + `LICENSE` propres). Surface de confusion et de supply chain tant que le lot L1 ne l'a pas archivé.

### 1.5 Manques pour une livraison reproductible

| # | Manque | Bloquant ? |
|---|---|---|
| M1 | Le produit n'est pas dans le dépôt de livraison | **oui** |
| M2 | Aucun CI dans le dépôt de livraison | **oui** |
| M3 | `:macrobenchmark` incompatible AGP 9 ⇒ la commande de release documentée échoue | **oui** (§2.2) |
| M4 | Aucune clé de signature release ⇒ l'APK est signé avec la clé de debug | **oui** (§3) |
| M5 | `versionName` / `versionCode` non bumpés en 1.2.0 | **oui** |
| M6 | `CHANGELOG.md`, `release/v1.2.0/`, `SHA256SUMS.txt` absents | oui |
| M7 | `gitleaks` et `dependencyCheck` absents | oui (sécurité) |
| M8 | `gh` CLI absent | oui (publication) |
| M9 | `notes-v1.2.0.md` absent | oui (publié ci-dessous) |
| M10 | `bmusic/mobile/` (prototype) non archivé | non bloquant, mais à faire |

---

## 2. REL-03 — Vérification du build final

### 2.1 Commandes exécutées

Toutes depuis `bmusic/`, avec `--offline` (le cache Gradle de l'environnement est chaud ; la CI, elle, résout en ligne).

| # | Commande | Résultat | Statut |
|---|---|---|---|
| 1 | `./gradlew --offline :app:testFdroidDebugUnitTest` | `BUILD SUCCESSFUL` — **193 tests / 0 échec** | ✅ |
| 2 | `./gradlew --offline :app:lintFdroidDebug` | `BUILD SUCCESSFUL` — 0 erreur | ✅ |
| 3 | `bash scripts/check-glass-policy.sh` | `✓ glass-policy OK` — exit 0 | ✅ |
| 4 | `./gradlew --offline clean assembleFdroidRelease` | `BUILD FAILED` — résolution de plugin | ❌ **reproductible** |
| 5 | `./gradlew --offline --no-configuration-cache :app:assembleFdroidRelease` | `BUILD SUCCESSFUL in 4m 6s` | ✅ |
| 6 | `./gradlew --offline --no-configuration-cache clean assembleFdroidRelease` + correctif `:macrobenchmark` | `BUILD SUCCESSFUL in 10s` | ✅ **correctif validé** |

> La commande 4 est **exactement** celle de `01-pipeline.md` §3.2 (`./gradlew clean assembleFdroidRelease bundleFdroidRelease`). Elle échoue.

### 2.2 Cause racine de l'échec de `clean` — diagnostic exact

`clean` est une tâche **multi-projets** : elle force la configuration de *tous* les projets, y compris `:macrobenchmark`. Or `gradle.properties` fixe `org.gradle.configureondemand=true` : sans `clean`, `:macrobenchmark` n'est jamais configuré, et son défaut reste invisible.

Le diagnostic a été établi par **trois expériences contrôlées**, chacune suivie d'une restauration de l'archive (dépôt laissé intact) :

| Expérience | Modification testée | Résultat observé |
|---|---|---|
| 1 | état initial | `Error resolving plugin [id: 'org.jetbrains.kotlin.android', version: '2.3.21'] > … the plugin is already on the classpath with an unknown version` |
| 2 | `macrobenchmark` : `id("org.jetbrains.kotlin.android")` **sans** version | `⛔ Failed to apply plugin 'org.jetbrains.kotlin.android'. The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.` |
| 3 | `macrobenchmark` : retrait du plugin **+** `targetProjectPath = ":app"` | `A problem occurred configuring project ':macrobenchmark'. > targetProjectPath cannot be null in test project macrobenchmark` |
| 4 | **les deux correctifs ensemble** | **`BUILD SUCCESSFUL in 10s`** ✅ |

**Cause racine** : `macrobenchmark/build.gradle.kts` L3 applique `id("org.jetbrains.kotlin.android") version "2.3.21"`, plugin **obsolète depuis AGP 9.0** ; et le module `com.android.test` exige `targetProjectPath`, absent de son bloc `android { }` (L5-6).

**Correctif à appliquer** (2 lignes, non appliqué — hors périmètre de la phase release) :

```kotlin
// macrobenchmark/build.gradle.kts
plugins {
    id("com.android.test")            // ligne 2 : + suppression de la ligne 3
}                                      // ligne 3 supprimée : id("org.jetbrains.kotlin.android") version "2.3.21"

android {
    namespace = "com.bmusic.app.macrobenchmark"
    targetProjectPath = ":app"         // ligne 6 : à ajouter
    compileSdk = 37
```

**Classification** : **bug de configuration du dépôt** (pas un bug du code applicatif, pas un problème d'environnement, pas une indisponibilité externe).

### 2.3 Contrôles SDK sur l'artefact réellement produit

| Contrôle | Commande | Résultat observé |
|---|---|---|
| badging | `aapt2 dump badging B-Music.apk` | `package: name='com.bmusic.app' versionCode='1' versionName='1.0.0'` · `targetSdkVersion:'36'` · `application-label:'B Music'` |
| signature | `apksigner verify --verbose --print-certs` | `Verifies` · v1 **false** · **v2 true** · v3 **false** · v3.1 **false** · v4 **false** · signataire : `DN: C=US, O=Android, CN=Android Debug` |
| taille | `stat -c%s` | 15 448 298 o = **14,73 Mio** (≤ 15 Mio ✅) / **15,44 MB** (> 15 MB ✗) — *l'unité du seuil n'est pas définie dans le dossier* |
| profil R8 | `app/build/outputs/mapping/fdroidRelease/mapping.txt` | présent — **128 409 693 o (122 Mio)** |
| baseline profile | `app/build/outputs/apk/fdroid/release/baselineProfiles/` | présent |
| Diagnostics | `grep Diagnostics app/src/main/AndroidManifest.xml` | aucune entrée → absent du bundle release ✅ |
| `aapt` / `apkanalyzer` / `zipalign` | `command -v` | **absents du `PATH`** (présents dans le SDK : `build-tools/*/`, `cmdline-tools/latest/bin/`) → à PATHifier pour exécuter les contrôles du pipeline |

### 2.4 Variables requises (noms uniquement, aucune valeur)

| Variable | Où | Nécessaire pour |
|---|---|---|
| `KEYSTORE_FILE` | env | signature release (sinon repli sur la clé de debug) |
| `KEY_ALIAS` | env | idem |
| `KEY_PASSWORD` | env | idem |
| `STORE_PASSWORD` | env | idem |
| `LASTFM_API_KEY` | env ou `local.properties` | flavors `github` / `playstore` uniquement |
| `LASTFM_SECRET` | env | idem (**à ne pas embarquer** — SEC-006) |
| `SIGNING_KEY`, `SIGNING_PROPERTIES` | secrets GitHub | workflow hérité |
| `TG_AAB_STORE_ID`, `TG_API_HASH`, `TG_API_ID`, `TG_BOT_TOKEN`, `TG_CHAT_ID` | secrets GitHub | workflow hérité (**diffusion Telegram à retirer**) |

---

## 3. REL-04 — Configurations, secrets et permissions

### 3.1 Secrets

| Contrôle | Résultat | Preuve |
|---|---|---|
| Secret dans l'arbre de travail | **aucun** | `git ls-files \| grep -iE '\.env\|\.jks\|\.keystore\|local\.properties\|google-services\.json\|\.pem\|\.p12'` → 0 résultat |
| `local.properties` ignoré | ✅ | `.gitignore:78` ; ne contient que `sdk.dir` |
| `keystore.properties` ignoré | ✅ | `.gitignore:22` ; **absent** du disque |
| `public.properties` suivi | ✅ | URLs publiques seules, **aucun secret** |
| **Clé de signature de l'APK produit** | ❌ **clé de debug** | `build.gradle.kts` L127-143 : `KEYSTORE_FILE` absent **et** `keystore.properties` absent ⇒ `signingConfigs.getByName("debug")`. Le keystore de debug est un fichier local auto-généré (`~/.android/debug.keystore`) : **cette clé n'est pas un secret de release**. `apksigner --print-certs` confirme `CN=Android Debug` |
| Clé Last.fm dans l'APK `fdroid` | non applicable | `canUseLastFm = github \|\| playstore` → le flavor `fdroid` n'a pas de champ Last.fm |
| Clé Last.fm dans l'APK `github`/`playstore` | ❌ **oui** | `LASTFM_SECRET` injecté en `BuildConfig` → extractible (SEC-006) |
| Historique git | ❌ clé API Last.fm | commits `e038993f`, `6a7ddd3b` (SEC-007) — **valeur non reproduite ici** |

### 3.2 Séparation production / développement

| Contrôle | Résultat | Preuve |
|---|---|---|
| `applicationId` debug distinct | ✅ | `applicationIdSuffix = ".debug"` (`build.gradle.kts` L152) |
| `versionName` debug distinct | ✅ | `versionNameSuffix = ".debug"` (L153) |
| Minification release | ✅ | `isMinifyEnabled = true`, `isShrinkResources = true` (L146-147) |
| Écran Diagnostics hors release | ✅ | aucune entrée `Diagnostics` dans le manifest fusionné |
| Secrets du flavor `fdroid` | ✅ absents | `canUseLastFm` exclut `fdroid` |

### 3.3 Permissions réellement contenues dans l'APK release

Relevé par `aapt2 dump badging` sur `B-Music.apk` (flavor `fdroid`) — **c'est la preuve au niveau de l'artefact**, pas seulement du manifeste source :

| Permission | Dans l'APK release `fdroid` | Usage dans le code | Verdict |
|---|---|---|---|
| `android.permission.INTERNET` | **oui** | réseau (téléchargeur, Last.fm) | ❌ **violation du contrat « zéro réseau » du flavor `fdroid`** |
| `android.permission.ACCESS_NETWORK_STATE` | **oui** | **0 fichier** du code | ❌ permission inutile **et** non conforme |
| `android.permission.BLUETOOTH` (`maxSdkVersion=30`) | oui | 3 fichiers | ✅ |
| `android.permission.BLUETOOTH_CONNECT` (`neverForLocation`) | oui | 3 fichiers | ✅ |
| `android.permission.POST_NOTIFICATIONS` | oui | 1 fichier | ✅ |
| `android.permission.READ_MEDIA_IMAGES` | oui | 2 fichiers | ⚠️ à revoir |
| `android.permission.READ_MEDIA_AUDIO` | oui | oui | ✅ |
| `android.permission.READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`) | oui | 2 fichiers | ✅ |
| `android.permission.WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=29`) | oui | 1 fichier | ⚠️ à revoir |
| `android.permission.FOREGROUND_SERVICE` | oui | oui | ✅ |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` | oui | oui | ✅ |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | oui | 0 fichier (transitif) | ⚠️ à vérifier |
| `android.permission.MODIFY_AUDIO_SETTINGS` | oui | 0 fichier (transitif) | ⚠️ à vérifier |
| `android.permission.WAKE_LOCK` | oui | oui | ✅ |
| `android.permission.RECEIVE_BOOT_COMPLETED` | **oui** | **non déclarée dans le manifeste de l'app** | ⚠️ **apportée par la fusion d'une bibliothèque** — à identifier et justifier |
| `com.bmusic.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | oui | — | ✅ permission interne |

**Constat** : `SEC-004` est confirmé **au niveau de l'artefact**. Le flavor `fdroid` n'est pas « zéro réseau ».

### 3.4 Journaux

| Contrôle | Résultat | Preuve |
|---|---|---|
| Journalisation de requêtes / URL | ✅ aucune | 3 `Log.w` dans tout le module downloader, aucun ne contient d'URL ni de requête |
| `Log.d` / `Log.v` supprimés en release | ❌ **non** | 15 occurrences ; `proguard-rules.pro` sans `-assumenosideeffects` → lisibles par une app tierce sur appareil rooté (SEC-010) |
| Télémétrie / analytics / crash-reporting réseau | ✅ aucun | 0 dépendance, 0 appel |
| Crash reports locaux | ⚠️ | `customactivityoncrash` installe un handler global ; contenu et durée de vie à vérifier |

### 3.5 Étapes manuelles de configuration restantes

1. Générer la clé de signature release et la stocker **hors du dépôt** (ou en secret GitHub `KEYSTORE_BASE64`).
2. Renseigner `KEYSTORE_FILE`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD` dans l'environnement de build.
3. Bumper `versionName` → `1.2.0` et `versionCode` → valeur strictement supérieure à celle de v1.1.x (règle §2).
4. Renseigner `LASTFM_API_KEY` / `LASTFM_SECRET` **si et seulement si** les flavors `github` / `playstore` sont construits.
5. Installer `gitleaks` et `dependencyCheck`, et brancher `check-no-glass.sh` en plus de `check-glass-policy.sh`.
6. Installer `gh` et configurer le remote (l'upstream `mardous/BoomingMusic` ne doit pas être modifié).
7. Produire `release/v1.2.0/` : `B-Music.apk`, `B-Music.aab`, `SHA256SUMS.txt`, `mapping.txt`.

---

## 4. Synthèse

| Domaine | État |
|---|---|
| Chaîne cible documentée | ✅ claire (`01-pipeline.md`) |
| Chaîne réelle conforme | ❌ non (`§1.2`) — la CI existante est déclarée non conforme et le dépôt livré n'en a aucune |
| Build release | ⚠️ **réussit** (`:app:assembleFdroidRelease`) mais **la commande documentée échoue** (`:macrobenchmark`) |
| Artefact | ⚠️ produit, minifié, 14,73 Mio — mais **signé avec la clé de debug** et en **version 1.0.0** |
| Permissions | ❌ `INTERNET` + `ACCESS_NETWORK_STATE` dans le flavor `fdroid` |
| Secrets dans le dépôt | ✅ aucun ; ❌ mais la **clé de signature de l'artefact est la clé de debug** |
| Reproductibilité | ⚠️ versions épinglées ✅, mais **commande de release cassée** et `gitleaks`/`dependencyCheck` absents |
