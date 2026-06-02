package com.safedrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safedrive.screens.AnalyticsMainScreen
import com.safedrive.screens.DrivingHabitDetailScreen
import com.safedrive.screens.SettingsScreen
import com.safedrive.screens.GuardianSettingScreen
import com.safedrive.screens.VehicleTypeScreen
import com.safedrive.model.DriveLog
import com.safedrive.model.DriveLogFilter
import com.safedrive.model.LocationData
import com.safedrive.service.DrivingStateManager
import com.safedrive.service.GpsManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 화면 꺼짐 방지 (주행 중 화면 유지)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            SafeDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeDriveApp()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == com.safedrive.utils.PermissionHelper.PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                // 권한이 거부된 경우 안내
                android.widget.Toast.makeText(
                    this,
                    "앱 사용을 위해 모든 권한이 필요합니다",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
fun SafeDriveApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val analyticsNavController = rememberNavController()
    val settingsNavController = rememberNavController()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Main Content based on selected tab (모든 Content를 항상 유지)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // HomeContent는 항상 존재하되, 보이지 않을 때만 숨김 (주행 상태 유지)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (selectedTab == 0) Modifier
                        else Modifier.size(0.dp)  // 다른 탭일 때는 크기를 0으로
                    )
            ) {
                HomeContent()
            }
            
            // 다른 Content들 (선택될 때만 표시)
            if (selectedTab == 1) {
                HistoryContent()
            }
            if (selectedTab == 2) {
                AnalyticsContent(analyticsNavController)
            }
            if (selectedTab == 3) {
                SettingsContent(settingsNavController)
            }
        }
        
        // Bottom Navigation
        BottomNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

@Composable
fun SafeDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2196F3),
            background = Color.White,
            surface = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black
        ),
        content = content
    )
}

@Composable
fun HomeContent() {
    val context = LocalContext.current
    val prefsManager = remember { com.safedrive.utils.PreferencesManager(context) }
    val ttsHelper = remember { com.safedrive.utils.TextToSpeechHelper(context) }
    
    // DrivingStateManager 초기화
    val drivingStateManager = remember { DrivingStateManager(context) }
    val gpsManager = remember { GpsManager(context) }
    val suddenAccelDetector = remember { com.safedrive.detection.SuddenAccelerationDetector() }
    val smsHelper = remember { com.safedrive.utils.SmsHelper(context) }
    
    // 현재 속도 상태 (GPS 또는 테스트 버튼으로 업데이트)
    var currentSpeed by remember { mutableStateOf(0f) }
    var currentAcceleration by remember { mutableStateOf(0f) }
    var currentGpsAccuracy by remember { mutableStateOf(10f) }
    var currentLocation by remember { mutableStateOf<LocationData?>(null) }
    
    // 급발진 의심 상태
    var isSuddenAcceleration by remember { mutableStateOf(false) }
    
    // 도로 타입 표시 (디버그용)
    var currentRoadType by remember { mutableStateOf(com.safedrive.detection.RoadType.UNKNOWN) }
    var detectionConfidence by remember { mutableStateOf(0f) }
    
    // 테스트 모드 (true: 버튼으로 조절, false: 실제 GPS)
    var isTestMode by rememberSaveable { mutableStateOf(true) }
    
    // 센서 상태
    var isGpsActive by rememberSaveable { mutableStateOf(false) }
    var isAccelerometerActive by rememberSaveable { mutableStateOf(false) }
    
    // 주행 상태 (화면 전환해도 유지됨!)
    var isDriving by rememberSaveable { mutableStateOf(false) }
    var drivingStartTime by rememberSaveable { mutableStateOf(0L) }
    var drivingDuration by rememberSaveable { mutableStateOf(0L) }
    
    // GPS 연결 완료 음성 안내 (한 번만)
    var hasAnnouncedGpsConnection by rememberSaveable { mutableStateOf(false) }
    var gpsDataCount by remember { mutableStateOf(0) }  // GPS 데이터 수신 횟수
    
    // 주행 시간 업데이트
    LaunchedEffect(isDriving) {
        while (isDriving) {
            kotlinx.coroutines.delay(1000L)
            drivingDuration = System.currentTimeMillis() - drivingStartTime
        }
    }
    
    // 실제 GPS 데이터 수집 및 속도 업데이트 (주행 상태 기반)
    LaunchedEffect(isDriving, isTestMode) {
        if (isDriving && !isTestMode) {
            // 주행 시작
            drivingStateManager.startDriving()
            isAccelerometerActive = true
            
            // GPS 데이터 수집
            launch {
                gpsManager.getLocationUpdates().collect { locationData ->
                    // GPS 활성화 표시
                    isGpsActive = true
                    
                    // GPS 데이터 수신 횟수 증가
                    gpsDataCount++
                    
                    // GPS 데이터가 3번 이상 수신되고 안정화되면 음성 안내 (한 번만)
                    if (gpsDataCount >= 3 && !hasAnnouncedGpsConnection && locationData.isAccurate) {
                        hasAnnouncedGpsConnection = true
                        ttsHelper.speak("GPS 연결 완료. 정상 주행 중입니다.")
                    }
                    
                    // 실제 GPS 속도로 업데이트
                    currentSpeed = locationData.speedKmh
                    currentGpsAccuracy = locationData.accuracy
                    currentLocation = locationData
                    drivingStateManager.updateLocation(locationData)
                }
            }
            
            // GPS 끊김 감지 (터널 등)
            launch {
                while (isDriving) {  // isDriving이 false가 되면 자동 종료
                    kotlinx.coroutines.delay(500L) // 0.5초마다 체크
                    
                    if (gpsManager.isGpsLost()) {
                        // GPS 끊김: 마지막 유효 속도 유지
                        val lastSpeed = gpsManager.getLastValidSpeed() * 3.6f // m/s → km/h
                        if (lastSpeed > 0) {
                            currentSpeed = lastSpeed
                            currentGpsAccuracy = 10f // 정확도 낮음 표시
                        }
                    }
                }
            }
            
            // 운전 상태 관찰 및 정교한 급발진 감지
            launch {
                drivingStateManager.drivingState.collect { state ->
                    if (!isDriving) return@collect  // 주행 종료 시 중단
                    
                    currentSpeed = state.currentSpeedKmh
                    
                    // 가속도 계산 (속도 변화율)
                    val accel = calculateAcceleration(currentSpeed)
                    currentAcceleration = accel
                    
                    // 정교한 급발진 감지 알고리즘
                    val detectionResult = suddenAccelDetector.detectSuddenAcceleration(
                        currentSpeed = currentSpeed,
                        acceleration = accel,
                        timestamp = System.currentTimeMillis(),
                        gpsAccuracy = currentGpsAccuracy
                    )
                    
                    // 도로 타입 및 신뢰도 업데이트
                    currentRoadType = detectionResult.roadType
                    detectionConfidence = detectionResult.confidence
                    
                    // 급발진 감지 (신뢰도 50% 이상) - 완화
                    if (detectionResult.isSuddenAcceleration && detectionResult.confidence >= 0.5f) {
                        isSuddenAcceleration = true
                        
                        // 급발진 이벤트를 즉시 주행 기록에 반영 (실시간 카운트 증가)
                        drivingStateManager.recordSuddenAcceleration()
                        
                        // 보호자에게 긴급 SMS 전송 (백그라운드에서)
                        if (prefsManager.hasGuardianInfo()) {
                            val (name, phone) = prefsManager.getGuardianInfo()
                            currentLocation?.let { location ->
                                smsHelper.sendEmergencySms(
                                    phoneNumber = phone,
                                    guardianName = name,
                                    currentSpeed = currentSpeed,
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                            }
                        }
                    }
                }
            }
        } else if (!isDriving) {
            // 주행 종료 시 센서 상태 초기화
            isGpsActive = false
            isAccelerometerActive = false
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isSuddenAcceleration) Color(0xFFFFEBEE) else Color.White
            )
    ) {
        // Navigation Header
        NavigationHeader(title = "Home")
        
        // Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isSuddenAcceleration) {
                // 급발진 의심 경고
                EmergencyAlert(
                    vehicleType = prefsManager.vehicleType,
                    onDismiss = { 
                        isSuddenAcceleration = false
                        ttsHelper.stop()
                    },
                    ttsHelper = ttsHelper
                )
            } else {
                // 스크롤 가능한 컨텐츠
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp, horizontal = 24.dp)
                ) {
                    item {
                        // Circular Speed Gauge (0-260 km/h)
                        val displaySpeed = if (currentSpeed < 5f) 0 else currentSpeed.toInt()
                        CircularSpeedGauge(
                            currentSpeed = displaySpeed,
                            maxSpeed = 260,
                            modifier = Modifier.size(280.dp)
                        )
                    }
                    
                    item {
                        // Driving Status Section
                        DrivingStatusSection(
                            roadType = currentRoadType,
                            isDriving = isDriving,
                            drivingDuration = drivingDuration,
                            gpsAccuracy = currentGpsAccuracy,
                            isGpsActive = isGpsActive
                        )
                    }
                    
                    item {
                        // Sensor Status Section
                        SensorStatusSection(
                            isGpsActive = isGpsActive,
                            isAccelerometerActive = isAccelerometerActive
                        )
                    }
                }
            }
        }
        
        // Bottom Control Section
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 모드 전환 버튼
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTestMode) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTestMode) "📱 테스트 모드" else "🛰️ 실제 GPS 모드",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTestMode) Color(0xFF2196F3) else Color(0xFF4CAF50)
                    )
                    Button(
                        onClick = { isTestMode = !isTestMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestMode) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isTestMode) "실제 모드" else "테스트 모드",
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 주행 시작/종료 버튼 (실제 모드일 때)
            if (!isTestMode) {
                Button(
                    onClick = {
                        if (isDriving) {
                            // 주행 종료
                            isDriving = false
                            hasAnnouncedGpsConnection = false  // 다음 주행을 위해 리셋
                            gpsDataCount = 0  // GPS 카운터 리셋
                            val session = drivingStateManager.stopDriving()
                            if (session != null) {
                                android.widget.Toast.makeText(
                                    context,
                                    "주행 기록 저장 완료! (${session.safetyScore}점)",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // 주행 시작
                            isDriving = true
                            drivingStartTime = System.currentTimeMillis()
                            drivingDuration = 0L
                            isTestMode = false  // 실제 모드로 전환
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDriving) Color(0xFFF44336) else Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDriving) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isDriving) "종료" else "시작"
                        )
                        Text(
                            text = if (isDriving) "주행 종료" else "주행 시작",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 테스트 버튼 (테스트 모드일 때만 표시)
            if (isTestMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            currentSpeed = (currentSpeed + 10).coerceAtMost(260f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("속도 +10")
                    }
                    Button(
                        onClick = { 
                            currentSpeed = (currentSpeed - 10).coerceAtLeast(0f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("속도 -10")
                    }
                    Button(
                        onClick = { 
                            isSuddenAcceleration = !isSuddenAcceleration
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSuddenAcceleration) Color.Green else Color.Red
                        )
                    ) {
                        Text(if (isSuddenAcceleration) "정상" else "급발진")
                    }
                }
            } else {
                // 실제 모드 안내
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "GPS",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "실제 GPS 속도로 게이지가 업데이트됩니다",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryContent() {
    HistoryScreen()
}

/**
 * 주행 기록 목록 화면 (History Screen)
 * - 안전 점수 기반 필터링 기능
 * - 전체/이벤트만 보기 토글
 * - DB 데이터 기반 (실제 주행 기록 반영)
 */
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dbHelper = remember { com.safedrive.repository.SafeDriveDbHelper(context) }
    
    // 필터 상태 관리
    var selectedFilter by remember { mutableStateOf(DriveLogFilter.ALL) }
    
    // DB에서 주행 기록 데이터 가져오기 (실시간 반영)
    var allDriveLogs by remember { mutableStateOf(dbHelper.getAllDriveLogs()) }
    
    // 화면 진입 시 최신 데이터 로드 (주행 후 즉시 반영)
    LaunchedEffect(Unit) {
        allDriveLogs = dbHelper.getAllDriveLogs()
    }
    
    // 필터링된 주행 기록 (allDriveLogs 변경 시에도 재계산)
    val filteredLogs = remember(selectedFilter, allDriveLogs) {
        when (selectedFilter) {
            DriveLogFilter.ALL -> allDriveLogs
            DriveLogFilter.EVENT -> allDriveLogs.filter { it.hasEvent }
        }
    }
    
            Column(
                modifier = Modifier
                    .fillMaxSize()
            .background(Color.White)
    ) {
        // Navigation Header
        NavigationHeader(title = "History")
        
        // Filter Buttons
        FilterButtonRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            eventCount = allDriveLogs.count { it.hasEvent }
        )
        
        // History List
        if (filteredLogs.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "이벤트가 없습니다",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        text = "안전 운전을 계속하세요! 👍",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLogs) { driveLog ->
                    DriveLogCard(driveLog)
                }
            }
        }
    }
}

/**
 * 필터 버튼 행
 */
@Composable
fun FilterButtonRow(
    selectedFilter: DriveLogFilter,
    onFilterSelected: (DriveLogFilter) -> Unit,
    eventCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 전체 보기 버튼
        FilterButton(
            text = "전체",
            isSelected = selectedFilter == DriveLogFilter.ALL,
            onClick = { onFilterSelected(DriveLogFilter.ALL) }
        )
        
        // 이벤트만 보기 버튼
        FilterButton(
            text = "이벤트만 ($eventCount)",
            isSelected = selectedFilter == DriveLogFilter.EVENT,
            onClick = { onFilterSelected(DriveLogFilter.EVENT) }
        )
    }
}

/**
 * 필터 버튼
 */
@Composable
fun FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Analytics Content with Navigation
 */
@Composable
fun AnalyticsContent(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "analytics_main"
    ) {
        composable("analytics_main") {
            AnalyticsMainScreen(
                onNavigateToHabitDetail = {
                    navController.navigate("habit_detail")
                }
            )
        }
        
        composable("habit_detail") {
            DrivingHabitDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Settings Content with Navigation
 */
@Composable
fun SettingsContent(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "settings_main"
    ) {
        composable("settings_main") {
            SettingsScreen(
                onNavigateToGuardian = {
                    navController.navigate("guardian_setting")
                },
                onNavigateToVehicleType = {
                    navController.navigate("vehicle_type")
                }
            )
        }
        
        composable("guardian_setting") {
            GuardianSettingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("vehicle_type") {
            VehicleTypeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun NavigationHeader(title: String = "Home") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.width(40.dp)) // Balance the back arrow
    }
}

@Composable
fun CircularSpeedGauge(
    currentSpeed: Int,
    maxSpeed: Int,
    modifier: Modifier = Modifier
) {
    val progress = currentSpeed.toFloat() / maxSpeed.toFloat()
    val sweepAngle = progress * 360f // Full circle (130km/h = 180도 = 반원)
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - 40.dp.toPx()) / 2
            
            // Background circle (light gray)
            drawCircle(
                color = Color(0xFFE0E0E0),
                radius = radius,
                center = center,
                style = Stroke(
                    width = 20.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Progress arc (blue)
            drawArc(
                color = Color(0xFF2196F3),
                startAngle = -90f, // Start from top
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = 20.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
        
        // Speed text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentSpeed.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 48.sp
            )
            Text(
                text = "km",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DrivingStatusSection(
    roadType: com.safedrive.detection.RoadType = com.safedrive.detection.RoadType.UNKNOWN,
    isDriving: Boolean = false,
    drivingDuration: Long = 0L,
    gpsAccuracy: Float = 0f,
    isGpsActive: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "운전 상태",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 주행 상태 표시
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when {
                isDriving && isGpsActive -> "정상 주행입니다"
                isDriving && !isGpsActive -> "GPS 연결 중..."
                else -> "주행중이 아닙니다"
            }
            
            val statusColor = when {
                isDriving && isGpsActive -> Color(0xFF4CAF50)  // 초록색
                isDriving && !isGpsActive -> Color(0xFFFFA726)  // 주황색
                else -> Color(0xFFF44336)  // 빨간색
            }
            
            val statusIcon = when {
                isDriving && isGpsActive -> "✓"
                isDriving && !isGpsActive -> "⏳"
                else -> "✗"
            }
            
            Text(
                text = statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusIcon,
                fontSize = 20.sp,
                color = statusColor
            )
        }
        
        // 주행 시간 표시
        if (isDriving && drivingDuration > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "주행 시간",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF2196F3)
                )
                Text(
                    text = formatDuration(drivingDuration),
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // 도로 타입 표시
        if (roadType != com.safedrive.detection.RoadType.UNKNOWN) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (roadType) {
                        com.safedrive.detection.RoadType.HIGHWAY -> Icons.Default.LocalShipping
                        com.safedrive.detection.RoadType.MAIN_ROAD -> Icons.Default.DirectionsCar
                        com.safedrive.detection.RoadType.ALLEY -> Icons.Default.House
                        com.safedrive.detection.RoadType.PARKING -> Icons.Default.LocalParking
                        else -> Icons.Default.Navigation
                    },
                    contentDescription = "도로 타입",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = roadType.description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        
        // GPS 정확도 표시 (실제 모드일 때)
        if (isDriving && gpsAccuracy > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            
            val accuracyQuality = when {
                gpsAccuracy < 5f -> "우수"
                gpsAccuracy < 10f -> "양호"
                gpsAccuracy < 20f -> "보통"
                else -> "낮음"
            }
            
            val accuracyColor = when {
                gpsAccuracy < 5f -> Color(0xFF4CAF50)
                gpsAccuracy < 10f -> Color(0xFF8BC34A)
                gpsAccuracy < 20f -> Color(0xFFFFA726)
                else -> Color(0xFFF44336)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS 정확도",
                    modifier = Modifier.size(16.dp),
                    tint = accuracyColor
                )
                Text(
                    text = "GPS 정확도: $accuracyQuality (±${gpsAccuracy.toInt()}m)",
                    fontSize = 13.sp,
                    color = accuracyColor
                )
            }
        }
    }
}

/**
 * 시간 포맷 (밀리초 → 시:분:초)
 */
fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000 / 60) % 60
    val hours = (durationMs / 1000 / 60 / 60)
    
    return if (hours > 0) {
        String.format("%d시간 %02d분 %02d초", hours, minutes, seconds)
    } else if (minutes > 0) {
        String.format("%d분 %02d초", minutes, seconds)
    } else {
        String.format("%d초", seconds)
    }
}

/**
 * 속도 변화율로 가속도 계산 (간단한 근사)
 */
private var lastSpeed = 0f
private var lastSpeedTime = 0L

fun calculateAcceleration(currentSpeed: Float): Float {
    val currentTime = System.currentTimeMillis()
    
    if (lastSpeedTime == 0L) {
        lastSpeed = currentSpeed
        lastSpeedTime = currentTime
        return 0f
    }
    
    val speedDiff = currentSpeed - lastSpeed // km/h
    val timeDiff = (currentTime - lastSpeedTime) / 1000f // seconds
    
    // 최소 시간 간격 (0.1초) 이하이면 이전 가속도 유지
    if (timeDiff < 0.1f) return 0f
    
    // km/h를 m/s로 변환 후 가속도 계산
    val speedDiffMps = speedDiff / 3.6f // m/s
    val acceleration = speedDiffMps / timeDiff // m/s²
    
    lastSpeed = currentSpeed
    lastSpeedTime = currentTime
    
    return acceleration
}

@Composable
fun SensorStatusSection(
    isGpsActive: Boolean,
    isAccelerometerActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "센서 상태",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // GPS 상태
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isGpsActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = "GPS 상태",
                tint = if (isGpsActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
        Text(
                text = "GPS: ${if (isGpsActive) "사용중" else "사용 안함"}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
                color = if (isGpsActive) Color.Black else Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 가속도계 상태
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isAccelerometerActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = "가속도계 상태",
                tint = if (isAccelerometerActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
        Text(
                text = "가속도계: ${if (isAccelerometerActive) "작동중" else "작동 안함"}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
                color = if (isAccelerometerActive) Color.Black else Color.Gray
        )
        }
    }
}

// ============ 급발진 경고 화면 ============

/**
 * 급발진 의심 경고 화면
 */
@Composable
fun EmergencyAlert(
    vehicleType: com.safedrive.utils.PreferencesManager.VehicleType,
    onDismiss: () -> Unit,
    ttsHelper: com.safedrive.utils.TextToSpeechHelper
) {
    val stopInstructions = when (vehicleType) {
        com.safedrive.utils.PreferencesManager.VehicleType.NORMAL -> listOf(
            "1. 브레이크를 밟으면서 기어 중립",
            "2. 사이드브레이크 올리기",
            "3. 시동 ACC까지"
        )
        com.safedrive.utils.PreferencesManager.VehicleType.SMART_KEY -> listOf(
            "1. 스마트키 탁탁탁 (3번 누르기)",
            "2. 우선 시동 끄기",
            "3. 브레이크 꾹 누르고 사이드브레이크 4초 당겨주기"
        )
    }
    
    val ttsText = when (vehicleType) {
        com.safedrive.utils.PreferencesManager.VehicleType.NORMAL -> 
            "급발진이 의심됩니다! 브레이크를 밟으면서 기어를 중립으로 바꾸세요. 사이드브레이크를 올리고 시동을 ACC까지 돌리세요."
        com.safedrive.utils.PreferencesManager.VehicleType.SMART_KEY -> 
            "급발진이 의심됩니다! 엔진 스타트 버튼을 세 번 누르세요. 시동을 끄고 브레이크를 꾹 누른 상태에서 사이드브레이크를 4초간 당겨주세요."
    }
    
    // TTS 실행 및 음성 종료 후 자동 복귀
    LaunchedEffect(Unit) {
        ttsHelper.speak(ttsText) {
            // 음성이 완전히 끝난 후 홈 화면 복귀
            onDismiss()
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // 경고 아이콘 및 제목
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFFF5252), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "경고",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        item {
            Text(
                text = "⚠️ 급발진 의심!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F),
                textAlign = TextAlign.Center
            )
        }
        
        item {
            Text(
                text = "아래 방법으로 차량을 정지시키세요",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        
        item {
            // 정지 방법 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (vehicleType == com.safedrive.utils.PreferencesManager.VehicleType.NORMAL) {
                            "일반 차량 정지 방법"
                        } else {
                            "스마트키 차량 정지 방법"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    stopInstructions.forEach { instruction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFF6F00), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = instruction.first().toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = instruction.substring(3),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                lineHeight = 24.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        
        item {
            // 경고 메시지
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "정보",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "침착하게 순서대로 따라하세요",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
        
        item {
            // 확인 버튼
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "상황 종료",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Home,
            label = "Home",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        
        BottomNavItem(
            icon = Icons.Default.History,
            label = "History",
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        
        BottomNavItem(
            icon = Icons.Default.PieChart,
            label = "Analytics",
            isSelected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
        
        BottomNavItem(
            icon = Icons.Default.Person,
            label = "Settings",
            isSelected = selectedTab == 3,
            onClick = { onTabSelected(3) }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF2196F3) else Color(0xFF757575),
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color(0xFF2196F3) else Color(0xFF757575)
        )
    }
}

/**
 * 주행 기록 카드 컴포넌트
 * - 안전 점수 자동 계산 및 표시
 * - 상태(정상/주의/경고)에 따른 색상 자동 적용
 */
@Composable
fun DriveLogCard(driveLog: DriveLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Navigate to detail */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Date, Time, Score, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Date and time
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = driveLog.date,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = driveLog.time,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                }
                
                // Right: Score and Status
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Score badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "점수:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${driveLog.score}점",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = driveLog.statusColor
                        )
                    }
                    
                    // Status label
                    Box(
                        modifier = Modifier
                            .background(
                                color = driveLog.statusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = driveLog.statusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = driveLog.statusColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom row: Duration, Distance, Event counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration and Distance
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Duration",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = driveLog.duration,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = "Distance",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = driveLog.distance,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    }
                }
                
                // Event counts (if any)
                if (driveLog.hardAccelerationCount > 0 || 
                    driveLog.hardBrakingCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (driveLog.hardAccelerationCount > 0) {
                            EventBadge("급가속 ${driveLog.hardAccelerationCount}", Color(0xFFF44336))
                        }
                        if (driveLog.hardBrakingCount > 0) {
                            EventBadge("급제동 ${driveLog.hardBrakingCount}", Color(0xFFFFA726))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 이벤트 배지 컴포넌트
 */
@Composable
fun EventBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

// ============ PREVIEW ============
// Android Studio에서 Split 또는 Design 탭으로 전환하면 빌드 없이 UI를 볼 수 있습니다!

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun SafeDriveAppPreview() {
    SafeDriveTheme {
        SafeDriveApp()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun HistoryContentPreview() {
    SafeDriveTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircularSpeedGaugePreview() {
    SafeDriveTheme {
        CircularSpeedGauge(
            currentSpeed = 45,
            maxSpeed = 120,
            modifier = Modifier.size(280.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DriveLogCardPreview() {
    SafeDriveTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Excellent score
            DriveLogCard(
                DriveLog(
                    1, "2024.10.11", "오후 2:30", "45분", "23.5 km",
                    hardAccelerationCount = 0, hardBrakingCount = 1
                )
            )
            // Caution score
            DriveLogCard(
                DriveLog(
                    2, "2024.10.09", "오후 6:45", "30분", "15.2 km",
                    hardAccelerationCount = 2, hardBrakingCount = 3
                )
            )
            // Warning score
            DriveLogCard(
                DriveLog(
                    3, "2024.10.06", "오후 1:30", "40분", "19.5 km",
                    hardAccelerationCount = 5, hardBrakingCount = 4
                )
            )
        }
    }
}
