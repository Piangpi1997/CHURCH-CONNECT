package org.cmf.churchconnect.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminQueueFiltersTest {
    private val applications = listOf(
        MemberApplication("app-1", "INDIVIDUAL", "PENDING_PAYMENT", "Ada Member", "+60123456789", "Setapak", emptyList(), 2000, "MYR", "PENDING"),
        MemberApplication("app-2", "FAMILY", "UNDER_REVIEW", "Ben Member", "", "", listOf("Lina Member"), 5000, "MYR", "PAID", "BANK-5002"),
        MemberApplication("app-3", "INDIVIDUAL", "APPROVED", "Cyd Member", "", "", emptyList(), 2000, "MYR", "PAID"),
        MemberApplication("app-4", "INDIVIDUAL", "REJECTED", "Dee Member", "", "", emptyList(), 2000, "MYR", "PAID", rejectionReason = "Please update your address"),
        MemberApplication("app-5", "INDIVIDUAL", "PAYMENT_VERIFIED", "Eli Member", "", "", emptyList(), 2000, "MYR", "PAID")
    )

    @Test fun statusFiltersSeparatePaymentReviewAndCompletedQueues() {
        assertEquals(listOf("app-1"), AdminQueueFilters.filter(applications, "", AdminQueueFilter.PAYMENT).map { it.id })
        assertEquals(listOf("app-5"), AdminQueueFilters.filter(applications, "", AdminQueueFilter.SUBMITTED).map { it.id })
        assertEquals(listOf("app-2"), AdminQueueFilters.filter(applications, "", AdminQueueFilter.IN_REVIEW).map { it.id })
    }

    @Test fun queryMatchesApplicantContactReferenceHouseholdAndApplicationIdCaseInsensitively() {
        assertEquals("app-1", AdminQueueFilters.filter(applications, "ADA", AdminQueueFilter.ALL).single().id)
        assertEquals("app-1", AdminQueueFilters.filter(applications, "+60123456789", AdminQueueFilter.ALL).single().id)
        assertEquals("app-2", AdminQueueFilters.filter(applications, "bank-5002", AdminQueueFilter.ALL).single().id)
        assertEquals("app-2", AdminQueueFilters.filter(applications, "Lina", AdminQueueFilter.ALL).single().id)
        assertEquals("app-4", AdminQueueFilters.filter(applications, "app-4", AdminQueueFilter.ALL).single().id)
    }

    @Test fun queryAndStatusFilterCanBeCombinedAndTrimWhitespace() {
        assertEquals("app-2", AdminQueueFilters.filter(applications, "  ben ", AdminQueueFilter.IN_REVIEW).single().id)
        assertEquals(emptyList<MemberApplication>(), AdminQueueFilters.filter(applications, "ben", AdminQueueFilter.PAYMENT))
    }
}
