import assert from "node:assert/strict";
import type { FastifyInstance } from "fastify";
import type { Pool } from "pg";

type Summary = { totalCodes: number; availableCodes: number; activeLicenses: number; boundDevices: number };

/** Serial fixture: the existing lifecycle test owns the disposable database. */
export async function assertAdminDashboardSummary(
  app: FastifyInstance, ownerDb: Pool, cookie: string, supportToken: string
): Promise<void> {
  const list = () => app.inject({
    method: "GET", url: "/v1/admin/activation-codes", headers: { cookie }
  });
  const unauthorized = await app.inject({ method: "GET", url: "/v1/admin/activation-codes" });
  assert.equal(unauthorized.statusCode, 401);
  assert.equal(unauthorized.json().summary, undefined);

  const original = await list();
  assert.equal(original.statusCode, 200);
  const baseline = original.json().summary as Summary;
  assert.ok(baseline);
  for (const value of Object.values(baseline)) {
    assert.equal(typeof value, "number");
    assert.ok(Number.isInteger(value) && value >= 0);
  }

  const owner = await ownerDb.connect();
  let committed = false;
  try {
    await owner.query("BEGIN");
    const installations = await owner.query<{ id: string }>(
      `INSERT INTO installations (installation_hash, platform, app_version)
       VALUES ('test-dashboard-summary-device-a', 'android', 'test'),
              ('test-dashboard-summary-device-b', 'android', 'test')
       RETURNING id`
    );
    await owner.query(
      `INSERT INTO activation_codes (code_hash, code_suffix, license_kind)
       SELECT 'test-dashboard-summary-unused-' || item, 'STAT00', 'lifetime'
         FROM generate_series(1, 512) AS item`
    );
    await owner.query(
      `INSERT INTO activation_codes
         (code_hash, code_suffix, license_kind, status, pre_activation_expires_at,
          activated_at, grant_starts_at, grant_expires_at, bound_installation_id, created_at)
       SELECT 'test-dashboard-summary-' || sample.name, 'STAT00', sample.kind, sample.status,
              sample.deadline, sample.activated, sample.starts, sample.expires,
              sample.binding, now() - interval '1 year'
         FROM (VALUES
           ('unused-expired', 'lifetime', 'unused', now() - interval '1 day', NULL, NULL, NULL, NULL),
           ('unused-future', 'lifetime', 'unused', now() + interval '1 day', NULL, NULL, NULL, NULL),
           ('annual-active', 'one_year', 'active', now() - interval '1 day', now() - interval '1 day', now() - interval '1 day', now() + interval '1 day', $1::uuid),
           ('lifetime-active', 'lifetime', 'active', now() - interval '1 day', now() - interval '1 day', now() - interval '1 day', NULL, $1::uuid),
           ('lifetime-unbound', 'lifetime', 'active', NULL, now() - interval '1 day', now() - interval '1 day', NULL, NULL),
           ('annual-expired', 'one_year', 'active', NULL, now() - interval '2 days', now() - interval '2 days', now() - interval '1 day', $2::uuid),
           ('lifetime-revoked', 'lifetime', 'revoked', NULL, now() - interval '1 day', now() - interval '1 day', NULL, $2::uuid),
           ('lifetime-future', 'lifetime', 'active', NULL, now(), now() + interval '1 day', NULL, $2::uuid),
           ('annual-incomplete', 'one_year', 'active', NULL, NULL, NULL, now() + interval '1 day', $2::uuid)
         ) AS sample(name, kind, status, deadline, activated, starts, expires, binding)`,
      installations.rows.map(row => row.id)
    );
    await owner.query("COMMIT");
    committed = true;

    const response = await list();
    assert.equal(response.statusCode, 200);
    const data = response.json() as { activationCodes: { status: string }[]; summary: Summary };
    // Old list consumers still receive a bounded array; valid old grants are outside it.
    assert.equal(data.activationCodes.length, 500);
    assert.ok(data.activationCodes.every(code => code.status === "unused"));
    assert.deepEqual(data.summary, {
      totalCodes: baseline.totalCodes + 521,
      availableCodes: baseline.availableCodes + 513,
      activeLicenses: baseline.activeLicenses + 3,
      boundDevices: baseline.boundDevices + 1
    });

    const support = await app.inject({
      method: "GET", url: "/v1/admin/activation-codes",
      headers: { authorization: `Bearer ${supportToken}` }
    });
    assert.equal(support.statusCode, 200);
    assert.deepEqual(support.json().summary, data.summary);
    assert.deepEqual(Object.keys(data.summary).sort(),
      ["activeLicenses", "availableCodes", "boundDevices", "totalCodes"]);
  } finally {
    try {
      if (committed) {
        await owner.query("BEGIN");
        await owner.query("DELETE FROM activation_codes WHERE code_hash LIKE 'test-dashboard-summary-%'");
        await owner.query("DELETE FROM installations WHERE installation_hash LIKE 'test-dashboard-summary-%'");
        await owner.query("COMMIT");
      } else {
        await owner.query("ROLLBACK");
      }
    } catch (error) {
      await owner.query("ROLLBACK");
      throw error;
    } finally {
      owner.release();
    }
  }
  const restored = await list();
  assert.equal(restored.statusCode, 200);
  assert.deepEqual(restored.json().summary, baseline);
}
