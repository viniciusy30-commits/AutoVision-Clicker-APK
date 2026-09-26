package com.autovision.clicker.vision

import android.graphics.Rect
import com.autovision.clicker.models.ImageMatchResult
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

class ImageRecognitionEngine {
    fun findImageOnScreen(screen: Mat, template: Mat, threshold: Double = .80): ImageMatchResult? {
        if (screen.empty() || template.empty()) return null
        val normalizedScreen = normalizeImage(screen)
        val normalizedTemplate = normalizeImage(template)
        val result = Mat()
        Imgproc.matchTemplate(normalizedScreen, normalizedTemplate, result, Imgproc.TM_CCOEFF_NORMED)
        val minMax = Core.minMaxLoc(result)
        val confidence = minMax.maxVal.coerceIn(0.0, 1.0)
        val x = max(0, minMax.maxLoc.x.toInt())
        val y = max(0, minMax.maxLoc.y.toInt())
        val bounds = Rect(x, y, x + template.cols(), y + template.rows())
        result.release()
        normalizedScreen.release()
        normalizedTemplate.release()
        return ImageMatchResult(bounds, confidence, "template + normalização")
            .takeIf { confidence >= threshold }
    }

    fun compareImages(first: Mat, second: Mat): Double {
        if (first.empty() || second.empty()) return 0.0
        val a = normalizeImage(first)
        val b = normalizeImage(second)
        val resized = Mat()
        Imgproc.resize(b, resized, a.size())
        val score = calculateSimilarity(a, resized)
        a.release()
        b.release()
        resized.release()
        return score
    }

    fun calculateSimilarity(first: Mat, second: Mat): Double {
        val grayA = toGray(first)
        val grayB = toGray(second)
        val result = Mat()
        Imgproc.matchTemplate(grayA, grayB, result, Imgproc.TM_CCOEFF_NORMED)
        val score = Core.minMaxLoc(result).maxVal.coerceIn(0.0, 1.0)
        grayA.release()
        grayB.release()
        result.release()
        return score
    }

    fun findContours(input: Mat): List<Mat> {
        val gray = toGray(input)
        Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 0.0)
        Imgproc.Canny(gray, gray, 60.0, 160.0)
        val contours = ArrayList<Mat>()
        Imgproc.findContours(gray, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        gray.release()
        return contours
    }

    fun normalizeImage(input: Mat): Mat {
        val output = Mat()
        val gray = toGray(input)
        Core.normalize(gray, output, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
        gray.release()
        return output
    }

    fun detectObjects(input: Mat) = ShapeRecognitionEngine().detectObjects(input)

    private fun toGray(input: Mat): Mat {
        if (input.channels() == 1) return input.clone()
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_RGBA2GRAY)
        return gray
    }
}