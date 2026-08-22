# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**LycoSheet** is a Kotlin Multiplatform Mobile (KMM) flashcard app targeting Android and iOS. Users create double-sided revision cards (recto/verso) for studying — vocabulary, definitions, etc. — then flip through them in a test session.

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.1.0 |
| UI (Android) | Compose + Material 3 | via Compose MP 1.7.0 |
| Async | Kotlinx Coroutines + Flow | 1.9.0 |
| Database | SQLDelight 2 | 2.0.2 |
| DI | Koin Multiplatform | 4.0.0 |
| ViewModel | androidx.lifecycle (KMP) | 2.8.7 |
| Build | Gradle Kotlin DSL + version catalog | AGP 8.6.0 |

## App Architecture

**Three-screen bottom-navigation app** following MVVM + Clean Architecture:

| Screen | Purpose |
|---|---|
| **Home (Create)** | Create and edit flashcards; each card has a `front` (recto) and `back` (verso) face |
| **Library / Revision** | Browse decks; start a test session — cards are shown one at a time, a tap flips the card (recto ↔ verso), swipe or button to advance |
| **Settings** | Appearance options (theme, font size) and app-level preferences |

### Module layout

```
shared/
  src/
    commonMain/kotlin/com/lycoris/lycosheet/
      data/model/          ← Card, Deck, StudySession
      data/repository/     ← interfaces + impl/ (SQLDelight-backed)
      di/                  ← DatabaseDriverFactory (expect) + SharedModule (Koin)
      domain/usecase/      ← card/ and deck/ use cases
      presentation/        ← home/ library/ study/ settings/ (ViewModel + State)
      util/                ← Platform.kt (expect fun currentTimeMillis())
    commonMain/sqldelight/com/lycoris/lycosheet/db/
      Card.sq, Deck.sq     ← SQLDelight schema + named queries
    androidMain/           ← DatabaseDriverFactory.android.kt, Platform.android.kt
    iosMain/               ← DatabaseDriverFactory.ios.kt, Platform.ios.kt

androidApp/src/main/kotlin/com/lycoris/lycosheet/android/
  LycoSheetApp.kt          ← Application class — Koin bootstrap
  MainActivity.kt          ← edge-to-edge host
  di/AndroidModule.kt      ← provides DatabaseDriverFactory(context)
  ui/theme/                ← Color.kt, Type.kt, Theme.kt (Material 3, light+dark)
  ui/navigation/           ← Screen.kt (sealed routes), NavGraph.kt
  ui/components/           ← FlashCard.kt (Y-axis flip animation)
  ui/home/                 ← HomeScreen.kt
  ui/library/              ← LibraryScreen.kt
  ui/study/                ← StudyScreen.kt
  ui/settings/             ← SettingsScreen.kt

iosApp/                    ← Xcode project (create separately via Android Studio KMM wizard)
```

### Core domain models

- **Card** — `id`, `front: String`, `back: String`, `deckId`, `createdAt`
- **Deck** — `id`, `name`, `description`, `cardCount`, `createdAt`
- **StudySession** — tracks progress through a deck test (current index, flipped state)

### Card flip mechanic

The flip is a pure UI animation driven by a boolean state (`isFrontVisible`). The ViewModel exposes `flipCard()` — no persistence needed for flip state, it resets per session.

## Build Commands

Run all Gradle tasks from the repo root.

```bash
# Build all targets
./gradlew build

# Install on connected Android device/emulator
./gradlew :androidApp:installDebug

# Shared module unit tests
./gradlew :shared:testDebugUnitTest

# Single test class
./gradlew :shared:testDebugUnitTest --tests "com.lycoris.lycosheet.data.repository.impl.DeckRepositoryImplTest"

# Lint
./gradlew :androidApp:lintDebug
./gradlew :shared:lint

# Regenerate SQLDelight code after .sq changes
./gradlew :shared:generateCommonMainLycoSheetDatabaseInterface

# iOS — create the Xcode project once via Android Studio KMM wizard, then:
open iosApp/iosApp.xcworkspace
```

## Key Conventions

- **State** — `MutableStateFlow` inside `ViewModel` (androidx.lifecycle KMP); screens collect via `collectAsState()`. Events (one-shot toasts) use a boolean flag in the state that the screen resets after consuming.
- **DI** — Koin. `sharedModule` (in `shared/di/SharedModule.kt`) wires repositories, use cases, and ViewModels. `androidModule` (in `androidApp/di/AndroidModule.kt`) provides the Android `DatabaseDriverFactory`. Koin is started in `LycoSheetApp.onCreate()`.
- **Database** — SQLDelight 2. Schema files live in `shared/src/commonMain/sqldelight/`. Generated code goes to `com.lycoris.lycosheet.db`. The `DatabaseDriverFactory` expect/actual pair handles the platform-specific SQLite driver.
- **expect/actual** — Only two pairs exist: `DatabaseDriverFactory` and `currentTimeMillis()`. Keep the pattern minimal.
- **Navigation** — `Screen` sealed object in `androidApp/ui/navigation/Screen.kt`. The Study route carries a `deckId: Long` argument: `"study/{deckId}"`. Bottom bar is hidden on the Study screen.
- **Card flip** — `FlashCard.kt` uses `animateFloatAsState` on `rotationY`. The back face applies its own `rotationY = 180f` so text reads correctly. `StudyViewModel.flipCard()` toggles `isFrontVisible`; no DB write occurs.
- **iOS** — The `iosApp/` Xcode project is not included (Linux environment). Create it via **Android Studio → New Project → KMM Application** and point it at this repo, or use the KMM Wizard. The `shared` framework exposes `SharedKt` and all ViewModels.
- **Adding a new screen** — add a route to `Screen.kt`, a `composable {}` in `NavGraph.kt`, a `ViewModel` + `State` in `shared/presentation/`, and register the ViewModel in `SharedModule.kt`.
