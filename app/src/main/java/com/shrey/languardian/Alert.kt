package com.shrey.languardian

import org.json.JSONObject

/**
 * Mirrors the JSON alert_server.py broadcasts over the WebSocket:
 * {
 *   "type": "alert",
 *   "severity": "critical" | "warning",
 *   "mac": "DE:AD:BE:EF:13:37",
 *   "ip": "192.168.1.10",
 *   "explanation": "IP address claimed by multiple MAC addresses...",
 *   "timestamp": 1234567890.123
 * }
 *
 * Keep this in sync with the Python side manually - there's no shared
 * schema between the two languages, so if you add a field on the server,
 * add it here too.
 */
data class Alert(
    val severity: String,
    val mac: String,
    val ip: String,
    val explanation: String,
    val timestampSeconds: Double
) {
    val isCritical: Boolean
        get() = severity.equals("critical", ignoreCase = true)

    companion object {
        /**
         * Parses a raw WebSocket text message into an Alert, or null if the
         * message isn't a well-formed alert (defensive - never trust
         * network input, even from your own server).
         */
        fun fromJson(raw: String): Alert? {
            return try {
                val obj = JSONObject(raw)
                if (obj.optString("type") != "alert") return null

                Alert(
                    severity = obj.optString("severity", "warning"),
                    mac = obj.optString("mac", "unknown"),
                    ip = obj.optString("ip", "unknown"),
                    explanation = obj.optString("explanation", "No details provided"),
                    timestampSeconds = obj.optDouble("timestamp", 0.0)
                )
            } catch (e: Exception) {
                // Malformed JSON from the server shouldn't crash the app -
                // log and drop the message instead.
                null
            }
        }
    }
}
