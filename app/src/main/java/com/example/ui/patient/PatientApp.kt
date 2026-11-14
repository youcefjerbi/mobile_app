package com.example.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.*

private val Teal = CareBlue
private val Ink = CareInk

@Composable
fun PatientApp(model: PatientViewModel = viewModel()) {
    var showAgentJourney by rememberSaveable { mutableStateOf(false) }
    if (showAgentJourney) {
        PersonalAgentDemo(onExit = { showAgentJourney = false })
        return
    }
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentials = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    var signingIn by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var form by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVisit by remember { mutableStateOf<CareAppointment?>(null) }
    var removeMedication by remember { mutableStateOf<Medication?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var previousSession by rememberSaveable { mutableStateOf("") }
    val currentSession = if (state.demo) "demo" else if (state.signedIn) "patient:${state.email}" else "signed-out"
    LaunchedEffect(currentSession) {
        if (previousSession != currentSession) {
            tab = 0; form = null; selectedVisit = null; removeMedication = null
            previousSession = currentSession
        }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            if (it in listOf("Medication added.", "Profile saved.", "Visit recorded. This does not book with a provider.")) form = null
            snackbar.showSnackbar(it)
            model.dismissMessage()
        }
    }
    CareTheme {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            if (state.signedIn || state.demo) GlassNavigation(tab,
                listOf("Today", "Meds", "Visits", "Me"),
                listOf(Icons.Outlined.Home, Icons.Outlined.Medication, Icons.Outlined.CalendarMonth, Icons.Outlined.Person)) { tab = it }

        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("carepulse +", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Teal)
                        if (state.demo) Text("Demo", style = MaterialTheme.typography.labelSmall)
                    }
                }
                item { AgentPicker() }
                if (!state.signedIn && !state.demo && !state.loading) {
                    item {
                        Text("Your care.\nYour agent.", fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
                        Text("Explain your situation once. Get care that fits your life.", modifier = Modifier.padding(vertical = 20.dp))
                        Button(onClick = { showAgentJourney = true }, modifier = Modifier.fillMaxWidth()) { Text("Meet your agent") }
                        Text("Scripted demo", fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                        Button(enabled = !signingIn, onClick = {
                            signingIn = true
                            onGoogleSignInClicked(context, credentials,
                                onAuthSuccess = { signingIn = false },
                                onAuthError = { signingIn = false; model.message(it) },
                                scope = scope,
                                onAuthCancelled = { signingIn = false })
                        }, modifier = Modifier.fillMaxWidth()) { Text(if (signingIn) "Signing in…" else "Sign in with Google") }
                        OutlinedButton(onClick = model::startDemo, enabled = !signingIn, modifier = Modifier.fillMaxWidth()) { Text("Try demo") }
                        Text("Patient accounts · Sign in to load and save your records in the connected CarePulse service. Demo changes are temporary.", fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp))
                    }
                } else if (state.loading) {
                    item { CircularProgressIndicator(); Text("Loading your records…") }
                } else if (state.error != null) {
                    item {
                        Text("We couldn't load your records", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(state.error.orEmpty())
                        Button(onClick = model::loadPatient) { Text("Retry") }
                        TextButton(onClick = model::leaveSession) { Text("Sign out") }
                    }
                } else {
                    item {
                        
                        Text(listOf("Today", "Medications", "Visits", "Profile")[tab], fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                        val name = state.profile.fullName.ifBlank { state.name }.substringBefore(" ").ifBlank { "there" }
                        if (tab == 0) Text("Hi, $name.")
                        if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp)); Text("Saving…", fontSize = 12.sp) }
                    }
                    if (tab == 0) item {
                        val taken = state.medications.count { CareRules.takenOn(it.takenToday, it.lastTakenDate, state.today) }
                        CareCard(Color(0xFFF3F6FF)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column {
                                    Text("Daily progress", style = MaterialTheme.typography.titleMedium)
                                    Text("$taken / ${state.medications.size}", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                    Text("Doses taken", style = MaterialTheme.typography.bodySmall)
                                }
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    CircularProgressIndicator(progress = { if (state.medications.isEmpty()) 0f else taken.toFloat() / state.medications.size }, modifier = Modifier.size(88.dp), color = CareGreen, strokeWidth = 9.dp, trackColor = Color(0xFFE1E7EF))
                                    Icon(Icons.Outlined.Check, "Daily dose progress", tint = CareGreen, modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                    }
                    if (tab == 0 || tab == 1) {
                        item { SectionTitle("Medications", "Add medication") { form = "medication" } }
                        if (state.medications.isEmpty()) item { EmptyCard("No medications added", "Add your existing prescribed routine to track it here.") }
                        items(if (tab == 0) state.medications.take(2) else state.medications, key = { "med-${it.id}" }) { med ->
                            CareCard {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Outlined.Medication, null, tint = CareBlue, modifier = Modifier.size(28.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(med.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text("${med.scheduledTime} · ${med.dosage}", style = MaterialTheme.typography.bodySmall)
                                        if (tab == 1 && med.instructions.isNotBlank()) Text(med.instructions, style = MaterialTheme.typography.bodySmall)
                                    }
                                    val taken = CareRules.takenOn(med.takenToday, med.lastTakenDate, state.today)
                                    IconToggleButton(checked = taken, enabled = !state.busy, onCheckedChange = { model.toggleMedication(med) }) {
                                        Icon(if (taken) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, if (taken) "Undo dose" else "Mark dose taken", tint = if (taken) CareGreen else CareInk)
                                    }
                                    if (tab == 1) IconButton(enabled = !state.busy, onClick = { removeMedication = med }) { Icon(Icons.Outlined.DeleteOutline, "Remove medication", tint = CareRed) }
                                }

                            }
                        }
                    }
                    if (tab == 0 && state.medications.size > 2) item { TextButton(onClick = { tab = 1 }) { Text("All medications →") } }
                    if (tab == 0 || tab == 2) {
                        val visits = if (tab == 0) state.appointments.filter { it.status !in listOf("completed", "cancelled") && (CareRules.appointmentMillis(it.scheduledDateTime) ?: 0) >= System.currentTimeMillis() }.take(1) else state.appointments
                        item { SectionTitle(if (tab == 0) "Next appointment" else "Your appointments", "Add") { form = "visit" } }
                        if (visits.isEmpty()) item { EmptyCard("No ${if (tab == 0) "upcoming " else ""}visits", "Record a visit you have arranged with your care team.") }
                        items(visits, key = { "visit-${it.id}" }) { visit ->
                            CareCard {
                                Text("${visit.scheduledDateTime} · ${visit.status}", color = Teal, fontSize = 12.sp)
                                Text(visit.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(visit.providerName.ifBlank { "Provider not recorded" })
                                if (tab == 2) Text(visit.facilityName.ifBlank { "Location not recorded" })
                                OutlinedButton(onClick = { selectedVisit = visit }) { Text("Details ↗") }
                            }
                        }
                    }
                    if (tab == 3) item {
                        CareCard {
                            Text(state.profile.fullName.ifBlank { state.name }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            if (state.email.isNotBlank()) Text(state.email)
                            HorizontalDivider()
                            ProfileLine("Insurance", state.profile.insuranceProvider)
                            ProfileLine("Allergies", state.profile.allergies)
                            ProfileLine("Emergency contact", state.profile.emergencyContact)
                            Button(enabled = !state.busy, onClick = { form = "profile" }) { Text("Edit profile") }
                            TextButton(enabled = !state.busy, onClick = model::leaveSession) { Text(if (state.demo) "Exit demo" else "Sign out") }
                        }
                    }
                }
            }
        }
        if (form != null) RecordForm(form!!, state, onDismiss = { form = null }, onSubmit = { fields ->
            when (form) {
                "medication" -> model.addMedication(fields[0], fields[1], fields[2], fields[3])
                "visit" -> model.addAppointment(fields[0], fields[1], fields[2], fields[3])
                "profile" -> model.saveProfile(fields[0], fields[1], fields[2], fields[3])
            }
        })
        selectedVisit?.let { original ->
            val visit = state.appointments.firstOrNull { it.id == original.id } ?: original
            AlertDialog(onDismissRequest = { selectedVisit = null }, title = { Text(visit.title) }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(visit.scheduledDateTime)
                    Text("${visit.durationMinutes} minutes · ${visit.status}")
                    Text(visit.providerName.ifBlank { "Provider not recorded" })
                    Text(visit.facilityName.ifBlank { "Location not recorded" })
                    if (visit.webCollectedPrep.isNotBlank()) Text(visit.webCollectedPrep)
                    Text("This is your personal visit record. Changes here do not book, cancel, or contact your provider.", fontSize = 12.sp)
                }
            }, confirmButton = { TextButton(onClick = { selectedVisit = null }) { Text("Done") } }, dismissButton = {
                if (visit.status !in listOf("completed", "cancelled")) TextButton(enabled = !state.busy, onClick = { model.completeAppointment(visit) }) { Text("Mark completed") }
            })
        }
        removeMedication?.let { med ->
            AlertDialog(onDismissRequest = { removeMedication = null }, title = { Text("Remove ${med.name}?") }, text = { Text("This removes the medication from your tracker. It does not change your prescription.") }, confirmButton = { TextButton(enabled = !state.busy, onClick = { model.removeMedication(med); removeMedication = null }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { removeMedication = null }) { Text("Keep") } })
        }
    }
}

@Composable
private fun CareCard(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onClick) { Icon(Icons.Outlined.Add, action, tint = CareBlue) }
    }
}

@Composable
private fun EmptyCard(title: String, description: String) { CareCard { Text(title, fontWeight = FontWeight.Bold); Text(description) } }

@Composable
private fun ProfileLine(label: String, value: String) { Column { Text(label, color = Teal, fontSize = 12.sp); Text(value.ifBlank { "Not recorded" }) } }

@Composable
private fun RecordForm(type: String, state: PatientState, onDismiss: () -> Unit, onSubmit: (List<String>) -> Unit) {
    val labels = when (type) {
        "medication" -> listOf("Medication name", "Prescribed dose", "Daily time (HH:mm)", "Instructions (optional)")
        "visit" -> listOf("Visit title", "Provider (optional)", "Location (optional)", "Date & time (YYYY-MM-DD HH:mm)")
        else -> listOf("Full name", "Insurance provider", "Allergies", "Emergency contact")
    }
    val initial = if (type == "profile") listOf(state.profile.fullName, state.profile.insuranceProvider, state.profile.allergies, state.profile.emergencyContact) else listOf("", "", if (type == "medication") "08:00" else "", "")
    var first by rememberSaveable(type) { mutableStateOf(initial[0]) }
    var second by rememberSaveable(type) { mutableStateOf(initial[1]) }
    var third by rememberSaveable(type) { mutableStateOf(initial[2]) }
    var fourth by rememberSaveable(type) { mutableStateOf(initial[3]) }
    AlertDialog(onDismissRequest = { if (!state.busy) onDismiss() }, title = { Text(when (type) { "medication" -> "Add daily medication"; "visit" -> "Record an appointment"; else -> "Edit health profile" }) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (type == "medication") Text("Copy your prescribed routine. This tracker does not recommend doses or support multiple daily doses per entry.", fontSize = 12.sp)
            if (type == "visit") Text("Record an existing appointment. This does not make a booking.", fontSize = 12.sp)
            OutlinedTextField(first, { first = it }, label = { Text(labels[0]) }, enabled = !state.busy, singleLine = true)
            OutlinedTextField(second, { second = it }, label = { Text(labels[1]) }, enabled = !state.busy, singleLine = true)
            OutlinedTextField(third, { third = it }, label = { Text(labels[2]) }, enabled = !state.busy, singleLine = true)
            OutlinedTextField(fourth, { fourth = it }, label = { Text(labels[3]) }, enabled = !state.busy)
            if (state.notice != null) Text(state.notice, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(enabled = !state.busy, onClick = { onSubmit(listOf(first, second, third, fourth)) }) { Text(if (state.busy) "Saving…" else "Save") } }, dismissButton = { TextButton(enabled = !state.busy, onClick = onDismiss) { Text("Cancel") } })
}
