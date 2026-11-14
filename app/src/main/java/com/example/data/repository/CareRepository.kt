package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.CareAppointment
import com.example.data.model.CareAuthorizationGrant
import com.example.data.model.CommsSignal
import com.example.data.model.Medication
import com.example.data.model.PatientHealthProfile
import com.example.data.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CareRepository(
    private val db: FirebaseFirestore,
    private val currentUidProvider: () -> String? = { Firebase.auth.currentUser?.uid }
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun logStreamError(error: Throwable, operationType: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && currentUidProvider() == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- USER PROFILE & ACCOUNT ROLE ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(uid)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.GET, docRef.path, uid)
                throw error
            }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertUserProfile(
        uid: String,
        accountType: AccountType,
        displayName: String,
        email: String,
        organizationName: String = "",
        licenseOrNpi: String = ""
    ) {
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "accountType" to accountType.value,
                    "displayName" to displayName.ifBlank { "CarePulse Member" }.take(100),
                    "email" to email.lowercase().take(160),
                    "organizationName" to organizationName.take(150),
                    "licenseOrNpi" to licenseOrNpi.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "accountType" to accountType.value,
                    "displayName" to displayName.ifBlank { "CarePulse Member" }.take(100),
                    "email" to email.lowercase().take(160),
                    "organizationName" to organizationName.take(150),
                    "licenseOrNpi" to licenseOrNpi.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- PATIENT HEALTH PROFILE & INSURANCE CARD ---

    fun observeHealthProfile(uid: String): Flow<PatientHealthProfile?> {
        val docRef = db.collection("users").document(uid)
            .collection("health_profile").document(PRIMARY_PROFILE_ID)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(PatientHealthProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.GET, docRef.path, uid)
                throw error
            }
    }

    suspend fun saveHealthProfile(uid: String, profile: PatientHealthProfile) {
        val docRef = db.collection("users").document(uid)
            .collection("health_profile").document(PRIMARY_PROFILE_ID)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "fullName" to profile.fullName.ifBlank { "Citizen Member" }.take(120),
                    "dateOfBirth" to profile.dateOfBirth.take(30),
                    "bloodType" to profile.bloodType.take(12),
                    "allergies" to profile.allergies.take(400),
                    "chronicConditions" to profile.chronicConditions.take(400),
                    "emergencyContact" to profile.emergencyContact.take(150),
                    "insuranceProvider" to profile.insuranceProvider.take(120),
                    "insuranceMemberId" to profile.insuranceMemberId.take(80),
                    "insuranceGroupNumber" to profile.insuranceGroupNumber.take(80),
                    "insurancePlanType" to profile.insurancePlanType.take(100),
                    "insuranceValidThru" to profile.insuranceValidThru.take(30),
                    "connectedEmail" to profile.connectedEmail.take(160),
                    "emailSyncEnabled" to profile.emailSyncEnabled,
                    "calendarSyncEnabled" to profile.calendarSyncEnabled,
                    "preferredClinicRadiusKm" to profile.preferredClinicRadiusKm.coerceIn(1L, 500L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to profile.fullName.ifBlank { "Citizen Member" }.take(120),
                    "dateOfBirth" to profile.dateOfBirth.take(30),
                    "bloodType" to profile.bloodType.take(12),
                    "allergies" to profile.allergies.take(400),
                    "chronicConditions" to profile.chronicConditions.take(400),
                    "emergencyContact" to profile.emergencyContact.take(150),
                    "insuranceProvider" to profile.insuranceProvider.take(120),
                    "insuranceMemberId" to profile.insuranceMemberId.take(80),
                    "insuranceGroupNumber" to profile.insuranceGroupNumber.take(80),
                    "insurancePlanType" to profile.insurancePlanType.take(100),
                    "insuranceValidThru" to profile.insuranceValidThru.take(30),
                    "connectedEmail" to profile.connectedEmail.take(160),
                    "emailSyncEnabled" to profile.emailSyncEnabled,
                    "calendarSyncEnabled" to profile.calendarSyncEnabled,
                    "preferredClinicRadiusKm" to profile.preferredClinicRadiusKm.coerceIn(1L, 500L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- MEDICATIONS ---

    fun observeMedications(uid: String): Flow<List<Medication>> {
        val colRef = db.collection("users").document(uid).collection("medications")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Medication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
    }

    suspend fun addMedication(uid: String, medication: Medication): String {
        val docRef = db.collection("users").document(uid).collection("medications").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to medication.name.ifBlank { "Medication" }.take(120),
            "dosage" to medication.dosage.ifBlank { "Standard dose" }.take(60),
            "frequency" to medication.frequency.take(60),
            "scheduledTime" to medication.scheduledTime.take(30),
            "timeSlot" to medication.timeSlot.takeIf { it in VALID_SLOTS } ?: "morning",
            "instructions" to medication.instructions.take(240),
            "takenToday" to medication.takenToday,
            "lastTakenDate" to medication.lastTakenDate.take(30),
            "prescribedBy" to medication.prescribedBy.take(120),
            "active" to medication.active,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun setMedicationTaken(uid: String, medicationId: String, taken: Boolean, todayDate: String) {
        require(medicationId.isNotBlank()) { "Medication ID is required for update." }
        val docRef = db.collection("users").document(uid).collection("medications").document(medicationId)
        try {
            docRef.update(mapOf(
                "takenToday" to taken,
                "lastTakenDate" to if (taken) todayDate else "",
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMedication(uid: String, medId: String) {
        val docRef = db.collection("users").document(uid).collection("medications").document(medId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- APPOINTMENTS & PREVENTIVE CARE ---

    fun observeAppointments(uid: String): Flow<List<CareAppointment>> {
        val colRef = db.collection("users").document(uid).collection("appointments")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CareAppointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
    }

    suspend fun addAppointment(uid: String, appt: CareAppointment): String {
        val docRef = db.collection("users").document(uid).collection("appointments").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to appt.title.ifBlank { "Preventive Visit" }.take(160),
            "specialty" to appt.specialty.take(100),
            "providerName" to appt.providerName.take(120),
            "facilityName" to appt.facilityName.take(160),
            "scheduledDateTime" to appt.scheduledDateTime.take(60),
            "durationMinutes" to appt.durationMinutes.coerceIn(5L, 480L),
            "status" to appt.status.takeIf { it in VALID_APPT_STATUSES } ?: "suggested",
            "agentRationale" to appt.agentRationale.take(600),
            "webCollectedPrep" to appt.webCollectedPrep.take(4000),
            "calendarSynced" to appt.calendarSynced,
            "emailNotified" to appt.emailNotified,
            "verifiedByProvider" to appt.verifiedByProvider,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAppointment(uid: String, appt: CareAppointment) {
        require(appt.id.isNotBlank()) { "Appointment ID is required for update." }
        val docRef = db.collection("users").document(uid).collection("appointments").document(appt.id)
        val updates = mapOf(
            "title" to appt.title.ifBlank { "Preventive Visit" }.take(160),
            "specialty" to appt.specialty.take(100),
            "providerName" to appt.providerName.take(120),
            "facilityName" to appt.facilityName.take(160),
            "scheduledDateTime" to appt.scheduledDateTime.take(60),
            "durationMinutes" to appt.durationMinutes.coerceIn(5L, 480L),
            "status" to appt.status.takeIf { it in VALID_APPT_STATUSES } ?: "booked",
            "agentRationale" to appt.agentRationale.take(600),
            "webCollectedPrep" to appt.webCollectedPrep.take(4000),
            "calendarSynced" to appt.calendarSynced,
            "emailNotified" to appt.emailNotified,
            "verifiedByProvider" to appt.verifiedByProvider,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAppointment(uid: String, appointmentId: String) {
        val docRef = db.collection("users").document(uid).collection("appointments").document(appointmentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- CALENDAR & EMAIL SIGNALS ---

    fun observeCommsSignals(uid: String): Flow<List<CommsSignal>> {
        val colRef = db.collection("users").document(uid).collection("comms_context")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CommsSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, colRef.path, uid)
                throw error
            }
    }

    suspend fun addCommsSignal(uid: String, signal: CommsSignal): String {
        val docRef = db.collection("users").document(uid).collection("comms_context").document()
        val payload = mapOf(
            "userId" to uid,
            "sourceType" to if (signal.sourceType == "email") "email" else "calendar",
            "title" to signal.title.ifBlank { "Event" }.take(160),
            "senderOrLocation" to signal.senderOrLocation.take(160),
            "timestampLabel" to signal.timestampLabel.take(60),
            "summary" to signal.summary.take(600),
            "actionNeeded" to signal.actionNeeded,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCommsSignal(uid: String, signalId: String) {
        val docRef = db.collection("users").document(uid).collection("comms_context").document(signalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- CARE AUTHORIZATIONS (PATIENT <-> PROVIDER/INSURER BRIDGE) ---

    fun observePatientAuthorizations(patientUid: String): Flow<List<CareAuthorizationGrant>> {
        val query = db.collection("care_authorizations")
            .whereEqualTo("patientUid", patientUid)
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CareAuthorizationGrant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "care_authorizations", patientUid)
                throw error
            }
    }

    fun observeProviderAuthorizations(providerEmail: String, currentUid: String): Flow<List<CareAuthorizationGrant>> {
        val normalizedEmail = providerEmail.trim().lowercase()
        val query = db.collection("care_authorizations")
            .whereEqualTo("grantStatus", "active")
            .whereEqualTo("providerEmail", normalizedEmail)
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CareAuthorizationGrant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "care_authorizations", currentUid)
                throw error
            }
    }

    suspend fun createCareAuthorization(grant: CareAuthorizationGrant): String {
        val docRef = db.collection("care_authorizations").document()
        val payload = mapOf(
            "patientUid" to grant.patientUid,
            "patientName" to grant.patientName.ifBlank { "Citizen Member" }.take(120),
            "patientEmail" to grant.patientEmail.lowercase().take(160),
            "providerEmail" to grant.providerEmail.trim().lowercase().take(160),
            "providerUid" to "",
            "accessCode" to grant.accessCode.take(32),
            "organizationLabel" to grant.organizationLabel.ifBlank { "Healthcare Provider" }.take(160),
            "grantStatus" to "active",
            "shareInsuranceCard" to grant.shareInsuranceCard,
            "shareMedications" to grant.shareMedications,
            "shareAppointments" to grant.shareAppointments,
            "sharePersonalClinical" to grant.sharePersonalClinical,
            "insuranceSnapshot" to grant.insuranceSnapshot.take(2000),
            "medicationsSnapshot" to grant.medicationsSnapshot.take(3000),
            "appointmentsSnapshot" to grant.appointmentsSnapshot.take(3000),
            "clinicalSnapshot" to grant.clinicalSnapshot.take(2000),
            "verificationStatus" to "pending",
            "verificationNotes" to "",
            "verifiedByEmail" to "",
            "verifiedAtLabel" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateGrantAsPatient(grant: CareAuthorizationGrant) {
        require(grant.id.isNotBlank()) { "Grant ID is required for update." }
        val docRef = db.collection("care_authorizations").document(grant.id)
        val updates = mapOf(
            "providerEmail" to grant.providerEmail.trim().lowercase().take(160),
            "organizationLabel" to grant.organizationLabel.ifBlank { "Healthcare Provider" }.take(160),
            "grantStatus" to if (grant.grantStatus == "revoked") "revoked" else "active",
            "shareInsuranceCard" to grant.shareInsuranceCard,
            "shareMedications" to grant.shareMedications,
            "shareAppointments" to grant.shareAppointments,
            "sharePersonalClinical" to grant.sharePersonalClinical,
            "insuranceSnapshot" to grant.insuranceSnapshot.take(2000),
            "medicationsSnapshot" to grant.medicationsSnapshot.take(3000),
            "appointmentsSnapshot" to grant.appointmentsSnapshot.take(3000),
            "clinicalSnapshot" to grant.clinicalSnapshot.take(2000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun revokeGrant(grantId: String) {
        val docRef = db.collection("care_authorizations").document(grantId)
        try {
            docRef.update(
                mapOf(
                    "grantStatus" to "revoked",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun verifyGrantAsProvider(
        grantId: String,
        providerUid: String,
        providerEmail: String,
        verificationStatus: String,
        verificationNotes: String,
        verifiedAtLabel: String
    ) {
        val docRef = db.collection("care_authorizations").document(grantId)
        val status = verificationStatus.takeIf { it in setOf("pending", "verified", "flagged") } ?: "verified"
        val updates = mapOf(
            "providerUid" to providerUid,
            "verificationStatus" to status,
            "verificationNotes" to verificationNotes.take(1000),
            "verifiedByEmail" to providerEmail.trim().lowercase().take(160),
            "verifiedAtLabel" to verifiedAtLabel.take(60),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun getAuthorizationById(grantId: String): CareAuthorizationGrant? {
        val docRef = db.collection("care_authorizations").document(grantId)
        return try {
            docRef.get().await()
                .toObject(CareAuthorizationGrant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    companion object {
        const val PRIMARY_PROFILE_ID = "primary"
        private val VALID_SLOTS = setOf("morning", "afternoon", "evening", "night")
        private val VALID_APPT_STATUSES = setOf("suggested", "approved", "booked", "completed", "cancelled")
    }
}
