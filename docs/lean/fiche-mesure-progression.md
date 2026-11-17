# B Music — Fiche de mesure LEAN : progression de téléchargement

> Agent LEAN — 17 novembre 2026. Branches : `feat/dl-progression` (code, commit `4f14dec3`) / `feat/glass-global` (docs).
> Fiche à remplir sur **votre téléphone**, sans ADB, sans émulateur. Aucune mesure n'est prétendue ici.
> Règle : une case « non mesuré » est une information honnête. Je ne conclus pas tant que vos valeurs ne sont pas renvoyées.

---

## 1. Budgets et état des preuves locales

| Indicateur | Budget | Preuve locale vérifiable | Statut |
|---|---|---|---|
| Mises à jour de progression | **≤ 4 / s** par téléchargement | `ProgressThrottle(minIntervalMs = 250L)` dans `ProgressLogic.kt:103` → intervalle plancher 250 ms = 4 émissions/s maximum ; test JVM `ProgressPublishPolicyTest.bus_emitsAtMostEvery250ms` vert | Conforme **sur le plan logique**. Fréquence réelle à confirmer par votre mesure |
| Mises à jour de notification | **≤ 1 / s** | Test JVM `ProgressPublishPolicyTest.notification_emitsAtMostEvery1000ms` (`ProgressThrottle(1000L)`) vert ; note : le câblage effectif de `setForeground` n'est pas encore livré (étape 2 du plan BUILD) | Conforme **au niveau du limiteur**, pas encore au niveau de l'émission réelle |
| Images lentes | **< 1 %** pendant un téléchargement actif, onglet Historique à l'écran | Aucune mesure locale possible : la valeur vient de l'écran Diagnostics (build debug) | **Valeur manquante** |
| Impact batterie | **≤ +5 % sur 30 min** | Aucune mesure locale possible | **Valeur manquante**. Tension à trancher : votre protocole décrit un téléchargement de **10 min**, le budget porte sur **30 min** — je n'extrapole pas |

### Pourquoi je ne peux pas mesurer localement

Ces quatre indicateurs sont des **métriques d'exécution** : elles dépendent du téléphone, du réseau et de l'écran. Les seules preuves que je peux produire hors appareil sont statiques (code, tests JVM) et elles sont listées ci-dessus. Toute affirmation numérique sur les images lentes ou la batterie serait inventée.

---

## 2. Protocole de mesure — 10 min d'audio public, onglet Historique ouvert

### Préparation (5 min)

| # | Quoi faire | Résultat attendu | Ce que vous me renvoyez |
|---|---|---|---|
| P.1 | Notez le modèle d'appareil, la version d'Android, le taux d'écran (60 / 120 Hz), le nom du fichier APK et le réseau (Wi-Fi / 4G) | Contexte connu | Ces 5 valeurs |
| P.2 | Ouvrez l'écran **Diagnostics** (build debug) et notez la valeur **« images lentes »** avant le test | Référence | % initial + capture de l'écran |
| P.3 | Ouvrez Paramètres → Batterie, notez le **% de batterie** | Référence | % initial + capture |
| P.4 | Activez le téléchargeur (Réglages) si ce n'est pas déjà fait ; collez un lien public d'un audio d'environ **10 min** | Lien prêt | Plateforme du lien (ex. Bandcamp / SoundCloud) |
| P.5 | Ouvrez l'onglet **Historique** et laissez-le affiché pendant le test | Historique à l'écran | Confirmation |

### Scénario — téléchargement actif

| # | Quoi faire | Résultat attendu | Ce que vous me renvoyez |
|---|---|---|---|
| M.1 | Lancez le téléchargement, gardez l'onglet Historique ouvert et l'écran allumé | Carte de progression visible | OK |
| M.2 | Observez la ligne concernée pendant 60 s : la barre avance, le pourcentage suit, la vitesse ne clignote pas | Barre monotone, chiffres stables | Toute anomalie notée |
| M.3 | Pendant le test, comptez visuellement sur 10 s le nombre de changements du pourcentage dans la ligne | **≤ 4 changements / 10 s** par téléchargement | Nombre observé (ou « non comptable ») |
| M.4 | Observez la notification sur le volet : comptez ses rafraîchissements sur 10 s | **≤ 1 / s**, texte cohérent avec l'app | Nombre observé + capture du volet |
| M.5 | Laissez le téléchargement se terminer (~10 min) | Fichier en bibliothèque, ligne « Terminé » | OK / échec + rapport Journal si échec |
| M.6 | Rouvrez l'écran **Diagnostics** ; notez la valeur « images lentes » | **< 1 %** | % final + capture de l'écran |
| M.7 | Rouvrez Paramètres → Batterie ; notez le % final | Delta sur 10 min | % final, **delta %**, capture |
| M.8 | Si la ventilation par application est affichée, ouvrez la fiche B Music | Consommation attribuée | % B Music ou « non affiché » |

> Pour le budget batterie **≤ +5 % / 30 min**, un seul test de 10 min ne suffit pas à le prouver. Deux options honnêtes : (a) répéter trois fois de suite le scénario pour cumuler 30 min, ou (b) me dire explicitement que vous acceptez un test de 10 min avec un budget ramené à **≤ +1,7 %**. Je n'extrapole aucun chiffre moi-même.

---

## 3. Analyse de vos valeurs

**Aucune valeur utilisateur n'a été reçue à ce jour.** Je ne rends donc aucun verdict GO / NO-GO, conformément à votre règle « si des valeurs manquent, liste-les sans conclure ».

### Valeurs manquantes (à me renvoyer)

1. Modèle d'appareil, version Android, taux d'écran, nom du fichier APK, réseau — P.1.
2. % « images lentes » initial (Diagnostics) + capture — P.2.
3. % batterie initial + capture — P.3.
4. Plateforme du lien public utilisé — P.4.
5. Confirmation onglet Historique ouvert — P.5.
6. Nombre de changements du pourcentage sur 10 s — M.3.
7. Nombre de rafraîchissements de la notification sur 10 s + capture du volet — M.4.
8. Résultat du téléchargement (OK / échec + rapport Journal) — M.5.
9. % « images lentes » final + capture — M.6.
10. % batterie final, delta %, capture — M.7.
11. Consommation B Music affichée ou « non affichée » — M.8.
12. Votre choix sur la durée du budget batterie : cumul 30 min ou budget ramené à 10 min.

### Ce que je pourrai conclure une fois les valeurs reçues

- **≤ 4 / s** : verdict sur M.3, croisé avec la preuve statique `ProgressThrottle(250)`.
- **≤ 1 / s** : verdict sur M.4, croisé avec le test JVM du limiteur 1000 ms — et seulement après le câblage effectif de la notification (étape 2 BUILD).
- **< 1 % images lentes** : verdict sur M.6 − P.2.
- **≤ +5 % / 30 min** : verdict sur M.7 − P.3, selon votre choix du point 12.

**Verdict global : en attente de vos valeurs. Aucune conclusion anticipée.**

---

## 4. Ce que je signale (sans modifier le code)

| # | Point | Gravité | Agent |
|---|---|---|---|
| 1 | Le limiteur de notification à 1000 ms existe dans les tests, mais `DownloadWorker.createForeground()` n'utilise pas encore `setProgress` ni le bus : la mesure M.4 mesurerait aujourd'hui l'ancienne notification. | Haut | BUILD |
| 2 | Le budget batterie (30 min) et le protocole (10 min) sont incohérents : à trancher avant mesure. | Moyen | PM |
| 3 | Le test de bout en bout avec serveur factice reste bloqué hors-ligne (aucun `mockwebserver` dans le cache Gradle). | Moyen | BUILD |

Aucune commande `adb` n'est utilisée ni demandée ; toutes les lectures de valeurs se font via l'interface du téléphone.
