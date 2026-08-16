package com.shrey.languardian

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * The dashboard screen: shows a live connection status pill, a stats bar
 * (total/warning/critical counts), and a scrolling feed of anomaly alerts.
 *
 * IMPORTANT - change SERVER_URL before running:
 * - Android emulator + server running on the same PC -> "ws://10.0.2.2:8765"
 * - Real physical phone on the same Wi-Fi as the server -> "ws://<PC's LAN IP>:8765"
 *   (find the LAN IP with `ipconfig` / `ifconfig` on the server machine)
 */
class MainActivity : AppCompatActivity() {

    // TODO: set this to match how you're running the server - see comment above
    private val serverUrl = "ws://10.0.2.2:8765"

    private lateinit var statusPill: LinearLayout
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var totalCount: TextView
    private lateinit var warningCount: TextView
    private lateinit var criticalCount: TextView
    private lateinit var emptyStateContainer: View

    private lateinit var alertAdapter: AlertAdapter
    private lateinit var webSocketManager: WebSocketManager
    private var pulseAnimator: ObjectAnimator? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusPill = findViewById(R.id.statusPill)
        statusDot = findViewById(R.id.statusDot)
        statusText = findViewById(R.id.statusText)
        totalCount = findViewById(R.id.totalCount)
        warningCount = findViewById(R.id.warningCount)
        criticalCount = findViewById(R.id.criticalCount)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)

        val recyclerView: RecyclerView = findViewById(R.id.alertRecyclerView)
        alertAdapter = AlertAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = alertAdapter

        alertAdapter.onCountsChanged = { total, warning, critical ->
            totalCount.text = total.toString()
            warningCount.text = warning.toString()
            criticalCount.text = critical.toString()
            emptyStateContainer.visibility = if (total == 0) View.VISIBLE else View.GONE
        }

        NotificationHelper.createChannel(this)
        requestNotificationPermissionIfNeeded()
        setConnectionState(connected = false)

        webSocketManager = WebSocketManager(
            serverUrl = serverUrl,
            onAlert = { alert ->
                alertAdapter.addAlert(alert)
                if (alert.isCritical) {
                    NotificationHelper.showAlert(this, alert)
                }
            },
            onConnectionChange = { connected -> setConnectionState(connected) }
        )
    }

    override fun onStart() {
        super.onStart()
        webSocketManager.connect()
    }

    override fun onStop() {
        super.onStop()
        webSocketManager.disconnect()
        pulseAnimator?.cancel()
    }

    /**
     * Updates the status pill's color/text and starts or stops the
     * breathing animation on the status dot - the one deliberately
     * animated element in this UI, signaling "live" the way a real
     * monitoring console would.
     */
    private fun setConnectionState(connected: Boolean) {
        val colorRes = if (connected) R.color.color_accent else R.color.color_offline
        val color = ContextCompat.getColor(this, colorRes)

        statusPill.backgroundTintList = ColorStateList.valueOf(color)
        statusText.text = if (connected) "CONNECTED" else "OFFLINE"

        pulseAnimator?.cancel()
        if (connected) {
            pulseAnimator = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.35f).apply {
                duration = 900
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else {
            statusDot.alpha = 1f
        }
    }

    /** Android 13+ requires explicit runtime permission to post notifications. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
