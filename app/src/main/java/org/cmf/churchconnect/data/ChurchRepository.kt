package org.cmf.churchconnect.data

import org.cmf.churchconnect.domain.*

interface ChurchRepository {
    val isDemo: Boolean
    suspend fun signIn(email: String, password: String, fullName: String? = null): UserProfile
    suspend fun demoSignIn(asAdmin: Boolean): UserProfile
    suspend fun signOut()
    suspend fun sendPasswordReset(email: String)
    suspend fun enablePushNotifications()
    suspend fun currentUser(): UserProfile?
    suspend fun churchConfig(): ChurchConfig
    suspend fun events(): List<ChurchEvent>
    suspend fun announcements(): List<Announcement>
    suspend fun notifications(): List<ChurchNotification>
    suspend fun myRegistration(): MemberApplication?
    suspend fun createRegistration(draft: RegistrationDraft): MemberApplication
    suspend fun submitPaymentReference(registrationId: String, reference: String)
    suspend fun submitRegistration(registrationId: String)
    suspend fun getDigitalId(): MemberCard
    suspend fun adminQueue(): List<MemberApplication>
    suspend fun beginReview(registrationId: String)
    suspend fun verifyManualPayment(registrationId: String, reference: String)
    suspend fun reviewRegistration(registrationId: String, approve: Boolean, reason: String? = null)
    suspend fun verifyQr(payload: String): QrVerification
    suspend fun markNotificationRead(notificationId: String)
}
