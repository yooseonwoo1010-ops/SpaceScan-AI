package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectRepository
import com.example.model.Project
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanVisualSystem
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
  repository: ProjectRepository,
  currentProjectId: String?,
  onBackClick: () -> Unit,
  onNewProjectClick: () -> Unit,
  onSelectProject: (Project) -> Unit
) {
  val projects by repository.projects.collectAsState()

  Scaffold(
    containerColor = SpaceDarkBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "프로젝트 보관함",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("back_button_project_list")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "뒤로가기",
              tint = Color.White
            )
          }
        },
        actions = {
          IconButton(
            onClick = onNewProjectClick,
            modifier = Modifier.testTag("add_project_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "새 프로젝트 추가",
              tint = CyanNeon
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SpaceSurfaceDark
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Header: Project counter & Create New Project button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "저장된 프로젝트 (${projects.size}개)",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "스캔 진행 중인 건물 및 완료된 데이터",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
          )
        }

        Button(
          onClick = onNewProjectClick,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ElectricBlue,
            contentColor = Color.White
          ),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("new_project_header_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "새 프로젝트", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Projects List
      if (projects.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.QrCodeScanner,
              contentDescription = null,
              tint = Color(0xFF475569),
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "저장된 프로젝트가 없습니다.",
              color = Color(0xFF94A3B8),
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onNewProjectClick,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
              Text("새 프로젝트 만들기")
            }
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(projects, key = { it.id }) { project ->
            val isCurrent = project.id == currentProjectId
            ProjectItemCard(
              project = project,
              isCurrent = isCurrent,
              onClick = { onSelectProject(project) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProjectItemCard(
  project: Project,
  isCurrent: Boolean,
  onClick: () -> Unit
) {
  val dateFormatted = remember(project.createdAt) {
    val df = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    df.format(Date(project.createdAt))
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(if (isCurrent) Color(0xFF13203C) else SpaceSurfaceElevated)
      .border(
        width = if (isCurrent) 1.5.dp else 1.dp,
        color = if (isCurrent) CyanNeon else SpaceCardBorder,
        shape = RoundedCornerShape(14.dp)
      )
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("project_item_${project.id}")
  ) {
    Column {
      // Top row: Project Name, Status badge & Mode badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Text(
            text = project.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1
          )
          if (isCurrent) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CyanNeon.copy(alpha = 0.2f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Text(
                text = "현재 선택됨",
                color = CyanNeon,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Mode badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (project.mode == "REAL") ScanCompletedGreen.copy(alpha = 0.18f) else ScanInProgressAmber.copy(alpha = 0.18f))
              .border(0.8.dp, if (project.mode == "REAL") ScanCompletedGreen else ScanInProgressAmber, RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (project.mode == "REAL") "REAL" else "DEMO",
              color = if (project.mode == "REAL") ScanCompletedGreen else ScanInProgressAmber,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Status badge
          val (statusLabel, statusColor) = when (project.status) {
            "COMPLETED" -> Pair("완료", ScanCompletedGreen)
            "SCANNING" -> Pair("스캔 중", CyanNeon)
            else -> Pair("신규", Color(0xFF94A3B8))
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(statusColor.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = statusLabel,
              color = statusColor,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Middle Row: Building name & Floor Count
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Apartment, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = project.buildingName, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "${project.floors}개 층", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = dateFormatted, color = Color(0xFF64748B), fontSize = 11.sp)
        }
      }

      if (project.description.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = project.description,
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Scan progress bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "스캔 진행률",
          color = Color(0xFF64748B),
          fontSize = 10.sp
        )
        Text(
          text = "${project.scanProgress}%",
          color = if (project.scanProgress > 0) CyanNeon else Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { project.scanProgress / 100f },
        color = CyanNeon,
        trackColor = Color(0xFF1E293B),
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
      )
    }
  }
}
