package com.example.data.model

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    LATE("Late"),
    HALF_DAY("Half Day"),
    ABSENT("Absent"),
    ON_LEAVE("On Leave");

    companion object {
        fun fromString(value: String): AttendanceStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: PRESENT
        }
    }
}
