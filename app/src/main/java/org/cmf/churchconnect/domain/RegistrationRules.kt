package org.cmf.churchconnect.domain

object RegistrationRules {
    val allowedTransitions = mapOf(
        "DRAFT" to setOf("PENDING_PAYMENT"),
        "PENDING_PAYMENT" to setOf("PAYMENT_VERIFIED"),
        "PAYMENT_VERIFIED" to setOf("SUBMITTED"),
        "SUBMITTED" to setOf("UNDER_REVIEW"),
        "UNDER_REVIEW" to setOf("APPROVED", "REJECTED")
    )

    fun canTransition(from: String, to: String): Boolean = to in allowedTransitions[from].orEmpty()

    fun validate(draft: RegistrationDraft): String? {
        if (draft.applicantName.trim().length < 2) return "Enter the applicant's full name."
        if (draft.phone.trim().length < 7) return "Enter a valid contact phone number."
        if (draft.address.trim().length < 5) return "Enter a complete mailing address."
        if (draft.type !in setOf("INDIVIDUAL", "FAMILY")) return "Choose an individual or family application."
        if (draft.type == "FAMILY" && draft.familyMembers.none { it.trim().length >= 2 }) return "Add at least one family member."
        if (!draft.privacyConsent) return "Privacy consent is required before submission."
        return null
    }

    fun canApprove(role: String, churchMatches: Boolean, paymentVerified: Boolean): Boolean =
        role in setOf("SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN") && churchMatches && paymentVerified

    fun canVerifyPayment(role: String, churchMatches: Boolean): Boolean =
        role in setOf("SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN", "FINANCE_ADMIN") && churchMatches

    fun memberNumber(prefix: String, sequence: Long): String {
        require(prefix.matches(Regex("[A-Z0-9-]{1,12}"))) { "Invalid member number prefix" }
        require(sequence > 0) { "Sequence must be positive" }
        return "$prefix-${sequence.toString().padStart(6, '0')}"
    }
}
