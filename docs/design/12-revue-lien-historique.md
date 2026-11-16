# B Music — Revue d'implémentation : onglets « Lien » et « Historique » (v1.3)

> Agent DESIGNER — REVUE. **Aucun code modifié.** Aucune mesure claimée sur un appareil.
> Date : 16 novembre 2026. Branche de design : `feat/glass-global`.
> Spécifications de référence : `11-onglets-lien-historique.md` (écrans), `13-liquid-glass-sobre.md` (verre downloader v1.2),
> `13-liquid-glass-sobre-global.md` (verre global v1.3), `12-tokens-composants-et-motion.md` (tokens), `Motion.kt`.
> Implementation revue : `bmusic/` branche `feat/dl-lien-diagnostic` (HEAD `9dc1445c`).

---

## 0. Méthode de revue

La revue repose sur **trois sources vérifiables sans appareil** :

| Source | Ce qu'elle permet | Ce qu'elle ne permet pas |
|---|---|---|
| **A — Tests JVM/Robolectric/Compose** | mesure **au dp près** du centrage, présence des états, des actions et des libellés | le rendu visuel (teinte, reflet, contraste perçu) |
| **B — Inspection du code** (`fichier:ligne`) | présence/absence d'un comportement, géométrie, courbes d'animation | l'aspect |
| **C — Scripts CI** (`check-glass-policy.sh`, `check-koin-bindings.py`) | absence de flou hors `GlassSurface`, complétude des bindings Koin | l'aspect |

**Captures d'écran : il n'existe aujourd'hui aucune infrastructure de capture dans le dépôt**
(`grep paparazzi|roborazzi|screenshot` sur `app/build.gradle.kts` et `gradle/libs.versions.toml` → **0 résultat** ;
`find` de `*.png` de référence → **0**). Les 25 captures C-01…C-25 du §3 et les 36 captures G-01…G-36 du
document global **n'existent pas** : la revue ne peut donc pas être close sur le rendu.

> Statut de chaque écart : **CONFORME** (prouvé) · **ÉCART** (prouvé, avec fichier:ligne) ·
> **À VÉRIFIER** (nécessite une capture).

---

## 1. Critères prioritaires

### 1.1 Zone de saisie centrée (horizontalement ET verticalement) — **CONFORME**

| Exigence | Implémentation | Preuve |
|---|---|---|
| Centrage horizontal | `Modifier.fillMaxSize()` + `widthIn(max = 560.dp)` + `padding(horizontal = 20.dp)` + `horizontalAlignment = CenterHorizontally` | `DownloadScreen.kt:237-244` |
| Centrage vertical | conteneur `fillMaxSize()` + `contentAlignment = Alignment.Center` | `DownloadScreen.kt:236` |
| Largeur plafonnée 560 dp, marges 20 dp | `widthIn(max = 560.dp)`, `padding(horizontal = 20.dp)` | `DownloadScreen.kt:242-243` |
| Clavier ouvert : le bloc reste visible | `Modifier.fillMaxSize().imePadding()` | `DownloadScreen.kt:237` |

**Mesure au dp près** (source A) — test `DownloadScreenTest` (Robolectric + Compose) :

| Mesure | Tolérance | Résultat |
|---|---|---|
| Centre X du bloc **vs** centre X de l'écran | ≤ 2 dp | **test vert** (`la zone de saisie est centree horizontalement`) |
| Centre Y du bloc **vs** centre Y de la zone de contenu | ≤ 16 dp | **test vert** (`… centree verticalement`) |

> Le centre Y est mesuré sur le **rectangle englobant** du bloc (icone 48 dp + titre + champ + boutons + phrase),
> d'où une tolérance plus large (16 dp) que l'axe horizontal, où la mesure est ponctuelle.
> **Écart E-01 de la v1.2 : résolu** (il était « à confirmer »).

### 1.2 Remontée en ressort — **CONFORME**

| Exigence | Implémentation | Preuve |
|---|---|---|
| Ressort, 220 ms, `SpringSnappy` | `animateFloatAsState(targetValue = risePx, animationSpec = if (reduced) snap() else Motion.SpringSnappy)` | `DownloadScreen.kt:226-232` |
| Amplitude | −120 dp | `DownloadScreen.kt:228` |
| Application par `translationY` (pas de déplacement de layout) | `Modifier.graphicsLayer { translationY = risePx }` | `DownloadScreen.kt:244` |
| Mouvement réduit → déplacement immédiat | `if (reduced) snap()` | `DownloadScreen.kt:232` |

**Écart E-02 de la v1.2 : résolu.**

### 1.3 États d'erreur avec une action — **ÉCART MAJEUR (E-03)**

| Exigence | Implémentation | Preuve |
|---|---|---|
| Un **code typé** par situation | 11 codes dans `DownloadErrorCode` + `userMessage` + `action` | `downloader/error/DownloadError.kt` |
| Le **message** est affiché | `state.error?.let { … }` | `DownloadScreen.kt:317` |
| **L'action est rendue** (bouton « Réessayer » / « Voir les sites » / « Ouvrir le journal ») | **l'action est calculée mais jamais rendue** : `errorAction` n'est lu par aucun composable de l'UI | `grep errorAction` sur `DownloadScreen.kt` → **0 occurrence** |

→ **14 textes en dur** dans `DownloadScreen.kt` (lignes 102, 168, 196, 255, 288, 307, 310, 367, 374, 377, 412, 452 …) ;
les 11 messages d'erreur typés ne sont **pas** dans `strings.xml` (`grep download_error|pris en charge` → **0**).

### 1.4 Lisibilité de l'historique — **ÉCARTS MAJEURS (E-10, E-11)**

| Exigence (`11` §4) | Implémentation | Preuve |
|---|---|---|
| Regroupement par date (Aujourd'hui / Hier / Cette semaine / Plus ancien) | **absent** | `grep "Aujourd'hui\|Hier\|Cette semaine"` → **0** |
| Ligne : miniature, titre, site, format, taille, état | titre + format + état | `DownloadScreen.kt:404-460` |
| 6 actions : Lire, Retélécharger, Copier le lien, Supprimer l'historique, Supprimer le fichier, Partager | **3 actions seulement** : « Reprendre », « Pause », « Annuler » | `grep` → `Annuler`, `Pause`, `Reprendre` |
| Ligne = hauteur constante, clés stables | à vérifier sur captures | — |
| Apparition échelonnée (8 max) | à vérifier sur captures | — |

### 1.5 Icônes cohérentes — **ÉCART MINEUR (E-13)**

| Icône | Spec `11` §2 | Présent ? |
|---|---|---|
| `ic_link` (2 anneaux, coupure ≥ 2,5 dp à 24 dp, trait 1,75 dp) | oui | ✅ `ic_link_24dp.xml` |
| `ic_history` (horloge) | oui | ✅ `ic_history_24dp.xml` |
| `ic_download` | existant | ✅ |
| `ic_pause` / `ic_cancel` | existants | ✅ |
| `ic_delete` | existant | ✅ |
| `ic_error` | existant | ✅ |
| `ic_paste` | `ic_content_paste_24dp.xml` | **absent** ❌ |
| `ic_copy_link` | `ic_content_copy_24dp.xml` | **absent** ❌ |
| `ic_resume` | à créer | **absent** ❌ |
| `ic_site_generic` (monogramme générique, **jamais** un logo de marque) | à créer | **absent** ❌ |

### 1.6 Contraste AA — **À VÉRIFIER**

Aucune mesure possible sans capture : `scripts/contrast_check.py` n'existe pas dans le dépôt
(`grep` → 0). Méthode à employer : mesurer le texte **rendu** (opacité du verre + fond de pochette)
sur la pochette la plus claire et la plus foncée de la banque d'images.

### 1.7 Aucun aspect vitreux — **CONFORME**

`bash scripts/check-glass-policy.sh` → `✓ glass-policy OK (liste blanche GlassSurface, rayon ≤24 dp, aucun effet vitreux)` (exit 0).

---

## 2. Tableau d'écarts par gravité

| ID | Gravité | Écart | Fichier:ligne | Preuve | Correction | Agent |
|---|---|---|---|---|---|---|
| **E-01** | ~~Bloquant~~ → **CONFORME** | Zone de saisie centrée : conforme (H et V) | `DownloadScreen.kt:236-244` | `DownloadScreenTest` : centrage horizontal (≤ 2 dp) et vertical (≤ 16 dp) **verts** | — | — |
| **E-02** | ~~Bloquant~~ → **CONFORME** | Remontée en ressort conforme | `DownloadScreen.kt:226-244` | `Motion.SpringSnappy` + `translationY` −120 dp + `snap()` si mouvement réduit | — | — |
| **E-03** | **Majeur** | L'**action** d'erreur n'est pas rendue (le code typé existe, le bouton non) | `DownloadScreen.kt:317` · `DownloadViewModel.kt` (`errorAction`) | `grep errorAction` dans l'UI → 0 | Rendre un `TextButton` selon `ErrorAction` (Réessayer / Voir les sites / Ouvrir le journal) | BUILD |
| **E-04** | **Majeur** | **14 textes en dur** au lieu de `strings.xml` | `DownloadScreen.kt:102,168,196,255,288,307,310,367,374,377,412,452…` | `grep -cE '"[A-ZÉÀ][^"]{6,}"'` → 14 | Externaliser (voir `11` §13.3) | BUILD |
| **E-05** | **Majeur** | Messages des 11 erreurs typées absents de `strings.xml` | `DownloadError.kt` | `grep download_error|pris en charge` dans `strings.xml` → 0 | Ajouter les 23 clés (FR/EN) | BUILD |
| **E-06** | **Majeur** | **Regroupement par date** absent (Aujourd'hui / Hier / Cette semaine / Plus ancien) | `DownloadScreen.kt:404-460` | `grep` → 0 | Grouper `vm.downloads` par jour, en-têtes collants | BUILD |
| **E-07** | **Majeur** | **3 actions** d'historique sur 6 (manquent Lire, Retélécharger, Copier le lien, Supprimer l'historique, Supprimer le fichier, Partager) | `DownloadScreen.kt:452` | `grep` → Annuler/Pause/Reprendre | Implémenter les 6 | BUILD |
| **E-08** | **Majeur** | **Puces des sites pris en charge** et **badge de détection en direct** absents (le `SiteRegistry` existe mais n'est pas utilisé par l'UI) | `registry/SiteRegistry.kt` (créé) · `DownloadScreen.kt` (0 usage) | `grep -c SiteRegistry` sur l'écran → 0 | Chips + badge (voir `11` §13.2) | BUILD |
| **E-09** | Mineur | Icônes manquantes : `ic_paste`, `ic_copy_link`, `ic_resume`, `ic_site_generic` | `res/drawable/` | `ls` | Créer les 4 vecteurs 24 dp, trait 1,75 dp | BUILD |
| **E-10** | Mineur | État « plusieurs liens » (extraction depuis un texte collé) non implémenté | `DownloadScreen.kt` | `grep extractLinks` → 0 | Liste à cases + « Tout télécharger » | BUILD |
| **E-11** | Mineur | Message de premier usage absent | `strings.xml` | `grep "ne contourne aucune protection"` → 0 | Ajouter la clé (voir `11` §13.3) | BUILD |
| **E-12** | Mineur | **Aucun verre réel** : `GlassSurface` pose `Color.Black.copy(alpha = 0.5f)` (opacité plate), pas de flou | `ui/theme/GlassSurface.kt:56` | lecture du code | Implémenter le flou réel + reflet + liseré + ombre + teinte adaptative (doc global §7) | BUILD |
| **E-13** | Mineur | `GlassTokens.Prominent` = 0,88 / 0,92 → hors de la bande globale (≤ 0,90) | `ui/theme/GlassTokens.kt:20-21` | lecture du code | Recaler à 0,86 / 0,90 | BUILD |
| **E-14** | Mineur | `check-glass-policy.sh` interdit tout rayon > 24 dp → les capsules (28/32 dp) seraient rejetées | `scripts/check-glass-policy.sh` | lecture du script | Accepter la forme dérivée « capsule = hauteur ÷ 2 » | BUILD |
| **E-15** | Mineur | Réglages « Intensité » et « Contraste renforcé » absents | `util/Preferences.kt:440-450` | `grep blur_intensity\|blur_contrast` → 0 | Créer les 2 préférences + l'aperçu en direct | BUILD |
| **E-16** | Mineur | Barre du bas non « capsule flottante », pas de pastille de sélection en verre | `AbsSlidingMusicPanelActivity.kt:137` | `NavigationBarView` Material | Capsule 16 dp / 64 dp + pastille qui glisse (doc global §7.1) | BUILD |
| **E-17** | **À VÉRIFIER** | Contraste AA non mesuré | — | `scripts/contrast_check.py` absent | Mesurer sur captures G-01…G-36 | BUILD + DESIGNER |
| **E-18** | **À VÉRIFIER** | Lisibilité de `ic_link` à 24 dp | `res/drawable/ic_link_24dp.xml` | capture C-20…C-25 | Vérifier la coupure ≥ 2,5 dp | DESIGNER |

> Aucun écart **bloquant** ne subsiste sur le centrage et la remontée : les deux écarts bloquants de la v1.2 (E-01, E-02)
> sont **résolus et prouvés**. Les écarts restants sont **majeurs** (comportement) ou **mineurs** (finition),
> plus deux écarts **à vérifier** qui exigent des captures.

---

## 3. Captures manquantes

### 3.1 Onglet Lien — état vide (C-01…C-07)

| # | Capture | Thème | Taille | Orientation | Clavier |
|---|---|---|---|---|---|
| C-01 | Lien vide | Sombre | Téléphone | Portrait | Fermé |
| C-02 | Lien vide | Clair | Téléphone | Portrait | Fermé |
| C-03 | Lien vide | Sombre | Tablette | Portrait | Fermé |
| C-04 | Lien vide | Clair | Tablette | Portrait | Fermé |
| C-05 | Lien vide | Sombre | Téléphone | Paysage | Fermé |
| C-06 | Lien vide | Sombre | Téléphone | Portrait | Ouvert |
| C-07 | Lien vide | Clair | Téléphone | Portrait | Ouvert |

### 3.2 Onglet Lien — après analyse (C-08, C-09)

| # | Capture | Thème | Taille | Orientation |
|---|---|---|---|---|
| C-08 | Lien après analyse | Sombre | Téléphone | Portrait |
| C-09 | Lien après analyse | Clair | Téléphone | Portrait |

### 3.3 Onglet Lien — états d'erreur (C-10…C-15)

| # | Capture | État | Thème |
|---|---|---|---|
| C-10 | Lien invalide | Sombre |
| C-11 | Site non pris en charge | Sombre |
| C-12 | Connexion requise | Sombre |
| C-13 | Contenu protégé | Sombre |
| C-14 | Moteur à mettre à jour | Sombre |
| C-15 | Hors ligne | Sombre |

### 3.4 Onglet Historique (C-16…C-19)

| # | Capture | Thème | Taille | Orientation | États de ligne |
|---|---|---|---|---|---|
| C-16 | Historique rempli | Sombre | Téléphone | Portrait | Terminé, En cours, Erreur, Fichier supprimé |
| C-17 | Historique rempli | Clair | Téléphone | Portrait | Terminé, En cours, Erreur, Fichier supprimé |
| C-18 | Historique vide | Sombre | Téléphone | Portrait | — |
| C-19 | Historique vide | Clair | Téléphone | Portrait | — |

### 3.5 Icônes de barre (C-20…C-25)

| # | Capture | Icône | Thème | Taille |
|---|---|---|---|---|
| C-20 | `ic_link` active | Sombre | Téléphone | 24 dp |
| C-21 | `ic_link` inactive | Sombre | Téléphone | 24 dp |
| C-22 | `ic_history` active | Sombre | Téléphone | 24 dp |
| C-23 | `ic_history` inactive | Sombre | Téléphone | 24 dp |
| C-24 | `ic_link` active | Clair | Téléphone | 24 dp |
| C-25 | `ic_history` active | Clair | Téléphone | 24 dp |

### 3.6 Les 36 captures du verre global (G-01…G-36)

Voir `13-liquid-glass-sobre-global.md` §9 : **verre / repli opaque × clair / sombre × pochette claire / foncée ×
portrait / paysage × clavier ouvert / fermé**, sur barre du bas, mini-lecteur, en-tête réduit, lecteur plein écran,
boutons ronds + curseur, feuille, menu, dialogue, champ, puces, Réglages, barre d'onglets, zone de saisie,
carte d'aperçu, en-têtes d'historique, File d'attente et transition mini-lecteur → plein écran.

**Total : 25 + 36 = 61 captures.** Aucune n'existe aujourd'hui : l'infrastructure de capture
(Paparazzi **ou** Roborazzi) doit être créée par BUILD avant toute vérification de rendu.

---

## 4. Ce qui est disponible aujourd'hui (à la place des captures)

| # | Mesure | Source | Résultat |
|---|---|---|---|
| M-01 | Centrage horizontal de la zone de saisie | `DownloadScreenTest` (Compose JVM) | **≤ 2 dp** ✅ |
| M-02 | Centrage vertical de la zone de saisie | `DownloadScreenTest` | **≤ 16 dp** ✅ |
| M-03 | Onglet Lien par défaut, contenu Historique non composé | `DownloadScreenTest` | ✅ |
| M-04 | Indicateur d'onglet qui glisse (`Motion.SpringSnappy`) | `DownloadScreen.kt:137-139` | ✅ |
| M-05 | Remontée en ressort (−120 dp, `SpringSnappy`, `snap()` si réduit) | `DownloadScreen.kt:226-244` | ✅ |
| M-06 | Aucun flou hors `GlassSurface` | `scripts/check-glass-policy.sh` | exit 0 ✅ |
| M-07 | Aucun binding Koin manquant | `scripts/check-koin-bindings.py` | exit 0 ✅ |
| M-08 | Erreurs typées : 11 codes, message FR, action | `DownloadErrorTest` (5 tests) | ✅ |
| M-09 | Registre : 13 sites, 65 URL réelles reconnues | `SiteRegistryTest` (5 tests) | ✅ |
| M-10 | Écran Journal : liste, détail, Partager, Effacer | `DownloadJournalScreenTest` (6 tests) | ✅ |
| M-11 | Pipeline bout en bout (moteur factice) + 1 cas par échec typé | `DownloadPipelineE2ETest` (9 tests) | ✅ |

---

## 5. Écrans existants inchangés

Les écrans hors « Télécharger » ne doivent pas régresser. État vérifié par inspection : aucun de ces écrans
n'a été modifié par la phase `feat/dl-lien-diagnostic` (les modifications portent sur `ui/screen/downloader/`,
`downloader/**`, `App.kt`, `MainActivity.kt` (crash NPE), `ui/screen/settings/` (entrée Journal)).

| Écran | État | Risque de régression |
|---|---|---|
| Accueil / Bibliothèque / Albums / Artistes / Playlists | non modifiés | nul (hors ajout de capsule, doc global §2) |
| Recherche locale | non modifiée | nul |
| Lecteur / File d'attente | non modifiés | nul (le verre global les concernera) |
| Réglages | **modifié** : ajout de l'entrée « Journal des téléchargements » | nul (ajout) |
| `playback/` | **non modifié** | nul |

---

## 6. Critères de conformité — résumé

| Critère | Statut | Preuve |
|---|---|---|
| Zone de saisie centrée H + V | ✅ **CONFORME** | M-01, M-02 |
| Remontée en ressort | ✅ **CONFORME** | M-05 |
| Un message + une action par erreur | ⚠️ **partiel** — message ✅, action ❌ (E-03) | `DownloadError.kt` + `DownloadScreen.kt:317` |
| Historique lisible et complet | ❌ **ÉCART** — pas de regroupement (E-06), 3 actions sur 6 (E-07) | `DownloadScreen.kt:404-460` |
| Icônes cohérentes | ⚠️ **partiel** — 6 sur 10 (E-09) | `res/drawable/` |
| Contraste AA | ❓ **À VÉRIFIER** | E-17 |
| Aucun aspect vitreux | ✅ **CONFORME** | M-06 |
| Textes externalisés | ❌ **ÉCART** — 14 textes en dur (E-04), 22 clés d'erreur manquantes (E-05) | `DownloadScreen.kt` |
| Verre réel (flou, reflet, teinte) | ❌ **ÉCART** (E-12) | `GlassSurface.kt:56` |
| Écrans existants inchangés | ✅ **CONFORME** | §5 |

---

## 7. Conclusion de la revue

**Verdict : REVUE PARTIELLE — 3 écarts bloquants de la v1.2 résolus, 6 écarts majeurs restants.**

- Les deux critères qui cardioppêchaient l'incident utilisateur (**centrage** et **remontée**) sont **conformes et
  prouvés au dp près** (M-01, M-02, M-05).
- Les 6 écarts **majeurs** (E-03…E-08) portent sur le **comportement** de l'écran Lien/Historique :
  action d'erreur non rendue, textes non externalisés, regroupement par date absent, actions d'historique
  incomplètes, puces/badge de sites absents.
- Les 10 écarts **mineurs** (E-09…E-16) portent sur la finition et l'application du verre global.
- **2 écarts restent à vérifier** (E-17, E-18) et **61 captures** manquent : l'infrastructure de capture
  (Paparazzi ou Roborazzi) doit être créée par BUILD.

**Aucun code n'a été modifié par cette revue.** Les corrections relèvent de l'agent BUILD, un écart par commit,
chacun accompagné du test qui échoue avant et passe après.

---

## 8. Fichiers associés

| Fichier | Rôle |
|---|---|
| `11-onglets-lien-historique.md` | spécification des écrans Lien/Historique (source de cette revue) |
| `13-liquid-glass-sobre.md` | verre v1.2, périmètre Téléchargeur (6 éléments) |
| `13-liquid-glass-sobre-global.md` | verre global v1.3 (toute l'app) + « Rendu iPhone récent » |
| `12-tokens-composants-et-motion.md` | tokens et `Motion.kt` |
| `01-design-system.md` | système de design (couleurs, typo) |
| `bmusic/app/src/main/java/com/bmusic/app/ui/screen/downloader/DownloadScreen.kt` | écran implémenté |
| `bmusic/app/src/main/java/com/bmusic/app/ui/theme/GlassSurface.kt` | composant verre implémenté |
| `bmusic/app/src/test/java/com/bmusic/app/ui/screen/downloader/DownloadScreenTest.kt` | mesures de centrage |

*Revue produite par l'agent DESIGNER. Aucun code modifié, aucune mesure claimée sur un appareil.*
