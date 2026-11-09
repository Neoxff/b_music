# FICHE-MESURE v1.1 — Étapes 0 à 4 liquid léger (sans adb)

**Protocole (sans adb) :**
1. Installe le build **debug** `BMusic-debug.apk` (ou `B-Music-debug.apk` selon ABI).
2. Ouvre **Réglages → appui long sur le numéro de version** (ligne à propos).
3. L'écran **Diagnostics** s'ouvre. Pour chaque scénario :
   - tape **Réinitialiser les compteurs**,
   - tape **Début de scénario** (choisis le libellé),
   - exécute le scénario,
   - tape **Fin de scénario**.
4. Tape **Exporter le rapport (partage Android)** : colle le texte exporté dans le tableau ci-dessous, une ligne par scénario.
5. **Aucune donnée personnelle** : le rapport ne contient ni titre, ni chemin, ni URI.

**Règle sans adb :** aucune commande adb depuis l'agent — tests JVM/Robolectric, captures Paparazzi/Roborazzi, mesures appareil via Diagnostics uniquement.
**Budgets (docs/lean/budgets-v1.2.md) :** images lentes <1% et 0 frame >16ms (60 Hz) ou >8ms (120 Hz), surcoût flou ≤2 ms/image, mémoire ≤15 Mo (RSS <200 Mo), batterie ≤+5% / 30 min, cold-start <600 ms, APK ≤+0,5 Mo (≤15,8 Mo).
**Auto-opt-out (budgets v1.2 §3) :** API <31, low-RAM, power-save, batterie <20%, animations réduites, ou images lentes persistantes via JankStats (>1% sur 5 s ou 2 frames >seuil ou RSS >170 Mo pendant 3 s).

---

## Scénarios (Étape 0 — baseline avant modification visuelle)

| Appareil (modèle, API, Hz) | Scénario | Images lentes % | Pire (ms) | Moyenne (ms) | Mémoire PSS (Ko) | Pic PSS (Ko) | Flou | Raison |
|---|---|---|---|---|---|---|---|---|
|  | défilement bibliothèque |  |  |  |  |  |  |  |
|  | défilement recherche |  |  |  |  |  |  |  |
|  | transition mini → plein écran |  |  |  |  |  |  |  |
|  | lecture 30 s |  |  |  |  |  |  |  |

**Export brut collé ici :**

```
(coller le rapport Diagnostics exporté)
```

## Surfaces (Étape 3 — UNE à la fois, attendre valeurs avant la suivante)

### (1) Mini-player

| Appareil (modèle, API, Hz) | images lentes % | pire (ms) | moyenne (ms) | PSS (Ko) | pic (Ko) | mode flou | raison |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

### (2) Barre de navigation du bas

| Appareil (modèle, API, Hz) | images lentes % | pire (ms) | moyenne (ms) | PSS (Ko) | pic (Ko) | mode flou | raison |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

### (3) Barres supérieures au-dessus des listes

| Appareil (modèle, API, Hz) | images lentes % | pire (ms) | moyenne (ms) | PSS (Ko) | pic (Ko) | mode flou | raison |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

### (4) Feuille d'aperçu du téléchargeur

| Appareil (modèle, API, Hz) | images lentes % | pire (ms) | moyenne (ms) | PSS (Ko) | pic (Ko) | mode flou | raison |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

## Lecteur plein écran + animations (Étape 4)

| Appareil (modèle, API, Hz) | images lentes % | pire (ms) | moyenne (ms) | PSS (Ko) | pic (Ko) | mode flou | raison |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

Scénarios Étape 4 : transition lente / rapide / interrompue · changement de titre en rafale · 2 h de lecture (relevé mémoire/température/batterie via l'écran Batterie du téléphone).

---

## Verdict (à remplir après chaque surface)

- Surface : 
- Budget dépassé ? (Oui → STOP, retour opaque / Non → surface suivante)
- Raison :

## Notes

- Référence : 60 Hz vs 120 Hz — records champs frames lents (>16 ms à 60 Hz, >8 ms à 120 Hz).
- Température/batterie : relevé sur l'écran Batterie du téléphone (Étape 4, 2 h).
- APK : mesuré par l'agent via `apkanalyzer apk file-size` / `download-size` / `summary` (sans adb).
