package com.example.model

data class ActivityLogEntry(
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String,
    val details: String,
    val isReversible: Boolean = true,
    val reverted: Boolean = false
)
