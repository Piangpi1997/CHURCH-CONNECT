export interface OnlineCheckoutRequest {
  paymentId: string;
  registrationId: string;
  userId: string;
  churchId: string;
  amountMinor: number;
  currency: string;
  idempotencyKey: string;
}

export interface OnlineCheckoutSession {
  providerId: string;
  providerTransactionId: string;
  checkoutUrl: string;
  expiresAtMillis: number;
}

/**
 * A provider adapter must validate its webhook signature before returning this
 * event. This is still an untrusted input to the payment-verification service:
 * the backend must reload and transactionally match the payment, registration,
 * owner, church, amount, currency, event ID, and server-owned payment state.
 */
export interface VerifiedProviderPayment {
  providerId: string;
  eventId: string;
  providerTransactionId: string;
  paymentId: string;
  registrationId: string;
  userId: string;
  churchId: string;
  amountMinor: number;
  currency: string;
  status: "PAID" | "FAILED" | "CANCELLED";
  occurredAtMillis: number;
}

/** Provider-specific network and signature handling; never embed its secrets in Android. */
export interface PaymentProvider {
  readonly id: string;
  createCheckoutSession(request: OnlineCheckoutRequest): Promise<OnlineCheckoutSession>;
  verifyWebhook(rawBody: Uint8Array, signature: string): Promise<VerifiedProviderPayment>;
}

/**
 * Extension seam for a future contracted provider. Passing null is deliberate
 * until the church selects a provider and supplies protected server-side config.
 * This service does not mutate payment state; verified events must go through a
 * separate trusted, idempotent Firestore verification transaction.
 */
export class PaymentService {
  constructor(private readonly provider: PaymentProvider | null) {}

  get onlineProviderId(): string | null {
    return this.provider?.id ?? null;
  }

  async createCheckoutSession(request: OnlineCheckoutRequest): Promise<OnlineCheckoutSession> {
    const provider = this.requireProvider();
    if (!Number.isSafeInteger(request.amountMinor) || request.amountMinor <= 0) {
      throw new Error("Online payment amount must be a positive integer in minor units.");
    }
    if (!/^[A-Z]{3}$/.test(request.currency)) {
      throw new Error("Online payment currency must be an uppercase ISO 4217 code.");
    }
    if (!request.idempotencyKey.trim()) {
      throw new Error("Online payment requires a stable idempotency key.");
    }
    return provider.createCheckoutSession(request);
  }

  async verifyWebhook(rawBody: Uint8Array, signature: string | null): Promise<VerifiedProviderPayment> {
    const provider = this.requireProvider();
    if (!signature?.trim()) throw new Error("Provider webhook signature is required.");
    const event = await provider.verifyWebhook(rawBody, signature);
    if (event.providerId !== provider.id) throw new Error("Provider identity mismatch.");
    return event;
  }

  private requireProvider(): PaymentProvider {
    if (!this.provider) {
      throw new Error("Online payment is not configured; use the manual offline payment flow.");
    }
    return this.provider;
  }
}
