# Sudoku Mobile

A Sudoku puzzle game for iOS and Android, built with Capacitor and vanilla JavaScript.

Ported from the [web version](https://github.com/alfredang/sudoku) into a native mobile app with haptic feedback, safe area support, and platform-specific integrations.

## Screenshots

<p align="center">
  <img src="screenshots/menu.png" alt="Menu Screen" width="300" />
  <img src="screenshots/game.png" alt="Game Screen" width="300" />
</p>

## Features

- **Three difficulty levels** - Easy (15 min), Medium (10 min), Hard (5 min)
- **Countdown timer** with urgency pulse animation under 30 seconds
- **Real-time validation** - highlights rule violations as you play
- **Haptic feedback** - tactile responses on cell selection, number entry, errors, and wins
- **Solution reveal** - color-coded answers showing your progress
- **Win celebration** - animated board on puzzle completion
- **Native feel** - safe area support, status bar theming, Android back button handling, app lifecycle management

## Tech Stack

- **Capacitor 8** - Native runtime for iOS and Android
- **Vanilla JavaScript** - No frameworks, ~550 lines of game logic
- **Vite 5** - Build tool
- **CSS3** - Grid layout, CSS custom properties, clamp() for responsive sizing

### Capacitor Plugins

| Plugin | Purpose |
|--------|---------|
| `@capacitor/haptics` | Tactile feedback on interactions |
| `@capacitor/status-bar` | Native status bar theming |
| `@capacitor/splash-screen` | Launch screen management |
| `@capacitor/app` | Lifecycle events, Android back button |
| `@capacitor/preferences` | Local storage for game state |

## Getting Started

### Prerequisites

- Node.js 18+
- Xcode 15+ (for iOS)
- Android Studio (for Android)

### Install

```bash
git clone https://github.com/alfredang/sudoku-mobile.git
cd sudoku-mobile
npm install
```

### Run in Browser

```bash
npm start
```

Opens at http://localhost:5173

### Run on iOS

```bash
npm run build && npx cap sync
npx cap open ios
```

In Xcode: select a simulator, press Cmd+R.

### Run on Android

```bash
npm run build && npx cap sync
npx cap open android
```

In Android Studio: wait for Gradle sync, click the play button.

## Project Structure

```
src/
  index.html              # Game HTML + Capacitor meta tags
  css/style.css           # Game CSS + mobile adaptations
  js/capacitor-init.js    # Capacitor plugin initialization
  public/js/game.js       # Game logic (puzzle generation, validation, UI)
capacitor.config.json     # Capacitor app configuration
vite.config.ts            # Vite build configuration
ios/                      # Xcode project
android/                  # Android Studio project
```

## Development Workflow

```bash
# 1. Edit source files in src/
# 2. Build and sync to native projects
npm run build && npx cap sync
# 3. Run on device/simulator
npx cap open ios       # or npx cap open android
```

## License

ISC
