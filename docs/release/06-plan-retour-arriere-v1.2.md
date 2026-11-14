# B Music — Plan de retour arrière (v1.2)

> Agent release, REL-06. 14 novembre 2026.
> **Aucune commande de ce document n'a été exécutée.** Les sections 4 et 5 listent des commandes **destructives ou sensibles** : chacune est accompagnée de son effet exact et **exige une validation explicite** avant exécution.
> État actuel : **aucune version de B Music n'a été publiée** (aucun tag, aucune release GitHub, aucun APK signé installé par un tiers). Le retour arrière ne porte donc **que** sur du code et des artefacts locaux.

---

## 1. Référence

| Objet | Référence | Preuve |
|---|---|---|
| Produit (code Kotlin) | `bmusic/` branche `fix/stabilite-1.2`, HEAD **`c488d525`** | `git -C bmusic log --oneline -1` |
| Dernier état connu bon | **`2377fe02`** (avant la phase sécurité) | `git -C bmusic log --oneline` |
| Dépôt de livraison | `Neoxff/b_music`, `main` @ **`ec13cc6`** | `git log --oneline -1` |
| Artefacts produits | `app/build/outputs/apk/fdroid/release/B-Music.apk` (15 448 298 o), `app/build/outputs/bundle/fdroidRelease/B-Music.aab` (24 400 163 o), `app/build/outputs/mapping/fdroidRelease/mapping.txt` (128 409 693 o) | `02-audit…` §2.3 |
| Version de schéma de base | `BMusicDatabase.VERSION = 10` | `core/BoomingDatabase.kt` L61 |

## 2. Trois scénarios

| # | Scénario | Retour arrière possible ? | Procédure |
|---|---|---|---|
| **A** | Rejet **avant** toute publication (état actuel) | ✅ oui, sans risque | §3 |
| **B** | Rejet **après** tag, **avant** diffusion publique | ✅ oui | §3 + §4 (retirer le tag) |
| **C** | Rejet **après** installation par des utilisateurs | ⚠️ **partiellement** — contrainte de base de données | §3 + §5 |

## 3. Procédure de restauration du code (non destructive pour l'historique)

**Recommandée** : un `git revert` par commit. L'historique reste traçable, et le commit de restauration est lui-même auditable.

```bash
# 1. Identifier le commit fautif
git -C bmusic log --oneline -5

# 2. Créer une branche de correction (rien n'est modifié sur la branche principale)
git -C bmusic switch -c fix/rollback-v1.2

# 3. Reverter le(s) commit(s) fautif(s), dans l'ordre inverse
git -C bmusic revert --no-commit <sha-1> <sha-2>
git -C bmusic commit -m "revert: annule <sujet> (<sha>)"

# 4. Contrôler (§6) AVANT de fusionner
```

**Si le fautif est le commit de migration (L1)**, qui touche beaucoup de fichiers, préférer une **branche de restauration** plutôt qu'un revert :

```bash
git -C bmusic switch -c restore/v1.2-pre-l1 <sha-de-référence>
```

## 4. Commandes sensibles ou destructives — **effet exact et validation demandée**

> Aucune de ces commandes n'est exécutée par l'agent release. Chacune **supprime des informations** : l'effet est décrit pour que la validation soit informée.

| # | Commande | Effet exact | Irréversible ? | Validation demandée |
|---|---|---|---|---|
| D1 | `git reset --hard <sha>` | **Écrase** l'index, l'arbre de travail et l'index de la branche : **tout commit non poussé de la branche courante est perdu**. Les fichiers non suivis ne sont pas touchés. | Oui (commits non poussés) | **Oui** — n'exécuter qu'après `git log --oneline` + `git stash list` pour vérifier qu'aucun travail n'est perdu |
| D2 | `git clean -fd` | **Supprime définitivement** tous les fichiers non suivis (non ignorés) : brouillons, notes, artefacts. Les fichiers ignorés (`.gitignore`) sont conservés sauf `-x`. | Oui | **Oui** — lister d'abord `git clean -nd` (simulation) et vérifier la liste |
| D3 | `rm -rf app/build` | **Supprime** tous les artefacts de build (APK, AAB, `mapping.txt`, classes). Rebuildable, mais le `mapping.txt` produit ne sera **pas** reproductible bit à bit. | Non (rebuildable) | **Non**, mais-copy des artefacts à conserver d'abord |
| D4 | `git tag -d v1.2.0` | Supprime le tag **local**. Le tag distant reste inchangé. | Non (recreatable) | **Oui** si le tag a déjà été poussé |
| D5 | `git push --force origin <branche>` | **Réécrit** l'historique du dépôt distant : toute personne ayant cloné cette branche devra resynchroniser. | Oui (pour les autres) | **Oui** — toujours préférer `--force-with-lease` |
| D6 | `git push --delete origin v1.2.0` | Supprime le tag distant. Les artefacts déjà téléchargés par des utilisateurs **restent valables** et leur clé de signature change si l'on regénère la clé. | Oui | **Oui** |
| D7 | `rm -rf ~/.android/debug.keystore` | Supprime la clé de debug locale : **tous les APK signés avec cette clé** deviennent non installables en mise à jour. | Oui | **Oui** — et **ne pas** le faire : la clé est déjà utilisée par l'artefact produit |

**Règle** : D1, D2, D5, D6 et D7 exigent une validation explicite, dans cet ordre, avant exécution. D3 et D4 peuvent être exécutés après simple information.

## 5. Contraintes de migration — le point qui peut rendre un rollback partiel

| Constat | Preuve | Conséquence |
|---|---|---|
| Schéma de base **v10** | `BoomingDatabase.kt` L61 `const val VERSION = 10` | Une installation having appliqué la migration 9→10 ne peut pas rouvrir une version antérieure. |
| **Aucune rétrogradation** | `grep -c "MIGRATION_10_9\|downgrade\|fallbackToDestructiveMigration"` → **0** | Room lève `IllegalStateException` (« A migration from 10 to 9 was required but not found ») : l'application **plante au démarrage**. |
| `10.json` **non suivi par git** | `git ls-files app/schemas` → seul `9.json` est suivi ; `10.json` est un fichier non suivi | Une reconstruction du dépôt depuis les sources **perd le schéma v10** : la migration 9→10 échoue à l'exécution. **À committer (lot L0).** |

**Règle de rollback** :

- **Scénario A/B** (aucune installation par un tiers) : rollback complet, aucun risque de base.
- **Scénario C** (des utilisateurs ont installé) :
  - soit la version restaurée a le **même `versionCode` de schéma** → aucun problème ;
  - soit elle est antérieure ⇒ le rollback **exige** soit une migration descendante ajoutée et testée, soit une réinstallation avec effacement des données de l'application. **Aucune des deux n'existe aujourd'hui.**

**Conclusion à retenir** : tant que `versionCode` de schéma et `versionCode` applicatif évoluent ensemble dans le même commit, le risque de rollback est nul. **Tout changement de schéma de base doit être livré avec sa rétrogradation ou.Render son impact explicitement.**

## 6. Configurations à préserver (ne jamais supprimer ni versionner)

| Élément | Emplacement | Rôle | Conséquence de sa perte |
|---|---|---|---|
| `local.properties` | racine du produit, **ignoré** (`.gitignore:78`) | `sdk.dir` | build impossible sur cette machine |
| `keystore.properties` | racine du produit, **ignoré** (`.gitignore:22`) | alias / mots de passe / fichier de la clé release | **APK non signable** avec la bonne clé |
| `KEYSTORE_FILE`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD` | environnement | idem, en CI | idem |
| `LASTFM_API_KEY`, `LASTFM_SECRET` | environnement | flavors `github` / `playstore` | scrobble et métadonnées Last.fm indisponibles |
| `public.properties` | **suivi par git** | URLs de distribution par flavor | — |
| `gradle.properties` | **suivi par git** | `configureondemand`, cache, parallélisme | — |
| `metadata/**` | **suivi par git** | textes F-Droid + `changelogs/<versionCode>.txt` | publication F-Droid incomplète |
| `app/schemas/**` | `9.json` suivi, `10.json` **non suivi** | schéma Room attendu à l'exécution | **migration 9→10 échoue** |
| `app/build/outputs/mapping/fdroidRelease/mapping.txt` | hors git | désobfuscation des stack traces release | stack traces illisibles |

## 7. Désactivation d'une intégration externe défaillante

| Intégration | Levier | Effet | Réversible |
|---|---|---|---|
| Module Téléchargeur | `Preferences.downloaderEnabled = false` | l'entrée « Télécharger » disparaît de la barre ; le module est inerte | oui (préférence) |
| Réseau (YouTube / Last.fm) | build **sans** les flavors `github` / `playstore` → flavor `fdroid` seul | aucun appel réseau possible | oui (rebuild) |
| Fonctionnalités réseau par catégorie | `NetworkFeature` (`data/model/network/NetworkFeature.kt`) | désactivation par famille de requêtes | oui |
| Moteur de recherche YouTube | `MediaExtractorEngine` injecté par Koin (`downloaderModule`) | remplacer le binding par une implémentation qui renvoie `Unsupported` → l'écran affiche « service indisponible » | oui (1 ligne de binding) |
| Ouverture externe | `IntentExt.openWeb()` | repli : ouvrir le lien dans le navigateur | oui |

**Procédure de désactivation d'une intégration défaillante en production** : ne pas publier un correctif à chaud ; rebuild avec le levier activé, re-vérifier la suite, puis republier. Une désactivation par configuration distante n'existe pas dans ce produit (zéro télémétrie, zéro configuration distante).

## 8. Vérifications après rollback

À exécuter **dans cet ordre**, sur l'arbre restauré :

| # | Commande | Critère de succès |
|---|---|---|
| 1 | `git status --short` | aucun fichier modifié non intentionnel |
| 2 | `./gradlew --offline :app:testFdroidDebugUnitTest` | `BUILD SUCCESSFUL`, **193 tests / 0 échec** |
| 3 | `./gradlew --offline :app:lintFdroidDebug` | `BUILD SUCCESSFUL`, 0 erreur |
| 4 | `bash scripts/check-glass-policy.sh` | `✓ glass-policy OK`, exit 0 |
| 5 | `./gradlew --offline clean assembleFdroidRelease` | `BUILD SUCCESSFUL` (exige le correctif `:macrobenchmark`, §2.2 de l'audit) |
| 6 | `aapt2 dump badging B-Music.apk` | `package`, `versionName`, `versionCode`, `targetSdkVersion` conformes à l'attendu |
| 7 | `apksigner verify --print-certs B-Music.apk` | `Verifies` **et** `CN` = l'identité de release attendue (**pas** `CN=Android Debug`) |
| 8 | `sha256sum B-Music.apk` | empreinte conforme à celle annoncée dans `SHA256SUMS.txt` |
| 9 | (si scénario C) `adb` indisponible ici | **à faire manuellement** : ouvrir l'app sur un appareil ayant la version précédente et vérifier que la base s'ouvre sans erreur |

## 9. Ce qui n'a pas de retour arrière

| Élément | Pourquoi |
|---|---|
| **Données de l'utilisateur** (bibliothèque, pochettes, favoris, historique de recherche) | Vivantes dans MediaStore et la base Room ; un rollback applicatif ne les touche pas, mais une réinstallation avec effacement les perd. **Sauvegarder avant toute opération destructive.** |
| **clé de signature release** | Si elle est perdue, l'application installée ne peut plus être mise à jour : seule une désinstallation et une réinstallation avec une nouvelle clé sont possibles. **À sauvegarder hors du dépôt, en double.** |
| **clé API Last.fm** présente dans l'historique git | Une réécriture d'historique est elle-même irréversible et invalide les clones existants : à n'engager qu'après décision explicite. |
| **Publication déjà faite** (release GitHub, F-Droid, Play) | Le retrait d'une version publiée chez F-Droid ou le Play Store passe par le canal de l'éditeur, hors de portée de ce dépôt. |
