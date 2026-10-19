# Architecture — 1.1

Native Java Android TV app and three bundled browser assets. No application runtime dependencies, cloud API or CDN. `MainActivity` handles TV UI; `BridgeService` owns a user-started connected-device foreground connection; `BridgeServer` owns bounded HTTP connections and authorization; `UploadStore` owns durable staged transfers. `MediaActivity` handles sampled photos/platform video and local playback commands. `SharedFileProvider` grants read-only individual content URIs.

The service starts from a visible activity, declares Android’s connectedDevice foreground-service type and prerequisites, displays a notification with Stop, and shuts down after two hours. A bounded, timed partial wake lock is held during transfers, not while simply idle. Process death does not restart it automatically. Android may still stop the process. Network changes revoke sessions and rebind. See [Android foreground-service requirements](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device).

## Protocol

Address `http://<private IPv4>:8765`. Strict matching Host and Origin; cross-site Fetch Metadata is rejected. One HTTP/1.1 request per connection, explicit length, no chunked request bodies, bounded headers, four workers and eight queued sockets. At most two concurrent downloads preserve worker capacity for controls. Socket read inactivity timeout: 30 seconds.

All API routes except pairing require a bearer token. Static HTML/JS/CSS are public. The native download route instead consumes a one-use ticket tied to a still-valid session. No general CORS access is granted. Responses use no-store, nosniff, no-referrer and same-origin CSP.

| Route | Purpose |
| --- | --- |
| POST `/api/pair` | `{code,name}` → `{request}`; queues TV approval |
| POST `/api/pair/status` | `{request}` → waiting or one-time `{token}` |
| GET `/api/status` | Device/version, session expiry, storage, media state and bounded events |
| GET `/api/files` | IDs, friendly names, categories, sizes and timestamps |
| POST `/api/upload/start` | `{name,size,signature}` → transfer ID and offset |
| POST `/api/upload/status` | `{id}` → committed offset / completion |
| POST `/api/upload/chunk` | Raw ≤2 MiB; `X-Upload-Id`, `X-Upload-Offset` |
| POST `/api/upload/finish` | `{id}` → atomic publication and final saved-file SHA-256 |
| POST `/api/upload/ack` | Remove completed transfer metadata |
| POST `/api/upload/discard` | Delete staged data; does not delete a finalized shared file |
| POST `/api/download` | `{name}` → one-minute, single-use download URL |
| GET `/download?ticket=…` | Native browser attachment; session must remain valid |
| POST / DELETE `/api/file` | Legacy authenticated bytes / shared-file deletion |
| POST `/api/open` | Request TV confirmation to open a shared file; visible TV UI required |
| POST `/api/media` | `{action}`; TV Bridge player only |
| POST `/api/send` | `{text,link}`; text ≤8000 characters, HTTP(S) links only |
| GET `/api/diagnostics` | User-requested JSON report |
| POST `/api/logout` | Revoke current session, tickets and its active sockets |

The original raw `/api/upload` remains compatible but is not the browser client’s transfer path.

## Durable upload contract

At most eight staged transfers. Random 128-bit transfer IDs are capabilities and require an approved session to use. Metadata and `.part` files reside outside the shared folder. Every chunk requires the exact committed offset and reserves 32 MiB free storage; a failed/truncated chunk rolls back to its prior length. Completed chunks are fsynced. Finish atomically moves the file into shared storage, then computes SHA-256. Repeating finish before acknowledgement is idempotent. Metadata/staged data older than 24 hours is removed on next server start.

Browser identity combines file name, size, modification time and a sampled noncryptographic checksum. The server’s saved-file SHA-256 is reported separately; it is not represented as source-hash verification. Phone local storage holds paused IDs/history; session storage holds the authorization token. Re-pairing at the same origin permits recovery; a new IP address is a new browser origin.

Names are sanitized and prefixed internally; friendly display names remove that prefix. Canonical path checks constrain access to the private shared directory. TV imports use Android’s document picker and copy only selected URIs. External viewers get temporary read permission for a content URI.

The service queues at most five text/link messages in memory when the TV UI is hidden. It does not launch activities secretly from the background. Pairing and link/file confirmation remain TV actions. Video controls use the active TV Bridge player only.
