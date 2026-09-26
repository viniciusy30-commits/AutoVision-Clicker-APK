package com.autovision.clicker.vision

import android.graphics.Color
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class ColorRecognitionEngine {
    fun findColorRegions(
        image: Mat,
        targetRgb: Int,
        tolerance: Int = 24,
        hsv: Boolean = true
    ): List<android.graphics.Rect> {
        if (image.empty()) return emptyList()
        val source = if (hsv) Mat().also {
            Imgproc.cvtColor(image, it, Imgproc.COLOR_RGBA2HSV)
        } else Mat().also {
            if (image.channels() == 4) Imgproc.cvtColor(image, it, Imgproc.COLOR_RGBA2RGB)
            else image.copyTo(it)
        }
        val lower: Scalar
        val upper: Scalar
        if (hsv) {
            val hsvColor = FloatArray(3)
            Color.colorToHSV(targetRgb, hsvColor)
            val h = hsvColor[0] / 2.0
            lower = Scalar((h - tolerance / 4.0).coerceAtLeast(0.0), 40.0, 40.0)
            upper = Scalar((h + tolerance / 4.0).coerceAtMost(179.0), 255.0, 255.0)
        } else {
            lower = Scalar(
                (Color.red(targetRgb) - tolerance).coerceAtLeast(0),
                (Color.green(targetRgb) - tolerance).coerceAtLeast(0),
                (Color.blue(targetRgb) - tolerance).coerceAtLeast(0)
            )
            upper = Scalar(
                (Color.red(targetRgb) + tolerance).coerceAtMost(255),
                (Color.green(targetRgb) + tolerance).coerceAtMost(255),
                (Color.blue(targetRgb) + tolerance).coerceAtMost(255)
            )
        }
        val mask = Mat()
        Core.inRange(source, lower, upper, mask)
        val contours = ArrayList<Mat>()
        Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        val regions = contours.map { Imgproc.boundingRect(it).let { r ->
            android.graphics.Rect(r.x, r.y, r.x + r.width, r.y + r.height)
        } }
        contours.forEach { it.release() }
        source.release()
        mask.release()
        return regions
    }

    fun dominantColor(image: Mat): Int? {
        if (image.empty()) return null
        val mean = Core.mean(image)
        return Color.rgb(mean.`val`[0].toInt().coerceIn(0, 255),
            mean.`val`[1].toInt().coerceIn(0, 255),
            mean.`val`[2].toInt().coerceIn(0, 255))
    }
}