package com.example.nagme.ui.edit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nagme.data.RecurrenceType
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun EditNagScreen(
    nagId: Long,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditNagViewModel = viewModel()
) {

    val context =
        LocalContext.current

    val nag by
    viewModel.nag.collectAsState()

    val nextScheduledAt by
    viewModel.nextScheduledAt.collectAsState()

    var text by remember {
        mutableStateOf("")
    }

    var scheduledAt by remember {
        mutableLongStateOf(
            System.currentTimeMillis() +
                    10 * 60_000L
        )
    }

    var recurrenceType by remember {
        mutableStateOf(
            RecurrenceType.ONCE
        )
    }

    var selectedDays by remember {
        mutableStateOf<Set<Int>>(
            emptySet()
        )
    }

    var timePeriodIntervalMinutes by remember {
        mutableStateOf<Int?>(60)
    }

    var timePeriodEndMinutesOfDay by remember {
        mutableStateOf<Int?>(17 * 60)
    }

    var enabled by remember {
        mutableStateOf(true)
    }

    var nagIntervalMinutes by remember {
        mutableStateOf<Int?>(null)
    }

    var maxNagCount by remember {
        mutableStateOf<Int?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var showDeleteConfirm by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(nagId) {
        viewModel.loadNag(nagId)
    }

    LaunchedEffect(nag) {

        nag?.let {

            text =
                it.text

            scheduledAt =
                it.startDateTime

            recurrenceType =
                it.recurrenceType

            selectedDays =
                it.selectedDays

            timePeriodIntervalMinutes =
                it.timePeriodIntervalMinutes
                    ?: 60

            timePeriodEndMinutesOfDay =
                it.timePeriodEndMinutesOfDay
                    ?: 17 * 60

            enabled =
                it.enabled

            nagIntervalMinutes =
                it.nagIntervalMinutes

            maxNagCount =
                it.maxNagCount
        }
    }

    fun chooseDate() {

        val pickerInitialTime =
            if (
                recurrenceType !=
                RecurrenceType.ONCE &&
                nextScheduledAt != null
            ) {
                nextScheduledAt!!
            } else {
                scheduledAt
            }

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    pickerInitialTime
            }

        DatePickerDialog(
            context,
            { _, year, month, day ->

                val updated =
                    Calendar.getInstance().apply {

                        timeInMillis =
                            scheduledAt

                        set(
                            Calendar.YEAR,
                            year
                        )

                        set(
                            Calendar.MONTH,
                            month
                        )

                        set(
                            Calendar.DAY_OF_MONTH,
                            day
                        )
                    }

                scheduledAt =
                    updated.timeInMillis
            },
            calendar.get(
                Calendar.YEAR
            ),
            calendar.get(
                Calendar.MONTH
            ),
            calendar.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }

    fun chooseTime() {

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    scheduledAt
            }

        TimePickerDialog(
            context,
            { _, hour, minute ->

                val updated =
                    Calendar.getInstance().apply {

                        timeInMillis =
                            scheduledAt

                        set(
                            Calendar.HOUR_OF_DAY,
                            hour
                        )

                        set(
                            Calendar.MINUTE,
                            minute
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )
                    }

                scheduledAt =
                    updated.timeInMillis
            },
            calendar.get(
                Calendar.HOUR_OF_DAY
            ),
            calendar.get(
                Calendar.MINUTE
            ),
            false
        ).show()
    }

    fun chooseTimePeriodEnd() {

        val endMinutes =
            timePeriodEndMinutesOfDay
                ?: 17 * 60

        TimePickerDialog(
            context,
            { _, hour, minute ->

                timePeriodEndMinutesOfDay =
                    hour * 60 + minute

                errorMessage = null
            },
            endMinutes / 60,
            endMinutes % 60,
            false
        ).show()
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

            Text(
                text = "Edit Nag",
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
                Modifier.height(28.dp)
        )

        SectionTitle(
            text = "WHAT"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                errorMessage = null
            },
            modifier =
                Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "What should I nag you about?"
                )
            },
            singleLine = true,
            shape =
                RoundedCornerShape(
                    16.dp
                ),
            colors =
                OutlinedTextFieldDefaults
                    .colors(
                        focusedTextColor =
                            Color.White,
                        unfocusedTextColor =
                            Color.White,
                        focusedBorderColor =
                            NagMeTeal,
                        unfocusedBorderColor =
                            NagMeBorder,
                        focusedContainerColor =
                            NagMeField,
                        unfocusedContainerColor =
                            NagMeField,
                        cursorColor =
                            NagMeTeal,
                        focusedPlaceholderColor =
                            NagMeMutedText,
                        unfocusedPlaceholderColor =
                            NagMeMutedText
                    )
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        if (
            recurrenceType ==
            RecurrenceType.ONCE
        ) {

            SectionTitle(
                text = "WHEN"
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    formatScheduledTime(
                        scheduledAt
                    ),
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
                    Modifier.height(12.dp)
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
                    text = "DATE",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        chooseDate()
                    }
                )

                SecondaryButton(
                    text = "TIME",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        chooseTime()
                    }
                )
            }

        } else {

            nextScheduledAt?.let { nextTime ->

                Text(
                    text =
                        "NEXT NAG SCHEDULED FOR",
                    color =
                        NagMeTeal,
                    fontWeight =
                        FontWeight.Bold,
                    style =
                        MaterialTheme.typography
                            .labelLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        formatScheduledTime(
                            nextTime
                        ),
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.SemiBold,
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
            }

            Spacer(
                modifier =
                    Modifier.height(22.dp)
            )

            Text(
                text =
                    "CHANGE SCHEDULE",
                color =
                    NagMeTeal,
                fontWeight =
                    FontWeight.Bold,
                style =
                    MaterialTheme.typography
                        .labelLarge
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
                    text = "DATE",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        chooseDate()
                    }
                )

                SecondaryButton(
                    text = "TIME",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        chooseTime()
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )

        SectionTitle(
            text = "REPEAT"
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        recurrenceOptions().forEach {
                (type, label) ->

            ChoiceButton(
                text = label,
                selected =
                    recurrenceType ==
                            type,
                onClick = {
                    recurrenceType =
                        type
                }
            )
        }

        if (
            recurrenceType ==
            RecurrenceType.SELECTED_DAYS
        ) {

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(
                text = "Choose days",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            DayRow(
                selectedDays =
                    selectedDays,
                onToggle = { day ->

                    selectedDays =
                        if (
                            day in selectedDays
                        ) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                }
            )
        }

        if (
            recurrenceType ==
            RecurrenceType.DURING_TIME_PERIOD
        ) {

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text = "Every",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            val periodInterval =
                timePeriodIntervalMinutes
                    ?: 60

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
                    onClick = {
                        timePeriodIntervalMinutes =
                            maxOf(
                                15,
                                periodInterval - 15
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
                                    NagMeTeal,
                                contentColor =
                                    NagMeDarkText
                            ),
                    onClick = {
                    }
                ) {

                    Text(
                        text =
                            formatInterval(
                                periodInterval
                            ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                SecondaryButton(
                    text = "+",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        timePeriodIntervalMinutes =
                            minOf(
                                720,
                                periodInterval + 15
                            )
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text = "From",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    formatTimeOnly(
                        scheduledAt
                    ),
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
                    Modifier.height(10.dp)
            )

            SecondaryButton(
                text = "START TIME",
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    chooseTime()
                }
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text = "Until",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    formatMinutesOfDay(
                        timePeriodEndMinutesOfDay
                            ?: 17 * 60
                    ),
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
                    Modifier.height(10.dp)
            )

            SecondaryButton(
                text = "END TIME",
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    chooseTimePeriodEnd()
                }
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text = "Days",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            DayRow(
                selectedDays =
                    selectedDays,
                onToggle = { day ->

                    selectedDays =
                        if (
                            day in selectedDays
                        ) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }

                    errorMessage = null
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )

        SectionTitle(
            text = "NAGGING"
        )

        nagIntervalMinutes?.let { interval ->

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text = "Nag every",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
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
                    onClick = {

                        nagIntervalMinutes =
                            maxOf(
                                1,
                                interval - 1
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
                                    NagMeTeal,
                                contentColor =
                                    NagMeDarkText
                            ),
                    onClick = {
                    }
                ) {

                    Text(
                        text =
                            if (
                                interval == 1
                            ) {
                                "1 min"
                            } else {
                                "$interval min"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                SecondaryButton(
                    text = "+",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {

                        nagIntervalMinutes =
                            minOf(
                                1440,
                                interval + 1
                            )
                    }
                )
            }
        }

        maxNagCount?.let { maximum ->

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text = "Maximum nags",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
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
                    onClick = {

                        maxNagCount =
                            maxOf(
                                1,
                                maximum - 1
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
                                    NagMeTeal,
                                contentColor =
                                    NagMeDarkText
                            ),
                    onClick = {
                    }
                ) {

                    Text(
                        text =
                            maximum.toString(),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                SecondaryButton(
                    text = "+",
                    modifier =
                        Modifier.weight(1f),
                    onClick = {

                        maxNagCount =
                            minOf(
                                99,
                                maximum + 1
                            )
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )

        SectionTitle(
            text = "STATUS"
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
                        NagMeCard
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
                        text = "Nag active",
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
                            if (enabled) {
                                "Turn off to pause this Nag."
                            } else {
                                "This Nag is paused."
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
                        enabled,
                    onCheckedChange = {
                        enabled = it
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
                                NagMeField,
                            uncheckedBorderColor =
                                NagMeBorder
                        )
                )
            }
        }

        errorMessage?.let {

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text = it,
                color =
                    NagMeDanger,
                fontWeight =
                    FontWeight.SemiBold
            )
        }

        Spacer(
            modifier =
                Modifier.height(36.dp)
        )

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {

                viewModel.saveChanges(
                    text = text,
                    scheduledAt =
                        scheduledAt,
                    recurrenceType =
                        recurrenceType,
                    selectedDays =
                        selectedDays,
                    enabled =
                        enabled,
                    timePeriodIntervalMinutes =
                        if (
                            recurrenceType ==
                            RecurrenceType.DURING_TIME_PERIOD
                        ) {
                            timePeriodIntervalMinutes
                        } else {
                            null
                        },
                    timePeriodEndMinutesOfDay =
                        if (
                            recurrenceType ==
                            RecurrenceType.DURING_TIME_PERIOD
                        ) {
                            timePeriodEndMinutesOfDay
                        } else {
                            null
                        },
                    nagIntervalMinutes =
                        nagIntervalMinutes,
                    maxNagCount =
                        maxNagCount,
                    onSaved =
                        onSaved,
                    onError = { error ->
                        errorMessage =
                            error
                    }
                )
            },
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
                    "SAVE CHANGES",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                showDeleteConfirm =
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
                    "DELETE NAG",
                fontWeight =
                    FontWeight.SemiBold
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )
    }

    if (
        showDeleteConfirm
    ) {

        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm =
                    false
            },
            containerColor =
                NagMeCard,
            title = {

                Text(
                    text =
                        "Delete Nag?",
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {

                Text(
                    text =
                        "This removes the Nag and its future scheduled occurrences. History is kept.",
                    color =
                        NagMeLightText
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.deleteNag(
                            onDeleted =
                                onDeleted,
                            onError = { error ->
                                errorMessage =
                                    error
                            }
                        )

                        showDeleteConfirm =
                            false
                    }
                ) {

                    Text(
                        text =
                            "DELETE",
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
                        showDeleteConfirm =
                            false
                    }
                ) {

                    Text(
                        text =
                            "CANCEL",
                        color =
                            NagMeMutedText
                    )
                }
            }
        )
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
        style =
            MaterialTheme.typography
                .titleMedium
    )
}

@Composable
private fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Spacer(
        modifier =
            Modifier.height(7.dp)
    )

    if (
        selected
    ) {

        Button(
            modifier =
                Modifier.fillMaxWidth(),
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
                    ),
            onClick =
                onClick
        ) {

            Text(
                text =
                    "✓ $text",
                fontWeight =
                    FontWeight.Bold
            )
        }

    } else {

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
            onClick =
                onClick
        ) {

            Text(text)
        }
    }
}

@Composable
private fun SecondaryButton(
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

private fun recurrenceOptions():
        List<Pair<RecurrenceType, String>> {

    return listOf(
        RecurrenceType.ONCE to
                "Once",

        RecurrenceType.DAILY to
                "Daily",

        RecurrenceType.WEEKDAYS to
                "Weekdays",

        RecurrenceType.WEEKENDS to
                "Weekends",

        RecurrenceType.WEEKLY to
                "Weekly",

        RecurrenceType.SELECTED_DAYS to
                "Choose days",

        RecurrenceType.DURING_TIME_PERIOD to
                "During time period"
    )
}

@Composable
private fun DayRow(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit
) {

    val days =
        listOf(
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F",
            Calendar.SATURDAY to "S",
            Calendar.SUNDAY to "S"
        )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {

        days.forEach {
                (day, label) ->

            if (
                day in selectedDays
            ) {

                Button(
                    modifier =
                        Modifier.weight(1f),
                    shape =
                        RoundedCornerShape(
                            14.dp
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
                        onToggle(day)
                    }
                ) {

                    Text(
                        text = label,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

            } else {

                OutlinedButton(
                    modifier =
                        Modifier.weight(1f),
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    NagMeLightText
                            ),
                    onClick = {
                        onToggle(day)
                    }
                ) {

                    Text(label)
                }
            }
        }
    }
}

private fun formatScheduledTime(
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

private fun formatTimeOnly(
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

private fun formatMinutesOfDay(
    minutesOfDay: Int
): String {

    val calendar =
        Calendar.getInstance().apply {

            set(
                Calendar.HOUR_OF_DAY,
                minutesOfDay / 60
            )

            set(
                Calendar.MINUTE,
                minutesOfDay % 60
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
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

private fun formatInterval(
    minutes: Int
): String {

    return when {

        minutes < 60 ->
            "$minutes min"

        minutes % 60 == 0 -> {

            val hours =
                minutes / 60

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
                minutes / 60

            val remainingMinutes =
                minutes % 60

            "${hours}h ${remainingMinutes}m"
        }
    }
}

/*
 * Cosmetic-only NagMe V1 palette.
 */
private val NagMeNavy =
    Color(0xFF06101F)

private val NagMeCard =
    Color(0xFF111C32)

private val NagMeField =
    Color(0xFF0B1628)

private val NagMeBorder =
    Color(0xFF506078)

private val NagMeTeal =
    Color(0xFF21D4C2)

private val NagMeLightText =
    Color(0xFFD7DFE8)

private val NagMeMutedText =
    Color(0xFF8995A7)

private val NagMeDarkText =
    Color(0xFF031218)

private val NagMeDanger =
    Color(0xFFFF7A8A)
