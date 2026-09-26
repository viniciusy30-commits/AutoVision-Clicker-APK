package com.autovision.clicker.ui

import android.app.Application
import android.graphics.Bitmap
import android.content.Intent
import android.content.Context
import android.view.WindowManager
import android.util.DisplayMetrics
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autovision.clicker.accessibility.AccessibilityController
import com.autovision.clicker.automation.AutomationEngine
import com.autovision.clicker.capture.CaptureForegroundService
import com.autovision.clicker.capture.ScreenCaptureManager
import com.autovision.clicker.models.ActionType
import com.autovision.clicker.models.AutomationAction
import com.autovision.clicker.models.AppStatus
import com.autovision.clicker.models.AutomationConfig
import com.autovision.clicker.models.RecognitionMode
import com.autovision.clicker.models.VisionResult
import com.autovision.clicker.settings.AppSettings
import com.autovision.clicker.utils.AppLogger
import com.autovision.clicker.vision.VisionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class DashboardState(
    val config: AutomationConfig = AutomationConfig(),
    val status: AppStatus = AppStatus.READY,
    val accessibilityEnabled: Boolean = false,
    val captureEnabled: Boolean = false,
    val visualDebug: Boolean = true,
    val result: VisionResult = VisionResult(),
    val lastFrame: Bitmap? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = AppSettings(application)
    private val accessibility = AccessibilityController(application)
    private val vision = VisionEngine()
    private val engine = AutomationEngine(accessibility, vision)
    private val capture = ScreenCaptureManager(application)
    private val config = MutableStateFlow(AutomationConfig())
    private val captureEnabled = MutableStateFlow(false)
    private val lastFrame = MutableStateFlow<Bitmap?>(null)
    private val result = MutableStateFlow(VisionResult())
    private val debug = MutableStateFlow(true)
    private val serviceStatusVersion = MutableStateFlow(0)
    private var automationJob: Job? = null

    val state: StateFlow<DashboardState> = combine(
        config, engine.status, captureEnabled, debug, lastFrame, result, serviceStatusVersion
    ) { values ->
        DashboardState(
            config = values[0] as AutomationConfig,
            status = values[1] as AppStatus,
            accessibilityEnabled = accessibility.isEnabled(),
            captureEnabled = values[2] as Boolean,
            visualDebug = values[3] as Boolean,
            lastFrame = values[4] as Bitmap?,
            result = values[5] as VisionResult
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init {
        viewModelScope.launch {
            settings.config.collect { config.value = it }
        }
        viewModelScope.launch {
            settings.visualDebug.collect { debug.value = it }
        }
        viewModelScope.launch {
            capture.frames.collect { frame ->
                lastFrame.value = frame
                captureEnabled.value = true
            }
        }
    }

    fun refreshServiceStatus() {
        serviceStatusVersion.value++
    }

    fun updateConfig(newConfig: AutomationConfig) {
        config.value = newConfig
        viewModelScope.launch { settings.saveConfig(newConfig) }
    }

    fun setVisualDebug(enabled: Boolean) {
        debug.value = enabled
        viewModelScope.launch { settings.setVisualDebug(enabled) }
    }

    fun analyzeDemo() {
        viewModelScope.launch {
            val demo = DemoImageFactory.create()
            lastFrame.value = demo
            result.value = vision.analyze(
                demo,
                mode = config.value.mode,
                weights = config.value.weights,
                minConfidence = config.value.minConfidence
            )
        }
    }

    fun analyze(bitmap: Bitmap) {
        viewModelScope.launch {
            lastFrame.value = bitmap
            result.value = vision.analyze(bitmap, mode = config.value.mode,
                weights = config.value.weights, minConfidence = config.value.minConfidence)
        }
    }

    fun startCapture(resultCode: Int, data: Intent) {
        val metrics = DisplayMetrics()
        val window = getApplication<Application>().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        window.defaultDisplay.getRealMetrics(metrics)
        getApplication<Application>().startForegroundService(
            Intent(getApplication(), CaptureForegroundService::class.java)
        )
        capture.start(resultCode, data, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        captureEnabled.value = true
    }

    fun stopCapture() {
        capture.stop()
        getApplication<Application>().stopService(Intent(getApplication(), CaptureForegroundService::class.java))
        captureEnabled.value = false
    }

    fun startAutomation() {
        automationJob?.cancel()
        automationJob = viewModelScope.launch {
            engine.execute(
                actions = listOf(AutomationAction(ActionType.IMAGE_MATCH, label = "Encontrar e clicar")),
                config = config.value,
                frameProvider = { lastFrame.value }
            )
        }
    }

    fun pause() {
        automationJob?.cancel()
        engine.pause()
    }

    fun stop() {
        automationJob?.cancel()
        engine.stop()
    }
    fun logs() = AppLogger.entries

    override fun onCleared() {
        capture.stop()
        super.onCleared()
    }
}

private object DemoImageFactory {
    fun create(): Bitmap {
        val bitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(11, 16, 32))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val top = listOf(android.graphics.Color.rgb(101, 228, 255), android.graphics.Color.rgb(167, 139, 250), android.graphics.Color.rgb(121, 242, 192))
        val bottom = listOf(top[2], top[0], top[1])
        top.forEachIndexed { index, color ->
            paint.color = color
            canvas.drawRoundRect(100f + index * 250, 90f, 260f + index * 250, 230f, 24f, 24f, paint)
        }
        bottom.forEachIndexed { index, color ->
            paint.color = color
            canvas.drawRoundRect(100f + index * 250, 360f, 260f + index * 250, 500f, 24f, 24f, paint)
        }
        return bitmap
    }
}