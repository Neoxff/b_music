# B Music — Recette fonctionnelle « Explorer » et « Liens » (v1.2)

> Agent Product Manager B Music. Date : 13 novembre 2026.
> Objet : dire **ce qui a été vérifié, ce qui n'a pas pu l'être, et ce qui reste à tester** — sans valider un point non testé.
> Protocole : scénarios de `docs/pm/07-criteres-acceptation-explorer-liens.md`, complétion par les scénarios de `04-acceptation-lancement.md` §1.2 (S1→S12) pour la non-régression.
> Renvois : `docs/pm/05-spec-explorer-liens.md` · `docs/pm/06-matrice-plateformes.md` · `docs/pm/07-criteres-acceptation-explorer-liens.md`.

## 1. Verdict de recette en l'état

| Question | Réponse |
|---|---|
| Les onglets Explorer et Liens existent-ils dans l'application construite ? | **Non.** L'application de référence (branche `fix/stabilite-1.2`, HEAD `2377fe02`) ne contient ni l'entrée de navigation, ni le fragment, ni le moteur de recherche fonctionnels. [Vérifié] |
| La recherche YouTube fonctionne-t-elle dans l'application construite ? | **Non.** L'onglet « Recherche » de l'écran *Télécharger* appelle une méthode absente du moteur courant : chaque appel échoue avec une erreur « non pris en charge ». [Vérifié] |
| Combien de scénarios ont été exécutés sur l'application ? | **0.** |
| Combien de scénarios sont validés ? | **0.** |
| Le document est-il donc une recette ou un plan de recette ? | **Un plan de recette**, avec le statut de chaque scénario explicitement déclaré. |

**Ce que ce document ne fait pas** : il ne déclare aucun scénario « réussi », « échoué » ou « partiellement réussi » qui n'aurait pas été observé sur un build. Aucun point n'est validé par analogie, par intention de code, ni par ce que ferait « logiquement » une implémentation conforme.

## 2. Pourquoi la recette est bloquée

| # | Cause | Nature | Levier |
|---|---|---|---|
| B1 | Les deux onglets ne sont pas enregistrés dans la barre de navigation (5 fichiers à modifier, cf. lot L6 de l'audit d'architecture). | Manque d'implémentation | Lot L6 |
| B2 | Le moteur de recherche YouTube n'est pas porté dans la branche courante (60 lignes à porter depuis `feat/dl-etape-2`). | Manque d'implémentation | Lot L3 (bloqué par la décision D1) |
| B3 | Les composants d'interface existent mais sont privés à l'écran *Télécharger* (`LienTab` est `private`) : ils doivent être extraits avant d'être promus. | Manque d'implémentation | Lot L4 |
| B4 | La table de détection de plateforme n'existe pas : les états « non pris en charge » exigés par la matrice n'ont pas de support. | Manque d'implémentation | Lot L5 |
| B5 | Le gating par variante n'existe pas : les permissions réseau par flavor ne sont pas encore séparées, donc la variante réseau ne peut pas être qualifiée séparément. | Manque d'implémentation | Lots L2 et L7 |
| B6 | La recette doit être exécutée sur un appareil avec réseau ; l'environnement de développement de ce document n'a pas d'appareil de référence ni de compte de testeur. | Ressource externe | Matrice de recette de `04-acceptation-lancement.md` §1.1 |

## 3. Préconditions de la recette (à réunir avant exécution)

| # | Précondition | Détail | Responsable |
|---|---|---|---|
| P1 | Build de la variante réseau | APK release signé de la variante réseau (jamais la variante `fdroid` pour ces scénarios) | release |
| P2 | Build de la variante `fdroid` | APK release `fdroid` pour les scénarios de non-régression réseau | release |
| P3 | Appareil de référence | Profil B de la matrice (`04-acceptation-lancement.md` §1.1) : milieu de gamme, écran 120 Hz | QA |
| P4 | Réseau | Wi-Fi et données mobiles, plus un mode avion pour les scénarios d'échec réseau | QA |
| P5 | Application YouTube | Installée **et** désinstallée en doublure (pour tester le repli navigateur) | QA |
| P6 | Jeu de liens de test | 12 liens versionnés, un par cas de la matrice (§4.3), **stockés hors du dépôt** | PM |
| P7 | Journal de recette | Modèle de tableau : ID · date · appareil · version · build · résultat · capture | PM |

## 4. Scénarios de recette

Légende du statut : **À exécuter** (non exécuté à ce jour) · **Bloqué** (dépend d'une cause listée en §2).

### 4.1 Explorer

| ID | Scénario | Résultat attendu (critère) | Statut |
|---|---|---|---|
| EX-R1 | Ouvrir l'onglet Explorer depuis la barre de navigation | L'entrée « Explorer » (loupe) est visible et mène à l'écran, champ vide | À exécuter (B1) |
| EX-R2 | Saisir « jazz piano » puis Entrée | Une liste de résultats s'affiche, chaque ligne avec miniature, titre, chaîne | À exécuter (B1, B2) |
| EX-R3 | Saisir 1 caractère | Message « Saisissez au moins 2 caractères. », aucune requête | À exécuter |
| EX-R4 | Saisir 201 caractères | Message « 200 caractères maximum. », texte non tronqué | À exécuter |
| EX-R5 | Saisir uniquement des espaces | Aucun message d'erreur, aucune requête | À exécuter |
| EX-R6 | Observer le chargement | Squelettes affichés, champ éditable | À exécuter (B2) |
| EX-R7 | Requête sans résultat (terme absurde) | État « Aucun résultat » + suggestion ; aucune liste vide | À exécuter (B2) |
| EX-R8 | Mode avion puis recherche | Message « Connexion impossible… » + bouton « Réessayer » ; **aucune liste affichée** | À exécuter (B2, P4) |
| EX-R9 | Service indisponible (déclenchement à définir) | Message « YouTube est momentanément indisponible… » + repli « Ouvrir dans YouTube » | À exécuter (B2) — **mode de déclenchement à définir avec le security-manager** |
| EX-R10 | « Voir plus » | Les résultats suivants s'ajoutent, les précédents restent | À exécuter (B2) |
| EX-R11 | Ouvrir un résultat | Fiche de détail (vignette, titre, chaîne, durée) | À exécuter (B1, B2) |
| EX-R12 | Fermer la fiche | La liste revient à l'identique, sans nouvelle requête | À exécuter (B1) |
| EX-R13 | « Ouvrir dans YouTube » | YouTube s'ouvre (application installée) | À exécuter (P5) |
| EX-R14 | « Ouvrir dans YouTube » sans YouTube installé | Le navigateur s'ouvre | À exécuter (P5) |
| EX-R15 | Rotation de l'écran pendant la recherche | Requête, résultats et position conservés | À exécuter |
| EX-R16 | TalkBack sur la liste de résultats | Chaque ligne annoncée « Titre — chaîne, durée » ; états annoncés | À exécuter (DO-6) |

### 4.2 Liens

| ID | Scénario | Résultat attendu | Statut |
|---|---|---|---|
| LI-R1 | Ouvrir l'onglet Liens | Entrée « Liens » (chaîne ouverte) visible ; champ et liste vides | À exécuter (B1) |
| LI-R2 | Coller 3 liens YouTube | 3 entrées, une par ligne, étiquette « YouTube » | À exécuter (B1, B4) |
| LI-R3 | Coller un bloc avec 1 ligne de texte libre | 2 entrées + compteur « 1 ligne ignorée » | À exécuter (B4) |
| LI-R4 | Ajouter un lien TikTok | Étiquette « TikTok » + état « non lisible dans B Music » + action externe | À exécuter (B4) |
| LI-R5 | Ajouter un lien Instagram / X / Snapchat / Facebook | Étiquette correcte + état non pris en charge | À exécuter (B4) |
| LI-R6 | Ajouter un lien SoundCloud | Étiquette « SoundCloud » + aperçu (titre, durée) | À exécuter (B2, B4, B5) |
| LI-R7 | Ajouter un lien Bandcamp / Vimeo | Étiquette + aperçu | À exécuter (B2, B4) |
| LI-R8 | Ajouter une URL de site inconnu | Étiquette « Plateforme non reconnue », **aucune requête réseau** | À exécuter (B4) |
| LI-R9 | Ajouter un lien invalide (`ftp://`, userinfo, fragment, espace interne, > 2048 car.) | Marqué « Lien non valide », aucune requête réseau | À exécuter (B4) |
| LI-R10 | Ajouter deux fois le même lien | 2ᵉ marqué « Déjà dans la liste » ; « Ajouter quand même » disponible | À exécuter (B4) |
| LI-R11 | Ajouter un lien vers `192.168.1.1/x` | Résolution refusée par la garde anti-SSRF, état d'indisponibilité | À exécuter (DO-3) |
| LI-R12 | Supprimer une entrée / « Tout effacer » | L'entrée disparaît ; confirmation avant effacement global | À exécuter (B1) |
| LI-R13 | Lien YouTube privé | État « Contenu privé ou indisponible. » | À exécuter (B2, P6) |
| LI-R14 | Lien YouTube supprimé / géo-bloqué / soumis à l'âge | États distincts, action externe disponible | À exécuter (B2, P6) |
| LI-R15 | « Ouvrir à l'extérieur » sur un lien non pris en charge | Application de la plateforme ou navigateur ; **jamais d'écran vide** | À exécuter (B4, P5) |
| LI-R16 | Partager une URL vers B Music | Onglet Liens ouvert, champ pré-rempli, aucune action automatique | À exécuter (B4) |

### 4.3 Non-régression et variantes

| ID | Scénario | Résultat attendu | Statut |
|---|---|---|---|
| NR-R1 | Variante `fdroid` : ouvrir l'application | Aucune entrée Explorer/Liens ; permissions sans `INTERNET` ni `ACCESS_NETWORK_STATE` | À exécuter (B5, lot L2) |
| NR-R2 | Variante `fdroid` : mode avion complet | 100 % fonctionnel (scan, lecture, playlists, favoris, notification, thème) | À exécuter |
| NR-R3 | Variante réseau : `aapt dump permissions` | `INTERNET` et `ACCESS_NETWORK_STATE` présents **et** documentés | À exécuter (B5) |
| NR-R4 | Scénarios S1→S12 de `04-acceptation-lancement.md` | Aucun écart ; en particulier S6 (rotation) et S9 (avion) | À exécuter |
| NR-R5 | Écran *Télécharger* inchangé | Comportement, options et libellés identiques à l'existant | À exécuter (B3) |
| NR-R6 | `bash scripts/check-glass-policy.sh` + `./gradlew lint` | Aucun résultat / build vert | À exécuter |
| NR-R7 | TalkBack sur les 2 nouveaux parcours | Annonces correctes, cibles ≥ 48 dp | À exécuter (DO-6) |

## 5. Les deux vérifications demandées en priorité

Le dossier demande explicitement de vérifier deux points. **Aucun n'est vérifié à ce jour** ; ils sont ici formalisés comme scénarios à ne pas manquer.

| # | Vérification demandée | Scénarios concernés | Statut |
|---|---|---|---|
| **V1** | « un lien non intégrable ne provoque pas d'écran vide » | LI-R4, LI-R5, LI-R8, LI-R13, LI-R14, LI-R15 | **Non vérifié** — bloqué par B1/B4 |
| **V2** | « une recherche YouTube affiche bien les résultats dans B Music » | EX-R2, EX-R7, EX-R11 | **Non vérifié** — bloqué par B1/B2 |

**Règle associée** : si l'un de ces deux points échoue à la recette, le bloc concerné est **bloquant** — il ne peut pas être reclassé en « secondaire » après coup.

## 6. Ce qu'il ne faut pas valider

| Piège | Pourquoi c'est un piège |
|---|---|
| « La recherche est codée, donc elle marche » | L'implémentation de référence existe sur une branche non courante ; elle n'a jamais été exécutée dans ce produit. |
| « L'onglet Recherche de l'écran Télécharger est déjà fonctionnel » | L'interface existe, le moteur non : l'appel échoue systématiquement. |
| « Les résultats YouTube s'affichent, donc l'intégration est officielle » | Le mécanisme est non officiel (décision D1 à acter). Une preuve fonctionnelle ne vaut pas une preuve de conformité. |
| « Le lien s'ouvre, donc la plateforme est prise en charge » | L'ouverture externe fonctionne pour **n'importe quelle** URL valide : elle ne prouve rien sur la capacité de B Music à lire la plateforme. |
| « L'aperçu s'affiche, donc la miniature est autorisée » | L'autorisation d'hôte est une règle de sécurité distincte : une substitution locale est un comportement correct, pas une preuve de chargement réseau. |
| « Le build passe, donc la recette est verte » | Le build ne prouve ni les états d'erreur, ni la non-régression, ni l'accessibilité. |
| « Le composant a été copié depuis l'écran Télécharger, donc il est correct » | La copie peut diverger ; les critères de `07-…md` font foi, pas le code antérieur. |

## 7. Ordre d'exécution recommandé

| Ordre | Passage | Condition de passage |
|---|---|---|
| 1 | NR-R1 → NR-R3 (variantes et permissions) | Les permissions correspondent à la promesse de chaque variante. |
| 2 | EX-R1 → EX-R5, EX-R11 → EX-R14 (accès, saisie, résultats, ouverture) | Le parcours nominal fonctionne dans l'application. |
| 3 | LI-R1 → LI-R10 (accès, saisie multiple, détection, validation, dédup) | Le cœur du parcours Liens fonctionne. |
| 4 | **V1** puis **V2** | Les deux points de priorité sont vérifiés. |
| 5 | EX-R6 → EX-R10, EX-R15, LI-R11 → LI-R16 (états d'erreur, pagination, contenus restreints, partage) | Tous les états de la matrice §6 de la spécification sont observés. |
| 6 | NR-R4 → NR-R7 (non-régression, accessibilité, lint) | Aucun écart sur les parcours existants. |

## 8. Journal de recette (à remplir à l'exécution)

| ID | Date | Appareil / version | Build (SHA) | Résultat | Capture | Remarque |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |

**Règle de remplissage** : une ligne ne peut porter « OK » que si le critère correspondant de `docs/pm/07-criteres-acceptation-explorer-liens.md` a été **observé**. Un scénario non exécuté reste vide. Toute anomalie bloque le scénario et le lot qui l'a révélée.
