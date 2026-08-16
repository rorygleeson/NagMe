package com.example.nagme.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nagme.data.OccurrenceStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {

    val history by
    viewModel.history.collectAsState()

    var showClearConfirmation by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NagMeNavy)
            .padding(24.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "History",
                color =
                    Color.White,
                fontWeight =
                    FontWeight.Bold,
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                modifier =
                    Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onBack,
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            contentColor =
                                NagMeLightText
                        )
            ) {
                Text("‹ BACK")
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Every Nag that has fired.",
            color =
                NagMeMutedText,
            style =
                MaterialTheme.typography
                    .bodyMedium
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        if (
            history.isEmpty()
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        20.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            NagMeCard
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            20.dp
                        )
                ) {

                    Text(
                        text =
                            "Nothing here yet",
                        color =
                            Color.White,
                        fontWeight =
                            FontWeight.Bold,
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "Nags will appear here once they have fired.",
                        color =
                            NagMeMutedText
                    )
                }
            }

        } else {

            LazyColumn(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                items(
                    items =
                        history,
                    key = { item ->
                        item.occurrence.id
                    }
                ) { item ->

                    HistoryCard(
                        item = item
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    showClearConfirmation =
                        true
                },
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            contentColor =
                                NagMeDanger
                        )
            ) {

                Text(
                    text =
                        "CLEAR HISTORY",
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }

    if (
        showClearConfirmation
    ) {

        AlertDialog(
            onDismissRequest = {
                showClearConfirmation =
                    false
            },
            containerColor =
                NagMeCard,
            title = {

                Text(
                    text =
                        "Clear history?",
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {

                Text(
                    text =
                        "This will permanently remove completed Nag history.",
                    color =
                        NagMeLightText
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.clearHistory()

                        showClearConfirmation =
                            false
                    }
                ) {

                    Text(
                        text = "CLEAR",
                        color =
                            NagMeDanger,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showClearConfirmation =
                            false
                    }
                ) {

                    Text(
                        text = "CANCEL",
                        color =
                            NagMeMutedText
                    )
                }
            }
        )
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem
) {

    val nag =
        item.nag

    val occurrence =
        item.occurrence

    val resultColor =
        when (
            occurrence.status
        ) {

            OccurrenceStatus.DONE ->
                NagMeTeal

            OccurrenceStatus.EXPIRED ->
                NagMePurpleLight

            OccurrenceStatus.MISSED ->
                NagMeDanger

            OccurrenceStatus.ACTIVE ->
                NagMePurpleLight

            OccurrenceStatus.SCHEDULED ->
                NagMeMutedText
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    NagMeCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text = nag.text,
                color =
                    Color.White,
                fontWeight =
                    FontWeight.SemiBold,
                style =
                    MaterialTheme.typography
                        .titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    historyResultText(
                        item
                    ),
                color =
                    resultColor,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    if (
                        occurrence.nagCount == 1
                    ) {
                        "Nagged 1 time"
                    } else {
                        "Nagged ${occurrence.nagCount} times"
                    },
                color =
                    NagMeLightText
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Scheduled ${
                        formatDateTime(
                            occurrence.scheduledAt
                        )
                    }",
                color =
                    NagMeMutedText,
                style =
                    MaterialTheme.typography
                        .bodySmall
            )
        }
    }
}

private fun historyResultText(
    item: HistoryItem
): String {

    val occurrence =
        item.occurrence

    return when (
        occurrence.status
    ) {

        OccurrenceStatus.DONE -> {

            occurrence.completedAt
                ?.let {
                    "Done at ${formatTime(it)}"
                }
                ?: "Done"
        }

        OccurrenceStatus.EXPIRED -> {
            "Not completed"
        }

        OccurrenceStatus.MISSED -> {
            "Missed"
        }

        OccurrenceStatus.ACTIVE -> {
            "Still nagging"
        }

        OccurrenceStatus.SCHEDULED -> {
            "Scheduled"
        }
    }
}

private fun formatTime(
    timestamp: Long
): String {

    return DateFormat
        .getTimeInstance(
            DateFormat.SHORT
        )
        .format(
            Date(timestamp)
        )
}

private fun formatDateTime(
    timestamp: Long
): String {

    return DateFormat
        .getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        )
        .format(
            Date(timestamp)
        )
}

/*
 * Cosmetic-only NagMe V1 palette.
 */
private val NagMeNavy =
    Color(0xFF06101F)

private val NagMeCard =
    Color(0xFF111C32)

private val NagMeTeal =
    Color(0xFF21D4C2)

private val NagMePurpleLight =
    Color(0xFFB690FF)

private val NagMeLightText =
    Color(0xFFD7DFE8)

private val NagMeMutedText =
    Color(0xFF8995A7)

private val NagMeDanger =
    Color(0xFFFF7A8A)