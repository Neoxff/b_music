# B Music — User stories & critères d'acceptation

> Agent Product Manager B Music. Date : 6 novembre 2026.
> Personas : **Léa** (24, étudiante), **Marc** (41, livreur, Android modeste), **Claire** (34, dev, lossless) — cf. `01-vision.md` §2.
> Format : « En tant que … » + **Given/When/Then** vérifiables. Chaque story : **Priorité** (P0 v1.0 / P1 v1.x), **Effort** (S ≤ 1 j · M 2–5 j · L ≥ 5 j), **Agent** (PM/BIZ/DSG/LNA/SEC/REL).
> Renvois : budgets `docs/lean/03-budgets-performance.md` · animations `docs/design/02-animations.md` · design system `docs/design/01-design-system.md` · audit `docs/lean/02-audit-gaspillage.md` · roadmap RICE `02-roadmap-backlog.md`.
> Total : **50 stories** (P0 = obligatoire v1.0).

---

## Épique ID — Identité (4)

### ID-1 — Nom d'APK (P0 · S · BIZ · ref ID-1)
En tant que **Claire**, je veux un APK nommé exactement `B-Music.apk` afin de le diffuser/partager en confiance.

- **Étant donné** le build `:app:assembleFdroidRelease`
- **Quand** la CI termine le build
- **Alors** l'artefact s'appelle exactement `B-Music.apk` (pas `BoomingMusic-*-universal.apk`)
- **Alors** `versionName = 1.0` et `applicationId = com.bmusic.*` (plus aucune référence `mardous`/`booming` dans l'APK)

### ID-2 — Nom affiché & icône adaptive (P0 · S · BIZ+DSG · ref ID-2)
En tant que **Marc**, je veux voir « B Music » sous l'icône, avec une icône adaptative et monochrome, afin de reconnaître mon appli parmi 40 icônes.

- **Étant donné** B Music installée (Android 8+)
- **Quand** Marc regarde le launcher (mode icône ronde ou carrée, ou icône thématique Android 13+)
- **Alors** le label est exactement « B Music » (`android:label` / `@string/app_name`, FR par défaut et EN)
- **Alors** l'icône adaptive est présente (`ic_launcher_foreground.xml` 66 dp de sécurité sur 108 dp, fond `#0B0B0E` uni)
- **Alors** l'icône monochrome est présente et lisible en glyphe thématique Android 13+ (`ic_launcher_monochrome.xml`)
- **Et** l'icône se voit à 24/48/192 dp sans perdre ses 4 barres (cf. `04-logo.md`)

### ID-3 — `B-Music.apk` signé + sources publiées (P0 · S · REL+BIZ · ref ID-3/RL-2)
En tant que **Claire**, je veux un APK signé reproductible et les sources GPL-3.0 publiées afin de pouvoir auditer et reconstruire.

- **Étant donné** le tag `v1.0.0`
- **Quand** la release est construite
- **Alors** `B-Music.apk` est signé (SHA-256 publié dans les notes de release)
- **Alors** les sources correspondantes sont publiées sur le dépôt public (GPL-3.0, attribution RetroMusic/Booming)
- **Et** `versionCode` cohérent avec tag (1 040 000+)

### ID-4 — About & attributions (P0 · S · BIZ · ref ID-4/SC-3)
En tant que **Claire**, je veux l'écran « À propos » avec les licences complètes afin de vérifier le respect GPL-3.0.

- **Étant donné** B Music installée
- **Quand** j'ouvre Réglages → À propos → Licences
- **Alors** j'y trouve GPL-3.0, copyright RetroMusic Player, Booming Music et B Music
- **Et** la liste des dépendances (aboutlibraries) s'affiche avec licences compatibles OSS uniquement (aucune dépendance propriétaire)
- **Et** aucune URL de tracking/analytics n'apparaît ; bouton « Copier le lien du dépôt » (pas de bouton ouvrant un navigateur automatiquement ? — non : un simple texte copiable suffit)

---

## Épique DS — Design system (5)

### DS-1 — Tokens sombre + Inter (P0 · M · DSG/BIZ · ref DS-1)
En tant que **Léa**, je veux un thème sombre profond (#0B0B0E, surfaces 1/2/3 tonales, Inter) afin d'une interface calme qui ne fatigue pas les yeux.

- **Étant donné** B Music en thème par défaut
- **Quand** n'importe quel écran s'affiche
- **Alors** le fond est `#0B0B0E`, les cartes/pochettes sont `surface1..3` (`#131318`/`#1A1A21`/`#22222B`), séparateurs 1 px `#2A2A34`
- **Alors** la typographie est Inter 400/500/600 uniquement (Scale 32/40, 22/28, 16/24, 13/18, 11/16) — vérifié par Lint que 700 et l'italique sont absents
- **Et** aucun écran ne contient de texte principal avec contraste < 4,5:1 sur son fond (LNA calcule, DSG confirme)

### DS-2 — Thème clair (P0 · S · DSG/BIZ · ref DS-2/ST-1)
En tant que **Marc** (qui lit dehors au soleil), je veux un thème clair lisible afin d'utiliser l'app ailleurs que dans le noir.

- **Étant donné** B Music en thème clair
- **Quand** l'écran s'affiche sous forte lumière ambiante
- **Alors** `#FAFAFB` / `#FFFFFF` / `#F2F2F5`, traits `#E4E4EA`, texte `#1B1B1F` (16,5:1), secondaire `#4A4A52` (8,4:1) — AA sur toute la carte
- **Et** le bascule Sombre/Clair/Système existe dans Réglages et s'applique instantanément sans redémarrage

### DS-3 — Migration écrans → nouveau look (P0 · L · BIZ · ref DS-3) — **GOULOT**
En tant que **Léa**, je veux que TOUTE app ait le nouveau design (pochette héros, lignes sobres) — pas seulement Now Playing — afin d'une cohérence « premium ».

- **Étant donné** le fork Booming migré
- **Quand** on ouvre Bibliothèque, Album, Artiste, Playlist, Recherche, File, Réglages, Now Playing
- **Alors** chaque écran utilise les tokens B Music (aucune couleur/police/icône héritée de Booming en dur)
- **Et** la grille d'albums : pochettes carrées R 12 dp, grille 2 colonnes, marge 20 dp ; lignes de titre 56 dp avec pochette 48 R 12
- **Et** aucun écran ne garde un header/tab « material » hérité non retouché (revue Designer obligatoire)
- **Et** les 8 écrans correspondent au document `docs/design/03-ecrans.md` (dp, états vide/chargement/erreur)

### DS-4 — Accent dynamique subtil (P0 · S · DSG/BIZ · ref DS-4/ST-2)
En tant que **Léa**, je veux que la pochette teinte seulement le bouton lecture, la barre de progression et un halo discret — pas tout l'écran — afin d'un effet "premium" qui respecte ma couleur.

- **Étant donné** B Music joue un album avec pochette colorée
- **Quand** j'ouvre le lecteur plein écran
- **Alors** l'accent extrait (Palette API, dérivé ≥ 4,5:1, désaturé 45 %) ne colore **que** : (1) bouton lecture/pause, (2) remplissage de la progression, (3) halo opaque derrière la pochette (jamais aucun blur)
- **Et** le reste de l'UI (nav, lignes, chips, textes) reste en neutres + accent de marque `#E8862E` uniquement
- **Et** si la pochette n'est pas exploitable (ex. dégradé, gris), l'app retombe sur `#E8862E` — jamais une couleur illisible
- **Et** le switch « Accent dynamique » dans Réglages désactive l'extraction (retour accent de marque) sans redémarrage

### DS-5 — Garde-fou « zéro verre » (P0 · S · LNA · ref DS-5)
En tant que **Claire**, je veux la garantie qu'aucun écran n'utilise flou/verre/transparence décorative afin que le design reste sobre et net.

- **Étant donné** n'importe quel écran
- **Quand** une revue de code est lancée : `grep -riE 'RenderEffect|Modifier.blur|blur\(|fill\(alpha|translucen|glass|frosted|BackdropBlur' app/src`
- **Alors** le résultat est **vide** (sauf scrims fonctionnels documentés `60 %` sur les modales)
- **Et** aucun fond semi-transparent décoratif (ex. `surface.copy(alpha = .x)`) n'existe ; les seules opacités sont : scrim modal et skeleton shimmer
- **Et** les séparateurs font exactement 1 px et l'élévation est tonale (surface1→3), jamais des ombres portées

---

## Épique AN — Animations (7)

### AN-1 — Mini-player → lecteur plein écran (geste suivi du doigt) (P0 · M · BIZ · ref AN-1/§anim 1)
En tant que **Léa**, je veux glisser le mini-player vers le haut et voir la pochette suivre mon doigt et se verrouiller en plein écran — ou revenir si je lâche tôt — afin d'un contrôle tactile « comme un vrai système ».

- **Étant donné** le mini-player visible (64 dp, pochette 48 R 12)
- **Quand** je glisse verticalement vers le haut avec le doigt (tap-and-hold exclu)
- **Alors** la pochette et le titre suivent le doigt 1:1 (pas de latence perceptible), le rayon interpole 12 → 28 dp, le fond passe à `#0B0B0E`
- **Quand** je relâche avec vélocité > 1 800 dp/s vers le haut ou au-delà de 40 % de la transition
- **Alors** le lecteur plein écran est verrouillé en ressort (spring dampingRatio 0,86 / stiffness 380, plafond 420 ms)
- **Quand** je relâche tôt (avant le seuil)
- **Alors** tout revient à l'état mini-player (ressort symétrique, signalé par haptique léger)
- **Et** à 120 Hz, la transition tient sans frame > 8 ms (mesuré `dumpsys gfxinfo` ou Macrobenchmark)

### AN-2 — Play/pause morph + rebond (P0 · S · BIZ · ref AN-2/§anim 2)
En tant que **Marc**, je veux que play↔pause se transforment (pas qu'ils basculent brutement) afin d'un feedback tactile rassurant.

- **Étant donné** le bouton lecture en plein écran ou dans le mini-player
- **Quand** je tape sur le bouton
- **Alors** l'icône se transforme morph play↔pause en ≈ 220 ms (EmphasizedDecelerate) + micro-rebond d'échelle 1.0→0.94→1.0 (ressort dampingRatio 0,55 / stiffness 700)
- **Et** le rebond se re-vise depuis la valeur courante si je re-tape en plein vol (pas de saut)

### AN-3 — Changement de piste (P0 · S · BIZ · ref AN-3/§anim 3)
En tant que **Léa**, je veux que la pochette glisse horizontalement (~280 ms) avec un fondu de titre décalé 40 ms afin de comprendre l'ordre spatial de la file.

- **Étant donné** la file contient ≥ 2 pistes
- **Quand** « suivant » : pochette sort/entre en slide ±28 dp + crossfade, titre/artiste en fondu 220 ms avec 40 ms de délai
- **Et** « précédent » : direction inversée, même durées
- **Et** en cas d'interruption en plein vol, l'état final est celui de la nouvelle piste (jamais de retour en arrière visuel)

### AN-4 — Barre de progression vivante (P0 · M · BIZ · ref AN-4/§anim 4)
En tant que **Léa**, je veux que la barre de progression soit épaisse quand je la touche et fluide à chaque frame — jamais à coups d'une seconde — afin de savoir précisément où je peux naviguer.

- **Étant donné** B Music joue et le lecteur est ouvert
- **Quand** mes doigts effleurent/pressent la piste
- **Alors** la piste passe de 4 à 8 dp en 120 ms, le curseur apparaît (120 ms, scale 0.6→1)
- **Et** la position avance chaque frame (interpolation 90 ms) — pas de saut « à la seconde » ; le timer affiche un `tnum` stable sans jitter horizontal
- **Et** pendant le drag manuel, la mise à jour automatique se met en pause ; au relâchement, resync sur la position réelle (±200 ms)

### AN-5 — Listes : entrée échelonnée (P0 · S · BIZ · ref AN-5/§anim 5)
En tant que **Marc**, je veux que les lignes de mes listes apparaissent avec un léger décalage (stagger 24 ms, max 8 éléments) afin d'une animation visible mais jamais bloquante.

- **Étant donné** la bibliothèque affiche une liste (Titres/Albums/Artistes/Playlists)
- **Quand** la liste s'affiche (premier affichage ou changement de filtre)
- **Alors** les éléments visibles entrent en translationY 16→0 + alpha, stagger **24 ms** par élément, **max 8** (le reste apparaît d'un bloc)
- **Et** en fling vertical rapide, les éléments hors écran sautent sans animation (aucune traînée de frames)
- **Et** les en-têtes collants (section A/B/C) ne clignotent jamais (hauteur constante, crossfade 150 ms sur le label)

### AN-6 — Navigation entre onglets (P0 · S · BIZ · ref AN-6/§anim 6)
En tant que **Léa**, je veux que les onglets Accueil/Recherche/File/Réglages glissent+fondent (300 ms, courbe d'emphase) afin d'une navigation claire.

- **Étant donné** la barre de navigation (4 onglets)
- **Quand** je tape un onglet ou swipe horizontalement
- **Alors** le contenu slide ±24 dp + fade en **300 ms** (courbe d'emphase standard), pas de ressort (prédictibilité)
- **Et** un swipe contraire en cours suit le doigt 1:1 et décide au relâchement
- **Et** l'onglet actif reste affiché en accent `#E8862E` (icône + label Micro)

### AN-7 — « Supprimer les animations » respectée (P0 · S · BIZ · ref AN-7/§anim 12)
En tant que **utilisateur mal à l'aise avec le mouvement**, je veux que « Supprimer les animations » du système coupe tout mouvement décoratif afin d'utiliser l'app sans gêne.

- **Étant donné** les animations système désactivées (`ANIMATOR_DURATION_SCALE == 0`)
- **Quand** j'ouvre n'importe quel écran / je joue une piste
- **Alors** toutes les durées sont 0–1 ms ; transitions = fondu simple ≤ 150 ms ; ressorts et staggers supprimés ; égaliseur coupé
- **Et** aucune information n'est véhiculée uniquement par un mouvement (l'état courant est toujours textuel/iconique)

---

## Épique PL — Lecteur (8)

### PL-1 — Lecture locale Media3 (P0 · S · BIZ · ref PL-1)
En tant que **Léa**, je veux lancer la lecture d'un titre en tap et entendre le son immédiatement (< 200 ms) — fichiers MP3/FLAC/OGG/AAC/M4A — afin de ne pas attendre.

- **Étant donné** B Music installée, bibliothèque scannée
- **Quand** je tape un titre dans la bibliothèque
- **Alors** l'audio démarre en **< 200 ms**, piste corrompue → toast « Fichier ignoré », l'app reste stable et lit le suivant
- **Et** FLAC lossless lu sans erreur ; file ≥ 100 titres sans crash
- **Et** fonctionnel en mode avion (aucun accès réseau tenté)

### PL-2 — File d'attente (P0 · M · BIZ · ref PL-2/AN-1)
En tant que **Marc**, je veux composer une file d'attente (ajouter, réordonner en glisser-déposer, retirer) et la retrouver après fermeture afin de ne jamais réorganiser en pleine route.

- **Étant donné** une file constituée (via « Lire ensuite » ou sélection multiple)
- **Quand** je maintiens sur le handle ≡ puis glisse une ligne
- **Alors** la ligne suit le doigt, les autres se décalent (animation placement), haptique léger au drop
- **Et** l'ordre persiste après kill/relance de l'app (restauration complète de la file + position)
- **Et** la durée totale de la file est affichée (« 24 titres · 1 h 32 »)
- **Quand** j'ouvre la file et tape « Effacer la file »
- **Alors** un sheet de confirmation s'affiche avant écrasement (« Effacer la file ? La lecture s'arrêtera »)

### PL-3 — Notification média + écran verrouillé (P0 · M · BIZ · ref PL-3)
En tant que **Marc**, je veux contrôler la lecture depuis l'écran de verrouillage et la notification (3 actions) afin de ne pas déverrouiller à chaque changement.

- **Étant donné** une piste joue
- **Quand** l'écran se verrouille ou la notification est déployée
- **Alors** la notification (MediaStyle) affiche : pochette, titre, artiste, 3 actions — **Précédent · Lecture/Pause · Suivant** — + temps écoulé en format grand
- **Et** les actions fonctionnent depuis l'écran verrouillé (playback state suit l'état réel)
- **Et** le verrouillage est `VISIBILITY_PUBLIC`, aucune fuite de chemin de fichier dans la notification
- **Et** au dernier morceau fini, la notification disparaît et le service s'arrête (pas de zombie foreground service)

### PL-4 — Focus audio / casque débranché / Bluetooth (P0 · M · BIZ · ref PL-4)
En tant que **Marc**, je veux que retirer mon casque mette la lecture en pause et que Bluetooth (voiture) prenne le relais proprement afin de ne jamais surprendre quelqu'un avec ma musique.

- **Étant donné** la lecture en cours via casque filaire/Bluetooth
- **Quand** le casque est débranché ou le Bluetooth voiture déconnecte
- **Alors** la lecture se met **en pause** (pas en stop) immédiatement ; à la reconnexion, lecture reprise automatiquement (ou selon préférence utilisateur)
- **Et** B Music cède le focus (ducking) à une navigation/sonnerie, puis reprend en fondu 200 ms après relâchement
- **Et** si un autre média prend le focus (YouTube), B Music se met en pause durablement

### PL-5 — Reprise après appel entrant (P0 · S · BIZ · ref PL-5)
En tant que **Marc**, je veux que la lecture reprenne exactement où elle s'était arrêtée après un appel afin de ne jamais « reperdre le fil » de l'album.

- **Étant donné** une piste joue
- **Quand** un appel entrant arrive (ou audio flash alertes)
- **Alors** B Music met en pause (duck/pause) pour la durée de l'appel
- **Quand** l'appel se termine
- **Alors** la lecture reprend à la seconde exacte où elle s'est arrêtée (± 0,5 s)
- **Et** si l'utilisateur a manuellement mis en pause pendant l'appel, la lecture ne reprend pas automatiquement

### PL-6 — Shuffle / Repeat persistants (P0 · S · BIZ · ref PL-7)
En tant que **Claire**, je veux que aléatoire + répéter (off/tout/un) mémorisent leur état entre lancements afin de piloter mon humeur sans y penser.

- **Étant donné** la lecture joue
- **Quand** je tape shuffle → icône s'active (accent) ; repeat → cycle off → tout → un
- **Alors** l'état persiste après kill/relance de l'app
- **Et** l'état est reflété dans la notification (icône active/inactive) et dans la file

### PL-7 — Gapless (P1 · M · BIZ · ref PL-8)
En tant que **Claire** (flux lossless), je veux la lecture sans coupure entre pistes gapless afin de mes mixes binaires entiers sans silence.

- **Étant donné** FLAC gapless (ex. live)
- **Quand** la lecture enchaîne les pistes
- **Alors** aucun trou perceptible entre pistes (transition < 50 ms)
- **Et** ReplayGain off par défaut (n'altère pas la chaîne audio)
- **Et** si l'appareil ne supporte pas, dégradation gracieuse sans crash

---

## Épique LB — Bibliothèque (7)

### LB-1 — Scan MediaStore + 5 000 titres (P0 · M · BIZ · ref LB-1)
En tant que **Marc**, je veux voir mes fichiers indexés et triés en quelques secondes afin de ne pas attendre à chaque ouverture.

- **Étant donné** 5 000 fichiers audio sur l'appareil
- **Quand** B Music démarre
- **Alors** la collection est affichée en **< 5 s** (critère PM MVP, vérifié par test)
- **Et** un nouveau fichier ajouté pendant l'exécution est pris en compte via MediaStore observer (< 3 s) ou au prochain lancement
- **Et** les formats .mp3/.flac/.m4a/.ogg sont supportés ; fichier illisible → skip + toast « Fichier ignoré », pas de crash

### LB-2 — Titres / Albums / Artistes / Playlists (P0 · M · BIZ · ref LB-2)
En tant que **Léa**, je veux naviguer par Titres / Albums / Artistes / Playlists dans une grille 2 colonnes avec pochettes carrées afin d'un parcours tactile moderne.

- **Étant donné** B Music installée avec collection scannée
- **Quand** j'ouvre Bibliothèque
- **Alors** les 4 onglets fonctionnent (Titres · Albums · Artistes · Playlists) ; Titres = liste (pochette 48 R 12), Albums/Artistes = grille 2 col. (R 12 dp), Playlists = tuiles + onglet dédié
- **Et** tap long sur un élément → sheet d'options (Lecture / Lire ensuite / Ajouter à la file / Favori)
- **Et** en-tête « X titres · Y heures » discret en haut

### LB-3 — Favoris (P0 · S · BIZ · ref LB-3)
En tant que **Léa**, je veux un cœur pour les titres favoris + vue « Favoris » afin d'y revenir en un tap.

- **Étant donné** n'importe quel écran de piste
- **Quand** je tape le cœur
- **Alors** il se remplit en ressort (§ anim 8, ~350 ms) + haptique ; vue Favoris accessible depuis Bibliothèque/Accueil
- **Et** ajout/retrait < 100 ms (base repos) ; état persistant ; compteur à jour partout visible

### LB-4 — Playlists : CRUD + .m3u (P0 · M · BIZ · ref LB-4)
En tant que **Claire**, je veux créer/renommer/supprimer une playlist, la réordonner et importer/exporter en .m3u afin de piloter mes collections.

- **Étant donné** l'écran Playlists
- **Quand** je crée une playlist (nom libre) / renomme / supprime (avec confirmation)
- **Alors** CRUD complet fonctionne en < 150 ms par opération ; doublons autorisés et annoncés
- **Et** la réordonnancement persiste ; import/export `.m3u` fonctionne en local (fichier de partage ou dossier choisi)
- **Et** suppression → toast « Playlist supprimée » + Annuler 5 s ; aucun contenu fichier effacé

### LB-5 — Smart playlists (P1 · M · BIZ · ref LB-5)
En tant que **Léa**, je veux des listes « Les plus écoutés » et « Écoutés récemment » afin de retrouver sans y réfléchir.

- **Étant donné** une collection scannée
- **Quand** j'ouvre les smart playlists
- **Alors** « Les plus écoutés » ordre par nombre de lectures décroissant ; « Récents » par date de dernière lecture
- **Et** les listes se mettent à jour automatiquement après chaque lecture

### LB-6 — Genres + listes blanches/noires (P2 · M · BIZ · ref LB-6/LB-7 — après v1.0)
En tant que **Claire**, je veux trier par genre et masquer des dossiers du scanner afin de filtrer mon bazar.

- **Étant donné** Réglages → Bibliothèque
- **Quand** je choisis un genre / coche « exclure ce dossier »
- **Alors** la bibliothèque est filtrée/réindexée ; les dossiers exclus ne remontent plus (ni notification, ni recherche)
- **Et** l'exclusion est persistante et réversible
- **Marquage : après v1.0**

---

## Épique SE — Recherche (2)

### SE-1 — Recherche locale ultra-rapide (P0 · M · BIZ/DSG · ref SE-1)
En tant que **Léa**, je veux une recherche locale (titres/albums/artistes) en < 150 ms, insensible aux accents/casse, avec 0 résultat explicite afin de trouver en un clin d'œil.

- **Étant donné** bibliothèque 5 000 titres
- **Quand** je tape dans le champ (focus immédiat)
- **Alors** résultats en **< 150 ms**, insensible à la casse et aux accents (NFD/strip accents), match sur titre + artiste + album
- **Et** état « Aucun résultat pour « xyz » » soigné + suggestion « Réindexer »
- **Et** debounce 120 ms ; historique local (20 derniers) au focus vide

### SE-2 — Résultats groupés + état vide (P0 · S · BIZ/DSG · ref SE-2)
En tant que **Marc**, je veux des résultats groupés par type (Titres/Albums/Artistes) afin de lire plus vite.

- **Étant donné** une requête avec résultats multiples
- **Quand** les résultats s'affichent
- **Alors** ils sont groupés (Titres · Albums · Artistes) avec en-têtes Micro sticky, chaque groupe plafonné à 6 résultats + « Voir tout »
- **Et** le skeleton de chargement est sobre (blocs `surface2`, jamais de spinner agressif)

---

## Épique ST — Paramètres (3)

### ST-1 — Apparence (P0 · S · BIZ · ref ST-1/DS-2)
En tant que **Claire**, je veux choisir Sombre (défaut) / Clair / Système afin d'un confort adapté.

- **Étant donné** Réglages → Apparence → Thème
- **Quand** je choisis un thème
- **Alors** il s'applique instantanément, persiste (DataStore), FR par défaut ; aucune autre app n'est affectée
- **Et** « Système » suit `isSystemInDarkTheme()` y compris en changement de mode à chaud

### ST-2 — Accent dynamique toggle (P0 · S · BIZ/DSG · ref ST-2/DS-4)
En tant que **Léa**, je veux activer/désactiver l'accent extrait de la pochette afin de choisir mon esthétique.

- **Étant donné** Réglages → Apparence → Accent dynamique (switch)
- **Quand** je le bascule
- **Alors** l'effet est immédiat (dose sur les 3 éléments autorisés seulement, cf. DS-4), état persisté, aucun autre élément affecté

### ST-3 — Fondu enchaîné + saute de silence (P2 · S · BIZ · après v1.0)
En tant que **Claire**, je veux un fondu enchaîné 0–12 s et l'option de sauter les silences en tête de piste afin de mixer mes albums.

- **Étant donné** Réglages → Lecture
- **Quand** j'active le fondu ou le saut de silence
- **Alors** fondu (crossfade) appliqué entre pistes ; saut de silence (défaut : off) ignore < 1,5 s de silence en tête
- **Marquage : après v1.0**

---

## Épique A11Y — Accessibilité (4)

### A11Y-1 — Contraste AA partout (P0 · S · LNA/DSG · ref A11Y-1)
En tant que **Marc** (vision parfois faible), je veux un contraste AA partout afin de lire sans effort.

- **Étant donné** n'importe quel écran de n'importe quel thème
- **Quand** LNA analyse avec outil contraste
- **Alors** texte principal ≥ 4,5:1, secondaire ≥ 4,5:1 (grands textes : ≥ 3:1 autorisé), aucun texte grisé < 4,5:1
- **Et** les icônes actives (accent) conservent ≥ 3:1 contre leurs voisines

### A11Y-2 — Cibles tactiles ≥ 48 dp (P0 · S · DSG/BIZ · ref A11Y-2)
En tant que **Marc** (gros doigts, conduite), je veux des cibles ≥ 48 dp afin de ne jamais rater un bouton.

- **Étant donné** tous les boutons/icônes interactifs
- **Quand** DSG audite
- **Alors** chaque cible fait **≥ 48 × 48 dp** (hitSlop si icône 24 dp), y compris mini-player et barre de navigation
- **Et** les séparateurs 1 px sont des zones de défilement, jamais des cibles

### A11Y-3 — TalkBack (P0 · M · DSG/BIZ · ref A11Y-3)
En tant que **utilisateur aveugle**, je veux B Music utilisable avec TalkBack afin d'écouter ma musique de façon autonome.

- **Étant donné** TalkBack activé
- **Quand** j'explore l'écran
- **Alors** chaque action lecture/pause/prev/suivant/file/recherche a un `contentDescription` FR/EN adéquat (« Lecture, titre — artiste »)
- **Et** les états (« en lecture », « favori », « répétition activée ») sont annoncés
- **Et** les 6 parcours principaux (ouvrir, chercher, jouer, file, favori, réglages) sont audités et passent

### A11Y-4 — FR par défaut + EN complet (P0 · M · BIZ · ref A11Y-4)
En tant que **utilisateur anglophone**, je veux l'interface en anglais sans aucune chaîne en français en dur afin de ne pas deviner.

- **Étant donné** B Music en locale `en`
- **Quand** j'ouvre chaque écran
- **Alors** 100 % des chaînes sont en EN (via `values-en/strings.xml`) ; **zéro** chaîne FR ou framework héritée en dur
- **Et** FR est la locale par défaut (`values/strings.xml`) avec EN fourni (`values-en/`) ; Lint `HardcodedText` passe
- **Et** le nom affiché reste exactement « B Music » dans les deux langues (nom propre)

---

## Épique PF — Performance (4)

### PF-1 — Cold start < 600 ms (P0 · M · BIZ/LNA · ref PF-1/§03 §4)
En tant que **Marc**, je veux que B Music s'ouvre en moins de 600 ms afin d'écouter sans délai.

- **Étant donné** un cold start (force-stop)
- **Quand** B Music démarre jusqu'au premier écran interactif
- **Alors** **< 600 ms** (médiane Macrobenchmark, 10 runs) ; < 250 ms en warm start
- **Et** Baseline Profile inclus dans le release build ; proguard/R8 actif ; cold start consigné dans la CI

### PF-2 — 5 000 titres : 0 frame perdue (P0 · M · LNA · ref PF-2/§03 §1)
En tant que **Léa**, je veux un défilement bibliothèque sans accroc à 5 000 titres afin d'une expérience fluide même chargée.

- **Étant donné** bibliothèque 5 000 titres
- **Quand** je scroll rapidement
- **Alors** **0 frame perdue**, **< 1 % de frames > 16 ms** (ou 8 ms @120 Hz) mesurées (`dumpsys gfxinfo` / TeamCity)
- **Et** pochettes en cache Coil borné (memoryCacheSize 15 %, diskCache 128 Mo) ; aucun OOM à 5 000 pochettes

### PF-3 — Mémoire < 200 Mo (P0 · S · LNA · ref PF-3/§03 §2)
En tant que **Marc** (vieux téléphone), je veux B Music qui ne fait pas tomber les autres apps afin de garder de la RAM.

- **Étant donné** lecture en cours, bibliothèque 5 000 titres
- **Quand** LNA profile (Android Studio / `dumpsys meminfo`)
- **Alors** **RSS < 200 Mo** ; pas de `largeHeap` ; pas de fuite sur 10 cycles lecture (LeakCanary debug)
- **Et** service arrêté proprement à la fin de file (pas de foreground zombie)

### PF-4 — Lint UnusedResources + APK < 15 Mo (P0 · S · LNA/BIZ · ref PF-4/§03 §7 pt.4)
En tant que **Claire**, je veux un APK léger sans ressources/deps mortes afin d'une base saine.

- **Étant donné** le build release
- **Quand** `lintFdroidRelease` + `dependencyAnalysis` passent
- **Alors** aucune dépendance à zéro référence ; aucune ressource inutilisée non déclarée ; `B-Music.apk` (arm64-v8a) **< 15 Mo**
- **Et** le CI échoue si l'APK ≥ 15 Mo (gate)

---

## Épique SEC — Sécurité (3)

### SEC-1 — Zéro réseau v1 (P0 · S · SEC · ref SC-1/01 §P6)
En tant que **Claire**, je veux la preuve qu'aucune donnée ne sort de B Music afin d'un usage offline intégral en confiance.

- **Étant donné** le build release v1.0
- **Quand** SEC audite : `aapt dump permissions` + packet-trace (mitmproxy 10 min) + grep de URLs
- **Alors** les permissions sont uniquement : `READ_MEDIA_AUDIO` (33+), `READ_EXTERNAL_STORAGE` (≤ 32), `READ_MEDIA_IMAGES` (≤ 32), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE(_MEDIA_PLAYBACK)`, `WAKE_LOCK`, `BLUETOOTH(_CONNECT)` — **aucune `INTERNET`, aucun `ACCESS_NETWORK_STATE`**
- **Et** **0 requête** réseau interceptée en usage normal (scan/play/recherche/playlists/thème)
- **Et** la connexion en mode avion complet (lecture, playlists, notification, thème) fonctionne à 100 %

### SEC-2 — Logs sans PII + purge complète (P0 · S · SEC · ref SC-2)
En tant que **Marc**, je veux une purge totale de mes données (désinstallation = rien de resté) et aucun log avec mes chemins.

- **Étant donné** B Music installée et utilisée
- **Quand** j'ouvre Réglages → À propos → Effacer mes données / désinstalle
- **Alors** les fichiers app sont supprimés/sans résidu hors sandbox ; logs en release silencieux (niveau ≤ ERROR) ; aucun chemin absolu dans les logs
- **Et** aucun fichier de log ne contient titre/artiste/chemin — uniquement des identifiants opaques si besoin

### SEC-3 — Dépendances auditées + licences OSS (P0 · S · SEC · ref SC-3/04-logo)
En tant que **Claire**, je veux la traçabilité des dépendances et leurs licences afin de vérifier qu'elles sont toutes OSS libres.

- **Étant donné** le lockfile (version catalog)
- **Quand** SEC audite (équivalent `npm/cargo audit` manuel via `dependencyInsight` + licence listing)
- **Alors** **0 dépendance propriétaire** ; licences MIT/Apache/BSD/OFL/GPL uniquement ; GPL-3.0 tenue (attribution)
- **Et** la liste des licences OSS est produite (page About / fichier LICENSE.txt) avant release
- **Et** aucun SDK Firebase/Crashlytics/AdMob/Analytics nulle part (vérifié par grep)

---

## Épique RL — Release (3)

### RL-1 — Beta 2 semaines + plan retours (P0 · S · PM/REL · ref RL-3)
En tant que **équipe B Music**, je veux une beta interne de 2 semaines avec un plan de retours structuré avant publication afin de ne pas publier en aveugle.

- **Étant donné** l'APK beta signé
- **Quand** les 8+ testeurs internes l'installent pendant 2 semaines
- **Alors** chaque testeur suit une checklist (installation, premier lancement, 5 000 titres, lecture 2 h, rotation) et remplit un questionnaire (H1–H6 de `01-vision.md`)
- **Et** les retours sont triés J+2/J+7/J+14 : bloquants (patch < 48 h), majeurs (patch < 1 semaine), mineurs (backlog v1.1)
- **Et** aucun blocker ouvert à J+14 → GO v1.0 ; sinon correction + nouvelle beta d'1 semaine

### RL-2 — `B-Music.apk` publié + sources (P0 · S · REL · ref RL-2/ID-3)
En tant que **Claire**, je veux télécharger `B-Music.apk` officiel avec SHA-256 et les sources afin de remplacer l'APK par une build auditable.

- **Étant donné** le tag v1.0.0 release
- **Quand** la publication est effectuée
- **Alors** `B-Music.apk` est disponible (relâché publiquement) avec SHA-256 publié ; les sources correspondantes sont publiques (GPL-3.0) ; notes FR/EN ; app_name « B Music » ; `B-Music.apk` nommé exactement

### RL-3 — Recette complète matrice appareils (P0 · M · PM/SEC · ref RL-1)
En tant que **Marc**, je veux que B Music marche autant sur mon vieil Android Go que sur le flagship de Claire afin de ne pas me sentir exclu.

- **Étant donné** la matrice de recette (cf. `04-acceptation-lancement.md` §1 : Android 8 → dernière, 60 Hz/120 Hz, petit/grand écran, mode avion, 2 h de lecture)
- **Quand** la matrice est exécutée
- **Alors** chaque cas passe (scénario documenté, capture d'écran, device, date) ; **0 bug bloquant** ; **crash-free ≥ 99 %** sur la période beta ; aucun écart visuel grave repéré (voile blanc, crash UI, blocage bouton)

---

## Récapitulatif

| Épique | Stories | P0 (v1.0) | Après v1.0 |
|---|---|---|---|
| Identité | ID-1…4 | 4 | 0 |
| Design system | DS-1…5 | 5 | 0 |
| Animations | AN-1…7 | 7 | 0 |
| Lecteur | PL-1…7 | 6 (PL-1..6) + PL-7 P1 | PL-6 P1(dans v1.0) / PL-8 P1 |
| Bibliothèque | LB-1…6 | 5 (LB-1..5) | LB-6 P2 (Genres) |
| Recherche | SE-1…2 | 2 | 0 |
| Paramètres | ST-1…3 | 2 (ST-1, ST-2) | ST-3 P2 |
| Accessibilité | A11Y-1…4 | 4 | 0 |
| Performance | PF-1…4 | 4 | 0 |
| Sécurité | SC-1…3 | 3 | 0 |
| Release | RL-1…3 | 3 | 0 |
| **Total** | **50** | **46** | **4 (LB-6, ST-3, PL-6 P1, PL-8 P1)** |

**Chemin critique** : ID → DS-1/DS-2 → DS-3 (goulot) → {DS-4, AN-*, LB, SE, A11Y} → PF → SC → RL.
**Estimation totale P0** : ≈ 4–6 semaines-homme (1 dev BIZ + DSG/LNA/SEC ponctuels), hors signature Release et device farm.
