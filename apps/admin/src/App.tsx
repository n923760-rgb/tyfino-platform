import { useEffect, useState, type FormEvent } from "react";
import { api, errorMessage } from "./api";
import { Button, Field, Loading } from "./components";
import { ActivationsPage, AuditPage, DashboardPage, SettingsPage } from "./pages";
import type { Admin } from "./types";

type Page = "dashboard" | "activations" | "settings" | "audit";
const pages: { id: Page; label: string; short: string; icon: string }[] = [
  { id: "dashboard", label: "الرئيسية", short: "الرئيسية", icon: "⌂" },
  { id: "activations", label: "أكواد التفعيل", short: "الأكواد", icon: "◇" },
  { id: "settings", label: "إعدادات التطبيق", short: "الإعدادات", icon: "⚙" },
  { id: "audit", label: "سجل العمليات", short: "السجل", icon: "≡" }
];

function currentPage(): Page {
  const value = window.location.hash.replace("#", "") as Page;
  return pages.some((item) => item.id === value) ? value : "dashboard";
}

function Brand({ compact = false }: { compact?: boolean }) {
  return <div className={`logo ${compact ? "logo-compact" : ""}`}><div>T</div><span>TYFINO<small>CONTROL</small></span></div>;
}

function Login({ onLogin }: { onLogin: (admin: Admin) => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [challengeToken, setChallengeToken] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError("");
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      const result = await api.login(String(values.email), String(values.password));
      if ("admin" in result) onLogin(result.admin);
      else setChallengeToken(result.challengeToken);
    }
    catch (reason) { setError(errorMessage(reason)); }
    finally { setBusy(false); }
  }
  async function verify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError("");
    const code = String(new FormData(event.currentTarget).get("code") ?? "").replace(/\s/g, "");
    try { onLogin((await api.verifyTotp(challengeToken, code)).admin); }
    catch (reason) { setError(errorMessage(reason)); }
    finally { setBusy(false); }
  }
  return <main className="login-screen">
    <section className="login-brand" aria-label="TYFINO Control"><div className="brand-orb">T</div><p>TYFINO <b>CONTROL</b></p><h1>التراخيص.<br />بوضوح كامل.</h1><small>لوحة خاصة لإدارة تفعيل تطبيق TYFINO فقط.</small></section>
    <section className="login-card"><Brand /><div className="login-copy"><span className="eyebrow">لوحة الإدارة</span><h2>{challengeToken ? "التحقق بخطوتين" : "تسجيل الدخول"}</h2><p>{challengeToken ? "أدخل الرمز الحالي من تطبيق المصادقة الخاص بالمالك." : "أدخل بيانات حسابك الإداري."}</p></div>
      {challengeToken ? <form className="form" onSubmit={verify}>
        <Field label="رمز التحقق"><input className="ltr" type="text" name="code" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" minLength={6} maxLength={6} required autoFocus /></Field>
        {error && <div className="form-error" role="alert">{error}</div>}<Button type="submit" busy={busy}>تحقق ودخول</Button>
        <Button type="button" variant="secondary" disabled={busy} onClick={() => { setChallengeToken(""); setError(""); }}>العودة لتسجيل الدخول</Button>
      </form> : <form className="form" onSubmit={submit}><Field label="البريد الإلكتروني"><input className="ltr" type="email" name="email" autoComplete="username" required autoFocus /></Field>
        <Field label="كلمة المرور"><input className="ltr" type="password" name="password" autoComplete="current-password" required /></Field>
        {error && <div className="form-error" role="alert">{error}</div>}<Button type="submit" busy={busy}>تسجيل الدخول</Button></form>}
      <footer><span className="health-dot ok" /> جلسة إدارية محمية</footer></section>
  </main>;
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
  const availablePages = admin.role === "support" ? pages.filter((item) => item.id !== "audit") : pages;
  const safePage = availablePages.some((item) => item.id === page) ? page : "dashboard";
  const content = safePage === "dashboard" ? <DashboardPage role={admin.role} /> : safePage === "activations" ? <ActivationsPage notify={notify} role={admin.role} /> : safePage === "settings" ? <SettingsPage notify={notify} role={admin.role} /> : <AuditPage />;
  return <div className="app-shell">
    <aside className="sidebar"><Brand /><nav aria-label="التنقل الرئيسي">{availablePages.map((item) => <button className={safePage === item.id ? "active" : ""} onClick={() => navigate(item.id)} key={item.id}><i aria-hidden="true">{item.icon}</i>{item.label}</button>)}</nav>
      <div className="sidebar-user"><div>{admin.email.slice(0, 1).toUpperCase()}</div><span><strong>{admin.email}</strong><small>{admin.role === "owner" ? "المالك" : admin.role === "admin" ? "مدير" : "دعم"}</small></span><button onClick={onLogout} aria-label="تسجيل الخروج">↗</button></div>
    </aside>
    <main className="content"><header className="mobile-header"><Brand compact /><button onClick={onLogout}>خروج</button></header>
      <nav className="mobile-nav" aria-label="التنقل الرئيسي">{availablePages.map((item) => <button className={safePage === item.id ? "active" : ""} onClick={() => navigate(item.id)} key={item.id}>{item.short}</button>)}</nav>
      <div className="content-inner">{content}</div></main>
    {toast && <div className={`toast toast-${toast.kind}`} role="status"><span>{toast.kind === "success" ? "✓" : "!"}</span>{toast.message}</div>}
  </div>;
}

export default function App() {
  const [state, setState] = useState<{ loading: boolean; admin: Admin | null }>({ loading: true, admin: null });
  useEffect(() => { api.me().then(({ admin }) => setState({ loading: false, admin })).catch(() => setState({ loading: false, admin: null })); }, []);
  if (state.loading) return <div className="splash"><div className="brand-orb">T</div><Loading /></div>;
  if (!state.admin) return <Login onLogin={(admin) => setState({ loading: false, admin })} />;
  return <Shell admin={state.admin} onLogout={() => { api.logout().catch(() => undefined).finally(() => setState({ loading: false, admin: null })); }} />;
}
