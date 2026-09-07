import type { FastifyInstance, FastifyRequest } from "fastify";
import { z } from "zod";
import type { AppConfig } from "../config.js";
import type { Database } from "../db.js";
import { decryptSecret, normalizeActivationCode, randomToken, tokenHash } from "../security.js";

const ActivateBody = z.object({
  code: z.string().min(8).max(32),
  deviceId: z.string().trim().min(8).max(256),
  platform: z.enum(["android", "android_tv", "ios", "tvos", "windows", "macos", "web", "other"]),
  model: z.string().trim().max(160).nullable().optional(),
  appVersion: z.string().trim().min(1).max(40)
});

type ConnectionRow = {
  activation_id: string;
  activation_status: "unused" | "active" | "revoked" | "expired";
  device_limit: number;
  starts_at: Date | null;
  activation_expires_at: Date | null;
  customer_status: "active" | "suspended" | "archived";
  account_id: string;
  account_active: boolean;
  account_expires_at: Date | null;
  host_active: boolean;
  protocol: "xtream" | "m3u" | "stalker";
  base_url: string;
  encrypted_username: string | null;
  encrypted_password: string | null;
  encrypted_playlist_url: string | null;
  encrypted_portal_data: string | null;
};

function bearerToken(request: FastifyRequest): string | null {
  const authorization = request.headers.authorization;
  return authorization?.startsWith("Bearer ") ? authorization.slice(7) : null;
}

function connectionPayload(row: ConnectionRow, key: Buffer) {
  return {
    protocol: row.protocol,
    baseUrl: row.base_url,
    username: decryptSecret(row.encrypted_username, key),
    password: decryptSecret(row.encrypted_password, key),
    playlistUrl: decryptSecret(row.encrypted_playlist_url, key),
    portalData: decryptSecret(row.encrypted_portal_data, key)
  };
}

export async function registerPlayerRoutes(
  app: FastifyInstance,
  db: Database,
  config: AppConfig
): Promise<void> {
  app.post("/v1/player/activate", {
    config: { rateLimit: { max: 12, timeWindow: "15 minutes" } }
  }, async (request, reply) => {
    const parsed = ActivateBody.safeParse(request.body);
    if (!parsed.success) return reply.code(400).send({ error: "invalid_request" });
    const value = parsed.data;
    const code = normalizeActivationCode(value.code);
    if (!code) return reply.code(400).send({ error: "invalid_activation_code" });

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const activation = await client.query<ConnectionRow>(
        `SELECT ac.id AS activation_id, ac.status AS activation_status, ac.device_limit,
                ac.starts_at, ac.expires_at AS activation_expires_at, c.status AS customer_status,
                pa.id AS account_id, pa.is_active AS account_active, pa.expires_at AS account_expires_at,
                ph.is_active AS host_active, ph.protocol, ph.base_url, pa.encrypted_username,
                pa.encrypted_password, pa.encrypted_playlist_url, pa.encrypted_portal_data
           FROM activation_codes ac
           JOIN customers c ON c.id = ac.customer_id
           JOIN provider_accounts pa ON pa.id = ac.provider_account_id
           JOIN provider_hosts ph ON ph.id = pa.host_id
          WHERE ac.code_hash = $1 FOR UPDATE OF ac`,
        [tokenHash(code, config.tokenPepper)]
      );
      const row = activation.rows[0];
      if (!row) {
        await client.query("ROLLBACK");
        return reply.code(401).send({ error: "activation_rejected" });
      }

      const now = new Date();
      if (row.activation_expires_at && row.activation_expires_at <= now) {
        await client.query("UPDATE activation_codes SET status = 'expired', updated_at = now() WHERE id = $1", [row.activation_id]);
        await client.query("COMMIT");
        return reply.code(403).send({ error: "activation_expired" });
      }
      if (row.activation_status === "revoked" || row.activation_status === "expired") {
        await client.query("ROLLBACK");
        return reply.code(403).send({ error: "activation_inactive" });
      }
      if (row.starts_at && row.starts_at > now) {
        await client.query("ROLLBACK");
        return reply.code(403).send({ error: "activation_not_started" });
      }
      if (row.customer_status !== "active" || !row.account_active || !row.host_active ||
          (row.account_expires_at && row.account_expires_at <= now)) {
        await client.query("ROLLBACK");
        return reply.code(403).send({ error: "subscription_inactive" });
      }

      const fingerprintHash = tokenHash(`device:${value.deviceId}`, config.tokenPepper);
      const existing = await client.query<{ id: string; status: "active" | "blocked" }>(
        `SELECT id, status FROM devices
          WHERE activation_code_id = $1 AND device_fingerprint_hash = $2`,
        [row.activation_id, fingerprintHash]
      );
      let deviceId = existing.rows[0]?.id;
      if (existing.rows[0]?.status === "blocked") {
        await client.query("ROLLBACK");
        return reply.code(403).send({ error: "device_blocked" });
      }
      if (!deviceId) {
        const count = await client.query<{ count: number }>(
          `SELECT count(*)::int AS count FROM devices
            WHERE activation_code_id = $1 AND status = 'active'`, [row.activation_id]
        );
        if ((count.rows[0]?.count ?? 0) >= row.device_limit) {
          await client.query("ROLLBACK");
          return reply.code(409).send({ error: "device_limit_reached" });
        }
        const inserted = await client.query<{ id: string }>(
          `INSERT INTO devices
           (activation_code_id, device_fingerprint_hash, platform, model, app_version)
           VALUES ($1,$2,$3,$4,$5) RETURNING id`,
          [row.activation_id, fingerprintHash, value.platform, value.model ?? null, value.appVersion]
        );
        deviceId = inserted.rows[0]!.id;
      } else {
        await client.query(
          `UPDATE devices SET platform = $2, model = $3, app_version = $4, last_seen_at = now()
            WHERE id = $1`, [deviceId, value.platform, value.model ?? null, value.appVersion]
        );
      }

      const sessionToken = randomToken();
      await client.query("UPDATE player_sessions SET revoked_at = now() WHERE device_id = $1 AND revoked_at IS NULL", [deviceId]);
      await client.query(
        `INSERT INTO player_sessions (device_id, token_hash, expires_at)
         VALUES ($1, $2, now() + interval '30 days')`,
        [deviceId, tokenHash(sessionToken, config.tokenPepper)]
      );
      await client.query(
        `UPDATE activation_codes SET status = 'active', activated_at = COALESCE(activated_at, now()),
                updated_at = now() WHERE id = $1`, [row.activation_id]
      );
      await client.query("COMMIT");

      return {
        sessionToken,
        expiresIn: 30 * 24 * 60 * 60,
        connection: connectionPayload(row, config.encryptionKey)
      };
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  });

  app.get("/v1/player/config", {
    config: { rateLimit: { max: 60, timeWindow: "1 minute" } }
  }, async (request, reply) => {
    const token = bearerToken(request);
    if (!token) return reply.code(401).send({ error: "authentication_required" });
    const result = await db.query<ConnectionRow & { device_id: string }>(
      `SELECT d.id AS device_id, ac.id AS activation_id, ac.status AS activation_status,
              ac.device_limit, ac.starts_at, ac.expires_at AS activation_expires_at,
              c.status AS customer_status, pa.id AS account_id, pa.is_active AS account_active,
              pa.expires_at AS account_expires_at, ph.is_active AS host_active, ph.protocol,
              ph.base_url, pa.encrypted_username, pa.encrypted_password,
              pa.encrypted_playlist_url, pa.encrypted_portal_data
         FROM player_sessions ps
         JOIN devices d ON d.id = ps.device_id
         JOIN activation_codes ac ON ac.id = d.activation_code_id
         JOIN customers c ON c.id = ac.customer_id
         JOIN provider_accounts pa ON pa.id = ac.provider_account_id
         JOIN provider_hosts ph ON ph.id = pa.host_id
        WHERE ps.token_hash = $1 AND ps.revoked_at IS NULL AND ps.expires_at > now()
          AND d.status = 'active' AND ac.status = 'active' AND c.status = 'active'
          AND pa.is_active = true AND ph.is_active = true
          AND (ac.expires_at IS NULL OR ac.expires_at > now())
          AND (pa.expires_at IS NULL OR pa.expires_at > now())`,
      [tokenHash(token, config.tokenPepper)]
    );
    const row = result.rows[0];
    if (!row) return reply.code(401).send({ error: "invalid_session" });
    await Promise.all([
      db.query("UPDATE player_sessions SET last_seen_at = now() WHERE token_hash = $1", [tokenHash(token, config.tokenPepper)]),
      db.query("UPDATE devices SET last_seen_at = now() WHERE id = $1", [row.device_id])
    ]);
    return { connection: connectionPayload(row, config.encryptionKey) };
  });
}
