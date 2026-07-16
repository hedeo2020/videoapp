"use client";

import { animate, stagger } from "animejs";
import { type DragEvent, type FormEvent, type MouseEvent, useEffect, useMemo, useRef, useState } from "react";

const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:4000/api/v1";
const nav = [
  "Overview",
  "Catalog",
  "Collections",
  "Videos",
  "Series",
  "Uploads",
  "Video Editor",
  "Processing",
  "File Manager",
  "Storage",
  "Users",
  "Device Sessions",
  "Messages",
  "Notifications",
  "API Tokens",
  "Playback sessions",
  "Watermark Trace",
  "Backup & Restore",
  "Activity",
  "Trash",
  "Audit logs",
  "Security",
  "Settings",
] as const;
type Tab = (typeof nav)[number];
const navGroups: { label: string; items: readonly Tab[] }[] = [
  { label: "Workspace", items: ["Overview", "Catalog", "Collections"] },
  {
    label: "Media library",
    items: ["Videos", "Series", "Uploads", "Video Editor", "Processing", "File Manager", "Storage"],
  },
  { label: "People", items: ["Users", "Device Sessions", "Messages", "Notifications"] },
  { label: "Access & trace", items: ["API Tokens", "Playback sessions", "Watermark Trace"] },
  { label: "Operations", items: ["Backup & Restore", "Activity", "Trash", "Audit logs", "Security", "Settings"] },
];
type Admin = { id: string; displayName: string; email: string; role: string };
type RecordItem = Record<string, unknown>;

export default function App() {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    fetch(`${API}/admin/auth/me`, { credentials: "include", headers: authHeaders() })
      .then((response) => (response.ok ? response.json() : null))
      .then((payload) => {
        if (payload?.accessToken) sessionStorage.setItem("ss_admin_access", payload.accessToken);
        setAdmin(payload);
      })
      .catch(() => setAdmin(null))
      .finally(() => setChecking(false));
  }, []);

  if (checking)
    return (
      <div className="center">
        <div className="spinner" />
        <p>Validating secure session</p>
      </div>
    );
  return admin ? <Dashboard admin={admin} /> : <Login onLogin={setAdmin} />;
}

function SecureLogo() {
  return (
    <span className="lockmark" aria-hidden="true">
      <svg viewBox="0 0 64 64" role="img">
        <rect x="14" y="27" width="36" height="27" rx="5" />
        <path d="M20 28V20c0-8 5-14 12-14s12 6 12 14v8h-6V20c0-5-3-9-6-9s-6 4-6 9v8z" />
        <circle cx="32" cy="40" r="4" className="lockhole" />
        <path d="M32 44v6" className="lockslot" />
      </svg>
    </span>
  );
}

function Login({ onLogin }: { onLogin: (admin: Admin) => void }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const data = new FormData(event.currentTarget);

    try {
      const response = await fetch(`${API}/admin/auth/login`, {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          email: data.get("email"),
          password: data.get("password"),
          deviceId: `admin-web-${navigator.userAgent.length}`,
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) return setError(payload.error?.message ?? "Sign in failed");
      sessionStorage.setItem("ss_csrf", payload.csrfToken);
      sessionStorage.setItem("ss_admin_access", payload.accessToken);
      onLogin(payload.user);
    } catch {
      setError("Sign in request could not reach the API. Check NEXT_PUBLIC_API_URL and ADMIN_ORIGIN in Coolify.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="login">
      <section>
        <div className="brand">
          <SecureLogo /> SecureStream
        </div>
        <small>ADMINISTRATION CONSOLE</small>
        <h1>Welcome back</h1>
        <p>Sign in with your administrator account.</p>
        <form onSubmit={submit}>
          <label>
            Email
            <input name="email" type="email" autoComplete="username" required />
          </label>
          <label>
            Password
            <input name="password" type="password" autoComplete="current-password" minLength={12} required />
          </label>
          {error && (
            <div className="formerror" role="alert">
              {error}
            </div>
          )}
          <button type="submit" className="primary" disabled={busy}>
            {busy ? "Signing in..." : "Sign in securely"}
          </button>
        </form>
        <footer>Protected by server-enforced roles, secure cookies, and audit logging.</footer>
      </section>
    </main>
  );
}

function Dashboard({ admin }: { admin: Admin }) {
  const [active, setActive] = useState<Tab>("Overview");
  const [data, setData] = useState<Record<string, unknown>>({});
  const [sidebarHidden, setSidebarHidden] = useState(false);
  const [collapsedNavGroups, setCollapsedNavGroups] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [conversionProgress, setConversionProgress] = useState<number | null>(null);
  const [uploadPhase, setUploadPhase] = useState("");
  const [notice, setNotice] = useState("");
  const [preview, setPreview] = useState<{ title: string; url: string; playable: boolean; format: string } | null>(
    null,
  );
  const [editing, setEditing] = useState<RecordItem | null>(null);
  const [newApiToken, setNewApiToken] = useState("");
  const mainRef = useRef<HTMLElement | null>(null);
  const noticeRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const mobile = window.matchMedia("(max-width: 900px)");
    if (mobile.matches) setSidebarHidden(true);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !sidebarHidden) setSidebarHidden(true);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [sidebarHidden]);

  const metrics = useMemo(
    () => [
      ["Videos", count(data.movies), "catalog"],
      ["Series", count(data.series), "episodic"],
      ["Users", count(data.users), "accounts"],
      ["Jobs", jobsTotal(data.processing), "queue"],
    ],
    [data],
  );
  const unreadMessages = useMemo(
    () => asArray(data.conversations).reduce((total, conversation) => total + Number(conversation.unreadCount ?? 0), 0),
    [data.conversations],
  );

  async function load(tab = active) {
    setLoading(true);
    setNotice("");
    try {
      const next: Record<string, unknown> = { ...data };
      const get = async (key: string, path: string) => {
        next[key] = await apiGet(path);
      };
      if (["Overview", "Videos", "Uploads", "Collections", "Catalog", "Users", "Video Editor"].includes(tab))
        await get("movies", "/admin/movies");
      if (tab === "File Manager") await get("files", "/admin/files");
      if (tab === "Storage") await get("storageBreakdown", "/admin/storage-breakdown");
      if (tab === "Video Editor") await get("editorJobs", "/admin/editor/jobs");
      if (["Overview", "Series"].includes(tab)) await get("series", "/admin/series");
      if (["Overview", "Collections", "Catalog", "Users"].includes(tab)) await get("collections", "/admin/collections");
      if (["Overview", "Processing"].includes(tab)) await get("processing", "/admin/processing/jobs");
      if (tab === "Overview") await get("system", "/admin/system-status");
      if (tab === "Overview") await get("cleanup", "/admin/storage-cleanup");
      if (tab === "Users") await get("users", "/admin/users");
      if (tab === "Device Sessions") await get("deviceSessions", "/admin/device-sessions");
      if (tab === "Messages") await get("conversations", "/admin/conversations");
      if (tab === "Messages") await get("users", "/admin/users");
      if (tab === "Notifications") await get("notifications", "/admin/notifications");
      if (tab === "Notifications") await get("users", "/admin/users");
      if (tab === "API Tokens") await get("apiTokens", "/admin/api-tokens");
      if (["Playback sessions", "Watermark Trace"].includes(tab)) await get("playback", "/admin/playback-sessions");
      if (tab === "Backup & Restore") await get("backups", "/admin/backups");
      if (tab === "Activity") await get("activity", "/admin/activity");
      if (tab === "Trash") await get("trash", "/admin/trash");
      if (tab === "Audit logs") await get("audit", "/admin/audit-logs");
      if (tab === "Security") await get("security", "/admin/security-events");
      if (tab === "Settings") await get("settings", "/admin/settings");
      setData(next);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Could not load this panel.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(active);
  }, [active]);
  useEffect(() => {
    if (active !== "Overview") return;
    let cancelled = false;
    const refreshSystem = async () => {
      try {
        const system = await apiGet("/admin/system-status");
        if (!cancelled) setData((current) => ({ ...current, system }));
      } catch {
        // Keep the existing snapshot visible if one realtime poll misses.
      }
    };
    const timer = setInterval(() => void refreshSystem(), 1000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [active]);
  useEffect(() => {
    if (active !== "Video Editor") return;
    const timer = setInterval(() => {
      void apiGet("/admin/editor/jobs")
        .then((editorJobs) => setData((current) => ({ ...current, editorJobs })))
        .catch(() => undefined);
    }, 1500);
    return () => clearInterval(timer);
  }, [active]);
  useEffect(() => {
    if (active !== "Messages") return;
    const refreshMessagesAsRead = async () => {
      await fetch(`${API}/admin/conversations/read-all`, {
        method: "PATCH",
        credentials: "include",
        headers: csrfHeaders(),
      }).catch(() => undefined);
      const conversations = await apiGet("/admin/conversations");
      setData((current) => ({ ...current, conversations }));
    };
    void refreshMessagesAsRead();
    const timer = setInterval(() => {
      void refreshMessagesAsRead().catch(() => undefined);
    }, 2500);
    return () => clearInterval(timer);
  }, [active]);
  useEffect(() => {
    let cancelled = false;
    const refreshConversations = async () => {
      try {
        const conversations = await apiGet("/admin/conversations");
        if (!cancelled) setData((current) => ({ ...current, conversations }));
      } catch {
        // Keep the last unread badge if one poll misses.
      }
    };
    void refreshConversations();
    const timer = setInterval(() => void refreshConversations(), 5000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (prefersReducedMotion() || !mainRef.current) return;
    const targets = mainRef.current.querySelectorAll(".metrics article,.panel,.workspace-note");
    animate(targets, {
      opacity: [0, 1],
      translateY: [14, 0],
      scale: [0.985, 1],
      duration: 520,
      delay: stagger(28),
      ease: "outCubic",
    });
  }, [active]);

  useEffect(() => {
    if (!notice || prefersReducedMotion() || !noticeRef.current) return;
    animate(noticeRef.current, {
      opacity: [0, 1],
      translateY: [-8, 0],
      duration: 320,
      ease: "outCubic",
    });
  }, [notice]);

  function animatePress(event: MouseEvent<HTMLButtonElement>) {
    if (prefersReducedMotion()) return;
    animate(event.currentTarget, { scale: [0.97, 1], duration: 260, ease: "outBack" });
  }

  function toggleNavGroup(label: string) {
    setCollapsedNavGroups((groups) => ({ ...groups, [label]: !groups[label] }));
  }

  async function uploadMovie(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const fileInput = form.elements.namedItem("file") as HTMLInputElement | null;
    const files = Array.from(fileInput?.files ?? []);
    if (!files.length) return setNotice("Choose at least one video file.");
    const formData = new FormData(form);
    const uploaded: RecordItem[] = [];
    setLoading(true);
    setUploadProgress(0);
    setConversionProgress(null);
    setUploadPhase(files.length > 1 ? `Uploading 1/${files.length}` : "Uploading");
    setNotice("");
    try {
      for (let index = 0; index < files.length; index += 1) {
        const file = files[index];
        setConversionProgress(null);
        setUploadProgress(files.length > 1 ? Math.round((index / files.length) * 100) : 0);
        setNotice(files.length > 1 ? `Uploading ${index + 1}/${files.length}: ${file.name}` : "");
        const payload = await uploadWithProgress(
          `${API}/admin/uploads/direct`,
          uploadFormData(formData, file, index, files.length),
          csrfHeaders(),
          (progress) =>
            setUploadProgress(
              files.length > 1 ? Math.round(((index + progress / 100) / files.length) * 100) : progress,
            ),
          (phase) => setUploadPhase(files.length > 1 ? `${phase} ${index + 1}/${files.length}` : phase),
          setConversionProgress,
        );
        uploaded.push(payload);
      }
      form.reset();
      setNotice(
        uploaded.length === 1
          ? `Uploaded "${uploaded[0].title}" as a draft title.`
          : `Uploaded ${uploaded.length} videos as draft titles.`,
      );
      await load("Uploads");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Upload failed.");
    } finally {
      setLoading(false);
      setUploadProgress(null);
      setConversionProgress(null);
      setUploadPhase("");
    }
  }

  async function publishMovie(id: string) {
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/movies/${id}/publish`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Publish failed");
      setNotice("Video published.");
      await load(active);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Publish failed.");
    } finally {
      setLoading(false);
    }
  }

  async function createTrimJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/editor/jobs/trim`, {
        method: "POST",
        credentials: "include",
        headers: { ...csrfHeaders(), "content-type": "application/json" },
        body: JSON.stringify({
          assetId: data.get("assetId"),
          title: optionalFormString(data, "title"),
          startSeconds: Number(data.get("startSeconds") ?? 0),
          endSeconds: Number(data.get("endSeconds") ?? 0),
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Trim job failed");
      setNotice("Video trim job started. The edited clip will appear as a new draft when ready.");
      await load("Video Editor");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Trim job failed.");
    } finally {
      setLoading(false);
    }
  }

  async function createApiToken(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    setLoading(true);
    setNotice("");
    setNewApiToken("");
    try {
      const response = await fetch(`${API}/admin/api-tokens`, {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json", ...csrfHeaders() },
        body: JSON.stringify({ name: String(formData.get("name") ?? "").trim() }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Token creation failed");
      form.reset();
      setNewApiToken(String(payload.token ?? ""));
      setNotice("API token created. Copy it now; it will only be shown once.");
      await load("API Tokens");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Token creation failed.");
    } finally {
      setLoading(false);
    }
  }

  async function revokeApiToken(token: RecordItem) {
    if (!token.id) return;
    if (!confirm(`Revoke "${String(token.name ?? "this token")}"? n8n automations using it will stop working.`)) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/api-tokens/${token.id}`, {
        method: "DELETE",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error?.message ?? "Token revoke failed");
      }
      setNotice("API token revoked.");
      await load("API Tokens");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Token revoke failed.");
    } finally {
      setLoading(false);
    }
  }

  async function runStorageCleanup() {
    if (
      !confirm(
        "Clean orphaned media files? This only removes files no longer referenced by the database and older than one hour.",
      )
    )
      return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/storage-cleanup`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Cleanup failed");
      setData((current) => ({ ...current, cleanup: payload }));
      setNotice(
        `Cleanup finished: deleted ${payload.deletedFiles ?? 0} files and reclaimed ${formatBytes(payload.deletedBytes)}.`,
      );
      await load("Overview");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Cleanup failed.");
    } finally {
      setLoading(false);
    }
  }

  async function updateSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/settings`, {
        method: "PATCH",
        credentials: "include",
        headers: { ...csrfHeaders(), "content-type": "application/json" },
        body: JSON.stringify({
          deleteOriginalAfterPreview: formData.get("deleteOriginalAfterPreview") === "on",
          maintenanceMode: formData.get("maintenanceMode") === "on",
          maintenanceMessage: String(formData.get("maintenanceMessage") ?? ""),
          backupScheduleEnabled: formData.get("backupScheduleEnabled") === "on",
          backupScheduleHour: Number(formData.get("backupScheduleHour") ?? 2),
          backupRetentionCount: Number(formData.get("backupRetentionCount") ?? 7),
          backupScheduleDrive: formData.get("backupScheduleDrive") === "on",
          storageWarningPercent: Number(formData.get("storageWarningPercent") ?? 80),
          androidLatestVersionName: String(formData.get("androidLatestVersionName") ?? ""),
          androidLatestVersionCode: Number(formData.get("androidLatestVersionCode") ?? 1),
          androidUpdateRequired: formData.get("androidUpdateRequired") === "on",
          androidUpdateMessage: String(formData.get("androidUpdateMessage") ?? ""),
          androidDownloadUrl: optionalFormString(formData, "androidDownloadUrl") ?? null,
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Settings update failed");
      setData((current) => ({ ...current, settings: payload }));
      setNotice("Settings saved.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Settings update failed.");
    } finally {
      setLoading(false);
    }
  }

  async function updateMovie(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing?.id) return;
    const form = event.currentTarget;
    const data = new FormData(form);
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/movies/${editing.id}`, {
        method: "PATCH",
        credentials: "include",
        headers: { ...csrfHeaders(), "content-type": "application/json" },
        body: JSON.stringify({
          title: data.get("title"),
          synopsis: data.get("synopsis"),
          maturityRating: data.get("maturityRating") || undefined,
          status: data.get("status"),
          featured: data.get("featured") === "on",
          posterUrl: optionalFormString(data, "posterUrl"),
          backdropUrl: optionalFormString(data, "backdropUrl"),
          trailerUrl: optionalFormString(data, "trailerUrl"),
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Update failed");
      setEditing(null);
      setNotice(`Updated "${payload.title}".`);
      await load(active);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Update failed.");
    } finally {
      setLoading(false);
    }
  }

  async function deleteMovie(movie: RecordItem) {
    if (!movie.id) return;
    const confirmed = confirm(
      `Delete "${String(movie.title ?? "this video")}"? This removes the catalog record and uploaded source file.`,
    );
    if (!confirmed) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/movies/${movie.id}`, {
        method: "DELETE",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error?.message ?? "Delete failed");
      }
      if (editing?.id === movie.id) setEditing(null);
      setPreview(null);
      setNotice(`Deleted "${String(movie.title ?? "video")}".`);
      await load(active);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Delete failed.");
    } finally {
      setLoading(false);
    }
  }

  async function createCollection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/collections`, {
        method: "POST",
        credentials: "include",
        headers: { ...csrfHeaders(), "content-type": "application/json" },
        body: JSON.stringify(collectionPayload(formData)),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Could not create folder");
      form.reset();
      setNotice(`Created folder "${payload.name}".`);
      await load("Collections");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Could not create folder.");
    } finally {
      setLoading(false);
    }
  }

  async function updateCollection(collection: RecordItem, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!collection.id) return;
    const formData = new FormData(event.currentTarget);
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/collections/${collection.id}`, {
        method: "PATCH",
        credentials: "include",
        headers: { ...csrfHeaders(), "content-type": "application/json" },
        body: JSON.stringify(collectionPayload(formData)),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Could not update folder");
      setNotice(`Updated folder "${payload.name}".`);
      await load("Collections");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Could not update folder.");
    } finally {
      setLoading(false);
    }
  }

  async function deleteCollection(collection: RecordItem) {
    if (!collection.id) return;
    const confirmed = confirm(`Delete folder "${String(collection.name ?? "Untitled")}"? Videos will not be deleted.`);
    if (!confirmed) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/collections/${collection.id}`, {
        method: "DELETE",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error?.message ?? "Could not delete folder");
      }
      setNotice(`Deleted folder "${String(collection.name ?? "Untitled")}".`);
      await load("Collections");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Could not delete folder.");
    } finally {
      setLoading(false);
    }
  }

  async function createUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/users`, {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json", ...csrfHeaders() },
        body: JSON.stringify(userPayload(new FormData(form), true)),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "User creation failed");
      if (payload?.id)
        setData((current) => ({
          ...current,
          users: [payload, ...asArray(current.users).filter((user) => user.id !== payload.id)],
        }));
      form.reset();
      setNotice("User created.");
      await load("Users");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "User creation failed.");
    } finally {
      setLoading(false);
    }
  }

  async function updateUser(user: RecordItem, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!user.id) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/users/${user.id}`, {
        method: "PATCH",
        credentials: "include",
        headers: { "content-type": "application/json", ...csrfHeaders() },
        body: JSON.stringify(userPayload(new FormData(event.currentTarget), false)),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "User update failed");
      setNotice("User updated.");
      await load("Users");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "User update failed.");
    } finally {
      setLoading(false);
    }
  }

  async function deleteUser(user: RecordItem) {
    if (!user.id) return;
    const confirmed = confirm(
      `Delete user "${String(user.email ?? "this user")}"? This removes their account, sessions, history, and access rules.`,
    );
    if (!confirmed) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/users/${user.id}`, {
        method: "DELETE",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error?.message ?? "User delete failed");
      }
      setNotice("User deleted.");
      await load("Users");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "User delete failed.");
    } finally {
      setLoading(false);
    }
  }

  async function sendAdminMessage(conversation: RecordItem, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!conversation.id) return;
    const form = event.currentTarget;
    const body = String(new FormData(form).get("body") ?? "").trim();
    if (!body) return setNotice("Message cannot be empty.");
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/conversations/${conversation.id}/messages`, {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json", ...csrfHeaders() },
        body: JSON.stringify({ body }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Message failed");
      form.reset();
      setNotice("Reply sent.");
      await load("Messages");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Message failed.");
    } finally {
      setLoading(false);
    }
  }

  async function startAdminConversation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    const userId = String(formData.get("userId") ?? "");
    const body = String(formData.get("body") ?? "").trim();
    if (!userId || !body) return setNotice("Choose a user and type a message.");
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/conversations`, {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json", ...csrfHeaders() },
        body: JSON.stringify({ userId, body }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Could not start chat");
      form.reset();
      setNotice("Chat started.");
      await load("Messages");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Could not start chat.");
    } finally {
      setLoading(false);
    }
  }

  async function sendNotification(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/notifications`, {
        method: "POST",
        credentials: "include",
        headers: { "content-type": "application/json", ...csrfHeaders() },
        body: JSON.stringify({
          title: formData.get("title"),
          body: formData.get("body"),
          allUsers: formData.get("allUsers") === "on",
          userIds: formData.getAll("userIds").map(String).filter(Boolean),
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Notification failed");
      form.reset();
      setNotice(`Notification sent to ${payload.sent ?? 0} users.`);
      await load("Notifications");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Notification failed.");
    } finally {
      setLoading(false);
    }
  }

  async function createBackup() {
    setLoading(true);
    setNotice("Creating backup. Large video libraries can take a while...");
    try {
      const response = await fetch(`${API}/admin/backups`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Backup failed");
      setNotice(`Backup ready: ${payload.name}`);
      await load("Backup & Restore");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Backup failed.");
    } finally {
      setLoading(false);
    }
  }

  async function restoreBackup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (
      !confirm(
        "Restore will replace users, catalog, folders, chats, notifications, watch data, and media files with the backup contents. Continue?",
      )
    )
      return;
    const form = event.currentTarget;
    const formData = new FormData(form);
    setLoading(true);
    setNotice("Restoring backup. Do not close this page...");
    try {
      const response = await fetch(`${API}/admin/backups/restore`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
        body: formData,
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Restore failed");
      form.reset();
      setNotice(`Restore complete. Media files restored: ${payload.mediaFiles ?? 0}`);
      await load("Backup & Restore");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Restore failed.");
    } finally {
      setLoading(false);
    }
  }

  async function deleteBackup(backup: RecordItem) {
    const name = String(backup.name ?? "");
    if (!name || !confirm(`Delete backup "${name}" from this server?`)) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/backups/${encodeURIComponent(name)}`, {
        method: "DELETE",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error?.message ?? "Backup delete failed");
      }
      setNotice("Backup deleted.");
      await load("Backup & Restore");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Backup delete failed.");
    } finally {
      setLoading(false);
    }
  }

  async function uploadBackupToGoogleDrive(backup: RecordItem) {
    const name = String(backup.name ?? "");
    if (!name) return;
    setLoading(true);
    setNotice("Uploading backup to Google Drive...");
    try {
      const response = await fetch(`${API}/admin/backups/${encodeURIComponent(name)}/google-drive`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Google Drive upload failed");
      setNotice(`Uploaded to Google Drive: ${String(payload.name ?? name)}`);
      await load("Backup & Restore");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Google Drive upload failed.");
    } finally {
      setLoading(false);
    }
  }

  async function runScheduledBackupNow() {
    setLoading(true);
    setNotice("Running scheduled backup now...");
    try {
      const response = await fetch(`${API}/admin/backups/run-scheduled-now`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Scheduled backup failed");
      setNotice(`Scheduled backup ready: ${payload.name}`);
      await load("Backup & Restore");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Scheduled backup failed.");
    } finally {
      setLoading(false);
    }
  }

  async function testAlert() {
    setLoading(true);
    setNotice("Sending test alert...");
    try {
      const response = await fetch(`${API}/admin/alerts/test`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Test alert failed");
      setNotice(payload.sent ? "Test alert sent." : "No alert target is configured yet.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Test alert failed.");
    } finally {
      setLoading(false);
    }
  }

  async function revokeDeviceSession(session: RecordItem) {
    const id = String(session.id ?? "");
    if (!id || !confirm("Logout this device session?")) return;
    setLoading(true);
    try {
      const response = await fetch(`${API}/admin/device-sessions/${id}`, {
        method: "DELETE",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) throw new Error("Could not revoke device session");
      setNotice("Device session revoked.");
      await load("Device Sessions");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Could not revoke device session.");
    } finally {
      setLoading(false);
    }
  }

  async function trashAction(kind: "users" | "movies", id: string, action: "restore" | "permanent") {
    const permanent = action === "permanent";
    if (permanent && !confirm("Permanently delete this item? This cannot be undone.")) return;
    setLoading(true);
    try {
      const response = await fetch(`${API}/admin/trash/${kind}/${id}/${action}`, {
        method: permanent ? "DELETE" : "POST",
        credentials: "include",
        headers: csrfHeaders(),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error?.message ?? "Trash action failed");
      }
      setNotice(permanent ? "Item permanently deleted." : "Item restored.");
      await load("Trash");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Trash action failed.");
    } finally {
      setLoading(false);
    }
  }

  function previewMovie(movie: RecordItem) {
    const asset = firstAsset(movie);
    if (!asset?.id) {
      setNotice("No uploaded video asset is available for this title yet.");
      return;
    }
    const token = sessionStorage.getItem("ss_admin_access") ?? "";
    const format =
      String(asset.manifestStorageKey ?? asset.sourceStorageKey ?? "")
        .split(".")
        .pop()
        ?.toLowerCase() ?? "unknown";
    const playable = ["mp4", "webm", "mov"].includes(format);
    const baseUrl = `${API}/admin/video-assets/${asset.id}/preview`;
    setPreview({
      title: String(movie.title ?? "Video preview"),
      url: token ? `${baseUrl}?token=${encodeURIComponent(token)}` : baseUrl,
      playable,
      format,
    });
  }

  return (
    <div className={`shell ${sidebarHidden ? "sidebar-hidden" : ""}`}>
      {!sidebarHidden && (
        <button
          className="sidebar-backdrop"
          type="button"
          aria-label="Close navigation"
          onClick={() => setSidebarHidden(true)}
        />
      )}
      <aside id="admin-navigation" aria-label="Administration navigation">
        <div className="brandrow">
          <div className="brand">
            <SecureLogo /> SecureStream
          </div>
          <button
            className="sidehide"
            type="button"
            onClick={(event) => {
              animatePress(event);
              setSidebarHidden(true);
            }}
            aria-label="Hide admin navigation"
            title="Hide admin navigation"
          >
            ‹
          </button>
        </div>
        <nav className="navgroups">
          {navGroups.map((group) => {
            const expanded = !collapsedNavGroups[group.label];
            const hasActiveItem = group.items.includes(active);
            return (
              <section
                className={`navgroup ${hasActiveItem ? "has-active" : ""}`}
                key={group.label}
                aria-label={group.label}
              >
                <button
                  className="navgroup-toggle"
                  type="button"
                  aria-expanded={expanded}
                  onClick={(event) => {
                    animatePress(event);
                    toggleNavGroup(group.label);
                  }}
                >
                  <span>{group.label}</span>
                  <i aria-hidden="true">{expanded ? "−" : "+"}</i>
                </button>
                {expanded && (
                  <div className="navgroup-items">
                    {group.items.map((item) => {
                      const badge = item === "Messages" ? unreadMessages : 0;
                      return (
                        <button
                          type="button"
                          aria-current={item === active ? "page" : undefined}
                          className={item === active ? "active" : ""}
                          key={item}
                          onClick={(event) => {
                            animatePress(event);
                            setActive(item);
                            if (window.matchMedia("(max-width: 900px)").matches) setSidebarHidden(true);
                          }}
                        >
                          <span>{item}</span>
                          {badge > 0 && (
                            <b className="navbadge" title={`${badge} unread messages`}>
                              {badge > 99 ? "99+" : badge}
                            </b>
                          )}
                        </button>
                      );
                    })}
                  </div>
                )}
              </section>
            );
          })}
        </nav>
        <div className="operator">
          <span>{admin.displayName.slice(0, 2).toUpperCase()}</span>
          <div>
            <b>{admin.displayName}</b>
            <small>{admin.role.replace("_", " ").toLowerCase()}</small>
          </div>
        </div>
      </aside>
      <main ref={mainRef} id="admin-content">
        <header>
          <div className="headtitle">
            <button
              className="menu-toggle"
              type="button"
              aria-controls="admin-navigation"
              aria-expanded={!sidebarHidden}
              onClick={(event) => {
                animatePress(event);
                setSidebarHidden((hidden) => !hidden);
              }}
            >
              <span aria-hidden="true">☰</span> {sidebarHidden ? "Menu" : "Hide menu"}
            </button>
            <div>
              <small>OPERATIONS CENTER</small>
              <h1>{active}</h1>
              <p>{panelSubtitle(active)}</p>
            </div>
            {unreadMessages > 0 && (
              <span className="messagepill">
                {unreadMessages} unread message{unreadMessages === 1 ? "" : "s"}
              </span>
            )}
          </div>
          <div className="actions">
            <button
              type="button"
              onClick={(event) => {
                animatePress(event);
                void load(active);
              }}
              disabled={loading}
            >
              {loading ? "Refreshing..." : "Refresh"}
            </button>
            <button
              type="button"
              className="primary"
              onClick={(event) => {
                animatePress(event);
                setActive("Uploads");
              }}
            >
              + Upload
            </button>
          </div>
        </header>
        {notice && (
          <div ref={noticeRef} className="formnote workspace-note">
            {notice}
          </div>
        )}
        {active === "Overview" && (
          <Overview metrics={metrics} data={data} loading={loading} onCleanup={runStorageCleanup} />
        )}
        {preview && <PreviewModal preview={preview} onClose={() => setPreview(null)} />}
        {editing && (
          <EditMoviePanel movie={editing} loading={loading} onCancel={() => setEditing(null)} onSubmit={updateMovie} />
        )}
        {active === "Catalog" && (
          <CatalogPanel
            collections={asArray(data.collections)}
            movies={asArray(data.movies)}
            loading={loading}
            onCreateCollection={createCollection}
            onUpdateCollection={updateCollection}
            onDeleteCollection={deleteCollection}
            onPreview={previewMovie}
            onEdit={setEditing}
            onDelete={deleteMovie}
          />
        )}
        {active === "Videos" && (
          <MoviesPanel
            movies={asArray(data.movies)}
            onPublish={publishMovie}
            onPreview={previewMovie}
            onEdit={setEditing}
            onDelete={deleteMovie}
          />
        )}
        {active === "Video Editor" && (
          <VideoEditorPanel
            movies={asArray(data.movies)}
            jobs={asArray(data.editorJobs)}
            loading={loading}
            onSubmit={createTrimJob}
          />
        )}
        {active === "File Manager" && <FileManagerPanel files={asArray(data.files)} />}
        {active === "Storage" && <StorageBreakdownPanel storage={data.storageBreakdown as RecordItem | undefined} />}
        {active === "Series" && <SeriesPanel series={asArray(data.series)} />}
        {active === "Uploads" && (
          <UploadsPanel
            movies={asArray(data.movies)}
            uploading={loading}
            uploadProgress={uploadProgress}
            conversionProgress={conversionProgress}
            uploadPhase={uploadPhase}
            onUpload={uploadMovie}
            onPublish={publishMovie}
            onPreview={previewMovie}
            onEdit={setEditing}
            onDelete={deleteMovie}
          />
        )}
        {active === "Processing" && <JsonPanel title="Processing jobs" value={data.processing} />}
        {active === "Collections" && (
          <CollectionsPanel
            collections={asArray(data.collections)}
            movies={asArray(data.movies)}
            loading={loading}
            onCreate={createCollection}
            onUpdate={updateCollection}
            onDelete={deleteCollection}
          />
        )}
        {active === "Users" && (
          <UsersPanel
            users={asArray(data.users)}
            movies={asArray(data.movies)}
            collections={asArray(data.collections)}
            loading={loading}
            onCreate={createUser}
            onUpdate={updateUser}
            onDelete={deleteUser}
          />
        )}
        {active === "Device Sessions" && (
          <DeviceSessionsPanel
            sessions={asArray(data.deviceSessions)}
            loading={loading}
            onRevoke={revokeDeviceSession}
          />
        )}
        {active === "Messages" && (
          <MessagesPanel
            conversations={asArray(data.conversations)}
            users={asArray(data.users)}
            loading={loading}
            onStart={startAdminConversation}
            onReply={sendAdminMessage}
          />
        )}
        {active === "Notifications" && (
          <NotificationsPanel
            notifications={asArray(data.notifications)}
            users={asArray(data.users)}
            loading={loading}
            onSend={sendNotification}
          />
        )}
        {active === "API Tokens" && (
          <ApiTokensPanel
            tokens={asArray(data.apiTokens)}
            newToken={newApiToken}
            loading={loading}
            onCreate={createApiToken}
            onRevoke={revokeApiToken}
          />
        )}
        {active === "Playback sessions" && <PlaybackPanel rows={asArray(data.playback)} />}
        {active === "Watermark Trace" && <WatermarkTracePanel rows={asArray(data.playback)} />}
        {active === "Backup & Restore" && (
          <BackupPanel
            backups={data.backups as RecordItem | undefined}
            loading={loading}
            onCreate={createBackup}
            onRestore={restoreBackup}
            onDelete={deleteBackup}
            onDriveUpload={uploadBackupToGoogleDrive}
            onScheduledNow={runScheduledBackupNow}
          />
        )}
        {active === "Activity" && <ActivityPanel rows={asArray(data.activity)} />}
        {active === "Trash" && (
          <TrashPanel trash={data.trash as RecordItem | undefined} loading={loading} onAction={trashAction} />
        )}
        {active === "Audit logs" && (
          <TablePanel rows={asArray(data.audit)} columns={["action", "targetType", "targetId", "createdAt"]} />
        )}
        {active === "Security" && (
          <TablePanel rows={asArray(data.security)} columns={["kind", "severity", "userId", "createdAt"]} />
        )}
        {active === "Settings" && (
          <SettingsPanel
            settings={data.settings as RecordItem | undefined}
            loading={loading}
            onSave={updateSettings}
            onTestAlert={testAlert}
          />
        )}
      </main>
    </div>
  );
}

function Overview({
  metrics,
  data,
  loading,
  onCleanup,
}: {
  metrics: string[][];
  data: Record<string, unknown>;
  loading: boolean;
  onCleanup: () => void;
}) {
  return (
    <>
      <section className="metrics">
        {metrics.map(([label, value, trend]) => (
          <article key={label}>
            <small>{label}</small>
            <strong>{value}</strong>
            <em>{trend}</em>
          </article>
        ))}
      </section>
      <SystemStatusPanel system={data.system as RecordItem | undefined} />
      <StorageCleanupPanel cleanup={data.cleanup as RecordItem | undefined} loading={loading} onCleanup={onCleanup} />
      <section className="grid">
        <JsonPanel title="Queue snapshot" value={data.processing} />
        <JsonPanel
          title="Recent catalog"
          value={{ videos: count(data.movies), collections: count(data.collections), series: count(data.series) }}
        />
      </section>
    </>
  );
}

function StorageCleanupPanel({
  cleanup,
  loading,
  onCleanup,
}: {
  cleanup?: RecordItem;
  loading: boolean;
  onCleanup: () => void;
}) {
  const preview = asArray(cleanup?.preview);
  return (
    <article className="panel cleanuppanel">
      <div className="panelhead">
        <div>
          <h2>Storage cleanup</h2>
          <p>Safely remove orphaned media files no longer referenced by videos, uploads, or renditions.</p>
        </div>
        <button
          type="button"
          className="danger"
          disabled={loading || !Number(cleanup?.orphanedFiles ?? 0)}
          onClick={onCleanup}
        >
          {loading ? "Working..." : "Clean orphaned files"}
        </button>
      </div>
      <div className="systemgrid cleanupgrid">
        <StatusStat
          label="Reclaimable"
          value={formatBytes(cleanup?.reclaimableBytes)}
          note={`${Number(cleanup?.orphanedFiles ?? 0)} orphaned files`}
        />
        <StatusStat
          label="Scanned files"
          value={String(cleanup?.totalFiles ?? 0)}
          note={`${Number(cleanup?.referencedFiles ?? 0)} referenced`}
        />
        <StatusStat
          label="Last deleted"
          value={formatBytes(cleanup?.deletedBytes)}
          note={`${Number(cleanup?.deletedFiles ?? 0)} files deleted`}
        />
        <StatusStat
          label="Failed"
          value={String(asArray(cleanup?.failed).length)}
          note="Files that could not be removed"
        />
      </div>
      {preview.length > 0 && (
        <div className="cleanupfiles">
          <b>Cleanup preview</b>
          {preview.map((file) => (
            <span key={String(file.key)}>
              {String(file.key)} · {formatBytes(file.sizeBytes)}
            </span>
          ))}
        </div>
      )}
      {!cleanup && <div className="formnote">Cleanup report is loading.</div>}
    </article>
  );
}

function StatusStat({ label, value, note }: { label: string; value: string; note: string }) {
  return (
    <div className="statuscard">
      <small>{label}</small>
      <strong>{value}</strong>
      <em>{note}</em>
    </div>
  );
}

function SystemStatusPanel({ system }: { system?: RecordItem }) {
  const memory = (system?.memory ?? {}) as RecordItem;
  const storage = (system?.storage ?? {}) as RecordItem;
  const cpu = (system?.cpu ?? {}) as RecordItem;
  const host = (system?.host ?? {}) as RecordItem;
  const network = (system?.network ?? {}) as RecordItem;
  const interfaces = asArray(network.interfaces);
  return (
    <article className="panel systempanel">
      <div className="panelhead">
        <div>
          <h2>System status</h2>
          <p>Live API server health, storage, and network snapshot.</p>
        </div>
        <div className="liveclock">
          <small>Live every second</small>
          <b>{system?.checkedAt ? formatDate(system.checkedAt) : "Waiting"}</b>
        </div>
      </div>
      <div className="systemgrid">
        <StatusGauge
          label="Memory"
          value={percent(memory.usedPercent)}
          detail={`${formatBytes(memory.usedBytes)} / ${formatBytes(memory.totalBytes)}`}
        />
        <StatusGauge
          label="Storage"
          value={percent(storage.usedPercent)}
          detail={`${formatBytes(storage.usedBytes)} / ${formatBytes(storage.totalBytes)}`}
          note={String(storage.path ?? "")}
        />
        <StatusGauge
          label="CPU load"
          value={cpuLoadPercent(cpu)}
          detail={`${Number(cpu.cores ?? 0)} cores - load ${formatLoad(cpu.loadAverage)}`}
        />
        <div className="statuscard">
          <small>Network</small>
          <strong>{Number(network.interfaceCount ?? interfaces.length)}</strong>
          <em>active interfaces</em>
          <div className="networklist">
            {interfaces.slice(0, 4).map((item, index) => (
              <span key={`${String(item.name)}-${index}`}>
                {String(item.name)} · {String(item.family)} · {String(item.address)}
              </span>
            ))}
          </div>
        </div>
      </div>
      <div className="systemmeta">
        <span>Host: {String(host.hostname ?? "unknown")}</span>
        <span>
          Platform: {String(host.platform ?? "")} {String(host.arch ?? "")}
        </span>
        <span>Uptime: {formatDuration(Number(host.uptimeSeconds ?? 0))}</span>
      </div>
    </article>
  );
}

function StatusGauge({ label, value, detail, note }: { label: string; value: number; detail: string; note?: string }) {
  const safe = Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div className="statuscard">
      <small>{label}</small>
      <strong>{safe}%</strong>
      <div className="statusbar">
        <i style={{ width: `${safe}%` }} />
      </div>
      <em>{detail}</em>
      {note && <span>{note}</span>}
    </div>
  );
}

function UploadsPanel({
  movies,
  uploading,
  uploadProgress,
  conversionProgress,
  uploadPhase,
  onUpload,
  onPublish,
  onPreview,
  onEdit,
  onDelete,
}: {
  movies: RecordItem[];
  uploading: boolean;
  uploadProgress: number | null;
  conversionProgress: number | null;
  uploadPhase: string;
  onUpload: (event: FormEvent<HTMLFormElement>) => void;
  onPublish: (id: string) => void;
  onPreview: (movie: RecordItem) => void;
  onEdit: (movie: RecordItem) => void;
  onDelete: (movie: RecordItem) => void;
}) {
  const converting = uploadPhase.startsWith("Converting preview");
  const visibleProgress = converting ? (conversionProgress ?? 0) : (uploadProgress ?? 0);
  return (
    <section className="grid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Upload videos</h2>
            <p>
              Use the device&apos;s native file picker. SecureStream does not rebuild the system dialog with custom
              controls.
            </p>
          </div>
          <b>Native picker</b>
        </div>
        <form onSubmit={onUpload}>
          <label>
            Title or batch prefix
            <input name="title" maxLength={160} placeholder="Optional. Blank uses each filename." />
          </label>
          <label>
            Synopsis
            <textarea name="synopsis" rows={4} />
          </label>
          <label>
            Maturity rating
            <input name="maturityRating" placeholder="PG-13" maxLength={20} />
          </label>
          <label className="nativepicker">
            <span>
              <b>Video files</b>
              <small>
                The system file browser opens with its own filename field, disclosure/expanded browser controls,
                sidebar, and format filter where the browser/OS supports it.
              </small>
            </span>
            <input name="file" type="file" accept="video/*,.mp4,.mov,.mkv,.webm,.avi,.wmv,.flv" multiple required />
          </label>
          <div className="formnote">
            Allowed formats are passed to the native picker through the browser file-type filter. Select one or many
            videos; each selected file becomes a separate draft.
          </div>
          {uploadProgress !== null && (
            <div className={`uploadprogress ${converting ? "processing" : ""}`}>
              <span style={{ width: `${visibleProgress}%` }} />
              <b>{converting ? `Converting preview... ${visibleProgress}%` : `Uploading ${visibleProgress}%`}</b>
            </div>
          )}
          <button type="submit" className="primary" disabled={uploading}>
            {uploading ? uploadPhase || "Working..." : "Upload selected videos"}
          </button>
        </form>
      </article>
      <MoviesPanel
        movies={movies}
        onPublish={onPublish}
        onPreview={onPreview}
        onEdit={onEdit}
        onDelete={onDelete}
        compact
      />
    </section>
  );
}

function MoviesPanel({
  movies,
  onPublish,
  onPreview,
  onEdit,
  onDelete,
  compact = false,
}: {
  movies: RecordItem[];
  onPublish: (id: string) => void;
  onPreview: (movie: RecordItem) => void;
  onEdit: (movie: RecordItem) => void;
  onDelete: (movie: RecordItem) => void;
  compact?: boolean;
}) {
  return (
    <article className="panel">
      <div className="panelhead">
        <div>
          <h2>{compact ? "Recent uploads" : "Videos"}</h2>
          <p>{movies.length} video records</p>
        </div>
      </div>
      <div className="rows">
        {movies.map((movie) => (
          <div className="row" key={String(movie.id)}>
            <div>
              <b>{String(movie.title ?? "Untitled")}</b>
              <small>
                {String(movie.status ?? "DRAFT")} - {count(movie.assets)} assets
              </small>
            </div>
            <div className="rowactions">
              <button type="button" disabled={!firstAsset(movie)} onClick={() => onPreview(movie)}>
                Check video
              </button>
              <button type="button" onClick={() => onEdit(movie)}>
                Edit
              </button>
              <button type="button" className="danger" onClick={() => onDelete(movie)}>
                Delete
              </button>
              <button type="button" disabled={movie.status === "PUBLISHED"} onClick={() => onPublish(String(movie.id))}>
                Publish
              </button>
            </div>
          </div>
        ))}
      </div>
    </article>
  );
}

function VideoEditorPanel({
  movies,
  jobs,
  loading,
  onSubmit,
}: {
  movies: RecordItem[];
  jobs: RecordItem[];
  loading: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const assets = movies.flatMap((movie) => asArray(movie.assets).map((asset) => ({ movie, asset })));
  return (
    <section className="grid editorgrid">
      <article className="panel upload editorpanel">
        <div className="panelhead">
          <div>
            <h2>Trim video</h2>
            <p>Cut a basic start/end clip and save it as a new draft video.</p>
          </div>
          <b>Safe edit</b>
        </div>
        <form onSubmit={onSubmit}>
          <label>
            Source video
            <select name="assetId" required>
              {assets.map(({ movie, asset }) => (
                <option key={String(asset.id)} value={String(asset.id)}>
                  {String(movie.title ?? "Untitled")} - {formatRuntime(asset.durationSeconds)} -{" "}
                  {String(asset.state ?? "UNKNOWN")}
                </option>
              ))}
            </select>
          </label>
          <label>
            New draft title
            <input name="title" placeholder="Leave blank to use original title + edited" maxLength={160} />
          </label>
          <div className="folderfields">
            <label>
              Start seconds
              <input name="startSeconds" type="number" min="0" step="0.1" defaultValue="0" required />
            </label>
            <label>
              End seconds
              <input name="endSeconds" type="number" min="0.1" step="0.1" placeholder="Example: 120" required />
            </label>
          </div>
          <small>This creates a new MP4 draft and does not overwrite the original upload.</small>
          <button type="submit" className="primary" disabled={loading || !assets.length}>
            {loading ? "Starting..." : "Start trim job"}
          </button>
        </form>
      </article>
      <article className="panel editorjobs">
        <div className="panelhead">
          <div>
            <h2>Editor jobs</h2>
            <p>{jobs.length} recent trim jobs</p>
          </div>
        </div>
        <div className="rows">
          {jobs.map((job) => (
            <div className="row editorjob" key={String(job.id)}>
              <div>
                <b>{String(job.title ?? "Edited draft")}</b>
                <small>
                  {String(job.phase ?? "Queued")} - {String(job.status ?? "QUEUED")}
                </small>
                {Boolean(job.error) && <small className="errorline">{String(job.error)}</small>}
              </div>
              <div>
                <strong>{percent(job.progress)}%</strong>
                <div className="statusbar">
                  <i style={{ width: `${percent(job.progress)}%` }} />
                </div>
                {Boolean(job.resultMovieId) && <small>Draft ready</small>}
              </div>
            </div>
          ))}
        </div>
        {!jobs.length && <div className="formnote">No editor jobs yet. Start by trimming one uploaded video.</div>}
      </article>
    </section>
  );
}

function FileManagerPanel({ files }: { files: RecordItem[] }) {
  const [query, setQuery] = useState("");
  const filtered = useMemo(
    () => files.filter((file) => fileSearchText(file).includes(query.trim().toLowerCase())),
    [files, query],
  );
  const totalBytes = filtered.reduce((sum, file) => sum + Number(file.sizeBytes ?? 0), 0);
  const formats = new Set(filtered.map((file) => String(file.format ?? "UNKNOWN")));
  return (
    <article className="panel filemanager">
      <div className="panelhead">
        <div>
          <h2>File manager</h2>
          <p>Track uploaded source files, previews, sizes, formats, and linked videos.</p>
        </div>
        <b>{filtered.length} files</b>
      </div>
      <div className="filesummary">
        <div>
          <small>Total size</small>
          <strong>{formatBytes(totalBytes)}</strong>
        </div>
        <div>
          <small>Formats</small>
          <strong>{formats.size}</strong>
        </div>
        <div>
          <small>Ready assets</small>
          <strong>{filtered.filter((file) => file.state === "READY").length}</strong>
        </div>
      </div>
      <label className="filesearch">
        Search files
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search by name, format, status, or video title"
        />
      </label>
      <div className="tablewrap">
        <table>
          <thead>
            <tr>
              <th>File</th>
              <th>Video</th>
              <th>Format</th>
              <th>Size</th>
              <th>Status</th>
              <th>Duration</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((file) => (
              <tr key={String(file.id)}>
                <td>
                  <b>{String(file.sourceFileName ?? fileNameFromKey(file.sourceStorageKey))}</b>
                  <small>{String(file.sourceStorageKey ?? "")}</small>
                  {Boolean(file.previewFileName) && <small>Preview: {String(file.previewFileName)}</small>}
                </td>
                <td>
                  {String(file.title ?? "Unattached")}
                  <small>
                    {String(file.ownerType ?? "Asset")} {file.ownerStatus ? `- ${String(file.ownerStatus)}` : ""}
                  </small>
                </td>
                <td>
                  <span className="filepill">{String(file.format ?? "UNKNOWN")}</span>
                  {Boolean(file.previewFormat) && <small>Preview {String(file.previewFormat)}</small>}
                </td>
                <td>{formatBytes(file.sizeBytes ?? file.expectedBytes)}</td>
                <td>
                  {String(file.state ?? "UNKNOWN")}
                  <small>
                    {file.uploadStatus ? `Upload ${String(file.uploadStatus).toLowerCase()}` : "No upload row"}
                  </small>
                  {file.failureReason ? <small className="danger-text">{String(file.failureReason)}</small> : null}
                </td>
                <td>{formatRuntime(file.durationSeconds)}</td>
                <td>{formatDateTime(file.updatedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!filtered.length && (
        <div className="formnote">No files found. Upload a video first or clear the search box.</div>
      )}
    </article>
  );
}

function PreviewModal({
  preview,
  onClose,
}: {
  preview: { title: string; url: string; playable: boolean; format: string };
  onClose: () => void;
}) {
  const [error, setError] = useState("");
  const [videoUrl, setVideoUrl] = useState("");
  useEffect(() => {
    setError("");
    setVideoUrl("");
    let objectUrl = "";
    fetch(preview.url, { credentials: "include", headers: authHeaders() })
      .then(async (response) => {
        if (!response.ok) throw new Error("Preview could not be loaded.");
        const blob = await response.blob();
        objectUrl = URL.createObjectURL(blob);
        setVideoUrl(objectUrl);
      })
      .catch((caught) => setError(caught instanceof Error ? caught.message : "Preview could not be loaded."));
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [preview.url]);
  return (
    <article className="panel preview">
      <div className="panelhead">
        <div>
          <h2>Check video before publishing</h2>
          <p>{preview.title}</p>
        </div>
        <button type="button" onClick={onClose}>
          Close
        </button>
      </div>
      {!preview.playable && (
        <div className="formnote">
          A browser MP4 preview was not created for this file yet. Re-upload after redeploying the API with FFmpeg
          enabled, or use Download source to check it locally.
        </div>
      )}
      {!videoUrl && !error && <div className="formnote">Loading preview...</div>}
      {videoUrl && (
        <video
          src={videoUrl}
          controls
          preload="metadata"
          onError={() =>
            setError(
              "The browser could not play this preview. Re-upload the video after redeploying the API so it can create a browser-compatible MP4 preview. Also make sure API storage is persistent at /data/media.",
            )
          }
        />
      )}
      {error && <div className="formerror">{error}</div>}
      <a className="download" href={preview.url}>
        Download source/preview
      </a>
    </article>
  );
}

function EditMoviePanel({
  movie,
  loading,
  onCancel,
  onSubmit,
}: {
  movie: RecordItem;
  loading: boolean;
  onCancel: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <article className="panel upload editpanel">
      <div className="panelhead">
        <div>
          <h2>Edit video</h2>
          <p>{String(movie.title ?? "Untitled")}</p>
        </div>
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
      <form onSubmit={onSubmit}>
        <label>
          Title
          <input name="title" required maxLength={160} defaultValue={String(movie.title ?? "")} />
        </label>
        <label>
          Synopsis
          <textarea name="synopsis" rows={4} required defaultValue={String(movie.synopsis ?? "")} />
        </label>
        <label>
          Maturity rating
          <input name="maturityRating" maxLength={20} defaultValue={String(movie.maturityRating ?? "")} />
        </label>
        <label>
          Status
          <select name="status" defaultValue={String(movie.status ?? "DRAFT")}>
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="UNPUBLISHED">Unpublished</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </label>
        <label className="toggle">
          <input name="featured" type="checkbox" defaultChecked={Boolean(movie.featured)} /> Feature this video in the
          app
        </label>
        <label>
          Poster photo URL
          <input name="posterUrl" type="url" placeholder="https://..." defaultValue={String(movie.posterUrl ?? "")} />
        </label>
        <label>
          Featured backdrop photo URL
          <input
            name="backdropUrl"
            type="url"
            placeholder="https://..."
            defaultValue={String(movie.backdropUrl ?? "")}
          />
        </label>
        <label>
          Trailer / featured video URL
          <input name="trailerUrl" type="url" placeholder="https://..." defaultValue={String(movie.trailerUrl ?? "")} />
        </label>
        <button type="submit" className="primary" disabled={loading}>
          {loading ? "Saving..." : "Save changes"}
        </button>
      </form>
    </article>
  );
}

function CatalogPanel({
  collections,
  movies,
  loading,
  onCreateCollection,
  onUpdateCollection,
  onDeleteCollection,
  onPreview,
  onEdit,
  onDelete,
}: {
  collections: RecordItem[];
  movies: RecordItem[];
  loading: boolean;
  onCreateCollection: (event: FormEvent<HTMLFormElement>) => void;
  onUpdateCollection: (collection: RecordItem, event: FormEvent<HTMLFormElement>) => void;
  onDeleteCollection: (collection: RecordItem) => void;
  onPreview: (movie: RecordItem) => void;
  onEdit: (movie: RecordItem) => void;
  onDelete: (movie: RecordItem) => void;
}) {
  return (
    <section className="grid">
      <CollectionsPanel
        collections={collections}
        movies={movies}
        loading={loading}
        onCreate={onCreateCollection}
        onUpdate={onUpdateCollection}
        onDelete={onDeleteCollection}
      />
      <MoviesPanel
        movies={movies}
        onPublish={() => undefined}
        onPreview={onPreview}
        onEdit={onEdit}
        onDelete={onDelete}
        compact
      />
    </section>
  );
}

function CollectionsPanel({
  collections,
  movies,
  loading,
  onCreate,
  onUpdate,
  onDelete,
}: {
  collections: RecordItem[];
  movies: RecordItem[];
  loading: boolean;
  onCreate: (event: FormEvent<HTMLFormElement>) => void;
  onUpdate: (collection: RecordItem, event: FormEvent<HTMLFormElement>) => void;
  onDelete: (collection: RecordItem) => void;
}) {
  return (
    <article className="panel folders">
      <div className="panelhead">
        <div>
          <h2>Folders / Collections</h2>
          <p>Create Android home sections and organize published videos.</p>
        </div>
        <b>{collections.length} folders</b>
      </div>
      <form className="folderform" onSubmit={onCreate}>
        <label>
          New folder name
          <input name="name" placeholder="Action, Kids, Drama..." required maxLength={120} />
        </label>
        <label>
          Sort order
          <input name="sortOrder" type="number" defaultValue={collections.length + 1} />
        </label>
        <label>
          Parent folder
          <select name="parentId" defaultValue="">
            <option value="">Top level</option>
            {collections.map((collection) => (
              <option key={String(collection.id)} value={String(collection.id)}>
                {folderLabel(collection)}
              </option>
            ))}
          </select>
        </label>
        <label className="toggle">
          <input name="published" type="checkbox" defaultChecked /> Show in Android app
        </label>
        <MovieChecklist movies={movies} />
        <button type="submit" className="primary" disabled={loading}>
          Create folder
        </button>
      </form>
      <div className="folderlist">
        {collections.map((collection) => (
          <CollectionEditor
            key={String(collection.id)}
            collection={collection}
            collections={collections}
            movies={movies}
            loading={loading}
            onUpdate={onUpdate}
            onDelete={onDelete}
          />
        ))}
      </div>
    </article>
  );
}

function CollectionEditor({
  collection,
  collections,
  movies,
  loading,
  onUpdate,
  onDelete,
}: {
  collection: RecordItem;
  collections: RecordItem[];
  movies: RecordItem[];
  loading: boolean;
  onUpdate: (collection: RecordItem, event: FormEvent<HTMLFormElement>) => void;
  onDelete: (collection: RecordItem) => void;
}) {
  const selectedIds = collectionMovieIds(collection);
  return (
    <form className="foldercard" onSubmit={(event) => onUpdate(collection, event)}>
      <div className="panelhead">
        <div>
          <h3>{String(collection.name ?? "Untitled folder")}</h3>
          <p>
            {String(collection.slug ?? "")} - {collection.published ? "Visible in Android" : "Draft/hidden"} -{" "}
            {count(collection.items)} videos
          </p>
        </div>
        <div className="rowactions">
          <button type="button" className="danger" disabled={loading} onClick={() => onDelete(collection)}>
            Delete
          </button>
          <button type="submit" disabled={loading}>
            Save folder
          </button>
        </div>
      </div>
      <div className="folderfields">
        <label>
          Name
          <input name="name" required maxLength={120} defaultValue={String(collection.name ?? "")} />
        </label>
        <label>
          Sort
          <input name="sortOrder" type="number" defaultValue={Number(collection.sortOrder ?? 0)} />
        </label>
        <label>
          Parent
          <select name="parentId" defaultValue={String(collection.parentId ?? "")}>
            <option value="">Top level</option>
            {collections
              .filter((candidate) => candidate.id !== collection.id)
              .map((candidate) => (
                <option key={String(candidate.id)} value={String(candidate.id)}>
                  {folderLabel(candidate)}
                </option>
              ))}
          </select>
        </label>
        <label className="toggle">
          <input name="published" type="checkbox" defaultChecked={Boolean(collection.published)} /> Show in Android app
        </label>
      </div>
      <MovieChecklist movies={movies} selectedIds={selectedIds} />
    </form>
  );
}

function MovieChecklist({ movies, selectedIds = [] }: { movies: RecordItem[]; selectedIds?: string[] }) {
  const published = useMemo(() => movies.filter((movie) => movie.status === "PUBLISHED"), [movies]);
  const movieIds = useMemo(() => new Set(published.map((movie) => String(movie.id))), [published]);
  const [checkedIds, setCheckedIds] = useState<string[]>([]);
  const [draggingId, setDraggingId] = useState<string | null>(null);

  useEffect(() => {
    setCheckedIds(selectedIds.filter((id) => movieIds.has(id)));
  }, [movies, movieIds, selectedIds.join("|")]);

  const checkedSet = new Set(checkedIds);
  const availableMovies = published.filter((movie) => !checkedSet.has(String(movie.id)));
  const movieById = new Map(published.map((movie) => [String(movie.id), movie]));

  function toggleMovie(id: string, checked: boolean) {
    setCheckedIds(checked ? [...checkedIds, id] : checkedIds.filter((movieId) => movieId !== id));
  }

  function moveDragged(event: DragEvent<HTMLDivElement>, targetId: string) {
    event.preventDefault();
    if (!draggingId || draggingId === targetId) return;
    const from = checkedIds.indexOf(draggingId);
    const to = checkedIds.indexOf(targetId);
    if (from < 0 || to < 0) return;
    const next = [...checkedIds];
    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);
    setCheckedIds(next);
  }

  if (!published.length) return <div className="formnote">Publish videos first, then add them to folders.</div>;
  return (
    <div className="foldervideos">
      <input name="movieIds" value="" readOnly hidden />
      <div className="draglist">
        <b>Selected videos - drag to arrange</b>
        {checkedIds.length ? (
          checkedIds.map((id, index) => {
            const movie = movieById.get(id);
            return (
              <div
                className={`dragrow${draggingId === id ? " dragging" : ""}`}
                key={id}
                draggable
                onDragStart={() => setDraggingId(id)}
                onDragEnd={() => setDraggingId(null)}
                onDragOver={(event) => moveDragged(event, id)}
              >
                <span className="draghandle" aria-hidden="true">
                  ☰
                </span>
                <span>
                  {index + 1}. {String(movie?.title ?? "Untitled")}
                </span>
                <button type="button" onClick={() => toggleMovie(id, false)}>
                  Remove
                </button>
                <input name="movieIds" value={id} readOnly hidden />
              </div>
            );
          })
        ) : (
          <div className="formnote">Choose videos below, then drag them here into the exact order you want.</div>
        )}
      </div>
      <div className="checkgrid">
        {availableMovies.map((movie) => {
          const id = String(movie.id);
          return (
            <label key={id}>
              <input
                type="checkbox"
                checked={false}
                onChange={(event) => toggleMovie(id, event.currentTarget.checked)}
              />{" "}
              <span>{String(movie.title ?? "Untitled")}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}

function UsersPanel({
  users,
  movies,
  collections,
  loading,
  onCreate,
  onUpdate,
  onDelete,
}: {
  users: RecordItem[];
  movies: RecordItem[];
  collections: RecordItem[];
  loading: boolean;
  onCreate: (event: FormEvent<HTMLFormElement>) => void;
  onUpdate: (user: RecordItem, event: FormEvent<HTMLFormElement>) => void;
  onDelete: (user: RecordItem) => void;
}) {
  return (
    <section className="grid usersgrid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Create user</h2>
            <p>Add viewer/operator accounts and choose their allowed videos.</p>
          </div>
          <b>{users.length} users</b>
        </div>
        <form onSubmit={onCreate}>
          <label>
            Email
            <input name="email" type="email" required />
          </label>
          <label>
            Display name
            <input name="displayName" required minLength={2} maxLength={80} />
          </label>
          <label>
            Password
            <input name="password" type="password" required minLength={12} />
          </label>
          <div className="folderfields">
            <label>
              Role
              <select name="role" defaultValue="VIEWER">
                <option value="VIEWER">Viewer</option>
                <option value="EDITOR">Editor</option>
                <option value="ADMIN">Admin</option>
                <option value="SUPER_ADMIN">Super admin</option>
              </select>
            </label>
            <label>
              Status
              <select name="status" defaultValue="ACTIVE">
                <option value="ACTIVE">Active</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="DISABLED">Disabled</option>
              </select>
            </label>
          </div>
          <label>
            Default folder
            <select name="defaultCollectionId" defaultValue="">
              <option value="">No default folder</option>
              {collections.map((collection) => (
                <option key={String(collection.id)} value={String(collection.id)}>
                  {folderLabel(collection)}
                </option>
              ))}
            </select>
          </label>
          <label className="toggle">
            <input name="accessRestricted" type="checkbox" defaultChecked /> Restrict this user to selected
            folders/videos
          </label>
          <AccessPicker movies={movies} collections={collections} />
          <button type="submit" className="primary" disabled={loading}>
            Create user
          </button>
        </form>
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Manage users</h2>
            <p>Rules apply to catalog, search, Play Now, and offline downloads.</p>
          </div>
        </div>
        <div className="userlist">
          {users.map((user) => (
            <UserEditor
              key={String(user.id)}
              user={user}
              movies={movies}
              collections={collections}
              loading={loading}
              onUpdate={onUpdate}
              onDelete={onDelete}
            />
          ))}
        </div>
      </article>
    </section>
  );
}

function ApiTokensPanel({
  tokens,
  newToken,
  loading,
  onCreate,
  onRevoke,
}: {
  tokens: RecordItem[];
  newToken: string;
  loading: boolean;
  onCreate: (event: FormEvent<HTMLFormElement>) => void;
  onRevoke: (token: RecordItem) => void;
}) {
  return (
    <section className="grid usersgrid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Create API token</h2>
            <p>Use this for n8n automations like creating users.</p>
          </div>
          <b>Bearer</b>
        </div>
        <form onSubmit={onCreate}>
          <label>
            Token name
            <input name="name" required minLength={2} maxLength={80} placeholder="n8n user creation workflow" />
          </label>
          <button type="submit" className="primary" disabled={loading}>
            {loading ? "Creating..." : "Create token"}
          </button>
        </form>
        {newToken && (
          <div className="tokenbox">
            <b>Copy this token now</b>
            <code>{newToken}</code>
            <small>It is shown once only. Store it in n8n credentials as a Bearer token.</small>
          </div>
        )}
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Automation tokens</h2>
            <p>{tokens.length} tokens created by your admin account.</p>
          </div>
        </div>
        <div className="userlist">
          {tokens.map((token) => (
            <div className="foldercard usercard" key={String(token.id)}>
              <div className="panelhead">
                <div>
                  <h3>{String(token.name ?? "API token")}</h3>
                  <p>
                    Created {formatDateTime(token.createdAt)}{" "}
                    {token.lastUsedAt ? `- last used ${formatDateTime(token.lastUsedAt)}` : "- never used"}
                  </p>
                  {Boolean(token.revokedAt) && <p>Revoked {formatDateTime(token.revokedAt)}</p>}
                </div>
                <div className="rowactions">
                  <button
                    type="button"
                    className="danger"
                    disabled={loading || Boolean(token.revokedAt)}
                    onClick={() => onRevoke(token)}
                  >
                    Revoke
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </article>
      <article className="panel userspanel api-docs">
        <div className="panelhead">
          <div>
            <h2>How to use this token in n8n</h2>
            <p>
              Use the HTTP Request node. Store the token as a credential or environment variable, not directly inside
              many workflow nodes.
            </p>
          </div>
        </div>
        <div className="docgrid">
          <div>
            <b>1. Auth header</b>
            <p>Every automation request must include this header.</p>
            <pre>{`Authorization: Bearer YOUR_TOKEN
Content-Type: application/json`}</pre>
          </div>
          <div>
            <b>2. Base URL</b>
            <p>Use your API URL, not the admin UI URL.</p>
            <pre>{API}</pre>
          </div>
          <div>
            <b>3. Token safety</b>
            <p>If a token leaks, revoke it here and create a new one. Tokens are shown once only.</p>
          </div>
        </div>
      </article>
      <article className="panel userspanel api-docs">
        <div className="panelhead">
          <div>
            <h2>Find folder and video IDs</h2>
            <p>Use these before creating a user so n8n can fill default folder and access rules.</p>
          </div>
        </div>
        <pre>{`GET ${API}/admin/collections
Authorization: Bearer YOUR_TOKEN

Response example:
[
  {
    "id": "folder-uuid",
    "name": "Comedy",
    "slug": "comedy",
    "published": true
  }
]

GET ${API}/admin/movies
Authorization: Bearer YOUR_TOKEN

Response example:
[
  {
    "id": "movie-uuid",
    "title": "My Video",
    "status": "PUBLISHED"
  }
]`}</pre>
      </article>
      <article className="panel userspanel api-docs">
        <div className="panelhead">
          <div>
            <h2>Create user with folder access</h2>
            <p>This is the recommended n8n request body for viewer accounts.</p>
          </div>
        </div>
        <pre>{`POST ${API}/admin/users
Authorization: Bearer YOUR_TOKEN
Content-Type: application/json

{
  "email": "viewer@example.com",
  "displayName": "Viewer Name",
  "password": "make-this-12-plus-chars",
  "role": "VIEWER",
  "status": "ACTIVE",
  "accessRestricted": true,
  "defaultCollectionId": "folder-uuid-or-null",
  "access": {
    "collectionIds": ["folder-uuid"],
    "movieIds": []
  }
}`}</pre>
      </article>
      <article className="panel userspanel api-docs">
        <div className="panelhead">
          <div>
            <h2>Common request bodies</h2>
            <p>Copy the pattern that matches your automation.</p>
          </div>
        </div>
        <pre>{`// User can see only one folder, and that folder opens by default
{
  "email": "viewer@example.com",
  "displayName": "Viewer Name",
  "password": "make-this-12-plus-chars",
  "role": "VIEWER",
  "status": "ACTIVE",
  "accessRestricted": true,
  "defaultCollectionId": "folder-uuid",
  "access": {
    "collectionIds": ["folder-uuid"],
    "movieIds": []
  }
}

// User can see selected folder plus one direct video
{
  "email": "viewer@example.com",
  "displayName": "Viewer Name",
  "password": "make-this-12-plus-chars",
  "role": "VIEWER",
  "status": "ACTIVE",
  "accessRestricted": true,
  "defaultCollectionId": "folder-uuid",
  "access": {
    "collectionIds": ["folder-uuid"],
    "movieIds": ["movie-uuid"]
  }
}

// User can see the full published catalog, no strict rule
{
  "email": "viewer@example.com",
  "displayName": "Viewer Name",
  "password": "make-this-12-plus-chars",
  "role": "VIEWER",
  "status": "ACTIVE",
  "accessRestricted": false,
  "defaultCollectionId": null,
  "access": {
    "collectionIds": [],
    "movieIds": []
  }
}`}</pre>
      </article>
      <article className="panel userspanel api-docs">
        <div className="panelhead">
          <div>
            <h2>n8n workflow recipe</h2>
            <p>A simple automation sequence for creating users from a form, sheet, or webhook.</p>
          </div>
        </div>
        <ol>
          <li>Webhook/Form Trigger receives email, name, password, and folder name.</li>
          <li>
            HTTP Request: <code>GET /admin/collections</code>.
          </li>
          <li>
            Filter/Code node: find the folder where <code>name</code> matches your requested folder.
          </li>
          <li>
            HTTP Request: <code>POST /admin/users</code> using that folder&apos;s <code>id</code>.
          </li>
          <li>Optional: send the login details to the user through email or chat.</li>
        </ol>
        <pre>{`// n8n expression idea after GET /admin/collections
{{ $json.find(folder => folder.name === "Comedy").id }}`}</pre>
      </article>
      <article className="panel userspanel api-docs">
        <div className="panelhead">
          <div>
            <h2>Troubleshooting</h2>
            <p>Most automation problems are one of these.</p>
          </div>
        </div>
        <div className="docgrid">
          <div>
            <b>401 token invalid</b>
            <p>
              Create a new token. Check the header starts with <code>Bearer ss_pat_</code>.
            </p>
          </div>
          <div>
            <b>403 forbidden</b>
            <p>The token owner must be an active admin/operator with user-management permission.</p>
          </div>
          <div>
            <b>400 validation error</b>
            <p>Password must be at least 12 characters. Folder/movie IDs must be real UUIDs.</p>
          </div>
          <div>
            <b>User created but folder not visible</b>
            <p>
              Make sure <code>accessRestricted</code> is true and the folder ID is in <code>access.collectionIds</code>.
              The API also auto-adds the default folder to allowed folders.
            </p>
          </div>
        </div>
      </article>
    </section>
  );
}

function UserEditor({
  user,
  movies,
  collections,
  loading,
  onUpdate,
  onDelete,
}: {
  user: RecordItem;
  movies: RecordItem[];
  collections: RecordItem[];
  loading: boolean;
  onUpdate: (user: RecordItem, event: FormEvent<HTMLFormElement>) => void;
  onDelete: (user: RecordItem) => void;
}) {
  return (
    <form className="foldercard usercard" onSubmit={(event) => onUpdate(user, event)}>
      <div className="panelhead">
        <div>
          <h3>{String(user.email ?? "User")}</h3>
          <p>
            {String(user.displayName ?? "")} - {String(user.role ?? "")} - {String(user.status ?? "")}
          </p>
        </div>
        <div className="rowactions">
          <button type="button" className="danger" disabled={loading} onClick={() => onDelete(user)}>
            Delete
          </button>
          <button type="submit" disabled={loading}>
            Save user
          </button>
        </div>
      </div>
      <div className="folderfields">
        <label>
          Email
          <input name="email" type="email" required defaultValue={String(user.email ?? "")} />
        </label>
        <label>
          Name
          <input
            name="displayName"
            required
            minLength={2}
            maxLength={80}
            defaultValue={String(user.displayName ?? "")}
          />
        </label>
        <label>
          Role
          <select name="role" defaultValue={String(user.role ?? "VIEWER")}>
            <option value="VIEWER">Viewer</option>
            <option value="EDITOR">Editor</option>
            <option value="ADMIN">Admin</option>
            <option value="SUPER_ADMIN">Super admin</option>
          </select>
        </label>
        <label>
          Status
          <select name="status" defaultValue={String(user.status ?? "ACTIVE")}>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </label>
      </div>
      <label>
        New password (leave blank to keep current)
        <input name="password" type="password" minLength={12} placeholder="Optional" />
      </label>
      <label>
        Default folder
        <select name="defaultCollectionId" defaultValue={String(user.defaultCollectionId ?? "")}>
          <option value="">No default folder</option>
          {collections.map((collection) => (
            <option key={String(collection.id)} value={String(collection.id)}>
              {folderLabel(collection)}
            </option>
          ))}
        </select>
      </label>
      <label className="toggle">
        <input name="accessRestricted" type="checkbox" defaultChecked={Boolean(user.accessRestricted)} /> Restrict this
        user to selected folders/videos
      </label>
      <AccessPicker
        movies={movies}
        collections={collections}
        selectedMovieIds={userMovieAccessIds(user)}
        selectedCollectionIds={userCollectionAccessIds(user)}
      />
    </form>
  );
}

function AccessPicker({
  movies,
  collections,
  selectedMovieIds = [],
  selectedCollectionIds = [],
}: {
  movies: RecordItem[];
  collections: RecordItem[];
  selectedMovieIds?: string[];
  selectedCollectionIds?: string[];
}) {
  const published = movies.filter((movie) => movie.status === "PUBLISHED");
  return (
    <div className="accesspicker">
      <div>
        <b>Allowed folders</b>
        <p>When restriction is enabled, only these folders and selected videos are visible.</p>
        <div className="checkgrid">
          {collections.map((collection) => (
            <label key={String(collection.id)}>
              <input
                name="collectionIds"
                type="checkbox"
                value={String(collection.id)}
                defaultChecked={selectedCollectionIds.includes(String(collection.id))}
              />{" "}
              <span>{folderLabel(collection)}</span>
            </label>
          ))}
        </div>
      </div>
      <div>
        <b>Allowed videos</b>
        <p>Direct video access can be combined with folder access.</p>
        <div className="checkgrid">
          {published.map((movie) => (
            <label key={String(movie.id)}>
              <input
                name="movieIds"
                type="checkbox"
                value={String(movie.id)}
                defaultChecked={selectedMovieIds.includes(String(movie.id))}
              />{" "}
              <span>{String(movie.title ?? "Untitled")}</span>
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}

function MessagesPanel({
  conversations,
  users,
  loading,
  onStart,
  onReply,
}: {
  conversations: RecordItem[];
  users: RecordItem[];
  loading: boolean;
  onStart: (event: FormEvent<HTMLFormElement>) => void;
  onReply: (conversation: RecordItem, event: FormEvent<HTMLFormElement>) => void;
}) {
  const viewers = users.filter((user) => user.role === "VIEWER" && user.status === "ACTIVE");
  return (
    <section className="grid usersgrid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Start chat</h2>
            <p>Message a viewer first, even before they contact you.</p>
          </div>
          <b>{viewers.length} viewers</b>
        </div>
        <form onSubmit={onStart}>
          <label>
            User
            <select name="userId" required defaultValue="">
              <option value="">Choose user</option>
              {viewers.map((user) => (
                <option key={String(user.id)} value={String(user.id)}>
                  {String(user.email ?? user.displayName)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Message
            <textarea name="body" rows={4} required maxLength={5000} placeholder="Type your first message..." />
          </label>
          <button type="submit" className="primary" disabled={loading}>
            Start chat
          </button>
        </form>
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>User messages</h2>
            <p>Full chat thread. Auto-refreshes every few seconds while this panel is open.</p>
          </div>
          <b>{conversations.length} chats</b>
        </div>
        <div className="userlist">
          {conversations.map((conversation) => {
            const user = conversation.user as RecordItem | undefined;
            const last = conversation.lastMessage as RecordItem | undefined;
            const sender = last?.sender as RecordItem | undefined;
            const messages = asArray(conversation.messages);
            return (
              <form
                className="foldercard"
                key={String(conversation.id)}
                onSubmit={(event) => onReply(conversation, event)}
              >
                <div className="panelhead">
                  <div>
                    <h3>{String(user?.displayName ?? "Viewer")}</h3>
                    <p>
                      {String(user?.email ?? "")} - {Number(conversation.unreadCount ?? 0)} unread
                    </p>
                  </div>
                  <small>{formatDateTime(conversation.updatedAt)}</small>
                </div>
                <div className="chatthread">
                  {(messages.length ? messages : last ? [last] : []).map((message, index) => {
                    const messageSender = message.sender as RecordItem | undefined;
                    const fromAdmin =
                      String(messageSender?.role ?? "").includes("ADMIN") ||
                      String(messageSender?.role ?? "").includes("EDITOR");
                    return (
                      <div
                        className={`chatbubble ${fromAdmin ? "adminbubble" : "userbubble"}`}
                        key={String(message.id ?? index)}
                      >
                        <small>
                          {fromAdmin ? "Admin" : String(messageSender?.displayName ?? "User")} -{" "}
                          {formatDateTime(message.createdAt)}
                        </small>
                        <span>{String(message.body ?? "")}</span>
                      </div>
                    );
                  })}
                  {!messages.length && !last && <div className="formnote">No messages yet.</div>}
                </div>
                <label>
                  Reply
                  <textarea name="body" rows={3} required maxLength={5000} placeholder="Type your reply..." />
                </label>
                <button type="submit" className="primary" disabled={loading}>
                  Send reply
                </button>
              </form>
            );
          })}
        </div>
        {!conversations.length && <div className="formnote">No user conversations yet. Start one from the form.</div>}
      </article>
    </section>
  );
}

function NotificationsPanel({
  notifications,
  users,
  loading,
  onSend,
}: {
  notifications: RecordItem[];
  users: RecordItem[];
  loading: boolean;
  onSend: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const viewers = users.filter((user) => user.role === "VIEWER" && user.status === "ACTIVE");
  return (
    <section className="grid usersgrid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Send update</h2>
            <p>Broadcast release notes, maintenance, or content updates to users.</p>
          </div>
          <b>{viewers.length} viewers</b>
        </div>
        <form onSubmit={onSend}>
          <label>
            Title
            <input name="title" required maxLength={120} placeholder="New videos added" />
          </label>
          <label>
            Message
            <textarea name="body" rows={4} required maxLength={2000} placeholder="Tell users what changed..." />
          </label>
          <label className="toggle">
            <input name="allUsers" type="checkbox" /> Send to all active viewers
          </label>
          <div className="accesspicker">
            <div>
              <b>Or choose specific users</b>
              <p>If “all active viewers” is unchecked, selected users will receive it.</p>
              <div className="checkgrid">
                {viewers.map((user) => (
                  <label key={String(user.id)}>
                    <input name="userIds" type="checkbox" value={String(user.id)} />{" "}
                    <span>{String(user.email ?? user.displayName)}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>
          <button type="submit" className="primary" disabled={loading}>
            Send notification
          </button>
        </form>
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Recent notifications</h2>
            <p>{notifications.length} sent updates</p>
          </div>
        </div>
        <div className="rows">
          {notifications.map((notification) => (
            <div className="row" key={String(notification.id)}>
              <div>
                <b>{String(notification.title ?? "Update")}</b>
                <small>
                  {String((notification.user as RecordItem | undefined)?.email ?? notification.userId)} -{" "}
                  {formatDateTime(notification.createdAt)}
                </small>
                <small>{String(notification.body ?? "")}</small>
              </div>
              <em>{notification.readAt ? "Read" : "Unread"}</em>
            </div>
          ))}
        </div>
      </article>
    </section>
  );
}

function SeriesPanel({ series }: { series: RecordItem[] }) {
  return (
    <article className="panel">
      <div className="panelhead">
        <div>
          <h2>Series</h2>
          <p>{series.length} series records</p>
        </div>
      </div>
      <div className="rows">
        {series.map((row) => (
          <div className="row" key={String(row.id)}>
            <div>
              <b>{String(row.title ?? "Untitled")}</b>
              <small>
                {String(row.status ?? "DRAFT")} - {count(row.seasons)} seasons
              </small>
            </div>
          </div>
        ))}
      </div>
    </article>
  );
}

function PlaybackPanel({ rows }: { rows: RecordItem[] }) {
  return (
    <article className="panel">
      <div className="panelhead">
        <div>
          <h2>Playback sessions</h2>
          <p>{rows.length} recent sessions</p>
        </div>
      </div>
      <div className="rows">
        {rows.map((row) => (
          <div className="row" key={String(row.id)}>
            <div>
              <b>{String((row.user as RecordItem | undefined)?.email ?? row.userId)}</b>
              <small>
                {String(row.deviceId ?? "")} - expires {format(row.expiresAt)}
              </small>
              <small>Watermark: {watermarkForSession(row)}</small>
            </div>
            <em>{row.revokedAt ? "Revoked" : "Active"}</em>
          </div>
        ))}
      </div>
    </article>
  );
}

function WatermarkTracePanel({ rows }: { rows: RecordItem[] }) {
  const [query, setQuery] = useState("");
  const parsed = parseWatermark(query);
  const matches = parsed
    ? rows.filter(
        (row) =>
          String(row.userId ?? "")
            .toLowerCase()
            .startsWith(parsed.userPrefix) &&
          String(row.id ?? "")
            .toLowerCase()
            .endsWith(parsed.sessionSuffix),
      )
    : [];
  return (
    <section className="grid tracegrid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Watermark Trace</h2>
            <p>Paste the watermark shown on a leaked/screen-recorded video.</p>
          </div>
          <b>
            {matches.length} match{matches.length === 1 ? "" : "es"}
          </b>
        </div>
        <label>
          Watermark code
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Example: a91f..7b2c" />
        </label>
        <div className="formnote">
          Format is <b>first 4 of user ID</b> + <b>..</b> + <b>last 4 of playback session ID</b>. Recent sessions are
          searched automatically.
        </div>
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Trace results</h2>
            <p>
              {parsed
                ? `Looking for user ${parsed.userPrefix} and session ending ${parsed.sessionSuffix}`
                : "Enter a watermark to start."}
            </p>
          </div>
        </div>
        <div className="userlist">
          {matches.map((row) => {
            const user = row.user as RecordItem | undefined;
            const asset = row.videoAsset as RecordItem | undefined;
            return (
              <div className="foldercard tracecard" key={String(row.id)}>
                <div className="panelhead">
                  <div>
                    <h3>{String(user?.email ?? row.userId)}</h3>
                    <p>
                      {String(user?.displayName ?? "Viewer")} - {sessionVideoTitle(asset)}
                    </p>
                  </div>
                  <em>{row.revokedAt ? "Revoked" : "Active"}</em>
                </div>
                <div className="tracefacts">
                  <span>
                    <b>Watermark</b>
                    {watermarkForSession(row)}
                  </span>
                  <span>
                    <b>User ID</b>
                    {String(row.userId ?? "")}
                  </span>
                  <span>
                    <b>Session ID</b>
                    {String(row.id ?? "")}
                  </span>
                  <span>
                    <b>Device</b>
                    {String(row.deviceId ?? "Unknown")}
                  </span>
                  <span>
                    <b>Created</b>
                    {formatDateTime(row.createdAt)}
                  </span>
                  <span>
                    <b>Expires</b>
                    {formatDateTime(row.expiresAt)}
                  </span>
                </div>
              </div>
            );
          })}
          {parsed && !matches.length && (
            <div className="formnote">
              No matching recent playback session found. The session may be older than the current recent-session list.
            </div>
          )}
        </div>
      </article>
    </section>
  );
}

function BackupPanel({
  backups,
  loading,
  onCreate,
  onRestore,
  onDelete,
  onDriveUpload,
  onScheduledNow,
}: {
  backups?: RecordItem;
  loading: boolean;
  onCreate: () => void;
  onRestore: (event: FormEvent<HTMLFormElement>) => void;
  onDelete: (backup: RecordItem) => void;
  onDriveUpload: (backup: RecordItem) => void;
  onScheduledNow: () => void;
}) {
  const items = asArray(backups?.items);
  const driveReady = Boolean(backups?.googleDriveConfigured);
  return (
    <section className="grid backupgrid">
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Create portable backup</h2>
            <p>Includes users, folders, catalog, access rules, chats, notifications, watch data, and media files.</p>
          </div>
          <b>{items.length} backups</b>
        </div>
        <div className="formnote">
          Backup files are URL/port safe. Media is stored by relative storage path, so it can restore on another server
          using a different domain.
        </div>
        <div className="formnote">
          Google Drive:{" "}
          {driveReady
            ? "Connected by server environment variables."
            : "Not connected. Set GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON and GOOGLE_DRIVE_FOLDER_ID on the API server."}
        </div>
        <div className="rowactions">
          <button type="button" disabled={loading} onClick={onScheduledNow}>
            Run schedule now
          </button>
          <button type="button" className="primary" disabled={loading} onClick={onCreate}>
            {loading ? "Working..." : "Create downloadable backup"}
          </button>
        </div>
      </article>
      <article className="panel upload">
        <div className="panelhead">
          <div>
            <h2>Restore backup</h2>
            <p>Upload a SecureStream backup .tar from this or another server.</p>
          </div>
          <b>Destructive</b>
        </div>
        <form onSubmit={onRestore}>
          <label>
            Backup .tar file
            <input name="file" type="file" accept=".tar,application/x-tar" required />
          </label>
          <div className="formnote">
            Restore first validates that the file is a real SecureStream backup with safe media paths. If validation
            fails, restore stops before replacing data.
          </div>
          <button type="submit" className="danger" disabled={loading}>
            {loading ? "Restoring..." : "Restore backup"}
          </button>
        </form>
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Available downloads</h2>
            <p>Stored under {String(backups?.storageRoot ?? "your media storage")}/backups.</p>
          </div>
        </div>
        <div className="rows">
          {items.map((backup) => (
            <div className="row" key={String(backup.name)}>
              <div>
                <b>{String(backup.name)}</b>
                <small>
                  {formatBytes(backup.sizeBytes)} - {formatDateTime(backup.createdAt)}
                </small>
              </div>
              <div className="rowactions">
                <a
                  className="download"
                  href={`${API}/admin/backups/${encodeURIComponent(String(backup.name))}/download`}
                >
                  Download
                </a>
                <button type="button" disabled={loading || !driveReady} onClick={() => onDriveUpload(backup)}>
                  Google Drive
                </button>
                <button type="button" className="danger" disabled={loading} onClick={() => onDelete(backup)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
        {!items.length && <div className="formnote">No backups yet. Create one first.</div>}
      </article>
    </section>
  );
}

function StorageBreakdownPanel({ storage }: { storage?: RecordItem }) {
  const categories = asArray(storage?.categories);
  return (
    <section className="grid">
      <article className="panel">
        <div className="panelhead">
          <div>
            <h2>Storage health</h2>
            <p>{String(storage?.storageRoot ?? "Media storage")}</p>
          </div>
          <b>{Number(storage?.usedPercent ?? 0)}%</b>
        </div>
        <div className="meter">
          <span style={{ width: `${Math.min(100, Number(storage?.usedPercent ?? 0))}%` }} />
        </div>
        <div className="tracefacts">
          <span>
            <b>Used</b>
            {formatBytes(storage?.usedBytes)}
          </span>
          <span>
            <b>Free</b>
            {formatBytes(storage?.freeBytes)}
          </span>
          <span>
            <b>Total</b>
            {formatBytes(storage?.totalBytes)}
          </span>
          <span>
            <b>Warning</b>
            {Number(storage?.warningPercent ?? 80)}%
          </span>
        </div>
        {Boolean(storage?.warning) && (
          <div className="formnote dangertext">
            Storage is above the warning threshold. Check large previews/backups or run cleanup.
          </div>
        )}
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Breakdown</h2>
            <p>Largest file groups in media storage.</p>
          </div>
        </div>
        <div className="rows">
          {categories.map((category) => (
            <div className="row" key={String(category.name)}>
              <div>
                <b>{String(category.name)}</b>
                <small>
                  {Number(category.fileCount ?? 0)} files - {formatBytes(category.sizeBytes)}
                </small>
              </div>
            </div>
          ))}
        </div>
      </article>
      <JsonPanel
        title="Largest files"
        value={Object.fromEntries(categories.map((category) => [String(category.name), category.largest]))}
      />
    </section>
  );
}

function DeviceSessionsPanel({
  sessions,
  loading,
  onRevoke,
}: {
  sessions: RecordItem[];
  loading: boolean;
  onRevoke: (session: RecordItem) => void;
}) {
  return (
    <article className="panel userspanel">
      <div className="panelhead">
        <div>
          <h2>Active device sessions</h2>
          <p>Logout lost phones, old Android installs, or suspicious sessions.</p>
        </div>
        <b>{sessions.length} active</b>
      </div>
      <div className="rows">
        {sessions.map((session) => {
          const user = session.user as RecordItem | undefined;
          return (
            <div className="row" key={String(session.id)}>
              <div>
                <b>{String(user?.email ?? session.userId)}</b>
                <small>
                  {String(user?.displayName ?? "Viewer")} - {String(session.deviceId ?? "unknown device")} - last used{" "}
                  {formatDateTime(session.lastUsedAt)}
                </small>
              </div>
              <div className="rowactions">
                <button type="button" className="danger" disabled={loading} onClick={() => onRevoke(session)}>
                  Logout device
                </button>
              </div>
            </div>
          );
        })}
      </div>
      {!sessions.length && <div className="formnote">No active device sessions.</div>}
    </article>
  );
}

function ActivityPanel({ rows }: { rows: RecordItem[] }) {
  return (
    <article className="panel userspanel">
      <div className="panelhead">
        <div>
          <h2>Admin activity timeline</h2>
          <p>Recent privileged actions across the platform.</p>
        </div>
        <b>{rows.length} events</b>
      </div>
      <div className="rows">
        {rows.map((row) => {
          const actor = row.actor as RecordItem | undefined;
          return (
            <div className="row" key={String(row.id)}>
              <div>
                <b>{String(row.action)}</b>
                <small>
                  {formatDateTime(row.createdAt)} - {String(actor?.email ?? "System")} - {String(row.targetType ?? "")}{" "}
                  {String(row.targetId ?? "")}
                </small>
              </div>
            </div>
          );
        })}
      </div>
    </article>
  );
}

function TrashPanel({
  trash,
  loading,
  onAction,
}: {
  trash?: RecordItem;
  loading: boolean;
  onAction: (kind: "users" | "movies", id: string, action: "restore" | "permanent") => void;
}) {
  const users = asArray(trash?.users);
  const movies = asArray(trash?.movies);
  const renderRow = (kind: "users" | "movies", item: RecordItem) => (
    <div className="row" key={String(item.id)}>
      <div>
        <b>{String(item.email ?? item.title ?? item.id)}</b>
        <small>
          {String(item.deletedReason ?? "Deleted")} - {formatDateTime(item.deletedAt)}
        </small>
      </div>
      <div className="rowactions">
        <button type="button" disabled={loading} onClick={() => onAction(kind, String(item.id), "restore")}>
          Restore
        </button>
        <button
          type="button"
          className="danger"
          disabled={loading}
          onClick={() => onAction(kind, String(item.id), "permanent")}
        >
          Delete forever
        </button>
      </div>
    </div>
  );
  return (
    <section className="grid">
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Deleted users</h2>
            <p>Restore accounts or permanently remove them.</p>
          </div>
          <b>{users.length}</b>
        </div>
        <div className="rows">{users.map((item) => renderRow("users", item))}</div>
        {!users.length && <div className="formnote">No deleted users.</div>}
      </article>
      <article className="panel userspanel">
        <div className="panelhead">
          <div>
            <h2>Deleted videos</h2>
            <p>Restore draft videos or permanently remove media files.</p>
          </div>
          <b>{movies.length}</b>
        </div>
        <div className="rows">{movies.map((item) => renderRow("movies", item))}</div>
        {!movies.length && <div className="formnote">No deleted videos.</div>}
      </article>
    </section>
  );
}

function TablePanel({ rows, columns }: { rows: RecordItem[]; columns: string[] }) {
  return (
    <article className="panel tablewrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>{column}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={String(row.id ?? index)}>
              {columns.map((column) => (
                <td key={column}>{format(row[column])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </article>
  );
}

function SettingsPanel({
  settings,
  loading,
  onSave,
  onTestAlert,
}: {
  settings?: RecordItem;
  loading: boolean;
  onSave: (event: FormEvent<HTMLFormElement>) => void;
  onTestAlert: () => void;
}) {
  const maintenance = (settings?.maintenance as RecordItem | undefined) ?? {};
  const backupSchedule = (settings?.backupSchedule as RecordItem | undefined) ?? {};
  const android = (settings?.android as RecordItem | undefined) ?? {};
  return (
    <section className="grid">
      <article className="panel">
        <div className="panelhead">
          <div>
            <h2>Platform controls</h2>
            <p>Maintenance, backups, alerts, storage warnings, and Android update checks.</p>
          </div>
        </div>
        <form className="stackform" onSubmit={onSave}>
          <label className="checkrow">
            <input
              name="deleteOriginalAfterPreview"
              type="checkbox"
              defaultChecked={Boolean(settings?.deleteOriginalAfterPreview)}
            />
            <span>
              <b>Delete original after MP4 preview is ready</b>
              <small>
                New uploads will keep only the browser/app playable MP4 after conversion succeeds. If conversion fails,
                the original stays.
              </small>
            </span>
          </label>
          <label className="checkrow">
            <input name="maintenanceMode" type="checkbox" defaultChecked={Boolean(maintenance.enabled)} />
            <span>
              <b>Maintenance mode</b>
              <small>
                Viewer logins and viewer API calls pause, while admins can still login and manage the system.
              </small>
            </span>
          </label>
          <label>
            Maintenance message
            <input name="maintenanceMessage" defaultValue={String(maintenance.message ?? "")} />
          </label>
          <div className="formgrid">
            <label>
              Backup hour (0-23)
              <input
                name="backupScheduleHour"
                type="number"
                min="0"
                max="23"
                defaultValue={String(backupSchedule.hour ?? 2)}
              />
            </label>
            <label>
              Keep backups
              <input
                name="backupRetentionCount"
                type="number"
                min="1"
                max="60"
                defaultValue={String(backupSchedule.retentionCount ?? 7)}
              />
            </label>
            <label>
              Storage warning %
              <input
                name="storageWarningPercent"
                type="number"
                min="1"
                max="99"
                defaultValue={String(settings?.storageWarningPercent ?? 80)}
              />
            </label>
          </div>
          <label className="checkrow">
            <input name="backupScheduleEnabled" type="checkbox" defaultChecked={Boolean(backupSchedule.enabled)} />
            <span>
              <b>Enable daily backup schedule</b>
              <small>The API creates one portable backup per day at the selected hour.</small>
            </span>
          </label>
          <label className="checkrow">
            <input name="backupScheduleDrive" type="checkbox" defaultChecked={Boolean(backupSchedule.uploadToDrive)} />
            <span>
              <b>Upload scheduled backups to Google Drive</b>
              <small>Requires the Google Drive service account env variables.</small>
            </span>
          </label>
          <div className="formgrid">
            <label>
              Android version name
              <input name="androidLatestVersionName" defaultValue={String(android.latestVersionName ?? "1.0.0")} />
            </label>
            <label>
              Android version code
              <input
                name="androidLatestVersionCode"
                type="number"
                min="1"
                defaultValue={String(android.latestVersionCode ?? 1)}
              />
            </label>
          </div>
          <label className="checkrow">
            <input name="androidUpdateRequired" type="checkbox" defaultChecked={Boolean(android.required)} />
            <span>
              <b>Force Android update</b>
              <small>The app can block old versions when AI Studio adds the version check.</small>
            </span>
          </label>
          <label>
            Android update message
            <input name="androidUpdateMessage" defaultValue={String(android.message ?? "")} />
          </label>
          <label>
            Android download URL
            <input
              name="androidDownloadUrl"
              placeholder="https://..."
              defaultValue={String(android.downloadUrl ?? "")}
            />
          </label>
          <div className="formnote">
            Alerts configured: {settings?.alertsConfigured ? "Yes" : "No"}. Use DISCORD_WEBHOOK_URL, ALERT_WEBHOOK_URL,
            or TELEGRAM_BOT_TOKEN + TELEGRAM_CHAT_ID on the API server.
          </div>
          <div className="rowactions">
            <button type="button" disabled={loading} onClick={onTestAlert}>
              Test alert
            </button>
            <button type="submit" className="primary" disabled={loading}>
              {loading ? "Saving..." : "Save settings"}
            </button>
          </div>
        </form>
      </article>
      <JsonPanel title="Runtime settings" value={settings} />
    </section>
  );
}

function JsonPanel({ title, value }: { title: string; value: unknown }) {
  return (
    <article className="panel">
      <div className="panelhead">
        <div>
          <h2>{title}</h2>
          <p>Live API data</p>
        </div>
      </div>
      <pre>{JSON.stringify(value ?? {}, null, 2)}</pre>
    </article>
  );
}

async function apiGet(path: string) {
  const response = await fetch(`${API}${path}`, { credentials: "include", headers: authHeaders() });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.error?.message ?? `Could not load ${path}`);
  return payload;
}

function uploadWithProgress(
  url: string,
  body: FormData,
  headers: Record<string, string>,
  onProgress: (progress: number) => void,
  onPhase: (phase: string) => void,
  onConversionProgress: (progress: number | null) => void,
): Promise<RecordItem> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    const uploadId = crypto.randomUUID();
    let pollTimer: ReturnType<typeof setInterval> | null = null;
    const stopPolling = () => {
      if (pollTimer) clearInterval(pollTimer);
      pollTimer = null;
    };
    const pollConversion = async () => {
      try {
        const progress = (await apiGet(`/admin/uploads/${uploadId}/progress`)) as { phase?: string; progress?: number };
        if (progress.phase?.startsWith("Converting")) onPhase("Converting preview");
        if (typeof progress.progress === "number")
          onConversionProgress(Math.max(0, Math.min(100, Math.round(progress.progress))));
      } catch {
        // Keep the upload alive even if one progress poll misses during deploy/network hiccups.
      }
    };
    const startPolling = () => {
      onConversionProgress(0);
      void pollConversion();
      pollTimer = setInterval(() => void pollConversion(), 1000);
    };
    request.open("POST", url);
    request.withCredentials = true;
    Object.entries(headers).forEach(([key, value]) => {
      request.setRequestHeader(key, value);
    });
    request.setRequestHeader("x-upload-id", uploadId);
    request.upload.onloadstart = () => onPhase("Uploading");
    request.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    request.upload.onload = () => {
      onProgress(100);
      onPhase("Converting preview");
      startPolling();
    };
    request.onload = () => {
      stopPolling();
      const payload = JSON.parse(request.responseText || "{}");
      if (request.status >= 200 && request.status < 300) {
        onProgress(100);
        onConversionProgress(100);
        resolve(payload);
      } else {
        reject(new Error(payload.error?.message ?? "Upload failed"));
      }
    };
    request.onerror = () => {
      stopPolling();
      reject(new Error("Upload request could not reach the API."));
    };
    request.send(body);
  });
}

function authHeaders(): Record<string, string> {
  const token = sessionStorage.getItem("ss_admin_access");
  return token ? { authorization: `Bearer ${token}` } : {};
}
function csrfHeaders(): Record<string, string> {
  return { ...authHeaders(), "x-csrf-token": sessionStorage.getItem("ss_csrf") ?? "" };
}
function optionalFormString(formData: FormData, key: string) {
  const value = String(formData.get(key) ?? "").trim();
  return value || undefined;
}
function uploadFormData(source: FormData, file: File, index: number, total: number) {
  const body = new FormData();
  const baseTitle = String(source.get("title") ?? "").trim();
  const title =
    total > 1
      ? baseTitle
        ? `${baseTitle} ${index + 1}`
        : titleFromFileName(file.name)
      : baseTitle || titleFromFileName(file.name);
  body.append("title", title);
  body.append("synopsis", String(source.get("synopsis") ?? ""));
  body.append("maturityRating", String(source.get("maturityRating") ?? ""));
  body.append("file", file, file.name);
  return body;
}
function titleFromFileName(name: string) {
  return (
    name
      .replace(/\.[^.]+$/, "")
      .replace(/[_-]+/g, " ")
      .trim()
      .slice(0, 160) || "Uploaded video"
  );
}
function collectionPayload(formData: FormData) {
  const movieIds = formData.getAll("movieIds").map(String).filter(Boolean);
  return {
    name: String(formData.get("name") ?? "").trim(),
    published: formData.get("published") === "on",
    sortOrder: Number(formData.get("sortOrder") || 0),
    parentId: String(formData.get("parentId") ?? "") || null,
    items: movieIds.map((movieId, index) => ({ movieId, sortOrder: index })),
  };
}
function userPayload(formData: FormData, requirePassword: boolean) {
  const password = String(formData.get("password") ?? "");
  const payload: Record<string, unknown> = {
    email: String(formData.get("email") ?? "").trim(),
    displayName: String(formData.get("displayName") ?? "").trim(),
    role: String(formData.get("role") ?? "VIEWER"),
    status: String(formData.get("status") ?? "ACTIVE"),
    accessRestricted: formData.get("accessRestricted") === "on",
    defaultCollectionId: optionalFormString(formData, "defaultCollectionId") ?? null,
    access: {
      movieIds: formData.getAll("movieIds").map(String).filter(Boolean),
      collectionIds: formData.getAll("collectionIds").map(String).filter(Boolean),
    },
  };
  if (password || requirePassword) payload.password = password;
  return payload;
}
function asArray(value: unknown): RecordItem[] {
  return Array.isArray(value) ? (value as RecordItem[]) : [];
}
function firstAsset(movie: RecordItem): RecordItem | null {
  return asArray(movie.assets)[0] ?? null;
}
function collectionMovieIds(collection: RecordItem) {
  return asArray(collection.items)
    .map((item) => String(item.movieId ?? (item.movie as RecordItem | undefined)?.id ?? ""))
    .filter(Boolean);
}
function userMovieAccessIds(user: RecordItem) {
  return asArray(user.movieAccess)
    .map((item) => String(item.movieId ?? ""))
    .filter(Boolean);
}
function userCollectionAccessIds(user: RecordItem) {
  return asArray(user.collectionAccess)
    .map((item) => String(item.collectionId ?? ""))
    .filter(Boolean);
}
function folderLabel(collection: RecordItem) {
  return `${String((collection.parent as RecordItem | undefined)?.name ? `${(collection.parent as RecordItem).name} / ` : "")}${String(collection.name ?? "Untitled")}`;
}
function count(value: unknown) {
  return String(Array.isArray(value) ? value.length : 0);
}
function jobsTotal(value: unknown) {
  const job = value as Record<string, number> | undefined;
  return String((job?.waiting ?? 0) + (job?.active ?? 0) + (job?.failed ?? 0));
}
function format(value: unknown) {
  if (value === null || value === undefined) return "";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}
function percent(value: unknown) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}
function formatBytes(value: unknown) {
  const bytes = Number(value ?? 0);
  if (!Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const index = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
  return `${(bytes / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`;
}
function formatLoad(value: unknown) {
  return Array.isArray(value) ? value.map((item) => Number(item).toFixed(2)).join(" / ") : "0.00";
}
function cpuLoadPercent(cpu: RecordItem) {
  const cores = Number(cpu.cores ?? 1) || 1;
  const firstLoad = Array.isArray(cpu.loadAverage) ? Number(cpu.loadAverage[0] ?? 0) : 0;
  return Math.min(100, Math.round((firstLoad / cores) * 100));
}
function formatDuration(seconds: number) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${days ? `${days}d ` : ""}${hours}h ${minutes}m`;
}
function formatDate(value: unknown) {
  const date = new Date(String(value));
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleTimeString();
}
function formatDateTime(value: unknown) {
  const date = new Date(String(value));
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleString("en-PH", { timeZone: "Asia/Manila" });
}
function fileNameFromKey(value: unknown) {
  return (
    String(value ?? "")
      .split(/[\\/]/)
      .pop() || "Unknown file"
  );
}
function fileSearchText(file: RecordItem) {
  return [
    file.title,
    file.sourceFileName,
    file.sourceStorageKey,
    file.previewFileName,
    file.format,
    file.previewFormat,
    file.state,
    file.uploadStatus,
    file.ownerStatus,
  ]
    .map((value) => String(value ?? "").toLowerCase())
    .join(" ");
}
function formatRuntime(value: unknown) {
  const seconds = Number(value ?? 0);
  if (!Number.isFinite(seconds) || seconds <= 0) return "Unknown";
  const minutes = Math.floor(seconds / 60);
  const remaining = Math.round(seconds % 60);
  return minutes ? `${minutes}m ${remaining}s` : `${remaining}s`;
}
function panelSubtitle(tab: Tab) {
  const labels: Record<Tab, string> = {
    Overview: "Platform snapshot and quick actions.",
    Catalog: "Published rails and title inventory.",
    Videos: "Review and publish uploaded videos.",
    "Video Editor": "Trim source videos and save edited clips as safe draft copies.",
    "File Manager": "Track uploaded files, sizes, formats, and storage details.",
    Storage: "Monitor storage quota, warning threshold, and largest file groups.",
    Series: "Manage episodic catalog records.",
    Uploads: "Upload source files and create draft videos.",
    Processing: "Monitor encoding and packaging jobs.",
    Collections: "Browse configured home-screen rails.",
    Users: "Review viewer and operator accounts.",
    "Device Sessions": "Logout active devices and manage user sessions.",
    Messages: "Reply to viewer messages.",
    Notifications: "Send updates to viewer dashboards.",
    "API Tokens": "Create and revoke automation tokens for n8n workflows.",
    "Playback sessions": "Inspect recent stream grants.",
    "Watermark Trace": "Paste a playback watermark and identify the user/session.",
    "Backup & Restore": "Download or restore a portable server migration backup.",
    Activity: "Review recent admin actions in timeline form.",
    Trash: "Restore or permanently delete removed users and videos.",
    "Audit logs": "Trace privileged admin actions.",
    Security: "Review security events.",
    Settings: "Inspect runtime configuration.",
  };
  return labels[tab];
}

function parseWatermark(value: string) {
  const match = value
    .trim()
    .toLowerCase()
    .match(/^([a-f0-9]{4})\s*\.\.\s*([a-f0-9]{4})$/);
  return match ? { userPrefix: match[1], sessionSuffix: match[2] } : null;
}

function watermarkForSession(row: RecordItem) {
  const userId = String(row.userId ?? "");
  const sessionId = String(row.id ?? "");
  return userId && sessionId ? `${userId.slice(0, 4)}..${sessionId.slice(-4)}` : "Unknown";
}

function sessionVideoTitle(asset?: RecordItem) {
  const movie = asset?.movie as RecordItem | undefined;
  const episode = asset?.episode as RecordItem | undefined;
  return String(movie?.title ?? episode?.title ?? asset?.id ?? "Unknown video");
}

function prefersReducedMotion() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}
