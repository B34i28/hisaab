package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.HisaabApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.AppLanguage
import com.example.util.EnglishStrings
import com.example.util.LocalAppLanguage
import com.example.util.LocalAppStrings
import com.example.util.ThemeMode
import com.example.util.UrduStrings

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: ShopViewModel
    private var pauseTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[ShopViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()

            // Note: FLAG_SECURE is omitted here so streaming emulator preview in browser can capture and render the app window

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val appStrings = if (appLanguage == AppLanguage.URDU) UrduStrings else EnglishStrings
            val layoutDirection = if (appLanguage == AppLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalAppStrings provides appStrings,
                LocalAppLanguage provides appLanguage,
                LocalLayoutDirection provides layoutDirection
            ) {
                MyApplicationTheme(darkTheme = isDark) {
                    HisaabApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pauseTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (pauseTimestamp > 0L && viewModel.isAppLockEnabled.value) {
            val elapsedMs = System.currentTimeMillis() - pauseTimestamp
            val timeoutSecs = viewModel.autoLockTimeoutSeconds.value
            if (elapsedMs >= timeoutSecs * 1000L) {
                viewModel.lockApp()
            }
        }
        pauseTimestamp = 0L
    }
}
