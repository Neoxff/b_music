# B Music — Baseline de référence (v1.2)

> Agent build B Music, phase BUILD-01, 14 novembre 2026.
> **Baseline = l'état de départ mesuré, contre lequel chaque lot vérifie sa non-régression.**
> Toute valeur ci-dessous est prouvée par `fichier:ligne` ou par la sortie d'une commande. Aucune valeur estimée.

---

## 1. Identité du produit à l'instant de la baseline

| Élément | Valeur | Preuve |
|---|---|---|
| Dépôt produit (non suivi) | `bmusic/` | copie locale, absente de `Neoxff/b_music` (F-P1-01) |
| Branche | `fix/stabilite-1.2` | `git -C bmusic rev-parse --abbrev-ref HEAD` |
| HEAD produit | `41839ee8` | `fix(security): ferme le SSRF du chemin URL collée` |
| HEAD amont | `2377fe02` | `fix(lint): WrongConstant gradientType RECTANGLE` |
| Dépôt de livraison | `Neoxff/b_music`, `main` | `5145a19` (devant 3 sur `origin/main`, **non poussé**) |
| Licence | GPL-3.0 modifié | `bmusic/LICENSE.txt` |

**Commits produit utiles** : `41839ee8` (sécurité) · `2377fe02` (lint) · `7d78664f` (NPE `findItem`) · `c1e01f24` (allowBackup=false + E1) · `8c391e88` (audit v1.2) · `20a7822` (prototype, `neoxff/main`) · `2a8eb426` (base du fork).
**Branches utiles** : `feat/dl-etape-2` (le `search` YouTube, ~60 lignes à porter) · `feat/dl-securite` (**divergente** : ne jamais merger, cherry-pick ciblé).

## 2. Baseline de compilation et de test

| Contrôle | Commande | Résultat attendu |
|---|---|---|
| Tests unitaires | `./gradlew --offline :app:testFdroidDebugUnitTest` | `BUILD SUCCESSFUL`, **77 tests verts** |
| Sécurité | `… --tests "com.bmusic.app.downloader.security.*"` | `SecurityModuleTest` 66/0/0 · `SsrfWiringTest` 11/0/0 |
| Lint | `./gradlew lint` | aucun `HardcodedText` |
| Glass policy | `bash scripts/check-glass-policy.sh` | passe |
| Compilation debug | `./gradlew --offline :app:assembleFdroidDebug` | succès |

**Suite de tests existante** (à ne jamais casser) : `SecurityModuleTest` (66) · `SsrfWiringTest` (11) · `NavigationMenuCrashTest` (régression NPE) · `DownloadDuplicateBehaviorTest` (E1, **non commité**, en échec Koin).

## 3. Baseline de la navigation

| Fait | Valeur mesurée | Preuve |
|---|---|---|
| Entrées max de la barre | **6** | `MAX_ITEM_COUNT` dans `material-1.14.0.aar` (bytecode) |
| Entrées actuelles | 5 catégories + `nav_download` = **6** | `updateTabs()` L464-489 · `MAX_VISIBLE_CATEGORIES = 5` |
| Reconstruction runtime | `menu.clear()` puis ré-ajout | `AbsSlidingMusicPanelActivity.kt` L466 |
| Destination de départ | 1re catégorie visible | `MainActivity.kt` L144-160 |
| Barre visible pour | 8 ids de catégories | `MainActivity.kt` L182-190 |
| Barre masquée pour | `nav_queue`, `nav_lyrics_editor`, `nav_play_info`, `nav_about`, **et tout `else`** | `MainActivity.kt` L195-206 |
| Icônes | 203 vecteurs `ic_*_24dp.xml` | `app/src/main/res/drawable/` |

## 4. Baseline des tokens (le code fait foi)

| Token | Valeur | Fichier |
|---|---|---|
| `backgroundDark` / `backgroundLight` | `#0B0B0E` / `#FAFAFB` | `ui/theme/Color.kt` |
| `surface` | `#131318` / `#FFFFFF` | idem |
| `onSurface` | `#F2F2F0` / `#1B1B1F` | idem |
| `onSurfaceVariant` | `#B8B8C0` / `#4A4A52` | idem |
| `primary` | `#E8862E` / `#A85B0B` | idem |
| `onPrimary` | `#241203` | idem |
| `error` | `#FFB4AB` / `#BA1A1A` | idem |
| `Dimens.TouchTarget` | **48 dp** | `ui/theme/Dimens.kt` L8 |
| `Dimens.ScreenMargin` | 20 dp | idem |
| `Shape` | 8 / 12 / 16 / 20 / 28 / Circle | `ui/theme/Shape.kt` |
| `Type` | Inter 400/500/600 | `ui/theme/Type.kt` |
| `Motion` | 120 / 220 / 300 / 420 ms · Skeleton 1200 ms · `StaggerMs 24` · `MaxStaggerItems 8` | `ui/theme/Motion.kt` |

**Contrastes vérifiés (WCAG)** : `onSurface/surface` 16,52 (sombre) / 17,17 (clair) · `onSurfaceVariant/surface` 9,40 / 8,78 · `primary/surface` 6,95 / 5,04 · `onPrimary/primary` 6,78 / 5,04 · `error/surface` 10,91 / 6,46.

## 5. Baseline de sécurité (après SEC-001/002/003)

| Contrôle | État | Preuve |
|---|---|---|
| Validation d'URL | 10 contrôles, ASCII strict | `UrlValidator.kt` |
| Anti-SSRF | parsing IPv6 canonique, fail-closed | `SsrfGuard.kt` |
| Résolveur guardé | branché sur les 2 clients | `GuardedDns.kt` |
| WebView / iframe / pont JS | **aucun** | `SEC-V-01` |
| Télémétrie | **aucune** | `SEC-V-02` |
| Secrets dans l'arbre | **aucun** | `SEC-V-03` |
| Historique de recherche | ≤ 10, dédoublonné, effaçable | `SEC-V-05` |
| `allowBackup` | `false` | `SEC-V-07` |
| Signature release | variables d'environnement | `SEC-V-08` |
| Minification release | `isMinifyEnabled` + `isShrinkResources` | `SEC-V-09` |

**Risques ouverts bloquants** : SEC-004 (permissions par flavor) · SEC-016 (docs de promesse) · SEC-005 (décision NewPipe). Voir `security/risk-register.md`.

## 6. Ce que la baseline **ne** contient pas

| Absent | Conséquence |
|---|---|
| `docs/architecture/` (5 livrables) | créé par cette phase |
| `app/src/fdroid/AndroidManifest.xml` | SEC-004 : `INTERNET` présente dans tous les APK |
| L'override `search` de l'engine | F-P1-02 : l'onglet Explorer n'a pas de moteur |
| `ic_link_open_24dp.xml` | F-DS-09 : pas d'icône pour Liens |
| Un registre de fournisseurs | F-P1-06 / SEC-013 : pas d'allowlist de plateformes |
| `values-en/` | F-PM-01 : le défaut est l'anglais |
| Baselines de capture | F-DS-07 : non-régression visuelle non prouvable |
| `Preferences` lié dans Koin de test | le test E1 échoue |

## 7. Invariants à préserver par tout lot

1. La suite de tests reste **verte** (77 + les nouveaux).
2. `check-glass-policy.sh` passe.
3. Le lint `HardcodedText` est propre.
4. Aucune chaîne en dur : tout passe par `R.string`.
5. Aucune icône Unicode : vecteur `ic_*_24dp.xml` au format du dépôt.
6. Cibles tactiles ≥ 48 dp (`Dimens.TouchTarget`).
7. Aucune surface translucide / floue / vitrée.
8. La barre basse ne dépasse **jamais** 6 entrées.
9. Aucun secret, aucune clé, aucun token dans le code ou le dépôt.
10. Aucun `try/catch` qui avale l'erreur.
11. Aucune nouvelle dépendance.
12. Un commit par lot, message Conventional Commits.
