package org.cmf.churchconnect.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import org.cmf.churchconnect.BuildConfig
import org.cmf.churchconnect.domain.*

class FirebaseChurchRepository : ChurchRepository {
    override val isDemo = false
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("asia-southeast1")
    private val churchId get() = BuildConfig.CHURCH_ID

    init {
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            auth.useEmulator("10.0.2.2", 9099)
            db.useEmulator("10.0.2.2", 8080)
            functions.useEmulator("10.0.2.2", 5001)
        }
    }

    override suspend fun signIn(email: String, password: String, fullName: String?): UserProfile {
        if (fullName.isNullOrBlank()) auth.signInWithEmailAndPassword(email.trim(), password).await()
        else auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val profile = call("ensureProfile", mapOf("fullName" to (fullName ?: auth.currentUser?.displayName.orEmpty())))
        return profileFrom(profile)
    }
    override suspend fun demoSignIn(asAdmin: Boolean): UserProfile = error("Demo mode is disabled when Firebase is connected.")
    override suspend fun signOut() { auth.signOut() }
    override suspend fun sendPasswordReset(email: String) { auth.sendPasswordResetEmail(email.trim()).await() }
    override suspend fun enablePushNotifications() {
        val token = FirebaseMessaging.getInstance().token.await()
        call("registerDeviceToken", mapOf("token" to token))
    }
    override suspend fun currentUser(): UserProfile? {
        val current = auth.currentUser ?: return null
        val doc = db.collection("users").document(current.uid).get().await()
        if (!doc.exists()) return null
        return UserProfile(current.uid, doc.getString("fullName") ?: "Member", current.email.orEmpty(), doc.getString("churchId") ?: churchId, doc.getString("role") ?: "MEMBER", doc.getString("memberNumber"))
    }
    override suspend fun churchConfig(): ChurchConfig {
        val d = call("getChurchConfig", emptyMap())
        return ChurchConfig(churchId, d["name"] as? String ?: "Christ Mission Fellowship", d["location"] as? String ?: "Setapak, Kuala Lumpur", d["currency"] as? String ?: "MYR", (d["individualFee"] as? Number)?.toLong() ?: 2000L, (d["familyFee"] as? Number)?.toLong() ?: 5000L, d["timezone"] as? String ?: "Asia/Kuala_Lumpur")
    }
    override suspend fun events(): List<ChurchEvent> = db.collection("events").whereEqualTo("churchId", churchId).get().await().documents
        .filter { it.getBoolean("published") == true }.map { d -> ChurchEvent(d.id, d.getString("title").orEmpty(), d.getString("description").orEmpty(), d.getString("location").orEmpty(), (d.getTimestamp("startsAt")?.toDate()?.time ?: 0L)) }.sortedBy { it.startsAt }
    override suspend fun announcements(): List<Announcement> = db.collection("announcements").whereEqualTo("churchId", churchId).get().await().documents
        .filter { it.getBoolean("published") == true }.map { d -> Announcement(d.id, d.getString("title").orEmpty(), d.getString("body").orEmpty(), d.getTimestamp("publishedAt")?.toDate()?.time ?: 0L, d.getString("priority") ?: "NORMAL") }.sortedByDescending { it.publishedAt }
    override suspend fun notifications(): List<ChurchNotification> {
        val uid = requireUid()
        return db.collection("notifications").whereEqualTo("recipientUserId", uid).get().await().documents.map { d -> ChurchNotification(d.id, d.getString("title").orEmpty(), d.getString("body").orEmpty(), d.getTimestamp("createdAt")?.toDate()?.time ?: 0L, d.getTimestamp("readAt") != null) }.sortedByDescending { it.createdAt }
    }
    override suspend fun myRegistration(): MemberApplication? {
        val uid = requireUid()
        val docs = db.collection("registrations").whereEqualTo("userId", uid).get().await().documents
        return docs.map(::applicationFrom).maxByOrNull { it.createdAt }
    }
    override suspend fun createRegistration(draft: RegistrationDraft): MemberApplication {
        val result = call("createRegistration", mapOf("type" to draft.type, "applicantName" to draft.applicantName.trim(), "phone" to draft.phone.trim(), "address" to draft.address.trim(), "familyMembers" to draft.familyMembers, "privacyConsent" to draft.privacyConsent))
        val id = result["registrationId"] as? String ?: error("The server did not return an application ID.")
        return db.collection("registrations").document(id).get().await().let(::applicationFrom)
    }
    override suspend fun submitPaymentReference(registrationId: String, reference: String) { call("submitPaymentReference", mapOf("registrationId" to registrationId, "paymentReference" to reference.trim())) }
    override suspend fun submitRegistration(registrationId: String) { call("submitRegistration", mapOf("registrationId" to registrationId)) }
    override suspend fun getDigitalId(): MemberCard {
        val d = call("getDigitalId", emptyMap())
        return MemberCard(d["name"] as? String ?: "Member", d["memberNumber"] as? String ?: "", d["membershipType"] as? String ?: "Individual", d["churchName"] as? String ?: "Christ Mission Fellowship", d["qrPayload"] as? String ?: error("Secure QR token missing."))
    }
    override suspend fun adminQueue(): List<MemberApplication> {
        val rows = call("getAdminQueue", emptyMap())["applications"] as? List<*> ?: emptyList<Any>()
        return rows.mapNotNull { row -> (row as? Map<*, *>)?.let(::applicationFromMap) }.sortedBy { it.createdAt }
    }
    override suspend fun beginReview(registrationId: String) { call("beginRegistrationReview", mapOf("registrationId" to registrationId)) }
    override suspend fun verifyManualPayment(registrationId: String, reference: String) { call("verifyManualPayment", mapOf("registrationId" to registrationId, "paymentReference" to reference.trim())) }
    override suspend fun reviewRegistration(registrationId: String, approve: Boolean, reason: String?) { call("reviewRegistration", mapOf("registrationId" to registrationId, "decision" to if (approve) "APPROVE" else "REJECT", "reason" to (reason ?: ""))) }
    override suspend fun verifyQr(payload: String): QrVerification {
        val d = call("verifyQr", mapOf("qrPayload" to payload.trim()))
        return QrVerification(d["valid"] == true, d["memberName"] as? String, d["memberNumber"] as? String, d["message"] as? String ?: "Verification complete.")
    }
    override suspend fun markNotificationRead(notificationId: String) { db.collection("notifications").document(notificationId).update("readAt", com.google.firebase.firestore.FieldValue.serverTimestamp()).await() }

    private suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?> {
        val raw = functions.getHttpsCallable(name).call(data).await().getData()
        @Suppress("UNCHECKED_CAST")
        return raw as? Map<String, Any?> ?: emptyMap()
    }
    private fun requireUid() = auth.currentUser?.uid ?: error("Please sign in again.")
    private fun profileFrom(d: Map<String, Any?>): UserProfile = UserProfile(requireUid(), d["fullName"] as? String ?: "Member", auth.currentUser?.email.orEmpty(), d["churchId"] as? String ?: churchId, d["role"] as? String ?: "MEMBER", d["memberNumber"] as? String)
    private fun applicationFrom(d: com.google.firebase.firestore.DocumentSnapshot): MemberApplication = MemberApplication(
        id = d.id, type = d.getString("type") ?: "INDIVIDUAL", status = d.getString("status") ?: "UNKNOWN", applicantName = d.getString("applicantName").orEmpty(),
        phone = d.getString("phone").orEmpty(), address = d.getString("address").orEmpty(), familyMembers = (d.get("familyMembers") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        amount = (d.getLong("amount") ?: 0L), currency = d.getString("currency") ?: "MYR", paymentStatus = d.getString("paymentStatus") ?: "PENDING",
        paymentReference = d.getString("paymentReference"), rejectionReason = d.getString("rejectionReason"), createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
    )
    private fun applicationFromMap(d: Map<*, *>): MemberApplication = MemberApplication(
        id = d["id"] as? String ?: "", type = d["type"] as? String ?: "INDIVIDUAL", status = d["status"] as? String ?: "UNKNOWN",
        applicantName = d["applicantName"] as? String ?: "", phone = d["phone"] as? String ?: "", address = d["address"] as? String ?: "",
        familyMembers = (d["familyMembers"] as? List<*>)?.filterIsInstance<String>().orEmpty(), amount = (d["amount"] as? Number)?.toLong() ?: 0L,
        currency = d["currency"] as? String ?: "MYR", paymentStatus = d["paymentStatus"] as? String ?: "PENDING",
        paymentReference = d["paymentReference"] as? String, rejectionReason = d["rejectionReason"] as? String,
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(), ownerUid = d["userId"] as? String
    )
}
