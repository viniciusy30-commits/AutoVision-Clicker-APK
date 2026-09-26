package com.autovision.clicker.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.provider.Settings
import android.content.Context
import com.autovision.clicker.utils.AppLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AccessibilityController(private val context: Context) {
    fun isEnabled(): Boolean = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )?.contains(context.packageName, ignoreCase = true) == true

    suspend fun click(x: Int, y: Int, durationMs: Long = 60): Boolean {
        val service = AutoVisionAccessibilityService.instance
            ?: return false.also { AppLogger.error("AccessibilityService indisponível") }
        return dispatch(service, gesture(x, y, durationMs))
    }

    suspend fun longPress(x: Int, y: Int, durationMs: Long): Boolean {
        val service = AutoVisionAccessibilityService.instance ?: return false
        return dispatch(service, gesture(x, y, durationMs.coerceAtLeast(500)))
    }

    suspend fun swipe(from: Point, to: Point, durationMs: Long): Boolean {
        val service = AutoVisionAccessibilityService.instance ?: return false
        val path = Path().apply {
            moveTo(from.x.toFloat(), from.y.toFloat())
            lineTo(to.x.toFloat(), to.y.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100)))
            .build()
        return dispatch(service, gesture)
    }

    private fun gesture(x: Int, y: Int, duration: Long): GestureDescription {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
    }

    private suspend fun dispatch(
        service: AutoVisionAccessibilityService,
        gesture: GestureDescription
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val accepted = service.dispatchGesture(gesture, object :
            android.accessibilityservice.AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (continuation.isActive) continuation.resume(false)
            }
        }, null)
        if (!accepted && continuation.isActive) continuation.resume(false)
    }
}