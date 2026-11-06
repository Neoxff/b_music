# B Music v1.0 — périmètre MVP

> Document de décision. Direction : produit Android **B Music**, APK attendu `B-Music.apk`.
> Date : 6 novembre 2026.
> Auteur : agent Lean B Music.

## 1. Base retenue

**Base choisie : [mardous/BoomingMusic](https://github.com/mardous/BoomingMusic)**

Pourquoi :

- Lecteur Android **local-first** ; aucun flux de streaming obligatoire.
- Fork GPL-3.0 de **RetroMusicPlayer**, avec un écran "Material-driven" déjà moderne et activement maintenu (v1.4.2, commits au 2 novembre 2026).
- Licence **GPL-3.0** ; la dérivation est autorisée, à condition de publier les sources, conserver la licence et les mentions de copyright.
- **Aucun Firebase, AdMob, Crashlytics, analytics ou SDK de tracking** détecté dans les dépendances du dépôt.
- Flavor `fdroid` avec `network_features_enabled_by_default=false`, `enable_builtin_updater=false` : c'est ce flavor qui servira de base à B Music v1.0. Les fonctionnalités réseau (paroles en ligne, Last.fm, mise à jour intégrée) seront **supprimées du build v1**, pas seulement masquées.
- Stack moderne : Kotlin/JVM 21, Jetpack Compose + Material 3, Room, Media3, Glance pour widgets, Coil, Navigation Compose, Koin, Gradle `minSdk 26 / targetSdk 36 / compileSdk 37`.
- Code en français (`values-fr-rFR`), anglais présent par défaut : FR/EN externalisés déjà en place.

Bases écartées :

- `RetroMusicPlayer/RetroMusicPlayer` : licence et fonctionnalités OK, mais code historique/XML plus lourd ; l'objectif v1 exige une refonte d'expérience, pas davantage de legacy à conserver.
- `zyrouge/symphony` : local/AGPL et élégant, mais moins complet en bibliothèque/queue pour un usage quotidien robuste.
- `bmaroti9/Citole` : Offline/Material 3 Expressive prometteur, mais trop jeune/en construction.
- Repos type `spotube`, `Musify`, `Levyra` : streaming/compte en ligne → hors périmètre.

## 2. État du dépôt à la date d'analyse

| Domaine | État dans le dépôt | Verdict |
|---|---|---|
| Lecture locale | Media/ExoPlayer, file, reprise lecture | Disponible |
| Dossiers, bibliothèque | Titres/albums/artistes, favoris, playlists, smart playlists | Disponible |
| Écran Now Playing | Refonte Material 3, thèmes | Disponible, à améliorer |
| Widgets | Glance + config | Disponible, à reporter |
| Éditeur de tags | Édition locale | Disponible, à reporter |
| Égaliseur | 15 bandes, AutoEq | Disponible, à reporter en v1.1+ |
| Sleep timer | Disponible | Disponible |
| Paroles | LRC/TTML + téléchargement en ligne | Présent mais à supprimer en v1 |
| Last.fm / scrobbling | Optionnel, besoin de clés/réseau | À supprimer |
| Android Auto | Support présent | À reporter |
| Mise à jour intégrée | `github`/`playstore` flavors | À supprimer |
| Réseaux / login | Aucun login, mais services en ligne optional | Supprimer du v1 |
| Tracking | Aucun détecté | Conserver cet état |
| Langue | FR + EN ; strings externalisées | Conserver/étendre |
| Cache/db | Room Schéma versionné | Utilisable, à conserver |
| Build | Gradle KTS, ABI splits, APK rename | À adapter pour `B-Music.apk` |
| APK cible | `BoomingMusic-<version>-fdroid-universal.apk` | Doit devenir `B-Music.apk` |

## 3. Principe de tri

Une fonctionnalité **reste dans v1.0** uniquement si elle améliore **au moins l'un** des trois points :

1. la fiabilité de la lecture,
2. la beauté/fluidité de l'interface,
3. ou l'indispensable gestion locale d'une bibliothèque.

Sinon, elle est **supprimée, reporter-ée ou désactivée**.

## 4. Tableau décisionnel B Music v1.0

| Fonctionnalité | État dans le dépôt | Décision | Effort | Valeur (1–5) | Justification |
|---|---|---|---|---|---|
| Lecture locale (MP3/FLAC/OGG/AAC/M4A) | Existant | Garder | M | 5 | Cœur de l'app |
| Tri/filtrage dossiers | Existant | AMÉLIORER | M | 4 | Gestion locale cohérente |
| Bibliothèque (titres/albums/artistes) | Existant | Garder | M | 5 | Exigence MVP bibliothèque |
| Recherche locale (titres/albums/artistes) | Existant | Garder | S | 5 | Recherche rapide offline |
| Écran Now Playing / lecteur plein écran | Existant | AMÉLIORER | M | 5 | Vitrine produit + animation 60/120 Hz |
| Mini-player fixe | Existant | Garder | S | 5 | MVP explicite |
| Barre de progression + seek | Existant | Garder | S | 4 | MVP explicite |
| Boutons précédent/suivant/lecture/pause | Existant | Garder | S | 5 | Indispensable |
| Queue d'attente + réordonnancement | Existant | Garder | M | 5 | MVP explicite |
| Shuffle / repeat (off-one-all) | Existant | Garder | S | 4 | MVP indispensable |
| Volume | Existant | Garder | S | 3 | Un device mobile peut le faire, mais utile |
| Notification média + lockscreen | Existant | Garder | M | 5 | MVP explicite |
| File/perte network Enabled (flavor fdroid) | `network_features_enabled_by_default=false` | Conserver + renforcer | S | 5 | Pas de données sortantes |
| Playlists utilisateur (create/import/rename/delete) | Existant | Garder | M | 5 | MVP bibliothèque |
| Playlists intelligentes (most played, recents) | Existant | AMÉLIORER | M | 3 | Valeur si coût faible |
| Favoris | Existant | Garder | S | 4 | Standard |
| Genre | Existant | Garder | S | 3 | MVP bibliothèque (option) |
| Dossiers noirs/blancs | Existant | Garder | M | 3 | Gestion bibliothèque propre |
| Widgets | Existant | REPORTER | M | 2 | MVP ne l'exige pas |
| Éditeur de tags | Existant | REPORTER | M | 2 | Non requis pour écouter |
| Paroles en ligne (LRC/TTML) | Existant | SUPPRIMER | M | 1 | Réseau + hors produit |
| Paroles intégrées (tags lyrics locaux) | Existant | REPORTER | S | 2 | Non-destructif mais non requis |
| Last.fm / scrobbling | Optionnel | SUPPRIMER | M | 1 | Données sortantes |
| Android Auto | Existant | REPORTER | L | 2 | MVP n'exige pas la voiture |
| Chromecast | Existant | SUPPRIMER | M | 1 | Réseau + hors MVP |
| EQ/AutoEq | Existant | REPORTER | M | 2 | Excellent plus tard, pas v1 |
| ReplayGain / gapless | Existant | Garder | M | 3 | Lecture confortable et fiable |
| Thème clair/sombre (AMOLED) | Existant | AMÉLIORER | M | 5 | MVP explicite + beauté |
| Accents Material You | Existant | Garder | S | 4 | 1 couleur court-circuitée, pas de dégradés |
| "Driving mode" / dashboard | Existant | REPORTER | M | 2 | Option usage mains libres |
| Mise à jour intégrée | Existant | SUPPRIMER | S | 0 | Réseau |
| Widgets home screen Glance | Existant | REPORTER | M | 2 | Pas pour v1 |
| Traduction FR/EN | Existant | Garder | S | 4 | FR par défaut + EN |
| Import playlist `.m3u` | Existant | Garder | S | 3 | Support playlists locales |
| Liste blanche/noire de dossiers | Existant | Garder | M | 3 | Contrôle bibliothèque |
| Gapless playback | Existant | Garder | M | 3 | Continuité audio |
| File d'erreurs / crash handler | Existant | Garder | S | 4 | Fiabilité |
| AR: publicité/tracking dépendances | Aucune | S'assurer reste absente | M | 5 | Zéro données |
| Build `fdroid` seulement | Existant | Garder | S | 5 | Exclut network/update par défaut |
| Renommer APK → B-Music.apk | À faire | Garder | S | 4 | Contrainte produit |
| Package id `com.bmusic` | À faire | Garder | M | 4 | Aucune dépendance à l'ancien id |
| Mentions licence/copyright | À ajouter FR/EN | Garder | S | 5 | Obligation GPL-3.0 |

### 4.1 Découpage

**GARDER TEL QUEL (valider + polir minuscule)**
Lecture locale, shuffle/repeat, queue, recherche, favoris, playlists, genres, gapless, import/export .m3u, file blacklist, crash handler, FR/EN, `fdroid` flavor, zero tracking.

**AMÉLIORER (design + animation)**
Now Playing/mini-player/seek, dark/AMOLED + Material You motion, bibliothèque (listes → cartes), recherche, thème global, layout, APK rename, package id, attribution.

**SUPPRIMER**
Réseaux/updater, Last.fm, lyrics en ligne, Chromecast, publicité/tracking (déjà absents mais verrouillés).

**REPORTER**
EQ, widgets, tag editor, paroles locales, Android Auto, driving mode, ReplayGain, smart playlists avancées, recommandation/vieux `Home` feed.

## 5. MVP B Music v1.0

### Nom & distribution
- Produit : **B Music**
- APK final : **B-Music.apk**
- Distribution : fichier APK offline, pas de store requis.
- Licence : GPL-3.0 ; attributions RetroMusicPlayer et Booming Music conservées.

### Fonctionnalités incluses
1. Lecture audio locale depuis le stockage local.
2. File d'attente (ajout, réordonnancement, suppression, dossier, sauvegarde d'état minimal).
3. Contrôles lecture/pause/précédent/suivant.
4. Barre de progression + seek.
5. Mini-player permanent hors écran lecture complète.
6. Lecteur plein écran avec pochette, titre/artiste/album.
7. Bibliothèque : titers, albums, artistes, favoris, playlists, genres.
8. Recherche locale sur titre/artiste/album.
9. Lecture en background avec notification média + lockscreen.
10. Mode sombre/clair (+ AMOLED optionnel).
11. Support FR/EN strings externalisées ; FR par défaut.

### Hors MVP v1.0
Tout ce qui nécessite un compte, internet, données personnelles, ou expérience non essentielle à l'écoute locale.

## 6. Règles produit

Interdit dans v1.0 :
- aucun service réseau externe requis pour écouter (zéro upload, zéro analytics),
- sensible localement uniquement ; aucun compte, aucun profil, aucune sync cloud,
- aucun tracking SDK, AdMob, Firebase,
- aucune dépendance propriétaire inutile,
- aucun glassmorphism / blur / gradients bruyants ; surfaces opaques, bords propres,
- 60 fps min, 120 fps si device supporte,
- `B-Music.apk` pour tous les releases ; aucun network call requis pour l'UI ou la lecture.

## 7. Hors périmètre v1.0

1. **Streaming en ligne / Spotify / YouTube / comptes / sync cloud.**
2. **Paroles synchronisées en ligne** (affichage lié à API externe).
3. **Égaliseur avancé** (15 bandes + AutoEq + presets partageables).
4. **Cast, Android Auto, widgets, wearable/tablette avancés.**
5. **Éditeur de tags et métadonnées batch** (rename/split/rewrite), minage de cover art par réseau.

## 8. Définition de terminé (10 points vérifiables)

1. Le dépôt B Music compile `./gradlew :app:assembleFdroidRelease` sans clé étrangère requise.
2. L'artifact final est nommé exactement `B-Music.apk`.
3. Aucune requête réseau sortante n'est visible pendant : scan, lecture, playlists, favoris, mode sombre/clair, notification (vérifié via `adb shell dumpsys netstats` et logs).
4. Mode avion complet : scan, lecture, playlists, notification média, mode sombre/clair fonctionnent à 100%.
5. La lecture locale fonctionne sur ≥ 3 devices (API 26, 31, 36), incluant FLAC lossless sans crash dans une file ≥ 100 titres.
6. `Now Playing` et mini-player maintiennent ≥ 60 fps pendant seek/barre de progression et transition (mesuré avec Macrobenchmark/FrameMetrics).
7. Les écrans bibliotecaires (titres/albums/artistes/favoris/playlists) s'affichent en < 1.5 s sur 1 961 titres de test, sans jank notable.
8. Notification média + lockscreen affichent titre/artiste, icônes play/pause/prev/next/seek, pochette locale, couverte au verrouillage.
9. UI 100% externalisée FR/EN (`values/strings.xml`, `values-fr/strings.xml`, `values-en` implicite) ; aucune chaîne brut en dur dans le code B Music.
10. Licence/attribution : `LICENSE`, `README.md` et écran About affichent GPL-3.0, copyright RetroMusicPlayer, Booming Music and dérivé; APK inclut licence texte.

## 9. Ce qui est fait, reste, risques

### Fait
- Décision de base : **mardous/BoomingMusic** (Topic `android-music-player`; 600+ stars; v1.4.2; active 2 novembre 2026; no Firebase/Crashlytics/analytics detected; fork of RetroMusicPlayer GPL-3.0).
- Audit rapide des fonctionnalités, build flavors, dependencies, écrans.
- Décision MVP vs v1.1 ; out-of-scope ; APK naming ; attribution requirement.

### Reste à faire
- Fork/clone `mardous/BoomingMusic` → `B-Music`.
- Changer package id, APK name, branding; supprimer code réseau/widgets/EQ/lyrics en ligne.
- Revue UI/UX, typographie FR/EN, écran About.
- Test réel device + benchmarks 60/120 fps.
- Publier APK `B-Music.apk` + pages FR/EN.

### Risques
- GPL-3.0 : dérivé devra rester open source (et livrer source).
- Booming code massif, fork de Retro : suppression réseau peut casser des dépendances si trop brutale.
- Retirer Google/GMS : fdroid flavor doit compiler sans service propriétaire.
- Material 3 Expressive sur appareils anciens : fallback nécessaire si animations > budget frame.
- Permission stockage sur Android 13+ : SAF + runtime read permission à tester.
