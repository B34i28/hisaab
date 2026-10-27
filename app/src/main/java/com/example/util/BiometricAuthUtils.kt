package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthUtils {

    /**
     * Checks if the device has strong biometric hardware and enrolled fingerprints.
     */
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Prompts the user with BiometricPrompt (BIOMETRIC_STRONG only).
     * Does NOT use DEVICE_CREDENTIAL or setDeviceCredentialAllowed.
     * Provides a negative button ("Use Hisaab PIN") that falls back to in-app PIN.
     * Automatically falls back to PIN after 3 failed fingerprint attempts.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Hisaab Fingerprint",
        subtitle: String = "Touch the fingerprint sensor to unlock",
        negativeButtonText: String = "Use Hisaab PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallbackToPin: () -> Unit
    ) {
        var failedAttempts = 0
        val executor = ContextCompat.getMainExecutor(activity)

        lateinit var biometricPrompt: BiometricPrompt

        biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onFallbackToPin()
                    } else {
                        onError(errString.toString())
                        onFallbackToPin()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    failedAttempts++
                    if (failedAttempts >= 3) {
                        biometricPrompt.cancelAuthentication()
                        onFallbackToPin()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
