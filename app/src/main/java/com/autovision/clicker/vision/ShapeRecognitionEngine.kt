package com.autovision.clicker.vision

import android.graphics.Rect
import com.autovision.clicker.models.ShapeType
import com.autovision.clicker.models.VisualObject
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.abs

class ShapeRecognitionEngine {
    fun detectObjects(image: Mat, minimumArea: Double = 180.0): List<VisualObject> {
        if (image.empty()) return emptyList()
        val gray = Mat()
        Imgproc.cvtColor(image, gray, if (image.channels() == 4) Imgproc.COLOR_RGBA2GRAY else Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, org.opencv.core.Size(5.0, 5.0), 0.0)
        Imgproc.Canny(gray, gray, 70.0, 180.0)
        val contours = ArrayList<Mat>()
        Imgproc.findContours(gray, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        val objects = contours.mapIndexedNotNull { index, contour ->
            val area = Imgproc.contourArea(contour)
            if (area < minimumArea) return@mapIndexedNotNull null
            val rect = Imgproc.boundingRect(contour)
            val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approximation = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approximation, .04 * perimeter, true)
            val shape = classify(approximation.total().toInt(), area, perimeter, rect.width, rect.height)
            val confidence = shapeConfidence(shape, approximation.total().toInt(), area, perimeter, rect.width, rect.height)
            val roi = Mat(
                image,
                org.opencv.core.Rect(rect.x, rect.y, rect.width, rect.height)
            )
            val mean = Core.mean(roi)
            val dominantColor = if (mean.`val`.size >= 3) {
                android.graphics.Color.rgb(
                    mean.`val`[0].toInt().coerceIn(0, 255),
                    mean.`val`[1].toInt().coerceIn(0, 255),
                    mean.`val`[2].toInt().coerceIn(0, 255)
                )
            } else null
            roi.release()
            VisualObject(
                id = index + 1,
                bounds = Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height),
                centerX = rect.x + rect.width / 2,
                centerY = rect.y + rect.height / 2,
                area = area,
                shape = shape,
                confidence = confidence,
                dominantColor = dominantColor
            ).also { approximation.release() }
        }.sortedBy { it.bounds.top }
        contours.forEach { it.release() }
        gray.release()
        return objects
    }

    private fun classify(vertices: Int, area: Double, perimeter: Double, width: Int, height: Int): ShapeType {
        val ratio = width.toDouble() / height.coerceAtLeast(1)
        val circularity = 4 * PI * area / (perimeter * perimeter).coerceAtLeast(.001)
        return when {
            circularity > .78 -> ShapeType.CIRCLE
            vertices == 3 -> ShapeType.TRIANGLE
            vertices == 4 && abs(ratio - 1.0) < .15 -> ShapeType.SQUARE
            vertices == 4 -> ShapeType.RECTANGLE
            vertices > 4 && circularity > .45 -> ShapeType.IRREGULAR
            else -> ShapeType.UNKNOWN
        }
    }

    private fun shapeConfidence(
        shape: ShapeType, vertices: Int, area: Double, perimeter: Double, width: Int, height: Int
    ): Double {
        val circularity = 4 * PI * area / (perimeter * perimeter).coerceAtLeast(.001)
        val geometric = when (shape) {
            ShapeType.CIRCLE -> circularity
            ShapeType.TRIANGLE -> if (vertices == 3) .92 else .55
            ShapeType.SQUARE -> if (abs(width.toDouble() / height - 1) < .15) .94 else .65
            ShapeType.RECTANGLE -> if (vertices == 4) .90 else .55
            ShapeType.IRREGULAR -> .72
            ShapeType.UNKNOWN -> .40
        }
        return geometric.coerceIn(0.0, 1.0)
    }
}