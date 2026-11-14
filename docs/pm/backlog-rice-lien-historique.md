# B Music — Backlog RICE : sites à améliorer et pistes d'évolution (v1.2)

> Agent Product Manager B Music. Date : 14 novembre 2026.
> Méthode : RICE = Reach (portée) × Impact × Confiance / Effort.
> Échelle d'effort : S ≤ 1 j · M 2–5 j · L ≥ 5 j.
> Source des données : matrice de tests remplie par l'utilisateur (docs/pm/recette-lien-historique.md) et stories de `docs/pm/07-criteres-lien.md` et `08-criteres-historique.md`.
> Règle : aucun site n'est déclaré « fonctionnel » sans le résultat utilisateur. Les abandons sont justifiés par l'impossibilité technique moteur, pas par une supposition.

---

## 1. Matrice de sites — évaluation à remplir après recette

| Site | Reach (1-5) | Impact (1-5) | Confiance (1-3) | Effort | Score RICE | Statut après recette |
|------|-------------|--------------|-----------------|--------|------------|----------------------|
| YouTube | 5 | 5 | 3 | S | 25 | OK / partiel / abandonné |
| TikTok | 4 | 4 | 3 | M | 10.7 | OK / partiel / abandonné |
| Instagram | 4 | 3 | 3 | M | 8 | OK / partiel / abandonné |
| Facebook | 3 | 3 | 3 | M | 6 | OK / partiel / abandonné |
| X | 3 | 3 | 3 | M | 6 | OK / partiel / abandonné |
| Snapchat | 2 | 3 | 2 | M | 4 | OK / partiel / abandonné |
| SoundCloud | 3 | 4 | 3 | M | 8 | OK / partiel / abandonné |
| Bandcamp | 2 | 3 | 3 | M | 4 | OK / partiel / abandonné |
| Vimeo | 2 | 3 | 3 | M | 4 | OK / partiel / abandonné |
| Dailymotion | 2 | 2 | 3 | M | 2.7 | OK / partiel / abandonné |
| Reddit | 2 | 2 | 2 | M | 1.3 | OK / partiel / abandonné |
| Twitch | 2 | 2 | 2 | M | 1.3 | OK / partiel / abandonné |

> **Comment remplir** : après avoir reçu la matrice complétée par l'utilisateur, remplacer chaque cellule « OK / partiel / abandonné » par le résultat réel. Un site marqué « abandonné » ne fait plus l'objet d'aucun effort tant que son moteur n'est pas capable de l'extraire.

---

## 2. Sites à améliorer (après recette)

L'ordre de priorité suit le score RICE décroissant, mais uniquement pour les sites dont le résultat utilisateur est « OK » ou « partiel ».

| Priorité | Site | Action recommandée | Justification |
|----------|------|-------------------|---------------|
| 1 | YouTube | Améliorer la robustesse de l'extraction (changement fréquent de structure HTML) | Plus fort Reach et Impact |
| 2 | SoundCloud | Améliorer la qualité de l'aperçu (pochette, durée) | Bonne note Impact, public musique |
| 3 | TikTok | Gérer les liens raccourcis et mobiles | Fort Reach chez les jeunes utilisateurs |
| 4 | Instagram / Facebook | Ajouter le support des Reels et Reels partagés | Reach moyen, Impact moyen |
| 5 | X | Support des vidéos embarquées | Reach moyen, Impact moyen |
| 6 | Bandcamp / Vimeo / Dailymotion | Support basique (aperçu + lien externe) | Reach faible, effort faible |
| 7 | Reddit / Twitch / Snapchat | Support minimal ou abandonné selon résultat | Reach faible, effort élevé pour peu de valeur |

**Critère d'abandon** : si, après recette, un site affiche systématiquement « Site non pris en charge » ou « Contenu protégé » sans possibilité d'aperçu, il est marqué **abandonné** tant que le moteur d'extraction ne permet pas son traitement. Aucun effort n'est alloué à un site abandonné.

---

## 3. Fonctionnalités hors périmètre (décision d'aujourd'hui)

Ces fonctionnalités sont **identifiées mais exclues du périmètre actuel** (v1.2). Elles peuvent être réévaluées pour v1.3+.

| Fonctionnalité | Raison d'exclusion | Statut | Possibilité v1.3+ |
|---------------|-------------------|--------|-------------------|
| **Recherche YouTube intégrée** | Décision D1 : pas de clé API, pas d'API officielle souhaitée. Le moteur NewPipe ne supporte pas `search()`. | Hors périmètre | Possible si une clé API est fournie par l'utilisateur et intégrée proprement |
| **Conversion MP3** | Nécessite un encodeur audio tiers (FFmpeg ou similaire), ajout de dépendance lourde, licences à vérifier. | Hors périmètre | Possible avec une dépendance légère et une licence compatible |
| **Aperçu en streaming** | Nécessite un lecteur intégré (WebView ou player externe), contredit la règle « pas de lecteur intégré ». | Hors périmètre | Non envisageable sans changement d'architecture |
| **Export de l'historique** (CSV/JSON) | Fonctionnalité de confort, pas bloquante pour v1.2. | Hors périmètre | Possible rapidement (S) pour v1.3 |
| **Téléchargement par lot (file d'attente manuelle)** | Déjà couvert par le « Tout télécharger » des plusieurs liens. | Déjà couvert | — |

---

## 4. Fonctionnalités abandonnées (moteur ne le permet pas)

À remplir après recette, uniquement si l'utilisateur rapporte qu'un site est systématiquement refusé par le moteur.

| Fonctionnalité / Site | Raison d'abandon | Preuve (résultat utilisateur) |
|---|---|---|
| — | — | — |

> Cette section reste vide tant qu'aucun résultat utilisateur ne confirme l'impossibilité technique d'un site. Aucun site n'est abandonné par supposition.

---

## 5. Backlog priorisé (après recette)

| ID | Élément | Type | Reach | Impact | Confiance | Effort | Score RICE | Justification |
|----|---------|------|-------|--------|-----------|--------|------------|---------------|
| B-01 | Robustesse YouTube (changement structure) | Amélioration | 5 | 5 | 3 | M | 12.5 | Site le plus utilisé, moteur le plus sollicité |
| B-02 | Support SoundCloud aperçu | Amélioration | 3 | 4 | 3 | S | 12 | Public musique, effort faible |
| B-03 | Support TikTok liens raccourcis/mobiles | Amélioration | 4 | 4 | 3 | M | 8 | Fort Reach, patterns d'URL variés |
| B-04 | Export historique (CSV/JSON) | Nouvelle fonctionnalité | 2 | 3 | 3 | S | 4 | Confort utilisateur, effort faible |
| B-05 | Support Instagram Reels | Amélioration | 4 | 3 | 3 | M | 4 | Reach moyen, structure complexe |
| B-06 | Support Facebook vidéos | Amélioration | 3 | 3 | 3 | M | 3 | Reach moyen, authentification possible |
| B-07 | Support X vidéos | Amélioration | 3 | 3 | 3 | M | 3 | Reach moyen, domaine x.com + twitter.com |
| B-08 | Support Bandcamp / Vimeo / Dailymotion | Amélioration | 2 | 3 | 3 | M | 2 | Reach faible, effort modéré |
| B-09 | Support Reddit / Twitch / Snapchat | Amélioration ou abandon | 2 | 2 | 2 | M | 0.9 | Reach faible, effort élevé |

> Le backlog est **vide tant que la recette n'a pas été remplie**. Les scores ci-dessus sont des estimations provisoires basées sur la portée supposée, pas sur des résultats utilisateur réels.

---

## 6. Règles de décision

| Situation | Décision |
|-----------|----------|
| Site « OK » à la recette | Maintenir le support, prioriser les améliorations selon RICE |
| Site « partiel » | Améliorer ce qui manque (aperçu, format, durée) selon RICE |
| Site « refus » systématique | Marquer « abandonné », ne plus allouer d'effort tant que le moteur ne permet pas |
| Site « non testé » | Ne pas inclure dans le backlog priorisé, attendre le résultat utilisateur |
| Recherche YouTube | Reste hors périmètre (décision D1) tant qu'aucune clé API n'est fournie |
| Conversion MP3 | Hors périmètre (dépendance lourde, licence) |
| Aperçu streaming | Hors périmètre (contredit « pas de lecteur intégré ») |

---

## 7. Ce qui est abandonné par conception (pas par résultat utilisateur)

| Élément | Raison |
|---------|--------|
| Recherche YouTube intégrée | Décision D1 : pas de clé API, pas d'API officielle, moteur NewPipe sans `search()` |
| Lecteur intégré / WebView | Règle produit : B Music n'instancie aucun lecteur intégré |
| Contournement de DRM / protection | Règle produit : contenus publics uniquement, aucun compte, aucun cookie |
| Clé API en dur dans le dépôt | Règle de sécurité : jamais de clé commitée |
