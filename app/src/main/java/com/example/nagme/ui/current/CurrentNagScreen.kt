package com.example.nagme.ui.current

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nagme.R
import java.text.DateFormat
import java.util.Date

@Composable
fun CurrentNagScreen(
    nagId: Long,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: CurrentNagViewModel = viewModel()
) {

    val state by
    viewModel.uiState.collectAsState()

    LaunchedEffect(nagId) {
        viewModel.load(nagId)
    }

    when {

        state.loading -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NagMeNavy),
                verticalArrangement =
                    Arrangement.Center,
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator(
                    color = NagMeTeal
                )
            }
        }

        state.nag == null -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NagMeNavy)
                    .padding(24.dp)
            ) {

                Text(
                    text = "Nag not found.",
                    color = Color.White,
                    fontWeight =
                        FontWeight.Bold,
                    style =
                        MaterialTheme.typography
                            .titleLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                SecondaryButton(
                    text = "BACK",
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick =
                        onBack
                )
            }
        }

        state.occurrence == null -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NagMeNavy)
                    .padding(24.dp)
            ) {

                Text(
                    text =
                        state.nag!!.text,
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.Bold,
                    style =
                        MaterialTheme.typography
                            .headlineMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text =
                        "This Nag is not currently active.",
                    color =
                        NagMeMutedText
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                SecondaryButton(
                    text = "BACK",
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick =
                        onBack
                )
            }
        }

        else -> {

            val nag =
                state.nag!!

            val occurrence =
                state.occurrence!!

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

                    Spacer(
                        modifier =
                            Modifier.weight(1f)
                    )

                    Image(
                        painter =
                            painterResource(
                                id =
                                    R.drawable
                                        .nagme_mascot
                            ),
                        contentDescription =
                            "NagMe mascot",
                        modifier =
                            Modifier.size(
                                54.dp
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                Text(
                    text =
                        "NAGGING NOW",
                    color =
                        NagMeTeal,
                    fontWeight =
                        FontWeight.Bold,
                    letterSpacing =
                        1.2.sp,
                    style =
                        MaterialTheme.typography
                            .labelLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = nag.text,
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.Bold,
                    style =
                        MaterialTheme.typography
                            .headlineMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    NagMeInfoCard
                            )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            )
                    ) {

                        InfoLabel(
                            text = "STARTED"
                        )

                        Text(
                            text =
                                occurrence.startedAt
                                    ?.let {
                                        formatTime(it)
                                    }
                                    ?: "Not started",
                            color =
                                Color.White,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )

                        InfoLabel(
                            text = "NAGGED"
                        )

                        Text(
                            text =
                                if (
                                    nag.maxNagCount != null
                                ) {

                                    "${occurrence.nagCount} of ${nag.maxNagCount} times"

                                } else {

                                    "${occurrence.nagCount} " +
                                            if (
                                                occurrence.nagCount == 1
                                            ) {
                                                "time"
                                            } else {
                                                "times"
                                            }
                                },
                            color =
                                Color.White,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )

                        val snoozedUntil =
                            occurrence.snoozedUntil

                        if (
                            snoozedUntil != null
                        ) {

                            InfoLabel(
                                text =
                                    "SNOOZED UNTIL"
                            )

                            Text(
                                text =
                                    formatTime(
                                        snoozedUntil
                                    ),
                                color =
                                    NagMePurpleLight,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                        } else {

                            InfoLabel(
                                text =
                                    "NEXT NAG"
                            )

                            Text(
                                text =
                                    occurrence.nextNagAt
                                        ?.let {
                                            formatTime(it)
                                        }
                                        ?: "None",
                                color =
                                    NagMePurpleLight,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (
                    nag.maxNagCount != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Text(
                        text =
                            "Maximum nags",
                        color =
                            NagMeLightText,
                        fontWeight =
                            FontWeight.SemiBold,
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        SecondaryButton(
                            text = "−",
                            modifier =
                                Modifier.weight(1f),
                            enabled =
                                nag.maxNagCount >
                                        maxOf(
                                            1,
                                            occurrence.nagCount
                                        ),
                            onClick = {

                                viewModel
                                    .updateMaxNagCount(
                                        newMaxNagCount =
                                            nag.maxNagCount - 1,
                                        onStopped =
                                            onBack
                                    )
                            }
                        )

                        Button(
                            modifier =
                                Modifier.weight(1f),
                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            NagMePurple,
                                        contentColor =
                                            Color.White
                                    ),
                            onClick = {
                            }
                        ) {

                            Text(
                                text =
                                    nag.maxNagCount
                                        .toString(),
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        SecondaryButton(
                            text = "+",
                            modifier =
                                Modifier.weight(1f),
                            enabled =
                                nag.maxNagCount < 99,
                            onClick = {

                                viewModel
                                    .updateMaxNagCount(
                                        newMaxNagCount =
                                            nag.maxNagCount + 1,
                                        onStopped =
                                            onBack
                                    )
                            }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),
                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    NagMeTeal,
                                contentColor =
                                    NagMeDarkText
                            ),
                    onClick = {

                        viewModel.markDone(
                            onDone = onDone
                        )
                    }
                ) {

                    Text(
                        text = "✓ DONE",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text = "Snooze",
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.Bold,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )

                Text(
                    text =
                        "Not ready yet? NagMe can come back later.",
                    color =
                        NagMeMutedText,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    SnoozeButton(
                        text = "5 MIN",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {

                            viewModel.snooze(
                                minutes = 5,
                                onSnoozed =
                                    onBack
                            )
                        }
                    )

                    SnoozeButton(
                        text = "10 MIN",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {

                            viewModel.snooze(
                                minutes = 10,
                                onSnoozed =
                                    onBack
                            )
                        }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    SnoozeButton(
                        text = "30 MIN",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {

                            viewModel.snooze(
                                minutes = 30,
                                onSnoozed =
                                    onBack
                            )
                        }
                    )

                    SnoozeButton(
                        text = "1 HOUR",
                        modifier =
                            Modifier.weight(1f),
                        onClick = {

                            viewModel.snooze(
                                minutes = 60,
                                onSnoozed =
                                    onBack
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLabel(
    text: String
) {

    Text(
        text = text,
        color =
            NagMeTeal,
        fontWeight =
            FontWeight.Bold,
        letterSpacing =
            0.8.sp,
        style =
            MaterialTheme.typography
                .labelMedium
    )
}

@Composable
private fun SnoozeButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    OutlinedButton(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            ButtonDefaults
                .outlinedButtonColors(
                    contentColor =
                        NagMeLightText
                ),
        onClick =
            onClick
    ) {

        Text(
            text = text,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    OutlinedButton(
        modifier =
            modifier,
        enabled =
            enabled,
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            ButtonDefaults
                .outlinedButtonColors(
                    contentColor =
                        NagMeLightText
                ),
        onClick =
            onClick
    ) {

        Text(
            text = text,
            fontWeight =
                FontWeight.SemiBold
        )
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

/*
 * Cosmetic-only NagMe V1 palette.
 */
private val NagMeNavy =
    Color(0xFF06101F)

private val NagMeInfoCard =
    Color(0xFF111C32)

private val NagMeTeal =
    Color(0xFF21D4C2)

private val NagMePurple =
    Color(0xFF7657C9)

private val NagMePurpleLight =
    Color(0xFFB690FF)

private val NagMeLightText =
    Color(0xFFD7DFE8)

private val NagMeMutedText =
    Color(0xFF8995A7)

private val NagMeDarkText =
    Color(0xFF031218)