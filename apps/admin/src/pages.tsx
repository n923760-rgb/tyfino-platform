import { useEffect, useMemo, useState, type FormEvent } from "react";
import { api, errorMessage } from "./api";
import { Button, EmptyState, Field, formatDate, licenseName, Loading, Modal, PageHeader, Status } from "./components";
import type { Activation, Admin, AuditLog } from "./types";

type Notify = (message: string, kind?: "success" | "error") => void;

function useData<T>(loader: () => Promise<T>, key = 0) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true; setError(""); setData(null);
    loader().then((value) => { if (active) setData(value); }).catch((reason) => { if (active) setError(errorMessage(reason)); });
    return () => { active = false; };
  }, [key]);
  return { data, error };
}

export function DashboardPage({ role }: { role: Admin["role"] }) {
  const { data, error } = useData(async () => {
    const [codes, settings, health, audit] = await Promise.all([
      api.activations(), api.settings(), api.health(),
      role === "support" ? Promise.resolve({ auditLogs: [] as AuditLog[] }) : api.auditLogs()
    ]);
    return { codes: codes.activationCodes, settings, health, audit: audit.auditLogs };
  });
  if (error) return <EmptyState title="تعذر تحميل لوحة المؤشرات" text={error} />;
  if (!data) return <Loading />;
  const stats = [
    { label: "الأكواد المتاحة", value: data.codes.filter((x) => x.status === "unused").length, note: "جاهزة للتفعيل", tone: "blue" },
    { label: "التراخيص النشطة", value: data.codes.filter((x) => x.status === "active").length, note: "سنة أو مدى الحياة", tone: "violet" },
    { label: "الأجهزة المرتبطة", value: data.codes.filter((x) => x.deviceBound).length, note: "جهاز واحد لكل كود", tone: "cyan" },
    { label: "التجربة المجانية", value: data.settings.trialEnabled ? "مفعّلة" : "متوقفة", note: "7 أيام", tone: data.settings.trialEnabled ? "green" : "gray" }
  ];
  return <>
    <PageHeader title="الرئيسية" description="ملخص حالة تراخيص تطبيق TYFINO." />
    <div className="stats-grid">{stats.map((item) => <article className={`stat-card tone-${item.tone}`} key={item.label}><span>{item.label}</span><strong>{item.value}</strong><small>{item.note}</small></article>)}</div>
    <div className="dashboard-grid">
      <section className="panel"><div className="panel-title"><h2>أحدث الأكواد</h2><span>{data.codes.length}</span></div>
        {data.codes.length ? <div className="simple-list">{data.codes.slice(0, 5).map((item) => <div key={item.id}><div><strong>{item.customerName || item.adminLabel || "بدون اسم"}</strong><small className="ltr">TYF-•••••-•••••-{item.codeSuffix}</small></div><Status value={item.status} /></div>)}</div> : <p className="muted">لم تُنشأ أكواد بعد.</p>}
      </section>
      <section className="panel"><div className="panel-title"><h2>حالة النظام</h2></div><div className="health-list">
        <div><span className="health-dot ok" />Licensing API</div><b>{data.health.status === "ready" ? "جاهز" : data.health.status}</b>
        <div><span className="health-dot ok" />قاعدة البيانات</div><b>{data.health.database === "connected" ? "متصلة" : data.health.database}</b>
        <div><span className="health-dot ok" />آخر نشاط إداري</div><b>{data.audit[0] ? formatDate(data.audit[0].createdAt) : "لا يوجد"}</b>
      </div></section>
    </div>
  </>;
}

export function ActivationsPage({ notify, role }: { notify: Notify; role: Admin["role"] }) {
  const canManage = role === "owner" || role === "admin";
  const [reload, setReload] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [createdCode, setCreatedCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<"all" | Activation["status"]>("all");
  const [resetTarget, setResetTarget] = useState<Activation | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<Activation | null>(null);
  const { data, error } = useData(async () => (await api.activations()).activationCodes, reload);
  const filtered = useMemo(() => (data ?? []).filter((item) => {
    const matchesFilter = filter === "all" || item.status === filter;
    const needle = query.trim().toLowerCase();
    const matchesQuery = !needle || [item.customerName, item.phoneNumber, item.adminLabel, item.externalReference, item.codeSuffix].some((value) => value?.toLowerCase().includes(needle));
    return matchesFilter && matchesQuery;
  }), [data, filter, query]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      const result = await api.createActivation({
        licenseKind: values.licenseKind,
        preActivationExpiresAt: values.preActivationExpiresAt ? new Date(String(values.preActivationExpiresAt)).toISOString() : null,
        customerName: values.customerName || null,
        phoneNumber: values.phoneNumber || null,
        externalReference: values.externalReference || null,
        adminLabel: values.adminLabel || null,
        internalNote: values.internalNote || null
      });
      setCreateOpen(false); setCreatedCode(result.code); setReload((x) => x + 1); notify("تم إنشاء كود التفعيل.");
    } catch (reason) { notify(errorMessage(reason), "error"); }
    finally { setBusy(false); }
  }

  async function revoke() {
    if (!revokeTarget) return; setBusy(true);
    try { await api.revokeActivation(revokeTarget.id); setRevokeTarget(null); setReload((x) => x + 1); notify("تم إلغاء الكود وجلساته."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
    finally { setBusy(false); }
  }

  async function reset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!resetTarget) return; setBusy(true);
    const reason = String(new FormData(event.currentTarget).get("reason") ?? "");
    try { await api.resetDevice(resetTarget.id, reason); setResetTarget(null); setReload((x) => x + 1); notify("تم فصل الجهاز السابق ويمكن التفعيل على جهاز بديل."); }
    catch (cause) { notify(errorMessage(cause), "error"); }
    finally { setBusy(false); }
  }

  return <>
    <PageHeader title="أكواد التفعيل" description="أنشئ تراخيص التطبيق وتابعها وأدر الجهاز المرتبط." action={canManage ? <Button onClick={() => setCreateOpen(true)}>+ كود جديد</Button> : undefined} />
    <div className="toolbar"><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="ابحث بالاسم أو الجوال أو آخر 6 خانات" aria-label="بحث في الأكواد" />
      <select value={filter} onChange={(event) => setFilter(event.target.value as typeof filter)} aria-label="تصفية حسب الحالة"><option value="all">كل الحالات</option><option value="unused">جديد</option><option value="active">نشط</option><option value="revoked">ملغي</option></select></div>
    {error ? <EmptyState title="تعذر تحميل الأكواد" text={error} /> : !data ? <Loading /> : filtered.length === 0 ? <EmptyState title={data.length ? "لا توجد نتائج" : "لا توجد أكواد بعد"} text={data.length ? "غيّر البحث أو الفلتر." : "أنشئ أول كود تفعيل لمدة سنة أو مدى الحياة."} /> :
      <div className="table-wrap"><table><thead><tr><th>الكود</th><th>العميل</th><th>المدة</th><th>الجهاز</th><th>الحالة</th><th>التفعيل / الانتهاء</th><th>الإجراءات</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}>
        <td><strong className="code-cell ltr">••••••-{item.codeSuffix}</strong><small>{item.adminLabel || "—"}</small></td>
        <td><strong>{item.customerName || "بدون اسم"}</strong><small className="ltr">{item.phoneNumber || item.externalReference || "—"}</small></td>
        <td>{licenseName(item.licenseKind)}</td><td><span className={`device-pill ${item.deviceBound ? "bound" : "free"}`}>{item.deviceBound ? "مرتبط" : "متاح"}</span></td>
        <td><Status value={item.status} /></td><td><span>{formatDate(item.activatedAt)}</span><small>{item.grantExpiresAt ? `ينتهي ${formatDate(item.grantExpiresAt)}` : item.licenseKind === "lifetime" ? "بدون انتهاء" : "لم يُفعّل"}</small></td>
        <td><div className="row-actions"><Button variant="ghost" disabled={!item.deviceBound || item.status !== "active"} onClick={() => setResetTarget(item)}>تغيير الجهاز</Button>{canManage && <Button variant="danger" disabled={item.status === "revoked"} onClick={() => setRevokeTarget(item)}>إلغاء</Button>}</div></td>
      </tr>)}</tbody></table></div>}

    {createOpen && <Modal title="إنشاء كود تفعيل" onClose={() => setCreateOpen(false)} width="wide"><form className="form" onSubmit={create}>
      <div className="choice-grid"><label><input type="radio" name="licenseKind" value="one_year" defaultChecked /><span><b>سنة واحدة</b><small>365 يومًا من أول تفعيل</small></span></label><label><input type="radio" name="licenseKind" value="lifetime" /><span><b>مدى الحياة</b><small>بدون تاريخ انتهاء</small></span></label></div>
      <div className="form-row"><Field label="اسم العميل"><input name="customerName" maxLength={120} /></Field><Field label="رقم الجوال"><input className="ltr" name="phoneNumber" inputMode="tel" maxLength={30} /></Field></div>
      <div className="form-row"><Field label="مرجع خارجي"><input name="externalReference" maxLength={200} /></Field><Field label="تصنيف إداري"><input name="adminLabel" maxLength={120} /></Field></div>
      <Field label="انتهاء الكود قبل استخدامه" hint="اختياري؛ لا يؤثر بعد التفعيل."><input className="ltr" type="datetime-local" name="preActivationExpiresAt" /></Field>
      <Field label="ملاحظة داخلية"><textarea name="internalNote" rows={3} maxLength={2000} /></Field>
      <div className="security-note">بيانات العميل داخلية ولا تُرسل إلى تطبيق Android.</div>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>إلغاء</Button><Button type="submit" busy={busy}>إنشاء الكود</Button></div>
    </form></Modal>}
    {createdCode && <Modal title="تم إنشاء الكود" onClose={() => setCreatedCode("")}><div className="code-result"><p>انسخه الآن؛ لن يظهر كاملًا مرة أخرى.</p><strong className="ltr">{createdCode}</strong><Button onClick={() => { void navigator.clipboard.writeText(createdCode); notify("تم نسخ الكود."); }}>نسخ الكود</Button></div></Modal>}
    {resetTarget && <Modal title="تغيير الجهاز المرتبط" onClose={() => setResetTarget(null)}><form className="form" onSubmit={reset}><p className="modal-text">سيتم إلغاء جلسة الجهاز السابق مع الحفاظ على مدة الترخيص.</p><Field label="سبب التغيير"><textarea name="reason" minLength={3} maxLength={500} rows={3} required /></Field><div className="form-actions"><Button type="button" variant="secondary" onClick={() => setResetTarget(null)}>رجوع</Button><Button type="submit" busy={busy}>تأكيد التغيير</Button></div></form></Modal>}
    {revokeTarget && <Modal title="إلغاء كود التفعيل" onClose={() => setRevokeTarget(null)}><div className="confirm-content"><p>سيُلغى الترخيص وجميع جلساته فورًا. لا يمكن استخدام الكود بعد ذلك.</p><div className="form-actions"><Button variant="secondary" onClick={() => setRevokeTarget(null)}>رجوع</Button><Button variant="danger" busy={busy} onClick={() => void revoke()}>إلغاء نهائي</Button></div></div></Modal>}
  </>;
}

export function SettingsPage({ notify, role }: { notify: Notify; role: Admin["role"] }) {
  const canManage = role === "owner" || role === "admin";
  const [reload, setReload] = useState(0);
  const [busy, setBusy] = useState(false);
  const { data, error } = useData(() => api.settings(), reload);
  async function toggle() {
    if (!data) return; setBusy(true);
    try { await api.updateSettings(!data.trialEnabled); setReload((x) => x + 1); notify(!data.trialEnabled ? "تم تفعيل التجربة المجانية." : "تم إيقاف التجربة المجانية."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
    finally { setBusy(false); }
  }
  return <><PageHeader title="إعدادات التطبيق" description="التحكم في خيارات الترخيص العامة." />
    {error ? <EmptyState title="تعذر تحميل الإعدادات" text={error} /> : !data ? <Loading /> : <section className="settings-panel"><div className="setting-row"><div><h2>التجربة المجانية لمدة 7 أيام</h2><p>عند تفعيلها يستطيع المستخدم بدء 168 ساعة من التطبيق. تثبيت التطبيق أو تسجيل Xtream لا يبدأ التجربة تلقائيًا.</p></div><button className={`switch ${data.trialEnabled ? "on" : ""}`} role="switch" aria-checked={data.trialEnabled} aria-label="التجربة المجانية" disabled={busy || !canManage} title={canManage ? undefined : "متاح للمالك والمدير فقط"} onClick={() => void toggle()}><span /></button></div>
      <div className="setting-facts"><div><span>عدد الأجهزة</span><b>جهاز واحد لكل كود</b></div><div><span>العمل دون اتصال</span><b>حتى 72 ساعة</b></div><div><span>تحديث الترخيص</span><b>كل 12 ساعة عند الاستخدام</b></div></div></section>}
  </>;
}

export function AuditPage() {
  const { data, error } = useData(async () => (await api.auditLogs()).auditLogs);
  const action: Record<string, string> = { "admin.login": "تسجيل دخول", "admin.logout": "تسجيل خروج", "activation.create": "إنشاء كود", "activation.revoke": "إلغاء كود", "activation.reset_device": "تغيير الجهاز", "settings.update": "تحديث الإعدادات" };
  return <><PageHeader title="سجل العمليات" description="أثر تدقيقي للعمليات الإدارية الحساسة." />
    {error ? <EmptyState title="تعذر تحميل السجل" text={error} /> : !data ? <Loading /> : data.length === 0 ? <EmptyState title="السجل فارغ" text="ستظهر العمليات الإدارية هنا تلقائيًا." /> : <div className="timeline">{data.map((item: AuditLog) => <article key={item.id}><span className="timeline-dot" /><div><strong>{action[item.action] ?? item.action}</strong><p>{item.adminEmail || "النظام"} · {item.ipAddress || "—"}</p></div><time>{formatDate(item.createdAt)}</time></article>)}</div>}
  </>;
}
