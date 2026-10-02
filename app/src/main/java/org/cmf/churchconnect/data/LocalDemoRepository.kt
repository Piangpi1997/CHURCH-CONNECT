package org.cmf.churchconnect.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cmf.churchconnect.domain.*
import java.util.Calendar
import java.util.UUID

/** In-memory preview only. It is never selected when a Firebase app is configured. */
class LocalDemoRepository : ChurchRepository {
    override val isDemo = true
    private val lock = Mutex()
    private var user: UserProfile? = null
    private var sequence = 12L
    private val applications = mutableListOf<MemberApplication>()
    private val notices = mutableListOf<ChurchNotification>()
    private val cardByUser = mutableMapOf<String, MemberCard>()
    private val eventList = listOf(
        ChurchEvent("demo-sunday", "Sunday Worship Service", "Worship, teaching and fellowship. Visitors are welcome.", "CMF Church · Setapak", demoDate(0, 9, 30)),
        ChurchEvent("demo-prayer", "Midweek Prayer Gathering", "A time to pray together for our church and community.", "Main hall", demoDate(3, 20, 0)),
        ChurchEvent("demo-youth", "Youth Fellowship", "Games, worship and a short Bible reflection.", "Fellowship room", demoDate(6, 16, 0))
    )
    private val newsList = listOf(
        Announcement("demo-welcome", "Welcome to Church Connect", "This preview uses sample content. Connect Firebase to load official church announcements.", System.currentTimeMillis(), "INFO"),
        Announcement("demo-service", "Sunday service", "Join us this Sunday at 9:30 AM in Setapak. All are welcome.", System.currentTimeMillis() - 3_600_000, "NORMAL")
    )

    override suspend fun signIn(email: String, password: String, fullName: String?): UserProfile = lock.withLock {
        require(email.contains("@") && password.length >= 6) { "Enter a valid email and a password of at least 6 characters." }
        val displayName = fullName?.trim()?.takeIf { it.isNotEmpty() } ?: email.substringBefore('@').replace('.', ' ').replaceFirstChar { it.uppercase() }
        val account = UserProfile("demo-${UUID.randomUUID()}", displayName, email, "cmf-setapak")
        user = account
        account
    }

    override suspend fun demoSignIn(asAdmin: Boolean): UserProfile = lock.withLock {
        val uid = if (asAdmin) "demo-admin" else "demo-member"
        val account = UserProfile(
            uid = uid,
            fullName = if (asAdmin) "Demo Church Admin" else "Demo Member",
            email = if (asAdmin) "admin@example.invalid" else "member@example.invalid",
            churchId = "cmf-setapak",
            role = if (asAdmin) "CHURCH_ADMIN" else "MEMBER",
            memberNumber = cardByUser[uid]?.number
        )
        user = account
        account
    }

    override suspend fun signOut() = lock.withLock { user = null }
    override suspend fun sendPasswordReset(email: String) { error("Password reset is unavailable in local demo mode.") }
    override suspend fun enablePushNotifications() { error("Push notifications are unavailable in local demo mode.") }
    override suspend fun currentUser(): UserProfile? = lock.withLock { user }
    override suspend fun churchConfig() = ChurchConfig("cmf-setapak", "Christ Mission Fellowship", "Setapak, Kuala Lumpur", "MYR", 2000, 5000)
    override suspend fun events() = eventList
    override suspend fun announcements() = newsList
    override suspend fun notifications(): List<ChurchNotification> = lock.withLock {
        notices.filter { it.recipientUid == user?.uid }.sortedByDescending { it.createdAt }
    }
    override suspend fun myRegistration(): MemberApplication? = lock.withLock { applications.lastOrNull { it.ownerUid == user?.uid } }

    override suspend fun createRegistration(draft: RegistrationDraft): MemberApplication = lock.withLock {
        val current = user ?: error("Sign in to apply.")
        RegistrationRules.validate(draft)?.let { error(it) }
        check(applications.none { it.ownerUid == current.uid && it.status !in setOf("APPROVED", "REJECTED") }) { "You already have an active application." }
        val config = churchConfig()
        val app = MemberApplication(
            id = "demo-reg-${UUID.randomUUID()}", type = draft.type, status = "PENDING_PAYMENT",
            applicantName = draft.applicantName.trim(), phone = draft.phone.trim(), address = draft.address.trim(),
            familyMembers = draft.familyMembers.map { it.trim() }.filter { it.isNotBlank() },
            amount = if (draft.type == "FAMILY") config.familyFee else config.individualFee,
            currency = config.currency, paymentStatus = "PENDING", createdAt = System.currentTimeMillis(), ownerUid = current.uid
        )
        applications.add(app)
        app
    }

    override suspend fun submitPaymentReference(registrationId: String, reference: String) = lock.withLock {
        val app = findApp(registrationId)
        check(app.ownerUid == user?.uid) { "This application does not belong to your account." }
        require(reference.trim().length >= 4) { "Enter the bank transfer reference." }
        check(app.status == "PENDING_PAYMENT") { "This application is not awaiting payment." }
        replace(app.copy(paymentStatus = "PROCESSING", paymentReference = reference.trim()))
        notify(app.ownerUid.orEmpty(), "Payment reference received", "A finance admin must verify the transfer before you can submit your application.")
    }

    override suspend fun submitRegistration(registrationId: String) = lock.withLock {
        val app = findApp(registrationId)
        check(app.ownerUid == user?.uid) { "This application does not belong to your account." }
        check(app.status == "PAYMENT_VERIFIED" && app.paymentStatus == "PAID") { "Payment must be verified before submission." }
        replace(app.copy(status = "SUBMITTED"))
        notify(app.ownerUid.orEmpty(), "Application submitted", "Your membership application is now in the review queue.")
    }

    override suspend fun getDigitalId(): MemberCard = lock.withLock {
        val current = user ?: error("Sign in to continue.")
        val card = cardByUser[current.uid] ?: error("A digital ID is available after your application is approved.")
        val refreshed = card.copy(qrPayload = "CMF1:${UUID.randomUUID()}")
        cardByUser[current.uid] = refreshed
        refreshed
    }

    override suspend fun adminQueue(): List<MemberApplication> = lock.withLock {
        require(user?.isAdmin == true) { "Admin access required." }
        applications.filter { it.status in setOf("PENDING_PAYMENT", "PAYMENT_VERIFIED", "SUBMITTED", "UNDER_REVIEW") }.sortedBy { it.createdAt }
    }

    override suspend fun beginReview(registrationId: String) = lock.withLock {
        require(user?.canReview == true) { "Church admin access required." }
        val app = findApp(registrationId)
        check(app.status == "SUBMITTED") { "Only submitted applications can enter review." }
        replace(app.copy(status = "UNDER_REVIEW"))
    }

    override suspend fun verifyManualPayment(registrationId: String, reference: String) = lock.withLock {
        require(user?.canVerifyPayments == true) { "Finance admin access required." }
        val app = findApp(registrationId)
        check(app.paymentStatus == "PROCESSING" && app.paymentReference == reference.trim()) { "Payment reference does not match the applicant's submitted reference." }
        replace(app.copy(paymentStatus = "PAID", status = "PAYMENT_VERIFIED"))
        notify(app.ownerUid.orEmpty(), "Payment verified", "Your payment was simulated as verified in this demo. You may now submit your application.")
    }

    override suspend fun reviewRegistration(registrationId: String, approve: Boolean, reason: String?) = lock.withLock {
        require(user?.canReview == true) { "Church admin access required." }
        val app = findApp(registrationId)
        check(app.status == "UNDER_REVIEW") { "Start the review before deciding." }
        if (approve) {
            check(app.paymentStatus == "PAID") { "Payment must be verified before approval." }
            sequence += 1
            val number = RegistrationRules.memberNumber("CMF", sequence)
            cardByUser[app.ownerUid.orEmpty()] = MemberCard(app.applicantName, number, app.type, "Christ Mission Fellowship", "CMF1:${UUID.randomUUID()}")
            replace(app.copy(status = "APPROVED"))
            notify(app.ownerUid.orEmpty(), "Membership approved", "Your member number is $number. Your digital ID is ready.")
        } else {
            require(!reason.isNullOrBlank()) { "A reason is required when rejecting an application." }
            replace(app.copy(status = "REJECTED", rejectionReason = reason.trim()))
            notify(app.ownerUid.orEmpty(), "Application update", "Your application was not approved. Please contact the church office for details.")
        }
    }

    override suspend fun verifyQr(payload: String): QrVerification = lock.withLock {
        require(user?.isAdmin == true) { "Authorized church staff must verify member IDs." }
        val card = cardByUser.values.firstOrNull { it.qrPayload == payload.trim() }
        if (card == null) QrVerification(false, message = "Invalid or expired QR token.")
        else QrVerification(true, card.name, card.number, "Active member ID · ${card.membershipType.lowercase().replaceFirstChar { it.uppercase() }}")
    }

    override suspend fun markNotificationRead(notificationId: String) = lock.withLock {
        val index = notices.indexOfFirst { it.id == notificationId && it.recipientUid == user?.uid }
        if (index >= 0) notices[index] = notices[index].copy(read = true)
    }

    private fun findApp(id: String) = applications.firstOrNull { it.id == id } ?: error("Application not found.")
    private fun replace(app: MemberApplication) { val index = applications.indexOfFirst { it.id == app.id }; if (index >= 0) applications[index] = app }
    private fun notify(uid: String, title: String, body: String) {
        notices.add(0, ChurchNotification(UUID.randomUUID().toString(), title, body, System.currentTimeMillis(), false, uid))
    }
    private fun demoDate(daysAhead: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); add(Calendar.DAY_OF_YEAR, daysAhead) }
        if (daysAhead == 0 && cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 7)
        return cal.timeInMillis
    }
}
