# Firebase Setup and Operations

## Required project configuration

1. Create the church's Firebase project and register the Android package `org.cmf.churchconnect`. Keep the real `app/google-services.json` local; it is ignored by Git and must not be placed in a source archive.
2. Enable Email/Password Authentication, Cloud Firestore, Cloud Functions, Cloud Storage, and Firebase Cloud Messaging. Deploy in the Functions region `asia-southeast1`.
3. Create `churches/{churchId}` using [`firestore/church-config.example.json`](../firestore/church-config.example.json) as a shape-only example. Set `registrationFees.individual`, `registrationFees.family`, and `registrationFees.version` as non-negative integer minor units and set a three-letter currency code. The sample means MYR 2,000 = RM20.00 and MYR 5,000 = RM50.00. Bump the configuration version when fees change. Existing payment records keep their original amount, currency, fee type, and version.
4. Set the same church document ID in the Android build property and Functions environment. Android uses `-PCHURCH_ID=...` (default `cmf-setapak`); Functions use `functions/.env` or the deployment environment variable `CHURCH_ID` (see `functions/.env.example`). Missing/invalid church fee configuration now fails closed instead of silently falling back to sample fees.
5. Install dependencies, compile, and deploy only after confirming the target project and reviewing the exact configuration:

```bash
cd functions && npm ci && npm test && npm run build && cd ..
firebase --project YOUR_VERIFIED_FIREBASE_PROJECT_ID deploy --only functions,firestore:rules,firestore:indexes,storage
```

No live project was configured or deployed during this work.

## Authentication and first administrator

Create the first account through the app. Verify the Firebase Auth UID independently, then grant the initial admin from a trusted machine using Application Default Credentials. The bootstrap tool refuses to run without the explicit flag and will not replace an existing admin:

```bash
cd functions
ALLOW_INITIAL_ADMIN_BOOTSTRAP=YES CHURCH_ID=cmf-setapak ADMIN_UID=THE_VERIFIED_AUTH_UID node scripts/bootstrap-admin.mjs
```

Do not set roles from an Android client. Callable Functions read the trusted `users/{uid}` profile and validate the role and church. Client Firestore rules deny writes to profiles at creation/deletion and allow only limited self-edits.

## App Check

The Android release source installs the Play Integrity App Check provider. Debug builds use Firebase's debug provider; register its debug token only in the development Firebase project's App Check console. Callable Functions enforce App Check in deployed environments; the Functions Emulator intentionally bypasses attestation because it cannot produce production tokens. Firestore and Storage enforcement are separate Firebase Console settings and were not enabled or verified against a live project. Until the project is provisioned, register the app, configure Play Integrity, register the development token, and enable App Check enforcement for each production Firebase service, report status as **CONFIGURED-NOT-ENFORCED**.

## Manual payment boundary

The current provider is `MANUAL_OFFLINE`. A member can submit a transfer reference, but that action only changes the record to `PROCESSING`; it never marks the payment as paid. An authorized Finance Admin, Pastor, or Church Admin must compare the submitted reference to bank evidence and use the server verification action. The server checks registration/user/church/amount/currency/fee type, creates an audit record, and only then permits submission and review.

There is no payment gateway, webhook/callback signature verification, provider transaction ID, online checkout, automatic settlement, or refund flow. Those are **NOT INTEGRATED**, not simulated as a live provider. Do not treat a client assertion or reference string as proof of payment.

## Admin workflow and membership IDs

Membership review roles are `SUPER_ADMIN`, `PASTOR`, and `CHURCH_ADMIN`. Finance roles additionally include `FINANCE_ADMIN`; Ushers may verify QR tokens. Finance queue responses omit applicant phone/address. Approval and member-number assignment occur in one Firestore transaction with a church-scoped counter and immutable member record. Rejection requires a reason and does not issue a member number or household record.

The first verified Firebase UID must be provisioned before church staff can use these workflows. Admin dashboard search/filter/detail and live operational procedures still need church-user validation.

## Notifications and content

FCM device tokens are stored server-side under each user's device subcollection. The Android messaging service re-registers refreshed tokens for a signed-in user. Notification-trigger delivery keeps per-notification/per-device delivery state and uses a stable Android notification tag so retries do not produce separate tray entries. Actual FCM delivery still requires valid project credentials, a registered token, notification permission, and device testing.

Published events and announcements are read from Firestore. Client writes are denied; no staff publishing/targeting UI or provider-specific FCM targeting workflow is included. Seed only verified church content using trusted administrative tooling.
