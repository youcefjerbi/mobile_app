package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AccountType(val value: String, val label: String) {
    PATIENT("patient", "Citizen / Patient"),
    PROVIDER("provider", "Healthcare Provider / Insurer");

    companion object {
        fun fromValue(value: String?): AccountType =
            entries.firstOrNull { it.value == value } ?: PATIENT
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountType: String = AccountType.PATIENT.value,
    val displayName: String = "",
    val email: String = "",
    val organizationName: String = "",
    val licenseOrNpi: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PatientHealthProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val emergencyContact: String = "",
    val insuranceProvider: String = "",
    val insuranceMemberId: String = "",
    val insuranceGroupNumber: String = "",
    val insurancePlanType: String = "",
    val insuranceValidThru: String = "",
    val connectedEmail: String = "",
    val emailSyncEnabled: Boolean = false,
    val calendarSyncEnabled: Boolean = false,
    val preferredClinicRadiusKm: Long = 15L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Medication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val scheduledTime: String = "",
    val timeSlot: String = "morning",
    val instructions: String = "",
    val takenToday: Boolean = false,
    val lastTakenDate: String = "",
    val prescribedBy: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CareAppointment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val specialty: String = "",
    val providerName: String = "",
    val facilityName: String = "",
    val scheduledDateTime: String = "",
    val durationMinutes: Long = 30L,
    val status: String = "suggested",
    val agentRationale: String = "",
    val webCollectedPrep: String = "",
    val calendarSynced: Boolean = false,
    val emailNotified: Boolean = false,
    val verifiedByProvider: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommsSignal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceType: String = "calendar",
    val title: String = "",
    val senderOrLocation: String = "",
    val timestampLabel: String = "",
    val summary: String = "",
    val actionNeeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CareAuthorizationGrant(
    @DocumentId val id: String = "",
    val patientUid: String = "",
    val patientName: String = "",
    val patientEmail: String = "",
    val providerEmail: String = "",
    val providerUid: String = "",
    val accessCode: String = "",
    val organizationLabel: String = "",
    val grantStatus: String = "active",
    val shareInsuranceCard: Boolean = true,
    val shareMedications: Boolean = true,
    val shareAppointments: Boolean = true,
    val sharePersonalClinical: Boolean = true,
    val insuranceSnapshot: String = "",
    val medicationsSnapshot: String = "",
    val appointmentsSnapshot: String = "",
    val clinicalSnapshot: String = "",
    val verificationStatus: String = "pending",
    val verificationNotes: String = "",
    val verifiedByEmail: String = "",
    val verifiedAtLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
