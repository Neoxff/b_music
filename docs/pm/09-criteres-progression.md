# B Music — Critères d'acceptation : progression de téléchargement

> Agent Product Manager B Music. Date : 17 novembre 2026.
> Prérequis : `docs/design/15-progression-telechargement.md`, `docs/pm/07-criteres-lien.md`, `docs/pm/08-criteres-historique.md`.
> Format : **Étant donné / Quand / Alors**, une assertion par ligne. Une story = un comportement testable = une seule raison de changer.
> Convention : **Priorité P0** = bloquant lancement · **P1** = majeur · **P2** = secondaire.
> **Effort** : S ≤ 1 j · M 2–5 j · L ≥ 5 j.
> **Preuve** : test JVM (unitaire/Robolectric/Compose) · capture (écran daté, appareil identifié) · journal exporté (DownloadTrace via Partager) · vérification manuelle (appareil, sans ADB).
> **Agent** : REL (Build) · LNA (Lean) · DSG (Designer) · SEC (Security) · PM.

---

## 0. Règle de preuve

Une story est **terminée** quand chacun de ses critères est **observé**. Un critère non observé est non satisfait. Aucun point n'est déclaré « validé » par analogie.

La **source unique** de progression est le bus `DownloadProgressBus` alimenté par `DownloadWorker` ; l'onglet Lien, l'onglet Historique et la notification lisent cette même source (SRC-1).

| Preuve | Signification |
|---|---|
| test JVM | Test unitaire/Robolectric/Compose vert, sans appareil ni réseau |
| capture | Capture d'écran datée avec identifiant d'appareil |
| journal exporté | Rapport DownloadTrace exporté par « Partager » de l'écran Journal |
| vérification manuelle | Appareil physique, scénario exécuté, résultat OK / partiel / refus |

---

## 1. Source unique et barre de progression (5 stories)

### SRC-1 — Une seule source pour trois consommateurs (P0 · M · REL · test JVM)

En tant que **Léa**, je veux que l'onglet Lien, l'onglet Historique et la notification lisent la même progression, afin de ne jamais voir trois chiffres différents.

- **Étant donné** un téléchargement en cours avec identifiant `42`
- **Quand** la progression est publiée par le worker dans le bus partagé
- **Alors** l'onglet Lien affiche le pourcentage issu du bus
- **Et** la ligne de l'onglet Historique pour l'identifiant `42` affiche le même pourcentage
- **Et** la notification affiche le même pourcentage

### BAR-1 — La barre avance pendant le téléchargement (P0 · S · REL · vérification manuelle)

En tant que **Léa**, je veux voir la barre avancer en continu, afin de savoir que le téléchargement progresse.

- **Étant donné** un téléchargement actif d'un fichier de 48,0 Mo
- **Quand** j'observe la carte de progression pendant 10 secondes
- **Alors** le remplissage de la barre augmente de façon visible et monotone
- **Et** la barre ne recule à aucun moment
- **Et** la transition entre deux mises à jour est interpolée sur 200–300 ms

### BAR-2 — Le pourcentage correspond à la barre (P0 · S · LNA · test JVM)

En tant que **Léa**, je veux que le chiffre affiché corresponde au remplissage, afin de pouvoir me fier à l'un ou à l'autre.

- **Étant donné** un snapshot avec 30,0 Mo téléchargés sur 48,0 Mo
- **Quand** la carte est rendue
- **Alors** le pourcentage affiché est `63 %`
- **Et** la largeur du remplissage correspond à 63 % de la piste (à 1 dp près)
- **Et** le calcul utilise un arrondi half-up (62,5 → 63), pas un arrondi au pair

### BAR-3 — Vitesse lissée, sans clignotement (P1 · M · LNA · test JVM)

En tant que **Léa**, je veux une vitesse lissée, afin que le texte ne saute pas à chaque paquet.

- **Étant donné** une série d'échantillons de transfert sur 3 secondes
- **Quand** la vitesse est calculée
- **Alors** elle est une moyenne mobile exponentielle, jamais une valeur instantanée
- **Et** elle tombe à `0 Ko/s` si aucun octet n'est reçu depuis 2 secondes
- **Et** le texte reste stable si la valeur affichée n'a pas changé (émission au plus toutes les 250 ms)

### BAR-4 — Formatage lisible (P1 · S · LNA · test JVM)

En tant que **Léa**, je veux une vitesse et une taille lisibles, afin de comprendre l'état du transfert.

- **Étant donné** une vitesse de 800 000 octets/s
- **Quand** le texte est formaté
- **Alors** il affiche `781 Ko/s`
- **Étant donné** une vitesse de 2 516 582 octets/s
- **Quand** le texte est formaté
- **Alors** il affiche `2,4 Mo/s` (virgule décimale française)
- **Et** la taille affiche `30,0 / 48,0 Mo`

### BAR-5 — Temps restant masqué si vitesse instable (P1 · S · LNA · test JVM)

En tant que **Léa**, je veux éviter un temps restant fantaisiste au démarrage.

- **Étant donné** moins de 2 secondes de données de vitesse
- **Quand** le snapshot est construit
- **Alors** le temps restant est masqué
- **Et** il apparaît seulement après 2 secondes de données stables
- **Et** il est masqué si la vitesse est nulle

---

## 2. Contrôles, TalkBack et pause/reprise (5 stories)

### CTL-1 — Boutons en icônes seulement (P0 · S · DSG · capture)

En tant que **Léa**, je veux des boutons sans libellé écrit, afin que la carte reste sobre.

- **Étant donné** un téléchargement en cours
- **Quand** la rangée de contrôles est affichée
- **Alors** les boutons Pause et Annuler sont des cercles de 40 dp en verre léger
- **Et** aucun texte de bouton n'est visible
- **Et** la zone tactile de chaque bouton est d'au moins 48 dp

### CTL-2 — Description TalkBack (P0 · S · DSG · test JVM)

En tant que **Camille**, utilisatrice TalkBack, je veux entendre l'action de chaque icône, afin de piloter le téléchargement.

- **Étant donné** le bouton en état « en cours »
- **Quand** TalkBack le parcourt
- **Alors** il annonce « Mettre en pause »
- **Étant donné** le bouton en état « en pause »
- **Quand** TalkBack le parcourt
- **Alors** il annonce « Reprendre »
- **Et** le bouton Annuler annonce « Annuler le téléchargement »

### CTL-3 — Icône qui change avec l'état (P1 · S · REL · test JVM)

En tant que **Léa**, je veux que l'icône reflète l'état, afin de savoir quelle action est disponible.

- **Étant donné** un téléchargement en cours
- **Quand** l'utilisateur appuie sur Pause
- **Alors** l'icône `ic_pause` est remplacée par `ic_resume`
- **Et** le changement s'accompagne d'une transition courte de 120 ms

### CTL-4 — Appui immédiat, état corrigé par le worker (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que l'appui réagisse immédiatement, afin de ne pas croire que le bouton ne fonctionne pas.

- **Étant donné** un téléchargement en cours
- **Quand** l'utilisateur appuie sur Pause
- **Alors** l'état affiché passe en « en pause » avant la confirmation du worker
- **Et** l'état affiché est remplacé par l'état réel du worker quand il arrive
- **Et** si le worker refuse, l'état précédent est restauré avec un message clair

### CTL-5 — Pause conserve, Reprendre repart de l'octet courant (P0 · M · REL · test JVM)

En tant que **Léa**, je veux ne pas perdre mon téléchargement en mettant en pause.

- **Étant donné** un téléchargement à 30,0 Mo sur 48,0 Mo
- **Quand** l'utilisateur met en pause puis reprend
- **Alors** le fichier partiel `.part` contient toujours 30,0 Mo
- **Et** la reprise émet une requête `Range: bytes=31457280-`
- **Et** le pourcentage ne repart jamais à zéro

---

## 3. Annulation, total inconnu et erreurs (5 stories)

### ANN-1 — Annuler demande confirmation (P0 · S · DSG · capture)

En tant que **Léa**, je veux confirmer l'annulation, afin d'éviter une suppression accidentelle.

- **Étant donné** un téléchargement en cours
- **Quand** l'utilisateur appuie sur le bouton Annuler
- **Alors** une petite feuille de confirmation s'affiche avec rayon 24 dp
- **Et** elle contient le texte « Annuler le téléchargement ? »
- **Et** elle propose « Annuler le téléchargement » et « Garder »

### ANN-2 — Annuler supprime le fichier partiel (P0 · M · REL · test JVM)

En tant que **Léa**, je veux que l'annulation nettoie le partiel, afin de ne pas laisser de fichier inutilisé.

- **Étant donné** la feuille de confirmation ouverte sur un téléchargement à 30,0 Mo
- **Quand** l'utilisateur confirme « Annuler le téléchargement »
- **Alors** l'entrée passe à l'état `CANCELED`
- **Et** le fichier `.part` correspondant n'existe plus
- **Et** l'annulation est tracée dans le journal d'étapes

### TOT-1 — Total inconnu : barre indéterminée, pas de faux pourcentage (P0 · M · REL · test JVM)

En tant que **Léa**, je veux qu'un flux sans `Content-Length` n'affiche pas de chiffre inventé.

- **Étant donné** un téléchargement dont le serveur n'envoie pas de `Content-Length`
- **Quand** le snapshot est construit
- **Alors** le pourcentage est `null` et aucun chiffre n'est affiché
- **Et** la barre est indéterminée sobre
- **Et** la taille affiche seulement les octets reçus
- **Et** la notification utilise `setProgress(100, 0, true)`

### ERR-1 — Réseau coupé : état erreur avec icône réessayer (P0 · M · REL · test JVM)

En tant que **Léa**, je veux comprendre pourquoi le téléchargement s'est arrêté et pouvoir réessayer.

- **Étant donné** un téléchargement en cours à 30,0 Mo
- **Quand** la connexion est coupée
- **Alors** l'entrée passe à l'état `FAILED`
- **Et** la barre passe en rouge atténé figée au point d'échec
- **Et** l'icône `ic_retry` remplace `ic_pause`
- **Et** l'action « Réessayer » est proposée avec un message clair
- **Et** l'échec est tracé dans le journal d'étapes

### FIN-1 — Terminé : barre pleine puis coche (P1 · S · DSG · capture)

En tant que **Léa**, je veux voir que le téléchargement est terminé.

- **Étant donné** un téléchargement à 100 %
- **Quand** le fichier est vérifié et enregistré
- **Alors** la barre est pleine puis remplacée par une coche
- **Et** l'état « Terminé · 48,0 Mo » s'affiche
- **Et** aucun bouton de contrôle n'est visible dans la carte de progression

---

## 4. Notification, file d'attente, rotation et flag (5 stories)

### NOT-1 — Notification cohérente avec l'app (P0 · M · REL · test JVM)

En tant que **Léa**, je veux que la notification affiche les mêmes chiffres que l'application.

- **Étant donné** un téléchargement à 30,0 / 48,0 Mo à 2,4 Mo/s
- **Quand** la notification est construite depuis le bus partagé
- **Alors** son texte est `63 % · 2,4 Mo/s`
- **Et** `setProgress(100, 63, false)` est utilisé
- **Et** `setOnlyAlertOnce(true)` est positionné
- **Et** `setOngoing(true)` est positionné pendant le téléchargement
- **Et** le canal est « Téléchargements » à faible importance (pas de son à chaque mise à jour)
- **Et** les actions Pause/Reprendre et Annuler sont présentes en icônes

### NOT-2 — Actions de notification sans libellé long (P1 · S · DSG · test JVM)

En tant que **Léa**, je veux des actions compactes dans la notification.

- **Étant donné** la notification d'un téléchargement en cours
- **Quand** les actions sont affichées
- **Alors** l'action Pause/Reprendre utilise une icône
- **Et** l'action Annuler utilise une icône
- **Et** si Android impose un libellé court d'un mot, celui-ci est « Pause », « Reprendre » ou « Annuler »
- **Et** cette limite système est documentée dans la fiche de recette

### QUE-1 — Plusieurs téléchargements en file (P1 · M · REL · test JVM)

En tant que **Léa**, je veux lancer plusieurs téléchargements sans les perdre.

- **Étant donné** trois liens analysés et « Tout télécharger » actionné
- **Quand** les trois téléchargements sont enqueués
- **Alors** un seul est à l'état `DOWNLOADING` à la fois (concurrence max 1)
- **Et** les deux autres sont à l'état `QUEUED`
- **Et** chaque ligne affiche sa propre progression issue du bus par identifiant
- **Et** aucune progression n'est mélangée entre deux identifiants

### ROT-1 — Rotation de l'écran sans perdre l'état (P1 · M · REL · capture)

En tant que **Léa**, je veux que la progression survive à une rotation, afin de ne pas perdre le fil.

- **Étant donné** un téléchargement à 63 % en portrait
- **Quand** l'appareil passe en paysage puis revient en portrait
- **Alors** le pourcentage affiché reste 63 % (à 1 % près, hors progression naturelle)
- **Et** l'état lecture/pause est conservé
- **Et** la carte conserve la largeur max 560 dp et les marges 20 dp

### FLG-1 — Flag désactivé : rien d'affiché (P0 · S · REL · test JVM)

En tant que **Léa**, je veux qu'aucune progression n'apparaisse quand le téléchargeur est désactivé.

- **Étant donné** `Preferences.DOWNLOADER_ENABLED = false`
- **Quand** l'application démarre ou revient au premier plan
- **Alors** aucun onglet Télécharger n'est visible
- **Et** aucune carte de progression n'est affichée
- **Et** aucune notification de téléchargement n'est émise
- **Et** aucune requête réseau de téléchargement n'est effectuée

---

## 5. Récapitulatif

| Bloc | Stories | Critères | Observables | Preuve dominante |
|---|---|---|---|---|
| 1. Source unique et barre | SRC-1, BAR-1…BAR-5 (6) | 29 | 18 | test JVM / vérification manuelle |
| 2. Contrôles, TalkBack, pause/reprise | CTL-1…CTL-5 (5) | 27 | 16 | capture / test JVM |
| 3. Annulation, total inconnu, erreurs | ANN-1, ANN-2, TOT-1, ERR-1, FIN-1 (5) | 28 | 18 | test JVM / capture |
| 4. Notification, file, rotation, flag | NOT-1, NOT-2, QUE-1, ROT-1, FLG-1 (5) | 33 | 19 | test JVM / capture |
| **Total** | **21 stories** | **117 critères** | **71 observables** | — |

**Règle de non-invention** : aucun chiffre de performance ni résultat de test n'est affirmé ici. Les valeurs `63 %`, `30,0 / 48,0 Mo`, `2,4 Mo/s` sont des **valeurs d'exemple** de critères, pas des mesures. Aucune mesure sur appareil n'est prétendue.

**Dépendances** : la réalisation des stories marquées « test JVM » suppose le câblage du worker au bus (étape 2 du plan BUILD, branche `feat/dl-progression`) ; les stories « capture » supposent les captures Paparazzi/Roborazzi produites par Build puis exécutées par l'utilisateur sans ADB.

---
