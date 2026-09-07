import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { requireAdmin, writeAudit } from "../auth.js";
import type { AppConfig } from "../config.js";
import type { Database } from "../db.js";
import { randomToken, tokenHash, verifyPassword } from "../security.js";

const LoginBody = z.object({
  email: z.string().email().max(254).transform((value) => value.toLowerCase()),
  password: z.string().min(1).max(256)
});

export async function registerAdminAuthRoutes(
  app: FastifyInstance,
  db: Database,
  config: AppConfig
): Promise<void> {
  app.post("/v1/admin/auth/login", {
    config: { rateLimit: { max: 8, timeWindow: "15 minutes" } }
  }, async (request, reply) => {
    const parsed = LoginBody.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request" });

    const result = await db.query<{
      id: string;
      email: string;
      role: "owner" | "admin" | "support";
      password_hash: string;
    }>(
      "SELECT id, email, role, password_hash FROM admins WHERE email = $1 AND is_active = true",
      [parsed.data.email]
    );
    const admin = result.rows[0];
    if (!admin || !(await verifyPassword(parsed.data.password, admin.password_hash))) {
      return reply.code(401).send({ error: "invalid_credentials" });
    }

    const token = randomToken();
    await db.query(
      `INSERT INTO admin_sessions (admin_id, token_hash, ip_address, user_agent, expires_at)
       VALUES ($1, $2, $3::inet, $4, now() + interval '12 hours')`,
      [admin.id, tokenHash(token, config.tokenPepper), request.ip, request.headers["user-agent"] ?? null]
    );
    await writeAudit(db, request, admin.id, "admin.login", "admin", admin.id);

    reply.setCookie("tyfino_admin_session", token, {
      httpOnly: true,
      secure: config.appEnv === "production",
      sameSite: "strict",
      path: "/",
      maxAge: 12 * 60 * 60
    });
    return { admin: { id: admin.id, email: admin.email, role: admin.role } };
  });

  app.get("/v1/admin/me", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config);
    if (!admin) return;
    return { admin };
  });

  app.post("/v1/admin/auth/logout", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config);
    if (!admin) return;
    const token = request.cookies.tyfino_admin_session ?? request.headers.authorization?.slice(7);
    if (token) {
      await db.query("UPDATE admin_sessions SET revoked_at = now() WHERE token_hash = $1", [
        tokenHash(token, config.tokenPepper)
      ]);
    }
    await writeAudit(db, request, admin.id, "admin.logout", "admin", admin.id);
    reply.clearCookie("tyfino_admin_session", { path: "/" });
    return reply.code(204).send();
  });
}
