package com.bmusic.app.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Écran Diagnostics (debug uniquement) — mesures sans adb.
 * Aucune donnée personnelle : aucun titre, chemin ou URI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    jank: JankStatsSnapshot,
    memory: MemorySnapshot,
    device: DeviceInfoSnapshot,
    blur: BlurStateSnapshot,
    scenarioRunning: Boolean,
    scenarioName: String,
    scenarioCount: Int,
    onStartScenario: (String) -> Unit,
    onEndScenario: () -> Unit,
    onResetJank: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scenarioLabel by remember { mutableStateOf("défilement bibliothèque") }
    var expandedScenarios by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Diagnostics (debug)", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Images perdues / durée (JankStats)", style = MaterialTheme.typography.titleMedium)
                Text("Images totales : ${jank.totalFrames}")
                Text("Images lentes : ${jank.slowFrames} (${"%.2f".format(jank.slowPercent)} %)")
                Text("Pire durée : ${"%.2f".format(jank.worstMs)} ms")
                Text("Durée moyenne : ${"%.2f".format(jank.avgMs)} ms")
                Text("Taux de rafraîchissement : ${device.refreshRateHz} Hz")
                Button(onClick = onResetJank) { Text("Réinitialiser les compteurs") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Mémoire", style = MaterialTheme.typography.titleMedium)
                Text("PSS : ${memory.pssKb} Ko")
                Text("Java heap : ${memory.javaHeapKb} Ko")
                Text("Native heap : ${memory.nativeHeapKb} Ko")
                Text("PSS pic session : ${memory.pssPeakKb} Ko")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Appareil", style = MaterialTheme.typography.titleMedium)
                Text("API : ${device.apiLevel} | Modèle : ${device.model}")
                Text("Low RAM device : ${device.isLowRam}")
                Text("Économiseur batterie : ${device.powerSave}")
                Text("Fréquence d'écran : ${device.refreshRateHz} Hz")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("État du flou", style = MaterialTheme.typography.titleMedium)
                Text("Mode : ${if (blur.active) "flou" else "opaque"}")
                Text("Raison : ${blur.reason}")
                Text("Surfaces floutées actives : ${blur.surfaceCount}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Scénarios de mesure", style = MaterialTheme.typography.titleMedium)
                Text("Nom du scénario (valeurs entre Début/Fin) :")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("défilement bibliothèque", "défilement recherche", "transition mini → plein écran", "lecture 30 s").forEach { label ->
                        FilterChip(
                            selected = scenarioLabel == label,
                            onClick = { scenarioLabel = label },
                            label = { Text(label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onStartScenario(scenarioLabel) }, enabled = !scenarioRunning) {
                        Text("Début de scénario")
                    }
                    Button(onClick = onEndScenario, enabled = scenarioRunning) {
                        Text("Fin de scénario")
                    }
                }
                if (scenarioRunning) Text("En cours : $scenarioName")
                Text("Scénarios enregistrés : $scenarioCount")
                TextButton(onClick = { expandedScenarios = !expandedScenarios }) {
                    Text(if (expandedScenarios) "Masquer les détails" else "Afficher les détails")
                }
            }
        }

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exporter le rapport (partage Android)")
        }
        Text(
            "Rapport texte (valeurs + horodatage + version). Aucune donnée personnelle.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
