package me.aliahad.audioplayer

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import me.aliahad.audioplayer.ui.theme.LightColorScheme
import me.aliahad.audioplayer.ui.theme.NightColorScheme
import kotlin.math.pow

// Feature: theme-support, Property 4: Color contrast meets accessibility minimum
// **Validates: Requirements 4.3**
class ColorContrastPropertyTest : FunSpec({

    fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            return if (channel <= 0.03928f) {
                channel / 12.92
            } else {
                ((channel + 0.055) / 1.055).pow(2.4)
            }
        }
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    data class ColorPair(val name: String, val foreground: Color, val background: Color)

    fun colorPairs(scheme: ColorScheme): List<ColorPair> = listOf(
        ColorPair("onBackground/background", scheme.onBackground, scheme.background),
        ColorPair("onSurface/surface", scheme.onSurface, scheme.surface),
        ColorPair("onPrimary/primary", scheme.onPrimary, scheme.primary),
        ColorPair("onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer)
    )

    test("Light scheme color pairs meet WCAG 4.5:1 contrast minimum") {
        colorPairs(LightColorScheme).forEach { pair ->
            val ratio = contrastRatio(pair.foreground, pair.background)
            withClue("${pair.name}: contrast ratio $ratio") {
                ratio shouldBeGreaterThanOrEqual 4.5
            }
        }
    }

    test("Night scheme color pairs meet WCAG 4.5:1 contrast minimum") {
        colorPairs(NightColorScheme).forEach { pair ->
            val ratio = contrastRatio(pair.foreground, pair.background)
            withClue("${pair.name}: contrast ratio $ratio") {
                ratio shouldBeGreaterThanOrEqual 4.5
            }
        }
    }
})
