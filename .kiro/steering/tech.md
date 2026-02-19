# Tech Stack & Build

## Language & UI
- Kotlin (JVM target 11)
- Jetpack Compose with Material 3 (dynamic color on Android 12+)
- Kotlin code style: `official`

## Key Libraries
| Library | Purpose |
|---|---|
| Media3 ExoPlayer 1.4.1 | Audio playback engine |
| Media3 MediaSession 1.4.1 | Lockscreen & notification media controls |
| Media3 UI | PlayerNotificationManager |
| AndroidX DataStore Preferences | Persisting playback state |
| AndroidX DocumentFile | SAF-based folder/file access |
| AndroidX Lifecycle (runtime-ktx, runtime-compose) | Lifecycle-aware coroutines & state collection |
| AndroidX Activity Compose | Compose integration with Activity |
| Compose Material Icons Extended | Icon set |

## Build System
- Gradle with Kotlin DSL (`.gradle.kts`)
- Version catalog at `gradle/libs.versions.toml`
- AGP 8.13.0, Kotlin 2.0.21
- Single `:app` module

## SDK Targets
- `minSdk` 31 (Android 12)
- `targetSdk` / `compileSdk` 36

## Common Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

## Signing
Release and debug builds both use the keystore at `keystore/release.jks`. Signing config is defined in `app/build.gradle.kts`.
