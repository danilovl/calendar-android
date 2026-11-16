package danilovl.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * The app defines every color itself. System dynamic colors (Material You, Android 12+) are not used:
 * they derive the palette from the wallpaper and tint menus, switches, checkboxes, etc.
 *
 * All roles are set explicitly — roles left unset fall back to the Material 3 baseline palette,
 * which is purple/lavender.
 */

private val LightColorScheme = lightColorScheme(
    primary = XiaomiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F0FF),
    onPrimaryContainer = Color(0xFF00296B),
    inversePrimary = Color(0xFF9EC2FF),

    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = XiaomiTextPrimary,

    tertiary = XiaomiBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6F0FF),
    onTertiaryContainer = Color(0xFF00296B),

    error = XiaomiRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF7A0000),

    background = XiaomiBg,
    onBackground = XiaomiTextPrimary,

    surface = XiaomiSurface,
    onSurface = XiaomiTextPrimary,
    surfaceVariant = Color(0xFFEFEFF4),
    onSurfaceVariant = Color(0xFF6E6E73),
    surfaceTint = Color.Transparent, // no colored overlay on elevated surfaces

    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,          // dropdown menus
    surfaceContainerHigh = Color.White,      // dialogs
    surfaceContainerHighest = Color(0xFFE5E5EA), // unchecked switch track

    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF2F2F7),

    outline = Color(0xFFAEAEB2),        // unchecked switch thumb/border
    outlineVariant = Color(0xFFE5E5EA), // dividers
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = XiaomiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3D8F),
    onPrimaryContainer = Color(0xFFD6E4FF),
    inversePrimary = XiaomiBlue,

    secondary = Color(0xFFAEAEB2),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A3A3C),
    onSecondaryContainer = Color.White,

    tertiary = XiaomiBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0A3D8F),
    onTertiaryContainer = Color(0xFFD6E4FF),

    error = XiaomiRed,
    onError = Color.White,
    errorContainer = Color(0xFF7A0000),
    onErrorContainer = Color(0xFFFFE5E5),

    background = Color.Black,
    onBackground = Color.White,

    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceTint = Color.Transparent,

    surfaceBright = Color(0xFF3A3A3C),
    surfaceDim = Color(0xFF121212),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF3A3A3C),

    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1A1A1A),

    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF3A3A3C),
    scrim = Color.Black
)

@Composable
fun CalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
