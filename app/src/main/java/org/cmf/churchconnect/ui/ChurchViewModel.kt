package org.cmf.churchconnect.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.cmf.churchconnect.data.ChurchRepository
import org.cmf.churchconnect.domain.*

class ChurchViewModel(private val repository: ChurchRepository) : ViewModel() {
    var state by mutableStateOf(AppState(isDemo = repository.isDemo))
        private set

    init { viewModelScope.launch { runCatching { repository.currentUser() }.getOrNull()?.let { loadUser(it) } } }

    fun signIn(email: String, password: String, fullName: String? = null) = perform {
        val account = repository.signIn(email, password, fullName)
        loadUser(account)
    }
    fun demoSignIn(asAdmin: Boolean) = perform { loadUser(repository.demoSignIn(asAdmin)) }
    fun signOut() = perform {
        repository.signOut()
        state = AppState(isDemo = repository.isDemo)
    }
    fun sendPasswordReset(email: String) = perform {
        require(email.contains("@")) { "Enter your account email first." }
        repository.sendPasswordReset(email)
        state = state.copy(message = "If the address is registered, a password-reset email has been sent.")
    }
    fun enablePushNotifications() = perform {
        repository.enablePushNotifications()
        state = state.copy(message = "Push notifications are enabled for this device.")
    }
    fun select(tab: String) = perform { state = state.copy(tab = tab, error = null); refreshSelected(tab) }
    fun refresh() = perform { refreshAll() }
    fun createRegistration(draft: RegistrationDraft) = perform {
        RegistrationRules.validate(draft)?.let { error(it) }
        val created = repository.createRegistration(draft)
        state = state.copy(registration = created, tab = "membership", message = "Application saved. Complete the payment verification step below.")
    }
    fun submitPaymentReference(reference: String) = perform {
        val app = requireNotNull(state.registration) { "Create an application first." }
        repository.submitPaymentReference(app.id, reference)
        state = state.copy(registration = repository.myRegistration(), message = "Payment reference sent for finance verification.")
    }
    fun submitRegistration() = perform {
        val app = requireNotNull(state.registration) { "Create an application first." }
        repository.submitRegistration(app.id)
        state = state.copy(registration = repository.myRegistration(), message = "Application submitted for church review.")
    }
    fun loadDigitalId() = perform { state = state.copy(digitalId = repository.getDigitalId()) }
    fun refreshAdmin() = perform { state = state.copy(adminQueue = repository.adminQueue()) }
    fun beginReview(id: String) = perform {
        repository.beginReview(id)
        state = state.copy(adminQueue = repository.adminQueue(), message = "Application moved into review.")
    }
    fun verifyPayment(id: String, reference: String) = perform {
        repository.verifyManualPayment(id, reference)
        state = state.copy(adminQueue = repository.adminQueue(), message = "Manual transfer recorded as verified by finance.")
    }
    fun review(id: String, approve: Boolean, reason: String? = null) = perform {
        repository.reviewRegistration(id, approve, reason)
        state = state.copy(adminQueue = repository.adminQueue(), message = if (approve) "Membership approved; member number assigned." else "Application rejected with reason.")
    }
    fun verifyQr(payload: String) = perform { state = state.copy(verifiedQr = repository.verifyQr(payload.trim())) }
    fun markRead(id: String) = perform {
        repository.markNotificationRead(id)
        state = state.copy(notifications = repository.notifications())
    }
    fun dismissMessage() { state = state.copy(message = null, error = null) }

    private suspend fun loadUser(account: UserProfile) {
        state = state.copy(user = account, isDemo = repository.isDemo, tab = "home", error = null)
        refreshAll()
    }
    private suspend fun refreshAll() {
        val user = state.user ?: return
        val config = repository.churchConfig()
        val events = repository.events()
        val announcements = repository.announcements()
        val registration = repository.myRegistration()
        val notifications = repository.notifications()
        val digitalId = if (user.memberNumber != null || registration?.status == "APPROVED") runCatching { repository.getDigitalId() }.getOrNull() else null
        val queue = if (user.isAdmin) runCatching { repository.adminQueue() }.getOrDefault(emptyList()) else emptyList()
        state = state.copy(config = config, events = events, announcements = announcements, registration = registration, notifications = notifications, digitalId = digitalId, adminQueue = queue)
    }
    private suspend fun refreshSelected(tab: String) {
        when (tab) {
            "home", "calendar", "news" -> {
                state = state.copy(config = repository.churchConfig(), events = repository.events(), announcements = repository.announcements())
            }
            "membership" -> state = state.copy(config = repository.churchConfig(), registration = repository.myRegistration())
            "id" -> state = state.copy(digitalId = runCatching { repository.getDigitalId() }.getOrNull())
            "admin" -> state = state.copy(adminQueue = repository.adminQueue())
            "notifications" -> state = state.copy(notifications = repository.notifications())
        }
    }
    private fun perform(block: suspend () -> Unit) {
        if (state.loading) return
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try { block() } catch (t: Throwable) {
                state = state.copy(error = t.message?.substringBefore(" (" )?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.")
            } finally { state = state.copy(loading = false) }
        }
    }
}
