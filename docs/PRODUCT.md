# TV Bridge product direction — 1.1

## The job

When something is on my phone and I want it on the TV—or I need a TV file back on my phone—I want to connect locally, move it with clear feedback, and keep using my TV normally.

Primary audience: an Android TV / Google TV owner with an iPhone or Android browser. The first real setup is Ismail’s Android 14 Hisilicon TV and iPhone. A lightweight TV app and a browser client keep installation effort and TV memory use low.

## Audit and decisions

| First-preview friction | Product decision | Acceptance |
| --- | --- | --- |
| Viewing media or selecting a TV file revoked pairing | User-started foreground connection, independent from activities | Server remains usable after the UI closes; Stop revokes access |
| A dropped upload restarted from zero | Durable 2 MiB chunks with rollback and recovery | Interrupted chunk preserves the last committed offset; restart recovery reproduces original bytes |
| Large downloads consumed browser RAM as a Blob | Browser-managed attachment using a short-lived, one-use ticket | Download contents match original; ticket reuse and revoked sessions fail |
| Filenames were technical and libraries hard to scan | Friendly names, category filters, search and sort | Original stored ID stays separate from display name |
| Phone could send but could not request viewing | Open on TV with visible TV confirmation | Background requests give an actionable instruction; no hidden launches |
| Playback still needed remote buttons | Phone play/pause, ±30 seconds, volume and close for TV Bridge video | Commands require an active player |
| Connection state and ownership were unclear | Live phone status, named sessions, individual disconnect, explicit Stop and automatic two-hour shutdown | Revoked tokens and issued download tickets stop working |

## Scope

Files, text, links, local media and diagnostics for TV Bridge. No account or backend. No bundled third-party playback providers. Text messages can wait in a bounded in-memory inbox of five messages when TV Bridge is not visible. Pairing still requires visible TV confirmation. The user can return to TV Bridge without losing approved sessions.

System-wide input, screen mirroring, Internet relays and other apps’ private diagnostics remain outside this release. These require different Android permissions or explicit integrations and would change the product’s trust model.

## Quality bar

- Small native Android application with bounded transfer buffers and sampled photo decoding.
- No silent installation, unexpected link opening, or broad storage enumeration.
- Clear first success: install on TV, scan, enter code, approve, send a small photo, confirm Open on TV.
- Recoverable upload interruption; native download handoff; meaningful empty/error states.
- Automatic build, lint, protocol/storage/QR tests, Android integration checks, activity/service continuity smoke checks, release-candidate launch, checksum and private production-certificate verification before publishing.
- Physical TV/iPhone usability is tracked separately from emulator test evidence.

## Publication channel

Version 1.1 is published through GitHub Releases as an installable release-mode APK. Google Play/App Store submission requires owner accounts and store review and is not represented as completed. The first stable release uses a privately backed-up production certificate; preview users must back up and reinstall because the original CI key was not retained. Future final APKs are checked against the committed production fingerprint. Store distribution also requires current store-policy review.
