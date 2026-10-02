# Test and Release Guide

## Reproducible checks

```bash
# Android JVM tests, debug and release APKs, optimized release lint (requires SDK 35)
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintRelease

# Cloud Functions unit tests and strict TypeScript build
cd functions
npm ci
npm test
npm run build

# Auth, Firestore, and Functions Emulator critical-flow tests
npm run test:integration

# Firestore and Storage Emulator rules/attack tests
cd ../security-tests
npm ci
npm test
```

The security-test package supplies the Firebase CLI used by the integration script. A first emulator run downloads the required binaries. Both emulator commands use the demo project ID `demo-church-connect`; they do not contact live services. Do not substitute a production project ID for emulator tests.

## Latest validation

**32 automated test cases passed; 0 failed; 0 skipped.** The total is 10 Android JVM tests, 7 Cloud Functions unit tests, 2 Functions Emulator end-to-end cases, 11 Firestore rules cases, and 2 Storage rules cases. The emulator flows cover individual/family registration, manual payment and authorized verification, admin review and rejection, member-number generation, and digital ID/QR. Rules tests cover private reads, cross-church isolation, client-side role/payment changes, audits, notifications, and approved Storage paths.

The final Gradle run passed `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease`, and `:app:lintRelease`. The debug APK is signed with the standard debug key. The optimized release APK passes R8/lint but is **unsigned** because no owner-approved production key was provided; it is not ready for distribution. Check package `org.cmf.churchconnect`, version `1.0.1` / versionCode `2`, target SDK 35, min SDK 26, app label **CMF CHURCH APP**, and the owner’s certificate fingerprint before any release.

Functions unit tests, strict TypeScript compilation, Functions Emulator tests, and Firestore/Storage Emulator tests all passed. The Functions emulator reported that its configured Node 20 runtime was using the host's Node 24 and that the current `firebase-functions` package is outdated. These warnings did not fail the tests; use a supported Node 20 build/deploy environment and review dependency upgrades separately because the CLI warns of potential breaking changes.

## Checks that still require church resources

No Android emulator/device was attached, so instrumentation/UI E2E was not run. No live Firebase project, valid App Check attestation, real FCM token/device, payment provider, production signing key, or App Check Console configuration was available. Do not treat emulator tests as production acceptance. Use [Android UI/device E2E](ANDROID_E2E_CHECKLIST.md), [Live FCM](LIVE_FCM_CHECKLIST.md), [Firebase setup](FIREBASE_PRODUCTION_SETUP.md), and [Deployment Guide](DEPLOYMENT_GUIDE.md) to close those gates.