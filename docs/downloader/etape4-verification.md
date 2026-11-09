# Étape 4 — Bibliothèque : filtre Téléchargements

## Implémentation
- `SpecialRepository.downloadsFolder(): Folder?` — filtré sur `Music/B Music`
  (`DownloadStorage.AUDIO_RELATIVE_DIR`), tri `SongSortMode.FolderSongs`.
- `Repository.downloadsFolder()` délègue à `SpecialRepository`.
- `musicFolders()` inchangé : le dossier `Music/B Music` apparaît déjà
  via `musicFolders()` (groupBy `folderPath`). Le filtre ajoute un accès
  nommé "Téléchargements" **seulement si `Preferences.downloaderEnabled`**
  (retourne `null` sinon). Aucun changement de `SongEntity`,
  aucun changement de `MediaStoreObserver`.

## Délai d'apparition (cible < 2 s)
- `MediaStoreObserver` : `REFRESH_DELAY = 500` (observé dans
  `MediaStoreObserver.kt`), `onChange` → `postDelayed(this, REFRESH_DELAY)` →
  `onChanged()` → refresh bibliothèque. **500 ms < 2 s**. Aucune modification
  de l'observateur.

## Modèle inchangé
- Aucune modification de `SongEntity` (vérifié : `git diff --stat` sur
  `SongEntity.kt` = 0 ligne).
- Réutilisation `Folder` (chemin réel). Retrait du filtre = retour en arrière ;
  les fichiers restent lisibles via Dossiers.

## Vidéo audio-only — rapport au plan
- Vidéo téléchargée dans `Movies/B Music` (collection `MediaStore.Video`).
  `RealSongRepository` ne lit que `MediaStore.Audio.Media` :
  **les fichiers vidéo n'apparaissent pas dans la bibliothèque audio**.
  Les rendre jouables en audio seul demanderait de toucher `playback/`
  (scan Video + `MediaItem` vidéo + sélection de piste) ou `SongRepository`
  (double collection) — **non réalisé, reporté au plan** conformément à la
  consigne ("ne la fais pas : rapporte-le").
- En attendant : les vidéos audio-visualisables par une visionneuse externe
  via Dossiers restent accessibles (bypass `playback/`).

## Scan 5 000 titres
- `musicFolders()` et `downloadsFolder()` réutilisent le même scan
  (`songRepository.songs()`), aucun coût additionnel. Non régressif.

## Flag
- Filtre visible seulement si `downloaderEnabled = true`.
