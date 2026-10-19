# TV Bridge

**Your phone. Your TV. One local connection.**

Share files, text and links between a phone browser and Android TV. No phone installation, account, ads, analytics or cloud upload. Independent from other TV apps.

[**Download TV Bridge 1.1.0**](https://github.com/smile-plzz/TV-Bridge/releases/latest) · [Release notes](docs/RELEASE-NOTES.md) · [Privacy](docs/PRIVACY.md)

## Get connected

1. Download `TV-Bridge-1.1.0.apk` from Releases. Install it on your Android TV using a USB drive or an existing transfer tool. Enable installation from that source if Android asks.
2. Launch **TV Bridge** on your TV. Connect your phone to the same trusted home network.
3. Scan the TV’s QR code, or type the exact `http://TV-IP:8765` address into Safari/Chrome.
4. Enter the six-digit TV code and choose **Allow** on the TV.
5. Choose a small photo on your phone. After upload, choose **Open on TV**, then confirm on the TV.

**Requirements:** Android TV / Google TV, Android 8.0+ (API 26), a modern iPhone/Android browser, and a home IPv4 network where devices can communicate. Guest Wi-Fi, device isolation and VPNs can prevent connection.

## What it does

| Feature | Experience |
| --- | --- |
| Send files | Multiple-file queue, progress/speed, pause and recovery using the same file |
| Receive TV files | Native browser downloads; add an existing TV file through Android’s document picker |
| Browse shared files | Friendly names, search, category filters and sort by date/name/size |
| Open on TV | Phone requests opening; TV confirms before viewing or installation |
| Photos and videos | Sampled photo viewer and native Android-codec video player |
| Phone playback controls | Play, pause, ±30 seconds, volume and close for TV Bridge video |
| Text and links | Display/copy text and confirm HTTP(S) links on TV; five messages can wait while the TV app is hidden |
| Connection management | Live phone status, named sessions, individual disconnect and Stop bridge |
| Diagnostics | Download version, device, memory/storage and recent event information |

The foreground connection stays active while opening media, using the file picker or watching another TV app. Stop it from TV Bridge or its notification. It stops automatically after **two hours**. Pairing codes last ten minutes; approved phone sessions last one hour. The service starts only when you launch TV Bridge; it does not start at boot.

## Resuming a transfer

Choose the **same file** again in the same browser and at the same TV address. The last committed 2 MiB chunk is recovered, even after a page refresh or bridge restart followed by pairing again. Paused transfer records appear on your phone; **Discard** removes them. Incomplete transfers expire after 24 hours. Phone metadata is kept in local browser storage; access tokens remain in session storage.

The sampled file identity uses name, size and first/last samples to avoid resuming the wrong selection. It is not a cryptographic proof of the source file. After completion, TV Bridge computes the saved file’s SHA-256 and shows it in this phone’s transfer history for optional independent comparison.

## Sharing an existing TV file

Choose **Files on TV → Add file from TV**. Only the selected file is copied into TV Bridge’s private shared directory; the original is left untouched. A compatible Android document picker/file manager must be available. Refresh the phone file list when import finishes. Broad storage access is not requested.

## Limits you should know

- Files up to **1 GiB**; a **32 MiB** TV storage reserve. One upload chunk and up to two downloads at a time. Keep the phone awake during uploads; mobile browsers can suspend background pages.
- Native downloads avoid app-side Blob buffering. If a download is interrupted, request a new download from Files; the one-use URL is not a permanent/resumable link.
- Local **HTTP is unencrypted**. Use only on a trusted home network. No Internet relay, HTTPS certificate installation, screen mirroring or system-wide remote control.
- Playback controls apply to TV Bridge’s player. The app cannot type into other applications’ fields or read their private reports.
- TV image/video codecs, document viewers and document pickers vary. APKs require Android’s installation-source setting and installer confirmation. Nothing installs silently.
- Stop, process death, network address changes and app updates invalidate sessions and download links. Reopen/pair again; staged upload data can still be recovered at the same browser origin.
- Files remain until deleted or the app is uninstalled. Download important files before uninstalling. Diagnostics and transfer history can include filenames.

## Updates and publication

The first stable release introduces a privately backed-up production signing identity. The original preview key was not retained by its CI configuration. **If you installed the 1.0 preview, download your shared files, uninstall that preview, then install 1.1.0.** Uninstalling deletes the old app’s stored files. Future stable updates must use the committed production certificate to install over 1.1 without intentionally deleting data.

CI builds/tests the release candidate and prepares an unpublished draft. The final APK is signed privately and checked against `release/signing-certificate.sha256`; private signing keys are never committed or stored in a public CI cache. See [publishing and signing](docs/PUBLISHING.md).

GitHub Releases is the distribution channel. A Google Play/App Store listing has not been submitted. Store copy and readiness notes are included in [STORE-LISTING.md](docs/STORE-LISTING.md).

## Build and verification

JDK 17, Gradle 8.13, Android SDK 35. No application runtime dependencies.

```sh
gradle assembleDebug assembleDebugAndroidTest lintDebug
gradle assembleRelease lintRelease
```

CI candidates use an ephemeral development key for testing. Finalize using the private production key with `tools/sign_release.py`, or set the signing variables described in [PUBLISHING.md](docs/PUBLISHING.md). A wrapper binary is not bundled; CI pins Gradle through setup-gradle.

CI validates HTTP framing, durable upload recovery, QR matrices against an independent encoder, JavaScript syntax, Android protocol and service continuity, release-candidate launch, production APK signatures and checksums. [Testing evidence and owner acceptance](docs/TESTING.md) distinguish emulator verification from physical TV/iPhone usability.

## Troubleshooting

| Problem | Action |
| --- | --- |
| Phone cannot reach TV | Exact current `http://` address, same LAN, no guest isolation or VPN |
| Pairing waits | Open TV Bridge on TV, approve within one minute; rotate expired code |
| Upload pauses/fails | Keep phone awake, choose the same file to resume; free TV space if needed |
| Paused transfer unavailable | Discard its phone record and start again; TV partials expire after 24 hours |
| Open on TV unavailable | Return to TV Bridge on TV, then request opening again |
| Playback buttons disabled | Open a video in TV Bridge’s player first |
| TV file picker/viewer missing | Install a compatible TV document picker/viewer |
| Connection stopped | Check the two-hour limit, notification, network change or app update; reopen/pair |
| Update certificate mismatch | Stop; compare `SIGNING.txt`. Back up files before any reinstall |

Created by **Ismail Hossain** · [GitHub](https://github.com/smile-plzz) · [Issues](https://github.com/smile-plzz/TV-Bridge/issues)
