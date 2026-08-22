# LycoSheet

A Kotlin Multiplatform Mobile flashcard app for Android and iOS. Create double-sided revision cards (recto/verso) and study them one by one with a tap-to-flip mechanic — ideal for vocabulary, definitions, and any recall-based learning.

## Features

- **Create cards** — write a front (question/term) and back (answer/definition), assign to a deck or create one on the fly
- **Library** — browse all decks with live card counts; start a study session from any deck
- **Study session** — cards displayed one at a time, tap to flip recto↔verso, swipe or button to advance
- **Settings** — appearance and app-level preferences

## Download

Latest release: [v0.1.1](https://github.com/Lycoris69/LycoSheet/releases/tag/v0.1.1)

| Platform | Asset |
|---|---|
| Android | `LycoSheet-v0.1.1.apk` |
| iOS (XCFramework) | `LycoSheet-v0.1.1.xcframework.zip` |

Install the APK directly on any Android device (API 24+). The XCFramework is for integrating the shared Kotlin logic into an Xcode project.

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.1.0 |
| UI (Android) | Compose + Material 3 | Compose MP 1.7.0 |
| Async | Kotlinx Coroutines + Flow | 1.9.0 |
| Database | SQLDelight 2 | 2.0.2 |
| DI | Koin Multiplatform | 4.0.0 |
| ViewModel | androidx.lifecycle (KMP) | 2.8.7 |
| Build | Gradle Kotlin DSL + version catalog | AGP 8.6.0 |

## Project Structure

```
shared/               ← KMP module (Android + iOS)
  commonMain/
    data/model/       ← Card, Deck, StudySession
    data/repository/  ← interfaces + SQLDelight-backed impls
    di/               ← Koin SharedModule, DatabaseDriverFactory (expect)
    domain/usecase/   ← deck/ and card/ use cases
    presentation/     ← home/ library/ study/ settings/ (ViewModel + State)
    sqldelight/       ← Card.sq, Deck.sq (schema + named queries)
  androidMain/        ← DatabaseDriverFactory.android.kt, Platform.android.kt
  iosMain/            ← DatabaseDriverFactory.ios.kt, Platform.ios.kt

androidApp/           ← Android application module
  ui/home/            ← HomeScreen.kt
  ui/library/         ← LibraryScreen.kt
  ui/study/           ← StudyScreen.kt
  ui/settings/        ← SettingsScreen.kt
  ui/components/      ← FlashCard.kt (Y-axis flip animation)
  ui/navigation/      ← Screen.kt (sealed routes), NavGraph.kt
  ui/theme/           ← Material 3 light/dark theme
  di/                 ← AndroidModule.kt (provides DatabaseDriverFactory)
```

## Build

```bash
# Android — build and install on a connected device/emulator
./gradlew :androidApp:installDebug

# Shared module unit tests
./gradlew :shared:testDebugUnitTest

# Regenerate SQLDelight code after editing .sq files
./gradlew :shared:generateCommonMainLycoSheetDatabaseInterface

# iOS XCFramework (requires macOS)
./gradlew :shared:assembleSharedReleaseXCFramework
# Output: shared/build/XCFrameworks/release/Shared.xcframework
```

The iOS XCFramework is also built automatically via GitHub Actions on every `v*` tag push and attached to the GitHub release.

## iOS Setup

The `iosApp/` Xcode project is not included in the repo (requires macOS to generate). Create it once via **Android Studio → New Project → KMM Application**, point it at this repo, then open `iosApp/iosApp.xcworkspace`. The shared framework exposes all ViewModels and domain logic to Swift.

## Architecture

MVVM + Clean Architecture:

- **ViewModel** — `androidx.lifecycle` KMP; state exposed as `StateFlow`, collected in Compose with `collectAsState()`
- **Repository** — SQLDelight 2, reactive `Flow<List<T>>` via `asFlow()` + `mapToList()`
- **DI** — Koin; ViewModels registered as `factory {}` in `sharedModule` (iOS-compatible), Android `koinViewModel()` manages lifecycle scope
- **Card flip** — pure UI animation (`animateFloatAsState` on `rotationY`), no DB write; resets each session
