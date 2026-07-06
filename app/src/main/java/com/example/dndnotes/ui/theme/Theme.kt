package com.example.dndnotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    secondary = Gold,
    onSecondary = Color.Black,
    background = theme.baseColor,
    surface = theme.baseColor.copy(alpha = 0.8f),
    onBackground = theme.textColor,
    onSurface = theme.textColor
)

@Composable
fun DndNotesTheme(
    theme: DndTheme = DndTheme.DARK_PARCHMENT,
    content: @Composable () -> Unit
) {
    val colorScheme = createColorScheme(theme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
