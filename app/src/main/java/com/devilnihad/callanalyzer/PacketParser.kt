package com.devilnihad.callanalyzer

import java.net.InetAddress

object PacketParser {
    fun parse(buf: ByteArray, len: Int): PacketEvent? {
        if (len < 20) return null
        val version = (buf[0].toInt() ushr 4)
        return if (version == 4) {
            parseV4(buf, len)
        } else if (version == 6) {
            parseV6(buf, len)
        } else {
            null
        }
    }

    private fun u8(b: Byte) = b.toInt() and 0xff
    private fun u16(b: ByteArray, i: Int) = (u8(b[i]) shl 8) or u8(b[i + 1])
    private fun ip4(b: ByteArray, i: Int) = "${u8(b[i])}.${u8(b[i + 1])}.${u8(b[i + 2])}.${u8(b[i + 3])}"

    private fun parseV4(b: ByteArray, len: Int): PacketEvent? {
        val ihl = (u8(b[0]) and 0x0f) * 4
        if (ihl < 20 || len < ihl) return null

        val proto = u8(b[9])
        val src = ip4(b, 12)
        val dst = ip4(b, 16)

        var srcPort = 0
        var dstPort = 0
        if ((proto == 6 || proto == 17) && len >= ihl + 4) {
            srcPort = u16(b, ihl)
            dstPort = u16(b, ihl + 2)
        }

        val name = when (proto) {
            6 -> "TCP"
            17 -> "UDP"
            1 -> "ICMP"
            else -> "IP-$proto"
        }

        val note = when {
            srcPort == 3478 || dstPort == 3478 || srcPort == 5349 || dstPort == 5349 -> "STUN/TURN candidate"
            srcPort == 53 || dstPort == 53 -> "DNS"
            srcPort == 443 || dstPort == 443 -> "HTTPS/TLS"
            else -> ""
        }

        return PacketEvent(
            protocol = name,
            src = src,
            dst = dst,
            srcPort = srcPort,
            dstPort = dstPort,
            bytes = len,
            note = note
        )
    }

    private fun parseV6(b: ByteArray, len: Int): PacketEvent? {
        if (len < 40) return null

        val nextHeader = u8(b[6])
        val src = InetAddress.getByAddress(b.copyOfRange(8, 24)).hostAddress ?: "::"
        val dst = InetAddress.getByAddress(b.copyOfRange(24, 40)).hostAddress ?: "::"

        var srcPort = 0
        var dstPort = 0
        if ((nextHeader == 6 || nextHeader == 17) && len >= 44) {
            srcPort = u16(b, 40)
            dstPort = u16(b, 42)
        }

        val name = when (nextHeader) {
            6 -> "TCP"
            17 -> "UDP"
            58 -> "ICMPv6"
            else -> "IPv6-$nextHeader"
        }

        val note = if (srcPort == 3478 || dstPort == 3478 || srcPort == 5349 || dstPort == 5349) {
            "STUN/TURN candidate"
        } else {
            ""
        }

        return PacketEvent(
            protocol = name,
            src = src,
            dst = dst,
            srcPort = srcPort,
            dstPort = dstPort,
            bytes = len,
            note = note
        )
    }
}
