package com.pomori

import android.content.Context
import android.os.Bundle
import com.pomori.platform.locale.AppLocale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pomori.ui.PomoriRoot
import com.pomori.ui.theme.PomoriTheme
import com.pomori.domain.model.ThemeMode
import com.pomori.domain.repository.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsStore

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PomoriTheme(darkTheme = dark) {
                // Scaffold'un iç boşluğu yok: her ekran kendi zeminini (ve ışımasını) durum çubuğunun altına
                // kadar çizer, içeriğini ise safeDrawingPadding ile güvenli alana oturtur.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    PomoriRoot(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
