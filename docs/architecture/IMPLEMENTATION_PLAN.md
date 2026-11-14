# B Music — Plan d'implémentation (v1.2)

> Agent build B Music, phase BUILD-01, 14 novembre 2026.
> **Plan de diff limité**, lot par lot. Chaque lot est un commit, avec son contrôle. Rien n'est commité sans que son contrôle soit vert.

---

## 1. Principes d'exécution

1. **Un lot = un commit** (Conventional Commits), un contrôle exécuté, un résultat consigné.
2. **Contrôle après chaque étape** : `./gradlew --offline :app:testFdroidDebugUnitTest` (77 tests de référence) + le test ciblé du lot.
3. **Aucune régression silencieuse** : si un test existant échoue, on explique la cause et on corrige — on ne supprime pas le test (BUILD-15).
4. **Ne pas réécrire ce qui marche** : les modules `downloader/security`, `downloader/manager`, `ui/theme` ne sont pas touchés, seulement étendus.
5. **Pas de dépendance nouvelle.**
6. **Les arbitrages ouverts bloquent leur lot**, pas les autres.

## 2. Cartographie des lots

| Lot | Contenu | BUILD | Dépend de | Contrôle |
|---|---|---|---|---|
| **B1** | Registre de fournisseurs (objet pur) + tests | 07 | — | `ProviderRegistryTest` |
| **B2** | Analyse d'un lot de texte : découpe, validation, dédup, bornes | 06 | B1 | `LinkBatchParserTest` |
| **B3** | Modèle d'état des liens (7 états) + transitions | 11, 13 | B1, B2 | `LinkEntryStateTest` |
| **B4** | Icônes `ic_link_open_24dp.xml` | 02 | — | lint + test de présence |
| **B5** | Navigation : budget de 6, destinations, barre visible, ré-ajout runtime | 02 | B4 | `NavigationMenuCrashTest` étendu |
| **B6** | Écran Liens (UI) : champ multi-lignes, liste, états, actions | 06, 11, 13 | B2, B3, B5 | `LinksScreenTest` (Robolectric) |
| **B7** | Port du `search` + écran Explorer (UI) | 03, 04 | ADR-002 | `SearchEngineTest` + `ExploreScreenTest` |
| **B8** | Fiche de détail + ouverture externe | 05 | ADR-003 | `ExternalOpenTest` |
| **B9** | Animations (transitions, squelettes) | 14 | B6, B7 | `check-glass-policy.sh` + lint |
| **B10** | Nettoyage : glyphes → vecteurs, chaînes en dur → ressources | 14 | B6, B7 | lint `HardcodedText` |

## 3. Plan de diff par lot

### B1 — Registre de fournisseurs

**Créé** : `app/src/main/java/com/bmusic/app/downloader/provider/ProviderRegistry.kt` (+ `ProviderDescriptor.kt`)
**Modifié** : néant
**Test** : `app/src/test/java/com/bmusic/app/downloader/provider/ProviderRegistryTest.kt`

```kotlin
object ProviderRegistry {
    fun detect(url: String): ProviderMatch?   // null = fournisseur inconnu
}
```

Chaque `ProviderDescriptor` porte : `id`, `labelRes`, `domains` (+ `aliases`), `shortLinkDomains`, `extractId(normalizedUrl)`, `integration` (enum : `ExternalOnly` | `EmbedAllowed` | `PreviewOnly`), `embedOrigins` (**vide** tant qu'ADR-003 n'est pas rendu), `previewAvailable`, `limitations`.

Fournisseurs : YouTube · TikTok · Facebook · Instagram · X · Snapchat. Équivalences : `x.com` ≡ `twitter.com` ; `vm.tiktok.com` / `vt.tiktok.com` (F-PM-03) ; `fb.watch` (développé, non résolu).
**Domaine inconnu ⇒ `null` ⇒ statut « fournisseur non pris en charge » + ouverture externe** (BUILD-07).

### B2 — Analyse d'un lot de liens

**Créé** : `app/src/main/java/com/bmusic/app/downloader/provider/LinkBatchParser.kt`
**Dépend de** : `UrlValidator` (existant, non modifié) · `ProviderRegistry` (B1)

```kotlin
object LinkBatchParser {
    const val MAX_URLS_PER_BATCH = 20
    const val MAX_BATCH_BYTES = 8 * 1024
    fun parse(raw: String, existing: Set<String>): LinkBatchResult
}
```

`LinkBatchResult` : `accepted: List<LinkEntry>` · `rejected: List<RejectedLink>` (raison courte) · `ignoredLines: Int` · `truncated: Boolean`.
Déduplication sur l'**URL normalisée** (`UrlValidator` normalisation — R18) ; un doublon est signalé et **n'est pas** ajouté (l'utilisateur peut forcer).

### B3 — Modèle d'état d'un lien

**Créé** : `app/src/main/java/com/bmusic/app/downloader/provider/LinkEntry.kt`
**7 états** (BUILD-11) : `Pending` · `Analyzing` · `Ready` · `ExternalOnly` · `Unavailable` · `InvalidUrl` · `TemporaryError`.
Chaque état porte les actions disponibles (ouvrir / copier / réessayer / retirer). Mapping depuis `ExtractorError` : `UrlInvalid→InvalidUrl`, `Unsupported→ExternalOnly`, `ProtectedContent→Unavailable`, `SourceUnavailable→TemporaryError`, `Network→TemporaryError`.

### B4 — Icône « Liens »

**Créé** : `app/src/main/res/drawable/ic_link_open_24dp.xml` — 2 anneaux, l'un ouvert, viewport 960×960, `tint="?attr/colorControlNormal"` (D10, ADR-009).
**Réutilisé tel quel** : `ic_search_24dp.xml` pour Explorer (loupe).

### B5 — Navigation

**Modifié** :
| Fichier | Modification |
|---|---|
| `app/src/main/res/navigation/graph_main.xml` | 2 destinations (`nav_explore`, `nav_links`) + fragments |
| `app/src/main/res/menu/menu_bottom_navigation.xml` | 2 items statiques (ids, titres, icônes) |
| `app/src/main/java/com/bmusic/app/ui/component/base/AbsSlidingMusicPanelActivity.kt` | `updateTabs()` : budget de 6, ré-ajout des 2 onglets |
| `app/src/main/java/com/bmusic/app/MainActivity.kt` | les 2 ids dans la branche « barre visible » ; `nav_download` également (F-P1-09) ; `saveTab` non Concerné (les onglets ne sont pas des catégories) |
| `app/src/main/java/com/bmusic/app/core/model/CategoryInfo.kt` | **inchangé** (`MAX_VISIBLE_CATEGORIES = 5` n'est pas touché — ADR-007) |
| **Créé** | `app/src/main/java/com/bmusic/app/core/model/BottomNavBudget.kt` |

```kotlin
object BottomNavBudget {
    const val MAX_ITEMS = 6          // limite Material (MAX_ITEM_COUNT)
    const val DISCOVERY_ITEMS = 2     // Explorer + Liens
    fun categorySlots(downloadVisible: Boolean): Int =
        MAX_ITEMS - DISCOVERY_ITEMS - (if (downloadVisible) 1 else 0)
}
```

**Test** : `NavigationMenuCrashTest` étendu — 4 combinaisons (drapeaux × réseau) × vérification que `menu.size() ≤ 6` et que les 2 ids sont présents.

### B6 — Écran Liens

**Créé** : `app/src/main/java/com/bmusic/app/ui/screen/links/` (`LinksFragment.kt`, `LinksViewModel.kt`, `LinksScreen.kt`)
**Non modifié** : `ui/screen/downloader/` (l'écran Téléchargeur n'est pas touché — PM §9)

Champ **multi-lignes** (plusieurs URL, une par ligne) · bouton « Ajouter » · bouton « Coller » (presse-papiers) · liste d'entrées avec plateforme, état, vignette/icône, actions (ouvrir, copier, retirer) · « Tout effacer » avec confirmation · compteur « X ligne ignorée ».
**Aucune** modification de `SearchTab`/`LienTab` existants : les deux écrans sont **distincts** (les onglets ne sont pas la promotion des onglets Téléchargeur).

### B7 — Port du moteur + écran Explorer

**Conditionné à ADR-002.** Port de l'override `search` depuis `feat/dl-etape-2` (L124-183), **avec** les corrections SEC-011 (encodage + `UrlValidator`).
Écran : champ de recherche (debounce 400 ms — R10), validation 2-200 caractères (R9), squelettes, résultats (miniature 16:9, titre 2 lignes, chaîne, durée — R11), pagination additive (R14), états d'erreur actionnables, annulation des requêtes obsolètes (BUILD-04).

### B8 — Fiche de détail + ouverture externe

**Conditionné à ADR-003.** Fiche : grande vignette, titre, chaîne, durée, actions « Ouvrir dans YouTube » et « Copier le lien ». Ouverture via `IntentExt.openWeb()` (R21, `ACTION_VIEW`).

### B9 / B10 — Animations et nettoyage

Durées nommées de `ui/theme/Motion.kt` uniquement · `scaleDuration(ms, reduced) → 0` pour la réduction des mouvements · **aucune** transparence/blur/reflet (`check-glass-policy.sh`) · glyphes Unicode → vecteurs · chaînes en dur → `R.string`.

## 4. Ordre d'exécution et parallelisme

```
B1 ─▶ B2 ─▶ B3 ─▶ B6 ─┐
                      ├─▶ B9
B4 ─▶ B5 ─────────────┘
        ADR-002 ─▶ B7 ─▶ B9
        ADR-003 ─▶ B8
B10 (nettoyage, après B6/B7)
```

**B1→B3 et B4→B5 sont parallélisables.** B6 et B7 dépendent de B5 (la navigation).

## 5. Ce qui n'est pas dans ce plan

| Élément | Raison |
|---|---|
| Backend / proxy YouTube | hors périmètre tant qu'ADR-002 n'est pas rendu (le proxy est **spécifié** dans `docs/security/02-…` §6.3) |
| WebView / lecteur intégré | hors périmètre tant qu'ADR-003 n'est pas rendu ; exige une revue sécurité dédiée (D-SEC-2) |
| File de lecture (BUILD-12) | le plan produit validé **n'inclut pas** de file de liens (PM §8 : suppression + « Tout effacer » uniquement) — BUILD-12 est donc **hors périmètre**, sa propre condition n'étant pas remplie |
| Migration vers `Neoxff/b_music` | lot L1, traité séparément |
| Publication / pipeline | lot L11 |

## 6. Définition de fin de la phase build

- [ ] B1→B5, B6, B9, B10 livrés et commités, un commit par lot.
- [ ] Suite de tests verte (77 de référence + les nouveaux), zéro régression.
- [ ] `check-glass-policy.sh` passe ; lint `HardcodedText` propre.
- [ ] ADR-002 et ADR-003 rendus (ou explicitement différés avec motif et date).
- [ ] `security/risk-register.md` mis à jour (SEC-012, SEC-013 closed si le registre est livré ; nouveau finding si le build en crée un).
- [ ] `docs/pm/08-recette-fonctionnelle-explorer-liens.md` : le journal reste vierge (recette = phase QA, hors périmètre du build).
