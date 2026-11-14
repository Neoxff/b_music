# B Music — Handoff agent (v1.2)

> Agent build B Music, phase BUILD-01, 14 novembre 2026.
> **État d'entrée pour l'agent qui reprend le travail.** Court : ce qui est fait, ce qui est bloqué, ce qu'il faut faire ensuite.

---

## 1. En une phrase

Le socle de sécurité des URL est corrigé et prouvé (SEC-001/002/003), les 5 livrables d'architecture existent, et les briques **non ambiguës** des deux onglets (registre de fournisseurs, saisie multi-liens, états, navigation) sont implémentables immédiatement — **deux décisions restent à rendre** (moteur de recherche, lecteur intégré) et conditionnent 3 des 15 prompts de build.

## 2. Où en est le dossier

| Phase | Statut | Preuve |
|---|---|---|
| PLAN (audit d'architecture) | ✅ | `docs/architecture/ARCHITECTURE_AUDIT.md` (écrit dans cette phase) |
| PM (spécification) | ✅ | `docs/pm/05-…` à `08-…` · commit `895f9af` |
| DESIGN (direction artistique) | ✅ | `docs/design/09-…` à `13-…` · commit `06ed0ba` |
| SECURITY | ✅ | `docs/security/02-…`, `03-…` · `security/risk-register.md` · commit `5145a19` |
| **BUILD** | 🔄 **en cours** | ce document |

## 3. Ce qui est fait dans le code produit

| Commit | Contenu |
|---|---|
| `c488d525` | **Squelettes de chargement + réduction des mouvements** (BUILD-14) + 13 tests de transitions d'état (BUILD-15) |
| `748c3f1e` | **Onglets Explorer et Liens** : navigation + budget Material + écrans Liens/Explorer + icône chaîne · 8 cas de navigation ajoutés |
| `89b772b8` | **Registre de fournisseurs + analyse de lot de liens** (BUILD-06/07) · 48 tests ajoutés |
| `cdea200c` | **Correctif Koin** : `Preferences` liée (bug de production) + arrêt de Koin après les tests |
| `41839ee8` | **Correctif sécurité** : SSRF fermé (`GuardedDns`), IPv6 canonique, hôtes ASCII · 11 tests de non-régression |
| `2377fe02` | lint (déjà en place) |

**État de la suite** : `./gradlew --offline :app:testFdroidDebugUnitTest` → **193 tests, 0 échec** ; `:app:lintFdroidDebug` propre ; `check-glass-policy.sh` OK.

**Commits du dépôt de livraison** : `5145a19` (sécurité) et le commit de cette phase (architecture + registre).

## 4. Les deux décisions à rendre

> Ces deux points sont documentés en détail dans `docs/architecture/TECHNICAL_DECISIONS.md` (ADR-002, ADR-003). **Recommandation du build : A pour les deux**, c'est-à-dire NewPipe (D-SEC-1) et métadonnées + ouverture externe (D-SEC-2, PM R13).
>
> **Données vérifiées le 14 novembre 2026** (`docs/pm/09-mecanismes-officiels-verifies.md`, sources officielles consultées en direct) :
> - **Aucune voie officielle de recherche YouTube sans clé serveur.** `search.list` exige une clé (`403` vérifié) ; l'IFrame Player API `listType=search` est déprécié depuis le 15/11/2020 ; aucune API YouTube Music publique n'est documentée.
> - **Le quota de la voie Data API v3 est de 100 `search.list` par jour et par projet de clé** — pas par utilisateur. La voie officielle rend donc la recherche **non exploitable en production** pour une base réelle.
> - **Un WebView ne apporterait la lecture que pour YouTube et TikTok.** Instagram et Snapchat n'ont **aucun** player natif ; Facebook et X exigent du DOM/JS tiers.
> - **Les oEmbed keyless de TikTok, Facebook et Instagram ne renvoient plus ni titre, ni miniature, ni auteur** (champs retirés par Meta le 2025-11-03). Seuls YouTube (titre/auteur/miniature, sans durée) et X (auteur/texte) sont exploitables — ce qui correspond exactement au `ProviderRegistry` livré (`previewAvailable` vrai pour YouTube/SoundCloud/Bandcamp/Vimeo/PeerTube, faux pour les 4 autres).

| # | Question | Option A (recommandée) | Option B | Impact si B |
|---|---|---|---|---|
| **ADR-002** | Moteur de recherche YouTube | **NewPipe** (scraping assumé, écrit) | API Data v3 via **backend proxy** | +1 backend à écrire/déployer/exploiter ; quota 100 recherches/jour ; app dépendante d'une infrastructure |
| **ADR-003** | Lecture des liens | **Métadonnées + ouverture externe** | WebView durci par fournisseur | +WebView + revue sécurité dédiée ; contredit PM §5/§10 ; surface d'attaque non nulle |

**Sans ces deux décisions**, les lots B7 (moteur + écran Explorer) et B8 (fiche de détail + lecteur) restent en attente. **B1→B6, B9, B10 ne dépendent d'aucune des deux** et peuvent être livrés immédiatement.

## 5. Prochaines étapes, dans l'ordre

1. **Rendre ADR-002 et ADR-003** (coordinator). Sans cela, B7/B8 ne démarrent pas.
2. **B1 → B2 → B3** : registre de fournisseurs, analyse de lot, modèle d'état. *Lots purs Kotlin, testables hors ligne, aucun réseau.*
3. **B4 → B5** : icône chaîne, navigation (budget de 6, 2 destinations, ré-ajout runtime).
4. **B6** : écran Liens.
5. **B9 / B10** : animations, nettoyage glyphes/chaînes en dur.
6. **B7** (si ADR-002 = A) : port du `search` depuis `feat/dl-etape-2` **avec** SEC-011 corrigé (encodage + `UrlValidator`), puis écran Explorer.
7. **B8** (si ADR-003 = A) : fiche de détail + ouverture externe.

## 6. Pièges à ne pas retomber dans

| Piège | Pourquoi | Réf. |
|---|---|---|
| Oublier un des 5 points de la navigation | le menu est **reconstruit à l'exécution** ⇒ oubli = re-crash du NPE `findItem` | F-P1-08 |
| Ajouter 2 items sans budget | la barre passe de 6 à 8 ⇒ `IllegalArgumentException` Material | F-DS-01, ADR-007 |
| Oublier `nav_download` dans la branche « barre visible » | la barre est **masquée** sur l'onglet Télécharger | F-P1-09 |
| `catch (_: Exception) {}` autour d'un garde | le contrôle ne sert plus à rien (fail-open) | SEC-001 |
| Un `OkHttpClient` sans `.dns(GuardedDns)` | SSRF ré-ouvert | SEC-001 |
| Chaînes en dur / glyphes Unicode | lint + exigence « ressources centralisées » | F-DS-02 |
| Modifier `ui/screen/downloader/` | l'écran Téléchargeur ne doit pas changer (PM §9) | PM R9 |
| Réécrire `downloader/security/` | il fonctionne et il est prouvé | — |

## 7. Commandes utiles

```bash
cd bmusic
./gradlew --offline :app:testFdroidDebugUnitTest                      # suite complète (77 + nouveaux)
./gradlew --offline :app:testFdroidDebugUnitTest --tests "…security.*" # sécurité
./gradlew --offline :app:assembleFdroidDebug                          # compilation
./gradlew lint                                                       # lint (HardcodedText)
bash scripts/check-glass-policy.sh                                   # zéro verre
git -C bmusic log --oneline -10
```

## 8. État git

| Dépôt | État |
|---|---|
| `bmusic/` (produit) | `fix/stabilite-1.2` @ `41839ee8` — **local, non poussé** ; 4 fichiers non commités (F-P2-01, lot L0) |
| `Neoxff/b_music` (livraison) | `main` @ `5145a19` — **devant 3 sur `origin/main`, non poussé** ; `bmusic/` y est non suivi |

**Ne rien pousser, merger ou taguer sans GO explicite.**

## 9. Documents à lire avant de coder

| Document | Ce qu'il apporte |
|---|---|
| `docs/architecture/ARCHITECTURE_AUDIT.md` | l'architecture réelle, les invariants, les 6 écarts code/doc |
| `docs/architecture/BASELINE.md` | l'état mesuré + les 12 invariants à préserver |
| `docs/architecture/TECHNICAL_DECISIONS.md` | les 13 ADR, dont les 2 à rendre |
| `docs/architecture/IMPLEMENTATION_PLAN.md` | le plan de diff lot par lot |
| `docs/pm/05-spec-explorer-liens.md` | R1→R21, les 9 états, les 42 microcopies, les parcours PM-02/PM-03 |
| `docs/design/11-maquettes-explorer-et-liens.md` | les 2 écrans, leurs 7 états |
| `docs/design/12-tokens-composants-et-motion.md` | 15 composants, 12 animations, 9 contrôles |
| `docs/security/02-modele-menaces-explorer-liens.md` | ce qui est interdit (WebView, HTML brut, secret client) |
| `security/risk-register.md` | les 16 risques + les 10 contrôles conformes |
