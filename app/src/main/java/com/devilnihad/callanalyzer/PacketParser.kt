package com.devilnihad.callanalyzer

import java.net.InetAddress

object PacketParser {
    fun parse(buf: ByteArray, len: Int): PacketEvent? {
        if (len < 20) return null
        val version = (buf[0].toInt() ushr 4)
        return if (version == 4) parseV4(buf, len) else if (version == 6) parseV6(buf, len) else null
    }

    private fun u8(b: Byte) = b.toInt() and 0xff
    private fun u16(b: ByteArray, i: Int) = (u8(b[i]) shl 8) or u8(b[i + 1])
    private fun ip4(b: ByteArray, i: Int) =
        "${u8(b[i])}.${u8(b[i+1])}.${u8(b[i+2])}.${u8(b[i+3])}"

    private fun parseV4(b: ByteArray, len: Int): PacketEvent? {
        val ihl = (u8(b[0]) and 0x0f) * 4
        if (ihl < 20 || len < ihl) return null
        val proto = u8(b[9])
        val src = ip4(b,12); val dst = ip4(b,16)
        var sp=0; var dp=0
        if ((proto==6 || proto==17) && len >= ihl+4) {
            sp=u16(b,ihl); dp=u16(b,ihl+2)
        }
        val name = when(proto) { 6 -> "TCP"; 17 -> "UDP"; 1 -> "ICMP"; else -> "IP-$proto" }
        val note = when {
            sp==3478 || dp==3478 || sp==5349 || dp==5349 -> "STUN/TURN candidate"
            sp==53 || dp==53 -> "DNS"
            sp==443 || dp==443 -> "HTTPS/TLS"
            else -> ""
        }
        return PacketEvent(protocol=name,src=src,dst=dst,srcPort=sp,dstPort=dp,bytes=len,note=note)
    }

    private fun parseV6(b: ByteArray, len: Int): PacketEvent? {
        if (len < 40) return null
        val next = u8(b[6])
        val src = InetAddress.getByAddress(b.copyOfRange(8,24)).hostAddress ?: "::"
        val dst = InetAddress.getByAddress(b.copyOfRange(24,40)).hostAddress ?: "::"
        var sp=0; var dp=0
        if ((next==6 || next==17) && len>=44) { sp=u16(b,40); dp=u16(b,42) }
        val name=when(next){6->"TCP";17->"UDP";58->"ICMPv6";else->"IPv6-$next"}
        val note=if(sp==3478||dp==3478||sp==5349||dp==5349)"STUN/TURN candidate" else ""
        return PacketEvent(protocol=name,src=src,dst=dst,srcPort=sp,dstPort=dp,bytes=len,note=note)
    }
}
