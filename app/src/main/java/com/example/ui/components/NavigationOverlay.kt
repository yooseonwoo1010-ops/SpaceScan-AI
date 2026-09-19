package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiRecommendation
import com.example.model.Floor
import com.example.model.UserPose
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated

@Composable
fun NavigationOverlay(
  floor: Floor,
  userPose: UserPose,
  breadcrumbs: List<Pair<Float, Float>>,
  recommendation: AiRecommendation,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isGuiding by remember { mutableStateOf(true) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceDarkBg)
      .padding(12.dp)
  ) {
    // Top Bar with Back Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "뒤로가기",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "AI 추천 경로 및 내비게이션",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp
        )
      }

      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.CompassCalibration,
          contentDescription = "나침반",
          tint = CyanNeon,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Next Target Summary Card (Screenshot 6 top card)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(SpaceSurfaceElevated)
        .border(1.2.dp, ElectricBlue, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF0077B6)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PinDrop,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "다음 스캔 장소: ${recommendation.nextTargetName}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                text = "현재 위치에서 약 ${recommendation.distanceMeters.toInt()}m",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
          }

          Button(
            onClick = { isGuiding = !isGuiding },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isGuiding) ScanCompletedGreen else ElectricBlue
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = if (isGuiding) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isGuiding) "안내 중" else "경로 시작",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(text = "중요도: ${recommendation.importance}", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          Text(text = "현재 커버리지: ${recommendation.coveragePercent}%", color = ScanInProgressAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2D Map showing the highlighted route
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
    ) {
      Indoor2DMapView(
        floor = floor,
        userPose = userPose,
        breadcrumbs = breadcrumbs,
        recommendation = recommendation,
        selectedRoomId = recommendation.nextTargetRoomId,
        onRoomSelected = {},
        showFullLegend = false,
        modifier = Modifier.fillMaxSize()
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Bottom Turn-by-Turn Instruction List (Matches Screenshot 6)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(SpaceSurfaceDark)
        .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Column {
        recommendation.routeSteps.forEach { step ->
          Row(
            modifier = Modifier.padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (step.stepNumber == 1) Icons.Default.Navigation else Icons.Default.TurnRight,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${step.stepNumber}. ${step.instruction}",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AccessTime,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "예상 소요 시간: 약 2분",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }

          Text(
            text = "주변을 확인하면서 이동하세요",
            color = ScanInProgressAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
