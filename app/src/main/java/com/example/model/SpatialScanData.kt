package com.example.model

enum class ArCoreTrackingStatus(val label: String, val dots: String) {
  TRACKING("공간 추적 정상", "●●●●●"),
  PAUSED("공간 추적 중...", "●●●○○"),
  STOPPED("공간 추적 중단", "○○○○○"),
  UNAVAILABLE("ARCore 미지원", "○○○○○")
}

enum class PlaneClassification {
  FLOOR,
  CEILING,
  WALL,
  TABLE,
  SEAT,
  DOOR,
  WINDOW,
  UNKNOWN
}

data class ScanPoint(
  val x: Float,
  val y: Float, // Elevation / height
  val z: Float,
  val confidence: Float = 1.0f,
  val timestamp: Long = System.currentTimeMillis()
)

data class ScanPlane(
  val id: String,
  val typeName: String,
  val classification: PlaneClassification = PlaneClassification.UNKNOWN,
  val centerX: Float,
  val centerY: Float,
  val centerZ: Float,
  val extentX: Float,
  val extentZ: Float,
  val areaM2: Float,
  val polygonPoints: List<Point3D> = emptyList(),
  val timestamp: Long = System.currentTimeMillis()
)

data class CameraPoseData(
  val x: Float,
  val y: Float,
  val z: Float,
  val qx: Float = 0f,
  val qy: Float = 0f,
  val qz: Float = 0f,
  val qw: Float = 1f,
  val yawDegrees: Float = 0f,
  val timestamp: Long = System.currentTimeMillis()
)

data class ScanSegment(
  val id: String,
  val projectId: String,
  val floorId: String,
  val name: String,
  val startTime: Long,
  val endTime: Long,
  val startPose: CameraPoseData?,
  val endPose: CameraPoseData?,
  val points: List<ScanPoint>,
  val planes: List<ScanPlane>,
  val cameraPath: List<CameraPoseData>,
  val coveragePercent: Int
)

data class ArHardwareStatus(
  val arCoreAvailable: Boolean = false,
  val cameraPermissionGranted: Boolean = false,
  val depthSupported: Boolean = false,
  val trackingStatus: ArCoreTrackingStatus = ArCoreTrackingStatus.PAUSED,
  val statusMessage: String = "ARCore 세션 초기화 중..."
)

data class ScanSessionStats(
  val isScanning: Boolean = false,
  val scanDurationSec: Long = 0L,
  val fps: Int = 30,
  val totalPoints: Int = 0,
  val activeFramePoints: Int = 0,
  val totalPlanes: Int = 0,
  val depthAvailable: Boolean = false,
  val currentPose: CameraPoseData? = null,
  val trackingStatus: ArCoreTrackingStatus = ArCoreTrackingStatus.PAUSED,
  val scanProgress: Int = 0,
  val segmentsCount: Int = 0
)
