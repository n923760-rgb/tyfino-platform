import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import Fastify from "fastify";
import pg from "pg";
import { loadConfig } from "./config.js";

const config = loadConfig();
const pool = new pg.Pool({
  connectionString: config.databaseUrl,
  max: 8,
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 5_000
});

const app = Fastify({
  logger: { level: config.logLevel },
  trustProxy: config.trustProxy,
  requestIdHeader: "x-request-id"
});

await app.register(helmet, { global: true });
await app.register(rateLimit, {
  global: true,
  max: 120,
  timeWindow: "1 minute"
});

app.get("/healthz", async () => ({
  status: "ok",
  service: "tyfino-api",
  version: "0.1.0"
}));

app.get("/readyz", async (_request, reply) => {
  try {
    await pool.query("SELECT 1");
    return { status: "ready", database: "connected" };
  } catch (error) {
    app.log.error({ error }, "Database readiness check failed");
    return reply.code(503).send({ status: "not_ready" });
  }
});

app.setNotFoundHandler(async (_request, reply) => {
  return reply.code(404).send({ error: "not_found" });
});

const shutdown = async (signal: string) => {
  app.log.info({ signal }, "Shutting down");
  await app.close();
  await pool.end();
  process.exit(0);
};

process.on("SIGTERM", () => void shutdown("SIGTERM"));
process.on("SIGINT", () => void shutdown("SIGINT"));

try {
  await app.listen({ host: "0.0.0.0", port: config.port });
} catch (error) {
  app.log.error(error);
  await pool.end();
  process.exit(1);
}
