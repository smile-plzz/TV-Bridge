# Privacy and connection security

TV Bridge has no account, analytics, cloud API, third-party advertising, external QR service, or external web assets. The QR is generated entirely on the TV. Transferred files live in the app’s private `shared` directory until deleted or the app is uninstalled. Android backups are disabled.

The server binds to the current local IPv4 address on TCP port 8765 while the TV activity is visible. It closes sockets and invalidates all pairing requests and session tokens when the activity stops. Changing networks restarts it with a new pairing code. No router port mapping is requested.

Pairing uses a random six-digit code, a 10-minute window, five attempts per minute per source address, and explicit TV confirmation. Pending approval lasts 60 seconds. An approved phone gets an unpredictable two-UUID bearer token with a fixed one-hour expiry, kept in browser session storage. Tokens are never included in URLs, diagnostics, or QR codes. Approval polling consumes the pairing request exactly once. Host/Origin checks, cross-site request rejection, strict content security policy, no CORS allowance, and no-store responses limit browser-based cross-origin attacks.

HTTP traffic, including file contents and session tokens, is **not encrypted**. A hostile network participant can observe or interfere with it. These controls do not make public/shared Wi-Fi safe. Use a trusted home LAN and do not forward the port to the Internet.

An approved phone can list, download, upload, and delete files inside TV Bridge’s shared directory. It cannot enumerate other TV storage. TV document-picker imports require the user to explicitly select a file. File paths are checked against the canonical shared directory. External viewers receive a read-only Android content URI with a temporary grant rather than an exposed filesystem path. Links are restricted to HTTP(S) and require TV approval.

Diagnostics are generated on request and include version, manufacturer/model, Android version, free storage, session count, uptime, capture timestamp, and the last 40 connection/transfer event descriptions. Event descriptions can include filenames. Reports omit pairing codes, tokens, file contents, sent text, and sent URLs. Review a report before sharing it.

The app requests Internet and network-state permissions. It does not request broad file access, accessibility services, microphone, camera, or contacts. Media and links can open another app, whose behavior and privacy policy are separate.
