# SafeDrive - 졸업작품 안드로이드 앱

SafeDrive는 운전자의 안전을 모니터링하는 안드로이드 앱입니다.

## 기능

- **원형 속도계**: 현재 속도를 시각적으로 표시
- **운전 상태 모니터링**: 실시간 운전 상태 확인
- **센서 상태**: GPS 및 가속도계 상태 표시
- **하단 네비게이션**: Home, History, Analytics, Settings

## 실행 방법

### 1. Android Studio에서 프로젝트 열기
```bash
# 프로젝트 폴더를 Android Studio에서 열기
```

### 2. Gradle 동기화
- Android Studio에서 "Sync Now" 버튼 클릭
- 또는 `./gradlew build` 실행

### 3. 앱 실행
- 에뮬레이터 또는 실제 기기 연결
- Run 버튼 클릭 또는 `Shift + F10`

### 4. 필수 권한
앱 실행 시 다음 권한이 필요합니다:
- 위치 권한 (GPS)
- 활동 인식 권한 (가속도계)

## 프로젝트 구조

```
app/
├── src/main/java/com/safedrive/
│   └── MainActivity.kt          # 메인 액티비티
├── src/main/res/
│   ├── values/
│   │   ├── strings.xml          # 앱 문자열 리소스
│   │   └── themes.xml           # 앱 테마
│   └── layout/                  # XML 레이아웃 (필요시)
├── build.gradle.kts             # 앱 레벨 Gradle 설정
└── AndroidManifest.xml          # 앱 매니페스트
```

## 주요 컴포넌트

### CircularSpeedGauge
- 원형 속도계 UI 컴포넌트
- 파란색 진행률 표시
- 45km/h 속도 표시

### DrivingStatusSection
- 운전 상태 정보 표시
- "정상 주행중입니다" 메시지
- 초록색 상태 표시

### SensorStatusSection
- GPS 및 가속도계 상태 표시
- 실시간 센서 정보

### BottomNavigationBar
- 4개 탭 네비게이션
- Home, History, Analytics, Settings

## 기술 스택

- **Kotlin**: 프로그래밍 언어
- **Jetpack Compose**: UI 프레임워크
- **Material 3**: 디자인 시스템
- **Android API 24+**: 최소 지원 버전

## 개발 환경

- Android Studio Hedgehog 이상
- Kotlin 1.9.10
- Compose BOM 2023.10.01
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

## 빌드 명령어

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 테스트 실행
./gradlew test

# 정리
./gradlew clean
```
# SafeDrive
개인 포트폴리오 "SafeDrive" 입니다.
