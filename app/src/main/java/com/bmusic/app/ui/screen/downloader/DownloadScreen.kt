package com.bmusic.app.ui.screen.downloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bmusic.app.R
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.ui.theme.Motion
import com.bmusic.app.ui.theme.rememberReducedMotion

/**
 * Étape 5 : socle de l'écran Télécharger — 2 onglets (Recherche / Lien).
 * Onglet Recherche : placeholder "bientôt disponible" jusqu'à l'étape 6.
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Télécharger") },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Retour" }) {
                            // flèche retour — icône système (téléchargeur, pas de glass)
                            Text("←")
                        }
                    },
                    actions = {
                        Text(
                            "↓",
                            modifier = Modifier.padding(end = 12.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
                TabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == 0,
                        onClick = { vm.selectTab(0) },
                        text = { Text("⌕ Recherche") },
                        modifier = Modifier.semantics { contentDescription = "Onglet Recherche" },
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { vm.selectTab(1) },
                        text = { Text("⛓ Lien") },
                        modifier = Modifier.semantics { contentDescription = "Onglet Lien" },
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (tab == 0) {
                PlaceholderRecherche()
            } else {
                LienTab(state, vm, clipboard)
            }
            if (downloads.isNotEmpty()) {
                Text("Téléchargements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                downloads.take(3).forEach { e ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(e.title, style = MaterialTheme.typography.titleSmall)
                            LinearProgressIndicator(
                                progress = { if (e.totalBytes > 0) (e.progressBytes.toFloat() / e.totalBytes).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row {
                                TextButton(onClick = { vm.pause(e.id) }) { Text("⏸ Pause") }
                                TextButton(onClick = { vm.resume(e.id) }) { Text("▶ Reprendre") }
                                TextButton(onClick = { vm.cancel(e.id) }) { Text("✕ Annuler") }
                                if (e.state == com.bmusic.app.downloader.data.DownloadState.COMPLETED) {
                                    Text("Terminé", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderRecherche() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Recherche — bientôt disponible (étape 6)")
    }
}

@Composable
private fun LienTab(state: DownloadViewModel.LinkState, vm: DownloadViewModel, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Coller un lien", style = MaterialTheme.typography.titleMedium)
        // DownloadTextField opaque, 56 dp
        OutlinedTextField(
            value = state.url,
            onValueChange = vm::onUrlChange,
            label = { Text("https://…") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            singleLine = true,
            supportingText = {
                Text("SoundCloud, Bandcamp, Vimeo, YouTube…")
            },
            trailingIcon = {
                if (state.url.isNotEmpty()) {
                    IconButton(onClick = { vm.onUrlChange("") }) { Text("×") }
                }
            }
        )
        Row {
            TextButton(onClick = {
                val paste = clipboard.getText()?.text?.trim().orEmpty()
                if (paste.isNotEmpty()) vm.onUrlChange(paste)
            }) { Text("Coller") }
            Button(onClick = vm::resolveLink) { Text("Aperçu") }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (state.busy) CircularProgressIndicator(Modifier)
        state.info?.let { info ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Text(info.title, style = MaterialTheme.typography.titleMedium)
                    Text(info.author, style = MaterialTheme.typography.bodyMedium)
                    Text("Durée : ${(info.durationMs / 1000)} s", style = MaterialTheme.typography.bodySmall)
                    // Segmented control Audio / Vidéo
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = state.audioVideo == StreamKind.AUDIO,
                            onClick = { vm.selectKind(StreamKind.AUDIO) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            label = { Text("Audio") }
                        )
                        SegmentedButton(
                            selected = state.audioVideo == StreamKind.VIDEO,
                            onClick = { vm.selectKind(StreamKind.VIDEO) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                            label = { Text("Vidéo") }
                        )
                    }
                    Text("Qualité : ${state.quality ?: "—"}", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = vm::enqueue,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) { Text("Télécharger") }
                    if (state.duplicate) {
                        Text("Déjà en cours — téléchargez ne re-téléchargera pas automatiquement.")
                    }
                }
            }
        }
    }
}

// koinViewModel provient de org.koin.compose.koinViewModel — aucun helper local.
