package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpaceScanColorScheme = darkColorScheme(
  primary = CyanNeon,
  onPrimary = Color(0xFF041421),
  primaryContainer = DeepBlueAccent,
  onPrimaryContainer = CyanNeon,
  secondary = VioletNeon,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF2E1C6A),
  onSecondaryContainer = PurpleNeon,
  tertiary = ElectricBlue,
  onTertiary = Color.White,
  background = SpaceDarkBg,
  onBackground = TextPrimary,
  surface = SpaceSurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = SpaceSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = SpaceCardBorder,
  outlineVariant = Color(0xFF1E293B),
  error = ScanRescanRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force modern dark high-tech theme for scanner aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SpaceScanColorScheme,
    typography = Typography,
    content = content
  )
}
