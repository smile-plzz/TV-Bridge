# Privacy and security — TV Bridge 1.1

TV Bridge has no account, advertising, analytics, external QR service, cloud upload API or external web assets. Files move over your home network. QR codes are generated on the TV. Android backups are disabled.

## Connection and access

The user launches the TV app to start a local IPv4 server on port 8765. A foreground service keeps it available while switching TV activities. Stop is available in the app and its notification. The service stops after two hours and does not auto-start at boot. Process death, Stop, updates and network changes invalidate in-memory sessions and download tickets.

Pairing requires a random six-digit code (ten-minute validity), explicit TV approval within a minute and rate-limited attempts. Approved sessions last one hour. Their random two-UUID tokens live in browser session storage and never appear in QR codes or diagnostics. The TV can disconnect a named phone; its current sockets and issued tickets are also revoked.

Local HTTP traffic—including files and tokens—is **unencrypted**. An adversarial network participant can observe or interfere with it. Use only a trusted home LAN; do not forward the port to the Internet. Origin/Host checks and restrictive browser headers reduce cross-origin attacks but do not make hostile Wi-Fi safe.

Native downloads use random one-use URLs, valid for one minute and tied to an authorized session. These URLs are short-lived capabilities; they may appear in the browser’s download records. The app sends no referrer and does not share the URLs externally.

## Storage

Received files and explicitly imported TV files live in the app’s private shared directory until deleted or the app is uninstalled. Other TV storage is not enumerated. The document picker lets the user select an individual file, which is copied without altering the original.

Incomplete transfers and their metadata are private to the TV app and expire after 24 hours (cleaned on server startup). Random transfer IDs plus an approved phone session permit recovery. Phone local storage keeps paused transfer IDs, filenames and the latest 20 completed-upload records, including saved-file checksums. Clear phone history removes completed history, not TV files. Discard removes a staged transfer. Browser site-data clearing removes phone records. A changed TV address creates a new browser origin.

Up to five incoming text/link messages are queued in TV app memory while its screen is hidden; they disappear when the process/service ends. Links and file opening require visible TV confirmation. Text is copied to the TV clipboard only on explicit TV action.

## Diagnostics

User-requested JSON reports include app/version, device/Android, available storage, heap figures, uptime, session count and the latest 40 connection/transfer events. Events can contain filenames. Reports omit pairing codes, bearer tokens, transfer IDs, download tickets, file contents, sent text and sent URLs. Review before sharing. Reports describe TV Bridge, not other apps.

## Android permissions

Internet/network state support the local connection. Connected-device foreground-service permissions and a network-state prerequisite support that connection’s lifecycle. A timed partial wake lock keeps the CPU available during transfers. TV Bridge does not change router settings or request broad file, accessibility, microphone, camera or contacts access. Request-package-install permission supports user-initiated APK opening; Android’s installation-source setting and installer approval still apply. External viewers and websites have separate privacy behavior.

Source/support: https://github.com/smile-plzz/TV-Bridge
