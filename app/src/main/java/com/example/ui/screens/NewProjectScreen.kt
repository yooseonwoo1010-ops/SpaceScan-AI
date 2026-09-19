package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectRepository
import com.example.model.Project
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ScanCompletedGreen
import com.example.ui.theme.ScanInProgressAmber
import com.example.ui.theme.ScanRescanRed
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDarkBg
import com.example.ui.theme.SpaceSurfaceDark
import com.example.ui.theme.SpaceSurfaceElevated
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
  repository: ProjectRepository,
  onBackClick: () -> Unit,
  onProjectCreated: (Project) -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // Form Fields
  var projectName by remember { mutableStateOf("") }
  var buildingName by remember { mutableStateOf("") }
  var floorsText by remember { mutableStateOf("3") }
  var description by remember { mutableStateOf("") }
  var scanMode by remember { mutableStateOf("REAL") } // "REAL" or "DEMO"

  // Validation errors
  var projectNameError by remember { mutableStateOf<String?>(null) }
  var buildingNameError by remember { mutableStateOf<String?>(null) }
  var floorsError by remember { mutableStateOf<String?>(null) }
  var generalErrorMessage by remember { mutableStateOf<String?>(null) }

  // Submission state for preventing multiple rapid clicks
  var isSubmitting by remember { mutableStateOf(false) }

  fun validateProjectForm(): Boolean {
    var isValid = true

    // 1. 프로젝트 이름 검증: 필수
    if (projectName.trim().isEmpty()) {
      projectNameError = "프로젝트 이름을 입력하세요."
      isValid = false
    } else {
      projectNameError = null
    }

    // 2. 건물 이름 검증: 필수
    if (buildingName.trim().isEmpty()) {
      buildingNameError = "건물 이름을 입력하세요."
      isValid = false
    } else {
      buildingNameError = null
    }

    // 3. 층수 검증: 1 이상
    val floorNum = floorsText.toIntOrNull()
    if (floorNum == null || floorNum < 1) {
      floorsError = "층수는 1층 이상이어야 합니다."
      isValid = false
    } else {
      floorsError = null
    }

    return isValid
  }

  fun handleSubmit() {
    if (isSubmitting) return

    coroutineScope.launch {
      Log.d("Project", "[Project] create start")
      isSubmitting = true
      generalErrorMessage = null

      try {
        // Step 1: Form validation
        if (!validateProjectForm()) {
          Log.w("Project", "[Project] validation failed: name='$projectName', building='$buildingName', floors='$floorsText'")
          isSubmitting = false
          return@launch
        }
        Log.d("Project", "[Project] validation passed")

        // Step 2: Generate project ID
        val projectId = "project_${System.currentTimeMillis()}"
        Log.d("Project", "[Project] generated id: $projectId")

        val floorCount = floorsText.toIntOrNull() ?: 3

        // Step 3: Create Project object with fresh building data
        val freshBuilding = repository.createFreshBuilding(
          projectId = projectId,
          buildingName = buildingName.trim(),
          floorCount = floorCount
        )

        val newProject = Project(
          id = projectId,
          name = projectName.trim(),
          buildingName = buildingName.trim(),
          floors = floorCount,
          description = description.trim(),
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis(),
          mode = scanMode,
          currentFloor = 1,
          scanProgress = 0,
          status = "NEW",
          building = freshBuilding
        )
        Log.d("Project", "[Project] project object created")

        // Step 4: Save project to persistent storage
        val saveSuccess = repository.saveProject(newProject)
        if (!saveSuccess) {
          throw IllegalStateException("Failed to save project to repository")
        }

        // Step 5 & 6: Update project list & Set current project
        Log.d("Project", "[Project] current project set")

        // Step 7: Navigate to Scanner
        Log.d("Project", "[Project] navigation to scanner")
        Log.d("Project", "[Project] create success")

        onProjectCreated(newProject)
      } catch (e: Exception) {
        Log.e("Project", "[Project] create failed", e)
        generalErrorMessage = "프로젝트 생성에 실패했습니다. 다시 시도해주세요."
        snackbarHostState.showSnackbar("프로젝트 생성에 실패했습니다. 다시 시도해주세요.")
      } finally {
        isSubmitting = false
      }
    }
  }

  Scaffold(
    containerColor = SpaceDarkBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "새 프로젝트 만들기",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "뒤로가기",
              tint = Color.White
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
        .padding(horizontal = 20.dp, vertical = 14.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // General error alert banner
      AnimatedVisibility(visible = generalErrorMessage != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ScanRescanRed.copy(alpha = 0.15f))
            .border(1.dp, ScanRescanRed, RoundedCornerShape(10.dp))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = ScanRescanRed)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = generalErrorMessage ?: "",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Card: Basic Info Form
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(SpaceSurfaceElevated)
          .border(1.dp, SpaceCardBorder, RoundedCornerShape(16.dp))
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "기본 정보 입력",
          color = CyanNeon,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )

        // 1. 프로젝트 이름 (필수)
        Column {
          OutlinedTextField(
            value = projectName,
            onValueChange = {
              projectName = it
              if (it.trim().isNotEmpty()) projectNameError = null
            },
            label = { Text("프로젝트 이름 *") },
            placeholder = { Text("예: 학교 본관 스캔", color = Color(0xFF64748B)) },
            leadingIcon = {
              Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon)
            },
            isError = projectNameError != null,
            singleLine = true,
            colors = customTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_project_name")
          )
          if (projectNameError != null) {
            Text(
              text = projectNameError ?: "",
              color = ScanRescanRed,
              fontSize = 11.sp,
              modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
          }
        }

        // 2. 건물 이름 (필수)
        Column {
          OutlinedTextField(
            value = buildingName,
            onValueChange = {
              buildingName = it
              if (it.trim().isNotEmpty()) buildingNameError = null
            },
            label = { Text("건물 이름 *") },
            placeholder = { Text("예: OO고등학교 본관", color = Color(0xFF64748B)) },
            leadingIcon = {
              Icon(Icons.Default.Apartment, contentDescription = null, tint = CyanNeon)
            },
            isError = buildingNameError != null,
            singleLine = true,
            colors = customTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_building_name")
          )
          if (buildingNameError != null) {
            Text(
              text = buildingNameError ?: "",
              color = ScanRescanRed,
              fontSize = 11.sp,
              modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
          }
        }

        // 3. 층수 (1 이상)
        Column {
          OutlinedTextField(
            value = floorsText,
            onValueChange = {
              floorsText = it.filter { ch -> ch.isDigit() }
              if (floorsText.isNotEmpty()) floorsError = null
            },
            label = { Text("층수 (1 이상) *") },
            placeholder = { Text("예: 3", color = Color(0xFF64748B)) },
            leadingIcon = {
              Icon(Icons.Default.Layers, contentDescription = null, tint = CyanNeon)
            },
            isError = floorsError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = customTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_floors")
          )
          if (floorsError != null) {
            Text(
              text = floorsError ?: "",
              color = ScanRescanRed,
              fontSize = 11.sp,
              modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
          }
        }

        // 4. 건물 설명 (선택)
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("건물 설명 (선택)") },
          placeholder = { Text("예: 교실과 복도 전체 3D 스캔 및 층별 지도 생성", color = Color(0xFF64748B)) },
          leadingIcon = {
            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF94A3B8))
          },
          minLines = 2,
          maxLines = 4,
          colors = customTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_description")
        )
      }

      // Card: Scan Mode Option (REAL SCAN vs DEMO MODE)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(SpaceSurfaceElevated)
          .border(1.dp, SpaceCardBorder, RoundedCornerShape(16.dp))
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "스캔 모드 선택",
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "실제 카메라를 사용할지, 에뮬레이터 가상 환경에서 체험할지 선택하세요.",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Real Camera Mode Option
          ModeSelectCard(
            title = "REAL SCAN",
            subtitle = "실제 카메라 & 센서",
            isSelected = scanMode == "REAL",
            icon = Icons.Default.CameraAlt,
            activeColor = ScanCompletedGreen,
            onClick = { scanMode = "REAL" },
            modifier = Modifier.weight(1f)
          )

          // Demo Mode Option
          ModeSelectCard(
            title = "DEMO MODE",
            subtitle = "가상 시뮬레이터",
            isSelected = scanMode == "DEMO",
            icon = Icons.Default.SmartToy,
            activeColor = ScanInProgressAmber,
            onClick = { scanMode = "DEMO" },
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Submit Button (Section 11: disabled when submitting)
      Button(
        onClick = { handleSubmit() },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ElectricBlue,
          disabledContainerColor = ElectricBlue.copy(alpha = 0.5f),
          contentColor = Color.White,
          disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("submit_create_project_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "프로젝트 생성 중...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        } else {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "프로젝트 생성",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun ModeSelectCard(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  activeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color(0xFF0F172A))
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) activeColor else SpaceCardBorder,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(12.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) activeColor else Color(0xFF94A3B8),
          modifier = Modifier.size(20.dp)
        )
        if (isSelected) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(activeColor, CircleShape)
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = title,
        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        color = Color(0xFF94A3B8),
        fontSize = 10.sp
      )
    }
  }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = CyanNeon,
  unfocusedBorderColor = SpaceCardBorder,
  errorBorderColor = ScanRescanRed,
  focusedLabelColor = CyanNeon,
  unfocusedLabelColor = Color(0xFF94A3B8),
  focusedTextColor = Color.White,
  unfocusedTextColor = Color.White,
  cursorColor = CyanNeon
)
