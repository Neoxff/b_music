# B Music — Vision (v1.0)

> Agent Product Manager B Music. Date : 6 novembre 2026.
> Sources : état réel du dépôt (`app/`, `design/`, `pm/`, `security/`, audit Lean `02-audit-gaspillage.md`) + périmètre Lean (`01-perimetre-mvp.md` : fork BoomingMusic v1.4.2, flavor `fdroid`, offline, FR/EN, `B-Music.apk`).
> Licence base : GPL-3.0 (attribution RetroMusic/Booming conservée — voir `01-perimetre-mvp.md` §8.10).
> Note : `pm/mvp-spec.md` (Sonore, Tauri 2) est **historique** — la décision Lean a tranché pour un fork natif Android. Ce document s'applique à cette décision.

## 1. Vision en une phrase

**« B Music : le lecteur de musique local le plus fluide et le plus beau — sans pub, sans compte, sans une seule donnée qui sort de votre téléphone. »**

**Promesse produit** : toute la collection de l'utilisateur (MP3/FLAC/OGG/AAC/M4A) devient une expérience premium — pochettes héroïques, 60/120 fps, zéro réseau, zéro distraction — pour moins de **15 Mo** et **< 600 ms** à froid.

## 2. Personas (profils ancrés dans le produit)

### Persona 1 — Léa, 24 ans, étudiante / photographe, collection de 2 400 titres
- **Appareil** : Android milieu de gamme (6 Go RAM, écran 120 Hz), sans forfait data abondant (Wi-Fi seulement).
- **Usage** : écoute quotidienne (trajets, editing photo), playlists de travail ; pochettes = critère fort (elle photographie ses albums physiques).
- **Frustrations actuelles** : les lecteurs grand public affichent des pubs et poussent des comptes ; les OSS récents (fossify…) sont lisses mais lents à l'ouverture ; les designs « material you » de certains forks sont lourds et criards.
- **Ce qui la fait adopter** : ouverture instantanée, mini-player + lecteur plein écran 60/120 fps, pochettes en héros visuel, accent dynamique subtil, zéro pub et zéro compte.
- **Déclencheur d'adoption** : "tap → son < 200 ms, interface silencieuse, et mes pochettes à l'honneur".

### Persona 2 — Marc, 41 ans, chauffeur-livreur, 5 000+ titres, vieil appareil
- **Appareil** : Android Go / ancien téléphone (Android 8–10), 2–3 Go RAM, écran 60 Hz, batterie fatiguée.
- **Usage** : 6–8 h/jour en fond sonore (névralgique : ne doit **jamais** s'interrompre), signalisation casque + Bluetooth voiture.
- **Frustrations actuelles** : apps lourdes qui drainent la batterie, notifications intrusives, mise à jour qui casse la lecture, lecteurs qui perdent la file d'attente après redémarrage, écran de verrouillage mal contrôlé.
- **Ce qui le fait adopter** : notification média + lockscreen robustes (3 actions, pochette, seek), reprise de lecture après appel/Bluetooth, CPU minimal (pas de widgets inutiles), **B-Music.apk** offline qui ne demande rien.
- **Déclencheur d'adoption** : "il joue partout, tout le temps, sans jamais me demander mon Google".

### Persona 3 — Claire, 34 ans, développeuse, Linux/Android, bibliothèque 5 000+ titres lossless
- **Appareil** : flagship Android, écran 120 Hz, émulateur Linux pour tester.
- **Usage** : FLAC haute résolution, dossiers plantés proprement, veut contrôler la bibliothèque (listes blanches), open source ou rien.
- **Frustrations actuelles** : aucun lecteur n'offre un design « à couper le souffle » **et** un vrai moteur FLAC/ordonnancement ; les forks Retro/Booming ont l'air daté ; les stores bourrés de trackers (légalité/politiques de confidentialité floues).
- **Ce qui la fait adopter** : GPL-3.0 + sources publiables, zéro télémétrie vérifiable (packet-trace clean), thème sombre soigné, réordonnancement de file fluide, playlists smart, FR/EN, APK reproductible.
- **Déclencheur d'adoption** : "GPL-3.0, airplane mode à 100 %, et un égaliseur en v1.1 quand la base sera fiable".

## 3. Analyse concurrentielle (5 lecteurs Android)

> Chiffres qualitatifs prudents ; pas de métriques inventées. Forces/faiblesses = consensus public + lecture des sources disponibles.

| Lecteur | Type / licence | Forces | Faiblesses | Ce que B Music fait mieux |
|---|---|---|---|---|
| **Booming Music** (base, v1.4.2) | OSS GPL-3.0 (fork Retro) | Bibliothèque complète (smart playlists, queue, EQ), Media3/Compose, FR/EN, actif (nov. 2026), zéro tracker | Design générique « material », réseau activé par défaut (flavored), updater intégré, univers écran chargé, icônes/identité datées | **Beauté + fluidité** : nouveau design system (pochette héro, surfaces opaques, Inter), **v1 100 % offline**, animations ressort, `B-Music.apk` signé, zéro réseau par construction |
| **Retro Music Player** (amont) | OSS GPL-3.0 — plus faiblement maintenu | UX éprouvée (2016–2023), playlists, Auto | Code historique fragmenté, Material 2 hérité, lenteurs sur vieux appareils | Base identique mais **refonte design + nettoyage** (voir `02-audit-gaspillage.md`) |
| **Musicolet** | Freeware (closed source), gratuit | Léger, offline réel, pochettes robustes, queue très bon, audible sur appareils modestes | Design utilitaire « façon 2016 », pas d'infos cover dynamiques, pas open source → audit impossible | Même sobriété + **open source auditable, design premium, 60/120 fps animations, FR/EN** |
| **Fossify Music Player** | OSS (Fossify, forks de Simple) | Minimal, rapide sur appareils modestes, offline | Trop minimal (pas de playlists avancées/queue riche), finition graphique faible, accent unique | **Compromis fonctionnalité/beauté** : bibliothèque complète + finition premium, sans jamais « plus de boutons que d'air » |
| **Poweramp** | Propriétaire, payant | Moteur audio légendaire (EQ, resampler), skins, 10+ ans | Hors ligne payant, pas open source, UI complexe/jargonneuse, écrans denses | OSS libre, **zéro jargon**, design system unique, cible 15 Mo vs ~30+ Mo, APK offline direct |

**Positionnement B Music** (2×2 mental) : plus beau que Booming/Retro/Musicolet, plus fonctionnel que Fossify, infiniment plus offline/privé que Poweramp, gratuitement — pour qui veut écouter, pas configurer.

## 4. Principes produit (5 maximum — servent d'arbitrage)

1. **La beauté et la fluidité passent avant le nombre de fonctionnalités.** Si une fonctionnalité ne rend pas B Music plus beau, plus fluide ou plus fiable à lire, elle est différée (`01-perimetre-mvp.md` §3).
2. **Zéro liquid glass.** Aucune transparence décorative, aucun flou, aucun reflet : surfaces opaques, élévation tonale, 1 px séparateurs. La couleur vient des pochettes — avec parcimonie.
3. **Offline absolu — zéro donnée sortante.** Scan, lecture, bibliothèque, favoris, notification : tout fonctionne 100 % hors-ligne, sans compte, sans analytics ; l'APK est distribuable sans store (vérification `adb`/packet-trace par Security).
4. **La pochette est l'UI.** L'intérêt visuel provient des pochettes ; l'interface reste silencieuse, les accents dynamiques n'envahissent jamais l'écran.
5. **Lisible par tous, utilisable par les appareils modestes.** Contraste AA obligatoire, cibles ≥ 48 dp, 60 fps sur Android Go/60 Hz, APK < 15 Mo.

## 5. Métriques de succès

| Métrique | Cible v1.0 | Comment mesurée |
|---|---|---|
| Taux de crash (sessions) | **< 0,3 %** | Play Console / beta Firebase-Crashlytics-free → suivi beta interne + `logcat` ; crash-free sessions note 4,6+ |
| Démarrage à froid (FROID) | **< 600 ms** jusqu'à premier écran | Macrobenchmark (`03-budgets-performance.md` §4) — médiane 10 runs |
| Jank notable à la lecture | **0 notable** ; < 1 % frames > budget | `dumpsys gfxinfo framestats` + Macrobenchmark FrameTimingMetric — 0 frame perdue défilement bibliothèque |
| Note store / beta | **≥ 4,6/5** | beta interne puis store (si publié) ; critère qualitatif + note beta |
| Rétention J7 | **≥ 40 %** des installs actives semaine 2 (beta) | suivi beta (local, opt-in, aucun tracking — comptage agrégée via questionnaire beta, pas d'analytics) |
| Mémoire RSS lecture | < 200 Mo (5 000 titres) | Profiler — `03-budgets-performance.md` §2 |

Notes honnêtes : rétention J7 sans télémétrie = **hypothèse mesurée par beta volontaire** (questionnaire), pas par analytics — déclaré comme telle. Les cibles 4,6+/0,3 %/600 ms sont des **engagements design**, pas des stats observées (aucun device mesuré aujourd'hui).

## 6. Risques produit et hypothèses à valider

### Risques (P × I + plan)

| Risque | P × I | Mitigation |
|---|---|---|
| R1 — Fork complexe (deletion réseau mal ciblée casse le build) | M × É | Audit `02-audit-gaspillage.md` = liste exacte des suppressions ; build CI par jalon ; jamais de gros bang — suppression par lots (Réseau → Widgets → Tags → Android Auto) |
| R2 — Trop peu de valeur perçue (« c'est Booming remappé ») | M × É | Design system complet (`01-design-system.md`) appliqué **partout**, animations ressort, logo propre ; démo courte 60–90 s en beta |
| R3 — Budgets non tenus (arm64 ≥ 15 Mo, jank > 1 %) | M × M | Règle « stop » (`03-budgets-performance.md`) opposable ; APK gate en CI ; Lint + macrobench avant Chaque merge |
| R4 — Matériaux GPL-3.0 non attribués (violation licence) | F × É | Écran About + LICENSE + mentions dans README APK — DoD #10 ; revue Release |
| R5 — Adoption faible / confusion nom (« B Music » vs « Booming ») | M × M | Nom officiel unique « B Music », `B-Music.apk`, pas de confusion logo (piste C unique) |
| R6 — Régression sur Android 8–10 (limite minSdk 26) | M × M | Matrice recette (`04-acceptation-lancement.md`) couvre Android 8/10/13/14/16 ; flash sur appareils modestes en beta |
| R7 — Splash/animations > 400 ms dégradent le ressenti | M × M | Splash 350–400 ms max (`04-logo.md`) ; reduced-motion honore le système |

### Hypothèses à valider (avec source de vérité)

1. **H1 — « La beauté fait adopter »** : valider que la refonte design (et non des features) est le facteur d'adoption déclaré → interview beta interne J+7 (n ≥ 8), question standardisée.
2. **H2 — « L'offline absolu est un différenciateur** » : valider que l'absence de compte/pub/réseau est citée spontanément → beta Q&A ; sinon pivoter le message (offline reste, mais différenciateur = beauté+GPL).
3. **H3 — « < 600 ms froid est tenable sur milieu de gamme 2023 »** : Macrobenchmark sur device cible (à obtenir en J1) ; si > 600 ms, invest Baseline Profiles avant features.
4. **H4 — « 5 000 titres scannés < 5 s »** : benchmark Room/MediaStore sur 5 000 fichiers dès Alpha (sinon penser index incrémental).
5. **H5 — « La liste d'écrans 8 suffit »** : aucun écran "manque" pour un usage quotidien complet (valider par scénarios `04-acceptation-lancement.md`).
6. **H6 — « FR par défaut + EN ne perturbe pas le public anglophone »** : strings externalisées dès Alpha ; vérifier EN complet avant Bêta.
