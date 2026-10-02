# Church Connect — CMF Church

Native Android foundation for Christ Mission Fellowship Church, Setapak, Kuala Lumpur. The project preserves the source brief’s Android + Firebase target: Jetpack Compose app, Firebase Authentication and Firestore, trusted TypeScript Cloud Functions, deny-by-default Firestore/Storage rules, and an Android-visible member/admin workflow.

## Run the app

Requirements: Android Studio or Android SDK 35, JDK 17 or later, and internet access for the first Gradle dependency download.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android device/emulator, or use Android Studio to open this folder. If `app/google-services.json` is absent, the app starts in **local demo mode**. Use **Member demo** to create a sample application, then sign out and use **Admin demo** to simulate finance verification and review. Switch back to Member demo to view its result and digital ID. Demo changes are in-memory only, clearly labeled, and never connect to church records or move money.

### Connect a Firebase project

1. Create/configure a Firebase project and register Android package `org.cmf.churchconnect`. Download its real `google-services.json` into `app/google-services.json`; do not commit that file.
2. Enable Email/Password Authentication, Firestore, Cloud Functions, Cloud Storage, and Firebase Cloud Messaging. Configure App Check before production release.
3. Create `churches/cmf-setapak` in Firestore using the shape in `firestore/church-config.example.json`. Fee values are integer minor units (MYR 2000 = RM20.00; 5000 = RM50.00); change them in this document, not in the app. Existing applications keep the fee and configuration version captured at creation.
4. Set `CHURCH_ID` in `functions/.env` to the same church document ID. Build and deploy Functions and rules from the project root:

```bash
cd functions && npm install && npm run build && cd ..
firebase --project YOUR_FIREBASE_PROJECT_ID deploy --only functions,firestore:rules,firestore:indexes,storage
```

5. Create the first user account in the app. After verifying the intended person's Firebase Auth UID, bootstrap the initial church administrator from a trusted machine with the correct Application Default Credentials. The script refuses to run without an explicit environment flag and refuses to replace another existing administrator:

```bash
cd functions
ALLOW_INITIAL_ADMIN_BOOTSTRAP=YES CHURCH_ID=cmf-setapak ADMIN_UID=THE_VERIFIED_AUTH_UID node scripts/bootstrap-admin.mjs
```

Do not grant administrative roles by letting a client write its profile. Function role checks read the trusted `users/{uid}` record, and the client rules prohibit changes to role, church, member number, registration, payment, QR and audit data.

### Firebase Emulator

For a local emulator only, copy `app/google-services.example.json` to `app/google-services.json`, install the Firebase CLI, and from the project root run `firebase emulators:start --project demo-church-connect`. Build/run with emulator routing enabled via `-PUSE_FIREBASE_EMULATORS=true` and use the Android Emulator (`10.0.2.2` is configured as its host). For example, run `./gradlew :app:installDebug -PUSE_FIREBASE_EMULATORS=true`. The example JSON/API key is intentionally fake and must never be used for a real Firebase deployment.

## Phase 1 behaviour

- Email/password account creation and sign-in with password reset; profile creation is server-controlled and new profiles always start as `MEMBER`.
- Individual/family applications, consent and required-field checks, server-calculated configurable fee snapshot, duplicate-active-application prevention, and enforced status transitions.
- Separate payment records. **Online payment is not integrated.** The included manual-offline flow accepts a transfer reference but never marks it paid from the client. A Finance Admin or church administrator must verify the received funds on the backend; the action records the method and audit trail. Only then can the applicant submit.
- Admin queue with same-church, trusted-role checks; approve/reject with reason; atomic, server-generated unique member number; in-app status notifications.
- Digital ID is created only after approval. QR payload is an opaque random token; only its hash is stored. Refreshing an ID replaces and invalidates its previous token. Verification requires authorized church staff and returns only minimal membership details.
- Church calendar and published announcements; private notification inbox; Cloud Functions include an FCM delivery trigger for registered device tokens.
- English and Burmese app-name resources are included, but most Compose screen text is currently English. Full Burmese and Tedim interface localization still needs completion and church-language review.

## Checks

```bash
# Android compile and unit tests
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Pure backend tests and strict TypeScript compile
cd functions && npm install && npm test && npm run build

# Firestore rules integration tests (requires Java and Firebase Emulator downloads)
cd security-tests && npm install && npm test
```

Security rules are in `firestore/firestore.rules` and `firestore/storage.rules`; composite indexes are in `firestore/firestore.indexes.json`. The rules emulator tests exercise self-approval, forged payment state, role escalation, cross-member and cross-church private reads, forged admin writes, audit tampering and notification ownership.

## Deployment and readiness boundary

This package contains no Firebase project ID/config, production credentials, verified admin UID, deployed backend, App Check attestation, or payment-provider contract. Those must be supplied/configured by the church owner before a connected production rollout. No deploy, privileged account change, live payment, or production submission was performed here. **Payment provider status: NOT INTEGRATED.** The app can be previewed without Firebase, but that preview is not a production backend.
