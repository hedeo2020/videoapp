# Threat model and security limitations

| Threat | Primary mitigation | Residual risk |
|---|---|---|
| Credential or refresh-token theft | Argon2id, short access lifetime, hashed rotating refresh sessions, reuse detection | A live authenticated session can be abused until detected/revoked |
| Shared accounts and concurrent-stream abuse | Device inventory, short playback grants, server concurrency limits | Coordinated sharing within limits remains possible |
| URL sharing and segment scraping | Private origins, session/video/device-scoped authorization, rapid expiry, revocation | An authorized client can observe delivered encrypted bytes |
| Rooting, hooking, emulators, repackaging | Play Integrity adapter, app-signature and risk signals, server deny/warn/log policy, obfuscation | Signals can be bypassed; never treated as proof |
| Screen or camera capture | `FLAG_SECURE`, secure surfaces, optional moving forensic watermark | External cameras and modified devices can still record content |
| Admin compromise | Separate admin flow, RBAC, CSRF protection, secure cookies, audit log | A privileged session can cause material damage |
| Malicious uploads | Content inspection, size limits, generated names, isolated FFmpeg, malware hook | Parser vulnerabilities require rapid patching and isolation |
| Storage/database leakage | Private network/buckets, least privilege, encryption and secret manager | Compromised infrastructure may expose metadata or media |
| SSRF and brute force | Destination allowlists, validation, egress controls, progressive throttling | Distributed low-rate attacks remain possible |
| Denial of service and insider abuse | Rate limits, resource quotas, audit trails, backups and separation of duties | A small VPS has finite capacity; trusted operators retain power |

Capture prevention is defense in depth, not an absolute guarantee. AES-encrypted HLS is not equivalent to Widevine DRM. SecureStream never presents development non-DRM playback as suitable for protected production content and must fail closed if a protected title lacks valid provider configuration.
