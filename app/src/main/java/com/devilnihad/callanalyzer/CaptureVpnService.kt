package com.devilnihad.callanalyzer

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CaptureVpnService : VpnService() {
    private var vpn: ParcelFileDescriptor? = null
    private val stop = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopCapture(); return START_NOT_STICKY }
        startForeground(7, notification())
        if (vpn == null) startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        AnalyzerStore.running = true
        stop.set(false)
        vpn = Builder()
            .setSession("DEVIL NIHAD Call Analyzer")
            .addAddress("10.8.0.2", 32)
            .addRoute("10.8.0.0", 24)
            .setBlocking(false)
            .establish()

        executor.execute {
            try {
                val fd = vpn?.fileDescriptor ?: return@execute
                FileInputStream(fd).use { input ->
                    val buf = ByteArray(32767)
                    while (!stop.get()) {
                        val n = input.read(buf)
                        if (n > 0) PacketParser.parse(buf, n)?.let { AnalyzerStore.add(it) }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun stopCapture() {
        stop.set(true)
        AnalyzerStore.running = false
        try { vpn?.close() } catch (_: Exception) {}
        vpn = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification {
        val channelId="capture"
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channelId,"Network Analyzer",NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this,channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Call Network Analyzer")
            .setContentText("Authorized-device capture is active")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() { stopCapture(); executor.shutdownNow(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
}
