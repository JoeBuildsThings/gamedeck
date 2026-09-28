# GameDeck 🎮⚡
> Rootless In-Game Performance Overlay & Optimizer for Android (10 to 14)
> Specifically tailored for MediaTek / Infinix Smart 9 class phones & general Android devices.

GameDeck is a native Android utility that provides a floating, non-intrusive in-game telemetry HUD (FPS, frame times, CPU load, RAM utilization, battery thermals) and reversible gaming profiles. It operates completely rootless and seamlessly integrates with **Shizuku** for privileged shell actions (hardware `SurfaceFlinger` latency querying, refresh rate locking, and safe background process trimming).

---

## Architecture Overview

```
com.example
├── GameDeckApp.kt                 # Application bootstrap & dependency container
├── MainActivity.kt                # Jetpack Compose UI (Bottom Navigation, 5 tabs)
├── model/                         # Pure domain models
│   ├── PerformanceMode.kt         # Balanced, Performance, Battery Saver
│   ├── TelemetryMetrics.kt        # Real-time metrics & 120-sample rolling frame times
│   ├── OverlayConfig.kt           # Coordinates, opacity, scale, toggles
│   ├── GameProfile.kt             # Per-game profile mappings
│   └── ActivityLogEntry.kt        # Reversible action log records
├── domain/                        # Pure business logic & unit testable parsers
│   ├── ModeEngine.kt              # Reversible actions & strict safety guardrails
│   └── SurfaceFlingerParser.kt    # Latency & layer parser (FPS, jank, 1% lows)
├── data/                          # Persistence layer
│   ├── local/                     # Room database (GameProfile, Session, ActivityLog)
│   ├── preferences/               # Jetpack DataStore Preferences
│   └── repository/                # GameDeckRepository orchestrator
├── system/                        # Android system integration
│   ├── shizuku/                   # ShizukuBridge shell executor with timeouts
│   ├── collectors/                # CPU (/proc/stat), RAM, Thermal, FPS collectors
│   ├── OverlayService.kt          # Foreground service with WindowManager HUD
│   ├── GameDeckTileService.kt     # Quick Settings Tile for mode cycling
│   ├── UsageDetector.kt           # Foreground app detection via UsageStatsManager
│   └── GameDeckAccessibilityService.kt # Optional zero-latency accessibility detector
└── ui/                            # Jetpack Compose UI layer
    ├── theme/                     # Dark gaming palette (OLED black, neon cyan, purple, red)
    ├── screens/                   # Home, Games, Sessions, Tools, Settings
    └── overlay/                   # Draggable HUD pill, rolling graph, and quick panel
```

---

## Build Instructions

### 1. Building in Android Studio
1. Open Android Studio (Ladybug or newer).
2. Open the project root folder.
3. Allow Gradle to sync.
4. Select `app` and run on your physical device or emulator:
   ```bash
   ./gradlew assembleDebug
   ```

### 2. Building on Phone via Termux
GameDeck avoids heavy annotation processors (uses KSP and lightweight standard AndroidX libraries) and can be built directly inside Termux with OpenJDK 17:
```bash
# 1. Install prerequisites in Termux
pkg update && pkg install -y openjdk-17 git gradle

# 2. Set Java Home
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk

# 3. Build APK
gradle assembleDebug
```
The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Phone Setup & Pairing Shizuku

### Step 1: Initial Permissions
Open GameDeck and tap the **Fix** button on each checklist item:
1. **Display Over Other Apps**: Allows GameDeck to draw the floating overlay pill (`TYPE_APPLICATION_OVERLAY`).
2. **Usage Access**: Allows GameDeck to detect when your registered games launch.
3. **Notification Permission**: Keeps the foreground service alive during gaming sessions.
4. **Battery Optimization Exemption**: Prevents OEM battery savers (Transsion HiOS / XOS) from killing the HUD.

### Step 2: Pairing Shizuku with Wireless Debugging (Android 11+)
1. Install **Shizuku** from Google Play or GitHub.
2. Connect to Wi-Fi.
3. On your phone, go to **Settings → Developer Options → Wireless Debugging** and turn it ON.
4. Tap **Pair device with pairing code**.
5. Open Shizuku, tap **Pairing**, and enter the 6-digit code from the notification.
6. In Shizuku, tap **Start**.
7. Return to GameDeck: you will see a prompt to authorize Shizuku. Tap **Grant Permission**.

---

## What Performance Mode Can & Cannot Do Without Root

### What GameDeck DOES Do (Honest & Safe):
- **Real FPS & Frame Times**: When Shizuku is granted, reads actual hardware frame present timestamps from `dumpsys SurfaceFlinger --latency <layer>`, calculating real-time FPS, frame variance, 1% low FPS, and jank rate.
- **Refresh Rate Locking**: Locks high refresh rate (e.g., 90Hz / 120Hz) via `Settings.System.peak_refresh_rate` where supported.
- **Do Not Disturb (DND)**: Automatically silences popups and call heads-up notifications during gameplay.
- **Safe Background Process Trimming**: Safely runs `am force-stop` **only** for user-selected third-party apps, and runs `cmd activity kill-all` / cache trim to free RAM for the foreground game.
- **Panic Restore**: Guarantees one-tap reversion of DND, refresh rates, and settings to original pre-game values.

### What it CANNOT Do (Without Kernel Root):
- Cannot overclock CPU or GPU clock frequencies (frequencies are strictly governed by kernel governor tables and thermal daemon `thermal-engine` / MediaTek Energy Aware Scheduling).
- Cannot modify system thermal trip thresholds.
- Cannot bypass thermal throttling when the device exceeds 45°C.

---

## Absolute Safety Rules
GameDeck strictly implements safety rules:
- **Never touches system packages**: Packages starting with `android`, `com.android`, `com.google.android.gms`, `com.mediatek`, `com.transsion`, or `com.hoffnung` are hard-blocked from process termination.
- **Zero package state modification**: GameDeck never executes `pm uninstall`, `pm disable`, `pm clear`, or `pm hide`.
- **Reversible actions**: All mode transitions are logged and reversible via the Panic Restore button.
