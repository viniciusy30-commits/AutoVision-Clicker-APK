package com.autovision.clicker.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import com.autovision.clicker.utils.AppLogger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.ByteBuffer

class ScreenCaptureManager(private val context: Context) {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var stopping = false
    private val _frames = MutableSharedFlow<Bitmap>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val frames = _frames.asSharedFlow()
    var width: Int = 0
        private set
    var height: Int = 0
        private set

    fun start(resultCode: Int, data: Intent, requestedWidth: Int, requestedHeight: Int, density: Int) {
        stop()
        stopping = false
        width = requestedWidth
        height = requestedHeight
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as android.media.projection.MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        worker = HandlerThread("screen-capture").also { it.start() }
        handler = Handler(worker!!.looper)
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ imageReader ->
            imageReader.acquireLatestImage()?.use { image ->
                val plane = image.planes.firstOrNull() ?: return@use
                val buffer: ByteBuffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmapWidth = width + rowPadding / pixelStride
                val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                val cropped = if (bitmapWidth != width) {
                    Bitmap.createBitmap(bitmap, 0, 0, width, height)
                } else bitmap
                if (cropped !== bitmap) bitmap.recycle()
                _frames.tryEmit(cropped)
            }
        }, handler)
        display = projection?.createVirtualDisplay(
            "AutoVisionCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface,
            null,
            handler
        )
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                AppLogger.info("Captura de tela encerrada pelo sistema")
                if (!stopping) stop()
            }
        }, handler)
        AppLogger.info("Captura de tela ativa (${width}x$height)")
    }

    fun stop() {
        stopping = true
        display?.release()
        display = null
        reader?.close()
        reader = null
        projection?.stop()
        projection = null
        worker?.quitSafely()
        worker = null
        handler = null
        stopping = false
    }
}