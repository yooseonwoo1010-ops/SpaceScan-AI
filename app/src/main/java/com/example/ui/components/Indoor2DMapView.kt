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
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
import com.example.model.ScanStatus
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanVisualSystem
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.ScanUnscannedSlate
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
  detectedPlanes: List<com.example.model.ScanPlane> = emptyList(),
  accumulatedPoints: List<com.example.model.ScanPoint> = emptyList(),
  showFullLegend: Boolean = true,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(0.7f, 3.5f)
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
        .pointerInput(floor) {
          detectTapGestures { tapOffset ->
            // Inverse transform to find meter coordinates
            val canvasW = size.width
            val canvasH = size.height
            val scaleFactor = (canvasW / 58f) * scale
            val originX = 4f * scaleFactor + offset.x
            val originY = 6f * scaleFactor + offset.y

            val clickedMetersX = (tapOffset.x - originX) / scaleFactor
            val clickedMetersY = (tapOffset.y - originY) / scaleFactor

            // Check if any room contains this coordinate
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
              radius = 5f,
              center = doorPos
            )
          }

          // Room Text label (using native canvas for sharp text)
          drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
              color = if (room.id == selectedRoomId) android.graphics.Color.CYAN else android.graphics.Color.WHITE
              textSize = (13f * scale).coerceIn(11f, 22f)
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
              textSize = (10f * scale).coerceIn(9f, 18f)
              isAntiAlias = true
              textAlign = android.graphics.Paint.Align.CENTER
            }

            val textCenter = metersToScreen(room.x, room.y)
            drawText(room.name.take(6), textCenter.x, textCenter.y - 4f, paint)
            drawText("${room.coveragePercent}%", textCenter.x, textCenter.y + 14f * scale, subPaint)
          }

          // Missing area warning indicator
          if (room.missingAreas.isNotEmpty()) {
            val warnPos = metersToScreen(room.x + room.width / 2f - 1.5f, room.y - room.height / 2f + 1.5f)
            drawCircle(
              color = ScanRescanRed,
              radius = 8f * scale,
              center = warnPos
            )
            drawCircle(
              color = Color.White,
              radius = 4f * scale,
              center = warnPos
            )
          }
        }

        // 3. Draw Stairs & Elevators & Restrooms
        floor.stairs.forEach { stair ->
          val pos = metersToScreen(stair.x, stair.y)
          drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(pos.x - 20f, pos.y - 20f),
            size = Size(40f, 40f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
          )
          drawContext.canvas.nativeCanvas.drawText("계단", pos.x, pos.y + 5f, android.graphics.Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = 10f * scale
            textAlign = android.graphics.Paint.Align.CENTER
          })
        }

        floor.elevators.forEach { elevator ->
          val pos = metersToScreen(elevator.x, elevator.y)
          drawRoundRect(
            color = Color(0xFF2E1C6A),
            topLeft = Offset(pos.x - 18f, pos.y - 18f),
            size = Size(36f, 36f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
          )
          drawContext.canvas.nativeCanvas.drawText("EV", pos.x, pos.y + 5f, android.graphics.Paint().apply {
            color = android.graphics.Color.MAGENTA
            textSize = 10f * scale
            textAlign = android.graphics.Paint.Align.CENTER
          })
        }

        floor.restrooms.forEach { rr ->
          val pos = metersToScreen(rr.x, rr.y)
          drawContext.canvas.nativeCanvas.drawText("화장실", pos.x, pos.y + 5f, android.graphics.Paint().apply {
            color = android.graphics.Color.CYAN
            textSize = 10f * scale
            textAlign = android.graphics.Paint.Align.CENTER
          })
        }

        // 3. Draw Real Detected Planes from ARCore
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
          val step = (accumulatedPoints.size / 300).coerceAtLeast(1)
          for (i in accumulatedPoints.indices step step) {
            val pt = accumulatedPoints[i]
            val ptScreen = metersToScreen(pt.x, -pt.z)
            drawCircle(
              color = ScanCompletedGreen.copy(alpha = 0.6f),
              radius = 2.5f,
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
              width = 4f,
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
          )
        }

        // 5. Draw AI Recommended Scan Route Path
        if (recommendation != null) {
          val targetRoom = floor.rooms.find { it.id == recommendation.nextTargetRoomId }
          if (targetRoom != null) {
            val userScreen = metersToScreen(userPose.x, userPose.y)
            val targetScreen = metersToScreen(targetRoom.x, targetRoom.y)

            val routePath = Path().apply {
              moveTo(userScreen.x, userScreen.y)
              // Go through corridor midpoint then into room
              val midX = (userScreen.x + targetScreen.x) / 2f
              val midY = 19f * baseScale + originY
              lineTo(midX, midY)
              lineTo(targetScreen.x, targetScreen.y)
            }

            // Glowing halo
            drawPath(
              path = routePath,
              color = CyanNeon.copy(alpha = 0.3f),
              style = Stroke(width = 10f, cap = StrokeCap.Round)
            )

            // Dynamic route line with arrow dash
            drawPath(
              path = routePath,
              color = CyanNeon,
              style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
              )
            )

            // Star / Flag Marker on Target
            drawCircle(
              color = ElectricBlue,
              radius = 12f * scale,
              center = targetScreen
            )
            drawCircle(
              color = Color.White,
              radius = 5f * scale,
              center = targetScreen
            )
          }
        }

        // 6. Draw User Location Marker with Heading Cone
        val userCenter = metersToScreen(userPose.x, userPose.y)

        // Pulsing radar ring
        drawCircle(
          color = CyanNeon.copy(alpha = pulseAlpha),
          radius = pulseRadius * scale,
          center = userCenter
        )

        // Orientation Heading Cone
        rotate(degrees = userPose.yawDegrees, pivot = userCenter) {
          val conePath = Path().apply {
            moveTo(userCenter.x, userCenter.y)
            lineTo(userCenter.x - 18f * scale, userCenter.y - 42f * scale)
            lineTo(userCenter.x + 18f * scale, userCenter.y - 42f * scale)
            close()
          }
          drawPath(
            path = conePath,
            brush = Brush.radialGradient(
              colors = listOf(CyanNeon.copy(alpha = 0.45f), Color.Transparent),
              center = userCenter,
              radius = 45f * scale
            )
          )

          // Direction arrow head
          val arrowPath = Path().apply {
            moveTo(userCenter.x, userCenter.y - 18f * scale)
            lineTo(userCenter.x - 7f * scale, userCenter.y - 6f * scale)
            lineTo(userCenter.x + 7f * scale, userCenter.y - 6f * scale)
            close()
          }
          drawPath(path = arrowPath, color = Color.White)
        }

        // Core user position dot
        drawCircle(
          color = Color(0xFF003566),
          radius = 10f * scale,
          center = userCenter
        )
        drawCircle(
          color = CyanNeon,
          radius = 7f * scale,
          center = userCenter
        )
        drawCircle(
          color = Color.White,
          radius = 3f * scale,
          center = userCenter
        )
      }

      val hasMapData = floor.rooms.isNotEmpty() || floor.corridors.isNotEmpty() || detectedPlanes.isNotEmpty() || accumulatedPoints.isNotEmpty() || breadcrumbs.isNotEmpty()
      if (!hasMapData) {
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
              imageVector = Icons.Default.Map,
              contentDescription = null,
              tint = Color(0xFF64748B),
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "아직 지도 없음",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "공간을 스캔하면 2D 평면도가 실시간 생성됩니다",
              color = CyanNeon,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
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
    LegendIconItem(icon = Icons.Default.Stairs, label = "계단")
    LegendIconItem(icon = Icons.Default.Elevator, label = "EV")
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
      tint = Color(0xFF94A3B8),
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
          text = "${userPose.floorId} 중앙 복도 (정확도 약 ${userPose.estimatedAccuracyMeters}m)",
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
        text = "좌표 (${userPose.x.toInt()}m, ${userPose.y.toInt()}m)",
        color = CyanNeon,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
