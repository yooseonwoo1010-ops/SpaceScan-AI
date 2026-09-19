package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.ProjectRepository
import com.example.model.Project
import com.example.sensors.PositionTracker
import com.example.sensors.VoiceAssistant
import com.example.ui.screens.MainStartScreen
import com.example.ui.screens.NewProjectScreen
import com.example.ui.screens.ProjectListScreen
import com.example.ui.screens.ScanWorkspaceScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
  START,
  NEW_PROJECT,
  PROJECT_LIST,
  WORKSPACE
}

class MainActivity : ComponentActivity() {

  private var positionTracker: PositionTracker? = null
  private var voiceAssistant: VoiceAssistant? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val scope = rememberCoroutineScope()
          val context = this@MainActivity

          // Initialize sensors and voice assistant once
          val tracker = remember {
            PositionTracker(context, scope).also { positionTracker = it }
          }
          val voice = remember {
            VoiceAssistant(context).also { voiceAssistant = it }
          }

          // Persistent Project Repository & Project State
          val projectRepository = remember { ProjectRepository(context) }
          var currentProject by remember { mutableStateOf<Project?>(null) }
          var isNewlyCreatedProject by remember { mutableStateOf(false) }

          var currentScreen by remember { mutableStateOf(AppScreen.START) }

          // Camera permission launcher
          val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { isGranted ->
            if (isGranted) {
              tracker.setDemoMode(false)
              currentScreen = AppScreen.WORKSPACE
              voice.speak("실시간 3D 스캔 모드를 시작합니다. 주변을 카메라로 천천히 비추세요.")
            } else {
              // Permission denied: fallback to Demo mode with voice notice
              tracker.setDemoMode(true)
              currentScreen = AppScreen.WORKSPACE
              voice.speak("카메라 권한이 허용되지 않아 가상 데모 모드로 실행합니다.")
            }
          }

          when (currentScreen) {
            AppScreen.START -> {
              MainStartScreen(
                onNewProjectClick = {
                  currentScreen = AppScreen.NEW_PROJECT
                },
                onStartDemoMode = {
                  val demoProject = projectRepository.getProjectById("project_sejong_default")
                    ?: projectRepository.projects.value.firstOrNull()
                  currentProject = demoProject
                  tracker.setDemoMode(true)
                  isNewlyCreatedProject = false
                  currentScreen = AppScreen.WORKSPACE
                  voice.speak("SpaceScan AI 가상 데모 모드를 시작합니다. 현재 위치는 2층 복도입니다.")
                },
                onOpenProject = {
                  currentScreen = AppScreen.PROJECT_LIST
                }
              )
            }

            AppScreen.NEW_PROJECT -> {
              NewProjectScreen(
                repository = projectRepository,
                onBackClick = {
                  currentScreen = AppScreen.START
                },
                onProjectCreated = { createdProject ->
                  currentProject = createdProject
                  isNewlyCreatedProject = true

                  if (createdProject.mode == "REAL") {
                    val hasPermission = ContextCompat.checkSelfPermission(
                      context,
                      Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                      tracker.setDemoMode(false)
                      currentScreen = AppScreen.WORKSPACE
                      voice.speak("${createdProject.name} 스캔을 시작합니다. 주변을 카메라로 비추세요.")
                    } else {
                      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                  } else {
                    tracker.setDemoMode(true)
                    currentScreen = AppScreen.WORKSPACE
                    voice.speak("${createdProject.name} 가상 데모 모드를 시작합니다.")
                  }
                }
              )
            }

            AppScreen.PROJECT_LIST -> {
              ProjectListScreen(
                repository = projectRepository,
                currentProjectId = currentProject?.id,
                onBackClick = {
                  currentScreen = AppScreen.START
                },
                onNewProjectClick = {
                  currentScreen = AppScreen.NEW_PROJECT
                },
                onSelectProject = { selectedProject ->
                  currentProject = selectedProject
                  isNewlyCreatedProject = false
                  tracker.setDemoMode(selectedProject.mode == "DEMO")
                  currentScreen = AppScreen.WORKSPACE
                  voice.speak("${selectedProject.name} 프로젝트를 불러왔습니다.")
                }
              )
            }

            AppScreen.WORKSPACE -> {
              ScanWorkspaceScreen(
                project = currentProject,
                tracker = tracker,
                voiceAssistant = voice,
                repository = projectRepository,
                onProjectUpdated = { updatedProject ->
                  currentProject = updatedProject
                },
                isNewlyCreated = isNewlyCreatedProject,
                onExit = {
                  currentScreen = AppScreen.START
                }
              )
            }
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    positionTracker?.release()
    voiceAssistant?.release()
  }
}
