package com.autovision.clicker.ui.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.autovision.clicker.MainActivity

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlay: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlay == null) showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 12)
            setBackgroundColor(Color.argb(230, 18, 26, 46))
        }
        val label = TextView(this).apply {
            text = "AutoVision  ● ON"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        val button = TextView(this).apply {
            text = "Abrir dashboard"
            setTextColor(Color.rgb(101, 228, 255))
            setPadding(0, 12, 0, 0)
            setOnClickListener {
                startActivity(Intent(this@OverlayService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        panel.addView(label)
        panel.addView(button)
        overlay = panel
        val type = if (android.os.Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 20; y = 160 }
        panel.setOnTouchListener(DragTouchListener(params))
        windowManager?.addView(panel, params)
    }

    override fun onDestroy() {
        overlay?.let { windowManager?.removeView(it) }
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DragTouchListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    windowManager?.updateViewLayout(v, params)
                }
            }
            return true
        }
    }
}