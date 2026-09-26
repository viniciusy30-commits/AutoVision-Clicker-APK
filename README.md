# Android Jetpack Compose CLI Template

A lightweight, clean, and CLI-first Android Jetpack Compose template. Designed for developers who prefer VS Code, Neovim, or terminal workflows over heavy IDEs.

This template is "Open Source First", optimized for F-Droid / IzzyOnDroid standards and maximum build transparency.

## Core Philosophy

1. Terminal Centric: no dependency on Android Studio. Built to be run with `gradlew` and `adb`.
2. Privacy & Transparency: no proprietary Google binaries, no obfuscated encryption blocks, fully compatible with IzzyOnDroid and F-Droid audit standards (`dependenciesInfo` is always disabled).
3. Minimalist: only essential dependencies included.

## Features

1. Modern UI stack: Jetpack Compose with Material3, light and dark theme plus dynamic color on Android 12+.
2. Broad compatibility: Android 8.0 (API 26) up to the latest (API 36).
3. Production ready: pre-configured R8 for minification and shrinking in release builds.
4. Clean architecture: zero boilerplate business logic, ready for your code immediately.
5. Offline capable: dependency versions are locked in the Gradle version catalog for reproducible builds.
6. Store ready: GitHub Actions CI builds the debug APK and runs unit tests on every push and pull request.

## Requirements

1. JDK 21 or higher.
2. Android SDK Command-line Tools (compileSdk 36 / targetSdk 36).
3. Git.

## Getting Started (Terminal Workflow)

### 1. Create a project from the template

Clone the repository and rename the package as needed:

```bash
git clone https://github.com/ahmedthebest31/Android-Compose-CLI-Template.git my-app
cd my-app
```

### 2. Configure the project

Open `gradle.properties` and edit the values:

```properties
APP_ID=com.yourcompany.project
APP_NAME=Your App Name
VERSION_CODE=1
VERSION_NAME=1.0.0
```

The `app_name` shown on the launcher is injected from `APP_NAME` at build time.

### 3. Build the app

No IDE required. Use the wrapper directly.

Debug build:

```bash
./gradlew assembleDebug
```

Release build (minified and signed, see Signing below):

```bash
./gradlew assembleRelease
```

### 4. Run unit tests

```bash
./gradlew test
```

### 5. Install and run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6. Clean the project

```bash
./gradlew clean
```

## Secure Signing (Release)

1. Copy the example file: `cp signing.properties.example signing.properties`
2. Open `signing.properties` and add your real keystore details (path, passwords, alias).
3. The build script automatically detects the file and signs the release APK.

Notes:

1. `signing.properties` is strictly ignored by Git to protect your secrets.
2. Without it, the release build produces an unsigned APK (the build still succeeds and prints a note).

## Project Structure

1. `app/src/main/java/com/ahmedsamy/app`: main Kotlin source code (Hello World sample).
2. `app/src/main/java/com/ahmedsamy/app/ui/theme`: Material3 theme, color and typography definitions.
3. `app/src/main/java/com/ahmedsamy/app/util`: small pure-Kotlin utilities with unit tests.
4. `app/src/test/java/com/ahmedsamy/app`: JVM unit tests (JUnit 4).
5. `gradle/libs.versions.toml`: centralized dependency management (version catalog).
6. `.github/workflows/build.yml`: CI pipeline for debug APK and unit tests.

## Offline Development

All dependency versions are locked in `gradle/libs.versions.toml`. After the first online build (which populates the Gradle cache), subsequent builds can run fully offline:

```bash
./gradlew --offline assembleDebug
./gradlew --offline test
```

## License

Released under the [MIT License](LICENSE).
