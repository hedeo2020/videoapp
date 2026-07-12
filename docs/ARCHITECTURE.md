# Architecture

SecureStream is a modular monorepo with one public API, one private processing worker, a browser admin application, and a native Android viewer. PostgreSQL is authoritative; Redis holds queues, rate-limit state, and ephemeral playback state. Source media stays private. Playback begins only after the API evaluates the user, title, device, risk, and stream-limit policy and issues a short-lived, scoped grant.

Production protected titles use DASH Common Encryption and an authorized Widevine provider. Development HLS is deliberately non-DRM. The worker boundary permits replacing FFmpeg HLS packaging with a commercial packager without exposing keys to clients.

## Milestones

1. Foundation: workspace, schema, API/admin/Android bases, Compose and CI.
2. Identity: rotating sessions, admin cookie auth, recovery, verification, RBAC and audit.
3. Catalog: CRUD, configurable rails, discovery and PostgreSQL search.
4. Media: multipart upload records, processing queue handoff, publication gate and worker boundary.
5. Playback: grants, progress, history, my-list, concurrent streams and watermark.
6. DRM: provider abstraction settings, Widevine license URL exposure and fail-closed policy.
7. Hardening: security/catalog tests, migration-backed schema changes, backups, observability and Coolify launch review.

## API route plan

All business routes are under `/api/v1`: `auth`, `account`, `catalog`, `search`, `my-list`, `history`, `playback`, `admin/auth`, `admin/movies`, `admin/series`, `admin/seasons`, `admin/collections`, `admin/video-assets`, `admin/uploads`, `admin/processing`, `admin/users`, and `admin/audit-logs`. `/health` and `/ready` are operational endpoints.

## Catalog, media, and playback implementation

Collections are configurable rails. Rail responses are filtered server-side so viewers only see published, currently available titles. Movies also require a ready asset before they can appear or be published. Search checks title, synopsis, genre, and tags while applying the same playback-readiness rules.

Uploads are tracked in PostgreSQL with expected and received byte counts. Completing an upload either marks an asset ready when a manifest is already provided or queues the worker for transcoding. The worker performs disk-space checks before FFprobe/FFmpeg execution.

Playback grants are created only after account, availability, asset, DRM, and concurrent-stream checks pass. The API records playback sessions, watch history, and progress, and returns only scoped playback metadata rather than raw storage credentials.

## Identity implementation

Access tokens expire quickly. Refresh values are opaque random credentials and only SHA-256 hashes are stored. Each rotation revokes the prior row and links to its replacement. Reuse of a revoked token revokes every live session in that token family and creates a high-severity security event. Five failed logins trigger progressive lock periods. Password reset revokes all sessions.

Administrator endpoints independently verify role permissions on the server. Admin browser authentication uses a short-lived HTTP-only cookie plus a double-submit CSRF token for mutations. Login, logout, user-state changes, and other privileged operations are recorded with a request ID and hashed IP address.
