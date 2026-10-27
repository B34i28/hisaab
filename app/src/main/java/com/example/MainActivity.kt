package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.ShopLedgerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.AppLanguage
import com.example.util.EnglishStrings
import com.example.util.LocalAppLanguage
import com.example.util.LocalAppStrings
import com.example.util.ThemeMode
import com.example.util.UrduStrings

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ShopViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

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
                    ShopLedgerApp(viewModel = viewModel)
                }
            }
        }
    }
}
