// Sonore — lecteur de musique 100 % local.
// Entrée utilisateur uniquement : fichiers choisis via le sélecteur système.

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Animated,
  Easing,
  FlatList,
  Modal,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { Ionicons } from '@expo/vector-icons';
import { setAudioModeAsync, useAudioPlayer, useAudioPlayerStatus } from 'expo-audio';

// ─── Design system épuré, sombre, sans flou ni glassmorphism ─────────────────
const Colors = {
  background: '#121212',
  surface: '#181818',
  surfaceRaised: '#282828',
  accent: '#1DB954',
  accentPressed: '#1AA34A',
  accentSoft: 'rgba(29, 185, 84, 0.16)',
  onAccent: '#121212',
  textPrimary: '#FFFFFF',
  textSecondary: '#B3B3B3',
  rowPressed: '#242424',
  iconPressed: 'rgba(255, 255, 255, 0.08)',
  disabledText: '#6A6A6A',
  border: '#2A2A2A',
};

const EXTENSIONS_RECONNUES = ['.mp3', '.flac', '.wav', '.ogg', '.m4a', '.aac'];
const VITESSES = [1, 1.25, 1.5, 2, 0.75];
const MINUTERIES = [
  { label: 'Aucun', minutes: 0 },
  { label: '15 min', minutes: 15 },
  { label: '30 min', minutes: 30 },
  { label: '45 min', minutes: 45 },
  { label: '60 min', minutes: 60 },
  { label: 'Fin du morceau', minutes: -1 },
];

const extensionDe = (nom = '') => {
  const point = nom.lastIndexOf('.');
  return point >= 0 ? nom.slice(point).toLowerCase() : '';
};

const titreDepuisNom = (nom = '') => {
  const point = nom.lastIndexOf('.');
  return (point > 0 ? nom.slice(0, point) : nom).trim() || 'Morceau sans titre';
};

const formatDuree = (secondes) => {
  if (!Number.isFinite(secondes) || secondes <= 0) return '--:--';
  const total = Math.floor(secondes);
  const min = Math.floor(total / 60);
  const sec = String(total % 60).padStart(2, '0');
  return `${min}:${sec}`;
};

const formatTaille = (octets = 0) => {
  if (!octets) return 'Taille inconnue';
  const mo = octets / (1024 * 1024);
  return `${mo >= 10 ? mo.toFixed(0) : mo.toFixed(1)} Mo`;
};

// Identifiant unique pour une occurrence de morceau dans la file ou une playlist.
const faireIdEntree = (track) => `${track.id}#${Date.now()}#${Math.random().toString(36).slice(2, 8)}`;

const melangerTableau = (source) => {
  const copie = [...source];
  for (let i = copie.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [copie[i], copie[j]] = [copie[j], copie[i]];
  }
  return copie;
};

// Bouton icône accessible et assez grand pour le pouce.
const IconeBouton = ({ name, label, onPress, size = 24, color = Colors.textPrimary, style, disabled = false }) => (
  <Pressable
    accessibilityRole="button"
    accessibilityLabel={label}
    accessibilityState={{ disabled }}
    disabled={disabled}
    hitSlop={8}
    onPress={onPress}
    style={({ pressed }) => [
      styles.iconeBouton,
      pressed && !disabled && styles.iconeBoutonPress,
      disabled && styles.iconeBoutonDesactif,
      style,
    ]}
  >
    <Ionicons name={name} size={size} color={disabled ? Colors.disabledText : color} />
  </Pressable>
);

// Barre interactive sans dépendance supplémentaire.
const BarreTactile = ({ value, max, onChange, accessibilityLabel, couleurRemplissage = Colors.accent, hauteur = 4 }) => {
  const [largeur, setLargeur] = useState(1);
  const ratio = max > 0 ? Math.max(0, Math.min(1, value / max)) : 0;

  const seek = (event) => {
    const x = event?.nativeEvent?.locationX ?? 0;
    onChange(Math.max(0, Math.min(1, x / largeur)) * max);
  };

  return (
    <Pressable
      accessibilityRole="adjustable"
      accessibilityLabel={accessibilityLabel}
      accessibilityValue={{ min: 0, max: Math.round(max || 0), now: Math.round(value || 0) }}
      onLayout={(e) => setLargeur(Math.max(1, e.nativeEvent.layout.width))}
      onPress={seek}
      style={[styles.barreZone, { height: Math.max(44, hauteur + 20) }]}
    >
      <View style={[styles.barrePiste, { height: hauteur, borderRadius: hauteur / 2 }]}>
        <View style={[styles.barreRemplissage, { width: `${ratio * 100}%`, backgroundColor: couleurRemplissage, borderRadius: hauteur / 2 }]} />
      </View>
      <View pointerEvents="none" style={[styles.barreCurseur, { left: `${ratio * 100}%` }]} />
    </Pressable>
  );
};

export default function App() {
  // ─── Bibliothèque locale, rien n'est persisté ─────────────────────────────
  const [morceaux, setMorceaux] = useState([]);
  const [erreur, setErreur] = useState('');
  const [notice, setNotice] = useState('');
  const [recherche, setRecherche] = useState('');
  const [onglet, setOnglet] = useState('titres'); // titres | favoris | playlists | file
  const [tri, setTri] = useState('az'); // az | za
  const [favoris, setFavoris] = useState(() => new Set());
  const [file, setFile] = useState([]); // ids dans l'ordre de lecture
  const [indexFile, setIndexFile] = useState(-1);
  const [dureesConnues, setDureesConnues] = useState({});
  const [recents, setRecents] = useState([]);
  const [lecteurOuvert, setLecteurOuvert] = useState(false);
  const [minuterieVisible, setMinuterieVisible] = useState(false);
  const [minuterie, setMinuterie] = useState(0); // 0 aucune, -1 fin du morceau, sinon minutes
  const [finMorceauDemandee, setFinMorceauDemandee] = useState(false);
  const [repetition, setRepetition] = useState('off'); // off | all | one
  const [aleatoire, setAleatoire] = useState(false);
  const [vitesseIndex, setVitesseIndex] = useState(0);
  const [volume, setVolume] = useState(0.85);
  const [muet, setMuet] = useState(false);
  // Playlists locales, gardées en mémoire pendant la session (rien n'est persisté).
  const [playlists, setPlaylists] = useState([]);
  const [playlistActiveId, setPlaylistActiveId] = useState(null);
  const [actionPiste, setActionPiste] = useState(null);
  const [playlistDialog, setPlaylistDialog] = useState({ visible: false, mode: 'create', id: null, targetTrack: null });
  const [playlistInput, setPlaylistInput] = useState('');
  const [playlistErreur, setPlaylistErreur] = useState('');

  const player = useAudioPlayer(null);
  const status = useAudioPlayerStatus(player);
  const timerRef = useRef(null);
  const noticeTimerRef = useRef(null);
  const finTraiteeRef = useRef('');
  const pulsation = useRef(new Animated.Value(1)).current;
  const apparitionMini = useRef(new Animated.Value(0)).current;

  const courant = indexFile >= 0 && indexFile < file.length ? file[indexFile] : null;
  const courantTrack = courant?.track ?? null;
  const enLecture = Boolean(status?.playing);
  const dureeCourante = courantTrack ? dureesConnues[courantTrack.id] ?? status?.duration ?? 0 : 0;
  const positionCourante = status?.currentTime ?? 0;
  const playlistActive = useMemo(
    () => playlists.find((p) => p.id === playlistActiveId) ?? null,
    [playlists, playlistActiveId],
  );

  // ─── Configuration audio : fond d'écran + écran verrouillé ────────────────
  useEffect(() => {
    setAudioModeAsync({
      playsInSilentMode: true,
      shouldPlayInBackground: true,
      interruptionMode: 'doNotMix',
    }).catch(() => {
      // L'app reste utilisable au premier plan si le mode audio est refusé.
    });
  }, []);

  useEffect(() => {
    player.volume = muet ? 0 : volume;
    player.playbackRate = VITESSES[vitesseIndex];
  }, [player, volume, muet, vitesseIndex]);

  // ─── Notification discrète, effacée après quelques secondes ─────────────────
  useEffect(() => {
    if (!notice) return undefined;
    noticeTimerRef.current = setTimeout(() => setNotice(''), 2400);
    return () => clearTimeout(noticeTimerRef.current);
  }, [notice]);

  // ─── Rappel des durées déjà chargées dans le lecteur ──────────────────────
  useEffect(() => {
    if (!courantTrack || !status?.isLoaded || !status?.duration) return;
    setDureesConnues((prev) => (
      prev[courantTrack.id] === status.duration ? prev : { ...prev, [courantTrack.id]: status.duration }
    ));
  }, [courantTrack, status?.duration, status?.isLoaded]);

  // ─── Metadonnées verrouillage, aucune donnée réseau ───────────────────────
  useEffect(() => {
    if (!courantTrack) {
      player.clearLockScreenControls?.();
      return;
    }
    player.setActiveForLockScreen?.(true, {
      title: courantTrack.title,
      artist: 'Fichier local',
      albumTitle: 'Sonore',
    });
  }, [courantTrack, player]);

  // ─── Fin naturelle du morceau ─────────────────────────────────────────────
  const jouerDepuisFile = useCallback((nouvelIndex, nouvelleFile = file, autoPlay = true) => {
    const cible = nouvelleFile[nouvelIndex];
    if (!cible?.track) return false;
    try {
      player.replace(cible.track.uri);
      if (autoPlay) player.play();
      setIndexFile(nouvelIndex);
      setFile(nouvelleFile);
      finTraiteeRef.current = '';
      setRecents((prev) => [cible.track, ...prev.filter((t) => t.id !== cible.track.id)].slice(0, 20));
      return true;
    } catch (e) {
      setErreur(`Fichier illisible : ${cible.track.name}`);
      return false;
    }
  }, [file, player]);

  const suivant = useCallback((depuisFin = false) => {
    if (!file.length || indexFile < 0) return;

    if (depuisFin && finMorceauDemandee) {
      player.pause();
      setFinMorceauDemandee(false);
      setMinuterie(0);
      return;
    }

    if (depuisFin && repetition === 'one') {
      player.seekTo(0);
      player.play();
      return;
    }

    let prochain = -1;
    if (aleatoire && file.length > 1) {
      const candidats = file.map((_, i) => i).filter((i) => i !== indexFile);
      prochain = candidats[Math.floor(Math.random() * candidats.length)];
    } else {
      prochain = indexFile + 1;
    }

    if (prochain >= file.length) {
      if (repetition === 'all') prochain = 0;
      else if (depuisFin) {
        player.pause();
        player.seekTo(0);
        return;
      } else {
        prochain = file.length - 1;
      }
    }
    jouerDepuisFile(prochain, file, true);
  }, [aleatoire, file, finMorceauDemandee, indexFile, jouerDepuisFile, player, repetition]);

  const precedent = useCallback(() => {
    if (!file.length || indexFile < 0) return;
    if (positionCourante > 3) {
      player.seekTo(0);
      return;
    }
    const cible = Math.max(0, indexFile - 1);
    jouerDepuisFile(cible, file, true);
  }, [file, indexFile, jouerDepuisFile, player, positionCourante]);

  useEffect(() => {
    if (!status?.didJustFinish || !courant) return;
    const cle = `${courant.entryId}:${Math.floor(Date.now() / 1000)}`;
    if (finTraiteeRef.current === cle) return;
    finTraiteeRef.current = cle;
    suivant(true);
  }, [courant, status?.didJustFinish, suivant]);

  // ─── Import via sélecteur système, sans copie ni réseau ───────────────────
  const choisirFichiers = async () => {
    setErreur('');
    try {
      const resultat = await DocumentPicker.getDocumentAsync({
        type: 'audio/*',
        multiple: true,
        copyToCacheDirectory: false,
      });

      if (resultat.canceled) {
        setErreur('Sélection annulée.');
        return;
      }

      const valides = (resultat.assets ?? [])
        .filter((asset) => EXTENSIONS_RECONNUES.includes(extensionDe(asset.name)))
        .map((asset, i) => ({
          id: `${asset.uri}#${i}#${Date.now()}`,
          uri: asset.uri,
          name: asset.name ?? `audio-${i + 1}`,
          title: titreDepuisNom(asset.name),
          extension: extensionDe(asset.name),
          size: asset.size ?? 0,
        }))
        .sort((a, b) => a.name.localeCompare(b.name, 'fr', { sensitivity: 'base', numeric: true }));

      if (!valides.length) {
        setErreur('Aucun fichier audio valide (.mp3, .flac, .wav, .ogg, .m4a, .aac).');
        return;
      }

      const nouvelleFile = valides.map((track) => ({ entryId: `${track.id}`, track }));
      setMorceaux(valides);
      setFile(nouvelleFile);
      setIndexFile(0);
      setFavoris(new Set());
      setRecents([]);
      setOnglet('titres');
      setRecherche('');
      setErreur('');
      setNotice('');
      setPlaylists([]);
      setPlaylistActiveId(null);
      setActionPiste(null);
      setPlaylistDialog({ visible: false, mode: 'create', id: null, targetTrack: null });
      setPlaylistInput('');
      setPlaylistErreur('');
      finTraiteeRef.current = '';
      try {
        player.replace(valides[0].uri);
        player.play();
        setRecents([valides[0]]);
      } catch (e) {
        setErreur('Fichier illisible. Essayez un autre morceau.');
      }
    } catch (e) {
      setErreur('Impossible de lire le sélecteur audio. Réessayez.');
    }
  };

  const basculerFavori = useCallback((trackId) => {
    setFavoris((prev) => {
      const next = new Set(prev);
      if (next.has(trackId)) next.delete(trackId);
      else next.add(trackId);
      return next;
    });
  }, []);

  const ajouterALaFile = useCallback((track) => {
    setFile((prev) => [...prev, { entryId: `${track.id}#${Date.now()}#${Math.random()}`, track }]);
    setErreur(`${track.title} ajouté à la file.`);
  }, []);

  const jouerMorceau = useCallback((track, contexte) => {
    const liste = contexte?.length ? contexte : morceaux;
    const nouvelleFile = liste.map((t) => ({ entryId: `${t.id}#${Date.now()}#${Math.random()}`, track: t }));
    const cible = nouvelleFile.findIndex((item) => item.track.id === track.id);
    jouerDepuisFile(Math.max(0, cible), nouvelleFile, true);
  }, [jouerDepuisFile, morceaux]);

  const deplacerDansFile = useCallback((index, direction) => {
    setFile((prev) => {
      const cible = index + direction;
      if (index < 0 || cible < 0 || cible >= prev.length) return prev;
      const copie = [...prev];
      [copie[index], copie[cible]] = [copie[cible], copie[index]];
      return copie;
    });
    setIndexFile((prev) => {
      if (prev === index) return index + direction;
      if (prev === index + direction) return index;
      return prev;
    });
  }, []);

  const retirerDeFile = useCallback((index) => {
    if (index === indexFile) {
      setErreur('Impossible de retirer le morceau en cours.');
      return;
    }
    setFile((prev) => prev.filter((_, i) => i !== index));
    setIndexFile((prev) => (prev > index ? prev - 1 : prev));
  }, [indexFile]);

  const viderFileSaufCourant = useCallback(() => {
    setFile((prev) => (indexFile >= 0 && prev[indexFile] ? [prev[indexFile]] : []));
    if (indexFile >= 0) setIndexFile(0);
  }, [indexFile]);

  // ─── Playlists locales (mémoire uniquement) ───────────────────────────────
  const ouvrirCreationDepuis = useCallback((track = null) => {
    setActionPiste(null);
    setPlaylistDialog({ visible: true, mode: 'create', id: null, targetTrack: track });
    setPlaylistInput('');
    setPlaylistErreur('');
    setOnglet('playlists');
  }, []);

  const ouvrirRenommage = useCallback((playlist) => {
    setPlaylistDialog({ visible: true, mode: 'rename', id: playlist.id, targetTrack: null });
    setPlaylistInput(playlist.name);
    setPlaylistErreur('');
  }, []);

  const fermerDialogPlaylist = useCallback(() => {
    setPlaylistDialog((prev) => ({ ...prev, visible: false }));
    setPlaylistErreur('');
  }, []);

  const enregistrerPlaylist = useCallback(() => {
    const nom = playlistInput.trim();
    if (!nom) {
      setPlaylistErreur('Donnez un nom à la playlist.');
      return;
    }
    const doublon = playlists.some(
      (p) => p.id !== playlistDialog.id && p.name.trim().toLowerCase() === nom.toLowerCase(),
    );
    if (doublon) {
      setPlaylistErreur('Une playlist porte déjà ce nom.');
      return;
    }

    if (playlistDialog.mode === 'rename') {
      setPlaylists((prev) => prev.map((p) => (p.id === playlistDialog.id ? { ...p, name: nom } : p)));
      setNotice('Playlist renommée.');
    } else {
      const nouvelle = {
        id: `playlist-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        name: nom,
        items: playlistDialog.targetTrack ? [{ entryId: faireIdEntree(playlistDialog.targetTrack), track: playlistDialog.targetTrack }] : [],
      };
      setPlaylists((prev) => [nouvelle, ...prev]);
      setPlaylistActiveId(nouvelle.id);
      setNotice(playlistDialog.targetTrack ? 'Playlist créée, morceau ajouté.' : 'Playlist créée.');
    }
    fermerDialogPlaylist();
  }, [fermerDialogPlaylist, playlistDialog, playlistInput, playlists]);

  const ajouterAPlaylist = useCallback((track, playlistId) => {
    setPlaylists((prev) => prev.map((playlist) => {
      if (playlist.id !== playlistId) return playlist;
      const dejaPresent = playlist.items.some((item) => item.track.id === track.id);
      if (dejaPresent) {
        setNotice('Déjà dans cette playlist.');
        return playlist;
      }
      setNotice(`Ajouté à « ${playlist.name} ».`);
      return { ...playlist, items: [...playlist.items, { entryId: faireIdEntree(track), track }] };
    }));
    setActionPiste(null);
  }, []);

  const retirerDePlaylist = useCallback((playlistId, index) => {
    setPlaylists((prev) => prev.map((playlist) => (
      playlist.id === playlistId
        ? { ...playlist, items: playlist.items.filter((_, i) => i !== index) }
        : playlist
    )));
  }, []);

  const deplacerDansPlaylist = useCallback((playlistId, index, direction) => {
    setPlaylists((prev) => prev.map((playlist) => {
      if (playlist.id !== playlistId) return playlist;
      const cible = index + direction;
      if (index < 0 || cible < 0 || cible >= playlist.items.length) return playlist;
      const items = [...playlist.items];
      [items[index], items[cible]] = [items[cible], items[index]];
      return { ...playlist, items };
    }));
  }, []);

  const supprimerPlaylist = useCallback((playlist) => {
    Alert.alert(
      'Supprimer la playlist ?',
      `« ${playlist.name} » sera supprimée. Les fichiers audio ne seront pas supprimés.`,
      [
        { text: 'Annuler', style: 'cancel' },
        {
          text: 'Supprimer',
          style: 'destructive',
          onPress: () => {
            setPlaylists((prev) => prev.filter((p) => p.id !== playlist.id));
            setPlaylistActiveId((prev) => (prev === playlist.id ? null : prev));
            setNotice('Playlist supprimée.');
          },
        },
      ],
    );
  }, []);

  const jouerPlaylist = useCallback((playlist, index = 0, shuffle = false) => {
    if (!playlist.items.length) {
      setNotice('Cette playlist est vide.');
      return;
    }
    const source = shuffle ? melangerTableau(playlist.items) : playlist.items;
    const nouvelleFile = source.map((item) => ({ entryId: faireIdEntree(item.track), track: item.track }));
    const cible = shuffle ? 0 : Math.max(0, Math.min(index, nouvelleFile.length - 1));
    jouerDepuisFile(cible, nouvelleFile, true);
    setOnglet('file');
  }, [jouerDepuisFile]);

  // ─── Filtrage, tri et listes dérivées ─────────────────────────────────────
  const morceauxFiltres = useMemo(() => {
    const q = recherche.trim().toLowerCase();
    let liste = morceaux;
    if (onglet === 'favoris') liste = liste.filter((t) => favoris.has(t.id));
    if (q) {
      liste = liste.filter((t) => `${t.title} ${t.name}`.toLowerCase().includes(q));
    }
    return [...liste].sort((a, b) => (tri === 'az'
      ? a.name.localeCompare(b.name, 'fr', { sensitivity: 'base', numeric: true })
      : b.name.localeCompare(a.name, 'fr', { sensitivity: 'base', numeric: true })));
  }, [favoris, morceaux, onglet, recherche, tri]);

  const fileAffichee = useMemo(() => file.map((item, index) => ({ ...item, index })), [file]);

  const rechercheBas = recherche.trim().toLowerCase();
  const playlistsFiltrees = useMemo(() => {
    if (!rechercheBas) return playlists;
    return playlists.filter((p) => p.name.toLowerCase().includes(rechercheBas));
  }, [playlists, rechercheBas]);
  const playlistItemsFiltres = useMemo(() => {
    const source = playlistActive?.items ?? [];
    const avecIndex = source.map((item, index) => ({ ...item, index }));
    if (!rechercheBas) return avecIndex;
    return avecIndex.filter(({ track }) => `${track.title} ${track.name}`.toLowerCase().includes(rechercheBas));
  }, [playlistActive, rechercheBas]);

  // ─── Animation du bouton play : ressort discret, pas de glassmorphism ─────
  useEffect(() => {
    Animated.spring(pulsation, {
      toValue: enLecture ? 1.08 : 1,
      friction: 6,
      tension: 90,
      useNativeDriver: true,
    }).start();
  }, [enLecture, pulsation]);

  useEffect(() => {
    Animated.timing(apparitionMini, {
      toValue: courantTrack ? 1 : 0,
      duration: courantTrack ? 220 : 160,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start();
  }, [apparitionMini, courantTrack]);

  // ─── Minuterie sommeil ────────────────────────────────────────────────────
  useEffect(() => () => clearTimeout(timerRef.current), []);
  const choisirMinuterie = (minutes) => {
    clearTimeout(timerRef.current);
    setMinuterie(minutes);
    setFinMorceauDemandee(minutes === -1);
    setMinuterieVisible(false);
    if (minutes > 0) {
      timerRef.current = setTimeout(() => {
        player.pause();
        setMinuterie(0);
        setErreur('Minuterie terminée. Lecture en pause.');
      }, minutes * 60 * 1000);
    }
  };

  const reinitialiser = () => {
    player.pause();
    player.clearLockScreenControls?.();
    setMorceaux([]);
    setFile([]);
    setIndexFile(-1);
    setFavoris(new Set());
    setRecents([]);
    setDureesConnues({});
    setRecherche('');
    setOnglet('titres');
    setErreur('');
    setNotice('');
    setPlaylists([]);
    setPlaylistActiveId(null);
    setActionPiste(null);
    setPlaylistDialog({ visible: false, mode: 'create', id: null, targetTrack: null });
    setPlaylistInput('');
    setPlaylistErreur('');
    setMinuterie(0);
    setFinMorceauDemandee(false);
    clearTimeout(timerRef.current);
  };

  const ligneMorceau = ({ item }) => {
    const actif = courantTrack?.id === item.id;
    const estFavori = favoris.has(item.id);
    return (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Lire ${item.title}, ${formatTaille(item.size)}`}
        onPress={() => jouerMorceau(item, morceauxFiltres)}
        style={({ pressed }) => [styles.ligneMorceau, actif && styles.ligneMorceauActive, pressed && styles.ligneMorceauPress]}
      >
        <View style={styles.vignette}>
          <Ionicons name="musical-notes" size={22} color={actif ? Colors.accent : Colors.textSecondary} />
        </View>
        <View style={styles.ligneTextes}>
          <Text numberOfLines={1} style={[styles.ligneTitre, actif && styles.texteAccent]}>{item.title}</Text>
          <Text numberOfLines={1} style={styles.ligneMeta}>{item.extension.replace('.', '').toUpperCase()} · {formatTaille(item.size)} · {formatDuree(dureesConnues[item.id])}</Text>
        </View>
        <IconeBouton name={estFavori ? 'heart' : 'heart-outline'} label={estFavori ? 'Retirer des favoris' : 'Ajouter aux favoris'} color={estFavori ? Colors.accent : Colors.textSecondary} size={20} onPress={() => basculerFavori(item.id)} />
        <IconeBouton name="ellipsis-horizontal" label={`Actions pour ${item.title}`} color={Colors.textSecondary} size={22} onPress={() => setActionPiste(item)} />
      </Pressable>
    );
  };

  const ligneFile = ({ item }) => {
    const actif = indexFile === item.index;
    return (
      <View style={[styles.ligneFile, actif && styles.ligneMorceauActive]}>
        <IconeBouton name="reorder-three" label="Élément de la file" size={20} color={Colors.textSecondary} onPress={() => {}} disabled />
        <Pressable style={styles.ligneTextes} accessibilityRole="button" accessibilityLabel={`Lire ${item.track.title}`} onPress={() => jouerDepuisFile(item.index, file, true)}>
          <Text numberOfLines={1} style={[styles.ligneTitre, actif && styles.texteAccent]}>{item.track.title}</Text>
          <Text numberOfLines={1} style={styles.ligneMeta}>Position {item.index + 1} · {item.track.extension.replace('.', '').toUpperCase()}</Text>
        </Pressable>
        <IconeBouton name="chevron-up" label="Monter dans la file" size={20} color={Colors.textSecondary} onPress={() => deplacerDansFile(item.index, -1)} disabled={item.index === 0} />
        <IconeBouton name="chevron-down" label="Descendre dans la file" size={20} color={Colors.textSecondary} onPress={() => deplacerDansFile(item.index, 1)} disabled={item.index === file.length - 1} />
        <IconeBouton name="close" label="Retirer de la file" size={20} color={Colors.textSecondary} onPress={() => retirerDeFile(item.index)} disabled={actif} />
      </View>
    );
  };

  const lignePlaylist = ({ item }) => {
    const actif = playlistActiveId === item.id;
    const nb = item.items.length;
    return (
      <View style={[styles.lignePlaylist, actif && styles.ligneMorceauActive]}>
        <IconeBouton name="albums-outline" label={`Playlist ${item.name}`} color={actif ? Colors.accent : Colors.textSecondary} size={22} onPress={() => setPlaylistActiveId(item.id)} />
        <Pressable style={styles.ligneTextes} accessibilityRole="button" accessibilityLabel={`Ouvrir la playlist ${item.name}, ${nb} morceaux`} onPress={() => setPlaylistActiveId(item.id)}>
          <Text numberOfLines={1} style={[styles.ligneTitre, actif && styles.texteAccent]}>{item.name}</Text>
          <Text numberOfLines={1} style={styles.ligneMeta}>{nb} morceau{nb !== 1 ? 'x' : ''}</Text>
        </Pressable>
        <IconeBouton name="play" label={`Lire ${item.name}`} size={21} onPress={() => jouerPlaylist(item, 0, false)} />
        <IconeBouton name="create-outline" label={`Renommer ${item.name}`} size={18} color={Colors.textSecondary} onPress={() => ouvrirRenommage(item)} />
        <IconeBouton name="trash-outline" label={`Supprimer ${item.name}`} size={18} color={Colors.textSecondary} onPress={() => supprimerPlaylist(item)} />
      </View>
    );
  };

  const lignePlaylistMorceau = ({ item }) => {
    const actif = courantTrack?.id === item.track.id;
    const itemsPlaylist = playlistActive?.items ?? [];
    return (
      <View style={[styles.ligneFile, actif && styles.ligneMorceauActive]}>
        <IconeBouton name="reorder-three" label="Élément de la playlist" size={20} color={Colors.textSecondary} onPress={() => {}} disabled />
        <Pressable style={styles.ligneTextes} accessibilityRole="button" accessibilityLabel={`Lire ${item.track.title}`} onPress={() => playlistActive && jouerPlaylist(playlistActive, item.index, false)}>
          <Text numberOfLines={1} style={[styles.ligneTitre, actif && styles.texteAccent]}>{item.track.title}</Text>
          <Text numberOfLines={1} style={styles.ligneMeta}>{item.index + 1}. {item.track.extension.replace('.', '').toUpperCase()} · {item.track.name}</Text>
        </Pressable>
        <IconeBouton name="chevron-up" label="Monter dans la playlist" size={20} color={Colors.textSecondary} onPress={() => playlistActive && deplacerDansPlaylist(playlistActive.id, item.index, -1)} disabled={item.index === 0} />
        <IconeBouton name="chevron-down" label="Descendre dans la playlist" size={20} color={Colors.textSecondary} onPress={() => playlistActive && deplacerDansPlaylist(playlistActive.id, item.index, 1)} disabled={item.index === itemsPlaylist.length - 1} />
        <IconeBouton name="close" label="Retirer de la playlist" size={20} color={Colors.textSecondary} onPress={() => playlistActive && retirerDePlaylist(playlistActive.id, item.index)} />
      </View>
    );
  };

  const vueFile = onglet === 'file';
  const vuePlaylists = onglet === 'playlists';
  const listeVide = !vueFile && !vuePlaylists && morceauxFiltres.length === 0;

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
        <StatusBar barStyle="light-content" backgroundColor={Colors.background} />
        <View style={styles.app}>
          {/* ─── Barre d'application ─────────────────────────────────────── */}
          <View style={styles.header}>
            <View style={styles.logoBloc}>
              <View style={styles.logoCercle}><Ionicons name="musical-notes" size={22} color={Colors.onAccent} /></View>
              <View>
                <Text style={styles.logoTexte}>Sonore</Text>
                <Text style={styles.logoSous}>{morceaux.length ? `${morceaux.length} morceau${morceaux.length > 1 ? 'x' : ''}` : '100 % local'}</Text>
              </View>
            </View>
            <View style={styles.headerActions}>
              <IconeBouton name="folder-open-outline" label="Choisir mes musiques" size={23} onPress={choisirFichiers} />
              {morceaux.length > 0 && <IconeBouton name="refresh-outline" label="Réinitialiser la bibliothèque" size={22} onPress={reinitialiser} />}
            </View>
          </View>

          {aucuneDonnee ? (
            <View style={styles.accueil}>
              <View style={styles.logoGrand}><Ionicons name="musical-notes" size={54} color={Colors.onAccent} /></View>
              <Text style={styles.accueilTitre}>Votre musique, ici</Text>
              <Text style={styles.accueilPhrase}>Choisissez vos fichiers audio. Rien ne sort de ce téléphone.</Text>
              {!!erreur && <Text accessibilityLiveRegion="polite" style={styles.messageErreur}>{erreur}</Text>}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Choisir mes musiques"
                onPress={choisirFichiers}
                style={({ pressed }) => [styles.boutonPrincipal, pressed && styles.boutonPrincipalPress]}
              >
                <Text style={styles.boutonPrincipalTexte}>Choisir mes musiques</Text>
              </Pressable>
            </View>
          ) : (
            <>
              {/* ─── Recherche et filtres ─────────────────────────────────── */}
              <View style={styles.rechercheBloc}>
                <View style={styles.rechercheChamp}>
                  <Ionicons name="search" size={19} color={Colors.textSecondary} />
                  <TextInput
                    accessibilityLabel="Rechercher dans la bibliothèque"
                    value={recherche}
                    onChangeText={setRecherche}
                    placeholder="Rechercher"
                    placeholderTextColor={Colors.textSecondary}
                    style={styles.rechercheInput}
                    autoCorrect={false}
                    autoCapitalize="none"
                    returnKeyType="search"
                  />
                  {!!recherche && <IconeBouton name="close-circle" label="Effacer la recherche" size={21} color={Colors.textSecondary} style={styles.clearRecherche} onPress={() => setRecherche('')} />}
                </View>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.puces}>
                  {[
                    ['titres', 'Titres'],
                    ['favoris', `Favoris${favoris.size ? ` (${favoris.size})` : ''}`],
                    ['playlists', `Playlists${playlists.length ? ` (${playlists.length})` : ''}`],
                    ['file', `File${file.length ? ` (${file.length})` : ''}`],
                  ].map(([cle, label]) => (
                    <Pressable
                      key={cle}
                      accessibilityRole="tab"
                      accessibilityState={{ selected: onglet === cle }}
                      onPress={() => setOnglet(cle)}
                      style={[styles.puce, onglet === cle && styles.puceActive]}
                    >
                      <Text style={[styles.puceTexte, onglet === cle && styles.puceTexteActive]}>{label}</Text>
                    </Pressable>
                  ))}
                  <Pressable accessibilityRole="button" accessibilityLabel="Inverser le tri A à Z ou Z à A" onPress={() => setTri((v) => (v === 'az' ? 'za' : 'az'))} style={styles.puce}>
                    <Ionicons name={tri === 'az' ? 'arrow-down-outline' : 'arrow-up-outline'} size={17} color={Colors.textPrimary} />
                    <Text style={styles.puceTexte}>{tri === 'az' ? 'A → Z' : 'Z → A'}</Text>
                  </Pressable>
                </ScrollView>
                {!!erreur && <Text accessibilityLiveRegion="polite" style={styles.messageErreurCompact}>{erreur}</Text>}
                {!!notice && <Text accessibilityLiveRegion="polite" style={styles.messageNotice}>{notice}</Text>}
              </View>

              {/* ─── Liste suivie, playlists ou file d'attente ───────────── */}
              {vuePlaylists && playlistActive ? (
                <View style={styles.playlistDetailHeader}>
                  <View style={styles.lignePlaylistInfos}>
                    <IconeBouton name="chevron-back" label="Retour aux playlists" size={26} onPress={() => setPlaylistActiveId(null)} />
                    <View style={styles.ligneTextes}>
                      <Text numberOfLines={1} style={styles.sectionTitre}>{playlistActive.name}</Text>
                      <Text numberOfLines={1} style={styles.ligneMeta}>{playlistActive.items.length} morceau{playlistActive.items.length !== 1 ? 'x' : ''}</Text>
                    </View>
                  </View>
                  <View style={styles.playlistActions}>
                    <IconeBouton name="play" label="Lire la playlist" size={21} onPress={() => playlistActive && jouerPlaylist(playlistActive, 0, false)} />
                    <IconeBouton name="shuffle" label="Lire en aléatoire" size={21} color={Colors.textSecondary} onPress={() => playlistActive && jouerPlaylist(playlistActive, 0, true)} />
                    <IconeBouton name="create-outline" label="Renommer la playlist" size={19} color={Colors.textSecondary} onPress={() => playlistActive && ouvrirRenommage(playlistActive)} />
                    <IconeBouton name="trash-outline" label="Supprimer la playlist" size={19} color={Colors.textSecondary} onPress={() => playlistActive && supprimerPlaylist(playlistActive)} />
                  </View>
                </View>
              ) : null}
              {vueFile ? (
                <View style={styles.fileHeader}>
                  <Text style={styles.sectionTitre}>File d’attente</Text>
                  <IconeBouton name="trash-outline" label="Vider la file sauf le morceau en cours" size={21} color={Colors.textSecondary} onPress={viderFileSaufCourant} />
                </View>
              ) : null}

              {vuePlaylists ? (
                playlistActive ? (
                  <FlatList
                    data={playlistItemsFiltres}
                    keyExtractor={(item) => item.entryId}
                    renderItem={lignePlaylistMorceau}
                    contentContainerStyle={[styles.listContenu, currentPaddingBottom(courantTrack)]}
                    getItemLayout={(_, index) => ({ length: 64, offset: 64 * index, index })}
                    initialNumToRender={12}
                    windowSize={8}
                    keyboardShouldPersistTaps="handled"
                    ListEmptyComponent={<Text style={styles.listeVide}>Playlist vide. Ajoutez des morceaux depuis la bibliothèque.</Text>}
                  />
                ) : (
                  <FlatList
                    data={playlistsFiltrees}
                    keyExtractor={(item) => item.id}
                    renderItem={lignePlaylist}
                    contentContainerStyle={[styles.listContenu, currentPaddingBottom(courantTrack)]}
                    getItemLayout={(_, index) => ({ length: 68, offset: 68 * index, index })}
                    initialNumToRender={12}
                    windowSize={8}
                    keyboardShouldPersistTaps="handled"
                    ListHeaderComponent={(
                      <View style={styles.fileHeader}>
                        <Text style={styles.sectionTitre}>Playlists</Text>
                        <IconeBouton name="add" label="Créer une playlist" size={23} color={Colors.textPrimary} onPress={() => ouvrirCreationDepuis()} />
                      </View>
                    )}
                    ListEmptyComponent={(
                      <View style={styles.playlistVide}>
                        <Ionicons name="albums-outline" size={42} color={Colors.textSecondary} />
                        <Text style={styles.playlistVideTitre}>{recherche ? 'Aucun résultat' : 'Aucune playlist'}</Text>
                        <Text style={styles.playlistVideTexte}>{recherche ? 'Changez la recherche.' : 'Créez une playlist locale pour vos morceaux favoris.'}</Text>
                      </View>
                    )}
                  />
                )
              ) : vueFile ? (
                <FlatList
                  data={fileAffichee}
                  keyExtractor={(item) => item.entryId}
                  renderItem={ligneFile}
                  contentContainerStyle={[styles.listContenu, currentPaddingBottom(courantTrack)]}
                  getItemLayout={(_, index) => ({ length: 64, offset: 64 * index, index })}
                  initialNumToRender={12}
                  windowSize={8}
                />
              ) : (
                <FlatList
                  data={morceauxFiltres}
                  keyExtractor={(item) => item.id}
                  renderItem={ligneMorceau}
                  contentContainerStyle={[styles.listContenu, currentPaddingBottom(courantTrack)]}
                  getItemLayout={(_, index) => ({ length: 68, offset: 68 * index, index })}
                  initialNumToRender={12}
                  windowSize={8}
                  keyboardShouldPersistTaps="handled"
                  ListEmptyComponent={<Text style={styles.listeVide}>Aucun résultat. Modifiez la recherche ou ajoutez des fichiers.</Text>}
                />
              )}

              {/* ─── Accès aux morceaux récents ──────────────────────────── */}
              {recents.length > 0 && onglet !== 'file' && onglet !== 'playlists' && (
                <View style={styles.recentsBloc}>
                  <Text style={styles.recentsTitre}>Récents</Text>
                  <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.recentsScroll}>
                    {recents.slice(0, 8).map((t) => (
                      <Pressable key={t.id} accessibilityRole="button" accessibilityLabel={`Lire ${t.title}`} onPress={() => jouerMorceau(t, morceaux)} style={styles.recentCarte}>
                        <Ionicons name="musical-note" size={17} color={Colors.accent} />
                        <Text numberOfLines={1} style={styles.recentTexte}>{t.title}</Text>
                      </Pressable>
                    ))}
                  </ScrollView>
                </View>
              )}

              {/* ─── Mini-lecteur ───────────────────────────────────────── */}
              {!!courantTrack && (
                <Animated.View style={[styles.miniLecteur, { opacity: apparitionMini, transform: [{ translateY: apparitionMini.interpolate({ inputRange: [0, 1], outputRange: [24, 0] }) }] }]}>
                  <Pressable accessibilityRole="button" accessibilityLabel="Ouvrir le lecteur" onPress={() => setLecteurOuvert(true)} style={styles.miniZonePrincipale}>
                    <View style={styles.miniVignette}><Ionicons name="musical-notes" size={20} color={Colors.textSecondary} /></View>
                    <View style={styles.ligneTextes}>
                      <Text numberOfLines={1} style={styles.miniTitre}>{courantTrack.title}</Text>
                      <Text numberOfLines={1} style={styles.ligneMeta}>{repetition === 'one' ? 'Répéter ce morceau' : aleatoire ? 'Lecture aléatoire' : 'Fichier local'}</Text>
                    </View>
                  </Pressable>
                  <IconeBouton name={enLecture ? 'pause' : 'play'} label={enLecture ? 'Mettre en pause' : 'Lire'} size={25} color={Colors.textPrimary} onPress={() => (enLecture ? player.pause() : player.play())} />
                  <IconeBouton name="play-skip-forward" label="Morceau suivant" size={23} color={Colors.textPrimary} onPress={() => suivant(false)} />
                </Animated.View>
              )}
            </>
          )}
        </View>
      </SafeAreaView>

      {/* ─── Lecteur plein écran ─────────────────────────────────────────── */}
      <Modal visible={lecteurOuvert && !!courantTrack} transparent animationType="slide" onRequestClose={() => setLecteurOuvert(false)} statusBarTranslucent>
        <SafeAreaView style={styles.lecteur} edges={['top', 'bottom', 'left', 'right']}>
          <View style={styles.lecteurHeader}>
            <IconeBouton name="chevron-down" label="Réduire le lecteur" size={28} onPress={() => setLecteurOuvert(false)} />
            <Text style={styles.lecteurSurtitre}>LECTURE EN COURS</Text>
            <IconeBouton name="timer-outline" label="Minuterie de sommeil" size={23} color={minuterie !== 0 ? Colors.accent : Colors.textPrimary} onPress={() => setMinuterieVisible(true)} />
          </View>

          <View style={styles.lecteurCorps}>
            <View style={styles.pochette}><Ionicons name="musical-notes" size={96} color={Colors.textSecondary} /></View>
            <Text numberOfLines={2} style={styles.lecteurTitre}>{courantTrack?.title}</Text>
            <Text numberOfLines={1} style={styles.lecteurArtiste}>Fichier local{courantTrack?.extension ? ` · ${courantTrack.extension.replace('.', '').toUpperCase()}` : ''}</Text>

            <View style={styles.progressionBloc}>
              <Text style={styles.temps}>{formatDuree(positionCourante)}</Text>
              <View style={styles.progressionFlex}>
                <BarreTactile value={positionCourante} max={dureeCourante || 0} onChange={(s) => player.seekTo(s)} accessibilityLabel="Position dans le morceau" />
              </View>
              <Text style={styles.temps}>{formatDuree(dureeCourante)}</Text>
            </View>

            <View style={styles.controles}>
              <IconeBouton name="shuffle" label={aleatoire ? 'Lecture aléatoire activée' : 'Activer la lecture aléatoire'} size={23} color={aleatoire ? Colors.accent : Colors.textSecondary} onPress={() => setAleatoire((v) => !v)} />
              <IconeBouton name="play-skip-back" label="Morceau précédent" size={30} onPress={precedent} />
              <Animated.View style={{ transform: [{ scale: pulsation }] }}>
                <TouchableOpacity accessibilityRole="button" accessibilityLabel={enLecture ? 'Mettre en pause' : 'Lire'} onPress={() => (enLecture ? player.pause() : player.play())} style={styles.playPrincipal} activeOpacity={0.85}>
                  <Ionicons name={enLecture ? 'pause' : 'play'} size={32} color={Colors.onAccent} style={{ marginLeft: enLecture ? 0 : 3 }} />
                </TouchableOpacity>
              </Animated.View>
              <IconeBouton name="play-skip-forward" label="Morceau suivant" size={30} onPress={() => suivant(false)} />
              <IconeBouton name={repetition === 'one' ? 'repeat-one' : 'repeat'} label={repetition === 'off' ? 'Activer la répétition' : repetition === 'all' ? 'Répéter tout' : 'Répéter ce morceau'} size={23} color={repetition === 'off' ? Colors.textSecondary : Colors.accent} onPress={() => setRepetition((v) => (v === 'off' ? 'all' : v === 'all' ? 'one' : 'off'))} />
            </View>

            <View style={styles.optionsLigne}>
              <Pressable accessibilityRole="button" accessibilityLabel="Changer la vitesse de lecture" onPress={() => setVitesseIndex((i) => (i + 1) % VITESSES.length)} style={styles.optionBouton}>
                <Ionicons name="speedometer-outline" size={18} color={Colors.textSecondary} />
                <Text style={styles.optionTexte}>{VITESSES[vitesseIndex]}×</Text>
              </Pressable>
              <Pressable accessibilityRole="button" accessibilityLabel={courantTrack && favoris.has(courantTrack.id) ? 'Retirer des favoris' : 'Ajouter aux favoris'} onPress={() => courantTrack && basculerFavori(courantTrack.id)} style={styles.optionBouton}>
                <Ionicons name={courantTrack && favoris.has(courantTrack.id) ? 'heart' : 'heart-outline'} size={18} color={courantTrack && favoris.has(courantTrack.id) ? Colors.accent : Colors.textSecondary} />
                <Text style={styles.optionTexte}>Favoris</Text>
              </Pressable>
              <Pressable accessibilityRole="button" accessibilityLabel="Voir la file d’attente" onPress={() => { setLecteurOuvert(false); setOnglet('file'); }} style={styles.optionBouton}>
                <Ionicons name="list-outline" size={18} color={Colors.textSecondary} />
                <Text style={styles.optionTexte}>File</Text>
              </Pressable>
            </View>

            <View style={styles.volumeBloc}>
              <IconeBouton name={muet || volume === 0 ? 'volume-mute' : 'volume-medium'} label={muet ? 'Réactiver le son' : 'Couper le son'} size={22} color={Colors.textSecondary} onPress={() => setMuet((v) => !v)} />
              <View style={styles.volumeFlex}><BarreTactile value={muet ? 0 : volume} max={1} onChange={(v) => { setMuet(false); setVolume(v); }} accessibilityLabel="Volume" hauteur={3} /></View>
              <Text style={styles.volumeTexte}>{Math.round((muet ? 0 : volume) * 100)}%</Text>
            </View>
          </View>
        </SafeAreaView>
      </Modal>

      {/* ─── Actions sur une piste ───────────────────────────────────────── */}
      <Modal visible={!!actionPiste} transparent animationType="fade" onRequestClose={() => setActionPiste(null)}>
        <Pressable style={styles.actionFond} onPress={() => setActionPiste(null)}>
          <View style={styles.actionCarte}>
            <Text numberOfLines={1} style={styles.actionTitre}>{actionPiste?.title}</Text>
            <Pressable accessibilityRole="button" accessibilityLabel="Ajouter à la file d’attente" onPress={() => { if (actionPiste) { ajouterALaFile(actionPiste); setActionPiste(null); } }} style={styles.actionOption}>
              <Ionicons name="add" size={20} color={Colors.accent} />
              <Text style={styles.actionTexte}>Ajouter à la file d’attente</Text>
            </Pressable>
            <Text style={styles.actionSection}>Playlists</Text>
            <ScrollView style={styles.actionScroll} keyboardShouldPersistTaps="handled">
              {playlists.length === 0 ? (
                <Text style={styles.actionVide}>Aucune playlist pour l’instant.</Text>
              ) : playlists.map((playlist) => (
                <Pressable
                  key={playlist.id}
                  accessibilityRole="button"
                  accessibilityLabel={`Ajouter à la playlist ${playlist.name}`}
                  onPress={() => actionPiste && ajouterAPlaylist(actionPiste, playlist.id)}
                  style={styles.actionOption}
                >
                  <Ionicons name="albums-outline" size={20} color={Colors.textSecondary} />
                  <View style={styles.actionTextes}>
                    <Text numberOfLines={1} style={styles.actionTexte}>{playlist.name}</Text>
                    <Text style={styles.actionMeta}>{playlist.items.length} morceau{playlist.items.length !== 1 ? 'x' : ''}</Text>
                  </View>
                </Pressable>
              ))}
            </ScrollView>
            <Pressable accessibilityRole="button" accessibilityLabel="Nouvelle playlist et ajouter" onPress={() => actionPiste && ouvrirCreationDepuis(actionPiste)} style={[styles.actionOption, styles.actionSeparateur]}>
              <Ionicons name="add" size={20} color={Colors.accent} />
              <Text style={styles.actionTexte}>Nouvelle playlist et ajouter</Text>
            </Pressable>
            <Pressable accessibilityRole="button" accessibilityLabel="Annuler" onPress={() => setActionPiste(null)} style={styles.actionAnnuler}>
              <Text style={styles.actionAnnulerTexte}>Annuler</Text>
            </Pressable>
          </View>
        </Pressable>
      </Modal>

      {/* ─── Créer / renommer une playlist ─────────────────────────────────── */}
      <Modal visible={playlistDialog.visible} transparent animationType="fade" onRequestClose={fermerDialogPlaylist}>
        <Pressable style={styles.actionFond} onPress={fermerDialogPlaylist}>
          <View style={styles.actionCarte}>
            <Text style={styles.actionTitre}>{playlistDialog.mode === 'rename' ? 'Renommer la playlist' : 'Nouvelle playlist'}</Text>
            <TextInput
              accessibilityLabel="Nom de la playlist"
              value={playlistInput}
              onChangeText={(text) => { setPlaylistInput(text); if (playlistErreur) setPlaylistErreur(''); }}
              placeholder="Nom de la playlist"
              placeholderTextColor={Colors.textSecondary}
              style={styles.actionInput}
              autoFocus
              returnKeyType="done"
              onSubmitEditing={enregistrerPlaylist}
            />
            {!!playlistErreur && <Text accessibilityLiveRegion="polite" style={styles.actionErreur}>{playlistErreur}</Text>}
            <View style={styles.dialogBoutons}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Annuler"
                onPress={fermerDialogPlaylist}
                style={[styles.dialogBouton, styles.dialogBoutonSecondaire]}
              >
                <Text style={styles.dialogTexteSecondaire}>Annuler</Text>
              </Pressable>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={playlistDialog.mode === 'rename' ? 'Enregistrer' : 'Créer'}
                accessibilityState={{ disabled: !playlistInput.trim() }}
                disabled={!playlistInput.trim()}
                onPress={enregistrerPlaylist}
                style={[styles.dialogBouton, styles.dialogBoutonPrimaire, !playlistInput.trim() && styles.dialogBoutonDesactif]}
              >
                <Text style={styles.dialogTextePrimaire}>{playlistDialog.mode === 'rename' ? 'Enregistrer' : 'Créer'}</Text>
              </Pressable>
            </View>
          </View>
        </Pressable>
      </Modal>

      {/* ─── Choix de la minuterie ───────────────────────────────────────── */}
      <Modal visible={minuterieVisible} transparent animationType="fade" onRequestClose={() => setMinuterieVisible(false)}>
        <Pressable style={styles.minuteFond} onPress={() => setMinuterieVisible(false)}>
          <View style={styles.minuteCarte}>
            <Text style={styles.minuteTitre}>Minuterie de sommeil</Text>
            {MINUTERIES.map((option) => (
              <Pressable key={option.label} accessibilityRole="button" accessibilityState={{ selected: minuterie === option.minutes }} onPress={() => choisirMinuterie(option.minutes)} style={styles.minuteOption}>
                <Text style={styles.minuteOptionTexte}>{option.label}</Text>
                {minuterie === option.minutes && <Ionicons name="checkmark" size={20} color={Colors.accent} />}
              </Pressable>
            ))}
          </View>
        </Pressable>
      </Modal>
    </SafeAreaProvider>
  );
}

const currentPaddingBottom = (courantTrack) => ({ paddingBottom: courantTrack ? 96 : 24 });

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  app: { flex: 1, backgroundColor: Colors.background },
  header: { minHeight: 72, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: Colors.border },
  logoBloc: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  logoCercle: { width: 40, height: 40, borderRadius: 20, backgroundColor: Colors.accent, alignItems: 'center', justifyContent: 'center' },
  logoTexte: { color: Colors.textPrimary, fontSize: 20, fontWeight: '800', letterSpacing: 0.2 },
  logoSous: { color: Colors.textSecondary, fontSize: 12, marginTop: 1 },
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  iconeBouton: { minWidth: 44, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22 },
  iconeBoutonPress: { backgroundColor: Colors.iconPressed },
  iconeBoutonDesactif: { opacity: 0.35 },

  accueil: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 28, paddingBottom: 64 },
  logoGrand: { width: 104, height: 104, borderRadius: 52, backgroundColor: Colors.accent, alignItems: 'center', justifyContent: 'center', marginBottom: 28 },
  accueilTitre: { color: Colors.textPrimary, fontSize: 30, lineHeight: 36, fontWeight: '800', textAlign: 'center' },
  accueilPhrase: { color: Colors.textSecondary, fontSize: 14, lineHeight: 20, textAlign: 'center', marginTop: 12, maxWidth: 320 },
  boutonPrincipal: { minHeight: 56, borderRadius: 500, backgroundColor: Colors.accent, justifyContent: 'center', alignItems: 'center', alignSelf: 'stretch', marginTop: 34 },
  boutonPrincipalPress: { backgroundColor: Colors.accentPressed, opacity: 0.92 },
  boutonPrincipalTexte: { color: Colors.onAccent, fontSize: 16, fontWeight: '800' },
  messageErreur: { color: '#FF8A8A', fontSize: 14, textAlign: 'center', marginTop: 18 },
  messageErreurCompact: { color: '#FF8A8A', fontSize: 13, marginTop: 8 },

  rechercheBloc: { paddingHorizontal: 16, paddingTop: 14, gap: 12 },
  rechercheChamp: { height: 48, borderRadius: 8, backgroundColor: Colors.surfaceRaised, flexDirection: 'row', alignItems: 'center', paddingLeft: 14, paddingRight: 4, gap: 8 },
  rechercheInput: { flex: 1, color: Colors.textPrimary, fontSize: 16, paddingVertical: 0 },
  clearRecherche: { minWidth: 38, minHeight: 38 },
  puces: { gap: 8, paddingRight: 16 },
  puce: { minHeight: 40, flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 14, borderRadius: 500, backgroundColor: Colors.surfaceRaised },
  puceActive: { backgroundColor: Colors.accent },
  puceTexte: { color: Colors.textPrimary, fontSize: 14, fontWeight: '600' },
  puceTexteActive: { color: Colors.onAccent },

  sectionTitre: { color: Colors.textPrimary, fontSize: 18, fontWeight: '800' },
  fileHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingTop: 16 },
  listContenu: { paddingHorizontal: 12, paddingTop: 10 },
  listeVide: { color: Colors.textSecondary, fontSize: 14, textAlign: 'center', marginTop: 64, paddingHorizontal: 24 },
  ligneMorceau: { height: 68, borderRadius: 8, flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 6 },
  ligneMorceauActive: { backgroundColor: Colors.surfaceRaised },
  ligneMorceauPress: { backgroundColor: Colors.rowPressed },
  vignette: { width: 44, height: 44, borderRadius: 6, backgroundColor: Colors.surfaceRaised, alignItems: 'center', justifyContent: 'center' },
  ligneTextes: { flex: 1, minWidth: 0 },
  ligneTitre: { color: Colors.textPrimary, fontSize: 16, fontWeight: '600' },
  texteAccent: { color: Colors.accent },
  ligneMeta: { color: Colors.textSecondary, fontSize: 12, marginTop: 3 },
  ligneFile: { height: 64, borderRadius: 8, flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 2 },

  recentsBloc: { paddingLeft: 16, paddingTop: 8, paddingBottom: 4 },
  recentsTitre: { color: Colors.textSecondary, fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1 },
  recentsScroll: { gap: 8, paddingVertical: 8, paddingRight: 16 },
  recentCarte: { maxWidth: 170, minHeight: 40, borderRadius: 20, paddingHorizontal: 12, backgroundColor: Colors.surfaceRaised, flexDirection: 'row', alignItems: 'center', gap: 7 },
  recentTexte: { color: Colors.textPrimary, fontSize: 13, maxWidth: 128 },

  miniLecteur: { position: 'absolute', left: 8, right: 8, bottom: 8, height: 64, borderRadius: 10, backgroundColor: Colors.surface, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8, gap: 4, borderWidth: StyleSheet.hairlineWidth, borderColor: Colors.border },
  miniZonePrincipale: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 10, minWidth: 0, minHeight: 48 },
  miniVignette: { width: 44, height: 44, borderRadius: 6, backgroundColor: Colors.surfaceRaised, alignItems: 'center', justifyContent: 'center' },
  miniTitre: { color: Colors.textPrimary, fontSize: 15, fontWeight: '700' },

  lecteur: { flex: 1, backgroundColor: Colors.background },
  lecteurHeader: { height: 56, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  lecteurSurtitre: { color: Colors.textSecondary, fontSize: 12, fontWeight: '800', letterSpacing: 1.4 },
  lecteurCorps: { flex: 1, paddingHorizontal: 22, justifyContent: 'center', paddingBottom: 8 },
  pochette: { width: '100%', aspectRatio: 1, maxHeight: 300, borderRadius: 10, backgroundColor: Colors.surfaceRaised, alignItems: 'center', justifyContent: 'center', marginBottom: 20 },
  lecteurTitre: { color: Colors.textPrimary, fontSize: 25, lineHeight: 31, fontWeight: '800' },
  lecteurArtiste: { color: Colors.textSecondary, fontSize: 14, marginTop: 6 },
  progressionBloc: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 18 },
  progressionFlex: { flex: 1 },
  temps: { color: Colors.textSecondary, fontSize: 12, fontVariant: ['tabular-nums'], width: 42, textAlign: 'center' },
  barreZone: { justifyContent: 'center', position: 'relative' },
  barrePiste: { height: 4, backgroundColor: Colors.surfaceRaised, overflow: 'hidden' },
  barreRemplissage: { height: '100%' },
  barreCurseur: { position: 'absolute', width: 12, height: 12, borderRadius: 6, backgroundColor: '#FFFFFF', top: '50%', marginTop: -6, marginLeft: -6 },

  controles: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 20 },
  playPrincipal: { width: 70, height: 70, borderRadius: 35, backgroundColor: Colors.accent, alignItems: 'center', justifyContent: 'center' },
  optionsLigne: { flexDirection: 'row', justifyContent: 'center', gap: 10, marginTop: 18 },
  optionBouton: { height: 40, minWidth: 88, borderRadius: 20, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', gap: 7, backgroundColor: Colors.surfaceRaised },
  optionTexte: { color: Colors.textPrimary, fontSize: 14, fontWeight: '700' },
  volumeBloc: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 12 },
  volumeFlex: { flex: 1 },
  volumeTexte: { color: Colors.textSecondary, width: 42, textAlign: 'right', fontSize: 12, fontVariant: ['tabular-nums'] },

  messageNotice: { color: Colors.accent, fontSize: 13, marginTop: 6 },
  playlistDetailHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10, paddingHorizontal: 12, paddingTop: 14 },
  lignePlaylistInfos: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8, minWidth: 0 },
  playlistActions: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  playlistVide: { alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, paddingTop: 70, gap: 12 },
  playlistVideTitre: { color: Colors.textPrimary, fontSize: 18, fontWeight: '800' },
  playlistVideTexte: { color: Colors.textSecondary, fontSize: 14, lineHeight: 20, textAlign: 'center', maxWidth: 280 },
  lignePlaylist: { height: 68, borderRadius: 8, flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 6 },
  actionFond: { flex: 1, backgroundColor: 'rgba(0,0,0,0.62)', alignItems: 'center', justifyContent: 'center', padding: 24 },
  actionCarte: { width: '100%', maxWidth: 420, borderRadius: 18, backgroundColor: Colors.surface, padding: 16, borderWidth: StyleSheet.hairlineWidth, borderColor: Colors.border },
  actionTitre: { color: Colors.textPrimary, fontSize: 18, lineHeight: 24, fontWeight: '800', marginBottom: 12 },
  actionSection: { color: Colors.textSecondary, fontSize: 12, fontWeight: '800', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 6, marginTop: 4 },
  actionScroll: { maxHeight: 260, marginBottom: 8 },
  actionOption: { minHeight: 48, flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 8, borderRadius: 8 },
  actionTexte: { flex: 1, minWidth: 0, color: Colors.textPrimary, fontSize: 15, fontWeight: '600' },
  actionTextes: { flex: 1, minWidth: 0 },
  actionMeta: { color: Colors.textSecondary, fontSize: 12, marginTop: 2 },
  actionVide: { color: Colors.textSecondary, fontSize: 14, paddingHorizontal: 8, paddingVertical: 16 },
  actionSeparateur: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: Colors.border, borderRadius: 0, marginTop: 4 },
  actionAnnuler: { minHeight: 46, justifyContent: 'center', borderRadius: 12, backgroundColor: Colors.surfaceRaised, marginTop: 8, alignItems: 'center' },
  actionAnnulerTexte: { color: Colors.textPrimary, fontSize: 15, fontWeight: '700' },
  actionInput: { minHeight: 50, borderRadius: 10, backgroundColor: Colors.surfaceRaised, color: Colors.textPrimary, fontSize: 16, paddingHorizontal: 14, marginBottom: 6 },
  actionErreur: { color: '#FF8A8A', fontSize: 13, marginBottom: 6 },
  dialogBoutons: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 10 },
  dialogBouton: { minHeight: 46, minWidth: 110, alignItems: 'center', justifyContent: 'center', borderRadius: 23, paddingHorizontal: 18 },
  dialogBoutonPrimaire: { backgroundColor: Colors.accent },
  dialogBoutonSecondaire: { backgroundColor: Colors.surfaceRaised },
  dialogBoutonDesactif: { backgroundColor: Colors.disabledText, opacity: 0.65 },
  dialogTextePrimaire: { color: Colors.onAccent, fontSize: 15, fontWeight: '800' },
  dialogTexteSecondaire: { color: Colors.textPrimary, fontSize: 15, fontWeight: '700' },
  minuteFond: { flex: 1, backgroundColor: 'rgba(0,0,0,0.62)', alignItems: 'center', justifyContent: 'center', padding: 24 },
  minuteCarte: { width: '100%', maxWidth: 380, borderRadius: 16, backgroundColor: Colors.surface, padding: 18, borderWidth: StyleSheet.hairlineWidth, borderColor: Colors.border },
  minuteTitre: { color: Colors.textPrimary, fontSize: 18, fontWeight: '800', marginBottom: 12 },
  minuteOption: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: 8, paddingHorizontal: 10 },
  minuteOptionTexte: { color: Colors.textPrimary, fontSize: 16 },
});
