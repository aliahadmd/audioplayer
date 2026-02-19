# Implementation Plan: Theme Support

## Overview

Replace the dynamic-color Material 3 theming with two custom color palettes (Light and Night mode), add a toggle to the main player screen, and persist the user's preference via DataStore. Implementation proceeds bottom-up: theme foundation (colors, typography), then persistence, then ViewModel state, then UI wiring.

## Tasks

- [x] 1. Define custom color palettes and typography
  - [x] 1.1 Replace color constants in `Color.kt`
    - Remove existing `Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40` color definitions
    - Define Light mode color constants using hex values from the design (e.g., `LightBackground = Color(0xFFFFF8F0)`, `LightOnBackground = Color(0xFF3E2723)`, etc.)
    - Define Night mode color constants using hex values from the design (e.g., `NightBackground = Color(0xFF2C1E17)`, `NightOnBackground = Color(0xFFF5E6D3)`, etc.)
    - Cover all Material 3 roles: background, surface, surfaceVariant, surfaceContainerLow/Container/ContainerHigh, primary, primaryContainer, secondary, secondaryContainer, tertiary, error, outline, outlineVariant, and all corresponding "on" colors
    - _Requirements: 1.1, 1.2, 5.2_

  - [x] 1.2 Build `LightColorScheme` and `NightColorScheme` in `Theme.kt`
    - Replace existing `DarkColorScheme` and `LightColorScheme` with `NightColorScheme` and `LightColorScheme` using `lightColorScheme()` and `darkColorScheme()` builders populated from the new color constants
    - Assign all Material 3 color roles listed in the design (primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, error, onError, outline, outlineVariant)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.2_

  - [x] 1.3 Update `AudioplayerTheme` composable in `Theme.kt`
    - Change signature to `AudioplayerTheme(isNightMode: Boolean, content: @Composable () -> Unit)`
    - Remove `darkTheme` and `dynamicColor` parameters and all dynamic-color / `Build.VERSION.SDK_INT` branching
    - Select `NightColorScheme` when `isNightMode` is true, `LightColorScheme` when false
    - Pass the selected scheme and existing `Typography` to `MaterialTheme`
    - _Requirements: 1.3, 1.4, 1.5_

  - [x] 1.4 Expand Typography scale in `Type.kt`
    - Define `headlineMedium`, `titleLarge`, `titleMedium`, `bodyLarge`, `bodyMedium`, and `labelMedium` with explicit `fontSize` and `fontWeight` values
    - _Requirements: 5.1_

  - [x] 1.5 Write property test: Surface container colors provide visual depth (Property 5)
    - **Property 5: Surface container colors provide visual depth**
    - For each color scheme (Light and Night), assert that `surfaceContainerLow`, `surfaceContainer`, and `surfaceContainerHigh` are pairwise distinct
    - **Validates: Requirements 5.2**

  - [x] 1.6 Write property test: Color contrast meets accessibility minimum (Property 4)
    - **Property 4: Color contrast meets accessibility minimum**
    - For each color scheme, iterate foreground/background role pairs (`onBackground`/`background`, `onSurface`/`surface`, `onPrimary`/`primary`, `onPrimaryContainer`/`primaryContainer`) and assert WCAG contrast ratio ≥ 4.5:1
    - Implement a helper function to compute relative luminance and contrast ratio
    - **Validates: Requirements 4.3**

- [x] 2. Checkpoint - Verify theme foundation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Add theme preference persistence
  - [x] 3.1 Add theme mode key and save function to `PlayerPreferences.kt`
    - Add `IS_NIGHT_MODE_KEY = booleanPreferencesKey("is_night_mode")`
    - Add `isNightMode: Boolean = true` field to `PlayerPreferencesData`
    - Read `isNightMode` from preferences in the existing `preferencesFlow` mapping, defaulting to `true` when absent
    - Add `suspend fun saveThemeMode(isNightMode: Boolean)` that writes only the theme key to DataStore
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 3.2 Write property test: Theme preference persistence round-trip (Property 3)
    - **Property 3: Theme preference persistence round-trip**
    - Generate random booleans via `Arb.boolean()`, save via `saveThemeMode()`, read back via `preferencesFlow`, assert equality
    - Use a test DataStore instance (in-memory or temp file)
    - **Validates: Requirements 3.1, 3.2**

- [x] 4. Expose theme state in ViewModel
  - [x] 4.1 Add `isNightMode` to `PlayerUiState` and `toggleTheme()` to `AudioPlayerViewModel`
    - Add `val isNightMode: Boolean = true` to the `PlayerUiState` data class
    - Read theme preference on ViewModel init and set initial `isNightMode` in `_uiState`
    - Implement `toggleTheme()`: flip `isNightMode` in `_uiState`, launch coroutine to call `preferences.saveThemeMode(newValue)`
    - Handle DataStore errors gracefully — catch exceptions, log warning, keep in-memory state change
    - _Requirements: 2.2, 3.1, 3.2, 3.3, 3.4_

  - [x] 4.2 Write property test: Toggle inverts theme state (Property 1)
    - **Property 1: Toggle inverts theme state**
    - Generate random booleans via `Arb.boolean()` for initial `isNightMode`, set state, call `toggleTheme()`, assert result is logical negation
    - **Validates: Requirements 2.2**

- [x] 5. Checkpoint - Verify persistence and ViewModel logic
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Wire theme into UI
  - [x] 6.1 Add theme toggle button to `MainActivity.kt`
    - Add `onToggleTheme: () -> Unit` parameter to `AudioPlayerScreen` composable
    - Add an `IconButton` in the top app bar `actions` slot (before the existing folder icon)
    - Show `Icons.Filled.LightMode` (sun) when `isNightMode` is true, `Icons.Filled.DarkMode` (moon) when false
    - Wire `onToggleTheme` to `viewModel.toggleTheme()` at the call site
    - _Requirements: 2.1, 2.3, 2.4_

  - [x] 6.2 Pass `isNightMode` to `AudioplayerTheme` and set system bar colors
    - In `MainActivity.onCreate` or the top-level composable, pass `uiState.isNightMode` to `AudioplayerTheme`
    - Call `enableEdgeToEdge()` with appropriate `SystemBarStyle` based on `isNightMode` so status bar and navigation bar match the active theme background
    - Use a `LaunchedEffect` or `DisposableEffect` keyed on `isNightMode` to update system bars when theme changes
    - _Requirements: 1.3, 1.4, 2.3, 4.1, 4.2_

  - [x] 6.3 Write property test: Theme icon reflects current mode (Property 2)
    - **Property 2: Theme icon reflects current mode**
    - Generate random booleans via `Arb.boolean()`, assert icon selection returns sun (`LightMode`) for `true` and moon (`DarkMode`) for `false`
    - **Validates: Requirements 2.4**

  - [x] 6.4 Write unit tests for theme integration
    - Test default theme is Night mode (`PlayerUiState().isNightMode == true`)
    - Test `LightColorScheme.background` equals `Color(0xFFFFF8F0)`
    - Test `NightColorScheme.background` equals `Color(0xFF2C1E17)`
    - Test `PlayerPreferencesData().isNightMode == true`
    - Test Typography defines all required styles with non-default values
    - _Requirements: 1.1, 1.2, 3.3, 5.1_

- [x] 7. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- All code is Kotlin targeting the existing single-module Android app structure
