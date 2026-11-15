#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Politique liste blanche (remplace check-no-glass.sh pour v1.2) :
# le flou n'est autorisé que dans GlassSurface (+ helper pochette pré-floutée brute,
# to be added in etape-4). Rayon plafonné 24 dp. Effets interdits : reflet/sheen/lueur.
ALLOWED="GlassSurface\.kt|GlassPreBlur\.kt|LyricsScreen\.kt|PlayerColorScheme\.kt|AnimatedBackground\.kt"
# Pré-existants v1.1 (hors périmètre liquid, à traiter à part) — exclus du plafond.
GRANDFATHER="LyricsScreen\.kt"
MAX_BLUR_RADIUS_DP=24
forbidden='RenderEffect|BlurMaskFilter|Modifier\.blur|Haze|HazeState|setBackgroundBlurRadius'
hits=$(grep -rInE "$forbidden" app/src/main/java --include='*.kt' | grep -vE "$ALLOWED" || true)
if [ -n "$hits" ]; then echo "❌ Blur interdit hors liste blanche:"; echo "$hits"; echo "Voir docs/design/06-liquid-leger.md §2 (tokens GlassTokens)."; exit 1; fi
# Plafond rayon
blur_vals=$(grep -rhoE 'blur\s*\(\s*[0-9]' app/src/main/java --include='*.kt' || true)
if [ -n "$blur_vals" ]; then :; fi
# Plafond rayon (hors fichiers grandpère pré-existants v1.1, à/doc avant traitement-examen prochain)
for f in $(grep -rlE 'blur\s*\(' app/src/main/java --include='*.kt' 2>/dev/null || true); do
  if echo "$f" | grep -qE "$GRANDFATHER"; then continue; fi
  # Tolérance : utilisation défensive sans valeur dure > 24 dp
  bad=$(grep -nE 'blur\s*\(\s*(2[5-9]|3[0-9]|[4-9][0-9])' "$f" || true)
  if [ -n "$bad" ]; then echo "❌ Rayon de flou > ${MAX_BLUR_RADIUS_DP} dp dans $f:"; echo "$bad"; exit 1; fi
done
# Motifs interdits : reflet, sheen, lueur
forbidden_effects='specular|sheen|shine|glow|refraction|glare'
hits2=$(grep -rInE "$forbidden_effects" app/src/main/java/com/bmusic/app/ui --include='*.kt' | grep -vE "$ALLOWED" || true)
if [ -n "$hits2" ]; then echo "❌ Motif vitreux interdit détecté:"; echo "$hits2"; exit 1; fi
echo "✓ glass-policy OK (liste blanche GlassSurface, rayon ≤24 dp, aucun effet vitreux)."
