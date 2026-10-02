package org.cmf.churchconnect.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cmf.churchconnect.R
import org.cmf.churchconnect.AppLocale
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.cmf.churchconnect.domain.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
 fun ChurchConnectApp(vm: ChurchViewModel, onEnablePush: () -> Unit, onLanguageChanged: (String) -> Unit = {}) {
    val state = vm.state
    GlassTheme {
        GlassBackdrop {
            Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
                if (state.user == null) AuthScreen(state, vm, onLanguageChanged) else MainShell(state, vm, onEnablePush, onLanguageChanged)
            }
        }
    }
}

@Composable
private fun AuthScreen(state: AppState, vm: ChurchViewModel, onLanguageChanged: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(top = 54.dp, bottom = 30.dp)) {
            item {
                BrandMark()
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.app_name), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${stringResource(R.string.church_name)} · ${stringResource(R.string.church_location)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.tagline), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(28.dp))
            }
            item { LanguageSelector(onLanguageChanged) }
            item {
                GlassCard(Modifier.fillMaxWidth().widthIn(max = GlassTokens.authMaxWidth), shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingMedium)) {
                        Text(if (creating) stringResource(R.string.auth_create_title) else stringResource(R.string.auth_welcome_title), fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (creating) stringResource(R.string.auth_create_subtitle) else stringResource(R.string.auth_welcome_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        if (creating) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.full_name)) }, singleLine = true)
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.password_requirement)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        if (state.error != null) InlineNotice(state.error, isError = true)
                        GlassButton(onClick = { vm.signIn(email, password, if (creating) name else null) }, Modifier.fillMaxWidth().height(52.dp), enabled = !state.loading) {
                            if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (creating) stringResource(R.string.create_account) else stringResource(R.string.sign_in))
                        }
                        if (!creating && !state.isDemo) TextButton(onClick = { vm.sendPasswordReset(email) }, Modifier.align(Alignment.CenterHorizontally), enabled = !state.loading) { Text(stringResource(R.string.forgot_password)) }
                        if (state.message != null) InlineNotice(state.message, isError = false, onDismiss = vm::dismissMessage)
                        TextButton(onClick = { creating = !creating; vm.dismissMessage() }, Modifier.align(Alignment.CenterHorizontally)) { Text(if (creating) stringResource(R.string.account_exists) else stringResource(R.string.account_new)) }
                        if (state.isDemo) {
                            HorizontalDivider()
                            Text(stringResource(R.string.local_preview), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.3.sp)
                            Text(stringResource(R.string.demo_explanation), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.demoSignIn(false) }, Modifier.weight(1f), enabled = !state.loading) { Text(stringResource(R.string.member_demo)) }
                                OutlinedButton(onClick = { vm.demoSignIn(true) }, Modifier.weight(1f), enabled = !state.loading) { Text(stringResource(R.string.admin_demo)) }
                            }
                        } else {
                            Text(stringResource(R.string.accounts_managed), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.church_full_name), fontSize = 10.sp, letterSpacing = 1.1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(state: AppState, vm: ChurchViewModel, onEnablePush: () -> Unit, onLanguageChanged: (String) -> Unit) {
    val user = state.user ?: return
    val actualTab = if (state.tab in setOf("id", "notifications", "admin")) "more" else state.tab
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = { Box(Modifier.padding(start = 10.dp)) { BrandMark(38.dp) } },
                title = { Column { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold, fontSize = 18.sp); Text(stringResource(R.string.church_name), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                actions = { GlassIconButton(onClick = { vm.refresh() }, enabled = !state.loading) { Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.refresh)) }; GlassIconButton(onClick = { vm.signOut() }) { Icon(Icons.Outlined.Logout, contentDescription = stringResource(R.string.sign_out)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(GlassTokens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f), RoundedCornerShape(26.dp)),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = GlassTokens.elevationCard
            ) {
                listOf(Triple("home", stringResource(R.string.home), Icons.Outlined.Home), Triple("membership", stringResource(R.string.apply), Icons.Outlined.AssignmentInd), Triple("calendar", stringResource(R.string.calendar), Icons.Outlined.Event), Triple("news", stringResource(R.string.news), Icons.Outlined.Campaign), Triple("more", stringResource(R.string.more), Icons.Outlined.MoreHoriz)).forEach { (tab, title, icon) ->
                    NavigationBarItem(selected = actualTab == tab, onClick = { vm.select(tab) }, icon = { Icon(icon, contentDescription = title) }, label = { Text(title) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer, selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.isDemo) DemoBanner()
            if (state.message != null) InlineNotice(state.message, isError = false, onDismiss = vm::dismissMessage)
            if (state.error != null) InlineNotice(state.error, isError = true, onDismiss = vm::dismissMessage)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (state.tab) {
                "home" -> HomeScreen(state, vm)
                "membership" -> MembershipScreen(state, vm)
                "calendar" -> CalendarScreen(state)
                "news" -> NewsScreen(state)
                "id" -> DigitalIdScreen(state, vm)
                "notifications" -> NotificationsScreen(state, vm)
                "admin" -> AdminScreen(state, vm)
                else -> MoreScreen(state, onOpen = vm::select, onEnablePush = onEnablePush, onLanguageChanged = onLanguageChanged)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AppState, vm: ChurchViewModel) {
    val user = state.user ?: return
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingLarge)) {
        item {
            GlassCard(shape = RoundedCornerShape(28.dp), emphasized = true) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(42.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.home_welcome), color = GlassTokens.heroHighlight, fontSize = 11.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.hello_user, user.fullName.substringBefore(' ')), fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(5.dp))
                    Text(stringResource(R.string.home_tagline), color = GlassTokens.heroSecondaryText, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FilledTonalButton(onClick = { vm.select("membership") }) { Icon(Icons.Outlined.PersonAddAlt, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.apply)) }
                        OutlinedButton(onClick = { vm.select("calendar") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Icon(Icons.Outlined.Event, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.events)) }
                    }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.next_gathering), action = stringResource(R.string.see_calendar), onAction = { vm.select("calendar") }) }
        if (state.events.isEmpty()) item { EmptyCard(stringResource(R.string.no_upcoming_events), stringResource(R.string.calendar_will_appear)) }
        else item { EventCard(state.events.first()) }
        item { SectionTitle(stringResource(R.string.stay_connected), action = stringResource(R.string.all_news), onAction = { vm.select("news") }) }
        items(state.announcements.take(2), key = { it.id }) { AnnouncementCard(it) }
        if (state.registration != null) item { ApplicationStatusCard(state.registration, state.config) }
        if (!state.isDemo && state.config == null) item { EmptyCard(stringResource(R.string.church_data_unavailable), stringResource(R.string.connection_retry)) }
    }
}

@Composable
private fun MembershipScreen(state: AppState, vm: ChurchViewModel) {
    val existing = state.registration
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingMedium)) {
        item { PageHeading(stringResource(R.string.apply), stringResource(R.string.apply_join_church)) }
        if (existing == null || existing.status in setOf("APPROVED", "REJECTED")) {
            item { NewApplicationForm(state.config, state.loading, vm) }
        } else {
            item { ApplicationStatusCard(existing, state.config) }
            if (existing.status == "PENDING_PAYMENT" && existing.paymentStatus != "PROCESSING") item { PaymentReferenceCard(existing, state.isDemo, vm) }
            if (existing.status == "PENDING_PAYMENT" && existing.paymentStatus == "PROCESSING") item {
                InfoCard(stringResource(R.string.awaiting_finance_verification), stringResource(R.string.reference_submitted, existing.paymentReference ?: "submitted"), Icons.Outlined.HourglassTop)
            }
            if (existing.status == "PAYMENT_VERIFIED") item {
                ActionCard(stringResource(R.string.payment_verified_title), stringResource(R.string.submit_application_for_review), button = stringResource(R.string.submit_application), loading = state.loading, onClick = { vm.submitRegistration() })
            }
            if (existing.status == "SUBMITTED" || existing.status == "UNDER_REVIEW") item {
                InfoCard(stringResource(R.string.application_under_review), stringResource(R.string.application_submitted_update), Icons.Outlined.FactCheck)
            }
            if (existing.status == "APPROVED") item {
                InfoCard(stringResource(R.string.you_are_member), stringResource(R.string.member_number_value, state.user?.memberNumber ?: stringResource(R.string.member_number_unavailable)), Icons.Outlined.Verified)
                Button(onClick = { vm.select("id") }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_digital_id)) }
            }
            if (existing.status == "REJECTED") item {
                InfoCard(stringResource(R.string.application_update), existing.rejectionReason ?: stringResource(R.string.contact_church_office), Icons.Outlined.Info)
                OutlinedButton(onClick = { vm.refresh() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.refresh_status)) }
            }
        }
        item { InfoCard(stringResource(R.string.about_fee), stringResource(R.string.registration_fee_info), Icons.Outlined.Info) }
    }
}

@Composable
private fun NewApplicationForm(config: ChurchConfig?, loading: Boolean, vm: ChurchViewModel) {
    var type by remember { mutableStateOf("INDIVIDUAL") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var family by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    GlassCard(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingCompact)) {
            Text(stringResource(R.string.new_application), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(stringResource(R.string.choose_application_type), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == "INDIVIDUAL", { type = "INDIVIDUAL" }, label = { Text(stringResource(R.string.individual)) })
                FilterChip(type == "FAMILY", { type = "FAMILY" }, label = { Text(stringResource(R.string.family)) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.registration_fee), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(config?.formattedFee(if (type == "FAMILY") config.familyFee else config.individualFee) ?: "Loading…", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.applicant_full_name)) }, singleLine = true)
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.phone_number)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.home_address)) }, minLines = 2)
            if (type == "FAMILY") OutlinedTextField(family, { family = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.family_members_hint)) }, minLines = 3)
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text(stringResource(R.string.consent_text), Modifier.padding(top = 11.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = {
                vm.createRegistration(RegistrationDraft(type, name, phone, address, family.lines().map(String::trim).filter(String::isNotBlank), consent))
            }, Modifier.fillMaxWidth().height(50.dp), enabled = !loading && config != null, shape = RoundedCornerShape(15.dp)) {
                Text(if (loading) stringResource(R.string.saving) else stringResource(R.string.create_application))
            }
            Text(stringResource(R.string.private_staff_only), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentReferenceCard(app: MemberApplication, demo: Boolean, vm: ChurchViewModel) {
    var reference by remember(app.id) { mutableStateOf("") }
    GlassCard(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(stringResource(R.string.payment_verification), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(stringResource(R.string.fee_payment_line, app.currency, app.amount / 100, app.amount % 100, stringResource(if (demo) R.string.payment_flow_demo else R.string.payment_flow_offline)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (demo) Text(stringResource(R.string.demo_payment_no_money), color = Color(0xFF8A5A00), fontSize = 12.sp)
            OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.transfer_reference)) }, singleLine = true)
            Button(onClick = { vm.submitPaymentReference(reference) }, Modifier.fillMaxWidth(), enabled = reference.trim().length >= 4 && !vm.state.loading) { Text(stringResource(R.string.send_for_verification)) }
            Text(stringResource(R.string.payment_reference_disclaimer), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ApplicationStatusCard(app: MemberApplication, config: ChurchConfig?) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_status), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                StatusPill(localizedApplicationStatus(app.status))
            }
            Text(stringResource(R.string.application_type_applicant, localizedType(app.type), app.applicantName), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.fee_payment_line, app.currency, app.amount / 100, app.amount % 100, localizedPaymentStatus(app.paymentStatus)), fontSize = 13.sp)
            Text(stringResource(R.string.application_number, app.id.takeLast(8).uppercase()), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarScreen(state: AppState) {
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingCompact)) {
        item { PageHeading(stringResource(R.string.church_calendar), stringResource(R.string.gather_grow_serve)) }
        if (state.events.isEmpty()) item { EmptyCard(stringResource(R.string.no_events), stringResource(R.string.new_events)) }
        items(state.events, key = { it.id }) { EventCard(it) }
    }
}

@Composable
private fun EventCard(event: ChurchEvent) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.width(55.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val date = Date(event.startsAt)
                Text(SimpleDateFormat("MMM", Locale.getDefault()).format(date).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(SimpleDateFormat("d", Locale.getDefault()).format(date), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(event.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(event.startsAt)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(event.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(event.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun NewsScreen(state: AppState) {
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingCompact)) {
        item { PageHeading(stringResource(R.string.announcements), stringResource(R.string.church_updates)) }
        if (state.announcements.isEmpty()) item { EmptyCard(stringResource(R.string.no_announcements), stringResource(R.string.official_announcements)) }
        items(state.announcements, key = { it.id }) { AnnouncementCard(it) }
    }
}

@Composable
private fun AnnouncementCard(item: Announcement) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (item.priority == "URGENT") StatusPill(stringResource(R.string.important))
            }
            Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(item.publishedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DigitalIdScreen(state: AppState, vm: ChurchViewModel) {
    val card = state.digitalId
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingMedium), horizontalAlignment = Alignment.CenterHorizontally) {
        item { PageHeading(stringResource(R.string.digital_id_heading), stringResource(R.string.secure_membership_card)) }
        if (card == null) item { EmptyCard(stringResource(R.string.digital_id_unavailable), stringResource(R.string.id_after_approval)) }
        else item {
            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), emphasized = true) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark(52.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.church_brand), color = GlassTokens.heroHighlight, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                    Text(card.churchName, color = GlassTokens.heroSecondaryText, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    QrImage(card.qrPayload, Modifier.size(188.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(10.dp))
                    Spacer(Modifier.height(15.dp))
                    Text(card.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(card.number, color = GlassTokens.heroHighlight, fontSize = 15.sp, letterSpacing = 1.8.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.member_type, localizedType(card.membershipType)), color = GlassTokens.heroSecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.qr_privacy), color = GlassTokens.heroSecondaryText, fontSize = 12.sp)
                }
            }
        }
        if (card != null) item { OutlinedButton(onClick = { vm.loadDigitalId() }, enabled = !state.loading) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.refresh_secure_qr)) } }
    }
}

@Composable
private fun QrImage(payload: String, modifier: Modifier = Modifier) {
    val bitmap = remember(payload) {
        runCatching {
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512)
            Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until 512) for (y in 0 until 512) setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }.getOrNull()
    }
    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = stringResource(R.string.secure_qr_accessibility), modifier = modifier) else Text(stringResource(R.string.qr_unavailable), modifier = modifier)
}

@Composable
private fun MoreScreen(state: AppState, onOpen: (String) -> Unit, onEnablePush: () -> Unit, onLanguageChanged: (String) -> Unit) {
    val user = state.user ?: return
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingCompact)) {
        item { PageHeading(stringResource(R.string.more), stringResource(R.string.account_tools_subtitle)) }
        item { LanguageSelector(onLanguageChanged) }
        item {
            GlassCard(shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(user.fullName, fontWeight = FontWeight.SemiBold); Text(user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    StatusPill(localizedRole(user.role))
                }
            }
        }
        item { MoreRow(Icons.Outlined.Badge, stringResource(R.string.digital_id_title), stringResource(R.string.member_number_and_qr)) { onOpen("id") } }
        item { MoreRow(Icons.Outlined.Notifications, stringResource(R.string.notifications), pluralStringResource(R.plurals.unread_notifications, state.notifications.count { !it.read }, state.notifications.count { !it.read })) { onOpen("notifications") } }
        if (!state.isDemo) item {
            GlassCard(shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.push_notifications), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.device_token_message), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    OutlinedButton(onClick = onEnablePush, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(7.dp)); Text(stringResource(R.string.enable_device_notifications)) }
                }
            }
        } else item { InfoCard(stringResource(R.string.push_notifications), stringResource(R.string.preview_push_message), Icons.Outlined.Notifications) }
        if (user.isAdmin) item { MoreRow(Icons.Outlined.AdminPanelSettings, stringResource(R.string.admin_workspace), stringResource(R.string.admin_tools_description)) { onOpen("admin") } }
        item { InfoCard(stringResource(R.string.privacy_permissions), stringResource(R.string.privacy_permissions_message), Icons.Outlined.Security) }
    }
}

@Composable
private fun NotificationsScreen(state: AppState, vm: ChurchViewModel) {
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingCompact)) {
        item { PageHeading(stringResource(R.string.notifications), stringResource(R.string.notification_updates)) }
        if (state.notifications.isEmpty()) item { EmptyCard(stringResource(R.string.caught_up), stringResource(R.string.new_updates)) }
        items(state.notifications, key = { it.id }) { n ->
            GlassCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { if (!n.read) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary)); Spacer(Modifier.width(8.dp)); Text(n.title, fontWeight = FontWeight.SemiBold) }
                    Text(n.body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(n.createdAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!n.read) TextButton(onClick = { vm.markRead(n.id) }) { Text(stringResource(R.string.mark_read)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(state: AppState, vm: ChurchViewModel) {
    val user = state.user ?: return
    var qrText by remember { mutableStateOf("") }
    var rejectId by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var queueFilter by remember { mutableStateOf(AdminQueueFilter.ALL) }
    val visibleApplications = remember(state.adminQueue, searchQuery, queueFilter) {
        AdminQueueFilters.filter(state.adminQueue, searchQuery, queueFilter)
    }
    if (rejectId != null) AlertDialog(
        onDismissRequest = { rejectId = null; reason = "" },
        title = { Text(stringResource(R.string.reject_application)) },
        text = { OutlinedTextField(reason, { reason = it }, label = { Text(stringResource(R.string.reason_required)) }, minLines = 2) },
        confirmButton = { TextButton(onClick = { val id = rejectId ?: return@TextButton; if (reason.isNotBlank()) { vm.review(id, false, reason); rejectId = null; reason = "" } }) { Text(stringResource(R.string.reject)) } },
        dismissButton = { TextButton(onClick = { rejectId = null }) { Text(stringResource(R.string.cancel)) } }
    )
    LazyColumn(Modifier.fillMaxWidth().widthIn(max = GlassTokens.contentMaxWidth), contentPadding = PaddingValues(GlassTokens.screenPadding), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { PageHeading(stringResource(R.string.admin_workspace), stringResource(R.string.authorized_admin_actions)) }
        item {
            GlassCard(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.qr_verification), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(stringResource(R.string.qr_verification_description), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    OutlinedTextField(qrText, { qrText = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.qr_token)) }, singleLine = true)
                    Button(onClick = { vm.verifyQr(qrText) }, modifier = Modifier.fillMaxWidth(), enabled = qrText.isNotBlank() && !state.loading) { Text(stringResource(R.string.verify_id)) }
                    state.verifiedQr?.let { result -> InlineNotice(if (result.valid) "${result.memberName} · ${result.memberNumber} · ${result.message}" else result.message, isError = !result.valid) }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.application_queue), action = stringResource(R.string.refresh), onAction = vm::refreshAdmin) }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.admin_search)) },
                singleLine = true,
                trailingIcon = if (searchQuery.isNotBlank()) ({ TextButton(onClick = { searchQuery = "" }) { Text(stringResource(R.string.clear)) } }) else null
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdminQueueFilter.values().toList(), key = { it.name }) { filter ->
                    FilterChip(selected = queueFilter == filter, onClick = { queueFilter = filter }, label = { Text(stringResource(when (filter) { AdminQueueFilter.ALL -> R.string.filter_all; AdminQueueFilter.PAYMENT -> R.string.filter_payment; AdminQueueFilter.SUBMITTED -> R.string.filter_submitted; AdminQueueFilter.IN_REVIEW -> R.string.filter_in_review })) })
                }
            }
        }
        if (state.adminQueue.isEmpty()) item {
            EmptyCard(if (state.loading) stringResource(R.string.loading_applications) else stringResource(R.string.queue_clear), if (state.loading) stringResource(R.string.wait_queue) else stringResource(R.string.empty_queue))
        } else if (visibleApplications.isEmpty()) item {
            EmptyCard(stringResource(R.string.no_matching_applications), stringResource(R.string.change_search_filter))
        }
        items(visibleApplications, key = { it.id }) { app ->
            AdminApplicationCard(app, user, state.loading, onBegin = { vm.beginReview(app.id) }, onVerifyPayment = { vm.verifyPayment(app.id, app.paymentReference.orEmpty()) }, onApprove = { vm.review(app.id, true) }, onReject = { rejectId = app.id; reason = "" })
        }
        item { InfoCard(stringResource(R.string.audit_integrity), stringResource(R.string.audit_integrity_message), Icons.Outlined.Policy) }
    }
}

@Composable
private fun AdminApplicationCard(app: MemberApplication, user: UserProfile, loading: Boolean, onBegin: () -> Unit, onVerifyPayment: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    var detailsExpanded by remember(app.id) { mutableStateOf(false) }
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(app.applicantName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                StatusPill(localizedApplicationStatus(app.status))
            }
            Text(stringResource(R.string.application_type_phone, localizedType(app.type), app.phone), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(stringResource(R.string.fee_payment_line, app.currency, app.amount / 100, app.amount % 100, localizedPaymentStatus(app.paymentStatus)), fontSize = 13.sp)
            if (app.paymentReference != null) Text(stringResource(R.string.transfer_ref_value, app.paymentReference), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (app.rejectionReason != null) Text(stringResource(R.string.rejection_reason_value, app.rejectionReason), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { detailsExpanded = !detailsExpanded }) { Text(if (detailsExpanded) stringResource(R.string.hide_details) else stringResource(R.string.application_details)) }
            if (detailsExpanded) {
                if (app.phone.isNotBlank()) Text(stringResource(R.string.phone_value, app.phone), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (app.address.isNotBlank()) Text(stringResource(R.string.address_value, app.address), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (app.familyMembers.isNotEmpty()) Text(stringResource(R.string.household_value, app.familyMembers.joinToString()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                user.canVerifyPayments && app.paymentStatus == "PROCESSING" -> Button(onClick = onVerifyPayment, Modifier.fillMaxWidth(), enabled = !loading) { Text(stringResource(R.string.verify_received_transfer)) }
                user.canReview && app.status == "SUBMITTED" -> Button(onClick = onBegin, Modifier.fillMaxWidth(), enabled = !loading) { Text(stringResource(R.string.start_review)) }
                user.canReview && app.status == "UNDER_REVIEW" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, Modifier.weight(1f), enabled = !loading) { Text(stringResource(R.string.reject)) }
                    Button(onClick = onApprove, Modifier.weight(1f), enabled = !loading && app.paymentStatus == "PAID") { Text(stringResource(R.string.approve)) }
                }
                app.paymentStatus == "PROCESSING" -> Text(stringResource(R.string.finance_verification_required), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                app.status == "PENDING_PAYMENT" -> Text(stringResource(R.string.waiting_transfer_reference), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                app.status == "PAYMENT_VERIFIED" -> Text(stringResource(R.string.fee_verified_message), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun EmptyCard(title: String, detail: String) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, detail: String, button: String, loading: Boolean, onClick: () -> Unit) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onClick, Modifier.fillMaxWidth(), enabled = !loading) { Text(button) }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    GlassBadge(text)
}

@Composable
private fun InlineNotice(text: String, isError: Boolean, onDismiss: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), shape = RoundedCornerShape(14.dp), color = if (isError) colors.errorContainer else colors.secondaryContainer) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), fontSize = 12.sp, color = if (isError) colors.onErrorContainer else colors.onSecondaryContainer)
            if (onDismiss != null) TextButton(onClick = onDismiss, contentPadding = PaddingValues(start = 8.dp, end = 0.dp)) { Text(stringResource(R.string.dismiss), fontSize = 11.sp) }
        }
    }
}

@Composable
private fun MoreRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(onClick = onClick, shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
private fun LanguageSelector(onLanguageChanged: (String) -> Unit) {
    val context = LocalContext.current
    val selected = AppLocale.selectedTag(context)
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selected) {
        "en" -> R.string.language_english
        "my" -> R.string.language_myanmar
        "ctd" -> R.string.language_tedim
        else -> R.string.language_system
    }
    Box(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.language_selector_value, stringResource(R.string.language), stringResource(selectedLabel)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                AppLocale.SYSTEM to R.string.language_system,
                "en" to R.string.language_english,
                "my" to R.string.language_myanmar,
                "ctd" to R.string.language_tedim
            ).forEach { (tag, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = { expanded = false; if (tag != selected) onLanguageChanged(tag) }
                )
            }
        }
    }
}

@Composable
private fun localizedType(value: String): String = when (value.uppercase(Locale.ROOT)) {
    "FAMILY" -> stringResource(R.string.family)
    else -> stringResource(R.string.individual)
}

@Composable
private fun localizedApplicationStatus(value: String): String = when (value.uppercase(Locale.ROOT)) {
    "DRAFT" -> stringResource(R.string.status_draft)
    "PENDING_PAYMENT" -> stringResource(R.string.status_pending_payment)
    "PAYMENT_VERIFIED" -> stringResource(R.string.status_payment_verified)
    "SUBMITTED" -> stringResource(R.string.status_submitted)
    "UNDER_REVIEW" -> stringResource(R.string.status_under_review)
    "APPROVED" -> stringResource(R.string.status_approved)
    "REJECTED" -> stringResource(R.string.status_rejected)
    else -> stringResource(R.string.status_draft)
}

@Composable
private fun localizedPaymentStatus(value: String): String = when (value.uppercase(Locale.ROOT)) {
    "PENDING" -> stringResource(R.string.payment_pending)
    "PROCESSING" -> stringResource(R.string.payment_processing)
    "PAID" -> stringResource(R.string.payment_paid)
    "FAILED" -> stringResource(R.string.payment_failed)
    "CANCELLED" -> stringResource(R.string.payment_cancelled)
    else -> value.replace('_', ' ').lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
}

@Composable
private fun localizedRole(value: String): String = when (value.uppercase(Locale.ROOT)) {
    "SUPER_ADMIN" -> stringResource(R.string.role_super_admin)
    "PASTOR" -> stringResource(R.string.role_pastor)
    "CHURCH_ADMIN" -> stringResource(R.string.role_church_admin)
    "FINANCE_ADMIN" -> stringResource(R.string.role_finance_admin)
    "USHER" -> stringResource(R.string.role_usher)
    else -> stringResource(R.string.role_member)
}

@Composable
private fun BrandMark(size: Dp = 70.dp) {
    val shape = RoundedCornerShape(size * 0.23f)
    Image(
        painter = painterResource(R.drawable.ic_launcher_art),
        contentDescription = stringResource(R.string.church_brand),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(shape).border(GlassTokens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
    )
}
@Composable
private fun DemoBanner() {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(stringResource(R.string.demo_banner), Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
