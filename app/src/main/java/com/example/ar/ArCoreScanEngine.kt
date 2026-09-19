package com.example.ar

import android.app.Activity
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.model.ArCoreTrackingStatus
import com.example.model.ArHardwareStatus
import com.example.model.CameraPoseData
import com.example.model.PlaneClassification
import com.example.model.Point3D
import com.example.model.ScanPlane
import com.example.model.ScanPoint
import com.example.model.ScanSegment
import com.example.model.ScanSessionStats
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.PointCloud
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ArCoreScanEngine(
  private val context: Context,
  private val scope: CoroutineScope
) : GLSurfaceView.Renderer {

  private val TAG = "ArCoreScanEngine"

  // ARCore Session
  private var session: Session? = null
  private var isInstallRequested = false
  private var cameraTextureId = -1

  // Status Flows
  private val _hardwareStatus = MutableStateFlow(ArHardwareStatus())
  val hardwareStatus = _hardwareStatus.asStateFlow()

  private val _sessionStats = MutableStateFlow(ScanSessionStats())
  val sessionStats = _sessionStats.asStateFlow()

  private val _accumulatedPoints = MutableStateFlow<List<ScanPoint>>(emptyList())
  val accumulatedPoints = _accumulatedPoints.asStateFlow()

  private val _detectedPlanes = MutableStateFlow<List<ScanPlane>>(emptyList())
  val detectedPlanes = _detectedPlanes.asStateFlow()

  private val _cameraPath = MutableStateFlow<List<CameraPoseData>>(emptyList())
  val cameraPath = _cameraPath.asStateFlow()

  private val _currentFramePoints = MutableStateFlow<List<ScanPoint>>(emptyList())
  val currentFramePoints = _currentFramePoints.asStateFlow()

  // Scanning Session State
  private val isScanning = AtomicBoolean(false)
  private var currentProjectId: String = ""
  private var currentFloorId: String = "1F"
  private var scanStartTime: Long = 0L
  private var timerJob: Job? = null

  // Spatial Voxel Grid for point cloud deduplication & memory efficiency (resolution: 4cm)
  private val voxelGrid = mutableSetOf<Long>()
  private val pointsList = mutableListOf<ScanPoint>()
  private val pathList = mutableListOf<CameraPoseData>()
  private val planesMap = mutableMapOf<String, ScanPlane>()
  private val exploredGridCells = mutableSetOf<Long>() // 50cm 2D cells for true coverage calculation

  // Frame timing & FPS
  private var lastFrameTimeNs = System.nanoTime()
  private var frameCount = 0
  private var lastFpsCalcTimeMs = System.currentTimeMillis()
  private var currentFps = 30

  // Fallback / Demo Mode flag
  private var isDemoFallback = false

  init {
    checkArCoreAvailability()
  }

  fun checkArCoreAvailability() {
    try {
      val availability = ArCoreApk.getInstance().checkAvailability(context)
      if (availability.isTransient) {
        // Re-check shortly
        scope.launch(Dispatchers.Main) {
          delay(500)
          checkArCoreAvailability()
        }
        return
      }

      val isSupported = availability.isSupported
      _hardwareStatus.value = _hardwareStatus.value.copy(
        arCoreAvailable = isSupported,
        statusMessage = when {
          isSupported -> "ARCore 공간 추적 엔진 사용 가능"
          availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> "이 기기는 ARCore를 지원하지 않습니다."
          else -> "ARCore 설치 및 확인이 필요합니다."
        }
      )
    } catch (e: Exception) {
      Log.w(TAG, "ARCore availability check failed: ${e.message}")
      _hardwareStatus.value = _hardwareStatus.value.copy(
        arCoreAvailable = false,
        statusMessage = "ARCore 상태 확인 실패 (에뮬레이터 또는 미지원 기기)"
      )
    }
  }

  fun setupSession(activity: Activity): Boolean {
    if (session != null) return true

    var exception: Exception? = null
    var message: String? = null

    try {
      if (ArCoreApk.getInstance().requestInstall(activity, !isInstallRequested) == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
        isInstallRequested = true
        return false
      }

      val newSession = Session(activity)
      val config = Config(newSession).apply {
        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        focusMode = Config.FocusMode.AUTO

        // Check & configure Depth API if supported
        if (newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
          depthMode = Config.DepthMode.AUTOMATIC
          _hardwareStatus.value = _hardwareStatus.value.copy(depthSupported = true)
        } else {
          depthMode = Config.DepthMode.DISABLED
          _hardwareStatus.value = _hardwareStatus.value.copy(depthSupported = false)
        }
      }

      newSession.configure(config)
      session = newSession

      _hardwareStatus.value = _hardwareStatus.value.copy(
        arCoreAvailable = true,
        cameraPermissionGranted = true,
        statusMessage = "ARCore 세션 생성 완료"
      )
      Log.i(TAG, "ARCore session created successfully")
      return true
    } catch (e: UnavailableArcoreNotInstalledException) {
      message = "Google Play Services for AR 설치가 필요합니다."
      exception = e
    } catch (e: UnavailableUserDeclinedInstallationException) {
      message = "ARCore 설치가 거부되었습니다."
      exception = e
    } catch (e: UnavailableApkTooOldException) {
      message = "ARCore 최신 버전 업데이트가 필요합니다."
      exception = e
    } catch (e: UnavailableSdkTooOldException) {
      message = "앱 업데이트가 필요합니다."
      exception = e
    } catch (e: UnavailableDeviceNotCompatibleException) {
      message = "이 기기에서 ARCore를 사용할 수 없습니다."
      exception = e
    } catch (e: Exception) {
      message = "ARCore 세션 초기화 실패: ${e.message}"
      exception = e
    }

    Log.e(TAG, "Failed to create ARCore session: $message", exception)
    _hardwareStatus.value = _hardwareStatus.value.copy(
      arCoreAvailable = false,
      statusMessage = message ?: "ARCore를 사용할 수 없습니다."
    )
    return false
  }

  fun resume(activity: Activity) {
    if (session == null) {
      setupSession(activity)
    }

    try {
      session?.resume()
      Log.i(TAG, "ARCore session resumed")
    } catch (e: CameraNotAvailableException) {
      Log.e(TAG, "Camera not available during resume", e)
      _hardwareStatus.value = _hardwareStatus.value.copy(
        statusMessage = "카메라를 사용할 수 없습니다. 권한을 확인하세요."
      )
      session = null
    } catch (e: Exception) {
      Log.e(TAG, "Exception during session resume", e)
    }
  }

  fun pause() {
    try {
      session?.pause()
      Log.i(TAG, "ARCore session paused")
    } catch (e: Exception) {
      Log.e(TAG, "Exception during session pause", e)
    }
  }

  fun destroy() {
    stopScan()
    try {
      session?.close()
      session = null
    } catch (e: Exception) {
      Log.e(TAG, "Exception during session close", e)
    }
  }

  fun startScan(projectId: String, floorId: String) {
    if (isScanning.get()) return

    currentProjectId = projectId
    currentFloorId = floorId
    scanStartTime = System.currentTimeMillis()
    isScanning.set(true)

    timerJob?.cancel()
    timerJob = scope.launch(Dispatchers.Default) {
      while (isActive && isScanning.get()) {
        delay(1000)
        val duration = (System.currentTimeMillis() - scanStartTime) / 1000
        _sessionStats.value = _sessionStats.value.copy(
          isScanning = true,
          scanDurationSec = duration
        )
      }
    }

    _sessionStats.value = _sessionStats.value.copy(
      isScanning = true,
      scanDurationSec = 0L
    )
    Log.i(TAG, "Spatial Scan Started for project: $projectId, floor: $floorId")
  }

  fun stopScan(): ScanSegment? {
    if (!isScanning.getAndSet(false)) return null

    timerJob?.cancel()
    val endTime = System.currentTimeMillis()

    val capturedPoints = pointsList.toList()
    val capturedPlanes = planesMap.values.toList()
    val capturedPath = pathList.toList()
    val finalCoverage = calculateRealProgress(capturedPoints.size, capturedPlanes.size, exploredGridCells.size)

    val segment = ScanSegment(
      id = "seg_${UUID.randomUUID().toString().take(8)}",
      projectId = currentProjectId,
      floorId = currentFloorId,
      name = "${currentFloorId} 스캔 세그먼트 #${_sessionStats.value.segmentsCount + 1}",
      startTime = scanStartTime,
      endTime = endTime,
      startPose = capturedPath.firstOrNull(),
      endPose = capturedPath.lastOrNull(),
      points = capturedPoints,
      planes = capturedPlanes,
      cameraPath = capturedPath,
      coveragePercent = finalCoverage
    )

    _sessionStats.value = _sessionStats.value.copy(
      isScanning = false,
      segmentsCount = _sessionStats.value.segmentsCount + 1,
      scanProgress = finalCoverage
    )

    Log.i(TAG, "Spatial Scan Stopped. Created segment with ${capturedPoints.size} points, ${capturedPlanes.size} planes, coverage: $finalCoverage%")
    return segment
  }

  fun resetProjectScanData(floorId: String = "1F") {
    isScanning.set(false)
    timerJob?.cancel()
    currentFloorId = floorId
    voxelGrid.clear()
    pointsList.clear()
    pathList.clear()
    planesMap.clear()
    exploredGridCells.clear()

    _accumulatedPoints.value = emptyList()
    _currentFramePoints.value = emptyList()
    _detectedPlanes.value = emptyList()
    _cameraPath.value = emptyList()

    _sessionStats.value = ScanSessionStats(
      isScanning = false,
      scanDurationSec = 0L,
      fps = 30,
      totalPoints = 0,
      activeFramePoints = 0,
      totalPlanes = 0,
      depthAvailable = _hardwareStatus.value.depthSupported,
      currentPose = null,
      trackingStatus = ArCoreTrackingStatus.PAUSED,
      scanProgress = 0,
      segmentsCount = 0
    )
  }

  // GLSurfaceView.Renderer implementation
  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glClearColor(0.05f, 0.07f, 0.12f, 1.0f)
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    cameraTextureId = textures[0]
    GLES20.glBindTexture(0x8D65, cameraTextureId) // GL_TEXTURE_EXTERNAL_OES
    GLES20.glTexParameteri(0x8D65, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(0x8D65, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(0x8D65, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
    GLES20.glTexParameteri(0x8D65, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

    session?.setCameraTextureName(cameraTextureId)
    initBackgroundRenderer()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    session?.setDisplayGeometry(0, width, height)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    val currentSession = session ?: return

    try {
      if (cameraTextureId != -1) {
        currentSession.setCameraTextureName(cameraTextureId)
      }

      val frame = currentSession.update()
      val camera = frame.camera

      // Calculate real FPS
      val nowMs = System.currentTimeMillis()
      frameCount++
      if (nowMs - lastFpsCalcTimeMs >= 1000) {
        currentFps = frameCount
        frameCount = 0
        lastFpsCalcTimeMs = nowMs
      }

      // 1. Process Real Tracking State
      val trackingState = camera.trackingState
      val trackingStatus = when (trackingState) {
        TrackingState.TRACKING -> ArCoreTrackingStatus.TRACKING
        TrackingState.PAUSED -> ArCoreTrackingStatus.PAUSED
        TrackingState.STOPPED -> ArCoreTrackingStatus.STOPPED
      }

      // 2. Process Camera Pose
      val pose = camera.displayOrientedPose
      val translation = pose.translation
      val rotation = pose.rotationQuaternion

      // Calculate yaw angle in degrees from quaternion
      val qx = rotation[0]
      val qy = rotation[1]
      val qz = rotation[2]
      val qw = rotation[3]
      val sinyCosp = 2f * (qw * qy + qx * qz)
      val cosyCosp = 1f - 2f * (qy * qy + qz * qz)
      val yawRad = atan2(sinyCosp.toDouble(), cosyCosp.toDouble()).toFloat()
      val yawDeg = (Math.toDegrees(yawRad.toDouble()).toFloat() + 360f) % 360f

      val cameraPoseData = CameraPoseData(
        x = translation[0],
        y = translation[1],
        z = translation[2],
        qx = qx,
        qy = qy,
        qz = qz,
        qw = qw,
        yawDegrees = yawDeg,
        timestamp = System.currentTimeMillis()
      )

      // 3. Update Camera Path if moved significantly (> 15cm or > 5 deg)
      var pathUpdated = false
      val lastPose = pathList.lastOrNull()
      if (lastPose == null || distanceBetween(lastPose, cameraPoseData) > 0.15f || kotlin.math.abs(lastPose.yawDegrees - cameraPoseData.yawDegrees) > 5f) {
        if (isScanning.get() && trackingState == TrackingState.TRACKING) {
          pathList.add(cameraPoseData)
          pathUpdated = true
          // Mark 2D grid exploration (50cm cell)
          val cellKey = getGridCellKey(cameraPoseData.x, cameraPoseData.z, 0.5f)
          exploredGridCells.add(cellKey)
        }
      }

      // 4. Extract Real Point Cloud from Frame
      var newPointsAdded = 0
      val framePoints = mutableListOf<ScanPoint>()
      try {
        val pointCloud: PointCloud = frame.acquirePointCloud()
        val pointsBuffer: FloatBuffer = pointCloud.points
        val pointCount = pointsBuffer.remaining() / 4

        for (i in 0 until pointCount) {
          val px = pointsBuffer.get(i * 4 + 0)
          val py = pointsBuffer.get(i * 4 + 1)
          val pz = pointsBuffer.get(i * 4 + 2)
          val confidence = pointsBuffer.get(i * 4 + 3)

          val scanPoint = ScanPoint(px, py, pz, confidence)
          framePoints.add(scanPoint)

          if (isScanning.get() && trackingState == TrackingState.TRACKING) {
            // Voxel filtering (4cm voxel)
            val voxelKey = getVoxelKey(px, py, pz, 0.04f)
            if (voxelGrid.add(voxelKey)) {
              pointsList.add(scanPoint)
              newPointsAdded++
            }
          }
        }
        pointCloud.release()
      } catch (e: Exception) {
        // Point cloud acquire safety
      }

      // 5. Extract Real Detected Planes from Session
      var planesUpdated = false
      val allPlanes = currentSession.getAllTrackables(Plane::class.java)
      for (plane in allPlanes) {
        if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
          val centerPose = plane.centerPose
          val polygon = plane.polygon
          val polyPoints = mutableListOf<Point3D>()
          val count = polygon.remaining() / 2

          for (p in 0 until count) {
            val lx = polygon.get(p * 2 + 0)
            val lz = polygon.get(p * 2 + 1)
            // Transform local plane vertex to world coordinate
            val worldVertex = centerPose.transformPoint(floatArrayOf(lx, 0f, lz))
            polyPoints.add(Point3D(worldVertex[0], worldVertex[1], worldVertex[2]))
          }

          val classification = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneClassification.FLOOR
            Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneClassification.CEILING
            Plane.Type.VERTICAL -> PlaneClassification.WALL
            else -> PlaneClassification.UNKNOWN
          }

          val scanPlane = ScanPlane(
            id = "pl_${plane.hashCode()}",
            typeName = plane.type.name,
            classification = classification,
            centerX = centerPose.tx(),
            centerY = centerPose.ty(),
            centerZ = centerPose.tz(),
            extentX = plane.extentX,
            extentZ = plane.extentZ,
            areaM2 = plane.extentX * plane.extentZ,
            polygonPoints = polyPoints
          )

          planesMap[scanPlane.id] = scanPlane
          planesUpdated = true
        }
      }

      // 6. Calculate Real Progress
      val currentProgress = if (isScanning.get()) {
        calculateRealProgress(pointsList.size, planesMap.size, exploredGridCells.size)
      } else {
        _sessionStats.value.scanProgress
      }

      // 7. Render background camera video feed to GL
      drawBackground(frame)

      // 8. Update Kotlin StateFlows
      _currentFramePoints.value = framePoints.take(200)
      if (newPointsAdded > 0) {
        _accumulatedPoints.value = pointsList.toList()
      }
      if (planesUpdated) {
        _detectedPlanes.value = planesMap.values.toList()
      }
      if (pathUpdated) {
        _cameraPath.value = pathList.toList()
      }

      _sessionStats.value = _sessionStats.value.copy(
        fps = currentFps,
        totalPoints = pointsList.size,
        activeFramePoints = framePoints.size,
        totalPlanes = planesMap.size,
        depthAvailable = _hardwareStatus.value.depthSupported,
        currentPose = cameraPoseData,
        trackingStatus = trackingStatus,
        scanProgress = currentProgress
      )

    } catch (e: Exception) {
      Log.e(TAG, "Error in onDrawFrame: ${e.message}")
    }
  }

  private fun distanceBetween(a: CameraPoseData, b: CameraPoseData): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
  }

  private fun getVoxelKey(x: Float, y: Float, z: Float, resolution: Float): Long {
    val ix = (x / resolution).toInt()
    val iy = (y / resolution).toInt()
    val iz = (z / resolution).toInt()
    return (ix.toLong() and 0x1FFFFFL) or
      ((iy.toLong() and 0x1FFFFFL) shl 21) or
      ((iz.toLong() and 0x1FFFFFL) shl 42)
  }

  private fun getGridCellKey(x: Float, z: Float, resolution: Float): Long {
    val ix = (x / resolution).toInt()
    val iz = (z / resolution).toInt()
    return (ix.toLong() and 0xFFFFFFFFL) or ((iz.toLong() and 0xFFFFFFFFL) shl 32)
  }

  /**
   * Real progress calculation based on genuine spatial data:
   * Starts strictly at 0%.
   * Increments as real points, planes, and explored floor space are recorded.
   */
  private fun calculateRealProgress(pointCount: Int, planeCount: Int, exploredCells: Int): Int {
    if (pointCount == 0 && planeCount == 0 && exploredCells == 0) return 0
    // Weighted scoring:
    // - 50cm explored cells (max ~80 cells = 40m²) -> up to 50%
    // - Detected planes (floors + walls, ~10 planes) -> up to 30%
    // - Point Cloud density (~3000 points) -> up to 20%
    val cellScore = (exploredCells * 1.25f).coerceAtMost(50f)
    val planeScore = (planeCount * 3.0f).coerceAtMost(30f)
    val pointScore = (pointCount / 150f).coerceAtMost(20f)
    return (cellScore + planeScore + pointScore).toInt().coerceIn(0, 100)
  }

  // --- OpenGL Background Renderer for Camera Stream ---
  private var bgProgram = 0
  private var bgPositionAttrib = 0
  private var bgTexCoordAttrib = 0
  private var bgTextureUniform = 0
  private var quadVertices: FloatBuffer? = null
  private var quadTexCoords: FloatBuffer? = null

  private fun initBackgroundRenderer() {
    val vertexShaderCode = """
      attribute vec4 a_Position;
      attribute vec2 a_TexCoord;
      varying vec2 v_TexCoord;
      void main() {
        gl_Position = a_Position;
        v_TexCoord = a_TexCoord;
      }
    """.trimIndent()

    val fragmentShaderCode = """
      #extension GL_OES_EGL_image_external : require
      precision mediump float;
      uniform samplerExternalOES u_Texture;
      varying vec2 v_TexCoord;
      void main() {
        gl_FragColor = texture2D(u_Texture, v_TexCoord);
      }
    """.trimIndent()

    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    bgProgram = GLES20.glCreateProgram()
    GLES20.glAttachShader(bgProgram, vertexShader)
    GLES20.glAttachShader(bgProgram, fragmentShader)
    GLES20.glLinkProgram(bgProgram)

    bgPositionAttrib = GLES20.glGetAttribLocation(bgProgram, "a_Position")
    bgTexCoordAttrib = GLES20.glGetAttribLocation(bgProgram, "a_TexCoord")
    bgTextureUniform = GLES20.glGetUniformLocation(bgProgram, "u_Texture")

    val coords = floatArrayOf(
      -1f, -1f, 0f,
      1f, -1f, 0f,
      -1f, 1f, 0f,
      1f, 1f, 0f
    )
    quadVertices = ByteBuffer.allocateDirect(coords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(coords)
      position(0)
    }

    val texCoords = floatArrayOf(
      0f, 1f,
      1f, 1f,
      0f, 0f,
      1f, 0f
    )
    quadTexCoords = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(texCoords)
      position(0)
    }
  }

  private fun drawBackground(frame: Frame) {
    if (bgProgram == 0) return

    GLES20.glUseProgram(bgProgram)
    GLES20.glDepthMask(false)
    GLES20.glDisable(GLES20.GL_DEPTH_TEST)

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(0x8D65, cameraTextureId)
    GLES20.glUniform1i(bgTextureUniform, 0)

    quadVertices?.let {
      GLES20.glEnableVertexAttribArray(bgPositionAttrib)
      GLES20.glVertexAttribPointer(bgPositionAttrib, 3, GLES20.GL_FLOAT, false, 0, it)
    }

    // Transform camera texture coords for orientation
    quadTexCoords?.let {
      frame.transformCoordinates2d(
        com.google.ar.core.Coordinates2d.IMAGE_NORMALIZED,
        it,
        com.google.ar.core.Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
        it
      )
      GLES20.glEnableVertexAttribArray(bgTexCoordAttrib)
      GLES20.glVertexAttribPointer(bgTexCoordAttrib, 2, GLES20.GL_FLOAT, false, 0, it)
    }

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    GLES20.glDisableVertexAttribArray(bgPositionAttrib)
    GLES20.glDisableVertexAttribArray(bgTexCoordAttrib)
    GLES20.glDepthMask(true)
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
  }

  private fun loadShader(type: Int, shaderCode: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
    return shader
  }
}
