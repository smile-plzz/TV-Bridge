# Verification and owner testing

## Automated gates

The release workflow runs:

1. **17 JVM transport checks**: valid framing, 1 GiB boundary, duplicate Content-Length, negative/oversized/invalid lengths, chunked-body rejection, CRLF enforcement, invalid targets/versions, header bounds, streaming exact lengths, truncated bodies, filename sanitization and reserved partial-transfer suffix handling.
2. **3 independent QR matrix comparisons**: common private IPv4 addresses; byte-mode version 4-L mask 0 compared with python-qrcode 8.2.
3. JavaScript syntax validation, Android debug/test APK compilation and Android lint.
4. **19 on-device protocol checks on API 35 emulator**: bundled page, unauthorized access, wrong code, hostile Origin, TV approval requirement, one-time pairing, authenticated upload/download byte equality, listing, traversal, unsafe link scheme, text, diagnostics, delete and logout revocation.
5. APK signature verification and SHA-256 generation. Only then is a GitHub preview published.

The integration runner is intentionally independent of remote-control timing and automatically approves a test request. It does not claim to validate real remote focus or iPhone UX. To run on a disposable emulator:

```sh
gradle assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w io.github.smileplzz.tvbridge.test/io.github.smileplzz.tvbridge.BridgeInstrumentation
```

Expected marker: `TV_BRIDGE_TESTS_OK: 19 checks`. The test writes/deletes a random shared file. Do not run while using TV Bridge; the test needs port 8765 free.

## Owner acceptance on the Hisilicon Android 14 TV + iPhone

- Install APK, confirm TV launcher icon/name, readable layout and visible remote focus.
- Scan QR from the sofa. Pair in Safari. Confirm wrong code fails and the phone cannot pair without selecting Allow on TV.
- Send a photo, a small MP4 and a document. Check progress and filenames; open the photo and video. Press Back and re-pair after viewing.
- Send several files sequentially. Cancel a larger transfer mid-way and refresh: no incomplete file should be listed. Retry from the start.
- Download a file on iPhone, then compare contents/size with the original. Check delete confirmation and free-space display.
- Add an existing TV file using the document picker, re-pair, refresh and download it to the phone. Confirm the original TV file remains untouched.
- Send ordinary text, copy it on TV, then send an HTTPS link. Confirm a link only opens after TV approval.
- Download diagnostic JSON; confirm version/device and recent events, without the pairing code or token.
- Rotate the code, press Home, switch apps, and change Wi-Fi. Old sessions must stop working; re-pair with the new code.
- Check About, Back navigation, no-network retry, no-compatible-viewer errors, and a second phone requesting approval.

## Current evidence

Initial local Java compilation against Android API 35 and browser JavaScript syntax validation passed. The local Gradle native library and ADB daemon could not start in the restricted Windows runtime; GitHub Actions supplies APK/lint/emulator gates. Consult the actual workflow run for the published commit rather than treating the existence of this test plan as a test pass. Physical TV/iPhone acceptance remains for the owner.
