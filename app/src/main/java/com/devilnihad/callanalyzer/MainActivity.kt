package com.devilnihad.callanalyzer

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var logView: TextView

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startService(Intent(this, CaptureVpnService::class.java))
        }
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }

        val title = TextView(this).apply {
            text = "DEVIL NIHAD\nNETWORK DIAGNOSTICS"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title)

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 16, 0, 12)
        }
        root.addView(status)

        val buttons = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val start = Button(this).apply {
            text = "START CAPTURE"
            setOnClickListener { startCapture() }
        }

        val stop = Button(this).apply {
            text = "STOP"
            setOnClickListener { stopCapture() }
        }

        val clear = Button(this).apply {
            text = "CLEAR"
            setOnClickListener {
                AnalyzerStore.clear()
                refresh()
            }
        }

        buttons.addView(start)
        buttons.addView(stop)
        buttons.addView(clear)
        root.addView(buttons)

        stats = TextView(this).apply {
            setPadding(0, 18, 0, 12)
            textSize = 16f
        }
        root.addView(stats)

        val export = Button(this).apply {
            text = "EXPORT CSV"
            setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TEXT, AnalyzerStore.csv())
                }
                startActivity(Intent.createChooser(shareIntent, "Export network CSV"))
            }
        }
        root.addView(export)

        logView = TextView(this).apply {
            textSize = 12f
            setPadding(0, 16, 0, 0)
        }

        root.addView(
            ScrollView(this).apply { addView(logView) },
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    private fun startCapture() {
        if (AnalyzerStore.running) return
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            startService(Intent(this, CaptureVpnService::class.java))
        }
    }

    private fun stopCapture() {
        startService(Intent(this, CaptureVpnService::class.java).setAction("STOP"))
        refresh()
    }

    private fun refresh() {
        status.text = if (AnalyzerStore.running) {
            "● CAPTURE ACTIVE — authorized device diagnostics only"
        } else {
            "○ Capture stopped"
        }

        stats.text = "Packets: ${AnalyzerStore.packets}    Bytes: ${DecimalFormat("#,###").format(AnalyzerStore.bytes)}"

        val recent = AnalyzerStore.events.takeLast(80).asReversed()
        logView.text = if (recent.isEmpty()) {
            "No packet events yet."
        } else {
            recent.joinToString("\n\n") { event ->
                "${event.protocol}  ${event.src}:${event.srcPort} → ${event.dst}:${event.dstPort}\n${event.bytes} bytes  ${event.note}"
            }
        }

        if (AnalyzerStore.running) {
            window.decorView.postDelayed({ refresh() }, 1000)
        }
    }
}
