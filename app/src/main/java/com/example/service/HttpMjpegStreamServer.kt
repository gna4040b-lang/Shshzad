package com.example.service

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class HttpMjpegStreamServer(
    private val port: Int = 8080,
    private val frameProvider: () -> Bitmap?
) {
    private val tag = "HttpMjpegServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)

    private val connectedClients = CopyOnWriteArrayList<Socket>()
    val clientCount = AtomicInteger(0)
    var isRunning: Boolean = false
        private set

    fun start(onStatusChange: (Boolean) -> Unit) {
        if (isRunning) return
        serverJob = serverScope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                isRunning = true
                onStatusChange(true)
                Log.i(tag, "Server started on port $port")

                while (isActive && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        handleClient(client)
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Server error: ${e.message}", e)
            } finally {
                stop()
                onStatusChange(false)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            connectedClients.forEach {
                try {
                    it.close()
                } catch (_: Exception) {}
            }
            connectedClients.clear()
            clientCount.set(0)
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(tag, "Error closing server: ${e.message}")
        }
        serverJob?.cancel()
        serverJob = null
    }

    private fun handleClient(socket: Socket) {
        serverScope.launch {
            connectedClients.add(socket)
            clientCount.incrementAndGet()
            try {
                val input = socket.getInputStream().bufferedReader()
                val requestLine = input.readLine() ?: return@launch
                var line = input.readLine()
                while (!line.isNullOrEmpty()) {
                    line = input.readLine()
                }

                val output = socket.getOutputStream()
                if (requestLine.contains("/videostream") || requestLine.contains("/stream")) {
                    streamMjpeg(output, socket)
                } else if (requestLine.contains("/snapshot")) {
                    serveSnapshot(output)
                } else {
                    serveDashboardHtml(output)
                }
            } catch (e: Exception) {
                // Client disconnected
            } finally {
                connectedClients.remove(socket)
                clientCount.decrementAndGet()
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun serveDashboardHtml(output: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Android Virtual Camera Stream</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; text-align: center; }
                    .card { max-width: 720px; margin: 0 auto; background: #1e293b; border-radius: 16px; padding: 24px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    h1 { color: #22d3ee; margin-top: 0; font-size: 24px; }
                    .video-container { position: relative; width: 100%; border-radius: 12px; overflow: hidden; background: #000; margin: 16px 0; border: 2px solid #334155; }
                    img.feed { width: 100%; height: auto; display: block; }
                    .badge { display: inline-block; padding: 6px 14px; border-radius: 9999px; font-weight: bold; font-size: 13px; text-transform: uppercase; }
                    .badge-live { background: #ef4444; color: white; }
                    .info { color: #94a3b8; font-size: 14px; line-height: 1.6; text-align: left; background: #0f172a; padding: 16px; border-radius: 8px; margin-top: 16px; }
                    code { background: #334155; padding: 2px 6px; border-radius: 4px; color: #38bdf8; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div style="margin-bottom: 12px;"><span class="badge badge-live">● Live Virtual Cam Feed</span></div>
                    <h1>Virtual Camera Studio</h1>
                    <p style="color: #94a3b8; margin: 0 0 16px 0;">Broadcasting from Android to Windows PC / Web / OBS Studio</p>
                    
                    <div class="video-container">
                        <img class="feed" src="/videostream" alt="Virtual Camera Live Video Stream" />
                    </div>

                    <div class="info">
                        <strong>Windows PC / OBS / Client Connection:</strong>
                        <ul>
                            <li><strong>Direct Stream URL:</strong> <code>/videostream</code></li>
                            <li><strong>Windows Media Foundation Driver:</strong> Point driver network client to this URL.</li>
                            <li><strong>OBS Studio:</strong> Add <em>Media Source</em> or <em>Browser Source</em> with this URL.</li>
                            <li><strong>USB Tethering (via ADB):</strong> Run <code>adb forward tcp:8080 tcp:8080</code> then access <code>http://localhost:8080/videostream</code> on PC.</li>
                        </ul>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveSnapshot(output: OutputStream) {
        val bitmap = frameProvider() ?: return
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()

        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun streamMjpeg(output: OutputStream, socket: Socket) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Cache-Control: no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                "Pragma: no-cache\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n\r\n"
        output.write(header.toByteArray(Charsets.US_ASCII))
        output.flush()

        val stream = ByteArrayOutputStream(64 * 1024)
        while (isRunning && !socket.isClosed && socket.isConnected) {
            val bitmap = frameProvider()
            if (bitmap != null) {
                stream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val imageBytes = stream.toByteArray()

                val frameHeader = "--frame\r\n" +
                        "Content-Type: image/jpeg\r\n" +
                        "Content-Length: ${imageBytes.size}\r\n\r\n"

                output.write(frameHeader.toByteArray(Charsets.US_ASCII))
                output.write(imageBytes)
                output.write("\r\n".toByteArray(Charsets.US_ASCII))
                output.flush()
            }
            // Delay to regulate frame rate according to provider
            Thread.sleep(30)
        }
    }

    companion object {
        fun getDeviceIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val addresses = intf.inetAddresses
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
