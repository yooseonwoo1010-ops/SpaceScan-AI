package com.example.model

object MockSchoolBuilding {

  fun createDefaultBuilding(): Building {
    val floor1Rooms = listOf(
      Room(
        id = "101",
        name = "101호 (일반교실)",
        floorId = "1F",
        x = 12f,
        y = 12f,
        width = 10f,
        height = 8f,
        coveragePercent = 96,
        status = ScanStatus.COMPLETED,
        importance = 0.8f,
        doors = listOf(Point3D(12f, 0f, 16f))
      ),
      Room(
        id = "102",
        name = "102호 (과학실)",
        floorId = "1F",
        x = 26f,
        y = 12f,
        width = 12f,
        height = 8f,
        coveragePercent = 71,
        status = ScanStatus.IN_PROGRESS,
        importance = 0.9f,
        doors = listOf(Point3D(26f, 0f, 16f))
      ),
      Room(
        id = "103",
        name = "103호 (컴퓨터실)",
        floorId = "1F",
        x = 42f,
        y = 12f,
        width = 14f,
        height = 8f,
        coveragePercent = 38,
        status = ScanStatus.UNSCANNED,
        importance = 0.95f,
        doors = listOf(Point3D(42f, 0f, 16f)),
        missingAreas = listOf(
          ScanMissingArea(
            id = "m103_1",
            roomId = "103",
            title = "103호 안쪽 벽면",
            description = "3D 포인트 클라우드 밀도 부족 (미스캔 62%)",
            location = Point3D(42f, 2.5f, 10f)
          )
        )
      )
    )

    val floor2Rooms = listOf(
      Room(
        id = "201",
        name = "201호",
        floorId = "2F",
        x = 14f,
        y = 12f,
        width = 11f,
        height = 8.5f,
        coveragePercent = 88,
        status = ScanStatus.RESCAN_NEEDED,
        importance = 0.85f,
        doors = listOf(Point3D(14f, 0f, 16.25f)),
        missingAreas = listOf(
          ScanMissingArea(
            id = "m201_north",
            roomId = "201",
            title = "201호 북쪽 벽",
            description = "스캔 품질이 낮습니다. 천장 및 모서리 데이터 부족",
            location = Point3D(14f, 2.4f, 8f),
            severity = "HIGH"
          )
        )
      ),
      Room(
        id = "202",
        name = "202호",
        floorId = "2F",
        x = 29f,
        y = 12f,
        width = 12f,
        height = 8.5f,
        coveragePercent = 74,
        status = ScanStatus.IN_PROGRESS,
        importance = 0.9f,
        doors = listOf(Point3D(29f, 0f, 16.25f))
      ),
      Room(
        id = "203",
        name = "203호 (도서실)",
        floorId = "2F",
        x = 44f,
        y = 12f,
        width = 12f,
        height = 8.5f,
        coveragePercent = 45,
        status = ScanStatus.UNSCANNED,
        importance = 0.75f,
        doors = listOf(Point3D(44f, 0f, 16.25f))
      )
    )

    val floor3Rooms = listOf(
      Room(
        id = "301",
        name = "301호 (음악실)",
        floorId = "3F",
        x = 14f,
        y = 12f,
        width = 11f,
        height = 8.5f,
        coveragePercent = 78,
        status = ScanStatus.IN_PROGRESS,
        importance = 0.8f
      ),
      Room(
        id = "302",
        name = "302호 (미술실)",
        floorId = "3F",
        x = 29f,
        y = 12f,
        width = 12f,
        height = 8.5f,
        coveragePercent = 45,
        status = ScanStatus.UNSCANNED,
        importance = 0.92f
      ),
      Room(
        id = "303",
        name = "303호 (세미나실)",
        floorId = "3F",
        x = 44f,
        y = 12f,
        width = 12f,
        height = 8.5f,
        coveragePercent = 60,
        status = ScanStatus.IN_PROGRESS,
        importance = 0.7f
      )
    )

    val floorB1Rooms = listOf(
      Room(
        id = "B101",
        name = "B1 기계실",
        floorId = "B1",
        x = 16f,
        y = 12f,
        width = 14f,
        height = 8f,
        coveragePercent = 90,
        status = ScanStatus.COMPLETED
      ),
      Room(
        id = "B102",
        name = "B1 체육창고",
        floorId = "B1",
        x = 36f,
        y = 12f,
        width = 16f,
        height = 8f,
        coveragePercent = 50,
        status = ScanStatus.IN_PROGRESS
      )
    )

    val floor1 = Floor(
      id = "1F",
      name = "1층",
      levelIndex = 1,
      rooms = floor1Rooms,
      corridors = listOf(
        Corridor("c1", "1F 중앙 복도", "1F", 4f, 19f, 52f, 19f, 4f, 94, ScanStatus.COMPLETED)
      ),
      stairs = listOf(
        Stair("st1_up", "1F 계단", "1F", 6f, 26f, "2F", ScanStatus.COMPLETED)
      ),
      elevators = listOf(
        Elevator("el1", "1F 엘리베이터", "1F", 36f, 26f, ScanStatus.COMPLETED)
      ),
      restrooms = listOf(
        Restroom("rr1", "1F 화장실", "1F", 46f, 26f)
      ),
      coveragePercent = 94,
      completedRoomsCount = 28,
      totalRoomsCount = 30
    )

    val floor2 = Floor(
      id = "2F",
      name = "2층",
      levelIndex = 2,
      rooms = floor2Rooms,
      corridors = listOf(
        Corridor("c2", "2F 중앙 복도", "2F", 4f, 19f, 52f, 19f, 4f, 74, ScanStatus.IN_PROGRESS)
      ),
      stairs = listOf(
        Stair("st2_down", "2F 하행 계단", "2F", 6f, 26f, "1F", ScanStatus.COMPLETED),
        Stair("st2_up", "2F 상행 계단", "2F", 12f, 26f, "3F", ScanStatus.COMPLETED)
      ),
      elevators = listOf(
        Elevator("el2", "2F 엘리베이터", "2F", 36f, 26f, ScanStatus.COMPLETED)
      ),
      restrooms = listOf(
        Restroom("rr2", "2F 화장실", "2F", 46f, 26f)
      ),
      coveragePercent = 72,
      completedRoomsCount = 23,
      totalRoomsCount = 32
    )

    val floor3 = Floor(
      id = "3F",
      name = "3층",
      levelIndex = 3,
      rooms = floor3Rooms,
      corridors = listOf(
        Corridor("c3", "3F 중앙 복도", "3F", 4f, 19f, 52f, 19f, 4f, 61, ScanStatus.IN_PROGRESS)
      ),
      stairs = listOf(
        Stair("st3_down", "3F 하행 계단", "3F", 12f, 26f, "2F", ScanStatus.COMPLETED)
      ),
      elevators = listOf(
        Elevator("el3", "3F 엘리베이터", "3F", 36f, 26f, ScanStatus.COMPLETED)
      ),
      restrooms = listOf(
        Restroom("rr3", "3F 화장실", "3F", 46f, 26f)
      ),
      coveragePercent = 61,
      completedRoomsCount = 18,
      totalRoomsCount = 30
    )

    val floorB1 = Floor(
      id = "B1",
      name = "지하 1층",
      levelIndex = 0,
      rooms = floorB1Rooms,
      corridors = listOf(
        Corridor("cB1", "B1 복도", "B1", 8f, 19f, 48f, 19f, 4f, 70, ScanStatus.IN_PROGRESS)
      ),
      stairs = listOf(
        Stair("stB1_up", "B1 계단", "B1", 6f, 26f, "1F", ScanStatus.COMPLETED)
      ),
      elevators = listOf(
        Elevator("elB1", "B1 엘리베이터", "B1", 36f, 26f, ScanStatus.COMPLETED)
      ),
      restrooms = emptyList(),
      coveragePercent = 70,
      completedRoomsCount = 6,
      totalRoomsCount = 8
    )

    return Building(
      id = "school_main",
      name = "세종관 (본관)",
      floors = listOf(floor3, floor2, floor1, floorB1),
      overallCoveragePercent = 78
    )
  }

  val schoolBuilding: Building by lazy { createDefaultBuilding() }

  // Pre-calculated AI Recommendations depending on current floor and position
  fun calculateRecommendation(userPose: UserPose, building: Building): AiRecommendation {
    val currentFloor = building.floors.find { it.id == userPose.floorId } ?: building.floors[1]
    
    // Find room with highest priority score:
    // SCAN_PRIORITY_SCORE = unscanned + importance + distance + ...
    val candidateRooms = currentFloor.rooms.sortedByDescending { room ->
      val unscannedFactor = (100 - room.coveragePercent) / 100f
      val dist = kotlin.math.hypot(room.x - userPose.x, room.y - userPose.y)
      val distFactor = (1f / (1f + dist * 0.1f))
      (unscannedFactor * 1.5f) + (room.importance * 1.2f) + (distFactor * 0.8f)
    }

    val targetRoom = candidateRooms.firstOrNull() ?: currentFloor.rooms.first()
    val dist = kotlin.math.hypot(targetRoom.x - userPose.x, targetRoom.y - userPose.y)
    val formattedDist = String.format("%.1f", dist.coerceAtLeast(4f)).toFloat()

    val steps = listOf(
      RouteStep(
        stepNumber = 1,
        instruction = "복도 직진 (${(formattedDist * 0.4f).toInt()}m)",
        distanceMeters = formattedDist * 0.4f,
        targetPoint = Point3D(userPose.x + 5f, 0f, 19f)
      ),
      RouteStep(
        stepNumber = 2,
        instruction = "${targetRoom.name} 진입 (${(formattedDist * 0.6f).toInt()}m)",
        distanceMeters = formattedDist * 0.6f,
        targetRoomId = targetRoom.id,
        targetPoint = Point3D(targetRoom.x, 0f, targetRoom.y)
      ),
      RouteStep(
        stepNumber = 3,
        instruction = "${targetRoom.name} 내부 벽면 및 천장 360° 스캔",
        distanceMeters = 0f,
        targetRoomId = targetRoom.id,
        targetPoint = Point3D(targetRoom.x, 0f, targetRoom.y)
      )
    )

    val reason = if (targetRoom.missingAreas.isNotEmpty()) {
      "미스캔 영역이 감지되었으며 스캔 품질이 부족합니다."
    } else if (dist < 15f) {
      "현재 위치와 가깝고 스캔 커버리지(${targetRoom.coveragePercent}%)가 낮습니다."
    } else {
      "공간 중요도가 높고 연결성 확보가 필요한 핵심 구역입니다."
    }

    val speech = if (targetRoom.missingAreas.isNotEmpty()) {
      "${targetRoom.name}의 스캔 품질이 낮습니다. 재스캔을 권장합니다."
    } else {
      "앞쪽 ${targetRoom.name}을 스캔하세요. 약 ${formattedDist.toInt()}m 거리입니다."
    }

    return AiRecommendation(
      nextTargetRoomId = targetRoom.id,
      nextTargetName = targetRoom.name,
      floorId = currentFloor.id,
      reason = reason,
      distanceMeters = formattedDist,
      priorityScore = 0.92f,
      importance = if (targetRoom.importance > 0.8f) "높음" else "보통",
      coveragePercent = targetRoom.coveragePercent,
      routeSteps = steps,
      feedbackSpeech = speech
    )
  }
}
