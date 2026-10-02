# Initial Church Administrator Setup

The bootstrap script at [`functions/scripts/bootstrap-admin.mjs`](../functions/scripts/bootstrap-admin.mjs) promotes one verified existing user to `CHURCH_ADMIN`. It requires the explicit `ALLOW_INITIAL_ADMIN_BOOTSTRAP=YES` guard, checks the Auth UID format, requires an existing user profile for the same church, refuses to replace a different existing administrator, and writes a server timestamp. It does not create an account or accept a role from the Android app.

## Owner/operator steps

1. Configure the verified Firebase project and church document, deploy reviewed rules/functions, and create the designated person’s account through the approved Authentication flow. Have the user sign in and complete profile setup so the server-side `users/{uid}` profile exists.
2. Independently verify the **Firebase Auth UID** and the church tenant assignment with church leadership. Do not identify an administrator by display name or email alone. The script grants `CHURCH_ADMIN` for the `CHURCH_ID` you specify; it does not grant `SUPER_ADMIN`.
3. From a trusted operator environment authenticated to the exact owner-approved Firebase project, use Application Default Credentials with least-privilege access. Do not save service-account JSON in this repository or source archive. Confirm the active gcloud/Firebase project before running the script.
4. From the repository's `functions/` directory, set the explicit one-time guard and the two verified values, then run:

```bash
ALLOW_INITIAL_ADMIN_BOOTSTRAP=YES \
CHURCH_ID=<VERIFIED_CHURCH_TENANT_ID> \
ADMIN_UID=<VERIFIED_FIREBASE_AUTH_UID> \
node scripts/bootstrap-admin.mjs
```

5. Verify the resulting profile role and tenant in the owner-approved project, record the approved operator and time, then remove the one-time environment values. Test access as the new administrator and as a normal member before continuing.

The script intentionally refuses to run if another `SUPER_ADMIN` or `CHURCH_ADMIN` already exists for that church. If that happens, stop and use the church's existing privileged-access and audit procedure; do not remove or demote an existing administrator to force the bootstrap through. This setup has **not** been performed in this task because no live Firebase project, verified administrator UID, or operator authorization was supplied.