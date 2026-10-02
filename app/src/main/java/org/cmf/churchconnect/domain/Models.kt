package org.cmf.churchconnect.domain

data class UserProfile(
    val uid: String,
    val fullName: String,
    val email: String,
    val churchId: String,
    val role: String = "MEMBER",
    val memberNumber: String? = null
) {
    val isAdmin: Boolean get() = role in setOf("SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN", "FINANCE_ADMIN", "USHER")
    val canReview: Boolean get() = role in setOf("SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN")
    val canVerifyPayments: Boolean get() = role in setOf("SUPER_ADMIN", "PASTOR", "CHURCH_ADMIN", "FINANCE_ADMIN")
}

data class ChurchConfig(
    val id: String,
    val name: String,
    val location: String,
    val currency: String,
    val individualFee: Long,
    val familyFee: Long,
    val timezone: String = "Asia/Kuala_Lumpur"
) {
    fun formattedFee(sen: Long): String = "${currency} ${(sen / 100)}.${(sen % 100).toString().padStart(2, '0')}"
}

data class ChurchEvent(val id: String, val title: String, val description: String, val location: String, val startsAt: Long)
data class Announcement(val id: String, val title: String, val body: String, val publishedAt: Long, val priority: String = "NORMAL")
data class MemberApplication(
    val id: String,
    val type: String,
    val status: String,
    val applicantName: String,
    val phone: String,
    val address: String,
    val familyMembers: List<String>,
    val amount: Long,
    val currency: String,
    val paymentStatus: String,
    val paymentReference: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerUid: String? = null
)
data class MemberCard(val name: String, val number: String, val membershipType: String, val churchName: String, val qrPayload: String)
data class ChurchNotification(val id: String, val title: String, val body: String, val createdAt: Long, val read: Boolean, val recipientUid: String? = null)
data class QrVerification(val valid: Boolean, val memberName: String? = null, val memberNumber: String? = null, val message: String)
data class RegistrationDraft(
    val type: String,
    val applicantName: String,
    val phone: String,
    val address: String,
    val familyMembers: List<String>,
    val privacyConsent: Boolean
)

data class AppState(
    val user: UserProfile? = null,
    val isDemo: Boolean = false,
    val loading: Boolean = false,
    val tab: String = "home",
    val error: String? = null,
    val message: String? = null,
    val config: ChurchConfig? = null,
    val events: List<ChurchEvent> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val notifications: List<ChurchNotification> = emptyList(),
    val registration: MemberApplication? = null,
    val digitalId: MemberCard? = null,
    val adminQueue: List<MemberApplication> = emptyList(),
    val verifiedQr: QrVerification? = null
)
