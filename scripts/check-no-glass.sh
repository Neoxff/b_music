#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
hits=$(grep -rInE 'RenderEffect|BlurMaskFilter|Modifier\.blur|Haze|HazeState|glassmorphism' app/src/main/java --include='*.kt' || true)
[ -z "$hits" ] || { echo "❌ Blur interdit détecté:"; echo "$hits"; echo "Voir docs/design/01-design-system.md (zéro liquid glass)"; exit 1; }
echo "✓ aucun blur/glass (RenderEffect/BlurMaskFilter/haze) dans app/src."
