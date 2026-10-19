# TV Bridge

**A standalone phone ↔ Android TV companion.** Open TV Bridge on your television, scan its QR code with your phone, approve pairing on the TV, and share files, text, and links over your home network.

Created for **Ismail Hossain** · [GitHub](https://github.com/smile-plzz)

## Download and install

Open [Releases](https://github.com/smile-plzz/TV-Bridge/releases) and download **TV-Bridge-1.0.0-preview.apk** from the newest successful preview. This is a sideloadable Android TV APK, not a phone app or Play Store release. The first release is published automatically after the build, lint, QR checks, and Android transfer checks pass.

1. Copy the APK to the TV using a USB drive or an existing file-transfer tool. You need this once, before TV Bridge can receive files itself.
2. Open it using the TV’s file manager and allow that installer to install unknown apps if Android asks.
3. Launch **TV Bridge** from the TV apps screen.
4. Put your phone on the same home network. Scan the QR code or type the exact `http://TV-IP:8765` address shown on screen into Safari or Chrome.
5. Enter the six-digit code, then choose **Allow** on the TV using its remote.

**Requirements:** Android TV / Google TV, Android 8.0+ (API 26); a modern iPhone or Android browser; an IPv4 home Wi-Fi/Ethernet network allowing devices to communicate. The TV and phone can use different Wi-Fi bands if their router bridges them.

## What you can do

| Feature | First release |
| --- | --- |
| Pair a phone | Local QR code, expiring six-digit code, explicit TV approval |
| Phone → TV files | Select several files; streamed sequential uploads, progress, cancellation |
| TV → phone files | Download received files or files explicitly added through the TV file picker |
| Manage files | List, open, delete, see storage availability |
| Photos and videos | Built-in sampled photo viewer and platform-codec video player |
| Send text | Show on TV and optionally copy to the TV clipboard |
| Send links | TV confirms before handing an HTTP(S) link to an installed app |
| Diagnostics | Download TV Bridge’s device/storage/session/event report as JSON |
| Disconnect | Disconnect one phone, rotate the TV code, or leave the TV app |

Phone navigation: **Files · Text & links · Connection**. No phone installation, account, cloud backend, ads, tracking, subscriptions, or Nook TV / NowShowing dependency.

### Files already on the TV

Choose **Files on TV → Add file from TV** and select a file in Android’s document picker. TV Bridge copies only that file into its private shared folder. Some TVs need a compatible file manager/document picker installed first. Returning from the picker starts a fresh connection; scan the new QR/code and pair again. Refresh the phone’s shared-file list after the import completes.

## Practical limits

- One upload at a time, at most **1 GiB per file**, with a **32 MiB** storage reserve. Files are streamed to disk rather than held in TV memory.
- Browser downloads use an authenticated Blob; very large downloads can exhaust phone browser memory. Files over 200 MiB prompt before download. Prefer smaller files when downloading to an iPhone.
- Transfers require TV Bridge to remain visible and the phone page to stay awake. Opening media, another app, a file picker, or pressing Home stops the server, cancels transfers and revokes all sessions. Reopening starts a new pairing code.
- Cancelled/interrupted uploads restart from the beginning. They are not resumable. Crash leftovers older than 24 hours are cleaned at startup.
- Local **HTTP is unencrypted**. Pair only on a trusted home network. No Internet relay, cloud access, mobile-data transfer, HTTPS certificate installation, or NAT traversal.
- Pairing is available for 10 minutes; each approved session lasts one hour. **Disconnect all / New code** restarts pairing.
- Photo/video support depends on the device’s Android decoders. Documents need an installed viewer. APKs need a compatible installer and Android’s normal installation approval; TV Bridge does not silently install applications or bypass security settings.
- This does not control other apps, inject typing into their fields, collect their private diagnostics, mirror your screen, or act as a system-wide remote. Integrations can be added later.

## Build and verification

JDK **17**, Gradle **8.13**, Android SDK platform **35** and build tools **35.0.0**. No application runtime dependencies. Install Gradle locally, or use the pinned Gradle setup in GitHub Actions; a wrapper binary is not bundled.

```sh
gradle assembleDebug assembleDebugAndroidTest lintDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`. [GitHub Actions](https://github.com/smile-plzz/TV-Bridge/actions) builds the app, verifies the HTTP transport, compares complete QR matrices with an independent QR implementation, runs transfer tests on Android API 35, and publishes a preview only after all gates pass.

**Signing:** previews are debug-signed for personal testing. The CI debug key is cached for update continuity, but cache retention is not guaranteed. Each release includes `SIGNING.txt` with its certificate and `SHA256SUMS.txt`. If a future APK uses a different certificate, Android requires uninstalling the old preview, which deletes its stored files. Download important files first. A stable private release key must be configured before production distribution.

See [testing](docs/TESTING.md), [architecture and protocol](docs/ARCHITECTURE.md), [privacy](docs/PRIVACY.md), and [release notes](docs/RELEASE-NOTES.md).

## Troubleshooting

| Problem | Try |
| --- | --- |
| Phone cannot open page | Use **http**, exact current address, same home LAN; disable VPN; avoid guest Wi-Fi/client isolation |
| No address on TV | Connect Wi-Fi/Ethernet, then Retry connection; IPv4 is required |
| Pairing rejected | Get current code, request once, approve on TV; rotate if the 10-minute window expired |
| Connection disappeared | Return to TV Bridge, scan its current QR, pair again |
| Transfer failed | Keep both screens awake; free storage; retry from start; ensure file ≤1 GiB |
| File will not open | Check format support or install a compatible TV viewer/installer |
| Cannot select a TV file | Install a TV-compatible Android document picker/file manager |
| APK update rejected | Compare signing certificates; different preview keys need uninstall/reinstall |

## Next candidates

Resumable uploads, streaming browser downloads without large Blobs, durable release signing, a foreground-service mode with explicit TV controls, and opt-in diagnostics integrations for other apps. System-wide remote control is a separate capability and requires a deliberate permissions design.
