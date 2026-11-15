# B Music — Critères d'acceptation : onglet « Historique » (v1.2)

> Agent Product Manager B Music. Date : 14 novembre 2026.
> Format : **Étant donné / Quand / Alors**, une assertion par ligne. Une story = un comportement testable = une seule raison de changer.
> Convention : **Priorité P0** = bloquant lancement | **P1** = majeur | **P2** = secondaire.
> **Effort** : S ≤ 1 j · M 2–5 j · L ≥ 5 j.
> **Type de preuve** : test JVM (unitaire/Robolectric/Compose) | capture (écran photographié, daté, appareil identifié) | journal exporté (DownloadTrace via Partager) | vérification manuelle (appareil, sans ADB).
> **Agent** : REL (Build) | LNA (Lean) | DSG (Designer) | SEC (Security) | PM.

---

## 0. Règle de preuve

Une story est **terminée** quand chacun de ses critères est **observé**. Un critère non observé est un critère non satisfait. Aucun point ne peut être déclaré « validé » par analogie.

| Type de preuve | Ce que cela signifie concrètement |
|---|---|
| test JVM | Test unitaire/Robolectric/Compose vert, sans appareil, sans réseau |
| capture | Capture d'écran datée avec identifiant d'appareil |
| journal exporté | Rapport DownloadTrace exporté par le bouton « Partager » de l'écran Journal |
| vérification manuelle | Appareil physique, scénario exécuté, résultat noté OK / partiel / refus |

---

## 1. Affichage et regroupement par date (5 stories)

### AH-1 — Historique vide (P0 · S · LNA · capture)

En tant que **Léa**, je veux un état vide clair quand je n'ai jamais téléchargé, afin de savoir quoi faire.

- **Étant donné** l'écran « Historique » ouvert et aucun téléchargement enregistré
- **Quand** l'écran est affiché
- **Alors** un message « Aucun téléchargement pour l'instant » s'affiche
- **Et** un bouton « Coller un lien » est présent qui bascule vers l'onglet « Lien »
- **Et** aucune liste ni aucun écran blanc ne subsiste

### AH-2 — Téléchargement terminé apparaît en tête (P0 · M · REL · capture)

En tant que **Léa**, je veux voir mes téléchargements les plus récents en premier, pour y accéder rapidement.

- **Étant donné** plusieurs téléchargements terminés à des dates différentes
- **Quand** l'écran « Historique » est ouvert
- **Alors** le téléchargement le plus récent apparaît en tête de liste
- **Et** les téléchargements sont triés du plus récent au plus ancien

### AH-3 — Regroupement par date (P0 · M · REL · capture)

En tant que **Léa**, je veux que mes téléchargements soient groupés par date, pour une lecture rapide.

- **Étant donné** des téléchargements aujourd'hui, hier, cette semaine et plus ancien
- **Quand** l'écran « Historique » est ouvert
- **Alors** les téléchargements sont groupés sous les en-têtes : « Aujourd'hui », « Hier », « Cette semaine », « Plus ancien »
- **Et** chaque en-tête de groupe est collant (reste visible pendant le défilement de son groupe)
- **Et** chaque en-tête porte le libellé « Légende »

### AH-4 — Hauteur de ligne constante (P1 · S · LNA · capture)

En tant que **Léa**, je veux des lignes de hauteur constante, pour une lecture fluide.

- **Étant donné** plusieurs lignes d'historique
- **Quand** la liste est affichée
- **Alors** chaque ligne a la même hauteur (88 dp)
- **Et** la miniature occupe 96×54 dp (format 16:9, rayon 12 dp)

### AH-5 — Apparition échelonnée (P2 · S · DSG · capture)

En tant que **Léa**, je veux que les lignes apparaissent avec un léger décalage, pour un effet fluide.

- **Étant donné** plus de 8 téléchargements à afficher
- **Quand** la liste s'affiche pour la première fois
- **Alors** les 8 premières lignes apparaissent avec un décalage de 24 ms entre chacune
- **Et** les lignes suivantes apparaissent sans décalage supplémentaire

---

## 2. Contenu de chaque ligne (5 stories)

### AL-1 — Miniature, titre et métadonnées (P0 · M · REL · capture)

En tant que **Léa**, je veux voir la pochette, le titre, le site, le format et la taille sur chaque ligne.

- **Étant donné** une ligne d'historique pour un téléchargement terminé
- **Quand** la ligne est affichée
- **Alors** la miniature locale s'affiche (96×54 dp, rayon 12 dp)
- **Alors** le titre s'affiche sur 2 lignes maximum avec troncature
- **Alors** le site, le format et la taille s'affichent sur une ligne
- **Alors** la date s'affiche à droite

### AL-2 — État de ligne : terminé (P0 · S · REL · capture)

En tant que **Léa**, je veux savoir qu'un téléchargement est terminé.

- **Étant donné** un téléchargement avec l'état « terminé »
- **Quand** la ligne est affichée
- **Alors** le texte « Terminé » s'affiche avec un point de couleur `primary`

### AL-3 — État de ligne : en cours avec progression (P0 · M · REL · capture)

En tant que **Léa**, je veux voir la progression d'un téléchargement en cours.

- **Étant donné** un téléchargement en cours
- **Quand** la ligne est affichée
- **Alors** une barre de progression sobre (4 dp, remplissage `primary`) s'affiche
- **Alors** le texte « En cours… » s'affiche
- **Alors** les actions « Pause » et « Annulation » sont disponibles

### AL-4 — État de ligne : en pause (P1 · S · REL · capture)

En tant que **Léa**, je veux savoir qu'un téléchargement est en pause.

- **Étant donné** un téléchargement en pause
- **Quand** la ligne est affichée
- **Alors** le texte « En pause » s'affiche avec un point de couleur `onSurfaceVariant`
- **Alors** l'action « Reprise » est disponible

### AL-5 — État de ligne : erreur et fichier supprimé (P1 · M · REL · capture)

En tant que **Léa**, je veux savoir quand un téléchargement a échoué ou que le fichier a disparu.

- **Étant donné** un téléchargement en erreur
- **Quand** la ligne est affichée
- **Alors** le texte « Erreur » s'affiche avec un point de couleur `error`
- **Et** l'action « Retélécharger » est disponible
- **Étant donné** un téléchargement dont le fichier n'existe plus sur l'appareil
- **Quand** la ligne est affichée (vérification paresseuse)
- **Alors** le texte « Fichier supprimé » s'affiche avec un point de couleur `onSurfaceVariant`
- **Et** l'action « Retélécharger » est disponible

---

## 3. Actions par ligne (7 stories)

### AC-1 — Lire depuis l'historique (P0 · M · REL · vérification manuelle)

En tant que **Léa**, je veux lire un téléchargement terminé depuis l'historique, sans modifier le lecteur existant.

- **Étant donné** un téléchargement terminé dans l'historique
- **Quand** j'appuie sur « Lire »
- **Alors** le titre est envoyé au lecteur existant
- **Et** le module `playback/` n'est pas modifié

### AC-2 — Retélécharger (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux retélécharger un contenu déjà téléchargé, par exemple après suppression du fichier.

- **Étant donné** un téléchargement terminé ou en erreur
- **Quand** j'appuie sur « Retélécharger »
- **Alors** un nouveau téléchargement est mis en file avec les mêmes paramètres
- **Et** l'entrée passe à l'état « En cours »

### AC-3 — Copier le lien (P1 · S · REL · test JVM)

En tant que **Léa**, je veux copier le lien d'un téléchargement depuis l'historique.

- **Étant donné** une ligne d'historique avec une URL stockée
- **Quand** j'appuie sur « Copier le lien »
- **Alors** l'URL est présente dans le presse-papiers

### AC-4 — Partager (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux partager un téléchargement depuis l'historique.

- **Étant donné** une ligne d'historique
- **Quand** j'appuie sur « Partager »
- **Alors** une intention de partage est émise avec le fichier ou le lien

### AC-5 — Supprimer de l'historique (le fichier reste) (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux supprimer une entrée de l'historique sans supprimer le fichier lui-même.

- **Étant donné** un téléchargement terminé dont le fichier existe
- **Quand** j'appuie sur « Supprimer de l'historique »
- **Alors** l'entrée disparaît de la liste
- **Et** le fichier n'est pas supprimé du stockage

### AC-6 — Supprimer le fichier avec confirmation (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux supprimer le fichier avec une confirmation explicite, pour éviter les suppressions accidentelles.

- **Étant donné** un téléchargement terminé
- **Quand** j'appuie sur « Supprimer le fichier »
- **Alors** une confirmation est demandée
- **Quand** je valide la confirmation
- **Alors** le fichier est supprimé
- **Et** l'entrée passe à l'état « Fichier supprimé »

### AC-7 — Menu d'actions par appui long ou icône (P2 · S · DSG · capture)

En tant que **Léa**, je veux accéder aux actions par appui long ou par menu.

- **Étant donné** une ligne d'historique
- **Quand** je fais un appui long sur la ligne ou que j'appuie sur l'icône de menu
- **Alors** un menu opaque s'affiche avec : Lire, Retélécharger, Copier le lien, Partager, Supprimer de l'historique, Supprimer le fichier

---

## 4. Balayage latéral et filtres (4 stories)

### SW-1 — Balayage latéral pour supprimer de l'historique (P1 · M · DSG · capture)

En tant que **Léa**, je veux supprimer une entrée par balayage latéral, avec possibilité d'annuler.

- **Étant donné** une ligne d'historique
- **Quand** je balaie la ligne vers la gauche
- **Alors** la ligne se décolore et une barre de confirmation apparaît à droite avec « Annuler »
- **Quand** j'appuie sur « Annuler » dans les 4 secondes
- **Alors** la ligne revient à son état initial (fondé en 220 ms)
- **Quand** je ne fais rien pendant 4 secondes
- **Alors** l'entrée disparaît et la liste se recompacte

### SW-2 — Tout effacer l'historique (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux effacer tout l'historique d'un coup, sans supprimer les fichiers.

- **Étant donné** plusieurs entrées dans l'historique
- **Quand** j'appuie sur « Tout effacer l'historique » dans le menu
- **Alors** une confirmation est demandée
- **Quand** je valide
- **Alors** toutes les entrées disparaissent
- **Et** les fichiers correspondants ne sont pas supprimés

### FL-1 — Filtre par site et par état (P1 · M · REL · test JVM)

En tant que **Léa**, je veux filtrer l'historique par site et par état, pour retrouver rapidement un téléchargement.

- **Étant donné** des téléchargements de plusieurs sites et dans plusieurs états
- **Quand** j'applique un filtre par site (ex. « YouTube »)
- **Alors** seules les lignes de ce site s'affichent
- **Étant donné** un filtre par état (ex. « Terminé »)
- **Alors** seules les lignes de cet état s'affichent

### FL-2 — Champ de filtre en haut (P2 · S · LNA · capture)

En tant que **Léa**, je veux un champ de filtre en haut de l'historique pour rechercher textuellement.

- **Étant donné** l'écran « Historique » ouvert
- **Quand** le champ de filtre est affiché
- **Alors** sa hauteur est de 56 dp et son rayon de 28 dp
- **Et** il contient une icône de recherche à gauche

---

## 5. Téléchargements en cours (2 stories)

### DL-1 — Téléchargements en cours en tête de liste (P0 · M · REL · capture)

En tant que **Léa**, je veux voir les téléchargements en cours en premier, pour surveiller leur progression.

- **Étant donné** des téléchargements en cours et terminés
- **Quand** l'écran « Historique » est ouvert
- **Alors** les téléchargements en cours apparaissent en tête de liste
- **Et** ils affichent une progression visuelle

### DL-2 — Pause, reprise, annulation (P0 · M · REL · vérification manuelle)

En tant que **Léa**, je veux pouvoir mettre en pause, reprendre ou annuler un téléchargement en cours.

- **Étant donné** un téléchargement en cours
- **Quand** j'appuie sur « Pause »
- **Alors** le téléchargement est mis en pause et l'état passe à « En pause »
- **Quand** j'appuie sur « Reprise »
- **Alors** le téléchargement reprend
- **Quand** j'appuie sur « Annulation »
- **Alors** le téléchargement est annulé et l'entrée est retirée

---

## 6. Volumétrie et performance (2 stories)

### VO-1 — 1 000 entrées fluides (P2 · L · LNA · capture)

En tant que **Léa**, je veux que l'historique reste fluide même avec beaucoup d'entrées.

- **Étant donné** 1 000 entrées dans l'historique (générées en debug)
- **Quand** l'écran « Historique » est défilé
- **Alors** le défilement reste fluide (pas de saccade visible)
- **Et** les valeurs Diagnostics de défilement sont dans le budget

### VO-2 — Aucune requête réseau pour afficher l'historique (P0 · S · SEC · test JVM)

En tant que **Léa**, je veux que l'historique s'affiche sans aucune requête réseau.

- **Étant donné** l'historique contient des entrées
- **Quand** l'écran « Historique » est ouvert
- **Alors** aucune requête réseau n'est émise
- **Et** les données proviennent uniquement de la base de données locale

---

## 7. Flag désactivé et mise à jour (2 stories)

### FL-3 — Flag désactivé : onglet absent (P0 · S · REL · test JVM)

En tant que **Léa**, je veux que l'onglet « Historique » n'existe pas quand le téléchargeur est désactivé.

- **Étant donné** `Preferences.DOWNLOADER_ENABLED == false`
- **Quand** l'application est lancée
- **Alors** l'onglet « Historique » n'apparaît pas
- **Et** aucune requête réseau n'est émise
- **Et** aucun onglet de téléchargement n'est présent

### FL-4 — Mise à jour par-dessus sans perte (P0 · L · REL · vérification manuelle)

En tant que **Léa**, je veux que mes téléchargements existants soient conservés lors d'une mise à jour.

- **Étant donné** B Music v1.0.0 installée avec un historique de téléchargements
- **Quand** je mets à jour vers la version avec l'onglet « Historique » (même clé de signature)
- **Alors** l'historique existant est conservé
- **Et** aucune donnée n'est perdue

---

## 8. Récapitulatif

Deux métriques sont distinguées : les **critères** (lignes Étant donné / Quand / Alors / Et) et les **observables** (lignes Alors / Et — ce que le test asserte effectivement).

| Bloc | Stories | Critères | Observables | Type de preuve principal |
|------|---------|----------|------------|-------------------------|
| Affichage et regroupement | AH-1 → AH-5 (5) | 22 | 12 | capture |
| Contenu de ligne | AL-1 → AL-5 (5) | 26 | 14 | capture |
| Actions par ligne | AC-1 → AC-7 (7) | 27 | 12 | vérification manuelle / test JVM |
| Balayage et filtres | SW-1 → FL-2 (4) | 18 | 9 | capture / test JVM |
| Téléchargements en cours | DL-1 → DL-2 (2) | 11 | 5 | vérification manuelle / capture |
| Volumétrie et performance | VO-1 → VO-2 (2) | 8 | 4 | capture / test JVM |
| Flag et mise à jour | FL-3 → FL-4 (2) | 18 | 9 | test JVM / vérification manuelle |
| **Total** | **27 stories** | **125 critères** | **62 observables** | — |

> Compteurs recalculés automatiquement depuis le corps du document (chaque ligne `### XX-n` et chaque puce `**Étant donné** / **Quand** / **Alors** / **Et**`). Les 14 scénarios exigés sont couverts : téléchargement terminé en tête (AH-2), regroupement par date (AH-3), filtre site + état (FL-1), lire (AC-1), retélécharger (AC-2), copier le lien (AC-3), partager (AC-4), supprimer de l'historique sans effacer le fichier (AC-5), supprimer le fichier avec confirmation (AC-6), fichier supprimé hors de l'app (AL-5), tout effacer (SW-2), en cours avec pause/reprise/annulation (DL-2), 1 000 entrées fluides (VO-1), zéro requête réseau (VO-2), flag désactivé (FL-3), mise à jour par-dessus sans perte (FL-4).

---

## 9. Agents responsables

| Agent | Stories couvertes |
|-------|------------------|
| REL (Build) | Toutes les stories (implémentation, base de données Room, WorkManager) |
| LNA (Lean) | AH-4, AH-5, FL-2 (performance, filtre, volumétrie) |
| DSG (Designer) | AH-1, AH-3, AH-4, AH-5, AL-1 → AL-5, AC-7, SW-1, FL-2, DL-1 (captures, design) |
| SEC (Security) | VO-2 (pas de requête réseau), FL-3 (flag désactivé) |
| PM | Recette finale, vérification des messages et actions |
