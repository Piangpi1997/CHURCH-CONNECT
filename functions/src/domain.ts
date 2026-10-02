export type RegistrationType = "INDIVIDUAL" | "FAMILY";
export type RegistrationStatus = "DRAFT" | "PENDING_PAYMENT" | "PAYMENT_VERIFIED" | "SUBMITTED" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";

const transitions: Record<RegistrationStatus, RegistrationStatus[]> = {
  DRAFT: ["PENDING_PAYMENT"],
  PENDING_PAYMENT: ["PAYMENT_VERIFIED"],
  PAYMENT_VERIFIED: ["SUBMITTED"],
  SUBMITTED: ["UNDER_REVIEW"],
  UNDER_REVIEW: ["APPROVED", "REJECTED"],
  APPROVED: [],
  REJECTED: [],
};

export function canTransition(from: string, to: RegistrationStatus): boolean {
  return (transitions[from as RegistrationStatus] ?? []).includes(to);
}

export function assertTransition(from: string, to: RegistrationStatus): void {
  if (!canTransition(from, to)) throw new Error(`Invalid registration transition: ${from} -> ${to}`);
}

export function calculateFee(type: RegistrationType, fees: { individual: number; family: number; currency?: string }): { amount: number; currency: string; feeType: RegistrationType } {
  const amount = type === "INDIVIDUAL" ? fees.individual : fees.family;
  if (!Number.isSafeInteger(amount) || amount < 0) throw new Error("Church fee configuration must be a non-negative integer in minor currency units.");
  return { amount, currency: fees.currency || "MYR", feeType: type };
}

export function formatMemberNumber(prefix: string, sequence: number): string {
  if (!/^[A-Z0-9-]{1,12}$/.test(prefix)) throw new Error("Invalid member number prefix.");
  if (!Number.isSafeInteger(sequence) || sequence < 1) throw new Error("Member sequence must be a positive safe integer.");
  return `${prefix}-${String(sequence).padStart(6, "0")}`;
}

export function mayPerformRole(role: string, allowed: readonly string[], userChurchId: string, resourceChurchId: string): boolean {
  return allowed.includes(role) && userChurchId === resourceChurchId;
}

export function normalizeReference(value: unknown): string {
  if (typeof value !== "string") throw new Error("A payment reference is required.");
  const reference = value.trim();
  if (reference.length < 4 || reference.length > 100 || /[<>\u0000-\u001f]/.test(reference)) throw new Error("Enter a valid payment reference (4–100 characters).");
  return reference;
}

export function validateApplication(input: Record<string, unknown>): {
  type: RegistrationType; applicantName: string; phone: string; address: string; familyMembers: string[];
} {
  const type = input.type;
  if (type !== "INDIVIDUAL" && type !== "FAMILY") throw new Error("Choose INDIVIDUAL or FAMILY registration.");
  const applicantName = cleanText(input.applicantName, "Applicant name", 2, 120);
  const phone = cleanText(input.phone, "Phone", 7, 32);
  if (!/^[+0-9() .-]+$/.test(phone)) throw new Error("Enter a valid phone number.");
  const address = cleanText(input.address, "Address", 5, 300);
  const rawMembers = input.familyMembers;
  if (!Array.isArray(rawMembers) || rawMembers.length > 12) throw new Error("Family members must be a list with at most 12 names.");
  const familyMembers = rawMembers.map((member) => cleanText(member, "Family member name", 2, 120));
  if (type === "INDIVIDUAL" && familyMembers.length) throw new Error("Individual applications cannot include family members.");
  if (type === "FAMILY" && familyMembers.length === 0) throw new Error("Add at least one family member.");
  if (familyMembers.some((name) => name.toLocaleLowerCase() === applicantName.toLocaleLowerCase())) throw new Error("The applicant should not be repeated in the family-member list.");
  if (input.privacyConsent !== true) throw new Error("Privacy consent is required.");
  return { type, applicantName, phone, address, familyMembers };
}

function cleanText(value: unknown, label: string, min: number, max: number): string {
  if (typeof value !== "string") throw new Error(`${label} is required.`);
  const text = value.trim();
  if (text.length < min || text.length > max || /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/.test(text)) throw new Error(`${label} must be ${min}–${max} characters.`);
  return text;
}
