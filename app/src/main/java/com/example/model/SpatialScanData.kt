package com.example.model

import android.graphics.Bitmap

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

data class MeshVertex(
  val x: Float,
  val y: Float,
  val z: Float,
  val nx: Float = 0f,
  val ny: Float = 1f,
  val nz: Float = 0f,
  val colorRgb: Long = 0xFF38BDF8 // Default light electric cyan
)

data class ScanMeshTriangle(
  val v1: Int,
  val v2: Int,
  val v3: Int
)

data class ScanMesh(
  val id: String,
  val planeId: String? = null,
  val classification: PlaneClassification = PlaneClassification.UNKNOWN,
  val vertices: List<MeshVertex> = emptyList(),
  val triangles: List<ScanMeshTriangle> = emptyList(),
  val minX: Float = 0f,
  val maxX: Float = 0f,
  val minY: Float = 0f,
  val maxY: Float = 0f,
  val minZ: Float = 0f,
  val maxZ: Float = 0f
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
  val pitchDegrees: Float = 0f,
  val timestamp: Long = System.currentTimeMillis()
)

data class ScanImage(
  val id: String,
  val projectId: String,
  val scanSegmentId: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val imageUri: String = "",
  val thumbnailBitmap: Bitmap? = null,
  val width: Int = 1920,
  val height: Int = 1080,
  val worldX: Float = 0f,
  val worldY: Float = 0f,
  val worldZ: Float = 0f,
  val cameraRotationYaw: Float = 0f,
  val cameraRotationPitch: Float = 0f,
  val mapX: Float = 0f,
  val mapY: Float = 0f,
  val floorId: String = "1F",
  val roomId: String? = null,
  val roomName: String? = null,
  val qualityScore: Float = 0.92f, // 0.0 ~ 1.0
  val isHighQuality: Boolean = true,
  val hasDepth: Boolean = true,
  val fovDegrees: Float = 68f
)

data class ScanLocation(
  val id: String,
  val worldX: Float,
  val worldY: Float,
  val worldZ: Float,
  val mapX: Float,
  val mapY: Float,
  val cameraYaw: Float,
  val imageIds: List<String> = emptyList()
)

data class CameraDebugInfo(
  val previewWidth: Int = 0,
  val previewHeight: Int = 0,
  val cameraResWidth: Int = 1920,
  val cameraResHeight: Int = 1080,
  val displayRotation: Int = 0,
  val scaleType: String = "FILL",
  val viewportStatus: String = "정상 (ACTIVE)",
  val cameraFacing: String = "BACK",
  val previewState: String = "ACTIVE",
  val isSizeValid: Boolean = true,
  val layoutErrorMsg: String? = null
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
  val meshes: List<ScanMesh> = emptyList(),
  val capturedImages: List<ScanImage> = emptyList(),
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
  val totalMeshes: Int = 0,
  val totalMeshVertices: Int = 0,
  val capturedImagesCount: Int = 0,
  val depthAvailable: Boolean = false,
  val currentPose: CameraPoseData? = null,
  val trackingStatus: ArCoreTrackingStatus = ArCoreTrackingStatus.PAUSED,
  val scanProgress: Int = 0,
  val segmentsCount: Int = 0,
  val debugInfo: CameraDebugInfo = CameraDebugInfo()
)

