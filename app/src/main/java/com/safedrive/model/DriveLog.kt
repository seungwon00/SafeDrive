package com.safedrive.model

import androidx.compose.ui.graphics.Color

data class DriveLog(
    val id: Long,
    val date: String,
    val time: String,
    val duration: String,
    val distance: String,
    val hardAccelerationCount: Int = 0,
    val hardBrakingCount: Int = 0
) {
    val score: Int
        get() = (100 - (hardAccelerationCount * 5) - (hardBrakingCount * 3))
            .coerceIn(0, 100)

    val statusLabel: String
        get() = when {
            score >= 85 -> "정상"
            score >= 70 -> "주의"
            else -> "경고"
        }

    val statusColor: Color
        get() = when {
            score >= 85 -> Color(0xFF4CAF50)
            score >= 70 -> Color(0xFFFFA726)
            else -> Color(0xFFF44336)
        }

    val hasEvent: Boolean
        get() = score < 70
}

enum class DriveLogFilter {
    ALL,
    EVENT
}
