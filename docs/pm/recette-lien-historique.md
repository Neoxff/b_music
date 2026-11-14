# B Music — Fiche de recette « Lien + Historique » à remplir sur téléphone

> Agent Product Manager B Music. Date : 14 novembre 2026.
> Objet : fiche de test à remplir **sur votre téléphone physique**. Aucun ADB, aucun émulateur.
> Pour chaque point : décrivez ce que vous faites, le résultat attendu, ce que vous observez (OK / partiel / refus), le message affiché, et ce que vous me renvoyez.
> **Ne prétendez jamais qu'un site marche sans votre résultat.** Si vous n'avez pas testé un site, laissez la case vide et notez « non testé ».
> Quand vous me renvoyez la fiche complète : j'analyse, je remonte les bugs (gravité, reproduction) et je transmets à Build avec un verdict GO / NO-GO. Je ne corrige pas le code moi-même.

---

## Informations de contexte (à remplir avant de commencer)

| Champ | Votre valeur |
|---|---|
| Appareil (modèle) | |
| Version d'Android | |
| Build de l'application (nom du fichier APK) | |
| Date de test | |
| Réseau utilisé (Wi-Fi / 4G / 5G / mode avion) | |
| Flag DOWNLOADER_ENABLED (on / off) | |

---

## 0) Lancement (froid, redémarrage, flag on/off)

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|---|---|---|---|
| 0.1 | Installer l'APK sur un téléphone vierge (ou désinstaller puis réinstaller) | L'application s'installe sans erreur | | OK / échec + message |
| 0.2 | Lancer l'application à froid (premier lancement) | L'application s'ouvre sur l'écran d'accueil, aucun crash | | OK / crash + log |
| 0.3 | Fermer l'application (bouton retour ou gestionnaire d'apps) puis relancer | L'application se relance, écran d'accueil | | OK / échec |
| 0.4 | Flag OFF : vérifier que l'entrée « Télécharger » n'est pas dans la barre de navigation | Aucune entrée « Télécharger », menu identique à v1.0.0 | | Présent / absent |
| 0.5 | Flag ON : vérifier que l'entrée « Télécharger » apparaît dans la barre de navigation | Entrée « Télécharger » visible en dernière position | | Présent / absent |

---

## 1) Onglet Lien : tableau site × cas

Pour chaque site, collez une **vraie URL publique** dans le champ, lancez l'analyse, et notez :

- **OK** : aperçu affiché, téléchargement possible
- **partiel** : aperçu partiel ou format limité
- **refus** : message d'erreur (lequel ?)
- **journal exporté** : si échec, exportez le journal (bouton « Partager » dans l'écran Journal) et notez l'étape qui a échoué

| Site | Cas testé | Vraie URL publique | Résultat (OK / partiel / refus) | Message affiché | Étape journal si échec |
|------|-----------|-------------------|---------------------------------|-----------------|----------------------|
| YouTube | Vidéo publique | | | | |
| YouTube | Audio uniquement | | | | |
| YouTube | Court format (Shorts) | | | | |
| YouTube | Lien raccourci (youtu.be) | | | | |
| YouTube | Lien mobile (m.youtube.com) | | | | |
| YouTube | Contenu privé | | | | |
| YouTube | Connexion requise | | | | |
| TikTok | Vidéo publique | | | | |
| TikTok | Lien raccourci | | | | |
| Instagram | Reel public | | | | |
| Instagram | Lien mobile | | | | |
| Facebook | Vidéo publique | | | | |
| Facebook | Lien mobile | | | | |
| X | Vidéo publique | | | | |
| X | Lien raccourci (t.co) | | | | |
| Snapchat | Lien public | | | | |
| Snapchat | Lien mobile | | | | |
| SoundCloud | Piste publique | | | | |
| SoundCloud | Lien mobile | | | | |
| Bandcamp | Album/piste public | | | | |
| Vimeo | Vidéo publique | | | | |
| Dailymotion | Vidéo publique | | | | |
| Reddit | Vidéo publique | | | | |
| Twitch | Clip ou chaîne | | | | |

**Ce que vous me renvoyez pour cette section :**
- Le tableau complété ci-dessus
- Pour chaque « refus » : le message exact affiché et l'étape du journal d'étapes
- Pour chaque « partiel » : ce qui fonctionne et ce qui manque

---

## 2) Zone de saisie centrée : captures

Prenez des captures d'écran (bouton power + volume bas) et notez ce que vous observez.

| # | Capture | Résultat attendu | Votre observation | Fichier à me renvoyer |
|---|---------|-----------------|-----------------|----------------------|
| 2.1 | Portrait, clavier fermé | Bloc de saisie centré horizontalement ET verticalement, largeur max 560 dp | | capture_2.1.png |
| 2.2 | Paysage, clavier fermé | Même centrage, largeur max 560 dp | | capture_2.2.png |
| 2.3 | Clavier ouvert (taper dans le champ) | Bloc de saisie reste visible, pas recouvert par le clavier | | capture_2.3.png |
| 2.4 | Champ vide, hauteur 56 dp, rayon 28 dp | Champ en forme « pilule », bouton « Coller » à droite | | capture_2.4.png |

**Ce que vous me renvoyez :** les 4 captures d'écran, avec la date et l'heure de prise.

---

## 3) Téléchargement complet : fichier dans la bibliothèque, lecture

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|---|---|---|---|
| 3.1 | Coller un lien YouTube public dans le champ « Lien » | Champ pré-rempli, lien valide | | URL utilisée |
| 3.2 | Appuyer sur « Analyser » | Aperçu apparaît (pochette, titre, auteur, durée, badge YouTube) | | capture_3.2.png |
| 3.3 | Choisir Audio ou Vidéo et une qualité | Sélection prise en compte | | format choisi |
| 3.4 | Appuyer sur « Télécharger » | Téléchargement mis en file, état « En cours » dans l'historique | | capture_3.4.png |
| 3.5 | Attendre la fin du téléchargement | État « Terminé », fichier présent dans l'historique | | capture_3.5.png |
| 3.6 | Appuyer sur « Lire » depuis l'historique | Le titre est envoyé au lecteur existant, lecture démarre | | OK / échec + message |

**Ce que vous me renvoyez :** captures 3.2, 3.4, 3.5 + confirmation lecture (OK/échec).

---

## 4) Onglet Historique : tous les scénarios du PM 08

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|---|---|---|---|
| 4.1 | Ouvrir l'onglet « Historique » | Onglet accessible, liste affichée (ou vide si aucun téléchargement) | | OK / échec |
| 4.2 | Vérifier le regroupement par date | En-têtes « Aujourd'hui », « Hier », « Cette semaine », « Plus ancien » | | capture_4.2.png |
| 4.3 | Filtrer par site | Seules les lignes du site sélectionné s'affichent | | site filtré + résultat |
| 4.4 | Filtrer par état | Seules les lignes de l'état sélectionné s'affichent | | état filtré + résultat |
| 4.5 | Appuyer sur « Lire » d'un téléchargement terminé | Lecture démarre dans le lecteur existant | | OK / échec |
| 4.6 | Appuyer sur « Retélécharger » | Nouveau téléchargement en file | | OK / échec |
| 4.7 | Appuyer sur « Copier le lien » | L'URL est dans le presse-papiers | | OK / échec |
| 4.8 | Appuyer sur « Partager » | Intention de partage émise | | OK / échec |
| 4.9 | Appuyer sur « Supprimer de l'historique » | Entrée disparaît, fichier conservé | | OK / échec |
| 4.10 | Appuyer sur « Supprimer le fichier » + confirmer | Fichier supprimé, entrée passe en « Fichier supprimé » | | OK / échec |
| 4.11 | Faire un balayage latéral sur une ligne | Barre de confirmation « Annuler » apparaît | | capture_4.11.png |
| 4.12 | Appuyer sur « Annuler » dans les 4 secondes | Ligne revient à son état initial | | OK / échec |
| 4.13 | Appuyer sur « Tout effacer l'historique » + confirmer | Toutes les entrées disparaissent, fichiers conservés | | OK / échec |
| 4.14 | Téléchargement en cours : appuyer sur « Pause » | Téléchargement mis en pause | | OK / échec |
| 4.15 | Téléchargement en pause : appuyer sur « Reprise » | Téléchargement reprend | | OK / échec |
| 4.16 | Téléchargement en cours : appuyer sur « Annulation » | Téléchargement annulé, entrée retirée | | OK / échec |

**Ce que vous me renvoyez :** captures 4.2 et 4.11 + résultats OK/échec pour chaque ligne.

---

## 5) Régression lecture et liquid léger

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|---|---|---|---|
| 5.1 | Lire un morceau depuis la bibliothèque (pas depuis l'historique) | Lecture normale, aucun écran vide | | OK / échec |
| 5.2 | Vérifier qu'aucun effet vitreux/blur n'apparaît sur les nouveaux écrans | Interface opaque, sans flou | | OK / effet vitreux détecté |
| 5.3 | Vérifier que les écrans existants (Accueil, Lecteur, Bibliothèque) sont inchangés | Même aspect qu'avant la mise à jour | | OK / différence notée |

**Ce que vous me renvoyez :** OK / échec pour chaque ligne + description de toute différence visuelle.

---

## 6) Fluidité (valeurs Diagnostics)

Ouvrez l'écran « Diagnostics » (build debug uniquement) et notez les valeurs.

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|---|---|---|---|
| 6.1 | Défilez rapidement l'historique (si > 20 entrées) | Pas de saccade visible, < 1 % images lentes | | valeur Diagnostics « images lentes » |
| 6.2 | Pendant un téléchargement, vérifiez Diagnostics | Pas de saccade, < 1 % images lentes | | valeur Diagnostics « images lentes » |
| 6.3 | Notez la mémoire (RSS) au démarrage | Dans le budget (< 200 Mo) | | valeur RSS Mo |
| 6.4 | Notez le démarrage à froid | < 600 ms | | valeur ms |

**Ce que vous me renvoyez :** les 4 valeurs Diagnostics brutes, telles qu'affichées.

---

## 7) Mise à jour par-dessus

| # | Quoi faire | Résultat attendu | Votre observation | Ce à me renvoyer |
|---|---|---|---|---|
| 7.1 | Installer l'ancienne version (v1.0.0) si disponible | Ancienne version fonctionnelle | | OK / non disponible |
| 7.2 | Ajouter quelques téléchargements dans l'historique | Entrées présentes | | nombre d'entrées |
| 7.3 | Installer la nouvelle version par-dessus (même clé de signature) | Mise à jour réussie, historique conservé | | historique conservé / perdu |

**Ce que vous me renvoyez :** résultat de la mise à jour (conservé / perdu) + nombre d'entrées avant/après.

---

## Ce que vous me renvoyez (résumé)

Lorsque vous avez rempli la fiche, envoyez-moi :

1. **Informations de contexte** (appareil, version Android, build, date, réseau, flag)
2. **Tableau 1) complété** (site × cas avec résultats et messages)
3. **Captures 2)** (4 fichiers image)
4. **Résultats 3)** (captures + confirmation lecture)
5. **Résultats 4)** (captures 4.2, 4.11 + OK/échec pour chaque scénario)
6. **Résultats 5)** (OK/échec + différences visuelles)
7. **Valeurs Diagnostics 6)** (4 valeurs brutes)
8. **Résultat 7)** (mise à jour conservée / perdue)

---

## Quand je renvoie la fiche : analyse et verdict

Dès réception, je procède ainsi :

1. **Analyse** : je compare vos observations aux résultats attendus de chaque point.
2. **Bugs** : pour chaque écart, je note la gravité (bloquant / majeur / mineur), les étapes de reproduction, et le message exact affiché.
3. **Verdict** :
   - **GO** : tous les points critiques (P0) sont OK, aucun bug bloquant. Je transmets à Build pour préparation de release.
   - **NO-GO** : au moins un point critique (P0) échoue ou présente un bug bloquant. Je liste les corrections à apporter avant nouvelle recette.
4. **Transmission à Build** : je ne corrige pas le code. Je transmets la liste des bugs avec gravité et reproduction à l'agent BUILD.

**Rappel important :** ne prétendez jamais qu'un site fonctionne si vous ne l'avez pas testé. Une case « non testé » est une information honnête et utile, pas un échec.
