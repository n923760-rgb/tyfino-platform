import type { PoolClient } from "pg";
import type { AppConfig } from "./config.js";
import { randomToken, tokenHash } from "./security.js";

const HOUR_MS = 3_600_000;
const SESSION_HOURS = 72;
const REFRESH_HOURS = 12;

export type EntitlementKind = "trial" | "one_year" | "lifetime";

export class LicensingError extends Error {
  constructor(public statusCode: number, public code: string, public retryable = false) {
    super(code);
  }
}

export function addHours(date: Date, hours: number): Date {
  return new Date(date.getTime() + hours * HOUR_MS);
}

export function entitlementPayload(kind: EntitlementKind, startsAt: Date, expiresAt: Date | null, now: Date) {
  const offlineLimit = addHours(now, SESSION_HOURS);
  const offlineValidUntil = expiresAt && expiresAt < offlineLimit ? expiresAt : offlineLimit;
  return {
    status: "active" as const,
    kind,
    startsAt: startsAt.toISOString(),
    expiresAt: expiresAt?.toISOString() ?? null,
    offlineValidUntil: offlineValidUntil.toISOString()
  };
}

export async function ensureInstallation(
  client: PoolClient,
  input: { installationId: string; platform: "android"; appVersion: string },
  config: AppConfig
): Promise<string> {
  const installationHash = tokenHash(`installation:${input.installationId}`, config.tokenPepper);
  const result = await client.query<{ id: string }>(
    `INSERT INTO installations (installation_hash, platform, app_version)
     VALUES ($1, $2, $3)
     ON CONFLICT (installation_hash) DO UPDATE SET
       platform = excluded.platform, app_version = excluded.app_version, last_seen_at = now()
     RETURNING id`,
    [installationHash, input.platform, input.appVersion]
  );
  return result.rows[0]!.id;
}

export async function issueSession(
  client: PoolClient,
  installationId: string,
  config: AppConfig,
  now: Date,
  owner: { activationCodeId?: string; trialEntitlementId?: string }
) {
  await client.query("UPDATE license_sessions SET revoked_at = now() WHERE installation_id = $1 AND revoked_at IS NULL", [installationId]);
  const token = randomToken();
  await client.query(
    `INSERT INTO license_sessions
       (installation_id, activation_code_id, trial_entitlement_id, token_hash, expires_at)
     VALUES ($1, $2, $3, $4, $5)`,
    [installationId, owner.activationCodeId ?? null, owner.trialEntitlementId ?? null,
      tokenHash(token, config.tokenPepper), addHours(now, SESSION_HOURS)]
  );
  return { token, refreshAfter: addHours(now, REFRESH_HOURS).toISOString() };
}
