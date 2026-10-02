# Deployment Guide

No live Firebase deployment or production release was performed for this source update. Use this owner-controlled sequence; emulator tests and unsigned builds are not production release evidence.

## Before a deployment window

1. Church owner verifies Firebase project ID/number, Android package, tenant ID, real `google-services.json`, Authentication providers, fee document, first-admin UID, deployment identity/IAM, Functions region, and billing prerequisites. Follow [Initial Admin Setup](INITIAL_ADMIN_SETUP.md) only after the user's account/profile and church/UID are independently verified. Keep local secrets outside Git and release archives.
2. Finance/legal owner confirms the manual-offline boundary or provides an approved online provider contract and secure server-side credentials. Until a provider integration passes its tests, online payment status remains **NOT INTEGRATED**.
3. Run Android unit/build/lint, Functions unit/type checks, Functions Emulator integration tests, Firestore and Storage rules tests, and inspect the security diff.
4. Validate App Check in a development project; ensure Play Integrity is registered for release and debug tokens exist only in the development project. Enable production enforcement per service only after valid-traffic testing.
5. Owner supplies and verifies the approved signing key in a protected local/CI environment. Build a signed candidate; verify signing certificate, package/version, manifest, R8, Firebase project, and release App Check provider.
6. Church reviewer approves Myanmar language; Tedim stays English fallback until a trusted church-language reviewer signs off.
7. Complete [Android UI/device E2E](ANDROID_E2E_CHECKLIST.md) and [Live FCM](LIVE_FCM_CHECKLIST.md) against the controlled non-production project. Approve a production pilot and rollback contact.

## Deploy reviewed backend/rules

After explicit project and tenant verification, review the exact deployment target and use:

```bash
firebase --project <VERIFIED_FIREBASE_PROJECT_ID> deploy --only functions,firestore:rules,firestore:indexes,storage
```

Never use a generic current-project alias for production; this repository has no configured Firebase project alias. After deploy, verify callable region, rules and indexes timestamps, App Check state, admin access, logs/audit monitoring, FCM, and cross-church denial using authorized test accounts.

## Go/no-go

Do not declare production ready while live Firebase and App Check enforcement are unverified, release is unsigned, payment status is misrepresented, required church-language review is open, or device/UI/FCM checks are not complete. Record accepted limitations and exact evidence in [`PHASE1_FINAL_REPORT.md`](PHASE1_FINAL_REPORT.md). Phase 2 remains blocked until Phase 1 production/security/E2E gates are closed or the church owner explicitly accepts a documented limited pilot.
