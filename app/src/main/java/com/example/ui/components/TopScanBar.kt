package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.ConfidenceLevel
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceSurfaceDark

@Composable
fun TopScanBar(
  userPose: UserPose,
  overallCoverage: Int,
  isDemoMode: Boolean,
  onRelocalizeClick: () -> Unit,
  onCoverageDetailsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            SpaceSurfaceDark.copy(alpha = 0.95f),
            SpaceSurfaceDark.copy(alpha = 0.75f)
          )
        )
      )
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Floor badge & Scan coverage
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0077B6), Color(0xFF00B4D8))))
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            text = userPose.floorId,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Scan progress chip
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .clickable { onCoverageDetailsClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = "스캔",
            tint = CyanNeon,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "스캔 $overallCoverage%",
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Middle: Mode indicator
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(if (isDemoMode) Color(0x33FFB300) else Color(0x3300E676))
          .border(1.dp, if (isDemoMode) ScanInProgressAmber else ScanCompletedGreen, CircleShape)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = if (isDemoMode) "DEMO 가상 모드" else "REAL SCAN",
          color = if (isDemoMode) ScanInProgressAmber else ScanCompletedGreen,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Right: Confidence indicator & Relocalization
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "위치 정확도: ",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
            val confColor = when (userPose.confidence) {
              ConfidenceLevel.HIGH -> ScanCompletedGreen
              ConfidenceLevel.MEDIUM -> ScanInProgressAmber
              ConfidenceLevel.LOW -> ScanRescanRed
              ConfidenceLevel.TRACKING -> CyanNeon
            }
            Text(
              text = userPose.confidence.label,
              color = confColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Text(
            text = "약 ${userPose.estimatedAccuracyMeters}m ${userPose.confidence.dots}",
            color = CyanNeon,
            fontSize = 10.sp,
            letterSpacing = 1.sp
          )
        }

        IconButton(
          onClick = onRelocalizeClick,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Cached,
            contentDescription = "재로컬라이제이션",
            tint = CyanNeon,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
