package com.autovision.clicker.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.autovision.clicker.models.ComparisonWeights
import com.autovision.clicker.models.RecognitionMode
import com.autovision.clicker.models.VisualObject
import com.autovision.clicker.models.VisionResult
import com.autovision.clicker.utils.AppLogger
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat

class VisionEngine {
    private val shapeEngine = ShapeRecognitionEngine()
    private val colorEngine = ColorRecognitionEngine()
    private val pairMatcher = PairMatcher()

    init {
        if (!OpenCVLoader.initDebug()) AppLogger.error("OpenCV não foi inicializado")
    }

    fun analyze(
        bitmap: Bitmap,
        topRoi: Rect? = null,
        bottomRoi: Rect? = null,
        mode: RecognitionMode = RecognitionMode.HYBRID,
        weights: ComparisonWeights = ComparisonWeights(),
        minConfidence: Double = .80
    ): VisionResult {
        val full = Mat()
        Utils.bitmapToMat(bitmap, full)
        val topMat = crop(full, topRoi ?: Rect(0, 0, full.cols(), full.rows() / 2))
        val bottomMat = crop(bottomRoi ?: Rect(0, full.rows() / 2, full.cols(), full.rows()))
        val rawTop = shapeEngine.detectObjects(topMat)
        val rawBottom = shapeEngine.detectObjects(bottomMat)
        val top = rawTop.map { shift(it, topRoi?.left ?: 0, topRoi?.top ?: 0) }
        val bottom = rawBottom.map { shift(it, bottomRoi?.left ?: 0, bottomRoi?.top ?: full.rows() / 2) }
        val pairs = pairMatcher.findVisualPairs(top, bottom, mode, weights)
        val validPairs = pairs.filter { it.score >= minConfidence }
        val logs = listOf(
            "Captura realizada",
            "${top.size + bottom.size} objetos encontrados",
            if (validPairs.isEmpty()) "Nenhuma correspondência acima do limite" else
                "Correspondência média: ${(validPairs.map { it.score }.average() * 100).toInt()}%",
            "${validPairs.size} pares prontos para ação"
        )
        logs.forEach(AppLogger::info)
        topMat.release()
        bottomMat.release()
        full.release()
        return VisionResult(top + bottom, validPairs, logs)
    }

    fun drawDebugOverlay(source: Bitmap, result: VisionResult): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        Canvas(output).apply {
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.CYAN
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            result.objects.forEach { objectItem ->
                drawRect(objectItem.bounds, boxPaint)
                drawText("OBJ %02d — %d%%".format(objectItem.id, (objectItem.confidence * 100).toInt()),
                    objectItem.bounds.left.toFloat(), objectItem.bounds.top.coerceAtLeast(28).toFloat(), textPaint)
            }
            result.pairs.forEach { pair ->
                boxPaint.color = Color.MAGENTA
                drawLine(pair.top.centerX.toFloat(), pair.top.centerY.toFloat(),
                    pair.bottom.centerX.toFloat(), pair.bottom.centerY.toFloat(), boxPaint)
                drawText("MATCH — ${(pair.score * 100).toInt()}%",
                    pair.top.centerX.toFloat(), pair.top.centerY.toFloat(), textPaint)
            }
        }
        return output
    }

    private fun crop(source: Mat, rect: Rect): Mat {
        val safe = Rect(
            rect.left.coerceIn(0, source.cols() - 1),
            rect.top.coerceIn(0, source.rows() - 1),
            rect.right.coerceIn(1, source.cols()),
            rect.bottom.coerceIn(1, source.rows())
        )
        return Mat(source, org.opencv.core.Rect(safe.left, safe.top, safe.width(), safe.height())).clone()
    }

    private fun shift(item: VisualObject, dx: Int, dy: Int) = item.copy(
        bounds = Rect(item.bounds.left + dx, item.bounds.top + dy, item.bounds.right + dx, item.bounds.bottom + dy),
        centerX = item.centerX + dx,
        centerY = item.centerY + dy
    )
}