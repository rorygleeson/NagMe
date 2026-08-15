package com.example.nagme.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChooseVoice: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {

    val settings by
    viewModel.settings.collectAsState()

    val context =
        LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            /*
             * Existing permission behaviour preserved.
             */
            viewModel.setShowNagNotifications(
                granted
            )
        }

    var editedName by remember(settings.userName) {
        mutableStateOf(settings.userName)
    }

    var showMissedWindowDialog by remember {
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
                text = "Settings",
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
            text = "PROFILE"
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

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                OutlinedTextField(
                    value =
                        editedName,
                    onValueChange = {
                        editedName = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text("Name")
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
                                cursorColor =
                                    NagMeTeal,
                                focusedLabelColor =
                                    NagMeTeal,
                                unfocusedLabelColor =
                                    NagMeMutedText,
                                focusedContainerColor =
                                    NagMeField,
                                unfocusedContainerColor =
                                    NagMeField
                            )
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),
                    enabled =
                        editedName.isNotBlank() &&
                                editedName.trim() !=
                                settings.userName,
                    onClick = {

                        viewModel.updateName(
                            editedName
                        )
                    },
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    NagMeTeal
                            )
                ) {

                    Text(
                        text = "SAVE NAME",
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(26.dp)
        )

        SectionTitle(
            text = "VOICE"
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

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                Text(
                    text = "Default voice",
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
                        friendlyStoredVoiceName(
                            settings.defaultVoiceName
                        ),
                    color =
                        NagMeMutedText,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick =
                        onChooseVoice,
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    NagMeTeal
                            )
                ) {

                    Text(
                        text =
                            "CHANGE VOICE",
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
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
                                "Speak when phone is on Silent",
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
                                "NagMe will still speak when Silent mode is enabled.",
                            color =
                                NagMeMutedText,
                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )
                    }

                    Switch(
                        checked =
                            settings.speakOnSilent,
                        onCheckedChange = { enabled ->

                            viewModel
                                .setSpeakOnSilent(
                                    enabled
                                )
                        },
                        colors =
                            nagMeSwitchColors()
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(26.dp)
        )

        SectionTitle(
            text = "NAGGING"
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

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

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
                                "Show Nag notifications",
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
                                "Show a notification whenever NagMe nags you.",
                            color =
                                NagMeMutedText,
                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )
                    }

                    Switch(
                        checked =
                            settings.showNagNotifications,
                        onCheckedChange = { enabled ->

                            /*
                             * Existing notification logic preserved
                             * exactly.
                             */
                            if (!enabled) {

                                viewModel
                                    .setShowNagNotifications(
                                        false
                                    )

                            } else {

                                if (
                                    Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.TIRAMISU
                                ) {

                                    val permissionGranted =
                                        ContextCompat
                                            .checkSelfPermission(
                                                context,
                                                Manifest.permission
                                                    .POST_NOTIFICATIONS
                                            ) ==
                                                PackageManager
                                                    .PERMISSION_GRANTED

                                    if (
                                        permissionGranted
                                    ) {

                                        viewModel
                                            .setShowNagNotifications(
                                                true
                                            )

                                    } else {

                                        notificationPermissionLauncher
                                            .launch(
                                                Manifest.permission
                                                    .POST_NOTIFICATIONS
                                            )
                                    }

                                } else {

                                    viewModel
                                        .setShowNagNotifications(
                                            true
                                        )
                                }
                            }
                        },
                        colors =
                            nagMeSwitchColors()
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )

                Text(
                    text =
                        "Missed Nag window",
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
                        formatMissedWindow(
                            settings
                                .missedNagWindowMinutes
                        ),
                    color =
                        NagMePurpleLight,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick = {
                        showMissedWindowDialog =
                            true
                    },
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    NagMeTeal
                            )
                ) {

                    Text(
                        text =
                            "CHANGE MISSED NAG WINDOW",
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(26.dp)
        )

        SectionTitle(
            text = "ABOUT"
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

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                Row {

                    Text(
                        text = "Nag",
                        color =
                            Color.White,
                        fontWeight =
                            FontWeight.Bold,
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )

                    Text(
                        text = "Me",
                        color =
                            NagMeTeal,
                        fontWeight =
                            FontWeight.Bold,
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        "Version 1.0",
                    color =
                        NagMeMutedText,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }

    if (
        showMissedWindowDialog
    ) {

        MissedWindowDialog(
            currentMinutes =
                settings
                    .missedNagWindowMinutes,
            onDismiss = {
                showMissedWindowDialog =
                    false
            },
            onSave = { minutes ->

                viewModel
                    .setMissedNagWindowMinutes(
                        minutes
                    )

                showMissedWindowDialog =
                    false
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
                .labelLarge
    )
}

@Composable
private fun MissedWindowDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {

    var selectedMinutes by remember {
        mutableIntStateOf(
            currentMinutes
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        containerColor =
            NagMeCard,
        title = {

            Text(
                text =
                    "Missed Nag window",
                color =
                    Color.White,
                fontWeight =
                    FontWeight.Bold
            )
        },
        text = {

            Column {

                Text(
                    text =
                        "If the phone was unavailable when a Nag was due, how late should NagMe still deliver it?",
                    color =
                        NagMeLightText
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                listOf(
                    30,
                    60,
                    120,
                    240
                ).forEach { minutes ->

                    if (
                        selectedMinutes ==
                        minutes
                    ) {

                        androidx.compose.material3.Button(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                selectedMinutes =
                                    minutes
                            },
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
                                    )
                        ) {

                            Text(
                                text =
                                    "✓ ${
                                        formatMissedWindow(
                                            minutes
                                        )
                                    }",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                    } else {

                        OutlinedButton(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                selectedMinutes =
                                    minutes
                            },
                            shape =
                                RoundedCornerShape(
                                    14.dp
                                ),
                            colors =
                                ButtonDefaults
                                    .outlinedButtonColors(
                                        contentColor =
                                            NagMeLightText
                                    )
                        ) {

                            Text(
                                text =
                                    formatMissedWindow(
                                        minutes
                                    )
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )
                }
            }
        },
        confirmButton = {

            TextButton(
                onClick = {
                    onSave(
                        selectedMinutes
                    )
                }
            ) {

                Text(
                    text = "SAVE",
                    color =
                        NagMeTeal,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
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

@Composable
private fun nagMeSwitchColors() =
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

private fun formatMissedWindow(
    minutes: Int
): String {

    return when {

        minutes < 60 ->
            "$minutes minutes"

        minutes == 60 ->
            "1 hour"

        minutes % 60 == 0 ->
            "${minutes / 60} hours"

        else ->
            "$minutes minutes"
    }
}

private fun friendlyStoredVoiceName(
    voiceName: String?
): String {

    if (
        voiceName.isNullOrBlank()
    ) {
        return "Phone default"
    }

    val cleaned =
        voiceName
            .removeSuffix(
                "-local"
            )
            .removeSuffix(
                "-network"
            )

    val shortName =
        cleaned
            .split("-")
            .lastOrNull()
            ?.uppercase()
            ?: cleaned.uppercase()

    return "Voice $shortName"
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

private val NagMePurpleLight =
    Color(0xFFB690FF)

private val NagMeLightText =
    Color(0xFFD7DFE8)

private val NagMeMutedText =
    Color(0xFF8995A7)

private val NagMeDarkText =
    Color(0xFF031218)