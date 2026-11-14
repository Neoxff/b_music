# B Music — Décisions techniques (ADR)

> Agent build B Music, phase BUILD-01, 14 novembre 2026.
> Chaque décision a un **statut** : `Actée` (elle lie), `À arbitrer` (elle attend le coordinator), `Rejetée` (documentée pour ne pas la rediscuter).
> Les décisions marquées `Actée` par le coordinator ou le security-manager sont **datées et sourcées**.

---

## 0. Index

| # | Décision | Statut | Décideur | Date |
|---|---|---|---|---|
| ADR-001 | Le produit est le Kotlin, dans `Neoxff/b_music` | Actée | coordinator | 2026-11-11 |
| ADR-002 | Moteur de recherche : NewPipe **ou** API officielle via proxy | **À arbitrer** | coordinator | — |
| ADR-003 | Lecteur intégré (WebView) **ou** métadonnées + ouverture externe | **À arbitrer** | coordinator | — |
| ADR-004 | Zéro réseau préservé dans le flavor `fdroid` | Actée | coordinator | 2026-11-11 |
| ADR-005 | Locale par défaut : le code fait foi (EN par défaut, FR dans `values-fr-rFR/`) | Actée | coordinator | 2026-11-13 |
| ADR-006 | Cibles tactiles : 48 dp (le code fait foi) | Actée | coordinator | 2026-11-13 |
| ADR-007 | Structure de navigation : **option A** (2 emplacements réservés, budget de 6) | Actée par le build, à confirmer | build | 2026-11-14 |
| ADR-008 | Tokens : le code fait foi | Actée | coordinator | 2026-11-13 |
| ADR-009 | Format d'icônes : Material Symbols rempli 960×960 (le code fait foi) | Actée | coordinator | 2026-11-13 |
| ADR-010 | Pas de RICE sur la ligne v1.2 | Actée | pm | 2026-11-13 |
| ADR-011 | Registre de fournisseurs : objet Kotlin pur, statuts honnêtes | Actée | build | 2026-11-14 |
| ADR-012 | Saisie multi-liens : bornes 20 URL / 8 Ko par lot, validation par entrée | Actée | build | 2026-11-14 |
| ADR-013 | Modèle d'état : 9 états, un seul état visible à la fois | Actée | pm | 2026-11-13 |

---

## ADR-001 — Le produit est le Kotlin, dans le dépôt de livraison

**Statut** : Actée (coordinator, 2026-11-11)

Le produit réel est la copie locale `bmusic/` (Kotlin/Compose, 1330 fichiers, 523 `.kt`/`.java`). Le dépôt `Neoxff/b_music` ne contient que le prototype Capacitor (145 fichiers, 0 Kotlin).

**Décision** : le Kotlin devient le produit dans `Neoxff/b_music`. Le prototype est archivé sous `prototype/` (lot L1).

**Rejetées** : implémenter dans le prototype seul (crée une 2ᵉ application, contredit le dossier) · promotion brute des onglets Téléchargeur (extraction + téléchargement interdits).

**Conséquence** : tant que L1 n'est pas fait, le code produit vit dans un dépôt non suivi → **tout lot doit être commité dans `bmusic/` en attendant**, et la migration L1 doit embarquer le correctif sécurité `41839ee8`.

---

## ADR-002 — Moteur de recherche YouTube : **À ARBITRER**

**Statut** : **À arbitrer** — le build n'implémente rien avant la décision.

### Le problème

Deux exigences du dossier s'opposent :

- **BUILD-03** : « le mécanisme officiel », « **ne fais pas de scraping** du site YouTube », « **aucune clé secrète dans le client** », pagination « selon les jetons fournis par l'API ».
- **D-SEC-1** (security-manager, actée le 2026-11-14) : NewPipe conservé, scraping **assumé** avec condition de revoyure.

Et la contrainte structurelle : **YouTube Data API v3 `search.list` exige une clé API**, qui ne peut pas vivre dans le client (règle du dossier, confirmée par `SEC-V-03` : 0 clé embarquée aujourd'hui). Il n'existe **aucun** backend dans le projet (`docs/security/02-…` §6.2).

### Les options — vérifiées le 14 novembre 2026

> Source : `docs/pm/09-mecanismes-officiels-verifies.md` (sources officielles consultées en direct).

| | **A — Proxy minimal + API Data v3** | **B — NewPipe (D-SEC-1)** | **C — Clé Data v3 restreinte à l'APK Android** |
|---|---|---|---|
| Honore « mécanisme officiel » | ✅ | ❌ (scraping) | ✅ |
| Honore « aucune clé dans le client » | ✅ (la clé est côté serveur) | ✅ (aucune clé) | ❌ (clé embarquée, même si inutilisable par un tiers) |
| Clé nécessaire | oui, côté serveur | **aucune** | oui, embarquée et **restreinte** (package + SHA-1, mécanisme officiel) |
| Nouveaux composants | **1 backend à écrire, déployer, sécuriser** | 0 | 0 |
| Coût | **L (≥ 5 j)** + hébergement + exploitation | **S (≤ 1 j)** (60 lignes à porter) | S |
| **Quota réel** | `search.list` : bucket dédié **100 appels/jour** ⇒ **~100 recherches/jour pour toute l'application** | pas de quota formel, mais captcha / blocage IP | **identique** : 100/jour |
| Fragmentation | l'app dépend d'une infrastructure qu'elle ne possède pas | dépendance à la page `youtube.com/results` | l'app dépend d'un quota projet global, non répartissable |
| Contrat de sécurité | §6.3 de `docs/security/02-…` (8 contrôles) déjà spécifié | D-SEC-1 déjà justifié | **viole la règle « aucune clé dans le client »** |
| Réversibilité | élevée (le client ne change pas) | élevée | faible |

**Fait bloquant vérifié** : **il n'existe aucune voie officielle de recherche YouTube sans clé serveur.** `search.list` exige une clé (`403 PERMISSION_DENIED` vérifié) ; l'IFrame Player API `listType=search` est déprécié depuis le 15/11/2020 ; aucune API YouTube Music publique n'est documentée. Le `youtubei`/Innertube n'est pas documenté ⇒ ce serait du scraping.

**Conséquence de produit, indépendante du choix** : le plafond de **100 recherches/jour** est celui du *projet de clé*, pas de l'utilisateur. Même avec un backend et un quota réparti, la voie Data API v3 rend la recherche **non exploitable en production** pour une base d'utilisateurs réelle. C'est l'argument le plus fort en faveur de l'option B.

### Recommandation du build : **B — NewPipe, avec D-SEC-1 mis à jour**

**Pourquoi** :

1. L'option A impose une **infrastructure nouvelle** (backend, hébergement, clé, rotation, quota) dans un projet dont la promesse produit est « application locale, zéro infrastructure ». Le coût n'est pas le code : c'est l'exploitation continue, la rotation de clé et le plafond de 100 recherches/jour, qui rend le service **inutilisable** en usage réel.
2. L'option C est **interdite** par le dossier : une clé dans un APK est extractible par n'importe qui.
3. L'option B a déjà un avis de sécurité écrit, des mesures de réduction (User-Agent neutre, aucun cookie, aucun secret, requêtes bornées à 2-200 caractères) et une condition de revoyure. Elle est **cohérente** avec le fait que le projet est GPL-3.0 et que NewPipeExtractor est la brique de référence du domaine.
4. Le BUILD-03 demande « la configuration adaptée à l'architecture existante » : NewPipeExtractor **est** l'architecture existante (le module `downloader/` l'utilise déjà).

**Ce que l'option A imposerait si elle est retenue** : le proxy de `docs/security/02-…` §6.3 (surface unique, validation des paramètres, rate-limit par IP et par clé, cache TTL, journalisation sans secrets, quota quotidien, clé hors dépôt, rotation trimestrielle) — **spécifié mais non écrit**, et écrit dans le dépôt de livraison ou dans un dépôt séparé à décider.

**Point de vigilance retenu quel que soit le choix** : la requête doit être **encodée** et passée par `UrlValidator` (SEC-011), et l'override `search` doit être porté depuis `feat/dl-etape-2` avec ces deux corrections.

---

## ADR-003 — Lecteur intégré (WebView) **ou** métadonnées + ouverture externe : **À ARBITRER**

**Statut** : **À arbitrer**.

### Le problème

- **BUILD-05 / 07 / 08 / 09 / 10** demandent des lecteurs intégrés : « le lecteur YouTube officiel », « origines d'iframe permises », « le lecteur intégré recommandé par TikTok », « widgets officiels » Meta/X.
- **D-SEC-2** (security-manager, actée le 2026-11-14) : **aucun WebView** ; l'aperçu Liens reste métadonnées + ouverture externe, avec une condition de revoyure explicite.
- **PM R13** : « Un résultat lead vers une page **externe** … — **jamais vers un lecteur intégré inexistant** » ; §5 : « ne décrit **aucune fonction nouvelle de lecteur** » ; §10 (priorisation) : **aucun bloc « lecture intégrée »** dans le MVP.

### Les options — vérifiées le 14 novembre 2026

> Source : `docs/pm/09-mecanismes-officiels-verifies.md` (sources officielles consultées en direct).

| | **A — Métadonnées + ouverture externe** | **B — WebView durci** | C — WebView par fournisseur, au cas par cas |
|---|---|---|---|
| Conformité PM | ✅ (R13, §5, §10) | ❌ (change la spec produit) | ⚠️ (change la spec pour 1-2 fournisseurs) |
| Conformité sécurité | ✅ (D-SEC-2) | ⚠️ (revue sécurité dédiée exigée) | ⚠️ (revue par fournisseur) |
| Surface d'attaque | **nulle** | WebView + JS + iframes tierces | idem, bornée |
| Plateformes réellement couvertes | les 6 en ouverture externe | **2 sur 6** : YouTube, TikTok. Facebook et X exigent du DOM/JS tiers ; **Instagram et Snapchat n'ont aucun player natif** | idem |
| Coût | S | **L (≥ 5 j)** + revue sécurité | M par fournisseur |
| Attributs imposés | lisibles | visibles (si la plateforme les impose) | visibles |
| Risque principal | l'utilisateur quitte l'app | XSS via contenu tiers, `postMessage` non vérifié | idem, multiplié |

### Recommandation du build : **A pour la v1.2**, avec B documentée comme lot séparé

**Pourquoi** :

1. **La spec produit validée ne contient pas de lecteur intégré.** PM §5 : « ne décrit aucune fonction nouvelle de lecteur ». PM §10 priorise explicitement « ouverture externe ». Implémenter un WebView, ce serait changer la spec produit **pendant** la phase build, sans arbitrage PM.
2. **La condition de revoyure de D-SEC-2 exige une revue sécurité dédiée** (origines autorisées listées, JS désactivé par défaut, aucun pont natif, vérification d'origine **et** de source sur `postMessage`, politique de navigation restrictive). C'est un travail de sécurité, pas un lot de build ; il ne peut pas être absorbé « en passant ».
3. **La surface nulle est un actif** : `SEC-V-01` est un contrôle **conforme**. L'ajouter pour 1-2 fournisseurs échange un risque mesuré contre plusieurs risques nouveaux, sur des plateformes où le résultat utilisable (TikTok, Instagram, X, Snapchat) est **majoritairement non intégrable** de toute façon.

**Ce que l'option B imposerait** : une `WebView` isolée (JS restricted à l'origine de l'embed, `allowList` d'origines, `shouldOverrideUrlLoading` qui refuse toute navigation hors allowlist, aucun `addJavascriptInterface`, `postMessage` vérifié origine + source, `WebViewAssetLoader` ou `loadDataWithBaseURL` avec base null pour interdire les chargements arbitraires) **et** une revue sécurité dédiée **et** un correctif `docs/security/01-modele-menaces.md` §3.3.

**Apport de l'option A, non nul** : l'aperçu **Affiche** titre, chaîne, durée, miniature et l'état d'intégration, et propose « Ouvrir dans YouTube » / « Copier le lien ». C'est la promesse du dossier ; elle ne demande pas de lecteur.

---

## ADR-004 — Zéro réseau préservé dans le flavor `fdroid`

**Statut** : Actée (coordinator, 2026-11-11)

Le flavor `fdroid` ne doit **pas** embarquer `INTERNET` : c'est le contrat v1.0 (`docs/lean/01-perimetre-mvp.md` L158, L165-166 ; `docs/security/01-modele-menaces.md` L32, L55-56). Implémentation par manifest merger (`tools:node="remove"`), preuve `aapt dump permissions` par variant. **Application : lot L2** (SEC-004, bloquant).

Les onglets Explorer et Liens sont donc présents **dans tous les flavors**, mais **inopérants** en `fdroid` : ils affichent l'état « Hors ligne » (R7, état « Erreur réseau »), jamais un écran vide.

---

## ADR-005 / ADR-008 / ADR-009 — Le code fait foi

**Statut** : Actées (coordinator, 2026-11-13)

| Sujet | Le code dit | La doc disait | Décision |
|---|---|---|---|
| Locale par défaut | `values/` = anglais (994 chaînes), `values-fr-rFR/` = FR (981), **pas de `values-en/`** | « FR par défaut, EN fourni » (A11Y-4) | **le code fait foi** ; A11Y-4 est corrigé |
| Cible tactile | `Dimens.TouchTarget = 48.dp` | 44 dp (design system, tokens) | **48 dp** ; la doc est corrigée |
| Tokens couleur | `#E8862E` / `#0B0B0E` | `#E8A33D` / `#121212` (corpus mort) | **le code fait foi** ; le corpus `docs/design/tokens/` est aligné |
| Format d'icônes | Material Symbols **rempli**, viewport 960×960 | « trait 1,75 dp, viewport 24×24 » | **le format réel** ; les 2 nouvelles icônes suivent le dépôt |

---

## ADR-007 — Structure de navigation : option A, avec budget explicite

**Statut** : Actée par le build le 2026-11-14, **à confirmer par le coordinator** (F-DS-01).

**Contrainte** : `BottomNavigationView` Material = **6 entrées max**. Aujourd'hui : 5 catégories + `nav_download` = 6. Ajouter 2 onglets ⇒ 8 ⇒ `IllegalArgumentException`.

**Décision** : **option A** de `docs/design/10-navigation-et-icones.md` §3, précisée :

1. Les deux onglets de découverte sont **réservés** dans la barre.
2. Les entrées existantes ne sont **ni supprimées ni renommées** (BUILD-02) : leur **ordre** et leur **libellé** sont inchangés.
3. Un **budget** remplace l'arithmétique implicite : `BottomNavBudget.MAX_ITEMS = 6`. `updateTabs()` réserve 2 emplacements (Explorer + Liens) + 1 pour `nav_download` (conditionnel), et n'ajoute les catégories que dans le budget restant, **dans l'ordre de la préférence utilisateur**.
4. Conséquence : quand les onglets sont présents, **au plus 3 catégories** apparaissent dans la barre ; les autres restent accessibles par le sélecteur de catégories des préférences (`CategoryInfoAdapter`) et par l'écran Bibliothèque. **Le plafond de préférence `MAX_VISIBLE_CATEGORIES = 5` n'est pas modifié** : aucun réglage utilisateur n'est changé.
5. Quand la fonctionnalité réseau est absente (flavor `fdroid` ou drapeaux désactivés), la barre est **identique à aujourd'hui**.

**Option B** (entrée unique « Découvrir ») reste le repli si le coordinator refuse la troncature des catégories. **Option C** (barre maison) est écartée : coût/risque défavorable et aucun baseline de non-régression (F-DS-07).

---

## ADR-010 — Pas de RICE sur la ligne v1.2

**Statut** : Actée (pm, 2026-11-13)

Le produit est « zéro télémétrie » par construction ⇒ le facteur **Reach** du RICE n'est pas mesurable. Le PM priorise donc explicitement sur 4 axes (valeur utilisateur, risque, coût, dépendances). **Aucun score chiffré n'est inventé.**

---

## ADR-011 — Registre de fournisseurs : objet Kotlin pur, statuts honnêtes

**Statut** : Actée (build, 2026-11-14)

**Décision** : un objet `ProviderRegistry` **sans dépendance réseau ni Android**, dans `downloader/provider/`, décrit pour chaque fournisseur : domaines reconnus (+ équivalences), règles d'extraction d'identifiant, méthode d'intégration **autorisée**, origines d'iframe **permises** (vide tant qu'ADR-003 n'est pas rendu), prévisualisation possible ou non, limitations connues, repli externe.

**Règle** : **aucune intégration n'est annoncée sans vérification** (F-PM-06). Un domaine inconnu reçoit le statut « fournisseur non pris en charge » + ouverture externe — **jamais** un lecteur improvisé (BUILD-07).

**Justification** : la détection de plateforme doit être **indépendante** de la résolution (R17) — un lien vers une plateforme non supportée est reconnu et affiché comme tel. C'est ce que le code ne fait pas aujourd'hui (F-P1-06 / SEC-013).

**Dépendances de l'ADR-002 et ADR-003** : le registre **expose** la méthode d'intégration autorisée ; il ne l'**invente** pas. Tant que ADR-003 n'est pas rendu, `embedOrigin` est vide et le repli est l'ouverture externe.

---

## ADR-012 — Bornes de la saisie multi-liens

**Statut** : Actée (build, 2026-11-14)

| Borne | Valeur | Justification |
|---|---|---|
| Nombre d'URL par lot | **20** | Le PM ne chiffre pas ; 20 est le plafond de résultats d'une page (R10) — même ordre de grandeur, et une liste de 100 URL n'est pas un usage de collage |
| Taille totale d'un lot | **8 Ko** | `UrlValidator.MAX_LENGTH` (2048) × 4 : borné sans interdire un lot réaliste |
| Taille d'une URL | **2048** | `UrlValidator.MAX_LENGTH` (R16) |
| Ligne ignorée | 1 ligne non conforme ⇒ **ignorée avec compteur**, les autres restent traitables | PM §8 « Lignes non conformes ignorées avec un compteur "X ligne ignorée" » |

**Règle de sécurité** : une URL invalide **ne bloque jamais** le lot ; elle produit une entrée d'état « Lien invalide » avec une **raison courte** et non technique.

---

## ADR-013 — Modèle d'état : un seul état visible à la fois

**Statut** : Actée (pm, 2026-11-13)

9 états (PM §6) : repos · saisie invalide · chargement · résultats · aucun résultat · erreur réseau · service indisponible · non pris en charge · contenu restreint.

**Règles** : un état d'erreur **remplace** la liste (R8) ; tout état d'erreur est **actionnable** (R7) ; aucun écran vide, aucun spinner infini, aucune exception visible, aucune fausse confirmation.
