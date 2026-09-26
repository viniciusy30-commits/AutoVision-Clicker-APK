package com.ahmedsamy.app

import com.ahmedsamy.app.util.StringUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilTest {

    @Test
    fun truncateMiddleShortTextIsUnchanged() {
        assertEquals("hello", StringUtil.truncateMiddle("hello", 10))
    }

    @Test
    fun truncateMiddleExactLengthIsUnchanged() {
        assertEquals("hello", StringUtil.truncateMiddle("hello", 5))
    }

    @Test
    fun truncateMiddleLongTextKeepsMiddleEllipsis() {
        assertEquals("abc...kl", StringUtil.truncateMiddle("abcdefghijkl", 8))
    }

    @Test
    fun truncateMiddleMaxLengthThreeReturnsHead() {
        assertEquals("abc", StringUtil.truncateMiddle("abcdefgh", 3))
    }

    @Test
    fun truncateMiddleMaxLengthTwoReturnsHead() {
        assertEquals("ab", StringUtil.truncateMiddle("abcdefgh", 2))
    }

    @Test
    fun truncateMiddleNonPositiveMaxReturnsEmpty() {
        assertEquals("", StringUtil.truncateMiddle("abc", 0))
    }

    @Test
    fun truncateMiddleEmptyTextReturnsEmpty() {
        assertEquals("", StringUtil.truncateMiddle("", 10))
    }

    @Test
    fun truncateMiddleAsciiAndUnicodeLengthAreConsistent() {
        assertEquals("aab...cd", StringUtil.truncateMiddle("aabbbbbcccd", 8))
    }
}
