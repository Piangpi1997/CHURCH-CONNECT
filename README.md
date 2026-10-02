# CMF Church App

Native Android application for Christ Mission Fellowship Church (CMF), Setapak, Kuala Lumpur. The current implementation combines Jetpack Compose, Firebase Authentication/Firestore/Cloud Functions/Cloud Storage/FCM, server-owned membership workflows, and an explicitly local-only demo mode. The launcher name **CMF CHURCH APP** and existing premium launcher artwork are preserved.

The Compose interface uses a shared light/dark Glass design system while preserving the existing workflows. See the [Glass UI implementation notes](docs/GLASS_UI_IMPLEMENTATION.md) for design choices, tested screens, and current product limits.

## Build and preview

Requirements: JDK 17+, Android SDK 35, and internet access for first-time dependency downloads.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintRelease
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. If no owner-provided release signing environment is configured, the release variant is unsigned and only suitable for build validation. Without `app/google-services.json`, the app uses an in-memory local preview; records are not sent to the church and no money moves. Firebase-backed sign-in requires the church's real Firebase configuration. Android UI/device tests require an attached emulator or device.

## Firebase and payment

Start with [Firebase production setup](docs/FIREBASE_PRODUCTION_SETUP.md), [guarded initial-admin setup](docs/INITIAL_ADMIN_SETUP.md), [App Check / Play Integrity](docs/APP_CHECK_SETUP.md), [admin and payment setup](docs/ADMIN_AND_PAYMENT_SETUP.md), and [payment provider status](docs/PAYMENT_PROVIDER_STATUS.md). The church project ID, real Firebase configuration, verified first-admin UID, App Check Console enforcement, online payment provider, and production signing key were not supplied. Never replace them with examples or fabricated credentials.

Registration fees are server-calculated from `churches/{churchId}.registrationFees` and saved as an immutable snapshot. Sample amounts are RM20 individual and RM50 family, but church leadership must approve actual fees. The only supported payment path is **manual offline transfer reference plus finance verification**. Online checkout, gateway callbacks, refunds, and automated reconciliation are **not integrated**.

## Localization

The app-owned interface has English and Myanmar resources, a persisted language selector, and a System default option. Tedim is listed as **English until reviewed**; no Tedim translation is claimed. See the [localization guide](docs/LOCALIZATION.md) for review and resource parity checks.

## Automated checks

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintRelease
(cd functions && npm test && npm run build && npm run test:integration)
(cd security-tests && npm test)
```

The Firebase Emulator suites use only demo project ID `demo-church-connect`; they do not contact production. The [Android E2E](docs/ANDROID_E2E_CHECKLIST.md) and [live FCM](docs/LIVE_FCM_CHECKLIST.md) checklists require a real test device and configured non-production Firebase project.

## Phase 1 deployment and release gates

See the [deployment guide](docs/DEPLOYMENT_GUIDE.md), [secure release signing guide](docs/RELEASE_SIGNING.md), [Firebase setup](docs/FIREBASE_SETUP.md), [security model](docs/SECURITY_MODEL.md), [test and release guide](docs/TEST_AND_RELEASE.md), and [Phase 1 readiness report](docs/PHASE1_FINAL_REPORT.md). Production readiness must remain **NOT READY** until the owner-controlled Firebase/App Check setup, signing, required church-language review, and device/FCM validation are complete or explicitly accepted for a limited pilot.
