# Architecture and local protocol

Native Java Android TV application with no application dependencies. Framework Views handle remote focus; a bounded four-thread HTTP server hosts three bundled browser assets. The browser uses native fetch/XHR, with no CDN or build tooling. A fixed version 4-L QR encoder produces the local URL; independent matrix comparisons run in CI.

`MainActivity` owns the server lifecycle and permission/confirmation UI. `BridgeServer` manages pairing, sessions, shared storage and JSON endpoints. `HttpWire` handles bounded HTTP/1.1 framing and exact-length streaming. `SharedFileProvider` exposes read-only, individually granted content URIs. `MediaActivity` uses sampled bitmap decoding and platform video playback.

Server address: `http://<TV IPv4>:8765`. Host must exactly match that address. Requests with an Origin must match it. All endpoints below require `Authorization: Bearer <token>` except pairing and static assets. Responses include no-store, nosniff, no-referrer and a restrictive same-origin CSP. HTTP/1.0, chunked request bodies, duplicate headers, oversized headers and bodies larger than 1 GiB are rejected. Browser raw File uploads supply Content-Length automatically.

| Method / path | Payload / result |
| --- | --- |
| GET `/`, `/app.js`, `/style.css` | Public bundled phone interface |
| POST `/api/pair` | JSON `{code, name}` → 202 `{request}`; TV confirmation queued |
| POST `/api/pair/status` | JSON `{request}` → 202 waiting or 200 `{token}`; one-time consume |
| GET `/api/files` | `{files:[{name,size,modified}], free, maxUpload}` |
| POST `/api/upload` | Raw binary body; URI-encoded `X-File-Name`; → `{name}` |
| POST `/api/file` | JSON `{name}` → attachment bytes |
| DELETE `/api/file` | JSON `{name}` → delete shared file |
| POST `/api/send` | JSON `{text,link}` → TV dialog; 8000 character maximum |
| GET `/api/diagnostics` | JSON attachment, generated at request time |
| POST `/api/logout` | Revoke current token |

Transport: one request per connection, explicit Content-Length and close. Uploads reserve 32 MiB free, stream in 64 KiB buffers, fsync to a `.part` file, then rename atomically to a generated timestamp/UUID filename. Only one upload runs at a time; partial files cannot be downloaded. Interrupted uploads remove their temporary file; crash leftovers older than 24 hours are cleaned on next start. Socket reads time out after 30 seconds of inactivity; there is no range/resume protocol yet. Client upload timeout is 30 minutes. Download buffering is on the phone, not the TV.

Server is deliberately activity-scoped. Browsing files/About keeps it alive; launching another activity closes it. This is not a background service. Android TV Home, activity switching, or network changes invalidate connections; the next visible session needs new pairing. TV-file import copies only a selected URI and leaves the original untouched.

Manual review before expanding scope: foreground-service policies, large download streaming in Safari, HTTPS trust/pairing model, durable signing, delegated third-party diagnostics, and any system-wide input permissions.

Platform references: [Android TV setup](https://developer.android.com/training/tv/get-started/create), [secure content URI sharing](https://developer.android.com/training/secure-file-sharing/share-file).
