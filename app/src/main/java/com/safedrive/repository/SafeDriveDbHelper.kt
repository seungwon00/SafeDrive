package com.safedrive.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.safedrive.model.DriveLog
import com.safedrive.model.WeeklyStatistics
import java.text.SimpleDateFormat
import java.util.*

class SafeDriveDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "safedrive.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_DRIVE_LOG = "drive_log"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_DURATION = "duration"
        private const val COLUMN_DISTANCE = "distance"
        private const val COLUMN_HARD_ACCEL = "hard_acceleration_count"
        private const val COLUMN_HARD_BRAKE = "hard_braking_count"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_DRIVE_LOG (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_DURATION TEXT NOT NULL,
                $COLUMN_DISTANCE TEXT NOT NULL,
                $COLUMN_HARD_ACCEL INTEGER NOT NULL,
                $COLUMN_HARD_BRAKE INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()

        db?.execSQL(createTable)
        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_DRIVE_LOG")
        onCreate(db)
    }

    private fun insertSampleData(db: SQLiteDatabase?) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("a h:mm", Locale.KOREAN)

        val sampleData = listOf(
            SampleDrive(0, 0, 0, "45분", "23.5 km", "오후 2:30"),
            SampleDrive(-1, 0, 1, "35분", "18.2 km", "오전 9:15"),
            SampleDrive(-2, 1, 0, "1시간 5분", "45.3 km", "오후 6:20"),
            SampleDrive(-4, 0, 1, "50분", "32.1 km", "오전 8:40"),
            SampleDrive(-5, 1, 1, "40분", "25.8 km", "오후 3:15"),
            SampleDrive(-6, 0, 2, "1시간 15분", "58.6 km", "오후 5:50"),
            SampleDrive(-9, 2, 2, "55분", "38.4 km", "오전 10:20"),
            SampleDrive(-10, 1, 3, "1시간", "42.7 km", "오후 2:40"),
            SampleDrive(-12, 0, 1, "30분", "16.9 km", "오후 7:10"),
            SampleDrive(-16, 0, 0, "45분", "28.3 km", "오전 7:50"),
            SampleDrive(-17, 1, 1, "50분", "33.5 km", "오후 1:20"),
            SampleDrive(-19, 0, 2, "1시간 10분", "52.1 km", "오후 4:30"),
            SampleDrive(-23, 5, 3, "40분", "22.6 km", "오전 11:10"),
            SampleDrive(-24, 1, 1, "35분", "19.4 km", "오후 3:40"),
            SampleDrive(-26, 0, 1, "1시간", "45.8 km", "오후 6:15"),
            SampleDrive(-30, 0, 0, "55분", "35.2 km", "오전 8:30"),
            SampleDrive(-31, 1, 2, "45분", "28.7 km", "오후 2:10"),
            SampleDrive(-33, 0, 1, "1시간 20분", "62.4 km", "오후 5:20"),
            SampleDrive(-37, 6, 4, "35분", "18.9 km", "오전 10:50"),
            SampleDrive(-38, 2, 3, "50분", "31.6 km", "오후 1:40"),
            SampleDrive(-40, 0, 0, "40분", "24.3 km", "오후 4:50"),
        )

        sampleData.forEach { sample ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, sample.dayOffset)

            val values = ContentValues().apply {
                put(COLUMN_DATE, dateFormat.format(calendar.time))
                put(COLUMN_TIME, sample.time)
                put(COLUMN_DURATION, sample.duration)
                put(COLUMN_DISTANCE, sample.distance)
                put(COLUMN_HARD_ACCEL, sample.hardAccel)
                put(COLUMN_HARD_BRAKE, sample.hardBrake)
                put(COLUMN_TIMESTAMP, calendar.timeInMillis)
            }

            db?.insert(TABLE_DRIVE_LOG, null, values)
        }
    }

    private data class SampleDrive(
        val dayOffset: Int,
        val hardAccel: Int,
        val hardBrake: Int,
        val duration: String,
        val distance: String,
        val time: String
    )

    fun insertDriveLog(driveLog: DriveLog): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, driveLog.date)
            put(COLUMN_TIME, driveLog.time)
            put(COLUMN_DURATION, driveLog.duration)
            put(COLUMN_DISTANCE, driveLog.distance)
            put(COLUMN_HARD_ACCEL, driveLog.hardAccelerationCount)
            put(COLUMN_HARD_BRAKE, driveLog.hardBrakingCount)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }

        return db.insert(TABLE_DRIVE_LOG, null, values)
    }

    fun getAllDriveLogs(): List<DriveLog> {
        val logs = mutableListOf<DriveLog>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DRIVE_LOG, null, null, null, null, null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                logs.add(DriveLog(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                    duration = it.getString(it.getColumnIndexOrThrow(COLUMN_DURATION)),
                    distance = it.getString(it.getColumnIndexOrThrow(COLUMN_DISTANCE)),
                    hardAccelerationCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_HARD_ACCEL)),
                    hardBrakingCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_HARD_BRAKE))
                ))
            }
        }

        return logs
    }

    fun getDriveLogsForWeeks(weeks: Int): List<DriveLog> {
        val logs = mutableListOf<DriveLog>()
        val db = readableDatabase
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val timestampThreshold = calendar.timeInMillis

        val cursor = db.query(
            TABLE_DRIVE_LOG, null,
            "$COLUMN_TIMESTAMP >= ?",
            arrayOf(timestampThreshold.toString()),
            null, null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                logs.add(DriveLog(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                    duration = it.getString(it.getColumnIndexOrThrow(COLUMN_DURATION)),
                    distance = it.getString(it.getColumnIndexOrThrow(COLUMN_DISTANCE)),
                    hardAccelerationCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_HARD_ACCEL)),
                    hardBrakingCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_HARD_BRAKE))
                ))
            }
        }

        return logs
    }

    fun getWeeklyStatistics(weeks: Int = 6): WeeklyStatistics {
        val logs = getDriveLogsForWeeks(weeks)

        val totalHardAccel = logs.sumOf { it.hardAccelerationCount }
        val totalHardBrake = logs.sumOf { it.hardBrakingCount }

        val avgHardAccel = if (logs.isNotEmpty()) totalHardAccel.toFloat() / weeks else 0f
        val avgHardBrake = if (logs.isNotEmpty()) totalHardBrake.toFloat() / weeks else 0f

        return WeeklyStatistics(
            totalHardAcceleration = totalHardAccel,
            totalHardBraking = totalHardBrake,
            averageHardAcceleration = avgHardAccel,
            averageHardBraking = avgHardBrake,
            weeklyHardAccelCounts = getWeeklyHardAccelerationCounts(weeks)
        )
    }

    private fun getWeeklyHardAccelerationCounts(weeks: Int): List<Int> {
        val calendar = Calendar.getInstance()
        val weeklyData = mutableListOf<Int>()

        for (weekOffset in 0 until weeks) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.WEEK_OF_YEAR, -weekOffset)

            val weekStart = calendar.apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val weekEnd = calendar.apply {
                add(Calendar.WEEK_OF_YEAR, 1)
            }.timeInMillis

            val db = readableDatabase
            val cursor = db.query(
                TABLE_DRIVE_LOG,
                arrayOf(COLUMN_HARD_ACCEL),
                "$COLUMN_TIMESTAMP >= ? AND $COLUMN_TIMESTAMP < ?",
                arrayOf(weekStart.toString(), weekEnd.toString()),
                null, null, null
            )

            var weeklyCount = 0
            cursor.use {
                while (it.moveToNext()) {
                    weeklyCount += it.getInt(it.getColumnIndexOrThrow(COLUMN_HARD_ACCEL))
                }
            }

            weeklyData.add(0, weeklyCount)
        }

        return weeklyData
    }
}
