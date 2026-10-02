# Firebase Production Setup

**Current status: CONFIGURATION REQUIRED.** This source tree has no live Firebase project configuration, `app/google-services.json`, Functions `.env`, verified first-admin UID, service-account credentials, or Firebase deployment authorization. Emulator success is not production validation. Do not copy example values into a real project or run deployment until the church owner verifies the target project and approves the configuration.

## Values the church must provide or verify

- Firebase project ID and project number, with an Android app registered as `org.cmf.churchconnect`.
- The real `google-services.json` for that exact app; keep it local and untracked.
- Enabled Firebase Authentication provider(s), Firestore, Cloud Storage, Cloud Functions, and Firebase Cloud Messaging.
- Church tenant/document ID (`CHURCH_ID`), approved individual/family fees in integer minor units, three-letter currency, and fee version.
- Independently verified Firebase Auth UID for the initial administrator, and the approved church role/tenant assignment.
- A trusted deployment identity with only the required project permissions, plus approved billing/Functions prerequisites for the selected Firebase plan and region.

## Provision and verify

1. Create or select the church-owned Firebase project. Confirm its project ID and number in the Firebase Console before entering any deploy command.
2. Register the Android app with package `org.cmf.churchconnect`; download the project-specific `google-services.json` to `app/google-services.json`. Never substitute `app/google-services.example.json` or commit/archive the real file.
3. Enable the intended Authentication provider, Firestore, Storage, Functions, and FCM. Functions use `asia-southeast1`. Configure Android Play Integrity for App Check separately; see [App Check Setup](APP_CHECK_SETUP.md).
4. Choose the church tenant ID and have church leadership approve the fee document. Start from [`firestore/church-config.example.json`](../firestore/church-config.example.json) for shape only; sample fees are not approval. Create `churches/{CHURCH_ID}` with `registrationFees.individual`, `family`, `currency`, and `version`, plus the required church identity, locale, and numbering-prefix fields.
5. Set Android with `-PCHURCH_ID=<verified-tenant-id>` and Functions with a local `functions/.env` copied from `.env.example`. Confirm both tenant IDs match the Firestore document. Keep `.env` out of Git.
6. Review Functions, Firestore rules/indexes, and Storage rules in a non-production project first. Run the regression suites below against the local Emulator Suite before requesting production deployment approval.
7. Create the first user through the intended Auth flow. Independently verify the UID and church assignment, then follow [Initial Admin Setup](INITIAL_ADMIN_SETUP.md) to use the guarded bootstrap script from a trusted operator environment. Never grant admin roles from the Android app.

## Local validation (does not contact production)

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
(cd functions && npm test && npm run build && npm run test:integration)
(cd security-tests && npm test)
```

The Emulator Suite intentionally uses `demo-church-connect`. Emulator tests do not prove production IAM, App Check enforcement, FCM delivery, rules deployment, or the correctness of live church configuration.

## Controlled deployment

Only after the owner reviews the exact project ID, tenant ID, configuration, and deployment identity, deploy with an explicit project ID:

```bash
firebase --project <VERIFIED_FIREBASE_PROJECT_ID> deploy --only functions,firestore:rules,firestore:indexes,storage
```

Then confirm deployment timestamps and callable region, rules, indexes, App Check service enforcement, initial-admin role, and a controlled end-to-end test account. Keep dev/emulator and production project IDs/configurations separate. See [Deployment Guide](DEPLOYMENT_GUIDE.md) for go/no-go ordering.
