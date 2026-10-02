# Church Connect build status

The project builds as a native Android application and includes a local preview path plus a Firebase-backed Phase 1 implementation. The APK in `dist/` is a **debug build**; because no real Firebase project config was provided, the distributed APK opens the explicitly labeled in-memory preview.

## Included workflows

The Android app provides email/password account flows, password reset, member and family applications, configurable fee snapshots, manual transfer-reference submission, an in-app notification inbox, church announcements and events, digital member IDs, and an admin workspace. When Firebase is configured, trusted Cloud Functions handle profile creation, payment verification, application review, approval/rejection, member-number generation, QR verification, and notification delivery. Finance users receive a reduced payment-review queue rather than direct access to household and contact records.

QR IDs use opaque rotating tokens; the backend stores only token hashes and returns minimal verification details. Client writes to protected membership, finance, role, audit, and QR records are denied by the Firestore rules. Offline demo changes stay in memory and are not payments or church records.

## Verification completed

- Android: `:app:testDebugUnitTest` and `:app:assembleDebug` succeeded. **5/5** Android domain unit tests passed; debug APK generated.
- Cloud Functions: `npm test` passed **5/5** domain tests; `npm run build` passed strict TypeScript compilation.
- Firestore security: Firebase Emulator run passed **7/7** rules tests, including owner-only queries, cross-church isolation, finance data minimization, denial of client-side approval/role/payment changes, notification ownership, and immutable audit/config writes.

The Functions package targets Node.js 20. The checks in this environment ran under Node.js 24 and completed successfully, with an engine-version warning from development tooling.

## Run and test

```bash
# Build and test Android (JDK 17+, Android SDK 35+)
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Build and test trusted Functions (Node.js 20 recommended)
cd functions && npm install && npm test && npm run build

# Test Firestore security rules (Java and Firebase Emulator download required)
cd security-tests && npm install && npm test
```

Install `dist/church-connect-debug.apk` on an Android device/emulator for the self-contained preview. Firebase-backed usage requires the church to add its own `app/google-services.json`, configure `churches/{CHURCH_ID}`, enable the listed Firebase services, deploy Functions and security rules, and bootstrap a verified initial administrator. Do not use the fake example Google Services file for production.

## Not production-connected yet

No real Firebase project, production credentials, App Check attestation, authorized administrator UID, deployment, or online payment-provider contract was supplied or configured. **Online payments are not integrated**: the app supports the manual/offline reference-and-finance-verification path only. Most Compose screen text remains English; the app-name resources include English and Burmese, but full Burmese/Tedim screen localization still needs translation and church review.
