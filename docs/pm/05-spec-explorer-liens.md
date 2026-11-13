# B Music — Spécification fonctionnelle « Explorer » et « Liens » (v1.2)

> Agent Product Manager B Music. Date : 13 novembre 2026.
> Base : audit d'architecture du 13 novembre 2026 (`docs/architecture/` à créer) + état réel du produit (branche `fix/stabilite-1.2`, HEAD `2377fe02`) + corpus PM existant (`01-vision.md`, `02-roadmap-backlog.md`, `03-user-stories.md`, `04-acceptation-lancement.md`).
> Renvois : `docs/pm/06-matrice-plateformes.md` (compatibilité) · `docs/pm/07-criteres-acceptation-explorer-liens.md` (critères Given/When/Then) · `docs/pm/08-recette-fonctionnelle-explorer-liens.md` (recette) · `docs/security/01-modele-menaces.md` · `docs/design/03-ecrans.md`.
> Format : règles métier numérotées et vérifiables · états explicites · microcopies paramétrées · séparation **MVP obligatoire / secondaire**.

## 0. Statut, portée et quatre avertissements

Ce document spécifie **deux onglets de navigation principale** ajoutés au produit existant : **Explorer** (recherche de vidéos YouTube) et **Liens** (dépôt de liens vidéo multi-plateformes). Il ne décrit aucune fonction nouvelle de lecteur, de bibliothèque ou de téléchargement.

Quatre avertissements conditionnent tout le reste du document. Ils sont la conséquence de l'état vérifié du dépôt, pas des préférences de rédaction.

| # | Avertissement | Source | Conséquence produit |
|---|---|---|---|
| **A1** | **Le contrat v1.0 « zéro réseau » reste intact.** Explorer et Liens n'existent que dans le *flavor* portant le réseau (`github` / `playstore`), jamais dans le flavor `fdroid`. | `docs/lean/01-perimetre-mvp.md` L158 et L165-166 · `docs/security/01-modele-menaces.md` L32 et L55-56 · `docs/pm/04-acceptation-lancement.md` L65 | La fiche produit et la politique de confidentialité doivent être nuancées par variante (lot L10). La recherche et la résolution de liens **ne sont pas des fonctions du lecteur local hors-ligne** : elles appartiennent à la variante réseau. |
| **A2** | **Aucune lecture vidéo intégrée n'est livrée.** Le code ne contient aucun WebView, sur aucune branche. | `git grep -il webview` sur 8 refs → vide | « Liens » = **prévisualisation de métadonnées + ouverture externe**. Toute promesse de lecteur intégré serait fausse. Décision D2 à acter avant l'implémentation. |
| **A3** | **La source des résultats est non officielle.** La recherche YouTube passe par l'extracteur NewPipe (scraping non authentifié de `youtube.com/results`), sans clé API et sans secret dans le client. | `NewPipeExtractorEngine.kt` L140 (branche `feat/dl-etape-2`) · `git grep -inE 'AIza|api_key' → 0 hit` | Risque de fragilité (ReCaptcha) et écart assumé au dossier. Décision D1 : à faire acter par le `security-manager` **avant** l'implémentation. |
| **A4** | **Le moteur de recherche n'existe pas encore dans le produit.** L'onglet « Recherche » de l'écran *Télécharger* appelle une méthode absente du moteur courant. | `MediaExtractorEngine.kt` L23-27 (défaut `Unsupported`) vs `NewPipeExtractorEngine.kt` sur HEAD (0 occurrence de `search`) | L'onglet Recherche actuel est une coquille : il échoue systématiquement. Cet onglet **ne doit pas être promu tel quel**. |

**Règle de vérité appliquée** : chaque affirmation de ce document est étiquetée **[Vérifié]** (observé dans le dépôt ou par commande), **[Déduit]** (raisonné), **[À vérifier]** (à confirmer) ou **[Bloqué]**.

## 1. Contexte

B Music est un lecteur **local** : bibliothèque MediaStore, lecture Media3 sur fichiers de l'appareil, aucune donnée sortante. La promesse v1.0 est explicite et publique (« aucune autorisation réseau n'est utilisée », `04-acceptation-lancement.md` L137-138).

La demande porte sur deux espaces de **découverte** :
- **Explorer** : « un onglet de recherche visuelle avec une loupe, présenté comme un petit navigateur de découverte » ; la saisie d'une requête doit faire apparaître des résultats YouTube **dans** B Music, avec au minimum miniature, titre et chaîne.
- **Liens** : « un onglet représenté par une icône de chaîne dont un maillon est ouvert » ; l'utilisateur colle un ou plusieurs liens (YouTube, TikTok, Snapchat, Facebook, Instagram, X et autres) et obtient une prévisualisation ou une lecture intégrée **lorsque la plateforme l'autorise**.

Les deux onglets ne sont donc **pas** une fonction de téléchargement : ils sont un point d'entrée vers des contenus externes. La fonctionnalité de téléchargement existante (écran *Télécharger*, derrière l'option `Preferences.downloaderEnabled`) reste **inchangée** et garde son propre point d'entrée.

## 2. Objectifs et non-objectifs

### 2.1 Objectifs (MVP)

| # | Objectif | Critère de succès (mesurable) |
|---|---|---|
| O1 | Les deux onglets sont **accessibles en un tap** depuis la navigation principale, dans la variante réseau. | Les deux entrées sont présentes dans la barre de navigation après reconstruction runtime, et un tap ouvre l'écran correspondant. |
| O2 | Une recherche YouTube **fonctionnelle** affiche des résultats **dans** B Music. | Une requête valide produit une liste de résultats contenant miniature, titre, chaîne et durée. |
| O3 | L'utilisateur **ne peut pas** confondre échec et succès. | Les états « chargement », « résultats », « aucun résultat » et « erreur » sont mutuellement exclusifs ; l'état erreur propose une action « Réessayer ». |
| O4 | L'ajout de liens **multiples** en une seule action est possible (un lien par ligne). | Une saisie de N lignes valides produit N entrées de liste. |
| O5 | Chaque lien est **identifié** et son état est explicite. | Chaque ligne porte une étiquette de plateforme et un état : prévisualisable, non pris en charge, invalide, doublon. |
| O6 | Le repli externe **fonctionne toujours** pour un lien non intégrable. | Un lien non pris en charge ouvre l'application ou le navigateur via `ACTION_VIEW`, sans écran vide. |
| O7 | La variante `fdroid` reste **strictement hors-ligne**. | `aapt dump permissions` du variant `fdroid` ne contient ni `INTERNET` ni `ACCESS_NETWORK_STATE` ; les onglets sont absents de la navigation. |

### 2.2 Non-objectifs (hors périmètre, avec raison)

| Exclu | Raison |
|---|---|
| Lecture vidéo intégrée dans l'application | Aucun WebView dans le code ; le coût de durcissement (JS, cookies, surface d'attaque) excède la valeur. Décision D2. |
| Téléchargement depuis Explorer ou Liens | Hors mission du dossier ; le téléchargement garde son écran dédié et son option. |
| Compte, connexion, synchronisation, historique cloud | Interdits par la vision v1.0 (`01-vision.md` §4 principe 3) ; non ré-introduits par une feature d'onglet. |
| Analyse, télémétrie, mesure d'usage des nouveaux onglets | `SEC-2`/`SEC-3` : aucune télémétrie. La priorisation ne peut donc pas s'appuyer sur des données d'usage (voir §10). |
| Clé API YouTube embarquée | Une clé dans un APK GPL est publiquement extractible. |
| Support de plateforme « officiel » déclaré | Interdit sans test fonctionnel sur la méthode officielle actuelle (règle PM-04). Voir `06-matrice-plateformes.md`. |

## 3. Utilisateurs

| Persona | Besoin | Ce que l'onglet doit lui apporter |
|---|---|---|
| **Léa** (24, étudiante) | « je regarde des vidéos sur mon téléphone, je veux les retrouver sans quitter ma musique » | Explorer : résultats dans l'app, vignette + titre + chaîne, ouverture en un tap. Liens : coller un lien reçu sur WhatsApp/Instagram et le retrouver. |
| **Claire** (34, développeuse) | « je veux comprendre ce que l'app fait et ne rien installer de fermé » | Liens : voir la plateforme détectée, le contenu, et être **informée** quand l'intégration n'est pas possible (message non technique, action externe). |
| **Marc** (41, livreur) | « je n'ai pas le temps, rien ne doit me bloquer » | États explicites, aucune erreur muette, actions secondary pas de charge inutile ; l'onglet reste discret. |

Aucun persona n'a besoin de compte, ni de réseau pour le cœur du produit (lecture locale) : les deux onglets sont une **extension de découverte**, pas une condition d'usage.

## 4. Périmètre fonctionnel

### 4.1 Inclus (MVP)

1. Onglet **Explorer** accessible depuis la barre de navigation, visible uniquement en variante réseau.
2. Champ de recherche avec icône loupe, validation de la requête, exécution, pagination.
3. Résultats YouTube : miniature, titre, chaîne, durée ; état vide ; état erreur + réessai.
4. Fiche de détail d'un résultat (métadonnées) + action **ouvrir dans YouTube** (externe) + copier le lien.
5. Onglet **Liens** accessible depuis la barre de navigation, visible uniquement en variante réseau.
6. Saisie d'un ou plusieurs liens (un par ligne) via champ et bouton « Coller ».
7. Détection de la plateforme, validation, déduplication, liste, suppression.
8. Prévisualisation des métadonnées d'un lien pris en charge.
9. État explicite « non pris en charge » + **ouverture externe** pour toute autre plateforme.
10. Microcopies FR + EN, paramétrées, centralisées.

### 4.2 Exclu (secondaire / hors périmètre)

Historique de recherche *(P1 — le composant existe déjà, voir §10)* · favoris · ajout à une playlist · filtres avancés *(le modèle ne connaît que `ALL` et `MUSIC`)* · partage d'un résultat · mode compact · préférences propres aux onglets · synchronisation.

## 5. Règles métier (numérotées, testables)

**Règles communes aux deux onglets**

| # | Règle | Source / justification |
|---|---|---|
| R1 | Un onglet n'est présent dans la barre de navigation que si la variante réseau est active **et** si l'option utilisateur « Explorer et Liens » est activée. | `NetworkFeature` existant + `network_features_enabled_by_default` (`fdroid` = `false`). [Vérifié] |
| R2 | Un onglet absent de la navigation ne doit jamais être accessible par une route hors navigation ; il ne doit pas non plus provoquer d'erreur au lancement. | Le menu est reconstruit à l'exécution (`updateTabs()`), d'où le NPE déjà livré et corrigé en `7d78664f`. [Vérifié] |
| R3 | Aucun secret (clé, jeton, session) n'est stocké, journalisé ou transmis pour ces onglets. | `SEC-2`/`SEC-3` ; aucun secret n'existe dans le dépôt. [Vérifié] |
| R4 | Aucune requête réseau n'est émise par la variante `fdroid`. | `SEC-1`. [Vérifié par `aapt dump permissions`, à rejouer après implémentation] |
| R5 | Les miniatures distantes passent par le chargeur Coil **dédié** et son garde-fou (HTTPS, liste d'hôtes, ≤ 2 Mio, ≤ 4 concurrentes) ; le chargeur global n'est jamais modifié. | `DownloaderCoilFactory.kt` L19-51. [Vérifié] |
| R6 | Une requête ou un lien utilisateur n'est jamais journalisé en clair. | `QueryValidator` (hash seul) ; `SEC-2`. [Vérifié] |
| R7 | Tout état d'erreur est **actionnable** : il nomme la cause et propose une action (« Réessayer », « Ouvrir à l'extérieur », « Corriger le lien »). | O3, O6. [Spécifié ici] |
| R8 | Un état d'erreur **réplace** la liste de résultats, il ne se superpose pas à une liste obsolète. | O3 (« ne jamais croire qu'une recherche a réussi si elle a échoué »). [Spécifié ici] |

**Explorer**

| # | Règle | Source |
|---|---|---|
| R9 | La requête est nettoyée (espaces de bord supprimés, caractères de contrôle retirés) puis validée : longueur **2 à 200** caractères ; en dehors, la recherche n'est pas émise. | `QueryValidator` MIN 2 / MAX 200 ; implémentation de `feat/dl-etape-2` L130-133. [Vérifié] |
| R10 | La recherche est déclenchée après un délai de stabilisation de la saisie (400 ms), et le nombre de résultats par page est plafonné (20 dans l'implémentation de référence). | `SearchViewModel` (debounce 400 ms) ; `feat/dl-etape-2` L179 `.take(20)`. [Vérifié] |
| R11 | Un résultat affiche au minimum **miniature, titre, chaîne** ; la durée est affichée quand elle est connue. | Demande utilisateur. `SearchResultItem` porte `title`, `channel`, `durationMs`, `thumbnailUrl`. [Vérifié] |
| R12 | Les résultats proviennent de **YouTube uniquement** en v1.2 ; aucune autre source n'est exposée à l'utilisateur. | L'implémentation de référence cible `youtube.com/results`. [Vérifié] |
| R13 | Un résultat lead vers une page **externe** (application YouTube si installée, sinon navigateur) — jamais vers un lecteur intégré inexistant. | `IntentExt.openWeb()` L35-36 ; A2. [Vérifié] |
| R14 | La pagination ne s'affiche que s'il existe une page suivante ; le chargement d'une page supplémentaire s'ajoute à la liste existante sans l'effacer. | `SearchPage.nextPageIndex`. [Vérifié] |

**Liens**

| # | Règle | Source |
|---|---|---|
| R15 | Une saisie peut contenir **plusieurs liens**, **un par ligne** ; les lignes vides sont ignorées. | Demande utilisateur. [Spécifié ici] |
| R16 | Chaque ligne est validée **avant** toute action réseau : longueur ≤ 2048 ; schémas `http`/`https` uniquement ; pas d'identifiant utilisateur (`@`) ; pas de fragment (`#`) ; pas de caractère de contrôle ni d'espace interne ; hôte syntaxiquement valide ; port 1-65535. | `UrlValidator` (MAX_LENGTH 2048, L56, L62, L65, L46, L50, L111-126, L94). [Vérifié] |
| R17 | La détection de plateforme est **indépendante** de la résolution : un lien vers une plateforme non supportée est reconnu et affiché comme tel, avec une action externe — il n'est pas rejeté silencieusement. | `NewPipeExtractorEngine` KDoc L30-34 (Instagram/TikTok/X non enregistrés) + besoin d'un état explicite. [Vérifié] |
| R18 | Les liens sont **dédupliqués** sur l'URL normalisée (schéma et hôte en minuscules, port par défaut retiré) ; un doublon est signalé et n'est pas ajouté une seconde fois. | `UrlValidator` normalisation L99-105 ; `SearchHistory` dédoublonné (max 10) comme précédent. [Vérifié] |
| R19 | La résolution d'un lien ne doit **jamais** déclencher de téléchargement ni d'extraction de flux depuis les onglets. | A2 + règle du dossier « ne pas télécharger, extraire ou contourner les protections ». [Spécifié ici] |
| R20 | Un contenu **privé, supprimé, soumis à l'âge, ou nécessitant une connexion** produit un état distinct, pas un échec générique. | `ExtractorError` distingue `ProtectedContent`, `SourceUnavailable`, `Network`, `Unsupported`, `UrlInvalid`, `QueryInvalid`. [Vérifié] |
| R21 | L'ouverture externe passe par `ACTION_VIEW` ; l'application demande l'installation de la cible au système. | `IntentExt.openWeb()`. [Vérifié] |

## 6. États et erreurs

Chaque écran de données expose exactement **un** état à la fois.

| État | Déclencheur | Ce que l'utilisateur voit | Action disponible |
|---|---|---|---|
| **Repos** | Onglet ouvert, aucune requête | Champ de recherche vide + hint ; pour Liens, champ d'ajout + liste vide | Saisir / coller |
| **Saisie invalide** | Longueur < 2 ou > 200 (Explorer) ; URL non conforme (Liens) | Message d'aide **sous le champ**, pas de toast, pas d'écran d'erreur | Corriger la saisie |
| **Chargement** | Requête émise / résolution demandée | Squelettes (jamais un spinner agressif) ; `docs/design/03-ecrans.md` §3 | Attendre ; annulation possible |
| **Résultats** | Réponse reçue et non vide | Liste des résultats / des liens | Ouvrir, prévisualiser, supprimer, paginer |
| **Aucun résultat** | Réponse reçue, liste vide | Message dédié + suggestion | Reformuler ; voir aussi `no_results` déjà présent en ressource |
| **Erreur réseau** | `ExtractorError.Network` | Message nommant la cause + bouton « Réessayer » | Réessayer |
| **Service indisponible** | `ExtractorError.SourceUnavailable` (ReCaptcha compris) | Message **sans jargon** + « Réessayer plus tard » + repli « Ouvrir dans YouTube » (Explorer) | Réessayer ; ouvrir à l'extérieur |
| **Non pris en charge** | `ExtractorError.Unsupported` ou plateforme hors matrice | Message nommant la plateforme + action « Ouvrir à l'extérieur » | Ouvrir à l'extérieur |
| **Contenu restreint** | `ExtractorError.ProtectedContent` | Message « contenu privé ou non accessible » ; aucune tentative de contournement | Ouvrir à l'extérieur |

**Interdiction explicite** : aucun de ces états ne peut se traduire par un écran vide, un message générique non actionnable, ou une liste affichée comme « réussie » alors que la requête a échoué.

## 7. Parcours Explorer (PM-02)

1. **Accès.** L'utilisateur ouvre l'onglet Explorer depuis la barre de navigation. Le champ de recherche a le focus, la liste est en état repos.
2. **Saisie.** L'utilisateur tape une requête. La loupe est affichée dans le champ ; une icône « effacer » apparaît dès qu'il y a du texte.
3. **Validation.** Après stabilisation de la saisie (400 ms), si la requête nettoyée fait 2 à 200 caractères, la recherche est émise ; sinon un message d'aide s'affiche sous le champ et **aucune** requête n'est émise.
4. **Chargement.** Des squelettes de lignes apparaissent ; le champ reste éditable.
5. **Résultats.** La liste remplace les squelettes : chaque ligne = miniature (ratio 16:9), titre (2 lignes maximum), chaîne, durée si connue. La liste est cliquable, la vignette est décorative (`contentDescription` sur la ligne).
6. **Sélection.** Un tap ouvre la **fiche de détail** (bottom sheet ou plein écran selon le format) : grande vignette, titre, chaîne, durée, actions.
7. **Actions de la fiche.** « Ouvrir dans YouTube » (externe) · « Copier le lien » (presse-papiers) · retour par gesture ou barre.
8. **Retour aux résultats.** Le retour restitue la liste **exactement** telle qu'elle était (position, page courante, contenu), sans rechargement ni nouvelle requête.
9. **Pagination.** Un bouton « Voir plus » en fin de liste charge la page suivante et **ajoute** les éléments ; l'erreur de pagination affiche un message sous la liste sans vider les résultats existants.
10. **Recherche suivante.** Une nouvelle saisie remplace la liste courante et réinitialise la pagination.
11. **Historique (P1).** À focus sur un champ vide, les 10 dernières requêtes (dédupliquées, ordre antéchronologique) sont proposées ; un tap relance la recherche.

**Comportements clavier et mobile**
- Clavier matériel : `Entrée` déclenche la recherche (elle n'est pas émise par la frappe seule) ; `Échap` quitte la fiche de détail.
- Clavier logiciel : masqué au scroll (comportement de `SE-1`).
- Rotation : la requête, les résultats et la position sont conservés (même exigence que `S6` de la matrice de recette).
- Mode avion / réseau absent : l'onglet est visible, la recherche échoue avec l'état « Erreur réseau » — **jamais** un écran vide.
- Accessibilité : chaque ligne de résultat est annoncée par TalkBack (« Titre — chaîne, durée »), chaque état a une annonce vocale (« Recherche en cours », « 12 résultats », « Aucun résultat »).

**Cas limites explicitement traités**

| Cas | Comportement attendu |
|---|---|
| Requête vide | Aucune requête ; l'onglet reste en état repos. |
| Requête uniquement composée d'espaces | Nettoyée → longueur < 2 → message d'aide, aucune requête. |
| Requête de 1 caractère | Message d'aide « 2 caractères minimum » (jamais de toast). |
| Requête > 200 caractères | Message d'aide « 200 caractères maximum » ; **la saisie n'est pas tronquée silencieusement**. |
| Requête avec caractères de contrôle | Nettoyée avant validation. |
| Erreur réseau | État erreur + « Réessayer ». |
| Service indisponible (ReCaptcha) | État « service indisponible » + repli externe ; aucun message technique. |
| Aucun résultat | État vide + suggestion de reformulation. |
| Vidéo non intégrable | Pas d'intégration : ouverture externe. |
| Résultats sans miniature | Vignette de substitution locale (pas de recherche réseau supplémentaire). |

## 8. Parcours Liens (PM-03)

1. **Accès.** Onglet Liens depuis la barre de navigation ; champ d'ajout vide, liste vide.
2. **Saisie multiple.** L'utilisateur colle un bloc contenant plusieurs liens, **un par ligne** (ou les saisit à la main) ; les lignes vides sont ignorées.
3. **Analyse.** Chaque ligne est validée (R16) puis identifiée (R17). Chaque ligne produit **une** entrée de liste avec : la plateforme détectée, l'état, et les actions disponibles.
4. **Dédoublonnage.** Une ligne dont l'URL normalisée existe déjà dans la liste est marquée « doublon » et n'est pas ajoutée une seconde fois ; l'utilisateur peut forcer l'ajout.
5. **Liste.** Chaque entrée affiche : vignette ou icône de plateforme, titre connu ou URL, étiquette de plateforme, état. La suppression est possible entrée par entrée, et « Tout effacer » est confirmé.
6. **Prévisualisation.** Sur une entrée prise en charge, un tap demande les métadonnées et affiche une fiche : vignette, titre, auteur/chaîne, durée, nature du contenu (audio/vidéo).
7. **Non pris en charge.** L'entrée affiche l'état « Cette plateforme n'est pas lue dans B Music » + l'action « Ouvrir à l'extérieur » ; **aucun écran vide, aucun échec silencieux**.
8. **Ouverture externe.** L'action ouvre l'URL via `ACTION_VIEW` : l'application de la plateforme si installée, sinon le navigateur.
9. **Partage (P1).** Un lien partagé depuis une autre application arrive dans B Music (`ACTION_SEND` déjà traité → `download_prefill_url`) et **pré-remplit** le champ Liens, sans déclencher d'action automatique.
10. **Vidage.** « Tout effacer » vide la liste après confirmation ; aucune donnée n'est conservée.

**Cas limites explicitement traités**

| Cas | Comportement attendu |
|---|---|
| Lien syntaxiquement invalide | Entrée marquée « Lien invalide », raison non technique, non ajoutée au réseau. |
| Plateforme non reconnue | Entrée marquée « Plateforme non reconnue » + ouverture externe possible. |
| Plateforme reconnue mais non lisible (TikTok, Instagram, X…) | Entrée marquée « Non pris en charge » + ouverture externe. |
| Lien privé / supprimé | État « Contenu privé ou indisponible » ; aucune tentative de contournement. |
| Lien soumis à restriction d'âge ou à une connexion | État « Contenu réservé à certains comptes » + ouverture externe. |
| Lien régional / géo-bloqué | État « Contenu indisponible dans votre région » + ouverture externe. |
| Doublon dans la même saisie | Signalé dans la liste, ajout unique. |
| Zone de texte contenant du texte libre autour des liens | Lignes non conformes ignorées avec un compteur « X ligne ignorée ». |

## 9. Fonctionnalités existantes à préserver (non-régression)

| Fonctionnalité | Preserved how |
|---|---|
| Bibliothèque, lecteur, file, playlists, favoris | Aucun changement de code ; vérification par la suite de tests existante (`testFdroidDebugUnitTest`). |
| Recherche locale | Reste dans son écran ; l'onglet Explorer est **distinct** (pas de fusion, pas de collision d'id de menu). |
| Écran *Télécharger* + option `Preferences.downloaderEnabled` | Inchangé ; les composants `SearchTab`/`LienTab` sont **extraits** (copiés ou factorisés) sans modifier le comportement de téléchargement. |
| Notification média, lockscreen, service de lecture | Aucun changement. |
| Réglages, thème, accent dynamique | Aucun changement, hors ajout d'une option pour les onglets (R1). |
| Promesse « zéro réseau » du variant `fdroid` | Préservée et **rendue vérifiable** par un test de permissions par variant. |
| `check-glass-policy.sh` | Passe toujours : aucun blur, aucune transparence décorative dans les nouveaux écrans. |
| GPL-3.0, attribution, licences | Inchangés ; aucune nouvelle dépendance propriétaire. |

## 10. Priorisation (PM-06)

**Méthode** : le corpus historique utilise le RICE (`02-roadmap-backlog.md` §2). Le RICE n'est **pas** appliqué ici, et le motif est explicite : le produit est « zéro télémétrie » par construction (`SEC-2`, `SEC-3`), donc le facteur **Reach n'est pas mesurable** et toute valeur chiffrée serait inventée. La priorisation retenue est donc **explicite et argumentée** sur quatre axes : valeur utilisateur, risque, coût, dépendances. Les coûts suivent l'échelle du dépôt (S ≤ 1 j · M 2-5 j · L ≥ 5 j).

| Bloc | Valeur utilisateur (raison) | Risque (P × I) | Coût | Dépendances | Verdict |
|---|---|---|---|---|---|
| Accès aux 2 onglets | Élevée — condition d'entrée de tout le reste | Élevé × Élevé (NPE déjà vécu sur le menu reconstruit) | M | Enregistrement nav (5 fichiers) | **MVP** |
| Moteur de recherche YouTube | Élevée — sans lui, l'onglet est vide | Élevé × Élevé (A3 : ToS non acté, ReCaptcha) | M | D1, port du moteur | **MVP** (après D1) |
| Affichage des résultats | Élevée — c'est la promesse visible | Faible × Moyen | M | Moteur, Coil dédié | **MVP** |
| États (chargement / vide / erreur / réessai) | Élevée — O3 : ne jamais mentir sur l'état | Faible × Élevé | S | — | **MVP** |
| Pagination | Moyenne — sans elle, la recherche est superficielle | Faible × Faible | M | Moteur | **MVP** |
| Fiche de détail + ouverture externe | Élevée — c'est l'action utile | Faible × Moyen | M | — | **MVP** |
| Liens : saisie multiple | Élevée — usage principal (coller un bloc) | Faible × Moyen | M | — | **MVP** |
| Liens : détection de plateforme | Élevée — condition de l'état explicite | Moyen × Moyen | M | `PlatformPolicy` (L5) | **MVP** |
| Liens : validation + dédup | Élevée — sécurité et propreté de la liste | Faible × Élevé | S | `UrlValidator` existant | **MVP** |
| Liens : prévisualisation | Moyenne — enrichit, non bloquant | Moyen × Moyen | M | Résolution | **MVP** |
| Liens : non pris en charge + ouverture externe | Élevée — O6 : jamais d'écran vide | Faible × Élevé | M | `PlatformPolicy` | **MVP** |
| Gating réseau par variante | Critique — condition de la promesse v1.0 | Faible × Élevé | S | Manifest par flavor (L2) | **MVP** |
| Microcopies FR/EN | Moyenne — bloquant lint + qualité perçue | Faible × Moyen | S | Arbitrage D5 | **MVP** |
| Historique de recherche | Faible — le composant existe déjà (max 10, dédoublonné) | Faible × Faible | S | — | **P1** |
| États de contenu restreint (privé/âge/connexion) | Faible — micro-suite déjà couverte par `ExtractorError` | Faible × Moyen | S | — | **P1** |
| Partage d'un résultat | Faible | Faible × Faible | S | — | **P2** |
| Favoris / playlist / filtres avancés / mode compact / préférences / sync | Faible à moyenne | Moyen × Moyen | M à L | — | **P2 / hors v1.2** |

**Règle de séquencement** : aucune ligne « effet visuel » ne précède une ligne fonctionnelle. Les états d'erreur et l'ouverture externe sont dans le MVP : ce sont eux qui empêchent l'utilisateur de croire à un succès.

## 11. Microcopies (PM-07)

**Règles de rédaction**
1. Une **chaîne par intention**, centralisée dans les ressources Android ; aucune chaîne en dur dans le code (lint `HardcodedText`).
2. Les messages d'erreur nomment **la cause en français courant** et proposezent **une action**. Aucun terme technique (« ReCaptcha », « HTTP 403 », « extractor », « SSRF »).
3. Aucune formulation n'affirme un succès non obtenu.
4. Aucune formulation ne promet une lecture intégrée pour une plateforme qui ne le permet pas.
5. Les noms de plateformes restent les noms propres (« YouTube », « TikTok ») ; le reste est traduit.
6. Les valeurs dynamiques passent par des paramètres (`%1$s`), jamais par concaténation.

**Conventions de ressources observées dans le dépôt** [Vérifié]
- `values/strings.xml` = 994 chaînes (langue par défaut du dépôt, anglais de l'upstream) ; `values-fr-rFR/strings.xml` = 981 chaînes ; **il n'existe pas de dossier `values-en/`**.
- `app_name` = « B Music » dans `values/donottranslate.xml` (non traduisible).
- La clé `no_results` existe déjà (« No results » / « Aucun résultat ») → réutilisable.
- **Écart à trancher (D5)** : la story `A11Y-4` de `03-user-stories.md` affirme « FR est la locale par défaut (`values/strings.xml`) avec EN fourni (`values-en/`) », ce que **le code ne confirme pas** (dossier par défaut = anglais, FR dans `values-fr-rFR/`, pas de `values-en/`). Décision attendue : soit aligner la documentation sur le code, soit Basculer le dossier par défaut sur le FR (migration de ~994 chaînes, à chiffrer).

**Textes (FR par défaut dans la spécification ; EN à fournir dans les deux dossiers de ressources)**

| Clé | FR | EN | Usage |
|---|---|---|---|
| `explorer_tab` | Explorer | Explore | Libellé d'onglet |
| `links_tab` | Liens | Links | Libellé d'onglet |
| `explore_search_hint` | Rechercher sur YouTube | Search on YouTube | Placeholder du champ |
| `explore_search_action` | Rechercher | Search | Bouton / action clavier |
| `explore_clear` | Effacer la recherche | Clear search | Icône d'effacement |
| `explore_query_too_short` | Saisissez au moins 2 caractères. | Enter at least 2 characters. | Erreur de saisie, sous le champ |
| `explore_query_too_long` | 200 caractères maximum. | 200 characters maximum. | Erreur de saisie |
| `explore_loading` | Recherche en cours… | Searching… | État de chargement (annonce vocale) |
| `explore_results_count` | %1$d résultat(s) | %1$d result(s) | Annonce vocale / bandeau |
| `explore_empty_title` | Aucun résultat | No results | État vide (réutiliser `no_results` si le ton convient) |
| `explore_empty_hint` | Essayez d'autres mots. | Try different words. | État vide |
| `explore_error_network` | Connexion impossible. Vérifiez votre réseau et réessayez. | Connection failed. Check your connection and try again. | Erreur réseau |
| `explore_error_service` | YouTube est momentanément indisponible. Réessayez plus tard. | YouTube is temporarily unavailable. Try again later. | Service indisponible |
| `explore_error_service_action` | Ouvrir dans YouTube | Open in YouTube | Repli externe |
| `explore_retry` | Réessayer | Try again | Action commune |
| `explore_load_more` | Voir plus | See more | Pagination |
| `explore_open_external` | Ouvrir dans YouTube | Open in YouTube | Action de la fiche |
| `explore_copy_link` | Copier le lien | Copy link | Action de la fiche |
| `explore_detail` | Détail | Details | Titre de la fiche |
| `explore_history_title` | Recherches récentes | Recent searches | Historique (P1) |
| `links_add_hint` | Collez un lien par ligne | Paste one link per line | Placeholder |
| `links_add_action` | Ajouter | Add | Bouton |
| `links_paste_action` | Coller | Paste | Bouton |
| `links_empty_title` | Aucun lien ajouté | No links added yet | État vide |
| `links_empty_hint` | Collez un lien pour voir sa plateforme et son aperçu. | Paste a link to see its platform and preview. | État vide |
| `links_invalid` | Lien non valide. | Invalid link. | État d'entrée |
| `links_unknown_platform` | Plateforme non reconnue | Unrecognised platform | État d'entrée |
| `links_unsupported_platform` | %1$s n'est pas lisible dans B Music. | %1$s cannot be played in B Music. | État d'entrée |
| `links_private_content` | Contenu privé ou indisponible. | Private or unavailable content. | État d'entrée |
| `links_age_restricted` | Ce contenu est réservé à certains comptes. | This content is restricted to certain accounts. | État d'entrée |
| `links_region_blocked` | Contenu indisponible dans votre région. | Content unavailable in your region. | État d'entrée |
| `links_duplicate` | Déjà dans la liste | Already in the list | État d'entrée |
| `links_duplicate_force` | Ajouter quand même | Add anyway | Action de doublon |
| `links_ignored_lines` | %1$d ligne(s) ignorée(s) | %1$d line(s) ignored | Compteur de saisie |
| `links_preview_loading` | Lecture des informations… | Loading details… | Chargement de l'aperçu |
| `links_preview_title` | Aperçu | Preview | Titre de la fiche |
| `links_open_external` | Ouvrir à l'extérieur | Open externally | Action de repli |
| `links_remove` | Retirer | Remove | Action par entrée |
| `links_clear_all` | Tout effacer | Clear all | Action de liste |
| `links_clear_all_confirm` | Retirer tous les liens ? | Remove all links? | Confirmation |
| `explore_links_enabled` | Explorer et Liens | Explore and Links | Libellé de l'option utilisateur (R1) |
| `explore_links_enabled_summary` | Permet de chercher sur YouTube et d'ouvrir des liens vidéo. | Allows searching YouTube and opening video links. | Résumé de l'option |

## 12. Accessibilité, localisation, performance

| Sujet | Exigence | Source / réserve |
|---|---|---|
| Cibles tactiles | **≥ 48 dp** pour toute action de ces deux écrans. | `A11Y-2` (PM) vs `Dimens.kt` `touchTarget = 44 dp` (code) → **écart à trancher (D6)** ; le PM retient 48 dp. |
| Contraste | AA sur tout texte, y compris les états d'erreur et les étiquettes de plateforme. | `A11Y-1`. |
| TalkBack | Chaque résultat et chaque entrée de lien est une cible unique annoncée ; les états (chargement, vide, erreur) sont annoncés. | `A11Y-3`, étendu aux 2 nouveaux parcours. |
| Mouvement | Aucune animation obligatoire ; respect de « supprimer les animations » ; squelettes non flashy. | `AN-7`, `DS-5`. |
| Zéro verre | Aucune surface translucide, aucun flou, aucun reflet. | `DS-5` + `check-glass-policy.sh` (CI). |
| Langues | Chaque nouvelle chaîne existe en FR et EN ; dossier par défaut à arbitrer (D5). | `A11Y-4` vs code réel. |
| Performance | Aucun impact mesurable sur le démarrage à froid (< 600 ms) ni sur le budget APK (< 15 Mo) ; les onglets ne sont pas instanciés au lancement (chargement paresseux). | `PF-1`, `PF-4` ; le code utilise des `factory` Koin (chargement paresseux). [Vérifié] |
| Vie privée | Aucune requête hors de l'action explicite de l'utilisateur ; aucune donnée d'usage collectée ; historique local limited à 10 entrées, effaçable. | `SEC-2` ; `SearchHistory` max 10. [Vérifié] |

## 13. Points ouverts (décisions requises)

| # | Décision | Options | Par défaut retenu | Qui décide | Échéance |
|---|---|---|---|---|---|
| **D1** | Acter l'écart « source non officielle » (NewPipe) au regard de la règle du dossier. | (a) acter avec justification écrite ; (b) revenir à l'API officielle Data v3 avec clé fournie par l'utilisateur ; (c) renoncer à Explorer | (a), acter par le `security-manager` | security-manager + coordinator | **avant implémentation** |
| **D2** | Prévisualisation « Liens ». | (a) métadonnées + ouverture externe ; (b) lecteur intégré WebView | (a) | coordinator + security-manager | avant implémentation |
| **D5** | Locale par défaut des nouvelles chaînes. | (a) suivre le code (défaut EN, FR dans `values-fr-rFR/`) et corriger `A11Y-4` ; (b) basculer le défaut sur FR (migration ~994 chaînes) | (a) pour v1.2 | PM + Lean | avant implémentation |
| **D6** | Taille des cibles tactiles. | (a) 48 dp (PM, `A11Y-2`) ; (b) 44 dp (`Dimens.kt`) | (a) | Lean + designer | avant maquette |
| **D7** | Placement des deux onglets dans la barre de navigation (à côté des onglets de bibliothèque, ou dans un groupe distinct) — la barre est reconstruite à l'exécution et `MAX_VISIBLE_CATEGORIES = 5`. | (a) groupe distinct après les catégories ; (b) catégories assimilées | (a) | designer + PM | avant maquette |
| **D4** | Reformulation des docs de promesse par variante (`SEC-1`, DoD #3, politique de confidentialité, fiche store). | — | à rédiger | PM + release | avant publication |

## 14. Récapitulatif

| Bloc | Stories | MVP (P0) | Secondaire |
|---|---|---|---|
| Explorer | EX-1 → EX-11 | 10 | 1 (historique) |
| Liens | LI-1 → LI-10 | 8 | 2 (contenu restreint, partage) |
| **Total** | **21** | **18** | **3** |

Critères d'acceptation : `docs/pm/07-criteres-acceptation-explorer-liens.md`.
Compatibilité plateformes : `docs/pm/06-matrice-plateformes.md`.
Recette : `docs/pm/08-recette-fonctionnelle-explorer-liens.md`.
