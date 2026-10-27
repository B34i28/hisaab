package com.example.ui.screens.lock

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BackupUtils
import com.example.util.BiometricAuthUtils
import com.example.util.LocalAppStrings
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsStateWithLifecycle()
    val isSecurityLoaded by viewModel.isSecurityLoaded.collectAsStateWithLifecycle()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Lockout delay logic (5 failed attempts -> 30s lockout, doubling on each subsequent failed round)
    var failedAttemptsCount by remember { mutableIntStateOf(0) }
    var lockoutRound by remember { mutableIntStateOf(1) }
    var lockoutSecondsRemaining by remember { mutableIntStateOf(0) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showConfirmClearDataDialog by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    val hasBiometric = remember { BiometricAuthUtils.canAuthenticate(context) }

    // Backup restore launcher for Forgot PIN flow
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val jsonString = BackupUtils.readFromUri(context, uri)
            if (jsonString != null) {
                try {
                    val backup = BackupUtils.parseBackupJson(jsonString)
                    viewModel.restoreBackup(
                        backup = backup,
                        onSuccess = {
                            viewModel.unlockApp()
                        },
                        onError = { err ->
                            errorMessage = err
                        }
                    )
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Invalid backup file"
                }
            }
        }
    }

    // Lockout countdown timer
    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            delay(1000L)
            lockoutSecondsRemaining -= 1
        }
    }

    fun triggerBiometricPrompt() {
        if (activity != null && isFingerprintEnabled && hasBiometric) {
            BiometricAuthUtils.authenticate(
                activity = activity,
                title = "Hisaab Fingerprint",
                subtitle = strings.fingerprintNote,
                negativeButtonText = strings.useHisaabPin,
                onSuccess = {
                    viewModel.unlockApp()
                },
                onError = { err ->
                    errorMessage = err
                },
                onFallbackToPin = {
                    // Falls back to in-app PIN screen seamlessly
                }
            )
        }
    }

    // Trigger fingerprint prompt on initial display if enabled
    LaunchedEffect(isFingerprintEnabled) {
        if (isFingerprintEnabled && hasBiometric && activity != null) {
            triggerBiometricPrompt()
        }
    }

    var showLegacyResetDialog by remember { mutableStateOf(false) }
    var legacyNewPin by remember { mutableStateOf("") }
    var legacyConfirmPin by remember { mutableStateOf("") }
    var legacyResetError by remember { mutableStateOf<String?>(null) }

    fun submitPin(pin: String) {
        if (lockoutSecondsRemaining > 0) return

        if (viewModel.verifyPin(pin)) {
            failedAttemptsCount = 0
            lockoutRound = 1
            errorMessage = null
            enteredPin = ""
            if (pin.length > 4) {
                showLegacyResetDialog = true
            } else {
                viewModel.unlockApp()
            }
        } else {
            failedAttemptsCount++
            enteredPin = ""
            if (failedAttemptsCount >= 5) {
                val delaySecs = 30 * lockoutRound
                lockoutSecondsRemaining = delaySecs
                lockoutRound *= 2
                failedAttemptsCount = 0
                errorMessage = "Too many failed attempts. Try again in $delaySecs seconds."
            } else {
                val remaining = 5 - failedAttemptsCount
                errorMessage = "Incorrect PIN. $remaining attempts remaining."
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = strings.hisaabLocked,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.enterAppPinPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error or lockout countdown message
            if (lockoutSecondsRemaining > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Too many failed attempts. Try again in $lockoutSecondsRemaining seconds.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // PIN Dots Indicator (PIN_LENGTH dots by default, expands for legacy >4-digit PIN input)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                val dotCount = maxOf(com.example.util.PinHasher.PIN_LENGTH, enteredPin.length)
                repeat(dotCount) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Keypad (1-9, Fingerprint/Clear, 0, Submitting/Backspace)
            val isKeypadDisabled = lockoutSecondsRemaining > 0 || !isSecurityLoaded

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                val keyRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )

                keyRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "bio" -> {
                                    if (isFingerprintEnabled && hasBiometric) {
                                        IconButton(
                                            onClick = { triggerBiometricPrompt() },
                                            enabled = !isKeypadDisabled,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Fingerprint Unlock",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(64.dp))
                                    }
                                }
                                "del" -> {
                                    IconButton(
                                        onClick = {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorMessage = null
                                            }
                                        },
                                        enabled = !isKeypadDisabled,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isKeypadDisabled) MaterialTheme.colorScheme.surfaceContainerLow
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clickable(enabled = !isKeypadDisabled) {
                                                if (enteredPin.length < 6) {
                                                    val next = enteredPin + key
                                                    enteredPin = next
                                                    errorMessage = null
                                                    // Auto-submit if PIN matches at 4 digits, or at up to 6 digits for legacy PIN
                                                    if (viewModel.verifyPin(next)) {
                                                        submitPin(next)
                                                    } else if (next.length == 6) {
                                                        // 6 digits entered and failed -> submit to trigger error & counter
                                                        submitPin(next)
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp
                                                ),
                                                color = if (isKeypadDisabled) MaterialTheme.colorScheme.outline
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button if user enters 4 or more digits
            if (enteredPin.length >= 4) {
                Button(
                    onClick = { submitPin(enteredPin) },
                    enabled = !isKeypadDisabled,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .testTag("submit_pin_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Forgot PIN button
            TextButton(
                onClick = { showForgotPinDialog = true },
                modifier = Modifier.testTag("forgot_pin_button")
            ) {
                Text(
                    text = strings.forgotPin,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Forgot PIN Dialog
    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(text = strings.forgotPinTitle) },
            text = { Text(text = strings.forgotPinWarning) },
            confirmButton = {
                Button(
                    onClick = {
                        showForgotPinDialog = false
                        openDocLauncher.launch(arrayOf("application/json"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = strings.restoreFromBackup)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showForgotPinDialog = false
                        showConfirmClearDataDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = strings.clearAllData)
                }
            }
        )
    }

    // Secondary Confirmation Dialog for Clear All Data
    if (showConfirmClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDataDialog = false },
            title = { Text(text = strings.deleteConfirmTitle) },
            text = { Text(text = "Are you sure you want to clear all data and reset Hisaab? All shops and financial ledgers will be permanently erased.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDataDialog = false
                        viewModel.clearAllData {
                            viewModel.unlockApp()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = strings.clearAllData)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDataDialog = false }) {
                    Text(text = strings.cancel)
                }
            }
        )
    }

    // Mandatory 4-Digit PIN Reset Dialog for Legacy >4-Digit Users
    if (showLegacyResetDialog) {
        AlertDialog(
            onDismissRequest = { /* Modal, user must update PIN */ },
            title = {
                Text(
                    text = "Set New 4-Digit PIN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Hisaab PINs are now exactly 4 digits. Please set a new 4-digit PIN to continue:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = legacyNewPin,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                legacyNewPin = input
                                legacyResetError = null
                            }
                        },
                        label = { Text("New 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = legacyConfirmPin,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                legacyConfirmPin = input
                                legacyResetError = null
                            }
                        },
                        label = { Text("Confirm 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (legacyResetError != null) {
                        Text(legacyResetError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (legacyNewPin.length != com.example.util.PinHasher.PIN_LENGTH) {
                            legacyResetError = "PIN must be exactly ${com.example.util.PinHasher.PIN_LENGTH} digits."
                        } else if (legacyNewPin != legacyConfirmPin) {
                            legacyResetError = "PINs do not match."
                        } else {
                            viewModel.setCustomPin(legacyNewPin) {
                                showLegacyResetDialog = false
                                viewModel.unlockApp()
                            }
                        }
                    }
                ) {
                    Text("Save & Unlock")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
