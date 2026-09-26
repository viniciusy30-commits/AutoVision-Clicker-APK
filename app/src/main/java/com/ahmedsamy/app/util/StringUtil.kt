package com.ahmedsamy.app.util

object StringUtil {

    fun truncateMiddle(text: String, maxLength: Int): String {
        if (maxLength <= 0) return ""
        if (text.length <= maxLength) return text
        if (maxLength <= 3) return text.take(maxLength)
        val keep = maxLength - 3
        val head = (keep + 1) / 2
        val tail = keep / 2
        return text.take(head) + "..." + text.takeLast(tail)
    }
}
