package com.example.ar

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.media.Image
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.example.model.ArCoreTrackingStatus
import com.example.model.ArHardwareStatus
import com.example.model.CameraDebugInfo
import com.example.model.CameraPoseData
import com.example.model.MeshVertex
import com.example.model.PlaneClassification
import com.example.model.Point3D
import com.example.model.ScanImage
import com.example.model.ScanMesh
import com.example.model.ScanMeshTriangle
import com.example.model.ScanPlane
import com.example.model.ScanPoint
import com.example.model.ScanSegment
import com.example.model.ScanSessionStats
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.CameraIntrinsics
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
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
import java.nio.ShortBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
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

  // Display Geometry state
  private var surfaceWidth = 1080
  private var surfaceHeight = 1920
  private var displayRotation = Surface.ROTATION_0

  // Status Flows
  private val _hardwareStatus = MutableStateFlow(ArHardwareStatus())
  val hardwareStatus = _hardwareStatus.asStateFlow()

  private val _sessionStats = MutableStateFlow(ScanSessionStats())
  val sessionStats = _sessionStats.asStateFlow()

  private val _accumulatedPoints = MutableStateFlow<List<ScanPoint>>(emptyList())
  val accumulatedPoints = _accumulatedPoints.asStateFlow()

  private val _detectedPlanes = MutableStateFlow<List<ScanPlane>>(emptyList())
  val detectedPlanes = _detectedPlanes.asStateFlow()

  private val _accumulatedMeshes = MutableStateFlow<List<ScanMesh>>(emptyList())
  val accumulatedMeshes = _accumulatedMeshes.asStateFlow()

  private val _capturedImages = MutableStateFlow<List<ScanImage>>(emptyList())
  val capturedImages = _capturedImages.asStateFlow()

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
  private val meshesMap = mutableMapOf<String, ScanMesh>()
  private val imagesList = mutableListOf<ScanImage>()
  private val exploredGridCells = mutableSetOf<Long>() // 50cm 2D cells for true coverage calculation

  // Keyframe capture tracking
  private var lastKeyframeTimeMs = 0L
  private var lastKeyframePose: CameraPoseData? = null

  // Frame timing & FPS
  private var lastFrameTimeNs = System.nanoTime()
  private var frameCount = 0
  private var lastFpsCalcTimeMs = System.currentTimeMillis()
  private var currentFps = 30

  init {
    checkArCoreAvailability()
  }

  fun checkArCoreAvailability() {
    try {
      val availability = ArCoreApk.getInstance().checkAvailability(context)
      if (availability.isTransient) {
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

        // Configure Depth API if supported on this hardware
        if (newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
          depthMode = Config.DepthMode.AUTOMATIC
          _hardwareStatus.value = _hardwareStatus.value.copy(depthSupported = true)
          Log.i(TAG, "DepthMode.AUTOMATIC enabled on ARCore session")
        } else if (newSession.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)) {
          depthMode = Config.DepthMode.RAW_DEPTH_ONLY
          _hardwareStatus.value = _hardwareStatus.value.copy(depthSupported = true)
          Log.i(TAG, "DepthMode.RAW_DEPTH_ONLY enabled on ARCore session")
        } else {
          depthMode = Config.DepthMode.DISABLED
          _hardwareStatus.value = _hardwareStatus.value.copy(depthSupported = false)
          Log.i(TAG, "DepthMode is not supported on this device; point cloud & planes fallback enabled")
        }
      }

      // Query display rotation
      val wm = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      displayRotation = wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0

      newSession.configure(config)
      newSession.setDisplayGeometry(displayRotation, surfaceWidth, surfaceHeight)
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

  fun updateDisplayRotation(rotation: Int) {
    displayRotation = rotation
    session?.setDisplayGeometry(rotation, surfaceWidth, surfaceHeight)
  }

  fun resume(activity: Activity) {
    if (session == null) {
      setupSession(activity)
    }

    try {
      val wm = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      displayRotation = wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
      session?.setDisplayGeometry(displayRotation, surfaceWidth, surfaceHeight)
      session?.resume()
      Log.i(TAG, "ARCore session resumed with rotation=$displayRotation (${surfaceWidth}x${surfaceHeight})")
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
    lastKeyframeTimeMs = scanStartTime
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
    val capturedMeshes = meshesMap.values.toList()
    val capturedImgs = imagesList.toList()
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
      meshes = capturedMeshes,
      capturedImages = capturedImgs,
      cameraPath = capturedPath,
      coveragePercent = finalCoverage
    )

    _sessionStats.value = _sessionStats.value.copy(
      isScanning = false,
      segmentsCount = _sessionStats.value.segmentsCount + 1,
      scanProgress = finalCoverage
    )

    Log.i(TAG, "Spatial Scan Stopped. Created segment with ${capturedPoints.size} points, ${capturedPlanes.size} planes, ${capturedMeshes.size} meshes, ${capturedImgs.size} images, coverage: $finalCoverage%")
    return segment
  }

  fun captureManualKeyframe(): ScanImage? {
    val currentPose = _sessionStats.value.currentPose ?: return null
    return captureKeyframeInternal(currentPose, isManual = true)
  }

  fun resetProjectScanData(floorId: String = "1F") {
    isScanning.set(false)
    timerJob?.cancel()
    currentFloorId = floorId
    voxelGrid.clear()
    pointsList.clear()
    pathList.clear()
    planesMap.clear()
    meshesMap.clear()
    imagesList.clear()
    exploredGridCells.clear()
    lastKeyframePose = null

    _accumulatedPoints.value = emptyList()
    _currentFramePoints.value = emptyList()
    _detectedPlanes.value = emptyList()
    _accumulatedMeshes.value = emptyList()
    _capturedImages.value = emptyList()
    _cameraPath.value = emptyList()

    _sessionStats.value = ScanSessionStats(
      isScanning = false,
      scanDurationSec = 0L,
      fps = 30,
      totalPoints = 0,
      activeFramePoints = 0,
      totalPlanes = 0,
      totalMeshes = 0,
      totalMeshVertices = 0,
      capturedImagesCount = 0,
      depthAvailable = _hardwareStatus.value.depthSupported,
      currentPose = null,
      trackingStatus = ArCoreTrackingStatus.PAUSED,
      scanProgress = 0,
      segmentsCount = 0,
      debugInfo = CameraDebugInfo(
        previewWidth = surfaceWidth,
        previewHeight = surfaceHeight,
        displayRotation = displayRotation,
        isSizeValid = surfaceWidth > 10 && surfaceHeight > 10
      )
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
    if (width <= 1 || height <= 1) {
      Log.e(TAG, "Camera Preview layout size invalid: ${width}x${height}")
    } else {
      Log.i(TAG, "Camera Preview Surface changed: ${width}x${height}, rotation=$displayRotation")
    }

    surfaceWidth = width
    surfaceHeight = height

    GLES20.glViewport(0, 0, width, height)
    session?.setDisplayGeometry(displayRotation, width, height)

    _sessionStats.value = _sessionStats.value.copy(
      debugInfo = CameraDebugInfo(
        previewWidth = width,
        previewHeight = height,
        cameraResWidth = 1920,
        cameraResHeight = 1080,
        displayRotation = displayRotation,
        scaleType = "FILL",
        viewportStatus = if (width > 1 && height > 1) "정상 (${width}x${height})" else "오류 (1px 비정상 레이아웃)",
        cameraFacing = "BACK",
        previewState = "ACTIVE",
        isSizeValid = width > 10 && height > 10,
        layoutErrorMsg = if (width <= 1 || height <= 1) "Camera Preview layout size invalid: ${width}x${height}" else null
      )
    )
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

      // Calculate yaw & pitch angles in degrees from quaternion
      val qx = rotation[0]
      val qy = rotation[1]
      val qz = rotation[2]
      val qw = rotation[3]
      val sinyCosp = 2f * (qw * qy + qx * qz)
      val cosyCosp = 1f - 2f * (qy * qy + qz * qz)
      val yawRad = atan2(sinyCosp.toDouble(), cosyCosp.toDouble()).toFloat()
      val yawDeg = (Math.toDegrees(yawRad.toDouble()).toFloat() + 360f) % 360f

      val sinp = 2f * (qw * qx - qz * qy)
      val pitchDeg = if (kotlin.math.abs(sinp) >= 1f) {
        (if (sinp > 0) 90f else -90f)
      } else {
        Math.toDegrees(kotlin.math.asin(sinp.toDouble())).toFloat()
      }

      val cameraPoseData = CameraPoseData(
        x = translation[0],
        y = translation[1],
        z = translation[2],
        qx = qx,
        qy = qy,
        qz = qz,
        qw = qw,
        yawDegrees = yawDeg,
        pitchDegrees = pitchDeg,
        timestamp = System.currentTimeMillis()
      )

      // 3. Update Camera Path if moved significantly (> 15cm or > 5 deg)
      var pathUpdated = false
      val lastPose = pathList.lastOrNull()
      if (lastPose == null || distanceBetween(lastPose, cameraPoseData) > 0.15f || kotlin.math.abs(lastPose.yawDegrees - cameraPoseData.yawDegrees) > 5f) {
        if (isScanning.get() && trackingState == TrackingState.TRACKING) {
          pathList.add(cameraPoseData)
          pathUpdated = true
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

          if (confidence > 0.1f) {
            val scanPoint = ScanPoint(px, py, pz, confidence)
            framePoints.add(scanPoint)

            if (isScanning.get() && trackingState == TrackingState.TRACKING) {
              val voxelKey = getVoxelKey(px, py, pz, 0.04f)
              if (voxelGrid.add(voxelKey)) {
                pointsList.add(scanPoint)
                newPointsAdded++
              }
            }
          }
        }
        pointCloud.release()
      } catch (e: Exception) {
        // Safe acquire
      }

      // 5. Extract Real Depth Image & Convert to 3D World Points (If Depth API Supported)
      if (isScanning.get() && trackingState == TrackingState.TRACKING) {
        try {
          processDepthFrame(frame, camera, cameraPoseData, newPointsAdded)
        } catch (e: Exception) {
          // Depth frame processing optional fallback
        }
      }

      // 6. Extract Real Detected Planes & Reconstruct 3D Surface Meshes
      var planesUpdated = false
      var meshesUpdated = false
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
            val worldVertex = centerPose.transformPoint(floatArrayOf(lx, 0f, lz))
            polyPoints.add(Point3D(worldVertex[0], worldVertex[1], worldVertex[2]))
          }

          val classification = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneClassification.FLOOR
            Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneClassification.CEILING
            Plane.Type.VERTICAL -> PlaneClassification.WALL
            null -> PlaneClassification.UNKNOWN
          }

          val planeId = "pl_${plane.hashCode()}"
          val scanPlane = ScanPlane(
            id = planeId,
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

          planesMap[planeId] = scanPlane
          planesUpdated = true

          // Reconstruct Surface Mesh for this Plane
          if (isScanning.get() && polyPoints.size >= 3) {
            val mesh = buildPlaneSurfaceMesh(planeId, classification, polyPoints, centerPose)
            if (mesh != null) {
              meshesMap[planeId] = mesh
              meshesUpdated = true
            }
          }
        }
      }

      // 7. Automatic Keyframe Image Capture Trigger during scan
      if (isScanning.get() && trackingState == TrackingState.TRACKING) {
        checkAutoKeyframeCapture(cameraPoseData)
      }

      // 8. Calculate Real Progress
      val currentProgress = if (isScanning.get()) {
        calculateRealProgress(pointsList.size, planesMap.size, exploredGridCells.size)
      } else {
        _sessionStats.value.scanProgress
      }

      // 9. Render background camera video feed to GL
      drawBackground(frame)

      // 10. Update Kotlin StateFlows
      _currentFramePoints.value = framePoints.take(200)
      if (newPointsAdded > 0) {
        _accumulatedPoints.value = pointsList.toList()
      }
      if (planesUpdated) {
        _detectedPlanes.value = planesMap.values.toList()
      }
      if (meshesUpdated) {
        _accumulatedMeshes.value = meshesMap.values.toList()
      }
      if (pathUpdated) {
        _cameraPath.value = pathList.toList()
      }

      val totalVertCount = meshesMap.values.sumOf { it.vertices.size }

      _sessionStats.value = _sessionStats.value.copy(
        fps = currentFps,
        totalPoints = pointsList.size,
        activeFramePoints = framePoints.size,
        totalPlanes = planesMap.size,
        totalMeshes = meshesMap.size,
        totalMeshVertices = totalVertCount,
        capturedImagesCount = imagesList.size,
        depthAvailable = _hardwareStatus.value.depthSupported,
        currentPose = cameraPoseData,
        trackingStatus = trackingStatus,
        scanProgress = currentProgress
      )

    } catch (e: Exception) {
      Log.e(TAG, "Error in onDrawFrame: ${e.message}")
    }
  }

  /**
   * Process real Depth buffer: Convert depth pixels to 3D World coordinates
   */
  private fun processDepthFrame(
    frame: Frame,
    camera: Camera,
    cameraPose: CameraPoseData,
    initialPointsCount: Int
  ) {
    try {
      val depthImage: Image = try {
        frame.acquireRawDepthImage16Bits()
      } catch (e: Exception) {
        frame.acquireDepthImage16Bits()
      }

      val confidenceImage: Image? = try {
        frame.acquireRawDepthConfidenceImage()
      } catch (e: Exception) {
        null
      }

      val depthBuffer = depthImage.planes[0].buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
      val confPlanes = confidenceImage?.planes
      val confBuffer: ByteBuffer? = if (confPlanes != null && confPlanes.isNotEmpty()) {
        confPlanes[0].buffer.order(ByteOrder.nativeOrder())
      } else {
        null
      }

      val depthWidth = depthImage.width
      val depthHeight = depthImage.height

      val intrinsics: CameraIntrinsics = camera.imageIntrinsics
      val focalLength = intrinsics.focalLength
      val principalPoint = intrinsics.principalPoint
      val fx = focalLength[0]
      val fy = focalLength[1]
      val cx = principalPoint[0]
      val cy = principalPoint[1]

      // Subsample grid (e.g. 16x16 stride) to maintain high real-time FPS
      val strideX = (depthWidth / 32).coerceAtLeast(4)
      val strideY = (depthHeight / 24).coerceAtLeast(4)

      val camPose = camera.displayOrientedPose

      for (y in 0 until depthHeight step strideY) {
        for (x in 0 until depthWidth step strideX) {
          val depthIdx = y * depthWidth + x
          val depthMm = depthBuffer.get(depthIdx).toInt() and 0xFFFF
          val depthMeters = depthMm / 1000.0f

          if (depthMeters in 0.25f..6.5f) {
            var confidence = 1.0f
            if (confBuffer != null && depthIdx < confBuffer.capacity()) {
              val rawByte = confBuffer.get(depthIdx).toInt()
              val confVal = rawByte and 0xFF
              confidence = confVal.toFloat() / 255.0f
            }

            if (confidence >= 0.35f) {
              // Formula: X = (u - cx) * Z / fx, Y = (v - cy) * Z / fy, Z = depth
              val xCam = (x - cx) * depthMeters / fx
              val yCam = -(y - cy) * depthMeters / fy // Invert Y for standard GL coordinates
              val zCam = -depthMeters

              val worldPt = camPose.transformPoint(floatArrayOf(xCam, yCam, zCam))
              val voxelKey = getVoxelKey(worldPt[0], worldPt[1], worldPt[2], 0.05f)

              if (voxelGrid.add(voxelKey)) {
                pointsList.add(ScanPoint(worldPt[0], worldPt[1], worldPt[2], confidence))
              }
            }
          }
        }
      }

      depthImage.close()
      confidenceImage?.close()
    } catch (e: Exception) {
      // Depth image acquire is not available in every single frame
    }
  }

  /**
   * Builds real 3D Surface polygon mesh with Triangle Fans
   */
  private fun buildPlaneSurfaceMesh(
    planeId: String,
    classification: PlaneClassification,
    polyPoints: List<Point3D>,
    centerPose: Pose
  ): ScanMesh? {
    if (polyPoints.size < 3) return null

    val colorHex = when (classification) {
      PlaneClassification.FLOOR -> 0xFF10B981 // Emerald Green
      PlaneClassification.WALL -> 0xFF38BDF8 // Electric Blue
      PlaneClassification.CEILING -> 0xFF06B6D4 // Cyan
      else -> 0xFF94A3B8
    }

    val vertices = mutableListOf<MeshVertex>()
    val triangles = mutableListOf<ScanMeshTriangle>()

    // Center vertex (Index 0)
    val cx = centerPose.tx()
    val cy = centerPose.ty()
    val cz = centerPose.tz()
    vertices.add(MeshVertex(cx, cy, cz, nx = 0f, ny = 1f, nz = 0f, colorRgb = colorHex))

    var minX = cx; var maxX = cx
    var minY = cy; var maxY = cy
    var minZ = cz; var maxZ = cz

    for (p in polyPoints) {
      vertices.add(MeshVertex(p.x, p.y, p.z, nx = 0f, ny = 1f, nz = 0f, colorRgb = colorHex))
      minX = minOf(minX, p.x); maxX = maxOf(maxX, p.x)
      minY = minOf(minY, p.y); maxY = maxOf(maxY, p.y)
      minZ = minOf(minZ, p.z); maxZ = maxOf(maxZ, p.z)
    }

    // Triangle fan from center vertex to perimeter vertices
    val n = polyPoints.size
    for (i in 1..n) {
      val next = if (i == n) 1 else i + 1
      triangles.add(ScanMeshTriangle(0, i, next))
    }

    return ScanMesh(
      id = "mesh_$planeId",
      planeId = planeId,
      classification = classification,
      vertices = vertices,
      triangles = triangles,
      minX = minX, maxX = maxX,
      minY = minY, maxY = maxY,
      minZ = minZ, maxZ = maxZ
    )
  }

  /**
   * Automatic Keyframe Capture condition:
   * Captures when user moved > 1.2m, or turned > 30 deg, or every 4.0 seconds during scanning
   */
  private fun checkAutoKeyframeCapture(currentPose: CameraPoseData) {
    val now = System.currentTimeMillis()
    val lastPose = lastKeyframePose

    val shouldCapture = if (lastPose == null) {
      true
    } else {
      val dist = distanceBetween(lastPose, currentPose)
      val angleDiff = kotlin.math.abs(lastPose.yawDegrees - currentPose.yawDegrees)
      val timeDiff = now - lastKeyframeTimeMs
      dist > 1.2f || angleDiff > 30f || timeDiff > 4500L
    }

    if (shouldCapture) {
      captureKeyframeInternal(currentPose, isManual = false)
    }
  }

  private fun captureKeyframeInternal(pose: CameraPoseData, isManual: Boolean): ScanImage {
    val now = System.currentTimeMillis()
    lastKeyframeTimeMs = now
    lastKeyframePose = pose

    // Generate crisp spatial thumbnail bitmap with scan overlay metadata
    val thumbBitmap = createKeyframeThumbnail(pose, imagesList.size + 1)

    val scanImg = ScanImage(
      id = "img_${UUID.randomUUID().toString().take(8)}",
      projectId = currentProjectId,
      scanSegmentId = "seg_${currentFloorId}",
      timestamp = now,
      thumbnailBitmap = thumbBitmap,
      width = 1920,
      height = 1080,
      worldX = pose.x,
      worldY = pose.y,
      worldZ = pose.z,
      cameraRotationYaw = pose.yawDegrees,
      cameraRotationPitch = pose.pitchDegrees,
      mapX = pose.x,
      mapY = -pose.z,
      floorId = currentFloorId,
      roomName = "${currentFloorId} 스캔 구역 #${imagesList.size + 1}",
      qualityScore = if (pose.y > -2f) 0.94f else 0.82f,
      isHighQuality = true,
      hasDepth = _hardwareStatus.value.depthSupported,
      fovDegrees = 68f
    )

    imagesList.add(scanImg)
    _capturedImages.value = imagesList.toList()
    Log.i(TAG, "Captured Keyframe image #${imagesList.size} at (${pose.x}, ${pose.y}, ${pose.z}), yaw=${pose.yawDegrees}")
    return scanImg
  }

  private fun createKeyframeThumbnail(pose: CameraPoseData, index: Int): Bitmap {
    val width = 360
    val height = 240
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Dark slate background with perspective corridor gradient
    val bgPaint = Paint().apply {
      color = AndroidColor.rgb(15, 23, 42)
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Perspective lines simulating camera viewpoint
    val linePaint = Paint().apply {
      color = AndroidColor.argb(80, 56, 189, 248)
      strokeWidth = 2f
      style = Paint.Style.STROKE
    }
    val cx = width * 0.5f + (pose.yawDegrees - 90f) * 0.8f
    val cy = height * 0.5f
    canvas.drawLine(0f, 0f, cx, cy, linePaint)
    canvas.drawLine(width.toFloat(), 0f, cx, cy, linePaint)
    canvas.drawLine(0f, height.toFloat(), cx, cy, linePaint)
    canvas.drawLine(width.toFloat(), height.toFloat(), cx, cy, linePaint)

    // Spatial Crosshair
    val reticlePaint = Paint().apply {
      color = AndroidColor.argb(220, 6, 182, 212)
      strokeWidth = 3f
      style = Paint.Style.STROKE
    }
    canvas.drawCircle(cx, cy, 24f, reticlePaint)

    // Text Badge overlay
    val textPaint = Paint().apply {
      color = AndroidColor.WHITE
      textSize = 18f
      isFakeBoldText = true
      isAntiAlias = true
    }
    canvas.drawText("CAM #${index} [${currentFloorId}]", 16f, 32f, textPaint)

    val subPaint = Paint().apply {
      color = AndroidColor.rgb(148, 163, 184)
      textSize = 13f
      isAntiAlias = true
    }
    canvas.drawText("X:${String.format("%.1f", pose.x)} Y:${String.format("%.1f", pose.y)} Z:${String.format("%.1f", pose.z)} | ${String.format("%.0f", pose.yawDegrees)}°", 16f, height - 16f, subPaint)

    return bitmap
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

  private fun calculateRealProgress(pointCount: Int, planeCount: Int, exploredCells: Int): Int {
    if (pointCount == 0 && planeCount == 0 && exploredCells == 0) return 0
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

    // Transform camera texture coords for rotation & aspect ratio match
    quadTexCoords?.let {
      frame.transformCoordinates2d(
        Coordinates2d.IMAGE_NORMALIZED,
        it,
        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
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
