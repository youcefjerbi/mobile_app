const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const { serverTimestamp } = require("firebase/firestore");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PATIENT_UID = "patient_alice_123";
const PATIENT_EMAIL = "alice@citizen.org";
const PROVIDER_UID = "provider_bob_456";
const PROVIDER_EMAIL = "dr.bob@clinic.org";
const OTHER_UID = "intruder_eve_789";
const OTHER_EMAIL = "eve@intruder.org";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function patientDb() {
  return testEnv.authenticatedContext(PATIENT_UID, {
    email: PATIENT_EMAIL,
    email_verified: true,
  }).firestore();
}

function providerDb(verified = true) {
  return testEnv.authenticatedContext(PROVIDER_UID, {
    email: PROVIDER_EMAIL,
    email_verified: verified,
  }).firestore();
}

function intruderDb() {
  return testEnv.authenticatedContext(OTHER_UID, {
    email: OTHER_EMAIL,
    email_verified: true,
  }).firestore();
}

function validGrantPayload(overrides = {}) {
  return {
    patientUid: PATIENT_UID,
    patientName: "Alice Citizen",
    patientEmail: PATIENT_EMAIL,
    providerEmail: PROVIDER_EMAIL,
    providerUid: "",
    accessCode: "CP8821QX",
    organizationLabel: "Nordic City Clinic",
    grantStatus: "active",
    shareInsuranceCard: true,
    shareMedications: true,
    shareAppointments: true,
    sharePersonalClinical: true,
    insuranceSnapshot: "CivicShield Health | Member ID: CS-994201",
    medicationsSnapshot: "Atorvastatin 20mg (08:00)",
    appointmentsSnapshot: "Annual Preventive Lipid Screening",
    clinicalSnapshot: "Blood Type O+ | Allergies: Penicillin",
    verificationStatus: "pending",
    verificationNotes: "",
    verifiedByEmail: "",
    verifiedAtLabel: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or write users, medications, or care_authorizations", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(PATIENT_UID).get());
  await assertFails(unauth.collection("users").doc(PATIENT_UID).collection("medications").get());
  await assertFails(unauth.collection("care_authorizations").get());
});

test("Patient owner: can create and read own UserProfile, HealthProfile, Medication, Appointment, CommsSignal", async () => {
  const db = patientDb();
  await assertSucceeds(db.collection("users").doc(PATIENT_UID).set({
    userId: PATIENT_UID,
    accountType: "patient",
    displayName: "Alice Citizen",
    email: PATIENT_EMAIL,
    organizationName: "",
    licenseOrNpi: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(db.collection("users").doc(PATIENT_UID).collection("health_profile").doc("primary").set({
    userId: PATIENT_UID,
    fullName: "Alice Citizen",
    dateOfBirth: "1991-04-18",
    bloodType: "O+",
    allergies: "Penicillin",
    chronicConditions: "Mild hyperlipidemia",
    emergencyContact: "John Citizen +1-555-0100",
    insuranceProvider: "CivicShield Health",
    insuranceMemberId: "CS-994201",
    insuranceGroupNumber: "GRP-401",
    insurancePlanType: "PPO Preventive Plus",
    insuranceValidThru: "12/2027",
    connectedEmail: PATIENT_EMAIL,
    emailSyncEnabled: true,
    calendarSyncEnabled: true,
    preferredClinicRadiusKm: 15,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(db.collection("users").doc(PATIENT_UID).collection("medications").doc("med1").set({
    userId: PATIENT_UID,
    name: "Atorvastatin",
    dosage: "20 mg",
    frequency: "Once daily",
    scheduledTime: "08:00",
    timeSlot: "morning",
    instructions: "Take with water",
    takenToday: false,
    lastTakenDate: "",
    prescribedBy: "Dr. Bob",
    active: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(db.collection("users").doc(PATIENT_UID).collection("appointments").doc("appt1").set({
    userId: PATIENT_UID,
    title: "Annual Preventive Lipid Panel",
    specialty: "Preventive Medicine",
    providerName: "Dr. Bob",
    facilityName: "Nordic City Clinic",
    scheduledDateTime: "2026-11-20 09:30",
    durationMinutes: 45,
    status: "suggested",
    agentRationale: "Due for annual screening",
    webCollectedPrep: "",
    calendarSynced: false,
    emailNotified: false,
    verifiedByProvider: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(db.collection("users").doc(PATIENT_UID).collection("comms_context").doc("sig1").set({
    userId: PATIENT_UID,
    sourceType: "calendar",
    title: "Team Standup",
    senderOrLocation: "Work Calendar",
    timestampLabel: "2026-11-20 11:00",
    summary: "Busy 11:00-11:30",
    actionNeeded: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(db.collection("users").doc(PATIENT_UID).collection("medications").get());
});

test("Cross-user isolation: another user cannot read or write patient private subcollections", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("users").doc(PATIENT_UID).collection("medications").doc("med1").set({
    userId: PATIENT_UID,
    name: "Vitamin D3",
    dosage: "2000 IU",
    frequency: "Daily",
    scheduledTime: "09:00",
    timeSlot: "morning",
    instructions: "With breakfast",
    takenToday: false,
    lastTakenDate: "",
    prescribedBy: "Self",
    active: true,
    createdAt: serverTimestamp(),
  }));

  const eve = intruderDb();
  await assertFails(eve.collection("users").doc(PATIENT_UID).collection("medications").doc("med1").get());
  await assertFails(eve.collection("users").doc(PATIENT_UID).collection("medications").get());
  await assertFails(eve.collection("users").doc(PATIENT_UID).collection("health_profile").doc("primary").get());
});

test("Shadow update rejection: ghost field on UserProfile fails", async () => {
  const db = patientDb();
  await assertFails(db.collection("users").doc(PATIENT_UID).set({
    userId: PATIENT_UID,
    accountType: "patient",
    displayName: "Alice",
    email: PATIENT_EMAIL,
    isAdmin: true,
    createdAt: serverTimestamp(),
  }));
});

test("CareAuthorization: authorized provider with verified email can list, view, and verify active grant", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("care_authorizations").doc("grant_1").set(validGrantPayload()));

  const bob = providerDb(true);
  await assertSucceeds(
    bob.collection("care_authorizations")
      .where("grantStatus", "==", "active")
      .where("providerEmail", "==", PROVIDER_EMAIL)
      .get()
  );

  await assertSucceeds(bob.collection("care_authorizations").doc("grant_1").update({
    providerUid: PROVIDER_UID,
    verificationStatus: "verified",
    verificationNotes: "Insurance PPO active and medication profile reviewed.",
    verifiedByEmail: PROVIDER_EMAIL,
    verifiedAtLabel: "2026-11-14 13:10",
    updatedAt: serverTimestamp(),
  }));
});

test("Email spoofing guard: provider with email_verified=false is rejected", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("care_authorizations").doc("grant_2").set(validGrantPayload()));

  const unverifiedBob = providerDb(false);
  await assertFails(unverifiedBob.collection("care_authorizations").doc("grant_2").get());
  await assertFails(
    unverifiedBob.collection("care_authorizations")
      .where("grantStatus", "==", "active")
      .where("providerEmail", "==", PROVIDER_EMAIL)
      .get()
  );
});

test("Query trust & isolation: provider cannot list unfiltered care_authorizations or read another provider's grant", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("care_authorizations").doc("grant_3").set(validGrantPayload()));

  const eve = intruderDb();
  await assertFails(eve.collection("care_authorizations").get());
  await assertFails(eve.collection("care_authorizations").doc("grant_3").get());
});

test("Revoked grant: provider immediately loses access once patient revokes grantStatus", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("care_authorizations").doc("grant_4").set(validGrantPayload()));

  const bob = providerDb(true);
  await assertSucceeds(bob.collection("care_authorizations").doc("grant_4").get());

  await assertSucceeds(alice.collection("care_authorizations").doc("grant_4").update({
    grantStatus: "revoked",
    updatedAt: serverTimestamp(),
  }));

  await assertFails(bob.collection("care_authorizations").doc("grant_4").get());
});

test("Provider cannot mutate patient clinical snapshots or grantStatus", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("care_authorizations").doc("grant_5").set(validGrantPayload()));

  const bob = providerDb(true);
  await assertFails(bob.collection("care_authorizations").doc("grant_5").update({
    insuranceSnapshot: "Tampered snapshot",
    updatedAt: serverTimestamp(),
  }));
});

test("Terminal state lock: completed appointment cannot be modified", async () => {
  const alice = patientDb();
  await assertSucceeds(alice.collection("users").doc(PATIENT_UID).collection("appointments").doc("appt_done").set({
    userId: PATIENT_UID,
    title: "Dental Cleaning",
    specialty: "Dentistry",
    providerName: "Dr. Lee",
    facilityName: "Smile Clinic",
    scheduledDateTime: "2026-10-01 10:00",
    durationMinutes: 45,
    status: "completed",
    agentRationale: "Routine 6-month cleaning",
    webCollectedPrep: "Bring ID",
    calendarSynced: true,
    emailNotified: true,
    verifiedByProvider: true,
    createdAt: serverTimestamp(),
  }));

  await assertFails(alice.collection("users").doc(PATIENT_UID).collection("appointments").doc("appt_done").update({
    title: "Edited after completion",
    updatedAt: serverTimestamp(),
  }));
});
