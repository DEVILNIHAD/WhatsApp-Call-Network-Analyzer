package com.devilnihad.callanalyzer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class PacketEvent(
    val time: Long = System.currentTimeMillis(),
    val protocol: String,
    val src: String,
    val dst: String,
    val srcPort: Int = 0,
    val dstPort: Int = 0,
    val bytes: Int = 0,
    val note: String = ""
)

object AnalyzerStore {
    val events = CopyOnWriteArrayList<PacketEvent>()

    @Volatile
    var running = false

    @Volatile
    var packets = 0L

    @Volatile
    var bytes = 0L

    fun add(e: PacketEvent) {
        if (events.size >= 5000) {
            events.removeAt(0)
        }
        events.add(e)
        packets++
        bytes += e.bytes
    }

    fun clear() {
        events.clear()
        packets = 0
        bytes = 0
    }

    fun csv(): String {
        val sb = StringBuilder("time,protocol,src,dst,srcPort,dstPort,bytes,note\n")
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        events.forEach {
            sb.append(format.format(Date(it.time))).append(',')
                .append(it.protocol).append(',')
                .append(it.src).append(',')
                .append(it.dst).append(',')
                .append(it.srcPort).append(',')
                .append(it.dstPort).append(',')
                .append(it.bytes).append(',')
                .append(it.note.replace(",", ";")).append('\n')
        }
        return sb.toString()
    }
}
