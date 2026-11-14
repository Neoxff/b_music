# B Music — Audit d'architecture (v1.2)

> Agent build B Music, phase BUILD-01, 14 novembre 2026.
> **Lecture seule.** Cet audit décrit l'architecture *existante* du produit telle qu'elle est, pour cadrer l'implémentation des onglets Explorer et Liens. Il ne propose pas de refonte.
> Périmètre produit : la copie locale `bmusic/` (branche `fix/stabilite-1.2`, HEAD `41839ee8`).
> Documents amont : `docs/pm/05-spec-explorer-liens.md` (spécification) · `docs/design/09-…` → `13-…` (design) · `docs/security/02-…` + `security/risk-register.md` (sécurité).

---

## 1. Forme du produit

| Élément | Valeur | Preuve |
|---|---|---|
| Langage / UI | Kotlin 2.3.21 · Jetpack Compose (BOM 2026.09.00) · Material3 | `gradle/libs.versions.toml` |
| Build | AGP 9.5.0 · Gradle · KSP 2.3.6 · JVM 21 | idem |
| SDK | `minSdk 26` · `targetSdk 36` · `compileSdk 37` | `app/build.gradle.kts` |
| Modules | `:app` + `:macrobenchmark` | `settings.gradle.kts` |
| Flavors | `github` · `fdroid` · `playstore` (dimension `version`) | `app/build.gradle.kts` L91-107 |
| DI | Koin 4.2.2 (`MainModule` + `DownloaderModule`) | `App.kt` L83 |
| Persistance | Room 2.8.5 (`BMusicDatabase` v10) + SharedPreferences | `core/BoomingDatabase.kt` |
| Image | Coil 3.6.3 | `coil/` |
| Réseau | Ktor 3.6.0 (features amont) · OkHttp (module downloader) | `data/remote/KtorClient.kt` · `downloader/manager/DownloaderHttpClient.kt` |
| Extraction | NewPipeExtractor 0.27.0 (module downloader) | `downloader/extractor/` |
| Tests | JUnit 4.13.2 · Robolectric 4.16.1 · `compose.ui.test.junit4` | `app/build.gradle.kts` L354-362 |

## 2. Couches et dépendances (ce qui est interdit de traverser)

```
ui/ (Compose, ViewModel)  ──▶  data/  ──▶  core/
                                    │
downloader/ (module自治)  ──▶  security/  (UrlValidator, SsrfGuard, GuardedDns, ImageUrlPolicy, QueryValidator)
```

| Règle | Origine | Conséquence pour les deux onglets |
|---|---|---|
| L'UI ne parle **jamais** directement au réseau | architecture existante | Explorer/Liens passent par un `ViewModel` → une interface de service |
| Le module `downloader/` possède **son** client HTTP (`DownloaderHttpClient`), distinct de Ktor | `DownloaderHttpClient` KDoc L9-18 | Les onglets réseau réutilisent ce client, pas celui de Last.fm |
| Toute URL entrante passe par `UrlValidator` | `downloader/security/` | R16 du PM |
| Toute destination réseau passe par `SsrfGuard` / `GuardedDns` | correctif SEC-001 | Aucun `OkHttpClient` nouveau sans `.dns(GuardedDns)` |
| Le socle de sécurité est **fail-closed** | `SsrfGuard` + `DownloadTransferEngine` | Un garde qui avale son exception est un bug (cf. SEC-001) |

## 3. Navigation — l'anatomie réelle

| Fait | Preuve | Conséquence |
|---|---|---|
| Barre basse = `BottomNavigationView` Material | `sliding_music_panel_layout.xml` L44-45 | **Plafond de 6 entrées** (`MAX_ITEM_COUNT`, vérifié dans le bytecode de `material-1.14.0`) |
| Le menu est **reconstruit à l'exécution** | `AbsSlidingMusicPanelActivity.updateTabs()` L464-489 : `menu.clear()` puis ré-ajout | Toute solution passe par `updateTabs()`, **jamais** par un menu statique |
| Les catégories visibles viennent d'une préférence | `Preferences.libraryCategories` L136 · `CategoryInfo.MAX_VISIBLE_CATEGORIES = 5` | 5 + 1 (`nav_download`) = **6**, exactement à la limite |
| `nav_download` est conditionnel | `updateTabs()` L478-482 (`Preferences.downloaderEnabled`) | Le seul jour de réserve existant est déjà pris |
| La barre est pilotée par destination | `MainActivity.setupNavigationController()` L176-206 | Un `else` **masque** la barre : un nouvel onglet non listé ⇒ barre invisible |
| `setupWithNavController` est branché | `MainActivity` L162 | Les ids du menu doivent exister dans `graph_main.xml` |

**Les 5 points à toucher pour un onglet top-level** (F-P1-08) : `graph_main.xml` (destination) · `menu_bottom_navigation.xml` (item statique) · `updateTabs()` (ré-ajout runtime) · `MainActivity` (barre visible + `saveTab`) · le fragment lui-même.

## 4. Module `downloader/` — l'état réel

| Brique | Fichier | État |
|---|---|---|
| Validation d'URL | `security/UrlValidator.kt` (127 l.) | 10 contrôles, ASCII strict (SEC-003) |
| Garde SSRF | `security/SsrfGuard.kt` (~300 l.) | IPv6 canonique (SEC-002) |
| Résolveur guardé | `security/GuardedDns.kt` | branché sur les 2 clients (SEC-001) |
| Miniatures | `security/ImageUrlPolicy.kt` | allowlist 7 hôtes (trop étroit — SEC-012) |
| Requête | `security/QueryValidator.kt` | 2-200 car. |
| Noms de fichiers | `security/FileNameSanitizer.kt` | anti-traversal, extensions en liste blanche |
| Client HTTP | `manager/DownloaderHttpClient.kt` | `.dns(GuardedDns)`, sans cookie, UA neutre |
| Moteur | `extractor/NewPipeExtractorEngine.kt` (144 l.) | `resolve` seul — **pas de `search`** (F-P1-02) |
| UI existante | `ui/screen/downloader/` | `SearchTab.kt` + `LienTab` (privé, `DownloadScreen.kt` L147-221) |

**Réutilisable tel quel** : `UrlValidator`, `SsrfGuard`, `GuardedDns`, `QueryValidator`, `ImageUrlPolicy`, `FileNameSanitizer`, `DownloaderHttpClient`.
**À étendre** : `ImageUrlPolicy.ALLOWED_HOSTS` (SEC-012), l'engine (adding `search`), `ImageUrlPolicy` pour les nouveaux CDN.

## 5. Conventions du dépôt (à respecter)

| Convention | Preuve |
|---|---|
| Chaînes **centralisées** en ressources, FR + EN | `values/strings.xml` (994) + `values-fr-rFR/strings.xml` (981) |
| Lint `HardcodedText` propre | CI `android.yml` L45 (`./gradlew lint`) |
| Icônes = vecteurs `ic_*_24dp.xml`, Material Symbols **rempli**, viewport 960×960, `tint="?attr/colorControlNormal"` | `res/drawable/` (203 vecteurs) |
| **Aucune** dépendance `material-icons`, **aucun** usage de `Icons.` | pas de `androidx.compose.material:material-icons` dans le toml |
| Tokens : `ui/theme/{Color,Dimens,Shape,Type,Motion}.kt` | `Dimens.TouchTarget = 48.dp` |
| Cibles tactiles ≥ 48 dp | `Dimens.kt` L8 |
| Animations : durées nommées dans `Motion.kt`, `scaleDuration(ms, reduced) → 0` | `ui/theme/Motion.kt` |
| Aucun effet « liquid » : `check-glass-policy.sh` passe en CI | `.github/workflows/android.yml` L42 |
| Commits Conventional Commits, un commit par tâche | historique du dépôt |
| Pas de `try/catch` qui masque l'erreur | `DownloadTransferEngine` (corrigé en SEC-001) |

## 6. Écarts relevés entre le code et la documentation

| # | Écart | Traitement |
|---|---|---|
| E1 | `docs/design/tokens/*.kt` (accent `#E8A33D`, fond `#121212`, 44 dp) n'est importé par **aucun** fichier ; le code réel est `#E8862E` / `#0B0B0E` / 48 dp | D9 : le **code fait foi**, la doc est alignée (lot L1) |
| E2 | Le corpus design documente des icônes « trait 1,75 dp, viewport 24×24 » ; l'implémentation est en Material Symbols rempli 960×960 | D10 : le **format réel** est retenu pour les 2 nouvelles icônes |
| E3 | `docs/design/01-design-system.md` §4 dit 44 dp | D6 : 48 dp (code conforme), **la doc est corrigée** |
| E4 | `nav_download` est absent de la liste « barre visible » de `MainActivity` ⇒ barre masquée sur l'onglet Télécharger | F-P1-09 : corrigé au même lot que les 2 nouveaux onglets |
| E5 | `nav_download` réutilise `@drawable/icon_home` comme icône | F-P1-10 : icône dédiée |
| E6 | `LienTab` est `private` dans `DownloadScreen.kt` ⇒ non réutilisable | L4 : extraction en composable public |

## 7. Ce que l'architecture permet — et interdit

| Possible | Interdit |
|---|---|
| Ajouter des onglets en réutilisant `graph_main.xml` + `updateTabs()` | Dépasser 6 entrées dans la barre (exception Material) |
| Réutiliser `UrlValidator` / `GuardedDns` / `DownloaderHttpClient` | Un `OkHttpClient` sans `GuardedDns` ; un `httpClient` vers un hôte non allowlisté |
| Composable Compose dans `ui/screen/` | Une surface translucide / floue (glass policy) |
| Un `ViewModel` par écran, état via `StateFlow` | Un état mutable global ; une exception remontée à l'UI |
| Un registre de fournisseurs en objet Kotlin pur | Un HTML brut rendu ; un WebView sans revue sécurité dédiée (D-SEC-2) |
| Des vecteurs `ic_*_24dp.xml` au format du dépôt | Un glyphe Unicode comme icône (F-DS-02) |
| Une entrée de liste avec état explicite | Un écran vide, un spinner infini, un message non actionnable (R7) |

## 8. Synthèse pour l'implémentation

1. **La navigation est le seul point qui impose un arbitrage produit** (plafond 6). Tout le reste est ajout local.
2. **Le module `downloader/` est réutilisable tel quel** pour la validation et le réseau : c'est un avantage, pas une contrainte.
3. **Les deux onglets n'ont besoin d'aucune dépendance nouvelle** : ni WebView, ni SDK de plateforme, ni backend — à condition de rester en « métadonnées + ouverture externe » (D-SEC-2) et de ne pas exiger de recherche officielle (D-SEC-3, non arbitré).
4. **Les écrans existants du Téléchargeur ne sont pas modifiés** : `SearchTab` / `LienTab` sont **extraits** (copiés ou factorisés), pas modifiés sur place (R9 du PM §9).
