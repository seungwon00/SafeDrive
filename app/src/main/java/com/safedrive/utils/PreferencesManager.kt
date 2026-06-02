package com.safedrive.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 관리자
 * - 알림 설정
 * - 보호자 정보 저장
 */
class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREF_NAME = "safedrive_prefs"
        
        // 알림 설정
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        
        // 보호자 정보
        private const val KEY_GUARDIAN_NAME = "guardian_name"
        private const val KEY_GUARDIAN_PHONE = "guardian_phone"
        
        // 차량 종류
        private const val KEY_VEHICLE_TYPE = "vehicle_type"
        
        // 기타 설정
        private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
    }
    
    /**
     * 차량 종류 enum
     */
    enum class VehicleType {
        NORMAL,      // 일반 차량
        SMART_KEY    // 스마트키 차량
    }
    
    // ============ 알림 설정 ============
    
    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()
    
    // ============ 보호자 정보 ============
    
    var guardianName: String
        get() = prefs.getString(KEY_GUARDIAN_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GUARDIAN_NAME, value).apply()
    
    var guardianPhone: String
        get() = prefs.getString(KEY_GUARDIAN_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GUARDIAN_PHONE, value).apply()
    
    /**
     * 보호자 정보 저장
     */
    fun saveGuardianInfo(name: String, phone: String) {
        prefs.edit().apply {
            putString(KEY_GUARDIAN_NAME, name)
            putString(KEY_GUARDIAN_PHONE, phone)
            apply()
        }
    }
    
    /**
     * 보호자 정보 존재 여부
     */
    fun hasGuardianInfo(): Boolean {
        return guardianName.isNotEmpty() && guardianPhone.isNotEmpty()
    }
    
    /**
     * 보호자 정보 가져오기
     */
    fun getGuardianInfo(): Pair<String, String> {
        return Pair(guardianName, guardianPhone)
    }
    
    // ============ 기타 설정 ============
    
    var isAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_ENABLED, value).apply()
    
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
    
    // ============ 차량 종류 ============
    
    /**
     * 차량 종류 저장
     */
    var vehicleType: VehicleType
        get() {
            val typeName = prefs.getString(KEY_VEHICLE_TYPE, VehicleType.NORMAL.name)
            return try {
                VehicleType.valueOf(typeName ?: VehicleType.NORMAL.name)
            } catch (e: Exception) {
                VehicleType.NORMAL
            }
        }
        set(value) = prefs.edit().putString(KEY_VEHICLE_TYPE, value.name).apply()
    
    /**
     * 차량 종류가 설정되었는지 확인
     */
    fun hasVehicleType(): Boolean {
        return prefs.contains(KEY_VEHICLE_TYPE)
    }
    
    /**
     * 차량 종류 한글 이름
     */
    fun getVehicleTypeName(): String {
        return when (vehicleType) {
            VehicleType.NORMAL -> "일반 차량"
            VehicleType.SMART_KEY -> "스마트키 차량"
        }
    }
    
    /**
     * 모든 설정 초기화
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

