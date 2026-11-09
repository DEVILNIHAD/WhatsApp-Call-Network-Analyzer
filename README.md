# Android Network Diagnostics

A small Android app with a polished dark dashboard for checking the current device's internet reachability and approximate HTTPS response time. It runs five short HTTPS requests to Android's public connectivity-check endpoint, shows response-time samples and failed checks, and can share results as CSV.

## Privacy and limitations

- This app checks only the device's internet connection; it does not inspect WhatsApp calls, decrypt traffic, or identify another caller's IP or location.
- HTTPS response time is an app-level measurement, not voice-call latency, ICMP ping, or definitive packet loss.
- The test contacts `connectivitycheck.gstatic.com`; results stay in memory until the user chooses to share them.
- Use it on devices and networks you own or are authorized to test.

## Build and run

Open the project in Android Studio, install Android SDK 35 if prompted, wait for Gradle sync, then run the `app` configuration on an Android 8.0+ device. The app needs internet access and network-state permission. It does not need VPN permission for this diagnostic test.

The GitHub Actions workflow builds a debug APK and uploads it as an artifact.
