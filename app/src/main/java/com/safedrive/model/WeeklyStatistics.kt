package com.safedrive.model

data class WeeklyStatistics(
    val totalHardAcceleration: Int,
    val totalHardBraking: Int,
    val averageHardAcceleration: Float,
    val averageHardBraking: Float,
    val weeklyHardAccelCounts: List<Int>
)
