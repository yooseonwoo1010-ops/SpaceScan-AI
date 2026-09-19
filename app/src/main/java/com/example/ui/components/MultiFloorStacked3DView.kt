package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Building
import com.example.model.Floor
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated

@Composable
fun MultiFloorStacked3DView(
  building: Building,
  currentFloorId: String,
  onSelectFloor: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceDarkBg)
      .padding(14.dp)
  ) {
    // Top Building Title & Total Progress Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.CorporateFare,
          contentDescription = "건물",
          tint = CyanNeon,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "건물 전체 보기 (3D 적층 뷰)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          )
          Text(
            text = "${building.name} • 4개 층 통합 관리",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF1E293B))
          .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = "전체 ${building.overallCoveragePercent}%",
          color = CyanNeon,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3D Exploded Isometric Canvas
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0D1424))
        .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Isometric slab dimensions
        val slabW = size.width * 0.65f
        val slabH = 65f
        val floorGap = size.height * 0.19f

        val floorsOrdered = building.floors // 3F, 2F, 1F, B1
        val slabCenters = floorsOrdered.mapIndexed { index, _ ->
          Offset(cx, cy - floorGap * 1.5f + index * floorGap)
        }

        // Draw vertical connector shafts (Stairs and Elevators)
        if (slabCenters.size >= 2) {
          val topCenter = slabCenters.first()
          val bottomCenter = slabCenters.last()

          // Left shaft (Stairs)
          drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.4f),
            start = Offset(topCenter.x - slabW * 0.35f, topCenter.y),
            end = Offset(bottomCenter.x - slabW * 0.35f, bottomCenter.y),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
          )

          // Right shaft (Elevators)
          drawLine(
            color = Color(0xFFA855F7).copy(alpha = 0.4f),
            start = Offset(topCenter.x + slabW * 0.35f, topCenter.y),
            end = Offset(bottomCenter.x + slabW * 0.35f, bottomCenter.y),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
          )
        }

        // Draw Isometric slabs for each floor
        floorsOrdered.forEachIndexed { index, floor ->
          val center = slabCenters[index]
          val isCurrent = floor.id == currentFloorId

          // Isometric diamond slab points
          val pTop = Offset(center.x, center.y - slabH / 2f)
          val pRight = Offset(center.x + slabW / 2f, center.y)
          val pBottom = Offset(center.x, center.y + slabH / 2f)
          val pLeft = Offset(center.x - slabW / 2f, center.y)

          val slabPath = Path().apply {
            moveTo(pTop.x, pTop.y)
            lineTo(pRight.x, pRight.y)
            lineTo(pBottom.x, pBottom.y)
            lineTo(pLeft.x, pLeft.y)
            close()
          }

          val fillBrush = if (isCurrent) {
            Brush.linearGradient(
              listOf(
                ElectricBlue.copy(alpha = 0.45f),
                CyanNeon.copy(alpha = 0.25f)
              )
            )
          } else {
            Brush.linearGradient(
              listOf(
                Color(0xFF1E293B).copy(alpha = 0.5f),
                Color(0xFF0F172A).copy(alpha = 0.35f)
              )
            )
          }

          drawPath(path = slabPath, brush = fillBrush)

          drawPath(
            path = slabPath,
            color = if (isCurrent) CyanNeon else SpaceCardBorder,
            style = Stroke(width = if (isCurrent) 3f else 1.2f)
          )

          // Draw internal room partitions inside the isometric slab
          val partition1A = Offset(center.x - slabW * 0.15f, center.y - slabH * 0.15f)
          val partition1B = Offset(center.x - slabW * 0.15f, center.y + slabH * 0.15f)
          val partition2A = Offset(center.x + slabW * 0.15f, center.y - slabH * 0.15f)
          val partition2B = Offset(center.x + slabW * 0.15f, center.y + slabH * 0.15f)

          drawLine(
            color = if (isCurrent) CyanNeon.copy(alpha = 0.6f) else Color(0xFF334155),
            start = partition1A,
            end = partition1B,
            strokeWidth = 1f
          )
          drawLine(
            color = if (isCurrent) CyanNeon.copy(alpha = 0.6f) else Color(0xFF334155),
            start = partition2A,
            end = partition2B,
            strokeWidth = 1f
          )

          // If current floor, draw user location pin on this slab
          if (isCurrent) {
            val userPinCenter = Offset(center.x - 15f, center.y)
            drawCircle(
              color = CyanNeon,
              radius = 8f,
              center = userPinCenter
            )
            drawCircle(
              color = Color.White,
              radius = 4f,
              center = userPinCenter
            )
          }
        }
      }

      // Left Floating Overlay: Floor selector pills with coverage %
      Column(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        building.floors.forEach { floor ->
          val isCurrent = floor.id == currentFloorId
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isCurrent) Color(0xFF0077B6) else Color(0xDD1E293B))
              .border(
                1.dp,
                if (isCurrent) CyanNeon else SpaceCardBorder,
                RoundedCornerShape(8.dp)
              )
              .clickable { onSelectFloor(floor.id) }
              .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = floor.id,
              color = if (isCurrent) Color.White else Color(0xFF94A3B8),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${floor.coveragePercent}%",
              color = if (isCurrent) CyanNeon else Color(0xFFCBD5E1),
              fontSize = 11.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Floor Cards Breakdown (Matches Screenshot 4)
    Text(
      text = "층별 상세 현황",
      color = Color(0xFFCBD5E1),
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      building.floors.forEach { floor ->
        val isCurrent = floor.id == currentFloorId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) Color(0xFF162544) else SpaceSurfaceElevated)
            .border(
              width = if (isCurrent) 1.5.dp else 1.dp,
              color = if (isCurrent) CyanNeon else SpaceCardBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelectFloor(floor.id) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isCurrent) CyanNeon else Color(0xFF334155)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = floor.id,
                color = if (isCurrent) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = floor.name,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                if (isCurrent) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "현재 위치",
                    color = CyanNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
              Text(
                text = "스캔 완료 ${floor.completedRoomsCount} / ${floor.totalRoomsCount} 구역",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "${floor.coveragePercent}%",
              color = if (floor.coveragePercent > 80) ScanCompletedGreen else ScanInProgressAmber,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { floor.coveragePercent / 100f },
              modifier = Modifier
                .width(65.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = if (floor.coveragePercent > 80) ScanCompletedGreen else CyanNeon,
              trackColor = Color(0xFF1E293B)
            )
          }
        }
      }
    }
  }
}
