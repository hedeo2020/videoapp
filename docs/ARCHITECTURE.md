# Architecture

SecureStream is a modular monorepo with one public API, one private processing worker, a browser admin application, and a native Android viewer. PostgreSQL is authoritative; Redis holds queues, rate-limit state, and ephemeral playback state. Source media stays private. Playback begins only after the API evaluates the user, title, device, risk, and stream-limit policy and issues a short-lived, scoped grant.

Production protected titles use DASH Common Encryption and an authorized Widevine provider. Development HLS is deliberately non-DRM. The worker boundary permits replacing FFmpeg HLS packaging with a commercial packager without exposing keys to clients.

## Milestones

1. Foundation: workspace, schema, API/admin/Android bases, Compose and CI.
2. Identity: rotating sessions, admin cookie auth, recovery, verification, RBAC and audit.
3. Catalog: CRUD, configurable rails, discovery and PostgreSQL search.
4. Media: multipart storage, inspection, encoding, cleanup and publication gate.
5. Playback: grants, Media3, progress, concurrent streams and watermark.
6. DRM: provider integration, license authorization and fail-closed policy.
7. Hardening: operational tests, backups, observability and Coolify launch review.

## API route plan

All business routes are under `/api/v1`: `auth`, `account`, `catalog`, `search`, `profiles`, `my-list`, `progress`, `playback`, `admin/auth`, `admin/videos`, `admin/uploads`, `admin/catalog`, `admin/users`, `admin/settings`, and `admin/audit-logs`. `/health` and `/ready` are operational endpoints.

## Identity implementation

Access tokens expire quickly. Refresh values are opaque random credentials and only SHA-256 hashes are stored. Each rotation revokes the prior row and links to its replacement. Reuse of a revoked token revokes every live session in that token family and creates a high-severity security event. Five failed logins trigger progressive lock periods. Password reset revokes all sessions.

Administrator endpoints independently verify role permissions on the server. Admin browser authentication uses a short-lived HTTP-only cookie plus a double-submit CSRF token for mutations. Login, logout, user-state changes, and other privileged operations are recorded with a request ID and hashed IP address.
