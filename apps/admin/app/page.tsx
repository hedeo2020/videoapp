"use client";

import { FormEvent, useEffect, useState } from "react";

const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:4000/api/v1";

type Admin = { id: string; displayName: string; email: string; role: string };

const metrics = [
  ["Total users", "12,480", "+8.2%"],
  ["Published titles", "842", "+19"],
  ["Active streams", "316", "Live"],
  ["Processing", "3", "1 warning"],
];

export default function App() {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    fetch(`${API}/admin/auth/me`, { credentials: "include" })
      .then((response) => (response.ok ? response.json() : null))
      .then(setAdmin)
      .catch(() => setAdmin(null))
      .finally(() => setChecking(false));
  }, []);

  if (checking) {
    return (
      <div className="center">
        <div className="spinner" />
        <p>Validating secure session</p>
      </div>
    );
  }

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
        body: JSON.stringify({
          email: data.get("email"),
          password: data.get("password"),
          deviceId: `admin-web-${navigator.userAgent.length}`,
        }),
      });
      const payload = await response.json().catch(() => ({}));

      if (!response.ok) {
        setError(payload.error?.message ?? "Sign in failed");
        return;
      }

      sessionStorage.setItem("ss_csrf", payload.csrfToken);
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
          <i>{">"}</i> SecureStream
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
          <button className="primary" disabled={busy}>
            {busy ? "Signing in..." : "Sign in securely"}
          </button>
        </form>
        <footer>Protected by server-enforced roles, secure cookies, and audit logging.</footer>
      </section>
    </main>
  );
}

function Dashboard({ admin }: { admin: Admin }) {
  return (
    <div className="shell">
      <aside>
        <div className="brand">
          <i>{">"}</i> SecureStream
        </div>
        <nav>
          {[
            "Overview",
            "Catalog",
            "Movies",
            "Series",
            "Uploads",
            "Processing",
            "Collections",
            "Users",
            "Playback sessions",
            "Audit logs",
            "Security",
            "Settings",
          ].map((item, index) => (
            <a className={index === 0 ? "active" : ""} key={item}>
              {item}
            </a>
          ))}
        </nav>
        <div className="operator">
          <span>{admin.displayName.slice(0, 2).toUpperCase()}</span>
          <div>
            <b>{admin.displayName}</b>
            <small>{admin.role.replace("_", " ").toLowerCase()}</small>
          </div>
        </div>
      </aside>
      <main>
        <header>
          <div>
            <small>OPERATIONS CENTER</small>
            <h1>Good evening, {admin.displayName.split(" ")[0]}</h1>
            <p>Here is what is happening across your platform.</p>
          </div>
          <div className="actions">
            <button>View system health</button>
            <button className="primary">+ Add title</button>
          </div>
        </header>
        <section className="metrics">
          {metrics.map(([label, value, trend]) => (
            <article key={label}>
              <small>{label}</small>
              <strong>{value}</strong>
              <em>{trend}</em>
            </article>
          ))}
        </section>
        <section className="grid">
          <article className="panel activity">
            <div className="panelhead">
              <div>
                <h2>Streaming activity</h2>
                <p>Concurrent sessions - last 24 hours</p>
              </div>
              <b>316 now</b>
            </div>
            <div className="chart">
              <div className="line" />
            </div>
            <div className="chartlabels">
              <span>12 AM</span>
              <span>6 AM</span>
              <span>12 PM</span>
              <span>6 PM</span>
              <span>Now</span>
            </div>
          </article>
          <article className="panel health">
            <div className="panelhead">
              <div>
                <h2>System health</h2>
                <p>Production - ARM64</p>
              </div>
              <b>All systems operational</b>
            </div>
            {[
              ["API", "42 ms"],
              ["PostgreSQL", "Connected"],
              ["Redis", "Connected"],
              ["Worker", "Heartbeat 9s ago"],
              ["Object storage", "Connected"],
            ].map(([label, value]) => (
              <div key={label}>
                <span>
                  <i /> {label}
                </span>
                <b>{value}</b>
              </div>
            ))}
          </article>
        </section>
      </main>
    </div>
  );
}
