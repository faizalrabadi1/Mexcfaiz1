package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TealPrimary,
    secondary = ObsidianGrey80,
    tertiary = ElectricBlue,
    background = DeepCarbonBackground,
    surface = ObsidianGrey80,
    onPrimary = Color.White,
    onSecondary = PlatinumWhite,
    onTertiary = PlatinumWhite,
    onBackground = PlatinumWhite,
    onSurface = PlatinumWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimary,
    secondary = ObsidianGrey80,
    tertiary = ElectricBlue,
    background = DeepCarbonBackground,
    surface = ObsidianGrey80,
    onPrimary = Color.White,
    onSecondary = PlatinumWhite,
    onTertiary = PlatinumWhite,
    onBackground = PlatinumWhite,
    onSurface = PlatinumWhite
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Light by default for Clean Minimalism
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme // Force clean light scheme for the dashboard terminal

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
