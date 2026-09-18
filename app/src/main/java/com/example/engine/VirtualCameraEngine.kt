package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.model.ResolutionOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VirtualCameraState(
    val isVirtualCamOn: Boolean = true,
    val isPlaying: Boolean = true,
    val isLooping: Boolean = true,
    val isServerBroadcasting: Boolean = false,
    val selectedVideoUri: Uri? = null,
    val videoName: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 10000L,
    val resolution: ResolutionOption = ResolutionOption.RES_720P,
    val targetFps: Int = 30,
    val frameCount: Long = 0L,
    val connectedClients: Int = 0,
    val serverUrl: String = ""
)

class VirtualCameraEngine(private val context: Context) {

    private val _state = MutableStateFlow(VirtualCameraState())
    val state: StateFlow<VirtualCameraState> = _state.asStateFlow()

    private var retriever: MediaMetadataRetriever? = null
    private var renderJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var currentFrameBitmap: Bitmap? = null

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var frameCounter = 0L
    private var playbackTimeMs = 0L

    init {
        startFrameGenerator()
    }

    fun setVideo(uri: Uri, displayName: String?) {
        scope.launch {
            try {
                retriever?.release()
                val newRetriever = MediaMetadataRetriever()
                newRetriever.setDataSource(context, uri)
                val durationStr = newRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 10000L

                retriever = newRetriever
                playbackTimeMs = 0L
                _state.value = _state.value.copy(
                    selectedVideoUri = uri,
                    videoName = displayName ?: "Custom Video Source",
                    durationMs = duration,
                    currentPositionMs = 0L,
                    isPlaying = true
                )
            } catch (e: Exception) {
                Log.e("VirtualCameraEngine", "Failed to load video: ${e.message}", e)
            }
        }
    }

    fun clearVideo() {
        retriever?.release()
        retriever = null
        playbackTimeMs = 0L
        _state.value = _state.value.copy(
            selectedVideoUri = null,
            videoName = null,
            durationMs = 10000L,
            currentPositionMs = 0L
        )
    }

    fun togglePlay() {
        _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
    }

    fun toggleLoop() {
        _state.value = _state.value.copy(isLooping = !_state.value.isLooping)
    }

    fun toggleCameraPower() {
        _state.value = _state.value.copy(isVirtualCamOn = !_state.value.isVirtualCamOn)
    }

    fun setResolution(res: ResolutionOption) {
        _state.value = _state.value.copy(resolution = res)
    }

    fun setFps(fps: Int) {
        _state.value = _state.value.copy(targetFps = fps)
    }

    fun seekTo(positionMs: Long) {
        playbackTimeMs = positionMs.coerceIn(0L, _state.value.durationMs)
        _state.value = _state.value.copy(currentPositionMs = playbackTimeMs)
    }

    fun setServerStatus(broadcasting: Boolean, url: String) {
        _state.value = _state.value.copy(
            isServerBroadcasting = broadcasting,
            serverUrl = url
        )
    }

    fun updateClientCount(count: Int) {
        _state.value = _state.value.copy(connectedClients = count)
    }

    fun getLatestFrame(): Bitmap? {
        return currentFrameBitmap
    }

    private fun startFrameGenerator() {
        renderJob?.cancel()
        renderJob = scope.launch {
            while (isActive) {
                val currentState = _state.value
                val delayMs = (1000L / currentState.targetFps.coerceIn(1, 60))
                val startCycle = System.currentTimeMillis()

                if (currentState.isPlaying) {
                    playbackTimeMs += delayMs
                    if (playbackTimeMs >= currentState.durationMs) {
                        if (currentState.isLooping) {
                            playbackTimeMs = 0L
                        } else {
                            playbackTimeMs = currentState.durationMs
                            _state.value = _state.value.copy(isPlaying = false)
                        }
                    }
                }

                frameCounter++
                _state.value = _state.value.copy(
                    currentPositionMs = playbackTimeMs,
                    frameCount = frameCounter
                )

                val bmp = generateFrame(currentState, playbackTimeMs)
                currentFrameBitmap = bmp

                val elapsed = System.currentTimeMillis() - startCycle
                val sleepTime = (delayMs - elapsed).coerceAtLeast(5L)
                delay(sleepTime)
            }
        }
    }

    private fun generateFrame(state: VirtualCameraState, posMs: Long): Bitmap {
        val width = state.resolution.width
        val height = state.resolution.height

        // If camera power is OFF, render Standby card
        if (!state.isVirtualCamOn) {
            return drawStandbyPattern(width, height)
        }

        // If video is selected, try extracting frame from video
        val currentRetriever = retriever
        if (state.selectedVideoUri != null && currentRetriever != null) {
            try {
                val timeUs = posMs * 1000L
                val frame = currentRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    val scaled = if (frame.width != width || frame.height != height) {
                        Bitmap.createScaledBitmap(frame, width, height, true)
                    } else {
                        frame
                    }
                    // Overlay subtle broadcast timecode watermark
                    return overlayTimecode(scaled, state, posMs)
                }
            } catch (_: Exception) {}
        }

        // Default: Generate real-time studio calibration & test pattern
        return drawCalibrationPattern(width, height, state, posMs)
    }

    private fun drawStandbyPattern(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0F172A"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw diagonal warning lines or standby card
        paint.color = Color.parseColor("#EF4444")
        paint.strokeWidth = 6f
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, paint.apply { style = Paint.Style.STROKE })

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = height * 0.08f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VIRTUAL CAMERA MUTED", width / 2f, height * 0.45f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = height * 0.045f
        canvas.drawText("DEVICE IS IN STANDBY (OFF)", width / 2f, height * 0.55f, paint)

        val timeStr = timeFormat.format(Date())
        paint.color = Color.parseColor("#F59E0B")
        paint.textSize = height * 0.035f
        canvas.drawText(timeStr, width / 2f, height * 0.65f, paint)

        return bitmap
    }

    private fun drawCalibrationPattern(width: Int, height: Int, state: VirtualCameraState, posMs: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw SMPTE style color bars on top 70%
        val barHeight = (height * 0.68f).toInt()
        val colors = intArrayOf(
            Color.parseColor("#C0C0C0"), // Grey
            Color.parseColor("#C0C000"), // Yellow
            Color.parseColor("#00C0C0"), // Cyan
            Color.parseColor("#00C000"), // Green
            Color.parseColor("#C000C0"), // Magenta
            Color.parseColor("#C00000"), // Red
            Color.parseColor("#0000C0")  // Blue
        )
        val barWidth = width.toFloat() / colors.size
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in colors.indices) {
            paint.color = colors[i]
            canvas.drawRect(i * barWidth, 0f, (i + 1) * barWidth, barHeight.toFloat(), paint)
        }

        // Draw bottom info control block
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(0f, barHeight.toFloat(), width.toFloat(), height.toFloat(), paint)

        // Animated scan line or pendulum in lower area
        val cycle = ((posMs % 2000L) / 2000f) * 2 * Math.PI
        val sweepX = ((Math.sin(cycle) + 1.0) / 2.0).toFloat() * width
        paint.color = Color.parseColor("#22D3EE")
        paint.strokeWidth = 4f
        canvas.drawLine(sweepX, barHeight.toFloat(), sweepX, height.toFloat(), paint)

        // Center card overlay
        val cardWidth = width * 0.65f
        val cardHeight = height * 0.28f
        val cardLeft = (width - cardWidth) / 2f
        val cardTop = (barHeight - cardHeight) / 2f

        paint.color = Color.parseColor("#CC0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight, 16f, 16f, paint)

        // Border
        paint.color = Color.parseColor("#38BDF8")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight, 16f, 16f, paint)

        // Title text
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.textSize = cardHeight * 0.26f
        canvas.drawText("VIRTUAL CAMERA ACTIVE", width / 2f, cardTop + cardHeight * 0.38f, paint)

        paint.color = Color.parseColor("#34D399")
        paint.textSize = cardHeight * 0.18f
        canvas.drawText("● LIVE BROADCAST | ${state.resolution.label} @ ${state.targetFps} FPS", width / 2f, cardTop + cardHeight * 0.64f, paint)

        val timeStr = timeFormat.format(Date())
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = cardHeight * 0.14f
        canvas.drawText("TIME: $timeStr | FRAME: #${state.frameCount}", width / 2f, cardTop + cardHeight * 0.86f, paint)

        return bitmap
    }

    private fun overlayTimecode(bitmap: Bitmap, state: VirtualCameraState, posMs: Long): Bitmap {
        val mutable = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Subtle bottom banner
        val bannerHeight = (mutable.height * 0.08f).coerceAtLeast(36f)
        paint.color = Color.parseColor("#99000000")
        canvas.drawRect(0f, mutable.height - bannerHeight, mutable.width.toFloat(), mutable.height.toFloat(), paint)

        paint.color = Color.parseColor("#34D399")
        paint.textSize = bannerHeight * 0.45f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("● VIRTUAL CAM | ${state.resolution.label} | ${state.targetFps}fps", 16f, mutable.height - bannerHeight * 0.35f, paint)

        val sec = posMs / 1000
        val mm = sec / 60
        val ss = sec % 60
        val timecode = String.format(Locale.US, "%02d:%02d / %02d:%02d", mm, ss, (state.durationMs / 1000) / 60, (state.durationMs / 1000) % 60)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(timecode, mutable.width - 16f, mutable.height - bannerHeight * 0.35f, paint)

        return mutable
    }

    fun release() {
        renderJob?.cancel()
        retriever?.release()
        retriever = null
        currentFrameBitmap?.recycle()
        currentFrameBitmap = null
    }
}
