# LycoSheet

A Kotlin Multiplatform Mobile flashcard app for Android and iOS. Create revision cards and study them with multiple question formats — tap-to-flip, multiple choice, fill-in-the-blank, or audio listening — ideal for vocabulary, definitions, and any recall-based learning.

## Features

- **Four card types** — choose when creating or editing; switch type non-destructively in the editor
  - 🃏 **Classic** — tap to flip front ↔ back
  - 🔘 **Multiple Choice** — pick the correct answer from up to 4 shuffled choices
  - ✏️ **Fill-in** — type the missing word and check your answer
  - 🎧 **Listening** — record audio; study by hearing the clip, then reveal the transcript and grade
- **Pronunciation clips** — attach an optional 🔊 pronunciation audio recording to any card type; plays during study and in the deck detail view
- **Create cards** — front (question/term) + back (answer/definition); assign to a deck or create one on the fly
- **Deck detail view** — tap any deck to see all its cards in a 2-column grid with type badges and seen counters; edit or delete individual cards inline
- **Library** — browse all decks with live card counts; start a study session from any deck
- **Study session** — per-type question UI; after answering, grade yourself:
  - 🔴 **Again** — didn't know it; card is re-queued at the end of the session
  - ⬜ **Ok** — knew it somewhat
  - 🟢 **Good** — knew it easily
- **Seen counter** — each card tracks how many times it has been studied across all sessions
- **Session summary** — completion screen shows your Again / Ok / Good breakdown
- **Offline-first** — audio files stored locally in `filesDir/audio/`; no network required
- **Settings** — appearance and app-level preferences

## Download

Latest release: [v0.1.6](https://github.com/Lycoris69/LycoSheet/releases/tag/v0.1.6)

| Platform | Asset |
|---|---|
| Android | `LycoSheet-v0.1.6.apk` |
| iOS (XCFramework) | `LycoSheet-v0.1.6.xcframework.zip` |

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
    data/model/       ← Card, CardType, Deck, StudySession, CardGrade
    data/repository/  ← interfaces + SQLDelight-backed impls
    di/               ← Koin SharedModule, DatabaseDriverFactory (expect)
    domain/usecase/   ← deck/ and card/ use cases
    presentation/     ← home/ library/ deck/ study/ settings/ (ViewModel + State)
    sqldelight/       ← Card.sq, Deck.sq + 1.sqm, 2.sqm, 3.sqm migrations
  androidMain/        ← DatabaseDriverFactory.android.kt, Platform.android.kt, AudioPlayer.android.kt
  iosMain/            ← DatabaseDriverFactory.ios.kt, Platform.ios.kt, AudioPlayer.ios.kt

androidApp/           ← Android application module
  audio/              ← AudioRecorderHelper.kt (MediaRecorder, saves to filesDir/audio/)
  ui/home/            ← HomeScreen.kt (type selector + dynamic form)
  ui/library/         ← LibraryScreen.kt, DeckDetailScreen.kt (card grid + editor)
  ui/study/           ← StudyScreen.kt (Classic / Multiple Choice / Fill-in / Listening)
  ui/settings/        ← SettingsScreen.kt
  ui/components/      ← FlashCard.kt (Y-axis flip), PronunciationRecorder.kt
  ui/navigation/      ← Screen.kt (sealed routes), NavGraph.kt
  ui/theme/           ← Material 3 light/dark theme
  di/                 ← AndroidModule.kt (provides DatabaseDriverFactory, AudioPlayer, AudioRecorderHelper)
```

## Database schema

Current version: **4**

| Migration | Change |
|---|---|
| 1 → 2 | `CardEntity.seen_count INTEGER DEFAULT 0` added |
| 2 → 3 | `CardEntity.card_type TEXT DEFAULT 'CLASSIC'` and `extra_data TEXT DEFAULT ''` added |
| 3 → 4 | `CardEntity.pronunciation_path TEXT DEFAULT ''` added |

`extra_data` stores pipe-delimited wrong choices for MC cards, and the audio file path for Listening cards. `pronunciation_path` is a separate optional pronunciation clip for any card type.

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

The iOS XCFramework is built automatically via GitHub Actions on every `v*` tag push and attached to the GitHub release via `gh release upload` (never creates a new release — the APK release must exist first).

## iOS Setup

The `iosApp/` Xcode project is not included in the repo (requires macOS to generate). Create it once via **Android Studio → New Project → KMM Application**, point it at this repo, then open `iosApp/iosApp.xcworkspace`. The shared framework exposes all ViewModels and domain logic to Swift.

## Architecture

MVVM + Clean Architecture:

- **ViewModel** — `androidx.lifecycle` KMP; state exposed as `StateFlow`, collected in Compose with `collectAsState()`
- **Repository** — SQLDelight 2, reactive `Flow<List<T>>` via `asFlow()` + `mapToList()`
- **DI** — Koin; ViewModels registered as `factory {}` in `sharedModule` (iOS-compatible), Android `koinViewModel()` manages lifecycle scope
- **Card types** — `CardType` enum (`CLASSIC | MULTIPLE_CHOICE | FILL_IN | LISTENING`); stored as enum name string in DB; `extraData` holds pipe-delimited wrong choices for MC or the audio file path for Listening
- **Pronunciation** — optional `pronunciationPath` column on every card; `PronunciationRecorder` widget (collapsible, record/preview/discard); `PronunciationButton` (🔊) shown in study and deck detail
- **Audio** — `expect class AudioPlayer` (Android: `MediaPlayer`; iOS: stub); `AudioRecorderHelper` (Android `MediaRecorder`, saves `<uuid>.m4a` to `filesDir/audio/`)
- **Edit non-destructive** — the edit dialog keeps independent local state per type; switching the type selector never clears data for another type
- **Study per-type UI** — keyed by `currentIndex` so local state (selected choice, user input, revealed) resets each card; grade buttons appear after answering for all types
- **Card flip** — pure UI animation (`animateFloatAsState` on `rotationY`), Classic only, no DB write
- **Grading** — `StudyViewModel.gradeCard(CardGrade)` records the grade in-memory and advances; `AGAIN` appends the card to the end of the session queue
- **Seen counter** — `IncrementCardSeenUseCase` increments `seen_count` in the DB each time a card is displayed; shown as a badge in study and deck detail
