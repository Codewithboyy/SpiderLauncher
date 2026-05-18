# SpiderLauncher — Android 🕷️📱

[![Android CI/CD](https://github.com/codewithboyy/SpiderLauncher/actions/workflows/android-ci.yml/badge.svg)](https://github.com/codewithboyy/SpiderLauncher/actions/workflows/android-ci.yml)

A native Android launcher for Minecraft Java Edition, built with **Kotlin + Jetpack Compose** and automated end-to-end with **GitHub Actions**.

---

## Features

| Feature | Status |
|---|---|
| Browse all Minecraft versions | ✅ |
| Download client JAR with SHA1 verify | ✅ |
| Dark / Light theme (Material You) | ✅ |
| Real-time console output | ✅ |
| RAM allocation slider | ✅ |
| Search & filter versions | ✅ |
| Snapshot version toggle | ✅ |
| PojavLauncher bridge support | ✅ |
| Signed release APK via CI | ✅ |
| Google Play AAB deployment | ✅ |

---

## GitHub Actions Workflows

### `android-ci.yml` — Main CI/CD Pipeline
Triggers on every push, PR, and tag.

| Job | Description |
|---|---|
| **Lint** | Android lint + Kotlin style checks |
| **Unit Tests** | `testDebugUnitTest` with JUnit report |
| **Build Debug APK** | Uploads unsigned APK as artifact |
| **Build Release APK** | Signs with keystore from secrets |
| **Build Release AAB** | For Play Store submission |
| **GitHub Release** | Auto-creates release on `v*` tags |
| **Instrumented Tests** | Runs on emulator (macOS runner) |

### `version-bump.yml` — Manual Version Bumper
Go to **Actions → Version Bump** and select `patch / minor / major`.  
Automatically updates `build.gradle`, commits, and pushes a new `v*` tag which triggers the full release pipeline.

### `play-store-deploy.yml` — Play Store Deployment
Go to **Actions → Deploy to Play Store** and select a track (`internal / alpha / beta / production`).

---

## Setting Up GitHub Actions Secrets

Go to your repo → **Settings → Secrets and Variables → Actions** and add:

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Your release keystore encoded as base64 |
| `KEYSTORE_PASSWORD` | Password for the keystore file |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Password for the key |
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Play service account JSON (for Play Store deploy) |

### Generating a keystore & encoding it

```bash
# 1. Generate keystore
keytool -genkey -v \
  -keystore spiderlauncher-release.jks \
  -alias spiderlauncher \
  -keyalg RSA -keysize 2048 \
  -validity 10000

# 2. Encode to base64 (copy the output → KEYSTORE_BASE64 secret)
base64 -i spiderlauncher-release.jks | pbcopy   # macOS
base64 spiderlauncher-release.jks | xclip        # Linux
```

---

## Building Locally

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps

```bash
cd android

# Debug build
./gradlew assembleDebug

# Release build (needs keystore env vars set)
./gradlew assembleRelease

# Run tests
./gradlew testDebugUnitTest

# Install on connected device
./gradlew installDebug
```

---

## Creating a Release

```bash
# Via GitHub Actions UI (recommended)
# Go to Actions → Version Bump → Run workflow → select bump type

# Or manually:
git tag -a v1.1.0 -m "Release 1.1.0"
git push origin v1.1.0
# GitHub Actions will build, sign, and publish automatically
```

---

## Project Structure

```
android/
├── .github/workflows/
│   ├── android-ci.yml        # Main CI/CD
│   ├── version-bump.yml      # Manual version bumper
│   └── play-store-deploy.yml # Play Store deployment
├── app/
│   ├── src/main/
│   │   ├── java/com/spiderlauncher/android/
│   │   │   ├── SpiderApp.kt
│   │   │   ├── model/         # Data models
│   │   │   ├── network/       # Retrofit API client
│   │   │   ├── repository/    # Data layer
│   │   │   ├── viewmodel/     # ViewModels
│   │   │   └── ui/            # Compose screens & theme
│   │   └── res/               # Resources
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
└── settings.gradle
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Networking | Retrofit 2 + OkHttp 4 |
| Async | Kotlin Coroutines + Flow |
| DI | Manual (no Hilt for simplicity) |
| Build | Gradle 8 |
| CI/CD | GitHub Actions |

---

## Disclaimer

This is an unofficial launcher. Minecraft is a trademark of Mojang Studios. Not affiliated with or endorsed by Mojang.
