package org.cmf.churchconnect.domain

import java.util.Locale

enum class AdminQueueFilter(val label: String) {
    ALL("All"),
    PAYMENT("Payment"),
    SUBMITTED("Submitted"),
    IN_REVIEW("In review")
}

object AdminQueueFilters {
    fun filter(
        applications: List<MemberApplication>,
        query: String,
        statusFilter: AdminQueueFilter
    ): List<MemberApplication> {
        val needle = query.trim().lowercase(Locale.ROOT)
        return applications.filter { app ->
            val statusMatches = when (statusFilter) {
                AdminQueueFilter.ALL -> true
                AdminQueueFilter.PAYMENT -> app.paymentStatus.uppercase(Locale.ROOT) in setOf("PENDING", "PROCESSING", "FAILED", "CANCELLED")
                AdminQueueFilter.SUBMITTED -> app.status.uppercase(Locale.ROOT) in setOf("SUBMITTED", "PAYMENT_VERIFIED")
                AdminQueueFilter.IN_REVIEW -> app.status.uppercase(Locale.ROOT) == "UNDER_REVIEW"
            }
            val searchableText = listOf(
                app.id, app.type, app.status, app.applicantName, app.phone, app.address,
                app.paymentStatus, app.paymentReference.orEmpty(), app.rejectionReason.orEmpty()
            ).plus(app.familyMembers).joinToString(" ").lowercase(Locale.ROOT)
            statusMatches && (needle.isEmpty() || searchableText.contains(needle))
        }
    }
}
