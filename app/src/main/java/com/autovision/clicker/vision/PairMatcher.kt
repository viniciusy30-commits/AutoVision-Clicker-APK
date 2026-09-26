package com.autovision.clicker.vision

import com.autovision.clicker.models.ComparisonWeights
import com.autovision.clicker.models.RecognitionMode
import com.autovision.clicker.models.VisualObject
import com.autovision.clicker.models.VisualPair
import kotlin.math.abs

class PairMatcher {
    fun findVisualPairs(
        top: List<VisualObject>,
        bottom: List<VisualObject>,
        mode: RecognitionMode = RecognitionMode.HYBRID,
        weights: ComparisonWeights = ComparisonWeights()
    ): List<VisualPair> {
        if (top.isEmpty() || bottom.isEmpty()) return emptyList()
        val normalized = weights.normalized()
        val scores = top.map { source ->
            bottom.map { target ->
                val shapeScore = when {
                    source.shape == target.shape -> 1.0
                    source.shape != com.autovision.clicker.models.ShapeType.UNKNOWN &&
                        target.shape != com.autovision.clicker.models.ShapeType.UNKNOWN -> .10
                    else -> (1.0 - abs(source.area - target.area) /
                        (source.area + target.area).coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
                }
                val colorScore = if (source.dominantColor == null || target.dominantColor == null) .5
                else colorDistance(source.dominantColor, target.dominantColor)
                val geometryScore = (1.0 - abs(source.bounds.width() - target.bounds.width()).toDouble() /
                    source.bounds.width().coerceAtLeast(target.bounds.width()).coerceAtLeast(1)).coerceIn(0.0, 1.0)
                val featureScore = (geometryScore + sizeScore(source, target)) / 2.0
                val score = when (mode) {
                    RecognitionMode.COLOR -> colorScore * .75 + geometryScore * .25
                    RecognitionMode.SHAPE -> shapeScore * normalized.shape +
                        featureScore * normalized.feature
                    RecognitionMode.HYBRID -> shapeScore * normalized.shape +
                        featureScore * normalized.feature + colorScore * normalized.color
                }
                score.coerceIn(0.0, 1.0)
            }
        }
        // Exhaustive assignment is intentional here: the number of detected
        // cards is small, and it avoids a locally good match stealing the
        // only globally correct candidate from a later card.
        var bestTotal = Double.NEGATIVE_INFINITY
        var bestPairs = emptyList<VisualPair>()
        fun search(sourceIndex: Int, used: BooleanArray, selected: List<VisualPair>, total: Double) {
            if (sourceIndex == top.size) {
                if (total > bestTotal) {
                    bestTotal = total
                    bestPairs = selected
                }
                return
            }
            for (targetIndex in bottom.indices) {
                if (!used[targetIndex]) {
                    used[targetIndex] = true
                    val pair = VisualPair(top[sourceIndex], bottom[targetIndex], scores[sourceIndex][targetIndex])
                    search(sourceIndex + 1, used, selected + pair, total + pair.score)
                    used[targetIndex] = false
                }
            }
        }
        search(0, BooleanArray(bottom.size), emptyList(), 0.0)
        return bestPairs
    }

    private fun colorDistance(first: Int, second: Int): Double {
        val dr = ((first shr 16 and 0xff) - (second shr 16 and 0xff)).toDouble()
        val dg = ((first shr 8 and 0xff) - (second shr 8 and 0xff)).toDouble()
        val db = ((first and 0xff) - (second and 0xff)).toDouble()
        return (1.0 - (dr * dr + dg * dg + db * db).let { kotlin.math.sqrt(it) } / 441.67).coerceIn(0.0, 1.0)
    }

    private fun sizeScore(first: VisualObject, second: VisualObject): Double {
        val firstArea = first.bounds.width().toDouble() * first.bounds.height()
        val secondArea = second.bounds.width().toDouble() * second.bounds.height()
        return (1.0 - abs(firstArea - secondArea) / (firstArea + secondArea).coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
    }
}