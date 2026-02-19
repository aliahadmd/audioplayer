# Requirements Document

## Introduction

The Audio Player app currently uses the default Material 3 dynamic color scheme, which delegates theming to the system wallpaper colors. This feature introduces a custom dual-theme system with two hand-crafted color palettes — a "Light" mode (creamy background with dark brown accents) and a "Night" mode (dark brown background with creamy white accents) — along with a user-facing toggle to switch between them. The selected theme preference is persisted across app restarts.

## Glossary

- **Theme_Switcher**: The UI control that allows the user to select between Light mode and Night mode.
- **Theme_Engine**: The component responsible for applying the selected color scheme to the entire Compose UI tree via Material 3 `MaterialTheme`.
- **Theme_Preference_Store**: The DataStore-backed persistence layer that saves and restores the user's selected theme mode.
- **Light_Mode**: The color scheme using a creamy/warm white background with dark brown foreground and accent colors.
- **Night_Mode**: The color scheme using a dark brown background with creamy white foreground and accent colors.
- **Color_Scheme**: A complete set of Material 3 color roles (primary, secondary, background, surface, onPrimary, onBackground, etc.) defining the visual appearance.

## Requirements

### Requirement 1: Custom Color Schemes

**User Story:** As a user, I want the app to have warm, aesthetically pleasing color palettes instead of system-derived dynamic colors, so that the app has a distinct and consistent visual identity.

#### Acceptance Criteria

1. THE Color_Scheme for Light_Mode SHALL define a creamy/warm white background, dark brown primary and text colors, and complementary surface and accent tones.
2. THE Color_Scheme for Night_Mode SHALL define a dark brown background, creamy white primary and text colors, and complementary surface and accent tones.
3. WHEN Light_Mode is active, THE Theme_Engine SHALL apply the Light_Mode Color_Scheme to all Material 3 color roles (background, surface, onBackground, onSurface, primary, onPrimary, secondary, tertiary).
4. WHEN Night_Mode is active, THE Theme_Engine SHALL apply the Night_Mode Color_Scheme to all Material 3 color roles (background, surface, onBackground, onSurface, primary, onPrimary, secondary, tertiary).
5. THE Theme_Engine SHALL replace the current dynamic color behavior with the custom Color_Schemes.

### Requirement 2: Theme Selection Toggle

**User Story:** As a user, I want to switch between Light mode and Night mode from within the app, so that I can choose the appearance that suits my preference or environment.

#### Acceptance Criteria

1. THE Theme_Switcher SHALL be accessible from the main player screen without navigating to a separate settings screen.
2. WHEN the user activates the Theme_Switcher, THE Theme_Engine SHALL toggle between Light_Mode and Night_Mode.
3. WHEN the theme changes, THE Theme_Engine SHALL apply the new Color_Scheme immediately without requiring an app restart.
4. THE Theme_Switcher SHALL visually indicate which mode is currently active (e.g., a sun/moon icon reflecting the current state).

### Requirement 3: Theme Preference Persistence

**User Story:** As a user, I want my selected theme to be remembered when I close and reopen the app, so that I do not have to re-select my preferred mode each time.

#### Acceptance Criteria

1. WHEN the user selects a theme via the Theme_Switcher, THE Theme_Preference_Store SHALL persist the selected mode to DataStore.
2. WHEN the app launches, THE Theme_Preference_Store SHALL provide the previously saved theme mode to the Theme_Engine.
3. IF no theme preference has been saved, THEN THE Theme_Engine SHALL default to Night_Mode.
4. WHEN the persisted preference is restored, THE Theme_Engine SHALL apply the corresponding Color_Scheme before the first frame is rendered to the user.

### Requirement 4: Consistent Theming Across UI Components

**User Story:** As a user, I want all parts of the app to reflect the selected theme consistently, so that the visual experience feels cohesive.

#### Acceptance Criteria

1. WHEN a theme is active, THE Theme_Engine SHALL apply the active Color_Scheme to the top app bar, bottom sheet, playlist items, playback controls, and all dialog surfaces.
2. WHEN a theme is active, THE Theme_Engine SHALL set the system status bar and navigation bar colors to match the active Color_Scheme background.
3. WHEN a theme is active, THE Theme_Engine SHALL ensure all text and icon colors use the corresponding "on" color roles (onBackground, onSurface, onPrimary) for sufficient contrast.

### Requirement 5: Style Enhancement

**User Story:** As a user, I want the overall visual style of the app to feel polished and refined, so that the experience is pleasant to use.

#### Acceptance Criteria

1. THE Theme_Engine SHALL define a complete Material 3 Typography scale (headlineMedium, titleLarge, titleMedium, bodyLarge, bodyMedium, labelMedium) with consistent font weights and sizes.
2. THE Color_Scheme for both modes SHALL define distinct surface container colors for elevated components (cards, bottom sheets) to create visual depth.
