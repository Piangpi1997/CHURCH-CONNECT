package org.cmf.churchconnect.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.cmf.churchconnect.domain.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF17191C)
private val Slate = Color(0xFF626A73)
private val Paper = Color(0xFFF4F5F6)
private val Mint = Color(0xFFDCEFE7)

@Composable
fun ChurchConnectApp(vm: ChurchViewModel, onEnablePush: () -> Unit) {
    val state = vm.state
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = Color(0xFFC5E8D7), background = Color(0xFF121416), surface = Color(0xFF1C1F22))
    else lightColorScheme(primary = Ink, onPrimary = Color.White, background = Paper, surface = Color.White, secondary = Color(0xFF60766C), outline = Color(0xFFE4E7E9))
    MaterialTheme(colorScheme = scheme, typography = Typography(), shapes = Shapes()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.user == null) AuthScreen(state, vm) else MainShell(state, vm, onEnablePush)
        }
    }
}

@Composable
private fun AuthScreen(state: AppState, vm: ChurchViewModel) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(top = 54.dp, bottom = 30.dp)) {
            item {
                BrandMark()
                Spacer(Modifier.height(18.dp))
                Text("Church Connect", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Christ Mission Fellowship · Setapak", color = Slate, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("One Church • One Community • One Mission", color = Slate, fontSize = 13.sp)
                Spacer(Modifier.height(28.dp))
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(if (creating) "Create your account" else "Welcome back", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (creating) "Start your membership journey." else "Sign in to stay connected with your church community.", color = Slate, fontSize = 14.sp)
                        if (creating) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password (6+ characters)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        if (state.error != null) InlineNotice(state.error, isError = true)
                        Button(onClick = { vm.signIn(email, password, if (creating) name else null) }, Modifier.fillMaxWidth().height(52.dp), enabled = !state.loading, shape = RoundedCornerShape(16.dp)) {
                            if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (creating) "Create account" else "Sign in")
                        }
                        if (!creating && !state.isDemo) TextButton(onClick = { vm.sendPasswordReset(email) }, Modifier.align(Alignment.CenterHorizontally), enabled = !state.loading) { Text("Forgot password?") }
                        if (state.message != null) InlineNotice(state.message, isError = false, onDismiss = vm::dismissMessage)
                        TextButton(onClick = { creating = !creating; vm.dismissMessage() }, Modifier.align(Alignment.CenterHorizontally)) { Text(if (creating) "Already have an account? Sign in" else "New here? Create an account") }
                        if (state.isDemo) {
                            HorizontalDivider()
                            Text("LOCAL PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate, letterSpacing = 1.3.sp)
                            Text("Explore the sample workflow. Demo changes stay in memory and are not church records or payments.", fontSize = 12.sp, color = Slate)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.demoSignIn(false) }, Modifier.weight(1f), enabled = !state.loading) { Text("Member demo") }
                                OutlinedButton(onClick = { vm.demoSignIn(true) }, Modifier.weight(1f), enabled = !state.loading) { Text("Admin demo") }
                            }
                        } else {
                            Text("Accounts are managed by the church's Firebase service. Use password reset on the sign-in provider if you cannot access your account.", fontSize = 12.sp, color = Slate)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("CHRIST MISSION FELLOWSHIP CHURCH", fontSize = 10.sp, letterSpacing = 1.1.sp, color = Slate)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(state: AppState, vm: ChurchViewModel, onEnablePush: () -> Unit) {
    val user = state.user ?: return
    val actualTab = if (state.tab in setOf("id", "notifications", "admin")) "more" else state.tab
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Church Connect", fontWeight = FontWeight.SemiBold, fontSize = 18.sp); Text("CMF · Setapak", fontSize = 11.sp, color = Slate) } },
                actions = { IconButton(onClick = { vm.refresh() }, enabled = !state.loading) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh") }; IconButton(onClick = { vm.signOut() }) { Icon(Icons.Outlined.Logout, contentDescription = "Sign out") } }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("membership", "Apply", Icons.Outlined.AssignmentInd), Triple("calendar", "Events", Icons.Outlined.Event), Triple("news", "News", Icons.Outlined.Campaign), Triple("more", "More", Icons.Outlined.MoreHoriz)).forEach { (tab, title, icon) ->
                    NavigationBarItem(selected = actualTab == tab, onClick = { vm.select(tab) }, icon = { Icon(icon, contentDescription = title) }, label = { Text(title) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                else -> MoreScreen(state, onOpen = vm::select, onEnablePush = onEnablePush)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AppState, vm: ChurchViewModel) {
    val user = state.user ?: return
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background == Paper) Ink else MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("WELCOME TO CMF", color = Color(0xFFC5E8D7), fontSize = 11.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Hello, ${user.fullName.substringBefore(' ')}.", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(5.dp))
                    Text("A place to grow in faith and serve together.", color = Color(0xFFD1D5D8), fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FilledTonalButton(onClick = { vm.select("membership") }) { Icon(Icons.Outlined.PersonAddAlt, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Membership") }
                        OutlinedButton(onClick = { vm.select("calendar") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Icon(Icons.Outlined.Event, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Events") }
                    }
                }
            }
        }
        item { SectionTitle("Your next gathering", action = "See calendar", onAction = { vm.select("calendar") }) }
        if (state.events.isEmpty()) item { EmptyCard("No upcoming events", "The church calendar will appear here when events are published.") }
        else item { EventCard(state.events.first()) }
        item { SectionTitle("Stay connected", action = "All news", onAction = { vm.select("news") }) }
        items(state.announcements.take(2), key = { it.id }) { AnnouncementCard(it) }
        if (state.registration != null) item { ApplicationStatusCard(state.registration, state.config) }
        if (!state.isDemo && state.config == null) item { EmptyCard("Unable to load church data", "Check your connection and try refreshing. The church may not have published any content yet.") }
    }
}

@Composable
private fun MembershipScreen(state: AppState, vm: ChurchViewModel) {
    val existing = state.registration
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeading("Membership", "Apply to join Christ Mission Fellowship.") }
        if (existing == null || existing.status in setOf("APPROVED", "REJECTED")) {
            item { NewApplicationForm(state.config, state.loading, vm) }
        } else {
            item { ApplicationStatusCard(existing, state.config) }
            if (existing.status == "PENDING_PAYMENT" && existing.paymentStatus != "PROCESSING") item { PaymentReferenceCard(existing, state.isDemo, vm) }
            if (existing.status == "PENDING_PAYMENT" && existing.paymentStatus == "PROCESSING") item {
                InfoCard("Awaiting finance verification", "Reference ${existing.paymentReference ?: "submitted"}. The finance team must confirm receipt; your application cannot be submitted until then.", Icons.Outlined.HourglassTop)
            }
            if (existing.status == "PAYMENT_VERIFIED") item {
                ActionCard("Payment verified", "Your fee is confirmed. Submit your application for church review.", button = "Submit application", loading = state.loading, onClick = { vm.submitRegistration() })
            }
            if (existing.status == "SUBMITTED" || existing.status == "UNDER_REVIEW") item {
                InfoCard("Application under review", "Your application has been submitted. You will see an update here when the church team completes its review.", Icons.Outlined.FactCheck)
            }
            if (existing.status == "APPROVED") item {
                InfoCard("You’re a member", "Your member number is ${state.user?.memberNumber ?: "available on your digital ID"}.", Icons.Outlined.Verified)
                Button(onClick = { vm.select("id") }, Modifier.fillMaxWidth()) { Text("Open digital member ID") }
            }
            if (existing.status == "REJECTED") item {
                InfoCard("Application update", existing.rejectionReason ?: "Please contact the church office for more information.", Icons.Outlined.Info)
                OutlinedButton(onClick = { vm.refresh() }, Modifier.fillMaxWidth()) { Text("Refresh status") }
            }
        }
        item { InfoCard("About the fee", "Fees are shown from the church configuration. Online payment is not connected in this build. Follow the church’s approved transfer process and submit its reference; a finance admin must verify it.", Icons.Outlined.Info) }
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
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New application", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text("Choose an application type", color = Slate, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == "INDIVIDUAL", { type = "INDIVIDUAL" }, label = { Text("Individual") })
                FilterChip(type == "FAMILY", { type = "FAMILY" }, label = { Text("Family") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Registration fee", color = Slate)
                Text(config?.formattedFee(if (type == "FAMILY") config.familyFee else config.individualFee) ?: "Loading…", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Applicant full name") }, singleLine = true)
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Home address") }, minLines = 2)
            if (type == "FAMILY") OutlinedTextField(family, { family = it }, Modifier.fillMaxWidth(), label = { Text("Other family members (one name per line)") }, minLines = 3)
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text("I consent to the church using this information to process my membership application and contact me about it.", Modifier.padding(top = 11.dp), fontSize = 12.sp, color = Slate)
            }
            Button(onClick = {
                vm.createRegistration(RegistrationDraft(type, name, phone, address, family.lines().map(String::trim).filter(String::isNotBlank), consent))
            }, Modifier.fillMaxWidth().height(50.dp), enabled = !loading && config != null, shape = RoundedCornerShape(15.dp)) {
                Text(if (loading) "Saving…" else "Create application")
            }
            Text("Your details are visible only to authorized church staff.", fontSize = 11.sp, color = Slate)
        }
    }
}

@Composable
private fun PaymentReferenceCard(app: MemberApplication, demo: Boolean, vm: ChurchViewModel) {
    var reference by remember(app.id) { mutableStateOf("") }
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Payment verification", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text("${app.currency} ${(app.amount / 100)}.${(app.amount % 100).toString().padStart(2, '0')} · ${if (demo) "sample payment flow" else "offline transfer review"}", color = Slate)
            if (demo) Text("Demo only: this simulates a reference and finance review. No money moves.", color = Color(0xFF8A5A00), fontSize = 12.sp)
            OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth(), label = { Text("Transfer reference") }, singleLine = true)
            Button(onClick = { vm.submitPaymentReference(reference) }, Modifier.fillMaxWidth(), enabled = reference.trim().length >= 4 && !vm.state.loading) { Text("Send for verification") }
            Text("Submitting a reference does not mark the payment as paid. An authorized finance administrator must verify the received funds.", fontSize = 12.sp, color = Slate)
        }
    }
}

@Composable
private fun ApplicationStatusCard(app: MemberApplication, config: ChurchConfig?) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Application status", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                StatusPill(app.status.replace('_', ' '))
            }
            Text("${app.type.lowercase().replaceFirstChar { it.uppercase() }} · ${app.applicantName}", color = Slate)
            Text("Fee: ${app.currency} ${(app.amount / 100)}.${(app.amount % 100).toString().padStart(2, '0')} · Payment: ${app.paymentStatus.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 13.sp)
            Text("Application ${app.id.takeLast(8).uppercase()}", fontSize = 11.sp, color = Slate)
        }
    }
}

@Composable
private fun CalendarScreen(state: AppState) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Church calendar", "Gather, grow and serve together.") }
        if (state.events.isEmpty()) item { EmptyCard("No events published", "New church events will appear here.") }
        items(state.events, key = { it.id }) { EventCard(it) }
    }
}

@Composable
private fun EventCard(event: ChurchEvent) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.width(55.dp).clip(RoundedCornerShape(15.dp)).background(Mint).padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val date = Date(event.startsAt)
                Text(SimpleDateFormat("MMM", Locale.ENGLISH).format(date).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(SimpleDateFormat("d", Locale.ENGLISH).format(date), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(event.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(SimpleDateFormat("EEE, d MMM · h:mm a", Locale.ENGLISH).format(Date(event.startsAt)), fontSize = 12.sp, color = Slate)
                Text(event.description, fontSize = 13.sp, color = Slate)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocationOn, null, Modifier.size(14.dp), tint = Slate); Spacer(Modifier.width(4.dp)); Text(event.location, fontSize = 12.sp, color = Slate) }
            }
        }
    }
}

@Composable
private fun NewsScreen(state: AppState) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Announcements", "Updates from your church community.") }
        if (state.announcements.isEmpty()) item { EmptyCard("No announcements yet", "Official announcements will appear here.") }
        items(state.announcements, key = { it.id }) { AnnouncementCard(it) }
    }
}

@Composable
private fun AnnouncementCard(item: Announcement) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (item.priority == "URGENT") StatusPill("IMPORTANT")
            }
            Text(item.body, color = Slate, fontSize = 14.sp)
            Text(SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(item.publishedAt)), color = Slate, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DigitalIdScreen(state: AppState, vm: ChurchViewModel) {
    val card = state.digitalId
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { PageHeading("Digital member ID", "A secure, verifiable membership card.") }
        if (card == null) item { EmptyCard("Digital ID not available", "Your ID will be issued after church admins approve your membership.") }
        else item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CHRIST MISSION FELLOWSHIP", color = Color(0xFFC5E8D7), fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                    Text(card.churchName, color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    QrImage(card.qrPayload, Modifier.size(188.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(10.dp))
                    Spacer(Modifier.height(15.dp))
                    Text(card.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(card.number, color = Color(0xFFC5E8D7), fontSize = 15.sp, letterSpacing = 1.8.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("${card.membershipType.lowercase().replaceFirstChar { it.uppercase() }} member", color = Color(0xFFD1D5D8), fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    Text("This QR contains an opaque verification token only. Staff can verify its active status; private contact and address details are not embedded.", color = Color(0xFFD1D5D8), fontSize = 12.sp)
                }
            }
        }
        if (card != null) item { OutlinedButton(onClick = { vm.loadDigitalId() }, enabled = !state.loading) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Refresh secure QR") } }
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
    if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Secure member verification QR code", modifier = modifier) else Text("QR unavailable", modifier = modifier)
}

@Composable
private fun MoreScreen(state: AppState, onOpen: (String) -> Unit, onEnablePush: () -> Unit) {
    val user = state.user ?: return
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("More", "Your account and church tools.") }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Mint), Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = Ink) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(user.fullName, fontWeight = FontWeight.SemiBold); Text(user.email, fontSize = 12.sp, color = Slate) }
                    StatusPill(user.role.replace('_', ' '))
                }
            }
        }
        item { MoreRow(Icons.Outlined.Badge, "Digital member ID", "Member number and secure QR") { onOpen("id") } }
        item { MoreRow(Icons.Outlined.Notifications, "Notifications", "${state.notifications.count { !it.read }} unread") { onOpen("notifications") } }
        if (!state.isDemo) item {
            Card(shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Push notifications", fontWeight = FontWeight.SemiBold)
                    Text("Share this device’s notification token with the church notification service. Android may ask for permission.", color = Slate, fontSize = 12.sp)
                    OutlinedButton(onClick = onEnablePush, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(7.dp)); Text("Enable on this device") }
                }
            }
        } else item { InfoCard("Push notifications", "Push delivery is not active in the in-memory preview. Sample updates are visible in the in-app inbox.", Icons.Outlined.Notifications) }
        if (user.isAdmin) item { MoreRow(Icons.Outlined.AdminPanelSettings, "Admin workspace", "Applications, payment checks and QR verification") { onOpen("admin") } }
        item { InfoCard("Privacy & permissions", "Your private membership information is visible only to authorized church staff. Admin actions are checked by trusted backend rules, not by hidden app buttons.", Icons.Outlined.Security) }
    }
}

@Composable
private fun NotificationsScreen(state: AppState, vm: ChurchViewModel) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Notifications", "Membership and church updates.") }
        if (state.notifications.isEmpty()) item { EmptyCard("You're all caught up", "New updates will appear here.") }
        items(state.notifications, key = { it.id }) { n ->
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { if (!n.read) Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4D8B6B))); Spacer(Modifier.width(8.dp)); Text(n.title, fontWeight = FontWeight.SemiBold) }
                    Text(n.body, color = Slate, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(SimpleDateFormat("d MMM · h:mm a", Locale.ENGLISH).format(Date(n.createdAt)), fontSize = 11.sp, color = Slate)
                        if (!n.read) TextButton(onClick = { vm.markRead(n.id) }) { Text("Mark read") }
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
    if (rejectId != null) AlertDialog(
        onDismissRequest = { rejectId = null; reason = "" },
        title = { Text("Reject application") },
        text = { OutlinedTextField(reason, { reason = it }, label = { Text("Reason (required)") }, minLines = 2) },
        confirmButton = { TextButton(onClick = { val id = rejectId ?: return@TextButton; if (reason.isNotBlank()) { vm.review(id, false, reason); rejectId = null; reason = "" } }) { Text("Reject") } },
        dismissButton = { TextButton(onClick = { rejectId = null }) { Text("Cancel") } }
    )
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { PageHeading("Admin workspace", "Authorized actions are verified by the backend.") }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Member QR verification", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("Paste a scanned CMF QR token to check its current active status. Private profile details are not returned.", color = Slate, fontSize = 13.sp)
                    OutlinedTextField(qrText, { qrText = it }, Modifier.fillMaxWidth(), label = { Text("QR token") }, singleLine = true)
                    Button(onClick = { vm.verifyQr(qrText) }, modifier = Modifier.fillMaxWidth(), enabled = qrText.isNotBlank() && !state.loading) { Text("Verify ID") }
                    state.verifiedQr?.let { result -> InlineNotice(if (result.valid) "${result.memberName} · ${result.memberNumber} · ${result.message}" else result.message, isError = !result.valid) }
                }
            }
        }
        item { SectionTitle("Application queue", action = "Refresh", onAction = vm::refreshAdmin) }
        if (state.adminQueue.isEmpty()) item { EmptyCard("Queue is clear", "New applications and payment references will appear here.") }
        items(state.adminQueue, key = { it.id }) { app ->
            AdminApplicationCard(app, user, state.loading, onBegin = { vm.beginReview(app.id) }, onVerifyPayment = { vm.verifyPayment(app.id, app.paymentReference.orEmpty()) }, onApprove = { vm.review(app.id, true) }, onReject = { rejectId = app.id; reason = "" })
        }
        item { InfoCard("Audit & financial integrity", "Member numbers are generated on the server at approval. Payment references remain unverified until finance staff confirm receipt. Demo approvals are simulation-only.", Icons.Outlined.Policy) }
    }
}

@Composable
private fun AdminApplicationCard(app: MemberApplication, user: UserProfile, loading: Boolean, onBegin: () -> Unit, onVerifyPayment: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(app.applicantName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                StatusPill(app.status.replace('_', ' '))
            }
            Text("${app.type} · ${app.phone}", color = Slate, fontSize = 13.sp)
            Text("${app.currency} ${(app.amount / 100)}.${(app.amount % 100).toString().padStart(2, '0')} · ${app.paymentStatus}", fontSize = 13.sp)
            if (app.address.isNotBlank()) Text(app.address, fontSize = 12.sp, color = Slate, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (app.familyMembers.isNotEmpty()) Text("Household: ${app.familyMembers.joinToString()}", fontSize = 12.sp, color = Slate)
            if (app.paymentReference != null) Text("Transfer ref: ${app.paymentReference}", fontSize = 12.sp, color = Slate)
            when {
                user.canVerifyPayments && app.paymentStatus == "PROCESSING" -> Button(onClick = onVerifyPayment, Modifier.fillMaxWidth(), enabled = !loading) { Text("Verify received transfer") }
                user.canReview && app.status == "SUBMITTED" -> Button(onClick = onBegin, Modifier.fillMaxWidth(), enabled = !loading) { Text("Start review") }
                user.canReview && app.status == "UNDER_REVIEW" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, Modifier.weight(1f), enabled = !loading) { Text("Reject") }
                    Button(onClick = onApprove, Modifier.weight(1f), enabled = !loading && app.paymentStatus == "PAID") { Text("Approve") }
                }
                app.paymentStatus == "PROCESSING" -> Text("Finance verification is required.", fontSize = 12.sp, color = Slate)
                app.status == "PENDING_PAYMENT" -> Text("Waiting for applicant's transfer reference.", fontSize = 12.sp, color = Slate)
                app.status == "PAYMENT_VERIFIED" -> Text("Fee verified. Applicant still needs to submit the application.", fontSize = 12.sp, color = Slate)
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 13.sp, color = Slate)
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
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Slate, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Slate, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, detail: String, button: String, loading: Boolean, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Slate, fontSize = 13.sp)
            Button(onClick = onClick, Modifier.fillMaxWidth(), enabled = !loading) { Text(button) }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(shape = RoundedCornerShape(30.dp), color = Mint) { Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink) }
}

@Composable
private fun InlineNotice(text: String, isError: Boolean, onDismiss: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), shape = RoundedCornerShape(14.dp), color = if (isError) Color(0xFFFFE7E5) else Mint) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), fontSize = 12.sp, color = if (isError) Color(0xFF8E302A) else Ink)
            if (onDismiss != null) TextButton(onClick = onDismiss, contentPadding = PaddingValues(start = 8.dp, end = 0.dp)) { Text("Dismiss", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun MoreRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Slate, fontSize = 12.sp) }
            Icon(Icons.Outlined.ChevronRight, null, tint = Slate)
        }
    }
}

@Composable
private fun BrandMark() {
    Box(Modifier.size(70.dp).clip(RoundedCornerShape(23.dp)).background(Ink), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Church, contentDescription = null, tint = Color(0xFFC5E8D7), modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun DemoBanner() {
    Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFF0D7)) {
        Text("DEMO MODE · Sample data only · No live church records or payments", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFF6F4A00), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
