# B Music — Décision Go / No-Go (v1.2)

> Agent release, REL-07. 14 novembre 2026.
> Décision rendue sur l'état du dépôt au commit produit `c488d525` et livraison `ec13cc6`.
> **Aucune publication, aucun tag, aucun push n'a été effectué.**

---

## 1. VERDICT

# 🔴 NO-GO

**La livraison est bloquée.** Neuf bloqueurs, dont deux critically bloquants au sens Android : l'artefact produit est signé avec la **clé de debug** et le variant « hors-ligne » embarque la permission `INTERNET`.

**Nature de la décision** : il ne s'agit pas d'un « GO avec réserves ». Un GO avec réserves suppose que les réserves sont documentables et non bloquantes ; ici, deux conditions sont des **interdits techniques de distribution** (clé de signature non distributible, permission réseau dans un variant contractualisé « zéro réseau »).

**Ce qui autoriserait une livraison limitée** : une livraison **interne de debug** (APK non signé ou signé debug, installée manuellement, sur un seul appareil de test) reste possible **après** correction du module `:macrobenchmark` et après le bump de version. Elle ne vaut **pas** livraison du produit v1.2 : voir §6.

---

## 2. Les sept conditions de livraison, une par une

| # | Condition | Verdict | Preuve |
|---|---|---|---|
| 1 | **Tests** | ✅ **conforme** | `./gradlew --offline :app:testFdroidDebugUnitTest` → `BUILD SUCCESSFUL`, **193 tests / 0 échec** (11 sécurité + 28 fournisseurs + 20 liens + 13 états + 8 navigation + le reste de la base) |
| 2 | **Analyse / lint / politique verre** | ✅ **conforme** | `:app:lintFdroidDebug` → `BUILD SUCCESSFUL`, 0 erreur · `check-glass-policy.sh` → `✓ glass-policy OK`, exit 0 |
| 3 | **Build** | ⚠️ **partiel** | `:app:assembleFdroidRelease` → `BUILD SUCCESSFUL in 4m 6s` **MAIS** la commande documentée `./gradlew clean assembleFdroidRelease` (`01-pipeline.md` §3.2) → **`BUILD FAILED`**. Cause racine identifiée et correctif validé : `:macrobenchmark` applique un plugin Kotlin obsolète en AGP 9 et n'a pas de `targetProjectPath` (`02-audit…` §2.2) |
| 4 | **Signature** | 🔴 **NON CONFORME — BLOQUANT** | `apksigner verify --print-certs` → `Verifies` (v2 seul) mais signataire **`DN: C=US, O=Android, CN=Android Debug`**. Cause : `build.gradle.kts` L127-143 — ni `KEYSTORE_FILE` ni `keystore.properties` ⇒ repli sur `signingConfigs.getByName("debug")`. **Une clé de debug n'est pas une clé de distribution** : elle n'est pas secrète, elle est locale, et elle ne permet pas les mises à jour signées de façon fiable |
| 5 | **Version** | 🔴 **NON CONFORME — BLOQUANT** | `aapt2 dump badging` → `versionCode='1' versionName='1.0.0'`. L'exigence `prerequis-v1.2.md` (versionName 1.2.0 + versionCode > v1.1.x) n'est **pas** satisfaite. Publier « 1.2.0 » avec un `versionCode 1` déjà publié interdisait toute mise à jour |
| 6 | **Permissions minimales / contrat du variant** | 🔴 **NON CONFORME — BLOQUANT** | `aapt2 dump badging` sur l'APK **release du variant `fdroid`** : `INTERNET` **présente**, `ACCESS_NETWORK_STATE` **présente** (0 usage dans le code). Le contrat v1.0 « zéro réseau » du variant est violé **au niveau de l'artefact** (SEC-004). `docs/release/01-pipeline.md` L11 porte un gate **NO-GO** de sécurité |
| 7 | **Fonctionnalités demandées** | 🔴 **NON CONFORME** | La **recherche YouTube ne fonctionne pas** : l'override `search` n'existe que sur `feat/dl-etape-2` (F-P1-02) ; sur HEAD l'engine lève `Unsupported` ⇒ l'écran affiche « service indisponible ». Les onglets Explorer et Liens existent, **mais la fonction principale d'Explorer est absente**. Par ailleurs **aucun lecteur intégré** n'existe (ADR-003 non rendu) : toutes les plateformes s'ouvrent à l'extérieur |

---

## 3. Definition of Done

| Contrat | État | Preuve |
|---|---|---|
| Checklist PM de lancement | 🔴 **0 / 22 cochée** | `docs/pm/04-acceptation-lancement.md` : 0 case `[x]`, 22 cases `[ ]` |
| Recette fonctionnelle Explorer/Liens | 🔴 **0 / 39 scénarios exécutés** | `docs/pm/08-recette-fonctionnelle-explorer-liens.md` §1 : « 0 exécuté / 0 validé » ; §2 : causes B1→B6 |
| Critères d'acceptation v1.2 (DO-1→DO-12) | 🔴 **non validés** | `docs/pm/07-criteres-acceptation-explorer-liens.md` — aucun des 12 critères n'est couvert par une preuve de recette |
| Gate sécurité | 🔴 **NO-GO** | `docs/release/01-pipeline.md` L11 ; `security/risk-register.md` §3 vague 1 : SEC-004, SEC-016 |
| Gate Lean (budgets de performance) | 🔴 **non mesuré** | `prerequis-v1.2.md` : « GO conditionnel … non mesuré » ; aucun export Diagnostics |
| Gate Plan | ⚠️ **partiel** | la phase build a produit un plan de diff et des ADR, mais ADR-002 et ADR-003 sont **non rendus** (BL-005) |

## 4. Non-régression et qualité

| Contrôle | Verdict | Preuve |
|---|---|---|
| Non-régression fonctionnelle | ✅ **conforme** | 193 tests / 0 échec ; les 6 tests de navigation couvrent 4 variantes de ressources × 2 états du drapeau Téléchargeur |
| Non-régression de l'artefact | ⚠️ **non vérifiée** | aucun artefact de référence antérieur à comparer ; `docs/release/01-pipeline.md` §1 mentionnait des APK non signés de 14 896 700 o, l'artefact actuel fait 15 448 298 o (+3,7 %) |
| **Qualité visuelle** | 🔴 **NON VÉRIFIÉE** | **aucune baseline de capture** (F-DS-07) et **aucun appareil** : la non-régression visuelle et le rendu réel des deux nouveaux écrans ne sont **pas prouvables**. La checklist `01-pipeline.md` §7 exige « tests d'installation propre (petit écran, grand écran, tablette) » — **non faits** |
| Accessibilité | ⚠️ **partiellement vérifiée** | cibles tactiles 48 dp et contrastes calculés (16,52 / 17,17 …) ; **annonces TalkBack et navigation clavier non vérifiées** (38 cas `NON EXÉCUTÉ` dans la matrice QA) |
| Alimentation de la chaîne | 🔴 **non conforme** | 9 actions CI en tag mutable (SEC-008), pas de scan de dépendances (SEC-015), pas de `gitleaks`, le dépôt de livraison n'a **aucun** workflow |

---

## 5. Les 9 bloqueurs

| # | Bloqueur | Sévérité | Action requise | Propriétaire |
|---|---|---|---|---|
| **B1** | **APK signé avec la clé de debug** | **Critique** | Générer une clé de release, la stocker hors du dépôt, injecter `KEYSTORE_FILE` / `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_PASSWORD` | devops |
| **B2** | **`INTERNET` + `ACCESS_NETWORK_STATE` dans le variant `fdroid`** | **Critique** | Créer `app/src/fdroid/AndroidManifest.xml` retirant les deux permissions ; preuve `aapt2 dump badging` par variant | build (L2) |
| **B3** | **Le produit n'est pas dans le dépôt de livraison** | **Critique** | Migrer l'arbre Kotlin vers `Neoxff/b_music`, archiver le prototype `mobile/` | build (L1) |
| **B4** | **La commande de release documentée échoue** | Élevée | Retirer le plugin Kotlin obsolète de `macrobenchmark` et ajouter `targetProjectPath = ":app"` (correctif **validé** : `BUILD SUCCESSFUL in 10s`) | build |
| **B5** | **Version non bumpée** (`1.0.0` / code 1) | Élevée | `versionName = 1.2.0`, `versionCode` selon la règle §2 de `01-pipeline.md` | build |
| **B6** | **La recherche YouTube ne fonctionne pas** | Élevée | Rendre ADR-002 puis porter l'override `search` (avec encodage de la requête + `UrlValidator`) | coordinator → build (L3) |
| **B7** | **Docs de promesse non nuancées** | Élevée | « Aucune autorisation réseau n'est utilisée » est faux pour les variants `github` / `playstore` | pm (L10) |
| **B8** | **Aucune CI dans le dépôt de livraison**, workflows existants non conformes | Élevée | Écrire les workflows de `01-pipeline.md` §3, pinner les actions par SHA, ajouter `gitleaks` et `dependencyCheck` | devops |
| **B9** | **Aucun contrôle manuel sur appareil** ; pas de baseline de capture | Élevée | Exécuter la matrice QA §3 et §5 sur au moins petit écran, grand écran et tablette ; produire les baselines de capture | qa |

**Bloquants secondaires, non bloquants pour une livraison mais à traiter** : SEC-006 (secret Last.fm embarqué), SEC-007 (clé dans l'historique git), SEC-008 (actions CI non épinglées), SEC-010 (journaux de débogage en release), SEC-012 (miniatures bloquées pour 4 plateformes), SEC-014 (`CoverProvider` exporté), SEC-015 (pas de scan de CVE), BL-004 (compensation Bibliothèque des catégories), BL-006 (icône Téléchargeur), ADR-003 (lecteur intégré non arbitré).

---

## 6. Options de livraison

### Option 1 — Publication publique de v1.2.0

**Refusée.** Elle exigerait de lever B1 à B9, dont B2 (violation du contrat de permission) et B7 (information inexacte à l'utilisateur). La levée de B1 seule ne suffit pas : Android refuse une signature de debug pour une distribution stable, et le contrat v1.0 du variant « zéro réseau » est explicitement violé par l'artefact.

### Option 2 — Livraison interne de debug (usage personnel)

**Possible, sous conditions**, et c'est la voie réaliste à court terme :

| Condition | État |
|---|---|
| Corriger `:macrobenchmark` (B4) | correctif **validé**, à appliquer |
| Bumper la version (B5) | requis pour ne pas confondre avec une installation antérieure |
| Signer avec la clé de debug — assumé et documenté | acceptable pour un usage personnel, **interdit** pour une distribution |
| Installer manuellement (`INSTALL_FAILED_UPDATE_INCOMPATIBLE` possible si une version signée différemment est déjà installée → désinstaller d'abord) | à faire |
| **Reconnaître que la recherche YouTube ne fonctionne pas** (B6) | l'onglet Explorer affiche « service indisponible » ; **il ne faut pas annoncer la fonction** |

**Ce que cette option ne corrige pas** : B2 (le variant `fdroid` reste non conforme au contrat « zéro réseau ») et B7 (la documentation reste inexacte). Elle est donc adaptée à un **test personnel**, pas à une communication de version.

### Option 3 — Livraison de v1.1.x existante

**À vérifier séparément** : le dossier mentionne un `versionCode` v1.1.x antérieur (`prerequis-v1.2.md`), mais **aucun artefact v1.1.x n'a été produit ni contrôlé dans cette phase**. Rien ne permet d'affirmer qu'une version antérieure est publiable ; le gate sécurité de `01-pipeline.md` L11 était déjà **NO-GO** le 8 novembre 2026.

---

## 7. Ce qu'il faut pour passer à GO

**Séquence minimale, dans cet ordre** :

1. **B4** — corriger `:macrobenchmark` → la commande de release documentée fonctionne.
2. **B1** — clé de signature release + injection CI.
3. **B5** — bump de version.
4. **B2** — manifest par flavor + preuve `aapt2 dump badging` par variant.
5. **B3** — migration du produit vers le dépôt de livraison (avec le correctif sécurité et le commit du schéma Room `10.json`).
6. **B8** — CI conforme dans le dépôt de livraison, actions épinglées, `gitleaks` + `dependencyCheck`.
7. **B6 / ADR-002** — rendre l'arbitrage du moteur de recherche, puis porter l'override.
8. **B9** — exécuter la matrice QA manuelle sur au moins 3 profils d'appareil, produire les baselines de capture.
9. **B7** — nuancer les docs de promesse et la fiche store.
10. Rejouer les 9 contrôles de `01-pipeline.md` §7, puis **demander un nouveau GO**.

**Critères minimaux de levée du NO-GO** : B1, B2, B3, B5, B8, B9 levés **et** preuve `aapt2 dump badging` par variant conforme **et** `apksigner verify` montrant une identité de release **et** signature de version 1.2.0 correcte. B6 et B7 peuvent rester ouverts pour une **livraison limitée explicitement documentée** (recherche non fonctionnelle annoncée comme telle) — mais **pas** pour une communication de version qui présente Explorer comme opérationnel.

---

## 8. Risque résiduel accepté pour cette décision

| Risque | Décideur | Date | Revoyure |
|---|---|---|---|
| Aucun risque de sécurité n'est accepté par cette décision : la publication est refusée | agent release | 2026-11-14 | à chaque levée de bloqueur |
| La non-publication est elle-même un risque : le produit reste non livré | coordinator | — | après B1→B5 |
| ADR-002 (moteur de recherche) et ADR-003 (lecteur intégré) non rendus | coordinator | — | condition de B6 |

---

## 9. Ce que la phase release a réellement produit

| Livrable | Contenu |
|---|---|
| `docs/release/02-audit-preparation-livraison-v1.2.md` | REL-01, REL-03, REL-04 : chaîne réelle, commandes exécutées, contrôles sur l'artefact, secrets et permissions |
| `docs/release/03-matrice-qa-v1.2.md` | REL-02 : 83 cas sur 19 domaines, avec statut et preuve |
| `docs/release/notes-v1.2.0.md` | REL-05 : notes de version fondées uniquement sur le code présent |
| `docs/release/06-plan-retour-arriere-v1.2.md` | REL-06 : 3 scénarios, 7 commandes sensibles avec effet exact et validation demandée, contrainte de schéma Room |
| `docs/release/07-decision-go-nogo-v1.2.md` | REL-07 : ce document |
| `security/risk-register.md` §9 | 6 findings de la phase release (REL-001 → REL-006) |

**Aucun artefact n'a été signé, tagué, poussé ni publié.** Le dépôt produit est en `fix/stabilite-1.2` @ `c488d525` (local) ; le dépôt de livraison est en `main` @ `ec13cc6` (**devant 5** sur `origin/main`, non poussé).
