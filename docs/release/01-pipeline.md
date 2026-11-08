# B Music — Chaîne de livraison v1.0.0

> Agent Release. Date : 8 novembre 2026.
> Statut : **NO-GO pour publication** — les gates Lean / Security / PM ne sont pas toutes vertes (voir §1). Ce document définit la chaîne cible ; aucun tag, aucune release GitHub, aucun push n'ont été effectués.

## 1. Gates bloquantes (vérifiées le 8 novembre 2026)

| Gate | État constaté | Preuve |
|---|---|---|
| Lean | **GO AVEC RÉSERVES**, pas GO | `docs/lean/04-revue-iteration-1.md` : verdict « GO AVEC RÉSERVES » ; budgets non mesurés sur device (cold-start, jank, mémoire) |
| Security | **NO-GO / incomplet** | `docs/security/01-modele-menaces.md` liste des correctifs critiques/hauts non validés ; `02-audit.md`, `03-signature.md`, `04-validation-finale.md` absents ; `INTERNET`/`ACCESS_NETWORK_STATE` toujours déclarés dans le manifest du fork |
| PM / recette | **Non validée** | `docs/pm/04-acceptation-lancement.md` : checklist §2 entièrement décochée ; matrice S1–S12 non exécutée sur devices A–E |
| GitHub | **Bloqué** | `gh` non installé (`command not found`) ; remote du fork = `https://github.com/mardous/BoomingMusic.git` (upstream, ne pas modifier) ; aucun dépôt utilisateur configuré |
| Artefacts locaux | OK (non signés release) | `B-Music.apk` 14 896 700 o, `B-Music.aab` 24 400 163 o ; `aapt` : `com.bmusic.app`, versionCode 1, versionName 1.0.0, label « B Music », minSdk 26, targetSdk 36 |

**Décision Release : STOP avant build signé / tag / publication.** Les sections suivantes décrivent la chaîne à activer une fois les gates vertes.

## 2. Versionnage

- Version sémantique : `versionName = 1.0.0` (`app/build.gradle.kts`, `currentVersionName`).
- `versionCode` : incrémental automatique. Règle cible : `versionCode = 1_000_000 * major + 100_000 * minor + 10_000 * patch + nombre de commits depuis le dernier tag` (ou, à défaut, compteur CI `GITHUB_RUN_NUMBER` injecté en variable d'environnement). Ne jamais réutiliser un `versionCode` publié.
- Tags Git : `vX.Y.Z` uniquement (ex. `v1.0.0`), tag annoté, jamais déplacé.
- Changelog : format **Keep a Changelog** dans `CHANGELOG.md` à la racine du dépôt Android (`bmusic/CHANGELOG.md`), sections `Added / Changed / Fixed / Removed`, entrée `## [1.0.0] - YYYY-MM-DD` générée depuis les commits Conventional Commits entre tags.

## 3. Pipeline GitHub Actions (cible)

### 3.1 Sur chaque Pull Request
1. `actions/checkout@v4` + `actions/setup-java@v4` (JDK 21, distribution `temurin`, cache Gradle).
2. `./gradlew lintFdroidRelease` (échec si erreur).
3. `./gradlew testFdroidDebugUnitTest` (tests unitaires).
4. `./gradlew assembleFdroidDebug` (build debug).
5. Scan dépendances : `./gradlew dependencyCheckAnalyze` (OWASP) ou `gradle dependency-submission` ; échec si CVE critique/haute non justifiée.
6. Contrôle anti-blur/glass : `scripts/check-no-glass.sh` (grep `RenderEffect`, `BlurMaskFilter`, `Modifier.blur`, `Haze`, `glassmorphism`, `BackdropBlur`) — doit sortir 0.
7. Contrôle budget APK : `B-Music.apk` (arm64) < 15 Mo, sinon échec.
8. Artefact debug uploadé (rétention 7 jours).

### 3.2 Sur tag `vX.Y.Z`
1. Vérification des gates : Lean GO, Security GO, PM recette validée (job `gate` qui échoue sinon).
2. `./gradlew clean assembleFdroidRelease bundleFdroidRelease` avec signature release (voir §4).
3. Contrôles APK : nom exact `B-Music.apk`, `apksigner verify --print-certs`, `aapt dump badging` (package, versionName/versionCode, label « B Music », icône adaptative), taille < 15 Mo, minSdk/targetSdk conformes.
4. Génération `SHA256SUMS.txt` (`sha256sum B-Music.apk B-Music.aab > SHA256SUMS.txt`).
5. Publication GitHub Release : `gh release create vX.Y.Z B-Music.apk B-Music.aab SHA256SUMS.txt --title "B Music X.Y.Z" --notes-file docs/release/notes-vX.Y.Z.md`.
6. Conservation de `mapping.txt` (R8) en artefact de release pour débogage (non publié publiquement si stack traces sensibles).

### 3.3 Workflows existants à remplacer
Les workflows hérités de l'upstream (`bmusic/.github/workflows/android.yml`, `release.yml`) ne sont **pas conformes** : ils ciblent `BMusic-*.apk`, envoient vers Telegram, utilisent des secrets Last.fm et ne produisent ni `B-Music.apk` ni `B-Music.aab` ni checksums. Ils doivent être remplacés par les workflows ci-dessus avant toute publication.

## 4. Secrets de signature

- Stockés uniquement dans **GitHub Secrets** : `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`.
- Jamais dans le dépôt : ni keystore, ni `keystore.properties`, ni `local.properties`, ni token.
- Injection CI : décodage base64 vers `$RUNNER_TEMP`, variables d'environnement `KEYSTORE_FILE`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD` (déjà supportées par `app/build.gradle.kts`).
- Scan `gitleaks` obligatoire avant tout push.

## 5. Canaux et promotion

| Canal | Contenu | Critères de promotion |
|---|---|---|
| Interne | build debug/release non signé distribué en direct | CI verte |
| Bêta fermée | `B-Music.apk` signé, GitHub Release en pré-release | gates Lean/Security/PM vertes + recette S1–S12 sur devices A–E |
| Production | GitHub Release stable + Google Play (AAB, Play App Signing) | crash-free ≥ 99 %, 0 bug bloquant, SHA-256 vérifié |

## 6. Reproductibilité

- Gradle **9.6.0** via wrapper commité (`gradle-wrapper.properties`), AGP **9.5.0**, Kotlin **2.3.21**, JDK **21** épinglés.
- `gradle.properties` commité (sans secrets) ; `local.properties` jamais commité.
- Builds CI depuis les sources du tag uniquement ; aucun artefact local commité.

## 7. Contrôles avant tag (checklist)

- [ ] Lean : GO (pas « avec réserves »), budgets mesurés sur device B (cold-start < 600 ms, jank < 1 %, RSS < 200 Mo)
- [ ] Security : `04-validation-finale.md` GO, `INTERNET`/`ACCESS_NETWORK_STATE` retirés en fdroid, `largeHeap` supprimé, composants exportés durcis
- [ ] PM : matrice S1–S12 verte sur A–E, 0 bug bloquant
- [ ] `gh auth status` OK, remote utilisateur configuré (upstream renommé, fork créé)
- [ ] `./gradlew clean assembleFdroidRelease bundleFdroidRelease` vert avec signature release
- [ ] `apksigner verify` OK, `aapt dump badging` conforme, `B-Music.apk` < 15 Mo
- [ ] Tests d'installation propre (petit écran, grand écran, tablette) + mise à jour par-dessus version précédente
- [ ] Test fumée : premier lancement, permission médias, lecture, mini-player → plein écran, notification, rotation, sombre/clair, arrêt du service
- [ ] `SHA256SUMS.txt` généré, `mapping.txt` conservé, fichiers placés dans `release/v1.0.0/`
