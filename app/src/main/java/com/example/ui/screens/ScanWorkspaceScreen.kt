package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Building
import com.example.model.MockSchoolBuilding
import com.example.model.Project
import com.example.model.Room
import com.example.model.ScanMissingArea
import com.example.sensors.PositionTracker
import com.example.sensors.VoiceAssistant
import com.example.ui.components.AiFeedbackBanner
import com.example.ui.components.AiSurveyManagerSheet
import com.example.ui.components.CoverageDialog
import com.example.ui.components.FloorSelector
import com.example.ui.components.Indoor2DMapView
import com.example.ui.components.MissingAreaDialog
import com.example.ui.components.MultiFloorStacked3DView
import com.example.ui.components.NavigationOverlay
import com.example.ui.components.RealCameraArScannerView
import com.example.ui.components.Spatial3DScannerView
import com.example.ui.components.TopScanBar
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import kotlinx.coroutines.launch

enum class WorkspaceViewMode {
  SPLIT_3D_AND_MAP, // Mode 3: Top 3D + Middle 2D Map
  MAP_FOCUSED,       // Mode 2: 2D Indoor Map Focused
  THREE_D_FOCUSED,   // Mode 1: 3D Scanner Focused
  BUILDING_OVERVIEW, // Mode 4: Multi-floor Stacked Overview
  AI_MANAGER         // Mode 5: AI Survey Manager
}

@Composable
fun ScanWorkspaceScreen(
  project: Project?,
  tracker: PositionTracker,
  voiceAssistant: VoiceAssistant,
  onExit: () -> Unit,
  isNewlyCreated: Boolean = false
) {
  // If project is null, display fallback message (Section 6)
  if (project == null) {
    Scaffold(
      containerColor = SpaceDarkBg
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = ScanRescanRed,
            modifier = Modifier.size(52.dp)
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "프로젝트가 없습니다.",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "홈에서 새 프로젝트를 생성하거나 기존 프로젝트를 선택하세요.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(18.dp))
          Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
          ) {
            Text("홈으로 이동")
          }
        }
      }
    }
    return
  }

  val building: Building = project.building
    ?: com.example.data.ProjectRepository.createFreshBuilding(project.id, project.buildingName, project.floors)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val isProjectDemo = project.id == "p-demo-sejong" || project.id == "project_sejong_default"

  LaunchedEffect(project.id) {
    tracker.resetForProject(
      isDemoMode = isProjectDemo,
      initialFloorId = building.floors.firstOrNull()?.id ?: "1F"
    )
  }

  val userPose by tracker.userPose.collectAsState()
  val breadcrumbs by tracker.breadcrumbs.collectAsState()
  val isDemoMode by tracker.isDemoMode.collectAsState()
  val isVoiceEnabled by voiceAssistant.isVoiceEnabled.collectAsState()
  val floorTransitionMsg by tracker.floorTransitionMessage.collectAsState()
  val relocalizationState by tracker.relocalizationState.collectAsState()

  // Selected floor & 2D/3D synchronized selected room
  val initialFloorId = remember(building) {
    building.floors.firstOrNull()?.id ?: "1F"
  }
  var activeFloorId by remember { mutableStateOf(userPose.floorId.ifEmpty { initialFloorId }) }
  var selectedRoom by remember { mutableStateOf<Room?>(null) }
  var viewMode by remember { mutableStateOf(WorkspaceViewMode.SPLIT_3D_AND_MAP) }
  var showCreationSuccessBanner by remember { mutableStateOf(isNewlyCreated) }

  // Modals & Navigation state
  var showCoverageDialog by remember { mutableStateOf(false) }
  var showAiManagerSheet by remember { mutableStateOf(false) }
  var showNavigationOverlay by remember { mutableStateOf(false) }
  var activeMissingAreaDialog by remember { mutableStateOf<ScanMissingArea?>(null) }
  var isCameraFeedEnabled by remember { mutableStateOf(false) }

  // Active Floor object
  val currentFloor = building.floors.find { it.id == activeFloorId }
    ?: building.floors.firstOrNull()
    ?: com.example.model.Floor(
      id = "1F",
      name = "1층",
      levelIndex = 0,
      rooms = emptyList(),
      corridors = emptyList(),
      stairs = emptyList(),
      elevators = emptyList(),
      restrooms = emptyList(),
      coveragePercent = 0,
      completedRoomsCount = 0,
      totalRoomsCount = 0
    )

  // Real-time AI Recommendation calculation (only available if demo or rooms exist)
  val aiRecommendation = remember(userPose, activeFloorId, building, isProjectDemo) {
    if (isProjectDemo || building.floors.any { it.rooms.isNotEmpty() }) {
      MockSchoolBuilding.calculateRecommendation(userPose, building)
    } else {
      null
    }
  }

  // Handle floor transition announcements
  LaunchedEffect(floorTransitionMsg) {
    floorTransitionMsg?.let { msg ->
      voiceAssistant.speak(msg)
      snackbarHostState.showSnackbar(msg)
      tracker.dismissFloorMessage()
    }
  }

  // Handle relocalization status
  LaunchedEffect(relocalizationState.isRelocalizing) {
    if (relocalizationState.statusMessage.isNotEmpty()) {
      snackbarHostState.showSnackbar(relocalizationState.statusMessage)
      if (relocalizationState.success) {
        voiceAssistant.speak(relocalizationState.statusMessage)
      }
    }
  }

  Scaffold(
    containerColor = SpaceDarkBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Column {
        TopScanBar(
          userPose = userPose,
          overallCoverage = building.overallCoveragePercent,
          isDemoMode = isDemoMode,
          projectName = project.name,
          onRelocalizeClick = {
            tracker.relocalize { msg ->
              voiceAssistant.speak(msg, priority = true)
            }
          },
          onCoverageDetailsClick = { showCoverageDialog = true }
        )

        // Project Creation Success Banner (Section 16)
        AnimatedVisibility(visible = showCreationSuccessBanner) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(ScanCompletedGreen.copy(alpha = 0.22f))
              .border(0.8.dp, ScanCompletedGreen.copy(alpha = 0.7f))
              .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ScanCompletedGreen,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "✓ 프로젝트 생성 완료 — ${project.name} (${project.buildingName})",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
            }
            IconButton(
              onClick = { showCreationSuccessBanner = false },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    },
    bottomBar = {
      // Bottom Navigation matching Reference Screenshot (3D, 지도, 전체층, AI)
      NavigationBar(
        containerColor = SpaceSurfaceDark,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 0.8.dp, color = SpaceCardBorder)
      ) {
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.SPLIT_3D_AND_MAP,
          onClick = { viewMode = WorkspaceViewMode.SPLIT_3D_AND_MAP },
          icon = { Icon(Icons.Default.VerticalSplit, contentDescription = "3D+지도") },
          label = { Text("분할뷰") },
          colors = navBarColors()
        )
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.THREE_D_FOCUSED,
          onClick = { viewMode = WorkspaceViewMode.THREE_D_FOCUSED },
          icon = { Icon(Icons.Default.ViewInAr, contentDescription = "3D") },
          label = { Text("3D") },
          colors = navBarColors()
        )
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.MAP_FOCUSED,
          onClick = { viewMode = WorkspaceViewMode.MAP_FOCUSED },
          icon = { Icon(Icons.Default.Map, contentDescription = "지도") },
          label = { Text("지도") },
          colors = navBarColors()
        )
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.BUILDING_OVERVIEW,
          onClick = { viewMode = WorkspaceViewMode.BUILDING_OVERVIEW },
          icon = { Icon(Icons.Default.Layers, contentDescription = "전체층") },
          label = { Text("전체층") },
          colors = navBarColors()
        )
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.AI_MANAGER,
          onClick = {
            viewMode = WorkspaceViewMode.AI_MANAGER
            showAiManagerSheet = true
          },
          icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI 관리") },
          label = { Text("AI") },
          colors = navBarColors()
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Main Content Area based on selected viewMode
      when (viewMode) {
        WorkspaceViewMode.SPLIT_3D_AND_MAP -> {
          // Mode 3: Real Camera + 3D AR Overlay + 2D Map Split Screen (45~55% Camera)
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            // Upper: 45~55% Real Camera + 3D AR Spatial Overlay
            Box(
              modifier = Modifier
                .weight(1.15f)
                .fillMaxWidth()
            ) {
              RealCameraArScannerView(
                floor = currentFloor,
                userPose = userPose,
                aiRecommendation = aiRecommendation,
                isDemoMode = isDemoMode,
                onHeadingRotated = { deltaDegrees ->
                  tracker.rotateHeading(deltaDegrees)
                },
                onToggleDemoMode = {
                  tracker.setDemoMode(!isDemoMode)
                }
              )

              // Floor selector floating on upper right
              FloorSelector(
                selectedFloorId = activeFloorId,
                onFloorSelected = { newFloorId ->
                  activeFloorId = newFloorId
                  tracker.setFloor(newFloorId)
                },
                modifier = Modifier
                  .align(Alignment.CenterEnd)
                  .padding(end = 6.dp)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle: 2D Indoor Map View
            Box(
              modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth()
            ) {
              Indoor2DMapView(
                floor = currentFloor,
                userPose = userPose,
                breadcrumbs = breadcrumbs,
                recommendation = aiRecommendation,
                selectedRoomId = selectedRoom?.id,
                onRoomSelected = { room ->
                  selectedRoom = room
                  if (room.missingAreas.isNotEmpty()) {
                    activeMissingAreaDialog = room.missingAreas.first()
                  }
                },
                showFullLegend = true
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lower: AI Feedback Banner
            AiFeedbackBanner(
              recommendation = aiRecommendation,
              isVoiceEnabled = isVoiceEnabled,
              onToggleVoice = { voiceAssistant.toggleVoice() },
              onViewRouteClick = { showNavigationOverlay = true }
            )
          }
        }

        WorkspaceViewMode.MAP_FOCUSED -> {
          // Mode 2: 2D Map Focused (Matches Screenshot 3)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(10.dp)
          ) {
            Indoor2DMapView(
              floor = currentFloor,
              userPose = userPose,
              breadcrumbs = breadcrumbs,
              recommendation = aiRecommendation,
              selectedRoomId = selectedRoom?.id,
              onRoomSelected = { room ->
                selectedRoom = room
                if (room.missingAreas.isNotEmpty()) {
                  activeMissingAreaDialog = room.missingAreas.first()
                }
              },
              showFullLegend = true,
              modifier = Modifier.fillMaxSize()
            )

            FloorSelector(
              selectedFloorId = activeFloorId,
              onFloorSelected = { newFloorId ->
                activeFloorId = newFloorId
                tracker.setFloor(newFloorId)
              },
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
            )
          }
        }

        WorkspaceViewMode.THREE_D_FOCUSED -> {
          // Mode 1: 3D Scanner Focused
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(10.dp)
          ) {
            Spatial3DScannerView(
              floor = currentFloor,
              userPose = userPose,
              selectedRoom = selectedRoom,
              onRoomClicked = { room ->
                selectedRoom = room
                if (room.missingAreas.isNotEmpty()) {
                  activeMissingAreaDialog = room.missingAreas.first()
                }
              },
              isCameraFeedEnabled = isCameraFeedEnabled,
              onToggleCameraFeed = { isCameraFeedEnabled = !isCameraFeedEnabled },
              modifier = Modifier.fillMaxSize()
            )

            FloorSelector(
              selectedFloorId = activeFloorId,
              onFloorSelected = { newFloorId ->
                activeFloorId = newFloorId
                tracker.setFloor(newFloorId)
              },
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
            )

            // Bottom overlay AI pill
            AiFeedbackBanner(
              recommendation = aiRecommendation,
              isVoiceEnabled = isVoiceEnabled,
              onToggleVoice = { voiceAssistant.toggleVoice() },
              onViewRouteClick = { showNavigationOverlay = true },
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
            )
          }
        }

        WorkspaceViewMode.BUILDING_OVERVIEW -> {
          // Mode 4: Multi-floor Stacked Overview (Matches Screenshot 4)
          MultiFloorStacked3DView(
            building = building,
            currentFloorId = activeFloorId,
            onSelectFloor = { floorId ->
              activeFloorId = floorId
              tracker.setFloor(floorId)
            }
          )
        }

        WorkspaceViewMode.AI_MANAGER -> {
          // Keep showing split behind dialog
          MultiFloorStacked3DView(
            building = building,
            currentFloorId = activeFloorId,
            onSelectFloor = { floorId ->
              activeFloorId = floorId
              tracker.setFloor(floorId)
            }
          )
        }
      }

      // Demo Mode Floating Movement Controller
      // Allows immediate evaluation of 3D/2D sync, floor change, and AI priority update
      if (isDemoMode && viewMode != WorkspaceViewMode.BUILDING_OVERVIEW) {
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE61E293B))
            .border(1.2.dp, CyanNeon, RoundedCornerShape(20.dp))
            .clickable {
              tracker.nextDemoStep { newFloor ->
                activeFloorId = newFloor
              }
            }
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
              contentDescription = "이동 시뮬레이션",
              tint = CyanNeon,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "다음 위치로 이동 (101호→복도→계단→201호)",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Dialog Overlays
      if (showCoverageDialog) {
        CoverageDialog(
          building = building,
          onDismiss = { showCoverageDialog = false }
        )
      }

      if (showAiManagerSheet) {
        AiSurveyManagerSheet(
          recommendation = aiRecommendation,
          onNavigateToTarget = {
            showAiManagerSheet = false
            showNavigationOverlay = true
          },
          onDismiss = {
            showAiManagerSheet = false
            if (viewMode == WorkspaceViewMode.AI_MANAGER) {
              viewMode = WorkspaceViewMode.SPLIT_3D_AND_MAP
            }
          }
        )
      }

      if (showNavigationOverlay && aiRecommendation != null) {
        NavigationOverlay(
          floor = currentFloor,
          userPose = userPose,
          breadcrumbs = breadcrumbs,
          recommendation = aiRecommendation,
          onClose = { showNavigationOverlay = false }
        )
      }

      activeMissingAreaDialog?.let { missingArea ->
        MissingAreaDialog(
          missingArea = missingArea,
          onStartRescan = {
            activeMissingAreaDialog = null
            voiceAssistant.speak("${missingArea.title} 재스캔을 시작합니다. 벽면을 천천히 비추세요.", priority = true)
          },
          onDismiss = { activeMissingAreaDialog = null }
        )
      }
    }
  }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
  selectedIconColor = CyanNeon,
  selectedTextColor = CyanNeon,
  unselectedIconColor = Color(0xFF64748B),
  unselectedTextColor = Color(0xFF64748B),
  indicatorColor = Color(0x3300E5FF)
)
