package me.aliahad.audioplayer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import me.aliahad.audioplayer.ui.theme.LightColorScheme
import me.aliahad.audioplayer.ui.theme.NightColorScheme

// Feature: theme-support, Property 5: Surface container colors provide visual depth
// **Validates: Requirements 5.2**
class SurfaceContainerDepthPropertyTest : FunSpec({

    test("Light scheme surface container colors are pairwise distinct") {
        LightColorScheme.surfaceContainerLow shouldNotBe LightColorScheme.surfaceContainer
        LightColorScheme.surfaceContainerLow shouldNotBe LightColorScheme.surfaceContainerHigh
        LightColorScheme.surfaceContainer shouldNotBe LightColorScheme.surfaceContainerHigh
    }

    test("Night scheme surface container colors are pairwise distinct") {
        NightColorScheme.surfaceContainerLow shouldNotBe NightColorScheme.surfaceContainer
        NightColorScheme.surfaceContainerLow shouldNotBe NightColorScheme.surfaceContainerHigh
        NightColorScheme.surfaceContainer shouldNotBe NightColorScheme.surfaceContainerHigh
    }
})
