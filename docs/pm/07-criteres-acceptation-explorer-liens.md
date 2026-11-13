# B Music — Critères d'acceptation « Explorer » et « Liens » (v1.2)

> Agent Product Manager B Music. Date : 13 novembre 2026.
> Format : **Étant donné / Quand / Alors**, une assertion par ligne. Une story = un comportement testable = une seule raison de changer.
> Spécification : `docs/pm/05-spec-explorer-liens.md` · Matrice plateformes : `docs/pm/06-matrice-plateformes.md` · Recette : `docs/pm/08-recette-fonctionnelle-explorer-liens.md`.
> Conventions du dépôt : échelle d'effort S ≤ 1 j · M 2-5 j · L ≥ 5 j ; agents PM/BIZ/DSG/LNA/SEC/REL.

## 0. Règle de preuve

Une story est **terminée** quand chacun de ses critères est **observé** :

| Type de critère | Preuve acceptable |
|---|---|
| Comportement d'interface | Test automatisé (Robolectric/JUnit) **ou** vérification manuelle objective : l'état attendu est visible et photographié (capture datée, appareil identifié). |
| Absence de régression | Suite existante verte : `./gradlew testFdroidDebugUnitTest` + `./gradlew lint` (gate CI `android.yml` L45/L48). |
| Permission / manifeste | Sortie de commande : `aapt dump permissions <apk>` (par variant). |
| Sécurité réseau | Absence de requête dans le journal de la garde réseau, ou `SsrfGuard`/`ImageUrlPolicy` testés unitairement (résolveur injecté). |
| Microcopie | La clé existe dans les deux dossiers de ressources, et le lint `HardcodedText` passe. |

Un critère **non observé** est un critère **non satisfait**. Aucun point ne peut être déclaré « validé » par analogie avec un autre.

---

## 1. Explorer

### EX-1 — Accès à l'onglet Explorer (P0 · M · BIZ)
En tant que **Léa**, je veux trouver un onglet **Explorer** dans la barre de navigation afin de chercher une vidéo sans quitter B Music.

- **Étant donné** B Music installée en variante réseau, avec l'option « Explorer et Liens » activée
- **Quand** j'ouvre l'application
- **Alors** la barre de navigation contient une entrée « Explorer » avec une icône loupe
- **Alors** un tap sur cette entrée ouvre l'écran Explorer, champ de recherche vide, liste en état repos
- **Et** l'écran est atteignable au clavier matériel (focus atteignable dans l'ordre de tabulation)
- **Étant donné** B Music installée en variante `fdroid`
- **Quand** j'ouvre l'application
- **Alors** aucune entrée « Explorer » n'apparaît dans la barre de navigation
- **Et** le lancement ne produit aucune erreur

### EX-2 — Saisie d'une requête (P0 · S · BIZ)
En tant que **Léa**, je veux saisir une recherche dans un champ clair afin de lancer une recherche YouTube.

- **Étant donné** l'écran Explorer ouvert
- **Quand** je tape « jazz piano »
- **Alors** le champ contient « jazz piano » et la loupe est visible dans le champ
- **Alors** une icône « effacer la recherche » apparaît
- **Quand** j'appuie sur la touche **Entrée** du clavier matériel
- **Alors** la recherche est déclenchée
- **Et** la frappe seule ne déclenche pas de recherche tant que l'appui sur Entrée n'a pas eu lieu

### EX-3 — Validation de la requête (P0 · S · BIZ)
En tant que **Léa**, je veux un retour immédiat si ma recherche est impossible à traiter, afin de ne pas attendre un résultat qui ne viendra pas.

- **Étant donné** l'écran Explorer ouvert
- **Quand** je saisis une requête d'**un seul caractère**
- **Alors** un message « Saisissez au moins 2 caractères. » s'affiche sous le champ
- **Et** aucune requête n'est émise
- **Étant donné** l'écran Explorer ouvert
- **Quand** je saisis une requête de **plus de 200 caractères**
- **Alors** un message « 200 caractères maximum. » s'affiche sous le champ
- **Et** le texte saisi n'est pas tronqué silencieusement
- **Étant donné** l'écran Explorer ouvert
- **Quand** je saisis uniquement des espaces
- **Alors** la requête est traitée comme vide : aucun message d'erreur, aucune requête émise
- **Alors** le message d'erreur disparaît dès que la requête redevient valide

### EX-4 — Chargement des résultats (P0 · S · BIZ)
En tant que **Léa**, je veux voir que la recherche est en cours, afin de ne pas croire à un écran vide.

- **Étant donné** une requête valide
- **Quand** la recherche est émise
- **Alors** des squelettes de lignes s'affichent à la place de la liste
- **Et** le champ de recherche reste éditable pendant le chargement
- **Et** aucune liste de résultats antérieurs n'est affichée pendant le chargement

### EX-5 — Affichage des résultats (P0 · M · BIZ)
En tant que **Léa**, je veux voir la vignette, le titre et la chaîne de chaque vidéo, afin de choisir sans ouvrir chaque lien.

- **Étant donné** une recherche ayant renvoyé au moins un résultat
- **Quand** les résultats s'affichent
- **Alors** chaque ligne affiche une **miniature**, un **titre** (2 lignes maximum) et le **nom de la chaîne**
- **Alors** la durée est affichée quand elle est connue, et absente sinon
- **Alors** la liste remplace intégralement les squelettes
- **Alors** la miniature est décorative : la ligne entière porte une description TalkBack « Titre — chaîne, durée »
- **Étant donné** un résultat dont la miniature est absente ou non autorisée
- **Alors** une vignette locale de substitution s'affiche, sans recherche réseau supplémentaire

### EX-6 — Aucun résultat (P0 · S · BIZ)
En tant que **Léa**, je veux comprendre qu'aucune vidéo ne correspond, afin de reformuler ma recherche.

- **Étant donné** une recherche ayant renvoyé zéro résultat
- **Alors** un état vide « Aucun résultat » s'affiche, avec la suggestion de reformuler
- **Et** aucune liste vide ni écran blanc ne subsiste
- **Et** le nombre de résultats annoncés est « 0 résultat »

### EX-7 — Erreur réseau et indisponibilité du service (P0 · M · BIZ)
En tant que **Léa**, je veux savoir précisément ce qui a échoué et pouvoir réessayer, afin de ne jamais croire qu'une recherche a réussi.

- **Étant donné** l'appareil hors ligne
- **Quand** je lance une recherche
- **Alors** un message « Connexion impossible. Vérifiez votre réseau et réessayez. » s'affiche
- **Et** un bouton « Réessayer » est présent
- **Et** aucune liste de résultats n'est affichée
- **Étant donné** le service YouTube momentanément indisponible
- **Quand** je lance une recherche
- **Alors** un message « YouTube est momentanément indisponible. Réessayez plus tard. » s'affiche
- **Et** l'action « Ouvrir dans YouTube » est proposée en repli
- **Et** aucun terme technique (« ReCaptcha », « HTTP », « extractor ») n'apparaît dans le message
- **Étant donné** un état d'erreur affiché
- **Quand** je tape « Réessayer »
- **Alors** une nouvelle recherche est émise et l'état d'erreur est remplacé par l'état de chargement

### EX-8 — Pagination (P0 · M · BIZ)
En tant que **Léa**, je veux charger la suite des résultats sans perdre ce que j'ai, afin d'explorer au-delà de la première page.

- **Étant donné** une recherche dont la réponse indique une page suivante
- **Alors** une action « Voir plus » est affichée en fin de liste
- **Quand** je tape « Voir plus »
- **Alors** les résultats de la page suivante sont **ajoutés** à la liste existante
- **Et** les résultats précédents restent visibles et inchangés
- **Étant donné** une recherche dont la réponse n'indique pas de page suivante
- **Alors** l'action « Voir plus » est absente
- **Étant donné** un échec de chargement de la page suivante
- **Alors** un message s'affiche **sous** la liste
- **Et** les résultats déjà affichés ne sont pas effacés

### EX-9 — Fiche de détail d'un résultat (P0 · M · BIZ)
En tant que **Léa**, je veux voir les détails d'une vidéo avant de l'ouvrir, afin de vérifier que c'est la bonne.

- **Étant donné** une liste de résultats
- **Quand** je tape un résultat
- **Alors** une fiche s'affiche avec : miniature, titre, chaîne, durée si connue
- **Alors** les actions « Ouvrir dans YouTube » et « Copier le lien » sont disponibles
- **Quand** je ferme la fiche (retour ou geste)
- **Alors** la liste réapparaît dans son état exact (mêmes éléments, même position, même page chargée)
- **Et** aucune nouvelle recherche n'est émise

### EX-10 — Ouverture externe d'un résultat (P0 · S · BIZ)
En tant que **Léa**, je veux ouvrir la vidéo dans YouTube, afin de la regarder dans son application native.

- **Étant donné** une fiche de résultat
- **Quand** je tape « Ouvrir dans YouTube »
- **Alors** une intention `ACTION_VIEW` est émise vers l'URL de la vidéo
- **Alors** l'application YouTube s'ouvre si elle est installée, sinon le navigateur
- **Et** B Music ne propose **aucun** lecteur intégré : aucun WebView n'est instancié
- **Étant donné** une fiche de résultat
- **Quand** je tape « Copier le lien »
- **Alors** l'URL de la vidéo est présente dans le presse-papiers

### EX-11 — Historique de recherche (P1 · S · BIZ)
En tant que **Léa**, je veux retrouver mes recherches précédentes, afin de relancer une recherche déjà faite.

- **Étant donné** au moins une recherche déjà effectuée
- **Quand** j'ouvre l'écran Explorer et que le champ est vide
- **Alors** les 10 dernières requêtes sont proposées, de la plus récente à la plus ancienne
- **Et** une requête identique n'apparaît qu'une seule fois
- **Quand** je tape une entrée de l'historique
- **Alors** la recherche correspondante est relancée
- **Et** l'historique est stocké localement et effaçable

---

## 2. Liens

### LI-1 — Accès à l'onglet Liens (P0 · M · BIZ)
En tant que **Léa**, je veux un onglet **Liens** identifiable par une icône de chaîne, afin de retrouver un lien vidéo que j'ai reçu.

- **Étant donné** B Music en variante réseau, option « Explorer et Liens » activée
- **Quand** j'ouvre l'application
- **Alors** la barre de navigation contient une entrée « Liens » avec une icône de chaîne dont un maillon est ouvert
- **Alors** un tap ouvre l'écran Liens, champ d'ajout vide, liste vide
- **Étant donné** B Music en variante `fdroid`
- **Alors** aucune entrée « Liens » n'apparaît et le lancement reste sans erreur

### LI-2 — Ajout d'un ou plusieurs liens (P0 · M · BIZ)
En tant que **Léa**, je veux coller plusieurs liens d'un coup, un par ligne, afin de traiter un bloc de liens reçu.

- **Étant donné** l'écran Liens ouvert
- **Quand** je colle un bloc contenant **3** liens séparés par des retours à la ligne
- **Alors** **3** entrées sont créées, une par ligne
- **Et** les lignes vides sont ignorées
- **Étant donné** un bloc contenant 2 liens valides et 1 ligne de texte libre
- **Alors** 2 entrées sont créées et un compteur « 1 ligne ignorée » est affiché
- **Étant donné** un lien collé depuis le presse-papiers
- **Quand** je tape « Coller »
- **Alors** le contenu du presse-papiers est placé dans le champ
- **Alors** aucune action réseau n'est déclenchée par le collage seul

### LI-3 — Détection de la plateforme (P0 · M · BIZ)
En tant que **Claire**, je veux savoir de quelle plateforme vient un lien avant d'agir, afin de comprendre ce que B Music peut en faire.

- **Étant donné** l'ajout d'un lien `https://www.youtube.com/watch?v=…`
- **Alors** l'entrée porte l'étiquette « YouTube »
- **Étant donné** l'ajout d'un lien `https://www.tiktok.com/@x/video/123`
- **Alors** l'entrée porte l'étiquette « TikTok »
- **Étant donné** l'ajout d'un lien `https://www.instagram.com/reel/…`
- **Alors** l'entrée porte l'étiquette « Instagram »
- **Étant donné** l'ajout d'un lien `https://x.com/…/status/…`
- **Alors** l'entrée porte l'étiquette « X »
- **Étant donné** l'ajout d'un lien `https://twitter.com/…/status/…`
- **Alors** l'entrée porte l'étiquette « X » (les deux domaines sont équivalents)
- **Étant donné** l'ajout d'un lien `https://soundcloud.com/…`
- **Alors** l'entrée porte l'étiquette « SoundCloud »
- **Étant donné** l'ajout d'un lien vers un site inconnu
- **Alors** l'entrée porte l'étiquette « Plateforme non reconnue »
- **Et** aucune requête réseau n'est émise pour déterminer la plateforme d'un hôte inconnu

### LI-4 — Validation des liens (P0 · M · BIZ)
En tant que **Claire**, je veux que les liens invalides soient refusés avec une raison claire, afin que rien de dangereux n'atteigne le réseau.

- **Étant donné** un lien de plus de 2048 caractères
- **Alors** l'entrée est marquée « Lien non valide » et aucune requête réseau n'est émise
- **Étant donné** un lien en `ftp://`
- **Alors** l'entrée est marquée « Lien non valide » et aucune requête réseau n'est émise
- **Étant donné** un lien contenant un identifiant utilisateur (`https://user@exemple.fr/x`)
- **Alors** l'entrée est marquée « Lien non valide »
- **Étant donné** un lien contenant un fragment (`#`)
- **Alors** l'entrée est marquée « Lien non valide »
- **Étant donné** un lien contenant un espace interne
- **Alors** l'entrée est marquée « Lien non valide »
- **Étant donné** un lien vers une adresse IP privée (`http://192.168.1.1/x`)
- **Alors** la résolution est refusée par la garde anti-SSRF et l'entrée affiche un état d'indisponibilité
- **Étant donné** une URL valide
- **Alors** la validation est purement locale : aucun appel réseau n'est déclenché par la validation seule

### LI-5 — Déduplication (P0 · S · BIZ)
En tant que **Léa**, je veux éviter d'ajouter deux fois le même lien, afin de garder une liste lisible.

- **Étant donné** une liste contenant `https://www.youtube.com/watch?v=ABC`
- **Quand** j'ajoute `https://www.youtube.com/watch?v=ABC`
- **Alors** l'entrée est marquée « Déjà dans la liste » et n'est pas ajoutée une seconde fois
- **Étant donné** la même URL écrite avec une casse différente sur le schéma et l'hôte
- **Alors** elle est considérée comme un doublon
- **Étant donné** une entrée marquée doublon
- **Alors** l'action « Ajouter quand même » est proposée

### LI-6 — Liste des liens et suppression (P0 · M · BIZ)
En tant que **Léa**, je veux voir mes liens et pouvoir en retirer, afin de garder la liste à jour.

- **Étant donné** 3 liens ajoutés
- **Alors** la liste affiche 3 entrées, chacune avec son étiquette de plateforme et son état
- **Quand** je supprime une entrée
- **Alors** elle disparaît de la liste et les autres entrées ne bougent pas
- **Quand** je tape « Tout effacer »
- **Alors** une confirmation est demandée avant suppression
- **Et** aucune donnée n'est conservée après validation de la suppression

### LI-7 — Prévisualisation d'un lien pris en charge (P0 · M · BIZ)
En tant que **Claire**, je veux voir ce que contient un lien avant d'agir, afin de vérifier qu'il s'agit bien du contenu attendu.

- **Étant donné** une entrée YouTube prise en charge
- **Quand** je tape l'entrée
- **Alors** un état de chargement des informations s'affiche, puis une fiche avec titre, chaîne et durée
- **Alors** la miniature est chargée via le chargeur d'images dédié, sous réserve de la politique d'hôtes autorisés
- **Étant donné** un lien SoundCloud ou Bandcamp
- **Alors** la fiche s'affiche si l'hôte de miniature est autorisé
- **Et** sinon une icône locale de substitution s'affiche, sans message d'erreur
- **Étant donné** un lien pris en charge dont la résolution échoue
- **Alors** un message d'erreur actionnable s'affiche et l'entrée reste dans la liste

### LI-8 — Lien non pris en charge et ouverture externe (P0 · M · BIZ)
En tant que **Léa**, je veux être informée qu'une plateforme n'est pas lisible dans B Music, et pouvoir l'ouvrir quand même, afin de ne pas rester bloquée.

- **Étant donné** une entrée TikTok
- **Alors** l'état « TikTok n'est pas lisible dans B Music. » s'affiche avec l'action « Ouvrir à l'extérieur »
- **Et** **aucun écran vide** ne s'affiche
- **Quand** je tape « Ouvrir à l'extérieur »
- **Alors** une intention `ACTION_VIEW` est émise vers l'URL
- **Et** l'application de la plateforme s'ouvre si elle est installée, sinon le navigateur
- **Étant donné** un lien vers une plateforme non reconnue
- **Alors** l'état « Plateforme non reconnue. » s'affiche avec la même action externe
- **Alors** B Music n'affiche **jamais** de lecteur intégré vide à la place de la plateforme

### LI-9 — Contenus restreints (P1 · M · BIZ)
En tant que **Claire**, je veux distinguer un contenu privé d'une panne, afin de comprendre pourquoi un lien ne se lit pas.

- **Étant donné** un lien vers une vidéo privée
- **Alors** l'état « Contenu privé ou indisponible. » s'affiche
- **Et** aucune tentative de contournement n'est engagée
- **Étant donné** un lien vers un contenu soumis à restriction d'âge
- **Alors** l'état « Ce contenu est réservé à certains comptes. » s'affiche
- **Étant donné** un lien vers un contenu indisponible dans la région
- **Alors** l'état « Contenu indisponible dans votre région. » s'affiche
- **Dans tous les cas**, l'action « Ouvrir à l'extérieur » reste disponible

### LI-10 — Partage d'un lien vers B Music (P1 · M · BIZ)
En tant que **Léa**, je veux partager un lien vers B Music, afin de retrouver une vidéo plus tard.

- **Étant donné** une autre application partageant une URL
- **Quand** je choisis B Music dans le partage
- **Alors** l'onglet Liens s'ouvre avec le champ **pré-rempli** de l'URL
- **Et** aucune résolution ni aucun téléchargement n'est déclenché automatiquement
- **Étant donné** un partage contenant du texte et non une URL
- **Alors** le champ est pré-rempli du texte et l'utilisateur décide de l'action

---

## 3. Definition of Done commune

Une fonctionnalité est **terminée** quand les **12** points suivants sont vérifiés. Ils s'appliquent à l'ensemble du périmètre Explorer + Liens.

| # | Point de la DoD | Vérification attendue |
|---|---|---|
| **DO-1** | Le code est intégré dans l'architecture existante : pas de nouvelle couche, pas de bibliothèque ajoutée, pas de framework remplacé. | Revue de diff : uniquement des ajouts dans les packages existants (`ui/screen/…`, `downloader/…`, `data/model/network/…`) ; `libs.versions.toml` inchangé. |
| **DO-2** | Le design est validé par le designer et respecte le design system (opaque, sans verre, cibles ≥ 48 dp, states vide/chargement/erreur). | Validation `lean designer` + `bash scripts/check-glass-policy.sh` sans résultat + revue des 2 écrans. |
| **DO-3** | Les tests pertinents sont **exécutés** et verts : suite existante + tests nouveaux (validation d'URL, détection de plateforme, moteur de recherche via un double de test). | Sortie de `./gradlew testFdroidDebugUnitTest` et `./gradlew lint` (job `check` de `android.yml` L42/L45/L48). |
| **DO-4** | Aucun secret n'est exposé : ni clé, ni jeton, ni en dur, ni dans les journaux, ni dans le dépôt. | `git grep -inE 'AIza|api[_-]?key|secret|token'` sur les fichiers ajoutés → aucune valeur ; les requêtes utilisateur ne sont jamais journalisées en clair. |
| **DO-5** | La gestion des erreurs est complète : chaque état d'erreur de la matrice §6 de la spécification a un message, une action et un test. | Table de correspondance erreur → message → action, avec au moins un cas automatisé par famille d'erreur. |
| **DO-6** | L'accessibilité est vérifiée : cibles ≥ 48 dp, contraste AA, TalkBack annoncé sur les 2 parcours, états annoncés, aucune information portée par la seule animation. | Audit `lean designer` + lint `Compose` sans avertissement bloquant + parcours TalkBack exécuté. |
| **DO-7** | Les performances ne régressent pas : démarrage à froid, taille d'APK et mémoire dans les budgets existants ; les onglets ne sont pas instanciés au lancement. | Mesures Macrobenchmark existantes + taille de l'APK release ≤ budget existant ; aucun `factory` Koin transformée en `single` pour ces écrans. |
| **DO-8** | Le build est vérifié pour **chaque** variante livrée, et les permissions réseau correspondent à la promesse de chaque variante. | `aapt dump permissions` par variant : `fdroid` sans `INTERNET`/`ACCESS_NETWORK_STATE`, variante réseau avec. |
| **DO-9** | Les microcopies existent en FR et EN, sont centralisées en ressources et paramétrées, et le lint `HardcodedText` passe. | Clés présentes dans les deux dossiers de ressources ; lint sans avertissement `HardcodedText`. |
| **DO-10** | La documentation de promesse est mise à jour par variante (politique de confidentialité, fiche store, `SEC-1`, DoD #3). | Documents publiés et cohérents avec le comportement réel de chaque variante. |
| **DO-11** | La non-régression du cœur du produit est prouvée : bibliothèque, lecteur, file, playlists, favoris, recherche locale, écran *Télécharger* inchangés. | Suite de tests existante verte + recette manuelle des parcours `S1`→`S12` de `04-acceptation-lancement.md`. |
| **DO-12** | Le journal de décisions est à jour : toute divergence entre la spécification et le code implémenté est écrite dans `docs/architecture/TECHNICAL_DECISIONS.md`. | Décisions D1, D2, D5, D6, D7 tracées avec leur date et leur auteur. |

## 4. Récapitulatif

| Bloc | Stories | Critères Given/When/Then | Points de DoD communs |
|---|---|---|---|
| Explorer | EX-1 → EX-11 (10 MVP + 1 P1) | 90 | 12 |
| Liens | LI-1 → LI-10 (8 MVP + 2 P1) | 90 | — |
| **Total** | **21 stories** | **180 assertions** | **12** |
