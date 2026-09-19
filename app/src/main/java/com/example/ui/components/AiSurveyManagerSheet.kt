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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AiRecommendation
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
fun AiSurveyManagerSheet(
  recommendation: AiRecommendation?,
  onNavigateToTarget: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedOptimizationMode by remember { mutableIntStateOf(0) } // 0: AI 자동 추천, 1: 직접 이동, 2: 전체 루트

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(SpaceDarkBg)
        .border(1.5.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CyanNeon, ElectricBlue))),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "AI 건물 스캔 관리자",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
              Text(
                text = "자율 공간 분석 및 실시간 경로 최적화",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Global Building Commentary Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF13203C))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "• 1층은 충분히 스캔되었습니다 (94%).\n• 2층 서쪽 영역의 데이터가 부족합니다.\n• 3층으로 이동하면 전체 스캔 효율이 높아집니다.",
              color = Color(0xFFE2E8F0),
              fontSize = 12.sp,
              lineHeight = 18.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recommended Next Area Card
        Text(
          text = "추천 다음 스캔 장소",
          color = Color(0xFFCBD5E1),
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

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
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0077B6))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = recommendation?.floorId ?: "2F",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = recommendation?.nextTargetName ?: "202호",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FF3D71))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(text = "중요도 높음", color = ScanRescanRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x3300E5FF))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(text = "거리 ${recommendation?.distanceMeters?.toInt() ?: 12}m", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFB300))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(text = "커버리지 ${recommendation?.coveragePercent ?: 45}%", color = ScanInProgressAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = onNavigateToTarget,
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "추천 경로 보기", fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recommendation Reasons Checklist
        Text(
          text = "추천 이유",
          color = Color(0xFFCBD5E1),
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          ReasonCheckItem("현재 위치와 가깝고 접근성이 우수합니다.")
          ReasonCheckItem("스캔 커버리지 및 포인트 클라우드 밀도가 낮습니다.")
          ReasonCheckItem("층별 수직 연결 및 3D 매쉬 완성도에 핵심인 구역입니다.")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scan Route Optimization Mode Selector
        Text(
          text = "스캔 루트 자동 최적화",
          color = Color(0xFFCBD5E1),
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("AI 자동 추천", "내가 직접 이동", "전체 루트 보기").forEachIndexed { index, mode ->
            val isSelected = selectedOptimizationMode == index
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) CyanNeon else SpaceSurfaceDark)
                .border(1.dp, if (isSelected) Color.White else SpaceCardBorder, RoundedCornerShape(8.dp))
                .clickable { selectedOptimizationMode = index }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = mode,
                color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Planned Scan Sequence (Section 13)
        Text(
          text = "AI 권장 전체 스캔 순서 (10단계)",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        val fullPlan = listOf(
          "1. 1F 중앙 복도 (완료)",
          "2. 101호 일반교실 (완료)",
          "3. 102호 과학실 (완료)",
          "4. 103호 컴퓨터실 (완료)",
          "5. 1F 계단 연결 (완료)",
          "6. 2F 중앙 복도 (진행중)",
          "7. 201호 교실 (진행중)",
          "8. 202호 실습실 (다음 추천)",
          "9. 2F 계단 이동 (예정)",
          "10. 3F 전체 스캔 (예정)"
        )

        fullPlan.take(6).forEach { planItem ->
          Text(
            text = planItem,
            color = if (planItem.contains("완료")) ScanCompletedGreen else if (planItem.contains("진행중") || planItem.contains("추천")) CyanNeon else Color(0xFF64748B),
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 1.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun ReasonCheckItem(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = ScanCompletedGreen,
      modifier = Modifier.size(15.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = text, color = Color(0xFFE2E8F0), fontSize = 11.sp)
  }
}
