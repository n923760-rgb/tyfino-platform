export type AppConfig = {
  port: number;
  databaseUrl: string;
  logLevel: string;
  trustProxy: boolean;
};

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
}

export function loadConfig(): AppConfig {
  const port = Number(process.env.API_PORT ?? "3000");
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("API_PORT must be a valid TCP port");
  }

  return {
    port,
    databaseUrl: required("DATABASE_URL"),
    logLevel: process.env.LOG_LEVEL ?? "info",
    trustProxy: (process.env.TRUST_PROXY ?? "true") === "true"
  };
}
