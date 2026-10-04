package com.example.dndnotes.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Guards the readability of the theme palette.
 *
 * The regression that motivated this: every screen draws on a transparent
 * Scaffold/Surface, so Material resolves the content color to `Color.Unspecified` and
 * `LocalContentColor` falls back to its default of black. On these dark themes that made
 * text unreadable. `DndNotesTheme` now seeds `LocalContentColor`, and these tests assert
 * the thing that actually matters - that the text and background colors are far enough
 * apart in luminance to be legible.
 */
class ThemeContrastTest {

    /** WCAG relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** WCAG contrast ratio, from 1.0 (identical) to 21.0 (black on white). */
    private fun contrast(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val lighter = max(a, b)
        val darker = min(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun schemeFor(theme: DndTheme) = darkColorScheme(
        primary = theme.primaryColor,
        onPrimary = theme.onPrimaryColor,
        background = theme.baseColor,
        onBackground = theme.textColor,
        surface = theme.baseColor.copy(alpha = 0.8f),
        onSurface = theme.textColor
    )

    @Test
    fun `body text is readable on the background for every theme`() {
        DndTheme.entries.forEach { theme ->
            val scheme = schemeFor(theme)
            val ratio = contrast(scheme.onBackground, scheme.background)
            assertTrue(
                "${theme.displayName}: onBackground vs background contrast was %.2f".format(ratio),
                ratio >= MIN_BODY_CONTRAST
            )
        }
    }

    @Test
    fun `body text is readable on the surface for every theme`() {
        DndTheme.entries.forEach { theme ->
            val scheme = schemeFor(theme)
            // surface is the theme colour at 80% alpha over the background; approximate
            // the composite since the test has no renderer.
            val composited = composite(scheme.background, scheme.surface)
            val ratio = contrast(scheme.onSurface, composited)
            assertTrue(
                "${theme.displayName}: onSurface vs surface contrast was %.2f".format(ratio),
                ratio >= MIN_BODY_CONTRAST
            )
        }
    }

    @Test
    fun `every theme is dark, so the default black text would have been unreadable`() {
        // This is the bug in one assertion: if the fallback content color is black, the
        // contrast against these backgrounds is far below the legibility threshold.
        DndTheme.entries.forEach { theme ->
            val blackOnBackground = contrast(Color.Black, theme.baseColor)
            assertTrue(
                "${theme.displayName} is dark enough that black text fails (%.2f)".format(blackOnBackground),
                blackOnBackground < MIN_BODY_CONTRAST
            )
        }
    }

    @Test
    fun `every theme background is dark`() {
        DndTheme.entries.forEach { theme ->
            assertTrue(
                "${theme.displayName} luminance %.3f".format(luminance(theme.baseColor)),
                luminance(theme.baseColor) < 0.1
            )
        }
    }

    @Test
    fun `primary buttons keep readable label contrast`() {
        DndTheme.entries.forEach { theme ->
            val ratio = contrast(theme.onPrimaryColor, theme.primaryColor)
            assertTrue(
                "${theme.displayName}: onPrimary vs primary contrast was %.2f".format(ratio),
                ratio >= MIN_BODY_CONTRAST
            )
        }
    }

    /** Composites [top] over [bottom] using [top]'s alpha channel. */
    private fun composite(bottom: Color, top: Color): Color {
        val alpha = top.alpha
        return Color(
            red = bottom.red * (1 - alpha) + top.red * alpha,
            green = bottom.green * (1 - alpha) + top.green * alpha,
            blue = bottom.blue * (1 - alpha) + top.blue * alpha,
            alpha = 1f
        )
    }

    private companion object {
        /** WCAG AA for normal-size body text. */
        const val MIN_BODY_CONTRAST = 4.5
    }
}