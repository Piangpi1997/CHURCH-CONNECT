import { createHash, randomBytes } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { CallableRequest, HttpsError, onCall as firebaseOnCall } from "firebase-functions/v2/https";
import { assertTransition, calculateFee, formatMemberNumber, mayPerformRole, normalizeReference, validateApplication } from "./domain";

initializeApp();
const db = getFirestore();
const region = "asia-southeast1";
const defaultChurchId = process.env.CHURCH_ID || "cmf-setapak";
const reviewerRoles = ["SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN"] as const;
const financeRoles = ["SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN", "FINANCE_ADMIN"] as const;
const qrVerifierRoles = [...financeRoles, "USHER"] as const;

type Request = CallableRequest<any>;
type Profile = { uid: string; churchId: string; role: string; fullName: string; email?: string; memberNumber?: string };
const onCall = (options: { region: string }, handler: (request: Request) => any) =>
  firebaseOnCall({ ...options, enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== "true" }, handler);

function authUid(request: Request): string {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in is required.");
  return uid;
}
function clean(value: unknown, label: string, max = 160): string {
  if (typeof value !== "string") throw new HttpsError("invalid-argument", `${label} is required.`);
  const text = value.trim();
  if (!text || text.length > max || /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/.test(text)) throw new HttpsError("invalid-argument", `${label} is invalid.`);
  return text;
}
function invalidInput<T>(work: () => T): T {
  try { return work(); }
  catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("invalid-argument", error instanceof Error ? error.message : "Invalid input.");
  }
}
function hash(value: string): string { return createHash("sha256").update(value).digest("hex"); }
function audit(tx: FirebaseFirestore.Transaction, actorUid: string, churchId: string, action: string, resource: string, metadata: Record<string, unknown> = {}) {
  tx.create(db.collection("auditLogs").doc(), { actorUid, churchId, action, resource, metadata, createdAt: FieldValue.serverTimestamp() });
}
function notify(tx: FirebaseFirestore.Transaction, recipientUserId: string, churchId: string, title: string, body: string, category: string) {
  tx.create(db.collection("notifications").doc(), { recipientUserId, churchId, title, body, category, readAt: null, createdAt: FieldValue.serverTimestamp() });
}
async function profile(uid: string): Promise<Profile> {
  const doc = await db.collection("users").doc(uid).get();
  if (!doc.exists) throw new HttpsError("failed-precondition", "Complete your account profile first.");
  const data = doc.data()!;
  return { uid, churchId: String(data.churchId || ""), role: String(data.role || "MEMBER"), fullName: String(data.fullName || "Member"), email: data.email as string | undefined, memberNumber: data.memberNumber as string | undefined };
}
async function authorized(uid: string, churchId: string, roles: readonly string[]): Promise<Profile> {
  const actor = await profile(uid);
  if (!mayPerformRole(actor.role, roles, actor.churchId, churchId)) throw new HttpsError("permission-denied", "This action is not permitted for your role or church.");
  return actor;
}
async function churchConfiguration(churchId: string) {
  const doc = await db.collection("churches").doc(churchId).get();
  if (!doc.exists) throw new HttpsError("failed-precondition", `Church configuration is required for ${churchId}.`);
  const data = doc.data()!;
  const fees = (data.registrationFees || {}) as Record<string, unknown>;
  const individualFee = fees.individual;
  const familyFee = fees.family;
  const currency = typeof fees.currency === "string" ? fees.currency : data.currency;
  const feeVersion = fees.version;
  if (!Number.isSafeInteger(individualFee) || Number(individualFee) < 0 || !Number.isSafeInteger(familyFee) || Number(familyFee) < 0) {
    throw new HttpsError("failed-precondition", "Church registration fees must be configured as non-negative integer minor units.");
  }
  if (typeof currency !== "string" || !/^[A-Z]{3}$/.test(currency)) {
    throw new HttpsError("failed-precondition", "Church registration currency must be configured as a three-letter ISO code.");
  }
  if (!Number.isSafeInteger(feeVersion) || Number(feeVersion) < 1) {
    throw new HttpsError("failed-precondition", "Church fee configuration version must be a positive integer.");
  }
  const prefix = typeof data.memberNumberPrefix === "string" ? data.memberNumberPrefix : "CMF";
  if (!/^[A-Z0-9-]{1,12}$/.test(prefix)) throw new HttpsError("failed-precondition", "Church member-number prefix is invalid.");
  return {
    name: typeof data.name === "string" ? data.name : "Christ Mission Fellowship Church",
    shortName: typeof data.shortName === "string" ? data.shortName : "CMF Church",
    location: typeof data.location === "string" ? data.location : "Setapak, Kuala Lumpur, Malaysia",
    timezone: typeof data.timezone === "string" ? data.timezone : "Asia/Kuala_Lumpur",
    currency,
    individualFee: Number(individualFee),
    familyFee: Number(familyFee),
    prefix,
    feeVersion: Number(feeVersion),
  };
}
async function registrationById(id: unknown) {
  const registrationId = clean(id, "Registration ID", 128);
  const ref = db.collection("registrations").doc(registrationId);
  const snapshot = await ref.get();
  if (!snapshot.exists) throw new HttpsError("not-found", "Application not found.");
  return { ref, data: snapshot.data()!, id: snapshot.id };
}
function registrationBelongsToChurch(data: FirebaseFirestore.DocumentData, churchId: string) {
  if (data.churchId !== churchId) throw new HttpsError("permission-denied", "Cross-church access is not permitted.");
}
function assertPaymentMatchesRegistration(registration: FirebaseFirestore.DocumentData, payment: FirebaseFirestore.DocumentData, registrationId: string, paymentId: string) {
  if (payment.transactionId !== paymentId) throw new HttpsError("failed-precondition", "Payment transaction identity is invalid.");
  if (payment.registrationId !== registrationId || payment.userId !== registration.userId || payment.churchId !== registration.churchId
      || payment.amount !== registration.amount || payment.currency !== registration.currency || payment.feeType !== registration.feeType) {
    throw new HttpsError("failed-precondition", "Payment transaction details do not match this application.");
  }
  if (!("transactionId" in payment) || !("providerTransactionId" in payment) || !("verifiedAt" in payment)) {
    throw new HttpsError("failed-precondition", "Payment transaction record is incomplete.");
  }
}

export const getChurchConfig = onCall({ region }, async () => {
  const config = await churchConfiguration(defaultChurchId);
  return { churchId: defaultChurchId, name: config.name, shortName: config.shortName, location: config.location, timezone: config.timezone, currency: config.currency, individualFee: config.individualFee, familyFee: config.familyFee, feeVersion: config.feeVersion };
});

export const ensureProfile = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const ref = db.collection("users").doc(uid);
  const existing = await ref.get();
  if (existing.exists) {
    const data = existing.data()!;
    return { fullName: data.fullName, role: data.role || "MEMBER", churchId: data.churchId || defaultChurchId, memberNumber: data.memberNumber || null };
  }
  const token = request.auth!.token;
  const fallbackName = typeof token.name === "string" && token.name.trim() ? token.name.trim() : String(token.email || "Member").split("@")[0];
  const fullName = typeof request.data?.fullName === "string" && request.data.fullName.trim() ? clean(request.data.fullName, "Full name", 120) : fallbackName.slice(0, 120);
  const email = typeof token.email === "string" ? token.email : "";
  await ref.create({ uid, fullName, email, churchId: defaultChurchId, role: "MEMBER", status: "ACTIVE", createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
  return { fullName, email, role: "MEMBER", churchId: defaultChurchId, memberNumber: null };
});

export const createRegistration = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const owner = await profile(uid);
  const validated = invalidInput(() => validateApplication((request.data || {}) as Record<string, unknown>));
  const config = await churchConfiguration(owner.churchId);
  const fee = invalidInput(() => calculateFee(validated.type, { individual: config.individualFee, family: config.familyFee, currency: config.currency }));
  const lockRef = db.collection("registrationLocks").doc(uid);
  const activeMemberRef = db.collection("members").doc(`${owner.churchId}_${uid}`);
  const registrationRef = db.collection("registrations").doc();
  const paymentRef = db.collection("payments").doc();
  await db.runTransaction(async (tx) => {
    const [lock, activeMember] = await Promise.all([tx.get(lockRef), tx.get(activeMemberRef)]);
    if (activeMember.exists) throw new HttpsError("already-exists", "This account already has an active membership.");
    const activeId = lock.get("registrationId") as string | undefined;
    if (activeId) {
      const active = await tx.get(db.collection("registrations").doc(activeId));
      if (active.exists && !["APPROVED", "REJECTED"].includes(String(active.get("status")))) throw new HttpsError("already-exists", "You already have an active membership application.");
    }
    tx.create(registrationRef, {
      churchId: owner.churchId, userId: uid, type: validated.type, applicantName: validated.applicantName, phone: validated.phone,
      address: validated.address, familyMembers: validated.familyMembers, privacyConsent: true,
      status: "PENDING_PAYMENT", paymentId: paymentRef.id, paymentStatus: "PENDING", amount: fee.amount,
      currency: fee.currency, feeType: fee.feeType, feeVersion: config.feeVersion,
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(paymentRef, {
      transactionId: paymentRef.id, registrationId: registrationRef.id, userId: uid, churchId: owner.churchId,
      amount: fee.amount, currency: fee.currency, feeType: fee.feeType,
      provider: "MANUAL_OFFLINE", providerTransactionId: null, status: "PENDING", mode: "MANUAL_OFFLINE", verifiedAt: null,
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
    });
    tx.set(lockRef, { churchId: owner.churchId, userId: uid, registrationId: registrationRef.id, updatedAt: FieldValue.serverTimestamp() });
    audit(tx, uid, owner.churchId, "REGISTRATION_CREATED", registrationRef.id, { type: validated.type, feeVersion: config.feeVersion });
  });
  return { registrationId: registrationRef.id, paymentId: paymentRef.id, amount: fee.amount, currency: fee.currency, feeType: fee.feeType, paymentMode: "MANUAL_OFFLINE" };
});

// Manual offline transfer is the only enabled method. A future online provider
// belongs behind PaymentService; a submitted reference must never mark a payment PAID.
export const submitPaymentReference = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const reference = invalidInput(() => normalizeReference(request.data?.paymentReference));
  const { ref, data, id } = await registrationById(request.data?.registrationId);
  registrationBelongsToChurch(data, (await profile(uid)).churchId);
  if (data.userId !== uid) throw new HttpsError("permission-denied", "You can only submit a payment reference for your own application.");
  if (data.status !== "PENDING_PAYMENT") throw new HttpsError("failed-precondition", "This application is not awaiting payment.");
  const paymentRef = db.collection("payments").doc(String(data.paymentId));
  await db.runTransaction(async (tx) => {
    const [registration, payment] = await Promise.all([tx.get(ref), tx.get(paymentRef)]);
    if (!registration.exists || registration.get("status") !== "PENDING_PAYMENT" || !payment.exists || payment.get("status") !== "PENDING") throw new HttpsError("failed-precondition", "Payment is no longer awaiting a reference.");
    assertPaymentMatchesRegistration(registration.data()!, payment.data()!, id, paymentRef.id);
    tx.update(paymentRef, { status: "PROCESSING", paymentReference: reference, updatedAt: FieldValue.serverTimestamp() });
    tx.update(ref, { paymentStatus: "PROCESSING", paymentReference: reference, updatedAt: FieldValue.serverTimestamp() });
    audit(tx, uid, String(data.churchId), "PAYMENT_REFERENCE_SUBMITTED", id, { paymentId: paymentRef.id });
  });
  return { accepted: true, status: "PROCESSING" };
});

export const verifyManualPayment = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const { ref, data, id } = await registrationById(request.data?.registrationId);
  const actor = await authorized(uid, String(data.churchId), financeRoles);
  const suppliedReference = invalidInput(() => normalizeReference(request.data?.paymentReference));
  const paymentRef = db.collection("payments").doc(String(data.paymentId));
  await db.runTransaction(async (tx) => {
    const [registration, payment] = await Promise.all([tx.get(ref), tx.get(paymentRef)]);
    if (!registration.exists || !payment.exists) throw new HttpsError("not-found", "Application or payment record not found.");
    assertPaymentMatchesRegistration(registration.data()!, payment.data()!, id, paymentRef.id);
    if (registration.get("status") !== "PENDING_PAYMENT" || payment.get("status") !== "PROCESSING") throw new HttpsError("failed-precondition", "Payment is not awaiting finance verification.");
    if (payment.get("paymentReference") !== suppliedReference) throw new HttpsError("failed-precondition", "The submitted transfer reference does not match.");
    tx.update(paymentRef, { status: "PAID", verifiedBy: uid, verificationMethod: "MANUAL_OFFLINE", verifiedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
    tx.update(ref, { status: "PAYMENT_VERIFIED", paymentStatus: "PAID", verifiedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
    audit(tx, uid, actor.churchId, "MANUAL_PAYMENT_VERIFIED", id, { paymentId: paymentRef.id, method: "MANUAL_OFFLINE" });
    notify(tx, String(data.userId), actor.churchId, "Payment verified", "Finance staff verified the payment reference. You can now submit your application.", "PAYMENT");
  });
  return { verified: true, status: "PAYMENT_VERIFIED", mode: "MANUAL_OFFLINE" };
});

export const submitRegistration = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const { ref, data, id } = await registrationById(request.data?.registrationId);
  const owner = await profile(uid);
  registrationBelongsToChurch(data, owner.churchId);
  if (data.userId !== uid) throw new HttpsError("permission-denied", "You can only submit your own application.");
  const paymentRef = db.collection("payments").doc(String(data.paymentId));
  await db.runTransaction(async (tx) => {
    const [registration, payment] = await Promise.all([tx.get(ref), tx.get(paymentRef)]);
    if (!registration.exists || !payment.exists || registration.get("status") !== "PAYMENT_VERIFIED" || payment.get("status") !== "PAID") throw new HttpsError("failed-precondition", "A verified payment is required before submission.");
    assertPaymentMatchesRegistration(registration.data()!, payment.data()!, id, paymentRef.id);
    assertTransition("PAYMENT_VERIFIED", "SUBMITTED");
    tx.update(ref, { status: "SUBMITTED", submittedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
    audit(tx, uid, owner.churchId, "REGISTRATION_SUBMITTED", id);
    notify(tx, uid, owner.churchId, "Application submitted", "Your membership application is in the church review queue.", "REGISTRATION");
  });
  return { submitted: true, status: "SUBMITTED" };
});

export const getAdminQueue = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const actor = await profile(uid);
  const mayReview = reviewerRoles.includes(actor.role as typeof reviewerRoles[number]);
  const mayVerify = financeRoles.includes(actor.role as typeof financeRoles[number]);
  if (!mayReview && !mayVerify) throw new HttpsError("permission-denied", "Admin access is required.");
  const docs = await db.collection("registrations").where("churchId", "==", actor.churchId).where("status", "in", ["PENDING_PAYMENT", "PAYMENT_VERIFIED", "SUBMITTED", "UNDER_REVIEW"]).get();
  const applications = docs.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id, type: data.type, status: data.status, applicantName: data.applicantName,
      phone: mayReview ? data.phone : "", address: mayReview ? data.address : "",
      familyMembers: mayReview ? data.familyMembers : [], amount: data.amount, currency: data.currency,
      paymentStatus: data.paymentStatus, paymentReference: data.paymentReference || null,
      rejectionReason: mayReview ? data.rejectionReason || null : null,
      createdAt: data.createdAt?.toMillis?.() || 0,
    };
  }).sort((a, b) => a.createdAt - b.createdAt);
  return { applications };
});

export const beginRegistrationReview = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const { ref, data, id } = await registrationById(request.data?.registrationId);
  const actor = await authorized(uid, String(data.churchId), reviewerRoles);
  await db.runTransaction(async (tx) => {
    const registration = await tx.get(ref);
    if (!registration.exists || registration.get("status") !== "SUBMITTED") throw new HttpsError("failed-precondition", "Only a submitted application can enter review.");
    assertTransition("SUBMITTED", "UNDER_REVIEW");
    tx.update(ref, { status: "UNDER_REVIEW", reviewerUid: uid, reviewStartedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
    audit(tx, uid, actor.churchId, "REGISTRATION_REVIEW_STARTED", id);
  });
  return { status: "UNDER_REVIEW" };
});

export const reviewRegistration = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const { ref, data, id } = await registrationById(request.data?.registrationId);
  const actor = await authorized(uid, String(data.churchId), reviewerRoles);
  const decision = request.data?.decision;
  if (decision !== "APPROVE" && decision !== "REJECT") throw new HttpsError("invalid-argument", "Choose APPROVE or REJECT.");
  const reason = decision === "REJECT" ? clean(request.data?.reason, "Rejection reason", 500) : "";
  const paymentRef = db.collection("payments").doc(String(data.paymentId));
  const memberRef = db.collection("members").doc(`${actor.churchId}_${String(data.userId)}`);
  const householdRef = db.collection("households").doc(`${actor.churchId}_${String(data.userId)}`);
  const counterRef = db.collection("churches").doc(actor.churchId).collection("counters").doc("memberNumbers");
  const config = decision === "APPROVE" ? await churchConfiguration(actor.churchId) : null;
  let assignedNumber: string | null = null;
  await db.runTransaction(async (tx) => {
    const [registration, payment] = await Promise.all([tx.get(ref), tx.get(paymentRef)]);
    if (!registration.exists || !payment.exists || registration.get("status") !== "UNDER_REVIEW") throw new HttpsError("failed-precondition", "Start the review before deciding.");
    assertPaymentMatchesRegistration(registration.data()!, payment.data()!, id, paymentRef.id);
    if (decision === "APPROVE") {
      const [counter, member] = await Promise.all([tx.get(counterRef), tx.get(memberRef)]);
      if (payment.get("status") !== "PAID") throw new HttpsError("failed-precondition", "A verified payment is required before approval.");
      if (member.exists) throw new HttpsError("already-exists", "An active member record already exists.");
      assertTransition("UNDER_REVIEW", "APPROVED");
      const next = Number(counter.get("nextSequence") || 1);
      assignedNumber = invalidInput(() => formatMemberNumber(config!.prefix, next));
      tx.set(counterRef, { nextSequence: next + 1, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      tx.create(memberRef, {
        churchId: actor.churchId, userId: String(data.userId), name: String(registration.get("applicantName")),
        memberNumber: assignedNumber, membershipType: String(registration.get("type")), status: "ACTIVE",
        householdId: registration.get("type") === "FAMILY" ? householdRef.id : null,
        createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(), activeQrHash: null,
      });
      if (registration.get("type") === "FAMILY") {
        tx.create(householdRef, {
          churchId: actor.churchId, primaryUserId: String(data.userId),
          primaryName: String(registration.get("applicantName")), members: registration.get("familyMembers") || [],
          status: "ACTIVE", createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
        });
      }
      tx.update(db.collection("users").doc(String(data.userId)), { memberNumber: assignedNumber, membershipStatus: "ACTIVE", householdId: registration.get("type") === "FAMILY" ? householdRef.id : null, updatedAt: FieldValue.serverTimestamp() });
      tx.update(ref, { status: "APPROVED", reviewedBy: uid, reviewedAt: FieldValue.serverTimestamp(), memberNumber: assignedNumber, updatedAt: FieldValue.serverTimestamp() });
      audit(tx, uid, actor.churchId, "REGISTRATION_APPROVED", id, { memberNumber: assignedNumber });
      notify(tx, String(data.userId), actor.churchId, "Membership approved", `Your membership is approved. Your member number is ${assignedNumber}.`, "REGISTRATION");
    } else {
      assertTransition("UNDER_REVIEW", "REJECTED");
      tx.update(ref, { status: "REJECTED", rejectionReason: reason, reviewedBy: uid, reviewedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
      audit(tx, uid, actor.churchId, "REGISTRATION_REJECTED", id, { reason });
      notify(tx, String(data.userId), actor.churchId, "Application update", "Your application was not approved. Please contact the church office for details.", "REGISTRATION");
    }
  });
  return decision === "APPROVE" ? { status: "APPROVED", memberNumber: assignedNumber } : { status: "REJECTED" };
});

export const getDigitalId = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const owner = await profile(uid);
  const memberRef = db.collection("members").doc(`${owner.churchId}_${uid}`);
  const token = randomBytes(32).toString("hex");
  const tokenHash = hash(token);
  const qrRef = db.collection("qrVerifications").doc(tokenHash);
  let previousHash: string | null = null;
  const snapshot = await db.runTransaction(async (tx) => {
    const member = await tx.get(memberRef);
    if (!member.exists || member.get("status") !== "ACTIVE") throw new HttpsError("failed-precondition", "Digital member IDs are available after approval.");
    previousHash = member.get("activeQrHash") as string | null;
    const oldRef = previousHash ? db.collection("qrVerifications").doc(previousHash) : null;
    if (oldRef) await tx.get(oldRef);
    if (oldRef) tx.delete(oldRef);
    tx.create(qrRef, { churchId: owner.churchId, userId: uid, memberId: memberRef.id, status: "ACTIVE", createdAt: FieldValue.serverTimestamp() });
    tx.update(memberRef, { activeQrHash: tokenHash, updatedAt: FieldValue.serverTimestamp() });
    audit(tx, uid, owner.churchId, "DIGITAL_ID_QR_ISSUED", memberRef.id);
    return member;
  });
  return { name: snapshot.get("name"), memberNumber: snapshot.get("memberNumber"), membershipType: snapshot.get("membershipType"), churchName: (await churchConfiguration(owner.churchId)).name, qrPayload: `CMF1:${token}` };
});

export const verifyQr = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const payload = clean(request.data?.qrPayload, "QR token", 160);
  const match = /^CMF1:([a-f0-9]{64})$/.exec(payload);
  if (!match) return { valid: false, message: "Invalid or expired member QR." };
  const tokenHash = hash(match[1]);
  const tokenRef = db.collection("qrVerifications").doc(tokenHash);
  const tokenDoc = await tokenRef.get();
  if (!tokenDoc.exists || tokenDoc.get("status") !== "ACTIVE") return { valid: false, message: "Invalid, expired, or revoked member QR." };
  const churchId = String(tokenDoc.get("churchId"));
  await authorized(uid, churchId, qrVerifierRoles);
  const memberDoc = await db.collection("members").doc(String(tokenDoc.get("memberId"))).get();
  if (!memberDoc.exists || memberDoc.get("status") !== "ACTIVE" || memberDoc.get("activeQrHash") !== tokenHash) return { valid: false, message: "Membership is inactive or this QR has expired." };
  return { valid: true, memberName: memberDoc.get("name"), memberNumber: memberDoc.get("memberNumber"), message: "Active member ID." };
});

export const registerDeviceToken = onCall({ region }, async (request) => {
  const uid = authUid(request);
  const user = await profile(uid);
  const token = clean(request.data?.token, "Device token", 4096);
  if (token.length < 20) throw new HttpsError("invalid-argument", "Invalid notification token.");
  const tokenId = hash(token);
  await db.collection("users").doc(uid).collection("devices").doc(tokenId).set({ token, churchId: user.churchId, updatedAt: FieldValue.serverTimestamp(), platform: "ANDROID" }, { merge: true });
  return { registered: true };
});

export const deliverPushNotification = onDocumentCreated({ document: "notifications/{notificationId}", region, retry: true }, async (event) => {
  const data = event.data?.data();
  if (!data || typeof data.recipientUserId !== "string") return;
  const devices = await db.collection("users").doc(data.recipientUserId).collection("devices").get();
  const tokens = devices.docs.map((doc) => ({ id: doc.id, token: doc.get("token") })).filter((x): x is { id: string; token: string } => typeof x.token === "string");
  if (!tokens.length) return;
  const notificationId = event.params.notificationId;
  const failures: string[] = [];
  await Promise.all(tokens.map(async (device) => {
    const deliveryRef = db.collection("notifications").doc(notificationId).collection("pushDeliveries").doc(device.id);
    const claimed = await db.runTransaction(async (tx) => {
      const current = await tx.get(deliveryRef);
      const lease = current.get("leaseUntil") as Timestamp | undefined;
      if (current.get("status") === "SENT") return false;
      if (current.get("status") === "IN_PROGRESS" && lease && lease.toMillis() > Date.now()) return false;
      tx.set(deliveryRef, {
        status: "IN_PROGRESS", attempts: FieldValue.increment(1),
        leaseUntil: Timestamp.fromMillis(Date.now() + 120_000), updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      return true;
    });
    if (!claimed) return;
    try {
      const messageId = await getMessaging().send({
        token: device.token,
        notification: { title: String(data.title || "Church Connect"), body: String(data.body || "You have a new church update.") },
        data: { notificationId, category: String(data.category || "GENERAL"), churchId: String(data.churchId || "") },
        android: { notification: { tag: notificationId } },
      });
      await deliveryRef.set({ status: "SENT", messageId, sentAt: FieldValue.serverTimestamp(), leaseUntil: null, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
    } catch (error) {
      const code = typeof error === "object" && error && "code" in error ? String(error.code) : "unknown";
      await deliveryRef.set({ status: "FAILED", failureCode: code, leaseUntil: null, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      if (["messaging/invalid-registration-token", "messaging/registration-token-not-registered"].includes(code)) {
        await db.collection("users").doc(data.recipientUserId as string).collection("devices").doc(device.id).delete();
      } else failures.push(code);
    }
  }));
  if (failures.length) throw new Error(`Push delivery failed for ${failures.length} device(s).`);
});
