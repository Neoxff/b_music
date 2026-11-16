package com.bmusic.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment

/**
 * Carte « L'app s'est arrêtée » — proposée au lancement suivant un crash.
 *
 * Sans adb, c'est le seul moyen de connaître la cause réelle du crash :
 * le rapport est produit par [CrashReporter] et se partage en texte.
 */
class CrashReportDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val report = CrashReporter.pendingReport(requireContext()).orEmpty()
        return ComposeView(requireContext()).apply {
            setContent {
                CrashReportContent(
                    report = report,
                    onShare = { shareReport(report) },
                    onClear = {
                        CrashReporter.clearPendingReport(requireContext())
                        dismiss()
                    },
                    onClose = { dismiss() },
                )
            }
        }
    }

    private fun shareReport(report: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_title))
            putExtra(Intent.EXTRA_TEXT, report)
        }
        runCatching {
            startActivity(Intent.createChooser(intent, getString(R.string.crash_report_share)))
        }
    }

    companion object {
        const val TAG = "CrashReportDialog"
    }
}

/** Contenu Compose de la carte de crash (testable sans appareil). */
@Composable
fun CrashReportContent(
    report: String,
    onShare: (String) -> Unit = {},
    onClear: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.crash_report_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.crash_report_message),
            style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SelectionContainer {
            Text(
                text = report,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { onShare(report) }) {
                Text(stringResource(R.string.crash_report_share))
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.crash_report_clear))
            }
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.crash_report_close))
            }
        }
    }
}
