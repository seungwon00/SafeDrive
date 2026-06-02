package com.safedrive.detection

import kotlin.math.abs

/**
 * 정교한 급발진 감지 알고리즘
 * - 도로 타입 추정
 * - 가속도 패턴 분석
 * - 다중 조건 검증
 * - 오탐 최소화
 */
class SuddenAccelerationDetector {
    
    // 속도 이력 (최근 10초)
    private val speedHistory = mutableListOf<SpeedData>()
    private val maxHistorySize = 10
    
    // 가속도 이력 (최근 5초)
    private val accelerationHistory = mutableListOf<AccelerationData>()
    private val maxAccelHistorySize = 5
    
    // 도로 타입 추정
    private var estimatedRoadType = RoadType.UNKNOWN
    
    /**
     * 급발진 의심 상황 판단
     * @return 급발진 의심 여부
     */
    fun detectSuddenAcceleration(
        currentSpeed: Float,
        acceleration: Float,
        timestamp: Long,
        gpsAccuracy: Float
    ): DetectionResult {
        
        // 1. 데이터 이력에 추가
        addSpeedData(currentSpeed, timestamp)
        addAccelerationData(acceleration, timestamp)
        
        // 2. 도로 타입 추정 (속도 패턴 기반)
        estimatedRoadType = estimateRoadType()
        
        // 3. 도로 타입별 동적 임계값 설정
        val threshold = getThresholdForRoadType(estimatedRoadType)
        
        // 4. GPS 정확도 확인 (정확도가 낮으면 신뢰도 낮음)
        if (gpsAccuracy > 10f) {
            return DetectionResult(
                isSuddenAcceleration = false,
                confidence = 0f,
                reason = "GPS 정확도 낮음",
                roadType = estimatedRoadType
            )
        }
        
        // 5. 다중 조건 검증
        val conditions = mutableListOf<Boolean>()
        
        // 조건 1: 가속도가 임계값 초과
        conditions.add(acceleration > threshold.accelerationThreshold)
        
        // 조건 2: 속도가 일정 수준 이상 (도로 타입별)
        conditions.add(currentSpeed > threshold.minSpeed)
        
        // 조건 3: 가속도가 지속적으로 증가 (급격한 변화)
        conditions.add(isAccelerationIncreasing())
        
        // 조건 4: 가속 지속 시간 (2초 이상) - 오탐 방지
        conditions.add(getAccelerationDuration() >= 2000L)
        
        // 조건 5: 속도 증가율이 비정상적 (3초 내 20 km/h 이상 증가) - 낮춤
        conditions.add(isAbnormalSpeedIncrease())
        
        // 조건 6: 정상적인 추월/합류가 아님 (패턴 분석)
        conditions.add(!isNormalOvertake())
        
        // 6. 신뢰도 계산 (조건 만족 비율)
        val confidence = conditions.count { it } / conditions.size.toFloat()
        
        // 7. 급발진 판단 (3개 이상 조건 만족 시) - 완화
        val isSuddenAcceleration = conditions.count { it } >= 3
        
        // 8. 결과 반환
        return DetectionResult(
            isSuddenAcceleration = isSuddenAcceleration,
            confidence = confidence,
            reason = buildReasonString(conditions, estimatedRoadType),
            roadType = estimatedRoadType,
            currentSpeed = currentSpeed,
            acceleration = acceleration
        )
    }
    
    /**
     * 도로 타입 추정 (속도 패턴 기반)
     */
    private fun estimateRoadType(): RoadType {
        if (speedHistory.size < 5) return RoadType.UNKNOWN
        
        val recentSpeeds = speedHistory.takeLast(5).map { it.speed }
        val avgSpeed = recentSpeeds.average().toFloat()
        val speedVariance = calculateVariance(recentSpeeds)
        
        return when {
            // 고속도로: 평균 속도 80+ km/h, 속도 변화 작음
            avgSpeed > 80 && speedVariance < 100 -> RoadType.HIGHWAY
            
            // 일반 도로: 평균 속도 40-80 km/h, 속도 변화 중간
            avgSpeed in 40f..80f && speedVariance < 300 -> RoadType.MAIN_ROAD
            
            // 골목길: 평균 속도 0-40 km/h, 속도 변화 큼
            avgSpeed < 40 && speedVariance > 100 -> RoadType.ALLEY
            
            // 주차장: 평균 속도 10 km/h 이하
            avgSpeed < 10 -> RoadType.PARKING
            
            else -> RoadType.UNKNOWN
        }
    }
    
    /**
     * 도로 타입별 임계값 반환
     */
    private fun getThresholdForRoadType(roadType: RoadType): DetectionThreshold {
        return when (roadType) {
            RoadType.HIGHWAY -> DetectionThreshold(
                accelerationThreshold = 3.0f,  // 고속도로: 3.0 (낮춤)
                minSpeed = 80f,                // 최소 속도 80 km/h (낮춤)
                description = "고속도로 (합류/추월 고려)"
            )
            RoadType.MAIN_ROAD -> DetectionThreshold(
                accelerationThreshold = 3.5f,  // 일반 도로: 3.5 (상향)
                minSpeed = 50f,                // 최소 속도 50 km/h (상향)
                description = "일반 도로"
            )
            RoadType.ALLEY -> DetectionThreshold(
                accelerationThreshold = 4.0f,  // 골목길: 4.0 (오탐 방지 강화)
                minSpeed = 30f,                // 최소 속도 30 km/h (상향)
                description = "골목길 (보행자 주의)"
            )
            RoadType.PARKING -> DetectionThreshold(
                accelerationThreshold = 5.0f,  // 주차장: 5.0 (오탐 방지 강화)
                minSpeed = 20f,                // 최소 속도 20 km/h (상향)
                description = "주차장"
            )
            RoadType.UNKNOWN -> DetectionThreshold(
                accelerationThreshold = 2.5f,  // 기본값: 2.5 (낮춤)
                minSpeed = 50f,                // 최소 속도 50 km/h (낮춤)
                description = "도로 타입 분석 중"
            )
        }
    }
    
    /**
     * 가속도가 지속적으로 증가하는지 확인
     */
    private fun isAccelerationIncreasing(): Boolean {
        if (accelerationHistory.size < 3) return false
        
        val recent = accelerationHistory.takeLast(3)
        return recent[0].acceleration < recent[1].acceleration &&
               recent[1].acceleration < recent[2].acceleration
    }
    
    /**
     * 가속도 지속 시간 계산
     */
    private fun getAccelerationDuration(): Long {
        if (accelerationHistory.isEmpty()) return 0
        
        val recent = accelerationHistory.filter { it.acceleration > 2.0f }  // 3.0 → 2.0 (낮춤)
        if (recent.isEmpty()) return 0
        
        return accelerationHistory.last().timestamp - recent.first().timestamp
    }
    
    /**
     * 비정상적인 속도 증가 판단 (3초 내 20 km/h 이상) - 완화
     */
    private fun isAbnormalSpeedIncrease(): Boolean {
        if (speedHistory.size < 3) return false
        
        val recent = speedHistory.takeLast(3)
        val speedIncrease = recent.last().speed - recent.first().speed
        val timeDiff = recent.last().timestamp - recent.first().timestamp
        
        // 3초 내 20 km/h 이상 증가 → 비정상 (30 → 20 낮춤)
        return timeDiff <= 3000 && speedIncrease >= 20
    }
    
    /**
     * 정상적인 추월/합류 패턴 판단
     */
    private fun isNormalOvertake(): Boolean {
        if (speedHistory.size < 5) return false
        
        val recent = speedHistory.takeLast(5)
        val speedChanges = mutableListOf<Float>()
        
        for (i in 1 until recent.size) {
            speedChanges.add(recent[i].speed - recent[i-1].speed)
        }
        
        // 정상 추월: 점진적 가속 (변화량이 일정)
        val variance = calculateVariance(speedChanges)
        
        // 분산이 작으면 (변화가 일정하면) 정상 추월
        return variance < 50
    }
    
    /**
     * 데이터 추가
     */
    private fun addSpeedData(speed: Float, timestamp: Long) {
        speedHistory.add(SpeedData(speed, timestamp))
        if (speedHistory.size > maxHistorySize) {
            speedHistory.removeAt(0)
        }
    }
    
    private fun addAccelerationData(acceleration: Float, timestamp: Long) {
        accelerationHistory.add(AccelerationData(acceleration, timestamp))
        if (accelerationHistory.size > maxAccelHistorySize) {
            accelerationHistory.removeAt(0)
        }
    }
    
    /**
     * 분산 계산
     */
    private fun calculateVariance(data: List<Float>): Float {
        if (data.isEmpty()) return 0f
        val mean = data.average().toFloat()
        return data.map { (it - mean) * (it - mean) }.average().toFloat()
    }
    
    /**
     * 이유 문자열 생성
     */
    private fun buildReasonString(conditions: List<Boolean>, roadType: RoadType): String {
        val reasons = mutableListOf<String>()
        
        if (conditions[0]) reasons.add("가속도 높음")
        if (conditions[1]) reasons.add("고속 주행")
        if (conditions[2]) reasons.add("지속적 가속")
        if (conditions[3]) reasons.add("장시간 지속")
        if (conditions[4]) reasons.add("급격한 속도 증가")
        if (conditions[5]) reasons.add("비정상 패턴")
        
        return "${roadType.description} - ${reasons.joinToString(", ")}"
    }
    
    /**
     * 이력 초기화
     */
    fun reset() {
        speedHistory.clear()
        accelerationHistory.clear()
        estimatedRoadType = RoadType.UNKNOWN
    }
}

/**
 * 도로 타입
 */
enum class RoadType(val description: String) {
    HIGHWAY("고속도로"),
    MAIN_ROAD("일반도로"),
    ALLEY("골목길"),
    PARKING("주차장"),
    UNKNOWN("분석중")
}

/**
 * 감지 임계값
 */
data class DetectionThreshold(
    val accelerationThreshold: Float,
    val minSpeed: Float,
    val description: String
)

/**
 * 감지 결과
 */
data class DetectionResult(
    val isSuddenAcceleration: Boolean,
    val confidence: Float,  // 신뢰도 (0.0 ~ 1.0)
    val reason: String,
    val roadType: RoadType,
    val currentSpeed: Float = 0f,
    val acceleration: Float = 0f
)

/**
 * 속도 데이터
 */
data class SpeedData(
    val speed: Float,
    val timestamp: Long
)

/**
 * 가속도 데이터
 */
data class AccelerationData(
    val acceleration: Float,
    val timestamp: Long
)

