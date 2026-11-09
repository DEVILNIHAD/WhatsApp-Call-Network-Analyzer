package com.devilnihad.callanalyzer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val samples = mutableListOf<ProbeResult>()

    private lateinit var networkLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var latencyValue: TextView
    private lateinit var successValue: TextView
    private lateinit var resultLabel: TextView
    private lateinit var progress: ProgressBar
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 17, 31)
        window.navigationBarColor = Color.rgb(8, 17, 31)
        buildUi()
        refreshNetworkLabel()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(color(R.color.screen_bg))
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
        }
        scroll.addView(page)

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = TextView(this).apply {
            text = "⌁"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.screen_bg))
            background = rounded(R.color.accent, 16)
        }
        brandRow.addView(mark, LinearLayout.LayoutParams(dp(54), dp(54)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        brandText.addView(label("YOUR DEVICE • NETWORK TOOLS", 11f, R.color.accent, true))
        brandText.addView(label("Connection health", 22f, R.color.text_primary, true).apply {
            setPadding(0, dp(4), 0, 0)
        })
        brandRow.addView(brandText)
        page.addView(brandRow)

        page.addView(label(
            "A quick, private check of this phone’s internet connection.",
            14f,
            R.color.text_muted,
            false
        ).apply { setPadding(0, dp(14), 0, dp(20)) })

        val networkCard = card()
        networkCard.orientation = LinearLayout.VERTICAL
        networkCard.addView(label("CURRENT CONNECTION", 11f, R.color.text_muted, true))
        networkLabel = label("Checking…", 19f, R.color.text_primary, true).apply {
            setPadding(0, dp(8), 0, 0)
        }
        networkCard.addView(networkLabel)
        page.addView(networkCard, marginParams(bottom = 14))

        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val latencyCard = metricCard("AVG RESPONSE", "—", "milliseconds")
        latencyValue = latencyCard.second
        val successCard = metricCard("CHECKS OK", "—", "out of 5")
        successValue = successCard.second
        metrics.addView(latencyCard.first, LinearLayout.LayoutParams(0, dp(112), 1f).apply {
            marginEnd = dp(7)
        })
        metrics.addView(successCard.first, LinearLayout.LayoutParams(0, dp(112), 1f).apply {
            marginStart = dp(7)
        })
        page.addView(metrics, marginParams(bottom = 18))

        runButton = Button(this).apply {
            text = "RUN CONNECTION TEST"
            textSize = 14f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(R.color.accent, 18)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { runTest() }
        }
        page.addView(runButton, LinearLayout.LayoutParams(-1, dp(56)))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = PROBE_COUNT
            progress = 0
            visibility = View.GONE
            progressTintList = android.content.res.ColorStateList.valueOf(color(R.color.accent))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.card_alt))
        }
        page.addView(progress, LinearLayout.LayoutParams(-1, dp(5)).apply {
            topMargin = dp(14)
            bottomMargin = dp(14)
        })

        statusLabel = label("Ready when you are", 13f, R.color.text_muted, false).apply {
            setPadding(0, dp(12), 0, dp(14))
        }
        page.addView(statusLabel)

        val resultCard = card().apply { orientation = LinearLayout.VERTICAL }
        resultCard.addView(label("TEST DETAILS", 11f, R.color.accent, true))
        resultLabel = label(
            "Run a test to see response-time samples and connection results.",
            14f,
            R.color.text_primary,
            false
        ).apply { setPadding(0, dp(10), 0, 0) }
        resultCard.addView(resultLabel)
        page.addView(resultCard, marginParams(bottom = 14))

        val shareButton = Button(this).apply {
            text = "SHARE RESULTS AS CSV"
            textSize = 13f
            isAllCaps = false
            setTextColor(color(R.color.text_primary))
            background = rounded(R.color.card_alt, 16)
            setOnClickListener { shareCsv() }
        }
        page.addView(shareButton, LinearLayout.LayoutParams(-1, dp(50)))

        page.addView(label(
            "Privacy note: tests contact Google’s public connectivity-check endpoint. This app does not inspect calls or identify other people.",
            11f,
            R.color.text_muted,
            false
        ).apply { setPadding(dp(2), dp(18), dp(2), 0) })

        setContentView(scroll)
    }

    private fun metricCard(title: String, initial: String, caption: String): Pair<LinearLayout, TextView> {
        val box = card().apply { orientation = LinearLayout.VERTICAL }
        box.addView(label(title, 10f, R.color.text_muted, true))
        val value = label(initial, 27f, R.color.text_primary, true).apply {
            setPadding(0, dp(7), 0, 0)
        }
        box.addView(value)
        box.addView(label(caption, 11f, R.color.text_muted, false))
        return box to value
    }

    private fun runTest() {
        runButton.isEnabled = false
        runButton.alpha = 0.65f
        progress.visibility = View.VISIBLE
        progress.progress = 0
        statusLabel.text = "Testing secure connection…"
        resultLabel.text = "Running five short HTTPS checks. Keep this screen open."
        latencyValue.text = "—"
        successValue.text = "—"

        worker.execute {
            val network = currentNetworkType()
            val outcomes = mutableListOf<Long?>()
            repeat(PROBE_COUNT) { index ->
                outcomes += probeOnce()
                runOnUiThread { progress.progress = index + 1 }
            }
            val successful = outcomes.filterNotNull()
            val average = successful.takeIf { it.isNotEmpty() }?.average()?.toLong()
            val timestamp = System.currentTimeMillis()
            val rows = outcomes.mapIndexed { index, latency ->
                ProbeResult(timestamp, network, index + 1, latency)
            }

            runOnUiThread {
                samples.addAll(rows)
                networkLabel.text = network
                latencyValue.text = average?.toString() ?: "—"
                successValue.text = "${successful.size}/$PROBE_COUNT"
                statusLabel.text = if (successful.size == PROBE_COUNT) {
                    "● All checks completed"
                } else {
                    "● ${successful.size} of $PROBE_COUNT checks succeeded"
                }
                resultLabel.text = buildString {
                    appendLine("Network: $network")
                    appendLine("Average HTTPS response: ${average?.let { "$it ms" } ?: "No response"}")
                    appendLine("Failed checks: ${PROBE_COUNT - successful.size} of $PROBE_COUNT")
                    appendLine()
                    append("Samples: ")
                    append(outcomes.mapIndexed { index, ms ->
                        "${index + 1}: ${ms?.let { "$it ms" } ?: "timeout/error"}"
                    }.joinToString("  •  "))
                    appendLine()
                    appendLine()
                    append("This is HTTPS response timing, not WhatsApp call latency or ICMP packet loss.")
                }
                progress.visibility = View.GONE
                runButton.isEnabled = true
                runButton.alpha = 1f
            }
        }
    }

    private fun probeOnce(): Long? {
        val connection = (URL(TEST_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            val start = System.nanoTime()
            val code = connection.responseCode
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            if (code in 200..299) elapsedMs else null
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun currentNetworkType(): String {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = manager.activeNetwork ?: return "Offline"
        val capabilities = manager.getNetworkCapabilities(active) ?: return "Unknown"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN connection"
            else -> "Other connection"
        }
    }

    private fun refreshNetworkLabel() {
        networkLabel.text = currentNetworkType()
    }

    private fun shareCsv() {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val csv = buildString {
            appendLine("time,network,check,https_response_ms")
            samples.forEach { row ->
                append(formatter.format(Date(row.timestamp))).append(',')
                    .append(row.network).append(',')
                    .append(row.number).append(',')
                    .append(row.latencyMs ?: "failed").append('\n')
            }
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        startActivity(Intent.createChooser(sendIntent, "Share diagnostics results"))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = rounded(R.color.card_bg, 20)
    }

    private fun label(textValue: String, size: Float, colorRes: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color(colorRes))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun rounded(colorRes: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color(colorRes))
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun marginParams(bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }

    private fun color(colorRes: Int): Int = getColor(colorRes)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private data class ProbeResult(
        val timestamp: Long,
        val network: String,
        val number: Int,
        val latencyMs: Long?
    )

    companion object {
        private const val PROBE_COUNT = 5
        private const val TEST_URL = "https://connectivitycheck.gstatic.com/generate_204"
    }
}
