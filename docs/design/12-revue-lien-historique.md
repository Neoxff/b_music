# B Music — Revue d'implémentation : onglets « Lien » et « Historique » (v1.2)

> Agent DESIGNER — REVUE D'IMPLÉMENTATION (lecture seule, aucun code modifié).
> Date : 14 novembre 2026.
> Référence : `docs/design/11-onglets-lien-historique.md` (spécifications).
> Méthode : comparaison de l'implémentation aux spécifications à partir des captures d'écran de tests (Paparazzi/Roborazzi) et des captures fournies par l'utilisateur.
> Règle : signale les écarts uniquement, sans modifier le code. Liste les captures manquantes.

---

## 0. Méthode de revue

Chaque critère de la spécification §10 de `11-onglets-lien-historique.md` est vérifié sur :

| Condition | Combinaisons testées |
|---|---|
| Thème | Clair · Sombre |
| Taille d'écran | Téléphone (360 dp) · Tablette (720 dp) |
| Orientation | Portrait · Paysage |
| Clavier | Fermé · Ouvert |

Les captures sont organisées par état d'écran (vide, saisie, validation, extraction, prêt, erreur, etc.) et par onglet (Lien, Historique).

---

## 1. Critères prioritaires (à vérifier en premier)

### 1.1 Centrage de la zone de saisie (Lien)

| Critère | Spécification | Statut | Preuve |
|---|---|---|---|
| Centrage horizontal | Le bloc de saisie est centré horizontalement, marge 20 dp respectées de chaque côté | ❓ Non vérifié | Capture manquante |
| Centrage vertical | Le bloc est centré verticalement dans l'espace disponible | ❓ Non vérifié | Capture manquante |
| Largeur max 560 dp | Le bloc ne dépasse jamais 560 dp de largeur | ❓ Non vérifié | Capture manquante |
| Mesure au dp près | Vérification pixel-perfect sur chaque capture | ❓ Non vérifié | Capture manquante |

**Captures requises pour valider D-8** :
- Lien vide, téléphone, portrait, thème sombre, clavier fermé
- Lien vide, téléphone, portrait, thème clair, clavier fermé
- Lien vide, tablette, portrait, thème sombre, clavier fermé
- Lien vide, tablette, portrait, thème clair, clavier fermé
- Lien vide, téléphone, paysage, thème sombre, clavier fermé
- Lien vide, téléphone, portrait, thème sombre, clavier ouvert
- Lien vide, téléphone, portrait, thème clair, clavier ouvert

### 1.2 Remontée en ressort de la zone de saisie

| Critère | Spécification | Statut | Preuve |
|---|---|---|---|
| Déclenchement | Après analyse réussie, le bloc remonte vers le haut | ❓ Non vérifié | Capture manquante |
| Courbe | `SpringSnappy` (0,55/700), 220 ms | ❓ Non vérifié | Capture manquante |
| Position finale | Le bloc est en haut de l'écran, la carte d'aperçu apparaît dessous | ❓ Non vérifié | Capture manquante |

**Captures requises** :
- Lien après analyse, téléphone, portrait, thème sombre
- Lien après analyse, téléphone, portrait, thème clair

### 1.3 États d'erreur (Lien)

| État | Spécification | Statut | Preuve |
|---|---|---|---|
| Lien invalide | Message en `error` + bouton « Réessayer » | ❓ Non vérifié | Capture manquante |
| Site non pris en charge | Message + bouton « Voir les sites » | ❓ Non vérifié | Capture manquante |
| Connexion requise | Message + bouton « Réessayer » | ❓ Non vérifié | Capture manquante |
| Contenu protégé | Message sans action | ❓ Non vérifié | Capture manquante |
| Moteur à mettre à jour | Message + bouton « Ouvrir le journal » | ❓ Non vérifié | Capture manquante |
| Hors ligne | Message + bouton « Réessayer » | ❓ Non vérifié | Capture manquante |

**Captures requises** : un par état, téléphone, portrait, thème sombre.

### 1.4 Lisibilité de l'historique

| Critère | Spécification | Statut | Preuve |
|---|---|---|---|
| Miniature 96×54 dp | Rayon 12 dp, format 16:9 | ❓ Non vérifié | Capture manquante |
| Titre 2 lignes max | `titleMedium`, `ellipsis` | ❓ Non vérifié | Capture manquante |
| Métadonnées lisibles | Site + format + taille sur 1 ligne | ❓ Non vérifié | Capture manquante |
| État de ligne | Badge texte + point de couleur | ❓ Non vérifié | Capture manquante |
| Actions par menu | Appui long ou `more_vert` | ❓ Non vérifié | Capture manquante |

**Captures requises** : Historique, téléphone, portrait, thème sombre, plusieurs lignes avec états variés (terminé, en cours, erreur, fichier supprimé).

### 1.5 Icônes cohérentes

| Icône | Spécification | Statut | Preuve |
|---|---|---|---|
| `ic_link` | Deux anneaux, ouverture visible à 24 dp | ❓ Non vérifié | Capture manquante |
| `ic_history` | Horloge lisible à 24 dp | ❓ Non vérifié | Capture manquante |
| `ic_site_generic` | Monogramme générique, pas de logo de marque | ❓ Non vérifié | Capture manquante |

**Captures requises** : onglet Lien (icône de barre) et onglet Historique (icône de barre), téléphone, portrait, thème sombre + clair.

### 1.6 Contraste AA

| Élément | Contraste requis | Statut | Preuve |
|---|---|---|---|
| Texte primaire sur surface | ≥ 4,5:1 | ❓ Non vérifié | `contrast_check.py` |
| Texte secondaire sur surface | ≥ 4,5:1 | ❓ Non vérifié | `contrast_check.py` |
| Texte sur accent | ≥ 4,5:1 | ❓ Non vérifié | `contrast_check.py` |
| UI (boutons, séparateurs) | ≥ 3:1 | ❓ Non vérifié | `contrast_check.py` |

---

## 2. Tableau d'écarts par gravité

| ID | Gravité | Écart | Fichier/ligne | Capture manquante | Action |
|---|---|---|---|---|---|
| E-01 | Bloquant | Zone de saisie non centrée (à confirmer) | `11-…` §3.1 | Toutes les captures « Lien vide » | Mesurer au dp près |
| E-02 | Bloquant | Remontée en ressort absente ou incorrecte | `11-…` §3.3 | Captures « Après analyse » | Vérifier courbe SpringSnappy |
| E-03 | Bloquant | État d'erreur sans action | `11-…` §3.2 | Captures de chaque état d'erreur | Ajouter bouton d'action |
| E-04 | Majeur | Contraste < 4,5:1 sur un état | `11-…` §1.6 | Résultat de `contrast_check.py` | Ajuster couleur |
| E-05 | Majeur | Icône `ic_link` illisible à 24 dp | `11-…` §2 | Capture icône de barre | Redessiner l'ouverture |
| E-06 | Mineur | Animation du rayon de flou détectée | `11-…` §8 | — | Supprimer (interdit) |
| E-07 | Mineur | Aspect vitreux (`copy(alpha)` décoratif) | `11-…` §0 | Résultat de `check-glass-policy.sh` | Supprimer |
| E-08 | Mineur | Texte en dur (pas de `strings.xml`) | `11-…` §9 | `grep` sur les écrans | Externaliser |

> **Note** : Ce tableau est vide jusqu'à ce que les captures soient examinées. Chaque ligne ci-dessus représente un écart *potentiel* à confirmer.

---

## 3. Captures manquantes

La liste suivante recense toutes les captures requises pour compléter la revue. **Aucune n'est disponible à ce stade** — elles doivent être produites par Paparazzi/Roborazzi ou fournies par l'utilisateur.

### 3.1 Onglet Lien — état vide

| # | Capture | Thème | Taille | Orientation | Clavier |
|---|---------|-------|--------|-------------|---------|
| C-01 | Lien vide | Sombre | Téléphone | Portrait | Fermé |
| C-02 | Lien vide | Clair | Téléphone | Portrait | Fermé |
| C-03 | Lien vide | Sombre | Tablette | Portrait | Fermé |
| C-04 | Lien vide | Clair | Tablette | Portrait | Fermé |
| C-05 | Lien vide | Sombre | Téléphone | Paysage | Fermé |
| C-06 | Lien vide | Sombre | Téléphone | Portrait | Ouvert |
| C-07 | Lien vide | Clair | Téléphone | Portrait | Ouvert |

### 3.2 Onglet Lien — après analyse

| # | Capture | Thème | Taille | Orientation |
|---|---------|-------|--------|-------------|
| C-08 | Lien après analyse | Sombre | Téléphone | Portrait |
| C-09 | Lien après analyse | Clair | Téléphone | Portrait |

### 3.3 Onglet Lien — états d'erreur

| # | Capture | État | Thème |
|---|---------|------|-------|
| C-10 | Lien invalide | Sombre | Téléphone |
| C-11 | Site non pris en charge | Sombre | Téléphone |
| C-12 | Connexion requise | Sombre | Téléphone |
| C-13 | Contenu protégé | Sombre | Téléphone |
| C-14 | Moteur à mettre à jour | Sombre | Téléphone |
| C-15 | Hors ligne | Sombre | Téléphone |

### 3.4 Onglet Historique

| # | Capture | Thème | Taille | Orientation | États de ligne |
|---|---------|-------|--------|-------------|----------------|
| C-16 | Historique rempli | Sombre | Téléphone | Portrait | Terminé, En cours, Erreur, Fichier supprimé |
| C-17 | Historique rempli | Clair | Téléphone | Portrait | Terminé, En cours, Erreur, Fichier supprimé |
| C-18 | Historique vide | Sombre | Téléphone | Portrait | — |
| C-19 | Historique vide | Clair | Téléphone | Portrait | — |

### 3.5 Icônes de barre

| # | Capture | Icône | Thème | Taille |
|---|---------|-------|-------|--------|
| C-20 | `ic_link` active | Sombre | Téléphone | 24 dp |
| C-21 | `ic_link` inactive | Sombre | Téléphone | 24 dp |
| C-22 | `ic_history` active | Sombre | Téléphone | 24 dp |
| C-23 | `ic_history` inactive | Sombre | Téléphone | 24 dp |
| C-24 | `ic_link` active | Clair | Téléphone | 24 dp |
| C-25 | `ic_history` active | Clair | Téléphone | 24 dp |

---

## 4. Captures disponibles (à remplir par l'utilisateur)

| # | Capture | Fournie par | Chemin | Commentaire |
|---|---------|-------------|--------|-------------|
| — | — | — | — | Aucune capture fournie à ce stade |

> L'utilisateur doit fournir les captures d'écran de tests (Paparazzi/Roborazzi) ou des captures réelles de l'application pour que cette revue puisse être complétée.

---

## 5. Écrans existants inchangés

| Écran | Statut | Preuve |
|---|---|---|
| Accueil | Inchangé | Aucune modification signalée |
| Lecteur | Inchangé | `playback/` non touché (règle DESIGN) |
| Bibliothèque | Inchangée | Aucune modification signalée |
| File d'attente | Inchangée | Aucune modification signalée |
| Réglages | Inchangée | Aucune modification signalée |
| À propos | Inchangée | Aucune modification signalée |

**Vérification requise** : confirmer qu'aucun écran existant n'a été modifié par l'implémentation des onglets Lien/Historique.

---

## 6. Critères de conformité — résumé

| ID | Critère | Statut | Preuve |
|---|---------|--------|--------|
| D-1 | Aucun `copy(alpha)` décoratif / `RenderEffect.blur` | ❓ | `grep` sur les écrans downloader |
| D-2 | Politique verre CI passe | ❓ | `scripts/check-glass-policy.sh` |
| D-3 | Aucun accent double par vue | ❓ | Revue + captures |
| D-4 | Aucun `fontSize` en dur | ❓ | `grep fontSize` |
| D-5 | Aucune chaîne en dur | ❓ | `grep Text("` |
| D-6 | Cibles ≥ 48 dp | ❓ | Audit `IconButton` |
| D-7 | Mouvement réduit = 0 ms | ❓ | Test JVM |
| D-8 | Bloc de saisie centré | ❓ | Captures C-01 à C-07 |
| D-9 | Contraste AA | ❓ | `contrast_check.py` |

---

## 7. Conclusion de la revue

**Statut global** : ❓ **En attente de captures**

La revue ne peut pas être complétée tant que les captures d'écran (C-01 à C-25) ne sont pas produites. Aucun écart n'est confirmé à ce stade ; tous les critères D-1 à D-9 sont marqués « non vérifié ».

**Prochaine étape** : l'utilisateur fournit les captures d'écran de tests (Paparazzi/Roborazzi) ou des captures réelles de l'application sur téléphone. Cette revue sera alors complétée avec le tableau d'écarts final.

---

## 8. Fichiers associés

| Fichier | Rôle |
|---|---|
| `docs/design/11-onglets-lien-historique.md` | Spécifications de design (référence) |
| `docs/design/01-design-system.md` | Système de design |
| `docs/design/12-tokens-composants-et-motion.md` | Tokens et mouvement |
| `bmusic/app/src/main/java/com/bmusic/app/ui/theme/Motion.kt` | Tokens de mouvement implémentés |

---

*Revue produite par l'agent DESIGNER. Aucun code n'est modifié par ce document. Les écarts sont signalés uniquement, sans modification du code.*
