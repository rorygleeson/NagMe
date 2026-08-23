package com.example.nagme.ui.current

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nagme.R
import com.example.nagme.data.Nag
import com.example.nagme.data.RecurrenceType
import java.text.DateFormat
import java.util.Date

@Composable
fun CurrentNagScreen(
    nagId: Long,
    onBack: () -> Unit,
    onDone: () -> Unit,
    showDoneButton: Boolean = false,
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

            var personalityMenuExpanded by remember(
                nag.id
            ) {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NagMeNavy)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(24.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

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

                    Spacer(
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

                if (
                    showDoneButton
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),
                        onClick = {

                            viewModel.markDone(
                                onDone =
                                    onDone
                            )
                        },
                        shape =
                            RoundedCornerShape(
                                16.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        NagMeTeal,
                                    contentColor =
                                        NagMeDarkText
                                )
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "✓ DONE",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "stop nagging",
                                fontSize =
                                    10.sp,
                                fontWeight =
                                    FontWeight.Normal
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }

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

                        recurrenceDisplayText(
                            nag
                        )?.let { recurrence ->

                            Spacer(
                                modifier =
                                    Modifier.height(18.dp)
                            )

                            InfoLabel(
                                text =
                                    "REPEATS"
                            )

                            Text(
                                text =
                                    recurrence,
                                color =
                                    Color.White,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text = "AI NAGGING",
                    color = NagMeTeal,
                    fontWeight = FontWeight.Bold,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

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
                                NagMeInfoCard
                        )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    "Use AI-generated nagging",
                                color =
                                    Color.White,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )

                            Text(
                                text =
                                    if (
                                        nag.aiEnabled
                                    ) {
                                        "AI will be used for the next nag."
                                    } else {
                                        "Off. Using your original reminder text."
                                    },
                                color =
                                    NagMeMutedText,
                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )
                        }

                        Switch(
                            checked =
                                nag.aiEnabled,
                            onCheckedChange = { enabled ->

                                if (
                                    !enabled
                                ) {
                                    personalityMenuExpanded =
                                        false
                                }

                                viewModel.updateAiSettings(
                                    aiEnabled =
                                        enabled,
                                    personality =
                                        nag.personality
                                )
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor =
                                        NagMeDarkText,
                                    checkedTrackColor =
                                        NagMeTeal,
                                    uncheckedThumbColor =
                                        NagMeMutedText,
                                    uncheckedTrackColor =
                                        NagMeInfoCard
                                )
                        )
                    }
                }

                if (
                    nag.aiEnabled
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text = "Personality",
                        color =
                            NagMeLightText,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    val selectedPersonality =
                        aiPersonalityOptions()
                            .firstOrNull { option ->
                                option.id ==
                                        nag.personality
                            }
                            ?: aiPersonalityOptions()
                                .first()

                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        OutlinedButton(
                            modifier =
                                Modifier.fillMaxWidth(),
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
                            onClick = {
                                personalityMenuExpanded =
                                    true
                            }
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        selectedPersonality.label,
                                    modifier =
                                        Modifier.weight(1f),
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    text = "▼",
                                    color =
                                        NagMeMutedText
                                )
                            }
                        }

                        DropdownMenu(
                            expanded =
                                personalityMenuExpanded,
                            onDismissRequest = {
                                personalityMenuExpanded =
                                    false
                            }
                        ) {

                            aiPersonalityOptions()
                                .forEach { option ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text =
                                                    option.label
                                            )
                                        },
                                        onClick = {

                                            personalityMenuExpanded =
                                                false

                                            viewModel.updateAiSettings(
                                                aiEnabled =
                                                    true,
                                                personality =
                                                    option.id
                                            )
                                        }
                                    )
                                }
                        }
                    }
                }

                if (
                    nag.nagIntervalMinutes != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Text(
                        text =
                            "Nag every",
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
                                nag.nagIntervalMinutes > 1,
                            onClick = {

                                viewModel
                                    .updateNagIntervalMinutes(
                                        newIntervalMinutes =
                                            nag.nagIntervalMinutes - 1,
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
                                    if (
                                        nag.nagIntervalMinutes == 1
                                    ) {
                                        "1 min"
                                    } else {
                                        "${nag.nagIntervalMinutes} min"
                                    },
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        SecondaryButton(
                            text = "+",
                            modifier =
                                Modifier.weight(1f),
                            enabled =
                                nag.nagIntervalMinutes < 1440,
                            onClick = {

                                viewModel
                                    .updateNagIntervalMinutes(
                                        newIntervalMinutes =
                                            nag.nagIntervalMinutes + 1,
                                        onStopped =
                                            onBack
                                    )
                            }
                        )
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
                        Modifier.height(12.dp)
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

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
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

private data class AiPersonality(
    val id: String,
    val label: String
)

private fun aiPersonalityOptions():
        List<AiPersonality> {

    return listOf(

        AiPersonality(
            id = "irish_mammy",
            label = "Irish Mammy"
        ),

        AiPersonality(
            id = "corporate_guru",
            label = "Corporate Guru"
        ),

        AiPersonality(
            id = "aussie_mate",
            label = "Aussie Mate"
        ),

        AiPersonality(
            id = "posh_butler",
            label = "Posh Butler"
        ),

        AiPersonality(
            id = "passive_aggressive",
            label = "Passive Aggressive"
        ),

        AiPersonality(
            id = "needy_ex",
            label = "Needy Ex"
        ),

        AiPersonality(
            id = "disappointed_dad",
            label = "Disappointed Dad"
        ),

        AiPersonality(
            id = "nosy_neighbour",
            label = "Nosy Neighbour"
        ),

        AiPersonality(
            id = "drama_queen",
            label = "Drama Queen"
        ),

        AiPersonality(
            id = "paranoid_detective",
            label = "Paranoid Detective"
        )
    )
}

private fun recurrenceDisplayText(
    nag: Nag
): String? {

    return when (
        nag.recurrenceType
    ) {

        RecurrenceType.ONCE ->
            null

        RecurrenceType.DAILY ->
            "Daily"

        RecurrenceType.WEEKDAYS ->
            "Weekdays"

        RecurrenceType.WEEKENDS ->
            "Weekends"

        RecurrenceType.WEEKLY ->
            "Weekly"

        RecurrenceType.SELECTED_DAYS -> {

            val orderedDays =
                listOf(
                    2 to "Mon",
                    3 to "Tue",
                    4 to "Wed",
                    5 to "Thu",
                    6 to "Fri",
                    7 to "Sat",
                    1 to "Sun"
                )

            orderedDays
                .filter { (day, _) ->
                    day in nag.selectedDays
                }
                .joinToString(
                    separator = ", "
                ) { (_, label) ->
                    label
                }
        }

        RecurrenceType.DURING_TIME_PERIOD -> {

            val interval =
                nag.timePeriodIntervalMinutes
                    ?: return null

            val endMinutes =
                nag.timePeriodEndMinutesOfDay
                    ?: return null

            val startCalendar =
                java.util.Calendar
                    .getInstance()
                    .apply {
                        timeInMillis =
                            nag.startDateTime
                    }

            val startMinutes =
                startCalendar.get(
                    java.util.Calendar.HOUR_OF_DAY
                ) * 60 +
                        startCalendar.get(
                            java.util.Calendar.MINUTE
                        )

            val orderedDays =
                listOf(
                    2 to "Mon",
                    3 to "Tue",
                    4 to "Wed",
                    5 to "Thu",
                    6 to "Fri",
                    7 to "Sat",
                    1 to "Sun"
                )

            val daysText =
                orderedDays
                    .filter { (day, _) ->
                        day in nag.selectedDays
                    }
                    .joinToString(
                        separator = ", "
                    ) { (_, label) ->
                        label
                    }

            val intervalText =
                when {

                    interval < 60 ->
                        "$interval min"

                    interval % 60 == 0 -> {

                        val hours =
                            interval / 60

                        if (
                            hours == 1
                        ) {
                            "1 hour"
                        } else {
                            "$hours hours"
                        }
                    }

                    else -> {

                        val hours =
                            interval / 60

                        val minutes =
                            interval % 60

                        "${hours}h ${minutes}m"
                    }
                }

            val periodText =
                "$intervalText • " +
                        "${formatMinutesOfDay(startMinutes)}–" +
                        formatMinutesOfDay(endMinutes)

            if (
                daysText.isBlank()
            ) {
                periodText
            } else {
                "$periodText • $daysText"
            }
        }
    }
}

private fun formatMinutesOfDay(
    minutesOfDay: Int
): String {

    val calendar =
        java.util.Calendar
            .getInstance()
            .apply {

                set(
                    java.util.Calendar.HOUR_OF_DAY,
                    minutesOfDay / 60
                )

                set(
                    java.util.Calendar.MINUTE,
                    minutesOfDay % 60
                )

                set(
                    java.util.Calendar.SECOND,
                    0
                )

                set(
                    java.util.Calendar.MILLISECOND,
                    0
                )
            }

    return DateFormat
        .getTimeInstance(
            DateFormat.SHORT
        )
        .format(
            calendar.time
        )
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