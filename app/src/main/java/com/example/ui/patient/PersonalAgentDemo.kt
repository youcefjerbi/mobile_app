package com.example.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** A standalone fictional pitch journey. Never reads or writes patient records or calls agents. */
@Composable
fun PersonalAgentDemo(onExit: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("Eero") }
    var city by rememberSaveable { mutableStateOf("Espoo") }
    var accessibility by rememberSaveable { mutableStateOf("Visually impaired") }
    var transport by rememberSaveable { mutableStateOf("Public transport is difficult; taxis stretch my budget") }
    var routine by rememberSaveable { mutableStateOf("Bingo tomorrow at 11:00") }
    var companion by rememberSaveable { mutableStateOf("Jenny, my dog") }
    var clinic by rememberSaveable { mutableStateOf(false) }
    var insurer by rememberSaveable { mutableStateOf(false) }
    var driver by rememberSaveable { mutableStateOf(false) }
    var coordinated by rememberSaveable { mutableStateOf(false) }
    var edit by rememberSaveable { mutableStateOf(false) }
    var question by rememberSaveable { mutableStateOf("") }
    var history by rememberSaveable { mutableStateOf(listOf("CarePulse: Good morning, Eero. Your sample care team has invited you to a follow-up. Let's make getting there easier, together.")) }
    val date = remember {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        SimpleDateFormat("d MMMM", Locale.UK).format(tomorrow.time)
    }
    fun ask(input: String) {
        val text = input.trim().take(600)
        if (text.isEmpty()) return
        val q = text.lowercase(Locale.ROOT)
        val reply = when {
            listOf("back", "pain", "cancer", "risk").any { it in q } -> "The follow-up invitation is a fictional clinician-supplied assessment. I cannot diagnose cancer or calculate risk from symptoms. We can talk through practical barriers to seeing your care team."
            listOf("transport", "taxi", "cost", "support", "insurance").any { it in q } -> "You told me: $transport. I can draft a request for accessible transport from $city and a review of possible travel support. Eligibility and coverage still need verification. Choose recipients in Care plan."
            listOf("where", "location", "helsinki").any { it in q } -> "The proposed visit is at the fictional Helsinki Care Centre, travelling from $city. An accessible pickup is included as a request, not a confirmed ride."
            listOf("appointment", "bingo", "book", "tomorrow", "plan").any { it in q } -> "I've proposed $date at 08:00–09:00 and a 07:00 pickup. Your routine says: $routine. Availability and return travel must be checked. Review Care plan to choose what to share."
            listOf("know", "remember", "situation").any { it in q } -> "I remember $city, $accessibility, your transport barrier ($transport), and your routine ($routine). You can edit these in My situation. Nothing is shared without review."
            listOf("dog", "jenny", "food").any { it in q } -> "I remember $companion. Dog food is only a reminder in this demo. No order has been placed."
            else -> "I'm a scripted pitch demo. Ask about transport, the location, the appointment, or your situation to explore this journey."
        }
        history = history + "You: $text" + "CarePulse: $reply"
        question = ""
        tab = 1
    }
    val green = CareBlue
    CareTheme {
        Scaffold(bottomBar = {
            GlassNavigation(tab, listOf("Today", "Agent", "Plan", "Me"),
                listOf(Icons.Outlined.Home, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.CalendarMonth, Icons.Outlined.Person)) { tab = it }

        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("✳ carepulse.", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = green)
                        TextButton(onClick = onExit) { Text("Exit demo") }
                    }
                    AgentPicker()
                    Text("Scripted demo · Not connected", style = MaterialTheme.typography.labelSmall)
                }
                item {
                    Text(listOf("Hi, $name.", "Your agent", "Your plan", "Your context")[tab], fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)

                }
                when (tab) {
                    0 -> {
                        item { AgentCard(Color(0xFFF1F5FF)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Care, connected.", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Outlined.AutoAwesome, null, tint = CareBlue)
                            }
                            Text("Your context. Your choice.")
                            Button(onClick = { tab = 1 }) { Icon(Icons.Outlined.ChatBubbleOutline, null); Spacer(Modifier.width(8.dp)); Text("Chat") }
                        } }
                        item { AgentCard {
                            Text("Tomorrow · Draft", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Icon(Icons.Outlined.DirectionsCar, "Pickup", tint = CareBlue); Text("07:00") }
                                Icon(Icons.Outlined.ArrowForward, null)
                                Column { Icon(Icons.Outlined.LocalHospital, "Visit", tint = CareRed); Text("08:00") }
                                Icon(Icons.Outlined.ArrowForward, null)
                                Column { Icon(Icons.Outlined.Home, "Routine", tint = CareGreen); Text("Your day") }
                            }
                            TextButton(onClick = { tab = 2 }) { Text("Review plan ↗") }
                        } }
                        item { AgentCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(city, fontWeight = FontWeight.Bold); Text(companion, style = MaterialTheme.typography.bodySmall) }
                                IconButton(onClick = { tab = 3 }) { Icon(Icons.Outlined.ArrowForward, "Your context") }
                            }
                        } }

                    }
                    1 -> {
                        item { Text("Scripted conversation · No messages leave this device.", fontSize = 12.sp) }
                        history.forEachIndexed { i, message -> item(key = "message-$i") {
                            AgentCard(if (message.startsWith("You:")) Color(0xFFE8EEDF) else Color.White) { Text(message, lineHeight = 24.sp) }
                        } }
                        item {
                            listOf("Where is the treatment centre?", "Can you help with transport costs?", "Will the appointment fit around bingo?", "What do you know about me?").forEach { prompt ->
                                OutlinedButton(onClick = { ask(prompt) }, modifier = Modifier.fillMaxWidth()) { Text(prompt) }
                            }
                            OutlinedTextField(question, { question = it.take(600) }, label = { Text("Ask your agent") }, modifier = Modifier.fillMaxWidth())
                            Button(enabled = question.isNotBlank(), onClick = { ask(question) }) { Text("Send") }
                            TextButton(onClick = { tab = 2 }) { Text("Review care plan →") }
                        }
                    }
                    2 -> {
                        item { AgentCard {
                            Text("PROPOSED JOURNEY · UNCONFIRMED", fontSize = 11.sp, color = green)
                            Text("$date · Helsinki", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("07:00 · Accessible pickup from $city\n08:00–09:00 · Follow-up at the fictional Helsinki Care Centre\nYour routine · $routine", lineHeight = 26.sp)
                            Text("Availability, pickup address and return travel need verification. Travel support requires eligibility and coverage review.", fontSize = 13.sp)
                        } }
                        item { AgentCard {
                            Text("Sharing", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Choose the recipients for this local simulation. No external service is contacted.")
                            AgentConsent("Clinic agent", "First name ($name), city ($city), follow-up invitation and routine ($routine).", clinic, !coordinated) { clinic = it }
                            AgentConsent("Insurance agent", "City ($city) and transport barrier ($transport). No clinical assessment.", insurer, !coordinated) { insurer = it }
                            AgentConsent("Transport agent", "First name ($name), city ($city), accessibility ($accessibility) and proposed appointment time. No home address.", driver, !coordinated) { driver = it }
                            Text("Not shared: conversation, dog, allergies, or full medical history.", fontSize = 12.sp)
                            if (!coordinated) {
                                Button(enabled = clinic, onClick = { coordinated = true }, modifier = Modifier.fillMaxWidth()) { Text("Simulate") }
                                Text("Clinic permission is required. Other recipients are optional.", fontSize = 12.sp)
                            } else {
                                Text("✓ Local simulation complete", color = green, fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = { coordinated = false }) { Text("Revise permissions & plan") }
                            }
                        } }
                        item { AgentCard {
                            Text("Activity", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (!coordinated) Text("Your permission comes first. Review each recipient to see the simulated coordination here.")
                            else {
                                Text("1 · Personal agent: your selected permissions recorded locally.")
                                Text("2 · Clinic agent (simulated): $date, 08:00–09:00 requested. Availability unverified.")
                                if (insurer) Text("Insurance agent (simulated): travel support review requested. No coverage approval issued.")
                                if (driver) Text("Transport agent (simulated): accessible 07:00 pickup requested. Ride unconfirmed.")
                                Text("Personal agent: a coordinated draft is ready to discuss with your care team. No real appointment, transport or payment was arranged.")
                            }
                        } }
                    }
                    3 -> {
                        item { AgentCard {
                            Text("About you", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            listOf("Name" to name, "Home" to city, "Accessibility" to accessibility, "Getting around" to transport, "Your routine" to routine, "Your companion" to companion).forEach { (label, value) ->
                                Text(label, color = green, fontSize = 12.sp)
                                Text(value)
                            }
                            Button(onClick = { edit = true }) { Text("Edit my situation") }
                            Text("Fictional information only. State survives activity recreation, but is not a permanent health record.", fontSize = 12.sp)
                        } }
                    }
                }
                item { Text("Junction 2026 · Built around your life.", color = Color(0xFF798574), fontSize = 11.sp) }
            }
        }
        if (edit) {
            var n by rememberSaveable { mutableStateOf(name) }
            var c by rememberSaveable { mutableStateOf(city) }
            var a by rememberSaveable { mutableStateOf(accessibility) }
            var t by rememberSaveable { mutableStateOf(transport) }
            var r by rememberSaveable { mutableStateOf(routine) }
            var d by rememberSaveable { mutableStateOf(companion) }
            val valid = listOf(n, c, a, t, r, d).all { it.isNotBlank() && it.length <= 240 }
            AlertDialog(onDismissRequest = { edit = false }, title = { Text("My situation") }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Saving clears previous sharing permissions and the simulated plan.", fontSize = 12.sp)
                    OutlinedTextField(n, { n = it.take(240) }, label = { Text("Name") })
                    OutlinedTextField(c, { c = it.take(240) }, label = { Text("Home city") })
                    OutlinedTextField(a, { a = it.take(240) }, label = { Text("Accessibility") })
                    OutlinedTextField(t, { t = it.take(240) }, label = { Text("Transport barriers") })
                    OutlinedTextField(r, { r = it.take(240) }, label = { Text("Daily routine") })
                    OutlinedTextField(d, { d = it.take(240) }, label = { Text("Companion") })
                }
            }, confirmButton = { TextButton(enabled = valid, onClick = {
                name = n.trim(); city = c.trim(); accessibility = a.trim(); transport = t.trim(); routine = r.trim(); companion = d.trim()
                clinic = false; insurer = false; driver = false; coordinated = false
                history = listOf("CarePulse: Thanks, $name. Your situation is updated. Review sharing again before coordinating.")
                edit = false
            }) { Text("Save") } }, dismissButton = { TextButton(onClick = { edit = false }) { Text("Cancel") } })
        }
    }
}

@Composable
private fun AgentCard(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun AgentConsent(title: String, detail: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Checkbox(checked = checked, onCheckedChange = change, enabled = enabled)
        Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 12.sp, lineHeight = 19.sp)
        }
    }
}
