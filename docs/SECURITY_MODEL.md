# Security Model and Data Permissions

## Trust boundaries

Firebase Authentication identifies the actor, but authorization comes from the trusted `users/{uid}` record read by Cloud Functions and Firestore/Storage rules. The client never grants itself a role, church assignment, member number, payment state, approval, QR credential, or audit entry. Cloud Functions use the Admin SDK and therefore bypass Firebase client rules; each privileged handler must keep its explicit role, church, ownership, state-transition, and integrity checks.

Callable Functions require App Check in deployed environments. The emulator bypass exists only for local tests. Real Firebase App Check enforcement has not been enabled/verified in a project for this task.

## Role boundaries

| Action | Allowed roles | Scope |
|---|---|---|
| Read/review full membership applications; start review; approve/reject | `SUPER_ADMIN`, `PASTOR`, `CHURCH_ADMIN` | Same church only |
| Verify manual payment reference | `SUPER_ADMIN`, `PASTOR`, `CHURCH_ADMIN`, `FINANCE_ADMIN` | Same church only |
| Read payment queue | Finance roles above | Same church; private contact/address fields are omitted |
| Verify active member QR | Finance roles plus `USHER` | Same church as the QR/member |
| Read audit logs | `SUPER_ADMIN`, or membership staff in the record's church | `SUPER_ADMIN` is global; other staff are same-church |
| Modify profile role/church/member number, registration, payment, membership, QR, or audit state | No client role | Server-only |

## Firestore collection access

| Collection | Client reads | Client writes |
|---|---|---|
| `churches/{churchId}` and `counters` | Church configuration is public-readable; counters denied | Denied |
| `users/{uid}` | Owner; same-church membership staff may read member profiles | Owner may edit only `fullName`, `preferredLanguage` (`en`, `my`, `ctd`; legacy `ted` accepted), and `updatedAt`; role/church/member-number changes denied |
| `members/{memberId}` | Owner or same-church membership staff | Denied |
| `households/{householdId}` | Primary user or same-church membership staff | Denied |
| `registrations/{id}` | Owner or same-church membership staff | Denied; callable Functions own transitions |
| `payments/{id}` | Owner or same-church finance staff | Denied; callable Functions own state |
| `events`, `announcements` | Published records are public-readable | Denied; no client publish path |
| `notifications/{id}` | Matching recipient only | Recipient may update only `readAt` to server request time |
| `auditLogs/{id}` | Same-church membership staff or `SUPER_ADMIN` | Denied |
| `qrVerifications`, `registrationLocks`, `users/{uid}/devices`, push-delivery records | Denied | Denied |
| Any other document | Denied by catch-all | Denied by catch-all |

Rules intentionally prevent direct Firestore client writes even for administrators; privileged actions must use validated callables. Published events and announcements are readable to every user (including signed-out clients) by design; they must contain only public church content.

## Storage

- Member profile images: owner can upload/update/delete their own image in their own church path; same-church staff can read. Content types are JPEG/PNG/WebP and size must be under 5 MiB.
- Church assets: same-church signed-in users can read; trusted same-church staff can write/delete valid image files under 5 MiB.
- Church documents: trusted same-church staff only, PDF/JPEG/PNG/WebP under 20 MiB.
- Cross-church access, public profile files, unrecognized paths, and other upload types are denied.

## Protected backend invariants

- Registration fees are loaded from required church configuration and snapshotted at creation; missing fee configuration fails closed.
- Payment transaction records include transaction/registration/user/church IDs, amount, currency, fee type, provider, provider transaction ID, status, creation/update timestamps, and verification timestamp. Manual transfers remain `PENDING`/`PROCESSING` until Finance verifies the matching record.
- State transitions are backend-enforced: `PENDING_PAYMENT` → `PAYMENT_VERIFIED` → `SUBMITTED` → `UNDER_REVIEW` → `APPROVED` or `REJECTED`.
- Duplicate active applications, payment-reference submission, verification, submission, and review transitions are rejected. Approval assigns a church-scoped sequence number and member record atomically.
- QR payloads contain only a random opaque token. Firestore stores its SHA-256 hash; refresh deletes the previous verification token. Verification requires staff authorization and returns only member name/number.
- Notification writes and audit events are transactional with critical changes. FCM push delivery has an idempotency ledger per notification/device, a retry lease, and a stable OS notification tag.

## Indexes

The supplied `firestore/firestore.indexes.json` defines composite indexes for registration queues, member application history, published events, published announcements, notification inboxes, and payments by registration/status. Deploy indexes together with rules and functions. Firebase may request additional indexes if future query filters/orderings are changed.

## Validation boundary

The local Emulator Suite tests cover denied self-approval, forged paid state, role escalation, other-member/cross-church private reads, protected collection writes, audit tampering, QR/lock opacity, event/announcement authoring, notification ownership, and Storage path/type checks. Emulator checks do not replace a production project rules review, App Check console enforcement, IAM review, or independent penetration test.
