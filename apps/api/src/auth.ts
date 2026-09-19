import type { FastifyReply, FastifyRequest } from "fastify";
import type { AppConfig } from "./config.js";
import type { Database } from "./db.js";
import { hashPassword, tokenHash } from "./security.js";

export type AdminIdentity = { id: string; email: string; role: "owner" | "admin" | "support" };

export async function ensureBootstrapAdmin(db: Database, config: AppConfig): Promise<void> {
  const count = await db.query<{ count: string }>("SELECT count(*)::text AS count FROM admins");
  if (count.rows[0]?.count !== "0") return;
  if (!config.bootstrapPassword) throw new Error("ADMIN_BOOTSTRAP_PASSWORD is required while the admins table is empty");
  const passwordHash = await hashPassword(config.bootstrapPassword);
  await db.query(
    `INSERT INTO admins (email, password_hash, role) VALUES ($1, $2, 'owner') ON CONFLICT (email) DO NOTHING`,
    [config.bootstrapEmail, passwordHash]
  );
}

export function adminSessionToken(request: FastifyRequest): string | undefined {
  const cookieToken = request.cookies.tyfino_admin_session;
  if (cookieToken) return cookieToken;
  const authorization = request.headers.authorization;
  return authorization?.startsWith("Bearer ") ? authorization.slice(7) : undefined;
}

export async function requireAdmin(
  request: FastifyRequest,
  reply: FastifyReply,
  db: Database,
  config: AppConfig,
  roles?: AdminIdentity["role"][]
): Promise<AdminIdentity | null> {
  const token = adminSessionToken(request);
  if (!token) {
    await reply.code(401).send({ error: "authentication_required" });
    return null;
  }
  const result = await db.query<AdminIdentity>(
    `SELECT a.id, a.email, a.role FROM admin_sessions s JOIN admins a ON a.id = s.admin_id
      WHERE s.token_hash = $1 AND s.revoked_at IS NULL AND s.expires_at > now() AND a.is_active = true`,
    [tokenHash(token, config.tokenPepper)]
  );
  const admin = result.rows[0];
  if (!admin) {
    await reply.code(401).send({ error: "invalid_session" });
    return null;
  }
  if (roles && !roles.includes(admin.role)) {
    await reply.code(403).send({ error: "insufficient_permission" });
    return null;
  }
  return admin;
}

export async function writeAudit(
  db: Pick<Database, "query">,
  request: FastifyRequest,
  adminId: string,
  action: string,
  entityType: string,
  entityId: string | null,
  metadata: Record<string, unknown> = {}
): Promise<void> {
  await db.query(
    `INSERT INTO audit_logs (admin_id, action, entity_type, entity_id, metadata, ip_address)
     VALUES ($1, $2, $3, $4, $5::jsonb, $6::inet)`,
    [adminId, action, entityType, entityId, JSON.stringify(metadata), request.ip]
  );
}
