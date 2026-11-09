# WhatsApp Call Network Analyzer — FINAL

A privacy-conscious Android network diagnostics app branded by **DEVIL NIHAD**.

## Included now
- Android `VpnService` capture foundation
- IPv4/IPv6 packet header parsing
- TCP/UDP/ICMP classification
- STUN/TURN candidate port detection (3478/5349)
- DNS and HTTPS port tagging
- Live packet/event dashboard
- Packet/byte counters
- CSV export
- Foreground notification while capture is active
- GitHub Actions debug APK build
- Android 8+ support

## Important limitation
This build uses a VPN capture interface for authorized-device diagnostics. It intentionally does **not** claim to reveal another person's physical location, decrypt WhatsApp traffic, or bypass WhatsApp/Meta relay infrastructure. The current capture loop is a foundation for transparent diagnostics; production-grade full-device forwarding/tun2socks requires a dedicated forwarding engine and extensive testing.

## Build
Run GitHub Actions → **Build Android APK**.

## Safety
Use only on devices and networks you own or are explicitly authorized to monitor.
