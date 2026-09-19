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
import com.example.model.MockSchoolBuilding
import com.example.sensors.PositionTracker
import com.example.sensors.VoiceAssistant
import com.example.ui.screens.MainStartScreen
import com.example.ui.screens.ScanWorkspaceScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
  START,
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

          var currentScreen by remember { mutableStateOf(AppScreen.START) }
          val building: com.example.model.Building = remember { MockSchoolBuilding.schoolBuilding }

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
                onStartRealScan = {
                  val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                  ) == PackageManager.PERMISSION_GRANTED

                  if (hasPermission) {
                    tracker.setDemoMode(false)
                    currentScreen = AppScreen.WORKSPACE
                    voice.speak("실시간 3D 스캔 모드를 시작합니다. 주변을 카메라로 천천히 비추세요.")
                  } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                  }
                },
                onStartDemoMode = {
                  tracker.setDemoMode(true)
                  currentScreen = AppScreen.WORKSPACE
                  voice.speak("SpaceScan AI 가상 데모 모드를 시작합니다. 현재 위치는 2층 복도입니다.")
                },
                onOpenProject = {
                  tracker.setDemoMode(true)
                  currentScreen = AppScreen.WORKSPACE
                  voice.speak("세종관 프로젝트를 불러왔습니다. 2층 지도를 확인하세요.")
                }
              )
            }

            AppScreen.WORKSPACE -> {
              ScanWorkspaceScreen(
                building = building,
                tracker = tracker,
                voiceAssistant = voice,
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
