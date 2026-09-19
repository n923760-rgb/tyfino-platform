import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import type { PoolClient } from "pg";
import { z } from "zod";
import { adminSessionToken, requireAdmin, writeAudit } from "../auth.js";
import type { AppConfig } from "../config.js";
import type { Database } from "../db.js";
import { randomToken, tokenHash, verifyPassword, verifyTotp } from "../security.js";

const LoginBody = z.object({
  email: z.string().email().max(254).transform((value) => value.toLowerCase()),
  password: z.string().min(1).max(256)
});

const TotpBody = z.object({
  challengeToken: z.string().min(32).max(256),
  code: z.string().regex(/^\d{6}$/)
});

type AdminRecord = {
  id: string;
  email: string;
  role: "owner" | "admin" | "support";
};

type Queryable = Database | PoolClient;

async function createAdminSession(
  db: Queryable,
  request: FastifyRequest,
  admin: AdminRecord,
  config: AppConfig
): Promise<string> {
  const token = randomToken();
  await db.query(
    `INSERT INTO admin_sessions (admin_id, token_hash, ip_address, user_agent, expires_at)
     VALUES ($1, $2, $3::inet, $4, now() + interval '12 hours')`,
    [admin.id, tokenHash(token, config.tokenPepper), request.ip, request.headers["user-agent"] ?? null]
  );
  await writeAudit(db, request, admin.id, "admin.login", "admin", admin.id, {
    secondFactor: admin.role === "owner" && Boolean(config.ownerTotpSecret)
  });
  return token;
}

function setSessionCookie(
  reply: FastifyReply,
  token: string,
  config: AppConfig
): void {
  reply.setCookie("tyfino_admin_session", token, {
    httpOnly: true,
    secure: config.appEnv === "production",
    sameSite: "strict",
    path: "/",
    maxAge: 12 * 60 * 60
  });
}

export async function registerAdminAuthRoutes(
  app: FastifyInstance,
  db: Database,
  config: AppConfig
): Promise<void> {
  app.post("/v1/admin/auth/login", {
    config: { rateLimit: { max: config.rateLimits.adminAuthPer15Minutes, timeWindow: "15 minutes" } }
  }, async (request, reply) => {
    const parsed = LoginBody.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request" });

    const result = await db.query<AdminRecord & { password_hash: string }>(
      "SELECT id, email, role, password_hash FROM admins WHERE email = $1 AND is_active = true",
      [parsed.data.email]
    );
    const admin = result.rows[0];
    if (!admin || !(await verifyPassword(parsed.data.password, admin.password_hash))) {
      return reply.code(401).send({ error: "invalid_credentials" });
    }

    if (admin.role === "owner" && config.ownerTotpSecret) {
      const challengeToken = randomToken();
      await db.query(
        `DELETE FROM admin_login_challenges
          WHERE expires_at < now() OR consumed_at < now() - interval '1 day'`
      );
      await db.query(
        `INSERT INTO admin_login_challenges
          (admin_id, token_hash, ip_address, user_agent, expires_at)
         VALUES ($1, $2, $3::inet, $4, now() + interval '5 minutes')`,
        [admin.id, tokenHash(challengeToken, config.tokenPepper), request.ip, request.headers["user-agent"] ?? null]
      );
      await writeAudit(db, request, admin.id, "admin.login_2fa_challenge", "admin", admin.id);
      return reply.code(202).send({ twoFactorRequired: true, challengeToken });
    }

    const token = await createAdminSession(db, request, admin, config);
    setSessionCookie(reply, token, config);
    return { admin: { id: admin.id, email: admin.email, role: admin.role } };
  });

  app.post("/v1/admin/auth/verify-totp", {
    config: { rateLimit: { max: config.rateLimits.adminAuthPer15Minutes, timeWindow: "15 minutes" } }
  }, async (request, reply) => {
    const parsed = TotpBody.safeParse(request.body);
    if (!parsed.success || !config.ownerTotpSecret) {
      return reply.code(400).send({ error: "invalid_request" });
    }

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const result = await client.query<AdminRecord & {
        challenge_id: string;
        attempts: number;
        last_totp_counter: string | null;
      }>(
        `SELECT c.id AS challenge_id, c.attempts, a.id, a.email, a.role, a.last_totp_counter
           FROM admin_login_challenges c
           JOIN admins a ON a.id = c.admin_id
          WHERE c.token_hash = $1 AND c.consumed_at IS NULL AND c.expires_at > now()
            AND c.attempts < 5 AND a.is_active = true AND a.role = 'owner'
          FOR UPDATE`,
        [tokenHash(parsed.data.challengeToken, config.tokenPepper)]
      );
      const challenge = result.rows[0];
      if (!challenge) {
        await client.query("ROLLBACK");
        return reply.code(401).send({ error: "invalid_two_factor_challenge" });
      }

      const previousCounter = challenge.last_totp_counter === null ? null : BigInt(challenge.last_totp_counter);
      const counter = verifyTotp(config.ownerTotpSecret, parsed.data.code, Date.now(), previousCounter);
      if (counter === null) {
        await client.query(
          "UPDATE admin_login_challenges SET attempts = attempts + 1 WHERE id = $1",
          [challenge.challenge_id]
        );
        await writeAudit(client, request, challenge.id, "admin.login_2fa_failed", "admin", challenge.id);
        await client.query("COMMIT");
        return reply.code(401).send({ error: "invalid_two_factor_code" });
      }

      const accepted = await client.query(
        `UPDATE admins SET last_totp_counter = $2, updated_at = now()
          WHERE id = $1 AND (last_totp_counter IS NULL OR last_totp_counter < $2) RETURNING id`,
        [challenge.id, counter.toString()]
      );
      if (accepted.rowCount !== 1) {
        await client.query("ROLLBACK");
        return reply.code(401).send({ error: "invalid_two_factor_code" });
      }
      await client.query(
        "UPDATE admin_login_challenges SET consumed_at = now() WHERE id = $1",
        [challenge.challenge_id]
      );
      const token = await createAdminSession(client, request, challenge, config);
      await client.query("COMMIT");
      setSessionCookie(reply, token, config);
      return { admin: { id: challenge.id, email: challenge.email, role: challenge.role } };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  });

  app.get("/v1/admin/me", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config);
    if (!admin) return;
    return { admin };
  });

  app.post("/v1/admin/auth/logout", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config);
    if (!admin) return;
    const token = adminSessionToken(request);
    if (token) {
      await db.query("UPDATE admin_sessions SET revoked_at = now() WHERE token_hash = $1", [
        tokenHash(token, config.tokenPepper)
      ]);
    }
    await writeAudit(db, request, admin.id, "admin.logout", "admin", admin.id);
    reply.clearCookie("tyfino_admin_session", { path: "/" });
    return reply.code(204).send();
  });

  app.post("/v1/admin/auth/revoke-other-sessions", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config, ["owner"]);
    if (!admin) return;
    const currentToken = adminSessionToken(request);
    if (!currentToken) return reply.code(401).send({ error: "authentication_required" });

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const revoked = await client.query(
        `UPDATE admin_sessions
            SET revoked_at = now()
          WHERE revoked_at IS NULL AND expires_at > now() AND token_hash <> $1
          RETURNING id`,
        [tokenHash(currentToken, config.tokenPepper)]
      );
      await writeAudit(client, request, admin.id, "admin.sessions_revoke_others", "admin_session", null, {
        revokedSessions: revoked.rowCount ?? 0
      });
      await client.query("COMMIT");
      return { revokedSessions: revoked.rowCount ?? 0 };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  });
}
