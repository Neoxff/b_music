# B Music v1.2 — Fiche de mesure de performance

> Agent Lean B Music. Date : 14 novembre 2026.
> Objet : fiche à remplir sur **votre téléphone physique**. Aucun ADB, aucun émulateur.
> Pour chaque scénario : décrivez ce que vous faites, le résultat attendu, ce que vous observez (valeur brute), et ce que vous me renvoyez.
> **Ne prétendez jamais une mesure que vous n'avez pas prise.** Une case « non mesuré » est une information honnête.
> Quand vous me renvoyez la fiche complète : j'analyse, je donne un verdict GO / NO-GO par budget, et je liste les valeurs manquantes sans conclure à votre place.

---

## Informations de contexte (à remplir avant de commencer)

| Champ | Votre valeur |
|---|---|
| Appareil (modèle) | |
| Version d'Android | |
| RAM | |
| Écran (60 Hz / 120 Hz) | |
| Build de l'application (nom du fichier APK) | |
| Flag DOWNLOADER_ENABLED (off / on) | |
| Date de mesure | |

---

## Tableau de synthèse (à remplir)

| Scénario | Images lentes % | Pire durée | Durée moyenne | Mémoire pic | Délai | Appareil |
|---|---|---|---|---|---|---|
| (A) Cold start flag OFF | | | | | | |
| (A) Cold start flag ON | | | | | | |
| (B) Défilement historique 1 000 lignes 30 s | | | | | | |
| (C) Lien collé → aperçu Site 1 | | | | | | |
| (C) Lien collé → aperçu Site 2 | | | | | | |
| (C) Lien collé → aperçu Site 3 | | | | | | |
| (D) Téléchargement audio 10 min pendant lecture | | | | | Coupure : oui / non | |
| (E) 30 min téléchargements (Batterie) | | | | | | |
| (F) Mini-player → plein écran ×10 | | | | | | |

---

## Scénarios de mesure

### A) Cold start — flag OFF puis ON, ×5

| # | Flag | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|------|------------|-----------------|------------------|------------------|
| A.1 | OFF | Forcer l'arrêt (`am force-stop` ou gestionnaire d'apps), puis lancer l'application à froid | Temps jusqu'au premier écran interactif < 600 ms | | durée ms (mesure 1) |
| A.2 | OFF | Idem, mesure 2 | < 600 ms | | durée ms (mesure 2) |
| A.3 | OFF | Idem, mesure 3 | < 600 ms | | durée ms (mesure 3) |
| A.4 | OFF | Idem, mesure 4 | < 600 ms | | durée ms (mesure 4) |
| A.5 | OFF | Idem, mesure 5 | < 600 ms | | durée ms (mesure 5) |
| A.6 | ON | Forcer l'arrêt, activer le flag DOWNLOADER_ENABLED, lancer à froid | < 600 ms | | durée ms (mesure 1) |
| A.7 | ON | Idem, mesure 2 | < 600 ms | | durée ms (mesure 2) |
| A.8 | ON | Idem, mesure 3 | < 600 ms | | durée ms (mesure 3) |
| A.9 | ON | Idem, mesure 4 | < 600 ms | | durée ms (mesure 4) |
| A.10 | ON | Idem, mesure 5 | < 600 ms | | durée ms (mesure 5) |

**Comment mesurer le cold start :** utilisez `am start -W com.bmusic.app/.MainActivity` (ou l'activité de lancement) et notez la valeur `TotalTime` ou `ThisTime` affichée. Ou utilisez Macrobenchmark si configuré. Notez la valeur brute.

**Ce que vous me renvoyez :** les 10 durées brutes en ms, flag OFF et flag ON.

---

### B) Défilement de l'historique — 1 000 lignes, 30 s

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|------------|-----------------|------------------|------------------|
| B.1 | Générer 1 000 entrées dans l'historique (build debug) | L'historique contient 1 000 lignes avec miniatures locales | | OK / non généré |
| B.2 | Ouvrir l'onglet Historique | L'écran s'affiche sans saccade | | OK / saccade notée |
| B.3 | Défilé rapidement pendant 30 s (haut/bas) | Images lentes < 1 %, pas de frame > 16 ms (60 Hz) / 8 ms (120 Hz) | | % images lentes, pire frame ms |

**Comment mesurer les images lentes :** utilisez `adb shell dumpsys gfxinfo com.bmusic.app framestats` avant et après le défilement, ou Macrobenchmark `FrameTimingMetric`. Notez le % de janky frames et la pire durée de frame.

**Ce que vous me renvoyez :** % images lentes, pire durée de frame, durée moyenne si disponible.

---

### C) Analyse d'un lien : délai « collage → aperçu » ×5 sur 3 sites

| # | Site | URL publique | Délai mesuré | Votre observation | Ce à me renvoyer |
|---|------|-------------|--------------|------------------|------------------|
| C.1 | Site 1 (ex. YouTube) | | < 800 ms | | durée ms (mesure 1) |
| C.2 | Site 1 | | < 800 ms | | durée ms (mesure 2) |
| C.3 | Site 1 | | < 800 ms | | durée ms (mesure 3) |
| C.4 | Site 1 | | < 800 ms | | durée ms (mesure 4) |
| C.5 | Site 1 | | < 800 ms | | durée ms (mesure 5) |
| C.6 | Site 2 (ex. SoundCloud) | | < 800 ms | | durée ms (mesure 1) |
| C.7 | Site 2 | | < 800 ms | | durée ms (mesure 2) |
| C.8 | Site 2 | | < 800 ms | | durée ms (mesure 3) |
| C.9 | Site 2 | | < 800 ms | | durée ms (mesure 4) |
| C.10 | Site 2 | | < 800 ms | | durée ms (mesure 5) |
| C.11 | Site 3 (ex. Bandcamp) | | < 800 ms | | durée ms (mesure 1) |
| C.12 | Site 3 | | < 800 ms | | durée ms (mesure 2) |
| C.13 | Site 3 | | < 800 ms | | durée ms (mesure 3) |
| C.14 | Site 3 | | < 800 ms | | durée ms (mesure 4) |
| C.15 | Site 3 | | < 800 ms | | durée ms (mesure 5) |

**Comment mesurer :** chronométrez entre l'appui sur « Coller » (ou la validation du lien) et l'apparition de la carte d'aperçu (pochette, titre, auteur visible). Notez la durée en secondes ou ms.

**Ce que vous me renvoyez :** les 15 durées brutes, les 3 sites testés, les 3 URL publiques utilisées.

---

### D) Téléchargement d'un audio de 10 minutes pendant la lecture

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|------------|-----------------|------------------|------------------|
| D.1 | Lancer la lecture d'un morceau dans le lecteur | Lecture normale, coupure : non | | OK / coupure |
| D.2 | Lancer le téléchargement d'un audio de 10 minutes en parallèle | Téléchargement se met en file, lecture continue sans interruption | | coupure : oui / non |
| D.3 | Attendre la fin du téléchargement | État « Terminé » dans l'historique | | OK / échec |

**Ce que vous me renvoyez :** coupure oui / non, message d'erreur si échec.

---

### E) 30 minutes de téléchargements avec l'écran Batterie

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|------------|-----------------|------------------|------------------|
| E.1 | Ouvrir l'écran Batterie d'Android avant le test | Valeur de référence notée | | % batterie initial |
| E.2 | Lancer plusieurs téléchargements (audio/vidéo) pendant 30 minutes | Consommation ≤ +5 % vs baseline | | % batterie après 30 min, delta % |
| E.3 | Vérifier qu'aucune boucle d'animation ne tourne en arrière-plan | Pas de réveil inutile | | OK / anomalie notée |

**Ce que vous me renvoyez :** % batterie initial, % après 30 min, delta, note sur les animations arrière-plan.

---

### F) Transition mini-player → plein écran ×10

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|------------|-----------------|------------------|------------------|
| F.1 | Depuis le mini-player, appuyer pour ouvrir le plein écran | Transition < 300 ms, fluide | | durée ms (mesure 1) |
| F.2 | Idem, mesure 2 | < 300 ms | | durée ms (mesure 2) |
| F.3 | Idem, mesure 3 | < 300 ms | | durée ms (mesure 3) |
| F.4 | Idem, mesure 4 | < 300 ms | | durée ms (mesure 4) |
| F.5 | Idem, mesure 5 | < 300 ms | | durée ms (mesure 5) |
| F.6 | Idem, mesure 6 | < 300 ms | | durée ms (mesure 6) |
| F.7 | Idem, mesure 7 | < 300 ms | | durée ms (mesure 7) |
| F.8 | Idem, mesure 8 | < 300 ms | | durée ms (mesure 8) |
| F.9 | Idem, mesure 9 | < 300 ms | | durée ms (mesure 9) |
| F.10 | Idem, mesure 10 | < 300 ms | | durée ms (mesure 10) |

**Ce que vous me renvoyez :** les 10 durées brutes en ms.

---

## Budgets de référence (docs/lean/budgets-v3.md)

| Indicateur | Budget |
|---|---|
| APK arm64-v8a release | ≤ 12 Mo |
| Cold start (flag OFF et ON) | < 600 ms |
| Warm start | < 250 ms |
| Images lentes (janky) | < 1 % |
| Frame > 16 ms (60 Hz) / > 8 ms (120 Hz) | interdite |
| Mémoire pic RSS | < 200 Mo |
| Cache de miniatures bornée | ≤ 16 Mo |
| Batterie, 30 min téléchargements | ≤ +5 % |
| Délai « lien collé → aperçu » (Wi-Fi) | < 800 ms |
| Requêtes réseau à l'ouverture Historique | 0 |

---

## Analyse de vos rapports (à remplir par moi après réception)

| Budget | Valeur observée | Statut (GO / NO-GO / Valeur manquante) | Commentaire |
|---|---|---|---|
| APK ≤ 12 Mo | | | |
| Cold start OFF < 600 ms | | | |
| Cold start ON < 600 ms | | | |
| Warm start < 250 ms | | | |
| Images lentes < 1 % | | | |
| Mémoire pic < 200 Mo | | | |
| Batterie ≤ +5 % | | | |
| Délai collage → aperçu < 800 ms | | | |
| Requêtes réseau Historique = 0 | | | |

**Verdict global** : à déterminer après réception de vos valeurs.

**Valeurs manquantes** : à lister ci-dessous (sans conclusion de ma part) :
1. 
2. 
3.

---

## Règle d'arrêt

Tout dépassement d'un seul budget est **refusé**, quelle que soit la qualité perçue de l'UI, tant que la régression n'est pas corrigée et re-mesurée. Aucun dépassement non justifié n'est accepté.
