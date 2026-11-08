# B Music — choix du dépôt de base (Build)

> Agent Build B Music. Date : 6 novembre 2026.
> Rappel : le brief autorise `{REPO_URL}` fourni par le porteur ; il n'a pas été substitué (placeholder). Le choix a donc été fait selon les critères éliminatoires, puis confirmé par le document Lean `01-perimetre-mvp.md` (§1 : BoomingMusic retenu).

## 1. Critères éliminatoires

Licence open source claire · dernier commit < 12 mois · compile avec Gradle récent · Kotlin · lecture via Media3/ExoPlayer ou équivalent · architecture lisible · UI Jetpack Compose (idéal) ou Material Components.

## 2. Candidats comparés (mesurés via API GitHub, 6 nov. 2026)

| Dépôt | Licence | Dernier push | Stars | Langage | UI / moteur | Architecture | Verdict |
|---|---|---|---|---|---|---|---|
| **mardous/BoomingMusic** (`master`) | **GPL-3.0** ✅ | **2026-11-02** (< 1 mois) | 600 | Kotlin | **Jetpack Compose + Material 3**, **Media3** (ExoPlayer + session), Koin, Room, Navigation Compose | Lisible : MVVM (ViewModels + Repository), flavors `github`/`fdroid`/`playstore`, FR/EN externalisés, LargeHeap à retirer | **RETENU** |
| RetroMusicPlayer/RetroMusicPlayer (`dev`) | GPL-3.0 ✅ | **2026-11-06** (actif) | 5 387 | Kotlin | Legacy View Binding/XML + Material, ExoPlayer, Koin | Plus riche fonctionnellement mais **code historique/XML dominant**, fork amont de Booming | Écarté (héritage lourd à défaire) |
| zyrouge/symphony (`main`) | **AGPL-3.0** ⚠️ | 2026-10-02 | 1 725 | Kotlin | Compose + Material 3, **projets plus petits / moins complets** (bibliothèque, file d'attente) | Lisible mais **surface fonctionnelle moindre** ; licence AGPL (contrainte forte pour redistribution) | Écarté |
| bmaroti9/Citole (`master`) | GPL-3.0 | 2026-07-29 | **17** | Kotlin | Compose, MD3 Expressive | **Trop jeune** (mai 2026, 17 stars, peu de tests) | Écarté |

## 3. Pourquoi BoomingMusic

1. **Licence** GPL-3.0 : dérivation autorisée avec publication des sources + conservation `LICENSE.txt` et mentions (NOTICE.md ajouté).
2. **Actif** : dernier push 2026-11-02, releases fréquentes (v1.4.2), issue → fix rapide.
3. **Stack cible exacte** : Compose + Media3 (la cible technique du brief — lecture fiable hors-ligne, notification média propre).
4. **flavors `fdroid`** : `network_features_enabled_by_default=false`, `enable_builtin_updater=false` — base idéale pour la promesse « zéro réseau » (on supprime réellement le réseau, au-delà du booléen, cf. audit lean actions #1–4).
5. **FR/EN déjà en place** (`values-fr-rFR` + `values/` EN) — le brief exige FR défaut + EN.
6. **Références techniques mesurés** : APK fdroid v1.4.2 = 14,5 Mo (arm64-v8a), universal 21,9 Mo → cible < 15 Mo tenable après nettoyage (audit Lean).
7. **Dépendances propres** : pas de Firebase/AdMob/Crashlytics détectés (audit Lean §1.3) ; Media3 + Coil présents, réutilisables pour le cache borné.

## 4. Ce que le fork conserve / ce que B Music change

**Conservé** : LICENSE.txt (GPL-3.0), copyright RetroMusic → Booming → B Music (NOTICE.md), historique git complet, modules existants (favoris, playlists, gapless, Media3, Coil, Room) — **B Music améliore, ne réécrit pas**.

**Changé (ordre des livrables)** : renommage complet → `B-Music.apk` → design system B Music → animations ressort → garde-fous CI (blur/glass interdits) → perf mesurée.

## 5. Limites connues (transparence)

- Fork volumineux (482 fichiers Kotlin, 106 layouts legacy cohabitant avec Compose) : migration écran par écran, pas de big-bang.
- `largeHeap=true` dans le manifest (à supprimer), Universal APK par défaut (à désactiver), lint `abortOnError=true` (à garder).
- Le dépôt porte des restes de l'amont (`com.mardous.booming` dans tout le code — renommage en `com.bmusic.app` requis).
