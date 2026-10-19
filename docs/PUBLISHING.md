# Publishing and operations

## Release pipeline

1. CI builds debug/test APKs and a release-mode candidate, runs debug/release lint, JVM transport/storage tests, browser file-identity checks and independent QR comparisons.
2. An Android API 35 emulator validates protocol, native downloads, resumable transfer integrity and foreground-service continuity. It captures the TV home screen and installs/launches the release-mode candidate.
3. CI prepares an **unpublished, asset-free draft release**. Candidates are test-signed and available as workflow artifacts; they are not production distribution APKs.
4. Retrieve the verified candidate and sign it privately using `tools/sign_release.py`. This verifies the result against the committed production SHA-256 certificate fingerprint and generates `SIGNING.txt` and `SHA256SUMS.txt`.
5. Attach only the production APK and its two verification files to the draft, then publish. Do not upload private keys or password files.

Example finalization:

```sh
python tools/sign_release.py candidate.apk --keystore /private/TV-Bridge-release.p12 --password-file /private/keystore-password.txt --sdk /path/to/android-sdk --output dist/TV-Bridge-1.1.0.apk
```

The private production signing backup is retained separately for the owner. The public certificate and fingerprint are in `release/`. Private signing material is never committed, placed in public Actions caches, or included in release assets. CI intentionally does not possess the production private key. A future owner-controlled secret-backed signing environment may automate finalization without changing the identity.

## One-time preview migration

The first preview's generated signing key was not retained at the path its workflow attempted to cache. It cannot be recovered from the APK. **Preview users must download important shared files, uninstall the preview, then install 1.1.0.** Uninstall deletes the old app’s files. This is a deliberate first-stable identity boundary, not a seamless upgrade. Future stable updates must use the backed-up production key and committed certificate.

## Versions and stores

1.1.0 uses versionCode 11000 and tag `v1.1.0`. Future versions must update Gradle versionName/versionCode, workflow artifact filename/tag and release notes together. A repeated job cannot overwrite an existing release. Documentation-only evidence commits use `[skip ci]`.

GitHub Releases is the publication channel. The repository includes a TV banner/icon, store-listing copy, privacy policy, installation help and known limits. It does not claim Google Play submission or approval. A store submission requires the owner’s account, current TV API/foreground-service policy review, disclosures, public support/privacy URLs and physical acceptance testing. No iOS binary is needed for the browser client.

## Go/no-go and recovery

- Publish only if byte-integrity tests, TV pairing approval, explicit Stop and foreground continuity pass, and the final APK matches the production certificate.
- Record the tested source SHA, successful workflow, signed APK digest and known physical-device gaps.
- Stop/reopen/re-pair to recover a broken local connection. Sessions/tickets are memory-only; incomplete uploads persist for at most 24 hours.
- If a stable update is unusable, ship a corrected higher versionCode under the same certificate. Avoid uninstalling merely to bypass a signature mismatch; back up files first.
- No analytics are collected. User-requested diagnostic JSON may contain filenames and must be reviewed before sharing.
