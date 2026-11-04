# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A fully native Android Sudoku game written in Kotlin with Jetpack Compose (Material 3). There is **no** cross-platform layer — this was migrated from a Capacitor/web/iOS hybrid to native Android only. Package: `com.alfredang.sudoku`. minSdk 24, target/compile SDK 36.

## Build & Run

Gradle requires a JDK; the Android Studio bundled JBR (21) works. Prefix commands with:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Common commands (run from repo root):

```bash
./gradlew :app:assembleDebug        # build debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease      # build (unsigned) release APK
./gradlew :app:installDebug         # build + install on the connected device/emulator
./gradlew clean
./gradlew :app:lint                 # Android lint
./gradlew :app:testDebugUnitTest    # JVM unit tests (none yet)
./gradlew :app:testDebugUnitTest --tests "com.alfredang.sudoku.SudokuEngineTest"  # single test class
```

Deploy/test on a device or the emulator (adb lives at `~/Library/Android/sdk/platform-tools/adb`):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.alfredang.sudoku/.MainActivity
```

`local.properties` (git-ignored) must point `sdk.dir` at the Android SDK.

## Architecture

Unidirectional state flow. The UI is a pure function of one immutable `UiState`; all mutation goes through `GameViewModel`.

- **`SudokuEngine.kt`** — pure, framework-free game logic: randomized-backtracking board generation, cell removal per `Difficulty`, placement/violation checks, win detection. No Android imports, so it is the unit-test target. `Difficulty` (EASY/MEDIUM/HARD) encodes both countdown duration and cells-to-remove. Cells are addressed by a packed `row*9+col` index (`SudokuEngine.keyOf`).

- **`GameViewModel.kt`** — single source of truth. Holds mutable `solution`/`board`/`given` internally but exposes only an immutable `UiState` (Compose `mutableStateOf`). After every change it rebuilds the immutable `cells: List<CellUi>` (value + `CellStyle` + error flag) via `buildCells`. The countdown is a `viewModelScope` coroutine; `revealSolution` produces the color-coded end state for Give Up / time-up. Haptics are emitted as a `SharedFlow<Haptic>` event stream — the ViewModel never touches Android haptic APIs directly.

- **`ui/SudokuApp.kt`** — all Compose UI (Menu vs Game screens, board, number pad, action buttons). Stateless: it renders `vm.ui` and calls `vm` methods. It collects `vm.haptics` in a `LaunchedEffect` and bridges each event to `view.performHapticFeedback(...)`. The board draws cell backgrounds/text per `CellStyle`, with the 3×3 grid lines painted by a `Canvas` overlay (thick teal every 3rd line, thin elsewhere).

- **`ui/theme/Theme.kt`** — Material 3 `SudokuTheme` plus the exact brand/board color constants ported from the original web game. The app is intentionally light-only (kid-friendly) regardless of system dark mode.

- **`MainActivity.kt`** — single `ComponentActivity`; `setContent { SudokuTheme { SudokuApp(viewModel) } }`.

### Key invariants when modifying game logic
- `UiState` and its members are immutable — never mutate `cells` in place; copy via `ui.copy(...)`.
- Cell rendering precedence in `BoardCell`: error > selected > given/reveal style > normal.
- Keep `SudokuEngine` free of Android/Compose imports so it stays unit-testable.

## Assets

Custom adaptive launcher icons live in `app/src/main/res/mipmap-*` (foreground PNGs + `mipmap-anydpi-v26` adaptive XML referencing `@color/ic_launcher_background`). `app-icon-1024.png`, `screenshots/`, and `store-assets/` are store-submission material, not used at runtime.
