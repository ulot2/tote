package app.tote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.tote.core.data.settings.Accent
import app.tote.core.data.settings.ThemeMode

/** Shape tokens: large card radii, pill buttons and chips (Plan §8.2). */
object ToteShapes {
    val card = RoundedCornerShape(22.dp)
    val cardLarge = RoundedCornerShape(26.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val field = RoundedCornerShape(18.dp)
    val badge = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** 4dp grid. */
object ToteSpacing {
    const val unit = 4
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val screen = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Content is centred and capped at this width on tablets and in landscape. */
    val maxContentWidth = 640.dp
}

object Tuck {
    val colors: ToteColors @Composable @ReadOnlyComposable get() = LocalToteColors.current
    val type: ToteType @Composable @ReadOnlyComposable get() = LocalToteType.current
}

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun ToteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accent: Accent = Accent.IRIS,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = remember(darkTheme, dynamicColor, accent) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            TotePalette.fromDynamic(scheme, darkTheme)
        } else {
            TotePalette.colors(darkTheme, accent)
        }
    }
    val type = remember { ToteType() }

    val material = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft, onPrimaryContainer = colors.textPrimary,
            secondary = colors.textSecondary, onSecondary = colors.background,
            background = colors.background, onBackground = colors.textPrimary,
            surface = colors.surface, onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised, onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = colors.background, surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surfaceRaised,
            surfaceContainerHighest = colors.surfaceHigh,
            outline = colors.outlineStrong, outlineVariant = colors.outline,
            error = colors.danger, scrim = colors.scrim,
        )
    } else {
        lightColorScheme(
            primary = colors.accentContent, onPrimary = colors.surface,
            primaryContainer = colors.accent, onPrimaryContainer = colors.onAccent,
            secondary = colors.textSecondary, onSecondary = colors.background,
            background = colors.background, onBackground = colors.textPrimary,
            surface = colors.surface, onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised, onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = colors.surface, surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surfaceRaised,
            surfaceContainerHighest = colors.surfaceHigh,
            outline = colors.outlineStrong, outlineVariant = colors.outline,
            error = colors.danger, scrim = colors.scrim,
        )
    }

    CompositionLocalProvider(LocalToteColors provides colors, LocalToteType provides type) {
        MaterialTheme(
            colorScheme = material,
            typography = type.toMaterial(),
            shapes = Shapes(
                extraSmall = ToteShapes.badge, small = ToteShapes.field, medium = ToteShapes.card,
                large = ToteShapes.cardLarge, extraLarge = ToteShapes.cardLarge,
            ),
            content = content,
        )
    }
}
