package com.autovision.clicker.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "AutoVision"
    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries = _entries.asStateFlow()

    fun info(message: String) {
        val line = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())} - $message"
        Log.i(TAG, message)
        _entries.value = (_entries.value + line).takeLast(100)
    }

    fun error(message: String, throwable: Throwable? = null) {
        val line = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())} - ERRO: $message"
        Log.e(TAG, message, throwable)
        _entries.value = (_entries.value + line).takeLast(100)
    }

    fun clear() {
        _entries.value = emptyList()
    }
}