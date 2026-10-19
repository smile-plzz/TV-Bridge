# TV Bridge 1.0.0 — first preview

Standalone Android TV companion with a phone browser interface. Independent from Nook TV and NowShowing.

Included: QR/code pairing with TV approval, sequential file uploads with progress/cancel, authenticated downloads and deletion, TV document-picker import, local photo/video viewing, text display/copy, confirmed HTTP(S) link opening, JSON diagnostics, expiring sessions, network change handling, and TV remote focus.

Install **TV-Bridge-1.0.0-preview.apk** on Android TV 8.0+; scan the on-screen QR using your phone. Both devices need the same trusted home network. Safari or Chrome works without a phone app.

This is a **debug-signed testing preview**. Automated build/lint, transport, QR matrix, and Android protocol tests gate publication. Real TV remote usability, iPhone browser behavior, OEM document pickers, and device-specific codecs still need owner testing.

Keep TV Bridge visible during transfers. Leaving it revokes sessions. Transfers restart from the beginning after interruption. Files ≤1 GiB; large browser downloads consume phone memory. Local HTTP is unencrypted. Diagnostics describe TV Bridge only. This release does not provide system-wide remote control or typing into other applications.

The preview signing certificate is included in `SIGNING.txt`. CI caches the signing key but cannot guarantee indefinite continuity; a later certificate change requires reinstalling. Save important shared files before uninstalling.
