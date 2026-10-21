package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.UserPreferences
import com.example.security.PinSecurityHelper
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning
import kotlinx.coroutines.delay

/**
 * Visual dots representing 4-digit PIN input state.
 */
@Composable
fun PinDotsIndicator(
    enteredDigitsCount: Int,
    totalDigits: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalDigits) {
            val isFilled = i < enteredDigitsCount
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) PrimaryBlue else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (isFilled) PrimaryBlue else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Tactile numeric keypad for PIN input.
 */
@Composable
fun NumericKeypad(
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9')
        )

        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        text = digit.toString(),
                        onClick = { if (isEnabled) onDigitClick(digit) },
                        isEnabled = isEnabled
                    )
                }
            }
        }

        // Bottom row: Empty/Done placeholder, 0, Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(68.dp)) // Spacer

            KeypadButton(
                text = "0",
                onClick = { if (isEnabled) onDigitClick('0') },
                isEnabled = isEnabled
            )

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(enabled = isEnabled) { onBackspaceClick() }
                    .testTag("pin_keypad_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .size(68.dp)
            .testTag("pin_keypad_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

/**
 * Reusable PIN Entry Card / Body inside dialogs.
 */
@Composable
fun PinEntryBody(
    title: String,
    subtitle: String,
    currentPin: String,
    errorMessage: String? = null,
    lockoutSeconds: Int = 0,
    onDigitEntered: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            PinDotsIndicator(enteredDigitsCount = currentPin.length)

            Spacer(modifier = Modifier.height(14.dp))

            // Error or Lockout Notice
            if (lockoutSeconds > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(WellbeingRestriction.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WellbeingRestriction,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Too many attempts. Locked for ${lockoutSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellbeingRestriction
                    )
                }
            } else if (!errorMessage.isNullOrEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = WellbeingRestriction,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            NumericKeypad(
                onDigitClick = onDigitEntered,
                onBackspaceClick = onBackspace,
                isEnabled = lockoutSeconds == 0
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_cancel_button")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Dialog to verify PIN before allowing a protected action.
 */
@Composable
fun VerifyPinDialog(
    userPreferences: UserPreferences,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    onRecordFailedAttempt: () -> Unit,
    onResetFailedAttempts: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(userPreferences.lockoutSecondsRemaining) }

    LaunchedEffect(userPreferences.lockoutUntilTimestamp) {
        while (true) {
            val remaining = userPreferences.lockoutSecondsRemaining
            lockoutSeconds = remaining
            if (remaining <= 0) break
            delay(1000L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        PinEntryBody(
            title = "🔒 Enter PIN",
            subtitle = "Enter your 4-digit PIN to continue.",
            currentPin = enteredPin,
            errorMessage = errorMessage,
            lockoutSeconds = lockoutSeconds,
            onDigitEntered = { digit ->
                if (enteredPin.length < 4) {
                    val newPin = enteredPin + digit
                    enteredPin = newPin
                    errorMessage = null

                    if (newPin.length == 4) {
                        // Validate PIN
                        val matches = PinSecurityHelper.verifyPin(
                            enteredPin = newPin,
                            salt = userPreferences.pinSalt,
                            expectedHash = userPreferences.pinHash
                        )
                        if (matches) {
                            onResetFailedAttempts()
                            onSuccess()
                            onDismiss()
                        } else {
                            onRecordFailedAttempt()
                            errorMessage = "Incorrect PIN."
                            enteredPin = ""
                        }
                    }
                }
            },
            onBackspace = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    errorMessage = null
                }
            },
            onCancel = onDismiss
        )
    }
}

/**
 * Full Set PIN Flow:
 * Step 1: Set 4-digit PIN
 * Step 2: Confirm PIN
 * Step 3: Success Confirmation
 */
@Composable
fun SetPinDialog(
    onPinCreated: (pinHash: String, salt: String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1 = Enter, 2 = Confirm, 3 = Success
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        when (step) {
            1 -> {
                PinEntryBody(
                    title = "Set a 4-Digit PIN",
                    subtitle = "Choose a 4-digit PIN to protect your ScrollLock settings.",
                    currentPin = firstPin,
                    errorMessage = errorMessage,
                    onDigitEntered = { digit ->
                        if (firstPin.length < 4) {
                            firstPin += digit
                            errorMessage = null
                            if (firstPin.length == 4) {
                                step = 2
                            }
                        }
                    },
                    onBackspace = {
                        if (firstPin.isNotEmpty()) {
                            firstPin = firstPin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onCancel = onDismiss
                )
            }
            2 -> {
                PinEntryBody(
                    title = "Confirm Your PIN",
                    subtitle = "Enter PIN Again to confirm.",
                    currentPin = confirmPin,
                    errorMessage = errorMessage,
                    onDigitEntered = { digit ->
                        if (confirmPin.length < 4) {
                            val newConfirm = confirmPin + digit
                            confirmPin = newConfirm
                            errorMessage = null
                            if (newConfirm.length == 4) {
                                if (newConfirm == firstPin) {
                                    // Match! Hash and save
                                    val salt = PinSecurityHelper.generateSalt()
                                    val hash = PinSecurityHelper.hashPin(firstPin, salt)
                                    onPinCreated(hash, salt)
                                    step = 3
                                } else {
                                    errorMessage = "PINs do not match. Please try again."
                                    confirmPin = ""
                                    firstPin = ""
                                    step = 1
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onCancel = onDismiss
                )
            }
            3 -> {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WellbeingSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WellbeingSuccess,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "✓ PIN Created Successfully",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your ScrollLock settings are now protected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pin_created_done_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Done", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Change PIN Flow:
 * Step 1: Enter Current PIN
 * Step 2: Enter New 4-Digit PIN
 * Step 3: Confirm New PIN
 * Step 4: Success confirmation
 */
@Composable
fun ChangePinDialog(
    userPreferences: UserPreferences,
    onPinChanged: (pinHash: String, salt: String) -> Unit,
    onRecordFailedAttempt: () -> Unit,
    onResetFailedAttempts: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1 = Verify Current, 2 = New, 3 = Confirm New, 4 = Success
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(userPreferences.lockoutSecondsRemaining) }

    LaunchedEffect(userPreferences.lockoutUntilTimestamp) {
        while (true) {
            val remaining = userPreferences.lockoutSecondsRemaining
            lockoutSeconds = remaining
            if (remaining <= 0) break
            delay(1000L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        when (step) {
            1 -> {
                PinEntryBody(
                    title = "Enter Current PIN",
                    subtitle = "Verify your current PIN to change it.",
                    currentPin = currentPinInput,
                    errorMessage = errorMessage,
                    lockoutSeconds = lockoutSeconds,
                    onDigitEntered = { digit ->
                        if (currentPinInput.length < 4) {
                            val next = currentPinInput + digit
                            currentPinInput = next
                            errorMessage = null
                            if (next.length == 4) {
                                val valid = PinSecurityHelper.verifyPin(
                                    enteredPin = next,
                                    salt = userPreferences.pinSalt,
                                    expectedHash = userPreferences.pinHash
                                )
                                if (valid) {
                                    onResetFailedAttempts()
                                    step = 2
                                } else {
                                    onRecordFailedAttempt()
                                    errorMessage = "Incorrect PIN."
                                    currentPinInput = ""
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (currentPinInput.isNotEmpty()) {
                            currentPinInput = currentPinInput.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onCancel = onDismiss
                )
            }
            2 -> {
                PinEntryBody(
                    title = "Enter New 4-Digit PIN",
                    subtitle = "Create your new 4-digit PIN.",
                    currentPin = newPinInput,
                    errorMessage = errorMessage,
                    onDigitEntered = { digit ->
                        if (newPinInput.length < 4) {
                            newPinInput += digit
                            errorMessage = null
                            if (newPinInput.length == 4) {
                                step = 3
                            }
                        }
                    },
                    onBackspace = {
                        if (newPinInput.isNotEmpty()) {
                            newPinInput = newPinInput.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onCancel = onDismiss
                )
            }
            3 -> {
                PinEntryBody(
                    title = "Confirm New PIN",
                    subtitle = "Enter your new PIN again to confirm.",
                    currentPin = confirmNewPinInput,
                    errorMessage = errorMessage,
                    onDigitEntered = { digit ->
                        if (confirmNewPinInput.length < 4) {
                            val next = confirmNewPinInput + digit
                            confirmNewPinInput = next
                            errorMessage = null
                            if (next.length == 4) {
                                if (next == newPinInput) {
                                    val salt = PinSecurityHelper.generateSalt()
                                    val hash = PinSecurityHelper.hashPin(newPinInput, salt)
                                    onPinChanged(hash, salt)
                                    step = 4
                                } else {
                                    errorMessage = "PINs do not match. Please try again."
                                    confirmNewPinInput = ""
                                    newPinInput = ""
                                    step = 2
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (confirmNewPinInput.isNotEmpty()) {
                            confirmNewPinInput = confirmNewPinInput.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onCancel = onDismiss
                )
            }
            4 -> {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WellbeingSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WellbeingSuccess,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "✓ PIN Changed Successfully",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your new PIN has been saved and is now active.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pin_changed_done_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Done", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Remove PIN Flow:
 * Step 1: Verify current PIN
 * Step 2: Confirmation Dialog
 */
@Composable
fun RemovePinDialog(
    userPreferences: UserPreferences,
    onPinRemoved: () -> Unit,
    onRecordFailedAttempt: () -> Unit,
    onResetFailedAttempts: () -> Unit,
    onDismiss: () -> Unit
) {
    var isVerified by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(userPreferences.lockoutSecondsRemaining) }

    LaunchedEffect(userPreferences.lockoutUntilTimestamp) {
        while (true) {
            val remaining = userPreferences.lockoutSecondsRemaining
            lockoutSeconds = remaining
            if (remaining <= 0) break
            delay(1000L)
        }
    }

    if (!isVerified) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = true)
        ) {
            PinEntryBody(
                title = "Enter Current PIN",
                subtitle = "Enter your PIN to remove PIN protection.",
                currentPin = enteredPin,
                errorMessage = errorMessage,
                lockoutSeconds = lockoutSeconds,
                onDigitEntered = { digit ->
                    if (enteredPin.length < 4) {
                        val next = enteredPin + digit
                        enteredPin = next
                        errorMessage = null
                        if (next.length == 4) {
                            val valid = PinSecurityHelper.verifyPin(
                                enteredPin = next,
                                salt = userPreferences.pinSalt,
                                expectedHash = userPreferences.pinHash
                            )
                            if (valid) {
                                onResetFailedAttempts()
                                isVerified = true
                            } else {
                                onRecordFailedAttempt()
                                errorMessage = "Incorrect PIN."
                                enteredPin = ""
                            }
                        }
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                onCancel = onDismiss
            )
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WellbeingRestriction,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Remove PIN Protection?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "This will allow anyone using this device to change ScrollLock settings.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPinRemoved()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WellbeingRestriction),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_remove_pin_button")
                ) {
                    Text("Remove PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
