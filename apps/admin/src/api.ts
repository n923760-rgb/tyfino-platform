import type { Activation, Admin, AuditLog, Customer, Device, Host, ProviderAccount } from "./types";

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") ?? "";

export class ApiError extends Error {
  constructor(public status: number, public code: string) {
    super(code);
  }
}

const messages: Record<string, string> = {
  invalid_credentials: "البريد الإلكتروني أو كلمة المرور غير صحيحة.",
  authentication_required: "انتهت الجلسة، سجّل الدخول مرة أخرى.",
  invalid_session: "انتهت الجلسة، سجّل الدخول مرة أخرى.",
  invalid_request: "تحقق من البيانات المدخلة.",
  host_exists: "هذا الهوست مضاف مسبقًا.",
  xtream_credentials_required: "اسم المستخدم وكلمة المرور مطلوبان لاشتراك Xtream.",
  playlist_url_required: "رابط قائمة M3U مطلوب.",
  portal_data_required: "بيانات Stalker / MAC مطلوبة.",
  invalid_customer_provider_mapping: "الاشتراك لا يخص العميل المحدد.",
  request_failed: "تعذر الاتصال بالخادم. حاول مرة أخرى.",
  internal_error: "حدث خطأ داخلي. حاول مرة أخرى."
};

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return messages[error.code] ?? `تعذر إتمام الطلب (${error.code}).`;
  return "تعذر الاتصال بالخادم. حاول مرة أخرى.";
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...options.headers }
  });
  if (response.status === 204) return undefined as T;
  const data = await response.json().catch(() => ({})) as { error?: string };
  if (!response.ok) throw new ApiError(response.status, data.error ?? "request_failed");
  return data as T;
}

const json = (value: unknown): RequestInit => ({ body: JSON.stringify(value) });

export const api = {
  me: () => request<{ admin: Admin }>("/v1/admin/me"),
  login: (email: string, password: string) => request<{ admin: Admin }>("/v1/admin/auth/login", {
    method: "POST", ...json({ email, password })
  }),
  logout: () => request<void>("/v1/admin/auth/logout", { method: "POST", ...json({}) }),

  customers: () => request<{ customers: Customer[] }>("/v1/admin/customers"),
  createCustomer: (value: object) => request<{ id: string }>("/v1/admin/customers", { method: "POST", ...json(value) }),
  updateCustomer: (id: string, value: object) => request<{ id: string }>(`/v1/admin/customers/${id}`, { method: "PATCH", ...json(value) }),

  hosts: () => request<{ hosts: Host[] }>("/v1/admin/hosts"),
  createHost: (value: object) => request<{ id: string }>("/v1/admin/hosts", { method: "POST", ...json(value) }),
  updateHost: (id: string, value: object) => request<{ id: string }>(`/v1/admin/hosts/${id}`, { method: "PATCH", ...json(value) }),

  accounts: () => request<{ providerAccounts: ProviderAccount[] }>("/v1/admin/provider-accounts"),
  createAccount: (value: object) => request<{ id: string }>("/v1/admin/provider-accounts", { method: "POST", ...json(value) }),
  updateAccount: (id: string, value: object) => request<{ id: string }>(`/v1/admin/provider-accounts/${id}`, { method: "PATCH", ...json(value) }),

  activations: () => request<{ activationCodes: Activation[] }>("/v1/admin/activation-codes"),
  createActivation: (value: object) => request<{ id: string; code: string }>("/v1/admin/activation-codes", { method: "POST", ...json(value) }),
  revokeActivation: (id: string) => request<{ id: string; status: string }>(`/v1/admin/activation-codes/${id}/revoke`, { method: "POST", ...json({}) }),

  devices: () => request<{ devices: Device[] }>("/v1/admin/devices"),
  updateDevice: (id: string, status: Device["status"]) => request<{ id: string }>(`/v1/admin/devices/${id}`, { method: "PATCH", ...json({ status }) }),
  auditLogs: () => request<{ auditLogs: AuditLog[] }>("/v1/admin/audit-logs")
};
