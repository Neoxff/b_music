package com.bmusic.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * B Music — GlassSurface (docs/design/06-liquid-leger.md, 07-animations-liquid.md).
 *
 * Composant UNIQUE autorisé à faire du flou (whitelist check-glass-policy.sh).
 * Selon GlassPolicy.isActive() : vrai flou d'arrière-plan (API 31+ layer) ou
 * repli opaque (surface pleine design system, même mise en page).
 * INTERDITS : aucun effet vitreux (pas de reflet, dégradé de lueur, réfraction).
 *
 * Le flou autorisé vit uniquement ici (plus le helper de pochette pré-floutée
 * de l'étape 4). CI stricte hors whitelist. Transparence structurée via
 * GlassTokens (opacité d'écran et tokens).
 */
@Composable
fun GlassSurface(
    level: GlassTokens.Level = GlassTokens.Level.Standard,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val active = GlassPolicy.isActiveComposed(
        mode = GlassMode.AUTO,
        context = LocalContext.current,
    )
    val alpha = GlassTokens.opacity(level, dark)
    val shape = RoundedCornerShape(GlassTokens.radius(level))
    val edge = if (dark) {
        if (level == GlassTokens.Level.Prominent) GlassTokens.EdgeProminentDark else GlassTokens.EdgeDark
    } else {
        if (level == GlassTokens.Level.Prominent) GlassTokens.EdgeProminentLight else GlassTokens.EdgeLight
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = if (active) 0.5f else 0f))
            .border(1.dp, edge, shape),
        content = content
    )
}
