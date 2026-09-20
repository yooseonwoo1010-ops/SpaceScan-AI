package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.DeviceCompatibilityDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated

@Composable
fun MainStartScreen(
  onNewProjectClick: () -> Unit,
  onStartDemoMode: () -> Unit,
  onOpenProject: () -> Unit,
  onRequestCameraPermission: () -> Unit = {}
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var showDeviceCheckDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = SpaceDarkBg,
    bottomBar = {
      NavigationBar(
        containerColor = SpaceSurfaceDark,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 0.8.dp, color = SpaceCardBorder)
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
          label = { Text("홈") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CyanNeon,
            selectedTextColor = CyanNeon,
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = Color(0x3300E5FF)
          )
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = {
            selectedTab = 1
            onOpenProject()
          },
          icon = { Icon(Icons.Default.FolderOpen, contentDescription = "프로젝트") },
          label = { Text("프로젝트") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CyanNeon,
            selectedTextColor = CyanNeon,
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = Color(0x3300E5FF)
          )
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
          label = { Text("설정/진단") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CyanNeon,
            selectedTextColor = CyanNeon,
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = Color(0x3300E5FF)
          )
        )
      }
    }
  ) { paddingValues ->
    if (selectedTab == 2) {
      // Settings & Device Diagnostic View
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "설정 및 기기 진단",
              color = Color.White,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "실제 안드로이드 기기 호환성 및 빌드 상태",
              color = CyanNeon,
              fontSize = 12.sp
            )
          }

          Button(
            onClick = { showDeviceCheckDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PhoneAndroid,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "기기 호환성 검사",
              color = Color.Black,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Build & Package Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = SpaceSurfaceDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = ScanCompletedGreen,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "앱 빌드 및 서명 정보",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            SettingsInfoRow("Build Variant", if (BuildConfig.DEBUG) "DEBUG (Phone Test Ready)" else "RELEASE")
            SettingsInfoRow("Signing Config", "Default Android Debug Signing")
            SettingsInfoRow("Application ID", BuildConfig.APPLICATION_ID)
            SettingsInfoRow("Version Name", BuildConfig.VERSION_NAME)
            SettingsInfoRow("Version Code", "${BuildConfig.VERSION_CODE}")
          }
        }

        // Architecture & Modules Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = SpaceSurfaceDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "핵심 모듈 상태",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            SettingsInfoRow("3D AR Engine", "ARCore 1.47 (VIO + Depth)")
            SettingsInfoRow("Sensor PDR", "Accelerometer + Gyroscope")
            SettingsInfoRow("Local Database", "Room SQLite (Offline-First)")
            SettingsInfoRow("UI Framework", "Jetpack Compose + Material 3")
            SettingsInfoRow("Camera Integration", "Camera2 / ARCore Session")
          }
        }

        // Developer Actions
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = SpaceSurfaceDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "기기 권한 및 테스트",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = onRequestCameraPermission,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
              border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("카메라 권한 다시 요청하기")
            }
          }
        }
      }
    } else {
      // Home Tab
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Brand Header
        Column {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(id = R.drawable.ic_spacescan_logo_1789816242558),
                contentDescription = "SpaceScan 로고",
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .border(1.dp, CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "SpaceScan AI",
                  color = Color.White,
                  fontWeight = FontWeight.Black,
                  fontSize = 24.sp,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "건물 내부용 3D 스캔 & AI 서베이",
                  color = CyanNeon,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            // Quick Device Check Button
            Surface(
              color = Color(0x3300E5FF),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f)),
              modifier = Modifier.clickable { showDeviceCheckDialog = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.PhoneAndroid,
                  contentDescription = null,
                  tint = CyanNeon,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "기기 검사",
                  color = CyanNeon,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "학교 전체를 스마트폰으로 3D 스캔하고\n실시간 2D 평면도 생성과 AI 경로 가이드를 경험하세요.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            lineHeight = 19.sp
          )
        }

        // Middle Architectural School Hero Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.verticalGradient(
                listOf(
                  Color(0xFF0F1B38),
                  Color(0xFF091024)
                )
              )
            )
            .border(1.2.dp, SpaceCardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0x3300E5FF))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(text = "세종관 (본관) 실시간 연동", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
              Box(
                modifier = Modifier
                  .clip(CircleShape)
                  .background(Color(0x3300E676))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(text = "측위 정확도 0.8m", color = ScanCompletedGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }

            // Center illustration / badges
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround,
              verticalAlignment = Alignment.CenterVertically
            ) {
              HeroStatItem("4개 층", "B1 ~ 3F 통합")
              HeroStatItem("78%", "전체 스캔 완료")
              HeroStatItem("AI 가이드", "실시간 음성 경로")
            }

            Text(
              text = "3D 스캔 + 2D 실내지도 + 층별 이동 추적 + AI 최적 경로",
              color = Color(0xFF64748B),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Action Cards (Buttons 1, 2, 3 matching Screenshot 1)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Button 1: 새 프로젝트 시작
          ActionFeatureCard(
            icon = Icons.Default.QrCodeScanner,
            title = "새 프로젝트 시작",
            subtitle = "새로운 프로젝트를 생성하고 3D/2D 건물 스캔을 시작합니다",
            gradient = Brush.horizontalGradient(listOf(ElectricBlue, Color(0xFF0077B6))),
            isHighlighted = true,
            onClick = onNewProjectClick
          )

          // Button 2: 기존 프로젝트 불러오기
          ActionFeatureCard(
            icon = Icons.Default.FolderOpen,
            title = "기존 프로젝트 불러오기",
            subtitle = "세종관 (본관) 저장된 3D/2D 지도를 불러옵니다",
            gradient = Brush.horizontalGradient(listOf(SpaceSurfaceElevated, SpaceSurfaceDark)),
            isHighlighted = false,
            onClick = onOpenProject
          )

          // Button 3: 데모 모드 (가상 건물 체험)
          ActionFeatureCard(
            icon = Icons.Default.SmartToy,
            title = "데모 모드 (체험하기)",
            subtitle = "가상 학교 건물에서 위치 추적과 AI 가이드를 체험하세요",
            gradient = Brush.horizontalGradient(listOf(Color(0xFF2E1C6A), Color(0xFF19113B))),
            badge = "추천",
            isHighlighted = false,
            onClick = onStartDemoMode
          )
        }

        Spacer(modifier = Modifier.height(4.dp))
      }
    }
  }

  // Device Compatibility Dialog
  if (showDeviceCheckDialog) {
    DeviceCompatibilityDialog(
      onDismiss = { showDeviceCheckDialog = false },
      onRequestCameraPermission = onRequestCameraPermission
    )
  }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
    Text(
      text = value,
      color = Color.White,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
private fun HeroStatItem(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp)
  }
}

@Composable
private fun ActionFeatureCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  gradient: Brush,
  badge: String? = null,
  isHighlighted: Boolean = false,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(gradient)
      .border(
        width = if (isHighlighted) 1.5.dp else 1.dp,
        color = if (isHighlighted) CyanNeon else SpaceCardBorder,
        shape = RoundedCornerShape(14.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33FFFFFF)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            if (badge != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(ScanInProgressAmber)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(text = badge, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
              }
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            maxLines = 1
          )
        }
      }

      Icon(
        imageVector = Icons.Default.ViewInAr,
        contentDescription = null,
        tint = CyanNeon,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
