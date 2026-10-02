# Test and Release Guide

## Reproducible checks

```bash
# Android local JVM tests and APK variants (requires Android SDK 35)
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease

# Cloud Functions unit tests and TypeScript build
cd functions
npm ci
npm test
npm run build

# Auth, Firestore, and Functions emulator critical-flow tests
npm run test:integration

# Firestore and Storage emulator rules attack tests
cd ../security-tests
npm ci
npm test
```

The security-test package includes the Firebase CLI used by the integration script. First-time emulator runs download emulator binaries/rules runtime. Emulator runs use only the `demo-church-connect` demo project ID and must never target live data.

The callable integration tests exercise configured individual/family fees, missing-configuration failure, payment record shape, duplicate reference/verification/submission/review handling, same-church authorization, finance queue redaction, concurrent member-number allocation, family household data, required rejection reasons, no member ID on rejection, QR validation, and QR rotation. The Firestore/Storage suites exercise client-write denial, private reads, role escalation, audit integrity, announcement/event writes, Storage owner/staff paths, and cross-church isolation. FCM delivery with real device tokens, real App Check attestations, payment-provider callbacks, and Android UI/device scenarios are not covered.

Latest validation passed **28 automated test cases**: 8 Android unit tests (including status/query filtering), 5 backend unit tests, 2 Functions Emulator integration tests, 11 Firestore rules tests, and 2 Storage rules tests.

## Current build evidence

- Android local unit tests: passed.
- Debug APK: built successfully and signed with the standard debug key.
- Optimized release variant: compiled and passed release lint/R8 checks, but no production signing key was supplied; the resulting release APK is unsigned and is not distributable to users.
- Functions unit tests: passed; strict TypeScript build passed.
- Functions Emulator integration tests: passed.
- Firestore and Storage Emulator attack tests: passed.
- No Android emulator/device was attached, so instrumentation/UI E2E was not run.

Build metadata: package `org.cmf.churchconnect`, version `1.0.1` / versionCode `2`, target SDK 35, min SDK 26. The launcher app name remains **CMF CHURCH APP**, and the premium icon assets were preserved. Debug-only cleartext traffic is scoped to `src/debug`; release manifest defaults to cleartext disabled. Release shrinking/optimization uses R8.

## Signing and production release

A production release is not ready until the church provisions and verifies its Firebase project, deploys reviewed Functions/rules/indexes/Storage rules, bootstraps the correct admin, registers App Check/Play Integrity and enables service enforcement, and signs with the approved upload key in a secure local/CI environment. No signing key, service-account credential, API secret, or live Firebase configuration belongs in Git or this source ZIP.

After owner-controlled signing is configured, rebuild the release variant and verify the package name, version code, manifest permissions, App Check provider, minification, Firebase project, and key fingerprint before distribution. Test on supported physical devices and Android versions; exercise notification permission/token refresh and offline/reconnect behavior against a non-production Firebase project first.
