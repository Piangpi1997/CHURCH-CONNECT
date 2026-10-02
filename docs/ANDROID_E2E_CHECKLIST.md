# Android UI and Device E2E Checklist

**Current status: NOT RUN — DEVICE/PRODUCTION ACCESS REQUIRED.** No Android emulator/device is attached in this environment. Do not mark any UI journey PASS from unit tests or Firebase Emulator results. Configure a dedicated non-production Firebase project and church-approved test accounts before exercising live-backed paths.

## Individual member path

- [ ] Install the candidate APK on supported Android versions; verify `CMF CHURCH APP`, package, launch icon, locale chooser, and system-default behavior.
- [ ] Create an account, sign in, open profile/home, and verify auth errors, password reset, session restoration, sign-out and expiry.
- [ ] Start Individual Registration; validate required name, phone, address, consent, and configured fee snapshot (confirm the actual church fee first).
- [ ] Submit a manual transfer reference. Confirm UI says it is awaiting verification and never displays the payment as paid before finance action.
- [ ] As a separate authorized finance test user, verify only matching bank evidence; confirm audit entry and applicant state. Reject mismatched, duplicate, wrong-user, wrong-church, and wrong-amount attempts.
- [ ] Submit the application; review it as an authorized staff user; test approve and reject-with-reason, with member self-approval denied.
- [ ] After approval, confirm unique CMF member number, digital member ID, opaque/secure QR, and server QR verification without exposing private profile fields.
- [ ] Confirm notification inbox, Home, Calendar, Announcement, and QR verification states with approved test data.

## Family path

- [ ] Create Family Registration; verify required household member list and configured family fee snapshot.
- [ ] Complete reference submission and finance verification with the same separation of duties.
- [ ] Submit, review, and approve; confirm household link and unique member-number allocation for all expected members.
- [ ] Confirm digital ID, QR, notification, and account behavior for each eligible family profile.

## Failure and recovery

- [ ] Deny notification permission; continue using membership features without a crash or false delivery state.
- [ ] Submit invalid/incomplete form and consent; verify inline error and no record creation.
- [ ] Exercise available payment cancel/failure and mismatched/duplicate reference behaviors without an online gateway.
- [ ] Disconnect network during reads/submission, reconnect, refresh, and confirm no duplicate registration or false success.
- [ ] Expire or revoke a session during a workflow; verify reauthentication and ownership checks.
- [ ] Scan malformed, unknown, revoked, and valid QR tokens; verify only minimal allowed status/name/number is returned.
- [ ] For FCM, test token refresh, permission denial, notification retry and duplicate callback/device delivery (see [Live FCM Checklist](LIVE_FCM_CHECKLIST.md)).
- [ ] Repeat critical authorization attacks: member self-approval, forged PAID, role escalation, cross-member/cross-church reads, audit writes, QR/lock reads, and unauthorized Storage writes.

Record device model, Android version, APK hash, Firebase project/tenant (never credentials), account role, steps, expected/actual result, and issue reference. Use only non-production test identities until the church authorizes a controlled production pilot.