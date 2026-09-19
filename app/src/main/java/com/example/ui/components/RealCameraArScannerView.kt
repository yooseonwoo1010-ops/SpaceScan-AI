package com.example.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ar.ArCoreScanEngine
import com.example.model.AiRecommendation
import com.example.model.ArCoreTrackingStatus
import com.example.model.CameraPoseData
import com.example.model.Floor
import com.example.model.PlaneClassification
import com.example.model.Point3D
import com.example.model.ScanPlane
import com.example.model.ScanPoint
import com.example.model.ScanSegment
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.ScanVisualSystem
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RealCameraArScannerView(
  floor: Floor,
  userPose: UserPose,
  aiRecommendation: AiRecommendation?,
  isDemoMode: Boolean,
  scanEngine: ArCoreScanEngine? = null,
  onHeadingRotated: (Float) -> Unit,
  onToggleDemoMode: () -> Unit = {},
  onScanSegmentCreated: (ScanSegment) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // Collect ARCore States if engine is present
  val hardwareStatus = scanEngine?.hardwareStatus?.collectAsState()?.value
  val sessionStats = scanEngine?.sessionStats?.collectAsState()?.value
  val accumulatedPoints = scanEngine?.accumulatedPoints?.collectAsState()?.value ?: emptyList()
  val framePoints = scanEngine?.currentFramePoints?.collectAsState()?.value ?: emptyList()
  val detectedPlanes = scanEngine?.detectedPlanes?.collectAsState()?.value ?: emptyList()
  val cameraPath = scanEngine?.cameraPath?.collectAsState()?.value ?: emptyList()

  val isScanning = sessionStats?.isScanning ?: false
  val trackingStatus = sessionStats?.trackingStatus ?: if (isDemoMode) ArCoreTrackingStatus.TRACKING else ArCoreTrackingStatus.PAUSED

  var showDebugPanel by remember { mutableStateOf(false) }
  var cameraBindingError by remember { mutableStateOf(false) }

  // Scanning laser animation
  val infiniteTransition = rememberInfiniteTransition(label = "ar_scanner")
  val laserSweep by infiniteTransition.animateFloat(
    initialValue = 0.1f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_sweep"
  )

  val pulseRing by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_ring"
  )

  // Target bearing for AI navigation arrow
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
  val headingDiff = ((targetBearing - userPose.yawDegrees + 540f) % 360f) - 180f

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceDarkBg)
      .clip(RoundedCornerShape(14.dp))
      .border(1.2.dp, if (isScanning) CyanNeon.copy(alpha = 0.8f) else SpaceCardBorder, RoundedCornerShape(14.dp))
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          onHeadingRotated(dragAmount.x * 0.25f)
        }
      }
  ) {
    // 1. Camera Feed Layer (GLSurfaceView for ARCore or CameraX Fallback or Photorealistic Demo)
    if (isDemoMode) {
      DemoCorridorCameraView(
        yawDegrees = userPose.yawDegrees,
        modifier = Modifier.fillMaxSize()
      )
    } else if (scanEngine != null && hardwareStatus?.arCoreAvailable == true) {
      ArGlSurfaceView(
        scanEngine = scanEngine,
        modifier = Modifier.fillMaxSize()
      )
    } else if (!cameraBindingError) {
      // CameraX fallback for non-ARCore devices
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

    // 2. Real-time AR Point Cloud & Detected Planes Projection Layer
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val camPose = sessionStats?.currentPose ?: CameraPoseData(
        x = userPose.x,
        y = userPose.z,
        z = -userPose.y,
        yawDegrees = userPose.yawDegrees
      )
      val camYawRad = Math.toRadians(camPose.yawDegrees.toDouble()).toFloat()

      // A. Real Detected Planes Rendering (Green for floors, Blue for walls)
      if (!isDemoMode && detectedPlanes.isNotEmpty()) {
        for (plane in detectedPlanes) {
          if (plane.polygonPoints.size >= 3) {
            val projectedPoly = mutableListOf<Offset>()
            for (pt in plane.polygonPoints) {
              val dx = pt.x - camPose.x
              val dy = pt.y - camPose.y
              val dz = pt.z - camPose.z

              // Rotate by camera heading
              val xCam = dx * cos(-camYawRad) - dz * sin(-camYawRad)
              val zCam = dx * sin(-camYawRad) + dz * cos(-camYawRad)
              val yCam = dy

              if (zCam > 0.2f) {
                val focal = (w * 0.75f) / zCam
                val sx = w * 0.5f + xCam * focal
                val sy = h * 0.5f - yCam * focal
                projectedPoly.add(Offset(sx, sy))
              }
            }

            if (projectedPoly.size >= 3) {
              val polyPath = Path().apply {
                moveTo(projectedPoly[0].x, projectedPoly[0].y)
                for (i in 1 until projectedPoly.size) {
                  lineTo(projectedPoly[i].x, projectedPoly[i].y)
                }
                close()
              }

              val planeColor = when (plane.classification) {
                PlaneClassification.FLOOR -> ScanCompletedGreen
                PlaneClassification.WALL -> ElectricBlue
                PlaneClassification.CEILING -> CyanNeon
                else -> Color(0xFF64748B)
              }

              // Draw translucent plane mesh
              drawPath(
                path = polyPath,
                color = planeColor.copy(alpha = if (isScanning) 0.28f else 0.12f)
              )
              // Draw crisp plane boundary outline
              drawPath(
                path = polyPath,
                color = planeColor.copy(alpha = 0.85f),
                style = Stroke(width = 2.0f)
              )
            }
          }
        }
      }

      // B. Real Accumulated 3D Point Cloud Rendering (Perspective Projected)
      if (!isDemoMode && accumulatedPoints.isNotEmpty()) {
        val sampleStep = (accumulatedPoints.size / 1200).coerceAtLeast(1)
        for (i in accumulatedPoints.indices step sampleStep) {
          val pt = accumulatedPoints[i]
          val dx = pt.x - camPose.x
          val dy = pt.y - camPose.y
          val dz = pt.z - camPose.z

          val xCam = dx * cos(-camYawRad) - dz * sin(-camYawRad)
          val zCam = dx * sin(-camYawRad) + dz * cos(-camYawRad)
          val yCam = dy

          if (zCam > 0.15f) {
            val focal = (w * 0.75f) / zCam
            val sx = w * 0.5f + xCam * focal
            val sy = h * 0.5f - yCam * focal

            if (sx in 0f..w && sy in 0f..h) {
              val ptSize = (3.5f / zCam).coerceIn(1.5f, 7.0f)
              drawCircle(
                color = ScanCompletedGreen.copy(alpha = (pt.confidence * 0.85f).coerceIn(0.3f, 0.95f)),
                radius = ptSize,
                center = Offset(sx, sy)
              )
            }
          }
        }
      }

      // C. Active Frame Point Cloud (Electric Blue / Cyan Highlights)
      if (!isDemoMode && framePoints.isNotEmpty() && isScanning) {
        for (pt in framePoints) {
          val dx = pt.x - camPose.x
          val dy = pt.y - camPose.y
          val dz = pt.z - camPose.z

          val xCam = dx * cos(-camYawRad) - dz * sin(-camYawRad)
          val zCam = dx * sin(-camYawRad) + dz * cos(-camYawRad)
          val yCam = dy

          if (zCam > 0.15f) {
            val focal = (w * 0.75f) / zCam
            val sx = w * 0.5f + xCam * focal
            val sy = h * 0.5f - yCam * focal

            if (sx in 0f..w && sy in 0f..h) {
              drawCircle(
                color = CyanNeon.copy(alpha = 0.9f),
                radius = 3.5f,
                center = Offset(sx, sy)
              )
            }
          }
        }
      }

      // D. Laser Sweep Scan Line (Visible during active scanning)
      if (isScanning) {
        val sweepY = h * laserSweep
        drawLine(
          brush = Brush.horizontalGradient(
            colors = listOf(
              Color.Transparent,
              CyanNeon.copy(alpha = 0.85f),
              ElectricBlue,
              CyanNeon.copy(alpha = 0.85f),
              Color.Transparent
            )
          ),
          start = Offset(0f, sweepY),
          end = Offset(w, sweepY),
          strokeWidth = 2.5f
        )
        // Subtle scan glow bar
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              CyanNeon.copy(alpha = 0.15f),
              Color.Transparent
            ),
            startY = sweepY - 24f,
            endY = sweepY + 24f
          ),
          topLeft = Offset(0f, sweepY - 24f),
          size = Size(w, 48f)
        )
      }

      // E. Central AR Reticle & Crosshair
      val cx = w * 0.5f
      val cy = h * 0.5f
      val reticleColor = if (isScanning) CyanNeon else Color.White.copy(alpha = 0.6f)

      drawCircle(
        color = reticleColor.copy(alpha = 0.4f),
        radius = 24.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
      )
      drawCircle(
        color = reticleColor,
        radius = 3.dp.toPx(),
        center = Offset(cx, cy)
      )
      // Small cross marks
      drawLine(reticleColor, Offset(cx - 14.dp.toPx(), cy), Offset(cx - 6.dp.toPx(), cy), 1.5f)
      drawLine(reticleColor, Offset(cx + 6.dp.toPx(), cy), Offset(cx + 14.dp.toPx(), cy), 1.5f)
      drawLine(reticleColor, Offset(cx, cy - 14.dp.toPx()), Offset(cx, cy - 6.dp.toPx()), 1.5f)
      drawLine(reticleColor, Offset(cx, cy + 6.dp.toPx()), Offset(cx, cy + 14.dp.toPx()), 1.5f)

      // F. AI Direction Guidance Badge in AR View
      if (aiRecommendation != null && kotlin.math.abs(headingDiff) > 20f) {
        val arrowDir = if (headingDiff > 0) 1 else -1
        val arrowX = if (arrowDir > 0) w - 36.dp.toPx() else 36.dp.toPx()
        val arrowColor = Color(0xFFC084FC)

        drawCircle(
          color = arrowColor.copy(alpha = 0.25f),
          radius = 20.dp.toPx() * pulseRing,
          center = Offset(arrowX, cy)
        )
      }
    }

    // 3. Top HUD: Real Tracking Status & Sensors State
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Real ARCore Tracking Status Badge
      Surface(
        color = Color(0xCC0B1120),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
          0.8.dp,
          when (trackingStatus) {
            ArCoreTrackingStatus.TRACKING -> ScanCompletedGreen.copy(alpha = 0.8f)
            ArCoreTrackingStatus.PAUSED -> ScanInProgressAmber.copy(alpha = 0.8f)
            else -> ScanRescanRed.copy(alpha = 0.8f)
          }
        )
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(
                color = when (trackingStatus) {
                  ArCoreTrackingStatus.TRACKING -> ScanCompletedGreen
                  ArCoreTrackingStatus.PAUSED -> ScanInProgressAmber
                  else -> ScanRescanRed
                },
                shape = CircleShape
              )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isDemoMode) "데모 모드 (DEMO MODE)" else trackingStatus.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
          if (!isDemoMode) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = trackingStatus.dots,
              color = when (trackingStatus) {
                ArCoreTrackingStatus.TRACKING -> ScanCompletedGreen
                ArCoreTrackingStatus.PAUSED -> ScanInProgressAmber
                else -> ScanRescanRed
              },
              fontSize = 9.sp
            )
          }
        }
      }

      // Right: Controls (Debug toggle & Demo/Real mode switch)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Debug Panel Toggle
        IconButton(
          onClick = { showDebugPanel = !showDebugPanel },
          modifier = Modifier
            .size(30.dp)
            .background(Color(0xCC0B1120), CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = "디버그 패널",
            tint = if (showDebugPanel) CyanNeon else Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
          )
        }

        // Demo / Real Toggle Pill
        Surface(
          color = if (isDemoMode) Color(0xFF7C3AED).copy(alpha = 0.35f) else Color(0xCC0B1120),
          shape = RoundedCornerShape(20.dp),
          border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (isDemoMode) Color(0xFFC084FC) else SpaceCardBorder
          ),
          modifier = Modifier.clickable { onToggleDemoMode() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isDemoMode) "DEMO" else "REAL SCAN",
              color = if (isDemoMode) Color(0xFFE9D5FF) else CyanNeon,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // 4. Collapsible Real-Time Debug Info Panel
    AnimatedVisibility(
      visible = showDebugPanel,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(top = 44.dp, start = 10.dp)
    ) {
      Surface(
        color = Color(0xEE0B1120),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, CyanNeon.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Text(
            text = "ARCore Spatial Telemetry",
            color = CyanNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "FPS: ${sessionStats?.fps ?: 30} | Depth: ${if (hardwareStatus?.depthSupported == true) "ON" else "OFF"}",
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "Points: ${accumulatedPoints.size} (Frame: ${framePoints.size})",
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "Planes: ${detectedPlanes.size} | Segments: ${sessionStats?.segmentsCount ?: 0}",
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
          val pose = sessionStats?.currentPose
          Text(
            text = "Pose: X=${String.format("%.2f", pose?.x ?: 0f)} Y=${String.format("%.2f", pose?.y ?: 0f)} Z=${String.format("%.2f", pose?.z ?: 0f)}",
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "Heading: ${String.format("%.1f", userPose.yawDegrees)}° | Progress: ${sessionStats?.scanProgress ?: 0}%",
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // 5. Bottom Live Scan Control Action Bar
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xEE0B1120))
          )
        )
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Real Metrics (Point count + Plane count)
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = null,
              tint = if (isScanning) CyanNeon else Color(0xFF94A3B8),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isDemoMode) "포인트: 1,420개" else "포인트: ${accumulatedPoints.size}개",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (isDemoMode) "감지 평면: 8개 | 진행률: ${floor.coveragePercent}%"
            else "감지 평면: ${detectedPlanes.size}개 | 진행률: ${sessionStats?.scanProgress ?: 0}%",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
          )
        }

        // Right: Primary Scan Start / Stop Button
        if (scanEngine != null && !isDemoMode) {
          Button(
            onClick = {
              if (isScanning) {
                val createdSegment = scanEngine.stopScan()
                createdSegment?.let { onScanSegmentCreated(it) }
              } else {
                scanEngine.startScan(projectId = floor.id, floorId = floor.id)
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isScanning) ScanRescanRed else CyanNeon
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(36.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isScanning) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isScanning) {
                  val sec = sessionStats?.scanDurationSec ?: 0L
                  val mm = sec / 60
                  val ss = sec % 60
                  String.format("스캔 중 %02d:%02d", mm, ss)
                } else {
                  "스캔 시작"
                },
                color = if (isScanning) Color.White else Color(0xFF0F172A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Photorealistic School Corridor Demo View for Demo Mode ONLY
 */
@Composable
private fun DemoCorridorCameraView(
  yawDegrees: Float,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val vpX = w * 0.5f + (yawDegrees - 90f) * 2.2f
    val vpY = h * 0.48f

    // Ceiling gradient
    val ceilingPath = Path().apply {
      moveTo(0f, 0f)
      lineTo(w, 0f)
      lineTo(vpX + w * 0.18f, vpY * 0.42f)
      lineTo(vpX - w * 0.18f, vpY * 0.42f)
      close()
    }
    drawPath(
      path = ceilingPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        startY = 0f,
        endY = vpY * 0.42f
      )
    )

    // Floor gradient
    val floorPath = Path().apply {
      moveTo(0f, h)
      lineTo(w, h)
      lineTo(vpX + w * 0.20f, vpY + (h - vpY) * 0.45f)
      lineTo(vpX - w * 0.20f, vpY + (h - vpY) * 0.45f)
      close()
    }
    drawPath(
      path = floorPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
        startY = vpY,
        endY = h
      )
    )

    // Left Wall
    val leftWall = Path().apply {
      moveTo(0f, 0f)
      lineTo(vpX - w * 0.18f, vpY * 0.42f)
      lineTo(vpX - w * 0.20f, vpY + (h - vpY) * 0.45f)
      lineTo(0f, h)
      close()
    }
    drawPath(
      path = leftWall,
      brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        startX = 0f,
        endX = vpX
      )
    )

    // Right Wall
    val rightWall = Path().apply {
      moveTo(w, 0f)
      lineTo(vpX + w * 0.18f, vpY * 0.42f)
      lineTo(vpX + w * 0.20f, vpY + (h - vpY) * 0.45f)
      lineTo(w, h)
      close()
    }
    drawPath(
      path = rightWall,
      brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
        startX = vpX,
        endX = w
      )
    )
  }
}

@Composable
private fun RealScannerViewfinderBackground(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.background(
      Brush.verticalGradient(
        colors = listOf(Color(0xFF0B1120), Color(0xFF020617))
      )
    ),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.Videocam,
        contentDescription = null,
        tint = Color(0xFF475569),
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "카메라 뷰파인더 대기 중",
        color = Color(0xFF64748B),
        fontSize = 12.sp
      )
    }
  }
}
