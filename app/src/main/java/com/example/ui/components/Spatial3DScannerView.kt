package com.example.ui.components

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Floor
import com.example.model.PlaneClassification
import com.example.model.Point3D
import com.example.model.Room
import com.example.model.ScanImage
import com.example.model.ScanMesh
import com.example.model.ScanPlane
import com.example.model.ScanPoint
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
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

enum class View3DMode(val label: String) {
  ORBIT("자유 궤도"),
  FIRST_PERSON("1인칭 시점"),
  TOP_DOWN("조감도 (TOP)")
}

@Composable
fun Spatial3DScannerView(
  floor: Floor,
  userPose: UserPose,
  selectedRoom: Room?,
  onRoomClicked: (Room) -> Unit,
  realPoints: List<ScanPoint> = emptyList(),
  realPlanes: List<ScanPlane> = emptyList(),
  realMeshes: List<ScanMesh> = emptyList(),
  capturedImages: List<ScanImage> = emptyList(),
  selectedImageId: String? = null,
  onImageClicked: (ScanImage) -> Unit = {},
  isDemoMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  // 3D camera rotation, pitch, and zoom
  var yawAngle by remember { mutableFloatStateOf(45f) }
  var pitchAngle by remember { mutableFloatStateOf(32f) }
  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  var viewMode by remember { mutableStateOf(View3DMode.ORBIT) }

  // If a room is selected, focus 3D view towards it
  LaunchedEffect(selectedRoom) {
    if (selectedRoom != null) {
      yawAngle = 55f
      pitchAngle = 35f
      zoomScale = 1.25f
    }
  }

  // Update angles when 1st person mode
  LaunchedEffect(viewMode, userPose) {
    if (viewMode == View3DMode.FIRST_PERSON) {
      yawAngle = userPose.yawDegrees
      pitchAngle = 10f
    } else if (viewMode == View3DMode.TOP_DOWN) {
      pitchAngle = 85f
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

  val hasScannedData = isDemoMode || floor.rooms.isNotEmpty() || floor.coveragePercent > 0 || realPoints.isNotEmpty() || realPlanes.isNotEmpty() || realMeshes.isNotEmpty()

  // Generate 3D point cloud from real ARCore scan or demo data
  val pointCloud = remember(floor.id, hasScannedData, realPoints, isDemoMode) {
    if (realPoints.isNotEmpty()) {
      realPoints.map { pt ->
        CloudPoint(
          x = pt.x,
          y = pt.y,
          z = pt.z,
          color = ScanCompletedGreen.copy(alpha = (pt.confidence * 0.85f).coerceIn(0.4f, 0.95f))
        )
      }
    } else if (isDemoMode && hasScannedData) {
      val list = mutableListOf<CloudPoint>()
      val rng = Random(floor.id.hashCode())

      for (i in 0..450) {
        val x = rng.nextFloat() * 36f - 18f
        val y = rng.nextFloat() * 3.6f - 1.8f
        val z = rng.nextFloat() * 26f - 13f
        val c = when (rng.nextInt(4)) {
          0 -> CyanNeon.copy(alpha = 0.85f)
          1 -> ElectricBlue.copy(alpha = 0.75f)
          2 -> ScanCompletedGreen.copy(alpha = 0.85f)
          else -> Color(0xFF90E0EF)
        }
        list.add(CloudPoint(x, y, z, c))
      }
      list
    } else {
      emptyList()
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
          if (viewMode != View3DMode.TOP_DOWN) {
            yawAngle = (yawAngle + dragAmount.x * 0.35f) % 360f
            pitchAngle = (pitchAngle - dragAmount.y * 0.25f).coerceIn(5f, 88f)
          } else {
            panOffset += dragAmount
          }
        }
      }
  ) {
    // 3D Spatial Canvas (Point Cloud + Reconstructed Meshes + Room Wireframes + LiDAR Beam + User Pose + 📷 Image Pins)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f + panOffset.x
      val cy = size.height / 2f + panOffset.y
      val yawRad = Math.toRadians(yawAngle.toDouble()).toFloat()
      val pitchRad = Math.toRadians(pitchAngle.toDouble()).toFloat()

      fun project3Dto2D(x: Float, y: Float, z: Float): Offset {
        // Rotate around Y-axis (yaw)
        val x1 = x * cos(yawRad) - z * sin(yawRad)
        val z1 = x * sin(yawRad) + z * cos(yawRad)

        // Rotate around X-axis (pitch)
        val y2 = y * cos(pitchRad) - z1 * sin(pitchRad)
        val z2 = y * sin(pitchRad) + z1 * cos(pitchRad)

        val depthScale = 16f * zoomScale
        val screenX = cx + x1 * depthScale
        val screenY = cy - y2 * depthScale
        return Offset(screenX, screenY)
      }

      // Draw Floor Ground Plane Grid Mesh
      val gridStep = 4f
      for (gx in -24..24 step 4) {
        val p1 = project3Dto2D(gx.toFloat(), -1.8f, -18f)
        val p2 = project3Dto2D(gx.toFloat(), -1.8f, 18f)
        drawLine(
          color = Color(0xFF1B2A4A).copy(alpha = 0.45f),
          start = p1,
          end = p2,
          strokeWidth = 1f
        )
      }
      for (gz in -18..18 step 4) {
        val p1 = project3Dto2D(-24f, -1.8f, gz.toFloat())
        val p2 = project3Dto2D(24f, -1.8f, gz.toFloat())
        drawLine(
          color = Color(0xFF1B2A4A).copy(alpha = 0.45f),
          start = p1,
          end = p2,
          strokeWidth = 1f
        )
      }

      // 1. Draw Real ARCore 3D Surface Meshes (Reconstructed Triangles)
      if (realMeshes.isNotEmpty()) {
        for (mesh in realMeshes) {
          val meshColor = when (mesh.classification) {
            PlaneClassification.FLOOR -> ScanCompletedGreen
            PlaneClassification.WALL -> ElectricBlue
            PlaneClassification.CEILING -> CyanNeon
            else -> Color(0xFF94A3B8)
          }

          val vertices = mesh.vertices
          val triangles = mesh.triangles
          val projectedVerts = vertices.map { project3Dto2D(it.x, it.y, it.z) }

          // Draw Mesh Triangles
          for (tri in triangles) {
            val idx1 = tri.v1
            val idx2 = tri.v2
            val idx3 = tri.v3

            if (idx1 in projectedVerts.indices && idx2 in projectedVerts.indices && idx3 in projectedVerts.indices) {
              val p1 = projectedVerts[idx1]
              val p2 = projectedVerts[idx2]
              val p3 = projectedVerts[idx3]

              val triPath = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
              }

              drawPath(path = triPath, color = meshColor.copy(alpha = 0.22f))
              drawPath(path = triPath, color = meshColor.copy(alpha = 0.65f), style = Stroke(width = 1.0f))
            }
          }
        }
      }

      // 2. Draw Room 3D Bounding Boxes & Architectural Volumes
      floor.rooms.forEach { room ->
        val isTarget = room.id == selectedRoom?.id
        val rx = (room.x - 28f) * 0.7f
        val rz = (room.y - 19f) * 0.7f
        val rw = (room.width / 2f) * 0.7f
        val rh = (room.height / 2f) * 0.7f
        val wallHeight = 2.4f

        val c1 = project3Dto2D(rx - rw, -1.8f, rz - rh)
        val c2 = project3Dto2D(rx + rw, -1.8f, rz - rh)
        val c3 = project3Dto2D(rx + rw, -1.8f, rz + rh)
        val c4 = project3Dto2D(rx - rw, -1.8f, rz + rh)

        val t1 = project3Dto2D(rx - rw, wallHeight, rz - rh)
        val t2 = project3Dto2D(rx + rw, wallHeight, rz - rh)
        val t3 = project3Dto2D(rx + rw, wallHeight, rz + rh)
        val t4 = project3Dto2D(rx - rw, wallHeight, rz + rh)

        val boxColor = when {
          isTarget -> CyanNeon
          room.status == com.example.model.ScanStatus.RESCAN_NEEDED -> ScanRescanRed
          room.status == com.example.model.ScanStatus.COMPLETED -> ScanCompletedGreen.copy(alpha = 0.75f)
          else -> ElectricBlue.copy(alpha = 0.45f)
        }

        // Base & Top & Pillars
        drawLine(boxColor, c1, c2, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, c2, c3, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, c3, c4, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, c4, c1, strokeWidth = if (isTarget) 3f else 1.5f)

        drawLine(boxColor, t1, t2, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, t2, t3, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, t3, t4, strokeWidth = if (isTarget) 3f else 1.5f)
        drawLine(boxColor, t4, t1, strokeWidth = if (isTarget) 3f else 1.5f)

        drawLine(boxColor, c1, t1, strokeWidth = 1f)
        drawLine(boxColor, c2, t2, strokeWidth = 1f)
        drawLine(boxColor, c3, t3, strokeWidth = 1f)
        drawLine(boxColor, c4, t4, strokeWidth = 1f)

        // Room label and Dimensions in 3D
        val labelPos = project3Dto2D(rx, wallHeight + 0.4f, rz)
        drawContext.canvas.nativeCanvas.drawText(
          "${room.name} (${room.width.toInt()}x${room.height.toInt()}m)",
          labelPos.x,
          labelPos.y,
          android.graphics.Paint().apply {
            color = if (isTarget) android.graphics.Color.CYAN else android.graphics.Color.WHITE
            textSize = (24f * zoomScale).coerceIn(18f, 32f)
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
          }
        )
      }

      // 3. Draw Point Cloud Particles
      pointCloud.forEach { pt ->
        val screenPt = project3Dto2D(pt.x, pt.y, pt.z)
        drawCircle(
          color = pt.color,
          radius = (2.4f * zoomScale).coerceIn(1.2f, 4.5f),
          center = screenPt
        )
      }

      // 4. Draw Captured Keyframe Images (📷 3D Pins & Direction)
      capturedImages.forEach { img ->
        val imgPos = project3Dto2D(img.worldX, img.worldY, img.worldZ)
        val isSelected = img.id == selectedImageId

        // Pin billboard
        drawCircle(
          color = if (isSelected) CyanNeon else Color(0xFF0284C7),
          radius = if (isSelected) 10f * zoomScale.coerceIn(0.8f, 1.8f) else 7f * zoomScale.coerceIn(0.8f, 1.5f),
          center = imgPos
        )
        drawCircle(
          color = Color.White,
          radius = 3f * zoomScale.coerceIn(0.8f, 1.5f),
          center = imgPos
        )

        // Draw camera look vector line
        val yawImgRad = Math.toRadians(img.cameraRotationYaw.toDouble()).toFloat()
        val lookX = img.worldX + 1.2f * sin(yawImgRad)
        val lookZ = img.worldZ - 1.2f * cos(yawImgRad)
        val lookPos = project3Dto2D(lookX, img.worldY, lookZ)

        drawLine(
          color = if (isSelected) CyanNeon else Color(0xFF38BDF8),
          start = imgPos,
          end = lookPos,
          strokeWidth = 2f
        )
      }

      // 5. Draw 3D User Pose Marker
      val userX3D = (userPose.x - 28f) * 0.7f
      val userZ3D = (userPose.y - 19f) * 0.7f
      val userScreen = project3Dto2D(userX3D, -1.8f, userZ3D)
      val userScreenHead = project3Dto2D(userX3D, 0.4f, userZ3D)

      drawLine(
        color = CyanNeon,
        start = userScreen,
        end = userScreenHead,
        strokeWidth = 3.5f
      )
      drawCircle(
        color = CyanNeon,
        radius = 8f * zoomScale.coerceIn(0.8f, 1.6f),
        center = userScreenHead
      )
      drawCircle(
        color = Color.White,
        radius = 3.5f * zoomScale.coerceIn(0.8f, 1.6f),
        center = userScreenHead
      )

      // 6. LiDAR Laser Scanline Sweep
      val scanYPos = -1.8f + laserY * 4.2f
      val scanP1 = project3Dto2D(-20f, scanYPos, -14f)
      val scanP2 = project3Dto2D(20f, scanYPos, -14f)
      val scanP3 = project3Dto2D(20f, scanYPos, 14f)
      val scanP4 = project3Dto2D(-20f, scanYPos, 14f)

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
        color = CyanNeon.copy(alpha = 0.75f),
        start = scanP1,
        end = scanP2,
        strokeWidth = 2f,
        cap = StrokeCap.Round
      )
    }

    // Top overlay badge: 3D Scanner Active status & Meshes count
    Row(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(10.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xCC0B132B))
        .border(1.dp, SpaceCardBorder, RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 5.dp),
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
        text = if (realMeshes.isNotEmpty()) "3D 공간 메쉬: ${realMeshes.size}개 | ${pointCloud.size} 포인트"
        else if (hasScannedData) "3D 실시간 공간 매핑 중 (${pointCloud.size} pts)"
        else "3D 스캔 데이터 대기 중",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Camera view mode selector & reset controls (Top Right)
    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Orbit / 1st-Person / Top View Mode Toggle
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xCC0B1120))
          .border(0.8.dp, SpaceCardBorder, RoundedCornerShape(16.dp))
          .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        View3DMode.values().forEach { mode ->
          val isSelected = viewMode == mode
          Surface(
            color = if (isSelected) CyanNeon else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
          ) {
            Text(
              text = when (mode) {
                View3DMode.ORBIT -> "궤도"
                View3DMode.FIRST_PERSON -> "1인칭"
                View3DMode.TOP_DOWN -> "조감도"
              },
              color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Reset View Button
      IconButton(
        onClick = {
          yawAngle = 45f
          pitchAngle = 32f
          zoomScale = 1.0f
          panOffset = Offset.Zero
          viewMode = View3DMode.ORBIT
        },
        modifier = Modifier
          .align(Alignment.End)
          .size(30.dp)
          .background(Color(0xCC0B1120), CircleShape)
          .border(0.8.dp, SpaceCardBorder, CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.CenterFocusStrong,
          contentDescription = "3D 뷰 리셋",
          tint = CyanNeon,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
