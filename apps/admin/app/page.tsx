"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:4000/api/v1";
const nav = ["Overview", "Catalog", "Movies", "Series", "Uploads", "Processing", "Collections", "Users", "Playback sessions", "Audit logs", "Security", "Settings"] as const;
type Tab = (typeof nav)[number];
type Admin = { id: string; displayName: string; email: string; role: string };
type RecordItem = Record<string, unknown>;

export default function App() {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    fetch(`${API}/admin/auth/me`, { credentials: "include", headers: authHeaders() })
      .then((response) => (response.ok ? response.json() : null))
      .then(setAdmin)
      .catch(() => setAdmin(null))
      .finally(() => setChecking(false));
  }, []);

  if (checking) return <div className="center"><div className="spinner" /><p>Validating secure session</p></div>;
  return admin ? <Dashboard admin={admin} /> : <Login onLogin={setAdmin} />;
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
        body: JSON.stringify({ email: data.get("email"), password: data.get("password"), deviceId: `admin-web-${navigator.userAgent.length}` }),
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
        <div className="brand"><i>{">"}</i> SecureStream</div>
        <small>ADMINISTRATION CONSOLE</small>
        <h1>Welcome back</h1>
        <p>Sign in with your administrator account.</p>
        <form onSubmit={submit}>
          <label>Email<input name="email" type="email" autoComplete="username" required /></label>
          <label>Password<input name="password" type="password" autoComplete="current-password" minLength={12} required /></label>
          {error && <div className="formerror" role="alert">{error}</div>}
          <button className="primary" disabled={busy}>{busy ? "Signing in..." : "Sign in securely"}</button>
        </form>
        <footer>Protected by server-enforced roles, secure cookies, and audit logging.</footer>
      </section>
    </main>
  );
}

function Dashboard({ admin }: { admin: Admin }) {
  const [active, setActive] = useState<Tab>("Overview");
  const [data, setData] = useState<Record<string, unknown>>({});
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState("");
  const [preview, setPreview] = useState<{ title: string; url: string } | null>(null);

  const metrics = useMemo(() => [
    ["Movies", count(data.movies), "catalog"],
    ["Series", count(data.series), "episodic"],
    ["Users", count(data.users), "accounts"],
    ["Jobs", jobsTotal(data.processing), "queue"],
  ], [data]);

  async function load(tab = active) {
    setLoading(true);
    setNotice("");
    try {
      const next: Record<string, unknown> = { ...data };
      const get = async (key: string, path: string) => { next[key] = await apiGet(path); };
      if (["Overview", "Movies", "Uploads"].includes(tab)) await get("movies", "/admin/movies");
      if (["Overview", "Series"].includes(tab)) await get("series", "/admin/series");
      if (["Overview", "Collections", "Catalog"].includes(tab)) await get("collections", "/admin/collections");
      if (["Overview", "Processing"].includes(tab)) await get("processing", "/admin/processing/jobs");
      if (tab === "Users") await get("users", "/admin/users");
      if (tab === "Playback sessions") await get("playback", "/admin/playback-sessions");
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

  useEffect(() => { void load(active); }, [active]);

  async function uploadMovie(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/uploads/direct`, { method: "POST", credentials: "include", headers: csrfHeaders(), body: new FormData(form) });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Upload failed");
      form.reset();
      setNotice(`Uploaded "${payload.title}" as a draft title.`);
      await load("Uploads");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Upload failed.");
    } finally {
      setLoading(false);
    }
  }

  async function publishMovie(id: string) {
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/movies/${id}/publish`, { method: "POST", credentials: "include", headers: csrfHeaders() });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.error?.message ?? "Publish failed");
      setNotice("Movie published.");
      await load(active);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Publish failed.");
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
    setPreview({ title: String(movie.title ?? "Video preview"), url: `${API}/admin/video-assets/${asset.id}/preview?token=${encodeURIComponent(token)}` });
  }

  return (
    <div className="shell">
      <aside>
        <div className="brand"><i>{">"}</i> SecureStream</div>
        <nav>{nav.map((item) => <button className={item === active ? "active" : ""} key={item} onClick={() => setActive(item)}>{item}</button>)}</nav>
        <div className="operator"><span>{admin.displayName.slice(0, 2).toUpperCase()}</span><div><b>{admin.displayName}</b><small>{admin.role.replace("_", " ").toLowerCase()}</small></div></div>
      </aside>
      <main>
        <header>
          <div><small>OPERATIONS CENTER</small><h1>{active}</h1><p>{panelSubtitle(active)}</p></div>
          <div className="actions"><button onClick={() => load(active)} disabled={loading}>{loading ? "Refreshing..." : "Refresh"}</button><button className="primary" onClick={() => setActive("Uploads")}>+ Upload</button></div>
        </header>
        {notice && <div className="formnote workspace-note">{notice}</div>}
        {active === "Overview" && <Overview metrics={metrics} data={data} />}
        {preview && <PreviewModal preview={preview} onClose={() => setPreview(null)} />}
        {active === "Catalog" && <CatalogPanel collections={asArray(data.collections)} movies={asArray(data.movies)} onPreview={previewMovie} />}
        {active === "Movies" && <MoviesPanel movies={asArray(data.movies)} onPublish={publishMovie} onPreview={previewMovie} />}
        {active === "Series" && <SeriesPanel series={asArray(data.series)} />}
        {active === "Uploads" && <UploadsPanel movies={asArray(data.movies)} uploading={loading} onUpload={uploadMovie} onPublish={publishMovie} onPreview={previewMovie} />}
        {active === "Processing" && <JsonPanel title="Processing jobs" value={data.processing} />}
        {active === "Collections" && <CollectionsPanel collections={asArray(data.collections)} />}
        {active === "Users" && <TablePanel rows={asArray(data.users)} columns={["email", "displayName", "role", "status", "createdAt"]} />}
        {active === "Playback sessions" && <PlaybackPanel rows={asArray(data.playback)} />}
        {active === "Audit logs" && <TablePanel rows={asArray(data.audit)} columns={["action", "targetType", "targetId", "createdAt"]} />}
        {active === "Security" && <TablePanel rows={asArray(data.security)} columns={["kind", "severity", "userId", "createdAt"]} />}
        {active === "Settings" && <JsonPanel title="Runtime settings" value={data.settings} />}
      </main>
    </div>
  );
}

function Overview({ metrics, data }: { metrics: string[][]; data: Record<string, unknown> }) {
  return (
    <>
      <section className="metrics">{metrics.map(([label, value, trend]) => <article key={label}><small>{label}</small><strong>{value}</strong><em>{trend}</em></article>)}</section>
      <section className="grid">
        <JsonPanel title="Queue snapshot" value={data.processing} />
        <JsonPanel title="Recent catalog" value={{ movies: count(data.movies), collections: count(data.collections), series: count(data.series) }} />
      </section>
    </>
  );
}

function UploadsPanel({ movies, uploading, onUpload, onPublish, onPreview }: { movies: RecordItem[]; uploading: boolean; onUpload: (event: FormEvent<HTMLFormElement>) => void; onPublish: (id: string) => void; onPreview: (movie: RecordItem) => void }) {
  return (
    <section className="grid">
      <article className="panel upload">
        <div className="panelhead"><div><h2>Upload title</h2><p>Create a draft movie and store the source file.</p></div><b>Draft</b></div>
        <form onSubmit={onUpload}>
          <label>Title<input name="title" required maxLength={160} /></label>
          <label>Synopsis<textarea name="synopsis" rows={4} /></label>
          <label>Maturity rating<input name="maturityRating" placeholder="PG-13" maxLength={20} /></label>
          <label>Video file<input name="file" type="file" accept="video/*,.m3u8,.mp4,.mov,.mkv" required /></label>
          <button className="primary" disabled={uploading}>{uploading ? "Uploading..." : "Upload title"}</button>
        </form>
      </article>
      <MoviesPanel movies={movies} onPublish={onPublish} onPreview={onPreview} compact />
    </section>
  );
}

function MoviesPanel({ movies, onPublish, onPreview, compact = false }: { movies: RecordItem[]; onPublish: (id: string) => void; onPreview: (movie: RecordItem) => void; compact?: boolean }) {
  return (
    <article className="panel">
      <div className="panelhead"><div><h2>{compact ? "Recent uploads" : "Movies"}</h2><p>{movies.length} movie records</p></div></div>
      <div className="rows">{movies.map((movie) => <div className="row" key={String(movie.id)}><div><b>{String(movie.title ?? "Untitled")}</b><small>{String(movie.status ?? "DRAFT")} - {count(movie.assets)} assets</small></div><div className="rowactions"><button disabled={!firstAsset(movie)} onClick={() => onPreview(movie)}>Check video</button><button disabled={movie.status === "PUBLISHED"} onClick={() => onPublish(String(movie.id))}>Publish</button></div></div>)}</div>
    </article>
  );
}

function PreviewModal({ preview, onClose }: { preview: { title: string; url: string }; onClose: () => void }) {
  return <article className="panel preview"><div className="panelhead"><div><h2>Check video before publishing</h2><p>{preview.title}</p></div><button onClick={onClose}>Close</button></div><video src={preview.url} controls preload="metadata" /></article>;
}

function CatalogPanel({ collections, movies, onPreview }: { collections: RecordItem[]; movies: RecordItem[]; onPreview: (movie: RecordItem) => void }) {
  return <section className="grid"><CollectionsPanel collections={collections} /><MoviesPanel movies={movies} onPublish={() => undefined} onPreview={onPreview} compact /></section>;
}

function CollectionsPanel({ collections }: { collections: RecordItem[] }) {
  return <article className="panel"><div className="panelhead"><div><h2>Collections</h2><p>{collections.length} rails configured</p></div></div><div className="rows">{collections.map((row) => <div className="row" key={String(row.id)}><div><b>{String(row.name ?? "Untitled")}</b><small>{String(row.slug ?? "")} - {row.published ? "Published" : "Draft"}</small></div><em>{count(row.items)} items</em></div>)}</div></article>;
}

function SeriesPanel({ series }: { series: RecordItem[] }) {
  return <article className="panel"><div className="panelhead"><div><h2>Series</h2><p>{series.length} series records</p></div></div><div className="rows">{series.map((row) => <div className="row" key={String(row.id)}><div><b>{String(row.title ?? "Untitled")}</b><small>{String(row.status ?? "DRAFT")} - {count(row.seasons)} seasons</small></div></div>)}</div></article>;
}

function PlaybackPanel({ rows }: { rows: RecordItem[] }) {
  return <article className="panel"><div className="panelhead"><div><h2>Playback sessions</h2><p>{rows.length} recent sessions</p></div></div><div className="rows">{rows.map((row) => <div className="row" key={String(row.id)}><div><b>{String((row.user as RecordItem | undefined)?.email ?? row.userId)}</b><small>{String(row.deviceId ?? "")} - expires {format(row.expiresAt)}</small></div><em>{row.revokedAt ? "Revoked" : "Active"}</em></div>)}</div></article>;
}

function TablePanel({ rows, columns }: { rows: RecordItem[]; columns: string[] }) {
  return <article className="panel tablewrap"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={String(row.id ?? index)}>{columns.map((column) => <td key={column}>{format(row[column])}</td>)}</tr>)}</tbody></table></article>;
}

function JsonPanel({ title, value }: { title: string; value: unknown }) {
  return <article className="panel"><div className="panelhead"><div><h2>{title}</h2><p>Live API data</p></div></div><pre>{JSON.stringify(value ?? {}, null, 2)}</pre></article>;
}

async function apiGet(path: string) {
  const response = await fetch(`${API}${path}`, { credentials: "include", headers: authHeaders() });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.error?.message ?? `Could not load ${path}`);
  return payload;
}

function authHeaders(): Record<string, string> {
  const token = sessionStorage.getItem("ss_admin_access");
  return token ? { authorization: `Bearer ${token}` } : {};
}
function csrfHeaders(): Record<string, string> {
  return { ...authHeaders(), "x-csrf-token": sessionStorage.getItem("ss_csrf") ?? "" };
}
function asArray(value: unknown): RecordItem[] { return Array.isArray(value) ? value as RecordItem[] : []; }
function firstAsset(movie: RecordItem): RecordItem | null { return asArray(movie.assets)[0] ?? null; }
function count(value: unknown) { return String(Array.isArray(value) ? value.length : 0); }
function jobsTotal(value: unknown) { const job = value as Record<string, number> | undefined; return String((job?.waiting ?? 0) + (job?.active ?? 0) + (job?.failed ?? 0)); }
function format(value: unknown) { if (value === null || value === undefined) return ""; if (typeof value === "object") return JSON.stringify(value); return String(value); }
function panelSubtitle(tab: Tab) {
  const labels: Record<Tab, string> = {
    Overview: "Platform snapshot and quick actions.",
    Catalog: "Published rails and title inventory.",
    Movies: "Review and publish uploaded movies.",
    Series: "Manage episodic catalog records.",
    Uploads: "Upload source files and create draft movies.",
    Processing: "Monitor encoding and packaging jobs.",
    Collections: "Browse configured home-screen rails.",
    Users: "Review viewer and operator accounts.",
    "Playback sessions": "Inspect recent stream grants.",
    "Audit logs": "Trace privileged admin actions.",
    Security: "Review security events.",
    Settings: "Inspect runtime configuration.",
  };
  return labels[tab];
}
