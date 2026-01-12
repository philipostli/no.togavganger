package no.togavganger.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme

@Composable
fun TogavgangerTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        content = content
    )
}

// Helper functions to access Material 3 colors that match protolayout-material3
// These are the exact values from protolayout-material3 colorScheme
// Logged from tile and converted from signed int to unsigned hex:
@Composable
fun getTertiaryContainerColor(): Color = Color(0xFF6C3A03) // From argb=-9684477

@Composable
fun getOnTertiaryContainerColor(): Color = Color(0xFFFFEEE2) // From argb=-4382

@Composable
fun getSurfaceContainerColor(): Color = Color(0xFF332E3C) // From tile: surfaceContainer

@Composable
fun getOnSurfaceColor(): Color = Color(0xFFF6EDFF) // From tile: onSurface
