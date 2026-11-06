# B Music — Acceptation & lancement v1.0

> Agent Product Manager B Music. Date : 6 novembre 2026.
> Objectif : **GO publiable** — tous les critères ci-dessous verts, ou retour en correction.
> Sources : budgets `docs/lean/03-budgets-performance.md` · sécurité `security/security-review.md` (Sonore, historique) + exigences v1.0 (zéro réseau) · DoD 10 points (`01-perimetre-mvp.md` §8) · stories `03-user-stories.md` · matrices design `docs/design/03-ecrans.md`.

## 1. Plan de recette

### 1.1 Matrice appareils (cases = à cocher par couple device×scénario)

| # | Profil | Android | Écran / Hz | Rôle | Scénarios obligatoires |
|---|---|---|---|---|---|
| A | Petit budget, 2–3 Go RAM | **8.1** (minSdk) | 5.5″ 60 Hz | Queue de peloton + « Android Go » | S1, S2, S3, S5, S7, S9 |
| B | Milieu de gamme 2023 | **13 / 14** | 6.1″ **120 Hz** | Device de référence (budgets) | **tous S1–S12** |
| C | Grand écran | **14 / 15** | 6.7″ 120 Hz | Comportement "plénitude" + tablette 7″ | S4, S6, S8, S10, S11 |
| D | Haut de gamme récent | **15 / 16** (dernière stable) | 6.2″ 120 Hz | Dernier SDK, thématique icons | S4, S9, S11, S12 |
| E | Émulateur CI | **9 / 12** | 6.0″ 60 Hz | Smoke CI rapide | S1, S2, S3 |

Device de référence pour les budgets = **B** (milieu de gamme 120 Hz) ; device de robustesse = **A** (Android Go). Toute métrique de perf est reportée sur B puis reproduite sur A (seuil dégradé admis uniquement sur A, documenté).

### 1.2 Scénarios de bout en bout

| ID | Scénario | Étapes | Critère de réussite |
|---|---|---|---|
| S1 | Premier lancement, permissions refusées | Installer → ouvrir → refuser l'accès audio → parcours | Message actionnable « Autoriser l'accès audio » + bouton ; aucune fuite ; re-demander ou guider Réglages système ; app stable |
| S2 | Premier lancement, permissions accordées | Accepte → scan démarre | Bibliothèque ≥ 5 000 titres affichée **< 5 s** ; écran vide sinon ; aucune permission réseau demandée (`aapt dump permissions` à re-vérifier) |
| S3 | Bibliothèque vide | Aucun fichier audio → lancer scan | État vide soigné (illustration + « Aucun titre » + bouton « Choisir un dossier ») ; pas de crash ; Scan re-essai OK |
| S4 | 5 000 titres complets | 5 000 fichiers → navigation, recherche, tri | Recherche < 150 ms ; grille d'albums 2 col. ; aucune erreur sur 100 titres lus au hasard ; mémoire < 200 Mo (profiler) |
| S5 | Lecture longue 2 h | Lire 2 h en continu avec écran éteint | 0 crash ; batterie/Wakelocks normaux (`dumpsys batterystats`) ; reprise notification/wake lock propre ; service média persistant |
| S6 | Rotation écran | Cold start → rotation 0°→90°→0° sur Accueil/Lecteur/File | Pas de crash, pas de perte de position ni de mise en page brisée ; mini-player persistant |
| S7 | Thème sombre ↔ clair ↔ système | Bascule à chaud sur lecteur en cours | Transition instantanée sans redémarrage ; mêmes durées animées ; aucun texte illisible |
| S8 | Transfert interruptions | Appel entrant + autre média prend focus | Suspension/reprise ≤ 0,5 s ; jamais deux lectures en même temps |
| S9 | Hors ligne total (mode avion) | Avion ON dès l'installation → usage complet | 100 % fonctionnel : scan/lecture/playlists/favoris/notification/réglages ; packet-trace = 0 requête |
| S10 | Bluetooth/casque | Raccord + débranchement + voiture | Pause au débranchement/déconnexion ; reprise à la reconnexion ; focus cédé/repris |
| S11 | Rémanence kill/relance | Kill app en lecture → relance | File restaurée ; favoris idem ; position restaurée ; aucun crash |
| S12 | Verrouillage 30 min | Laisser jouer verrouillé 30 min | Notification média fonctionnelle (pochette, 3 actions) ; « capture le chemin fichier » absent (VISIBILITY_PUBLIC sobre) ; déverrouille → app reprend là où elle en était |

### 1.3 Exécution

- Déroulage **manuel** sur A/B/C/D + smoke automatisé (S1–S3) sur E en CI.
- Chaque cas = capture d'écran + device + date + signature QA dans le tableau de recette.
- Crash pendant la recette → stop, bug bloquant, retour en correction + re-cycle intégral des cas S1–S12 (pas de recette partielle).

## 2. Checklist « Définition de terminé v1.0 »

### 2.1 Fonctionnel (stories 03)

- [ ] **ID-1…4** : `B-Music.apk`, label « B Music », icônes adaptive + monochrome, About + licences GPL-3.0 à jour.
- [ ] **DS-1…5** : tokens sombre/clair appliqués partout, accent dynamique confiné (3 éléments), zéro blur/glass (grep vide).
- [ ] **AN-1…7** : mini→plein (spring 0,86/380, drag suivi du doigt), play/pause morph 220 ms + rebond, progression frame-by-frame, listes stagger 24 ms (max 8), reduced-motion OK.
- [ ] **PL-1…7** : lecture, file réordonnable + persistante, notification média + lockscreen, focus audio/casque/BT/appel, shuffle/repeat persistants, gapless dégradé proprement.
- [ ] **LB-1…5**, **SE-1…2**, **ST-1…2** : bibliothèque + scan < 5 s, favoris, playlists CRUD + .m3u, recherche < 150 ms, thème + accent dynamique.
- [ ] **A11Y-1…4** : AA partout, cibles ≥ 48 dp, TalkBack sur les 6 parcours, FR défaut + EN complet.

### 2.2 Budgets Lean (cf. `docs/lean/03-budgets-performance.md`) — device B

- [ ] **APK** : `B-Music.apk` **< 15 Mo** (arm64-v8a) ; universal non produit ; R8 + `shrinkResources` vérifiés.
- [ ] **Démarrage** : froid **< 600 ms** (médiane Macrobenchmark), chaud **< 250 ms** ; Baseline Profile embarqué.
- [ ] **Jank / frames** : défilement 5 000 titres **0 frame perdue**, **< 1 % janky** (`dumpsys gfxinfo framestats` + Macrobenchmark) ; Now Playing/seek transitions ≤ 16 ms (ou 8 ms @120 Hz).
- [ ] **Mémoire** : RSS lecture **< 200 Mo** ; Coil `memoryCacheSize = 15 %`, `diskCacheSize = 128 Mo` ; pas de `largeHeap`.
- [ ] **Batterie** : pas de boucle d'animation en arrière-plan ; FGS `mediaPlayback` arrêté à la fin de file ; `dumpsys batterystats` propre sur 2 h.

### 2.3 Sécurité (rapport `security/` — v1.0 actualisé)

- [ ] **SEC-1 zéro réseau** : `aapt dump permissions` sans `INTERNET`/`ACCESS_NETWORK_STATE` ; packet-trace 10 min (mitmproxy) = **0 requête sortante** ; CSP/webview sans ressource distante.
- [ ] **SEC-2 path/traversal** : aucun accès hors dossiers autorisés (test `../`, symlink) — vérifié côté fork (fichiers/uris MediaStore uniquement ; pas de lecteur arbitraire).
- [ ] **SEC-3–6** : pochette ≤ 20 Mo / décodage borné, fichier piégé ignoré règle + toast, jamais de `innerHTML` sur données de tags.
- [ ] **SEC-7–10** : pas de commande IPC générique ; logs ≤ ERROR en release, sans PII ; **aucune télémétrie** (grep analytics/crashlytics/sentry → vide).
- [ ] **SEC-11–13** : lockfile + licence listing (0 dépendance propriétaire/Firebase/AdMob) ; `READ_MEDIA_*` minSdk correct ; `meta-data` Android Auto retiré en v1, `largeHeap` supprimé, `requestLegacyExternalStorage` supprimé.
- [ ] **Verdict Security** : aucune faille **critique ou haute ouverte** ; checklist pré-release verte ; attribution GPL-3.0 complète (texte LICENSE + mentions README/About) ; **sources publiées**.

### 2.4 Release prête

- [ ] Matrice recette §1 verte (tous S1–S12, devices A–E) ; **0 bug bloquant**, **crash-free ≥ 99 %** beta.
- [ ] `B-Music.apk` signé ; **SHA-256 publié** ; notes de release FR/EN ; `versionName`/`versionCode` cohérents.
- [ ] Sources du tag publiées ; `LICENSE.txt` à jour ; README FR/EN.
- [ ] DoD 10 points `01-perimetre-mvp.md` §8 cochée (compile fdroid release sans clé étrangère requise).
- [ ] Plan retours §4 actif (J+0 → J+14) avec canal dédié et propriétaire désigné.

### 2.5 Décision GO / NO-GO

- **GO** — toutes les cases cochées. Sinon **NO-GO** : retour au jalon concerné (Alpha/Bêta/Security) avec relève intermédiaire d'1 semaine max + re-validation complète.

## 3. Fiche de publication

### 3.1 Description courte (≤ 80 caractères)

- **FR** : `Lecteur local Android : fluide, sobre, zéro pub, zéro compte, 60/120 fps.` *(74 caractères)*
- **EN** : `Local Android player: fluid, clean, no ads, no account, 60/120 fps.` *(66 caractères)*

### 3.2 Description longue

**FR**

> **B Music** est le lecteur de musique local pour Android : vos fichiers (MP3, FLAC, OGG, AAC, M4A), sans compte, sans publicité, sans réseau.
>
> • **Fluide** — 60/120 fps, démarrage < 600 ms, pochettes en héros (style Material 3 opaque, zéro verre, zéro flou).
> • **Sobre** — pas de pubs, pas de trackers, pas de compte ; thèmes sombre/clair, accent dérivé de la pochette.
> • **Vôtre** — 100 % hors ligne ; vos données ne quittent pas l'appareil (zéro autorisation réseau).
> • **Complet** — titres, albums, artistes, playlists, recherche locale (< 150 ms), favoris, file d'attente réordonnable, notification média + écran de verrouillage.
> • **Open source** — GPL-3.0 ; code public ; attribution RetroMusic Player / Booming Music / B Music.
>
> Minimum Android 8.0. APK `B-Music.apk` (arm64-v8a), < 15 Mo. Français par défaut, English also included.

**EN**

> **B Music** is the local music player for Android: your files (MP3, FLAC, OGG, AAC, M4A), no account, no ads, no network use.
>
> • **Fluid** — 60/120 fps, under 600 ms cold start, album art as the visual hero (opaque Material 3, zero glass, zero blur).
> • **Clean** — no ads, no trackers, no account; dark/light themes, accent derived from the artwork.
> • **Yours** — 100% offline; nothing leaves your device (no network permission).
> • **Complete** — tracks, albums, artists, playlists, instant local search (< 150 ms), favorites, reorderable queue, media notification + lockscreen.
> • **Open source** — GPL-3.0; code public; attribution RetroMusic Player / Booming Music / B Music.
>
> Minimum Android 8.0. APK `B-Music.apk` (arm64-v8a), < 15 MB. French default, English also included.

### 3.3 Mots-clés

`lecteur musique local, musique hors ligne, music player, offline music, FLAC, MP3, lecteur audio, music player Android, sans pub, sans compte, open source, lecteur local, B Music, B-Music.apk`

### 3.4 Captures d'écran (5, sobres — fond `#0B0B0E`, aucun cadre marketing surchargé)

1. **Accueil** — grille d'albums 2 colonnes, pochettes carrées R 12, Display « Bibliothèque », mini-player en bas.
2. **Lecteur plein écran** — pochette grande (≈ 80 % largeur), titre 22/28, artiste en accent, cercle lecture 72 dp, barre de progression — capture à 120 Hz si possible.
3. **File d'attente** — liste réordonnable, pochette + handle ≡, en-tête « 24 titres · 1 h 32 ».
4. **Recherche** — champ focus, résultats groupés Titres/Albums/Artistes sur fond uni, état « Aucun résultat » soigné non nécessaire ici.
5. **Réglages** — sections Apparence (thème, accent dynamique) / Lecture / À propos (version + licences).

Règles screenshots : aucun mock-up device flottant ; aucune photo utilisateur ; aucune stat inventée ; captures 1080×2340 ou 1440×3040, exportés optimisés (WebP lossless).

### 3.5 Texte de présentation (fiche courte type « À propos »)

> B Music est un lecteur de musique libre, hors ligne, pour Android. Il lit vos fichiers locaux, sans pub, sans compte, sans réseau — avec un design sombre soigné et une fluidité 60/120 fps. Open source (GPL-3.0).

### 3.6 Politique de confidentialité

> **B Music ne collecte, ne transmet et ne partage aucune donnée personnelle. Aucune donnée n'est collectée.**
> Aucune autorisation réseau n'est utilisée ; aucune télémétrie, aucun analytics, aucune publicité. Toute la bibliothèque, les favoris, les playlists et les préférences restent sur votre appareil et disparaissent à la désinstallation. Aucun compte n'est requis. Contact : dépôt public (lien dans l'écran À propos).

## 4. Plan de retours utilisateurs — 2 premières semaines

### 4.1 Canal & collecte

- **Canal principal** : issue tracker du dépôt public (template « bug » FR/EN : device, Android, build `B-Music.apk` version, étapes, attendu/observé, logcat facultatif).
- **Canal secondaire** : questionnaire beta (Google Form ou formulaire self-hosted statique — pas d'analytics) à J+7 et J+14 pour les testeurs signés.
- **Triage** : PM + 1 contributeur quotidien (min. 1 passage / 24 h en semaine) ; aucun canal privé/mail (évite la fuite d'enregistrements personnels).

### 4.2 Taxonomie & SLA

| Classe | Exemples | SLA |
|---|---|---|
| **Bloquant** | crash au scan/lecture, APK ne s'installe pas, perm refusée puis bloqué, perte de la file | patch **< 48 h** |
| **Majeur** | recherche casse accents, transition mini→plein saccadée, notification média perdue | fix **< 1 semaine** |
| **Mineur** | typo FR/EN, alignement écran, contraste limite | backlog **v1.1** |
| **Won't** | streaming, compte, EQ, widgets, Android Auto | **refusé** (hors v1.0 — cf. `01-vision.md`) ; réponse type documentée |

### 4.3 Cadence

- **J+0** : publication + annonce sulcule (dépôt, mastodon/framasphere ou simple tag — selon canal public choisi).
- **J+1 → J+3** : veille active ; premier bilan (installation count, retours, bugs).
- **J+4 / J+7** : questionnaire beta intermédiaire ; triages ; correctifs 1.0.1 pour bloquants.
- **J+7 / J+14** : deuxième bilan ; **1.0.1/1.0.2** si besoin ; note « résolu/wontfix » sur chaque issue.
- **J+14** : **gate GO v1.1** — décision documentée (continuer, patch final, ou geler) + rétrospective courte (ce qui a marché / ce qui n'a pas marché, 1 page) dans `docs/pm/05-retro-j14.md`.

### 4.4 Garde-fous

- Aucun retour utilisateur ne déclenche de nouvelle dépendance réseau ou tracking — jamais.
- Aucune fonctionnalité n'entre « en urgence » dans v1.0 après cette porte — ce serait un cycle de plus.
- Si ≥ 3 testeurs remontent le **même** problème majeur → patch prioritaire même hors roadmap.
