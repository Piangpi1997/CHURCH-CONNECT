package org.cmf.churchconnect.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.cmf.churchconnect.AppLocale
import org.cmf.churchconnect.R
import org.cmf.churchconnect.data.ChurchRepository
import org.cmf.churchconnect.domain.*

class ChurchViewModel(private val repository: ChurchRepository, application: Application) : AndroidViewModel(application) {
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
        require(email.contains("@")) { text(R.string.email_required) }
        repository.sendPasswordReset(email)
        state = state.copy(message = text(R.string.password_reset_sent))
    }
    fun enablePushNotifications() = perform {
        repository.enablePushNotifications()
        state = state.copy(message = text(R.string.push_enabled))
    }
    fun select(tab: String) = perform { state = state.copy(tab = tab, error = null); refreshSelected(tab) }
    fun refresh() = perform { refreshAll() }
    fun createRegistration(draft: RegistrationDraft) = perform {
        RegistrationRules.validate(draft)?.let { error(localizeValidation(it)) }
        val created = repository.createRegistration(draft)
        state = state.copy(registration = created, tab = "membership", message = text(R.string.application_saved))
    }
    fun submitPaymentReference(reference: String) = perform {
        val app = requireNotNull(state.registration) { "Create an application first." }
        repository.submitPaymentReference(app.id, reference)
        state = state.copy(registration = repository.myRegistration(), message = text(R.string.payment_reference_sent))
    }
    fun submitRegistration() = perform {
        val app = requireNotNull(state.registration) { "Create an application first." }
        repository.submitRegistration(app.id)
        state = state.copy(registration = repository.myRegistration(), message = text(R.string.application_submitted))
    }
    fun loadDigitalId() = perform { state = state.copy(digitalId = repository.getDigitalId()) }
    fun refreshAdmin() = perform { state = state.copy(adminQueue = repository.adminQueue()) }
    fun beginReview(id: String) = perform {
        repository.beginReview(id)
        state = state.copy(adminQueue = repository.adminQueue(), message = text(R.string.application_in_review))
    }
    fun verifyPayment(id: String, reference: String) = perform {
        repository.verifyManualPayment(id, reference)
        state = state.copy(adminQueue = repository.adminQueue(), message = text(R.string.manual_transfer_verified))
    }
    fun review(id: String, approve: Boolean, reason: String? = null) = perform {
        repository.reviewRegistration(id, approve, reason)
        state = state.copy(adminQueue = repository.adminQueue(), message = text(if (approve) R.string.membership_approved else R.string.application_rejected))
    }
    fun verifyQr(payload: String) = perform { state = state.copy(verifiedQr = repository.verifyQr(payload.trim())) }
    fun markRead(id: String) = perform {
        repository.markNotificationRead(id)
        state = state.copy(notifications = repository.notifications())
    }
    fun dismissMessage() { state = state.copy(message = null, error = null) }

    private fun text(resourceId: Int): String = AppLocale.wrap(getApplication<Application>()).getString(resourceId)

    private fun localizeValidation(message: String): String = when (message) {
        "Enter the applicant's full name." -> text(R.string.validation_full_name)
        "Enter a valid contact phone number." -> text(R.string.validation_phone)
        "Enter a complete mailing address." -> text(R.string.validation_address)
        "Choose an individual or family application." -> text(R.string.validation_type)
        "Add at least one family member." -> text(R.string.validation_family_member)
        "Privacy consent is required before submission." -> text(R.string.validation_consent)
        else -> message
    }

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
                state = state.copy(error = t.message?.substringBefore(" (" )?.takeIf { it.isNotBlank() } ?: text(R.string.app_error_fallback))
            } finally { state = state.copy(loading = false) }
        }
    }
}
