---
name: wear-os-togavganger
description: Follows project conventions for Togavganger Wear OS app. Uses Context7 for up-to-date Wear OS and Kotlin documentation. Use when modifying Wear OS code, adding Compose components, Tiles, or when troubleshooting build/compilation errors.
---

# Wear OS Togavganger

## Context7 Workflow (Required Before Code Changes)

Before writing or modifying Wear OS/Kotlin/Compose code:

1. Call **resolve-library-id** MCP tool with `libraryName` and `query` to get the Context7 library ID.
2. Call **query-docs** MCP tool with the resolved `libraryId` and a specific `query` to fetch current documentation and examples.

Relevant libraries to resolve:

| Topic | libraryName |
|-------|-------------|
| Wear Compose Material3 | `androidx wear compose material3` |
| Wear Compose / Wear OS | `androidx wear compose` |
| Wear Tiles / Protolayout | `androidx wear tiles` or `androidx wear protolayout` |
| Horologist | `horologist` |
| Kotlin Coroutines | `kotlin coroutines` |

## Project Versions (gradle/libs.versions.toml)

Do not introduce dependencies that conflict with these versions:

| Component | Version |
|-----------|---------|
| Kotlin | 2.0.21 |
| AGP | 8.13.2 |
| compileSdk / targetSdk | 36 |
| minSdk | 34 |
| Wear Compose Material | 1.2.1 |
| Wear Compose Material3 | 1.5.6 |
| Wear Compose Foundation | 1.2.1 |
| Compose BOM | 2024.09.00 |
| Wear Tiles | 1.4.0 |
| Protolayout | 1.3.0 |
| Horologist | 0.6.17 |
| Lifecycle ViewModel/Runtime Compose | 2.8.6 |

## Architecture Conventions

- **ViewModel**: `AndroidViewModel` with `MutableStateFlow<TrainUiState>` and `sealed class TrainEvent`.
- **UI state**: Collect via `collectAsState()`; dispatch events to ViewModel.
- **Repository**: `withContext(Dispatchers.IO)`; use raw `HttpURLConnection` (no Retrofit/OkHttp).
- **Preferences**: `StationPreferences` via `SharedPreferences`.
- **Wear Tile**: Horologist `SuspendingTileService` with protolayout-material3.
- **Complication**: `MainComplicationService` in `complication/` package.

## Pitfalls and Rules

### Material 2 vs Material 3

Existing code mixes M2 and M3. Do not add new M2 usage.

- **M2** (wear.compose.material): `Button`, `Dialog`, `MaterialTheme` – keep only where already used.
- **M3** (wear.compose.material3): `ScreenScaffold`, `EdgeButton`, `Text`, `TimeText`, etc. – use for all new components.

### Activity in Composables

Do not pass `ComponentActivity` (or any `Activity`) down through composable parameters. Use `LocalContext.current` or lambda callbacks.

### Error Handling

Do not use magic strings like `"Feil"`. Prefer `Result<T>` or sealed classes for error states.

### Colors and Theme

`Theme.kt` uses hardcoded hex values to align with protolayout Tile colors. If you change theme colors, update both Compose UI and Tile layout.

## Compilation Checklist

After code changes:

- [ ] No new imports from `wear.compose.material` for new components (use M3).
- [ ] No `Activity` or `ComponentActivity` parameters in new composables.
- [ ] Run `ReadLints` on modified files.
- [ ] Run `./gradlew assembleDebug` or equivalent to verify build.

## Key Project Paths

- [MainActivity.kt](app/src/main/java/no/togavganger/presentation/MainActivity.kt)
- [TrainViewModel](app/src/main/java/no/togavganger/presentation/viewmodel/TrainViewModel.kt)
- [TrainRepository](app/src/main/java/no/togavganger/data/repository/TrainRepository.kt)
- [MainTileService](app/src/main/java/no/togavganger/tile/MainTileService.kt)
- [Theme.kt](app/src/main/java/no/togavganger/presentation/theme/Theme.kt)
- [libs.versions.toml](gradle/libs.versions.toml)
