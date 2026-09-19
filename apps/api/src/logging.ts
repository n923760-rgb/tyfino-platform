import type { LoggerOptions } from "pino";

const SENSITIVE_FIELDS = [
  "authorization",
  "cookie",
  "password",
  "activationCode",
  "challengeToken",
  "token",
  "ownerTotpSecret",
  "bootstrapPassword",
  "tokenPepper",
  "databaseUrl"
] as const;

const REDACTION_PATHS = [
  "req.headers.authorization",
  "req.headers.cookie",
  "request.headers.authorization",
  "request.headers.cookie",
  "res.headers['set-cookie']",
  "response.headers['set-cookie']",
  "body.code",
  "req.body.code",
  ...SENSITIVE_FIELDS.flatMap((field) => [field, `*.${field}`, `*.*.${field}`, `*.*.*.${field}`])
];

export function apiLoggerOptions(level: string): LoggerOptions {
  return {
    level,
    redact: {
      paths: [...REDACTION_PATHS],
      censor: "[REDACTED]"
    }
  };
}

export function routeLabel(routePattern: string | undefined): string {
  return routePattern?.startsWith("/") ? routePattern : "unmatched";
}

export function safeErrorSummary(error: unknown): { type: string; code?: string } {
  if (!(error instanceof Error)) return { type: "UnknownError" };
  const code = (error as Error & { code?: unknown }).code;
  return typeof code === "string" ? { type: error.name, code } : { type: error.name };
}
