# B Music — Fiche de mesure LEAN : widgets liquid glass

> Agent LEAN — 17 novembre 2026. Branche `feat/widgets-glass`, commit `98db3f07`.
> Fiche à remplir sur **votre téléphone**, sans ADB, sans émulateur. Aucune mesure n'est prétendue ici.
> Règle : une case « non mesuré » est une information honnête. Je ne conclus pas tant que vos valeurs ne sont pas renvoyées.

---

## 1. Budgets et état des preuves locales

| Indicateur | Budget exigé | Valeur prouvée localement | Source de la preuve | Statut |
|---|---|---|---|---|
| Poids du cache de pochettes floutées | valeur en Mo à fixer | **0 Mo** : aucun cache de bitmaps floutés n'existe. `BLURRED_ARTWORK_PX = 96` et `artworkCacheKey()` sont déclarés mais **non utilisés** | `WidgetGlassPolicy.kt:23,47` ; `grep` → 1 seule occurrence chacun (déclaration) | Fait local. Budget cible à fixer : **≤ 2 Mo** (proposition, non mesurée) |
| Mémoire d'un bitmap de widget | borne < limite de transaction du lanceur | Placeholder servi en `content://` URI, **pas de bitmap dans la transaction RemoteViews**. Bitmap source mis en cache : `512×512` ARGB_8888 = **1 048 576 o = 1,0 MiB** ; cible floutée `96×96` = 36 864 o = **0,035 MiB** | `WidgetArtwork.kt:30-42` (URI) ; `CoverProvider.kt:485-486` (`SMALL_SIZE/FALLBACK_DIMENSION = 512`) ; `WidgetGlassPolicy.kt:23` | Fait local pour le calcul. **Limite exacte de transaction du lanceur : non vérifiable ici** (outil web indisponible) → voir valeurs manquantes |
| Fréquence de mise à jour | aucune mise à jour plus fréquente que nécessaire | Prouvé statiquement : tick intervalle **≥ 2 000 ms** ; snapshot complet ignore la timeline ; ticker actif seulement si `isPlaying && screenOn` ; `stop()` annule `refreshJob` et `tickerJob` | `WidgetProgress.kt:24,28-30` ; `WidgetUpdater.kt:120-148` ; `WidgetPresenter.kt:76-80,113-122` ; test JVM `WidgetGlassPolicyTest` vert | Conforme sur le plan logique. Fréquence réelle à confirmer par votre mesure |
| Impact batterie à l'arrêt de la lecture | nul (0 %) | Statiquement : aucun job ne tourne quand `isPlaying = false` (`syncTicker` retourne immédiatement) | `WidgetPresenter.kt:115` | Fait logique. **Mesure batterie absente** |
| Taille APK ajoutée | < 0,3 Mo | **+64 octets = 0,00006 Mo** (APK `feat/widgets-glass` 15 447 399 o vs `v1.4.5` livrée 15 447 335 o) | `stat` sur les deux APK release arm64 | Conforme, très sous le budget |

### Rappel d'honnêteté sur le périmètre

Les ressources visuelles (drawable verre, couleurs clair/sombre) créées during la phase BUILD ont été **retirées** car non référencées (éliminées par `shrinkResources`, APK +64 o seulement). Le chiffre **+64 o** couvre uniquement la logique de repaint committée (`98db3f07`), pas un rendu verre livré. Je ne prétends donc pas que le cache flouté de 96 px existe : il reste à implémenter.

---

## 2. Protocole de mesure — 30 minutes de lecture, 3 tailles de widget posées

### Préparation (5 min)

| # | Quoi faire | Résultat attendu | Ce que vous me renvoyez |
|---|---|---|---|
| P.1 | Notez le modèle d'appareil, la version d'Android, le taux d'écran (60 / 120 Hz) et le nom du fichier APK installé | Contexte connu | Ces 4 valeurs |
| P.2 | Ouvrez Paramètres → Batterie ; notez le **% de batterie** et, si disponible, le temps d'écran affiché | Valeur de référence | % initial, temps d'écran initial |
| P.3 | Ajoutez sur l'écran d'accueil les **3 tailles** : lecteur compact **4×1**, lecteur moyen **4×2**, grand **4×4** (si le 4×4 n'existe pas dans votre build, notez « absent ») | 3 widgets posés | Confirmation + capture d'écran de l'accueil |
| P.4 | Assurez-vous que la lecture est **arrêtée** avant de commencer | Aucun ticker actif | OK |

### Scénario A — 30 min de lecture avec les 3 widgets

| # | Quoi faire | Résultat attendu | Ce que vous me renvoyez |
|---|---|---|---|
| A.1 | Lancez la lecture d'un morceau (ou d'une file) ; gardez l'écran allumé et les widgets visibles | Lecture continue | OK |
| A.2 | Laissez tourner **30 minutes** sans interaction | Aucune saccade, progression des widgets fluide | Toute anomalie notée |
| A.3 | Après 30 min, ouvrez Paramètres → Batterie | — | % de batterie final, **delta %**, consommation affichée pour « B Music » si l'écran la donne |
| A.4 | Si l'écran Batterie propose la ventilation par application, ouvrez la fiche B Music | — | Temps d'activité et % attribué à B Music, ou « non affiché » |
| A.5 | Capturez l'écran Batterie | Preuve | La capture |

### Scénario B — 30 min à l'arrêt, widgets toujours posés

| # | Quoi faire | Résultat attendu | Ce que vous me renvoyez |
|---|---|---|---|
| B.1 | Mettez la lecture en **pause**, écran allumé, widgets visibles | Aucun job widget actif | OK |
| B.2 | Notez le % de batterie | Référence | % initial |
| B.3 | Laissez 30 minutes sans interaction | **Delta = 0 %** (budget « impact nul à l'arrêt ») | % final, **delta %**, capture de l'écran Batterie |

### Scénario C — fréquence de mise à jour (observation qualitative)

| # | Quoi faire | Résultat attendu | Ce que vous me renvoyez |
|---|---|---|---|
| C.1 | Pendant la lecture, regardez la progression du widget moyen 4×2 pendant 10 s | Mise à jour visible environ toutes les 2 s (jamais plus fréquente) | « conforme » / « trop rapide » / « trop lente » |
| C.2 | Mettez en pause, observez 10 s | Progression **figée**, aucun clignotement | « figée » / « bouge encore » |

> Aucune commande `adb` n'est utilisée ni demandée. Les captures et lectures de % se font uniquement via l'interface du téléphone.

---

## 3. Analyse de vos valeurs

**Aucune valeur utilisateur n'a été reçue à ce jour.** Je ne peux donc porter aucun verdict GO / NO-GO, conformément à votre règle « si des valeurs manquent, liste-les sans conclure ».

### Valeurs manquantes (à me renvoyer)

1. Modèle d'appareil, version d'Android, taux d'écran (60/120 Hz), nom du fichier APK — P.1.
2. % de batterie initial avant le test de lecture — P.2.
3. Confirmation que les 3 tailles de widget (4×1, 4×2, 4×4) sont posées, ou « 4×4 absent » — P.3 + capture.
4. % de batterie après 30 min de lecture et **delta %** — A.3.
5. Consommation « B Music » affichée par l'écran Batterie, ou « non affichée » — A.4 + capture A.5.
6. % de batterie initial et final à l'arrêt, et **delta %** (doit être 0) — B.1, B.2, B.3 + capture.
7. Observations de fréquence de progression en lecture et en pause — C.1, C.2.
8. Toute anomalie visuelle notée pendant les 30 min — A.2.
9. **Limite exacte de transaction du lanceur / RemoteViews** (fait externe) : je n'ai pas pu la vérifier, l'outil de recherche web renvoyant « API key required ». Elle reste à confirmer par documentation officielle avant de figer la borne « bitmap < limite ».

### Ce que je pourrai conclure une fois les valeurs reçues

- Budget APK < 0,3 Mo : déjà prouvé localement (+0,00006 Mo) — aucun apport de votre part nécessaire.
- Cache flouté ≤ 2 Mo : budget proposé, non vérifiable avant implémentation du cache.
- Batterie à l'arrêt = 0 % : verdict dépend de votre valeur B.3.
- Batterie en lecture ≤ +5 % : verdict dépend de votre valeur A.3 (budget hérité de `budgets-v3.md`).
- Fréquence de mise à jour : verdict dépend de C.1/C.2 croisés avec la preuve statique (≥ 2 s).

**Verdict global : en attente de vos valeurs. Aucune conclusion anticipée.**
