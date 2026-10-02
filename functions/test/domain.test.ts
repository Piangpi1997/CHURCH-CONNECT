import test from "node:test";
import assert from "node:assert/strict";
import { assertTransition, calculateFee, formatMemberNumber, mayPerformRole, normalizeReference, validateApplication } from "../src/domain";

test("fee engine uses configured integer minor units", () => {
  assert.deepEqual(calculateFee("INDIVIDUAL", { individual: 2100, family: 5100 }), { amount: 2100, currency: "MYR", feeType: "INDIVIDUAL" });
  assert.throws(() => calculateFee("FAMILY", { individual: 1.5, family: -1 }));
});
test("state machine rejects shortcuts and post-approval changes", () => {
  assert.doesNotThrow(() => assertTransition("UNDER_REVIEW", "APPROVED"));
  assert.throws(() => assertTransition("PENDING_PAYMENT", "APPROVED"));
  assert.throws(() => assertTransition("APPROVED", "REJECTED"));
});
test("only a same-church authorized role passes authorization", () => {
  assert.equal(mayPerformRole("FINANCE_ADMIN", ["FINANCE_ADMIN", "CHURCH_ADMIN"], "cmf", "cmf"), true);
  assert.equal(mayPerformRole("MEMBER", ["FINANCE_ADMIN", "CHURCH_ADMIN"], "cmf", "cmf"), false);
  assert.equal(mayPerformRole("CHURCH_ADMIN", ["CHURCH_ADMIN"], "church-a", "church-b"), false);
});
test("application validation enforces consent and family rules", () => {
  const individual = { type: "INDIVIDUAL", applicantName: "Ada Example", phone: "+60123456789", address: "Setapak, Kuala Lumpur", familyMembers: [], privacyConsent: true };
  assert.deepEqual(validateApplication(individual).familyMembers, []);
  assert.throws(() => validateApplication({ ...individual, privacyConsent: false }));
  assert.throws(() => validateApplication({ ...individual, type: "FAMILY" }));
  assert.throws(() => validateApplication({ ...individual, type: "INDIVIDUAL", familyMembers: ["Another Person"] }));
});
test("references and server-generated member numbers are constrained", () => {
  assert.equal(normalizeReference("  TXN-1234  "), "TXN-1234");
  assert.throws(() => normalizeReference("x"));
  assert.equal(formatMemberNumber("CMF", 1), "CMF-000001");
  assert.throws(() => formatMemberNumber("CMF", 0));
});
