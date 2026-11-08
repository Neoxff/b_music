# Captures de référence v1.0.0

**Statut : non générées** — aucun device/émulateur n’était disponible le
8 novembre 2026 (`adb devices` vide). Ces captures doivent être produites
sur device avant l’étape 5 (UI du téléchargeur) pour servir de référence
avant/après.

## Procédure

1. Installer l’APK de référence :
   `adb install bmusic/app/build/outputs/apk/fdroid/release/B-Music.apk`
2. Pour chaque écran et chaque thème (sombre par défaut, clair via
   Réglages → Apparence), capturer :
   `adb exec-out screencap -p > docs/baseline/captures/<nom>.png`
3. Nommer les fichiers : `<ecran>_<theme>.png`
   (ex. `home_dark.png`, `player_light.png`).

## Fichiers attendus (14)

| Écran | Sombre | Clair |
|---|---|---|
| Accueil | `home_dark.png` | `home_light.png` |
| Titres | `songs_dark.png` | `songs_light.png` |
| Albums | `albums_dark.png` | `albums_light.png` |
| Lecteur plein écran | `player_dark.png` | `player_light.png` |
| File d’attente | `queue_dark.png` | `queue_light.png` |
| Réglages | `settings_dark.png` | `settings_light.png` |
| Mini-player (sur Accueil) | `miniplayer_dark.png` | `miniplayer_light.png` |

Règles : résolution native du device, aucune retouche, aucun effet verre
(témoin du respect de l’interdit design).
