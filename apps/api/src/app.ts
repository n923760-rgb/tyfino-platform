import cookie from "@fastify/cookie";
import cors from "@fastify/cors";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import Fastify from "fastify";
import { ensureBootstrapAdmin } from "./auth.js";
import type { AppConfig } from "./config.js";
import type { Database } from "./db.js";
import { registerAdminAuthRoutes } from "./routes/admin-auth.js";
import { registerAdminRoutes } from "./routes/admin.js";
import { registerPlayerRoutes } from "./routes/player.js";

export async function buildApp(config: AppConfig, db: Database) {
  const app = Fastify({
    logger: { level: config.logLevel },
    trustProxy: config.trustProxy,
    requestIdHeader: "x-request-id",
    bodyLimit: 64 * 1024
  });

  await app.register(cookie);
  const allowedOrigins = new Set([config.adminOrigin, ...config.playerOrigins]);
  await app.register(cors, {
    origin: (origin, callback) => {
      if (!origin || allowedOrigins.has(origin)) callback(null, true);
      else callback(new Error("Origin rejected"), false);
    },
    credentials: true,
    methods: ["GET", "POST", "PATCH", "DELETE", "OPTIONS"]
  });
  await app.register(helmet, { global: true });
  await app.register(rateLimit, {
    global: true,
    max: 120,
    timeWindow: "1 minute"
  });

  app.addHook("onRequest", async (request, reply) => {
    if (!request.url.startsWith("/v1/admin/")) return;
    const origin = request.headers.origin;
    if (origin && origin !== config.adminOrigin) {
      return reply.code(403).send({ error: "origin_rejected" });
    }
  });

  app.get("/healthz", async () => ({
    status: "ok",
    service: "tyfino-api",
    version: "0.2.0"
  }));

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
  await registerPlayerRoutes(app, db, config);

  app.setNotFoundHandler(async (_request, reply) => {
    return reply.code(404).send({ error: "not_found" });
  });

  app.setErrorHandler(async (error, request, reply) => {
    request.log.error({ error }, "Unhandled request error");
    return reply.code(500).send({ error: "internal_error", requestId: request.id });
  });

  return app;
}
