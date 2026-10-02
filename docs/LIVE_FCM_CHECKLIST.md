# Live FCM Test Checklist

**Current status: NOT RUN — DEVICE/PRODUCTION ACCESS REQUIRED.** FCM code paths exist, including token registration/refresh and server-side delivery state, but this task had no live Firebase project, registered device token, or attached Android device. Emulator/unit success is not proof of a real FCM delivery.

Use a dedicated non-production Firebase project, approved test accounts, an App Check-valid signed/debug test build for that project, and actual Android devices. Record project ID, app version, device/Android version, permission state, token lifecycle event, notification ID, send result, and observed delivery; never put the token or service credential in a public report.

- [ ] Grant notification permission on Android 13+; confirm the user inbox and system notification appear once for an authorized test notification.
- [ ] Deny permission; verify no false “delivered” claim, no crash, and in-app inbox remains available.
- [ ] Sign in, register a token, force token refresh, and confirm the server replaces/updates the correct user/device record.
- [ ] Sign out, then sign in as a different test user on the same device; verify token ownership moves safely and the former user cannot receive private content.
- [ ] Send approved test notifications while app is foregrounded, backgrounded, and process-stopped; verify channel label, title/body fallback, tap behavior, and locale.
- [ ] Simulate transient delivery failure and retry; confirm server delivery ledger progresses and stable notification ID/tag prevents multiple tray entries.
- [ ] Replay the same notification/callback; confirm duplicate handling is idempotent. Verify a second distinct notification still appears.
- [ ] Test offline/reconnect and expired/revoked auth; ensure unauthorized or stale tokens cannot access private inbox data.
- [ ] Inspect Firebase Console delivery metrics and server audit/delivery state after each test; confirm selected project and App Check enforcement.

Do not place production tokens, service-account JSON, or provider secrets in Git, APKs, logs, screenshots, or test documentation.