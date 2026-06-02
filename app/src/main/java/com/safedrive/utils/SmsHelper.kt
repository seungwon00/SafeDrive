package com.safedrive.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * SMS 전송 헬퍼
 * - 급발진 발생 시 보호자에게 알림 전송
 */
class SmsHelper(private val context: Context) {
    
    /**
     * 급발진 경고 SMS 전송 (백그라운드 자동 전송)
     * @param phoneNumber 보호자 전화번호
     * @param guardianName 보호자 이름
     * @param currentSpeed 현재 속도
     * @param latitude 위도
     * @param longitude 경도
     */
    fun sendEmergencySms(
        phoneNumber: String,
        guardianName: String,
        currentSpeed: Float,
        latitude: Double,
        longitude: Double
    ) {
        // SMS 메시지 구성
        val message = """
[SafeDrive 긴급 알림]

${guardianName}님, 
운전자에게 급발진 의심 상황이 발생했습니다!

현재 속도: ${currentSpeed.toInt()} km/h
위치: https://maps.google.com/?q=${latitude},${longitude}

운전자의 안전을 확인해주세요.

- SafeDrive 안전 운전 앱
        """.trimIndent()
        
        // 긴급 상황이므로 백그라운드에서 자동 전송
        sendSmsViaManager(phoneNumber, message)
    }
    
    /**
     * 백업 SMS 전송 방법 (SmsManager 사용)
     */
    private fun sendSmsViaManager(phoneNumber: String, message: String) {
        // SMS 권한 확인
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        try {
            val smsManager = SmsManager.getDefault()
            
            // 메시지가 긴 경우 분할 전송
            if (message.length > 70) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 주간 리포트 SMS 전송 (Android 기본 메시지 앱 사용)
     * @param phoneNumber 보호자 전화번호
     * @param guardianName 보호자 이름
     * @param weeklyScore 주간 평균 점수
     * @param hardAccelCount 급가속 횟수
     * @param hardBrakeCount 급제동 횟수
     */
    fun sendWeeklyReport(
        phoneNumber: String,
        guardianName: String,
        weeklyScore: Int,
        hardAccelCount: Int,
        hardBrakeCount: Int
    ) {
        val message = """
[SafeDrive 주간 리포트]

${guardianName}님,
이번 주 운전 안전 점수: ${weeklyScore}점

급가속: ${hardAccelCount}회
급제동: ${hardBrakeCount}회

${if (weeklyScore >= 85) "안전하게 운전하고 있습니다! 👍" else "조금 더 안전운전이 필요합니다."}

- SafeDrive
        """.trimIndent()
        
        try {
            // Android 기본 메시지 앱 열기
            val smsIntent = Intent(Intent.ACTION_VIEW)
            smsIntent.data = Uri.parse("sms:$phoneNumber")
            smsIntent.putExtra("sms_body", message)
            smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            // 메시지 앱이 있는지 확인
            if (smsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(smsIntent)
            } else {
                // 메시지 앱이 없으면 대체 방법 사용
                sendSmsViaManager(phoneNumber, message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 실패 시 백업 방법 사용
            sendSmsViaManager(phoneNumber, message)
        }
    }
}

