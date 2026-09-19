package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceSurfaceDark

@Composable
fun FloorSelector(
  selectedFloorId: String,
  floors: List<String> = listOf("3F", "2F", "1F", "B1"),
  onFloorSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(SpaceSurfaceDark.copy(alpha = 0.9f))
      .border(1.dp, SpaceCardBorder, RoundedCornerShape(20.dp))
      .padding(4.dp)
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      floors.forEach { floorId ->
        val isSelected = floorId == selectedFloorId
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              if (isSelected) {
                Brush.linearGradient(listOf(CyanNeon, ElectricBlue))
              } else {
                Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF162032)))
              }
            )
            .border(
              width = if (isSelected) 1.5.dp else 0.5.dp,
              color = if (isSelected) Color.White else SpaceCardBorder,
              shape = CircleShape
            )
            .clickable { onFloorSelected(floorId) },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = floorId,
            color = if (isSelected) Color.Black else Color(0xFFE2E8F0),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
          )
        }
      }
    }
  }
}
