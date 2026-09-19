package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Building
import com.example.model.Corridor
import com.example.model.Elevator
import com.example.model.Floor
import com.example.model.MockSchoolBuilding
import com.example.model.Point3D
import com.example.model.Project
import com.example.model.Restroom
import com.example.model.Room
import com.example.model.ScanStatus
import com.example.model.Stair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val context: Context) {

  private val prefs = context.getSharedPreferences("spacescan_projects_storage", Context.MODE_PRIVATE)
  private val PREFS_KEY_PROJECTS = "saved_projects_json"

  // In-memory cache of Building objects to avoid re-generating
  private val buildingCache = mutableMapOf<String, Building>()

  private val _projects = MutableStateFlow<List<Project>>(emptyList())
  val projects = _projects.asStateFlow()

  init {
    loadProjects()
  }

  fun loadProjects(): List<Project> {
    val loaded = mutableListOf<Project>()

    // 1. Default system project (세종관 본관 - 데모 전용)
    val sejongProject = createDemoProject()
    sejongProject.building?.let { buildingCache[sejongProject.id] = it }
    loaded.add(sejongProject)

    // 2. User created projects from SharedPreferences
    val jsonString = prefs.getString(PREFS_KEY_PROJECTS, null)
    if (!jsonString.isNullOrEmpty()) {
      try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val id = obj.getString("id")
          val name = obj.getString("name")
          val buildingName = obj.getString("buildingName")
          val floors = obj.getInt("floors")
          val description = obj.optString("description", "")
          val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
          val mode = obj.optString("mode", "REAL")
          val currentFloor = obj.optInt("currentFloor", 1)
          val scanProgress = obj.optInt("scanProgress", 0)
          val status = obj.optString("status", "NEW")

          val bld = buildingCache.getOrPut(id) {
            createFreshBuilding(id, buildingName, floors)
          }

          val project = Project(
            id = id,
            name = name,
            buildingName = buildingName,
            floors = floors,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mode = mode,
            currentFloor = currentFloor,
            scanProgress = scanProgress,
            status = status,
            building = bld
          )
          loaded.add(project)
        }
      } catch (e: Exception) {
        Log.e("Project", "Failed to parse saved projects", e)
      }
    }

    _projects.value = loaded
    return loaded
  }

  fun saveProject(project: Project): Boolean {
    return try {
      Log.d("Project", "[Project] project saved")
      val currentList = _projects.value.toMutableList()
      val index = currentList.indexOfFirst { it.id == project.id }
      if (index >= 0) {
        currentList[index] = project
      } else {
        currentList.add(project)
      }

      // Cache building object
      project.building?.let {
        buildingCache[project.id] = it
      }

      // Persist user-created projects to SharedPreferences
      val userProjects = currentList.filter { it.id != "project_sejong_default" }
      val jsonArray = JSONArray()
      for (p in userProjects) {
        val obj = JSONObject().apply {
          put("id", p.id)
          put("name", p.name)
          put("buildingName", p.buildingName)
          put("floors", p.floors)
          put("description", p.description)
          put("createdAt", p.createdAt)
          put("updatedAt", p.updatedAt)
          put("mode", p.mode)
          put("currentFloor", p.currentFloor)
          put("scanProgress", p.scanProgress)
          put("status", p.status)
        }
        jsonArray.put(obj)
      }
      prefs.edit().putString(PREFS_KEY_PROJECTS, jsonArray.toString()).apply()

      _projects.value = currentList
      Log.d("Project", "[Project] project list updated")
      true
    } catch (e: Exception) {
      Log.e("Project", "[Project] create failed in saveProject", e)
      false
    }
  }

  fun getProjectById(id: String): Project? {
    return _projects.value.find { it.id == id }
  }

  fun deleteProject(id: String) {
    val currentList = _projects.value.toMutableList()
    currentList.removeAll { it.id == id }
    buildingCache.remove(id)

    val userProjects = currentList.filter { it.id != "project_sejong_default" }
    val jsonArray = JSONArray()
    for (p in userProjects) {
      val obj = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("buildingName", p.buildingName)
        put("floors", p.floors)
        put("description", p.description)
        put("createdAt", p.createdAt)
        put("updatedAt", p.updatedAt)
        put("mode", p.mode)
        put("currentFloor", p.currentFloor)
        put("scanProgress", p.scanProgress)
        put("status", p.status)
      }
      jsonArray.put(obj)
    }
    prefs.edit().putString(PREFS_KEY_PROJECTS, jsonArray.toString()).apply()
    _projects.value = currentList
  }

  fun createDemoProject(): Project {
    val sejongBuilding = loadDemoBuilding()
    return Project(
      id = "project_sejong_default",
      name = "세종관 3D 스캔 (데모)",
      buildingName = "세종관 (본관)",
      floors = 4,
      description = "학교 본관 4개 층 3D 공간 스캔 및 2D 실내지도 통합 관리",
      createdAt = 1726700000000L,
      updatedAt = System.currentTimeMillis(),
      mode = "DEMO",
      currentFloor = 2,
      scanProgress = 78,
      status = "SCANNING",
      building = sejongBuilding
    )
  }

  fun loadDemoBuilding(): Building {
    return MockSchoolBuilding.schoolBuilding
  }

  fun createFreshBuilding(projectId: String, buildingName: String, floorCount: Int): Building {
    return Companion.createFreshBuilding(projectId, buildingName, floorCount)
  }

  companion object {
    /**
     * Generates a completely fresh Building structure with 0% scan progress and NO rooms/corridors
     * so a new project has completely isolated, empty state.
     */
    fun createFreshBuilding(projectId: String, buildingName: String, floorCount: Int): Building {
      val floorList = mutableListOf<Floor>()
      val safeFloorCount = floorCount.coerceIn(1, 20)

      for (floorNum in 1..safeFloorCount) {
        val floorId = "${floorNum}F"
        floorList.add(
          Floor(
            id = floorId,
            name = "${floorNum}층",
            levelIndex = floorNum - 1,
            rooms = emptyList(),
            corridors = emptyList(),
            stairs = emptyList(),
            elevators = emptyList(),
            restrooms = emptyList(),
            coveragePercent = 0,
            completedRoomsCount = 0,
            totalRoomsCount = 0
          )
        )
      }

      return Building(
        id = "bld_$projectId",
        name = buildingName,
        floors = floorList,
        overallCoveragePercent = 0
      )
    }
  }
}
