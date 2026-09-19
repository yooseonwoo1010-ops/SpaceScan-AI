package com.example.model

enum class ScanStatus {
  COMPLETED,
  IN_PROGRESS,
  UNSCANNED,
  RESCAN_NEEDED
}

enum class ConfidenceLevel(val label: String, val score: Float, val dots: String) {
  HIGH("높음", 0.95f, "●●●●●"),
  MEDIUM("중간", 0.65f, "●●●○○"),
  LOW("낮음", 0.35f, "●●○○○"),
  TRACKING("추적 중...", 0.10f, "○ ○ ○ ○ ○")
}

data class Point3D(
  val x: Float,
  val y: Float, // Elevation / height
  val z: Float
)

data class UserPose(
  val x: Float, // 2D floorplan coordinate X (meters)
  val y: Float, // 2D floorplan coordinate Y (meters)
  val z: Float = 0f, // vertical offset
  val yawDegrees: Float = 0f, // Orientation / azimuth
  val floorId: String = "2F",
  val confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
  val estimatedAccuracyMeters: Float = 1.2f,
  val timestamp: Long = System.currentTimeMillis()
)

data class Room(
  val id: String,
  val name: String,
  val floorId: String,
  val x: Float, // Center X in meters
  val y: Float, // Center Y in meters
  val width: Float, // Width in meters
  val height: Float, // Height in meters
  val coveragePercent: Int,
  val status: ScanStatus,
  val importance: Float = 1.0f, // 0.0 to 1.0
  val doors: List<Point3D> = emptyList(),
  val missingAreas: List<ScanMissingArea> = emptyList(),
  val isTargeted: Boolean = false
)

data class ScanMissingArea(
  val id: String,
  val roomId: String,
  val title: String,
  val description: String,
  val location: Point3D,
  val severity: String = "HIGH"
)

data class Corridor(
  val id: String,
  val name: String,
  val floorId: String,
  val startX: Float,
  val startY: Float,
  val endX: Float,
  val endY: Float,
  val width: Float,
  val coveragePercent: Int = 84,
  val status: ScanStatus = ScanStatus.IN_PROGRESS
)

data class Stair(
  val id: String,
  val name: String,
  val floorId: String,
  val x: Float,
  val y: Float,
  val connectsToFloorId: String,
  val status: ScanStatus = ScanStatus.COMPLETED
)

data class Elevator(
  val id: String,
  val name: String,
  val floorId: String,
  val x: Float,
  val y: Float,
  val status: ScanStatus = ScanStatus.COMPLETED
)

data class Restroom(
  val id: String,
  val name: String,
  val floorId: String,
  val x: Float,
  val y: Float,
  val width: Float = 4f,
  val height: Float = 4f
)

data class Floor(
  val id: String, // "B1", "1F", "2F", "3F"
  val name: String,
  val levelIndex: Int, // e.g. -1, 0, 1, 2
  val rooms: List<Room>,
  val corridors: List<Corridor>,
  val stairs: List<Stair>,
  val elevators: List<Elevator>,
  val restrooms: List<Restroom>,
  val coveragePercent: Int,
  val completedRoomsCount: Int,
  val totalRoomsCount: Int
)

data class RouteStep(
  val stepNumber: Int,
  val instruction: String,
  val distanceMeters: Float,
  val targetRoomId: String? = null,
  val targetPoint: Point3D
)

data class AiRecommendation(
  val nextTargetRoomId: String,
  val nextTargetName: String,
  val floorId: String,
  val reason: String,
  val distanceMeters: Float,
  val priorityScore: Float,
  val importance: String = "높음",
  val coveragePercent: Int,
  val routeSteps: List<RouteStep>,
  val feedbackSpeech: String
)

data class Building(
  val id: String,
  val name: String,
  val floors: List<Floor>,
  val overallCoveragePercent: Int = 78
)
