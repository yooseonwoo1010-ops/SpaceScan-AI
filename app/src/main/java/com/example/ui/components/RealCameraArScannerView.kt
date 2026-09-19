package com.example.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.AiRecommendation
import com.example.model.Floor
import com.example.model.Room
import com.example.model.ScanStatus
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanVisualSystem
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RealCameraArScannerView(
  floor: Floor,
  userPose: UserPose,
  aiRecommendation: AiRecommendation?,
  isDemoMode: Boolean,
  onHeadingRotated: (Float) -> Unit,
  onToggleDemoMode: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraHardware by remember { mutableStateOf(true) }
  var cameraBindingError by remember { mutableStateOf(false) }

  // Scanning laser line animation
  val infiniteTransition = rememberInfiniteTransition(label = "ar_scanner")
  val laserPosition by infiniteTransition.animateFloat(
    initialValue = 0.12f,
    targetValue = 0.88f,
    animationSpec = infiniteRepeatable(
      animation = tween(2400, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_sweep"
  )

  // AR Pulse animation
  val pulseRing by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_ring"
  )

  // Simulated 3D feature points for LiDAR overlay
  val featurePoints = remember(floor.id) {
    val rng = Random(floor.id.hashCode() + 42)
    List(120) {
      Triple(rng.nextFloat(), rng.nextFloat(), rng.nextInt(4))
    }
  }

  // Heading calculation relative to AI recommendation target
  val targetBearing = remember(userPose, aiRecommendation) {
    if (aiRecommendation != null) {
      val targetRoom = floor.rooms.find { it.id == aiRecommendation.nextTargetRoomId }
      if (targetRoom != null) {
        val dx = targetRoom.x - userPose.x
        val dy = targetRoom.y - userPose.y
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        (angleDeg + 360f) % 360f
      } else 90f
    } else 90f
  }

  // Angle difference between current camera heading and target
  val headingDiff = ((targetBearing - userPose.yawDegrees + 540f) % 360f) - 180f

  val hasScannedData = isDemoMode || floor.rooms.isNotEmpty() || floor.coveragePercent > 0

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceDarkBg)
      .clip(RoundedCornerShape(14.dp))
      .border(1.2.dp, SpaceCardBorder, RoundedCornerShape(14.dp))
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          // Dragging rotates camera heading smoothly
          onHeadingRotated(dragAmount.x * 0.25f)
        }
      }
  ) {
    // 1. Camera Feed: Real Phone CameraX or Photorealistic School Corridor Demo
    if (isDemoMode) {
      // Photorealistic School Corridor Perspective View (Demo Camera Mode ONLY)
      DemoCorridorCameraView(
        yawDegrees = userPose.yawDegrees,
        modifier = Modifier.fillMaxSize()
      )
    } else if (hasCameraHardware && !cameraBindingError) {
      AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
          }
          try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
              try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                  it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                  lifecycleOwner,
                  CameraSelector.DEFAULT_BACK_CAMERA,
                  preview
                )
              } catch (e: Exception) {
                cameraBindingError = true
              }
            }, ContextCompat.getMainExecutor(ctx))
          } catch (e: Exception) {
            cameraBindingError = true
          }
          previewView
        },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      RealScannerViewfinderBackground(modifier = Modifier.fillMaxSize())
    }

    // 2. AR 3D Scan Spatial Overlay: Mesh, Point Cloud, Bounding Boxes, & 6-Color System
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Corridor 3D Perspective vanishing point anchored by user's heading
      val vpX = w * 0.5f + (userPose.yawDegrees - 90f) * 2.2f
      val vpY = h * 0.48f

      if (hasScannedData) {
        // A. AR Structure Overlay: Left Wall (🟩 스캔 완료 - COMPLETED)
      // Section 15: 스캔 완료 -> 일반 화면처럼 보이게 매우 약한 투명도 (alpha ~ 0.10)
      val leftWallPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.22f, vpY * 0.45f)
        lineTo(w * 0.22f, h - (h - vpY) * 0.45f)
        lineTo(0f, h)
        close()
      }
      drawPath(
        path = leftWallPath,
        color = ScanVisualSystem.Completed.copy(alpha = ScanVisualSystem.getCameraOverlayAlpha(ScanStatus.COMPLETED))
      )
      drawPath(
        path = leftWallPath,
        color = ScanVisualSystem.Completed.copy(alpha = 0.35f),
        style = Stroke(width = 1.5f)
      )

      // Subtle green wireframe grid on left wall
      for (i in 1..4) {
        val frac = i / 5f
        val topPt = Offset(w * 0.22f * frac, vpY * 0.45f * frac)
        val btmPt = Offset(w * 0.22f * frac, h - (h - vpY) * (1f - frac * 0.55f))
        drawLine(
          color = ScanVisualSystem.Completed.copy(alpha = 0.22f),
          start = topPt,
          end = btmPt,
          strokeWidth = 1f
        )
      }

      // B. AR Structure Overlay: Right Wall (🟦 현재 스캔 진행 중 - IN_PROGRESS)
      // Section 15: 반투명 BLUE (alpha ~ 0.42)
      val rightWallPath = Path().apply {
        moveTo(w, 0f)
        lineTo(w * 0.78f, vpY * 0.45f)
        lineTo(w * 0.78f, h - (h - vpY) * 0.45f)
        lineTo(w, h)
        close()
      }
      drawPath(
        path = rightWallPath,
        color = ScanVisualSystem.InProgress.copy(alpha = ScanVisualSystem.getCameraOverlayAlpha(ScanStatus.IN_PROGRESS))
      )
      drawPath(
        path = rightWallPath,
        color = ScanVisualSystem.InProgress,
        style = Stroke(width = 2f)
      )

      // Active wireframe mesh on right wall
      for (i in 1..4) {
        val frac = i / 5f
        val topPt = Offset(w - (w * 0.22f) * frac, vpY * 0.45f * frac)
        val btmPt = Offset(w - (w * 0.22f) * frac, h - (h - vpY) * (1f - frac * 0.55f))
        drawLine(
          color = ScanVisualSystem.InProgress.copy(alpha = 0.5f),
          start = topPt,
          end = btmPt,
          strokeWidth = 1.2f
        )
      }

      // C. AR Structure Overlay: Ceiling (🟨 품질 낮음 / 추가 촬영 권장 - LOW_QUALITY)
      // Section 15: 반투명 YELLOW (alpha ~ 0.48)
      val ceilingPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w * 0.78f, vpY * 0.45f)
        lineTo(w * 0.22f, vpY * 0.45f)
        close()
      }
      drawPath(
        path = ceilingPath,
        color = ScanVisualSystem.LowQuality.copy(alpha = ScanVisualSystem.getCameraOverlayAlpha(ScanStatus.LOW_QUALITY))
      )
      drawPath(
        path = ceilingPath,
        color = ScanVisualSystem.LowQuality.copy(alpha = 0.6f),
        style = Stroke(width = 1.5f)
      )

      // D. AR Structure Overlay: Floor (🟩 스캔 완료 - COMPLETED)
      // Perspective depth grid lines
      val floorPath = Path().apply {
        moveTo(0f, h)
        lineTo(w, h)
        lineTo(w * 0.78f, h - (h - vpY) * 0.45f)
        lineTo(w * 0.22f, h - (h - vpY) * 0.45f)
        close()
      }
      drawPath(
        path = floorPath,
        color = ScanVisualSystem.Completed.copy(alpha = 0.12f)
      )
      // Floor perspective guide lines
      for (i in 1..3) {
        val frac = i / 4f
        val startPt = Offset(w * frac, h)
        val endPt = Offset(w * 0.22f + (w * 0.56f) * frac, h - (h - vpY) * 0.45f)
        drawLine(
          color = ScanVisualSystem.Completed.copy(alpha = 0.35f),
          start = startPt,
          end = endPt,
          strokeWidth = 1f
        )
      }

      // E. Front Door (🟪 AI 추천 대상 - AI_RECOMMENDED)
      // Section 15: 반투명 PURPLE (alpha ~ 0.52)
      val doorLeft = w * 0.43f
      val doorTop = vpY * 0.55f
      val doorW = w * 0.14f
      val doorH = (h - vpY) * 0.55f
      drawRoundRect(
        color = ScanVisualSystem.AiRecommended.copy(alpha = ScanVisualSystem.getCameraOverlayAlpha(ScanStatus.AI_RECOMMENDED)),
        topLeft = Offset(doorLeft, doorTop),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(6f, 6f)
      )
      drawRoundRect(
        color = ScanVisualSystem.AiRecommended,
        topLeft = Offset(doorLeft, doorTop),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.5f * pulseRing)
      )

      // F. North Wall Defect Area (🟥 재스캔 필요 - RESCAN_NEEDED)
      // Red warning hatch overlay
      val defectLeft = w * 0.08f
      val defectTop = h * 0.38f
      val defectW = w * 0.12f
      val defectH = h * 0.24f
      drawRoundRect(
        color = ScanVisualSystem.RescanNeeded.copy(alpha = ScanVisualSystem.getCameraOverlayAlpha(ScanStatus.RESCAN_NEEDED)),
        topLeft = Offset(defectLeft, defectTop),
        size = Size(defectW, defectH),
        cornerRadius = CornerRadius(4f, 4f)
      )
      drawRoundRect(
        color = ScanVisualSystem.RescanNeeded,
        topLeft = Offset(defectLeft, defectTop),
        size = Size(defectW, defectH),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
      )

      // G. 3D LiDAR Point Cloud Particles (Spatial depth cloud)
      featurePoints.forEach { pt ->
        val px = w * pt.first
        val py = h * pt.second
        val ptColor = when (pt.third) {
          0 -> ScanVisualSystem.Completed.copy(alpha = 0.75f)
          1 -> ScanVisualSystem.InProgress.copy(alpha = 0.85f)
          2 -> ScanVisualSystem.AiRecommended.copy(alpha = 0.8f)
          else -> CyanNeon.copy(alpha = 0.8f)
        }
        drawCircle(
          color = ptColor,
          radius = 2.2f,
          center = Offset(px, py)
        )
      }
    }

      // H. Moving LiDAR Laser Scan Sweep Line
      val laserY = h * laserPosition
      drawLine(
        color = CyanNeon.copy(alpha = 0.85f),
        start = Offset(w * 0.05f, laserY),
        end = Offset(w * 0.95f, laserY),
        strokeWidth = 2f,
        cap = StrokeCap.Round
      )
      // Laser beam vertical glow band
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            CyanNeon.copy(alpha = 0f),
            CyanNeon.copy(alpha = 0.25f),
            CyanNeon.copy(alpha = 0f)
          ),
          startY = laserY - 14f,
          endY = laserY + 14f
        ),
        topLeft = Offset(w * 0.05f, laserY - 14f),
        size = Size(w * 0.9f, 28f)
      )

      // I. Active Scan Boundary Overlay Reticle (Section 5)
      drawScanBoundaryReticle(w, h, CyanNeon)
    }

    if (hasScannedData) {
      // 3. AR Floating Labels on Structures (Demo or Scanned data)
      // Left wall: 🟩 스캔 완료
      StructureArBadge(
        text = "왼쪽 벽: 🟩 스캔 완료",
        badgeColor = ScanVisualSystem.Completed,
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp)
      )

      // Right wall: 🟦 스캔 중
      StructureArBadge(
        text = "오른쪽 벽: 🟦 스캔 중",
        badgeColor = ScanVisualSystem.InProgress,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 12.dp)
      )

      // Ceiling: 🟨 추가 스캔 필요
      StructureArBadge(
        text = "천장: 🟨 추가 스캔 필요",
        badgeColor = ScanVisualSystem.LowQuality,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 44.dp)
      )

      // Defect corner: 🟥 재스캔 필요
      StructureArBadge(
        text = "201호 북쪽: 🟥 재스캔 필요",
        badgeColor = ScanVisualSystem.RescanNeeded,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 12.dp, bottom = 48.dp)
      )

      // Next target door: 🟪 AI 추천
      StructureArBadge(
        text = "앞쪽 203호: 🟪 AI 추천 (24m)",
        badgeColor = ScanVisualSystem.AiRecommended,
        modifier = Modifier
          .align(Alignment.Center)
          .padding(bottom = 20.dp)
      )

      // 4. AR Directional Guidance Indicator towards AI Next Target (Section 10)
      ArDirectionalGuideBanner(
        headingDiff = headingDiff,
        recommendation = aiRecommendation,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 8.dp)
      )

      // 5. Real-time Object Recognition HUD Card (Section 9)
      AiObjectRecognitionHudCard(
        currentRoomName = "2F 중앙 복도",
        completionRate = 87,
        recognizedObjects = "벽 4 · 문 3 · 창문 6 · 계단 1",
        aiAnalysis = "현재 복도 구조 확보됨 (오른쪽 벽 데이터 보강 중)",
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 8.dp, start = 8.dp)
      )
    } else {
      // Clean Empty State for Fresh / Real Projects (Section 6 & 20)
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0B132B))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
          Text(
            text = "아직 스캔 데이터 없음",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "카메라를 움직여 스캔하세요",
            color = CyanNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Initial HUD Card for Real Scan
      AiObjectRecognitionHudCard(
        currentRoomName = "${floor.name.ifEmpty { floor.id }} 미스캔 구역",
        completionRate = floor.coveragePercent,
        recognizedObjects = "스캔 준비 중",
        aiAnalysis = "카메라를 천천히 이동하며 벽면과 바닥을 스캔하세요.",
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 8.dp, start = 8.dp)
      )
    }

    // 6. Camera Status & Compass Heading HUD (Section 6 & 8)
    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 8.dp, end = 8.dp),
      horizontalAlignment = Alignment.End
    ) {
      // Real scan / Demo Mode badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(CircleShape)
          .background(Color(0xCC0B132B))
          .border(1.dp, if (isDemoMode) ScanVisualSystem.LowQuality else ScanVisualSystem.Completed, CircleShape)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .background(if (isDemoMode) ScanVisualSystem.LowQuality else ScanVisualSystem.Completed, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = if (isDemoMode) "DEMO MODE" else "REAL SCAN",
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.Black
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Heading & Camera Reticle Badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xCC0F172A))
          .border(0.8.dp, SpaceCardBorder, RoundedCornerShape(6.dp))
          .padding(horizontal = 7.dp, vertical = 3.dp)
      ) {
        val headingText = when ((userPose.yawDegrees / 45f).toInt() % 8) {
          0 -> "북 (N)"
          1 -> "북동 (NE)"
          2 -> "동 (E)"
          3 -> "남동 (SE)"
          4 -> "남 (S)"
          5 -> "남서 (SW)"
          6 -> "서 (W)"
          else -> "북서 (NW)"
        }
        Text(
          text = "▲ ${userPose.yawDegrees.toInt()}° $headingText",
          color = CyanNeon,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * Photorealistic corridor view for Demo Camera Mode in Android Emulator
 */
@Composable
private fun DemoCorridorCameraView(
  yawDegrees: Float,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val panOffset = (yawDegrees - 90f) * 1.8f
    val vpX = w * 0.5f + panOffset
    val vpY = h * 0.48f

    // 1. Back Wall (End of Hallway)
    val backWallW = w * 0.32f
    val backWallH = h * 0.30f
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        startY = vpY - backWallH / 2f,
        endY = vpY + backWallH / 2f
      ),
      topLeft = Offset(vpX - backWallW / 2f, vpY - backWallH / 2f),
      size = Size(backWallW, backWallH)
    )

    // End of hallway double doors
    val doorW = backWallW * 0.42f
    val doorH = backWallH * 0.72f
    drawRoundRect(
      color = Color(0xFF334155),
      topLeft = Offset(vpX - doorW / 2f, vpY + backWallH / 2f - doorH),
      size = Size(doorW, doorH),
      cornerRadius = CornerRadius(2f, 2f)
    )
    // Emergency Exit Sign above door
    drawRoundRect(
      color = Color(0xFF10B981),
      topLeft = Offset(vpX - 16f, vpY + backWallH / 2f - doorH - 12f),
      size = Size(32f, 8f),
      cornerRadius = CornerRadius(2f, 2f)
    )

    // 2. Left Wall with architectural perspective (soft school interior cream/slate)
    val leftWall = Path().apply {
      moveTo(0f, 0f)
      lineTo(vpX - backWallW / 2f, vpY - backWallH / 2f)
      lineTo(vpX - backWallW / 2f, vpY + backWallH / 2f)
      lineTo(0f, h)
      close()
    }
    drawPath(
      path = leftWall,
      brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
      )
    )

    // Left Wall classroom door frame (201호 / 202호)
    val lDoorTop = vpY - backWallH * 0.2f
    val lDoorBtm = vpY + backWallH * 0.9f
    drawRoundRect(
      color = Color(0xFF26334D),
      topLeft = Offset(w * 0.08f, h * 0.25f),
      size = Size(w * 0.16f, h * 0.55f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    // Door window
    drawRoundRect(
      color = Color(0xFF64748B).copy(alpha = 0.35f),
      topLeft = Offset(w * 0.10f, h * 0.30f),
      size = Size(w * 0.12f, h * 0.18f),
      cornerRadius = CornerRadius(3f, 3f)
    )

    // 3. Right Wall with architectural perspective
    val rightWall = Path().apply {
      moveTo(w, 0f)
      lineTo(vpX + backWallW / 2f, vpY - backWallH / 2f)
      lineTo(vpX + backWallW / 2f, vpY + backWallH / 2f)
      lineTo(w, h)
      close()
    }
    drawPath(
      path = rightWall,
      brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
      )
    )

    // Right Wall classroom door frame (203호)
    drawRoundRect(
      color = Color(0xFF26334D),
      topLeft = Offset(w * 0.76f, h * 0.25f),
      size = Size(w * 0.16f, h * 0.55f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
      color = Color(0xFF64748B).copy(alpha = 0.35f),
      topLeft = Offset(w * 0.78f, h * 0.30f),
      size = Size(w * 0.12f, h * 0.18f),
      cornerRadius = CornerRadius(3f, 3f)
    )

    // 4. Ceiling with recessed linear LED lighting
    val ceiling = Path().apply {
      moveTo(0f, 0f)
      lineTo(w, 0f)
      lineTo(vpX + backWallW / 2f, vpY - backWallH / 2f)
      lineTo(vpX - backWallW / 2f, vpY - backWallH / 2f)
      close()
    }
    drawPath(
      path = ceiling,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1322), Color(0xFF192238))
      )
    )

    // Ceiling Light panels
    for (i in 0..2) {
      val frac = (i + 1) * 0.28f
      val lightY = h * 0.08f + i * 18f
      val lightW = w * 0.24f * (1f - i * 0.2f)
      drawRoundRect(
        color = Color(0xFFF1F5F9).copy(alpha = 0.45f),
        topLeft = Offset(vpX - lightW / 2f, lightY),
        size = Size(lightW, 4f),
        cornerRadius = CornerRadius(2f, 2f)
      )
    }

    // 5. Polished Hallway Floor with subtle reflection lines
    val floor = Path().apply {
      moveTo(0f, h)
      lineTo(w, h)
      lineTo(vpX + backWallW / 2f, vpY + backWallH / 2f)
      lineTo(vpX - backWallW / 2f, vpY + backWallH / 2f)
      close()
    }
    drawPath(
      path = floor,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF151E2E), Color(0xFF0B101B))
      )
    )

    // Floor tile perspective seams
    for (step in 1..4) {
      val fY = (vpY + backWallH / 2f) + (h - (vpY + backWallH / 2f)) * (step / 5f)
      drawLine(
        color = Color(0xFF334155).copy(alpha = 0.4f),
        start = Offset(0f, fY),
        end = Offset(w, fY),
        strokeWidth = 1f
      )
    }
  }
}

/**
 * Reticle overlay indicating active camera scan boundary
 */
private fun DrawScope.drawScanBoundaryReticle(w: Float, h: Float, color: Color) {
  val marginX = w * 0.14f
  val marginY = h * 0.16f
  val cornerLen = 22f

  val l = marginX
  val r = w - marginX
  val t = marginY
  val b = h - marginY

  // Top-left corner
  drawLine(color, Offset(l, t), Offset(l + cornerLen, t), strokeWidth = 2f)
  drawLine(color, Offset(l, t), Offset(l, t + cornerLen), strokeWidth = 2f)

  // Top-right corner
  drawLine(color, Offset(r, t), Offset(r - cornerLen, t), strokeWidth = 2f)
  drawLine(color, Offset(r, t), Offset(r, t + cornerLen), strokeWidth = 2f)

  // Bottom-left corner
  drawLine(color, Offset(l, b), Offset(l + cornerLen, b), strokeWidth = 2f)
  drawLine(color, Offset(l, b), Offset(l, b - cornerLen), strokeWidth = 2f)

  // Bottom-right corner
  drawLine(color, Offset(r, b), Offset(r - cornerLen, b), strokeWidth = 2f)
  drawLine(color, Offset(r, b), Offset(r, b - cornerLen), strokeWidth = 2f)

  // Center crosshair
  val cx = w / 2f
  val cy = h / 2f
  drawLine(color.copy(alpha = 0.6f), Offset(cx - 8f, cy), Offset(cx + 8f, cy), strokeWidth = 1.5f)
  drawLine(color.copy(alpha = 0.6f), Offset(cx, cy - 8f), Offset(cx, cy + 8f), strokeWidth = 1.5f)
}

/**
 * AR Status badge anchored on 3D elements in camera
 */
@Composable
private fun StructureArBadge(
  text: String,
  badgeColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xE60A0F1D))
      .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = text,
      color = Color.White,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

/**
 * AI Real-time Object Recognition HUD Card (Section 9)
 */
@Composable
private fun AiObjectRecognitionHudCard(
  currentRoomName: String,
  completionRate: Int,
  recognizedObjects: String,
  aiAnalysis: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xCC090E1A))
      .border(0.8.dp, CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = CyanNeon,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "$currentRoomName ($completionRate% 완료)",
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "객체 인식: $recognizedObjects",
        color = Color(0xFF94A3B8),
        fontSize = 9.sp
      )
      Text(
        text = "AI 분석: $aiAnalysis",
        color = CyanNeon,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

/**
 * In-Camera AR Directional Guidance Banner (Section 10)
 */
@Composable
private fun ArDirectionalGuideBanner(
  headingDiff: Float,
  recommendation: AiRecommendation?,
  modifier: Modifier = Modifier
) {
  val targetName = recommendation?.nextTargetName ?: "203호"
  val distMeters = recommendation?.distanceMeters?.toInt() ?: 24

  // Direction instruction based on angular difference
  val (arrowText, actionText) = when {
    headingDiff in -25f..25f -> Pair("▲", "앞쪽 $distMeters m 직진")
    headingDiff < -25f -> Pair("◀", "왼쪽으로 ${(-headingDiff).toInt()}° 회전 (${distMeters}m)")
    else -> Pair("▶", "오른쪽으로 ${headingDiff.toInt()}° 회전 (${distMeters}m)")
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(Color(0xE6240046))
      .border(1.2.dp, ScanVisualSystem.AiRecommended, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 5.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = arrowText,
        color = ScanVisualSystem.AiRecommended,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "$targetName AI 추천 · $actionText",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun RealScannerViewfinderBackground(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    // High-tech dark scanner background
    drawRect(Color(0xFF070C18))

    // Tech spatial grid
    val step = 44f
    var x = 0f
    while (x < w) {
      drawLine(
        color = Color(0xFF1E293B).copy(alpha = 0.35f),
        start = Offset(x, 0f),
        end = Offset(x, h),
        strokeWidth = 1f
      )
      x += step
    }
    var y = 0f
    while (y < h) {
      drawLine(
        color = Color(0xFF1E293B).copy(alpha = 0.35f),
        start = Offset(0f, y),
        end = Offset(w, y),
        strokeWidth = 1f
      )
      y += step
    }

    // Viewfinder crosshairs
    val cx = w / 2f
    val cy = h / 2f
    drawLine(CyanNeon.copy(alpha = 0.4f), Offset(cx - 24f, cy), Offset(cx + 24f, cy), 1.5f)
    drawLine(CyanNeon.copy(alpha = 0.4f), Offset(cx, cy - 24f), Offset(cx, cy + 24f), 1.5f)
    drawCircle(CyanNeon.copy(alpha = 0.25f), radius = 36f, center = Offset(cx, cy), style = Stroke(1.2f))
  }
}
