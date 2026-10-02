# Firebase App Check Setup

**Current status: CONFIGURED-NOT-ENFORCED.** `MainActivity` installs the debug provider only in debug builds and the Play Integrity provider in release builds. Callable Functions enforce App Check outside the Functions Emulator. No live project or Firebase Console access was supplied, so provider registration and service enforcement cannot be verified here. Firestore and Storage enforcement are separate Console settings.

## Configure the development project first

1. Register the exact Android package `org.cmf.churchconnect` in the development Firebase project and confirm that the installed app uses that project's `google-services.json`.
2. Register the Android app in Firebase App Check. Configure Play Integrity for the release candidate in the appropriate project and link the Google Play app as required by the church's distribution setup.
3. Install a local **debug** build, obtain its Firebase App Check debug token from the local development run, and register that token only in the development project's App Check console. Do not commit, paste into an issue, or add it to a production project.
4. Exercise Auth, Functions, Firestore, and Storage against the development project with valid attestation before enabling any enforcement.

## Enable enforcement deliberately

In the Firebase Console App Check pages, verify the app registration and valid request metrics, then enable enforcement one service at a time for the services the app uses (Cloud Functions, Firestore, and Storage). Monitor rejected requests after each change and keep a rollback owner/contact available. Functions Emulator bypass is intentional and does not indicate a production pass.

## Release verification

- Confirm the release APK uses the production Firebase project config and release provider (Play Integrity), not the debug provider.
- Confirm no debug token is stored in source, CI variables intended for production, release assets, or the production App Check console.
- Confirm valid release attestations reach each service and that invalid/unregistered clients are rejected after enforcement.
- Record the Console enforcement state and timestamp for each service. Code initialization alone is not enforcement evidence.

Until these checks are performed in the church's Firebase project, report **CONFIGURED-NOT-ENFORCED**, not ENFORCED.