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
      .then((payload) => {
        if (payload?.accessToken) sessionStorage.setItem("ss_admin_access", payload.accessToken);
        setAdmin(payload);
      })
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
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [conversionProgress, setConversionProgress] = useState<number | null>(null);
  const [uploadPhase, setUploadPhase] = useState("");
  const [notice, setNotice] = useState("");
  const [preview, setPreview] = useState<{ title: string; url: string; playable: boolean; format: string } | null>(null);
  const [editing, setEditing] = useState<RecordItem | null>(null);

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
      if (["Overview", "Movies", "Uploads", "Collections", "Catalog"].includes(tab)) await get("movies", "/admin/movies");
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
    setUploadProgress(0);
    setConversionProgress(null);
    setUploadPhase("Uploading");
    setNotice("");
    try {
      const payload = await uploadWithProgress(`${API}/admin/uploads/direct`, new FormData(form), csrfHeaders(), setUploadProgress, setUploadPhase, setConversionProgress);
      form.reset();
      setNotice(`Uploaded "${payload.title}" as a draft title.`);
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
    const confirmed = confirm(`Delete "${String(movie.title ?? "this video")}"? This removes the catalog record and uploaded source file.`);
    if (!confirmed) return;
    setLoading(true);
    setNotice("");
    try {
      const response = await fetch(`${API}/admin/movies/${movie.id}`, { method: "DELETE", credentials: "include", headers: csrfHeaders() });
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
      const response = await fetch(`${API}/admin/collections/${collection.id}`, { method: "DELETE", credentials: "include", headers: csrfHeaders() });
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

  function previewMovie(movie: RecordItem) {
    const asset = firstAsset(movie);
    if (!asset?.id) {
      setNotice("No uploaded video asset is available for this title yet.");
      return;
    }
    const token = sessionStorage.getItem("ss_admin_access") ?? "";
    const format = String(asset.manifestStorageKey ?? asset.sourceStorageKey ?? "").split(".").pop()?.toLowerCase() ?? "unknown";
    const playable = ["mp4", "webm", "mov"].includes(format);
    const baseUrl = `${API}/admin/video-assets/${asset.id}/preview`;
    setPreview({ title: String(movie.title ?? "Video preview"), url: token ? `${baseUrl}?token=${encodeURIComponent(token)}` : baseUrl, playable, format });
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
        {editing && <EditMoviePanel movie={editing} loading={loading} onCancel={() => setEditing(null)} onSubmit={updateMovie} />}
        {active === "Catalog" && <CatalogPanel collections={asArray(data.collections)} movies={asArray(data.movies)} loading={loading} onCreateCollection={createCollection} onUpdateCollection={updateCollection} onDeleteCollection={deleteCollection} onPreview={previewMovie} onEdit={setEditing} onDelete={deleteMovie} />}
        {active === "Movies" && <MoviesPanel movies={asArray(data.movies)} onPublish={publishMovie} onPreview={previewMovie} onEdit={setEditing} onDelete={deleteMovie} />}
        {active === "Series" && <SeriesPanel series={asArray(data.series)} />}
        {active === "Uploads" && <UploadsPanel movies={asArray(data.movies)} uploading={loading} uploadProgress={uploadProgress} conversionProgress={conversionProgress} uploadPhase={uploadPhase} onUpload={uploadMovie} onPublish={publishMovie} onPreview={previewMovie} onEdit={setEditing} onDelete={deleteMovie} />}
        {active === "Processing" && <JsonPanel title="Processing jobs" value={data.processing} />}
        {active === "Collections" && <CollectionsPanel collections={asArray(data.collections)} movies={asArray(data.movies)} loading={loading} onCreate={createCollection} onUpdate={updateCollection} onDelete={deleteCollection} />}
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

function UploadsPanel({ movies, uploading, uploadProgress, conversionProgress, uploadPhase, onUpload, onPublish, onPreview, onEdit, onDelete }: { movies: RecordItem[]; uploading: boolean; uploadProgress: number | null; conversionProgress: number | null; uploadPhase: string; onUpload: (event: FormEvent<HTMLFormElement>) => void; onPublish: (id: string) => void; onPreview: (movie: RecordItem) => void; onEdit: (movie: RecordItem) => void; onDelete: (movie: RecordItem) => void }) {
  const converting = uploadPhase === "Converting preview";
  const visibleProgress = converting ? conversionProgress ?? 0 : uploadProgress ?? 0;
  return (
    <section className="grid">
      <article className="panel upload">
        <div className="panelhead"><div><h2>Upload title</h2><p>Create a draft movie and store the source file.</p></div><b>Draft</b></div>
        <form onSubmit={onUpload}>
          <label>Title<input name="title" required maxLength={160} /></label>
          <label>Synopsis<textarea name="synopsis" rows={4} /></label>
          <label>Maturity rating<input name="maturityRating" placeholder="PG-13" maxLength={20} /></label>
          <label>Video file<input name="file" type="file" accept="video/*,.mp4,.mov,.mkv,.webm,.avi,.wmv,.flv" required /></label>
          <small>Most video formats are accepted. The server creates an MP4/H.264 preview for browser playback after upload.</small>
          {uploadProgress !== null && <div className={`uploadprogress ${converting ? "processing" : ""}`}><span style={{ width: `${visibleProgress}%` }} /><b>{converting ? `Converting preview... ${visibleProgress}%` : `Uploading ${visibleProgress}%`}</b></div>}
          <button className="primary" disabled={uploading}>{uploading ? uploadPhase || "Working..." : "Upload title"}</button>
        </form>
      </article>
      <MoviesPanel movies={movies} onPublish={onPublish} onPreview={onPreview} onEdit={onEdit} onDelete={onDelete} compact />
    </section>
  );
}

function MoviesPanel({ movies, onPublish, onPreview, onEdit, onDelete, compact = false }: { movies: RecordItem[]; onPublish: (id: string) => void; onPreview: (movie: RecordItem) => void; onEdit: (movie: RecordItem) => void; onDelete: (movie: RecordItem) => void; compact?: boolean }) {
  return (
    <article className="panel">
      <div className="panelhead"><div><h2>{compact ? "Recent uploads" : "Movies"}</h2><p>{movies.length} movie records</p></div></div>
      <div className="rows">{movies.map((movie) => <div className="row" key={String(movie.id)}><div><b>{String(movie.title ?? "Untitled")}</b><small>{String(movie.status ?? "DRAFT")} - {count(movie.assets)} assets</small></div><div className="rowactions"><button disabled={!firstAsset(movie)} onClick={() => onPreview(movie)}>Check video</button><button onClick={() => onEdit(movie)}>Edit</button><button className="danger" onClick={() => onDelete(movie)}>Delete</button><button disabled={movie.status === "PUBLISHED"} onClick={() => onPublish(String(movie.id))}>Publish</button></div></div>)}</div>
    </article>
  );
}

function PreviewModal({ preview, onClose }: { preview: { title: string; url: string; playable: boolean; format: string }; onClose: () => void }) {
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
    return () => { if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [preview.url]);
  return <article className="panel preview"><div className="panelhead"><div><h2>Check video before publishing</h2><p>{preview.title}</p></div><button onClick={onClose}>Close</button></div>{!preview.playable && <div className="formnote">A browser MP4 preview was not created for this file yet. Re-upload after redeploying the API with FFmpeg enabled, or use Download source to check it locally.</div>}{!videoUrl && !error && <div className="formnote">Loading preview...</div>}{videoUrl && <video src={videoUrl} controls preload="metadata" onError={() => setError("The browser could not play this preview. Re-upload the video after redeploying the API so it can create a browser-compatible MP4 preview. Also make sure API storage is persistent at /data/media.")} />}{error && <div className="formerror">{error}</div>}<a className="download" href={preview.url}>Download source/preview</a></article>;
}

function EditMoviePanel({ movie, loading, onCancel, onSubmit }: { movie: RecordItem; loading: boolean; onCancel: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <article className="panel upload editpanel"><div className="panelhead"><div><h2>Edit video</h2><p>{String(movie.title ?? "Untitled")}</p></div><button onClick={onCancel}>Cancel</button></div><form onSubmit={onSubmit}><label>Title<input name="title" required maxLength={160} defaultValue={String(movie.title ?? "")} /></label><label>Synopsis<textarea name="synopsis" rows={4} required defaultValue={String(movie.synopsis ?? "")} /></label><label>Maturity rating<input name="maturityRating" maxLength={20} defaultValue={String(movie.maturityRating ?? "")} /></label><label>Status<select name="status" defaultValue={String(movie.status ?? "DRAFT")}><option value="DRAFT">Draft</option><option value="PUBLISHED">Published</option><option value="UNPUBLISHED">Unpublished</option><option value="ARCHIVED">Archived</option></select></label><button className="primary" disabled={loading}>{loading ? "Saving..." : "Save changes"}</button></form></article>;
}

function CatalogPanel({ collections, movies, loading, onCreateCollection, onUpdateCollection, onDeleteCollection, onPreview, onEdit, onDelete }: { collections: RecordItem[]; movies: RecordItem[]; loading: boolean; onCreateCollection: (event: FormEvent<HTMLFormElement>) => void; onUpdateCollection: (collection: RecordItem, event: FormEvent<HTMLFormElement>) => void; onDeleteCollection: (collection: RecordItem) => void; onPreview: (movie: RecordItem) => void; onEdit: (movie: RecordItem) => void; onDelete: (movie: RecordItem) => void }) {
  return <section className="grid"><CollectionsPanel collections={collections} movies={movies} loading={loading} onCreate={onCreateCollection} onUpdate={onUpdateCollection} onDelete={onDeleteCollection} /><MoviesPanel movies={movies} onPublish={() => undefined} onPreview={onPreview} onEdit={onEdit} onDelete={onDelete} compact /></section>;
}

function CollectionsPanel({ collections, movies, loading, onCreate, onUpdate, onDelete }: { collections: RecordItem[]; movies: RecordItem[]; loading: boolean; onCreate: (event: FormEvent<HTMLFormElement>) => void; onUpdate: (collection: RecordItem, event: FormEvent<HTMLFormElement>) => void; onDelete: (collection: RecordItem) => void }) {
  return (
    <article className="panel folders">
      <div className="panelhead"><div><h2>Folders / Collections</h2><p>Create Android home sections and organize published videos.</p></div><b>{collections.length} folders</b></div>
      <form className="folderform" onSubmit={onCreate}>
        <label>New folder name<input name="name" placeholder="Action, Kids, Drama..." required maxLength={120} /></label>
        <label>Sort order<input name="sortOrder" type="number" defaultValue={collections.length + 1} /></label>
        <label className="toggle"><input name="published" type="checkbox" defaultChecked /> Show in Android app</label>
        <MovieChecklist movies={movies} />
        <button className="primary" disabled={loading}>Create folder</button>
      </form>
      <div className="folderlist">
        {collections.map((collection) => <CollectionEditor key={String(collection.id)} collection={collection} movies={movies} loading={loading} onUpdate={onUpdate} onDelete={onDelete} />)}
      </div>
    </article>
  );
}

function CollectionEditor({ collection, movies, loading, onUpdate, onDelete }: { collection: RecordItem; movies: RecordItem[]; loading: boolean; onUpdate: (collection: RecordItem, event: FormEvent<HTMLFormElement>) => void; onDelete: (collection: RecordItem) => void }) {
  const selected = collectionMovieIds(collection);
  return (
    <form className="foldercard" onSubmit={(event) => onUpdate(collection, event)}>
      <div className="panelhead"><div><h3>{String(collection.name ?? "Untitled folder")}</h3><p>{String(collection.slug ?? "")} - {collection.published ? "Visible in Android" : "Draft/hidden"} - {count(collection.items)} videos</p></div><div className="rowactions"><button type="button" className="danger" disabled={loading} onClick={() => onDelete(collection)}>Delete</button><button disabled={loading}>Save folder</button></div></div>
      <div className="folderfields">
        <label>Name<input name="name" required maxLength={120} defaultValue={String(collection.name ?? "")} /></label>
        <label>Sort<input name="sortOrder" type="number" defaultValue={Number(collection.sortOrder ?? 0)} /></label>
        <label className="toggle"><input name="published" type="checkbox" defaultChecked={Boolean(collection.published)} /> Show in Android app</label>
      </div>
      <MovieChecklist movies={movies} selected={selected} />
    </form>
  );
}

function MovieChecklist({ movies, selected = new Set<string>() }: { movies: RecordItem[]; selected?: Set<string> }) {
  const published = movies.filter((movie) => movie.status === "PUBLISHED");
  if (!published.length) return <div className="formnote">Publish videos first, then add them to folders.</div>;
  return <div className="checkgrid">{published.map((movie) => <label key={String(movie.id)}><input name="movieIds" type="checkbox" value={String(movie.id)} defaultChecked={selected.has(String(movie.id))} /> <span>{String(movie.title ?? "Untitled")}</span></label>)}</div>;
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

function uploadWithProgress(url: string, body: FormData, headers: Record<string, string>, onProgress: (progress: number) => void, onPhase: (phase: string) => void, onConversionProgress: (progress: number | null) => void): Promise<RecordItem> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    const uploadId = crypto.randomUUID();
    let pollTimer: ReturnType<typeof setInterval> | null = null;
    const stopPolling = () => { if (pollTimer) clearInterval(pollTimer); pollTimer = null; };
    const pollConversion = async () => {
      try {
        const progress = await apiGet(`/admin/uploads/${uploadId}/progress`) as { phase?: string; progress?: number };
        if (progress.phase?.startsWith("Converting")) onPhase("Converting preview");
        if (typeof progress.progress === "number") onConversionProgress(Math.max(0, Math.min(100, Math.round(progress.progress))));
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
    Object.entries(headers).forEach(([key, value]) => request.setRequestHeader(key, value));
    request.setRequestHeader("x-upload-id", uploadId);
    request.upload.onloadstart = () => onPhase("Uploading");
    request.upload.onprogress = (event) => { if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100)); };
    request.upload.onload = () => { onProgress(100); onPhase("Converting preview"); startPolling(); };
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
    request.onerror = () => { stopPolling(); reject(new Error("Upload request could not reach the API.")); };
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
function collectionPayload(formData: FormData) {
  const movieIds = formData.getAll("movieIds").map(String).filter(Boolean);
  return {
    name: String(formData.get("name") ?? "").trim(),
    published: formData.get("published") === "on",
    sortOrder: Number(formData.get("sortOrder") || 0),
    items: movieIds.map((movieId, index) => ({ movieId, sortOrder: index })),
  };
}
function asArray(value: unknown): RecordItem[] { return Array.isArray(value) ? value as RecordItem[] : []; }
function firstAsset(movie: RecordItem): RecordItem | null { return asArray(movie.assets)[0] ?? null; }
function collectionMovieIds(collection: RecordItem) { return new Set(asArray(collection.items).map((item) => String(item.movieId ?? (item.movie as RecordItem | undefined)?.id ?? "")).filter(Boolean)); }
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
