package com.autovision.clicker.automation

import android.graphics.Bitmap
import com.autovision.clicker.accessibility.AccessibilityController
import com.autovision.clicker.models.ActionType
import com.autovision.clicker.models.AutomationAction
import com.autovision.clicker.models.AutomationConfig
import com.autovision.clicker.models.AppStatus
import com.autovision.clicker.models.VisionResult
import com.autovision.clicker.utils.AppLogger
import com.autovision.clicker.vision.VisionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AutomationEngine(
    private val accessibility: AccessibilityController,
    private val vision: VisionEngine
) {
    private val _status = MutableStateFlow(AppStatus.READY)
    val status = _status.asStateFlow()
    private var clicksThisMinute = 0

    suspend fun execute(
        actions: List<AutomationAction>,
        config: AutomationConfig,
        frameProvider: suspend () -> Bitmap?
    ) = withContext(Dispatchers.Default) {
        _status.value = AppStatus.RUNNING
        clicksThisMinute = 0
        try {
            repeat(config.repetitions.coerceAtLeast(1)) {
                executeActions(actions, config, frameProvider)
            }
            _status.value = AppStatus.READY
            AppLogger.info("Sequência concluída")
        } catch (cancelled: CancellationException) {
            _status.value = AppStatus.PAUSED
            AppLogger.info("Automação pausada")
            throw cancelled
        } catch (error: Exception) {
            _status.value = AppStatus.ERROR
            AppLogger.error("Automação interrompida: ${error.message}", error)
        }
    }

    fun pause() {
        _status.value = AppStatus.PAUSED
        AppLogger.info("Pausa solicitada")
    }

    fun stop() {
        _status.value = AppStatus.READY
        AppLogger.info("Parada imediata")
    }

    private suspend fun executeActions(
        actions: List<AutomationAction>,
        config: AutomationConfig,
        frameProvider: suspend () -> Bitmap?
    ) {
        actions.forEach { action ->
            when (action.type) {
                ActionType.CLICK -> executeClick(action, frameProvider, config)
                ActionType.DOUBLE_CLICK -> {
                    executeClick(action, frameProvider, config)
                    delay(80)
                    executeClick(action, frameProvider, config)
                }
                ActionType.LONG_PRESS -> executeClick(action, frameProvider, config, action.durationMs.coerceAtLeast(700))
                ActionType.SWIPE -> AppLogger.info("SWIPE requer pontos definidos no editor de sequência")
                ActionType.WAIT -> delay(action.durationMs)
                ActionType.IMAGE_MATCH, ActionType.COLOR_MATCH -> executeClick(action, frameProvider, config)
                ActionType.SEQUENCE -> executeActions(action.children, config, frameProvider)
            }
            delay(action.pauseAfterMs.coerceAtLeast(config.intervalMs))
        }
    }

    private suspend fun executeClick(
        action: AutomationAction,
        frameProvider: suspend () -> Bitmap?,
        config: AutomationConfig,
        duration: Long = 60
    ) {
        if (clicksThisMinute >= config.maxClicksPerMinute) {
            _status.value = AppStatus.PAUSED
            throw IllegalStateException("Limite de cliques por minuto atingido")
        }
        val frame = frameProvider() ?: return AppLogger.info("Nenhum frame disponível; clique ignorado")
        val result: VisionResult = vision.analyze(frame, mode = config.mode,
            weights = config.weights, minConfidence = action.confidenceRequired ?: config.minConfidence)
        val pair = result.pairs.firstOrNull()
        if (pair == null) {
            AppLogger.info("Correspondência ambígua ou abaixo da confidence mínima; não clicar")
            return
        }
        if (accessibility.click(pair.bottom.centerX, pair.bottom.centerY, duration)) {
            clicksThisMinute++
            AppLogger.info("Clique executado em ${pair.bottom.centerX},${pair.bottom.centerY}")
        } else {
            AppLogger.error("AccessibilityService recusou o gesto")
        }
    }
}