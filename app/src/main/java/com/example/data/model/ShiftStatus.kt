package com.example.data.model

enum class ShiftStatus(val label: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SCHEDULED("Scheduled"),
    COMPLETED("Completed");

    companion object {
        fun fromString(value: String): ShiftStatus {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
            } ?: ACTIVE
        }
    }
}
