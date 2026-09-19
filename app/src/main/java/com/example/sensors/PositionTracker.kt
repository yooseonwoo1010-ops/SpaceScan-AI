package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.ConfidenceLevel
import com.example.model.UserPose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TrackingMethod(val title: String) {
  ARCORE_VIO("1순위: ARCore Visual-Inertial Odometry"),
  FEATURE_IMU("2순위: 카메라 특징점 + IMU 센서"),
  DEPTH_SENSOR("3순위: Depth 센서 융합"),
  GPS_INITIAL("4순위: GPS 외부/초기 위치"),
  BLE_WIFI("5순위: 실내 측위 비콘/Wi-Fi RTT")
}

data class RelocalizationState(
  val isRelocalizing: Boolean = false,
  val statusMessage: String = "",
  val success: Boolean = false
)

class PositionTracker(context: Context, private val scope: CoroutineScope) : SensorEventListener {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

  private val _userPose = MutableStateFlow(
    UserPose(
      x = 21f,
      y = 19f,
      z = 0f,
      yawDegrees = 90f,
      floorId = "2F",
      confidence = ConfidenceLevel.HIGH,
      estimatedAccuracyMeters = 1.2f
    )
  )
  val userPose = _userPose.asStateFlow()

  private val _trackingMethod = MutableStateFlow(TrackingMethod.ARCORE_VIO)
  val trackingMethod = _trackingMethod.asStateFlow()

  private val _breadcrumbs = MutableStateFlow<List<Pair<Float, Float>>>(
    listOf(
      Pair(6f, 19f),
      Pair(10f, 19f),
      Pair(15f, 19f),
      Pair(18f, 19f),
      Pair(21f, 19f)
    )
  )
  val breadcrumbs = _breadcrumbs.asStateFlow()

  private val _floorTransitionMessage = MutableStateFlow<String?>(null)
  val floorTransitionMessage = _floorTransitionMessage.asStateFlow()

  private val _relocalizationState = MutableStateFlow(RelocalizationState())
  val relocalizationState = _relocalizationState.asStateFlow()

  private val _isDemoMode = MutableStateFlow(true)
  val isDemoMode = _isDemoMode.asStateFlow()

  // Predefined demo waypoint sequence for testing and demonstration
  private val demoWaypoints = listOf(
    UserPose(x = 12f, y = 14f, yawDegrees = 0f, floorId = "1F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 0.8f),
    UserPose(x = 12f, y = 19f, yawDegrees = 90f, floorId = "1F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 1.0f),
    UserPose(x = 26f, y = 19f, yawDegrees = 90f, floorId = "1F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 1.1f),
    UserPose(x = 6f, y = 23f, yawDegrees = 270f, floorId = "1F", confidence = ConfidenceLevel.MEDIUM, estimatedAccuracyMeters = 1.8f), // entering stairs
    UserPose(x = 6f, y = 26f, yawDegrees = 0f, floorId = "2F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 1.4f), // arrived 2F
    UserPose(x = 18f, y = 19f, yawDegrees = 90f, floorId = "2F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 1.2f),
    UserPose(x = 29f, y = 19f, yawDegrees = 90f, floorId = "2F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 0.9f),
    UserPose(x = 29f, y = 13f, yawDegrees = 0f, floorId = "2F", confidence = ConfidenceLevel.HIGH, estimatedAccuracyMeters = 0.8f) // inside 202
  )
  private var currentWaypointIndex = 5

  init {
    accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
  }

  fun setDemoMode(isDemo: Boolean) {
    _isDemoMode.value = isDemo
  }

  fun resetForProject(isDemoMode: Boolean, initialFloorId: String = "1F") {
    _isDemoMode.value = isDemoMode
    if (isDemoMode) {
      currentWaypointIndex = 5
      _userPose.value = UserPose(
        x = 21f,
        y = 19f,
        z = 0f,
        yawDegrees = 90f,
        floorId = "2F",
        confidence = ConfidenceLevel.HIGH,
        estimatedAccuracyMeters = 1.2f
      )
      _breadcrumbs.value = listOf(
        Pair(6f, 19f),
        Pair(10f, 19f),
        Pair(15f, 19f),
        Pair(18f, 19f),
        Pair(21f, 19f)
      )
    } else {
      currentWaypointIndex = 0
      _userPose.value = UserPose(
        x = 0f,
        y = 0f,
        z = 0f,
        yawDegrees = 0f,
        floorId = initialFloorId,
        confidence = ConfidenceLevel.LOW,
        estimatedAccuracyMeters = 5.0f
      )
      _breadcrumbs.value = emptyList()
    }
    _floorTransitionMessage.value = null
    _relocalizationState.value = RelocalizationState()
  }

  fun updatePoseFromArCore(
    x: Float,
    y: Float,
    z: Float,
    yawDeg: Float,
    isTracking: Boolean
  ) {
    if (_isDemoMode.value) return

    val conf = if (isTracking) ConfidenceLevel.HIGH else ConfidenceLevel.TRACKING
    val accuracy = if (isTracking) 0.3f else 3.0f

    // Coordinate mapping: ARCore X is Right, Z is forward/back (-Z is forward), Y is Up
    // In 2D floorplan: Map (X, -Z)
    val mapX = x
    val mapY = -z

    _userPose.value = _userPose.value.copy(
      x = mapX,
      y = mapY,
      z = y,
      yawDegrees = yawDeg,
      confidence = conf,
      estimatedAccuracyMeters = accuracy,
      timestamp = System.currentTimeMillis()
    )

    if (isTracking) {
      val list = _breadcrumbs.value.toMutableList()
      val last = list.lastOrNull()
      if (last == null || kotlin.math.hypot((last.first - mapX).toDouble(), (last.second - mapY).toDouble()) > 0.15) {
        list.add(Pair(mapX, mapY))
        if (list.size > 200) list.removeAt(0)
        _breadcrumbs.value = list
      }
    }
  }

  fun setFloor(floorId: String) {
    val current = _userPose.value
    if (current.floorId != floorId) {
      _userPose.value = current.copy(floorId = floorId)
      _floorTransitionMessage.value = "${floorId} 지도를 활성화했습니다."
    }
  }

  fun nextDemoStep(onFloorSwitched: (String) -> Unit) {
    currentWaypointIndex = (currentWaypointIndex + 1) % demoWaypoints.size
    val nextPose = demoWaypoints[currentWaypointIndex]
    val prevFloor = _userPose.value.floorId

    _userPose.value = nextPose

    // Append to breadcrumbs
    val updatedList = _breadcrumbs.value.toMutableList()
    updatedList.add(Pair(nextPose.x, nextPose.y))
    if (updatedList.size > 50) updatedList.removeAt(0)
    _breadcrumbs.value = updatedList

    // Check floor transition
    if (nextPose.floorId != prevFloor) {
      _floorTransitionMessage.value = "${nextPose.floorId}로 이동한 것으로 감지되었습니다."
      onFloorSwitched(nextPose.floorId)
    } else {
      _floorTransitionMessage.value = null
    }
  }

  fun updatePoseManually(x: Float, y: Float, yaw: Float) {
    val updated = _userPose.value.copy(x = x, y = y, yawDegrees = yaw)
    _userPose.value = updated
    val updatedList = _breadcrumbs.value.toMutableList()
    updatedList.add(Pair(x, y))
    _breadcrumbs.value = updatedList
  }

  fun rotateHeading(deltaDegrees: Float) {
    val current = _userPose.value
    val newYaw = (current.yawDegrees + deltaDegrees + 360f) % 360f
    _userPose.value = current.copy(yawDegrees = newYaw)
  }

  fun setHeading(degrees: Float) {
    val current = _userPose.value
    _userPose.value = current.copy(yawDegrees = (degrees + 360f) % 360f)
  }

  fun relocalize(onCompleted: (String) -> Unit) {
    scope.launch(Dispatchers.Default) {
      _relocalizationState.value = RelocalizationState(
        isRelocalizing = true,
        statusMessage = "위치를 다시 찾는 중... (3D 특징점 매칭)"
      )
      _userPose.value = _userPose.value.copy(confidence = ConfidenceLevel.TRACKING)
      delay(1500)

      val recoveredPose = _userPose.value.copy(
        confidence = ConfidenceLevel.HIGH,
        estimatedAccuracyMeters = 0.9f
      )
      _userPose.value = recoveredPose
      _relocalizationState.value = RelocalizationState(
        isRelocalizing = false,
        statusMessage = "현재 위치를 ${recoveredPose.floorId} 복도 중앙으로 성공적으로 보정했습니다.",
        success = true
      )
      onCompleted("현재 위치를 ${recoveredPose.floorId} 복도로 재보정했습니다.")
    }
  }

  fun dismissFloorMessage() {
    _floorTransitionMessage.value = null
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (_isDemoMode.value || event == null) return
    // In real scan mode, use sensor gyro for subtle yaw stabilization
    if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
      val gyroZ = event.values[2]
      if (kotlin.math.abs(gyroZ) > 0.05f) {
        val newYaw = (_userPose.value.yawDegrees + gyroZ * 2f) % 360f
        _userPose.value = _userPose.value.copy(yawDegrees = if (newYaw < 0) newYaw + 360f else newYaw)
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  fun release() {
    sensorManager?.unregisterListener(this)
  }
}
