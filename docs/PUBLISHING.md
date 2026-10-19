# Publishing and operations

## GitHub distribution

The main-branch workflow builds debug/test APKs for instrumentation and a **release-mode APK** for distribution. Both lint variants must pass. JVM protocol/storage checks, independent QR comparisons and Android integration/service tests gate publication. The release APK is installed over the test target and launched before publishing. Outputs include APK, SHA-256 and signing certificate information.

For 1.1.0 the published tag is `v1.1.0`. Subsequent product versions must update Gradle versionName/versionCode, APK filename, workflow tag and release notes together. Documentation-only evidence commits use `[skip ci]`. A repeated workflow cannot overwrite an existing release tag.

## Signing and upgrades

The 1.0 preview already installed a signing identity. 1.1 retains that cached certificate so Android can upgrade it without deleting shared files. The APK uses the release build type (not debuggable); the retained certificate was originally generated as a development certificate. It is appropriate to be explicit about this distinction.

Before publication, CI downloads the newest release’s `SIGNING.txt` and compares its SHA-256 certificate digest with the new APK. If the cache disappears or a different key is used, publication **fails** rather than silently shipping an incompatible update. Never uninstall merely to bypass an unexpected signature mismatch without backing up your files.

The CI cache is not a permanent key vault. For longer-term/store distribution the owner must back up the original key privately, or deliberately migrate to a durable release identity before widespread adoption. Do not commit private keystores or passwords. Gradle accepts `TVBRIDGE_KEYSTORE`, `TVBRIDGE_STORE_PASSWORD`, `TVBRIDGE_KEY_ALIAS` and `TVBRIDGE_KEY_PASSWORD` for owner-controlled builds. Key changes cannot be presented as ordinary compatible updates.

## Google Play readiness

This repository includes a TV banner/icon, store-listing draft, privacy policy, app purpose, installation help and known limits. It does not claim a submitted or approved Play listing. Before a store submission: configure the owner’s production signing, re-check current TV target API and foreground-service policies, provide public support/privacy URLs, complete the store’s disclosures and run physical TV/iPhone acceptance. No iOS binary is required for the browser client.

## Release checks

1. Verify the source commit, CI results and certificate guard.
2. Install the release APK on an Android device; test TV-screen closure while the service remains reachable.
3. Confirm published assets, checksum and version, and update test evidence.
4. On owner hardware: test QR pairing, paused/resumed large upload, native Safari download, file picker, photo/video, phone playback controls and Stop.

## Recovery and rollback

- If connection behaves unexpectedly, use **Stop bridge**, reopen, and pair again. Server sessions/tickets are memory-only and are revoked on stop or process death. Partial uploads persist for at most 24 hours.
- If an upgrade is unusable, retain files and publish a corrected build with a higher versionCode under the same certificate. Avoid a normal downgrade to 1.0, which Android may reject. Back up files before an uninstall/reinstall.
- Do not publish if upload bytes differ, pairing bypasses TV approval, Stop fails to revoke access, or the signing certificate changes unexpectedly.
- No analytics are sent. Device-specific failures are diagnosed through user-requested JSON reports, which may contain filenames.
