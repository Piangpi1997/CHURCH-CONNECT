# Admin and Payment Setup

## Establish trusted administrators

1. Create the user's Authentication account through the app or Firebase Console.
2. Verify the UID and church assignment through the organization's approved identity process.
3. Grant the first `SUPER_ADMIN` only via the guarded bootstrap tool in [Firebase Setup](FIREBASE_SETUP.md). Subsequent role changes must be made by trusted server-side administration—not through Android client writes.
4. Assign least privilege:
   - `SUPER_ADMIN`: global audit visibility and operational access.
   - `PASTOR` / `CHURCH_ADMIN`: same-church membership review and approval.
   - `FINANCE_ADMIN`: same-church payment review and payment queue access, without applicant phone/address in finance queue responses.
   - `USHER`: same-church QR/member verification, not membership approval.
5. Test a member, reviewer, finance user, usher, and other-church admin separately before launch. Never rely on hiding a button as the authorization boundary.

The Compose Admin workspace now supports refresh, text search (name, application ID, phone when authorized, family name, transfer reference, status), active Payment/Submitted/In-review filters, expandable application details, QR token verification, finance verification, approval, and rejection with a required reason. The backend still rechecks every role, church, state, amount and reference. Local queue filtering is covered by Kotlin tests; live staff acceptance/device testing remains outstanding.

## Fee and registration policy

Configure `churches/{churchId}.registrationFees` in Firestore with `individual`, `family`, `currency`, and `version`. The values are integer minor units: the supplied example is `2000` = RM20.00 individual and `5000` = RM50.00 family. These are initial example fees, not a signed financial approval. Church leadership must confirm the actual fees before opening registrations. Every payment stores a fee snapshot, so later fee changes do not modify historical amounts.

Server-controlled status path:

`DRAFT → PENDING_PAYMENT → PAYMENT_VERIFIED → SUBMITTED → UNDER_REVIEW → APPROVED | REJECTED`

Approval requires verified payment. Rejection requires a reason and does not allocate a member number or create a household membership record. Approval atomically assigns the church-scoped sequence and creates member/household records.

## Payment procedure and boundary

**Production payment status: NOT INTEGRATED.** The app supports a manual offline transfer reference only:

1. Applicant submits a transfer/reference string; the record becomes `PROCESSING`, not `PAID`.
2. Authorized Finance Admin/Pastor/Church Admin independently checks the church bank receipt/ledger outside the app.
3. Finance verifies the exact server-stored registration, owner, church, configured amount, currency, fee type, and submitted reference through the backend.
4. The server records the verification time and audit event. Only then may the applicant submit for church review.

The app has no online checkout, gateway credentials, webhook signature verification, provider transaction IDs, automated callback replay protection, reconciliation, or refunds. Do not claim the offline reference is an online payment, do not skip the bank evidence check, and do not mark the payment verified on a client's behalf.

If online payment is later required, choose the provider and legal/finance owner first. Implement provider signature/transaction/amount/currency/registration/user/church/idempotency/replay/server-time/audit checks, test the provider sandbox, document refunds/chargebacks, and rerun the full critical path before changing the status to `TEST/DEVELOPMENT ONLY` or `LIVE`.
