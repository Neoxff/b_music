# B Music — revue de fin d'itération n° 1

> Agent Lean B Music. Date : 6 novembre 2026.
> Périmètre de la revue : livraisons « Designer » (commit `ed77629` — design system « Sonore », tokens, maquette, 4 écrans) et « Build » (commit `70802b1` — emballage Capacitor, APK debug + release signé ; `app/`), plus `01944f1` (PM) et `f014365` (Security).

## 1. Verdict : **GO AVEC RÉSERVES**

L'itération produit des artefacts utiles (maquette belle, tokens propres, APK maquette signé, permissions minimales, CSP stricte) **mais aucune des exigences de valeur (lecture, bibliothèque, lecteur) n'est implémentée** : le « lecteur » livré est une page web statique emballée. Les budgets ne sont ni mesurés ni mesurables en l'état. Le dépôt contient **trois prototypes parallèles** (Expo `mobile/`, Tauri `tauri/`, Capacitor `app/`) — duplication pure sans ajout de valeur — et **contredit** la décision de base actée dans `01-perimetre-mvp.md` (fork natif BoomingMusic `fdroid`).

## 2. Les 4 questions de la revue

### Q1 — Fluidité, beauté ou fiabilité ?

| Livraison | Sert ? | Verdict |
|---|---|---|
| Design system « Sonore » (tokens opaques, pas de blur, 4 écrans) | **Beauté** — oui (direction conforme : pas de liquid glass, opacité, typo forte) | garder comme **référence UI** |
| Maquette `maquette.html` (offline, tokens réutilisés) | Beauté — oui, en tant que cible ; ce n'est **pas un produit** | archiver en `design/` |
| Capacitor `app/` (APK signé, minSdk moderne, permissions minimales, CSP) | Fiabilité **du wrapper** seulement ; aucune fiabilité de lecture (pas d'audio) | **retirer du chemin critique** |
| `mobile/` (Expo, 361 Mo node_modules) | Rien (développé puis abandonné ?) | **retirer** |
| `tauri/` (Tauri 2 mobile, build de test) | Rien prouvé ; Tauri mobile jeune (risque actée par PM) | **archiver / retirer** |

**Décision : Q1 = 1/3 des livraisons sert la valeur. 2 prototypes à retirer.**

### Q2 — Réutilisation d'existant vs création nouvelle ?

- ❌ Le cœur décide par `01-perimetre-mvp.md` était le **fork BoomingMusic** (lecture fiable, bibliothèque complète, notification média, GPL-3.0, FR/EN). Au lieu de l'amorcer, Designer+Build ont créé **trois frontends neufs** (Expo → Tauri → Capacitor) — le code le plus réutilisable du dépôt (le fork) n'a **jamais été cloné côté produit** (l'audit de `02-audit-gaspillage.md` a dû être fait sur `/tmp`).
- ✅ Une réutilisation correcte : la maquette réutilise ses propres tokens, pas de recopie de libs concurrentes d'animation côté web.

**Décision : Q2 = non conforme.** Le dépôt réécrit au lieu d'améliorer — exactement ce que la mission interdit.

### Q3 — Nombre d'écrans / variantes / paramètres en hausse ?

- 4 écrans maquettés, OK (périmètre MVP = 5–6 écrans). Rien d'excessif côté maquette.
- Mais **3 stacks** (Expo, Tauri, Capacitor) + `design/maquette.html` + `app/www` : 4 représentations du même produit — inflation de variantes sans besoin.
- Aucun écran bibliothèque réelle, aucun service audio : l'augmentation est **structurelle (stacks), pas fonctionnelle**.

**Décision : Q3 = inflation, à ramener à 1 stack.**

### Q4 — Budgets avant / après

| Budget | Avant (base BoomingMusic v1.4.2, mesuré) | Après (état repo, mesuré) | Δ / verdict |
|---|---|---|---|
| APK release | arm64-v8a **14,5 Mo** (GitHub v1.4.2) ; universal 21,9 Mo ❌ | Capacitor release **0,93 Mo** (maquette sans audio) | Taille « gagnante », valeur nulle — **non comparable** |
| Démarrage froid | non mesuré (cible < 600 ms) | non mesuré (aucun Macrobenchmark, aucun device) — défauts : 3 stacks, `largeHeap` non traité | ❌ **non mesurable, non mesuré** |
| Mémoire | `largeHeap=true` dans le manifest Booming → à supprimer | n/a (pas de lecture) | ❌ non instrumenté |
| Jank / 60-120 fps | jamais mesuré (compose déjà présent côté fork) | web maquette : animations à valider | ❌ aucune preuve |
| Android Lint `UnusedResources` | non exécutable ici (gradle 9.6 download interrompue) ; analyse statique faite (cf. `02`) | n/a | ⚠️ rapport Lint manquant |

**Décision : Q4 = 0/4 budgets vérifiés. Aucune livraison de cette itération n'apporte de preuve de fluidité ou de mémoire.**

## 3. Ce qui est bon, à conserver

1. Tokens « Sonore » (couleurs opaques, élévation tonale, 1 px séparators, typo forte) — **directive design v1**.
2. `app/` manifest : permissions minimales, `allowBackup=false`, `usesCleartextTraffic=false`, CSP stricte — hygiène à recopier dans le fork.
3. Processus : APK release signé automatisé ; doc security (`f014365`) et PM (`01944f1`) utiles ; `01-perimetre-mvp.md` (base BoomingMusic) reste la **décision opposable**.
4. Interdits respectés : aucun tracking, aucune pub, aucun réseau dans la maquette.

## 4. Actions correctives (impact décroissant, max 5)

1. **Trancher la stack une fois pour toutes** : archiver `mobile/` et `tauri/` hors du dépôt produit ; le produit v1.0 = **fork BoomingMusic `fdroid`** (cf. `01`). Effort S. Impact : élimine 380 Mo de `node_modules` et 2 dead-ends.
2. **Amorcer le fork** (`B-Music`) : clone, package `com.bmusic`, APK `B-Music.apk`, suppression réseau/widgets/tag-editor/Chromecast (liste exacte et gains dans `02-audit-gaspillage.md` actions #1–#5). Effort M. Impact : **le produit existe enfin**.
3. **Mesurer avant d'ajouter** : Macrobenchmark `StartupBench` + `LibraryScrollBench` sur le fork ; tableaux froid/chaud/jank dans chaque PR ; Baseline Profiles. Effort M. Impact : contrat de fluidité réel (sinon Q4 restera rouge).
4. **Porte Lint + budgets en CI/local** : `lintFdroidRelease` + échec si budget `03` dépassé ; APK arm64-v8a mesuré < 15 Mo à chaque build. Effort S. Impact : `03-budgets-performance.md` devient opposable, pas déclaratif.
5. **Design tokens → Compose theme** : porter `tokens.css` en `Color.kt`/`Type.kt`/`Shape.kt` Material 3 (opaque, pas de blur) dans le fork ; **supprimer** la piste Capacitor du chemin de release. Effort M. Impact : « beauté » livrée dans le vrai produit, pas dans une maquette.

## 5. Risques si GO inconditionnel

- Le repo devient un cimetière de prototypes ; l'APK livré n'est pas un lecteur ; les budgets (15 Mo, 600 ms, 200 Mo, 1 % jank) restent des intentions.
- Dépendance GPL-3.0 (fork BoomingMusic) exige attribution visible — à faire au plus tard J1 du fork.
- Tauri mobile : risque de blocage build déjà identifié par PM — trancher vite pour éviter le sunk cost.

**GO pour la suite, AVEC RÉSERVES** : l'itération 1 valide le *design* et les *processus*, pas le *produit*. Les 5 actions ci-dessus conditionnent l'itération 2. Aucun passage en « GO » tant que lecture + file + mini-player + notification média ne tournent pas sur device, mesurés selon `03-budgets-performance.md`.
