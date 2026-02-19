package me.aliahad.audioplayer

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.aliahad.audioplayer.ui.theme.LightColorScheme
import me.aliahad.audioplayer.ui.theme.NightColorScheme
import me.aliahad.audioplayer.ui.theme.Typography as AppTypography

// Feature: theme-support, Task 6.4: Unit tests for theme integration
// Validates: Requirements 1.1, 1.2, 3.3, 5.1
class ThemeIntegrationUnitTest : FunSpec({

    test("Default theme is Night mode - PlayerUiState().isNightMode == true") {
        PlayerUiState().isNightMode shouldBe true
    }

    test("LightColorScheme background equals Color(0xFFFFF8F0)") {
        LightColorScheme.background shouldBe Color(0xFFFFF8F0)
    }

    test("NightColorScheme background equals Color(0xFF2C1E17)") {
        NightColorScheme.background shouldBe Color(0xFF2C1E17)
    }

    test("Default PlayerPreferencesData has isNightMode == true") {
        PlayerPreferencesData().isNightMode shouldBe true
    }

    test("Typography defines headlineMedium with non-default values") {
        AppTypography.headlineMedium shouldNotBe TextStyle.Default
    }

    test("Typography defines titleLarge with non-default values") {
        AppTypography.titleLarge shouldNotBe TextStyle.Default
    }

    test("Typography defines titleMedium with non-default values") {
        AppTypography.titleMedium shouldNotBe TextStyle.Default
    }

    test("Typography defines bodyLarge with non-default values") {
        AppTypography.bodyLarge shouldNotBe TextStyle.Default
    }

    test("Typography defines bodyMedium with non-default values") {
        AppTypography.bodyMedium shouldNotBe TextStyle.Default
    }

    test("Typography defines labelMedium with non-default values") {
        AppTypography.labelMedium shouldNotBe TextStyle.Default
    }
})
