package com.bmusic.app.ui.screen.downloader

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.bmusic.app.R
import com.bmusic.app.downloader.trace.DownloadTrace
import com.bmusic.app.downloader.trace.DownloadTraceStore
import com.bmusic.app.downloader.trace.TraceEvent
import com.bmusic.app.downloader.trace.TraceResult
import com.bmusic.app.downloader.trace.TraceSession

/**
 * Écran « Journal » — présent en **debug ET release** : c'est le seul moyen
 * de diagnostic sans adb.
 *
 * Contenu : les 50 dernières tentatives, le détail des étapes d'une tentative,
 * les boutons « Partager » (rapport texte) et « Effacer ».
 *
 * Aucune donnée personnelle : seuls le domaine et le hash du lien apparaissent.
 */
class DownloadJournalDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var sessions by remember { mutableStateOf(DownloadTrace.sessions()) }
                DownloadJournalContent(
                    sessions = sessions,
                    onShare = { shareReport(it) },
                    onClear = {
                        DownloadTraceStore.clear(requireContext())
                        sessions = DownloadTrace.sessions()
                    },
                    onClose = { dismiss() },
                )
            }
        }
    }

    private fun shareReport(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.downloader_journal_title))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            startActivity(Intent.createChooser(intent, getString(R.string.downloader_journal_share)))
        }
    }

    companion object {
        const val TAG = "DownloadJournalDialog"
    }
}

/** Contenu Compose du Journal (réutilisé par le dialogue et par les tests). */
@Composable
fun DownloadJournalContent(
    sessions: List<TraceSession> = DownloadTrace.sessions(),
    onShare: (String) -> Unit = {},
    onClear: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    var selected by remember { mutableStateOf<TraceSession?>(null) }
    val session = selected

    if (session != null) {
        TraceSessionDetail(
            session = session,
            onBack = { selected = null },
            onShare = onShare,
        )
        return
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.downloader_journal_title),
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Actions en en-tête : toujours atteignables, même quand la liste
        // est longue ou quand l'écran est petit.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onShare(DownloadTrace.report()) }) {
                Text(stringResource(R.string.downloader_journal_share))
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.downloader_journal_clear))
            }
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.downloader_journal_close))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

        if (sessions.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.downloader_journal_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(sessions, key = { it.id }) { s ->
                    JournalSessionRow(session = s, onClick = { selected = s })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun JournalSessionRow(session: TraceSession, onClick: () -> Unit) {
    val failure = session.firstFailure()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = session.domain ?: stringResource(R.string.downloader_journal_unknown_domain),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(
                R.string.downloader_journal_session_summary,
                session.linkHash ?: "-",
                session.events.size,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        if (failure != null) {
            Text(
                text = failure.errorCode?.name
                    ?: stringResource(R.string.downloader_journal_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TraceSessionDetail(
    session: TraceSession,
    onBack: () -> Unit,
    onShare: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = session.domain ?: stringResource(R.string.downloader_journal_unknown_domain),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.downloader_journal_hash, session.linkHash ?: "-"),
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onShare(DownloadTrace.report()) }) {
                Text(stringResource(R.string.downloader_journal_share))
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.downloader_journal_close))
            }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            items(session.events, key = { it.step.code + it.atEpochMs }) { e ->
                TraceEventRow(e)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TraceEventRow(event: TraceEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = event.step.code,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (event.result == TraceResult.OK) {
                stringResource(R.string.downloader_journal_ok)
            } else {
                stringResource(R.string.downloader_journal_failed)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text(text = "${event.durationMs} ms", style = MaterialTheme.typography.bodySmall)
    }
    if (event.errorCode != null || event.technicalMessage != null) {
        Text(
            text = listOfNotNull(event.errorCode?.name, event.technicalMessage).joinToString(" : "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
