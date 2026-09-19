package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.ScanStatus

object ScanVisualSystem {
  val Completed = ScanGreenCompleted      // 🟩 #10B981
  val InProgress = ScanBlueInProgress    // 🟦 #3B82F6
  val Unscanned = ScanGrayUnscanned      // ⬜ #64748B
  val LowQuality = ScanYellowLowQuality  // 🟨 #F59E0B
  val RescanNeeded = ScanRedRescan       // 🟥 #EF4444
  val AiRecommended = ScanPurpleAi       // 🟪 #A855F7

  fun getColor(status: ScanStatus, isAiTarget: Boolean = false): Color {
    if (isAiTarget) return AiRecommended
    return when (status) {
      ScanStatus.COMPLETED -> Completed
      ScanStatus.IN_PROGRESS -> InProgress
      ScanStatus.UNSCANNED -> Unscanned
      ScanStatus.LOW_QUALITY -> LowQuality
      ScanStatus.RESCAN_NEEDED -> RescanNeeded
      ScanStatus.AI_RECOMMENDED -> AiRecommended
    }
  }

  // Camera AR Overlay transparency (Section 15)
  // 스캔 완료: 일반 화면 또는 매우 약한 투명 표시
  // 스캔 진행 중: 반투명 BLUE
  // 미스캔: 반투명 GRAY
  // 품질 낮음: 반투명 YELLOW
  // 재스캔 필요: 강한 반투명 RED
  // AI 추천: 반투명 PURPLE
  fun getCameraOverlayAlpha(status: ScanStatus, isAiTarget: Boolean = false): Float {
    if (isAiTarget) return 0.52f
    return when (status) {
      ScanStatus.COMPLETED -> 0.10f
      ScanStatus.IN_PROGRESS -> 0.42f
      ScanStatus.UNSCANNED -> 0.35f
      ScanStatus.LOW_QUALITY -> 0.48f
      ScanStatus.RESCAN_NEEDED -> 0.65f
      ScanStatus.AI_RECOMMENDED -> 0.52f
    }
  }

  // 2D Map fill alpha
  fun getMapFillAlpha(status: ScanStatus, isAiTarget: Boolean = false): Float {
    if (isAiTarget) return 0.35f
    return when (status) {
      ScanStatus.COMPLETED -> 0.28f
      ScanStatus.IN_PROGRESS -> 0.28f
      ScanStatus.UNSCANNED -> 0.22f
      ScanStatus.LOW_QUALITY -> 0.32f
      ScanStatus.RESCAN_NEEDED -> 0.35f
      ScanStatus.AI_RECOMMENDED -> 0.38f
    }
  }
}
