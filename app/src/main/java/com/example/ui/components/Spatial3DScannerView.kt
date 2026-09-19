package com.example.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.model.Floor
import com.example.model.Point3D
import com.example.model.Room
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class CloudPoint(
  val x: Float,
  val y: Float,
  val z: Float,
  val color: Color
)

@Composable
fun Spatial3DScannerView(
  floor: Floor,
  userPose: UserPose,
  selectedRoom: Room?,
  onRoomClicked: (Room) -> Unit,
  isCameraFeedEnabled: Boolean = true,
  onToggleCameraFeed: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // 3D camera rotation and pitch angles
  var yawAngle by remember { mutableFloatStateOf(45f) }
  var pitchAngle by remember { mutableFloatStateOf(28f) }
  var zoomScale by remember { mutableFloatStateOf(1f) }

  // If a room is selected, focus 3D view towards it
  LaunchedEffect(selectedRoom) {
    if (selectedRoom != null) {
      yawAngle = 60f
      pitchAngle = 35f
    }
  }

  // Scanning laser beam animation
  val infiniteTransition = rememberInfiniteTransition(label = "lidar")
  val laserY by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laserY"
  )

  val hasScannedData = floor.rooms.isNotEmpty() || floor.coveragePercent > 0

  // Generate realistic architectural 3D point cloud (only when scanned or demo data exists)
  val pointCloud = remember(floor.id, hasScannedData) {
    if (!hasScannedData) {
      emptyList<CloudPoint>()
    } else {
      val list = mutableListOf<CloudPoint>()
      val rng = Random(floor.id.hashCode())

      // Corridor and Room wall & floor points
      for (i in 0..300) {
        val x = rng.nextFloat() * 40f - 20f
        val y = rng.nextFloat() * 4f - 2f // height
        val z = rng.nextFloat() * 30f - 15f
        val c = when (rng.nextInt(4)) {
          0 -> CyanNeon.copy(alpha = 0.8f)
          1 -> ElectricBlue.copy(alpha = 0.7f)
          2 -> ScanCompletedGreen.copy(alpha = 0.8f)
          else -> Color(0xFF90E0EF)
        }
        list.add(CloudPoint(x, y, z, c))
      }

      // Add Red alert warning point cloud for low quality area
      for (i in 0..60) {
        val x = rng.nextFloat() * 8f - 14f
        val y = rng.nextFloat() * 3f
        val z = rng.nextFloat() * 2f + 8f
        list.add(CloudPoint(x, y, z, ScanRescanRed))
      }
      list
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceDarkBg)
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          yawAngle = (yawAngle + dragAmount.x * 0.35f) % 360f
          pitchAngle = (pitchAngle - dragAmount.y * 0.25f).coerceIn(10f, 80f)
        }
      }
  ) {
    // 1. CameraX Feed (when available and enabled) or High-Tech Spatial Grid
    if (isCameraFeedEnabled) {
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
              } catch (_: Exception) {
                // In emulators without camera hardware, fallback handled seamlessly
              }
            }, ContextCompat.getMainExecutor(ctx))
          } catch (_: Exception) {}
          previewView
        },
        modifier = Modifier.fillMaxSize()
      )

      // Dark sci-fi tinted overlay for readable point cloud
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xB3060C18))
      )
    }

    // 2. 3D Spatial Canvas (Point Cloud + Room Wireframes + LiDAR Beam + User Pose)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val yawRad = Math.toRadians(yawAngle.toDouble()).toFloat()
      val pitchRad = Math.toRadians(pitchAngle.toDouble()).toFloat()

      fun project3Dto2D(x: Float, y: Float, z: Float): Offset {
        // Rotate around Y-axis (yaw)
        val x1 = x * cos(yawRad) - z * sin(yawRad)
        val z1 = x * sin(yawRad) + z * cos(yawRad)

        // Rotate around X-axis (pitch)
        val y2 = y * cos(pitchRad) - z1 * sin(pitchRad)
        val z2 = y * sin(pitchRad) + z1 * cos(pitchRad)

        val depthScale = 14f * zoomScale
        val screenX = cx + x1 * depthScale
        val screenY = cy - y2 * depthScale
        return Offset(screenX, screenY)
      }

      // Draw Floor Ground Plane Mesh
      val gridStep = 4f
      for (gx in -20..20 step 4) {
        val p1 = project3Dto2D(gx.toFloat(), -2f, -16f)
        val p2 = project3Dto2D(gx.toFloat(), -2f, 16f)
        drawLine(
          color = Color(0xFF1B2A4A).copy(alpha = 0.5f),
          start = p1,
          end = p2,
          strokeWidth = 1f
        )
      }
      for (gz in -16..16 step 4) {
        val p1 = project3Dto2D(-20f, -2f, gz.toFloat())
        val p2 = project3Dto2D(20f, -2f, gz.toFloat())
        drawLine(
          color = Color(0xFF1B2A4A).copy(alpha = 0.5f),
          start = p1,
          end = p2,
          strokeWidth = 1f
        )
      }

      // Draw Room 3D Bounding Boxes
      floor.rooms.forEach { room ->
        val isTarget = room.id == selectedRoom?.id
        val rx = (room.x - 28f) * 0.7f
        val rz = (room.y - 19f) * 0.7f
        val rw = (room.width / 2f) * 0.7f
        val rh = (room.height / 2f) * 0.7f
        val wallHeight = 2.4f

        val c1 = project3Dto2D(rx - rw, -2f, rz - rh)
        val c2 = project3Dto2D(rx + rw, -2f, rz - rh)
        val c3 = project3Dto2D(rx + rw, -2f, rz + rh)
        val c4 = project3Dto2D(rx - rw, -2f, rz + rh)

        val t1 = project3Dto2D(rx - rw, wallHeight, rz - rh)
        val t2 = project3Dto2D(rx + rw, wallHeight, rz - rh)
        val t3 = project3Dto2D(rx + rw, wallHeight, rz + rh)
        val t4 = project3Dto2D(rx - rw, wallHeight, rz + rh)

        val boxColor = when {
          isTarget -> CyanNeon
          room.status == com.example.model.ScanStatus.RESCAN_NEEDED -> ScanRescanRed
          room.status == com.example.model.ScanStatus.COMPLETED -> ScanCompletedGreen.copy(alpha = 0.6f)
          else -> ElectricBlue.copy(alpha = 0.4f)
        }

        // Base rectangle
        drawLine(boxColor, c1, c2, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, c2, c3, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, c3, c4, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, c4, c1, strokeWidth = if (isTarget) 3f else 1.5f)

        // Top rectangle
        drawLine(boxColor, t1, t2, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, t2, t3, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, t3, t4, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, t4, t1, strokeWidth = if (isTarget) 3f else 1.5f)

        // Pillars
        drawLine(boxColor, c1, t1, strokeWidth = 1f)
        drawLine(boxColor, c2, t2, strokeWidth = 1f)
        drawLine(boxColor, c3, t3, strokeWidth = 1f)
        drawLine(boxColor, c4, t4, strokeWidth = 1f)

        // Room label in 3D
        val labelPos = project3Dto2D(rx, wallHeight + 0.5f, rz)
        drawContext.canvas.nativeCanvas.drawText(
          room.name.take(6),
          labelPos.x,
          labelPos.y,
          android.graphics.Paint().apply {
            color = if (isTarget) android.graphics.Color.CYAN else android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
          }
        )
      }

      // Draw Point Cloud Particles
      pointCloud.forEach { pt ->
        val screenPt = project3Dto2D(pt.x, pt.y, pt.z)
        drawCircle(
          color = pt.color,
          radius = if (pt.color == ScanRescanRed) 3.5f else 2.2f,
          center = screenPt
        )
      }

      // Draw 3D User Pose Marker
      val userX3D = (userPose.x - 28f) * 0.7f
      val userZ3D = (userPose.y - 19f) * 0.7f
      val userScreen = project3Dto2D(userX3D, -1.8f, userZ3D)
      val userScreenHead = project3Dto2D(userX3D, 0.5f, userZ3D)

      // User column line
      drawLine(
        color = CyanNeon,
        start = userScreen,
        end = userScreenHead,
        strokeWidth = 3f
      )
      drawCircle(
        color = CyanNeon,
        radius = 8f,
        center = userScreenHead
      )
      drawCircle(
        color = Color.White,
        radius = 4f,
        center = userScreenHead
      )

      // LiDAR Laser Scanline Sweep
      val scanYPos = -2f + laserY * 4.5f
      val scanP1 = project3Dto2D(-18f, scanYPos, -12f)
      val scanP2 = project3Dto2D(18f, scanYPos, -12f)
      val scanP3 = project3Dto2D(18f, scanYPos, 12f)
      val scanP4 = project3Dto2D(-18f, scanYPos, 12f)

      val laserPath = Path().apply {
        moveTo(scanP1.x, scanP1.y)
        lineTo(scanP2.x, scanP2.y)
        lineTo(scanP3.x, scanP3.y)
        lineTo(scanP4.x, scanP4.y)
        close()
      }

      drawPath(
        path = laserPath,
        color = CyanNeon.copy(alpha = 0.08f)
      )
      drawLine(
        color = CyanNeon.copy(alpha = 0.7f),
        start = scanP1,
        end = scanP2,
        strokeWidth = 2f,
        cap = StrokeCap.Round
      )
    }

    // Top overlay badge: 3D Scanner Active status
    Row(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(10.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xCC0B132B))
        .border(1.dp, SpaceCardBorder, RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(if (hasScannedData) CyanNeon else Color.Gray)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (hasScannedData) "3D 실시간 공간 매핑 중..." else "3D 스캔 준비 (데이터 대기 중)",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Empty state overlay for fresh project
    if (!hasScannedData) {
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
          Icon(
            imageVector = Icons.Default.ViewInAr,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "아직 3D 스캔 데이터 없음",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "카메라를 움직여 3D 포인트 클라우드를 수집하세요",
            color = CyanNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // Camera feed toggle button & Perspective reset
    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(8.dp)
    ) {
      IconButton(
        onClick = onToggleCameraFeed,
        modifier = Modifier
          .clip(CircleShape)
          .background(Color(0xCC1E293B))
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = "카메라 뷰 토글",
          tint = if (isCameraFeedEnabled) CyanNeon else Color.Gray,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      IconButton(
        onClick = {
          yawAngle = 45f
          pitchAngle = 28f
          zoomScale = 1f
        },
        modifier = Modifier
          .clip(CircleShape)
          .background(Color(0xCC1E293B))
      ) {
        Icon(
          imageVector = Icons.Default.ViewInAr,
          contentDescription = "3D 뷰 리셋",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    // Bottom warning chip if near low-quality unscanned wall
    val warningRoom = floor.rooms.find { it.status == com.example.model.ScanStatus.RESCAN_NEEDED }
    if (warningRoom != null) {
      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xCC7F1D1D))
          .border(1.dp, ScanRescanRed, RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "스캔 품질 경고",
          tint = ScanRescanRed,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "재스캔 필요: ${warningRoom.missingAreas.firstOrNull()?.title ?: warningRoom.name}",
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
