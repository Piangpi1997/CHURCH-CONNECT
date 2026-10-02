# Release Signing Guide

**Current status: SIGNING KEY REQUIRED.** The optimized release APK can be built unsigned for validation, but is not a distributable church release. No production/upload keystore or signing credentials were supplied. Do not generate a substitute key, commit a key, or put signing secrets in a source ZIP.

The Gradle release configuration accepts four environment values when the owner has provisioned an approved keystore outside the repository:

- `CHURCH_RELEASE_STORE_FILE` — absolute path to the owner-controlled keystore.
- `CHURCH_RELEASE_STORE_PASSWORD` — protected secret.
- `CHURCH_RELEASE_KEY_ALIAS` — alias for the approved upload key.
- `CHURCH_RELEASE_KEY_PASSWORD` — protected secret.

If none are set, `assembleRelease` remains unsigned. If only some are set, Gradle fails instead of silently producing a release under an incomplete configuration. CI should inject these values from its protected secret store and make the keystore available as a protected file; never echo secrets or upload the keystore as a normal build artifact.

After the authorized owner configures those values in a secure local/CI environment:

```bash
./gradlew :app:clean :app:assembleRelease :app:lintRelease
```

Verify the APK signature with Android `apksigner verify --verbose --print-certs`, confirm the signing certificate fingerprint through the independent owner record, and inspect package `org.cmf.churchconnect`, version `1.0.1` / versionCode `2`, release manifest, Play Integrity App Check provider, and R8 mapping. Test the signed candidate on supported devices before distribution. The app label remains **CMF CHURCH APP** and existing premium launcher art is unchanged.

A signed build is only one release gate: production Firebase provisioning, App Check enforcement, payment policy, localization review, and device/FCM checks must also be complete or explicitly accepted by the church.