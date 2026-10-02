import assert from "node:assert/strict";
import test from "node:test";
import { PaymentService } from "../src/payments/payment-provider.js";

const unavailable = new PaymentService(null);

const checkout = {
  paymentId: "payment-test",
  registrationId: "registration-test",
  userId: "member-test",
  churchId: "church-test",
  amountMinor: 2000,
  currency: "MYR",
  idempotencyKey: "registration-test:checkout-v1",
};

test("online checkout fails closed when no contracted provider is configured", async () => {
  assert.equal(unavailable.onlineProviderId, null);
  await assert.rejects(
    unavailable.createCheckoutSession(checkout),
    /Online payment is not configured; use the manual offline payment flow\./,
  );
});

test("provider callbacks cannot be accepted without a configured provider", async () => {
  await assert.rejects(
    unavailable.verifyWebhook(new Uint8Array(), "signature-placeholder"),
    /Online payment is not configured; use the manual offline payment flow\./,
  );
});
