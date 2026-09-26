package com.autovision.clicker

import android.os.Bundle
import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autovision.clicker.ui.DashboardRoute
import com.autovision.clicker.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            mainViewModel?.startCapture(result.resultCode, result.data!!)
        }
    }
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            mainViewModel = vm
            DashboardRoute(vm)
        }
    }

    fun requestScreenCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        capturePermission.launch(manager.createScreenCaptureIntent())
    }

    override fun onResume() {
        super.onResume()
        mainViewModel?.refreshServiceStatus()
    }
}