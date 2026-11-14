# B Music — Notes de version 1.2.0 (brouillon)

> Agent release, REL-05. 14 novembre 2026.
> **Brouillon non publié.** Fondées **exclusivement** sur ce qui est présent dans le code à la date du 14 novembre 2026 (commits `41839ee8` → `c488d525`).
> ⚠️ L'artefact produit est encore en `versionName 1.0.0` / `versionCode 1` : le bump de version est un prérequis, pas un acquis.

---

## Nouveautés

### Deux onglets de navigation

- **Explorer** — un onglet de recherche de vidéos YouTube, avec champ de recherche, validation de la saisie (2 à 200 caractères), stabilisation de la saisie (400 ms), états de chargement / vide / erreur / réessai, et pagination additive (« Voir plus »).
- **Liens** — un onglet de dépôt de liens multi-plateformes : **zone de collage acceptant une ou plusieurs URL, une par ligne**, avec validation de chaque entrée, déduplication, retrait d'une entrée et « Tout effacer ».

### Détection de plateforme

Un registre central reconnaît **YouTube, TikTok, Facebook, Instagram, X, Snapchat**, ainsi que SoundCloud, Bandcamp, Vimeo et PeerTube. Chaque lien importé affiche sa plateforme, son état et les actions disponibles. Les liens courts (`vm.tiktok.com`, `vt.tiktok.com`, `fb.watch`) et l'équivalence `x.com` ≡ `twitter.com` sont reconnus.

**Ce qui est réellement lisible dans l'application** : YouTube, SoundCloud, Bandcamp, Vimeo et PeerTube — l'application en extrait les métadonnées (titre, auteur, miniature, durée).

**Ce qui ne l'est pas** : TikTok, Facebook, Instagram, X et Snapchat sont **reconnus mais pas lus** dans B Music. Un lien vers ces plateformes affiche « Ouverture externe uniquement » et propose de l'ouvrir dans l'application de la plateforme ou le navigateur. **Aucune lecture intégrée n'existe sur aucune plateforme** : tous les liens s'ouvrent à l'extérieur.

### Correction de navigation

La barre basse peut contenir **6 entrées au maximum** (limite de la bibliothèque Material). Avec les deux nouveaux onglets, le nombre de catégories de bibliothèque visibles simultanément dans la barre passe de 5 à **3**. L'ordre des catégories est inchangé et le réglage « catégories visibles » des préférences n'est pas modifié : les catégories écartées de la barre restent accessibles par le sélecteur de catégories.

---

## Améliorations

- **Icône « Liens »** créée (chaîne au maillon ouvert) ; l'icône « Explorer » reprend la loupe existante. Plus aucune icône Unicode comme icône d'interface.
- **Chargement par squelettes** au lieu d'un indicateur circulaire, conformément à la spécification produit.
- **Respect du réglage système « supprimer les animations »** : la durée de l'apparition des résultats passe à 0 quand `ANIMATOR_DURATION_SCALE` vaut 0.
- Les libellés de l'onglet « Télécharger » et les nouveaux textes sont **centralisés en ressources**, en français et en anglais.
- La durée de conservation de l'historique de recherche reste bornée à 10 entrées, dédupliquées et effaçables.

---

## Corrections

- **Faille SSRF sur le chemin « URL collée »** — le module de téléchargement suivait les redirections HTTP sans vérifier la destination, et le garde-fou anti-SSRF du transfert ignorait ses propres exceptions. Un lien collé pouvait donc provoquer une requête vers le réseau local de l'utilisateur (box, NAS, partage Wi-Fi) ou vers une adresse de métadonnées cloud. Corrigé : le résolveur DNS est désormais branché sur le client HTTP du module (l'adresse est validée **avant** connexion, et **chaque redirection** est revalidée), le garde-fou du transfert ne peut plus être ignoré, et les adresses loopback écrites sous une forme non canonique (`0:0:0:0:0:0:0:1`, `::127.0.0.1`) sont désormais refusées. **11 tests de non-régression** accompagnent le correctif.
- **Hôtes à caractères Unicode refusés** — une adresse contenant un « o » cyrillique (`yоutube.com`) était acceptée et s'affichait comme `youtube.com`. Les hôtes sont désormais limités à l'ASCII.
- **Plantage du module « Télécharger »** — la dépendance `Preferences` n'était déclarée dans aucun conteneur d'injection : la première action du module (ajout, pause ou annulation d'un téléchargement) interrompait l'application. Corrigé.
- **Fiabilité de la suite de tests** — l'injection de dépendances n'était pas arrêtée entre les tests : la suite complète pouvait échouer ou non selon l'ordre d'exécution. Corrigé ; la suite est désormais **déterministe**.

---

## Limitations connues

- **La recherche YouTube ne renvoie pas encore de résultats.** L'écran Explorer est complet (saisie, validation, états, pagination, squelettes), mais le moteur de recherche n'est pas encore activé : toute recherche répond « service temporairement indisponible » avec une action « Réessayer ». **Aucune vidéo ne peut donc être cherchée dans cette version.**
- **Aucune lecture intégrée.** Aucun lecteur vidéo n'est embarqué : tous les liens s'ouvrent dans l'application de la plateforme ou le navigateur. L'application ne lit pas les vidéos.
- **Métadonnées indisponibles sur 4 plateformes.** Pour TikTok, Facebook, Instagram et X, les sources officielles accessibles sans clé ne fournissent plus ni titre, ni miniature, ni auteur : la fiche affiche la plateforme et le lien, sans inventer d'information.
- **Miniatures absentes pour 4 plateformes.** SoundCloud, Bandcamp, Vimeo et PeerTube sont détectés et lisibles, mais leurs pochettes sont bloquées par la liste blanche d'images : une vignette de substitution locale s'affiche à la place.
- **Deux catégories de bibliothèque en moins dans la barre** (voir « Améliorations »). La compensation par une grille dans l'écran Bibliothèque n'est pas encore en place.
- L'onglet « Télécharger » reste masqué tant que l'option correspondante n'est pas activée dans les préférences.
- L'application n'envoie **aucune** donnée d'usage, de télémétrie ou de rapport d'incident : ni identifiant, ni analyse, ni crash-reporting distant.

---

## Prérequis de configuration

Cette version ne peut pas être publiée tant que les éléments suivants ne sont pas faits :

1. **Bump de version** : `versionName` → `1.2.0` et `versionCode` → une valeur strictement supérieure à celle de la version précédente.
2. **Signature release** : une clé de signature dédiée, stockée hors du dépôt (secret CI ou gestionnaire de secrets). En l'absence de clé, l'artefact produit est **signé avec la clé de debug**, qui n'est pas une clé de distribution.
3. **CI dans le dépôt de livraison** : le dépôt qui porte la release n'a aujourd'hui aucun workflow.
4. **Analyse de secrets** (`gitleaks`) et **analyse de dépendances** avant toute publication.
5. **Retrait de la permission réseau du variant hors-ligne** : le variant « fdroid » embarque aujourd'hui la permission `INTERNET` alors que son contrat est « zéro réseau ».
6. **Secrets Last.fm** : à renseigner uniquement pour les variants qui contactent Last.fm, et le secret partagé ne doit pas être embarqué dans l'application.

---

## Problèmes non résolus

- Le moteur de recherche YouTube (mécanisme officiel avec quota de 100 recherches par jour et par projet, ou bibliothèque tierce non officielle) et le choix d'un éventuel lecteur intégré **ne sont pas arbitrés**.
- Le **secret partagé Last.fm** reste embarqué dans les variants qui l'utilisent.
- Une **clé d'API Last.fm** subsiste dans l'historique Git du projet amont.
- Les **actions de la chaîne d'intégration continue** ne sont pas épinglées par empreinte de commit.
- Les **journaux de débogage** ne sont pas retirés du build release.
- La **restauration d'une sauvegarde importée** n'a pas fait l'objet d'un audit de sécurité.
- Le **module de mesure de performance** (`macrobenchmark`) est incompatible avec la version de l'outil de build : il empêche l'exécution de la commande de release documentée tant qu'il n'est pas corrigé.
- Le produit n'existe que dans une copie de travail locale : le dépôt de livraison ne contient pas encore l'application.
- Les **docs de promesse** (« aucune autorisation réseau n'est utilisée ») doivent être nuancées par variant avant publication.

---

## Signature et authenticité

Les empreintes de la clé de signature release et le fichier `SHA256SUMS.txt` seront joints à la publication. Ils **ne figurent pas dans ce brouillon** : la clé n'existe pas encore, et l'empreinte de la clé de debug qui signerait l'artefact actuel ne doit pas être publiée comme une empreinte de release.
