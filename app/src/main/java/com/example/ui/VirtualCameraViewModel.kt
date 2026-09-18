package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audit.SystemCameraAuditor
import com.example.engine.VirtualCameraEngine
import com.example.engine.VirtualCameraState
import com.example.model.ResolutionOption
import com.example.model.SystemAuditResult
import com.example.service.HttpMjpegStreamServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VirtualCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = VirtualCameraEngine(application)
    private var streamServer: HttpMjpegStreamServer? = null

    val engineState: StateFlow<VirtualCameraState> = engine.state

    private val _auditResult = MutableStateFlow<SystemAuditResult?>(null)
    val auditResult: StateFlow<SystemAuditResult?> = _auditResult.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        refreshAudit()
        // Periodically monitor client connections if server is running
        viewModelScope.launch {
            while (isActive) {
                streamServer?.let { server ->
                    if (server.isRunning) {
                        engine.updateClientCount(server.clientCount.get())
                    }
                }
                delay(1000)
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun refreshAudit() {
        _auditResult.value = SystemCameraAuditor.performAudit(getApplication())
    }

    fun onVideoPicked(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver
        var displayName: String? = null
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        engine.setVideo(uri, displayName ?: "Imported Video")
    }

    fun clearVideo() {
        engine.clearVideo()
    }

    fun toggleCameraPower() {
        engine.toggleCameraPower()
    }

    fun togglePlay() {
        engine.togglePlay()
    }

    fun toggleLoop() {
        engine.toggleLoop()
    }

    fun setResolution(option: ResolutionOption) {
        engine.setResolution(option)
    }

    fun setFps(fps: Int) {
        engine.setFps(fps)
    }

    fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
    }

    fun toggleBroadcastServer() {
        val currentBroadcasting = engine.state.value.isServerBroadcasting
        if (currentBroadcasting) {
            streamServer?.stop()
            streamServer = null
            engine.setServerStatus(false, "")
        } else {
            val server = HttpMjpegStreamServer(
                port = 8080,
                frameProvider = { engine.getLatestFrame() }
            )
            streamServer = server
            server.start { running ->
                val ip = HttpMjpegStreamServer.getDeviceIpAddress()
                val url = if (running) "http://$ip:8080/videostream" else ""
                engine.setServerStatus(running, url)
            }
        }
    }

    fun getLatestFrame() = engine.getLatestFrame()

    override fun onCleared() {
        super.onCleared()
        streamServer?.stop()
        engine.release()
    }
}
