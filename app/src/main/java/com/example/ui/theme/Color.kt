package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// SpaceScan Futuristic Theme Palette
val SpaceDarkBg = Color(0xFF090D16)
val SpaceSurfaceDark = Color(0xFF111827)
val SpaceSurfaceElevated = Color(0xFF1A2337)
val SpaceCardBorder = Color(0xFF263552)

// Neon Accents
val CyanNeon = Color(0xFF00E5FF)
val CyanGlow = Color(0x3300E5FF)
val ElectricBlue = Color(0xFF2979FF)
val DeepBlueAccent = Color(0xFF0D47A1)
val VioletNeon = Color(0xFF7C4DFF)
val PurpleNeon = Color(0xFFB388FF)

// Unified 6-Color Scan System
val ScanGreenCompleted = Color(0xFF10B981)   // 🟩 스캔 완료
val ScanBlueInProgress = Color(0xFF3B82F6)   // 🟦 현재 스캔 진행 중
val ScanGrayUnscanned = Color(0xFF64748B)    // ⬜ 아직 스캔되지 않은 영역
val ScanYellowLowQuality = Color(0xFFF59E0B) // 🟨 스캔 품질 낮음 / 추가 권장
val ScanRedRescan = Color(0xFFEF4444)        // 🟥 재스캔 필요
val ScanPurpleAi = Color(0xFFA855F7)         // 🟪 AI 추천 다음 스캔 영역

// Backward compatibility aliases
val ScanCompletedGreen = ScanGreenCompleted
val ScanInProgressAmber = ScanBlueInProgress
val ScanUnscannedSlate = ScanGrayUnscanned
val ScanRescanRed = ScanRedRescan
val ScanQualityWarning = ScanYellowLowQuality

// Text & Content
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
