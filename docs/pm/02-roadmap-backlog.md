# B Music — Roadmap & backlog priorisé (RICE)

> Agent Product Manager B Music. Date : 6 novembre 2026.
> Base d'effort : fork `mardous/BoomingMusic` v1.4.2 (`fdroid`), à créer — **non amorcé aujourd'hui** (audit fait sur clone `/tmp`, cf. `02-audit-gaspillage.md`).
> RICE = (Reach × Impact × Confidence) / Effort. Reach ∈ [0,5–3] (beta/musique locale francophone, ordre de grandeur), Impact (3=massif, 2=fort, 1=moyen, 0,5=faible), Confidence (100/80/50 %), Effort en semaines-personnes. **Scores = estimations de cadrage**, à revalider à la fin de l'Alpha.
> Efforts : S ≤ 1 j · M = 2–5 j · L ≥ 5 j.

## 1. Roadmap en 3 jalons

### Jalon Alpha — « B Music existe » (cible : build Alpha installable)
Objectif : fork fonctionnel, renommé, avec le nouveau design system appliqué. La **lecture marche** ; ce n'est pas encore "à couper le souffle", c'est stable et propre.

| Porte Alpha (critères de sortie) | Mesure |
|---|---|
| Fork clone `fdroid` compilé, package `com.bmusic`, label « B Music » | `gradlew :app:assembleFdroidRelease` OK |
| `B-Music.apk` ≤ 15 Mo (arm64-v8a) | mesuré au build |
| Suppressions Réseau + Widgets + tag editor + Android Auto + updater selon audit | PR de suppression, CI verte |
| Thème sombre/clair B Music appliqué (couleurs + Inter) sur TOUS les écrans | revue Designer |
| Lecture locale + recherche + favoris + playlists + notification média fonctionnels sur device | checklist Alpha (cf. `03`) |
| Zéro permission `INTERNET`/`ACCESS_NETWORK_STATE` | `aapt dump permissions` |

Démo Alpha : écran d'ouverture en < 600 ms, bibliothèque 5 000 titres scroll, lecture, lecteur — rien d'autre à montrer.

### Jalon Bêta — « B Music est fluide et beau » (cible : beta interne signée)
Objectif : animations complètes, polish, accessibilité, accents dynamiques, FR/EN complet.

| Porte Bêta | Mesure |
|---|---|
| Toutes les animations du design system implémentées + reduced-motion | revue 60/120 fps |
| Accent dynamique (Palette) sur CTA lecture + progression + halo (3 éléments autorisés) | demo |
| FR 100 % + EN 100 % (strings externalisées, aucune chaîne en dur) | lint `HardcodedText` |
| Accessibilité : TalkBack OK sur 6 parcours, cibles ≥ 48 dp, contraste AA | test manuel + Lint |
| Macrobenchmark : froid < 600 ms, défilement bibliothèque 0 frame perdue, < 1 % janky | rapport joint |
| Baseline Profile inclus dans release | PR |
| Beta interne (n ≥ 8), questionnaire H1/H2 | résultats dans `docs/pm/05-beta-results.md` (à créer) |

### Jalon v1.0 — « B Music est digne de confiance » (cible : publication)
Objectif : sécurité validée, licence propre, publication APK + documentation.

| Porte v1.0 | Mesure |
|---|---|
| Recette complète `04-acceptation-lancement.md` verte sur matrice devices | matrice signée |
| Budgets `03-budgets-performance.md` tous verts (APK, démarrage, mémoire, jank, batterie) | tableau vert |
| Security : SEC-01…SEC-13 validés (packet-trace, permissions, About/licences) | rapport revu |
| `B-Music.apk` signé, SHA-256 publié, sources publiées (GPL-3.0), attribution complète | release notes |
| DoD v1.0 100 % (cf. `04`) | checklist cochée |

## 2. Épiques et backlog RICE

**Légende agents** : PM = Product Manager · BIZ = Build · DSG = Designer · LNA = Lean · SEC = Security · REL = Release.
**Marquage** : **[v1.0]** obligatoire pour v1.0 · **[v1.1+]** après v1.0.

| ID | Épique | Story (résumé) | R | I | C | E (sem) | RICE | Dépendances | Agent |
|---|---|---|---|---|---|---|---|---|---|
| ID-1 | Identité | Renommage fork → B Music (package, label, APK) [v1.0] | 2 | 2 | 100 | 1 | **4,0** | fork amorcé | BIZ |
| ID-2 | Identité | Logo piste C + icône adaptive + monochrome [v1.0] | 2 | 2 | 90 | 0,5 | **7,2** | — | DSG |
| ID-3 | Identité | `B-Music.apk` signé + SHA-256 + release notes [v1.0] | 1 | 2 | 100 | 0,5 | **4,0** | ID-1 | REL |
| ID-4 | Identité | Écran About + licences OSS (aboutlibraries) + GPL mentions [v1.0] | 1 | 1 | 100 | 0,5 | **2,0** | fork | BIZ |
| DS-1 | Design system | Tokens sombre (`#0B0B0E` etc.) + Inter embarqué [v1.0] | 2 | 3 | 90 | 1,5 | **3,6** | ID-1 | DSG+BIZ |
| DS-2 | Design system | Tokens clair + bascule thème système [v1.0] | 2 | 2 | 90 | 1 | **3,6** | DS-1 | DSG+BIZ |
| DS-3 | Design system | Migration TOUS écrans vers thème (pochette héro, rows, sheets) [v1.0] | 3 | 3 | 80 | 4 | **1,8** | DS-1/2 | BIZ |
| DS-4 | Design system | Accent dynamique (Palette) — CTA + progression + halo uniquement [v1.0] | 2 | 2 | 80 | 1 | **3,2** | DS-3 | DSG+BIZ |
| DS-5 | Design system | Garde-fou « zéro blur/glass » (revue + Lint custom) [v1.0] | 3 | 1 | 100 | 0,5 | **6,0** | DS-3 | LNA |
| AN-1 | Animations | Mini-player → plein écran (shared element pochette + ressort drag) [v1.0] | 3 | 3 | 70 | 2 | **3,15** | DS-3 | BIZ |
| AN-2 | Animations | Play/pause morph + rebond échelle [v1.0] | 3 | 2 | 80 | 0,5 | **9,6** | — | BIZ |
| AN-3 | Animations | Changement de piste (slide + fondu décalé 40 ms) [v1.0] | 3 | 2 | 80 | 0,5 | **9,6** | — | BIZ |
| AN-4 | Animations | Progression lissée frame-by-frame + piste 4↔8 dp [v1.0] | 3 | 2 | 80 | 1 | **4,8** | — | BIZ |
| AN-5 | Animations | Listes : entrée stagger 24 ms (max 8) [v1.0] | 3 | 1 | 80 | 0,5 | **4,8** | DS-3 | BIZ |
| AN-6 | Animations | Onglets glissement + fondu 300 ms [v1.0] | 2 | 1 | 80 | 0,5 | **3,2** | DS-3 | BIZ |
| AN-7 | Animations | Reduced motion (« Supprimer les animations ») [v1.0] | 2 | 2 | 90 | 0,5 | **7,2** | AN-1..6 | BIZ |
| PL-1 | Lecteur | Lecture locale Media3 (MP3/FLAC/OGG/AAC/M4A) + reprise file [v1.0] | 3 | 3 | 95 | 1 | **8,55** | fork | BIZ |
| PL-2 | Lecteur | File d'attente : ajout/réordonner/persistance état [v1.0] | 3 | 3 | 90 | 1,5 | **5,4** | PL-1 | BIZ |
| PL-3 | Lecteur | Notification média + lockscreen (pochette, 3 actions) [v1.0] | 3 | 3 | 90 | 1 | **8,1** | PL-1 | BIZ |
| PL-4 | Lecteur | Focus audio + casque débranché pause + BT reconnexion [v1.0] | 3 | 2 | 90 | 1 | **5,4** | PL-1 | BIZ |
| PL-5 | Lecteur | Reprise après appel entrant [v1.0] | 3 | 2 | 80 | 0,5 | **9,6** | PL-4 | BIZ |
| PL-6 | Lecteur | Sleep timer (conservé du fork) [v1.1+] | 2 | 1 | 80 | 0,5 | **3,2** | PL-1 | BIZ |
| PL-7 | Lecteur | Shuffle/repeat (off/tout/un) persistés [v1.0] | 3 | 1 | 90 | 0,5 | **5,4** | PL-2 | BIZ |
| PL-8 | Lecteur | Gapless (ReplayGain off par défaut) [v1.0] | 2 | 1 | 60 | 1 | **1,2** | PL-1 | BIZ |
| LB-1 | Bibliothèque | Scan MediaStore + 5 000 titres < 5 s audit [v1.0] | 3 | 3 | 80 | 1,5 | **4,8** | fork | BIZ |
| LB-2 | Bibliothèque | Titres / Albums / Artistes / Playlists — écran bibliothèque [v1.0] | 3 | 3 | 90 | 2 | **4,05** | DS-3 | BIZ |
| LB-3 | Bibliothèque | Favoris + vue dédiée [v1.0] | 3 | 2 | 90 | 0,5 | **10,8** | LB-2 | BIZ |
| LB-4 | Bibliothèque | Playlists créer/renommer/supprimer/réordonner + `.m3u` [v1.0] | 3 | 2 | 90 | 1 | **5,4** | LB-2 | BIZ |
| LB-5 | Bibliothèque | Smart playlists (plus écoutés, récents) [v1.0] | 2 | 1 | 70 | 1 | **1,4** | LB-2 | BIZ |
| LB-6 | Bibliothèque | Genres [v1.1+] | 2 | 1 | 70 | 0,5 | **2,8** | LB-2 | BIZ |
| LB-7 | Bibliothèque | Listes blanches/noires de dossiers [v1.1+] | 2 | 1 | 60 | 1 | **1,2** | LB-2 | BIZ |
| SE-1 | Recherche | Recherche locale < 150 ms (FTS/accents/casse) [v1.0] | 3 | 2 | 80 | 1,5 | **3,2** | LB-2 | BIZ |
| SE-2 | Recherche | Résultats groupés (Titres/Albums/Artistes) + état vide [v1.0] | 2 | 1 | 80 | 0,5 | **3,2** | SE-1 | BIZ |
| ST-1 | Paramètres | Thème Sombre/Clair/Système (FR+EN) [v1.0] | 2 | 2 | 90 | 0,5 | **7,2** | DS-2 | BIZ |
| ST-2 | Paramètres | Accent dynamique on/off (extrait pochette) [v1.0] | 2 | 1 | 80 | 0,5 | **3,2** | DS-4 | BIZ |
| ST-3 | Paramètres | Fondu enchaîné (0–12 s) + saute de silence (off défaut) [v1.1+] | 1 | 1 | 70 | 0,5 | **1,4** | PL-1 | BIZ |
| ST-4 | Paramètres | Crash handler propre + « Effacer mes données » (purge) [v1.0] | 1 | 1 | 90 | 0,5 | **1,8** | fork | BIZ |
| A11Y-1 | Accessibilité | Contraste AA vérifié partout (≥ 4,5:1 texte) [v1.0] | 3 | 2 | 90 | 0,5 | **10,8** | DS-3 | LNA+DSG |
| A11Y-2 | Accessibilité | Cibles ≥ 48 dp + `hitSlop` boutons icônes [v1.0] | 3 | 2 | 90 | 0,5 | **10,8** | DS-3 | DSG+BIZ |
| A11Y-3 | Accessibilité | TalkBack : labels actions lecture/file/recherche [v1.0] | 3 | 2 | 80 | 1 | **4,8** | DS-3 | DSG+BIZ |
| A11Y-4 | Accessibilité | FR par défaut + EN complet (zéro chaîne en dur) [v1.0] | 3 | 2 | 90 | 1 | **5,4** | DS-3 | BIZ |
| PF-1 | Performance | Cold start < 600 ms + Baseline Profiles [v1.0] | 3 | 3 | 70 | 2 | **3,15** | DS-3 | BIZ+LNA |
| PF-2 | Performance | Défilement 5 000 titres : 0 frame perdue, < 1 % janky [v1.0] | 3 | 3 | 70 | 1,5 | **4,2** | DS-3 | LNA |
| PF-3 | Performance | RSS lecture < 200 Mo + caches bornés [v1.0] | 3 | 2 | 80 | 1 | **4,8** | PL-1 | LNA |
| PF-4 | Performance | Lint `UnusedResources` + APK arm64 < 15 Mo gate [v1.0] | 3 | 2 | 90 | 0,5 | **10,8** | ID-1 | LNA+BIZ |
| SC-1 | Sécurité | Zéro réseau v1 (permissions supprimées, packet-trace clean) [v1.0] | 3 | 3 | 90 | 1 | **8,1** | fork | SEC |
| SC-2 | Sécurité | Logs sans PII + purge données complète testée [v1.0] | 1 | 1 | 90 | 0,5 | **1,8** | fork | SEC |
| SC-3 | Sécurité | `npm/cargo`-équivalent manuel : audit dépendances + licences OSS [v1.0] | 1 | 2 | 90 | 0,5 | **3,6** | fork | SEC |
| RL-1 | Release | Matrice recette (`04-acceptation-lancement.md`) verte [v1.0] | 2 | 2 | 90 | 1 | **3,6** | tout Alpha/Bêta | SEC+PM |
| RL-2 | Release | `B-Music.apk` + sources publiées + SHA-256 + notes [v1.0] | 1 | 3 | 100 | 0,5 | **6,0** | RL-1 | REL |
| RL-3 | Release | Beta 2 semaines + plan retours J+14 (cf. `04`) [v1.0] | 2 | 2 | 80 | 0,5 | **6,4** | RL-2 | PM+REL |
| EQ-1 | (après v1) | EQ/AutoEq (reporté v1.1+, cf. `01`) [v1.1+] | 2 | 1 | 60 | 3 | **0,4** | PF-1 | BIZ |
| WT-1 | (après v1) | Widgets Glance (reporté) [v1.1+] | 2 | 1 | 50 | 3 | **0,33** | PL-3 | BIZ |

## 3. Chemin critique

```
fork + ID-1 (renommage)
   └─> DS-1/DS-2 (tokens sombre/clair) ──> DS-3 (migration TOUS les écrans)   ← GOULOT n°1
        ├─> DS-4 (accent dynamique)                                             ← GOULOT n°2
        ├─> AN-1..7 (animations) — nécessitent des écrans Compose migrés
        ├─> LB/SE (bibliothèque, recherche) — sur thème migré
        └─> A11Y (contraste/talkback) — sur thème final + avant Bêta
             └─> PF-1..4 (Macrobenchmark) — après polish (Bêta)
                  └─> SC-1..3 + RL-1 (recette) ──> RL-2/RL-3 (publication)
```

**Goulots** :
1. **DS-3 (migration complète des écrans)** bloque animations, bibliothèque, recherche, accessibilité — tout le reste en dépend. Budget : 3–5 j BIZ, revue Designer bloquante. **Aucune animation ne démarre avant que NowPlaying + Mini-player + Bibliothèque soient migrés.**
2. **DS-4 (accent dynamique)** bloque DS-3 final (CTA + progression atteignent le thème dynamique) — faire l'extraction Palette tôt en DS-3, activer après migration.
3. **Fork réseau (SC-1 avec suppressions par lots)** — risque R1 : la suppression mal ciblée casse le build ; traiter en 3 PRs séquentielles (Réseau, Widgets, Divers), chacune verte CI.
4. **PF-1/PF-2** doivent tourner sur **device réel** (pas émulateur) — appareil de recette à obtenir dès l'Alpha (téléphone milieu de gamme 120 Hz + Android Go 60 Hz).

## 4. Ce qui est obligatoire pour v1.0 vs après

**Obligatoire v1.0** (les items marqués [v1.0] ci-dessus) : renommage + icône, design system complet (sombre/clair), animations essentielles, mini-player → plein écran, notification média + lockscreen, lecture/file/favoris/playlists/recherche, accessibilité AA + TalkBack + FR/EN, budgets performance (< 15 Mo, < 600 ms, < 200 Mo, < 1 % jank), sécurité zéro-réseau validée, publication `B-Music.apk` + sources.

**Après v1.0** (hors v1.0, reporté) : EQ/AutoEq, widgets écran d'accueil, éditeur de tags, paroles locales (.lrc), Android Auto, Genres (LB-6), listes blanches/noires (LB-7), fondu enchaîné/saut de silence (ST-3), mode conduite, recommandation. Justification collective : `01-perimetre-mvp.md` §4/§7 (la règle du tiers).
