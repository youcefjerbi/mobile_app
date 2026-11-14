package com.example.ui.patient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.authStateFlow
import com.example.data.model.*
import com.example.data.repository.CareRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PatientState(
    val signedIn: Boolean = false,
    val demo: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val name: String = "",
    val email: String = "",
    val profile: PatientHealthProfile = PatientHealthProfile(),
    val medications: List<Medication> = emptyList(),
    val appointments: List<CareAppointment> = emptyList(),
    val today: String = CareRules.today()
)

class PatientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository by lazy { CareRepository(application) }
    private val auth = Firebase.auth
    private val mutableState = MutableStateFlow(PatientState())
    val state = mutableState.asStateFlow()
    private var recordsJob: Job? = null
    private var writeJob: Job? = null
    private var session = 0

    init {
        viewModelScope.launch {
            auth.authStateFlow().collect { user ->
                if (user != null || !state.value.demo) {
                    resetSession()
                    if (user == null) mutableState.value = PatientState(loading = false)
                    else loadPatient()
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                mutableState.update { it.copy(today = CareRules.today()) }
            }
        }
    }

    private fun resetSession() {
        session++
        recordsJob?.cancel()
        writeJob?.cancel()
    }

    fun loadPatient() {
        val user = auth.currentUser ?: return
        recordsJob?.cancel()
        mutableState.value = PatientState(signedIn = true, name = user.displayName.orEmpty(), email = user.email.orEmpty())
        recordsJob = viewModelScope.launch {
            try {
                val account = repository.getUserProfile(user.uid)
                check(account == null || account.accountType == AccountType.PATIENT.value) {
                    "This version supports patient accounts. Your provider account has not been changed."
                }
                if (account == null) repository.upsertUserProfile(user.uid, AccountType.PATIENT, user.displayName.orEmpty(), user.email.orEmpty())
                combine(repository.observeHealthProfile(user.uid), repository.observeMedications(user.uid), repository.observeAppointments(user.uid)) { profile, medications, appointments ->
                    Triple(profile, medications, appointments)
                }.collect { (profile, medications, appointments) ->
                    mutableState.update { it.copy(loading = false, error = null,
                        profile = profile ?: PatientHealthProfile(fullName = user.displayName.orEmpty()),
                        medications = medications.filter { med -> med.active }.sortedBy { med -> med.scheduledTime },
                        appointments = appointments.sortedBy { appointment -> appointment.scheduledDateTime }) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutableState.update { it.copy(loading = false, error = e.message ?: "Could not load your records. Check your connection and retry.") }
            }
        }
    }

    fun startDemo() {
        resetSession()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 4 * 86_400_000L))
        mutableState.value = PatientState(demo = true, loading = false, name = "Alex Morgan",
            profile = PatientHealthProfile(fullName = "Alex Morgan", insuranceProvider = "Sample Health Plan"),
            medications = listOf(
                Medication(id = "demo-1", name = "Vitamin D3", dosage = "1 capsule", scheduledTime = "08:00", instructions = "With breakfast"),
                Medication(id = "demo-2", name = "Daily supplement", dosage = "1 tablet", scheduledTime = "19:00", timeSlot = "evening", instructions = "With dinner")
            ), appointments = listOf(CareAppointment(id = "demo-visit", title = "Annual wellness visit", providerName = "Dr. Sarah Chen", facilityName = "Riverside Medical Center", scheduledDateTime = "$date 10:30", status = "booked")))
    }

    fun leaveSession() {
        resetSession()
        auth.signOut()
        mutableState.value = PatientState(loading = false)
    }
    fun message(value: String) { mutableState.update { it.copy(notice = value) } }
    fun dismissMessage() { mutableState.update { it.copy(notice = null) } }

    private fun write(success: String, demoChange: (PatientState) -> PatientState, remote: suspend (String) -> Unit) {
        if (state.value.busy || state.value.loading || state.value.error != null) return
        if (state.value.demo) {
            mutableState.update { demoChange(it).copy(notice = success) }
            return
        }
        val uid = auth.currentUser?.uid ?: return
        val currentSession = session
        mutableState.update { it.copy(busy = true) }
        writeJob = viewModelScope.launch {
            try {
                withTimeout(20_000) { remote(uid) }
                if (session == currentSession) mutableState.update { it.copy(notice = success) }
            } catch (e: TimeoutCancellationException) {
                if (session == currentSession) message("Sync is taking longer than expected. Check your records before retrying; the change may still reach the server.")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (session == currentSession) message("Could not save the change. Check your connection and try again.")
            } finally {
                if (session == currentSession) mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun toggleMedication(medication: Medication) {
        val today = CareRules.today()
        val taken = !CareRules.takenOn(medication.takenToday, medication.lastTakenDate, today)
        write(if (taken) "Dose marked as taken." else "Dose mark undone.", { s ->
            s.copy(medications = s.medications.map { if (it.id == medication.id) it.copy(takenToday = taken, lastTakenDate = if (taken) today else "") else it })
        }) { uid -> repository.setMedicationTaken(uid, medication.id, taken, today) }
    }

    fun addMedication(name: String, dose: String, time: String, instructions: String): Boolean {
        val error = CareRules.medicationError(name.trim(), dose.trim(), time.trim())
        if (error != null) { message(error); return false }
        val med = Medication(id = UUID.randomUUID().toString(), name = name.trim(), dosage = dose.trim(), scheduledTime = time.trim(), instructions = instructions.trim().take(240), frequency = "Daily", timeSlot = when (time.trim().take(2).toInt()) { in 0..11 -> "morning"; in 12..16 -> "afternoon"; else -> "evening" })
        write("Medication added.", { it.copy(medications = (it.medications + med).sortedBy { m -> m.scheduledTime }) }) { repository.addMedication(it, med) }
        return true
    }

    fun removeMedication(med: Medication) = write("Medication removed.", { s -> s.copy(medications = s.medications.filterNot { it.id == med.id }) }) { repository.deleteMedication(it, med.id) }

    fun addAppointment(title: String, provider: String, facility: String, date: String): Boolean {
        if (title.isBlank() || title.length > 160 || CareRules.appointmentMillis(date) == null) {
            message("Enter a visit title and a valid date/time: YYYY-MM-DD HH:mm."); return false
        }
        val appointment = CareAppointment(id = UUID.randomUUID().toString(), title = title.trim(), providerName = provider.trim().take(120), facilityName = facility.trim().take(160), scheduledDateTime = date, status = "booked")
        write("Visit recorded. This does not book with a provider.", { it.copy(appointments = (it.appointments + appointment).sortedBy { a -> a.scheduledDateTime }) }) { repository.addAppointment(it, appointment) }
        return true
    }

    fun completeAppointment(appointment: CareAppointment) = write("Visit marked completed.", { s -> s.copy(appointments = s.appointments.map { if (it.id == appointment.id) it.copy(status = "completed") else it }) }) { repository.updateAppointment(it, appointment.copy(status = "completed")) }

    fun saveProfile(name: String, insurance: String, allergies: String, emergency: String): Boolean {
        if (name.isBlank() || name.length > 120) { message("Enter your full name (1–120 characters)."); return false }
        val profile = state.value.profile.copy(fullName = name.trim(), insuranceProvider = insurance.trim().take(120), allergies = allergies.trim().take(400), emergencyContact = emergency.trim().take(150))
        write("Profile saved.", { it.copy(profile = profile) }) { repository.saveHealthProfile(it, profile) }
        return true
    }
}
