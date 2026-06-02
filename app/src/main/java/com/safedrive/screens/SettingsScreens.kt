package com.safedrive.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safedrive.utils.PreferencesManager
import com.safedrive.utils.SmsHelper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.CircleShape

/**
 * 설정 메인 화면
 * - Account 섹션
 * - More 섹션
 * - 알림 스위치 (SharedPreferences)
 */
@Composable
fun SettingsScreen(
    onNavigateToGuardian: () -> Unit,
    onNavigateToVehicleType: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    // 알림 설정 상태
    var notificationEnabled by remember { mutableStateOf(prefsManager.isNotificationEnabled) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 헤더
                Text(
                    text = "설정",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                // Account 섹션
                SettingsSectionCard(
                    title = "Account",
                    items = listOf(
                        SettingItem(
                            icon = Icons.Default.AccountCircle,
                            title = "프로필 설정",
                            onClick = { /* TODO */ }
                        ),
                        SettingItem(
                            icon = Icons.Default.FamilyRestroom,
                            title = "보호자 설정",
                            onClick = onNavigateToGuardian,
                            subtitle = if (prefsManager.hasGuardianInfo()) {
                                val (name, _) = prefsManager.getGuardianInfo()
                                "등록됨: $name"
                            } else {
                                "미등록"
                            }
                        )
                    )
                )
            }
            
            item {
                // More 섹션
                SettingsSectionCard(
                    title = "More",
                    items = listOf(
                        SettingItem(
                            icon = Icons.Default.Notifications,
                            title = "알림",
                            hasSwitch = true,
                            switchState = notificationEnabled,
                            onSwitchChanged = { enabled ->
                                notificationEnabled = enabled
                                prefsManager.isNotificationEnabled = enabled
                            }
                        ),
                        SettingItem(
                            icon = Icons.Default.DirectionsCar,
                            title = "차량 종류",
                            subtitle = if (prefsManager.hasVehicleType()) {
                                prefsManager.getVehicleTypeName()
                            } else {
                                "미설정"
                            },
                            onClick = onNavigateToVehicleType
                        ),
                        SettingItem(
                            icon = Icons.Default.Info,
                            title = "앱 정보",
                            subtitle = "버전 1.0.0",
                            onClick = { /* TODO */ }
                        ),
                        SettingItem(
                            icon = Icons.Default.Policy,
                            title = "개인정보 처리방침",
                            onClick = { /* TODO */ }
                        ),
                        SettingItem(
                            icon = Icons.Default.Logout,
                            title = "로그아웃",
                            onClick = { /* TODO */ },
                            isDestructive = true
                        )
                    )
                )
            }
        }
    }
}

/**
 * 설정 섹션 카드
 */
@Composable
fun SettingsSectionCard(
    title: String,
    items: List<SettingItem>
) {
    Column {
        // 섹션 타이틀
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        // 섹션 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEachIndexed { index, item ->
                    SettingItemRow(item)
                    if (index < items.size - 1) {
                        Divider(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 설정 항목 행
 */
@Composable
fun SettingItemRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.hasSwitch && item.onClick != null) {
                item.onClick?.invoke()
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 아이콘 + 텍스트
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (item.isDestructive) Color.Red else Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.isDestructive) Color.Red else Color.Black
                )
                
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // 오른쪽: 스위치 또는 화살표
        if (item.hasSwitch) {
            Switch(
                checked = item.switchState,
                onCheckedChange = item.onSwitchChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2196F3),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        } else if (item.onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "이동",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 설정 항목 데이터 클래스
 */
data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: (() -> Unit)? = null,
    val hasSwitch: Boolean = false,
    val switchState: Boolean = false,
    val onSwitchChanged: ((Boolean) -> Unit)? = null,
    val isDestructive: Boolean = false
)

// ============ 보호자 설정 화면 ============

/**
 * 보호자 설정 화면
 * - 전화번호, 이름 입력
 * - SharedPreferences 저장
 */
@Composable
fun GuardianSettingScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val smsHelper = remember { SmsHelper(context) }
    
    // 입력 상태
    var guardianName by remember { mutableStateOf(prefsManager.guardianName) }
    var guardianPhone by remember { mutableStateOf(prefsManager.guardianPhone) }
    
    // 입력 유효성 검증
    val isValid = guardianName.isNotBlank() && guardianPhone.isNotBlank()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }
            Text(
                text = "보호자 설정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Divider(color = Color.LightGray.copy(alpha = 0.3f))
        
        // 메인 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // 설명
            Text(
                text = "위험 상황 발생 시 알림을 받을\n보호자 정보를 입력해주세요.",
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 이름 입력
            OutlinedTextField(
                value = guardianName,
                onValueChange = { guardianName = it },
                label = { Text("보호자 이름") },
                placeholder = { Text("예) 홍길동") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "이름",
                        tint = Color(0xFF2196F3)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF2196F3)
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 전화번호 입력
            OutlinedTextField(
                value = guardianPhone,
                onValueChange = { 
                    // 숫자와 하이픈만 허용
                    if (it.all { char -> char.isDigit() || char == '-' }) {
                        guardianPhone = it
                    }
                },
                label = { Text("전화번호") },
                placeholder = { Text("010-1234-5678") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "전화번호",
                        tint = Color(0xFF2196F3)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF2196F3)
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 안내 메시지
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "정보",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "입력한 정보는 안전하게 암호화되어 저장됩니다.",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue 버튼
            Button(
                onClick = {
                    // SharedPreferences에 저장
                    prefsManager.saveGuardianInfo(guardianName, guardianPhone)
                    
                    // 주간 운전 습관 리포트 SMS 전송
                    val dbHelper = com.safedrive.repository.SafeDriveDbHelper(context)
                    val statistics = dbHelper.getWeeklyStatistics(6)
                    val weeklyScore = (100 - (statistics.averageHardAcceleration * 5) - (statistics.averageHardBraking * 3)).toInt().coerceIn(0, 100)
                    
                    smsHelper.sendWeeklyReport(
                        phoneNumber = guardianPhone,
                        guardianName = guardianName,
                        weeklyScore = weeklyScore,
                        hardAccelCount = statistics.totalHardAcceleration,
                        hardBrakeCount = statistics.totalHardBraking
                    )
                    
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============ 차량 종류 선택 화면 ============

/**
 * 차량 종류 선택 화면
 * - 일반 차량
 * - 스마트키 차량
 */
@Composable
fun VehicleTypeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    // 선택된 차량 종류
    var selectedType by remember { 
        mutableStateOf(prefsManager.vehicleType) 
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }
            Text(
                text = "차량 종류",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Divider(color = Color.LightGray.copy(alpha = 0.3f))
        
        // 메인 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // 설명
            Text(
                text = "차량 종류를 선택해주세요.",
                fontSize = 16.sp,
                color = Color.Gray
            )
            
            Text(
                text = "급발진 의심 상황 시 차량을 멈추는 방법을 안내해드립니다.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 차량 종류 선택 카드
            VehicleTypeCard(
                type = PreferencesManager.VehicleType.NORMAL,
                title = "일반 차량",
                description = "브레이크 페달로 차량을 정지합니다",
                icon = Icons.Default.DirectionsCar,
                isSelected = selectedType == PreferencesManager.VehicleType.NORMAL,
                onClick = { selectedType = PreferencesManager.VehicleType.NORMAL }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            VehicleTypeCard(
                type = PreferencesManager.VehicleType.SMART_KEY,
                title = "스마트키 차량",
                description = "엔진 스타트 버튼을 3초간 길게 누르세요",
                icon = Icons.Default.Key,
                isSelected = selectedType == PreferencesManager.VehicleType.SMART_KEY,
                onClick = { selectedType = PreferencesManager.VehicleType.SMART_KEY }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 안내 메시지
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "경고",
                        tint = Color(0xFFFF6F00),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "급발진 의심 시",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                        Text(
                            text = "홈 화면에 선택하신 차량 종류에 맞는 정지 방법이 표시됩니다.",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 확인 버튼
            Button(
                onClick = {
                    // SharedPreferences에 저장
                    prefsManager.vehicleType = selectedType
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "확인",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 차량 종류 선택 카드
 */
@Composable
fun VehicleTypeCard(
    type: PreferencesManager.VehicleType,
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 아이콘
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isSelected) Color(0xFF2196F3) else Color.LightGray.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // 텍스트
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF1976D2) else Color.Black
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // 라디오 버튼
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2196F3),
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}

