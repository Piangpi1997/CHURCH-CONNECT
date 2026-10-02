import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import { initializeApp, deleteApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const projectId = "demo-church-connect";
const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099";
const functionsHost = process.env.FUNCTIONS_EMULATOR_HOST || "127.0.0.1:5001";
let adminApp;
let db;
const actors = {};

async function createActor(key, role, churchId = "cmf-setapak") {
  const response = await fetch(`http://${authHost}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: `${key}@example.invalid`, password: "test-password-123", returnSecureToken: true }),
  });
  const auth = await response.json();
  assert.equal(response.ok, true, `Auth emulator signup failed for ${key}: ${auth.error?.message || JSON.stringify(auth)}`);
  await db.collection("users").doc(auth.localId).set({
    uid: auth.localId, churchId, role, fullName: key, email: auth.email, status: "ACTIVE",
  });
  return { uid: auth.localId, token: auth.idToken };
}

async function call(name, actor, data = {}) {
  const response = await fetch(`http://${functionsHost}/${projectId}/asia-southeast1/${name}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${actor.token}` },
    body: JSON.stringify({ data }),
  });
  const body = await response.json();
  if (!response.ok || body.error) {
    const error = new Error(body.error?.message || `Callable ${name} returned HTTP ${response.status}`);
    error.code = String(body.error?.status || "UNKNOWN").toLowerCase().replaceAll("_", "-");
    throw error;
  }
  return body.result;
}

async function expectCallableError(work, code) {
  await assert.rejects(work, (error) => {
    assert.equal(error.code, code, `Unexpected callable error: ${error.message}`);
    return true;
  });
}

const individualDraft = {
  type: "INDIVIDUAL", applicantName: "Ada Member", phone: "+60123456789",
  address: "Setapak, Kuala Lumpur", familyMembers: [], privacyConsent: true,
};

before(async () => {
  process.env.FIRESTORE_EMULATOR_HOST ||= "127.0.0.1:8080";
  adminApp = initializeApp({ projectId }, `phase1-integration-${Date.now()}`);
  db = getFirestore(adminApp);
  await db.collection("churches").doc("cmf-setapak").set({
    churchId: "cmf-setapak", name: "Christ Mission Fellowship Church", location: "Setapak, Kuala Lumpur, Malaysia",
    currency: "MYR", timezone: "Asia/Kuala_Lumpur", memberNumberPrefix: "CMF",
    registrationFees: { individual: 2375, family: 5875, currency: "MYR", version: 3 },
  });
  actors.memberA = await createActor("member-a", "MEMBER");
  actors.memberB = await createActor("member-b", "MEMBER");
  actors.memberC = await createActor("member-c", "MEMBER");
  actors.finance = await createActor("finance-a", "FINANCE_ADMIN");
  actors.admin = await createActor("admin-a", "CHURCH_ADMIN");
  actors.usher = await createActor("usher-a", "USHER");
  actors.outsiderAdmin = await createActor("admin-b", "CHURCH_ADMIN", "another-church");
  actors.misconfigured = await createActor("member-missing-config", "MEMBER", "missing-church");
});

after(async () => {
  if (adminApp) await deleteApp(adminApp);
});

test("Phase 1 individual and family critical paths are server-controlled and idempotent", async () => {
  await expectCallableError(() => call("createRegistration", actors.misconfigured, individualDraft), "failed-precondition");
  await expectCallableError(() => call("createRegistration", actors.memberA, { ...individualDraft, privacyConsent: false }), "invalid-argument");

  const first = await call("createRegistration", actors.memberA, individualDraft);
  assert.equal(first.amount, 2375, "the immutable transaction amount must come from church configuration");
  assert.equal(first.currency, "MYR");
  const firstPayment = (await db.collection("payments").doc(first.paymentId).get()).data();
  assert.deepEqual({
    transactionId: firstPayment.transactionId, registrationId: firstPayment.registrationId,
    userId: firstPayment.userId, churchId: firstPayment.churchId, amount: firstPayment.amount,
    currency: firstPayment.currency, feeType: firstPayment.feeType, provider: firstPayment.provider,
    providerTransactionId: firstPayment.providerTransactionId, status: firstPayment.status,
    verifiedAt: firstPayment.verifiedAt,
  }, {
    transactionId: first.paymentId, registrationId: first.registrationId, userId: actors.memberA.uid,
    churchId: "cmf-setapak", amount: 2375, currency: "MYR", feeType: "INDIVIDUAL",
    provider: "MANUAL_OFFLINE", providerTransactionId: null, status: "PENDING", verifiedAt: null,
  });
  assert.ok(firstPayment.createdAt && firstPayment.updatedAt);
  await expectCallableError(() => call("createRegistration", actors.memberA, individualDraft), "already-exists");
  await expectCallableError(() => call("beginRegistrationReview", actors.memberA, { registrationId: first.registrationId }), "permission-denied");
  await expectCallableError(() => call("beginRegistrationReview", actors.memberB, { registrationId: first.registrationId }), "permission-denied");
  await expectCallableError(() => call("beginRegistrationReview", actors.outsiderAdmin, { registrationId: first.registrationId }), "permission-denied");
  await expectCallableError(() => call("reviewRegistration", actors.memberA, { registrationId: first.registrationId, decision: "APPROVE" }), "permission-denied");
  await expectCallableError(() => call("reviewRegistration", actors.memberB, { registrationId: first.registrationId, decision: "APPROVE" }), "permission-denied");
  await expectCallableError(() => call("verifyManualPayment", actors.memberA, { registrationId: first.registrationId, paymentReference: "BANK-1001" }), "permission-denied");

  await call("submitPaymentReference", actors.memberA, { registrationId: first.registrationId, paymentReference: "BANK-1001" });
  await expectCallableError(() => call("submitPaymentReference", actors.memberA, { registrationId: first.registrationId, paymentReference: "BANK-1001" }), "failed-precondition");
  await expectCallableError(() => call("verifyManualPayment", actors.finance, { registrationId: first.registrationId, paymentReference: "BANK-WRONG" }), "failed-precondition");
  await call("verifyManualPayment", actors.finance, { registrationId: first.registrationId, paymentReference: "BANK-1001" });
  await expectCallableError(() => call("verifyManualPayment", actors.finance, { registrationId: first.registrationId, paymentReference: "BANK-1001" }), "failed-precondition");
  await expectCallableError(() => call("submitRegistration", actors.memberB, { registrationId: first.registrationId }), "permission-denied");
  await call("submitRegistration", actors.memberA, { registrationId: first.registrationId });
  await expectCallableError(() => call("submitRegistration", actors.memberA, { registrationId: first.registrationId }), "failed-precondition");
  await expectCallableError(() => call("getDigitalId", actors.memberA), "failed-precondition");

  const second = await call("createRegistration", actors.memberB, {
    ...individualDraft, type: "FAMILY", applicantName: "Bo Family", familyMembers: ["Lin Family"],
  });
  assert.equal(second.amount, 5875);
  await call("submitPaymentReference", actors.memberB, { registrationId: second.registrationId, paymentReference: "BANK-2002" });
  await call("verifyManualPayment", actors.finance, { registrationId: second.registrationId, paymentReference: "BANK-2002" });
  await call("submitRegistration", actors.memberB, { registrationId: second.registrationId });

  await call("beginRegistrationReview", actors.admin, { registrationId: first.registrationId });
  await call("beginRegistrationReview", actors.admin, { registrationId: second.registrationId });
  await expectCallableError(() => call("beginRegistrationReview", actors.admin, { registrationId: first.registrationId }), "failed-precondition");
  const approvals = await Promise.all([
    call("reviewRegistration", actors.admin, { registrationId: first.registrationId, decision: "APPROVE" }),
    call("reviewRegistration", actors.admin, { registrationId: second.registrationId, decision: "APPROVE" }),
  ]);
  assert.deepEqual(new Set(approvals.map((entry) => entry.memberNumber)), new Set(["CMF-000001", "CMF-000002"]));

  const familyDoc = await db.collection("households").doc(`cmf-setapak_${actors.memberB.uid}`).get();
  assert.equal(familyDoc.exists, true);
  assert.equal(familyDoc.get("primaryUserId"), actors.memberB.uid);
  assert.deepEqual(familyDoc.get("members"), ["Lin Family"]);

  const card = await call("getDigitalId", actors.memberA);
  assert.match(card.qrPayload, /^CMF1:[a-f0-9]{64}$/);
  assert.equal(card.memberNumber, approvals.find((entry) => entry.status === "APPROVED" && entry.memberNumber === card.memberNumber)?.memberNumber);
  const oldQr = card.qrPayload;
  const refreshed = await call("getDigitalId", actors.memberA);
  assert.notEqual(refreshed.qrPayload, oldQr);
  assert.equal((await call("verifyQr", actors.usher, { qrPayload: oldQr })).valid, false);
  const validQr = await call("verifyQr", actors.usher, { qrPayload: refreshed.qrPayload });
  assert.equal(validQr.valid, true);
  assert.equal(validQr.memberNumber, card.memberNumber);
  assert.equal(Object.hasOwn(validQr, "phone"), false);
  await expectCallableError(() => call("verifyQr", actors.memberB, { qrPayload: refreshed.qrPayload }), "permission-denied");

  const audit = await db.collection("auditLogs").where("churchId", "==", "cmf-setapak").get();
  assert.ok(audit.size >= 8, "expected durable server-side audit events for critical actions");
});

test("admin queue authorization and rejection preserve the no-member-number rule", async () => {
  await expectCallableError(() => call("getAdminQueue", actors.memberC), "permission-denied");
  const application = await call("createRegistration", actors.memberC, individualDraft);
  await call("submitPaymentReference", actors.memberC, { registrationId: application.registrationId, paymentReference: "BANK-3003" });
  await call("verifyManualPayment", actors.finance, { registrationId: application.registrationId, paymentReference: "BANK-3003" });
  await call("submitRegistration", actors.memberC, { registrationId: application.registrationId });
  const financeQueue = await call("getAdminQueue", actors.finance);
  assert.equal(financeQueue.applications.some((entry) => entry.id === application.registrationId), true);
  const financeRow = financeQueue.applications.find((entry) => entry.id === application.registrationId);
  assert.equal(financeRow.phone, "", "finance queue must not expose applicant contact details");
  assert.equal(financeRow.address, "", "finance queue must not expose private addresses");

  await call("beginRegistrationReview", actors.admin, { registrationId: application.registrationId });
  await expectCallableError(() => call("reviewRegistration", actors.admin, { registrationId: application.registrationId, decision: "REJECT", reason: "" }), "invalid-argument");
  const rejected = await call("reviewRegistration", actors.admin, { registrationId: application.registrationId, decision: "REJECT", reason: "Please contact the church office to update your application." });
  assert.equal(rejected.status, "REJECTED");
  const stored = await db.collection("registrations").doc(application.registrationId).get();
  assert.equal(stored.get("status"), "REJECTED");
  assert.equal(stored.get("rejectionReason"), "Please contact the church office to update your application.");
  assert.equal(stored.get("memberNumber"), undefined);
  await expectCallableError(() => call("getDigitalId", actors.memberC), "failed-precondition");
});
