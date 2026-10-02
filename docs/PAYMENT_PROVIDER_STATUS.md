# Payment Status and Provider Integration

**Online payment status: NOT INTEGRATED.** The only supported method is `MANUAL_OFFLINE`: an applicant submits a transfer reference and an authorized Finance Admin, Pastor, or Church Admin checks the church's bank evidence and verifies the exact record. Reference submission sets `PROCESSING`; it does not mean paid. The server records `PAID` only after authorized manual verification and writes an audit record. Preserve this boundary unless a real provider is selected and approved.

There is no online checkout, provider contract, gateway SDK, callback endpoint, webhook signature verification, provider transaction ID, automatic reconciliation, chargeback or refund workflow. A provider-neutral `PaymentProvider` / fail-closed `PaymentService` extension contract is now defined in [`functions/src/payments/payment-provider.ts`](../functions/src/payments/payment-provider.ts); with no configured provider, its online checkout and webhook methods reject. The existing manual-offline callables are still the only enabled path. No credentials or provider setup were supplied. Do not create a fake provider or label the interface or manual verification as a live online integration.

## Provider decision required

The church's finance/legal owner must choose and contract a provider, confirm supported currencies and settlement accounts, decide how refunds/chargebacks are handled, and supply sandbox and production credentials through an approved secure configuration channel. Do not put credentials in Git, a source ZIP, a mobile APK, logs, or chat.

## Required server-side integration boundary

A future integration must inject a contracted adapter into `PaymentService` and follow a server-owned flow such as:

`PaymentService → selected PaymentProvider adapter → provider API → authenticated webhook/callback → PaymentVerificationService`

Before any record can become paid, verify provider signature, transaction status, amount, currency, registration, user, church/tenant, and server timestamp. Enforce idempotency and replay protection, reject duplicate or mismatched callbacks, and write immutable audit events. Client-supplied “paid” state must never be trusted. Keep provider secrets on trusted servers, not in the Android client.

Use a provider sandbox and test duplicate/out-of-order callbacks, forged signatures, mismatched amounts/currencies/tenants/users, expiry, cancellation, retry, refunds, and replay before any production launch. Until that work and independent validation are complete, retain **NOT INTEGRATED**. Existing manual transfer flow remains available.
