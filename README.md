# SafeDrive 🚗

> **노인 운전자를 위한 페달 오조작 감지 및 안전운전 보조 앱**

한국교통안전공단(TS) 자동차안전연구원 자료에 따르면, 급발진 의심사고의 **75.2%가 60대 이상**에서 발생하며 대부분 페달 오조작이 원인입니다.  
SafeDrive는 스마트폰 센서(GPS + 가속도계)를 활용해 위험 운전 패턴을 실시간으로 감지하고, 보호자에게 알림을 전달하는 안드로이드 앱입니다.

---

## 주요 기능

- **실시간 속도 모니터링**: GPS 기반 원형 속도계 UI
- **위험 운전 감지**: 급가속 / 급제동 패턴 실시간 감지 및 기록
- **TTS 음성 안내**: 위험 상황 발생 시 즉각 음성 경고
- **보호자 SMS 알림**: 위험 이벤트 발생 시 보호자에게 문자 발송
- **운전 기록 분석**: 주행 이력 및 안전 점수 확인 (History / Analytics)

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| 센서 | GPS, 가속도계 (Accelerometer) |
| 로컬 DB | SQLite |
| 알림 | SMS, TTS |
| Min SDK | Android 7.0 (API 24) |

---

## 프로젝트 구조

```
app/src/main/java/com/safedrive/
├── model/              # 데이터 모델
│   ├── DriveLog.kt         - 주행 기록, 안전 점수, 상태 레이블
│   ├── DrivingModels.kt    - DrivingState, DrivingSession, DrivingContext
│   ├── LocationData.kt     - GPS 위치/속도 데이터
│   └── WeeklyStatistics.kt - 주간 통계 집계
├── service/            # 센서 및 비즈니스 로직
│   ├── DrivingStateManager.kt  - 가속도계 기반 급가속/급제동 감지
│   └── GpsManager.kt           - GPS 속도 수집 및 스무딩
├── repository/         # 데이터 저장소
│   └── SafeDriveDbHelper.kt    - SQLite CRUD, 주간 통계 쿼리
├── detection/          # 급발진 감지 알고리즘
│   └── SuddenAccelerationDetector.kt  - 6조건 중 3개 이상 감지 로직
├── screens/            # 화면 UI (Jetpack Compose)
│   ├── AnalyticsScreens.kt     - 주간 통계, 그래프
│   └── SettingsScreens.kt      - 보호자 설정, 차량 종류 선택
├── utils/              # 유틸리티
│   ├── PreferencesManager.kt   - SharedPreferences 래퍼
│   ├── SmsHelper.kt            - 보호자 SMS 발송
│   ├── TextToSpeechHelper.kt   - TTS 음성 경고
│   └── PermissionHelper.kt     - 런타임 권한 처리
└── MainActivity.kt     # 앱 진입점, 홈/히스토리 화면
```

---

## 실행 방법

### 1. 프로젝트 클론
```bash
git clone https://github.com/seungwon00/SafeDrive.git
```

### 2. Android Studio에서 열기
- Android Studio Hedgehog 이상 권장
- `Sync Now` 클릭 또는 `./gradlew build`

### 3. 필수 권한
앱 실행 시 아래 권한이 필요합니다:
- 위치 권한 (GPS)
- 활동 인식 권한 (가속도계)

### 4. 실행
```bash
# 디버그 빌드
./gradlew assembleDebug

# 테스트
./gradlew test
```

---

## 현재 한계 및 개선 예정

현재 버전은 **로컬 앱** 수준으로, 아래 기능들이 미구현 상태입니다.  
Spring Boot 서버 연동을 통해 실제 서비스 수준으로 개선할 예정입니다.

### 개선 예정 목록
- [ ] **Spring Boot 서버 개발** — 주행 데이터 수집 및 저장 API
- [ ] **AWS 배포** — EC2 + RDS 기반 실서비스 환경 구성
- [ ] **사용자 인증** — 로그인/로그아웃, 보호자 계정 관리
- [ ] **운전 점수 비교** — 다른 운전자와 점수 비교 기능 실제 구현 (현재 미구현)
- [ ] **안전 점수 공식 개선** — 주행 거리 기반 정규화 적용
- [ ] **노인 UX 개선** — 큰 버튼, 고대비 색상, 음성 위주 인터페이스
- [ ] **Flutter 리팩토링** — Android / iOS 크로스플랫폼 지원

---

## 개발 배경

급발진 의심 사고의 법적 판정 기준이 모호하고, 실제로는 대부분 페달 오조작이 원인임에도 불구하고 고령 운전자를 위한 예방 솔루션이 부족하다는 문제의식에서 출발했습니다.  
정부 공식 자료(TS 자동차안전연구원)를 참고해 타깃과 기능을 설계했습니다.

---

## 개발 환경

- Android Studio Hedgehog 이상
- Kotlin 1.9.10
- Compose BOM 2023.10.01
- Target SDK: 34 (Android 14)
