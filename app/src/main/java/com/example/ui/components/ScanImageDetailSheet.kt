package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScanImage
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanImageDetailSheet(
  scanImage: ScanImage?,
  sheetState: SheetState,
  onDismiss: () -> Unit,
  onViewIn3D: (ScanImage) -> Unit,
  onViewOnMap: (ScanImage) -> Unit,
  modifier: Modifier = Modifier
) {
  if (scanImage == null) return

  val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
  val dateString = dateFormat.format(Date(scanImage.timestamp))

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = SpaceSurfaceDark,
    contentColor = Color.White,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 8.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color(0xFF475569))
      )
    },
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(CyanNeon.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "스캔 키프레임 이미지",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = scanImage.roomName ?: "지정 구역",
              color = CyanNeon,
              fontSize = 12.sp
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "닫기",
            tint = Color(0xFF94A3B8)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Main Image / Thumbnail Preview
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF0F172A))
          .border(1.dp, SpaceCardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (scanImage.thumbnailBitmap != null) {
          Image(
            bitmap = scanImage.thumbnailBitmap.asImageBitmap(),
            contentDescription = "스캔 이미지 캡처",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = null,
              tint = Color(0xFF475569),
              modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "스캔 메타데이터 Keyframe #${scanImage.id.takeLast(5)}",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp
            )
          }
        }

        // Overlay Badge
        Surface(
          color = Color(0xCC0B1120),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
        ) {
          Text(
            text = "품질 ${(scanImage.qualityScore * 100).toInt()}% • Depth",
            color = CyanNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Metadata Grid Card
      Surface(
        color = SpaceSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "3D 공간 메타데이터 (Spatial Telemetry)",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "촬영 일시", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Text(text = dateString, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Column {
              Text(text = "화각 (FOV)", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Text(text = "${scanImage.fovDegrees.toInt()}°", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "3D 카메라 위치 (X, Y, Z)", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Text(
                text = "X=${String.format("%.2f", scanImage.worldX)}m  Y=${String.format("%.2f", scanImage.worldY)}m  Z=${String.format("%.2f", scanImage.worldZ)}m",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "회전 각도 (Yaw, Pitch)", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Text(
                text = "Yaw=${String.format("%.1f", scanImage.cameraRotationYaw)}°  Pitch=${String.format("%.1f", scanImage.cameraRotationPitch)}°",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            Column {
              Text(text = "2D 지도 평면 좌표", color = Color(0xFF94A3B8), fontSize = 10.sp)
              Text(
                text = "(${String.format("%.1f", scanImage.mapX)}m, ${String.format("%.1f", scanImage.mapY)}m)",
                color = CyanNeon,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Navigation Actions: "View in 3D" & "View on Map"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = {
            onViewOnMap(scanImage)
            onDismiss()
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
          border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "2D 지도에서 보기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = {
            onViewIn3D(scanImage)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ViewInAr,
            contentDescription = null,
            tint = Color(0xFF0F172A),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "3D 뷰에서 보기",
            color = Color(0xFF0F172A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
