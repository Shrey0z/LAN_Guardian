package com.shrey.languardian

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Connects to the Python alert_server.py over WebSocket and forwards
 * parsed Alert objects to whoever is listening.
 *
 * Design notes:
 * - Auto-reconnects with a fixed backoff. On a real LAN, your phone's
 *   Wi-Fi will drop the connection sometimes (screen off, roaming
 *   between rooms) - a dashboard that silently stops working is worse
 *   than useless, it's misleading. Reconnect logic is not optional here.
 * - Callbacks are dispatched on the main thread so MainActivity can
 *   update UI directly without you having to remember runOnUiThread
 *   everywhere.
 */
class WebSocketManager(
    private val serverUrl: String, // e.g. "ws://192.168.1.5:8765"
    private val onAlert: (Alert) -> Unit,
    private val onConnectionChange: (connected: Boolean) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSockets are long-lived, no read timeout
        .build()

    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shouldReconnect = true
    private val reconnectDelayMs = 5000L

    fun connect() {
        shouldReconnect = true
        openSocket()
    }

    private fun openSocket() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                mainHandler.post { onConnectionChange(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val alert = Alert.fromJson(text) ?: return
                mainHandler.post { onAlert(alert) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                mainHandler.post { onConnectionChange(false) }
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("WebSocket", "Connection failed: ${t.message}", t)
                mainHandler.post { onConnectionChange(false) }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        mainHandler.postDelayed({
            if (shouldReconnect) openSocket()
        }, reconnectDelayMs)
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "Activity destroyed")
        webSocket = null
    }
}
