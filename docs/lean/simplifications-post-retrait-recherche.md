# B Music v1.2 — Simplifications post-retrait-recherche (analyse Lean)

> Agent Lean B Music. Date : 14 novembre 2026.
> Objet : identifier ce qui reste de superflu après le retrait de la recherche YouTube (`search()` de `MediaExtractorEngine`, écran Explorer), et proposer au maximum 5 simplifications classées par impact, chiffrées en Mo avec `apkanalyzer`.
> Contrainte : **ne toucher aux deux onglets demandés** (Lien et Historique) ni à aucun de leurs composants.
> Règle : les chiffres ci-dessous sont des **estimations à valider par Build** avec `apkanalyzer` après build release. Aucun chiffre n'est affirmé comme mesuré.

---

## 0. Ce qui a déjà été retiré

| Élément | Action | Statut |
|---|---|---|
| `search()` de `MediaExtractorEngine` | Retiré de l'interface et des implémentations | Fait |
| Écran Explorer (`ExploreFragment`, `ExploreState`, `ExploreScreen`) | Fragment retiré du graphe de navigation, onglet loupe retiré du menu | Fait |
| Référence `nav_explore` | Retirée de `graph_main.xml`, `MainActivity.kt`, `AbsSlidingMusicPanelActivity.kt` | Fait |
| Test `NavigationMenuCrashTest` (assertion `nav_explore`) | Mis à jour | Fait |

---

## 1. Ce qui reste de superflu (inventaire)

### 1.1 Dépendances

| Dépendance | Rôle dans la recherche YouTube | Recommandation |
|---|---|---|
| Aucune dépendance spécifique identifiée à ce jour | La recherche YouTube utilisait `MediaExtractorEngine` (déjà présent) sans dépendance dédiée | À valider par `dependencyAnalysis` |

### 1.2 Ressources

| Ressource | Taille estimée | Recommandation |
|---|---|---|
| Strings de l'écran Explorer (titre, état vide, messages d'erreur spécifiques) | ~500 Ko | Retirer si non utilisé ailleurs |
| Icône loupe spécifique à l'écran Explorer (si distincte de `ic_search_24dp.xml` de la recherche locale) | ~10 Ko | Retirer si non utilisée par la recherche locale |
| Layouts spécifiques à l'écran Explorer | ~50 Ko | Retirer |

### 1.3 Classes et code

| Classe | Rôle | Recommandation |
|---|---|---|
| `ExploreFragment.kt` | Fragment de l'écran Explorer (retiré du graphe) | Supprimer |
| `ExploreState.kt` | État de l'écran Explorer | Supprimer |
| `ExploreScreen.kt` | UI de l'écran Explorer | Supprimer |
| Tests unitaires de l'écran Explorer | Tests liés à l'écran retiré | Supprimer |

### 1.4 Réglages et R8

| Élément | Rôle | Recommandation |
|---|---|---|
| Règles R8 spécifiques à l'écran Explorer (si présentes dans `proguard-rules.pro`) | Conservation des classes de l'écran | Retirer |
| Références à `com.bmusic.app.ui.screen.explore.*` dans le mapping R8 | Obsolètes après retrait | Nettoyé automatiquement par R8 |

### 1.5 États d'écran

| État | Rôle | Recommandation |
|---|---|---|
| `ExploreState` (machine à états de l'écran Explorer) | Obsolète | Supprimer avec les fichiers de l'écran |

---

## 2. Simplifications proposées (max 5, classées par impact)

| # | Simplification | Élément | Impact estimé (Mo) | Effort | Risque | Preuve requise |
|---|---------------|---------|-------------------|--------|--------|----------------|
| S-1 | Supprimer les fichiers de l'écran Explorer (`ExploreFragment.kt`, `ExploreState.kt`, `ExploreScreen.kt`) | Code Kotlin retiré | **~50–150 Ko** | S | Faible | `apkanalyzer` avant/après |
| S-2 | Supprimer les strings de l'écran Explorer non utilisées ailleurs | `strings.xml` | **~100–300 Ko** | S | Faible | `apkanalyzer` avant/après + `lint UnusedResources` |
| S-3 | Supprimer les layouts de l'écran Explorer | `res/layout/*explore*` | **~50–100 Ko** | S | Faible | `apkanalyzer` avant/après |
| S-4 | Supprimer les tests unitaires de l'écran Explorer | `src/test/*explore*` | **~50–200 Ko** (sources de test, pas d'impact APK) | S | Faible | `apkanalyzer` n'affecte pas les sources de test ; vérification par `git diff` |
| S-5 | Supprimer les règles R8 spécifiques à l'écran Explorer | `proguard-rules.pro` | **~0 Mo** (impact sur le mapping, pas sur la taille) | S | Faible | Revue du mapping R8 généré |

**Gain total estimé : ~200–750 Ko sur l'APK** (sources de test non incluses, elles n'impactent pas l'APK release).

---

## 3. Ce qui NE DOIT PAS ÊTRE TOUCHÉ (deux onglets demandés)

| Élément | Raison |
|---|---|
| Package `com.bmusic.app.downloader` (LIEN) | Onglet Lien — en cours d'implémentation |
| Package `com.bmusic.app.ui.screen.links` (HISTORIQUE) | Onglet Historique — en cours d'implémentation |
| Package `com.bmusic.app.data.local.search.*` (recherche locale bibliothèque) | Fonctionnalité distincte, conservée |
| `SearchFragment.kt`, `SearchViewModel.kt` (recherche locale) | Recherche locale de la bibliothèque, pas la recherche YouTube |
| `ic_search_24dp.xml` | Utilisé par la recherche locale de la bibliothèque |
| `SearchRepository.kt`, `SearchQuery.kt`, `SearchFilter.kt` | Recherche locale, conservée |

---

## 4. Règle d'arrêt

> Toute simplification doit être validée par `apkanalyzer` avant/après build release. Aucun chiffre ci-dessus n'est affirmé comme mesuré. Build doit fournir le rapport `apkanalyzer apk size` pour chaque simplification appliquée.

---

## 5. Ordre d'exécution recommandé

| Ordre | Simplification | Condition |
|-------|---------------|-----------|
| 1 | S-1 (fichiers Explorer) | Après retrait complet du graphe de navigation |
| 2 | S-2 (strings Explorer) | Après vérification qu'aucun autre écran ne les référence |
| 3 | S-3 (layouts Explorer) | Après vérification d'absence de référence |
| 4 | S-4 (tests Explorer) | En parallèle, sans impact APK |
| 5 | S-5 (règles R8) | Après revue du mapping R8 généré |
