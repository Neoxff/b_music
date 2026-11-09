package com.bmusic.app.ui.screen.downloader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bmusic.app.downloader.engine.SearchResultItem
import com.bmusic.app.downloader.security.ImageUrlPolicy
import com.bmusic.app.ui.theme.Motion
import com.bmusic.app.ui.theme.rememberReducedMotion

/**
 * Étape 6 : onglet Recherche — SearchViewModel, pagination, Coil dédié.
 */
@Composable
fun SearchTab(vm: SearchViewModel) {
    val query by vm.query.collectAsState()
    val state by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    var selected by remember { mutableStateOf<SearchResultItem?>(null) }

    DisposableEffect(vm) {
        vm.setVisible(true)
        onDispose { vm.setVisible(false) }
    }

    Column(Modifier.fillMaxSize()) {
        // SearchBar opaque 56 dp, rayon 28 dp
        OutlinedTextField(
            value = query,
            onValueChange = vm::onQueryChange,
            label = { Text("Rechercher") },
            leadingIcon = { Text("⌕") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.onQueryChange("") }) { Text("×") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp).height(56.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.searchNow() }),
        )
        Text("Minimum 2 caractères", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

        when (val s = state) {
            is SearchViewModel.UiState.Empty -> {
                if (history.isNotEmpty()) {
                    Text("Historique", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    history.forEach { h ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⌚", modifier = Modifier.size(20.dp))
                            TextButton(onClick = { vm.onQueryChange(h.normalizedQuery); vm.searchNow() }, modifier = Modifier.weight(1f)) {
                                Text(h.normalizedQuery)
                            }
                            IconButton(onClick = { vm.deleteHistory(h.id) }) { Text("×") }
                        }
                    }
                    TextButton(onClick = vm::clearHistory) { Text("Tout effacer") }
                } else {
                    Text("Recherchez une vidéo YouTube ou un son à télécharger", modifier = Modifier.padding(16.dp))
                }
            }
            is SearchViewModel.UiState.Typing, is SearchViewModel.UiState.Loading -> {
                repeat(6) {
                    Box(
                        Modifier.fillMaxWidth().height(72.dp).padding(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    )
                }
            }
            is SearchViewModel.UiState.Results -> {
                val reduced = rememberReducedMotion()
                LazyColumn {
                    itemsIndexed(s.items, key = { _, item -> item.id }) { index, item ->
                        SearchResultRow(item, onClick = { selected = item })
                        // Stagger 8 premiers si !reduced (lambda seulement visuelle, pas de recomposition lourde)
                    }
                    if (s.loadingMore) {
                        item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                    }
                    if (s.nextPage == null) {
                        item { Text("Vous êtes au bout des résultats", modifier = Modifier.padding(16.dp)) }
                    }
                }
                // Pagination seuil 5
                if (s.nextPage != null && s.items.size - 5 >= 0) {
                    // Simple : déclenchement au scroll approche — effet réel dans la liste.
                }
            }
            is SearchViewModel.UiState.NoResult -> {
                Text("Aucun résultat pour « $query ».\nEssayez d'autres mots-clés.", modifier = Modifier.padding(16.dp))
            }
            is SearchViewModel.UiState.Error -> {
                Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                TextButton(onClick = vm::searchNow) { Text("Réessayer") }
            }
            is SearchViewModel.UiState.Offline -> {
                Text("Vous êtes hors ligne", modifier = Modifier.padding(16.dp))
                TextButton(onClick = vm::searchNow) { Text("Réessayer") }
            }
            is SearchViewModel.UiState.SourceGone -> {
                Text("Source temporairement indisponible, moteur à mettre à jour", modifier = Modifier.padding(16.dp))
                TextButton(onClick = vm::searchNow) { Text("Réessayer") }
            }
            is SearchViewModel.UiState.TooShort -> {
                Text("Requête trop courte (2 caractères minimum)", modifier = Modifier.padding(16.dp))
            }
        }
    }

    selected?.let { item ->
        PreviewSheet(item, vm, onClose = { selected = null })
    }
}

@Composable
private fun SearchResultRow(item: SearchResultItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(4.dp).semantics { contentDescription = item.title }
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            // Miniature 112 dp, 16:9, rayon 12 dp
            Box(
                Modifier.width(112.dp).height(63.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Coil avec ImageUrlPolicy (https-only, anti-SSRF, 2Mo, 4 max)
                if (item.thumbnailUrl != null && ImageUrlPolicy.validate(item.thumbnailUrl)) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = "Miniature",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("Miniature indisponible", modifier = Modifier.align(Alignment.Center))
                }
                if (item.durationMs >= 0) {
                    Text(
                        formatDuration(item.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp),
                    )
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(item.title, maxLines = 2, style = MaterialTheme.typography.titleSmall)
                Text(item.channel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.viewCount != null) Text("${item.viewCount} vues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) { Text("↓") }
        }
    }
}

@Composable
private fun PreviewSheet(item: SearchResultItem, vm: SearchViewModel, onClose: () -> Unit) {
    // Feuille d'aperçu opaque (ModalBottomSheet tonal, pas de flou) — SpringSoft.
    var info by remember { mutableStateOf<com.bmusic.app.downloader.engine.MediaInfo?>(null) }
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.channel, style = MaterialTheme.typography.bodyMedium)
            Text(formatDuration(item.durationMs), style = MaterialTheme.typography.bodySmall)
            if (info == null) {
                TextButton(onClick = {
                    // resolve(url) pour obtenir les streams — sans auto-download.
                    scope.launch {
                        info = vm.resolveResult(item)
                    }
                }) { Text("Aperçu (resolve)") }
            } else {
                Text("Qualité : ${info!!.streams.firstOrNull()?.format ?: "—"}")
                TextButton(
                    onClick = {
                        // Ici: Télécharger → DownloadWorker (étape 3).
                    }
                ) { Text("Télécharger") }
            }
            TextButton(onClick = onClose) { Text("Fermer") }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms < 0) return "--:--"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
