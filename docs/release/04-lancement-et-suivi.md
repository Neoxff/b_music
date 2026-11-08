# B Music — Lancement et suivi v1.0.0

> Agent Release. Date : 8 novembre 2026.
> Statut : **plan prêt, déploiement bloqué** — ne pas lancer tant que les gates Lean / Security / PM ne sont pas vertes (voir `01-pipeline.md` §1).

## 1. Déploiement progressif

| Palier | Part | Point de contrôle |
|---|---|---|
| Bêta fermée | testeurs invités | 24 h de stabilité, 0 crash bloquant |
| Production 5 % | Google Play staged rollout | 24 h |
| Production 20 % | — | 24 h |
| Production 50 % | — | 24 h |
| Production 100 % | — | bilan J+7 |

### Critères d'arrêt (rollback immédiat)
- Taux de crash > 0,5 %
- ANR > 0,2 %
- Note moyenne < 4,0
- Bug de lecture reproductible (scan, lecture, file, notification)

## 2. Suivi sans traqueur

- Aucun SDK d'analytics, aucun crash reporter tiers dans l'app.
- Sources autorisées uniquement :
  - Android vitals / rapports de plantage anonymes de Google Play (agrégés, sans identifiant).
  - GitHub Issues du dépôt public (retours volontaires des utilisateurs).
- Aucune donnée d'écoute, aucun identifiant publicitaire, aucun endpoint réseau en fdroid.

## 3. Plan de retour arrière

- Conserver l'APK/AAB de la version précédente (artefact de release + tag Git).
- Procédure de pause : interrompre le staged rollout dans Google Play Console ; retirer ou marquer la GitHub Release concernée.
- Correctif critique : publier `1.0.1` en < 48 h (même pipeline, `versionCode` incrémenté).
- Communication : note de release « correctif », message dans le README et sur l'issue tracker ; aucune notification push (pas de réseau).

## 4. Triage des retours

### Étiquettes
`bug`, `animation`, `design`, `lecture`, `demande`, `sécurité`, `release`

### Modèle d'issue (bug)
```
Device / Android :
Version B Music (versionName + versionCode) :
Étapes de reproduction :
Résultat attendu / observé :
Logcat (optionnel, sans données personnelles) :
```

### SLA
| Classe | Exemples | SLA |
|---|---|---|
| Bloquant | crash scan/lecture, APK non installable, perte de file | patch < 48 h |
| Majeur | recherche cassée, transition saccadée, notification perdue | fix < 1 semaine |
| Mineur | typo, alignement, contraste limite | backlog v1.1 |
| Hors périmètre | streaming, compte, EQ, widgets, Android Auto | refusé avec réponse type |

## 5. Rétrospective J+14

- [ ] Ce qui a marché (fluidité, design, installation, retours)
- [ ] Ce qui a bloqué (build, signature, recette, distribution)
- [ ] Backlog v1.1 priorisé (RICE)
- [ ] Mise à jour du changelog (Keep a Changelog)
- [ ] Décision documentée : continuer / patch final / geler

## 6. Checklist de lancement (cochable)

- [ ] Gates Lean / Security / PM vertes
- [ ] Build release signé vert en CI, `apksigner verify` OK
- [ ] `B-Music.apk` exact, < 15 Mo, `aapt` conforme (package, version, label, icône)
- [ ] Tests d'installation propre (3 profils) + mise à jour par-dessus version précédente
- [ ] Test fumée complet (premier lancement → arrêt du service)
- [ ] `release/v1.0.0/` : `B-Music.apk`, `B-Music.aab`, `SHA256SUMS.txt`, `mapping.txt`
- [ ] GitHub Release créée avec notes FR/EN, SHA-256 du README vérifié
- [ ] Google Play : AAB + Play App Signing, staged rollout 5 % configuré
- [ ] F-Droid : métadonnées `fastlane/metadata` préparées (licence GPL-3.0 compatible)
- [ ] Page de téléchargement : README avec badge de version, boutons, empreinte SHA-256, instructions « sources inconnues »
- [ ] Politique de confidentialité hébergée (URL publique), classification du contenu, déclaration de sécurité des données
