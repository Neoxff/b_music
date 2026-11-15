package com.bmusic.app.ui.screen.downloader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bmusic.app.R
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.ui.theme.GlassMode
import com.bmusic.app.ui.theme.GlassPolicy
import com.bmusic.app.ui.theme.GlassSurface
import com.bmusic.app.ui.theme.GlassTokens
import com.bmusic.app.ui.theme.Motion
import com.bmusic.app.ui.theme.rememberReducedMotion
import com.bmusic.app.util.Preferences

/**
 * Écran « Télécharger » — deux onglets : **Lien** (par défaut) et **Historique**.
 *
 * Spécifications : `docs/design/11-onglets-lien-historique.md` §1 (structure),
 * §3 (onglet Lien, zone de saisie centrée), `docs/design/13-liquid-glass-sobre.md`
 * §1 (où le verre est autorisé) et §3 (animations via `Motion.kt` uniquement).
 *
 * Règles tenues ici :
 * - la zone de saisie est **centrée horizontalement et verticalement** à l'état
 *   vide, largeur plafonnée à 560 dp, marges 20 dp, champ 56 dp / rayon 28 dp ;
 * - elle **reste visible** quand le clavier s'ouvre (`imePadding`) et elle
 *   **remonte en ressort** quand l'aperçu apparaît ;
 * - le verre n'est posé que sur la barre d'onglets, la zone de saisie, la carte
 *   d'aperçu et la mini-barre de progression ;
 * - les animations passent **exclusivement** par les tokens de `Motion.kt` ;
 * - jamais d'animation du rayon de flou ni de l'opacité pendant un défilement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    vm: DownloadViewModel = org.koin.compose.viewmodel.koinViewModel(),
    onBack: () -> Unit = {},
) {
    val state by vm.link.collectAsState()
    val tab by vm.tab.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val clipboard = LocalClipboardManager.current
    val glassMode = remember { GlassPolicy.fromPreference(Preferences.blurEffects) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Télécharger") },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Retour" }) {
                            Icon(painterResource(R.drawable.ic_close_24dp), contentDescription = null)
                        }
                    },
                )
                GlassTabBar(
                    selected = tab,
                    onSelect = vm::selectTab,
                    glassMode = glassMode,
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> LienTab(state, vm, clipboard, glassMode)
                else -> HistoriqueTab(downloads, vm, glassMode)
            }
        }
    }
}

/**
 * Barre de deux onglets en verre : « Lien » (par défaut) et « Historique ».
 * Indicateur de 3 dp en accent qui glisse en ressort (`Motion.SpringSnappy`).
 */
@Composable
private fun GlassTabBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    glassMode: GlassMode,
) {
    val reduced = rememberReducedMotion()
    val offset by animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = if (reduced) snap() else Motion.SpringSnappy,
        label = "tabIndicator",
    )
    var barWidth by remember { mutableStateOf(0) }

    GlassSurface(
        level = GlassTokens.Level.Standard,
        mode = glassMode,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Box(Modifier.fillMaxSize().onSizeChanged { barWidth = it.width }) {
            // Indicateur 3 dp en accent : demi-largeur de barre, glissé en ressort.
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.5f)
                    .height(3.dp)
                    .graphicsLayer { translationX = offset * barWidth / 2f }
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(Modifier.fillMaxSize()) {
                GlassTabItem(
                    label = "Lien",
                    iconRes = R.drawable.ic_link_24dp,
                    selected = selected == 0,
                    onClick = { onSelect(0) },
                    modifier = Modifier.weight(1f),
                )
                GlassTabItem(
                    label = "Historique",
                    iconRes = R.drawable.ic_history_24dp,
                    selected = selected == 1,
                    onClick = { onSelect(1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GlassTabItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Onglet $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/**
 * Onglet LIEN — zone de saisie centrée.
 *
 * À l'état vide, le bloc est centré horizontalement **et** verticalement dans
 * l'espace disponible. Quand l'aperçu apparaît, le bloc **remonte en ressort**
 * et la carte d'aperçu s'affiche dessous ; il revient au centre à l'effacement.
 */
@Composable
private fun LienTab(
    state: DownloadViewModel.LinkState,
    vm: DownloadViewModel,
    clipboard: ClipboardManager,
    glassMode: GlassMode,
) {
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current
    val risePx by animateFloatAsState(
        targetValue = if (state.info != null) {
            with(density) { (-120).dp.toPx() }
        } else {
            0f
        },
        animationSpec = if (reduced) snap() else Motion.SpringSnappy,
        label = "lienRise",
    )

    Box(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp)
                .graphicsLayer { translationY = risePx },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_link_24dp),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Coller un lien",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Zone de saisie — surface verre (site autorisé : §1(c) de la spec).
            GlassSurface(
                level = GlassTokens.Level.Standard,
                mode = glassMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.url,
                        onValueChange = vm::onUrlChange,
                        placeholder = { Text("https://…") },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        trailingIcon = {
                            if (state.url.isNotEmpty()) {
                                IconButton(onClick = { vm.onUrlChange("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close_24dp),
                                        contentDescription = "Effacer le lien",
                                    )
                                }
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                val paste = clipboard.getText()?.text?.trim().orEmpty()
                                if (paste.isNotEmpty()) vm.onUrlChange(paste)
                            },
                        ) { Text("Coller") }
                        Button(
                            onClick = vm::resolveLink,
                            enabled = !state.busy && state.url.isNotBlank(),
                        ) { Text("Analyser") }
                    }
                    Text(
                        text = "Collez un lien YouTube, TikTok, Instagram…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.busy) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Text(
                        text = "  Vérification du lien…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Carte d'aperçu — surface verre (site autorisé : §1(b) de la spec).
            state.info?.let { info ->
                GlassSurface(
                    level = GlassTokens.Level.Standard,
                    mode = glassMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(info.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${info.author} · ${info.durationMs / 1000} s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = state.audioVideo == StreamKind.AUDIO,
                                onClick = { vm.selectKind(StreamKind.AUDIO) },
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                label = { Text("Audio") }
                            )
                            SegmentedButton(
                                selected = state.audioVideo == StreamKind.VIDEO,
                                onClick = { vm.selectKind(StreamKind.VIDEO) },
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                label = { Text("Vidéo") }
                            )
                        }
                        Text(
                            text = "Qualité : ${state.quality ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = vm::enqueue,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text("Télécharger") }
                        if (state.duplicate) {
                            Text(
                                text = "Déjà en cours — le téléchargement ne relancera rien.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Onglet HISTORIQUE — liste locale, **aucune requête réseau** à l'affichage.
 * Regroupement par date et filtres : à venir (`feat/dl-historique`).
 */
@Composable
private fun HistoriqueTab(
    downloads: List<DownloadEntity>,
    vm: DownloadViewModel,
    glassMode: GlassMode,
) {
    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_history_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Aucun téléchargement pour l'instant",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(downloads, key = { it.id }) { e ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(e.title, style = MaterialTheme.typography.titleSmall)
                // Mini-barre de progression — surface verre (site autorisé : §1(f)).
                GlassSurface(
                    level = GlassTokens.Level.Minimal,
                    mode = glassMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LinearProgressIndicator(
                        progress = {
                            if (e.totalBytes > 0) {
                                (e.progressBytes.toFloat() / e.totalBytes).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row {
                    TextButton(onClick = { vm.pause(e.id) }) { Text("Pause") }
                    TextButton(onClick = { vm.resume(e.id) }) { Text("Reprendre") }
                    TextButton(onClick = { vm.cancel(e.id) }) { Text("Annuler") }
                    if (e.state == DownloadState.COMPLETED) {
                        Text(
                            text = "Terminé",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
