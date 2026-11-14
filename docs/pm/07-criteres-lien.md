# B Music — Critères d'acceptation : onglet « Lien » (v1.2)

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

## 1. Zone de saisie — centrage et affichage (5 stories)

### ZS-1 — Centrage portrait, clavier fermé (P0 · S · LNA · capture)

En tant que **Léa**, je veux que la zone de saisie soit centrée horizontalement et verticalement, afin qu'elle soit immédiatement visible au lancement.

- **Étant donné** l'écran « Lien » ouvert, téléphone, portrait, clavier fermé
- **Quand** l'écran est affiché
- **Alors** le bloc de saisie (champ + boutons) est centré horizontalement dans l'espace disponible
- **Et** le bloc de saisie est centré verticalement dans l'espace disponible
- **Et** la largeur du bloc ne dépasse pas 560 dp

### ZS-2 — Centrage paysage, clavier fermé (P0 · S · LNA · capture)

En tant que **Léa**, je veux le même centrage en paysage, afin de ne pas perdre la zone de saisie.

- **Étant donné** l'écran « Lien » ouvert, téléphone, paysage, clavier fermé
- **Quand** l'écran est affiché
- **Alors** le bloc de saisie est centré horizontalement et verticalement
- **Et** la largeur du bloc ne dépasse pas 560 dp

### ZS-3 — Centrage tablette, clavier fermé (P0 · S · LNA · capture)

En tant que **Léa**, je veux le même centrage sur tablette, afin que l'écran soit cohérent sur tous les formats.

- **Étant donné** l'écran « Lien » ouvert, tablette, paysage, clavier fermé
- **Quand** l'écran est affiché
- **Alors** le bloc de saisie est centré horizontalement et verticalement
- **Et** la largeur du bloc ne dépasse pas 560 dp

### ZS-4 — Clavier ouvert, zone visible (P0 · S · LNA · capture)

En tant que **Léa**, je veux que la zone de saisie reste visible quand le clavier s'ouvre, afin de pouvoir continuer à taper.

- **Étant donné** l'écran « Lien » ouvert, téléphone, portrait, thème sombre
- **Quand** le clavier s'affiche
- **Alors** le bloc de saisie n'est pas recouvert par le clavier
- **Et** le bloc de saisie reste au moins partiellement visible (imePadding appliqué)
- **Et** le champ de saisie reste éditable

### ZS-5 — Hauteur du champ et marge (P1 · S · LNA · capture)

En tant que **Léa**, je veux que le champ de saisie ait la bonne hauteur et les bonnes marges, pour un confort de lecture.

- **Étant donné** l'écran « Lien » ouvert
- **Quand** le champ de saisie est affiché
- **Alors** la hauteur du champ est de 56 dp
- **Et** les marges latérales du champ sont de 20 dp
- **Et** le rayon du champ est de 28 dp (forme « pilule »)

---

## 2. Bouton « Coller » et « effacer » (4 stories)

### CP-1 — Coller depuis le presse-papiers (P0 · S · REL · test JVM)

En tant que **Léa**, je veux coller un lien depuis le presse-papiers, sans avoir à le retaper.

- **Étant donné** le presse-papiers contient `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- **Quand** j'appuie sur le bouton « Coller »
- **Alors** le champ de saisie contient `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- **Et** aucune requête réseau n'est émise par le seul collage

### CP-2 — Presse-papiers vide (P1 · S · REL · test JVM)

En tant que **Léa**, je veux que le clic sur « Coller » ne fasse rien de nuisible quand le presse-papiers est vide.

- **Étant donné** le presse-papiers est vide
- **Quand** j'appuie sur le bouton « Coller »
- **Alors** le champ de saisie reste inchangé
- **Et** aucun message d'erreur ne s'affiche
- **Et** aucune requête réseau n'est émise

### CP-3 — Effacer le champ (P1 · S · REL · test JVM)

En tant que **Léa**, je veux effacer le champ en un clic pour saisir un nouveau lien.

- **Étant donné** le champ de saisie contient `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- **Quand** j'appuie sur le bouton « effacer »
- **Alors** le champ de saisie est vide
- **Et** le bouton « effacer » n'est plus visible

### CP-4 — Bouton « Coller » désactivé (P2 · S · REL · test JVM)

En tant que **Léa**, je veux que le bouton « Coller » ne soit pas cliquable quand le presse-papiers ne contient pas de lien exploitable.

- **Étant donné** le presse-papiers contient du texte sans URL valide
- **Quand** l'écran « Lien » est ouvert
- **Alors** le bouton « Coller » est désactivé ou non présent

---

## 3. Détection de site et nettoyage (8 stories — un par site de la liste)

### DS-1 — YouTube, lien standard (P0 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien YouTube standard.

- **Étant donné** je colle `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « YouTube »
- **Et** le statut affiche « Pris en charge »

### DS-2 — TikTok (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien TikTok.

- **Étant donné** je colle `https://www.tiktok.com/@x/video/123456789`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « TikTok »

### DS-3 — Instagram (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien Instagram.

- **Étant donné** je colle `https://www.instagram.com/reel/ABC123/`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « Instagram »

### DS-4 — Facebook (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien Facebook.

- **Étant donné** je colle `https://www.facebook.com/watch?v=123456789`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « Facebook »

### DS-5 — X (ancien Twitter) (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien X.

- **Étant donné** je colle `https://x.com/utilisateur/status/123456789`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « X »

### DS-6 — Snapchat (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien Snapchat.

- **Étant donné** je colle `https://www.snapchat.com/t/xyz/abc123`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « Snapchat »

### DS-7 — SoundCloud (P1 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse un lien SoundCloud.

- **Étant donné** je colle `https://soundcloud.com/artiste/titre`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « SoundCloud »

### DS-8 — Bandcamp, Vimeo, Dailymotion, Reddit, Twitch (P2 · M · REL · test JVM)

En tant que **Léa**, je veux que B Music reconnaisse les autres plateformes de la liste.

- **Étant donné** je colle l'un des liens suivants : `bandcamp.com`, `vimeo.com`, `dailymotion.com`, `reddit.com`, `twitch.tv`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette de la plateforme correspondante

---

## 4. Formats de lien particuliers (5 stories)

### LP-1 — Lien raccourci (P1 · M · REL · test JVM)

En tant que **Léa**, je veux qu'un lien raccourci (ex. `youtu.be/...`) soit reconnu.

- **Étant donné** je colle `https://youtu.be/dQw4w9WgXcQ`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « YouTube »

### LP-2 — Lien mobile (P1 · M · REL · test JVM)

En tant que **Léa**, je veux qu'un lien au format mobile (`m.youtube.com`) soit reconnu.

- **Étant donné** je colle `https://m.youtube.com/watch?v=dQw4w9WgXcQ`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée porte l'étiquette « YouTube »

### LP-3 — Paramètres de suivi (P1 · S · REL · test JVM)

En tant que **Léa**, je veux que les paramètres de suivi (`utm_*`, `fbclid`, `igshid`, `si`) soient nettoyés avant analyse.

- **Étant donné** je colle `https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=newsletter&fbclid=abc123`
- **Quand** l'analyse est déclenchée
- **Alors** l'URL utilisée pour l'analyse ne contient **pas** `utm_source`, `fbclid`, `igshid`, ni `si`
- **Et** l'entrée porte l'étiquette « YouTube »

### LP-4 — Texte contenant plusieurs liens (P1 · M · REL · test JVM)

En tant que **Léa**, je veux coller un bloc de texte contenant plusieurs liens et les sélectionner individuellement.

- **Étant donné** je colle un bloc contenant 2 liens valides et du texte libre
- **Quand** l'analyse est déclenchée
- **Alors** 2 entrées sont créées, une par lien détecté
- **Et** un compteur indique le nombre de lignes ignorées
- **Et** chaque entrée a une case à cocher individuelle

### LP-5 — Limite 10 liens (P2 · S · REL · test JVM)

En tant que **Léa**, je veux une limite raisonnable sur le nombre de liens extraits d'un bloc de texte.

- **Étant donné** un bloc contenant 12 liens valides
- **Quand** l'analyse est déclenchée
- **Alors** au maximum 10 entrées sont créées
- **Et** un message indique que le nombre de liens a été limité

---

## 5. États d'erreur et refus (9 stories)

### ER-1 — Lien invalide (P0 · S · SEC · test JVM)

En tant que **Léa**, je veux qu'un lien invalide soit refusé avec un message clair, afin de ne pas attendre un résultat impossible.

- **Étant donné** je colle `ftp://example.com/file`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée est marquée « Lien non valide »
- **Et** un bouton « Réessayer » est présent
- **Et** aucune requête réseau n'est émise

### ER-2 — Lien avec fragment (P1 · S · SEC · test JVM)

En tant que **Léa**, je veux qu'un lien contenant un fragment (`#`) soit refusé.

- **Étant donné** je colle `https://www.youtube.com/watch?v=dQw4w9WgXcQ#t=60`
- **Quand** l'analyse est déclenchée
- **Alors** l'entrée est marquée « Lien non valide »
- **Et** aucune requête réseau n'est émise

### ER-3 — Site non pris en charge (P0 · M · REL · test JVM)

En tant que **Léa**, je veux savoir qu'une plateforme n'est pas prise en charge, afin de ne pas rester bloquée.

- **Étant donné** je colle un lien vers une plateforme non listée (ex. `https://exemple-inconnu.com/video/123`)
- **Quand** l'analyse est déclenchée
- **Alors** l'état « Site non pris en charge » s'affiche
- **Et** un bouton « Voir les sites pris en charge » est présent
- **Et** aucun écran vide n'est affiché

### ER-4 — Contenu privé (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux distinguer un contenu privé d'une panne.

- **Étant donné** je colle un lien vers une vidéo YouTube privée
- **Quand** l'analyse est déclenchée
- **Alors** l'état « Contenu privé ou indisponible. » s'affiche
- **Et** aucune tentative de contournement n'est engagée
- **Et** l'action « Ouvrir à l'extérieur » reste disponible

### ER-5 — Connexion requise (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux savoir qu'un compte est nécessaire.

- **Étant donné** je colle un lien vers un contenu soumis à connexion
- **Quand** l'analyse est déclenchée
- **Alors** l'état « Connexion requise » s'affiche
- **Et** un bouton « Réessayer » est présent

### ER-6 — Contenu protégé (P1 · M · SEC · vérification manuelle)

En tant que **Léa**, je veux qu'un contenu protégé ne soit pas lu sans autorisation.

- **Étant donné** je colle un lien vers un contenu DRM ou protégé
- **Quand** l'analyse est déclenchée
- **Alors** l'état « Contenu protégé » s'affiche
- **Et** aucun lecteur intégré n'est instancié
- **Et** aucune tentative de contournement n'est engagée

### ER-7 — Moteur à mettre à jour (P2 · M · REL · journal exporté)

En tant que **Léa**, je veux savoir quand le moteur d'extraction doit être mis à jour.

- **Étant donné** le moteur d'extraction ne reconnaît plus un site précédemment pris en charge
- **Quand** l'analyse est déclenchée
- **Alors** l'état « Moteur à mettre à jour » s'affiche
- **Et** un bouton « Ouvrir le journal » est présent

### ER-8 — Hors ligne (P0 · S · REL · vérification manuelle)

En tant que **Léa**, je veux un message clair quand je suis hors ligne.

- **Étant donné** l'appareil est en mode avion
- **Quand** je lance une analyse de lien
- **Alors** l'état « Hors ligne » s'affiche
- **Et** un bouton « Réessayer » est présent
- **Et** aucune requête réseau n'est émise

### ER-9 — Doublon (P1 · S · REL · test JVM)

En tant que **Léa**, je veux éviter d'ajouter deux fois le même lien.

- **Étant donné** la liste contient déjà `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- **Quand** j'ajoute le même lien
- **Alors** l'entrée est marquée « Déjà dans la liste » et n'est pas ajoutée une seconde fois
- **Et** l'action « Ajouter quand même » est proposée

---

## 6. Journal d'étapes et messages d'erreur (4 stories)

### JE-1 — Trace du journal à chaque échec (P0 · S · REL · journal exporté)

En tant que **Léa**, je veux que chaque échec laisse une trace dans le journal, afin de pouvoir diagnostiquer.

- **Étant donné** un échec d'analyse de lien (quel qu'en soit la cause)
- **Quand** l'échec se produit
- **Alors** une entrée est ajoutée au journal d'étapes (DownloadTrace)
- **Et** l'entrée contient : horodatage, étape (VALIDATION / SITE_RECONNU / MOTEUR_INITIALISÉ / EXTRACTION...), résultat (échec), code d'erreur typé, message technique court
- **Et** le lien est réduit au domaine et à un hash (aucune donnée personnelle)

### JE-2 — Message clair par état d'erreur (P0 · S · SEC · test JVM)

En tant que **Léa**, je veux un message clair et une action pour chaque état d'erreur.

- **Étant donné** chaque état d'erreur de la liste (invalide, non pris en charge, privé, connexion requise, protégé, moteur à jour, hors ligne, doublon)
- **Quand** l'état s'affiche
- **Alors** un message en français est visible
- **Et** une action est associée (« Réessayer », « Voir les sites », « Ouvrir le journal », etc.)

### JE-3 — Partage du journal (P1 · S · REL · vérification manuelle)

En tant que **Léa**, je veux pouvoir partager le journal d'étapes.

- **Étant donné** le journal d'étapes contient au moins une entrée
- **Quand** j'appuie sur « Partager » dans l'écran Journal
- **Alors** un rapport texte est généré et prêt à être partagé

### JE-4 — Aucune donnée personnelle dans le journal (P0 · S · SEC · test JVM)

En tant que **Léa**, je veux que le journal ne contienne aucune donnée personnelle.

- **Étant donné** un lien `https://www.youtube.com/watch?v=ID_PERSO`
- **Quand** une entrée de journal est créée pour ce lien
- **Alors** l'entrée ne contient que le domaine et un hash du lien
- **Et** l'URL complète n'apparaît pas dans le journal

---

## 7. Partage depuis une autre application (3 stories)

### SH-1 — Partage d'une URL vers B Music (P1 · M · REL · vérification manuelle)

En tant que **Léa**, je veux partager une URL vers B Music pour la retrouver plus tard.

- **Étant donné** une autre application partage une URL vers B Music
- **Quand** je choisis B Music dans le partage
- **Alors** l'onglet « Lien » s'ouvre avec le champ pré-rempli de l'URL
- **Et** aucune action automatique n'est déclenchée

### SH-2 — Partage de texte sans URL (P2 · S · REL · vérification manuelle)

En tant que **Léa**, je veux que le partage de texte sans URL soit géré proprement.

- **Étant donné** une autre application partage du texte sans URL
- **Quand** je choisis B Music dans le partage
- **Alors** le champ est pré-rempli du texte
- **Et** l'utilisateur décide de l'action suivante

### SH-3 — Jamais de démarrage automatique (P0 · S · SEC · test JVM)

En tant que **Léa**, je veux qu'aucun téléchargement ne démarre automatiquement depuis un partage.

- **Étant donné** une URL est pré-remplie depuis un partage
- **Quand** l'écran « Lien » s'affiche
- **Alors** aucune analyse ni aucun téléchargement n'est déclenché automatiquement
- **Et** l'utilisateur doit appuyer explicitement sur « Analyser »

---

## 8. Test de bout en bout en release (1 story)

### E2E-1 — Bout en bout en release (P0 · L · REL · journal exporté)

En tant que **Léa**, je veux que tout le parcours fonctionne sur l'APK release minifié.

- **Étant donné** l'APK release de B Music (variante réseau, R8 minifié)
- **Quand** je colle un lien YouTube public, que j'analyse, que je choisis le format et que je lance le téléchargement
- **Alors** le téléchargement se met en file
- **Et** le fichier apparaît dans l'historique avec l'état « Terminé »
- **Et** le fichier est lisible depuis la bibliothèque
- **Et** chaque étape est tracée dans le journal d'étapes
- **Et** aucun crash ne survient pendant le parcours

---

## 9. Récapitulatif

| Bloc | Stories | Critères Given/When/Then | Type de preuve principal |
|------|---------|-------------------------|-------------------------|
| Zone de saisie — centrage | ZS-1 → ZS-5 (5) | 20 | capture |
| Coller / effacer | CP-1 → CP-4 (4) | 16 | test JVM |
| Détection de site | DS-1 → DS-8 (8) | 32 | test JVM |
| Formats de lien | LP-1 → LP-5 (5) | 20 | test JVM |
| États d'erreur | ER-1 → ER-9 (9) | 36 | test JVM / vérification manuelle |
| Journal d'étapes | JE-1 → JE-4 (4) | 16 | journal exporté / test JVM |
| Partage | SH-1 → SH-3 (3) | 12 | vérification manuelle / test JVM |
| Bout en bout release | E2E-1 (1) | 6 | journal exporté |
| **Total** | **39 stories** | **178 assertions** | — |

---

## 10. Agents responsables

| Agent | Stories couvertes |
|-------|------------------|
| REL (Build) | Toutes les stories (implémentation, build release, tests JVM) |
| LNA (Lean) | ZS-1 → ZS-5 (conception zone de saisie) |
| DSG (Designer) | ZS-1 → ZS-5 (captures, contrastes, centrage) |
| SEC (Security) | ER-1, ER-2, ER-3, ER-6, JE-4, SH-3 (sécurité, SSRF, pas de lecteur intégré) |
| PM | Recette finale, vérification des messages et actions |
