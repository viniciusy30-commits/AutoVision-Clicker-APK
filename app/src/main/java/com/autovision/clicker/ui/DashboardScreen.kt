package com.autovision.clicker.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autovision.clicker.models.AppStatus
import com.autovision.clicker.models.AutomationConfig
import com.autovision.clicker.models.RecognitionMode
import com.autovision.clicker.ui.theme.AutoVisionTheme

@Composable
fun DashboardRoute(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val logs by viewModel.logs().collectAsStateWithLifecycle()
    val context = LocalContext.current
    AutoVisionTheme {
        DashboardScreen(
            state = state,
            logs = logs,
            onAnalyzeDemo = viewModel::analyzeDemo,
            onAnalyzeImage = viewModel::analyze,
            onStartCapture = {
                (context as? com.autovision.clicker.MainActivity)
                    ?.requestScreenCapture()
            },
            onStartAutomation = viewModel::startAutomation,
            onStopCapture = viewModel::stopCapture,
            onPause = viewModel::pause,
            onStop = viewModel::stop,
            onConfigChange = viewModel::updateConfig,
            onDebugChange = viewModel::setVisualDebug
        )
    }
}

@Composable
private fun DashboardScreen(
    state: DashboardState,
    logs: List<String>,
    onAnalyzeDemo: () -> Unit,
    onAnalyzeImage: (Bitmap) -> Unit,
    onStartCapture: () -> Unit,
    onStartAutomation: () -> Unit,
    onStopCapture: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onConfigChange: (AutomationConfig) -> Unit,
    onDebugChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        // ImageDecoder would add an API 28 branch; Android 10+ makes this safe.
        val bitmap = android.graphics.ImageDecoder.decodeBitmap(
            android.graphics.ImageDecoder.createSource(
                context.contentResolver, uri
            )
        )
        onAnalyzeImage(bitmap)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("AutoVision", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("CLICKER / VISION LAB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                AssistChip(onClick = {}, label = { Text(statusLabel(state.status)) },
                    leadingIcon = { Box(Modifier.size(8.dp).background(statusColor(state.status), RoundedCornerShape(8.dp))) })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("AUTOMAÇÃO", statusLabel(state.status), Modifier.weight(1f))
                MetricCard("CAPTURA", if (state.captureEnabled) "Ativa" else "Aguardando", Modifier.weight(1f))
                MetricCard("AÇÕES", state.result.pairs.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Controles", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStartAutomation) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Iniciar")
                        }
                        Button(onClick = onAnalyzeDemo) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Executar teste")
                        }
                        OutlinedButton(onClick = onPause) { Text("Pausar") }
                        OutlinedButton(onClick = onStop) {
                            Icon(Icons.Outlined.Stop, null)
                            Spacer(Modifier.size(4.dp))
                            Text("Parar")
                        }
                    }
                    Text(
                        "Acessibilidade: ${if (state.accessibilityEnabled) "conectada" else "desativada"}  •  " +
                            "Captura: ${if (state.captureEnabled) "ativa" else "não autorizada"}",
                        color = if (state.accessibilityEnabled && state.captureEnabled)
                            MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onStartCapture) { Text("Autorizar captura") }
                        OutlinedButton(onClick = onStopCapture) { Text("Parar captura") }
                    }
                    Text("Intervalo de análise: ${state.config.analysisIntervalMs} ms")
                    Slider(
                        value = state.config.analysisIntervalMs.toFloat(),
                        onValueChange = {
                            val interval = listOf(50L, 100L, 250L, 500L, 1000L).minBy { value ->
                                kotlin.math.abs(value - it)
                            }
                            onConfigChange(state.config.copy(analysisIntervalMs = interval))
                        },
                        valueRange = 50f..1000f,
                        steps = 3
                    )
                    Text("Confidence mínima: ${(state.config.minConfidence * 100).toInt()}%")
                    Slider(
                        value = state.config.minConfidence.toFloat(),
                        onValueChange = { onConfigChange(state.config.copy(minConfidence = it.toDouble())) },
                        valueRange = .50f..1f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BugReport, null, tint = MaterialTheme.colorScheme.secondary)
                        Text("  Visual Debug", Modifier.weight(1f))
                        Switch(checked = state.visualDebug, onCheckedChange = onDebugChange)
                    }
                }
            }
        }
        item {
            Text("Modo de reconhecimento", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecognitionMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.config.mode == mode,
                        onClick = { onConfigChange(state.config.copy(mode = mode)) },
                        label = { Text(mode.name) }
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vision Lab", style = MaterialTheme.typography.titleLarge)
                    Text("Analise o cenário integrado TOP: A B C / BOTTOM: C A B ou carregue uma imagem real.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAnalyzeDemo) { Text("Cenário de teste") }
                        OutlinedButton(onClick = { picker.launch("image/*") }) {
                            Icon(Icons.Outlined.Tune, null)
                            Spacer(Modifier.size(4.dp))
                            Text("Carregar imagem")
                        }
                    }
                    state.lastFrame?.let { frame ->
                        Image(frame.asImageBitmap(), "Captura processada", Modifier.fillMaxWidth().height(180.dp))
                    }
                    if (state.result.pairs.isNotEmpty()) {
                        Text("Pares encontrados", style = MaterialTheme.typography.titleMedium)
                        state.result.pairs.forEach { pair ->
                            Text("OBJ ${pair.top.id} → OBJ ${pair.bottom.id}  •  ${(pair.score * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item { Text("Logs", style = MaterialTheme.typography.titleLarge) }
        items(logs.takeLast(12)) { line ->
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            LinearProgressIndicator(
                progress = { if (state.result.objects.isEmpty()) 0f else state.result.pairs.size.toFloat() /
                    state.result.objects.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun statusLabel(status: AppStatus) = when (status) {
    AppStatus.DISABLED -> "Desativado"
    AppStatus.READY -> "Pronto"
    AppStatus.RUNNING -> "Executando"
    AppStatus.PAUSED -> "Pausado"
    AppStatus.ERROR -> "Erro"
}

@Composable
private fun statusColor(status: AppStatus) = when (status) {
    AppStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
    AppStatus.ERROR -> MaterialTheme.colorScheme.error
    AppStatus.PAUSED -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}