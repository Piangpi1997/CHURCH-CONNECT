# CMF Church App

Native Android application for Christ Mission Fellowship Church (CMF), Setapak, Kuala Lumpur. The current implementation combines Jetpack Compose, Firebase Authentication/Firestore/Cloud Functions/Cloud Storage/FCM, server-owned membership workflows, and an explicitly local-only demo mode. The launcher name **CMF CHURCH APP** and the existing premium launcher artwork are preserved.

## Build and preview

Requirements: JDK 17+, Android SDK 35, and internet access for first-time dependency downloads.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Without `app/google-services.json`, the app uses an in-memory local preview; its records are not sent to the church and no money moves. Firebase-backed sign-in requires the church's real Firebase configuration. Emulator/device UI tests require an attached Android emulator or device.

## Firebase and production setup

Start with [Environment and Deployment](docs/ENVIRONMENT_AND_DEPLOYMENT.md), [Firebase Setup](docs/FIREBASE_SETUP.md), [Admin and Payment Setup](docs/ADMIN_AND_PAYMENT_SETUP.md), [Database Collections and Indexes](docs/DATABASE_COLLECTIONS.md), [Security Model](docs/SECURITY_MODEL.md), and [Test and Release Guide](docs/TEST_AND_RELEASE.md). The church project ID, Firebase configuration, verified first-admin UID, Play Integrity/App Check console registration, payment-provider contract, and production signing key were not supplied for this task. Never replace them with example or fabricated credentials.

Registration fees are server-calculated from `churches/{churchId}.registrationFees` and stored with each payment as an immutable snapshot. The default values in the example configuration are RM20 individual and RM50 family, in integer minor units. The only implemented payment path is **manual offline transfer reference + finance verification**; online payment, gateway callbacks, refunds, and automated reconciliation are **not integrated**.

## Automated checks

```bash
# Android unit tests and debug APK
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Backend unit tests and type-check/build
(cd functions && npm ci && npm test && npm run build)

# Auth + Firestore + Functions Emulator integration tests
(cd functions && npm run test:integration)

# Firestore + Storage Emulator security tests
(cd security-tests && npm ci && npm test)
```

The emulator integration test covers server-calculated fees, duplicate registration/reference/submission/review protection, same-church roles, finance verification, concurrent member-number assignment, family records, explicit rejection reasons, and secure QR issuance/rotation. Security tests cover client-write denial, private-data isolation, role escalation, audit logs, QR/lock opacity, announcements/events, and Storage paths/MIME types.

## Current completion boundary

The server-side critical individual/family flows and emulator rule suites are validated. The optimized release variant builds but is **unsigned**. The app has no live Firebase environment in this workspace, the manual transfer process is not an online payment integration, and full Tedim/Burmese Compose localization, live FCM delivery, and Android UI E2E remain incomplete. See [the Phase 1 audit](docs/PHASE1_FINAL_REPORT.md) for exact PASS/PARTIAL/CONFIGURATION REQUIRED statuses and blockers.
