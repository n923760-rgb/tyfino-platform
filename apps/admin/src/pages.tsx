import { useEffect, useMemo, useState, type FormEvent } from "react";
import { api, errorMessage } from "./api";
import { Button, EmptyState, Field, formatDate, Loading, Modal, PageHeader, protocolName, Status } from "./components";
import type { Activation, AuditLog, Customer, Device, Host, ProviderAccount } from "./types";

type Notify = (message: string, kind?: "success" | "error") => void;

function useData<T>(loader: () => Promise<T>, key = 0) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    setError("");
    loader().then((value) => active && setData(value)).catch((reason) => active && setError(errorMessage(reason)));
    return () => { active = false; };
  }, [key]);
  return { data, error };
}

export function DashboardPage() {
  const { data, error } = useData(async () => {
    const [customers, hosts, accounts, activations, devices] = await Promise.all([
      api.customers(), api.hosts(), api.accounts(), api.activations(), api.devices()
    ]);
    return {
      customers: customers.customers,
      hosts: hosts.hosts,
      accounts: accounts.providerAccounts,
      activations: activations.activationCodes,
      devices: devices.devices
    };
  });
  if (error) return <EmptyState title="تعذر تحميل لوحة المؤشرات" text={error} />;
  if (!data) return <Loading />;
  const stats = [
    ["العملاء النشطون", data.customers.filter((x) => x.status === "active").length, "عميل"],
    ["أكواد التفعيل", data.activations.filter((x) => ["unused", "active"].includes(x.status)).length, "كود"],
    ["الأجهزة النشطة", data.devices.filter((x) => x.status === "active").length, "جهاز"],
    ["اشتراكات المزود", data.accounts.filter((x) => x.isActive).length, "اشتراك"]
  ];
  const expiring = data.activations.filter((x) => x.expiresAt && new Date(x.expiresAt).getTime() < Date.now() + 7 * 86400000 && new Date(x.expiresAt) > new Date());
  return <>
    <PageHeader title="نظرة عامة" description="حالة منصة TYFINO واشتراكات عملاء Techify." />
    <div className="stats-grid">{stats.map(([label, value, unit]) => <article className="stat-card" key={String(label)}>
      <span>{label}</span><strong>{value}</strong><small>{unit}</small>
    </article>)}</div>
    <div className="dashboard-grid">
      <section className="panel"><div className="panel-title"><h2>تنبيهات قريبة</h2><span>{expiring.length}</span></div>
        {expiring.length ? <div className="simple-list">{expiring.slice(0, 6).map((item) => <div key={item.id}>
          <div><strong>{item.customerName}</strong><small>ينتهي {formatDate(item.expiresAt)}</small></div><Status value={item.status} />
        </div>)}</div> : <p className="muted">لا توجد أكواد تنتهي خلال 7 أيام.</p>}
      </section>
      <section className="panel"><div className="panel-title"><h2>حالة الربط</h2></div>
        <div className="health-list"><div><span className="health-dot ok" />Backend API</div><b>متصل</b>
          <div><span className="health-dot ok" />Provider hosts</div><b>{data.hosts.filter((x) => x.isActive).length} نشط</b>
          <div><span className="health-dot ok" />Database</div><b>جاهزة</b></div>
      </section>
    </div>
  </>;
}

export function CustomersPage({ notify }: { notify: Notify }) {
  const [reload, setReload] = useState(0);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const { data, error } = useData(async () => (await api.customers()).customers, reload);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      await api.createCustomer({ displayName: values.displayName, phone: values.phone || null, whatsapp: values.whatsapp || null, notes: values.notes || null });
      setOpen(false); setReload((x) => x + 1); notify("تمت إضافة العميل.");
    } catch (reason) { notify(errorMessage(reason), "error"); } finally { setBusy(false); }
  }
  async function status(customer: Customer, value: Customer["status"]) {
    try { await api.updateCustomer(customer.id, { status: value }); setReload((x) => x + 1); notify("تم تحديث حالة العميل."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
  }
  return <>
    <PageHeader title="العملاء" description="سجل عملاء Techify وربط حالاتهم." action={<Button onClick={() => setOpen(true)}>+ إضافة عميل</Button>} />
    {error ? <EmptyState title="تعذر تحميل العملاء" text={error} /> : !data ? <Loading /> : data.length === 0 ?
      <EmptyState title="لا يوجد عملاء بعد" text="أضف أول عميل لربطه باشتراك المزود وكود TYF." /> :
      <div className="table-wrap"><table><thead><tr><th>العميل</th><th>التواصل</th><th>الحالة</th><th>تاريخ الإضافة</th><th /></tr></thead><tbody>
        {data.map((item) => <tr key={item.id}><td><strong>{item.displayName}</strong><small>{item.notes || "بدون ملاحظات"}</small></td>
          <td>{item.whatsapp || item.phone || "—"}</td><td><Status value={item.status} /></td><td>{formatDate(item.createdAt)}</td>
          <td><select className="compact-select" value={item.status} onChange={(e) => void status(item, e.target.value as Customer["status"])}>
            <option value="active">نشط</option><option value="suspended">موقوف</option><option value="archived">مؤرشف</option>
          </select></td></tr>)}</tbody></table></div>}
    {open && <Modal title="إضافة عميل جديد" onClose={() => setOpen(false)}><form className="form" onSubmit={submit}>
      <Field label="اسم العميل"><input name="displayName" required minLength={2} /></Field>
      <div className="form-row"><Field label="رقم الجوال"><input name="phone" inputMode="tel" /></Field><Field label="واتساب"><input name="whatsapp" inputMode="tel" /></Field></div>
      <Field label="ملاحظات"><textarea name="notes" rows={3} /></Field>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={() => setOpen(false)}>إلغاء</Button><Button type="submit" busy={busy}>حفظ العميل</Button></div>
    </form></Modal>}
  </>;
}

export function HostsPage({ notify }: { notify: Notify }) {
  const [reload, setReload] = useState(0); const [open, setOpen] = useState(false); const [busy, setBusy] = useState(false);
  const { data, error } = useData(async () => (await api.hosts()).hosts, reload);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); const values = Object.fromEntries(new FormData(event.currentTarget));
    try { await api.createHost({ label: values.label, protocol: values.protocol, baseUrl: values.baseUrl }); setOpen(false); setReload((x) => x + 1); notify("تمت إضافة الهوست."); }
    catch (reason) { notify(errorMessage(reason), "error"); } finally { setBusy(false); }
  }
  async function toggle(item: Host) {
    try { await api.updateHost(item.id, { isActive: !item.isActive }); setReload((x) => x + 1); notify("تم تحديث الهوست."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
  }
  return <>
    <PageHeader title="الهوستات" description="عناوين مزودي Xtream وM3U وStalker." action={<Button onClick={() => setOpen(true)}>+ إضافة هوست</Button>} />
    {error ? <EmptyState title="تعذر تحميل الهوستات" text={error} /> : !data ? <Loading /> : data.length === 0 ? <EmptyState title="لا توجد هوستات" text="أضف عنوان مزودك قبل إنشاء اشتراك للعميل." /> :
      <div className="cards-grid">{data.map((item) => <article className="item-card" key={item.id}><div className="item-card-head"><span className="protocol">{protocolName(item.protocol)}</span><Status value={item.isActive ? "active" : "suspended"} /></div>
        <h3>{item.label}</h3><p className="ltr url">{item.baseUrl}</p><footer><small>أضيف {formatDate(item.createdAt)}</small><Button variant="ghost" onClick={() => void toggle(item)}>{item.isActive ? "تعطيل" : "تفعيل"}</Button></footer></article>)}</div>}
    {open && <Modal title="إضافة هوست" onClose={() => setOpen(false)}><form className="form" onSubmit={submit}>
      <Field label="اسم تعريفي"><input name="label" placeholder="Main Provider" required /></Field>
      <Field label="نوع الاتصال"><select name="protocol"><option value="xtream">Xtream Codes</option><option value="m3u">M3U</option><option value="stalker">Stalker / MAC</option></select></Field>
      <Field label="رابط الهوست" hint="مثال: https://provider.example.com"><input className="ltr" name="baseUrl" type="url" required /></Field>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={() => setOpen(false)}>إلغاء</Button><Button type="submit" busy={busy}>حفظ الهوست</Button></div>
    </form></Modal>}
  </>;
}

export function AccountsPage({ notify }: { notify: Notify }) {
  const [reload, setReload] = useState(0); const [open, setOpen] = useState(false); const [busy, setBusy] = useState(false); const [hostId, setHostId] = useState("");
  const { data, error } = useData(async () => {
    const [accounts, customers, hosts] = await Promise.all([api.accounts(), api.customers(), api.hosts()]);
    return { accounts: accounts.providerAccounts, customers: customers.customers, hosts: hosts.hosts.filter((x) => x.isActive) };
  }, reload);
  const selectedHost = data?.hosts.find((x) => x.id === hostId);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      await api.createAccount({ customerId: values.customerId, hostId: values.hostId, externalReference: values.externalReference || null,
        username: values.username || null, password: values.password || null, playlistUrl: values.playlistUrl || null,
        portalData: values.portalData || null, expiresAt: values.expiresAt ? new Date(String(values.expiresAt)).toISOString() : null,
        maxConnections: values.maxConnections ? Number(values.maxConnections) : null });
      setOpen(false); setHostId(""); setReload((x) => x + 1); notify("تم حفظ الاشتراك وتشفير بياناته.");
    } catch (reason) { notify(errorMessage(reason), "error"); } finally { setBusy(false); }
  }
  async function toggle(item: ProviderAccount) {
    try { await api.updateAccount(item.id, { isActive: !item.isActive }); setReload((x) => x + 1); notify("تم تحديث الاشتراك."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
  }
  return <>
    <PageHeader title="اشتراكات المزود" description="ربط بيانات مزود IPTV بكل عميل بشكل مشفّر." action={<Button onClick={() => setOpen(true)}>+ ربط اشتراك</Button>} />
    {error ? <EmptyState title="تعذر تحميل الاشتراكات" text={error} /> : !data ? <Loading /> : data.accounts.length === 0 ? <EmptyState title="لا توجد اشتراكات مربوطة" text="اربط حساب المزود بالعميل، ثم أنشئ له كود TYF." /> :
      <div className="table-wrap"><table><thead><tr><th>العميل</th><th>الهوست</th><th>النوع</th><th>الانتهاء</th><th>الحالة</th><th /></tr></thead><tbody>{data.accounts.map((item) =>
        <tr key={item.id}><td><strong>{item.customerName}</strong><small>{item.externalReference || "بدون مرجع"}</small></td><td>{item.hostLabel}</td><td>{protocolName(item.protocol)}</td><td>{formatDate(item.expiresAt)}</td><td><Status value={item.isActive ? "active" : "suspended"} /></td>
          <td><Button variant="ghost" onClick={() => void toggle(item)}>{item.isActive ? "تعطيل" : "تفعيل"}</Button></td></tr>)}</tbody></table></div>}
    {open && <Modal title="ربط اشتراك مزود" width="wide" onClose={() => setOpen(false)}><form className="form" onSubmit={submit}>
      <div className="form-row"><Field label="العميل"><select name="customerId" required defaultValue=""><option value="" disabled>اختر العميل</option>{data?.customers.filter((x) => x.status === "active").map((x) => <option value={x.id} key={x.id}>{x.displayName}</option>)}</select></Field>
        <Field label="الهوست"><select name="hostId" required value={hostId} onChange={(e) => setHostId(e.target.value)}><option value="" disabled>اختر الهوست</option>{data?.hosts.map((x) => <option value={x.id} key={x.id}>{x.label} — {protocolName(x.protocol)}</option>)}</select></Field></div>
      {selectedHost?.protocol === "xtream" && <div className="form-row"><Field label="اسم المستخدم"><input className="ltr" name="username" required autoComplete="off" /></Field><Field label="كلمة مرور الاشتراك"><input className="ltr" name="password" required autoComplete="new-password" /></Field></div>}
      {selectedHost?.protocol === "m3u" && <Field label="رابط M3U"><input className="ltr" name="playlistUrl" type="url" required /></Field>}
      {selectedHost?.protocol === "stalker" && <Field label="بيانات MAC / Portal"><textarea className="ltr" name="portalData" rows={3} required /></Field>}
      <div className="form-row"><Field label="تاريخ الانتهاء"><input name="expiresAt" type="datetime-local" /></Field><Field label="عدد الاتصالات"><input name="maxConnections" type="number" min="1" max="100" /></Field></div>
      <Field label="مرجع داخلي"><input name="externalReference" placeholder="رقم الطلب أو ملاحظة مختصرة" /></Field>
      <div className="security-note">بيانات الدخول تُشفّر قبل تخزينها ولا تظهر في قوائم الإدارة.</div>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={() => setOpen(false)}>إلغاء</Button><Button type="submit" busy={busy} disabled={!selectedHost}>حفظ وتشفير</Button></div>
    </form></Modal>}
  </>;
}

export function ActivationsPage({ notify }: { notify: Notify }) {
  const [reload, setReload] = useState(0); const [open, setOpen] = useState(false); const [busy, setBusy] = useState(false); const [customerId, setCustomerId] = useState(""); const [code, setCode] = useState("");
  const { data, error } = useData(async () => {
    const [activations, customers, accounts] = await Promise.all([api.activations(), api.customers(), api.accounts()]);
    return { activations: activations.activationCodes, customers: customers.customers, accounts: accounts.providerAccounts.filter((x) => x.isActive) };
  }, reload);
  const matchingAccounts = useMemo(() => data?.accounts.filter((x) => x.customerId === customerId) ?? [], [data, customerId]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      const result = await api.createActivation({ customerId: values.customerId, providerAccountId: values.providerAccountId,
        deviceLimit: Number(values.deviceLimit), expiresAt: values.expiresAt ? new Date(String(values.expiresAt)).toISOString() : null });
      setOpen(false); setCode(result.code); setReload((x) => x + 1); notify("تم إنشاء كود التفعيل.");
    } catch (reason) { notify(errorMessage(reason), "error"); } finally { setBusy(false); }
  }
  async function revoke(item: Activation) {
    if (!window.confirm(`إلغاء كود العميل ${item.customerName}؟ ستتوقف جلسات أجهزته.`)) return;
    try { await api.revokeActivation(item.id); setReload((x) => x + 1); notify("تم إلغاء الكود وجلساته."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
  }
  return <>
    <PageHeader title="أكواد التفعيل" description="أكواد TYF المستقلة عن اسم مستخدم المزود." action={<Button onClick={() => setOpen(true)}>+ إنشاء كود</Button>} />
    {error ? <EmptyState title="تعذر تحميل الأكواد" text={error} /> : !data ? <Loading /> : data.activations.length === 0 ? <EmptyState title="لا توجد أكواد" text="أنشئ كودًا بعد ربط اشتراك المزود بالعميل." /> :
      <div className="table-wrap"><table><thead><tr><th>العميل</th><th>الكود</th><th>الأجهزة</th><th>الانتهاء</th><th>الحالة</th><th /></tr></thead><tbody>{data.activations.map((item) =>
        <tr key={item.id}><td><strong>{item.customerName}</strong></td><td className="ltr code-cell">TYF-••••-{item.codeSuffix}</td><td>{item.deviceCount} / {item.deviceLimit}</td><td>{formatDate(item.expiresAt)}</td><td><Status value={item.status} /></td>
          <td>{!(["revoked", "expired"].includes(item.status)) && <Button variant="danger" onClick={() => void revoke(item)}>إلغاء</Button>}</td></tr>)}</tbody></table></div>}
    {open && <Modal title="إنشاء كود TYF" onClose={() => setOpen(false)}><form className="form" onSubmit={submit}>
      <Field label="العميل"><select name="customerId" value={customerId} onChange={(e) => setCustomerId(e.target.value)} required><option value="" disabled>اختر العميل</option>{data?.customers.filter((x) => x.status === "active").map((x) => <option key={x.id} value={x.id}>{x.displayName}</option>)}</select></Field>
      <Field label="اشتراك المزود"><select name="providerAccountId" required defaultValue=""><option value="" disabled>اختر الاشتراك</option>{matchingAccounts.map((x) => <option key={x.id} value={x.id}>{x.hostLabel} — {protocolName(x.protocol)}</option>)}</select></Field>
      <div className="form-row"><Field label="عدد الأجهزة"><input name="deviceLimit" type="number" min="1" max="10" defaultValue="1" required /></Field><Field label="انتهاء الكود"><input name="expiresAt" type="datetime-local" /></Field></div>
      <div className="form-actions"><Button type="button" variant="secondary" onClick={() => setOpen(false)}>إلغاء</Button><Button type="submit" busy={busy}>إنشاء الكود</Button></div>
    </form></Modal>}
    {code && <Modal title="تم إنشاء كود التفعيل" onClose={() => setCode("")}><div className="code-result"><p>انسخ الكود الآن؛ لن يظهر كاملًا مرة أخرى.</p><strong className="ltr">{code}</strong><Button onClick={() => { void navigator.clipboard.writeText(code); notify("تم نسخ الكود."); }}>نسخ الكود</Button></div></Modal>}
  </>;
}

export function DevicesPage({ notify }: { notify: Notify }) {
  const [reload, setReload] = useState(0); const { data, error } = useData(async () => (await api.devices()).devices, reload);
  async function toggle(item: Device) {
    const status = item.status === "active" ? "blocked" : "active";
    try { await api.updateDevice(item.id, status); setReload((x) => x + 1); notify(status === "blocked" ? "تم حظر الجهاز وإلغاء جلسته." : "تمت إعادة تفعيل الجهاز."); }
    catch (reason) { notify(errorMessage(reason), "error"); }
  }
  return <>
    <PageHeader title="الأجهزة" description="الأجهزة المسجلة وآخر اتصال وحدود الاستخدام." />
    {error ? <EmptyState title="تعذر تحميل الأجهزة" text={error} /> : !data ? <Loading /> : data.length === 0 ? <EmptyState title="لا توجد أجهزة مسجلة" text="ستظهر الأجهزة هنا بعد إدخال العميل كود التفعيل." /> :
      <div className="table-wrap"><table><thead><tr><th>العميل</th><th>الجهاز</th><th>النظام</th><th>آخر اتصال</th><th>الحالة</th><th /></tr></thead><tbody>{data.map((item) =>
        <tr key={item.id}><td><strong>{item.customerName}</strong><small className="ltr">••••-{item.codeSuffix}</small></td><td>{item.model || "غير محدد"}<small>v{item.appVersion}</small></td><td>{item.platform}</td><td>{formatDate(item.lastSeenAt)}</td><td><Status value={item.status} /></td>
          <td><Button variant={item.status === "active" ? "danger" : "ghost"} onClick={() => void toggle(item)}>{item.status === "active" ? "حظر" : "استعادة"}</Button></td></tr>)}</tbody></table></div>}
  </>;
}

export function AuditPage() {
  const { data, error } = useData(async () => (await api.auditLogs()).auditLogs);
  const action: Record<string, string> = { "admin.login": "تسجيل دخول", "admin.logout": "تسجيل خروج", "customer.create": "إضافة عميل", "customer.update": "تحديث عميل", "host.create": "إضافة هوست", "host.update": "تحديث هوست", "provider_account.create": "ربط اشتراك", "provider_account.update": "تحديث اشتراك", "activation.create": "إنشاء كود", "activation.revoke": "إلغاء كود", "device.status": "تغيير جهاز" };
  return <>
    <PageHeader title="سجل العمليات" description="أثر تدقيقي للعمليات الإدارية الحساسة." />
    {error ? <EmptyState title="تعذر تحميل السجل" text={error} /> : !data ? <Loading /> : data.length === 0 ? <EmptyState title="السجل فارغ" text="ستظهر العمليات الإدارية هنا تلقائيًا." /> :
      <div className="timeline">{data.map((item: AuditLog) => <article key={item.id}><span className="timeline-dot" /><div><strong>{action[item.action] ?? item.action}</strong><p>{item.adminEmail || "النظام"} · {item.ipAddress || "—"}</p></div><time>{formatDate(item.createdAt)}</time></article>)}</div>}
  </>;
}
