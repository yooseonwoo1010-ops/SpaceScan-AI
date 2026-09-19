package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Building
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark

@Composable
fun CoverageDialog(
  building: Building,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(SpaceDarkBg)
        .border(1.5.dp, SpaceCardBorder, RoundedCornerShape(16.dp))
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.PieChart,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "스캔 커버리지 현황",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "닫기",
              tint = Color(0xFF94A3B8)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center: Overall Circular Chart & Floor pills
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceSurfaceDark)
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          // Circular gauge
          Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
              progress = { building.overallCoveragePercent / 100f },
              modifier = Modifier.size(90.dp),
              color = CyanNeon,
              strokeWidth = 9.dp,
              trackColor = Color(0xFF1E293B)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${building.overallCoveragePercent}%",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
              )
              Text(
                text = "전체 스캔",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
              )
            }
          }

          // Floor quick badges
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            building.floors.take(3).forEach { floor ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        floor.coveragePercent > 85 -> ScanCompletedGreen
                        floor.coveragePercent > 65 -> CyanNeon
                        else -> ScanInProgressAmber
                      }
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = floor.id,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                  text = "${floor.coveragePercent}%",
                  color = Color(0xFFCBD5E1),
                  fontSize = 12.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Floor-by-Floor detail breakdown
        Text(
          text = "층별 상세 현황",
          color = Color(0xFFCBD5E1),
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        building.floors.forEach { floor ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = floor.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "(${floor.completedRoomsCount}/${floor.totalRoomsCount} 구역)",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
              )
            }
            Text(
              text = "${floor.coveragePercent}%",
              color = if (floor.coveragePercent > 80) ScanCompletedGreen else CyanNeon,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }

          LinearProgressIndicator(
            progress = { floor.coveragePercent / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (floor.coveragePercent > 80) ScanCompletedGreen else ElectricBlue,
            trackColor = Color(0xFF1E293B)
          )
          Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Key Space Coverage (101, 102, 103, Corridor)
        Text(
          text = "주요 공간별 커버리지 (2F)",
          color = Color(0xFFCBD5E1),
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val sampleSpaces = listOf(
          Pair("201호 (교실)", 88),
          Pair("202호 (실습실)", 74),
          Pair("203호 (도서실)", 45),
          Pair("중앙 복도", 84)
        )

        sampleSpaces.forEach { (name, pct) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = name, color = Color(0xFFE2E8F0), fontSize = 11.sp)
            Text(text = "$pct%", color = if (pct > 80) ScanCompletedGreen else if (pct < 50) ScanRescanRed else CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(2.dp))
          LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = if (pct > 80) ScanCompletedGreen else if (pct < 50) ScanRescanRed else CyanNeon,
            trackColor = Color(0xFF1E293B)
          )
          Spacer(modifier = Modifier.height(6.dp))
        }
      }
    }
  }
}
