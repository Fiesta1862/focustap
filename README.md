# FocusTap

A minimalist Android focus blocker inspired by the Brick physical screen-time
lock. Distracting apps are shielded until you physically tap a paired NFC tag
(or scan a printed QR anchor).

**Status:** functional MVP, open source, alpha quality. The release APK is
signed with the Android debug key so it can be installed without a keystore.

## Features

- **Physical anchors** — register an NTAG215 by its immutable UID, or generate
  and print a QR anchor. Scanning the same anchor starts and ends sessions.
- **Focus modes** — Deep Work, Study and Sleep by default; create your own with
  an app picker and website-domain list. Phone, Messages, Maps and Settings
  remain accessible.
- **Shield screen** — a full-screen, high-contrast lock showing the active mode
  and elapsed time. Back is disabled while locked.
- **Strict mode** — device-admin safeguards plus clock-change detection during
  a session.
- **Emergency unlock** — three uses per calendar month with a 60-second
  cooldown.
- **Dashboard** — live lock state, mode selector, today / 7-day focus time,
  streak and session history.
- **Local-first** — all state lives in DataStore on the device. No account,
  backend or analytics.

## Tech stack

Native Android, Kotlin, Jetpack Compose, Material 3, DataStore, ZXing, Android
NFC APIs. Built with Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24.

```
app/src/main/java/com/focustap/app/
├── MainActivity.kt                 Compose entry point + NFC foreground dispatch
├── FocusTapApp.kt                  Application, initialises FocusController
├── core/FocusController.kt         Shared state, anchor matching, session control
├── data/                           Models, DataStore repository
├── nfc/NfcAnchorManager.kt         Tag UID read + optional NDEF write
├── qr/QrManager.kt                 QR generation + scan intents
├── blocking/                       Accessibility guard, foreground service,
│                                   device admin, clock/boot receivers, shield
└── ui/                             Theme, navigation, ViewModel, screens
```

## How blocking works

| Capability | Implementation | Notes |
| --- | --- | --- |
| Detect blocked app opening | `AccessibilityService` watches window-state changes only. No screen content is read. | User enables it in Settings. |
| Show the shield | Launches `ShieldActivity` over the blocked app. | Back press is consumed. |
| Persistent session reminder | `ForegroundService` with a low-priority ongoing notification. | Uses `specialUse` foreground-service type on Android 14+. |
| Strict mode | `DeviceAdminReceiver` warns and locks the device if someone disables it; `TimeChangeReceiver` reacts to clock edits. | Full uninstall prevention requires **device-owner** provisioning (see below). |
| Website domains | Stored per mode and exposed in the UI. | DNS/VPN enforcement is **not implemented yet** — see Roadmap. |

### Device-owner (optional, for "cannot uninstall")

A user-profile device admin cannot block uninstalls on modern Android. On a
dedicated device you can provision FocusTap as device owner before adding an
account:

```sh
adb shell dpm set-device-owner com.focustap.app/.blocking.FocusDeviceAdminReceiver
```

This normally requires a factory reset first. It is entirely optional; the
accessibility shield, anchor pairing and emergency unlock work without it.

## Build locally

You need JDK 17 and Android SDK 34 installed, plus `ANDROID_HOME` configured.

```sh
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Releases

Pushing a `v*` tag (for example `v0.1.0-alpha`) triggers
`.github/workflows/release.yml`, which builds the APK on GitHub Actions and
publishes it as a GitHub Release. You can also run the workflow manually from
the Actions tab.

## Roadmap

- VPN-based enforcement for blocked website domains
- Biometric fallback for emergency unlock
- Wear OS companion anchor
- Exported/imported mode presets

## Safety note

FocusTap is a self-control tool. Do not provision it as device owner on a device
you cannot recover, and keep at least one emergency unlock available when you
are relying on it for accessibility or safety.
