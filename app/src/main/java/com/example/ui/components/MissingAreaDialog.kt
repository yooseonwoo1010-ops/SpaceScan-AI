package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ScanMissingArea
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark

@Composable
fun MissingAreaDialog(
  missingArea: ScanMissingArea,
  onStartRescan: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(SpaceDarkBg)
        .border(1.5.dp, ScanRescanRed, RoundedCornerShape(16.dp))
        .padding(16.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x33FF3D71)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = ScanRescanRed,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "미스캔 구역 탐지",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF94A3B8))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Red Warning Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x337F1D1D))
            .border(1.dp, ScanRescanRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(ScanRescanRed)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "재스캔 필요",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = missingArea.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = missingArea.description,
              color = Color(0xFFFCA5A5),
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulated 3D wireframe preview of missing zone
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(10.dp))
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Red warning cube wireframe
            drawRect(
              color = ScanRescanRed.copy(alpha = 0.2f),
              topLeft = Offset(cx - 60f, cy - 40f),
              size = Size(120f, 80f)
            )
            drawRect(
              color = ScanRescanRed,
              topLeft = Offset(cx - 60f, cy - 40f),
              size = Size(120f, 80f),
              style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            )

            drawLine(
              color = ScanRescanRed,
              start = Offset(cx - 60f, cy - 40f),
              end = Offset(cx - 90f, cy - 20f),
              strokeWidth = 1.5f
            )
            drawLine(
              color = ScanRescanRed,
              start = Offset(cx + 60f, cy - 40f),
              end = Offset(cx + 30f, cy - 20f),
              strokeWidth = 1.5f
            )
            drawLine(
              color = ScanRescanRed,
              start = Offset(cx + 60f, cy + 40f),
              end = Offset(cx + 30f, cy + 60f),
              strokeWidth = 1.5f
            )
            drawLine(
              color = ScanRescanRed,
              start = Offset(cx - 60f, cy + 40f),
              end = Offset(cx - 90f, cy + 60f),
              strokeWidth = 1.5f
            )
          }

          Text(
            text = "3D 스캔 데이터 부족 영역 (적색 표시)",
            color = ScanRescanRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(text = "나중에", color = Color(0xFF94A3B8))
          }

          Button(
            onClick = onStartRescan,
            modifier = Modifier.weight(1.5f),
            colors = ButtonDefaults.buttonColors(containerColor = ScanRescanRed),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(text = "재스캔 시작", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
