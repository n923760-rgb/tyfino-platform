import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { fileURLToPath } from "node:url";
import pg from "pg";
import { buildApp } from "./app.js";
import type { AppConfig } from "./config.js";
import { totpCodeAt } from "./security.js";

const databaseUrl = process.env.TEST_DATABASE_URL;

test("licensing API enforces the V1 boundary and lifecycle", { skip: !databaseUrl }, async () => {
  const db = new pg.Pool({ connectionString: databaseUrl! });
  const schemaPath = fileURLToPath(new URL("../../../database/init/001_schema.sql", import.meta.url));
  await db.query("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
  await db.query(await readFile(schemaPath, "utf8"));
  const config: AppConfig = {
    port: 3000,
    databaseUrl: databaseUrl!,
    logLevel: "silent",
    trustProxy: false,
    appEnv: "test",
    adminOrigin: "https://admin.test",
    tokenPepper: "test-pepper-that-is-at-least-thirty-two-characters",
    bootstrapEmail: "owner@tyfino.test",
    bootstrapPassword: "test-password-long-enough",
    ownerTotpSecret: "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
  };
  const app = await buildApp(config, db);
  try {
    const liveness = await app.inject({ method: "GET", url: "/healthz" });
    assert.equal(liveness.statusCode, 200);
    assert.equal(liveness.json().status, "ok");
    const readiness = await app.inject({ method: "GET", url: "/readyz" });
    assert.equal(readiness.statusCode, 200);
    assert.deepEqual(readiness.json(), { status: "ready", database: "connected", schema: "current" });

    const login = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/login",
      payload: { email: config.bootstrapEmail, password: config.bootstrapPassword }
    });
    assert.equal(login.statusCode, 202);
    assert.equal(login.cookies.length, 0);
    const challenge = login.json() as { twoFactorRequired: boolean; challengeToken: string };
    assert.equal(challenge.twoFactorRequired, true);
    const validTotpCode = totpCodeAt(config.ownerTotpSecret!, Date.now());
    assert.ok(validTotpCode);

    const rejectedCode = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/verify-totp",
      payload: { challengeToken: challenge.challengeToken, code: validTotpCode === "000000" ? "000001" : "000000" }
    });
    assert.equal(rejectedCode.statusCode, 401);
    assert.equal(rejectedCode.json().error, "invalid_two_factor_code");

    const verified = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/verify-totp",
      payload: {
        challengeToken: challenge.challengeToken,
        code: validTotpCode
      }
    });
    assert.equal(verified.statusCode, 200);
    const cookie = verified.cookies.find((item) => item.name === "tyfino_admin_session");
    assert.ok(cookie);

    const consumedChallenge = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/verify-totp",
      payload: { challengeToken: challenge.challengeToken, code: validTotpCode }
    });
    assert.equal(consumedChallenge.statusCode, 401);
    assert.equal(consumedChallenge.json().error, "invalid_two_factor_challenge");

    const replayLogin = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/login",
      payload: { email: config.bootstrapEmail, password: config.bootstrapPassword }
    });
    assert.equal(replayLogin.statusCode, 202);
    const replayChallenge = replayLogin.json() as { challengeToken: string };
    const replayedCounter = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/verify-totp",
      payload: { challengeToken: replayChallenge.challengeToken, code: validTotpCode }
    });
    assert.equal(replayedCounter.statusCode, 401);
    assert.equal(replayedCounter.json().error, "invalid_two_factor_code");

    const issued = await app.inject({
      method: "POST",
      url: "/v1/admin/activation-codes",
      headers: { cookie: `tyfino_admin_session=${cookie.value}` },
      payload: { licenseKind: "one_year", customerName: "Test customer" }
    });
    assert.equal(issued.statusCode, 201);
    const issuedBody = issued.json() as { id: string; code: string };
    const activationCode = issuedBody.code;
    assert.match(activationCode, /^TYF-[0-9A-HJKMNP-TV-Z-]+$/);

    const installationA = "A".repeat(43);
    const installationB = "B".repeat(43);
    const rejectedProviderField = await app.inject({
      method: "POST",
      url: "/v1/licensing/trials/start",
      payload: { installationId: installationA, platform: "android", appVersion: "1.0.0", host: "https://iptv.invalid" }
    });
    assert.equal(rejectedProviderField.statusCode, 400);
    assert.equal(rejectedProviderField.json().error.code, "INVALID_REQUEST");

    const trial = await app.inject({
      method: "POST",
      url: "/v1/licensing/trials/start",
      payload: { installationId: installationA, platform: "android", appVersion: "1.0.0" }
    });
    assert.equal(trial.statusCode, 200);
    assert.equal(trial.json().entitlement.kind, "trial");

    const activated = await app.inject({
      method: "POST",
      url: "/v1/licensing/activations",
      payload: { activationCode, installationId: installationA, platform: "android", appVersion: "1.0.0" }
    });
    assert.equal(activated.statusCode, 200);
    assert.equal(activated.json().entitlement.kind, "one_year");

    const secondDevice = await app.inject({
      method: "POST",
      url: "/v1/licensing/activations",
      payload: { activationCode, installationId: installationB, platform: "android", appVersion: "1.0.0" }
    });
    assert.equal(secondDevice.statusCode, 409);
    assert.equal(secondDevice.json().error.code, "DEVICE_LIMIT_REACHED");

    const reset = await app.inject({
      method: "POST",
      url: `/v1/admin/activation-codes/${issuedBody.id}/reset-device`,
      headers: { cookie: `tyfino_admin_session=${cookie.value}` },
      payload: { reason: "Customer replaced the device" }
    });
    assert.equal(reset.statusCode, 200);

    const replacementDevice = await app.inject({
      method: "POST",
      url: "/v1/licensing/activations",
      payload: { activationCode, installationId: installationB, platform: "android", appVersion: "1.0.0" }
    });
    assert.equal(replacementDevice.statusCode, 200);
    const replacementSession = replacementDevice.json().session.token as string;

    const refreshed = await app.inject({
      method: "POST",
      url: "/v1/licensing/entitlements/refresh",
      headers: { authorization: `Bearer ${replacementSession}` },
      payload: { installationId: installationB, platform: "android", appVersion: "1.0.1" }
    });
    assert.equal(refreshed.statusCode, 200);
    assert.equal(refreshed.json().entitlement.kind, "one_year");

    await Promise.all(Array.from({ length: 12 }, (_, index) => db.query(
      `INSERT INTO audit_logs (action, entity_type, entity_id, metadata)
       VALUES ('audit.concurrent_test', 'test', $1, $2::jsonb)`,
      [String(index), JSON.stringify({ index })]
    )));

    const audit = await app.inject({
      method: "GET",
      url: "/v1/admin/audit-logs",
      headers: { cookie: `tyfino_admin_session=${cookie.value}` }
    });
    assert.equal(audit.statusCode, 200);
    assert.equal(audit.json().integrityVerified, true);
    assert.ok((audit.json().auditLogs as unknown[]).length > 0);
    await assert.rejects(
      db.query("UPDATE audit_logs SET action = 'tampered' WHERE id = (SELECT min(id) FROM audit_logs)"),
      (error: unknown) => (error as { code?: string }).code === "55000"
    );
    await assert.rejects(
      db.query("DELETE FROM audit_logs WHERE id = (SELECT min(id) FROM audit_logs)"),
      (error: unknown) => (error as { code?: string }).code === "55000"
    );
    const integrityAfterRejectedMutations = await db.query<{ valid: boolean }>(
      "SELECT verify_audit_log_chain() AS valid"
    );
    assert.equal(integrityAfterRejectedMutations.rows[0]?.valid, true);

    const privilegedClient = await db.connect();
    try {
      await privilegedClient.query("BEGIN");
      await privilegedClient.query("ALTER TABLE audit_logs DISABLE TRIGGER audit_logs_append_only");
      await privilegedClient.query("UPDATE audit_logs SET action = 'tampered' WHERE id = (SELECT min(id) FROM audit_logs)");
      const tamperedIntegrity = await privilegedClient.query<{ valid: boolean }>(
        "SELECT verify_audit_log_chain() AS valid"
      );
      assert.equal(tamperedIntegrity.rows[0]?.valid, false);
    } finally {
      await privilegedClient.query("ROLLBACK");
      privilegedClient.release();
    }

    const legacyPlayer = await app.inject({ method: "GET", url: "/v1/player/config" });
    assert.equal(legacyPlayer.statusCode, 404);
    const forbiddenTables = await db.query<{ count: string }>(
      `SELECT count(*)::text AS count FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name IN ('provider_hosts', 'provider_accounts', 'player_sessions')`
    );
    assert.equal(forbiddenTables.rows[0]?.count, "0");

    await db.query("DROP TABLE app_settings");
    const missingSchemaReadiness = await app.inject({ method: "GET", url: "/readyz" });
    assert.equal(missingSchemaReadiness.statusCode, 503);
    assert.deepEqual(missingSchemaReadiness.json(), { status: "not_ready" });
    const livenessWithoutReadiness = await app.inject({ method: "GET", url: "/healthz" });
    assert.equal(livenessWithoutReadiness.statusCode, 200);
  } finally {
    await app.close();
    await db.end();
  }
});
