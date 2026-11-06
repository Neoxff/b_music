# Sonore — Design system (lecteur audio local)

> Nom de travail : **Sonore**. Produit local (fichiers MP3/FLAC de l'utilisateur), ergonomie « Spotify-like » sans en être un : aucun logo, nom, vert #1DB954 ni icône propriétaire. Accent **ambre #E8A33D**, icônes **Lucide** (ISC, libres), police **Inter** (SIL OFL) avec repli système. Aucune ressource externe chargée (contrainte sécurité : pas de CDN/police distante → tout est embarqué ou système).

Fichier source des tokens : `design/tokens.css` (variables CSS). Maquette interactive : `design/maquette.html`.

## 1. Design tokens (extrait JSON)

```json
{
  "color": {
    "bg": { "base": "#121212", "elevated": "#181818", "card": "#1f1f1f",
            "cardHover": "#262626", "pressed": "#2e2e2e", "border": "#2a2a2a" },
    "accent": { "base": "#e8a33d", "hover": "#f2b355", "pressed": "#cf8f2b",
                "soft": "rgba(232,163,61,.14)", "onAccent": "#1a1206" },
    "text": { "primary": "#ffffff", "secondary": "#b3b3b3",
              "muted": "#8a8a8a", "disabled": "#5f5f5f" }
  },
  "font": { "family": "Inter, system-ui, sans-serif",
            "scale": { "xs": 12, "sm": 13, "md": 14, "lg": 16, "xl": 20, "2xl": 24, "3xl": 32 },
            "weight": { "regular": 400, "medium": 500, "semibold": 600, "bold": 700 } },
  "space": [4, 8, 12, 16, 20, 24, 32, 48],
  "radius": { "sm": 4, "md": 8, "lg": 12, "pill": 999 },
  "shadow": { "card": "0 8px 24px rgba(0,0,0,.45)", "bar": "0 -8px 24px rgba(0,0,0,.35)" },
  "motion": { "fast": "150ms", "base": "200ms", "slow": "250ms",
              "ease": "cubic-bezier(.2,0,0,1)" },
  "target": { "min": 40 }
}
```

Contrastes mesurés sur `--bg-base` : `#fff` 18.9:1 · `--text-secondary` 7.7:1 · `--text-muted` 5.0:1 · accent `#e8a33d` ≈ 8.5:1 (texte) ; `--on-accent` sur accent ≈ 9:1. **AA respecté partout** ; `--text-disabled` réservé aux éléments non interactifs.

## 2. Composants (états)

| Composant | Rôle | États |
|---|---|---|
| `AppShell` | sidebar + main + `PlayerBar` (grille fixe) | sidebar étendue / compacte (`--sidebar-w-min`, ≤900px) |
| `SidebarNavItem` | navigation (Accueil, Recherche, File, Bibliothèque) | default · hover (`--bg-card`) · actif (accent-soft + barre 3px accent à gauche) · focus-visible (ring) |
| `Card` | pochette album/playlist/artiste | default · hover (élévation +150ms, bouton play visible) · focus-visible (ring) · actif (pressed) |
| `PlayFab` | bouton play circulaire ambre sur carte | caché (opacity 0, translateY 8px) → visible au hover/focus-within (200ms) |
| `Button` | primaire (pill ambre / texte `--on-accent`) · secondaire (contour) | hover · pressed · disabled (`--text-disabled`, cursor not-allowed) · focus-visible |
| `IconButton` | précédent/lecture/suivant, aléatoire, répéter, volume | hover · **actif** (accent + point 4px sous l'icône pour repeat/shuffle) · disabled · focus-visible · cible ≥40px |
| `TrackRow` | ligne de piste (recherche, album, file) | default · hover · **en cours** (titre accent + icône animée) · focus-visible · sélection multiple (checkbox) |
| `SeekBar` / `VolumeSlider` | progression / volume | remplissage accent, piste `--bg-pressed`, curseur visible au hover/focus, clavier ←/→ (seek) et ↑/↓ (volume), `role="slider"` + `aria-value*` |
| `Chip` | filtres (« Tous », « Artistes », « Albums »…) | default · actif (fond blanc / texte noir, pattern Spotify-like mais neutre) · focus-visible |
| `SearchInput` | champ de recherche | default · focus (bordure accent 1px) · rempli (bouton × pour effacer) · placeholder `--text-muted` |
| `Panel` | panneau File d'attente / en-tête d'album | `--bg-elevated`, `--radius-lg`, `--shadow-card` |
| `EmptyState` | bibliothèque vide, recherche sans résultat, file vide | icône Lucide 48px `--text-muted` + titre + CTA |
| `Toast` | action confirmée (« Ajouté à la file ») | auto-dismiss 3s, `role="status"` |

## 3. Écrans (MVP)

### 3.1 Accueil / Bibliothèque
Grille responsive `repeat(auto-fill, minmax(160px, 1fr))` de `Card` (pochette carrée `--radius-md`, titre 1 ligne ellipsis, sous-titre type « Album · Artiste » ou « Playlist »). Sections : « Écoutés récemment » (scroll horizontal), « Vos albums », « Vos playlists ». En-tête avec `Chip`s de filtre. État vide → `EmptyState` « Votre bibliothèque est vide — Importez vos fichiers… » + bouton **Importer**.

### 3.2 Vue album / playlist
En-tête : grande pochette 192px (`--shadow-card`), sur-titre « ALBUM » (`--text-xs`, letter-spacing), titre `--text-2xl/3xl` bold, méta `--text-sm` muted (« Artiste · 2023 · 12 titres, 47 min »). Actions : gros bouton play accent (56px) + `IconButton` (cœur, plus d'options…). Corps : liste de `TrackRow` (# / titre / durée), ligne en cours surlignée. Clic droit / bouton contexte → menu (Lire ensuite, Ajouter à la file…).

### 3.3 Recherche
`SearchInput` large (max 480px) + `Chip`s (Tous, Titres, Albums, Artistes, Playlists). Résultats : grille de `Card` (« Meilleur résultat » en carte large optionnelle) + liste de `TrackRow`. Aucun résultat → `EmptyState` + suggestion. Recherche **locale** uniquement (index des tags fichier).

### 3.4 File d'attente
Panneau latéral droit (ou onglet plein écran en compact) listant la piste en cours (« En écoute », accent) puis « À suivre » : `TrackRow` réordonnable (glisser-déposer en souris, boutons ↑/↓ au clavier pour a11y), retrait via bouton ×, total en pied (« 6 titres · 28 min »). File vide → `EmptyState`.

### 3.5 Barre de lecture (fixe, `--playerbar-h`, `grid-template-columns: 1fr 2fr 1fr`)
- **Gauche** : pochette 56px (`--radius-sm`), titre (1 ligne, ellipsis) / artiste (`--text-sm` muted), bouton cœur (toggle like).
- **Centre** : `IconButton` aléatoire → précédent → **lecture/pause** (bouton accent 40px, icône noire) → suivant → répéter (badge « 1 » en mode répéter-une). En dessous, `SeekBar` avec temps écoulés / restants `--text-xs` muted ; seek au clic et au drag, ←/→ au clavier.
- **Droite** : icône file d'attente (bascule panneau 3.4), icône volume + `VolumeSlider`, bouton plein écran **absent du MVP** (voir Idées futures).
- Périphérie : filet séparateur `--border-subtle`, `--shadow-bar`.

## 4. Micro-interactions (150–250 ms, `--ease-standard`)
- Survol carte : `transform: translateY(-2px)` + `--shadow-card` + bouton play fade/slide-in (`--motion-base`).
- Bouton lecture : `scale(.97)` au pressed ; icône lecture↔pause en crossfade 150 ms.
- `TrackRow` hover : fond `--bg-card` ; changement de piste : barre de progression réinitialisée en `--motion-slow`.
- Chips : transition couleurs 150 ms. `:focus-visible` partout via `--focus-ring` (double anneau base+accent, visible sur fond clair comme sombre).
- `prefers-reduced-motion: reduce` → toutes les durées à 0.

## 5. Accessibilité
Cibles ≥40px (44px visé pour la barre de lecture) · focus visible systématique · ordre de tabulation logique (sidebar → main → playerbar) · pièges de focus dans les menus avec Échap pour fermer · `aria-label` sur tous les `IconButton` · `aria-current="page"` dans la sidebar · sliders en `role="slider"` avec flèches clavier · contraste AA vérifié (§1) · hiérarchie titres h1 (écran) → h2 (sections).

## 6. Décisions et compromis
- **Ambre #E8A33D plutôt que teal/violet** : chaleur « hi-fi », ≥8:1 sur fond sombre, aucun risque de confusion avec le vert Spotify (#1DB954) ni le rouge Apple Music ; compromis : moins « tech », assumé pour un produit local grand public.
- **Inter + pile système, zéro asset distant** : conformité sécurité immédiate (pas de CDN), compromis : pas de variable font Display — le 32px reste en 600, pas besoin de graisse spéciale.
- **Pochettes = dégradés CSS générés depuis le hash du fichier** (maquette) : aucun asset externe/copyright ; compromis : pas de vraies jaquettes dans la démo, branchées plus tard sur les tags ID3 embarqués.
- **File d'attente en panneau latéral, pas en écran** : évite un 5ᵉ écran et préserve le contexte de lecture ; compromis : largeur réduite <1280px (repli en onglet).
- **Niveau de finition MVP volontairement "plat"** (pas de blur/glassmorphism) : coût perf nul sur machines modestes, cohérence sombre ; l'élégance vient du rythme 4/8px et des transitions courtes.

## 7. Idées futures (hors MVP, non conçues)
Paroles synchronisées · égaliseur · thèmes clairs/personnalisables · mode "écran de veille" · intégration serveur DLNA · scrobbling local Last.fm.
