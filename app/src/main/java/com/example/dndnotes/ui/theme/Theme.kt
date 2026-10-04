package com.example.dndnotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color

enum class DndTheme(
    val displayName: String,
    val baseColor: Color,
    val primaryColor: Color,
    val onPrimaryColor: Color = Color.White,
    val textColor: Color = Color.White
) {
    DARK_PARCHMENT("Dark Parchment", ThemeColors.DarkParchment, Color(0xFFE57373), Color.Black, Color.White),
    DEEP_FOREST("Deep Forest", ThemeColors.DeepForest, Color(0xFF81C784), Color.Black, Color.White),
    DUNGEON_STONE("Dungeon Stone", ThemeColors.DungeonStone, Color(0xFFAFB42B), Color.Black, Color.White),
    BLOOD_MOON("Blood Moon", ThemeColors.BloodMoon, Color(0xFFFF8A65), Color.Black, Color.White),
    OCEAN_DEPTHS("Ocean Depths", ThemeColors.OceanDepths, Color(0xFF4DD0E1), Color.Black, Color.White),
    ROYAL_PURPLE("Royal Purple", ThemeColors.RoyalPurple, Color(0xFFBA68C8), Color.Black, Color.White),
    MIDNIGHT_BLUE("Midnight Blue", ThemeColors.MidnightBlue, Color(0xFF7986CB), Color.Black, Color.White),
    EMBER("Ember", ThemeColors.Ember, Color(0xFFFFB74D), Color.Black, Color.White),
    SHADOW_GREEN("Shadow Green", ThemeColors.ShadowGreen, Color(0xFF4DB6AC), Color.Black, Color.White),
    DARK_WINE("Dark Wine", ThemeColors.DarkWine, Color(0xFFF06292), Color.Black, Color.White),
    SLATE("Slate", ThemeColors.Slate, Color(0xFF90A4AE), Color.Black, Color.White),
    OBSIDIAN("Obsidian", ThemeColors.Obsidian, Color(0xFFE0E0E0), Color.Black, Color.White)
}

private fun createColorScheme(theme: DndTheme) = darkColorScheme(
    primary = theme.primaryColor,
    onPrimary = theme.onPrimaryColor,
    primaryContainer = theme.primaryColor.copy(alpha = 0.22f),
    onPrimaryContainer = theme.textColor,
    secondary = Gold,
    onSecondary = Color.Black,
    secondaryContainer = Gold.copy(alpha = 0.22f),
    onSecondaryContainer = theme.textColor,
    background = theme.baseColor,
    onBackground = theme.textColor,
    surface = theme.baseColor.copy(alpha = 0.8f),
    onSurface = theme.textColor,
    surfaceVariant = lighten(theme.baseColor, 0.08f),
    onSurfaceVariant = theme.textColor.copy(alpha = 0.78f),
    // Dialogs, menus and cards must not fall back to Material's default purple-grey,
    // which clashes with every one of these themes.
    surfaceContainer = lighten(theme.baseColor, 0.06f),
    surfaceContainerHigh = lighten(theme.baseColor, 0.10f),
    surfaceContainerHighest = lighten(theme.baseColor, 0.14f),
    surfaceContainerLow = lighten(theme.baseColor, 0.03f),
    surfaceContainerLowest = theme.baseColor,
    surfaceTint = theme.baseColor,
    inverseSurface = lighten(theme.baseColor, 0.85f),
    inverseOnSurface = theme.baseColor,
    outline = theme.textColor.copy(alpha = 0.45f),
    outlineVariant = theme.textColor.copy(alpha = 0.22f),
    scrim = Color.Black
)

/**
 * Moves [color] toward white by [amount] (0..1).
 *
 * The themes are all very dark, so container and variant roles need a small lift to be
 * distinguishable from the background without washing the palette out.
 */
private fun lighten(color: Color, amount: Float): Color = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
    alpha = color.alpha
)

@Composable
fun DndNotesTheme(
    theme: DndTheme = DndTheme.DARK_PARCHMENT,
    content: @Composable () -> Unit
) {
    val colorScheme = createColorScheme(theme)

    // Every screen in this app draws on a transparent Scaffold/Surface so the user's
    // background image shows through. Material resolves the content color of a
    // transparent container to Color.Unspecified, which leaves LocalContentColor at its
    // default of Color.Black - black text on a near-black theme, i.e. unreadable.
    //
    // Seeding LocalContentColor here fixes that for every screen at once, and keeps the
    // text color tied to the active theme instead of hardcoding it per screen.
    CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
