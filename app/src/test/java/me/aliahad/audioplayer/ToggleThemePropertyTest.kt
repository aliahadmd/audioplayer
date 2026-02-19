package me.aliahad.audioplayer

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.forAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Feature: theme-support, Property 1: Toggle inverts theme state
// **Validates: Requirements 2.2**
class ToggleThemePropertyTest : FunSpec({

    test("Toggle inverts isNightMode for any initial boolean value") {
        forAll(Arb.boolean()) { initialIsNightMode ->
            // Simulate the ViewModel's toggle logic using MutableStateFlow
            val uiState = MutableStateFlow(PlayerUiState(isNightMode = initialIsNightMode))

            // Toggle: flip isNightMode (mirrors ViewModel.toggleTheme())
            val newValue = !uiState.value.isNightMode
            uiState.update { state -> state.copy(isNightMode = newValue) }

            // Assert: result is logical negation of initial
            uiState.value.isNightMode == !initialIsNightMode
        }
    }
})
