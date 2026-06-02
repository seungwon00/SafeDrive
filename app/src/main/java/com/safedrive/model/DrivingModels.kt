package com.safedrive.model

data class DrivingState(
    val isActive: Boolean = false,
    val currentSpeedKmh: Float = 0f,
    val hardAccelerationCount: Int = 0,
    val hardBrakingCount: Int = 0
)

data class DrivingSession(
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val distanceMeters: Float,
    val hardAccelerationCount: Int,
    val hardBrakingCount: Int,
    val averageSpeedKmh: Float
) {
    val safetyScore: Int
        get() = (100 - (hardAccelerationCount * 5) - (hardBrakingCount * 3))
            .coerceIn(0, 100)

    fun toDriveLog(id: Long): DriveLog {
        return DriveLog(
            id = id,
            date = formatDate(startTime),
            time = formatTime(startTime),
            duration = formatDuration(durationMs),
            distance = String.format("%.1f km", distanceMeters / 1000f),
            hardAccelerationCount = hardAccelerationCount,
            hardBrakingCount = hardBrakingCount
        )
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = (durationMs / 1000 / 60).toInt()
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}시간 ${mins}분" else "${mins}분"
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREAN)
        return sdf.format(java.util.Date(timestamp))
    }
}

enum class DrivingContext {
    HIGH_SPEED,
    LOW_SPEED
}
