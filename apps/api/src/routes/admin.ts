import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { requireAdmin, writeAudit } from "../auth.js";
import type { AppConfig } from "../config.js";
import type { Database } from "../db.js";
import { generateActivationCode, tokenHash } from "../security.js";

const UuidParams = z.object({ id: z.string().uuid() }).strict();
const CreateCode = z.object({
  licenseKind: z.enum(["one_year", "lifetime"]),
  preActivationExpiresAt: z.string().datetime({ offset: true }).nullable().optional(),
  customerName: z.string().trim().max(120).nullable().optional(),
  phoneNumber: z.string().trim().max(30).nullable().optional(),
  externalReference: z.string().trim().max(200).nullable().optional(),
  adminLabel: z.string().trim().max(120).nullable().optional(),
  internalNote: z.string().trim().max(2000).nullable().optional()
}).strict();
const ResetBody = z.object({ reason: z.string().trim().min(3).max(500) }).strict();
const SettingsBody = z.object({ trialEnabled: z.boolean() }).strict();

export async function registerAdminRoutes(app: FastifyInstance, db: Database, config: AppConfig): Promise<void> {
  app.get("/v1/admin/activation-codes", async (request, reply) => {
    if (!(await requireAdmin(request, reply, db, config))) return;
    const result = await db.query(
      `SELECT id, code_suffix AS "codeSuffix", license_kind AS "licenseKind", status,
              pre_activation_expires_at AS "preActivationExpiresAt", activated_at AS "activatedAt",
              grant_starts_at AS "grantStartsAt", grant_expires_at AS "grantExpiresAt",
              (bound_installation_id IS NOT NULL) AS "deviceBound", customer_name AS "customerName",
              phone_number AS "phoneNumber", external_reference AS "externalReference",
              admin_label AS "adminLabel", internal_note AS "internalNote", created_at AS "createdAt"
         FROM activation_codes ORDER BY created_at DESC LIMIT 500`
    );
    return { activationCodes: result.rows };
  });

  app.post("/v1/admin/activation-codes", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const parsed = CreateCode.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request" });
    const value = parsed.data;
    for (let attempt = 0; attempt < 5; attempt += 1) {
      const code = generateActivationCode();
      try {
        const result = await db.query<{ id: string }>(
          `INSERT INTO activation_codes
             (code_hash, code_suffix, license_kind, pre_activation_expires_at, customer_name,
              phone_number, external_reference, admin_label, internal_note, created_by)
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) RETURNING id`,
          [tokenHash(code, config.tokenPepper), code.slice(-6), value.licenseKind,
            value.preActivationExpiresAt ?? null, value.customerName ?? null, value.phoneNumber ?? null,
            value.externalReference ?? null, value.adminLabel ?? null, value.internalNote ?? null, admin.id]
        );
        const id = result.rows[0]!.id;
        await writeAudit(db, request, admin.id, "activation.create", "activation_code", id, {
          codeSuffix: code.slice(-6), licenseKind: value.licenseKind
        });
        return reply.code(201).send({ id, code });
      } catch (error) {
        if ((error as { code?: string }).code !== "23505") throw error;
      }
    }
    return reply.code(503).send({ error: "code_generation_failed" });
  });

  app.get("/v1/admin/activation-codes/:id", async (request, reply) => {
    if (!(await requireAdmin(request, reply, db, config))) return;
    const params = UuidParams.safeParse(request.params);
    if (!params.success) return reply.code(400).send({ error: "invalid_request" });
    const result = await db.query(
      `SELECT id, code_suffix AS "codeSuffix", license_kind AS "licenseKind", status,
              pre_activation_expires_at AS "preActivationExpiresAt", activated_at AS "activatedAt",
              grant_starts_at AS "grantStartsAt", grant_expires_at AS "grantExpiresAt",
              (bound_installation_id IS NOT NULL) AS "deviceBound", customer_name AS "customerName",
              phone_number AS "phoneNumber", external_reference AS "externalReference",
              admin_label AS "adminLabel", internal_note AS "internalNote", created_at AS "createdAt"
         FROM activation_codes WHERE id = $1`, [params.data.id]
    );
    if (!result.rows[0]) return reply.code(404).send({ error: "activation_not_found" });
    return { activationCode: result.rows[0] };
  });

  app.post("/v1/admin/activation-codes/:id/revoke", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    if (!params.success) return reply.code(400).send({ error: "invalid_request" });
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const result = await client.query<{ id: string }>(
        `UPDATE activation_codes SET status = 'revoked', revoked_at = now(), updated_at = now()
          WHERE id = $1 AND status <> 'revoked' RETURNING id`, [params.data.id]
      );
      if (!result.rows[0]) {
        await client.query("ROLLBACK");
        return reply.code(404).send({ error: "activation_not_found" });
      }
      await client.query("UPDATE license_sessions SET revoked_at = now() WHERE activation_code_id = $1 AND revoked_at IS NULL", [params.data.id]);
      await writeAudit(client, request, admin.id, "activation.revoke", "activation_code", params.data.id);
      await client.query("COMMIT");
      return { id: params.data.id, status: "revoked" };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  });

  app.post("/v1/admin/activation-codes/:id/reset-device", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config, ["owner", "admin", "support"]);
    if (!admin) return;
    const params = UuidParams.safeParse(request.params);
    const body = ResetBody.safeParse(request.body);
    if (!params.success || !body.success) return reply.code(400).send({ error: "invalid_request" });
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const current = (await client.query<{ bound_installation_id: string | null; status: string; grant_expires_at: Date | null }>(
        "SELECT bound_installation_id, status, grant_expires_at FROM activation_codes WHERE id = $1 FOR UPDATE",
        [params.data.id]
      )).rows[0];
      if (!current) {
        await client.query("ROLLBACK");
        return reply.code(404).send({ error: "activation_not_found" });
      }
      if (current.status === "revoked" || (current.grant_expires_at && current.grant_expires_at <= new Date())) {
        await client.query("ROLLBACK");
        return reply.code(409).send({ error: "activation_inactive" });
      }
      await client.query("UPDATE license_sessions SET revoked_at = now() WHERE activation_code_id = $1 AND revoked_at IS NULL", [params.data.id]);
      await client.query("UPDATE activation_codes SET bound_installation_id = NULL, updated_at = now() WHERE id = $1", [params.data.id]);
      await writeAudit(client, request, admin.id, "activation.reset_device", "activation_code", params.data.id, {
        reasonProvided: body.data.reason.length > 0,
        previousBinding: current.bound_installation_id ? "present" : "none"
      });
      await client.query("COMMIT");
      return { id: params.data.id, deviceBound: false };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  });

  app.get("/v1/admin/app-settings", async (request, reply) => {
    if (!(await requireAdmin(request, reply, db, config))) return;
    const row = (await db.query<{ trial_enabled: boolean }>("SELECT trial_enabled FROM app_settings WHERE singleton = true")).rows[0]!;
    return { trialEnabled: row.trial_enabled };
  });

  app.patch("/v1/admin/app-settings", async (request, reply) => {
    const admin = await requireAdmin(request, reply, db, config, ["owner", "admin"]);
    if (!admin) return;
    const parsed = SettingsBody.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request" });
    await db.query("UPDATE app_settings SET trial_enabled = $1, updated_at = now() WHERE singleton = true", [parsed.data.trialEnabled]);
    await writeAudit(db, request, admin.id, "settings.update", "app_settings", null, { trialEnabled: parsed.data.trialEnabled });
    return { trialEnabled: parsed.data.trialEnabled };
  });

  app.get("/v1/admin/audit-logs", async (request, reply) => {
    if (!(await requireAdmin(request, reply, db, config, ["owner", "admin"]))) return;
    const [result, integrity] = await Promise.all([db.query(
      `SELECT al.id, al.action, al.entity_type AS "entityType", al.entity_id AS "entityId",
              al.metadata, al.ip_address AS "ipAddress", al.created_at AS "createdAt",
              a.email AS "adminEmail"
         FROM audit_logs al LEFT JOIN admins a ON a.id = al.admin_id
        ORDER BY al.created_at DESC LIMIT 1000`
    ), db.query<{ valid: boolean }>("SELECT verify_audit_log_chain() AS valid")]);
    return { auditLogs: result.rows, integrityVerified: integrity.rows[0]?.valid === true };
  });
}
