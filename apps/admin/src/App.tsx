import { useEffect, useState, type FormEvent } from "react";
import { api, errorMessage } from "./api";
import { Button, Field, Loading } from "./components";
import { AccountsPage, ActivationsPage, AuditPage, CustomersPage, DashboardPage, DevicesPage, HostsPage } from "./pages";
import type { Admin } from "./types";

type Page = "dashboard" | "customers" | "hosts" | "accounts" | "activations" | "devices" | "audit";

const pages: { id: Page; label: string; short: string }[] = [
  { id: "dashboard", label: "نظرة عامة", short: "الرئيسية" },
  { id: "customers", label: "العملاء", short: "العملاء" },
  { id: "hosts", label: "الهوستات", short: "الهوستات" },
  { id: "accounts", label: "اشتراكات المزود", short: "الاشتراكات" },
  { id: "activations", label: "أكواد التفعيل", short: "الأكواد" },
  { id: "devices", label: "الأجهزة", short: "الأجهزة" },
  { id: "audit", label: "سجل العمليات", short: "السجل" }
];

function currentPage(): Page {
  const value = window.location.hash.replace("#", "") as Page;
  return pages.some((item) => item.id === value) ? value : "dashboard";
}

function Login({ onLogin }: { onLogin: (admin: Admin) => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError("");
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try { onLogin((await api.login(String(values.email), String(values.password))).admin); }
    catch (reason) { setError(errorMessage(reason)); } finally { setBusy(false); }
  }
  return <main className="login-screen"><section className="login-brand"><div className="brand-orb"><span>T</span></div><p>TYFINO <b>by Techify</b></p><h1>إدارة أبسط.<br />تشغيل أسرع.</h1><small>منصة خاصة لإدارة عملاء التطبيق والأكواد والأجهزة.</small></section>
    <section className="login-card"><div className="mobile-brand">TYFINO <span>CONTROL</span></div><div><span className="eyebrow">لوحة خاصة</span><h2>تسجيل الدخول</h2><p>استخدم حساب إدارة Techify.</p></div>
      <form className="form" onSubmit={submit}><Field label="البريد الإلكتروني"><input className="ltr" type="email" name="email" autoComplete="username" required /></Field>
        <Field label="كلمة المرور"><input className="ltr" type="password" name="password" autoComplete="current-password" required /></Field>
        {error && <div className="form-error">{error}</div>}<Button type="submit" busy={busy}>دخول آمن</Button></form>
      <footer><span className="health-dot ok" /> اتصال مشفّر ومحمي</footer></section></main>;
}

function Shell({ admin, onLogout }: { admin: Admin; onLogout: () => void }) {
  const [page, setPage] = useState<Page>(currentPage());
  const [toast, setToast] = useState<{ message: string; kind: "success" | "error" } | null>(null);
  useEffect(() => {
    const listener = () => setPage(currentPage()); window.addEventListener("hashchange", listener);
    return () => window.removeEventListener("hashchange", listener);
  }, []);
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(null), 3500); return () => window.clearTimeout(timer); }, [toast]);
  function notify(message: string, kind: "success" | "error" = "success") { setToast({ message, kind }); }
  function navigate(value: Page) { window.location.hash = value; setPage(value); }
  const content = page === "dashboard" ? <DashboardPage /> : page === "customers" ? <CustomersPage notify={notify} /> :
    page === "hosts" ? <HostsPage notify={notify} /> : page === "accounts" ? <AccountsPage notify={notify} /> :
    page === "activations" ? <ActivationsPage notify={notify} /> : page === "devices" ? <DevicesPage notify={notify} /> : <AuditPage />;
  return <div className="app-shell">
    <aside className="sidebar"><div className="logo"><div>T</div><span>TYFINO<small>CONTROL</small></span></div>
      <nav>{pages.map((item, index) => <button className={page === item.id ? "active" : ""} onClick={() => navigate(item.id)} key={item.id}><i>{index + 1}</i>{item.label}</button>)}</nav>
      <div className="sidebar-user"><div>{admin.email.slice(0, 1).toUpperCase()}</div><span><strong>{admin.email}</strong><small>{admin.role === "owner" ? "المالك" : admin.role === "admin" ? "مدير" : "دعم"}</small></span><button onClick={onLogout} title="تسجيل الخروج">↗</button></div>
    </aside>
    <main className="content"><header className="mobile-header"><div className="logo"><div>T</div><span>TYFINO<small>CONTROL</small></span></div><button onClick={onLogout}>خروج</button></header>
      <div className="mobile-nav">{pages.map((item) => <button className={page === item.id ? "active" : ""} onClick={() => navigate(item.id)} key={item.id}>{item.short}</button>)}</div>
      <div className="content-inner">{content}</div></main>
    {toast && <div className={`toast toast-${toast.kind}`}><span>{toast.kind === "success" ? "✓" : "!"}</span>{toast.message}</div>}
  </div>;
}

export default function App() {
  const [state, setState] = useState<{ loading: boolean; admin: Admin | null }>({ loading: true, admin: null });
  useEffect(() => { api.me().then(({ admin }) => setState({ loading: false, admin })).catch(() => setState({ loading: false, admin: null })); }, []);
  if (state.loading) return <div className="splash"><div className="brand-orb"><span>T</span></div><Loading /></div>;
  if (!state.admin) return <Login onLogin={(admin) => setState({ loading: false, admin })} />;
  return <Shell admin={state.admin} onLogout={() => { api.logout().catch(() => undefined).finally(() => setState({ loading: false, admin: null })); }} />;
}
