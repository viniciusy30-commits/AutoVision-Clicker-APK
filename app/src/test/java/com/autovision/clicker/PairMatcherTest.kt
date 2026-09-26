package com.autovision.clicker

import android.graphics.Rect
import com.autovision.clicker.models.RecognitionMode
import com.autovision.clicker.models.ShapeType
import com.autovision.clicker.models.VisualObject
import com.autovision.clicker.vision.PairMatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class PairMatcherTest {
    @Test
    fun matchesDifferentPositionsByShapeAndGeometry() {
        fun item(id: Int, x: Int, y: Int, shape: ShapeType) = VisualObject(
            id, Rect(x, y, x + 80, y + 80), x + 40, y + 40, 6400.0, shape, .95
        )
        val top = listOf(item(1, 0, 0, ShapeType.CIRCLE), item(2, 100, 0, ShapeType.SQUARE),
            item(3, 200, 0, ShapeType.TRIANGLE))
        val bottom = listOf(item(4, 0, 100, ShapeType.TRIANGLE), item(5, 100, 100, ShapeType.CIRCLE),
            item(6, 200, 100, ShapeType.SQUARE))
        val pairs = PairMatcher().findVisualPairs(top, bottom, RecognitionMode.SHAPE)
        assertEquals(3, pairs.size)
        assertEquals(ShapeType.CIRCLE, pairs[0].bottom.shape)
        assertEquals(ShapeType.SQUARE, pairs[1].bottom.shape)
        assertEquals(ShapeType.TRIANGLE, pairs[2].bottom.shape)
    }
}