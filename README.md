# LumiAI — Android Flashlight App

Flashlight app with Free (AdMob) and Pro (one-time purchase) tiers.
Pro tier unlocks Gemini Nano AI features.

## Tech Stack
- **Language**: Kotlin 1.9 + Coroutines
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Flash control**: CameraX
- **Persistence**: Room + DataStore
- **Billing**: Google Play Billing 7.x (one-time INAPP)
- **Ads**: AdMob + UMP (GDPR-compliant consent)
- **Analytics**: Firebase Analytics + Crashlytics
- **AI (Pro)**: Gemini Nano on-device + ML Kit

## Setup
1. Copy `local.properties.template` → `local.properties` and fill values
2. Add your `google-services.json` from Firebase console
3. Replace AdMob IDs in `app/build.gradle.kts` (release block)
4. Create `pro_unlock` in-app product in Play Console

## Architecture
```
MainActivity
 └── NavHost
      ├── FlashScreen ──── FlashViewModel
      │                        ├── FlashRepository (CameraX)
      │                        ├── BillingRepository (Play Billing)
      │                        ├── SettingsRepository (DataStore)
      │                        └── StrobeController (Coroutines)
      ├── SettingsScreen
      ├── ProPaywallScreen
      └── OnboardingScreen
```

## Checklist before Play Store submission
- [ ] Replace test AdMob IDs with real ones
- [ ] Add google-services.json
- [ ] Create `pro_unlock` INAPP product in Play Console (€2.99 suggested)
- [ ] Generate release keystore and configure local.properties
- [ ] Set correct ADMOB_APP_ID in local.properties
- [ ] Test consent flow on EU device/emulator (UMP)
- [ ] Add Privacy Policy URL (required for any app using camera/ads)
- [ ] Replace stub icons with real artwork
- [ ] Test on device WITHOUT flash hardware (screen mode fallback)
