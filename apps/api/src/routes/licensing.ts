import type { FastifyInstance, FastifyRequest } from "fastify";
import { z } from "zod";
import type { AppConfig } from "../config.js";
import type { Database } from "../db.js";
import { addHours, ensureInstallation, entitlementPayload, issueSession, LicensingError, type EntitlementKind } from "../licensing.js";
import { normalizeActivationCode, tokenHash } from "../security.js";

const InstallationBody = z.object({
  installationId: z.string().min(32).max(128).regex(/^[A-Za-z0-9_-]+$/),
  platform: z.literal("android"),
  appVersion: z.string().trim().min(1).max(40)
}).strict();

const ActivationBody = InstallationBody.extend({ activationCode: z.string().min(1).max(64) }).strict();

type TrialRow = { id: string; starts_at: Date; expires_at: Date; revoked_at: Date | null };
type ActivationRow = {
  id: string;
  license_kind: "one_year" | "lifetime";
  status: "unused" | "active" | "revoked";
  pre_activation_expires_at: Date | null;
  activated_at: Date | null;
  grant_starts_at: Date | null;
  grant_expires_at: Date | null;
  bound_installation_id: string | null;
};

function bearerToken(request: FastifyRequest): string | null {
  const value = request.headers.authorization;
  return value?.startsWith("Bearer ") && value.length > 7 ? value.slice(7) : null;
}

function success(requestId: string, now: Date, entitlement: ReturnType<typeof entitlementPayload>, session: { token: string; refreshAfter: string }) {
  return { serverTime: now.toISOString(), requestId, entitlement, session };
}

function sendError(reply: { code: (status: number) => { send: (body: unknown) => unknown } }, requestId: string, error: LicensingError) {
  return reply.code(error.statusCode).send({
    serverTime: new Date().toISOString(),
    requestId,
    error: { code: error.code, message: error.code, retryable: error.retryable }
  });
}

export async function registerLicensingRoutes(app: FastifyInstance, db: Database, config: AppConfig): Promise<void> {
  app.post("/v1/licensing/trials/start", {
    config: { rateLimit: { max: 5, timeWindow: "1 hour" } }
  }, async (request, reply) => {
    const parsed = InstallationBody.safeParse(request.body);
    if (!parsed.success) return sendError(reply, request.id, new LicensingError(400, "INVALID_REQUEST"));
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const installationId = await ensureInstallation(client, parsed.data, config);
      const paid = await client.query("SELECT 1 FROM activation_codes WHERE bound_installation_id = $1 AND status = 'active'", [installationId]);
      if (paid.rowCount) throw new LicensingError(409, "TRIAL_ALREADY_USED");
      const settings = await client.query<{ trial_enabled: boolean }>("SELECT trial_enabled FROM app_settings WHERE singleton = true");
      if (!settings.rows[0]?.trial_enabled) throw new LicensingError(403, "TRIAL_UNAVAILABLE");
      let trial = (await client.query<TrialRow>(
        "SELECT id, starts_at, expires_at, revoked_at FROM trial_entitlements WHERE installation_id = $1 FOR UPDATE",
        [installationId]
      )).rows[0];
      const now = new Date();
      if (!trial) {
        trial = (await client.query<TrialRow>(
          `INSERT INTO trial_entitlements (installation_id, starts_at, expires_at)
           VALUES ($1, $2, $3) RETURNING id, starts_at, expires_at, revoked_at`,
          [installationId, now, addHours(now, 168)]
        )).rows[0]!;
      }
      if (trial.revoked_at || trial.expires_at <= now) throw new LicensingError(409, "TRIAL_ALREADY_USED");
      const session = await issueSession(client, installationId, config, now, { trialEntitlementId: trial.id });
      await client.query("COMMIT");
      return success(request.id, now, entitlementPayload("trial", trial.starts_at, trial.expires_at, now), session);
    } catch (error) {
      await client.query("ROLLBACK");
      if (error instanceof LicensingError) return sendError(reply, request.id, error);
      throw error;
    } finally {
      client.release();
    }
  });

  app.post("/v1/licensing/activations", {
    config: { rateLimit: { max: 8, timeWindow: "15 minutes" } }
  }, async (request, reply) => {
    const parsed = ActivationBody.safeParse(request.body);
    if (!parsed.success) return sendError(reply, request.id, new LicensingError(400, "INVALID_REQUEST"));
    const code = normalizeActivationCode(parsed.data.activationCode);
    if (!code) return sendError(reply, request.id, new LicensingError(422, "ACTIVATION_REJECTED"));
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const activation = (await client.query<ActivationRow>(
        `SELECT id, license_kind, status, pre_activation_expires_at, activated_at,
                grant_starts_at, grant_expires_at, bound_installation_id
           FROM activation_codes WHERE code_hash = $1 FOR UPDATE`,
        [tokenHash(code, config.tokenPepper)]
      )).rows[0];
      const now = new Date();
      if (!activation || activation.status === "revoked" ||
          (activation.pre_activation_expires_at && activation.pre_activation_expires_at <= now)) {
        throw new LicensingError(422, "ACTIVATION_REJECTED");
      }
      const installationId = await ensureInstallation(client, parsed.data, config);
      if (activation.bound_installation_id && activation.bound_installation_id !== installationId) {
        throw new LicensingError(409, "DEVICE_LIMIT_REACHED");
      }
      if (activation.grant_expires_at && activation.grant_expires_at <= now) {
        throw new LicensingError(403, "ENTITLEMENT_EXPIRED");
      }
      let startsAt = activation.grant_starts_at;
      let expiresAt = activation.grant_expires_at;
      if (!startsAt) {
        startsAt = now;
        expiresAt = activation.license_kind === "one_year" ? addHours(now, 365 * 24) : null;
      }
      await client.query(
        `UPDATE activation_codes SET status = 'active', activated_at = COALESCE(activated_at, $2),
           grant_starts_at = COALESCE(grant_starts_at, $2), grant_expires_at = COALESCE(grant_expires_at, $3),
           bound_installation_id = $4, updated_at = now() WHERE id = $1`,
        [activation.id, now, expiresAt, installationId]
      );
      const session = await issueSession(client, installationId, config, now, { activationCodeId: activation.id });
      await client.query("COMMIT");
      return success(request.id, now, entitlementPayload(activation.license_kind, startsAt, expiresAt, now), session);
    } catch (error) {
      await client.query("ROLLBACK");
      if (error instanceof LicensingError) return sendError(reply, request.id, error);
      throw error;
    } finally {
      client.release();
    }
  });

  app.post("/v1/licensing/entitlements/refresh", {
    config: { rateLimit: { max: 20, timeWindow: "1 hour" } }
  }, async (request, reply) => {
    const parsed = InstallationBody.safeParse(request.body);
    const token = bearerToken(request);
    if (!parsed.success) return sendError(reply, request.id, new LicensingError(400, "INVALID_REQUEST"));
    if (!token) return sendError(reply, request.id, new LicensingError(401, "SESSION_INVALID"));
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const installationHash = tokenHash(`installation:${parsed.data.installationId}`, config.tokenPepper);
      const session = (await client.query<{
        id: string; installation_id: string; activation_code_id: string | null; trial_entitlement_id: string | null;
      }>(
        `SELECT s.id, s.installation_id, s.activation_code_id, s.trial_entitlement_id
           FROM license_sessions s JOIN installations i ON i.id = s.installation_id
          WHERE s.token_hash = $1 AND s.revoked_at IS NULL AND s.expires_at > now()
            AND i.installation_hash = $2 FOR UPDATE OF s`,
        [tokenHash(token, config.tokenPepper), installationHash]
      )).rows[0];
      if (!session) throw new LicensingError(401, "SESSION_INVALID");
      const now = new Date();
      let kind: EntitlementKind;
      let startsAt: Date;
      let expiresAt: Date | null;
      if (session.activation_code_id) {
        const row = (await client.query<ActivationRow>(
          `SELECT id, license_kind, status, pre_activation_expires_at, activated_at,
                  grant_starts_at, grant_expires_at, bound_installation_id
             FROM activation_codes WHERE id = $1 FOR UPDATE`, [session.activation_code_id]
        )).rows[0];
        if (!row || row.status === "revoked" || row.bound_installation_id !== session.installation_id) {
          throw new LicensingError(403, "ENTITLEMENT_REVOKED");
        }
        if (!row.grant_starts_at) throw new LicensingError(401, "SESSION_INVALID");
        if (row.grant_expires_at && row.grant_expires_at <= now) throw new LicensingError(403, "ENTITLEMENT_EXPIRED");
        kind = row.license_kind;
        startsAt = row.grant_starts_at;
        expiresAt = row.grant_expires_at;
      } else if (session.trial_entitlement_id) {
        const row = (await client.query<TrialRow>(
          "SELECT id, starts_at, expires_at, revoked_at FROM trial_entitlements WHERE id = $1 FOR UPDATE",
          [session.trial_entitlement_id]
        )).rows[0];
        if (!row || row.revoked_at) throw new LicensingError(403, "ENTITLEMENT_REVOKED");
        if (row.expires_at <= now) throw new LicensingError(403, "ENTITLEMENT_EXPIRED");
        kind = "trial";
        startsAt = row.starts_at;
        expiresAt = row.expires_at;
      } else {
        throw new LicensingError(401, "SESSION_INVALID");
      }
      await client.query("UPDATE license_sessions SET revoked_at = now() WHERE id = $1", [session.id]);
      const nextSession = await issueSession(client, session.installation_id, config, now,
        session.activation_code_id ? { activationCodeId: session.activation_code_id } : { trialEntitlementId: session.trial_entitlement_id! });
      await client.query("UPDATE installations SET app_version = $2, last_seen_at = now() WHERE id = $1", [session.installation_id, parsed.data.appVersion]);
      await client.query("COMMIT");
      return success(request.id, now, entitlementPayload(kind, startsAt, expiresAt, now), nextSession);
    } catch (error) {
      await client.query("ROLLBACK");
      if (error instanceof LicensingError) return sendError(reply, request.id, error);
      throw error;
    } finally {
      client.release();
    }
  });

  app.post("/v1/licensing/sessions/revoke", async (request, reply) => {
    const token = bearerToken(request);
    if (!token) return sendError(reply, request.id, new LicensingError(401, "SESSION_INVALID"));
    await db.query("UPDATE license_sessions SET revoked_at = now() WHERE token_hash = $1 AND revoked_at IS NULL", [tokenHash(token, config.tokenPepper)]);
    return reply.code(204).send();
  });
}
