import type { Activation, Admin, AuditLog } from "./types";

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") ?? "";

export class ApiError extends Error {
  constructor(public status: number, public code: string) { super(code); }
}

const messages: Record<string, string> = {
  invalid_credentials: "البريد الإلكتروني أو كلمة المرور غير صحيحة.",
  invalid_two_factor_challenge: "انتهت مهلة التحقق. ابدأ تسجيل الدخول مرة أخرى.",
  invalid_two_factor_code: "رمز التحقق غير صحيح أو سبق استخدامه.",
  authentication_required: "انتهت الجلسة، سجّل الدخول مرة أخرى.",
  invalid_session: "انتهت الجلسة، سجّل الدخول مرة أخرى.",
  invalid_request: "تحقق من البيانات المدخلة.",
  insufficient_permission: "ليس لديك صلاحية لتنفيذ هذه العملية.",
  activation_not_found: "كود التفعيل غير موجود.",
  activation_inactive: "لا يمكن تعديل كود ملغي أو منتهي.",
  code_generation_failed: "تعذر إنشاء كود جديد. حاول مرة أخرى.",
  request_failed: "تعذر الاتصال بالخادم. حاول مرة أخرى.",
  internal_error: "حدث خطأ داخلي. حاول مرة أخرى."
};

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return messages[error.code] ?? `تعذر إتمام الطلب (${error.code}).`;
  return messages.request_failed;
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
  login: (email: string, password: string) => request<
    { admin: Admin } | { twoFactorRequired: true; challengeToken: string }
  >("/v1/admin/auth/login", { method: "POST", ...json({ email, password }) }),
  verifyTotp: (challengeToken: string, code: string) => request<{ admin: Admin }>(
    "/v1/admin/auth/verify-totp",
    { method: "POST", ...json({ challengeToken, code }) }
  ),
  logout: () => request<void>("/v1/admin/auth/logout", { method: "POST", ...json({}) }),
  health: () => request<{ status: string; database: string }>("/readyz"),
  activations: () => request<{ activationCodes: Activation[] }>("/v1/admin/activation-codes"),
  createActivation: (value: object) => request<{ id: string; code: string }>("/v1/admin/activation-codes", { method: "POST", ...json(value) }),
  revokeActivation: (id: string) => request<{ id: string; status: string }>(`/v1/admin/activation-codes/${id}/revoke`, { method: "POST", ...json({}) }),
  resetDevice: (id: string, reason: string) => request<{ id: string; deviceBound: boolean }>(`/v1/admin/activation-codes/${id}/reset-device`, { method: "POST", ...json({ reason }) }),
  settings: () => request<{ trialEnabled: boolean }>("/v1/admin/app-settings"),
  updateSettings: (trialEnabled: boolean) => request<{ trialEnabled: boolean }>("/v1/admin/app-settings", { method: "PATCH", ...json({ trialEnabled }) }),
  auditLogs: () => request<{ auditLogs: AuditLog[] }>("/v1/admin/audit-logs")
};
