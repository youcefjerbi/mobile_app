package com.example.data.agent

import android.util.Log
import com.example.data.model.CareAppointment
import com.example.data.model.CommsSignal
import com.example.data.model.Medication
import com.example.data.model.PatientHealthProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PreventiveSuggestion(
    val title: String,
    val specialty: String,
    val suggestedDateTime: String,
    val durationMinutes: Long,
    val rationale: String
)

data class CoordinatedBookingPacket(
    val providerName: String,
    val facilityName: String,
    val scheduledDateTime: String,
    val webCollectedPrep: String,
    val webSourcesSummary: String
)

class PersonalHealthAgentService {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val suggestionsModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "suggestions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "Preventive care appointment title"),
                            "specialty" to Schema.string(description = "Medical specialty such as Primary Care, Cardiology, Dentistry, Ophthalmology, Dermatology"),
                            "suggestedDateTime" to Schema.string(description = "Conflict-free date and time formatted as YYYY-MM-DD HH:mm"),
                            "durationMinutes" to Schema.integer(description = "Appointment duration in minutes, 15 to 90"),
                            "rationale" to Schema.string(description = "Personalized explanation referencing patient health info, insurance coverage, medications, calendar, or email notices")
                        )
                    ),
                    description = "2 to 3 personalized preventive care suggestions"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    suspend fun suggestPreventiveCare(
        profile: PatientHealthProfile?,
        medications: List<Medication>,
        signals: List<CommsSignal>,
        existingAppointments: List<CareAppointment>,
        todayDate: String
    ): List<PreventiveSuggestion> {
        val contextSummary = buildPatientContext(profile, medications, signals, existingAppointments)
        val prompt = """
            You are CarePulse, an empathetic personal health agent for a citizen.
            Today's date is $todayDate.
            Analyze the citizen's personal health profile, insurance coverage, daily medications, connected calendar busy blocks, and health email notices below.
            Recommend 2 to 3 high-value, conflict-free preventive care appointments (avoiding times already occupied in their calendar signals or existing appointments).
            Make the rationale specific to their actual profile, insurance plan, medications, or email notices.

            $contextSummary
        """.trimIndent()

        val response = withTimeout(60_000L) {
            suggestionsModel.generateContent(prompt)
        }
        val jsonText = response.text ?: throw IllegalStateException("Health agent returned an empty response.")
        val root = JSONObject(jsonText)
        val array = root.optJSONArray("suggestions") ?: JSONArray()
        val list = mutableListOf<PreventiveSuggestion>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            list.add(
                PreventiveSuggestion(
                    title = item.optString("title", "Preventive Health Check").take(150),
                    specialty = item.optString("specialty", "Primary Care").take(90),
                    suggestedDateTime = item.optString("suggestedDateTime", "$todayDate 10:00").take(50),
                    durationMinutes = item.optLong("durationMinutes", 30L).coerceIn(15L, 120L),
                    rationale = item.optString("rationale", "Recommended routine preventive screening.").take(550)
                )
            )
        }
        return list
    }

    /**
     * Triggered ONLY after the patient gives explicit okay/approval to book and coordinate treatment.
     * Queries real public healthcare endpoints (CMS NPI Registry + OpenFDA Drug Label API) over OkHttp
     * and synthesizes a unified clinical & administrative coordination packet using Gemini.
     */
    suspend fun collectWebInfoAndCoordinateAppointment(
        appointment: CareAppointment,
        profile: PatientHealthProfile?,
        medications: List<Medication>,
        signals: List<CommsSignal>
    ): CoordinatedBookingPacket {
        val webFindings = withContext(Dispatchers.IO) {
            fetchLiveClinicalAndProviderWebData(
                specialty = appointment.specialty,
                medications = medications
            )
        }

        val contextSummary = buildPatientContext(profile, medications, signals, listOf(appointment))
        val prompt = """
            The patient just gave explicit approval ("Okay") to book and coordinate the following appointment:
            - Appointment: ${appointment.title} (${appointment.specialty})
            - Preferred Time: ${appointment.scheduledDateTime} (${appointment.durationMinutes} min)
            - Agent Rationale: ${appointment.agentRationale}

            Live Internet Lookup Results collected from public healthcare & drug registries:
            $webFindings

            Patient Insurance, Clinical Profile, Medications, Calendar & Email Context:
            $contextSummary

            Create a concise, well-structured Pre-Visit & Provider Coordination Packet containing:
            1. Matched Provider / Facility & Registry Reference (use the live NPI lookup if present, otherwise standard clinical department info)
            2. Insurance & Pre-Authorization Checklist (Member ID, Group #, plan type, documents to bring)
            3. Medication & Allergy Preparation Notes (incorporating OpenFDA guidance and patient allergies)
            4. Calendar & Email Coordination Summary (confirming no conflict and what email confirmation to expect)

            Keep the response under 1800 characters, clear with bullet points, and directly actionable.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            chatModel.generateContent(prompt)
        }
        val packetText = response.text?.trim()
            ?: throw IllegalStateException("Agent could not synthesize coordination packet.")

        val primaryProviderLine = webFindings.lines()
            .firstOrNull { it.startsWith("NPI Provider:") }
            ?.removePrefix("NPI Provider:")
            ?.trim()

        val matchedProviderName = primaryProviderLine?.substringBefore("|")?.trim()
            ?.takeIf { it.isNotBlank() } ?: appointment.providerName.ifBlank { "Assigned ${appointment.specialty} Specialist" }
        val matchedFacility = primaryProviderLine?.substringAfter("|", "")?.trim()
            ?.takeIf { it.isNotBlank() } ?: appointment.facilityName.ifBlank { "In-Network ${appointment.specialty} Center" }

        return CoordinatedBookingPacket(
            providerName = matchedProviderName.take(115),
            facilityName = matchedFacility.take(150),
            scheduledDateTime = appointment.scheduledDateTime,
            webCollectedPrep = packetText.take(3800),
            webSourcesSummary = webFindings.take(600)
        )
    }

    suspend fun analyzeMedicationSchedule(
        medications: List<Medication>,
        profile: PatientHealthProfile?
    ): String {
        val medNames = medications.filter { it.active }.map { it.name }.take(3)
        val fdaNotes = withContext(Dispatchers.IO) {
            fetchOpenFdaMedicationNotes(medNames)
        }
        val medListText = medications.joinToString("\n") { med ->
            "- ${med.name} ${med.dosage} (${med.frequency}, ${med.scheduledTime}, ${med.timeSlot}): ${med.instructions} | Taken today: ${med.takenToday}"
        }
        val prompt = """
            Review this citizen's daily medication schedule and provide a concise, practical adherence & timing check (under 900 characters):
            Allergies: ${profile?.allergies ?: "None recorded"}
            Conditions: ${profile?.chronicConditions ?: "None recorded"}
            Active Medications:
            $medListText

            Live OpenFDA Label Reference:
            $fdaNotes

            Highlight: (1) optimal spacing/food timing, (2) any allergy or interaction caution to confirm with their doctor, and (3) a supportive adherence tip. Include a brief reminder to confirm medication changes with a licensed clinician.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            chatModel.generateContent(prompt)
        }
        return response.text?.trim() ?: "No medication guidance generated."
    }

    fun streamAgentChat(
        userMessage: String,
        profile: PatientHealthProfile?,
        medications: List<Medication>,
        appointments: List<CareAppointment>,
        signals: List<CommsSignal>
    ): Flow<String> = flow {
        val contextSummary = buildPatientContext(profile, medications, signals, appointments)
        val prompt = """
            You are CarePulse, the citizen's Personal Health Agent connected to their digital insurance card, personal email notices, device calendar, medications, and preventive care appointments.
            Respond helpfully, concisely, and empathetically. Always ground answers in their actual profile below. Never invent insurance IDs or medications not listed.

            $contextSummary

            Citizen Question: $userMessage
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
    }

    private fun fetchLiveClinicalAndProviderWebData(
        specialty: String,
        medications: List<Medication>
    ): String {
        val sb = StringBuilder()
        // 1. Query CMS NPI Registry API v2.1 for real verified providers matching specialty
        try {
            val encodedTaxonomy = URLEncoder.encode(specialty.ifBlank { "Internal Medicine" }, "UTF-8")
            val npiUrl = "https://npiregistry.cms.hhs.gov/api/?version=2.1&taxonomy_description=$encodedTaxonomy&limit=2"
            val request = Request.Builder().url(npiUrl).get().build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        for (i in 0 until results.length()) {
                            val r = results.getJSONObject(i)
                            val npi = r.optString("number", "")
                            val basic = r.optJSONObject("basic")
                            val first = basic?.optString("first_name", "").orEmpty()
                            val last = basic?.optString("last_name", "").orEmpty()
                            val org = basic?.optString("organization_name", "").orEmpty()
                            val providerDisplay = if (org.isNotBlank()) org else "Dr. $first $last".trim()
                            val addresses = r.optJSONArray("addresses")
                            val addrObj = if (addresses != null && addresses.length() > 0) addresses.getJSONObject(0) else null
                            val city = addrObj?.optString("city", "").orEmpty()
                            val state = addrObj?.optString("state", "").orEmpty()
                            val phone = addrObj?.optString("telephone_number", "").orEmpty()
                            sb.appendLine("NPI Provider: $providerDisplay (NPI #$npi) | $city, $state $phone")
                        }
                    } else {
                        sb.appendLine("NPI Registry queried for '$specialty': no direct match returned, using in-network directory defaults.")
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "NPI Registry lookup unavailable: ${e.message}")
            sb.appendLine("NPI Registry note: Live registry unreachable (${e.message ?: "offline"}); continuing with profile coordination.")
        }

        // 2. Query OpenFDA Drug Label API for active medications
        val medNames = medications.filter { it.active }.map { it.name }.take(2)
        if (medNames.isNotEmpty()) {
            sb.appendLine(fetchOpenFdaMedicationNotes(medNames))
        }
        return sb.toString().trim()
    }

    private fun fetchOpenFdaMedicationNotes(medNames: List<String>): String {
        if (medNames.isEmpty()) return "No active medications to query."
        val sb = StringBuilder()
        for (med in medNames) {
            try {
                val cleanName = med.substringBefore(" ").trim()
                val encoded = URLEncoder.encode(cleanName, "UTF-8")
                val fdaUrl = "https://api.fda.gov/drug/label.json?search=openfda.generic_name:$encoded+openfda.brand_name:$encoded&limit=1"
                val request = Request.Builder().url(fdaUrl).get().build()
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            val item = results.getJSONObject(0)
                            val dosageAdmin = item.optJSONArray("dosage_and_administration")?.optString(0)?.take(220)
                            val warnings = item.optJSONArray("warnings_and_cautions")?.optString(0)?.take(220)
                                ?: item.optJSONArray("warnings")?.optString(0)?.take(220)
                            sb.appendLine("OpenFDA [$med]: ${dosageAdmin ?: "Standard label"} | Caution: ${warnings ?: "Follow clinician guidance"}")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "OpenFDA lookup skipped for $med: ${e.message}")
            }
        }
        return sb.toString().ifBlank { "OpenFDA reference checked; follow prescriber instructions." }
    }

    private fun buildPatientContext(
        profile: PatientHealthProfile?,
        medications: List<Medication>,
        signals: List<CommsSignal>,
        appointments: List<CareAppointment>
    ): String {
        val profileBlock = if (profile != null) {
            """
            Patient Profile:
            - Name: ${profile.fullName}, DOB: ${profile.dateOfBirth}, Blood Type: ${profile.bloodType}
            - Allergies: ${profile.allergies.ifBlank { "None" }}
            - Conditions: ${profile.chronicConditions.ifBlank { "None" }}
            - Insurance: ${profile.insuranceProvider} (${profile.insurancePlanType}), Member ID: ${profile.insuranceMemberId}, Group: ${profile.insuranceGroupNumber}, Valid Thru: ${profile.insuranceValidThru}
            - Connected Email: ${profile.connectedEmail} (Sync=${profile.emailSyncEnabled}), Calendar Sync=${profile.calendarSyncEnabled}
            """.trimIndent()
        } else {
            "Patient Profile: Not yet completed."
        }

        val medsBlock = if (medications.isEmpty()) {
            "Medications: None logged."
        } else {
            "Medications:\n" + medications.joinToString("\n") {
                "- ${it.name} ${it.dosage} @ ${it.scheduledTime} (${it.timeSlot}), takenToday=${it.takenToday}, instructions=${it.instructions}"
            }
        }

        val signalsBlock = if (signals.isEmpty()) {
            "Connected Calendar & Email Signals: None synced yet."
        } else {
            "Connected Calendar & Email Signals:\n" + signals.joinToString("\n") {
                "- [${it.sourceType.uppercase()}] ${it.timestampLabel}: ${it.title} (${it.senderOrLocation}) -> ${it.summary}"
            }
        }

        val apptsBlock = if (appointments.isEmpty()) {
            "Existing Appointments: None."
        } else {
            "Existing Appointments:\n" + appointments.joinToString("\n") {
                "- [${it.status}] ${it.scheduledDateTime}: ${it.title} with ${it.providerName} at ${it.facilityName}"
            }
        }

        return "$profileBlock\n\n$medsBlock\n\n$signalsBlock\n\n$apptsBlock"
    }

    companion object {
        private const val TAG = "PersonalHealthAgent"
    }
}
