package org.cmf.churchconnect.domain

import org.junit.Assert.*
import org.junit.Test

class RegistrationRulesTest {
    private val individual = RegistrationDraft("INDIVIDUAL", "Ada Example", "+60123456789", "Setapak, Kuala Lumpur", emptyList(), true)

    @Test fun validIndividualPassesAndMissingConsentFails() {
        assertNull(RegistrationRules.validate(individual))
        assertNotNull(RegistrationRules.validate(individual.copy(privacyConsent = false)))
    }
    @Test fun familyRequiresAtLeastOneAdditionalMember() {
        assertNotNull(RegistrationRules.validate(individual.copy(type = "FAMILY")))
        assertNull(RegistrationRules.validate(individual.copy(type = "FAMILY", familyMembers = listOf("Lin Example"))))
    }
    @Test fun paymentAndApprovalTransitionsAreConstrained() {
        assertTrue(RegistrationRules.canTransition("PENDING_PAYMENT", "PAYMENT_VERIFIED"))
        assertFalse(RegistrationRules.canTransition("PENDING_PAYMENT", "APPROVED"))
        assertFalse(RegistrationRules.canTransition("APPROVED", "REJECTED"))
    }
    @Test fun onlyCorrectRolesAndVerifiedPaymentsCanApprove() {
        assertTrue(RegistrationRules.canApprove("CHURCH_ADMIN", true, true))
        assertFalse(RegistrationRules.canApprove("MEMBER", true, true))
        assertFalse(RegistrationRules.canApprove("CHURCH_ADMIN", false, true))
        assertFalse(RegistrationRules.canApprove("PASTOR", true, false))
        assertTrue(RegistrationRules.canVerifyPayment("FINANCE_ADMIN", true))
        assertFalse(RegistrationRules.canVerifyPayment("MEMBER", true))
    }
    @Test fun memberNumbersAreFormattedAndRejectInvalidSequence() {
        assertEquals("CMF-000001", RegistrationRules.memberNumber("CMF", 1))
        assertEquals("CMF-1000000", RegistrationRules.memberNumber("CMF", 1_000_000))
        assertThrows(IllegalArgumentException::class.java) { RegistrationRules.memberNumber("CMF", 0) }
    }
}
