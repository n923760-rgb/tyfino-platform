import cookie from "@fastify/cookie";
import cors from "@fastify/cors";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import Fastify from "fastify";
import { ensureBootstrapAdmin } from "./auth.js";
import type { AppConfig } from "./config.js";
import type { Database } from "./db.js";
import { LicensingError } from "./licensing.js";
import { registerAdminAuthRoutes } from "./routes/admin-auth.js";
import { registerAdminRoutes } from "./routes/admin.js";
import { registerLicensingRoutes } from "./routes/licensing.js";

export async function buildApp(config: AppConfig, db: Database) {
  const app = Fastify({ logger: { level: config.logLevel }, trustProxy: config.trustProxy, requestIdHeader: "x-request-id", bodyLimit: 64 * 1024 });
  await app.register(cookie);
  await app.register(cors, {
    origin: (origin, callback) => {
      if (!origin || origin === config.adminOrigin) callback(null, true);
      else callback(new Error("Origin rejected"), false);
    },
    credentials: true,
    methods: ["GET", "POST", "PATCH", "OPTIONS"]
  });
  await app.register(helmet, { global: true });
  await app.register(rateLimit, { global: true, max: 120, timeWindow: "1 minute" });
  app.addHook("onRequest", async (request, reply) => {
    if (!request.url.startsWith("/v1/admin/")) return;
    const origin = request.headers.origin;
    if (origin && origin !== config.adminOrigin) return reply.code(403).send({ error: "origin_rejected" });
  });
  app.get("/healthz", async () => ({ status: "ok", service: "tyfino-api", version: "0.2.0" }));
  app.get("/readyz", async (_request, reply) => {
    try {
      await db.query("SELECT 1");
      return { status: "ready", database: "connected" };
    } catch (error) {
      app.log.error({ error }, "Database readiness check failed");
      return reply.code(503).send({ status: "not_ready" });
    }
  });
  await ensureBootstrapAdmin(db, config);
  await registerAdminAuthRoutes(app, db, config);
  await registerAdminRoutes(app, db, config);
  await registerLicensingRoutes(app, db, config);
  app.setNotFoundHandler(async (_request, reply) => reply.code(404).send({ error: "not_found" }));
  app.setErrorHandler(async (error, request, reply) => {
    if (error instanceof LicensingError) {
      return reply.code(error.statusCode).send({ serverTime: new Date().toISOString(), requestId: request.id,
        error: { code: error.code, message: error.code, retryable: error.retryable } });
    }
    request.log.error({ error }, "Unhandled request error");
    return reply.code(500).send({ error: "internal_error", requestId: request.id });
  });
  return app;
}
