import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import { z } from "zod";
import { requireAdmin, writeAudit, type AdminIdentity } from "../auth.js";
import type { AppConfig } from "../config.js";
import type { Database } from "../db.js";
import { encryptSecret, generateActivationCode, tokenHash } from "../security.js";

const UuidParams = z.object({ id: z.string().uuid() });
const OptionalDate = z.string().datetime({ offset: true }).nullable().optional();

const CustomerCreate = z.object({
  displayName: z.string().trim().min(2).max(120),
  phone: z.string().trim().max(30).nullable().optional(),
  whatsapp: z.string().trim().max(30).nullable().optional(),
  notes: z.string().trim().max(2000).nullable().optional()
});

const CustomerUpdate = CustomerCreate.partial().extend({
  status: z.enum(["active", "suspended", "archived"]).optional()
});

const HostCreate = z.object({
  label: z.string().trim().min(2).max(120),
  protocol: z.enum(["xtream", "m3u", "stalker"]),
  baseUrl: z.string().url().max(2048).refine((value) => ["http:", "https:"].includes(new URL(value).protocol)),
  isActive: z.boolean().optional().default(true)
});
const HostUpdate = HostCreate.omit({ protocol: true }).partial();

const AccountCreate = z.object({
  customerId: z.string().uuid(),
  hostId: z.string().uuid(),
  externalReference: z.string().trim().max(200).nullable().optional(),
  username: z.string().max(500).nullable().optional(),
  password: z.string().max(500).nullable().optional(),
  playlistUrl: z.string().url().max(4096).nullable().optional(),
  portalData: z.string().max(8000).nullable().optional(),
  expiresAt: OptionalDate,
  maxConnections: z.number().int().min(1).max(100).nullable().optional()
});
const AccountUpdate = AccountCreate.omit({ customerId: true, hostId: true }).partial().extend({
  isActive: z.boolean().optional()
});

const ActivationCreate = z.object({
  customerId: z.string().uuid(),
  providerAccountId: z.string().uuid(),
  deviceLimit: z.number().int().min(1).max(10).default(1),
  startsAt: OptionalDate,
  expiresAt: OptionalDate
}).refine(
  (value) => !value.startsAt || !value.expiresAt || new Date(value.expiresAt) > new Date(value.startsAt),
  { message: "expiresAt must be later than startsAt" }
);

const DeviceStatus = z.object({ status: z.enum(["active", "blocked"]) });

async function adminFor(
  request: FastifyRequest,
  reply: FastifyReply,
  db: Database,
  config: AppConfig,
  roles?: AdminIdentity["role"][]
): Promise<AdminIdentity | null> {
  return requireAdmin(request, reply, db, config, roles);
}

export async function registerAdminRoutes(
  app: FastifyInstance,
  db: Database,
  config: AppConfig
): Promise<void> {
  app.get("/v1/admin/customers", async (request, reply) => {
    if (!(await adminFor(request, reply, db, config))) return;
    const result = await db.query(
      `SELECT id, display_name AS "displayName", phone, whatsapp, notes, status,
              created_at AS "createdAt", updated_at AS "updatedAt"
         FROM customers ORDER BY created_at DESC LIMIT 500`
    );
    return { customers: result.rows };
  });

  app.post("/v1/admin/customers", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin", "support"]);
    if (!admin) return;
    const parsed = CustomerCreate.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request", issues: parsed.error.flatten() });
    const value = parsed.data;
    const result = await db.query<{ id: string }>(
      `INSERT INTO customers (display_name, phone, whatsapp, notes)
       VALUES ($1, $2, $3, $4) RETURNING id`,
      [value.displayName, value.phone ?? null, value.whatsapp ?? null, value.notes ?? null]
    );
    const id = result.rows[0]!.id;
    await writeAudit(db, request, admin.id, "customer.create", "customer", id);
    return reply.code(201).send({ id });
  });

  app.patch("/v1/admin/customers/:id", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin", "support"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    const body = CustomerUpdate.safeParse(request.body);
    if (!params.success || !body.success || Object.keys(body.data).length === 0) {
      return reply.code(400).send({ error: "invalid_request" });
    }
    const current = await db.query<{
      display_name: string; phone: string | null; whatsapp: string | null; notes: string | null;
      status: "active" | "suspended" | "archived";
    }>("SELECT display_name, phone, whatsapp, notes, status FROM customers WHERE id = $1", [params.data.id]);
    const row = current.rows[0];
    if (!row) return reply.code(404).send({ error: "customer_not_found" });
    const value = body.data;
    await db.query(
      `UPDATE customers SET display_name = $2, phone = $3, whatsapp = $4, notes = $5,
              status = $6, updated_at = now() WHERE id = $1`,
      [params.data.id, value.displayName ?? row.display_name, value.phone === undefined ? row.phone : value.phone,
       value.whatsapp === undefined ? row.whatsapp : value.whatsapp,
       value.notes === undefined ? row.notes : value.notes, value.status ?? row.status]
    );
    await writeAudit(db, request, admin.id, "customer.update", "customer", params.data.id, {
      fields: Object.keys(value)
    });
    return { id: params.data.id };
  });

  app.get("/v1/admin/hosts", async (request, reply) => {
    if (!(await adminFor(request, reply, db, config))) return;
    const result = await db.query(
      `SELECT id, label, protocol, base_url AS "baseUrl", is_active AS "isActive",
              created_at AS "createdAt", updated_at AS "updatedAt"
         FROM provider_hosts ORDER BY label`
    );
    return { hosts: result.rows };
  });

  app.post("/v1/admin/hosts", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const parsed = HostCreate.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request", issues: parsed.error.flatten() });
    const value = parsed.data;
    try {
      const result = await db.query<{ id: string }>(
        `INSERT INTO provider_hosts (label, protocol, base_url, is_active)
         VALUES ($1, $2, $3, $4) RETURNING id`,
        [value.label, value.protocol, value.baseUrl.replace(/\/$/, ""), value.isActive]
      );
      const id = result.rows[0]!.id;
      await writeAudit(db, request, admin.id, "host.create", "provider_host", id);
      return reply.code(201).send({ id });
    } catch (error) {
      if ((error as { code?: string }).code === "23505") return reply.code(409).send({ error: "host_exists" });
      throw error;
    }
  });

  app.patch("/v1/admin/hosts/:id", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    const body = HostUpdate.safeParse(request.body);
    if (!params.success || !body.success || Object.keys(body.data).length === 0) {
      return reply.code(400).send({ error: "invalid_request" });
    }
    const current = await db.query<{ label: string; base_url: string; is_active: boolean }>(
      "SELECT label, base_url, is_active FROM provider_hosts WHERE id = $1", [params.data.id]
    );
    const row = current.rows[0];
    if (!row) return reply.code(404).send({ error: "host_not_found" });
    const value = body.data;
    const baseUrl = value.baseUrl?.replace(/\/$/, "") ?? row.base_url;
    await db.query(
      `UPDATE provider_hosts SET label = $2, base_url = $3, is_active = $4, updated_at = now()
        WHERE id = $1`, [params.data.id, value.label ?? row.label, baseUrl, value.isActive ?? row.is_active]
    );
    await writeAudit(db, request, admin.id, "host.update", "provider_host", params.data.id, {
      fields: Object.keys(value)
    });
    return { id: params.data.id };
  });

  app.get("/v1/admin/provider-accounts", async (request, reply) => {
    if (!(await adminFor(request, reply, db, config))) return;
    const result = await db.query(
      `SELECT pa.id, pa.customer_id AS "customerId", c.display_name AS "customerName",
              pa.host_id AS "hostId", h.label AS "hostLabel", h.protocol,
              pa.external_reference AS "externalReference", pa.expires_at AS "expiresAt",
              pa.max_connections AS "maxConnections", pa.is_active AS "isActive",
              (pa.encrypted_username IS NOT NULL) AS "hasUsername",
              (pa.encrypted_password IS NOT NULL) AS "hasPassword",
              (pa.encrypted_playlist_url IS NOT NULL) AS "hasPlaylistUrl",
              (pa.encrypted_portal_data IS NOT NULL) AS "hasPortalData",
              pa.created_at AS "createdAt", pa.updated_at AS "updatedAt"
         FROM provider_accounts pa
         JOIN customers c ON c.id = pa.customer_id
         JOIN provider_hosts h ON h.id = pa.host_id
        ORDER BY pa.created_at DESC LIMIT 500`
    );
    return { providerAccounts: result.rows };
  });

  app.post("/v1/admin/provider-accounts", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const parsed = AccountCreate.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request", issues: parsed.error.flatten() });
    const value = parsed.data;
    const host = await db.query<{ protocol: "xtream" | "m3u" | "stalker" }>(
      "SELECT protocol FROM provider_hosts WHERE id = $1 AND is_active = true", [value.hostId]
    );
    const protocol = host.rows[0]?.protocol;
    if (!protocol) return reply.code(404).send({ error: "host_not_found" });
    if (protocol === "xtream" && (!value.username || !value.password)) {
      return reply.code(400).send({ error: "xtream_credentials_required" });
    }
    if (protocol === "m3u" && !value.playlistUrl) return reply.code(400).send({ error: "playlist_url_required" });
    if (protocol === "stalker" && !value.portalData) return reply.code(400).send({ error: "portal_data_required" });

    const result = await db.query<{ id: string }>(
      `INSERT INTO provider_accounts
       (customer_id, host_id, external_reference, encrypted_username, encrypted_password,
        encrypted_playlist_url, encrypted_portal_data, expires_at, max_connections)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING id`,
      [value.customerId, value.hostId, value.externalReference ?? null,
       encryptSecret(value.username, config.encryptionKey), encryptSecret(value.password, config.encryptionKey),
       encryptSecret(value.playlistUrl, config.encryptionKey), encryptSecret(value.portalData, config.encryptionKey),
       value.expiresAt ?? null, value.maxConnections ?? null]
    );
    const id = result.rows[0]!.id;
    await writeAudit(db, request, admin.id, "provider_account.create", "provider_account", id, { protocol });
    return reply.code(201).send({ id });
  });

  app.patch("/v1/admin/provider-accounts/:id", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    const body = AccountUpdate.safeParse(request.body);
    if (!params.success || !body.success || Object.keys(body.data).length === 0) {
      return reply.code(400).send({ error: "invalid_request" });
    }
    const current = await db.query<{
      external_reference: string | null; encrypted_username: string | null; encrypted_password: string | null;
      encrypted_playlist_url: string | null; encrypted_portal_data: string | null; expires_at: Date | null;
      max_connections: number | null; is_active: boolean; protocol: "xtream" | "m3u" | "stalker";
    }>(
      `SELECT pa.external_reference, pa.encrypted_username, pa.encrypted_password,
              pa.encrypted_playlist_url, pa.encrypted_portal_data, pa.expires_at,
              pa.max_connections, pa.is_active, ph.protocol
         FROM provider_accounts pa JOIN provider_hosts ph ON ph.id = pa.host_id
        WHERE pa.id = $1`, [params.data.id]
    );
    const row = current.rows[0];
    if (!row) return reply.code(404).send({ error: "provider_account_not_found" });
    const value = body.data;
    const username = value.username === undefined ? row.encrypted_username : encryptSecret(value.username, config.encryptionKey);
    const password = value.password === undefined ? row.encrypted_password : encryptSecret(value.password, config.encryptionKey);
    const playlist = value.playlistUrl === undefined ? row.encrypted_playlist_url : encryptSecret(value.playlistUrl, config.encryptionKey);
    const portal = value.portalData === undefined ? row.encrypted_portal_data : encryptSecret(value.portalData, config.encryptionKey);
    if (row.protocol === "xtream" && (!username || !password)) {
      return reply.code(400).send({ error: "xtream_credentials_required" });
    }
    if (row.protocol === "m3u" && !playlist) return reply.code(400).send({ error: "playlist_url_required" });
    if (row.protocol === "stalker" && !portal) return reply.code(400).send({ error: "portal_data_required" });
    await db.query(
      `UPDATE provider_accounts SET external_reference = $2, encrypted_username = $3,
              encrypted_password = $4, encrypted_playlist_url = $5, encrypted_portal_data = $6,
              expires_at = $7, max_connections = $8, is_active = $9, updated_at = now()
        WHERE id = $1`,
      [params.data.id,
       value.externalReference === undefined ? row.external_reference : value.externalReference,
       username, password, playlist, portal,
       value.expiresAt === undefined ? row.expires_at : value.expiresAt,
       value.maxConnections === undefined ? row.max_connections : value.maxConnections,
       value.isActive ?? row.is_active]
    );
    await writeAudit(db, request, admin.id, "provider_account.update", "provider_account", params.data.id, {
      fields: Object.keys(value)
    });
    return { id: params.data.id };
  });

  app.get("/v1/admin/activation-codes", async (request, reply) => {
    if (!(await adminFor(request, reply, db, config))) return;
    const result = await db.query(
      `SELECT ac.id, ac.customer_id AS "customerId", c.display_name AS "customerName",
              ac.provider_account_id AS "providerAccountId", ac.code_suffix AS "codeSuffix",
              ac.status, ac.device_limit AS "deviceLimit", ac.starts_at AS "startsAt",
              ac.expires_at AS "expiresAt", ac.activated_at AS "activatedAt",
              ac.created_at AS "createdAt", count(d.id)::int AS "deviceCount"
         FROM activation_codes ac
         JOIN customers c ON c.id = ac.customer_id
         LEFT JOIN devices d ON d.activation_code_id = ac.id AND d.status = 'active'
        GROUP BY ac.id, c.display_name ORDER BY ac.created_at DESC LIMIT 500`
    );
    return { activationCodes: result.rows };
  });

  app.post("/v1/admin/activation-codes", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin", "support"]);
    if (!admin) return;
    const parsed = ActivationCreate.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request", issues: parsed.error.flatten() });
    const value = parsed.data;
    const mapping = await db.query(
      `SELECT 1 FROM provider_accounts WHERE id = $1 AND customer_id = $2 AND is_active = true`,
      [value.providerAccountId, value.customerId]
    );
    if (!mapping.rowCount) return reply.code(400).send({ error: "invalid_customer_provider_mapping" });

    for (let attempt = 0; attempt < 5; attempt += 1) {
      const code = generateActivationCode();
      try {
        const result = await db.query<{ id: string }>(
          `INSERT INTO activation_codes
           (customer_id, provider_account_id, code_hash, code_suffix, device_limit, starts_at, expires_at)
           VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING id`,
          [value.customerId, value.providerAccountId, tokenHash(code, config.tokenPepper), code.slice(-4),
           value.deviceLimit, value.startsAt ?? null, value.expiresAt ?? null]
        );
        const id = result.rows[0]!.id;
        await writeAudit(db, request, admin.id, "activation.create", "activation_code", id, {
          codeSuffix: code.slice(-4), deviceLimit: value.deviceLimit
        });
        return reply.code(201).send({ id, code });
      } catch (error) {
        if ((error as { code?: string }).code !== "23505") throw error;
      }
    }
    return reply.code(503).send({ error: "code_generation_failed" });
  });

  app.post("/v1/admin/activation-codes/:id/revoke", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin", "support"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    if (!params.success) return reply.code(400).send({ error: "invalid_request" });
    const result = await db.query(
      `UPDATE activation_codes SET status = 'revoked', revoked_at = now(), updated_at = now()
       WHERE id = $1 AND status <> 'revoked' RETURNING id`, [params.data.id]
    );
    if (!result.rowCount) return reply.code(404).send({ error: "activation_not_found" });
    await db.query(
      `UPDATE player_sessions SET revoked_at = now()
       WHERE device_id IN (SELECT id FROM devices WHERE activation_code_id = $1)`, [params.data.id]
    );
    await writeAudit(db, request, admin.id, "activation.revoke", "activation_code", params.data.id);
    return { id: params.data.id, status: "revoked" };
  });

  app.get("/v1/admin/devices", async (request, reply) => {
    if (!(await adminFor(request, reply, db, config))) return;
    const result = await db.query(
      `SELECT d.id, d.activation_code_id AS "activationCodeId", ac.code_suffix AS "codeSuffix",
              c.display_name AS "customerName", d.platform, d.model, d.app_version AS "appVersion",
              d.status, d.first_seen_at AS "firstSeenAt", d.last_seen_at AS "lastSeenAt"
         FROM devices d JOIN activation_codes ac ON ac.id = d.activation_code_id
         JOIN customers c ON c.id = ac.customer_id
        ORDER BY d.last_seen_at DESC LIMIT 1000`
    );
    return { devices: result.rows };
  });

  app.patch("/v1/admin/devices/:id", async (request, reply) => {
    const admin = await adminFor(request, reply, db, config, ["owner", "admin", "support"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    const body = DeviceStatus.safeParse(request.body);
    if (!params.success || !body.success) return reply.code(400).send({ error: "invalid_request" });
    const result = await db.query("UPDATE devices SET status = $2 WHERE id = $1 RETURNING id", [
      params.data.id, body.data.status
    ]);
    if (!result.rowCount) return reply.code(404).send({ error: "device_not_found" });
    if (body.data.status === "blocked") {
      await db.query("UPDATE player_sessions SET revoked_at = now() WHERE device_id = $1", [params.data.id]);
    }
    await writeAudit(db, request, admin.id, "device.status", "device", params.data.id, { status: body.data.status });
    return { id: params.data.id, status: body.data.status };
  });

  app.get("/v1/admin/audit-logs", async (request, reply) => {
    if (!(await adminFor(request, reply, db, config, ["owner", "admin"]))) return;
    const result = await db.query(
      `SELECT al.id, al.action, al.entity_type AS "entityType", al.entity_id AS "entityId",
              al.metadata, al.ip_address AS "ipAddress", al.created_at AS "createdAt",
              a.email AS "adminEmail"
         FROM audit_logs al LEFT JOIN admins a ON a.id = al.admin_id
        ORDER BY al.created_at DESC LIMIT 1000`
    );
    return { auditLogs: result.rows };
  });
}
