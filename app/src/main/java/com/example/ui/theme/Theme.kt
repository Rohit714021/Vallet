package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VaultDarkColorScheme = darkColorScheme(
  primary = ElectricMint,
  onPrimary = ObsidianBg,
  primaryContainer = ElectricMintDark,
  onPrimaryContainer = ElectricMint,
  secondary = CoolSapphire,
  onSecondary = ObsidianBg,
  secondaryContainer = CoolSapphireDark,
  onSecondaryContainer = CoolSapphire,
  tertiary = WarmAmber,
  onTertiary = ObsidianBg,
  tertiaryContainer = WarmAmberDark,
  onTertiaryContainer = WarmAmber,
  error = CoralCrimson,
  onError = ObsidianBg,
  errorContainer = CoralCrimsonDark,
  onErrorContainer = CoralCrimson,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = VaultSurface,
  onSurface = TextPrimary,
  surfaceVariant = VaultCard,
  onSurfaceVariant = TextSecondary,
  surfaceContainerLowest = ObsidianBg,
  surfaceContainerLow = VaultSurface,
  surfaceContainer = VaultSurfaceElevated,
  surfaceContainerHigh = VaultCard,
  surfaceContainerHighest = Color(0xFF243244),
  outline = VaultBorder,
  outlineVariant = Color(0xFF1D2836),
)

private val VaultLightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD1FAE5),
  onPrimaryContainer = Color(0xFF064E3B),
  secondary = LightSecondary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDBEAFE),
  onSecondaryContainer = Color(0xFF1E3A8A),
  tertiary = LightTertiary,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFEF3C7),
  onTertiaryContainer = Color(0xFF78350F),
  error = LightError,
  onError = Color.White,
  errorContainer = Color(0xFFFFE4E6),
  onErrorContainer = Color(0xFF881337),
  background = LightBg,
  onBackground = Color(0xFF0F172A),
  surface = LightSurface,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
)

val VaultShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ObsidianVaultTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) VaultDarkColorScheme else VaultLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = VaultTypography,
    shapes = VaultShapes,
    content = content,
  )
}
