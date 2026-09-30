import { useEffect, useId, useRef, type ReactNode } from "react";

export function Button({ children, variant = "primary", busy, ...props }: {
  children: ReactNode;
  variant?: "primary" | "secondary" | "danger" | "ghost";
  busy?: boolean;
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button {...props} className={`button button-${variant}`} disabled={busy || props.disabled}>
    {busy ? "جارٍ التنفيذ…" : children}
  </button>;
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>;
}

export function Modal({ title, children, onClose, width = "normal" }: {
  title: string; children: ReactNode; onClose: () => void; width?: "normal" | "wide";
}) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialog.showModal();
    closeRef.current?.focus();
    return () => {
      if (dialog.open) dialog.close();
      if (opener?.isConnected) opener.focus();
    };
  }, []);
  return <dialog ref={dialogRef} className={`modal modal-${width}`} aria-labelledby={titleId}
    onCancel={(event) => { event.preventDefault(); onClose(); }}
    onMouseDown={(event) => {
      if (event.target !== event.currentTarget) return;
      const bounds = event.currentTarget.getBoundingClientRect();
      if (event.clientX < bounds.left || event.clientX > bounds.right ||
          event.clientY < bounds.top || event.clientY > bounds.bottom) onClose();
    }}>
    <header><h2 id={titleId}>{title}</h2><button ref={closeRef} type="button" className="icon-button" onClick={onClose} aria-label="إغلاق">×</button></header>
    <div className="modal-content">{children}</div>
  </dialog>;
}

const statusText: Record<string, string> = { active: "نشط", unused: "جديد", revoked: "ملغي" };
export function Status({ value }: { value: string }) {
  return <span className={`status status-${value}`}>{statusText[value] ?? value}</span>;
}

export function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="empty"><div className="empty-mark" aria-hidden="true">T</div><h3>{title}</h3><p>{text}</p></div>;
}

export function PageHeader({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="page-header"><div><h1>{title}</h1><p>{description}</p></div>{action}</div>;
}

export function Loading({ label = "جارٍ التحميل" }: { label?: string }) {
  return <div className="loading" role="status" aria-label={label}><span /><span /><span /></div>;
}

export function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("ar-SA", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

export function licenseName(value: "one_year" | "lifetime") {
  return value === "one_year" ? "سنة واحدة" : "مدى الحياة";
}
