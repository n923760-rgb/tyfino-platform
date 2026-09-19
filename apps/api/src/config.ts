export type AppConfig = {
  port: number;
  databaseUrl: string;
  logLevel: string;
  trustProxy: boolean;
  appEnv: "development" | "test" | "production";
  adminOrigin: string;
  tokenPepper: string;
  bootstrapEmail: string;
  bootstrapPassword?: string;
  ownerTotpSecret?: string;
};

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
}

export function ownerTotpSecretForEnvironment(
  appEnv: AppConfig["appEnv"],
  rawValue: string | undefined
): string | undefined {
  const secret = rawValue?.trim().replace(/\s/g, "").toUpperCase();
  if (secret && (!/^[A-Z2-7]{32,}$/.test(secret) || secret.includes("CHANGE"))) {
    throw new Error("ADMIN_OWNER_TOTP_SECRET must be an RFC 4648 Base32 secret containing at least 160 bits");
  }
  if (appEnv === "production" && !secret) {
    throw new Error("ADMIN_OWNER_TOTP_SECRET is required in production");
  }
  return secret;
}

export function loadConfig(): AppConfig {
  const port = Number(process.env.API_PORT ?? "3000");
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("API_PORT must be a valid TCP port");
  }
  const appEnv = (process.env.APP_ENV ?? "development") as AppConfig["appEnv"];
  if (!["development", "test", "production"].includes(appEnv)) {
    throw new Error("APP_ENV must be development, test, or production");
  }
  const bootstrapPassword = process.env.ADMIN_BOOTSTRAP_PASSWORD?.trim();
  if (bootstrapPassword && (bootstrapPassword.length < 14 || bootstrapPassword.includes("CHANGE_ME"))) {
    throw new Error("ADMIN_BOOTSTRAP_PASSWORD must be at least 14 characters and changed from the example");
  }
  const tokenPepper = required("TOKEN_PEPPER");
  if (tokenPepper.length < 32 || tokenPepper.includes("CHANGE_ME")) {
    throw new Error("TOKEN_PEPPER must be at least 32 characters and changed from the example");
  }
  const ownerTotpSecret = ownerTotpSecretForEnvironment(appEnv, process.env.ADMIN_OWNER_TOTP_SECRET);
  return {
    port,
    databaseUrl: required("DATABASE_URL"),
    logLevel: process.env.LOG_LEVEL ?? "info",
    trustProxy: (process.env.TRUST_PROXY ?? "true") === "true",
    appEnv,
    adminOrigin: required("ADMIN_ORIGIN"),
    tokenPepper,
    bootstrapEmail: required("ADMIN_BOOTSTRAP_EMAIL").toLowerCase(),
    bootstrapPassword,
    ownerTotpSecret
  };
}
