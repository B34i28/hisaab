package com.example.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.util.BiometricAuthUtils

@Composable
fun AppLockScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val customPin by viewModel.customPin.collectAsStateWithLifecycle()
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val activity = context as? FragmentActivity
    val hasDeviceAuth = remember { BiometricAuthUtils.canAuthenticate(context) }

    fun triggerBiometricPrompt() {
        if (activity != null && hasDeviceAuth) {
            BiometricAuthUtils.authenticate(
                activity = activity,
                title = "Unlock ShopLedger",
                subtitle = "Verify your biometric or device screen lock",
                onSuccess = {
                    viewModel.unlockApp()
                },
                onError = { error ->
                    errorMessage = error
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (hasDeviceAuth && activity != null) {
            triggerBiometricPrompt()
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
                text = "ShopLedger Locked",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (customPin != null) "Enter your 4-digit PIN or use device lock" else "Confirm your device credentials to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
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

            // PIN Dots Indicator (if custom PIN is enabled)
            if (customPin != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Numeric Keypad
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
                                        if (hasDeviceAuth) {
                                            IconButton(
                                                onClick = { triggerBiometricPrompt() },
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Use Biometrics",
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
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clickable {
                                                    if (enteredPin.length < 4) {
                                                        val next = enteredPin + key
                                                        enteredPin = next
                                                        errorMessage = null
                                                        if (next.length == 4) {
                                                            if (next == customPin) {
                                                                viewModel.unlockApp()
                                                            } else {
                                                                errorMessage = "Incorrect PIN. Try again."
                                                                enteredPin = ""
                                                            }
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
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasDeviceAuth) {
                Button(
                    onClick = { triggerBiometricPrompt() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .testTag("unlock_biometric_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock with Device Lock",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
