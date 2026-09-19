package com.example.model

data class Project(
  val id: String,
  val name: String,
  val buildingName: String,
  val floors: Int,
  val description: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val mode: String = "REAL", // "DEMO" | "REAL"
  val currentFloor: Int = 1,
  val scanProgress: Int = 0,
  val scans: List<String> = emptyList(),
  val floorPlans: List<String> = emptyList(),
  val rooms: List<Room> = emptyList(),
  val currentPosition: UserPose? = null,
  val status: String = "NEW", // "NEW" | "SCANNING" | "COMPLETED"
  val segments: List<ScanSegment> = emptyList(),
  val building: Building? = null
)
