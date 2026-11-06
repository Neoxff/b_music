# Sonore MVP — Spec produit (PM)

## 1. Spécification MVP

**Objectif** : un lecteur de musique local, hors ligne, sans compte, pour qui possède une collection de fichiers (MP3/FLAC/…) et veut une interface soignée type « Spotify-like » sans dépendance réseau.

**Utilisateur** : propriétaire d'une collection locale (≈ 1 000–5 000 fichiers), Android d'abord, usage perso/beta privée.

**Stack (arbitrage PM)** : React + TypeScript + **Tauri 2 mobile** (choix de l'utilisateur, stack libre). Enveloppe Rust native mince, UI web embarquée. ⚠️ Risque : Tauri mobile encore jeune. **Bascule prévue** sur Expo/React Native si, à J1, le build Android n'est pas stable (critère : 2 builds consécutifs OK + lecture audio native validée en < 2 jours-homme d'essai).

**Périmètre MVP (MoSCoW, ajusté)** :
- **Must** : scan d'un dossier choisi par l'utilisateur · lecture/pause/suivant/précédent · barre de progression seekable · volume · métadonnées (tags ID3/FLAC) + pochettes embarquées · recherche locale · playlists (créer/renommer/supprimer/réordonner).
- **Should** : file d'attente · aléatoire / répéter (état persistant) · favoris · lecture en arrière-plan + contrôles écran de verrouillage (indispensable de fait sur mobile).
- **Could** : paroles embarquées (fichiers .lrc) · mini-lecteur · thème clair.
- **Won't (v1)** : streaming/compte/recommandations/social (brief) · égaliseur (retiré : Complexité DSP + réglages, sans valeur prouvée pour un MVP perso) · **raccourcis clavier** (relégués : device cible = mobile, clavier inexistant ; redeviendront Should si une version desktop sort) · publication store/signature · fetch de pochettes en ligne (zéro réseau : pochettes embarquées uniquement).

**Sécurité (non négociable, prime sur le design)** : zéro requête réseau (CSP bloquant, aucune dépendance distante, aucun CDN/police/image externe — la maquette respecte déjà cela) · accès fichiers via storage scopé (URI persistants), jamais les permissions larges · aucune donnée ne sort de l'appareil · pas de télémétrie. Le design s'adapte ; il n'a rien à retirer.

**Métriques cibles** (✅ = exigence du brief, ◐ = cible PM à valider) : scan 1 000 fichiers affichés < 5 s ✅ · latence lecture→son < 200 ms ◐ · démarrage à froid < 2 s (Android milieu de gamme) ◐ · bibliothèque ≥ 5 000 fichiers ◐ · 0 bug bloquant à la sortie J3 ◐ · crash-free ≥ 99 % sur la beta ◐.

**Hypothèses explicites** : Android d'abord (iOS = après MVP, décision de plateforme à reconfirmer au J2) · fichiers déjà tagués proprement dans la collection (pas d'édition de tags dans le MVP) · un seul utilisateur · pas de publication → pas de builds signés. Si une hypothèse est fausse, l'impact est noté en §4 du journal.

---

## 2. Backlog priorisé

| # | Story (« En tant que…, je veux…, afin de… ») | Prio | Taille | Critères d'acceptation mesurables |
|---|---|---|---|---|
| S1 | Utilisateur, je veux sélectionner un dossier de musique, afin que l'app indexe ma collection | Must | M | Dossier sélectionné persistant entre lancements ; 1 000 fichiers scannés+affichés < 5 s ; formats .mp3/.flac/.m4a/.ogg |
| S2 | Utilisateur, je veux lire une piste au tap, afin d'écouter immédiatement | Must | M | Tap → son < 200 ms ; piste inconnue/corrompue → toast d'erreur, pas de crash |
| S3 | Utilisateur, je veux pause/next/prev + progress bar, afin de contrôler la lecture | Must | M | Play/pause fiable < 100 ms ; seek au tap et au drag ; next/prev enchaînent en fin de piste |
| S4 | Utilisateur, je veux le volume + mute, afin d'ajuster l'écoute | Must | S | Slider 0–100 ; mute mémorisé ; volume système respecté |
| S5 | Utilisateur, je veux voir métadonnées et pochettes, afin de m'y retrouver | Must | M | Titre/artiste/album/durée lus des tags ; pochette embarquée affichée < 300 ms ou placeholder généré ; mono-fichier sans pochette OK |
| S6 | Utilisateur, je veux une recherche locale, afin de trouver un titre sans scroller | Must | M | Résultats < 150 ms pour 5 000 fichiers ; insensible à la casse/aux accents ; « 0 résultat » explicite |
| S7 | Utilisateur, je veux créer/éditer/supprimer une playlist, afin d'organiser mes écoutes | Must | L | CRUD playlist complet ; réordonnancement persisté (< 150 ms par déplacement) ; doublons autorisés, annoncés |
| S8 | Utilisateur, je veux ajouter/retirer un titre d'une playlist, afin de composer mes listes | Must | S | Ajout en 2 taps max ; retrait avec confirmation inline (undo 5 s) |
| S9 | Utilisateur, je veux la file d'attente, afin d'enchaîner ma sélection sans y penser | Should | M | « Lire ensuite » / « Ajouter à la file » ; réordonnançable ; total durée affiché |
| S10 | Utilisateur, je veux aléatoire/répéter, afin de varier ou boucler mes écoutes | Should | S | 3 états repeat (off/tout/un) persistés ; shuffle sème correct, pas de perte de piste |
| S11 | Utilisateur, je veux mes favoris, afin de retrouver mes titres préférés | Should | S | Cœur partout (liste, player bar) ; vue « Favoris » ; ajout/retrait < 100 ms |
| S12 | Utilisateur, je veux la lecture en arrière-plan + contrôles écran verrouillé, afin d'écouter sans l'app ouverte | Should | L | Lecture continue écran éteint ; notification média + media session Android fonctionnels ; tap notification = retour app |
| S13 | Utilisateur, je veux que l'état (playlist, volume, favoris, file) survive à un redémarrage, afin de ne rien perdre | Should | S | Restauration intégrale après kill/relance ; aucune migration destructive en v1 |
| S14 | Utilisateur, je veux importer des pochettes manquantes depuis un dossier local sidecar, afin d'habiller les fichiers nus | Could | S | Fichier `cover.jpg` dans le dossier album pris en charge ; sinon placeholder |
| S15 | Utilisateur, je veux voir les paroles (.lrc) embarquées, afin de suivre sans quitter l'app | Could | M | Affichage sync sur fichier .lrc présent ; sinon masqué, pas de message d'erreur |
| S16 | Utilisateur, je veux un mini-lecteur + thème clair, afin d'adapter l'usage | Could | M | Cohabitation sans conflit de hiérarchie UI ; thème via tokens existants |
| S17 | Utilisateur, je veux une navigation clavier complète, afin d'utiliser un clavier/mule virtuelle | Won't (v1) | M | Relégué : device cible mobile |
| S18 | Équipe, je veux 0 bug bloquant au jalon J3, afin de sortir une beta utilisable | Must | — | Liste de bugs J3 vide des bloquants ; smoke test E2E des 4 écrans + lecteur vert |

**Arbitrages de périmètre** : raccourcis clavier et égaliseur **sortis** (device mobile / complexité DSP sans valeur prouvée MVP). File d'attente et aléatoire restent en **Should** — ils sont déjà maquettés (Designer) pour un coût faible. Favoris promus en Should pour la parité perçue avec l'UX de référence, mais coupable de dérapage si le Must déborde (voir journal D5).

---

## 3. Roadmap en 3 jalons

- **J1 — Lecture de base** (cible : build Android installable perso) : S1, S2, S3, S4, S5. **Critères de sortie** : tap→son < 200 ms ; scan 1 000 fichiers < 5 s ; lecture en arrière-plan (S12) **non incluse** (volontaire, J2/J3) mais audio continu dans l'app ; 0 crash sur 50 pistes lues. **Porte : décision Go/No-Go Tauri 2 mobile** — si instable → bascule Expo/RN.
- **J2 — Bibliothèque et playlists** : S6, S7, S8, S12. **Critères de sortie** : recherche < 150 ms sur 5 000 fichiers ; playlist CRUD complet ; lecture en arrière-plan + notification média OK ; restauration d'état (S13) active.
- **J3 — Finitions et confort** : S9, S10, S11, + stabilisation, accessibilité (focus, AA, reduced-motion), smoke E2E, 0 bug bloquant. **Critères de sortie** : 0 bug bloquant ; crash-free ≥ 99 % sur 1 semaine de beta perso ; AA vérifié sur les 4 écrans ; release signée localement inutilisée (usage perso).

---

## 4. Journal des décisions

| # | Décision | Justification / alternative écartée |
|---|---|---|
| D1 | Android d'abord | iOS implique compte dev Apple + signature : surcoût non justifié pour usage perso ; reconfirmé à J2 |
| D2 | Tauri 2 mobile plutôt qu'Expo/RN | Choix utilisateur (stack libre) ; **alternative Expo** si critère de bascule J1 non tenu (2 builds + lecture native KO en 2 j) — réversible, coût borné |
| D3 | Zéro réseau (CSP, storage scopé, pochettes embarquées) | Sécurité non négociable ; élimine scripts distants, CDN, fetching de covers (alternative écartée) — le design s'adaptait déjà seul (maquette 100 % offline) |
| D4 | Égaliseur retiré du MVP | Complexité DSP + UX réglages pour un bénéfice non prouvé ; le coût se paie sur J2/J3 |
| D5 | Favoris et file d'attente en Should, avec surveillance | Valeur perçue élevée pour coût faible ; **si Must déborde > 30 % de marge, S11/S12 basculent en J3 ou s'effacent** |
| D6 | Pas d'édition de tags / pas de publication store | Hors cible perso ; reste en backlog v2 |
| D7 | Designer (maquette `design/`) validé sans modif | Tokens offline, accent ambre, cibles ≥ 40 px, AA : conforme règles marque et sécurité ; conflit Designer/Security résolu côté Security par défaut (sécurité prime, ici aucun retrait nécessaire) |

**Risques principaux** : R1 instabilité Tauri mobile (P×I moyen×élevé → mitigation : porte J1, bascule Expo) · R2 tags malformés dans la collection (bas → mitigation : pochette placeholder, skip+toast) · R3 scope creep beta perso (moyen → mitigation : compensation via D5, tout ajout = arbitrage explicite).
