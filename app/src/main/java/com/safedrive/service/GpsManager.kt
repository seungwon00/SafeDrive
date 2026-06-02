package com.safedrive.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.safedrive.model.LocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

class GpsManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val speedBuffer = mutableListOf<Float>()
    private val maxBufferSize = 1

    private val accuracyThreshold = 6.0f
    private val minMovementSpeedKmh = 5.0f

    private var lastGpsUpdateTime = 0L
    private var lastValidSpeed = 0f
    private val gpsTimeoutMs = 2000L

    fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            100L
        ).apply {
            setMinUpdateIntervalMillis(100L)
            setMaxUpdateDelayMillis(200L)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (location.accuracy > accuracyThreshold) {
                        val currentAvgSpeed = if (speedBuffer.isNotEmpty()) {
                            speedBuffer.average().toFloat()
                        } else {
                            0f
                        }

                        trySend(LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            speedMps = currentAvgSpeed,
                            speedKmh = currentAvgSpeed * 3.6f,
                            accuracy = location.accuracy,
                            timestamp = location.time,
                            isAccurate = false
                        ))
                        return
                    }

                    val rawSpeedMps = if (location.hasSpeed()) location.speed else 0f

                    val smoothedSpeedMps: Float
                    val smoothedSpeedKmh: Float

                    if (rawSpeedMps < 1.5f) {
                        speedBuffer.clear()
                        speedBuffer.add(0f)
                        smoothedSpeedMps = 0f
                        smoothedSpeedKmh = 0f
                    } else {
                        speedBuffer.add(rawSpeedMps)
                        if (speedBuffer.size > maxBufferSize) {
                            speedBuffer.removeAt(0)
                        }
                        smoothedSpeedMps = speedBuffer.average().toFloat()
                        smoothedSpeedKmh = smoothedSpeedMps * 3.6f
                    }

                    lastGpsUpdateTime = System.currentTimeMillis()
                    lastValidSpeed = smoothedSpeedMps

                    trySend(LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speedMps = smoothedSpeedMps,
                        speedKmh = smoothedSpeedKmh,
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        isAccurate = true
                    ))
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close()
            return@callbackFlow
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            speedBuffer.clear()
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun resetSpeedBuffer() {
        speedBuffer.clear()
    }

    fun isStationary(speedKmh: Float): Boolean {
        return speedKmh < minMovementSpeedKmh
    }

    fun isGpsLost(): Boolean {
        return (System.currentTimeMillis() - lastGpsUpdateTime) > gpsTimeoutMs
    }

    fun getLastValidSpeed(): Float {
        return lastValidSpeed
    }
}
