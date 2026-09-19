package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ar.ArCoreScanEngine
import com.example.data.ProjectRepository
import com.example.model.ArCoreTrackingStatus
import com.example.model.Building
import com.example.model.MockSchoolBuilding
import com.example.model.Project
import com.example.model.Room
import com.example.model.ScanImage
import com.example.model.ScanMissingArea
import com.example.model.ScanSegment
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
import com.example.ui.components.ScanImageDetailSheet
import com.example.ui.components.Spatial3DScannerView
import com.example.ui.components.TopScanBar
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import kotlinx.coroutines.launch

enum class WorkspaceViewMode {
  SPLIT_3D_AND_MAP, // Mode: 45% Camera/AR + 30% 2D Map + 15% AI/Images
  MAP_FOCUSED,       // Mode: Full 2D Indoor Map Focused
  THREE_D_FOCUSED,   // Mode: Full 3D Reconstructed Mesh/Point Viewer
  BUILDING_OVERVIEW, // Mode: Multi-floor Stacked Overview
  AI_MANAGER         // Mode: AI Survey Manager
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanWorkspaceScreen(
  project: Project?,
  tracker: PositionTracker,
  voiceAssistant: VoiceAssistant,
  repository: ProjectRepository? = null,
  onProjectUpdated: (Project) -> Unit = {},
  onExit: () -> Unit,
  isNewlyCreated: Boolean = false
) {
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

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val isProjectDemo = project.id == "p-demo-sejong" || project.id == "project_sejong_default" || project.mode == "DEMO"

  // Initialize Real ARCore Engine
  val scanEngine = remember(project.id) {
    ArCoreScanEngine(context, scope)
  }

  DisposableEffect(project.id) {
    onDispose {
      scanEngine.destroy()
    }
  }

  val building: Building = project.building
    ?: ProjectRepository.createFreshBuilding(project.id, project.buildingName, project.floors)

  LaunchedEffect(project.id) {
    tracker.resetForProject(
      isDemoMode = isProjectDemo,
      initialFloorId = building.floors.firstOrNull()?.id ?: "1F"
    )
    if (!isProjectDemo) {
      scanEngine.resetProjectScanData(building.floors.firstOrNull()?.id ?: "1F")
    }
  }

  // Connect ARCore realtime pose to PositionTracker
  LaunchedEffect(scanEngine, isProjectDemo) {
    if (!isProjectDemo) {
      scanEngine.sessionStats.collect { stats ->
        val pose = stats.currentPose
        if (pose != null) {
          tracker.updatePoseFromArCore(
            x = pose.x,
            y = pose.y,
            z = pose.z,
            yawDeg = pose.yawDegrees,
            isTracking = stats.trackingStatus == ArCoreTrackingStatus.TRACKING
          )
        }
      }
    }
  }

  val userPose by tracker.userPose.collectAsState()
  val breadcrumbs by tracker.breadcrumbs.collectAsState()
  val isDemoMode by tracker.isDemoMode.collectAsState()
  val isVoiceEnabled by voiceAssistant.isVoiceEnabled.collectAsState()
  val floorTransitionMsg by tracker.floorTransitionMessage.collectAsState()
  val relocalizationState by tracker.relocalizationState.collectAsState()

  // Real scan state from ARCoreScanEngine
  val accumulatedPoints by scanEngine.accumulatedPoints.collectAsState()
  val detectedPlanes by scanEngine.detectedPlanes.collectAsState()
  val accumulatedMeshes by scanEngine.accumulatedMeshes.collectAsState()
  val capturedImages by scanEngine.capturedImages.collectAsState()
  val sessionStats by scanEngine.sessionStats.collectAsState()

  // Selected floor & 2D/3D synchronized selected room & selected image
  val initialFloorId = remember(building) {
    building.floors.firstOrNull()?.id ?: "1F"
  }
  var activeFloorId by remember { mutableStateOf(userPose.floorId.ifEmpty { initialFloorId }) }
  var selectedRoom by remember { mutableStateOf<Room?>(null) }
  var selectedImageForDetail by remember { mutableStateOf<ScanImage?>(null) }
  var viewMode by remember { mutableStateOf(WorkspaceViewMode.SPLIT_3D_AND_MAP) }
  var showCreationSuccessBanner by remember { mutableStateOf(isNewlyCreated) }

  // Modals & Navigation state
  var showCoverageDialog by remember { mutableStateOf(false) }
  var showAiManagerSheet by remember { mutableStateOf(false) }
  var showNavigationOverlay by remember { mutableStateOf(false) }
  var activeMissingAreaDialog by remember { mutableStateOf<ScanMissingArea?>(null) }

  val imageDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

  // Real-time AI Recommendation calculation
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

  // Handle Scan Segment creation & Project auto-sync
  val handleScanSegmentCreated: (ScanSegment) -> Unit = { segment ->
    if (repository != null) {
      val updated = repository.addScanSegment(project.id, segment)
      if (updated != null) {
        onProjectUpdated(updated)
        val msg = "스캔 저장 완료: 포인트 ${segment.points.size}개, 메쉬 ${segment.planes.size}개 (진행률: ${updated.scanProgress}%)"
        scope.launch {
          snackbarHostState.showSnackbar(msg)
          voiceAssistant.speak(msg)
        }
      }
    }
  }

  val handleKeyframeCaptured: (ScanImage) -> Unit = { image ->
    scope.launch {
      val msg = "📷 스캔 키프레임 캡처 완료 (${String.format("%.1f", image.mapX)}m, ${String.format("%.1f", image.mapY)}m)"
      snackbarHostState.showSnackbar(msg)
      voiceAssistant.speak("키프레임 사진이 캡처되었습니다.")
    }
  }

  Scaffold(
    containerColor = SpaceDarkBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Column {
        TopScanBar(
          userPose = userPose,
          overallCoverage = if (isDemoMode) building.overallCoveragePercent else project.scanProgress,
          isDemoMode = isDemoMode,
          projectName = project.name,
          onRelocalizeClick = {
            tracker.relocalize { msg ->
              voiceAssistant.speak(msg, priority = true)
            }
          },
          onCoverageDetailsClick = { showCoverageDialog = true }
        )

        // Project Creation Success Banner
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
      NavigationBar(
        containerColor = SpaceSurfaceDark,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 0.8.dp, color = SpaceCardBorder)
      ) {
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.SPLIT_3D_AND_MAP,
          onClick = { viewMode = WorkspaceViewMode.SPLIT_3D_AND_MAP },
          icon = { Icon(Icons.Default.VerticalSplit, contentDescription = "분할뷰") },
          label = { Text("분할뷰") },
          colors = navBarColors()
        )
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.THREE_D_FOCUSED,
          onClick = { viewMode = WorkspaceViewMode.THREE_D_FOCUSED },
          icon = { Icon(Icons.Default.ViewInAr, contentDescription = "3D") },
          label = { Text("3D 뷰어") },
          colors = navBarColors()
        )
        NavigationBarItem(
          selected = viewMode == WorkspaceViewMode.MAP_FOCUSED,
          onClick = { viewMode = WorkspaceViewMode.MAP_FOCUSED },
          icon = { Icon(Icons.Default.Map, contentDescription = "지도") },
          label = { Text("2D 지도") },
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
      when (viewMode) {
        WorkspaceViewMode.SPLIT_3D_AND_MAP -> {
          // Layout Allocation: Camera (45-48%), Map (28-30%), AI & Images (18-20%)
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            // 1. Camera Preview & AR Projection Layer (46% Screen Height)
            Box(
              modifier = Modifier
                .weight(1.35f)
                .fillMaxWidth()
            ) {
              RealCameraArScannerView(
                floor = currentFloor,
                userPose = userPose,
                aiRecommendation = aiRecommendation,
                isDemoMode = isDemoMode,
                scanEngine = if (!isDemoMode) scanEngine else null,
                onHeadingRotated = { deltaDegrees ->
                  tracker.rotateHeading(deltaDegrees)
                },
                onToggleDemoMode = {
                  tracker.setDemoMode(!isDemoMode)
                },
                onScanSegmentCreated = handleScanSegmentCreated,
                onKeyframeCaptured = handleKeyframeCaptured
              )

              FloorSelector(
                selectedFloorId = activeFloorId,
                onFloorSelected = { newFloorId ->
                  activeFloorId = newFloorId
                  tracker.setFloor(newFloorId)
                  if (!isDemoMode) scanEngine.resetProjectScanData(newFloorId)
                },
                modifier = Modifier
                  .align(Alignment.CenterEnd)
                  .padding(end = 4.dp)
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. 2D Map with 800% Zoom and Camera Keyframe 📷 Pins (28% Screen Height)
            Box(
              modifier = Modifier
                .weight(1.0f)
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
                detectedPlanes = if (!isDemoMode) detectedPlanes else emptyList(),
                accumulatedPoints = if (!isDemoMode) accumulatedPoints else emptyList(),
                capturedImages = if (!isDemoMode) capturedImages else emptyList(),
                selectedImageId = selectedImageForDetail?.id,
                onImageSelected = { img ->
                  selectedImageForDetail = img
                },
                showFullLegend = false
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Captured Keyframe Images Carousel (Horizontal Strip)
            if (capturedImages.isNotEmpty() && !isDemoMode) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Collections,
                  contentDescription = null,
                  tint = CyanNeon,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "스캔 이미지 (${capturedImages.size}장)",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(capturedImages) { img ->
                  Box(
                    modifier = Modifier
                      .size(width = 64.dp, height = 48.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF0F172A))
                      .border(
                        1.dp,
                        if (img.id == selectedImageForDetail?.id) CyanNeon else SpaceCardBorder,
                        RoundedCornerShape(6.dp)
                      )
                      .clickable { selectedImageForDetail = img }
                  ) {
                    if (img.thumbnailBitmap != null) {
                      Image(
                        bitmap = img.thumbnailBitmap.asImageBitmap(),
                        contentDescription = "썸네일",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                          .size(20.dp)
                          .align(Alignment.Center)
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
            }

            // 4. AI Feedback Banner
            AiFeedbackBanner(
              recommendation = aiRecommendation,
              isVoiceEnabled = isVoiceEnabled,
              onToggleVoice = { voiceAssistant.toggleVoice() },
              onViewRouteClick = { showNavigationOverlay = true }
            )
          }
        }

        WorkspaceViewMode.MAP_FOCUSED -> {
          // Full 2D Map Focused
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp)
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
              detectedPlanes = if (!isDemoMode) detectedPlanes else emptyList(),
              accumulatedPoints = if (!isDemoMode) accumulatedPoints else emptyList(),
              capturedImages = if (!isDemoMode) capturedImages else emptyList(),
              selectedImageId = selectedImageForDetail?.id,
              onImageSelected = { img ->
                selectedImageForDetail = img
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
                .padding(end = 4.dp)
            )
          }
        }

        WorkspaceViewMode.THREE_D_FOCUSED -> {
          // Full 3D Reconstructed Mesh / Point Cloud Viewer
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp)
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
              realPoints = if (!isDemoMode) accumulatedPoints else emptyList(),
              realPlanes = if (!isDemoMode) detectedPlanes else emptyList(),
              realMeshes = if (!isDemoMode) accumulatedMeshes else emptyList(),
              capturedImages = if (!isDemoMode) capturedImages else emptyList(),
              selectedImageId = selectedImageForDetail?.id,
              onImageClicked = { img ->
                selectedImageForDetail = img
              },
              isDemoMode = isDemoMode,
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
                .padding(end = 4.dp)
            )

            // Bottom AI Recommendation Pill
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "데모 이동 (탭하여 다음 지점으로)",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Exit / Back Button on top left
      IconButton(
        onClick = onExit,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp)
          .clip(CircleShape)
          .background(Color(0xCC0F172A))
          .border(0.8.dp, SpaceCardBorder, CircleShape)
          .size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "작업공간 나가기",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }

  // Modals & Sheets
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
        viewMode = WorkspaceViewMode.SPLIT_3D_AND_MAP
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
      onDismiss = { activeMissingAreaDialog = null },
      onStartRescan = {
        activeMissingAreaDialog = null
        voiceAssistant.speak("${missingArea.title} 재스캔을 시작합니다. 벽면 모서리를 향해 이동하세요.")
      }
    )
  }

  // Scan Image Detail Sheet
  selectedImageForDetail?.let { img ->
    ScanImageDetailSheet(
      scanImage = img,
      sheetState = imageDetailSheetState,
      onDismiss = { selectedImageForDetail = null },
      onViewIn3D = { targetImage ->
        selectedImageForDetail = null
        viewMode = WorkspaceViewMode.THREE_D_FOCUSED
      },
      onViewOnMap = { targetImage ->
        selectedImageForDetail = null
        viewMode = WorkspaceViewMode.MAP_FOCUSED
      }
    )
  }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
  selectedIconColor = CyanNeon,
  selectedTextColor = CyanNeon,
  unselectedIconColor = Color(0xFF64748B),
  unselectedTextColor = Color(0xFF64748B),
  indicatorColor = SpaceSurfaceElevated
)
