package com.pomori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.pomori.ui.PomoriRoot
import com.pomori.ui.theme.PomoriTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomoriTheme {
                // Scaffold'un iç boşluğu yok: her ekran kendi zeminini (ve ışımasını) durum çubuğunun altına
                // kadar çizer, içeriğini ise safeDrawingPadding ile güvenli alana oturtur.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    PomoriRoot(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
