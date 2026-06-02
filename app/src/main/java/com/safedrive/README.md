# SafeDrive - 운전 상태 관리 시스템

## 📋 개요

SafeDrive는 GPS와 가속도 센서를 활용하여 운전 패턴을 분석하고 안전 점수를 계산하는 앱입니다.

---

## 🎯 주요 기능

### 1. 운전 문맥 기반 임계값 차등 적용

속도에 따라 다른 급가속/급제동 기준을 적용합니다.

#### 고속 주행 (60 km/h 초과)
- **급가속 임계값**: 3.5 m/s²
- **급제동 임계값**: -3.5 m/s²
- **이유**: 고속도로에서는 정상적인 가속이 더 크기 때문

#### 저속/시내 주행 (60 km/h 이하)
- **급가속 임계값**: 2.5 m/s²
- **급제동 임계값**: -2.5 m/s²
- **이유**: 시내에서는 더 부드러운 운전이 기대되기 때문

#### 지속 시간 조건
- 모든 이벤트는 **0.8초 이상 지속**되어야 유효함
- 순간적인 센서 노이즈를 제거

---

### 2. GPS 데이터 안정화 필터

#### 정확도 필터
- GPS accuracy가 **6m 이하**인 데이터만 사용
- 정확도가 낮은 데이터는 속도 계산에서 제외

#### 이동 평균 필터
- 최근 **3개의 속도 값**을 평균하여 사용
- GPS 신호 불안정으로 인한 속도 튀김 현상 제거
- 부드럽고 안정적인 속도 데이터 제공

---

### 3. 유효 주행 기록 저장 조건

다음 조건을 만족하는 주행 기록만 DB에 저장합니다.

#### 조건 1: 최소 이동 거리
- **500m 이상** 이동해야 함
- 주차장 이동 등 짧은 이동은 제외

#### 조건 2: 정지 시간 비율
- 정지 시간(속도 5 km/h 이하)이 전체 시간의 **80% 이하**
- 신호 대기 시간이 너무 긴 주행은 제외

---

## 🛠️ 사용 방법

### 기본 사용

```kotlin
// 1. Manager 초기화
val gpsManager = GpsManager(context)
val drivingStateManager = DrivingStateManager(context)

// 2. 주행 시작
drivingStateManager.startDriving()

// 3. GPS 데이터 수신 (Coroutine Flow)
lifecycleScope.launch {
    gpsManager.getLocationUpdates().collect { locationData ->
        // 속도 및 위치 업데이트
        drivingStateManager.updateLocation(locationData)
        
        // 현재 운전 문맥 확인
        val context = drivingStateManager.getDrivingContext()
        when (context) {
            DrivingContext.HIGH_SPEED -> {
                // 고속 주행 중
            }
            DrivingContext.LOW_SPEED -> {
                // 저속 주행 중
            }
        }
    }
}

// 4. 주행 상태 관찰
lifecycleScope.launch {
    drivingStateManager.drivingState.collect { state ->
        println("속도: ${state.currentSpeedKmh} km/h")
        println("급가속: ${state.hardAccelerationCount}회")
        println("급제동: ${state.hardBrakingCount}회")
        println("급회전: ${state.sharpTurnCount}회")
    }
}

// 5. 주행 종료
val session = drivingStateManager.stopDriving()
if (session != null) {
    // 유효한 주행 기록 - DB 저장
    val driveLog = session.toDriveLog(id = generateId())
    saveToDB(driveLog)
} else {
    // 유효하지 않은 주행 기록 - 무시
    println("주행 기록이 유효성 검증을 통과하지 못했습니다.")
}
```

---

## 📊 데이터 모델

### LocationData
```kotlin
data class LocationData(
    val latitude: Double,           // 위도
    val longitude: Double,          // 경도
    val speedMps: Float,            // 속도 (m/s) - 평활화됨
    val speedKmh: Float,            // 속도 (km/h) - 평활화됨
    val accuracy: Float,            // GPS 정확도 (미터)
    val timestamp: Long,            // 타임스탬프
    val isAccurate: Boolean = true  // 정확도 필터 통과 여부
)
```

### DrivingState
```kotlin
data class DrivingState(
    val isActive: Boolean = false,          // 주행 중 여부
    val currentSpeedKmh: Float = 0f,        // 현재 속도
    val hardAccelerationCount: Int = 0,     // 급가속 횟수
    val hardBrakingCount: Int = 0,          // 급제동 횟수
    val sharpTurnCount: Int = 0             // 급회전 횟수
)
```

### DrivingSession
```kotlin
data class DrivingSession(
    val startTime: Long,                    // 시작 시간
    val endTime: Long,                      // 종료 시간
    val durationMs: Long,                   // 주행 시간
    val distanceMeters: Float,              // 이동 거리
    val hardAccelerationCount: Int,         // 급가속 횟수
    val hardBrakingCount: Int,              // 급제동 횟수
    val sharpTurnCount: Int,                // 급회전 횟수
    val averageSpeedKmh: Float,             // 평균 속도
    val safetyScore: Int                    // 안전 점수 (0-100)
)
```

---

## 🔢 안전 점수 계산 공식

```
Score = 100 - (급가속 × 5) - (급제동 × 3) - (급회전 × 2)
```

### 점수 범위: 0 ~ 100점

| 점수 | 등급 | 색상 |
|------|------|------|
| 85점 이상 | 정상 (Excellent) | 🟢 초록색 |
| 70~84점 | 주의 (Caution) | 🟠 주황색 |
| 70점 미만 | 경고 (Warning) | 🔴 빨간색 |

---

## ⚠️ 권한 요구사항

### AndroidManifest.xml에 추가 필요

```xml
<!-- 위치 권한 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- 센서 권한 (자동 부여) -->
<!-- 가속도계는 별도 권한 불필요 -->
```

### 런타임 권한 요청

```kotlin
// Activity에서 권한 요청
val permissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
```

---

## 🧪 테스트 시나리오

### 시나리오 1: 고속도로 주행
- 속도: 100 km/h
- 급가속 임계값: 3.5 m/s²
- 예상: 일반 가속은 급가속으로 감지되지 않음

### 시나리오 2: 시내 주행
- 속도: 40 km/h
- 급가속 임계값: 2.5 m/s²
- 예상: 부드러운 운전 유도

### 시나리오 3: 주차장 이동
- 거리: 200m
- 결과: **DB 저장 안 됨** (최소 거리 미달)

### 시나리오 4: 신호 대기 많은 주행
- 총 시간: 10분
- 정지 시간: 9분 (90%)
- 결과: **DB 저장 안 됨** (정지 시간 초과)

---

## 📈 성능 최적화

### GPS 업데이트 주기
- 기본: **1초**
- 최소: **0.5초**
- 최대 지연: **2초**

### 센서 샘플링
- 가속도계: `SENSOR_DELAY_UI` (일반적으로 60Hz)

### 메모리 사용
- 속도 버퍼: 최근 3개 값만 저장 (메모리 효율적)

---

## 🔧 커스터마이징

### 임계값 조정

```kotlin
class DrivingStateManager(context: Context) {
    // 속도 기준
    private val highSpeedThresholdKmh = 60.0f  // 변경 가능
    
    // 급가속 임계값
    private val hardAccelThresholdHighSpeed = 3.5f  // 변경 가능
    private val hardAccelThresholdLowSpeed = 2.5f   // 변경 가능
    
    // 지속 시간
    private val eventDurationThresholdMs = 800L  // 변경 가능
    
    // 유효 주행 조건
    private val minValidDistanceMeters = 500f  // 변경 가능
    private val maxStationaryTimeRatio = 0.8f  // 변경 가능
}
```

---

## 🐛 문제 해결

### GPS 속도가 0으로 표시됨
- `location.hasSpeed()` 확인
- 기기가 움직이는지 확인
- GPS 신호 상태 확인

### 급가속이 너무 많이 감지됨
- 임계값 증가 고려
- 지속 시간 조건 증가 고려
- 센서 캘리브레이션 확인

### 주행 기록이 저장되지 않음
- 최소 거리 500m 이상 이동했는지 확인
- 정지 시간 비율 확인
- 로그로 `validateDrivingSession()` 결과 확인

---

## 📝 개발자 노트

### 왜 이동 평균 필터를 사용하나요?
GPS 신호는 환경에 따라 불안정할 수 있습니다. 이동 평균 필터는 순간적인 노이즈를 제거하고 부드러운 속도 데이터를 제공합니다.

### 왜 0.8초 지속 시간이 필요한가요?
순간적인 센서 오류나 과속방지턱을 지나는 등의 상황을 제외하기 위함입니다. 실제 급가속/급제동은 일정 시간 지속됩니다.

### 왜 유효성 검증이 필요한가요?
- 주차장 이동, 신호 대기 시간 등 실제 주행이 아닌 상황을 제외
- 의미 있는 데이터만 수집하여 통계 정확도 향상
- 불필요한 DB 공간 절약

---

## 📚 참고 자료

- [Android Location API](https://developer.android.com/training/location)
- [Sensor Overview](https://developer.android.com/guide/topics/sensors/sensors_overview)
- [Moving Average Filter](https://en.wikipedia.org/wiki/Moving_average)
- [Kalman Filter](https://en.wikipedia.org/wiki/Kalman_filter) (향후 개선 고려)

---

## 🚀 향후 개선 계획

1. **Kalman Filter 도입**: 더 정교한 GPS 데이터 필터링
2. **Machine Learning**: 운전 패턴 학습 및 개인화된 임계값
3. **실시간 알림**: 위험 운전 즉시 알림
4. **클라우드 동기화**: 여러 기기 간 데이터 공유
5. **배터리 최적화**: 백그라운드 동작 개선

---

## 📄 라이센스

Copyright (c) 2024 SafeDrive Team
All rights reserved.

