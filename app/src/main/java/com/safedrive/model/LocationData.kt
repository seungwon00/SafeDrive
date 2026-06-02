package com.safedrive.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val speedKmh: Float,
    val accuracy: Float,
    val timestamp: Long,
    val isAccurate: Boolean = true
)
