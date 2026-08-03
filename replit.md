# boykta net — Djezzy Self-Service Android App

## Overview
**boykta net** is a dark-themed Djezzy (Algerian telecom) self-service Android app built with Kotlin + Jetpack Compose. It lets subscribers manage their accounts, transfer credit/data, activate offers, send free SMS, and more — all via the official Djezzy API.

## Architecture
| Layer | Files |
|---|---|
| UI screens | `ui/screens/*.kt` |
| ViewModels | `viewmodel/*.kt` |
| API layer | `data/api/DjezzyApi.kt`, `data/api/ApiClient.kt` |
| Models | `data/models/ApiModels.kt` |
| Local storage | `data/local/TokenStorage.kt` (DataStore) |
| Navigation | `navigation/Screen.kt`, `navigation/NavGraph.kt` |
| Ads | `ads/AdsManager.kt` (Start.io, App ID: 207841284) |
| SMS auto-fill | `data/receiver/SmsReceiver.kt` |

## Screens
| Screen | Route | Purpose |
|---|---|---|
| SplashScreen | `splash` | Token validity check → Dashboard or Auth |
| AuthScreen | `auth` | 6-box OTP login, SMS auto-fill, shake on error, multi-account |
| DashboardScreen | `dashboard` | Balance, active package card, 5-button grid, account switcher |
| OffersScreen | `offers` | Paid offers list, shake / activate-product flow, ads |
| FlexyScreen | `flexy` | Data transfer (500MB/1GB/2GB selector) + credit transfer |
| FreeSmsScreen | `free_sms` | كلمني / فليكسيلي only |
| WalkWinScreen | `walk_win` | 2 GB weekly + 7-day countdown + 100 DZD shortcut |
| SettingsScreen | `settings` | Smart network service toggles, Ranati disable, logout |
| MgmScreen | `mgm` | Send/track MGM invitations (max 5), activate reward |

## Key Design Rules
- **Dark theme only**: black `#000`, neon blue `#00D4FF`, dark gray cards
- **No emojis** — Material/SVG icons only
- **Western digits** (Locale.US) everywhere — no Arabic-Indic numerals
- **Max 5 saved accounts** stored in DataStore via `TokenStorage.saveAccount()`
- **ABI split**: `armeabi-v7a` only for release builds

## API
- Base URL: `https://apim.djezzy.dz/mobile-api/`
- OkHttp with auto token-refresh Authenticator (`ApiClient.kt`)
- All screens use the `DjezzyApi` Retrofit interface

## CI/CD
- `.github/workflows/android-build.yml` — JDK 17, decode keystore, `assembleRelease`, upload APKs
- Secrets required: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

## Build
```bash
./gradlew assembleRelease
```

## User Preferences
- Western (Locale.US) number formatting everywhere — never Arabic-Indic digits
- Dark theme only — no light theme support
- No emoji in UI — use Material Outlined icons
- ABI: armeabi-v7a only for now, arm64-v8a to be added after testing
- MGM invitations must be a separate screen (not a tab in FreeSms)
- FlexyScreen: internet tab first, credit tab second
- WalkWin must show 7-day countdown and 100 DZD shortcut button
