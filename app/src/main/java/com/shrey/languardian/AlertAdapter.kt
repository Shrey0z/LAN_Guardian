package com.shrey.languardian

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertAdapter(private val alerts: MutableList<Alert> = mutableListOf()) :
    RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /** Called by MainActivity whenever the list changes, to keep the stats bar in sync. */
    var onCountsChanged: ((total: Int, warning: Int, critical: Int) -> Unit)? = null

    class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val severityStripe: View = view.findViewById(R.id.severityStripe)
        val severityLabel: TextView = view.findViewById(R.id.severityLabel)
        val macIpLine: TextView = view.findViewById(R.id.macIpLine)
        val explanationLine: TextView = view.findViewById(R.id.explanationLine)
        val timeLabel: TextView = view.findViewById(R.id.timeLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        val context = holder.itemView.context

        val severityColorRes = if (alert.isCritical) R.color.color_critical else R.color.color_warning
        val severityColor = ContextCompat.getColor(context, severityColorRes)

        // Left stripe: solid severity color
        holder.severityStripe.setBackgroundColor(severityColor)

        // Badge: tint the shared pill drawable's background at runtime, dark text on top
        // of the bright severity color for contrast (matches the status pill treatment).
        holder.severityLabel.text = alert.severity.uppercase()
        holder.severityLabel.backgroundTintList = ColorStateList.valueOf(severityColor)
        holder.severityLabel.setTextColor(ContextCompat.getColor(context, R.color.color_bg))

        holder.macIpLine.text = "${alert.mac}  \u2192  ${alert.ip}"
        holder.explanationLine.text = alert.explanation
        holder.timeLabel.text = timeFormat.format(Date((alert.timestampSeconds * 1000).toLong()))
    }

    override fun getItemCount(): Int = alerts.size

    /** Adds newest alert to the top of the list - most recent threats should be visible first. */
    fun addAlert(alert: Alert) {
        alerts.add(0, alert)
        notifyItemInserted(0)
        emitCounts()
    }

    private fun emitCounts() {
        val total = alerts.size
        val critical = alerts.count { it.isCritical }
        val warning = total - critical
        onCountsChanged?.invoke(total, warning, critical)
    }
}
