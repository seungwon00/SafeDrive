package com.safedrive.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safedrive.repository.SafeDriveDbHelper
import com.safedrive.model.WeeklyStatistics

/**
 * Analytics 메인 화면
 * - 급가속 횟수 및 주간 변화 그래프
 * - DB 데이터 기반 통계 표시
 * - 임계값 기반 코멘트
 */
@Composable
fun AnalyticsMainScreen(
    onNavigateToHabitDetail: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { SafeDriveDbHelper(context) }
    
    // 화면이 표시될 때마다 최신 데이터 로드 (주행 후 즉시 반영)
    var statistics by remember { mutableStateOf(dbHelper.getWeeklyStatistics(6)) }
    
    // 화면 진입 시 데이터 새로고침
    LaunchedEffect(Unit) {
        statistics = dbHelper.getWeeklyStatistics(6)
    }
    
    // 임계값: 6주 평균 4회 초과 시 경고
    val threshold = 4f
    val isHighRisk = statistics.averageHardAcceleration > threshold
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 헤더
                Text(
                    text = "운전 분석",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            item {
                // 급가속 통계 카드
                HardAccelerationCard(
                    totalCount = statistics.totalHardAcceleration,
                    averageCount = statistics.averageHardAcceleration,
                    isHighRisk = isHighRisk
                )
            }
            
            item {
                // 주간 변화 그래프 카드
                WeeklyTrendCard(
                    weeklyData = statistics.weeklyHardAccelCounts,
                    currentWeekCount = statistics.weeklyHardAccelCounts.lastOrNull() ?: 0
                )
            }
            
            item {
                // 나의 운전 습관 보기 카드 (클릭 → 상세 화면)
                DrivingHabitSummaryCard(
                    hardAccelCount = statistics.totalHardAcceleration,
                    hardBrakeCount = statistics.totalHardBraking,
                    onClick = onNavigateToHabitDetail
                )
            }
            
            item {
                // 다른 운전자와 비교 카드
                ComparisonCard(
                    myScore = calculateAverageScore(statistics),
                    averageScore = 75
                )
            }
        }
    }
}

/**
 * 급가속 통계 카드
 */
@Composable
fun HardAccelerationCard(
    totalCount: Int,
    averageCount: Float,
    isHighRisk: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighRisk) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "급가속",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$totalCount",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHighRisk) Color(0xFFFF6F00) else Color(0xFF4CAF50)
                        )
                        Text(
                            text = "회",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isHighRisk) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isHighRisk) Color(0xFFFF6F00) else Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 코멘트
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isHighRisk) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isHighRisk) Color(0xFFFF6F00) else Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHighRisk) {
                        "급발진 위험이 다소 높은 편입니다!"
                    } else {
                        "안전한 운전을 하고 계십니다! 👍"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isHighRisk) Color(0xFFFF6F00) else Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "최근 6주 평균: ${String.format("%.1f", averageCount)}회/주",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * 주간 변화 그래프 카드
 */
@Composable
fun WeeklyTrendCard(
    weeklyData: List<Int>,
    currentWeekCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "주간 변화 추이",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "최근 6주간 급가속 횟수",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 그래프
            WeeklyTrendGraph(
                data = weeklyData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 이번 주 통계
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "이번 주",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${currentWeekCount}회",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                
                val change = if (weeklyData.size >= 2) {
                    currentWeekCount - weeklyData[weeklyData.size - 2]
                } else {
                    0
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "지난 주 대비",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (change > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (change > 0) Color.Red else Color(0xFF4CAF50)
                        )
                        Text(
                            text = "${kotlin.math.abs(change)}회",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (change > 0) Color.Red else Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 주간 변화 그래프
 */
@Composable
fun WeeklyTrendGraph(
    data: List<Int>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOrNull() ?: 10
    val minValue = 0
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1).coerceAtLeast(1)
        
        // 그리드 라인
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // 데이터 라인 및 포인트
        if (data.isNotEmpty()) {
            val path = Path()
            
            data.forEachIndexed { index, value ->
                val x = spacing * index
                val y = height - (height * (value - minValue) / (maxValue - minValue).coerceAtLeast(1))
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                // 포인트
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                
                // 외곽선
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // 라인 그리기
            drawPath(
                path = path,
                color = Color(0xFF2196F3),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

/**
 * 운전 습관 요약 카드 (클릭 가능)
 */
@Composable
fun DrivingHabitSummaryCard(
    hardAccelCount: Int,
    hardBrakeCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "나의 운전 습관 보기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "상세보기",
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HabitStatItem("급가속", hardAccelCount, Color(0xFFFF6F00))
                HabitStatItem("급제동", hardBrakeCount, Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun HabitStatItem(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${count}회",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 다른 운전자와 비교 카드
 */
@Composable
fun ComparisonCard(
    myScore: Int,
    averageScore: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "다른 운전자와 비교",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreItem("내 점수", myScore, Color(0xFF2196F3))
                ScoreItem("평균 점수", averageScore, Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val difference = myScore - averageScore
            Text(
                text = if (difference >= 0) {
                    "평균보다 ${difference}점 높습니다! 🎉"
                } else {
                    "평균보다 ${-difference}점 낮습니다. 안전운전을 실천해보세요!"
                },
                fontSize = 14.sp,
                color = if (difference >= 0) Color(0xFF4CAF50) else Color(0xFFFF6F00),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ScoreItem(label: String, score: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${score}점",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 평균 점수 계산
 */
private fun calculateAverageScore(statistics: WeeklyStatistics): Int {
    val avgHardAccel = statistics.averageHardAcceleration
    val avgHardBrake = statistics.averageHardBraking
    
    return (100 - (avgHardAccel * 5) - (avgHardBrake * 3))
        .toInt()
        .coerceIn(0, 100)
}

// ============ 운전 습관 상세 화면 ============

/**
 * 운전 습관 상세 화면
 * - 급가속, 급제동, 급회전 상세 분석
 * - 개선 제안
 */
@Composable
fun DrivingHabitDetailScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { SafeDriveDbHelper(context) }
    val statistics = remember { dbHelper.getWeeklyStatistics(6) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 헤더 with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = "나의 운전 습관",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            item {
                // 전체 점수 카드
                OverallScoreCard(
                    score = calculateAverageScore(statistics)
                )
            }
            
            item {
                // 급가속 상세 카드
                HabitDetailCard(
                    title = "급가속",
                    icon = Icons.Default.Speed,
                    count = statistics.totalHardAcceleration,
                    average = statistics.averageHardAcceleration,
                    color = Color(0xFFFF6F00),
                    description = "급가속은 연료 소비를 증가시키고 사고 위험을 높입니다.",
                    tips = listOf(
                        "천천히 가속 페달을 밟아주세요",
                        "앞차와의 거리를 충분히 확보하세요",
                        "신호 예측 운전을 실천하세요"
                    )
                )
            }
            
            item {
                // 급제동 상세 카드
                HabitDetailCard(
                    title = "급제동",
                    icon = Icons.Default.LocalParking,
                    count = statistics.totalHardBraking,
                    average = statistics.averageHardBraking,
                    color = Color(0xFFF44336),
                    description = "급제동은 후방 추돌 사고의 주요 원인입니다.",
                    tips = listOf(
                        "적절한 차간 거리를 유지하세요",
                        "미리 브레이크를 부드럽게 밟아주세요",
                        "전방 상황을 지속적으로 확인하세요"
                    )
                )
            }
            
            item {
                // 개선 목표 카드
                ImprovementGoalCard(statistics)
            }
        }
    }
}

/**
 * 전체 점수 카드
 */
@Composable
fun OverallScoreCard(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                score >= 85 -> Color(0xFFE8F5E9)
                score >= 70 -> Color(0xFFFFF3E0)
                else -> Color(0xFFFFEBEE)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "전체 안전 점수",
                fontSize = 16.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // 원형 배경
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        style = Stroke(width = 16.dp.toPx())
                    )
                    
                    // 점수에 따른 원형 그래프
                    drawArc(
                        color = when {
                            score >= 85 -> Color(0xFF4CAF50)
                            score >= 70 -> Color(0xFFFF6F00)
                            else -> Color(0xFFF44336)
                        },
                        startAngle = -90f,
                        sweepAngle = (score / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx())
                    )
                }
                
                Text(
                    text = "$score",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score >= 85 -> Color(0xFF4CAF50)
                        score >= 70 -> Color(0xFFFF6F00)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    score >= 85 -> "훌륭해요! 안전한 운전을 유지하세요 🎉"
                    score >= 70 -> "괜찮아요! 조금만 더 주의하면 완벽해요 ⚠️"
                    else -> "주의가 필요해요. 안전운전을 실천해주세요 ⚠️"
                },
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 운전 습관 상세 카드
 */
@Composable
fun HabitDetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    average: Float,
    color: Color,
    description: String,
    tips: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "최근 6주: ${count}회 (평균 ${String.format("%.1f", average)}회/주)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 설명
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 개선 팁
            Text(
                text = "개선 방법",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tip,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

/**
 * 개선 목표 카드
 */
@Composable
fun ImprovementGoalCard(statistics: WeeklyStatistics) {
    val targetHardAccel = (statistics.totalHardAcceleration * 0.7).toInt()
    val targetHardBrake = (statistics.totalHardBraking * 0.7).toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "목표",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "다음 주 목표",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "30% 개선 목표로 안전운전을 실천해보세요!",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GoalItem("급가속", statistics.totalHardAcceleration, targetHardAccel)
            Spacer(modifier = Modifier.height(8.dp))
            GoalItem("급제동", statistics.totalHardBraking, targetHardBrake)
        }
    }
}

@Composable
fun GoalItem(label: String, current: Int, target: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${current}회",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
                tint = Color.Gray
            )
            Text(
                text = "${target}회",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }
    }
}

