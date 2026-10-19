# TV Bridge 1.1.0

A standalone local phone ↔ Android TV companion, distributed as a release-mode APK through GitHub Releases.

## Improvements

- Keep the connection while viewing media, selecting a TV file or using another TV app. User-started foreground service, explicit Stop and two-hour automatic shutdown.
- Recoverable 2 MiB upload chunks, progress/speed, pause, persistent transfer identities, rollback of interrupted chunks and SHA-256 of completed TV files.
- Browser-native downloads through one-minute, one-use authorized links. Large files no longer require an app-created Blob in phone memory.
- File search, category filters, sorting, friendly names and phone-requested Open on TV with TV confirmation.
- Phone controls for TV Bridge video: play/pause, ±30 seconds, volume and close.
- Named phone sessions, individual disconnect that closes active connections, live connection status, recent phone transfer history and richer diagnostics.
- TV banner with product name, improved About/help, store-listing draft and publication/upgrade guidance.
- Release/debug lint, protocol/storage/QR checks, Android transfer tests, foreground-service continuity, release launch and certificate-continuity gates.

## Installation and upgrades

Install `TV-Bridge-1.1.0.apk` on Android TV 8.0+, scan the QR with your phone, enter the code and approve on TV. Both devices need the same trusted IPv4 LAN. Existing preview installations can upgrade under the preserved signing identity without intentionally deleting app data.

The release APK is not debuggable. Its certificate is retained from the original development preview for update compatibility. The key cache is not permanent storage; publication fails if the certificate changes. A durable owner-controlled signing setup and store-policy review remain required before Google Play distribution.

## Known limits

Local HTTP is unencrypted. Files ≤1 GiB. Keep the phone awake for uploads. Resume requires the same browser origin and file selection; partials expire after 24 hours. Interrupted downloads need a new download request. The bridge stops after two hours; sessions expire after one hour. Media and document support vary by TV. This does not provide system-wide remote control, mirroring or private diagnostics for other apps.

Physical TV/iPhone UX and device-specific codecs remain owner acceptance; emulator checks do not replace that evidence. This release does not claim a submitted or approved app-store listing.
