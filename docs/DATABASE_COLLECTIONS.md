# Database Collections and Firestore Indexes

The schema is church-scoped where the record contains `churchId`. Firestore rules and callable Functions enforce tenant boundaries; collection/document IDs are not authorization. Admin SDK writes bypass client rules and must be validated in Functions.

| Path | Purpose | Mutations |
|---|---|---|
| `churches/{churchId}` | Public church identity, locale, currency, registration fees, numbering prefix and setup version | Trusted operator only; clients read, never write |
| `churches/{churchId}/counters/memberNumbers` | Atomic per-church CMF member-number sequence | Approval transaction only; clients denied |
| `users/{uid}` | Firebase UID, church, role, profile, membership and preferred-language fields | Server/admin bootstrap only; owner limited to `fullName`, validated `preferredLanguage`, and `updatedAt` |
| `users/{uid}/devices/{tokenId}` | FCM registration token and platform/update metadata | Token registration/revocation Functions only |
| `registrations/{registrationId}` | Individual/family form, fee snapshot, payment/status/review fields | Callable transaction only; owner/reviewer read by rule |
| `registrationLocks/{uid}` | Active-registration lock prevents duplicate submissions | Callable transaction only; client denied |
| `payments/{paymentId}` | Immutable transaction identifiers/amount/provider/status/timestamps and manual verification | Callable transaction only; owner/finance read by rule |
| `members/{churchId}_{uid}` | Approved membership and unique member number | Approval transaction only; owner/reviewer read by rule |
| `households/{churchId}_{uid}` | Approved family/household relationship | Approval transaction only; primary user/reviewer read by rule |
| `qrVerifications/{sha256Token}` | Server-side hash, member ID, church, status, issue/rotation metadata | Digital-ID/verification Functions only; clients denied |
| `events/{eventId}` | Public published church schedule entries | Trusted content publisher only; published read-only to clients |
| `announcements/{announcementId}` | Public published church updates | Trusted content publisher only; published read-only to clients |
| `notifications/{notificationId}` | Recipient-scoped in-app content and read state | Server-generated; recipient may mark only `readAt` |
| `notifications/{notificationId}/pushDeliveries/{deviceId}` | Per-notification/device retry lease and delivery idempotency state | FCM trigger only; client denied |
| `auditLogs/{auditId}` | Actor/church/action/resource/metadata/server timestamp | Server transaction only; read limited to authorized staff |

The client also uses Storage paths `churches/{churchId}/members/{uid}/profile/{filename}`, `churches/{churchId}/assets/{filename}`, and `churches/{churchId}/documents/{filename}`; see [Security Model](SECURITY_MODEL.md) for access rules and upload limits.

## Composite indexes (`firestore/firestore.indexes.json`)

| Collection group | Fields in order | Query use |
|---|---|---|
| `registrations` | `churchId ASC`, `status ASC`, `createdAt DESC` | Same-church admin work queue by status and newest first |
| `registrations` | `userId ASC`, `createdAt DESC` | Member's applications, newest first |
| `events` | `churchId ASC`, `published ASC`, `startsAt ASC` | Published church calendar, nearest first |
| `announcements` | `churchId ASC`, `published ASC`, `publishedAt DESC` | Published church announcements, newest first |
| `notifications` | `recipientUserId ASC`, `createdAt DESC` | Recipient inbox, newest first |
| `payments` | `registrationId ASC`, `status ASC` | Resolve payment state for a registration |

Deploy the checked-in indexes with `firebase deploy --only firestore:indexes`. Index files support query planning; they do not grant access. If a future query adds a where/order combination, run it against the Emulator/production staging project and deploy any newly suggested index only after reviewing its fields.
