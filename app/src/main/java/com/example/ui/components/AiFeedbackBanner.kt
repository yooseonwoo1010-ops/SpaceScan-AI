package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiRecommendation
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceSurfaceDark

@Composable
fun AiFeedbackBanner(
  recommendation: AiRecommendation?,
  isVoiceEnabled: Boolean,
  onToggleVoice: () -> Unit,
  onViewRouteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wave")
  val wave1 by infiniteTransition.animateFloat(
    initialValue = 4f,
    targetValue = 18f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "w1"
  )
  val wave2 by infiniteTransition.animateFloat(
    initialValue = 16f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(450, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "w2"
  )
  val wave3 by infiniteTransition.animateFloat(
    initialValue = 8f,
    targetValue = 22f,
    animationSpec = infiniteRepeatable(
      animation = tween(380, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "w3"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(
        Brush.verticalGradient(
          listOf(
            SpaceSurfaceDark.copy(alpha = 0.96f),
            Color(0xFF0F172A).copy(alpha = 0.98f)
          )
        )
      )
      .border(1.2.dp, CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Column {
      // Top row: AI Tag & Voice toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(CyanNeon, ElectricBlue))),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "AI 안내",
              tint = Color.Black,
              modifier = Modifier.size(15.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "AI 실시간 추천",
            color = CyanNeon,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )

          // Audio waveform indicator
          if (isVoiceEnabled) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Box(modifier = Modifier.width(3.dp).height(wave1.dp).background(CyanNeon, CircleShape))
              Box(modifier = Modifier.width(3.dp).height(wave2.dp).background(CyanNeon, CircleShape))
              Box(modifier = Modifier.width(3.dp).height(wave3.dp).background(CyanNeon, CircleShape))
            }
          }
        }

        // Voice toggle chip
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(0.8.dp, SpaceCardBorder, RoundedCornerShape(12.dp))
            .clickable { onToggleVoice() }
            .padding(horizontal = 8.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "음성 토글",
            tint = if (isVoiceEnabled) CyanNeon else Color.Gray,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isVoiceEnabled) "음성 ON" else "음성 OFF",
            color = if (isVoiceEnabled) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Middle: Target Recommendation & Distance & Action Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = recommendation?.feedbackSpeech ?: "앞쪽 복도를 먼저 스캔하세요.",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "추천 이유: ${recommendation?.reason ?: "현재 위치와 가깝고 미스캔 영역"}",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            maxLines = 1
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = onViewRouteClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = ElectricBlue,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "경로",
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "경로 보기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
