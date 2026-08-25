package com.example.nagme.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nagme.data.RecurrenceType
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun AddNagScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AddNagViewModel = viewModel()
) {

    val context =
        LocalContext.current

    val focusManager =
        LocalFocusManager.current

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

    /*
     * Only used for DURING_TIME_PERIOD.
     *
     * Example:
     * 60 = create a new occurrence every hour.
     */
    var timePeriodIntervalMinutes by remember {
        mutableIntStateOf(60)
    }

    /*
     * Only used for DURING_TIME_PERIOD.
     *
     * Stored as minutes since midnight.
     * Default = 5:00 pm.
     */
    var timePeriodEndMinutesOfDay by remember {
        mutableIntStateOf(
            17 * 60
        )
    }

    var nagIntervalMinutes by remember {
        mutableIntStateOf(10)
    }

    var stopMode by remember {
        mutableStateOf(
            StopMode.UNTIL_DONE
        )
    }

    var maxNagCount by remember {
        mutableIntStateOf(3)
    }

    var maxDurationMinutes by remember {
        mutableIntStateOf(60)
    }

    /*
     * AI is deliberately OFF by default.
     *
     * When OFF, the personality control is hidden.
     */
    var aiEnabled by remember {
        mutableStateOf(false)
    }

    /*
     * Irish Mammy is the default personality if the
     * user chooses to enable AI for this Nag.
     */
    var personality by remember {
        mutableStateOf("irish_mammy")
    }

    var personalityMenuExpanded by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun chooseDate() {

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    scheduledAt
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

        val initialHour =
            timePeriodEndMinutesOfDay / 60

        val initialMinute =
            timePeriodEndMinutesOfDay % 60

        TimePickerDialog(
            context,
            { _, hour, minute ->

                timePeriodEndMinutesOfDay =
                    hour * 60 + minute

                errorMessage = null
            },
            initialHour,
            initialMinute,
            false
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                NagMeNavy
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    focusManager.clearFocus()
                }
            }
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                24.dp
            )
    ) {

        Text(
            text =
                "Add Nag",
            color =
                Color.White,
            fontWeight =
                FontWeight.Bold,
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Text(
            text =
                "Tell NagMe what needs doing.",
            color =
                NagMeMutedText,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )

        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )

        SectionTitle(
            text =
                "WHAT SHOULD I NAG YOU ABOUT?"
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        OutlinedTextField(
            value =
                text,
            onValueChange = {

                text =
                    it

                errorMessage =
                    null
            },
            modifier =
                Modifier.fillMaxWidth(),
            placeholder = {

                Text(
                    text =
                        "e.g. Put the bins out"
                )
            },
            singleLine =
                true,
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
                Modifier.height(
                    28.dp
                )
        )

        SectionTitle(
            text =
                if (
                    recurrenceType ==
                    RecurrenceType.DURING_TIME_PERIOD
                ) {
                    "STARTING / FROM"
                } else {
                    "WHEN"
                }
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
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
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
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
                text =
                    "DATE",
                modifier =
                    Modifier.weight(1f),
                onClick = {
                    chooseDate()
                }
            )

            SecondaryButton(
                text =
                    "TIME",
                modifier =
                    Modifier.weight(1f),
                onClick = {
                    chooseTime()
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )

        SectionTitle(
            text =
                "SCHEDULE"
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        recurrenceOptions()
            .forEach {
                    (type, label) ->

                ChoiceButton(
                    text =
                        label,
                    selected =
                        recurrenceType ==
                                type,
                    onClick = {

                        recurrenceType =
                            type

                        errorMessage =
                            null
                    }
                )
            }

        if (
            recurrenceType ==
            RecurrenceType.SELECTED_DAYS
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Choose days",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            DayRow(
                selectedDays =
                    selectedDays,
                onToggle = { day ->

                    selectedDays =
                        if (
                            day in
                            selectedDays
                        ) {

                            selectedDays -
                                    day

                        } else {

                            selectedDays +
                                    day
                        }

                    errorMessage =
                        null
                }
            )
        }

        if (
            recurrenceType ==
            RecurrenceType.DURING_TIME_PERIOD
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )

            Text(
                text =
                    "Every",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
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
                    text =
                        "−",
                    modifier =
                        Modifier.weight(1f),
                    enabled =
                        timePeriodIntervalMinutes >
                                15,
                    onClick = {

                        timePeriodIntervalMinutes =
                            maxOf(
                                15,
                                timePeriodIntervalMinutes -
                                        15
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
                            formatInterval(
                                timePeriodIntervalMinutes
                            ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                SecondaryButton(
                    text =
                        "+",
                    modifier =
                        Modifier.weight(1f),
                    enabled =
                        timePeriodIntervalMinutes <
                                720,
                    onClick = {

                        timePeriodIntervalMinutes =
                            minOf(
                                720,
                                timePeriodIntervalMinutes +
                                        15
                            )
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )

            Text(
                text =
                    "Until",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    formatMinutesOfDay(
                        timePeriodEndMinutesOfDay
                    ),
                color =
                    Color.White,
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
                        10.dp
                    )
            )

            SecondaryButton(
                text =
                    "END TIME",
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    chooseTimePeriodEnd()
                }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )

            Text(
                text =
                    "Days",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            DayRow(
                selectedDays =
                    selectedDays,
                onToggle = { day ->

                    selectedDays =
                        if (
                            day in
                            selectedDays
                        ) {

                            selectedDays -
                                    day

                        } else {

                            selectedDays +
                                    day
                        }

                    errorMessage =
                        null
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )

        SectionTitle(
            text =
                "NAG ME EVERY"
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        listOf(
            2,
            5,
            10,
            15,
            30,
            60
        ).forEach { minutes ->

            ChoiceButton(
                text =
                    if (
                        minutes ==
                        60
                    ) {
                        "1 hour"
                    } else {
                        "$minutes minutes"
                    },
                selected =
                    nagIntervalMinutes ==
                            minutes,
                onClick = {

                    nagIntervalMinutes =
                        minutes
                }
            )
        }

        /*
         * AI settings are intentionally per Nag.
         *
         * Existing/non-AI behaviour remains the default.
         */
        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )

        SectionTitle(
            text =
                "AI NAGGING"
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
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
                        "Use AI-generated nagging",
                    color =
                        Color.White,
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
                            4.dp
                        )
                )

                Text(
                    text =
                        if (
                            aiEnabled
                        ) {
                            "NagMe will generate fresh, increasingly persistent wording."
                        } else {
                            "Off by default. NagMe will use your original reminder text."
                        },
                    color =
                        NagMeMutedText,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            Switch(
                checked =
                    aiEnabled,
                onCheckedChange = { enabled ->

                    aiEnabled =
                        enabled

                    if (
                        !enabled
                    ) {
                        personalityMenuExpanded =
                            false
                    }
                }
            )
        }

        /*
         * Hide personality completely while AI is disabled.
         */
        if (
            aiEnabled
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            Text(
                text =
                    "PERSONALITY",
                color =
                    NagMeMutedText,
                fontWeight =
                    FontWeight.Bold,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            val selectedPersonality =
                aiPersonalityOptions()
                    .firstOrNull {
                        it.id ==
                                personality
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
                            text =
                                "▼",
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

                                    personality =
                                        option.id

                                    personalityMenuExpanded =
                                        false
                                }
                            )
                        }
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )

        SectionTitle(
            text =
                "STOP NAGGING"
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        ChoiceButton(
            text =
                "When I press DONE",
            selected =
                stopMode ==
                        StopMode.UNTIL_DONE,
            onClick = {

                stopMode =
                    StopMode.UNTIL_DONE
            }
        )

        ChoiceButton(
            text =
                "After a maximum number of nags",
            selected =
                stopMode ==
                        StopMode.MAX_NAGS,
            onClick = {

                stopMode =
                    StopMode.MAX_NAGS
            }
        )

        if (
            stopMode ==
            StopMode.MAX_NAGS
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Text(
                text =
                    "Maximum nags",
                color =
                    NagMeLightText,
                fontWeight =
                    FontWeight.SemiBold,
                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
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
                    text =
                        "−",
                    modifier =
                        Modifier.weight(1f),
                    enabled =
                        maxNagCount > 1,
                    onClick = {

                        if (
                            maxNagCount > 1
                        ) {
                            maxNagCount--
                        }
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
                            maxNagCount
                                .toString(),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                SecondaryButton(
                    text =
                        "+",
                    modifier =
                        Modifier.weight(1f),
                    enabled =
                        maxNagCount < 99,
                    onClick = {

                        if (
                            maxNagCount < 99
                        ) {
                            maxNagCount++
                        }
                    }
                )
            }
        }

        ChoiceButton(
            text =
                "After a maximum duration",
            selected =
                stopMode ==
                        StopMode.MAX_DURATION,
            onClick = {

                stopMode =
                    StopMode.MAX_DURATION
            }
        )

        if (
            stopMode ==
            StopMode.MAX_DURATION
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            listOf(
                5,
                30,
                60,
                120
            ).forEach { minutes ->

                ChoiceButton(
                    text =
                        when (
                            minutes
                        ) {

                            60 ->
                                "1 hour"

                            120 ->
                                "2 hours"

                            else ->
                                "$minutes minutes"
                        },
                    selected =
                        maxDurationMinutes ==
                                minutes,
                    onClick = {

                        maxDurationMinutes =
                            minutes
                    }
                )
            }
        }

        errorMessage
            ?.let { error ->

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Text(
                    text =
                        error,
                    color =
                        NagMeError,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

        Spacer(
            modifier =
                Modifier.height(
                    36.dp
                )
        )

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onCancel,
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
                    "CANCEL"
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
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

                errorMessage =
                    null

                val stopWhenDone =
                    stopMode ==
                            StopMode.UNTIL_DONE

                val resolvedMaxNagCount =
                    if (
                        stopMode ==
                        StopMode.MAX_NAGS
                    ) {
                        maxNagCount
                    } else {
                        null
                    }

                val resolvedMaxDuration =
                    if (
                        stopMode ==
                        StopMode.MAX_DURATION
                    ) {
                        maxDurationMinutes
                    } else {
                        null
                    }

                viewModel.saveNag(
                    text =
                        text,
                    scheduledAt =
                        scheduledAt,
                    recurrenceType =
                        recurrenceType,
                    selectedDays =
                        selectedDays,
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
                    stopWhenDone =
                        stopWhenDone,
                    maxNagCount =
                        resolvedMaxNagCount,
                    maxNagDurationMinutes =
                        resolvedMaxDuration,

                    /*
                     * New per-Nag AI settings.
                     */
                    aiEnabled =
                        aiEnabled,
                    personality =
                        personality,

                    onSaved =
                        onSaved,
                    onError = { error ->

                        errorMessage =
                            error
                    }
                )
            }
        ) {

            Text(
                text =
                    "SAVE NAG",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )
    }
}

@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text =
            text,
        color =
            NagMeTeal,
        fontWeight =
            FontWeight.Bold,
        style =
            MaterialTheme
                .typography
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
            Modifier.height(
                7.dp
            )
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

            Text(
                text =
                    text
            )
        }
    }
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
                day in
                selectedDays
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

                        onToggle(
                            day
                        )
                    }
                ) {

                    Text(
                        text =
                            label,
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

                        onToggle(
                            day
                        )
                    }
                ) {

                    Text(
                        text =
                            label
                    )
                }
            }
        }
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
            text =
                text,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

private fun recurrenceOptions():
        List<Pair<RecurrenceType, String>> {

    return listOf(

        RecurrenceType.ONCE to
                "One time only",

        RecurrenceType.DAILY to
                "Every day",

        RecurrenceType.WEEKDAYS to
                "Weekdays",

        RecurrenceType.WEEKENDS to
                "Weekends",

        RecurrenceType.WEEKLY to
                "Every week",

        RecurrenceType.SELECTED_DAYS to
                "Choose days",

        RecurrenceType.DURING_TIME_PERIOD to
                "During time period"
    )
}

/*
 * Stable IDs are deliberately separate from the
 * user-facing personality names.
 *
 * The IDs will later be sent to Cloud Run/Gemini.
 */
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
            id = "karen",
            label = "Karen"
        ),

        AiPersonality(
            id = "foul_mouthed_fury",
            label = "Foul-Mouthed Fury"
        ),

        AiPersonality(
            id = "the_tyrant",
            label = "The Tyrant"
        ),

        AiPersonality(
            id = "mafia_boss",
            label = "Mafia Boss"
        ),

        AiPersonality(
            id = "trump",
            label = "Trump"
        )
    )
}

private enum class StopMode {
    UNTIL_DONE,
    MAX_NAGS,
    MAX_DURATION
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

private fun formatMinutesOfDay(
    minutesOfDay: Int
): String {

    val calendar =
        Calendar
            .getInstance()
            .apply {

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
 * NagMe V1 visual palette.
 *
 * These values are intentionally local to this screen for now.
 */
private val NagMeNavy =
    Color(0xFF06101F)

private val NagMeField =
    Color(0xFF111C32)

private val NagMeBorder =
    Color(0xFF506078)

private val NagMeTeal =
    Color(0xFF21D4C2)

private val NagMePurple =
    Color(0xFF7657C9)

private val NagMeLightText =
    Color(0xFFD7DFE8)

private val NagMeMutedText =
    Color(0xFF8995A7)

private val NagMeDarkText =
    Color(0xFF031218)

private val NagMeError =
    Color(0xFFFF7A8A)
