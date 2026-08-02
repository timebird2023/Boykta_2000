# boykta net — Kotlin Android App

## Project overview

A native Android application (Kotlin + Jetpack Compose) for Djezzy mobile subscribers in Algeria. The app provides a clean, dark-theme UI for:

- OTP-based login using the Djezzy OAuth2 API
- Balance and active packages dashboard
- Data package activation (13 offers, shake + activate-product endpoints)
- Credit and data (Flexy) transfers with history
- Free SMS — CallMe / FlexyLi
- MGM (refer-a-friend) invitations and reward activation
- Walk & Win weekly 2GB reward
- Settings / account management

Monetisation: Start.io interstitial ads (App ID `207841284`) shown after each successful operation.

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Networking | Retrofit 2 + OkHttp 4 |
| Local storage | DataStore Preferences |
| ViewModel | AndroidX ViewModel / Lifecycle |
| Ads | Start.io SDK 4.x |
| Build | Gradle (Kotlin DSL), ABI splits |
| CI/CD | GitHub Actions |

## Project structure

```
app/src/main/java/com/boykta/net/
  MainActivity.kt              — entry point, SDK init, edge-to-edge
  ads/
    AdsManager.kt              — Start.io preload + show interstitial
  data/
    api/
      ApiClient.kt             — OkHttp + Retrofit singleton
      DjezzyApi.kt             — All Retrofit endpoints
    local/
      TokenStorage.kt          — DataStore: token, msisdn, account name
    models/
      ApiModels.kt             — Request/response data classes + PAID_OFFERS list
    receiver/
      SmsReceiver.kt           — Auto-reads OTP from SMS
  navigation/
    Screen.kt                  — Route constants
    NavGraph.kt                — NavHost with all composable destinations
  ui/
    theme/                     — Color, Type, Theme (dark only)
    components/
      Modals.kt                — SuccessModal, ErrorModal, ConfirmModal
    screens/
      SplashScreen.kt          — Token validity check + animated splash
      AuthScreen.kt            — Phone + OTP login, SMS auto-read
      DashboardScreen.kt       — Balance, active packages, history, service grid
      OffersScreen.kt          — 13 paid offers with confirm + ad flow
      FlexyScreen.kt           — Credit & data transfer + history tabs
      FreeSmsScreen.kt         — CallMe / FlexyLi + MGM tabs
      WalkWinScreen.kt         — Walk & Win 2GB weekly reward
      SettingsScreen.kt        — Account info, share, developer link, logout
  viewmodel/
    AuthViewModel.kt
    DashboardViewModel.kt
    OffersViewModel.kt
    FlexyViewModel.kt
```

## Build

Replit is used for **code editing and linting only**. The actual Gradle build runs on GitHub Actions.

### GitHub Actions secrets required

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore (.jks) |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |
| `STORE_PASSWORD` | Keystore password |

### CI/CD flow

1. Push to `main` → GitHub Actions triggers `.github/workflows/android-build.yml`
2. Workflow decodes keystore, runs `./gradlew assembleRelease`
3. Produces two APKs: `arm64-v8a` and `armeabi-v7a` (≈ 4 MB each after R8)
4. APKs uploaded as workflow artifacts for 14 days

## API reference

**Base URL:** `https://apim.djezzy.dz/mobile-api/`

**Required headers on every request:**
```
User-Agent: MobileApp/3.0.6
Accept: application/json
Accept-Language: ar
Authorization: Bearer {token}
```

**MSISDN format:** `2137XXXXXXXX` (213 + local number without leading 0)

**Error handling:** Always parse `response.message.ar` and display in the ErrorModal. Never crash on 4xx/5xx.

## User preferences

- Dark theme only — no emojis in UI
- Arabic RTL layout throughout
- Success modal always shows: "تمت العملية بنجاح. يرجى متابعة الصفحة ليصلك كل جديد."
- Interstitial ad fires immediately after the success modal is dismissed
