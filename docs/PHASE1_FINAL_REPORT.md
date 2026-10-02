# Phase 1 Final Readiness Report

## 1. Firebase Production

**CONFIGURATION REQUIRED.** No owner Firebase project/configuration, live `google-services.json`, deployment IAM, verified initial-admin UID, or approved live church fee document was available. Emulator work stayed on `demo-church-connect`; nothing was deployed to production.

## 2. App Check

**CONFIGURED-NOT-ENFORCED.** Debug builds select Firebase's debug provider; release builds select Play Integrity. Callable Functions enforce App Check outside the Emulator, but live app registration, valid Play Integrity traffic, and Firebase Console enforcement for Functions/Firestore/Storage were not available to verify.

## 3. Online Payment

**NOT INTEGRATED.** No provider, contract, checkout, callback endpoint, webhook signature verification, automatic reconciliation, refund, or chargeback workflow is configured. A provider-neutral `PaymentProvider`/`PaymentService` extension seam now rejects checkout and webhooks when no provider is configured; it is not a gateway. The existing **manual offline transfer + trusted finance verification: PASS** in Functions Emulator tests. A submitted reference remains `PROCESSING`, never `PAID` by itself.

## 4. Signed Release

**SIGNING KEY REQUIRED.** Debug APK signature verified. The version `1.0.1` / versionCode `2` optimized release APK builds and passes R8/release lint but is intentionally unsigned without the church's key. Gradle accepts signing data only through the documented environment-variable path and rejects partial signing configuration.

## 5. Localization

**PARTIAL.** English app-owned UI resources are complete and cover the resource-backed Compose copy. English and Myanmar catalogs have matching 204 resource keys and matching formatting placeholders. Myanmar strings are present but still need church/native review and actual-device display/accessibility review. Tedim (`ctd`) is **CHURCH LANGUAGE REVIEW REQUIRED**: its complete catalog deliberately duplicates English as explicit fallback—not a claimed translation—and the selector labels it accordingly. Language selection persists, follows System default when chosen, and synchronizes with Android 13+ per-app language settings.

## 6. Android UI/Device E2E

**NOT RUN — DEVICE/PRODUCTION ACCESS REQUIRED.** No Android device or emulator was attached. Individual, family, failure/recovery, and QR test plans are documented; emulator/server tests do not count as screen/device E2E.

## 7. Live FCM

**NOT RUN.** No live Firebase project or real Android FCM token/device was available. Permission, token refresh, retries, deduplication, and foreground/background delivery require the [live FCM checklist](LIVE_FCM_CHECKLIST.md).

## 8. Regression Tests

- **Passed: 32** — 10 Android JVM tests; 7 Functions unit tests; 2 Functions Emulator flows; 11 Firestore rules tests; 2 Storage rules tests.
- **Failed: 0.**
- **Skipped: 0 automated cases.** Device/UI E2E, live FCM/App Check, production Firebase, and payment-gateway verification were not run and are listed separately, not counted as passing tests.
- Final Android `testDebugUnitTest`, debug/release assembly, strict Functions TypeScript build, Functions Emulator tests, Firestore/Storage Emulator tests, and `lintRelease` all completed successfully.

## 9. Security

**PASS (local/emulator evidence).** Rules and backend emulator tests deny self-approval, forged `PAID`, role escalation, cross-member/cross-church private access, unauthorized content and Storage writes, QR/lock access, and audit-log tampering. This does not prove production Firebase configuration, IAM, App Check, or live data isolation.

## 10. Remaining Manual Actions

1. The church owner must select/verify the Firebase project ID and project number, register package `org.cmf.churchconnect`, provide the matching `google-services.json` through the approved local/CI path, choose Auth providers, configure Firestore/Storage/Functions/FCM, review indexes and rules, and confirm the exact tenant ID and approved fee/currency/version document.
2. A trusted operator must verify the initial administrator's Auth UID and church assignment, then follow [Initial Admin Setup](INITIAL_ADMIN_SETUP.md) from an owner-controlled environment using least-privilege Application Default Credentials. Deployment requires an explicit verified project ID and permission to deploy reviewed Functions/rules/indexes/Storage.
3. Firebase owners must register Play Integrity for the real Android app, use debug tokens only in a development project, validate requests, then deliberately enable and record App Check enforcement for each intended service.
4. Finance/legal leadership must either accept manual-offline payments as the launch policy or choose and contract a payment provider, provide sandbox/production configuration through secure server-side secrets, and approve transaction/refund procedures. Provider implementation and validation are still required before any online-payment claim.
5. The church must supply and verify its release/upload keystore and credentials through the protected environment documented in [Release Signing](RELEASE_SIGNING.md); then verify the signed certificate and distribute only the verified artifact.
6. A church-approved Myanmar reviewer should review terminology and small-screen/accessibility behavior; a trusted Tedim reviewer must supply/approve Tedim text before that locale can be called translated.
7. Provide an Android device/emulator, approved non-production Firebase project/accounts, and real test FCM tokens to complete the documented individual/family/recovery E2E and live delivery checks.

## 11. Files Changed

Android work: `app/build.gradle.kts` (environment-only release signing); `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/locales_config.xml`, and `AppLocale.kt` (Android/system locale support and persistence); `MainActivity.kt`, `ChurchConnectApp.kt`, `ChurchViewModel.kt`, and `ChurchMessagingService.kt` (localized screens, messages, and notification fallback); English/Myanmar/Tedim resources; and `AppLocaleTest.kt`.

Payment boundary and tests: `functions/src/payments/payment-provider.ts`, `functions/test/payment-provider.test.ts`, and a clarifying comment in `functions/src/index.ts`. Documentation: `README.md`, `docs/BUILD_STATUS.md`, `docs/TEST_AND_RELEASE.md`, this report, and the new Firebase production, initial-admin, App Check, payment, signing, localization, Android E2E, live FCM, and deployment guides.

## 12. Remaining Blockers

**Critical:** real Firebase configuration/deployment/IAM/admin; App Check Console/Play Integrity enforcement; a church-owned production signing key; device/UI and live FCM acceptance before broad release. **High:** online payment remains unavailable if the church requires a gateway rather than the current manual-offline method. **Medium:** native Myanmar review and a trusted Tedim translation/review.

## 13. Production Readiness

**NOT READY.** The app-owned localization/resources, Android language preference path, provider boundary, docs, emulator security suites, and unsigned build/lint are in place. Production Firebase, service enforcement, signing, church-language review, and live device/FCM validation remain open; no emulator result is represented as a production pass.

## 14. Phase 2 Gate

**BLOCKED.** Keep Phase 2 closed until the church owner accepts a documented payment policy and closes or explicitly approves a limited pilot for the outstanding Firebase, App Check, signing, and device/security gates.
