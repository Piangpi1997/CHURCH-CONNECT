# CMF Church App Build Status

The native Android project has a resource-backed English/Myanmar interface, persisted app-language selection, Android 13+ per-app language synchronization, Firebase-backed Phase 1 workflows, an explicitly local-only demo path, and a shared light/dark Glass UI. Tedim is an English fallback pending church review. The app title **CMF CHURCH APP** and existing premium launcher artwork are unchanged. See [Glass UI implementation](GLASS_UI_IMPLEMENTATION.md).

## Latest verification

- Android `:app:testDebugUnitTest`: **10/10 passed**; `:app:assembleDebug`, `:app:assembleRelease`, and `:app:lintRelease` passed. The release APK is unsigned.
- Cloud Functions unit tests: **7/7 passed**; strict TypeScript build passed.
- Functions Emulator integration flows: **2/2 passed**.
- Firestore and Storage Emulator rules/security: **13/13 passed** (11 Firestore, 2 Storage).
- Total: **32 passed, 0 failed, 0 skipped.** The Firebase Emulator runs used only `demo-church-connect`; they do not prove production Firebase/App Check or live FCM behavior. No Android emulator/device was attached, so runtime screenshots, TalkBack, large-font layout, and physical QR scanning were not checked.
- A static spot check of selected base-palette text/surface pairs measured 5.59:1 or higher; this is not a rendered-screen contrast certification. Localized resources were unchanged and no new user-visible strings were added.

The Functions Emulator output warned that the project targets Node 20 while this test host ran Node 24 and that the `firebase-functions` dependency is outdated. `npm ci` also reported dependency audit advisories (9 moderate in Functions and 19 in the security-test dependency tree, including 1 critical); this UI-only change did not alter dependency manifests. Tests still passed. Use the supported Node 20 runtime for deployment and review dependency upgrades separately.

## Artifacts and product boundary

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (signed by the standard debug key). The optimized release APK is `app/build/outputs/apk/release/app-release-unsigned.apk`; it passes R8/release lint and remains unsigned because no owner-provided release key was supplied. Version is `1.0.1` / versionCode `2`, package `org.cmf.churchconnect`, min SDK 26, target SDK 35. A release signing key is required before distribution.

No live Firebase project configuration, `app/google-services.json`, Functions `.env`, deployment authorization, verified first-admin UID, production App Check enforcement, or online payment provider was supplied. Without a real Firebase app config, the app uses an in-memory preview; it creates no church records and moves no money. Manual offline transfer reference plus trusted finance verification is the only payment path.

## Run the checks

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintRelease
(cd functions && npm ci && npm test && npm run build && npm run test:integration)
(cd security-tests && npm ci && npm test)
```

For owner-controlled production actions see [Firebase Production Setup](FIREBASE_PRODUCTION_SETUP.md), [Initial Admin Setup](INITIAL_ADMIN_SETUP.md), [App Check Setup](APP_CHECK_SETUP.md), [Payment Provider Status](PAYMENT_PROVIDER_STATUS.md), [Release Signing](RELEASE_SIGNING.md), [Android E2E](ANDROID_E2E_CHECKLIST.md), [Live FCM](LIVE_FCM_CHECKLIST.md), [Deployment Guide](DEPLOYMENT_GUIDE.md), and the [Phase 1 report](PHASE1_FINAL_REPORT.md).
