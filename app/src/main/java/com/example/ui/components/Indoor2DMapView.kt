package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiRecommendation
import com.example.model.Floor
import com.example.model.Room
import com.example.model.ScanImage
import com.example.model.ScanPlane
import com.example.model.ScanPoint
import com.example.model.ScanStatus
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.ScanUnscannedSlate
import com.example.ui.theme.ScanVisualSystem
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Indoor2DMapView(
  floor: Floor,
  userPose: UserPose,
  breadcrumbs: List<Pair<Float, Float>>,
  recommendation: AiRecommendation?,
  selectedRoomId: String?,
  onRoomSelected: (Room) -> Unit,
  detectedPlanes: List<ScanPlane> = emptyList(),
  accumulatedPoints: List<ScanPoint> = emptyList(),
  capturedImages: List<ScanImage> = emptyList(),
  selectedImageId: String? = null,
  onImageSelected: (ScanImage) -> Unit = {},
  showFullLegend: Boolean = true,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(1.0f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(0.25f, 8.0f)
    offset += panChange
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseRadius by infiniteTransition.animateFloat(
    initialValue = 8f,
    targetValue = 28f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseRadius"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceDarkBg)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
        .transformable(state = transformableState)
        .pointerInput(floor, capturedImages) {
          detectTapGestures(
            onDoubleTap = {
              scale = 1.0f
              offset = Offset.Zero
            },
            onTap = { tapOffset ->
              val canvasW = size.width
              val scaleFactor = (canvasW / 58f) * scale
              val originX = 4f * scaleFactor + offset.x
              val originY = 6f * scaleFactor + offset.y

              val clickedMetersX = (tapOffset.x - originX) / scaleFactor
              val clickedMetersY = (tapOffset.y - originY) / scaleFactor

              // 1. Check if clicked near any captured scan image 📷 icon (within 2.5m)
              val clickedImage = capturedImages.find { img ->
                val dx = img.mapX - clickedMetersX
                val dy = img.mapY - clickedMetersY
                (dx * dx + dy * dy) < (2.5f * 2.5f)
              }
              if (clickedImage != null) {
                onImageSelected(clickedImage)
                return@detectTapGestures
              }

              // 2. Check if clicked any room
              val clickedRoom = floor.rooms.find { r ->
                val left = r.x - r.width / 2f
                val right = r.x + r.width / 2f
                val top = r.y - r.height / 2f
                val bottom = r.y + r.height / 2f
                clickedMetersX in left..right && clickedMetersY in top..bottom
              }
              if (clickedRoom != null) {
                onRoomSelected(clickedRoom)
              }
            }
          )
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val baseScale = (size.width / 58f) * scale
        val originX = 4f * baseScale + offset.x
        val originY = 6f * baseScale + offset.y

        fun metersToScreen(mx: Float, my: Float): Offset {
          return Offset(originX + mx * baseScale, originY + my * baseScale)
        }

        // Draw architectural background grid
        drawFloorGrid(baseScale, originX, originY, size.width, size.height)

        // 1. Draw Corridors
        floor.corridors.forEach { corridor ->
          val pStart = metersToScreen(corridor.startX, corridor.startY)
          val pEnd = metersToScreen(corridor.endX, corridor.endY)
          val corridorHeight = corridor.width * baseScale

          drawRoundRect(
            color = Color(0xFF131D31),
            topLeft = Offset(pStart.x, pStart.y - corridorHeight / 2f),
            size = Size(pEnd.x - pStart.x, corridorHeight),
            cornerRadius = CornerRadius(6f, 6f)
          )

          drawRoundRect(
            color = Color(0xFF203254),
            topLeft = Offset(pStart.x, pStart.y - corridorHeight / 2f),
            size = Size(pEnd.x - pStart.x, corridorHeight),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2f)
          )
        }

        // 2. Draw Rooms with Scan Coverage Fill
        floor.rooms.forEach { room ->
          val leftMeters = room.x - room.width / 2f
          val topMeters = room.y - room.height / 2f
          val pTopLeft = metersToScreen(leftMeters, topMeters)
          val roomWidthPx = room.width * baseScale
          val roomHeightPx = room.height * baseScale

          val isAiTarget = room.id == recommendation?.nextTargetRoomId
          val statusColor = ScanVisualSystem.getColor(room.status, isAiTarget)
          val fillAlpha = ScanVisualSystem.getMapFillAlpha(room.status, isAiTarget)

          val fillColor = statusColor.copy(alpha = fillAlpha)

          val borderColor = when {
            room.id == selectedRoomId -> CyanNeon
            isAiTarget -> ScanVisualSystem.AiRecommended
            else -> statusColor
          }

          val strokeWidth = if (room.id == selectedRoomId || isAiTarget) 4f else 2f

          // Room background
          drawRoundRect(
            color = fillColor,
            topLeft = pTopLeft,
            size = Size(roomWidthPx, roomHeightPx),
            cornerRadius = CornerRadius(8f, 8f)
          )

          // Room boundary wall
          drawRoundRect(
            color = borderColor,
            topLeft = pTopLeft,
            size = Size(roomWidthPx, roomHeightPx),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = strokeWidth)
          )

          // Draw Doors
          room.doors.forEach { door ->
            val doorPos = metersToScreen(door.x, door.z)
            drawCircle(
              color = Color(0xFF00E5FF),
              radius = 5f * scale.coerceIn(0.8f, 2.5f),
              center = doorPos
            )
          }

          // Room Text label (only if scale >= 0.5f)
          if (scale >= 0.45f) {
            drawContext.canvas.nativeCanvas.apply {
              val paint = android.graphics.Paint().apply {
                color = if (room.id == selectedRoomId) android.graphics.Color.CYAN else android.graphics.Color.WHITE
                textSize = (13f * scale).coerceIn(11f, 26f)
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
              }
              val subPaint = android.graphics.Paint().apply {
                color = when (room.status) {
                  ScanStatus.COMPLETED -> android.graphics.Color.GREEN
                  ScanStatus.RESCAN_NEEDED -> android.graphics.Color.RED
                  else -> android.graphics.Color.LTGRAY
                }
                textSize = (10f * scale).coerceIn(9f, 20f)
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
              }

              val textCenter = metersToScreen(room.x, room.y)
              drawText(room.name.take(7), textCenter.x, textCenter.y - 4f, paint)
              drawText("${room.coveragePercent}%", textCenter.x, textCenter.y + 14f * scale.coerceIn(0.8f, 1.8f), subPaint)
            }
          }
        }

        // 3. Draw Detected Planes from ARCore
        if (detectedPlanes.isNotEmpty()) {
          for (plane in detectedPlanes) {
            val centerScreen = metersToScreen(plane.centerX, -plane.centerZ)
            val pWidth = plane.extentX * baseScale
            val pHeight = plane.extentZ * baseScale

            val isFloor = plane.classification == com.example.model.PlaneClassification.FLOOR
            val planeColor = if (isFloor) ScanCompletedGreen else ElectricBlue

            drawRoundRect(
              color = planeColor.copy(alpha = if (isFloor) 0.25f else 0.40f),
              topLeft = Offset(centerScreen.x - pWidth / 2f, centerScreen.y - pHeight / 2f),
              size = Size(pWidth.coerceAtLeast(10f), pHeight.coerceAtLeast(10f)),
              cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
              color = planeColor,
              topLeft = Offset(centerScreen.x - pWidth / 2f, centerScreen.y - pHeight / 2f),
              size = Size(pWidth.coerceAtLeast(10f), pHeight.coerceAtLeast(10f)),
              cornerRadius = CornerRadius(4f, 4f),
              style = Stroke(width = 1.5f)
            )
          }
        }

        // Draw Real Point Cloud on 2D Map (Green spatial dots)
        if (accumulatedPoints.isNotEmpty()) {
          val step = (accumulatedPoints.size / 400).coerceAtLeast(1)
          for (i in accumulatedPoints.indices step step) {
            val pt = accumulatedPoints[i]
            val ptScreen = metersToScreen(pt.x, -pt.z)
            drawCircle(
              color = ScanCompletedGreen.copy(alpha = 0.65f),
              radius = 2.5f * scale.coerceIn(0.7f, 2.0f),
              center = ptScreen
            )
          }
        }

        // 4. Draw Breadcrumbs (Visited Path)
        if (breadcrumbs.size >= 2) {
          val breadcrumbPath = Path()
          val first = metersToScreen(breadcrumbs.first().first, breadcrumbs.first().second)
          breadcrumbPath.moveTo(first.x, first.y)

          for (i in 1 until breadcrumbs.size) {
            val pt = metersToScreen(breadcrumbs[i].first, breadcrumbs[i].second)
            breadcrumbPath.lineTo(pt.x, pt.y)
          }

          drawPath(
            path = breadcrumbPath,
            color = ElectricBlue.copy(alpha = 0.5f),
            style = Stroke(
              width = 4f * scale.coerceIn(0.8f, 2.0f),
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
          )
        }

        // 5. Draw Keyframe Captured Images 📷 Pins & FOV Cones on Map
        if (capturedImages.isNotEmpty()) {
          for (img in capturedImages) {
            val imgScreen = metersToScreen(img.mapX, img.mapY)
            val isSelected = img.id == selectedImageId
            val yawRad = Math.toRadians(img.cameraRotationYaw.toDouble()).toFloat()

            // Draw Camera FOV Cone
            val fovRad = Math.toRadians(img.fovDegrees.toDouble() / 2.0).toFloat()
            val coneDist = 28f * scale.coerceIn(0.8f, 2.5f)

            val conePath = Path().apply {
              moveTo(imgScreen.x, imgScreen.y)
              val p1x = imgScreen.x + coneDist * sin(yawRad - fovRad)
              val p1y = imgScreen.y - coneDist * cos(yawRad - fovRad)
              val p2x = imgScreen.x + coneDist * sin(yawRad + fovRad)
              val p2y = imgScreen.y - coneDist * cos(yawRad + fovRad)
              lineTo(p1x, p1y)
              lineTo(p2x, p2y)
              close()
            }

            drawPath(
              path = conePath,
              color = if (isSelected) CyanNeon.copy(alpha = 0.45f) else Color(0xFF38BDF8).copy(alpha = 0.20f)
            )

            // Draw Camera Icon Marker Circle
            val pinRadius = if (isSelected) 12f * scale.coerceIn(0.8f, 1.8f) else 9f * scale.coerceIn(0.8f, 1.5f)
            drawCircle(
              color = if (isSelected) CyanNeon else Color(0xFF0284C7),
              radius = pinRadius,
              center = imgScreen
            )
            drawCircle(
              color = Color.White,
              radius = pinRadius * 0.4f,
              center = imgScreen
            )

            // At high zoom levels (>= 2.5x), render crisp thumbnail preview
            if (scale >= 2.5f && img.thumbnailBitmap != null) {
              val thumbW = 48f * (scale / 2.5f).coerceIn(1.0f, 2.0f)
              val thumbH = 32f * (scale / 2.5f).coerceIn(1.0f, 2.0f)
              val thumbLeft = imgScreen.x - thumbW / 2f
              val thumbTop = imgScreen.y - pinRadius - thumbH - 4f

              drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(thumbLeft - 2f, thumbTop - 2f),
                size = Size(thumbW + 4f, thumbH + 4f),
                cornerRadius = CornerRadius(4f, 4f)
              )
              drawRoundRect(
                color = if (isSelected) CyanNeon else Color(0xFF38BDF8),
                topLeft = Offset(thumbLeft - 2f, thumbTop - 2f),
                size = Size(thumbW + 4f, thumbH + 4f),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 1.5f)
              )
            }
          }
        }

        // 6. Draw User Location Marker with Heading Cone
        val userCenter = metersToScreen(userPose.x, userPose.y)

        drawCircle(
          color = CyanNeon.copy(alpha = pulseAlpha),
          radius = pulseRadius * scale.coerceIn(0.7f, 2.0f),
          center = userCenter
        )

        rotate(degrees = userPose.yawDegrees, pivot = userCenter) {
          val conePath = Path().apply {
            moveTo(userCenter.x, userCenter.y)
            lineTo(userCenter.x - 18f * scale.coerceIn(0.7f, 2.0f), userCenter.y - 42f * scale.coerceIn(0.7f, 2.0f))
            lineTo(userCenter.x + 18f * scale.coerceIn(0.7f, 2.0f), userCenter.y - 42f * scale.coerceIn(0.7f, 2.0f))
            close()
          }
          drawPath(
            path = conePath,
            brush = Brush.radialGradient(
              colors = listOf(CyanNeon.copy(alpha = 0.45f), Color.Transparent),
              center = userCenter,
              radius = 45f * scale.coerceIn(0.7f, 2.0f)
            )
          )

          val arrowPath = Path().apply {
            moveTo(userCenter.x, userCenter.y - 18f * scale.coerceIn(0.7f, 2.0f))
            lineTo(userCenter.x - 7f * scale.coerceIn(0.7f, 2.0f), userCenter.y - 6f * scale.coerceIn(0.7f, 2.0f))
            lineTo(userCenter.x + 7f * scale.coerceIn(0.7f, 2.0f), userCenter.y - 6f * scale.coerceIn(0.7f, 2.0f))
            close()
          }
          drawPath(path = arrowPath, color = Color.White)
        }

        drawCircle(
          color = Color(0xFF003566),
          radius = 10f * scale.coerceIn(0.7f, 2.0f),
          center = userCenter
        )
        drawCircle(
          color = CyanNeon,
          radius = 7f * scale.coerceIn(0.7f, 2.0f),
          center = userCenter
        )
        drawCircle(
          color = Color.White,
          radius = 3f * scale.coerceIn(0.7f, 2.0f),
          center = userCenter
        )
      }

      // Zoom & Map Action Controls Overlay (Top Right)
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Zoom Level Pill
        Surface(
          color = Color(0xCC0B1120),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(0.8.dp, CyanNeon.copy(alpha = 0.4f))
        ) {
          Text(
            text = "${(scale * 100).toInt()}%",
            color = CyanNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          )
        }

        // Zoom In Button
        IconButton(
          onClick = { scale = (scale * 1.35f).coerceAtMost(8.0f) },
          modifier = Modifier
            .size(30.dp)
            .background(Color(0xCC0B1120), CircleShape)
            .border(0.8.dp, SpaceCardBorder, CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "확대",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }

        // Zoom Out Button
        IconButton(
          onClick = { scale = (scale / 1.35f).coerceAtLeast(0.25f) },
          modifier = Modifier
            .size(30.dp)
            .background(Color(0xCC0B1120), CircleShape)
            .border(0.8.dp, SpaceCardBorder, CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "축소",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }

        // Center on User Button
        IconButton(
          onClick = {
            scale = 1.2f
            offset = Offset.Zero
          },
          modifier = Modifier
            .size(30.dp)
            .background(Color(0xCC0B1120), CircleShape)
            .border(0.8.dp, CyanNeon.copy(alpha = 0.6f), CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "내 위치 중심",
            tint = CyanNeon,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Bottom info strip: Current Location & Legend
    if (showFullLegend) {
      Spacer(modifier = Modifier.height(6.dp))
      MapLegendBar()
      Spacer(modifier = Modifier.height(6.dp))
      CurrentLocationStatusCard(userPose = userPose)
    }
  }
}

private fun DrawScope.drawFloorGrid(
  scale: Float,
  originX: Float,
  originY: Float,
  width: Float,
  height: Float
) {
  val step = 5f * scale // Every 5 meters
  if (step > 15f) {
    var x = originX % step
    while (x < width) {
      drawLine(
        color = Color(0xFF14213D).copy(alpha = 0.4f),
        start = Offset(x, 0f),
        end = Offset(x, height),
        strokeWidth = 1f
      )
      x += step
    }

    var y = originY % step
    while (y < height) {
      drawLine(
        color = Color(0xFF14213D).copy(alpha = 0.4f),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1f
      )
      y += step
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapLegendBar() {
  FlowRow(
    modifier = Modifier
      .fillMaxWidth()
      .background(SpaceSurfaceElevated.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
      .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    LegendItem(color = ScanVisualSystem.Completed, label = "스캔 완료")
    LegendItem(color = ScanVisualSystem.InProgress, label = "스캔 진행 중")
    LegendItem(color = ScanVisualSystem.Unscanned, label = "미스캔")
    LegendItem(color = ScanVisualSystem.LowQuality, label = "품질 낮음")
    LegendItem(color = ScanVisualSystem.RescanNeeded, label = "재스캔 필요")
    LegendItem(color = ScanVisualSystem.AiRecommended, label = "AI 추천")
    LegendIconItem(icon = Icons.Default.CameraAlt, label = "스캔 이미지 (📷)")
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(text = label, color = Color(0xFFCBD5E1), fontSize = 10.sp)
  }
}

@Composable
private fun LegendIconItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = CyanNeon,
      modifier = Modifier.size(12.dp)
    )
    Spacer(modifier = Modifier.width(3.dp))
    Text(text = label, color = Color(0xFFCBD5E1), fontSize = 10.sp)
  }
}

@Composable
fun CurrentLocationStatusCard(userPose: UserPose) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF131F37))
      .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 7.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(Color(0xFF00B4D8)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Navigation,
          contentDescription = "현재 위치",
          tint = Color.White,
          modifier = Modifier.size(14.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = "현재 위치",
          color = Color(0xFF94A3B8),
          fontSize = 10.sp
        )
        Text(
          text = "${userPose.floorId} 중앙 구역 (정확도 약 ${userPose.estimatedAccuracyMeters}m)",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0x3300E5FF))
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Text(
        text = "좌표 (${String.format("%.1f", userPose.x)}m, ${String.format("%.1f", userPose.y)}m)",
        color = CyanNeon,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
