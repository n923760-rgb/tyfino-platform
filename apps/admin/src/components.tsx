import type { ReactNode } from "react";

export function Button({ children, variant = "primary", busy, ...props }: {
  children: ReactNode;
  variant?: "primary" | "secondary" | "danger" | "ghost";
  busy?: boolean;
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button className={`button button-${variant}`} disabled={busy || props.disabled} {...props}>
    {busy ? "جارٍ التنفيذ…" : children}
  </button>;
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>;
}

export function Modal({ title, children, onClose, width = "normal" }: {
  title: string; children: ReactNode; onClose: () => void; width?: "normal" | "wide";
}) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
    if (event.target === event.currentTarget) onClose();
  }}>
    <section className={`modal modal-${width}`} role="dialog" aria-modal="true" aria-label={title}>
      <header><h2>{title}</h2><button className="icon-button" onClick={onClose} aria-label="إغلاق">×</button></header>
      <div className="modal-content">{children}</div>
    </section>
  </div>;
}

const statusText: Record<string, string> = {
  active: "نشط", suspended: "موقوف", archived: "مؤرشف", unused: "جديد",
  revoked: "ملغي", expired: "منتهي", blocked: "محظور"
};

export function Status({ value }: { value: string }) {
  return <span className={`status status-${value}`}>{statusText[value] ?? value}</span>;
}

export function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="empty"><div className="empty-mark">T</div><h3>{title}</h3><p>{text}</p></div>;
}

export function PageHeader({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="page-header"><div><h1>{title}</h1><p>{description}</p></div>{action}</div>;
}

export function Loading() {
  return <div className="loading"><span /><span /><span /></div>;
}

export function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("ar-SA", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

export function protocolName(value: string) {
  return value === "xtream" ? "Xtream Codes" : value === "m3u" ? "M3U" : "Stalker / MAC";
}
