import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { fileURLToPath } from "node:url";
import pg from "pg";
import { buildApp } from "./app.js";
import type { AppConfig } from "./config.js";

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
    bootstrapPassword: "test-password-long-enough"
  };
  const app = await buildApp(config, db);
  try {
    const login = await app.inject({
      method: "POST",
      url: "/v1/admin/auth/login",
      payload: { email: config.bootstrapEmail, password: config.bootstrapPassword }
    });
    assert.equal(login.statusCode, 200);
    const cookie = login.cookies.find((item) => item.name === "tyfino_admin_session");
    assert.ok(cookie);

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

    const legacyPlayer = await app.inject({ method: "GET", url: "/v1/player/config" });
    assert.equal(legacyPlayer.statusCode, 404);
    const forbiddenTables = await db.query<{ count: string }>(
      `SELECT count(*)::text AS count FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name IN ('provider_hosts', 'provider_accounts', 'player_sessions')`
    );
    assert.equal(forbiddenTables.rows[0]?.count, "0");
  } finally {
    await app.close();
    await db.end();
  }
});
