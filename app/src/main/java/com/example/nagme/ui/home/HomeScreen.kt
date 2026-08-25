package com.example.nagme.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.nagme.data.Nag
import com.example.nagme.data.RecurrenceType
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    onAddNag: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onOpenActiveNag: (Long) -> Unit,
    onEditUpcomingNag: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {

    val state by
    viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NagMeNavy)
            .navigationBarsPadding()
            .padding(16.dp)
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
                            R.drawable.nagme_mascot
                    ),
                contentDescription =
                    "NagMe mascot",
                modifier =
                    Modifier.size(58.dp)
            )

            Spacer(
                modifier =
                    Modifier.size(10.dp)
            )

            Row(
                modifier =
                    Modifier.weight(1f),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Nag",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = "Me",
                    color = NagMeTeal,
                    fontSize = 30.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick =
                    onSettings,
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            contentColor =
                                NagMeTeal
                        )
            ) {

                Text("Settings")
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        LazyColumn(
            modifier =
                Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            item {

                SectionTitle(
                    text =
                        "NAGGING NOW"
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                if (
                    state.active.isEmpty()
                ) {

                    Text(
                        text =
                            "Nothing nagging you right now.",
                        color =
                            NagMeMutedText,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        modifier =
                            Modifier.padding(
                                vertical = 6.dp
                            )
                    )
                }
            }

            items(
                items =
                    state.active,
                key = {
                    "active_${it.occurrence.id}"
                }
            ) { item ->

                ActiveNagCard(
                    item = item,
                    onEdit = {

                        onOpenActiveNag(
                            item.nag.id
                        )
                    },
                    onDone = {

                        viewModel.markDone(
                            item
                        )
                    }
                )
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                SectionTitle(
                    text =
                        "UP NEXT"
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                if (
                    state.upcoming.isEmpty()
                ) {

                    Text(
                        text =
                            "No upcoming Nags.",
                        color =
                            NagMeMutedText,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            items(
                items =
                    state.upcoming,
                key = {
                    "scheduled_${it.occurrence.id}"
                }
            ) { item ->

                UpcomingNagCard(
                    item = item,
                    onEdit = {

                        onEditUpcomingNag(
                            item.nag.id
                        )
                    }
                )
            }

            if (
                state.paused.isNotEmpty()
            ) {

                item {

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    SectionTitle(
                        text =
                            "PAUSED"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }

                items(
                    items =
                        state.paused,
                    key = {
                        "paused_${it.id}"
                    }
                ) { nag ->

                    PausedNagCard(
                        nag = nag,
                        onClick = {

                            onEditUpcomingNag(
                                nag.id
                            )
                        }
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onHistory,
            shape =
                RoundedCornerShape(
                    18.dp
                ),
            colors =
                ButtonDefaults
                    .outlinedButtonColors(
                        contentColor =
                            Color.White
                    )
        ) {

            Text(
                text =
                    "History"
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onAddNag,
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
                    )
        ) {

            Text(
                text =
                    "+ ADD NAG",
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        color =
            NagMeTeal,
        fontWeight =
            FontWeight.Bold,
        letterSpacing =
            1.2.sp,
        style =
            MaterialTheme
                .typography
                .titleMedium
    )
}

@Composable
private fun ActiveNagCard(
    item: HomeNagItem,
    onEdit: () -> Unit,
    onDone: () -> Unit
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
                    NagMeActiveCard
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.Top
            ) {

                Text(
                    text =
                        "🔊 ${item.nag.text}",
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.Bold,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    modifier =
                        Modifier.weight(1f)
                )

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                OutlinedButton(
                    modifier =
                        Modifier
                            .size(
                                44.dp
                            )
                            .offset(
                                x = 4.dp,
                                y = (-5).dp
                            ),
                    onClick =
                        onEdit,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),
                    contentPadding =
                        PaddingValues(
                            0.dp
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            NagMeActiveCard
                        ),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    NagMeTeal
                            )
                ) {

                    Text(
                        text = "✎",
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (
                                item.nag.maxNagCount !=
                                null
                            ) {

                                "Nagged ${item.occurrence.nagCount} of ${item.nag.maxNagCount} times"

                            } else {

                                "Nagged ${item.occurrence.nagCount} " +
                                        if (
                                            item.occurrence.nagCount ==
                                            1
                                        ) {
                                            "time"
                                        } else {
                                            "times"
                                        }
                            },
                        color =
                            NagMeLightText
                    )

                    item.occurrence
                        .snoozedUntil
                        ?.let {

                            Text(
                                text =
                                    "Snoozed until ${formatTime(it)}",
                                color =
                                    NagMePurpleLight
                            )
                        }
                        ?: item.occurrence
                            .nextNagAt
                            ?.let {

                                Text(
                                    text =
                                        "Nag #${item.occurrence.nagCount + 1} at ${formatTime(it)}",
                                    color =
                                        NagMePurpleLight
                                )
                            }

                    recurrenceDisplayText(
                        item.nag
                    )?.let { recurrence ->

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "↻ Repeats $recurrence",
                            color =
                                NagMeMutedText,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Button(
                    modifier =
                        Modifier
                            .width(116.dp)
                            .height(48.dp),
                    onClick =
                        onDone,
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp
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
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        Text(
                            text =
                                "✓ DONE",
                            modifier =
                                Modifier.offset(
                                    y = (-2).dp
                                ),
                            fontSize =
                                14.sp,
                            fontWeight =
                                FontWeight.Bold,
                            lineHeight =
                                14.sp,
                            maxLines = 1
                        )

                        Text(
                            text =
                                "stop nagging",
                            modifier =
                                Modifier.offset(
                                    y = 1.dp
                                ),
                            fontSize =
                                10.sp,
                            fontWeight =
                                FontWeight.Normal,
                            lineHeight =
                                10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingNagCard(
    item: HomeNagItem,
    onEdit: () -> Unit
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
                    NagMeUpcomingCard
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    18.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.Top
            ) {

                Text(
                    text =
                        item.nag.text,
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.SemiBold,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    modifier =
                        Modifier.weight(1f)
                )

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                OutlinedButton(
                    modifier =
                        Modifier
                            .size(
                                44.dp
                            )
                            .offset(
                                x = 4.dp,
                                y = (-5).dp
                            ),
                    onClick =
                        onEdit,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),
                    contentPadding =
                        PaddingValues(
                            0.dp
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            NagMeUpcomingCard
                        ),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    NagMeTeal
                            )
                ) {

                    Text(
                        text = "✎",
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    formatDateTime(
                        item.occurrence
                            .scheduledAt
                    ),
                color =
                    NagMeLightText
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    nagBehaviourText(
                        item.nag
                    ),
                color =
                    NagMeTeal
            )

            recurrenceDisplayText(
                item.nag
            )?.let { recurrence ->

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "↻ Repeats $recurrence",
                    color =
                        NagMeMutedText,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun PausedNagCard(
    nag: Nag,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick =
                    onClick
            ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    NagMePausedCard
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    nag.text,
                color =
                    NagMeMutedText,
                fontWeight =
                    FontWeight.SemiBold,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    "Paused",
                color =
                    NagMePurpleLight
            )

            Text(
                text =
                    "Tap to edit or turn back on",
                color =
                    NagMeMutedText,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
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

            val startDate =
                java.util.Calendar
                    .getInstance()
                    .apply {
                        timeInMillis =
                            nag.startDateTime
                    }

            val startMinutes =
                startDate.get(
                    java.util.Calendar.HOUR_OF_DAY
                ) * 60 +
                        startDate.get(
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

                        if (hours == 1) {
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

            if (daysText.isBlank()) {
                periodText
            } else {
                "$periodText • $daysText"
            }
        }
    }
}

private fun nagBehaviourText(
    nag: Nag
): String {

    val interval =
        nag.nagIntervalMinutes

    val nagText =
        if (
            interval == null
        ) {
            "Nag once"
        } else {
            "Every $interval min"
        }

    val stopText =
        when {

            nag.maxNagCount != null ->
                "Max ${nag.maxNagCount} nags"

            nag.maxNagDurationMinutes != null ->
                "Max ${nag.maxNagDurationMinutes} min"

            nag.stopWhenDone ->
                "Until DONE"

            else ->
                ""
        }

    return if (
        stopText.isBlank()
    ) {
        nagText
    } else {
        "$nagText • $stopText"
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
): String =
    DateFormat
        .getTimeInstance(
            DateFormat.SHORT
        )
        .format(
            Date(timestamp)
        )

private fun formatDateTime(
    timestamp: Long
): String =
    DateFormat
        .getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        )
        .format(
            Date(timestamp)
        )

private val NagMeNavy =
    Color(0xFF06101F)

private val NagMeActiveCard =
    Color(0xFF152A3A)

private val NagMeUpcomingCard =
    Color(0xFF111C32)

private val NagMePausedCard =
    Color(0xFF151827)

private val NagMeTeal =
    Color(0xFF21D4C2)

private val NagMePurpleLight =
    Color(0xFFB690FF)

private val NagMeLightText =
    Color(0xFFD7DFE8)

private val NagMeMutedText =
    Color(0xFF8995A7)

private val NagMeDarkText =
    Color(0xFF031218)

