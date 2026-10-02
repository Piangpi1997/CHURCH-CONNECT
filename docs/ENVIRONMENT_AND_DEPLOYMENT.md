# Environment and Deployment Guide

## Configuration map

| Value | Location | Default / handling |
|---|---|---|
| Android Firebase options | `app/google-services.json` | Required for live Firebase. Keep local and untracked; never commit. Without it the app selects local preview mode. |
| Android church tenant | Gradle property `CHURCH_ID` | Defaults to `cmf-setapak`; override with `-PCHURCH_ID=...`. Must match Functions and `churches/{churchId}`. |
| Android emulator routing | Gradle property `USE_FIREBASE_EMULATORS` | Defaults to `false`. Set `-PUSE_FIREBASE_EMULATORS=true` only for a development/emulator build; never use it in a release. |
| Functions church tenant | `functions/.env` | Copy `functions/.env.example` and set `CHURCH_ID` to the same tenant ID. `.env` is ignored by Git. |
| Firebase CLI project | explicit `--project` on each operation | No project alias is configured. Always inspect the ID and pass the verified non-production/production project explicitly. |
| Admin bootstrap identity | `ADMIN_UID` environment variable | Must be a separately verified Firebase Auth UID; grant only through the guarded trusted bootstrap script. |
| Admin bootstrap guard | `ALLOW_INITIAL_ADMIN_BOOTSTRAP=YES` | Required in addition to the verified UID and `CHURCH_ID`; script refuses to overwrite an existing admin. |

Never put Firebase service-account keys, API secrets, signing keys, push tokens, bank evidence, or user credentials in the source tree, build scripts, screenshots, issue text, or source ZIP.

## Local development

Install Node dependencies separately for Functions and Emulator rules tests:

```bash
(cd functions && npm ci)
(cd security-tests && npm ci)
```

For local client-to-emulator development, first register a debug Firebase app and use its local-only `google-services.json`/App Check debug token. The Android Emulator reaches the host through `10.0.2.2`; physical devices need the host's reachable LAN address and local firewall rules. Build debug with:

```bash
./gradlew :app:assembleDebug -PCHURCH_ID=cmf-setapak -PUSE_FIREBASE_EMULATORS=true
```

Cloud Functions Emulator tests start Auth/Firestore/Functions using demo project ID `demo-church-connect`. Firestore/Storage rules tests start a separate Auth-free Firestore/Storage suite. They use ephemeral local test data and do not contact the production project.

## Deployment sequence

1. Owner verifies Firebase project ID, tenant ID, service region, app registration, Auth providers, Firebase fee configuration, and initial admin UID.
2. Create and protect the non-committed local `app/google-services.json` and `functions/.env`. Verify `functions/.env` `CHURCH_ID` matches the Android build property and `churches/{churchId}` document.
3. Run backend tests/build, Android tests/debug build, Functions Emulator integration, Firestore/Storage rules suites, and a manual security review.
4. Configure Play Integrity App Check and the debug token in the correct Firebase projects. Enable App Check enforcement per service only after testing valid client attestation.
5. Deploy reviewed Functions, Firestore rules/indexes and Storage rules to the explicit verified project:

```bash
firebase --project YOUR_VERIFIED_PROJECT_ID deploy --only functions,firestore:rules,firestore:indexes,storage
```

6. Verify deployed callable names, region, rule timestamps, indexes, role bootstrap, public content, notification permission/token refresh, and audit monitoring. Re-test cross-church denial using non-production accounts.
7. Produce a signed release only in the approved secure signing environment. Inspect the final manifest/package/version and test an actual signed candidate before distribution.

This task performed **no live deployment**: the workspace has no configured project alias, production Firebase config, service account, verified admin UID, App Check console access, or signing key.
