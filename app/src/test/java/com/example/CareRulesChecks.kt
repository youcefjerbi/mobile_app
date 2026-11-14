package com.example

import com.example.data.model.CareRules

/** Also runnable without an Android SDK using the Kotlin CLI. */
fun main() {
    check(CareRules.takenOn(true, "2026-11-14", "2026-11-14"))
    check(!CareRules.takenOn(true, "2026-11-13", "2026-11-14"))
    check(!CareRules.takenOn(false, "2026-11-14", "2026-11-14"))
    check(!CareRules.takenOn(true, "", "2026-11-14"))
    listOf("00:00", "08:00", "23:59").forEach { check(CareRules.validTime(it)) }
    listOf("24:00", "08:60", "8:00", "", "08:00 AM").forEach { check(!CareRules.validTime(it)) }
    check(CareRules.medicationError("Prescription", "1 tablet", "08:00") == null)
    check(CareRules.medicationError(" ", "1 tablet", "08:00") != null)
    check(CareRules.medicationError("a".repeat(121), "1 tablet", "08:00") != null)
    check(CareRules.medicationError("Prescription", "", "08:00") != null)
    check(CareRules.medicationError("Prescription", "1 tablet", "25:00") != null)
    check(CareRules.appointmentMillis("2028-02-29 10:30") != null)
    listOf("2026-02-29 10:30", "2026-04-31 10:30", "2026-11-14 24:00", "2026-11-14 10:30 extra", "2026-1-1 10:30", "").forEach { check(CareRules.appointmentMillis(it) == null) { "Accepted invalid date: $it" } }
    println("PASS: daily rollover, dose undo semantics, time validation, required fields, length limits, leap years and strict appointment dates")
}
