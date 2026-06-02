package com.safedrive.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.safedrive.model.DrivingContext
import com.safedrive.model.DrivingSession
import com.safedrive.model.DrivingState
import com.safedrive.model.LocationData
import com.safedrive.repository.SafeDriveDbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class DrivingStateManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val dbHelper = SafeDriveDbHelper(context)

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val highSpeedThresholdKmh = 60.0f

    private val hardAccelThresholdHighSpeed = 3.5f
    private val hardAccelThresholdLowSpeed = 2.5f

    private val hardBrakeThresholdHighSpeed = -3.5f
    private val hardBrakeThresholdLowSpeed = -2.5f

    private val eventDurationThresholdMs = 800L

    private val minValidDistanceMeters = 10f
    private val maxStationaryTimeRatio = 0.8f

    private val _drivingState = MutableStateFlow(DrivingState())
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    private var currentSpeedKmh = 0f

    private var accelerationStartTime = 0L
    private var lastAcceleration = 0f

    private var sessionStartTime = 0L
    private var totalDistanceMeters = 0f
    private var stationaryTimeMs = 0L
    private var lastLocationData: LocationData? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        val accelThreshold = if (currentSpeedKmh > highSpeedThresholdKmh) {
            hardAccelThresholdHighSpeed
        } else {
            hardAccelThresholdLowSpeed
        }

        val brakeThreshold = if (currentSpeedKmh > highSpeedThresholdKmh) {
            hardBrakeThresholdHighSpeed
        } else {
            hardBrakeThresholdLowSpeed
        }

        if (acceleration > accelThreshold) {
            if (accelerationStartTime == 0L) {
                accelerationStartTime = System.currentTimeMillis()
                lastAcceleration = acceleration
            } else {
                val duration = System.currentTimeMillis() - accelerationStartTime
                if (duration >= eventDurationThresholdMs) {
                    _drivingState.value = _drivingState.value.copy(
                        hardAccelerationCount = _drivingState.value.hardAccelerationCount + 1
                    )
                    accelerationStartTime = 0L
                }
            }
        } else if (acceleration < brakeThreshold) {
            if (accelerationStartTime == 0L) {
                accelerationStartTime = System.currentTimeMillis()
                lastAcceleration = acceleration
            } else {
                val duration = System.currentTimeMillis() - accelerationStartTime
                if (duration >= eventDurationThresholdMs) {
                    _drivingState.value = _drivingState.value.copy(
                        hardBrakingCount = _drivingState.value.hardBrakingCount + 1
                    )
                    accelerationStartTime = 0L
                }
            }
        } else {
            accelerationStartTime = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun updateSpeed(speedKmh: Float) {
        currentSpeedKmh = speedKmh
        _drivingState.value = _drivingState.value.copy(currentSpeedKmh = speedKmh)
    }

    fun updateLocation(locationData: LocationData) {
        if (!locationData.isAccurate) return

        lastLocationData?.let { prevLocation ->
            val distance = calculateDistance(
                prevLocation.latitude, prevLocation.longitude,
                locationData.latitude, locationData.longitude
            )
            totalDistanceMeters += distance

            if (locationData.speedKmh < 5.0f) {
                val timeDiff = locationData.timestamp - prevLocation.timestamp
                stationaryTimeMs += timeDiff
            }
        }

        lastLocationData = locationData
        updateSpeed(locationData.speedKmh)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun startDriving() {
        sessionStartTime = System.currentTimeMillis()
        totalDistanceMeters = 0f
        stationaryTimeMs = 0L
        lastLocationData = null

        _drivingState.value = DrivingState(
            isActive = true,
            currentSpeedKmh = 0f,
            hardAccelerationCount = 0,
            hardBrakingCount = 0
        )

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopDriving(): DrivingSession? {
        val endTime = System.currentTimeMillis()
        val totalDurationMs = endTime - sessionStartTime

        sensorManager.unregisterListener(this)

        if (!validateDrivingSession(totalDurationMs)) {
            _drivingState.value = _drivingState.value.copy(isActive = false)
            return null
        }

        val session = DrivingSession(
            startTime = sessionStartTime,
            endTime = endTime,
            durationMs = totalDurationMs,
            distanceMeters = totalDistanceMeters,
            hardAccelerationCount = _drivingState.value.hardAccelerationCount,
            hardBrakingCount = _drivingState.value.hardBrakingCount,
            averageSpeedKmh = calculateAverageSpeed(totalDistanceMeters, totalDurationMs)
        )

        val driveLog = session.toDriveLog(id = System.currentTimeMillis())
        dbHelper.insertDriveLog(driveLog)

        _drivingState.value = _drivingState.value.copy(isActive = false)

        return session
    }

    private fun validateDrivingSession(totalDurationMs: Long): Boolean {
        if (totalDistanceMeters < minValidDistanceMeters) return false

        val stationaryRatio = stationaryTimeMs.toFloat() / totalDurationMs.toFloat()
        if (stationaryRatio > maxStationaryTimeRatio) return false

        return true
    }

    private fun calculateAverageSpeed(distanceMeters: Float, durationMs: Long): Float {
        if (durationMs == 0L) return 0f
        val durationHours = durationMs / 1000f / 3600f
        return (distanceMeters / 1000f) / durationHours
    }

    fun recordSuddenAcceleration() {
        _drivingState.value = _drivingState.value.copy(
            hardAccelerationCount = _drivingState.value.hardAccelerationCount + 1
        )
    }

    fun getDrivingContext(): DrivingContext {
        return if (currentSpeedKmh > highSpeedThresholdKmh) {
            DrivingContext.HIGH_SPEED
        } else {
            DrivingContext.LOW_SPEED
        }
    }
}
