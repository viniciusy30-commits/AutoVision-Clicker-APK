package com.autovision.clicker.models

import android.graphics.Rect

enum class AppStatus { DISABLED, READY, RUNNING, PAUSED, ERROR }
enum class RecognitionMode { COLOR, SHAPE, HYBRID }
enum class ActionType { CLICK, DOUBLE_CLICK, LONG_PRESS, SWIPE, WAIT, IMAGE_MATCH, COLOR_MATCH, SEQUENCE }
enum class ShapeType { CIRCLE, SQUARE, RECTANGLE, TRIANGLE, IRREGULAR, UNKNOWN }

data class ComparisonWeights(
    val shape: Double = .50,
    val feature: Double = .30,
    val color: Double = .20
) {
    fun normalized(): ComparisonWeights {
        val total = (shape + feature + color).coerceAtLeast(.0001)
        return ComparisonWeights(shape / total, feature / total, color / total)
    }
}

data class RegionOfInterest(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun toRect() = Rect(left, top, right, bottom)
}

data class VisualObject(
    val id: Int,
    val bounds: Rect,
    val centerX: Int,
    val centerY: Int,
    val area: Double,
    val shape: ShapeType,
    val confidence: Double,
    val dominantColor: Int? = null,
    val normalizedFeature: FloatArray? = null
)

data class VisualPair(
    val top: VisualObject,
    val bottom: VisualObject,
    val score: Double
)

data class ImageMatchResult(
    val bounds: Rect,
    val confidence: Double,
    val method: String
)

data class VisionResult(
    val objects: List<VisualObject> = emptyList(),
    val pairs: List<VisualPair> = emptyList(),
    val logs: List<String> = emptyList(),
    val processedAtMs: Long = System.currentTimeMillis()
)

data class AutomationConfig(
    val intervalMs: Long = 250,
    val repetitions: Int = 1,
    val minConfidence: Double = .80,
    val mode: RecognitionMode = RecognitionMode.HYBRID,
    val weights: ComparisonWeights = ComparisonWeights(),
    val analysisIntervalMs: Long = 250,
    val maxClicksPerMinute: Int = 300,
    val pauseOnUnexpectedChange: Boolean = true
)

data class AutomationAction(
    val type: ActionType,
    val durationMs: Long = 0,
    val pauseAfterMs: Long = 100,
    val confidenceRequired: Double? = null,
    val children: List<AutomationAction> = emptyList(),
    val label: String = type.name
)

data class AutomationProfile(
    val id: String,
    val name: String,
    val config: AutomationConfig = AutomationConfig(),
    val actions: List<AutomationAction> = emptyList(),
    val topRoi: RegionOfInterest? = null,
    val bottomRoi: RegionOfInterest? = null
)