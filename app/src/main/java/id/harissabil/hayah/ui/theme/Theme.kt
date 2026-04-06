package id.harissabil.hayah.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = Primary,
        primaryContainer = PrimaryContainer,
        secondary = Secondary,
        secondaryContainer = SecondaryContainer,
        tertiary = Tertiary,
        tertiaryContainer = TertiaryContainer,
        surface = Surface,
        onSurface = OnSurface,
        background = Surface,
        onBackground = OnSurface,
        surfaceContainerLowest = SurfaceContainerLowest,
        surfaceContainerLow = SurfaceContainerLow,
        surfaceContainerHigh = SurfaceContainerHighest,
        outlineVariant = OutlineVariant,
        inverseOnSurface = InverseOnSurface,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        primaryContainer = PrimaryContainerDark,
        secondary = SecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        tertiary = TertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        background = SurfaceDark,
        onBackground = OnSurfaceDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainerHigh = SurfaceContainerHighestDark,
        outlineVariant = OutlineVariantDark,
        inverseOnSurface = InverseOnSurfaceDark,
    )

@Composable
fun HayahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+, disabled by default for Hayah branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme.switch(isDark = darkTheme),
        typography = Typography,
        content = content,
    )
}

@Composable
private fun animateColor(targetValue: Color) =
    animateColorAsState(
        targetValue = targetValue,
        label = "AnimateColor",
    ).value

@Composable
fun ColorScheme.switch(isDark: Boolean) =
    copy(
        primary = animateColor(if (isDark) PrimaryDark else Primary),
        primaryContainer = animateColor(if (isDark) PrimaryContainerDark else PrimaryContainer),
        secondary = animateColor(if (isDark) SecondaryDark else Secondary),
        secondaryContainer = animateColor(if (isDark) SecondaryContainerDark else SecondaryContainer),
        tertiary = animateColor(if (isDark) TertiaryDark else Tertiary),
        tertiaryContainer = animateColor(if (isDark) TertiaryContainerDark else TertiaryContainer),
        surface = animateColor(if (isDark) SurfaceDark else Surface),
        onSurface = animateColor(if (isDark) OnSurfaceDark else OnSurface),
        background = animateColor(if (isDark) SurfaceDark else Surface),
        onBackground = animateColor(if (isDark) OnSurfaceDark else OnSurface),
        surfaceContainerLowest = animateColor(if (isDark) SurfaceContainerLowestDark else SurfaceContainerLowest),
        surfaceContainerLow = animateColor(if (isDark) SurfaceContainerLowDark else SurfaceContainerLow),
        surfaceContainerHigh = animateColor(if (isDark) SurfaceContainerHighestDark else SurfaceContainerHighest),
        outlineVariant = animateColor(if (isDark) OutlineVariantDark else OutlineVariant),
        inverseOnSurface = animateColor(if (isDark) InverseOnSurfaceDark else InverseOnSurface),
    )
